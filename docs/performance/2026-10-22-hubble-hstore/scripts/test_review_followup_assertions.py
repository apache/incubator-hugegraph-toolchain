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

"""Negative checks for the specific SDK boundary; no live services required."""
import ast
import hashlib
import pathlib
import types
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parents[1] / 'comparison/final-followup/review_followup_assertions.py'
# Load only the pure assertion helper: importing the historical executable would
# otherwise issue live fixture requests and overwrite its output.
helper = next(node for node in ast.parse(SCRIPT.read_text()).body
              if isinstance(node, ast.FunctionDef) and node.name == 'assert_long_key_sdk_error')
namespace = {'hashlib': hashlib}
exec(compile(ast.Module(body=[helper], type_ignores=[]), str(SCRIPT), 'exec'), namespace)
assert_sdk_error = namespace['assert_long_key_sdk_error']


class LongMapKeyRejectionTest(unittest.TestCase):
    def response(self, status=400, size=300):
        return types.SimpleNamespace(status_code=status, content=b'x' * size)

    def payload(self, **overrides):
        value = {'status': 400, 'message': 'Failed to deserialize: {"requestId":"fixture","status":{"code":200},"result":{...}', 'cause': None}
        value.update(overrides)
        return value

    def test_expected_http_and_application_400_sdk_failure_is_accepted(self):
        result = assert_sdk_error(self.response(), self.payload())
        self.assertTrue(result['sdk_deserialization_failed'])
        self.assertEqual(result['http_status'], 400)
        self.assertEqual(result['response_sha256'], hashlib.sha256(b'x' * 300).hexdigest())

    def test_unrelated_errors_wrong_status_or_non_sdk_signatures_fail(self):
        cases = [(401, self.payload(status=401, message='Unauthorized')),
                 (403, self.payload(status=403, message='Forbidden')),
                 (400, self.payload(message='Schema lookup failed')),
                 (500, self.payload(status=500, message='Server unavailable')),
                 (500, self.payload()), (400, self.payload(status=500)),
                 (200, self.payload()), (400, self.payload(message='')),
                 (400, self.payload(message='Unexpected Failed to deserialize: {}')),
                 (400, self.payload(message='Failed to deserialize')),
                 (400, self.payload(message='Use asynchronous mode for an unbounded aggregate'))]
        for status, payload in cases:
            with self.subTest(status=status, payload=payload):
                with self.assertRaises(AssertionError):
                    assert_sdk_error(self.response(status), payload)

    def test_size_and_cause_bounds_remain_required(self):
        for response, payload in [(self.response(), self.payload(message='Failed to deserialize: {' + 'x' * 2048)),
                                  (self.response(size=8192), self.payload()),
                                  (self.response(), self.payload(cause='internal detail'))]:
            with self.subTest(response=response, payload=payload):
                with self.assertRaises(AssertionError):
                    assert_sdk_error(response, payload)


if __name__ == '__main__':
    unittest.main()
