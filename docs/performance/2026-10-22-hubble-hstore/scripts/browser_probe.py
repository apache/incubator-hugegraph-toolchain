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

"""Shared, import-safe browser observation and CLI helpers for this report."""
import pathlib
import re
from urllib.parse import urlsplit
import shutil

INIT = """(() => {
  window.__perfLongTasks=[];window.__perfGaps=[];
  new PerformanceObserver(list => {for(const e of list.getEntries())window.__perfLongTasks.push({start:e.startTime,duration:e.duration});}).observe({type:'longtask',buffered:true});
  let last=performance.now();setInterval(()=>{const now=performance.now();window.__perfGaps.push({at:now,gap:now-last});last=now;},50);
  const dataGenerations=new WeakMap();let nextGeneration=0;
  window.__graphSnapshot=(includeIds=false)=>{
    const el=document.querySelector('[data-hubble-graph]');if(!el)return null;
    let f=el[Object.keys(el).find(k=>k.startsWith('__reactFiber$'))];
    for(let i=0;f&&i<30;i++,f=f.return){
      let h=f.memoizedState;
      for(let j=0;h&&j<80;j++,h=h.next){
        const c=h.memoizedState;
        if(c&&typeof c.getSnapshot==='function'){
          const s=c.getSnapshot();
          if(!dataGenerations.has(s.data))dataGenerations.set(s.data,++nextGeneration);
          return {status:s.status,data_generation:dataGenerations.get(s.data),nodes:s.data.nodes.length,
            edges:s.data.edges.length,error:s.error?String(s.error):null,
            ...(includeIds?{node_ids:s.data.nodes.map(n=>String(n.data.raw.id)).sort(),
              edge_ids:s.data.edges.map(e=>JSON.stringify([String(e.data.raw.id),String(e.data.raw.source),String(e.data.raw.target)])).sort()}: {})};
        }
      }
    }
    return null;
  };
})();"""


def add_browser_arguments(parser):
    parser.add_argument('--base-url', default='http://127.0.0.1:3000')
    parser.add_argument('--output-dir', type=pathlib.Path, default=pathlib.Path('hubble-results'))
    parser.add_argument('--chromium', default=shutil.which('chromium') or 'chromium')
    parser.add_argument('--timeout-ms', type=int, default=90000)


def prepare_browser_args(parser, args):
    if args.timeout_ms <= 0:
        parser.error('--timeout-ms must be positive')
    args.base_url = args.base_url.rstrip('/')
    args.output_dir.mkdir(parents=True, exist_ok=True)
    return args


def put_query(page, query):
    editor = page.locator('.cm-content[contenteditable="true"]:visible').last
    editor.fill(query)
    if editor.inner_text().strip() != query:
        raise AssertionError('Query editor did not retain the requested statement')


def wait_for_count_result(page, payload, expected=12000):
    """Require the returned scalar, then the active result table's exact cell."""
    rows = ((payload.get('data') or {}).get('json_view') or {}).get('data')
    assert payload.get('status') == 200 and rows == [expected], 'Unexpected count-query result'
    page.get_by_role('tab', name='Table', exact=True).click()
    value = re.compile(r'^(?:' + re.escape(str(expected)) + '|' + re.escape(f'{expected:,}') + r')$')
    cell = page.get_by_role('tabpanel', name='Table', exact=True).get_by_role('cell', name=value)
    cell.wait_for()
    return {'response_count': rows[0], 'rendered_count': cell.inner_text()}


def matches_query_response(response, query, space, graph):
    """Bind the observed response to the exact query and graph just submitted."""
    try:
        request = response.request
        path = f'/graphspaces/{space}/graphs/{graph}/gremlin-query'
        if request.method != 'POST' or not urlsplit(response.url).path.endswith(path):
            return False
        body = request.post_data_json
        return body == query if isinstance(body, str) else isinstance(body, dict) and body.get('content') == query
    except (AttributeError, TypeError, ValueError):
        return False


def wait_for_graph_result(page, payload, previous, expected_nodes=None, expected_type=None, expected_rows=None):
    """Require newly adopted, settled graph data matching this query response."""
    data = payload.get('data') or {}
    graph = data.get('graph_view') or {}
    vertices, edges = graph.get('vertices'), graph.get('edges')
    assert payload.get('status') == 200 and isinstance(vertices, list) and isinstance(edges, list), \
        'Query did not return graph arrays'
    if expected_nodes is not None:
        assert len(vertices) == expected_nodes, 'Unexpected graph vertex count in query response'
    if expected_type is not None:
        assert data.get('type') == expected_type, 'Unexpected query response type'
    if expected_rows is not None:
        rows = (data.get('json_view') or {}).get('data')
        assert isinstance(rows, list) and len(rows) == expected_rows, 'Unexpected query response row count'
    node_ids = [str(vertex['id']) for vertex in vertices]
    edge_ids = [[str(edge['id']), str(edge['source']), str(edge['target'])] for edge in edges]
    previous_generation = (previous or {}).get('data_generation', 0)
    page.wait_for_function("""expected => {
        const s=window.__graphSnapshot(true);
        return s && s.status==='ready' && !s.error && s.data_generation>expected.previous_generation
          && JSON.stringify(s.node_ids)===JSON.stringify(expected.node_ids.sort())
          && JSON.stringify(s.edge_ids)===JSON.stringify(expected.edge_ids.map(e=>JSON.stringify(e)).sort());
    }""", arg={'previous_generation': previous_generation, 'node_ids': node_ids, 'edge_ids': edge_ids})
    return {'previous_generation': previous_generation, 'response_nodes': len(vertices),
            'response_edges': len(edges), 'snapshot': page.evaluate('window.__graphSnapshot()')}


def wait_for_canvas_limit_notice(page):
    text = 'Graph canvas collapsed for readability'
    notice = page.get_by_text(text, exact=True)
    notice.wait_for(state='visible')
    return {'canvas_limit_notice': notice.is_visible(),
            'canvas_limit_details': page.get_by_role('tabpanel').filter(has_text=text).inner_text()}


def anonymous_route_redirected(record):
    """Anonymous login/account deep links must land on same-origin navigation."""
    try:
        requested = record['requested']
        source = urlsplit(record['requested_url'])
        destination = urlsplit(record['url'])
        return (requested in ('/login', '/account') and source.path == requested
                and source.scheme in ('http', 'https') and source.hostname is not None
                and (source.scheme, source.hostname, source.port) ==
                    (destination.scheme, destination.hostname, destination.port)
                and destination.path == '/navigation'
                and not destination.query and not destination.fragment)
    except (KeyError, TypeError, ValueError):
        return False


def async_submission_task_id(http_status, payload):
    """A successful submission must identify the task actually created."""
    assert http_status == 200 and isinstance(payload, dict) and payload.get('status') == 200, \
        'Async submission returned an HTTP or application error'
    data = payload.get('data')
    assert isinstance(data, dict), 'Async submission did not return task data'
    task_id = data.get('task_id')
    assert (isinstance(task_id, str) and task_id.strip()) or (type(task_id) is int and task_id > 0), \
        'Async submission did not return a nonempty task ID'
    return task_id


def browser_exit_code(records):
    """A saved diagnostic must never silently turn a failed run into success."""
    for record in records:
        if 'error' in record or record.get('errors') or record.get('page_errors'):
            return 2
        if record.get('http_status', 200) != 200:
            return 2
        response = record.get('response')
        if isinstance(response, dict) and response.get('status', 200) != 200:
            return 2
        if record.get('additional_query_requests', 0) != 0:
            return 2
        if ('canvas_limit_notice' in record or record.get('render_mode') == 'table_fallback') \
                and record.get('canvas_limit_notice') is not True:
            return 2
        if record.get('stage') == 'async_submission':
            try:
                task_id = async_submission_task_id(record.get('http_status'), response)
                if type(record.get('task_id')) is not type(task_id) or record['task_id'] != task_id:
                    return 2
            except AssertionError:
                return 2
        if record.get('stage') == 'anonymous_route':
            if (not anonymous_route_redirected(record)
                    or record.get('login_form_visible') or record.get('account_link_visible')):
                return 2
    return 0
