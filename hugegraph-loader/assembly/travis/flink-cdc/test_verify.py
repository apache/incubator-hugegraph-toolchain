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

import gzip
import hashlib
import io
import json
from pathlib import Path
import subprocess
import sys
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

from verify import Acceptance, GRAPHS, normalize, redact, request, set_property, validate_candidate_sdk, validate_jdbc_driver


class AcceptanceHelpersTest(unittest.TestCase):
    def test_identity_api_probe_uses_owned_graph_port_and_isolated_repository(self):
        with tempfile.TemporaryDirectory() as directory:
            args = SimpleNamespace(work_root=directory, evidence=directory + "/evidence",
                                   server_sha="server", toolchain_sha="toolchain", maven_repo=directory + "/m2")
            acceptance = Acceptance(args)
            suite = ET.Element("testsuite", tests="1", failures="0", errors="0", skipped="0")
            ET.SubElement(suite, "testcase", name="testIdentityChangesDeletesAndReplay")
            with patch.object(acceptance, "run", return_value="probe output") as run, \
                    patch("verify.ET.parse", return_value=ET.ElementTree(suite)) as report:
                acceptance.verify_identity_api()
            command = run.call_args.args[0]
            self.assertEqual(command[:3], ["mvn", "-o", "test"])
            self.assertEqual(command[command.index("-pl") + 1], "hugegraph-client,hugegraph-loader")
            self.assertIn("-Dtest=HugeGraphIdentityIntegrationTest", command)
            self.assertIn("-Dmaven.repo.local=" + str(Path(args.maven_repo).resolve()), command)
            self.assertIn("-Dflink.identity.integration=true", command)
            self.assertIn("-Dflink.identity.port=38080", command)
            self.assertIn("-Dflink.identity.graph=" + GRAPHS[0], command)
            self.assertIn(acceptance.prefix + "-identity.xml", str(report.call_args.args[0]))
            self.assertEqual(acceptance.report["phases"]["identity_api_probe"]["counts"]["skipped"], 0)

    def test_identity_api_probe_rejects_skipped_missing_or_failed_test(self):
        with tempfile.TemporaryDirectory() as directory:
            args = SimpleNamespace(work_root=directory, evidence=directory + "/evidence",
                                   server_sha="server", toolchain_sha="toolchain", maven_repo=directory + "/m2")
            acceptance = Acceptance(args)
            for counts, name in (({"tests": "1", "skipped": "1"}, "testIdentityChangesDeletesAndReplay"),
                                 ({"tests": "0"}, "other"),
                                 ({"tests": "1", "failures": "1"}, "testIdentityChangesDeletesAndReplay"),
                                 ({"tests": "1", "errors": "1"}, "testIdentityChangesDeletesAndReplay"),
                                 ({"tests": "1"}, "other")):
                with self.subTest(counts=counts, name=name):
                    suite = ET.Element("testsuite", **counts)
                    ET.SubElement(suite, "testcase", name=name)
                    with patch.object(acceptance, "run", return_value="probe output"), \
                            patch("verify.ET.parse", return_value=ET.ElementTree(suite)), \
                            self.assertRaisesRegex(AssertionError, "without failures or skips"):
                        acceptance.verify_identity_api()
            self.assertNotIn("identity_api_probe", acceptance.report["phases"])

    def test_identity_api_failure_stops_before_starting_flink_mysql(self):
        with tempfile.TemporaryDirectory() as directory:
            archive = Path(directory) / "server.tar.gz"
            archive.write_bytes(b"test-only-archive")
            manifest = Path(directory) / "candidate-sdk.json"
            manifest.write_text(json.dumps({"commit": "server"}))
            args = SimpleNamespace(work_root=directory, evidence=directory + "/evidence",
                                   server_sha="server", toolchain_sha="toolchain", sdk_manifest=str(manifest),
                                   server_archive=str(archive), jdbc_driver="driver",
                                   server_archive_sha256=hashlib.sha256(archive.read_bytes()).hexdigest())
            acceptance = Acceptance(args)
            with patch("verify.validate_jdbc_driver", return_value={}), patch("verify.socket.socket"), \
                    patch.object(acceptance, "start_server") as server, \
                    patch.object(acceptance, "verify_identity_api", side_effect=RuntimeError("API probe failed")), \
                    patch.object(acceptance, "start_flink_mysql") as engine, \
                    self.assertRaisesRegex(RuntimeError, "API probe failed"):
                acceptance.verify()
            server.assert_called_once_with()
            engine.assert_not_called()

    def test_null_removal_is_distinct_from_literal_null_and_stale_value(self):
        source = normalize([{"id": 2, "name": "two", "age": 22, "city": None}])
        empty = normalize([{"id": 2, "properties": {"name": "two", "age": 22}}], graph=True)
        self.assertEqual(source, empty)
        for value in ("null", "old-city"):
            stale = normalize([{"id": 2, "properties": {"name": "two", "age": 22, "city": value}}], graph=True)
            self.assertNotEqual(source, stale)

    def test_duplicate_ids_are_not_hidden_by_dictionary_conversion(self):
        with self.assertRaises(AssertionError):
            normalize([{"id": 1}, {"id": 1}])

    def test_graph_id_type_and_unexpected_properties_fail(self):
        with self.assertRaises(AssertionError):
            normalize([{"id": "1", "properties": {}}], graph=True)
        with self.assertRaises(AssertionError):
            normalize([{"id": 1, "properties": {"extra": "unexpected"}}], graph=True)

    def test_decodes_compressed_graph_api_response(self):
        response = io.BytesIO(gzip.compress(b'{"vertices": []}'))
        response.headers = {"Content-Encoding": "gzip"}
        with patch("verify.urllib.request.urlopen", return_value=response):
            self.assertEqual(request("http://fixture/vertices"), {"vertices": []})

    def test_redacts_credential_fields(self):
        self.assertNotIn("fake-value", redact('password=fake-value token:fake-value "token":"fake-value"'))

    def test_property_replacement_does_not_create_multiple_active_values(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "server.properties"
            path.write_text("#store=example\nstore=old\n")
            set_property(path, "store", "new")
            set_property(path, "port", "38080")
            self.assertEqual(path.read_text().count("store=new"), 1)
            self.assertNotIn("store=old", path.read_text())
            self.assertIn("port=38080", path.read_text())

    def test_rejects_wrong_external_driver_before_runtime_start(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "driver.jar"
            path.write_bytes(b"wrong-driver")
            with self.assertRaisesRegex(AssertionError, "pinned mysql:mysql-connector-java:8.0.28"):
                validate_jdbc_driver(path)

    def test_rejects_sdk_manifest_from_another_server_source(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "candidate-sdk.json"
            selected = "a" * 40
            path.write_text(json.dumps({"commit": "b" * 40}))
            with self.assertRaisesRegex(AssertionError, "selected server source"):
                validate_candidate_sdk(path, selected)
            path.write_text(json.dumps({"commit": selected}))
            self.assertEqual(validate_candidate_sdk(path, selected)["commit"], selected)

    def test_default_mode_does_not_start_or_require_docker(self):
        script = Path(__file__).with_name("verify.py")
        output = subprocess.check_output([sys.executable, str(script)], env={"PATH": ""}, text=True)
        self.assertEqual(json.loads(output)["mode"], "plan")


if __name__ == "__main__":
    unittest.main()
