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

"""Check live default limits, query semantics, and bounded preview metadata."""
import json,pathlib,time
import requests
ROOT=pathlib.Path('/workspace/hubble-perf');S=requests.Session();S.trust_env=False;S.headers['Accept-Encoding']='identity'
H='http://127.0.0.1:8088/api/v1.3'
def query(space,graph,content,preview=True):
 start=time.perf_counter();r=S.post(H+f'/graphspaces/{space}/graphs/{graph}/gremlin-query',json=dict(content=content,preview=preview),timeout=310)
 obj=r.json();assert r.status_code==200 and obj['status']==200,(r.status_code,str(obj)[:500])
 d=obj['data'];gv=d.get('graph_view') or {};verts=gv.get('vertices') or [];edges=gv.get('edges') or [];rows=(d.get('json_view') or {}).get('data') or []
 rec=dict(graph=graph,query=content,preview=preview,ms=(time.perf_counter()-start)*1000,bytes=len(r.content),rows=len(rows),vertices=len(verts),edges=len(edges),type=d.get('type'),metadata={k:v for k,v in d.items() if k.endswith('limit') or k.endswith('truncated') or k=='default_limit_applied'})
 assert len(verts)<=1000 and len(edges)<=500,rec
 ids={str(v['id']) for v in verts}
 for e in edges:
  assert str(e.get('outV',e.get('source'))) in ids and str(e.get('inV',e.get('target'))) in ids, 'dangling preview edge'
 return rec,rows

def main():
 records=[]
 for space,graph in [('perf_space_0998','perf_small64'),('perf_space_0999','perf_large8192')]:
  for label,content in [('default','g.V()'),('vertices1000','g.V().limit(1000)'),('vertices12000','g.V().limit(12000)'),('paths1000','g.V().outE().inV().path().limit(1000)'),('paths12000','g.V().outE().inV().path().limit(12000)'),('count','g.V().count()'),('no_op_write',"g.V().hasId(-1).drop()")]:
   rec,rows=query(space,graph,content);rec['label']=label
   if label=='default':assert rec['rows']==300 and rec['metadata'].get('default_limit_applied') is True,rec
   if label=='vertices1000':assert rec['rows']==1000 and rec['vertices']==1000,rec
   if '12000' in label:assert rec['rows']==1000 and rec['metadata'].get('result_truncated') is True,rec
   if label=='count':assert rows==[12000] and not rec['metadata'].get('default_limit_applied'),rec
   if label=='no_op_write':assert rec['rows']==0 and not rec['metadata'].get('default_limit_applied'),rec
   records.append(rec);print(json.dumps(rec),flush=True)
  for kind in ['V','E']:
   rec,rows=query(space,graph,f'g.{kind}().count()');assert rows==[12000],rec
   rec['label']='final_'+kind+'_count';rec['count']=rows[0];records.append(rec)
 (ROOT/'comparison-after-assertions.json').write_text(json.dumps(records,indent=2)+'\n')
if __name__=='__main__':main()
