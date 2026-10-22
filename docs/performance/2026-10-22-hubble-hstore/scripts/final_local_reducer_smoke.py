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

"""Read-only live smoke checks for local-reducer bounds after final review."""
import json
from pathlib import Path
import requests

url = 'http://127.0.0.1:8088/api/v1.3/graphspaces/perf_space_0998/graphs/perf_small64/gremlin-query'
session = requests.Session()
session.trust_env = False
session.headers['Accept-Encoding'] = 'identity'
cases = [
    ('local_default', 'g.V().valueMap().count(local)', 300, True, False),
    ('local_qualified', 'g.V().valueMap().count(Scope.local)', 300, True, False),
    ('local_bounded', 'g.V().limit(10).valueMap().count(local)', 10, False, False),
    ('local_large_preview', 'g.V().valueMap().count(local).limit(12000)', 1000, False, True),
    ('global_count', 'g.V().count()', 1, False, False),
]
records = []
for name, query, size, defaulted, truncated in cases:
    response = session.post(url, json={'content': query, 'preview': True}, timeout=30)
    result = response.json()
    assert response.status_code == 200 and result['status'] == 200, (name, response.status_code, str(result)[:300])
    data = result['data']
    rows = data['json_view']['data']
    assert len(rows) == size, (name, len(rows))
    assert bool(data.get('default_limit_applied')) == defaulted, name
    assert bool(data.get('result_truncated')) == truncated, name
    assert not data.get('properties_truncated'), name
    assert rows == ([12000] if name == 'global_count' else [1] * size), name
    records.append({'name': name, 'query': query, 'http_status': response.status_code,
                    'rows': len(rows), 'response_bytes': len(response.content),
                    'default_limit_applied': defaulted, 'result_truncated': truncated})
output = {'performance_sample': False, 'records': records, 'passed': len(records),
          'property_budget_verified_by_unit_tests': True,
          'note': '10,000-value projection budget is covered by nested/repeated-object unit tests; these live requests verify reducer semantics only'}
Path('/workspace/hubble-perf/final-local-reducer-smoke.json').write_text(json.dumps(output, indent=2) + '\n')
print(json.dumps({'passed': len(records), 'performance_sample': False}))
