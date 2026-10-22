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

import json, os
from benchmark import H,B,call,measure

SPACE=os.environ.get('PERF_GRAPH_SPACE','perf_space_0997')
if call('GET',B+f'/graphspaces/{SPACE}/graphs')['graphs']:
    raise RuntimeError('Graph scaling requires an empty dedicated GraphSpace; choose PERF_GRAPH_SPACE. Existing graphs are not deleted.')
for target in [1,10,50]:
    existing=call('GET',B+f'/graphspaces/{SPACE}/graphs')['graphs']
    for i in range(len(existing),target):
        name=f'perf_meta_{i:03d}'
        call('POST',H+f'/graphspaces/{SPACE}/graphs/{name}',{'nickname':name})
    print('GRAPHS READY',target,flush=True)
    for label,path in [
        ('hubble_graph_names','/list'),
        ('hubble_graphs_page1','?page_no=1&page_size=1'),
        ('hubble_graphs_page10','?page_no=1&page_size=10'),
        ('hubble_graph_metadata','/perf_meta_000/get')
    ]:
        measure(label,H+f'/graphspaces/{SPACE}/graphs'+path,n=10,warmup=2,graphs=target,space=SPACE)
