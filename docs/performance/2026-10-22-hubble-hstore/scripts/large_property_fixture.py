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

"""Seed an isolated large-property graph, without overwriting existing graph data."""
import argparse,base64,hashlib,json,pathlib,requests
parser=argparse.ArgumentParser();parser.add_argument('--resume-empty',action='store_true');args=parser.parse_args()
ROOT=pathlib.Path('/workspace/hubble-perf');S=requests.Session();S.trust_env=False
H='http://127.0.0.1:8088/api/v1.3';B='http://127.0.0.1:18080';SPACE='perf_space_0996';GRAPH='perf_property_preview';IDS=[9007199254740993,9007199254740995]
def payload(key,size):return base64.b64encode(hashlib.shake_256(key.encode()).digest(size)).decode()[:size]
def req(method,url,body=None):
 r=S.request(method,url,json=body,timeout=60);r.raise_for_status();d=r.json();assert not isinstance(d,dict) or not isinstance(d.get('status'),int) or d['status']==200,(url,str(d)[:300]);return d
base=B+f'/graphspaces/{SPACE}/graphs/{GRAPH}'
existing=req('GET',B+f'/graphspaces/{SPACE}/graphs')['graphs']
if GRAPH in existing:
 if not args.resume_empty:raise RuntimeError('Fixture already exists; refusing to overwrite it.')
 assert not req('GET',base+'/graph/vertices?limit=1')['vertices'] and not req('GET',base+'/graph/edges?limit=1')['edges'],'Cannot resume a graph containing data'
else:req('POST',H+f'/graphspaces/{SPACE}/graphs/{GRAPH}',{'nickname':GRAPH})
schema=req('GET',base+'/schema')
for name in ['payload','boundary256','boundary257']:
 if name not in [x['name'] for x in schema['propertykeys']]:req('POST',base+'/schema/propertykeys',dict(name=name,data_type='TEXT',cardinality='SINGLE'))
if not schema['vertexlabels']:req('POST',base+'/schema/vertexlabels',dict(name='node',id_strategy='CUSTOMIZE_NUMBER',properties=['payload','boundary256','boundary257']))
if not schema['edgelabels']:req('POST',base+'/schema/edgelabels',dict(name='link',source_label='node',target_label='node',frequency='SINGLE',properties=['payload']))
vertices=[dict(id=identifier,label='node',properties=dict(payload=payload(f'{GRAPH}:v:{identifier}',size),boundary256='界'*256,boundary257='x'*257)) for identifier,size in zip(IDS,[100*1024,1024*1024])]
req('POST',base+'/graph/vertices/batch',vertices)
edge=dict(label='link',outV=IDS[0],inV=IDS[1],outVLabel='node',inVLabel='node',properties=dict(payload=payload(f'{GRAPH}:edge',1024*1024)))
req('POST',base+'/graph/edges/batch',[edge])
record=dict(space=SPACE,graph=GRAPH,vertex_ids=IDS,vertices=[],edges=[])
for vertex in vertices:
 d=req('GET',base+'/graph/vertices/'+str(vertex['id']));p=d['properties'];assert p==vertex['properties']
 record['vertices'].append(dict(id=d['id'],lengths={k:len(v) for k,v in p.items()},sha256={k:hashlib.sha256(v.encode()).hexdigest() for k,v in p.items()}))
edges=req('GET',base+'/graph/edges?limit=10')['edges'];assert len(edges)==1 and edges[0]['properties']==edge['properties']
for e in edges:record['edges'].append(dict(id=e['id'],outV=e['outV'],inV=e['inV'],payload_length=len(e['properties']['payload']),sha256=hashlib.sha256(e['properties']['payload'].encode()).hexdigest()))
(ROOT/'large-property-fixture.json').write_text(json.dumps(record,indent=2,ensure_ascii=False)+'\n');print(json.dumps(record,indent=2,ensure_ascii=False))
