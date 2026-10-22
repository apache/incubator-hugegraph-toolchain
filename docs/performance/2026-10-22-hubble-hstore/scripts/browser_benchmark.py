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

"""Exercise the production UI in Chromium; observe renderer state without changing app code."""
import json, pathlib, time, traceback, re
from playwright.sync_api import sync_playwright
ROOT=pathlib.Path('/workspace/hubble-perf')

INIT="""(() => {
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

def save(item):
    with (ROOT/'browser.jsonl').open('a') as f:f.write(json.dumps(item,ensure_ascii=False)+'\n')
    print(json.dumps(item,ensure_ascii=False),flush=True)

with sync_playwright() as p:
    browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox','--disable-dev-shm-usage','--remote-debugging-port=9222'])
    for graph,space in [('perf_small64','perf_space_0998'),('perf_large8192','perf_space_0999')]:
        context=browser.new_context(viewport={'width':1440,'height':1000})
        context.add_init_script(INIT)
        page=context.new_page();page.set_default_timeout(120000)
        errors=[];page.on('pageerror',lambda e: errors.append(str(e)))
        try:
            started=time.time();nav=time.perf_counter()
            page.goto(f'http://127.0.0.1:3000/gremlin/{space}/{graph}',wait_until='domcontentloaded')
            page.get_by_role('button',name='Run Query',exact=True).wait_for()
            page.wait_for_function("!document.querySelector('.cm-content')?.getAttribute('aria-disabled')")
            page.wait_for_load_state('networkidle',timeout=120000)
            save(dict(stage='navigation',graph=graph,ts=started,ms=(time.perf_counter()-nav)*1000,errors=errors[:],url=page.url,text=page.locator('body').inner_text()[:900],resources=page.evaluate("performance.getEntriesByType('resource').filter(e=>e.name.includes('/api/')).map(e=>({url:e.name,ms:e.duration,bytes:e.decodedBodySize}))")))
            for label,query,expected in [('default','g.V()',250),('paths12000','g.V().outE().inV().path().limit(12000)',12000)]:
                editor=page.locator('.cm-content').first
                editor.click();page.keyboard.press('Control+a');page.keyboard.insert_text(query)
                page.get_by_role('button',name='Run Query',exact=True).wait_for(state='visible')
                marker=page.evaluate('performance.now()');start=time.perf_counter();started=time.time()
                record=dict(stage='query',graph=graph,label=label,query=query,ts=started)
                try:
                    with page.expect_response(lambda r:'/gremlin-query' in r.url and r.request.method=='POST',timeout=120000) as info:
                        page.get_by_role('button',name='Run Query',exact=True).click()
                    response=info.value;response.finished();record['http_ms']=(time.perf_counter()-start)*1000
                    record['http_status']=response.status
                    (ROOT/'browser-phase.json').write_text(json.dumps(dict(graph=graph,label=label,phase='downloaded',text=page.locator('body').inner_text()[:1600],tabs=page.get_by_role('tab').all_text_contents())))
                    if expected > 300:
                        page.get_by_text(re.compile(r'12,?000 result rows, displayed with pagination')).wait_for(timeout=15000)
                        record['render_mode']='table_fallback'
                    else:
                        page.wait_for_function("n=>{const s=window.__graphSnapshot();return s&&s.status==='ready'&&s.nodes===n;}",arg=expected,timeout=120000)
                        record['render_mode']='canvas'
                    record['ready_ms']=(time.perf_counter()-start)*1000
                    record['snapshot']=page.evaluate('window.__graphSnapshot()')
                    page.wait_for_timeout(2000)
                    record['metrics']=page.evaluate("start=>({heap_used:performance.memory?.usedJSHeapSize,long_tasks:window.__perfLongTasks.filter(t=>t.start>=start),max_timer_gap:Math.max(0,...window.__perfGaps.filter(t=>t.at>=start).map(t=>t.gap)),resources:performance.getEntriesByType('resource').filter(e=>e.startTime>=start&&e.name.includes('/gremlin-query')).map(e=>({ms:e.duration,bytes:e.decodedBodySize}))})",marker)
                    canvas=page.locator('[data-hubble-graph]')
                    box=canvas.bounding_box(timeout=2000) if expected<=300 and canvas.count() else None
                    if box:
                        page.mouse.move(box['x']+box['width']*.5,box['y']+box['height']*.5)
                        gesture=time.perf_counter();page.mouse.wheel(0,250);page.wait_for_timeout(250)
                        record['wheel_command_ms']=(time.perf_counter()-gesture)*1000-250
                    page.screenshot(path=str(ROOT/'screenshots'/f'{graph}-{label}.png'))
                    if expected > 300:
                        page.get_by_role('tab',name=re.compile(r'Graph$')).click(timeout=10000)
                        record['canvas_limit_notice']=page.get_by_text('Graph canvas collapsed for readability',exact=True).is_visible()
                        record['canvas_limit_details']=page.locator('[role="tabpanel"]').filter(has_text='Graph canvas collapsed for readability').inner_text()
                        page.screenshot(path=str(ROOT/'screenshots'/f'{graph}-{label}-canvas-limit.png'))
                except Exception as e:
                    record['error']=str(e)[:500];record['elapsed_ms']=(time.perf_counter()-start)*1000
                    try:record['snapshot']=page.evaluate('window.__graphSnapshot()');record['text']=page.locator('body').inner_text()[-1600:]
                    except Exception:pass
                record['errors']=errors[:];save(record)
        except Exception as e:
            save(dict(stage='navigation_error',graph=graph,error=str(e)[:700],errors=errors[:]))
        context.close()
    browser.close()
