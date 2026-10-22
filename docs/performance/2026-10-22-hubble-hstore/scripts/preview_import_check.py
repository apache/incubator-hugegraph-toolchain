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

"""Check the dedicated preview-import guard using a non-writable marked file."""
import argparse
import hashlib
import json
import pathlib

import requests

GUARD_MESSAGE = 'A truncated preview cannot be imported as full graph data'


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument('--fixture', type=pathlib.Path, required=True,
                        help='Fixture metadata with space, graph, vertex IDs and property SHA256s.')
    parser.add_argument('--hubble-api-url', default='http://127.0.0.1:8088/api/v1.3')
    parser.add_argument('--server-url', default='http://127.0.0.1:18080')
    parser.add_argument('--output', type=pathlib.Path,
                        default=pathlib.Path('hubble-results/preview-import-check.json'))
    args = parser.parse_args(argv)
    fixture = json.loads(args.fixture.read_text())
    session = requests.Session()
    session.trust_env = False
    session.headers['Accept-Language'] = 'en-US'
    base = f"/graphspaces/{fixture['space']}/graphs/{fixture['graph']}"
    record = {'original_vertex_sha256_unchanged': False, 'guard_verified': False}

    def verify_originals():
        assert fixture['vertices'], 'Expected original vertex metadata'
        for vertex in fixture['vertices']:
            response = session.get(args.server_url.rstrip('/') + base +
                                   '/graph/vertices/' + str(vertex['id']), timeout=30)
            response.raise_for_status()
            current = response.json()
            assert str(current['id']) == str(vertex['id']), 'Original vertex identity changed'
            assert vertex['sha256'], 'Expected original property SHA256s'
            for key, digest in vertex['sha256'].items():
                actual = hashlib.sha256(current['properties'][key].encode()).hexdigest()
                assert actual == digest, 'Original property SHA256 changed'

    def upload(payload):
        response = session.post(args.hubble_api_url.rstrip('/') + base + '/import',
                                files={'file': ('preview-check.json', json.dumps(payload).encode(),
                                                'application/json')}, timeout=30)
        return response.status_code, response.json()

    try:
        verify_originals()
        # A valid empty import exercises authentication and the ordinary parser
        # without creating or modifying any graph elements.
        status, data = upload({'vertices': [], 'edges': []})
        record['empty_control'] = {'http_status': status, 'response': data}
        assert status == 200 and data.get('status') == 200, 'Empty import control failed'
        # This is valid ordinary import data even if the preview guard is removed;
        # only the root marker changes the empty control into a rejected preview.
        root_payload = {'properties_truncated': True, 'vertices': [], 'edges': []}
        status, data = upload(root_payload)
        record['root_preview_guard'] = {'http_status': status, 'response': data,
                                        'submitted_file': root_payload}
        assert status == 400 and data.get('status') == 400, 'Root preview guard must reject empty preview data'
        assert data.get('message') == GUARD_MESSAGE, 'Dedicated root preview-import guard was not reached'
        # Deliberately omit id/label so a missing guard cannot write this fixture.
        # Only the dedicated guard message counts, never generic parse/auth errors.
        payload = {'vertices': [{'~hubble_properties_preview': True}], 'edges': []}
        status, data = upload(payload)
        record.update(http_status=status, response=data, submitted_file=payload,
                      contains_vertex_id_or_label=False)
        assert status == 400 and data.get('status') == 400, 'Expected HTTP/application 400'
        assert data.get('message') == GUARD_MESSAGE, 'Dedicated preview-import guard was not reached'
        record['guard_verified'] = True
        verify_originals()
        record['original_vertex_sha256_unchanged'] = True
    except Exception as error:
        record['validation_error'] = str(error)[:1000]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(record, indent=2, ensure_ascii=False) + '\n')
    print(json.dumps(record, indent=2, ensure_ascii=False))
    return 0 if record['guard_verified'] and record['original_vertex_sha256_unchanged'] else 2


if __name__ == '__main__':
    raise SystemExit(main())
