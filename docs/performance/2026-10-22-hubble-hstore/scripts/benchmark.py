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

"""Repeatable local Hubble + PD + HStore measurements; raw observations in JSONL."""
import collections, json, math, pathlib, statistics, sys, time
import requests

ROOT=pathlib.Path('/workspace/hubble-perf')
S=requests.Session(); S.trust_env=False
H='http://127.0.0.1:8088/api/v1.3'
B='http://127.0.0.1:18080'

def call(method,url,body=None,timeout=310):
    r=S.request(method,url,json=body,timeout=timeout)
    if r.status_code>=400: raise RuntimeError(f'{method} {url}: {r.status_code} {r.text[:350]}')
    obj=r.json()
    if isinstance(obj,dict) and isinstance(obj.get('status'),int) and obj['status']!=200:
        raise RuntimeError(f'{method} {url}: {str(obj)[:350]}')
    return obj

def trace_since(offset):
    p=ROOT/'trace.jsonl'
    if not p.exists(): return []
    with p.open() as f:
        f.seek(offset)
        return [json.loads(l) for l in f if l.endswith('\n')]

def measure(label,url,body=None,n=20,warmup=3,**tags):
    method='POST' if body is not None else 'GET'
    for _ in range(warmup): call(method,url,body)
    records=[]
    for i in range(n):
        p=ROOT/'trace.jsonl';offset=p.stat().st_size if p.exists() else 0
        started=time.time();start=time.perf_counter(); error=None
        try:
            r=S.request(method,url,json=body,timeout=310)
            elapsed=(time.perf_counter()-start)*1000
            raw=r.content; obj=r.json()
            status=obj.get('status',r.status_code) if isinstance(obj,dict) else r.status_code
            if status!=200: error=str(obj)[:300]
        except Exception as e:
            elapsed=(time.perf_counter()-start)*1000;status=0;raw=b'';error=str(e)
        traces=trace_since(offset)
        counts=collections.Counter(t['kind'] for t in traces if 'ms' in t)
        paths=collections.Counter(t.get('path') for t in traces if t['kind']=='server-http' and 'ms' in t)
        grpc=collections.Counter(t.get('path') for t in traces if t['kind']=='pd-grpc')
        rec=dict(label=label,iteration=i,ts=started,ms=elapsed,status=status,bytes=len(raw),trace_counts=dict(counts),server_paths=dict(paths),grpc_paths=dict(grpc),**tags)
        if error: rec['error']=error
        with (ROOT/'samples.jsonl').open('a') as f: f.write(json.dumps(rec)+'\n')
        records.append(rec)
    good=[r['ms'] for r in records if r['status']==200]
    summary=dict(label=label,n=n,success=len(good),p50_ms=statistics.median(good) if good else None,p95_ms=sorted(good)[math.ceil(.95*len(good))-1] if good else None,bytes=records[-1]['bytes'],trace_counts=records[-1]['trace_counts'],**tags)
    with (ROOT/'summary.jsonl').open('a') as f: f.write(json.dumps(summary)+'\n')
    print(json.dumps(summary),flush=True)
    return records

def spaces():
    existing=call('GET',B+'/graphspaces')['graphSpaces']
    if existing != ['DEFAULT']:
        raise RuntimeError('Space scaling requires a fresh disposable cluster containing only DEFAULT; existing data is not deleted.')
    for target in [1,10,100,500,1000]:
        for i in range(len(existing),target):
            name=f'perf_space_{i:04d}'
            call('POST',B+'/graphspaces',dict(name=name,nickname=name,cpu_limit=64,memory_limit=128,storage_limit=1000000,max_graph_number=100,max_role_number=100,auth=False))
            existing.append(name)
        print(f'SPACES READY {len(existing)}',flush=True)
        n=20 if target<=100 else 10
        for label,url in [('server_spaces',B+'/graphspaces'),('hubble_space_names',H+'/graphspaces/list'),('hubble_spaces_page10',H+'/graphspaces?page_no=1&page_size=10'),('hubble_spaces_page1',H+'/graphspaces?page_no=1&page_size=1'),('hubble_space_detail',H+'/graphspaces/DEFAULT')]:
            measure(label,url,n=n,spaces=target)
        measure('hubble_spaces_all',H+'/graphspaces?all=true',n=5,warmup=1,spaces=target)

if __name__=='__main__':
    if sys.argv[1]=='spaces': spaces()
