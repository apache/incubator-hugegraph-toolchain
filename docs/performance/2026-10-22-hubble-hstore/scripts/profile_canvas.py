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

import argparse,collections,gzip,json,pathlib,time
from browser_probe import INIT, add_browser_arguments, prepare_browser_args, put_query

def main(argv=None):
    parser=argparse.ArgumentParser()
    add_browser_arguments(parser)
    parser.add_argument('--phase',default='after-adaptive')
    args=prepare_browser_args(parser,parser.parse_args(argv))
    ROOT=args.output_dir
    (ROOT/'comparison-screenshots').mkdir(exist_ok=True)
    from playwright.sync_api import sync_playwright
    with sync_playwright() as p:
     b=p.chromium.launch(executable_path=args.chromium,headless=True,args=['--no-sandbox','--disable-dev-shm-usage'])
     c=b.new_context(viewport={'width':1440,'height':1000});c.add_init_script(INIT);page=c.new_page();page.set_default_timeout(args.timeout_ms)
     page.goto(args.base_url+'/gremlin/perf_space_0998/perf_small64',wait_until='networkidle')
     page.get_by_role('button',name='Run Query',exact=True).wait_for()
     put_query(page,'g.V().limit(1000)')
     session=c.new_cdp_session(page);session.send('Profiler.enable');session.send('Profiler.setSamplingInterval',{'interval':1000});session.send('Profiler.start')
     start=time.perf_counter();page.get_by_role('button',name='Run Query',exact=True).click();page.wait_for_function("()=>{let s=window.__graphSnapshot();return s&&s.status==='ready'&&s.nodes===1000;}",timeout=args.timeout_ms)
     elapsed=(time.perf_counter()-start)*1000;profile=session.send('Profiler.stop')['profile']
     with gzip.open(ROOT/f'canvas1000-{args.phase}.cpuprofile.json.gz','wt') as f:json.dump(profile,f)
     nodes={n['id']:n for n in profile['nodes']};own=collections.Counter();inclusive=collections.Counter();parents={child:n['id'] for n in profile['nodes'] for child in n.get('children',[])}
     for nodeid,delta in zip(profile.get('samples',[]),profile.get('timeDeltas',[])):
      own[nodeid]+=delta
      cursor=nodeid
      while cursor in nodes:
       inclusive[cursor]+=delta
       if cursor not in parents:break
       cursor=parents[cursor]
     rows=[]
     for nid,amount in own.most_common(35):
      n=nodes[nid];frame=n['callFrame'];chain=[];cur=nid
      for _ in range(10):
       chain.append(nodes[cur]['callFrame']['functionName'])
       if cur not in parents:break
       cur=parents[cur]
      rows.append(dict(function=frame['functionName'],self_ms=round(amount/1000,2),inclusive_ms=round(inclusive[nid]/1000,2),url=frame['url'],line=frame['lineNumber']+1,column=frame['columnNumber']+1,callers=chain))
     result=dict(phase=args.phase,elapsed_ms=elapsed,snapshot=page.evaluate('window.__graphSnapshot()'),duration_ms=(profile['endTime']-profile['startTime'])/1000,hotspots=rows)
     (ROOT/f'canvas1000-{args.phase}-profile-summary.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2));b.close()
    return 0

if __name__=='__main__':
    raise SystemExit(main())
