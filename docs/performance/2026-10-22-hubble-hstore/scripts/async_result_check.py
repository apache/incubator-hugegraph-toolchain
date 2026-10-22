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

"""Check async result limits without downloading a successful full result."""
import argparse
import json
import pathlib
import time

import requests

ROOT = pathlib.Path('/workspace/hubble-perf')
TERMINAL_STATES = {'success', 'failed', 'cancelled', 'canceled'}


def poll_task(session, task_url, record, timeout=300, poll_interval=1):
    """Return whether a terminal state was observed within the polling budget."""
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        remaining = deadline - time.monotonic()
        try:
            response = session.get(task_url, params={'with_result': 'false'},
                                   timeout=min(15, max(remaining, 0.001)))
            response.raise_for_status()
        except requests.Timeout:
            if time.monotonic() >= deadline:
                break
            raise
        task = response.json()
        status = task.get('task_status', task.get('status'))
        record['statuses'].append({'ts': time.time(), 'status': status})
        print(status, flush=True)
        if str(status).lower() in TERMINAL_STATES:
            record.update(final_status=status, task_metadata=task,
                          task_execution_ms=task.get('task_update', 0) -
                                            task.get('task_create', 0),
                          timed_out=False)
            if str(status).lower() == 'failed':
                response = session.get(task_url, params={'with_result': 'true'}, timeout=15)
                response.raise_for_status()
                details = response.json()
                record['failure_result'] = str(details.get('task_result', details.get('result')))[:1000]
            return True
        remaining = deadline - time.monotonic()
        if remaining > 0:
            time.sleep(min(poll_interval, remaining))
    record.update(final_status='TIMEOUT', timed_out=True, polling_timeout_seconds=timeout)
    return False


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', default='async-large-result-check.json')
    parser.add_argument('--timeout', type=float, default=300,
                        help='Maximum task polling duration in seconds (default: 300).')
    parser.add_argument('--poll-interval', type=float, default=1)
    args = parser.parse_args()
    if args.timeout <= 0 or args.poll_interval <= 0:
        parser.error('--timeout and --poll-interval must be positive')
    session = requests.Session()
    session.trust_env = False
    space = 'perf_space_0999'
    graph = 'perf_large8192'
    query = 'g.V().limit(12000)'
    response = session.post(
        f'http://127.0.0.1:8088/api/v1.3/graphspaces/{space}/graphs/{graph}/gremlin-query/async-task',
        json={'content': query}, timeout=30)
    data = response.json()
    assert response.status_code == 200 and data['status'] == 200, data
    task_id = data['data']['task_id']
    record = dict(graph=graph, space=space, query=query, task_id=task_id,
                  submission=data, statuses=[])
    print('submitted', task_id, flush=True)
    terminal = poll_task(
        session,
        f'http://127.0.0.1:18080/graphspaces/{space}/graphs/{graph}/tasks/{task_id}',
        record, args.timeout, args.poll_interval)
    (ROOT / args.output).write_text(json.dumps(record, indent=2) + '\n')
    print(json.dumps(record, indent=2), flush=True)
    return 0 if terminal else 2


if __name__ == '__main__':
    raise SystemExit(main())
