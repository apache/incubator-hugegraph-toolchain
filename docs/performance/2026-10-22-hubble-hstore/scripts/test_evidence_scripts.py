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
                return FakeResponse({'task_result': 'size exceeded limit'})
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
        self.assertEqual(record['failure_result'], 'size exceeded limit')
        self.assertFalse(record['timed_out'])

    def test_cli_timeout_records_timeout_and_nonzero_exit(self):
        clock = [0]
        session = mock.Mock()
        submitted = FakeResponse({'status': 200, 'data': {'task_id': 1}})
        submitted.status_code = 200
        session.post.return_value = submitted
        session.get.return_value = FakeResponse({'task_status': 'running'})
        with tempfile.TemporaryDirectory() as directory, \
             mock.patch.object(ASYNC, 'ROOT', pathlib.Path(directory)), \
             mock.patch.object(ASYNC.requests, 'Session', return_value=session), \
             mock.patch.object(ASYNC.time, 'monotonic', lambda: clock[0]), \
             mock.patch.object(ASYNC.time, 'sleep', lambda seconds: clock.__setitem__(0, clock[0] + seconds)), \
             mock.patch('sys.argv', ['async_result_check.py', '--timeout', '2']), \
             contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(ASYNC.main(), 2)
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


if __name__ == '__main__':
    unittest.main()
