# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Run local failure-path checks; no running HugeGraph services are required."""
import ast
import contextlib
import copy
import hashlib
import http.client
import http.server
import importlib.util
import io
import json
import pathlib
import socket
import shutil
import subprocess
import sys
import urllib.parse
import tempfile
import threading
import time
import unittest
from unittest import mock

SCRIPTS = pathlib.Path(__file__).resolve().parent


def module(name):
    spec = importlib.util.spec_from_file_location(name, SCRIPTS / (name + '.py'))
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


TRACE = module('trace_proxy')
ASYNC = module('async_result_check')
BROWSER = module('browser_probe')
SMOKE = module('final_stack_smoke')
# The property probe is intentionally an executable fixture script. Load only
# its integrity helper, without sending its live benchmark requests on import.
node = next(n for n in ast.parse((SCRIPTS / 'large_property_probe.py').read_text()).body
            if isinstance(n, ast.FunctionDef) and n.name == 'verify_original_edges')
namespace = {'hashlib': hashlib}
exec(compile(ast.Module(body=[node], type_ignores=[]), 'edge_integrity_helper', 'exec'), namespace)
verify_edges = namespace['verify_original_edges']


@contextlib.contextmanager
def serving(server):
    thread = threading.Thread(target=lambda: server.serve_forever(poll_interval=0.01), daemon=True)
    thread.start()
    try:
        yield server.server_address[1]
    finally:
        server.shutdown()
        server.server_close()
        thread.join(timeout=2)
        assert not thread.is_alive()


class Upstream(http.server.BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-Length', '12')
        if self.path == '/folded':
            self.send_header('X-Probe', 'safe\r\n X-Evil: injected')
        self.end_headers()
        self.wfile.write(b'ok' if self.path == '/partial' else b'hello world!')
        self.close_connection = True


class ProxyFailureTest(unittest.TestCase):
    def setUp(self):
        self.events = []
        self.patch = mock.patch.object(TRACE, 'emit', self.events.append)
        self.patch.start()
        self.addCleanup(self.patch.stop)

    def get(self, port):
        connection = http.client.HTTPConnection('127.0.0.1', port, timeout=2)
        self.addCleanup(connection.close)
        connection.request('GET', '/partial')
        response = connection.getresponse()
        body = response.read()
        self.assertEqual(response.status, 502)
        self.assertEqual(response.getheader('Connection'), 'close')
        self.assertIn(b'Bad Gateway', body)

    def test_refused_upstream_returns_502_promptly(self):
        # Keep the port reserved but non-listening so a different process cannot
        # take it between selection and the proxy's connection attempt.
        with socket.socket() as upstream:
            upstream.bind(('127.0.0.1', 0))
            proxy = TRACE.create_http_proxy(0, upstream.getsockname()[1], 'test')
            started = time.monotonic()
            with serving(proxy) as port:
                self.get(port)
            self.assertLess(time.monotonic() - started, 2)
        self.assertEqual(self.events[-1]['status'], 502)

    def test_incomplete_upstream_body_returns_502(self):
        with serving(http.server.ThreadingHTTPServer(('127.0.0.1', 0), Upstream)) as upstream:
            with serving(TRACE.create_http_proxy(0, upstream, 'test')) as port:
                self.get(port)
        self.assertEqual(self.events[-1]['status'], 502)

    def test_folded_upstream_header_cannot_split_downstream_response(self):
        with serving(http.server.ThreadingHTTPServer(('127.0.0.1', 0), Upstream)) as upstream:
            with serving(TRACE.create_http_proxy(0, upstream, 'test')) as port:
                with contextlib.closing(http.client.HTTPConnection('127.0.0.1', port, timeout=2)) as connection:
                    connection.request('GET', '/folded')
                    response = connection.getresponse()
                    self.assertEqual(response.status, 200)
                    self.assertEqual(response.read(), b'hello world!')
                    self.assertNotIn('\r', response.getheader('X-Probe'))
                    self.assertNotIn('\n', response.getheader('X-Probe'))
                    self.assertIsNone(response.getheader('X-Evil'))

    def test_downstream_failure_after_headers_closes_without_second_status(self):
        class BrokenWriter:
            def __init__(self, original):
                self.original = original

            def write(self, data):
                raise ConnectionResetError('simulated downstream disconnect')

            def __getattr__(self, name):
                return getattr(self.original, name)

        with serving(http.server.ThreadingHTTPServer(('127.0.0.1', 0), Upstream)) as upstream:
            proxy = TRACE.create_http_proxy(0, upstream, 'test')
            handler = proxy.RequestHandlerClass

            class AfterHeadersFailure(handler):
                def end_headers(self):
                    super().end_headers()
                    self.wfile = BrokenWriter(self.wfile)

            proxy.RequestHandlerClass = AfterHeadersFailure
            with serving(proxy) as port:
                with socket.create_connection(('127.0.0.1', port), timeout=2) as client:
                    client.sendall(b'GET /complete HTTP/1.1\r\nHost: localhost\r\n\r\n')
                    reply = b''
                    while chunk := client.recv(4096):
                        reply += chunk
                self.assertEqual(reply.count(b'HTTP/1.1 '), 1)
                self.assertTrue(reply.startswith(b'HTTP/1.1 200'))
                self.assertNotIn(b'502', reply)
        self.assertTrue(self.events[0]['response_started'])
        self.assertEqual(self.events[-1]['status'], 200)


class FakeResponse:
    def __init__(self, data):
        self.data = data

    def raise_for_status(self):
        pass

    def json(self):
        return self.data


class AsyncPollingTest(unittest.TestCase):
    def test_terminal_after_126_seconds_fits_default_budget(self):
        clock = [0]
        session = mock.Mock()

        def get(url, params, timeout):
            if params['with_result'] == 'true':
                return FakeResponse({'task_result': 'Task result size 99665305 exceeded limit 16777216 bytes'})
            if clock[0] == 0:
                clock[0] = 126
                return FakeResponse({'task_status': 'running'})
            return FakeResponse({'task_status': 'failed', 'task_create': 1000, 'task_update': 127471})

        session.get.side_effect = get
        record = {'statuses': []}
        with mock.patch.object(ASYNC.time, 'monotonic', lambda: clock[0]), \
             mock.patch.object(ASYNC.time, 'sleep', lambda seconds: clock.__setitem__(0, clock[0] + seconds)), \
             contextlib.redirect_stdout(io.StringIO()):
            self.assertTrue(ASYNC.poll_task(session, '/task', record))
        self.assertEqual(record['final_status'], 'failed')
        self.assertEqual(record['task_execution_ms'], 126471)
        self.assertTrue(record['expected_limit_rejection'])
        self.assertEqual(record['retained_limit_bytes'], 16777216)
        self.assertFalse(record['timed_out'])

    def test_cli_timeout_records_timeout_and_nonzero_exit(self):
        clock = [0]
        session = mock.Mock()
        submitted = FakeResponse({'status': 200, 'data': {'task_id': 1}})
        submitted.status_code = 200
        session.post.return_value = submitted
        session.get.return_value = FakeResponse({'task_status': 'running'})
        with tempfile.TemporaryDirectory() as directory, \
             mock.patch.object(ASYNC.requests, 'Session', return_value=session), \
             mock.patch.object(ASYNC.time, 'monotonic', lambda: clock[0]), \
             mock.patch.object(ASYNC.time, 'sleep', lambda seconds: clock.__setitem__(0, clock[0] + seconds)), \
             contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(ASYNC.main(['--timeout', '2', '--output', str(pathlib.Path(directory) / 'async-large-result-check.json')]), 2)
            record = json.loads((pathlib.Path(directory) / 'async-large-result-check.json').read_text())
        self.assertEqual(record['final_status'], 'TIMEOUT')
        self.assertTrue(record['timed_out'])
        self.assertEqual(len(record['statuses']), 2)
        self.assertTrue(all(call.kwargs['params']['with_result'] == 'false'
                            for call in session.get.call_args_list))


class EdgeIntegrityTest(unittest.TestCase):
    def test_original_edge_must_exist_with_matching_identity_endpoints_and_payload(self):
        expected = [{'id': 'edge1', 'outV': 1, 'inV': 2, 'payload_length': 4,
                     'sha256': hashlib.sha256(b'data').hexdigest()}]
        edge = {'id': 'edge1', 'outV': 1, 'inV': 2, 'properties': {'payload': 'data'}}
        session = mock.Mock()
        session.get.return_value = FakeResponse({'edges': [edge]})
        verify_edges(session, '/edges', expected)
        invalid = [[], [edge, edge]]
        for field, value in [('id', 'different'), ('outV', 3), ('inV', 3)]:
            changed = copy.deepcopy(edge)
            changed[field] = value
            invalid.append([changed])
        changed = copy.deepcopy(edge)
        changed['properties']['payload'] = 'else'
        invalid.append([changed])
        for edges in invalid:
            with self.subTest(edges=edges):
                session.get.return_value = FakeResponse({'edges': edges})
                with self.assertRaises(AssertionError):
                    verify_edges(session, '/edges', expected)


class ScenarioHandler(http.server.BaseHTTPRequestHandler):
    """Tiny fake services: no HugeGraph process or graph writes are involved."""
    def log_message(self, *args):
        pass

    def reply(self, status, data):
        body = json.dumps(data).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if '/vertices/' in self.path:
            self.reply(200, {'id': 'v1', 'label': 'node', 'properties': {'payload': 'original'}})
        elif '/tasks/' in self.path:
            if urllib.parse.parse_qs(urllib.parse.urlsplit(self.path).query).get('with_result') == ['true']:
                self.reply(200, {'task_result': self.server.failure})
            else:
                self.reply(200, {'task_status': self.server.state})
        else:
            body = b'<html><body><div class="cm-content" contenteditable="true"></div><button>Run Query</button></body></html>'
            self.send_response(200)
            self.send_header('Content-Length', str(len(body)))
            self.end_headers()
            self.wfile.write(body)

    def do_POST(self):
        body = self.rfile.read(int(self.headers['Content-Length']))
        if self.path.endswith('/import'):
            if b'properties_truncated' in body:
                self.reply(self.server.root_status, {'status': self.server.root_status,
                                                     'message': self.server.root_message})
            elif b'~hubble_properties_preview' not in body:
                self.reply(200, {'status': 200, 'data': {}})
            else:
                self.reply(self.server.import_status,
                           {'status': self.server.import_status, 'message': self.server.import_message})
        else:
            self.reply(200, {'status': 200, 'data': {'task_id': 1}})


class ReproductionCliTest(unittest.TestCase):
    def run_script(self, name, arguments, cwd):
        return subprocess.run([sys.executable, str(SCRIPTS / name), *arguments],
                              cwd=cwd, capture_output=True, text=True, timeout=45)

    def server(self):
        server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), ScenarioHandler)
        server.state = 'failed'
        server.failure = 'Task result size 99665305 exceeded limit 16777216 bytes'
        server.import_status = 400
        server.root_status = 400
        server.root_message = 'A truncated preview cannot be imported as full graph data'
        server.import_message = 'A truncated preview cannot be imported as full graph data'
        self.active_server = server
        return server

    def test_help_requires_no_private_runtime_files_from_empty_cwd(self):
        names = ['browser_benchmark.py', 'browser_comparison.py', 'browser_large_properties.py',
                 'profile_canvas.py', 'final_stack_smoke.py', 'async_result_check.py',
                 'preview_import_check.py']
        with tempfile.TemporaryDirectory() as directory:
            for name in names:
                with self.subTest(script=name):
                    result = self.run_script(name, ['--help'], directory)
                    self.assertEqual(result.returncode, 0, result.stderr)
                    self.assertIn('--', result.stdout)

    def test_async_cli_rejects_unexpected_terminal_states_and_retains_evidence(self):
        scenarios = [('failed', 'Task result size 99665305 exceeded limit 16777216 bytes', 0),
                     ('success', '', 2), ('cancelled', '', 2), ('canceled', '', 2),
                     ('failed', 'unrelated schema failure', 2),
                     ('failed', 'Task result size 9000000 exceeded limit 8388608 bytes', 2),
                     ('failed', 'Task result size 1 exceeded limit 16777216 bytes', 2),
                     ('running', '', 2)]
        with tempfile.TemporaryDirectory() as directory, serving(self.server()) as port:
            base = f'http://127.0.0.1:{port}'
            for state, failure, expected in scenarios:
                with self.subTest(state=state, failure=failure):
                    # The context owns this server until all subprocesses exit.
                    self.active_server.state = state
                    self.active_server.failure = failure
                    output = pathlib.Path(directory) / 'task.json'
                    result = self.run_script('async_result_check.py',
                        ['--hubble-api-url', base, '--server-url', base, '--output', str(output),
                         '--timeout', '0.08', '--poll-interval', '0.01'], directory)
                    self.assertEqual(result.returncode, expected, result.stderr + result.stdout)
                    record = json.loads(output.read_text())
                    self.assertEqual(record['expected_limit_rejection'], expected == 0)
                    self.assertEqual(record['final_status'], 'TIMEOUT' if state == 'running' else state)

    def test_import_cli_requires_the_specific_guard_not_an_arbitrary_error(self):
        with tempfile.TemporaryDirectory() as directory, serving(self.server()) as port:
            base = f'http://127.0.0.1:{port}'
            fixture = pathlib.Path(directory) / 'fixture.json'
            fixture.write_text(json.dumps({'space': 's', 'graph': 'g', 'vertices': [
                {'id': 'v1', 'sha256': {'payload': hashlib.sha256(b'original').hexdigest()}}]}))
            for status, message, expected in [(400, 'A truncated preview cannot be imported as full graph data', 0),
                    (400, 'Missing vertex label', 2), (401, 'Unauthorized', 2),
                    (400, 'Invalid schema', 2), (200, 'success', 2)]:
                with self.subTest(status=status, message=message):
                    self.active_server.import_status = status
                    self.active_server.import_message = message
                    output = pathlib.Path(directory) / 'import.json'
                    result = self.run_script('preview_import_check.py', ['--fixture', str(fixture),
                        '--hubble-api-url', base, '--server-url', base, '--output', str(output)], directory)
                    self.assertEqual(result.returncode, expected, result.stderr + result.stdout)
                    record = json.loads(output.read_text())
                    self.assertEqual(record['guard_verified'], expected == 0)
                    self.assertEqual(record['original_vertex_sha256_unchanged'], expected == 0)
                    self.assertEqual(record['empty_control']['http_status'], 200)
                    self.assertEqual(record['root_preview_guard']['http_status'], 400)

    def test_import_cli_fails_when_valid_empty_root_preview_guard_is_missing(self):
        with tempfile.TemporaryDirectory() as directory, serving(self.server()) as port:
            base = f'http://127.0.0.1:{port}'
            fixture = pathlib.Path(directory) / 'fixture.json'
            fixture.write_text(json.dumps({'space': 's', 'graph': 'g', 'vertices': [
                {'id': 'v1', 'sha256': {'payload': hashlib.sha256(b'original').hexdigest()}}]}))
            for status, message in [(200, 'success'), (400, 'Unknown field')]:
                with self.subTest(status=status, message=message):
                    self.active_server.root_status = status
                    self.active_server.root_message = message
                    output = pathlib.Path(directory) / 'import.json'
                    result = self.run_script('preview_import_check.py', ['--fixture', str(fixture),
                        '--hubble-api-url', base, '--server-url', base, '--output', str(output)], directory)
                    self.assertEqual(result.returncode, 2, result.stderr + result.stdout)
                    record = json.loads(output.read_text())
                    self.assertFalse(record['guard_verified'])
                    self.assertEqual(record['empty_control']['http_status'], 200)
                    self.assertEqual(record['root_preview_guard']['http_status'], status)
                    self.assertIn('validation_error', record)

    @unittest.skipUnless(shutil.which('chromium') and importlib.util.find_spec('playwright'),
                         'Chromium and Playwright required for real browser exit-code check')
    def test_browser_timeout_keeps_diagnostics_and_exits_nonzero(self):
        with tempfile.TemporaryDirectory() as directory, serving(self.server()) as port:
            for name, positional, output_name in [('browser_comparison.py', ['before'], 'browser-comparison-before.jsonl'),
                                                  ('browser_benchmark.py', [], 'browser.jsonl')]:
                with self.subTest(script=name):
                    output = pathlib.Path(directory) / name
                    result = self.run_script(name, [*positional, '--base-url', f'http://127.0.0.1:{port}',
                        '--output-dir', str(output), '--timeout-ms', '1000'], directory)
                    self.assertEqual(result.returncode, 2, result.stderr + result.stdout)
                    records = [json.loads(line) for line in (output / output_name).read_text().splitlines()]
                    self.assertTrue(any(record.get('stage') == 'query' and 'error' in record for record in records))

    @unittest.skipUnless(shutil.which('chromium') and importlib.util.find_spec('playwright'),
                         'Chromium and Playwright required for scoped count check')
    def test_count_result_ignores_unrelated_totals_and_history_in_browser(self):
        from playwright.sync_api import sync_playwright, TimeoutError as BrowserTimeout
        with sync_playwright() as playwright:
            browser = playwright.chromium.launch(executable_path=shutil.which('chromium'),
                headless=True, args=['--no-sandbox', '--disable-dev-shm-usage'])
            page = browser.new_page()
            page.set_default_timeout(300)
            page.set_content('<p>Graph total: 12000</p><p>History: 12000</p>'
                '<button role="tab">Table</button><section role="tabpanel" aria-label="Table">'
                '<table><tbody><tr><td>7</td></tr></tbody></table></section>')
            wrong = {'status': 200, 'data': {'json_view': {'data': [7]}}}
            with self.assertRaises(AssertionError):
                BROWSER.wait_for_count_result(page, wrong)
            correct = {'status': 200, 'data': {'json_view': {'data': [12000]}}}
            with self.assertRaises(BrowserTimeout):
                BROWSER.wait_for_count_result(page, correct)
            page.locator('td').evaluate("cell => cell.textContent = '12000'")
            self.assertEqual(BROWSER.wait_for_count_result(page, correct)['rendered_count'], '12000')
            browser.close()

    def test_count_response_cannot_be_satisfied_by_totals_or_history(self):
        page = mock.Mock()
        # The surrounding page can contain the fixture count even when the query
        # response is wrong. It must not be consulted before payload validation.
        page.locator.return_value.inner_text.return_value = 'Graph total: 12000; history: 12000'
        for rows in [[7], [1200], [0], [], [12000, 12000], ['12000'], None]:
            with self.subTest(rows=rows), self.assertRaises(AssertionError):
                BROWSER.wait_for_count_result(page, {'status': 200, 'data': {'json_view': {'data': rows}}})
        page.get_by_role.assert_not_called()
        cell = page.get_by_role.return_value.get_by_role.return_value
        cell.inner_text.return_value = '12000'
        result = BROWSER.wait_for_count_result(page, {'status': 200, 'data': {'json_view': {'data': [12000]}}})
        self.assertEqual(result, {'response_count': 12000, 'rendered_count': '12000'})
        self.assertIn(mock.call('tabpanel', name='Table', exact=True), page.get_by_role.call_args_list)
        cell.wait_for.assert_called_once()

    def test_anonymous_routes_require_the_expected_same_origin_redirect(self):
        for requested in ['/login', '/account']:
            valid = {'stage': 'anonymous_route', 'requested': requested,
                     'requested_url': 'http://localhost:3000' + requested,
                     'url': 'http://localhost:3000/navigation',
                     'login_form_visible': 0, 'account_link_visible': 0}
            self.assertEqual(BROWSER.browser_exit_code([valid]), 0)
            for destination in ['http://localhost:3000' + requested,
                                'http://localhost:3000/warning',
                                'http://other.invalid:3000/navigation',
                                'http://localhost:3000/navigation?blocked=true', '']:
                with self.subTest(requested=requested, destination=destination):
                    self.assertEqual(BROWSER.browser_exit_code([{**valid, 'url': destination}]), 2)
            incomplete = {key: value for key, value in valid.items() if key != 'requested_url'}
            self.assertEqual(BROWSER.browser_exit_code([incomplete]), 2)

    def test_query_response_requires_the_exact_submitted_content_and_graph(self):
        query = 'g.V().outE().inV().path().limit(1000)'
        request = mock.Mock(method='POST', post_data_json={'content': query, 'preview': True})
        response = mock.Mock(url='http://localhost/api/v1.3/graphspaces/s/graphs/g/gremlin-query',
                             request=request)
        self.assertTrue(BROWSER.matches_query_response(response, query, 's', 'g'))
        with mock.patch.object(request, 'post_data_json', query):
            self.assertTrue(BROWSER.matches_query_response(response, query, 's', 'g'))
        for body in [None, [], {}, 'g.V().limit(1000)', {'content': 'g.V().limit(1000)'}]:
            with self.subTest(body=body), mock.patch.object(request, 'post_data_json', body):
                self.assertFalse(BROWSER.matches_query_response(response, query, 's', 'g'))
        for path in ['/graphspaces/s/graphs/other/gremlin-query',
                     '/graphspaces/s/graphs/g/gremlin-query/async-task', '/gremlin-query']:
            with self.subTest(path=path), mock.patch.object(response, 'url', 'http://localhost' + path):
                self.assertFalse(BROWSER.matches_query_response(response, query, 's', 'g'))
        with mock.patch.object(request, 'method', 'GET'):
            self.assertFalse(BROWSER.matches_query_response(response, query, 's', 'g'))

    def test_canvas_fallback_requires_an_explicit_visible_notice(self):
        valid = {'stage': 'query', 'render_mode': 'table_fallback', 'http_status': 200,
                 'canvas_limit_notice': True, 'canvas_limit_details': 'mounted text'}
        self.assertEqual(BROWSER.browser_exit_code([valid]), 0)
        for value in [False, None, 0, '', 'true']:
            with self.subTest(value=value):
                self.assertEqual(BROWSER.browser_exit_code([{**valid, 'canvas_limit_notice': value}]), 2)
        self.assertEqual(BROWSER.browser_exit_code([
            {key: value for key, value in valid.items() if key != 'canvas_limit_notice'}]), 2)
        self.assertEqual(BROWSER.browser_exit_code([{'canvas_limit_notice': False}]), 2)

    @unittest.skipUnless(shutil.which('chromium') and importlib.util.find_spec('playwright'),
                         'Chromium and Playwright required for graph-generation check')
    def test_graph_result_rejects_stale_unsettled_or_wrong_graph_in_browser(self):
        from playwright.sync_api import sync_playwright, TimeoutError as BrowserTimeout
        with sync_playwright() as playwright:
            browser = playwright.chromium.launch(executable_path=shutil.which('chromium'),
                headless=True, args=['--no-sandbox', '--disable-dev-shm-usage'])
            page = browser.new_page()
            page.set_default_timeout(250)
            page.set_content('<div data-hubble-graph></div>')
            page.evaluate(BROWSER.INIT)
            page.evaluate("""() => {
                window.installGraph = (state='ready', bad=null) => {
                    window.graphState={status:state,error:bad==='error'?'layout failed':null,data:{
                      nodes:[{data:{raw:{id:bad==='node'?'wrong':'a'}}},{data:{raw:{id:'b'}}}],
                      edges:[{data:{raw:{id:bad==='edge'?'wrong':'e',source:'a',
                        target:bad==='endpoint'?'wrong':'b'}}}]}};
                };
                window.installGraph();
                document.querySelector('[data-hubble-graph]').__reactFiber$probe={
                  memoizedState:{memoizedState:{getSnapshot:()=>window.graphState}}};
            }""")
            previous = page.evaluate('window.__graphSnapshot()')
            payload = {'status': 200, 'data': {'type': 'PATH', 'json_view': {'data': [1, 2]},
                'graph_view': {'vertices': [{'id': 'a'}, {'id': 'b'}],
                               'edges': [{'id': 'e', 'source': 'a', 'target': 'b'}]}}}
            arguments = {'expected_nodes': 2, 'expected_type': 'PATH', 'expected_rows': 2}
            # The old ready-only predicate accepts this unchanged preceding result.
            page.wait_for_function("() => window.__graphSnapshot().status==='ready'")
            with self.assertRaises(BrowserTimeout):
                BROWSER.wait_for_graph_result(page, payload, previous, **arguments)
            for state, bad in [('ready', 'node'), ('ready', 'edge'), ('ready', 'endpoint'),
                               ('ready', 'error'), ('updating', None), ('disposed', None)]:
                with self.subTest(state=state, bad=bad):
                    page.evaluate('([state,bad])=>window.installGraph(state,bad)', [state, bad])
                    with self.assertRaises(BrowserTimeout):
                        BROWSER.wait_for_graph_result(page, payload, previous, **arguments)
            for changed in [{'type': 'VERTEX'}, {'json_view': {'data': [1]}},
                            {'graph_view': {'vertices': [], 'edges': []}}]:
                wrong = copy.deepcopy(payload)
                wrong['data'].update(changed)
                with self.subTest(response=changed), self.assertRaises(AssertionError):
                    BROWSER.wait_for_graph_result(page, wrong, previous, **arguments)
            page.evaluate("setTimeout(()=>window.installGraph('ready'),100)")
            result = BROWSER.wait_for_graph_result(page, payload, previous, **arguments)
            self.assertGreater(result['snapshot']['data_generation'], previous['data_generation'])
            self.assertEqual(result['snapshot']['status'], 'ready')
            self.assertEqual(result['response_edges'], 1)
            browser.close()

    @unittest.skipUnless(shutil.which('chromium') and importlib.util.find_spec('playwright'),
                         'Chromium and Playwright required for visible canvas notice check')
    def test_hidden_mounted_canvas_notice_fails_in_browser(self):
        from playwright.sync_api import sync_playwright, TimeoutError as BrowserTimeout
        with sync_playwright() as playwright:
            browser = playwright.chromium.launch(executable_path=shutil.which('chromium'),
                headless=True, args=['--no-sandbox', '--disable-dev-shm-usage'])
            page = browser.new_page()
            page.set_default_timeout(250)
            page.set_content('<section role="tabpanel" style="display:none">'
                '<p>Graph canvas collapsed for readability</p><p>Limit 1000</p></section>')
            notice = page.get_by_text('Graph canvas collapsed for readability', exact=True)
            self.assertFalse(notice.is_visible())
            self.assertIn('Limit 1000', page.locator('[role="tabpanel"]').inner_text())
            with self.assertRaises(BrowserTimeout):
                BROWSER.wait_for_canvas_limit_notice(page)
            page.locator('section').evaluate("panel=>panel.style.display='block'")
            result = BROWSER.wait_for_canvas_limit_notice(page)
            self.assertTrue(result['canvas_limit_notice'])
            self.assertIn('Limit 1000', result['canvas_limit_details'])
            browser.close()

    def test_async_submission_requires_matching_nonempty_task_id(self):
        for task_id in [1, '138']:
            valid = {'stage': 'async_submission', 'http_status': 200, 'task_id': task_id,
                     'response': {'status': 200, 'data': {'task_id': task_id}}, 'errors': []}
            self.assertEqual(BROWSER.browser_exit_code([valid]), 0)
            payloads = [None, [], {}, {'data': {'task_id': task_id}},
                        {'status': 400, 'data': {'task_id': task_id}},
                        {'status': 200}, {'status': 200, 'data': None},
                        {'status': 200, 'data': []}, {'status': 200, 'data': {}}]
            payloads.extend({'status': 200, 'data': {'task_id': value}}
                            for value in [None, '', '  ', [], {}, True, False, 0, -1, 1.5])
            for payload in payloads:
                with self.subTest(task_id=task_id, payload=payload):
                    self.assertEqual(BROWSER.browser_exit_code([{**valid, 'response': payload}]), 2)
            for changed in [{'http_status': 503}, {'task_id': None}, {'task_id': ''}, {'task_id': True},
                            {'task_id': 'different'}, {'errors': ['broken']}]:
                with self.subTest(changed=changed):
                    self.assertEqual(BROWSER.browser_exit_code([{**valid, **changed}]), 2)
            for field in ['http_status', 'response', 'task_id']:
                with self.subTest(missing=field):
                    self.assertEqual(BROWSER.browser_exit_code([
                        {key: value for key, value in valid.items() if key != field}]), 2)

    def test_archived_followup_manifest_matches_all_artifacts(self):
        root = SCRIPTS.parent / 'comparison' / 'final-followup'
        manifest = json.loads((root / 'manifest.json').read_text())
        for item in manifest['files']:
            with self.subTest(file=item['file']):
                data = (root / item['file']).read_bytes()
                self.assertEqual(len(data), item['bytes'])
                self.assertEqual(hashlib.sha256(data).hexdigest(), item['sha256'])

    def test_default_notice_requires_both_complete_clauses_and_exact_limit(self):
        for text in ['No result limit was set. This query defaults to at most 300 results.',
                     'No result limit was set; This query defaults to at most 300 results']:
            self.assertIsNotNone(SMOKE.DEFAULT_NOTICE.fullmatch(text))
        for text in ['No result limit was set', 'This query defaults to at most 300 results',
                     'No result limit was set; This query defaults to at most 3000 results',
                     'No result limit was set; This query defaults to at most 250 results',
                     'No result limit was set; This query defaults to at most 300 results but failed']:
            self.assertIsNone(SMOKE.DEFAULT_NOTICE.fullmatch(text))

    def test_browser_saved_failures_include_empty_assertion_messages_and_context_errors(self):
        self.assertEqual(BROWSER.browser_exit_code([{'stage': 'query', 'http_status': 200}]), 0)
        for record in [{'error': ''}, {'stage': 'graph_context_switch', 'error': 'missing option'},
                       {'http_status': 503}, {'response': {'status': 400}}, {'page_errors': ['broken']},
                       {'errors': ['broken']}, {'additional_query_requests': 1}]:
            with self.subTest(record=record):
                self.assertEqual(BROWSER.browser_exit_code([record]), 2)


if __name__ == '__main__':
    unittest.main()
