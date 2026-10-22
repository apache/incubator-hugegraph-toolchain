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

import json, pathlib, time, hashlib, base64
from benchmark import ROOT, S, H, B, call, measure

SPACE='perf_space_0999'
COUNT=12000

def seed():
    for graph,size in [('perf_small64',64),('perf_large8192',8192)]:
        SPACE='perf_space_0998' if graph=='perf_small64' else 'perf_space_0999'
        base=B+f'/graphspaces/{SPACE}/graphs/{graph}'
        graphs=call('GET',B+f'/graphspaces/{SPACE}/graphs')['graphs']
        if graph in graphs:
            raise RuntimeError(f'Seed requires a fresh graph name: {SPACE}/{graph} already exists. Use run for the retained dataset.')
        if graph not in graphs:
            call('POST',H+f'/graphspaces/{SPACE}/graphs/{graph}',{'nickname':graph})
        schemas=call('GET',base+'/schema')
        if not schemas.get('propertykeys'):
            call('POST',base+'/schema/propertykeys',dict(name='payload',data_type='TEXT',cardinality='SINGLE'))
        if not schemas.get('vertexlabels'):
            call('POST',base+'/schema/vertexlabels',dict(name='node',id_strategy='CUSTOMIZE_NUMBER',properties=['payload']))
        if not schemas.get('edgelabels'):
            call('POST',base+'/schema/edgelabels',dict(name='link',source_label='node',target_label='node',frequency='SINGLE',properties=['payload']))
        for kind in ['vertices','edges']:
            start=time.perf_counter()
            for offset in range(0,COUNT,100):
                batch=[]
                for i in range(offset,min(offset+100,COUNT)):
                    v=i+1
                    props={'payload':base64.b64encode(hashlib.shake_256(f'{graph}:{kind}:{v}'.encode()).digest(size)).decode()[:size]}
                    batch.append(dict(label='node',id=v,properties=props) if kind=='vertices' else dict(label='link',outV=v,inV=(v%COUNT)+1,outVLabel='node',inVLabel='node',properties=props))
                call('POST',base+f'/graph/{kind}/batch',batch)
            elapsed=time.perf_counter()-start
            rec=dict(graph=graph,kind=kind,count=COUNT,property_bytes=size,seconds=elapsed,elements_per_second=COUNT/elapsed)
            with (ROOT/'ingest.jsonl').open('a') as f:f.write(json.dumps(rec)+'\n')
            print('SEEDED',json.dumps(rec),flush=True)
        for element in ['V','E']:
            result=call('POST',B+'/gremlin',dict(gremlin=f'g.{element}().count()',bindings={},language='gremlin-groovy',aliases={'g':'__g_'+SPACE+'-'+graph}))
            print('COUNT',graph,element,str(result)[:200],flush=True)

def run():
    for graph,size in [('perf_small64',64),('perf_large8192',8192)]:
        SPACE='perf_space_0998' if graph=='perf_small64' else 'perf_space_0999'
        hb=H+f'/graphspaces/{SPACE}/graphs/{graph}'
        sb=B+f'/graphspaces/{SPACE}/graphs/{graph}'
        for label,url,body in [
            ('graph_metadata',hb+'/get',None),
            ('graph_overview_counts',hb+'/gremlin-query',None),
            ('server_vertices_250',sb+'/graph/vertices?limit=250',None),
            ('server_edges_250',sb+'/graph/edges?limit=250',None),
            ('server_vertices_12000',sb+'/graph/vertices?limit=12000',None),
            ('server_edges_12000',sb+'/graph/edges?limit=12000',None),
            ('hubble_default_vertices',hb+'/gremlin-query',{'content':'g.V()'}),
            ('hubble_vertices_12000',hb+'/gremlin-query',{'content':'g.V().limit(12000)'}),
            ('hubble_paths_12000',hb+'/gremlin-query',{'content':'g.V().outE().inV().path().limit(12000)'}),
        ]:
            measure(label,url,body,n=5 if '12000' in label else 10,warmup=1,graph=graph,property_bytes=size,vertices=COUNT,edges=COUNT)
        measure('hubble_space_detail_loaded',H+f'/graphspaces/{SPACE}',n=5,warmup=1,graph=graph,property_bytes=size)

if __name__=='__main__':
    import sys
    if sys.argv[1]=='seed':seed()
    elif sys.argv[1]=='run':run()
