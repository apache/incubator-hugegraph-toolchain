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

"""Check existing async result limits without downloading a successful full result."""
import argparse,json,pathlib,time
import requests
parser=argparse.ArgumentParser();parser.add_argument('--output',default='async-large-result-check.json');args=parser.parse_args()
S=requests.Session();S.trust_env=False;ROOT=pathlib.Path('/workspace/hubble-perf')
space='perf_space_0999';graph='perf_large8192';query='g.V().limit(12000)'
r=S.post(f'http://127.0.0.1:8088/api/v1.3/graphspaces/{space}/graphs/{graph}/gremlin-query/async-task',json={'content':query},timeout=30)
d=r.json();assert r.status_code==200 and d['status']==200,d
taskid=d['data']['task_id'];record=dict(graph=graph,space=space,query=query,task_id=taskid,submission=d,statuses=[])
print('submitted',taskid,flush=True)
for i in range(60):
 r=S.get(f'http://127.0.0.1:18080/graphspaces/{space}/graphs/{graph}/tasks/{taskid}',params={'with_result':'false'},timeout=15);r.raise_for_status();task=r.json();status=task.get('task_status',task.get('status'))
 record['statuses'].append(dict(ts=time.time(),status=status));print(status,flush=True)
 if str(status).lower() in ['success','failed','cancelled']:
  record['final_status']=status;record['task_metadata']=task;record['task_execution_ms']=task.get('task_update',0)-task.get('task_create',0)
  if str(status).lower()=='failed':
   details=S.get(f'http://127.0.0.1:18080/graphspaces/{space}/graphs/{graph}/tasks/{taskid}',params={'with_result':'true'},timeout=15).json();record['failure_result']=str(details.get('task_result',details.get('result')))[:1000]
  break
 time.sleep(1)
(ROOT/args.output).write_text(json.dumps(record,indent=2)+'\n')
print(json.dumps(record,indent=2),flush=True)
