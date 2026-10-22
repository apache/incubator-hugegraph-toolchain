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

"""Exercise bounded query previews and the pre-submit async gate in local Chromium."""
import argparse, json, pathlib, re, time
from playwright.sync_api import sync_playwright
ROOT=pathlib.Path('/workspace/hubble-perf')
INIT=pathlib.Path(__file__).with_name('browser_benchmark.py').read_text().split('INIT="""',1)[1].split('"""',1)[0]

def save(phase,record):
    with (ROOT/f'browser-comparison-{phase}.jsonl').open('a') as f:f.write(json.dumps(record,ensure_ascii=False)+'\n')
    print(json.dumps(record,ensure_ascii=False),flush=True)

def put_query(page,query):
    editor=page.locator('.cm-content').first;editor.click();page.keyboard.press('Control+a');page.keyboard.insert_text(query)

def main():
    parser=argparse.ArgumentParser();parser.add_argument('phase');args=parser.parse_args()
    shots=ROOT/'comparison-screenshots';shots.mkdir(exist_ok=True)
    with sync_playwright() as p:
        browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox','--disable-dev-shm-usage'])
        for graph,space in [('perf_small64','perf_space_0998'),('perf_large8192','perf_space_0999')]:
            context=browser.new_context(viewport={'width':1440,'height':1000});context.add_init_script(INIT)
            page=context.new_page();page.set_default_timeout(90000);errors=[];requests=[]
            page.on('pageerror',lambda e:errors.append(str(e)))
            page.on('request',lambda r:requests.append(dict(url=r.url,method=r.method,post_data=r.post_data)) if '/gremlin-query' in r.url and r.method=='POST' else None)
            nav=time.perf_counter();page.goto(f'http://127.0.0.1:3000/gremlin/{space}/{graph}',wait_until='domcontentloaded')
            page.get_by_role('button',name='Run Query',exact=True).wait_for();page.wait_for_load_state('networkidle',timeout=90000)
            save(args.phase,dict(stage='navigation',graph=graph,ms=(time.perf_counter()-nav)*1000,url=page.url,auth_mode='anonymous',account_visible=page.get_by_text('Accounts',exact=True).count(),resources=page.evaluate("performance.getEntriesByType('resource').filter(e=>e.name.includes('/api/')).map(e=>({url:e.name,ms:e.duration,bytes:e.decodedBodySize}))"),errors=errors[:]))
            for label,query,expected in [('default','g.V()',300 if args.phase=='after' else 250),('vertices1000','g.V().limit(1000)',1000),('paths1000','g.V().outE().inV().path().limit(1000)',None),('count','g.V().count()',None)]:
                put_query(page,query);marker=page.evaluate('performance.now()');started=time.perf_counter();record=dict(stage='query',label=label,graph=graph,query=query)
                try:
                    with page.expect_response(lambda r:'/gremlin-query' in r.url and r.request.method=='POST',timeout=90000) as info:page.get_by_role('button',name='Run Query',exact=True).click()
                    response=info.value;response.finished();record['http_ms']=(time.perf_counter()-started)*1000;record['http_status']=response.status
                    if label=='default' or (args.phase=='after' and label=='vertices1000'):
                        page.wait_for_function("n=>{let s=window.__graphSnapshot();return s&&s.status==='ready'&&s.nodes===n;}",arg=expected,timeout=90000)
                    elif label=='count':
                        page.wait_for_function("document.body.innerText.includes('12000') || document.body.innerText.includes('12,000')")
                    elif args.phase=='before':
                        page.get_by_text(re.compile('1,?000 result rows, displayed with pagination')).wait_for()
                    else:
                        page.wait_for_function("()=>{let s=window.__graphSnapshot();return s&&s.status==='ready';}")
                    record['ready_ms']=(time.perf_counter()-started)*1000;record['snapshot']=page.evaluate('window.__graphSnapshot()');record['text']=page.locator('body').inner_text()[-2000:]
                    record['metrics']=page.evaluate("start=>({heap_used:performance.memory?.usedJSHeapSize,long_tasks:window.__perfLongTasks.filter(t=>t.start>=start),resources:performance.getEntriesByType('resource').filter(e=>e.startTime>=start&&e.name.includes('/gremlin-query')).map(e=>({ms:e.duration,bytes:e.decodedBodySize}))})",marker)
                    page.screenshot(path=str(shots/f'{args.phase}-{graph}-{label}.png'))
                except Exception as e:record['error']=str(e)[:600];record['text']=page.locator('body').inner_text()[-2000:]
                record['errors']=errors[:];save(args.phase,record)
            if args.phase=='after':
                put_query(page,'g.V().limit(12000)');before=len(requests);page.get_by_role('button',name='Run Query',exact=True).click()
                page.get_by_text(re.compile('This query explicitly requests more than 1000 results')).wait_for()
                page.wait_for_timeout(500)
                record=dict(stage='large_query_gate',graph=graph,query='g.V().limit(12000)',additional_query_requests=len(requests)-before,notice=page.locator('body').inner_text()[-1700:],errors=errors[:])
                page.screenshot(path=str(shots/f'{args.phase}-{graph}-large-gate.png'));save(args.phase,record)
                if graph=='perf_small64':
                    page.get_by_role('button',name='Switch to Async Task',exact=True).click()
                    with page.expect_response(lambda r:'/gremlin-query/async-task' in r.url and r.request.method=='POST') as info:page.get_by_role('button',name='Submit Task',exact=True).click()
                    r=info.value;r.finished();task=r.json()
                    save(args.phase,dict(stage='async_submission',graph=graph,http_status=r.status,task_id=task.get('data',{}).get('task_id'),response=task,errors=errors[:]))
                    page.screenshot(path=str(shots/f'{args.phase}-{graph}-async-submitted.png'))
            if args.phase=='after' and graph=='perf_large8192':
                try:
                    selector=page.get_by_role('combobox',name='Current GraphSpace',exact=True)
                    selector.click()
                    holder=page.locator('.ant-select-dropdown:visible .rc-virtual-list-holder')
                    if holder.count():holder.evaluate('el=>el.scrollTop=el.scrollHeight')
                    page.locator('.ant-select-dropdown:visible .ant-select-item-option[title="perf_space_0997"]').click(timeout=15000)
                    page.get_by_role('combobox',name='Current graph',exact=True).wait_for(state='visible')
                    page.get_by_role('combobox',name='Current graph',exact=True).click(timeout=20000)
                    page.locator('.ant-select-dropdown:visible .ant-select-item-option-content').filter(has_text=re.compile('^perf_meta_001$')).click(timeout=15000)
                    page.wait_for_url(re.compile('/gremlin/perf_space_0997/perf_meta_001'),timeout=20000)
                    page.wait_for_load_state('networkidle')
                    save(args.phase,dict(stage='graph_context_switch',url=page.url,graphspace=page.get_by_role('combobox',name='Current GraphSpace',exact=True).get_attribute('aria-description'),errors=errors[:]))
                    page.screenshot(path=str(shots/f'{args.phase}-anonymous-context-switch.png'))
                except Exception as e:save(args.phase,dict(stage='graph_context_switch',error=str(e)[:600],url=page.url))
                for route in ['/login','/account']:
                    page.goto('http://127.0.0.1:3000'+route,wait_until='networkidle')
                    save(args.phase,dict(stage='anonymous_route',requested=route,url=page.url,login_form_visible=page.get_by_role('textbox',name='Username',exact=True).count(),account_link_visible=page.get_by_role('link',name='Accounts',exact=True).count(),errors=errors[:]))
                    page.screenshot(path=str(shots/f'{args.phase}-anonymous-{route[1:]}.png'))
            page.wait_for_load_state('networkidle',timeout=30000)
            context.close()
        browser.close()
if __name__=='__main__':main()
