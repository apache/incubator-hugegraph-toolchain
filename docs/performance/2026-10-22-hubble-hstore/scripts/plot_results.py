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

import csv,json,pathlib
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np
ROOT=pathlib.Path('/workspace/hubble-perf')
data=[json.loads(l) for l in (ROOT/'summary.jsonl').read_text().splitlines()]
fig,axes=plt.subplots(1,3,figsize=(17,5.4))
colors=['#2563eb','#e87920','#14a184','#7c3aed']
for color,label,title in zip(colors,['hubble_space_names','hubble_spaces_page10','hubble_spaces_all'],['Names','Page size 10','All with statistics']):
    series=[x for x in data if x['label']==label and 'spaces' in x]
    axes[0].plot([x['spaces'] for x in series],[x['p50_ms'] for x in series],'-o',label=title,color=color)
axes[0].set(xscale='log',xlabel='GraphSpaces',ylabel='p50 latency (ms)',title='List cost grows with space count')
axes[0].set_xticks([1,10,100,500,1000],['1','10','100','500','1000']);axes[0].legend(fontsize=8)
labels=['graph_metadata','graph_overview_counts','server_vertices_12000','hubble_default_vertices','hubble_vertices_12000','hubble_paths_12000']
names=['Metadata','V/E count','Server 12k V','Hubble default V','Hubble 12k V','Hubble 12k paths']
for off,graph,color,legend in [(-.19,'perf_small64',colors[0],'64 B property'),(.19,'perf_large8192',colors[1],'8 KiB property')]:
    series=[next(x for x in data if x['label']==label and x.get('graph')==graph) for label in labels]
    axes[1].barh(np.arange(len(labels))+off,[x['p50_ms'] for x in series],height=.35,label=legend,color=color)
    axes[2].barh(np.arange(len(labels))+off,[x['bytes']/1024**2 for x in series],height=.35,label=legend,color=color)
axes[1].set(xscale='log',xlabel='p50 latency (ms), log scale',title='12,000 vertices + 12,000 edges')
axes[2].set(xscale='log',xlabel='Response body (MiB), log scale',title='Hubble serializes multiple views')
for ax in axes[1:]:ax.set_yticks(np.arange(len(labels)),names);ax.invert_yaxis();ax.legend(fontsize=8)
for ax in axes:ax.grid(axis='x',alpha=.2);ax.set_axisbelow(True)
fig.suptitle('Local source build: Hubble + PD + HStore | 4 vCPU / 16 GiB | anonymous mode',fontsize=13)
fig.tight_layout();fig.savefig(ROOT/'performance.png',dpi=160);fig.savefig(ROOT/'performance.svg')
with (ROOT/'summary.csv').open('w') as f:
    columns=['label','spaces','graphs','graph','property_bytes','n','success','p50_ms','p95_ms','bytes','server_transport','phase','excluded_from_report','exclusion_reason']
    w=csv.DictWriter(f,fieldnames=columns,extrasaction='ignore');w.writeheader();w.writerows(data)
