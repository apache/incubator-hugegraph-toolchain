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

"""Read-only live assertions for traversal limits and long map keys after review."""
import hashlib,json,pathlib
import requests
ROOT=pathlib.Path('/workspace/hubble-perf')
S=requests.Session();S.trust_env=False;S.headers['Accept-Encoding']='identity'
H='http://127.0.0.1:8088/api/v1.3';records=[]
def query(label,space,graph,content):
 r=S.post(H+f'/graphspaces/{space}/graphs/{graph}/gremlin-query',json={'content':content,'preview':True},timeout=120)
 o=r.json();assert r.status_code==200 and o['status']==200,(label,r.status_code,str(o)[:300])
 d=o['data'];rows=(d.get('json_view')or{}).get('data')or[];gv=d.get('graph_view')or{}
 rec={'label':label,'query':content,'bytes':len(r.content),'rows':len(rows),'vertices':len(gv.get('vertices')or[]),'edges':len(gv.get('edges')or[]),'metadata':{k:v for k,v in d.items()if k.endswith('limit')or k.endswith('truncated')or k=='default_limit_applied'}}
 assert rec['vertices']<=1000 and rec['edges']<=500,rec
 records.append(rec);return rec,rows
F=json.loads((ROOT/'large-property-fixture.json').read_text())
r=S.post(H+f"/graphspaces/{F['space']}/graphs/{F['graph']}/gremlin-query",json={'content':"g.V().groupCount().by('payload')",'preview':True},timeout=120)
o=r.json()
if r.status_code==200 and o.get('status')==200:
 d=o['data'];rows=d['json_view']['data'];assert rows==[{}] and d.get('properties_truncated')is True
 records.append({'label':'long_map_keys_1MiB','http_status':r.status_code,'bytes':len(r.content),'rows':len(rows),'keys_omitted':True})
else:
 records.append({'label':'long_map_keys_1MiB','http_status':r.status_code,'bytes':len(r.content),'message_chars':len(o.get('message')or''),'message_prefix':(o.get('message')or'')[:80],'response_sha256':hashlib.sha256(r.content).hexdigest(),'sdk_deserialization_failed':'Failed to deserialize'in(o.get('message')or''),'limitation':'SDK rejected a huge field name before Hubble preview; full message omitted from artifact'})
 assert len(o.get('message')or'')<=2048 and len(r.content)<8192 and o.get('cause')is None,records[-1]
rec,rows=query('long_map_keys_8192','perf_space_0999','perf_large8192',"g.V().limit(2).groupCount().by('payload')")
assert rows==[{}] and rec['bytes']<32768 and rec['metadata'].get('properties_truncated')is True,rec
space='perf_space_0998';graph='perf_small64'
rec,rows=query('terminal_read_default',space,graph,'g.V().fold().unfold().out()')
assert len(rows)==300 and rec['metadata'].get('default_limit_applied')is True,rec
rec,rows=query('terminal_read_explicit_preview',space,graph,'g.V().fold().unfold().out().limit(12000)')
assert len(rows)==1000 and rec['metadata'].get('result_truncated')is True and not rec['metadata'].get('default_limit_applied'),rec
for label,content in [('terminal_count','g.V().fold().unfold().out().count()'),('aggregate_count','g.V().count()')]:
 rec,rows=query(label,space,graph,content);assert rows==[12000] and not rec['metadata'].get('default_limit_applied'),rec;rec['count']=rows[0]
for v in F['vertices']:
 o=S.get(f"http://127.0.0.1:18080/graphspaces/{F['space']}/graphs/{F['graph']}/graph/vertices/{v['id']}",timeout=30).json()
 for k,digest in v['sha256'].items():assert hashlib.sha256(o['properties'][k].encode()).hexdigest()==digest
result={'performance_sample':False,'server':'patched Struct','records':records,'original_property_sha256_unchanged':True}
(ROOT/'review-followup-assertions.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
