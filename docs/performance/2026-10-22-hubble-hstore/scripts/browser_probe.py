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
  window.__graphSnapshot=()=>{
    const el=document.querySelector('[data-hubble-graph]');if(!el)return null;
    let f=el[Object.keys(el).find(k=>k.startsWith('__reactFiber$'))];
    for(let i=0;f&&i<30;i++,f=f.return){
      let h=f.memoizedState;
      for(let j=0;h&&j<80;j++,h=h.next){
        const c=h.memoizedState;
        if(c&&typeof c.getSnapshot==='function'){
          const s=c.getSnapshot();return {status:s.status,nodes:s.data.nodes.length,edges:s.data.edges.length,error:s.error?String(s.error):null};
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
