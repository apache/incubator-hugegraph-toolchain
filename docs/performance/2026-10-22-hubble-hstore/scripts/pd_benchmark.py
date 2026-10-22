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

import json, math, pathlib, statistics, sys, time
ROOT=pathlib.Path('/workspace/hubble-perf')
sys.path.insert(0,str(ROOT/'proto'))
import grpc, pdpb_pb2, pdpb_pb2_grpc

with grpc.insecure_channel('127.0.0.1:8686') as channel:
    stub=pdpb_pb2_grpc.PDStub(channel)
    for name in ['DEFAULT','']:
        request=pdpb_pb2.GetGraphSpaceRequest(graph_Space_Name=name)
        values=[]
        for i in range(33):
            start=time.perf_counter();res=stub.GetGraphSpace(request,timeout=10);ms=(time.perf_counter()-start)*1000
            if i>=3:values.append(ms)
        item=dict(label='pd_GetGraphSpace_'+('single' if name else 'all'),n=len(values),p50_ms=statistics.median(values),p95_ms=sorted(values)[math.ceil(len(values)*.95)-1],returned_spaces=len(res.graph_space),response_bytes=res.ByteSize(),header=str(res.header))
        print(json.dumps(item),flush=True)
        with (ROOT/'pd-summary.jsonl').open('a') as f:f.write(json.dumps(item)+'\n')
