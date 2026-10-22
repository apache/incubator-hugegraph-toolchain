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

"""Measure bounded property previews and confirm original data stays unchanged."""
import argparse,hashlib,json,pathlib,re,time
import requests
ROOT=pathlib.Path('/workspace/hubble-perf');F=json.loads((ROOT/'large-property-fixture.json').read_text());S=requests.Session();S.trust_env=False;S.headers['Accept-Encoding']='identity'
parser=argparse.ArgumentParser();parser.add_argument('phase');parser.add_argument('--assert-trim',action='store_true');args=parser.parse_args()
base=f"/graphspaces/{F['space']}/graphs/{F['graph']}";records=[]
def properties(x):
 if isinstance(x,dict):
  if 'payload' in x:yield x
  for v in x.values():yield from properties(v)
 elif isinstance(x,list):
  for v in x:yield from properties(v)
for label,content in [('vertices','g.V()'),('edges','g.E()'),('paths','g.V().outE().inV().path()'),('scalar',f"g.V({F['vertex_ids'][1]}).values('payload')"),('ids','g.V().id()'),('count','g.V().count()')]:
 started=time.perf_counter();r=S.post('http://127.0.0.1:8088/api/v1.3'+base+'/gremlin-query',json={'content':content,'preview':True},timeout=60);o=r.json();assert r.status_code==200 and o['status']==200,(label,r.status_code,str(o)[:300]);d=o['data']
 gv=d.get('graph_view')or{};verts=gv.get('vertices')or[];edges=gv.get('edges')or[];rows=(d.get('json_view')or{}).get('data')or[]
 props=list(properties(d));payload_lens=[len(p['payload'])for p in props];rootflag=d.get('properties_truncated',False)
 rec=dict(label=label,query=content,ms=(time.perf_counter()-started)*1000,bytes=len(r.content),rows=len(rows),vertices=len(verts),edges=len(edges),properties_truncated=rootflag,property_preview_limit=d.get('property_preview_limit'),payload_lengths=payload_lens,vertex_ids=[v['id']for v in verts],boundary256_lengths=[len(p['boundary256'])for p in props if 'boundary256'in p],boundary257_lengths=[len(p['boundary257'])for p in props if 'boundary257'in p])
 if label in ['vertices','edges','paths']:
  assert {str(v['id']) for v in verts}=={str(i)for i in F['vertex_ids']},rec
 if args.assert_trim and label in ['vertices','edges','paths','scalar']:
  assert rootflag and d['property_preview_limit']==256 and len(r.content)<32768,rec
  for p in props:
   assert len(p['payload'])<320 and re.search(r'preview 256/(102400|1048576) chars',p['payload']),rec
   if 'boundary256'in p:assert p['boundary256']=='界'*256,rec
   if 'boundary257'in p:assert 'preview 256/257 chars'in p['boundary257'],rec
  if label=='scalar':assert len(rows[0])<320 and '1048576 chars'in rows[0],rec
 if label=='ids':assert set(rows)==set(F['vertex_ids']) and not rootflag,rec
 if label=='count':assert rows==[2] and not rootflag,rec
 records.append(rec);print(json.dumps(rec),flush=True)
for v in F['vertices']:
 r=S.get('http://127.0.0.1:18080'+base+'/graph/vertices/'+str(v['id']),timeout=30);r.raise_for_status();d=r.json()
 assert d['id']==v['id']
 for k,digest in v['sha256'].items():assert hashlib.sha256(d['properties'][k].encode()).hexdigest()==digest,(v['id'],k)
for e in S.get('http://127.0.0.1:18080'+base+'/graph/edges?limit=10',timeout=30).json()['edges']:
 assert hashlib.sha256(e['properties']['payload'].encode()).hexdigest()==F['edges'][0]['sha256']
(ROOT/f'large-property-{args.phase}.json').write_text(json.dumps(dict(fixture=F,records=records,original_data_sha256_unchanged=True),indent=2,ensure_ascii=False)+'\n')
