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

import argparse,json,pathlib,re
from browser_probe import INIT, add_browser_arguments, prepare_browser_args, put_query

def main(argv=None):
    parser=argparse.ArgumentParser()
    add_browser_arguments(parser)
    args=prepare_browser_args(parser,parser.parse_args(argv))
    ROOT=args.output_dir
    (ROOT/'comparison-screenshots').mkdir(exist_ok=True)
    R=ROOT
    from playwright.sync_api import sync_playwright
    with sync_playwright()as p:
     b=p.chromium.launch(executable_path=args.chromium,headless=True,args=['--no-sandbox','--disable-dev-shm-usage']);c=b.new_context(viewport={'width':1440,'height':1000});c.add_init_script(INIT);page=c.new_page();page.set_default_timeout(args.timeout_ms);errors=[];page.on('pageerror',lambda e:errors.append(str(e)))
     page.goto(args.base_url+'/gremlin/perf_space_0998/perf_small64',wait_until='networkidle');put_query(page,'g.V()')
     with page.expect_response(lambda r:'/gremlin-query'in r.url and r.request.method=='POST')as info:page.get_by_role('button',name='Run Query',exact=True).click()
     info.value.finished();page.wait_for_function("()=>{const s=window.__graphSnapshot();return s&&s.status==='ready'&&s.nodes===300;}");page.get_by_text('No result limit was set. This query defaults to at most 300 results.',exact=True).wait_for();snapshot=page.evaluate('window.__graphSnapshot()')
     page.get_by_role('combobox',name='Current GraphSpace',exact=True).click();holder=page.locator('.ant-select-dropdown:visible .rc-virtual-list-holder');page.wait_for_timeout(200);holder.evaluate('el=>el.scrollTop=el.scrollHeight') if holder.count() else None;page.locator('.ant-select-dropdown:visible .ant-select-item-option[title="perf_space_0997"]').click();page.get_by_role('combobox',name='Current graph',exact=True).click();page.locator('.ant-select-dropdown:visible .ant-select-item-option-content').filter(has_text=re.compile('^perf_meta_001$')).click();page.wait_for_url(re.compile('/gremlin/perf_space_0997/perf_meta_001'));page.wait_for_load_state('networkidle');assert errors==[]
     record={'stage':'final_stack_functional_smoke','server':'configured endpoint','performance_sample':False,'default_snapshot':snapshot,'default_notice':True,'context_url':page.url,'page_errors':errors};(R/'browser-final-stack-smoke.json').write_text(json.dumps(record,indent=2)+'\n');print(json.dumps(record));c.close();b.close()
    return 0

if __name__=='__main__':
    raise SystemExit(main())
