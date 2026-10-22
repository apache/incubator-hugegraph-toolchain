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

import json, pathlib, time
ROOT=pathlib.Path('/workspace/hubble-perf')
while True:
    row={'ts':time.time()}
    for name in ['pd','store','server','hubble']:
        try:
            pid=(ROOT/f'{name}.pid').read_text().strip()
            status=pathlib.Path(f'/proc/{pid}/status').read_text()
            rss=int(next(l.split()[1] for l in status.splitlines() if l.startswith('VmRSS:')))
            stat=pathlib.Path(f'/proc/{pid}/stat').read_text().split()
            row[name]={'rss_kib':rss,'cpu_ticks':int(stat[13])+int(stat[14]),'pid':int(pid)}
        except (OSError, StopIteration):pass
    with (ROOT/'resources.jsonl').open('a') as f:f.write(json.dumps(row)+'\n')
    time.sleep(.5)
