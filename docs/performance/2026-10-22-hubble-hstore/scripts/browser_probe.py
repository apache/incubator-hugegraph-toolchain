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
        if record.get('stage') == 'anonymous_route':
            if record.get('login_form_visible') or record.get('account_link_visible'):
                return 2
    return 0
