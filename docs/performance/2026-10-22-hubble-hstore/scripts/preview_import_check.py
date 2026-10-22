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

"""Reject a marked, invalid preview upload without supplying writable graph data."""
import argparse,hashlib,json,pathlib,requests
ROOT=pathlib.Path('/workspace/hubble-perf');F=json.loads((ROOT/'large-property-fixture.json').read_text());S=requests.Session();S.trust_env=False
parser=argparse.ArgumentParser();parser.add_argument('--output',default='preview-import-check.json');args=parser.parse_args()
base=f"/graphspaces/{F['space']}/graphs/{F['graph']}";payload={'vertices':[{'~hubble_properties_preview':True}],'edges':[]}
r=S.post('http://127.0.0.1:8088/api/v1.3'+base+'/import',files={'file':('marked-preview.json',json.dumps(payload).encode(),'application/json')},timeout=30)
d=r.json();assert r.status_code>=400 or d.get('status',200)!=200,(r.status_code,d)
for vertex in F['vertices']:
 current=S.get('http://127.0.0.1:18080'+base+'/graph/vertices/'+str(vertex['id']),timeout=30).json()
 for key,digest in vertex['sha256'].items():assert hashlib.sha256(current['properties'][key].encode()).hexdigest()==digest
record=dict(http_status=r.status_code,response=d,submitted_file=payload,contains_vertex_id_or_label=False,original_vertex_sha256_unchanged=True)
(ROOT/args.output).write_text(json.dumps(record,indent=2,ensure_ascii=False)+'\n');print(json.dumps(record,indent=2,ensure_ascii=False))
