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

"""Verify huge-property previews and read-only graph details in actual Chromium."""
import argparse,hashlib,json,pathlib,time
import requests
from browser_probe import INIT, add_browser_arguments, prepare_browser_args, put_query

def main(argv=None):
    parser=argparse.ArgumentParser()
    add_browser_arguments(parser)
    parser.add_argument('--fixture',type=pathlib.Path,required=True)
    parser.add_argument('--server-url',default='http://127.0.0.1:18080')
    args=prepare_browser_args(parser,parser.parse_args(argv))
    ROOT=args.output_dir
    (ROOT/'comparison-screenshots').mkdir(exist_ok=True)
    F=json.loads(args.fixture.read_text())
    init=INIT.replace('const s=c.getSnapshot();return','window.__graphController=c;const s=c.getSnapshot();return')
    from playwright.sync_api import sync_playwright
    with sync_playwright() as p:
     b=p.chromium.launch(executable_path=args.chromium,headless=True,args=['--no-sandbox','--disable-dev-shm-usage'])
     c=b.new_context(viewport={'width':1440,'height':1000});c.add_init_script(init);page=c.new_page();page.set_default_timeout(args.timeout_ms);errors=[];writes=[]
     page.on('pageerror',lambda e:errors.append(str(e)));page.on('request',lambda r:writes.append({'url':r.url,'method':r.method}) if r.method in ['PUT','PATCH'] and ('/vertex/'in r.url or '/edge/'in r.url) else None)
     page.goto(f"{args.base_url}/gremlin/{F['space']}/{F['graph']}",wait_until='networkidle')
     put_query(page,'g.V()')
     with page.expect_response(lambda r:'/gremlin-query'in r.url and r.request.method=='POST')as info:page.get_by_role('button',name='Run Query',exact=True).click()
     response=info.value;response.finished();assert response.status==200
     page.wait_for_function("()=>{const s=window.__graphSnapshot();return s&&s.status==='ready'&&s.nodes===2&&s.edges===1;}")
     notice='Long properties show only the first 256 characters and their original length. Preview properties cannot be edited.'
     page.get_by_text(notice,exact=True).wait_for()
     raw=page.evaluate("""()=>window.__graphController.getSnapshot().data.nodes.map(n=>({id:String(n.data.raw.id),preview:n.data.raw['~hubble_properties_preview'],payload_length:String(n.data.raw.properties.payload).length,boundary256_length:String(n.data.raw.properties.boundary256).length}))""")
     assert {r['id']for r in raw}=={str(x)for x in F['vertex_ids']};assert all(r['preview']is True and r['payload_length']<320 and r['boundary256_length']==256 for r in raw)
     page.mouse.move(20,20);page.screenshot(path=str(ROOT/'comparison-screenshots/after-large-property-graph.png'))
     point=page.evaluate("""wanted=>{const c=window.__graphController;const n=c.getSnapshot().data.nodes.find(n=>String(n.data.raw.id)===wanted);return c.runtime.graph.getViewportByCanvas(c.runtime.graph.getElementPosition(n.id));}""",str(F['vertex_ids'][1]))
     box=page.locator('[data-hubble-graph]').bounding_box();page.mouse.click(box['x']+point[0],box['y']+point[1])
     readonly='These properties contain truncated previews and cannot be edited. Use the original data for changes.'
     page.get_by_text(readonly,exact=True).wait_for();drawer=page.locator('.ant-drawer:visible');edit=drawer.get_by_role('button',name='Edit',exact=True);assert edit.is_disabled();assert '1048576 chars'in drawer.inner_text()
     page.wait_for_timeout(350);edit_box=edit.bounding_box();page.mouse.click(edit_box['x']+edit_box['width']/2,edit_box['y']+edit_box['height']/2);assert edit.is_disabled();page.wait_for_timeout(250);assert writes==[]
     page.mouse.move(20,20);page.screenshot(path=str(ROOT/'comparison-screenshots/after-large-property-readonly.png'))
     drawer.get_by_role('button',name='Close',exact=True).last.click();page.get_by_role('tab',name='JSON',exact=True).click();page.wait_for_timeout(150)
     page.mouse.move(20,20);page.screenshot(path=str(ROOT/'comparison-screenshots/after-large-property-json.png'))
     assert errors==[]
     record=dict(stage='large_properties',space=F['space'],graph=F['graph'],snapshot=page.evaluate('window.__graphSnapshot()'),raw_preview=raw,preview_notice=notice,readonly_notice=readonly,edit_disabled=True,mutation_requests=writes,page_errors=errors)
     page.wait_for_load_state('networkidle');c.close();b.close()
     S=requests.Session();S.trust_env=False
     for v in F['vertices']:
      d=S.get(f"{args.server_url.rstrip('/')}/graphspaces/{F['space']}/graphs/{F['graph']}/graph/vertices/{v['id']}",timeout=30).json()
      for key,digest in v['sha256'].items():assert hashlib.sha256(d['properties'][key].encode()).hexdigest()==digest
     record['original_vertex_sha256_unchanged']=True
     (ROOT/'browser-large-properties.json').write_text(json.dumps(record,indent=2)+'\n');print(json.dumps(record,indent=2))
    return 0

if __name__=='__main__':
    raise SystemExit(main())
