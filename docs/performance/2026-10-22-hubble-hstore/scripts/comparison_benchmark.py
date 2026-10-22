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

"""Direct, fixed-encoding before/after Hubble measurements; no retained response bodies."""
import argparse, collections, gc, json, math, pathlib, re, statistics, time
import requests
ROOT=pathlib.Path('/workspace/hubble-perf')
H='http://127.0.0.1:8088/api/v1.3'
S=requests.Session(); S.trust_env=False; S.headers['Accept-Encoding']='identity'

def request(label,path,body=None,phase='',iteration=-1):
    gc.collect()
    start=time.perf_counter(); first=None; size=0; prefix=b''
    p=ROOT/'trace.jsonl'; offset=p.stat().st_size if p.exists() else 0
    with S.request('POST' if body is not None else 'GET',H+path,json=body,timeout=310,stream=True) as response:
        headers_ms=(time.perf_counter()-start)*1000
        for chunk in response.iter_content(256*1024):
            if first is None: first=(time.perf_counter()-start)*1000
            size+=len(chunk)
            if len(prefix)<8192:prefix+=chunk[:8192-len(prefix)]
        elapsed=(time.perf_counter()-start)*1000
        record=dict(phase=phase,label=label,iteration=iteration,ts=time.time(),ms=elapsed,headers_ms=headers_ms,first_body_ms=first,bytes=size,http_status=response.status_code,content_encoding=response.headers.get('Content-Encoding','identity'),path=path,query=body.get('content') if body else None)
    traces=[]
    if p.exists():
        with p.open() as f:
            f.seek(offset)
            traces=[json.loads(line) for line in f if line.endswith('\n')]
    record['trace_counts']=dict(collections.Counter(t['kind'] for t in traces if 'ms' in t))
    status_match=re.search(rb'"status"\s*:\s*(\d+)',prefix)
    record['application_status']=int(status_match.group(1)) if status_match else None
    record['error_prefix']=prefix.decode(errors='replace')[:500] if response.status_code!=200 or record['application_status'] not in [None,200] else None
    with (ROOT/f'comparison-{phase}-samples.jsonl').open('a') as f:f.write(json.dumps(record)+'\n')
    print(json.dumps({k:record[k] for k in ['phase','label','iteration','ms','bytes','http_status','error_prefix']}),flush=True)
    return record

def main():
    parser=argparse.ArgumentParser();parser.add_argument('phase');parser.add_argument('--large',action='store_true');parser.add_argument('--metadata-only',action='store_true');parser.add_argument('--only',nargs='+');parser.add_argument('--preview',action='store_true');parser.add_argument('--warmup',type=int,default=1);parser.add_argument('--repeat',type=int);args=parser.parse_args()
    scenarios=[('space_names','/graphspaces/list',None,5),('spaces_page10','/graphspaces?page_no=1&page_size=10',None,5),('spaces_all','/graphspaces?all=true',None,3),('graphs_page1','/graphspaces/perf_space_0997/graphs?page_no=1&page_size=1',None,5),('graphs_page10','/graphspaces/perf_space_0997/graphs?page_no=1&page_size=10',None,5),('spaces_brief','/graphspaces?brief=true&page_no=1&page_size=-1',None,5),('graphs_brief','/graphspaces/perf_space_0997/graphs?brief=true&page_no=1&page_size=-1',None,5)]
    if not args.metadata_only:
        for graph,space in [('perf_small64','perf_space_0998'),('perf_large8192','perf_space_0999')]:
            prefix='/graphspaces/'+space+'/graphs/'+graph+'/gremlin-query'
            queries=[('default','g.V()'),('vertices300','g.V().limit(300)'),('vertices1000','g.V().limit(1000)'),('paths1000','g.V().outE().inV().path().limit(1000)'),('count','g.V().count()')]
            if args.large:queries += [('vertices12000','g.V().limit(12000)'),('paths12000','g.V().outE().inV().path().limit(12000)')]
            scenarios += [(graph+'_'+label,prefix,{'content':query},3 if '12000' in label else 5) for label,query in queries]
    for label,path,body,n in scenarios:
        if args.only and label not in args.only:continue
        if args.preview and body is not None:body={**body,'preview':True}
        if args.repeat is not None:n=args.repeat
        for _ in range(args.warmup):request(label,path,body,args.phase,-1)
        records=[request(label,path,body,args.phase,i) for i in range(n)]
        good=[r['ms'] for r in records if r['http_status']==200 and not r['error_prefix']]
        summary=dict(phase=args.phase,label=label,n=n,warmup=args.warmup,success=len(good),p50_ms=statistics.median(good) if good else None,p95_ms=sorted(good)[math.ceil(.95*len(good))-1] if good else None,bytes=records[-1]['bytes'],encoding='identity',transport='direct-discovered-server')
        with (ROOT/f'comparison-{args.phase}-summary.jsonl').open('a') as f:f.write(json.dumps(summary)+'\n')
        print('SUMMARY',json.dumps(summary),flush=True)
if __name__=='__main__':main()
