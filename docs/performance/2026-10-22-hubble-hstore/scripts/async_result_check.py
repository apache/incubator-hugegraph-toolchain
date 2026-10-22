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
import re
import time

import requests

EXPECTED_LIMIT_BYTES = 16 * 1024 * 1024
TERMINAL_STATES = {'success', 'failed', 'cancelled', 'canceled'}


def poll_task(session, task_url, record, timeout=300, poll_interval=1):
    """Succeed only for the expected retained 16 MiB result-size rejection."""
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
                failure = str(details.get('task_result', details.get('result')))
                record['failure_result'] = failure[:1000]
                match = re.search(r'Task result size (\d+) exceeded limit (\d+) bytes', failure)
                if match:
                    size, limit = map(int, match.groups())
                    record.update(reported_result_bytes=size, retained_limit_bytes=limit)
                    if size > limit and limit == EXPECTED_LIMIT_BYTES:
                        record['expected_limit_rejection'] = True
                        return True
            record['expected_limit_rejection'] = False
            record['validation_error'] = 'Unexpected terminal outcome; expected the retained 16 MiB size rejection'
            return False
        remaining = deadline - time.monotonic()
        if remaining > 0:
            time.sleep(min(poll_interval, remaining))
    record.update(final_status='TIMEOUT', timed_out=True, polling_timeout_seconds=timeout, expected_limit_rejection=False)
    return False


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=pathlib.Path, default=pathlib.Path('hubble-results/async-large-result-check.json'))
    parser.add_argument('--hubble-api-url', default='http://127.0.0.1:8088/api/v1.3')
    parser.add_argument('--server-url', default='http://127.0.0.1:18080')
    parser.add_argument('--space', default='perf_space_0999')
    parser.add_argument('--graph', default='perf_large8192')
    parser.add_argument('--timeout', type=float, default=300,
                        help='Maximum task polling duration in seconds (default: 300).')
    parser.add_argument('--poll-interval', type=float, default=1)
    args = parser.parse_args(argv)
    if args.timeout <= 0 or args.poll_interval <= 0:
        parser.error('--timeout and --poll-interval must be positive')
    session = requests.Session()
    session.trust_env = False
    space = args.space
    graph = args.graph
    query = 'g.V().limit(12000)'
    record = dict(graph=graph, space=space, query=query, statuses=[],
                  expected_limit_rejection=False)
    accepted = False
    try:
        response = session.post(
            f'{args.hubble_api_url.rstrip("/")}/graphspaces/{space}/graphs/{graph}/gremlin-query/async-task',
            json={'content': query}, timeout=30)
        data = response.json()
        record.update(submission=data, submission_http_status=response.status_code)
        assert response.status_code == 200 and data['status'] == 200, 'Task submission failed'
        task_id = data['data']['task_id']
        record['task_id'] = task_id
        print('submitted', task_id, flush=True)
        accepted = poll_task(
            session,
            f'{args.server_url.rstrip("/")}/graphspaces/{space}/graphs/{graph}/tasks/{task_id}',
            record, args.timeout, args.poll_interval)
    except Exception as error:
        record['validation_error'] = str(error)[:1000]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(record, indent=2) + '\n')
    print(json.dumps(record, indent=2), flush=True)
    return 0 if accepted else 2


if __name__ == '__main__':
    raise SystemExit(main())
