#!/usr/bin/env python3
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import csv
import importlib.util
import io
import json
import os
import subprocess
import sys
import tarfile
import tempfile
import unittest
from pathlib import Path
from unittest import mock


SCRIPT = Path(__file__).with_name("run_live_hubble_smoke.py")
SPEC = importlib.util.spec_from_file_location("run_live_hubble_smoke", SCRIPT)
SMOKE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SMOKE)


class UrlValidationTest(unittest.TestCase):

    SENTINEL = "URL_INPUT_SECRET_7aQ"
    ENCODED = "".join(f"%{ord(char):02x}" for char in SENTINEL)

    @classmethod
    def invalid_urls(cls):
        return [
            f"https://user:{cls.ENCODED}@example.invalid/prefix",
            f"https://{cls.ENCODED}@example.invalid/prefix",
            f"https://user%40{cls.ENCODED}/prefix",
            f"https://example.invalid/prefix?token={cls.ENCODED}",
            f"https://example.invalid/prefix#{cls.ENCODED}",
            f"https://example.invalid/prefix\n{cls.SENTINEL}",
            f"file:///tmp/{cls.SENTINEL}"
        ]

    def assert_no_secret(self, text):
        self.assertNotIn(self.SENTINEL, text)
        self.assertNotIn(self.ENCODED, text)

    @staticmethod
    def guarded_environment(root):
        guard = root / "guards"
        guard.mkdir()
        marker = root / "forbidden-action.txt"
        (guard / "sitecustomize.py").write_text(
            "import os, pathlib, subprocess, tempfile, urllib.request\n"
            "def forbidden(*args, **kwargs):\n"
            "    pathlib.Path(os.environ['TEST_ACTION_MARKER']).write_text('attempted')\n"
            "    raise RuntimeError('Forbidden runtime action')\n"
            "urllib.request.OpenerDirector.open = forbidden\n"
            "subprocess.Popen = forbidden\n"
            "tempfile.mkdtemp = forbidden\n", encoding="utf-8")
        env = {**os.environ, "PYTHONPATH": str(guard), "PYTHONDONTWRITEBYTECODE": "1",
               "TEST_ACTION_MARKER": str(marker), "HUBBLE_URL": "http://127.0.0.1:8088",
               "HUBBLE_SMOKE_SERVER_URL": "http://127.0.0.1:8080"}
        return env, marker

    def test_public_urls_preserve_origins_and_deployment_prefixes(self):
        for url in ("http://127.0.0.1:8088", "https://example.invalid/deployment/hubble/",
                    "http://[::1]:8080/prefix%20name"):
            self.assertEqual(url, SMOKE.public_http_url(url, "hubbleUrl"))

    def test_invalid_url_diagnostic_is_only_the_field_name(self):
        urls = self.invalid_urls() + ["https://@example.invalid/", "https://example.invalid/?",
                                      "https://example.invalid/#", "https:example.invalid",
                                      "https://example.invalid:bad/", "https://[broken"]
        for index, url in enumerate(urls):
            with self.subTest(case=index):
                with self.assertRaises(ValueError) as error:
                    SMOKE.public_http_url(url, "serverUrl")
                self.assertEqual("Invalid serverUrl", str(error.exception))
                self.assert_no_secret(str(error.exception))

    def test_main_validates_before_runtime_report_http_or_archive_setup(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "existing.json"
            output.write_text('{"status":"passed"}', encoding="utf-8")
            with mock.patch("sys.argv", [str(SCRIPT), "unused.tar.gz", "--hubble-url", self.invalid_urls()[0],
                                         "--json-output", str(output)]), \
                    mock.patch.object(SMOKE, "require_safe_archive_extraction") as archive, \
                    mock.patch.object(SMOKE, "request") as request, \
                    mock.patch.object(SMOKE, "is_healthy") as health, \
                    mock.patch.object(SMOKE.tempfile, "mkdtemp") as runtime, \
                    mock.patch.object(SMOKE.subprocess, "Popen") as start, \
                    mock.patch("sys.stderr", new_callable=io.StringIO) as stderr:
                with self.assertRaises(SystemExit) as error:
                    SMOKE.main()
            self.assertEqual(2, error.exception.code)
            self.assertEqual("Invalid hubbleUrl\n", stderr.getvalue())
            for action in (archive, request, health, runtime, start):
                action.assert_not_called()
            report = json.loads(output.read_text())
            self.assertEqual("failed", report["status"])
            self.assertEqual("url-validation", report["stage"])
            self.assert_no_secret(output.read_text())

    def test_independent_python_cli_rejects_secrets_before_any_runtime_action(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            env, marker = self.guarded_environment(root)
            for field in ("hubble", "server"):
                for index, url in enumerate(self.invalid_urls()):
                    with self.subTest(field=field, case=index):
                        output = root / f"{field}-{index}.json"
                        output.write_text('{"status":"passed"}', encoding="utf-8")
                        result = subprocess.run([sys.executable, str(SCRIPT), "unused.tar.gz",
                                                 f"--{field}-url", url, "--json-output", str(output)],
                                                env=env, capture_output=True, text=True, check=False)
                        self.assertEqual(2, result.returncode)
                        self.assertEqual("", result.stdout)
                        self.assertEqual(f"Invalid {field}Url\n", result.stderr)
                        report = json.loads(output.read_text())
                        self.assertEqual("failed", report["status"])
                        self.assertNotIn("hubble_url", report)
                        self.assertNotIn("server_url", report)
                        for text in (result.stdout, result.stderr, output.read_text()):
                            self.assert_no_secret(text)
                        self.assertFalse(marker.exists())

    def test_invalid_cli_atomically_replaces_a_real_read_only_old_report(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            env, marker = self.guarded_environment(root)
            output = root / "read-only.json"
            output.write_text('{"status":"passed"}', encoding="utf-8")
            output.chmod(0o444)
            old_inode = output.stat().st_ino
            result = subprocess.run([sys.executable, str(SCRIPT), "unused.tar.gz", "--hubble-url",
                                     self.invalid_urls()[0], "--json-output", str(output)],
                                    env=env, capture_output=True, text=True, check=False)
            self.assertEqual(2, result.returncode)
            self.assertEqual("", result.stdout)
            self.assertEqual("Invalid hubbleUrl\n", result.stderr)
            self.assertEqual("failed", json.loads(output.read_text())["status"])
            self.assertNotEqual(old_inode, output.stat().st_ino)
            self.assertEqual([], list(root.glob(".read-only.json.*.tmp")))
            self.assertFalse(marker.exists())
            self.assert_no_secret(result.stderr + output.read_text())

    def test_real_os_write_failure_removes_stale_success_and_preserves_exit_two(self):
        try:
            import resource
            import signal
        except ImportError:
            self.skipTest("POSIX resource limits are required for the OS write-failure case")

        def forbid_file_growth():
            signal.signal(signal.SIGXFSZ, signal.SIG_IGN)
            resource.setrlimit(resource.RLIMIT_FSIZE, (0, 0))

        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            env, marker = self.guarded_environment(root)
            evidence = root / self.SENTINEL
            evidence.mkdir()
            output = evidence / "read-only.json"
            output.write_text('{"status":"passed"}', encoding="utf-8")
            output.chmod(0o444)
            result = subprocess.run([sys.executable, str(SCRIPT), "unused.tar.gz", "--hubble-url",
                                     self.invalid_urls()[0], "--json-output", str(output)],
                                    env=env, preexec_fn=forbid_file_growth,
                                    capture_output=True, text=True, check=False)
            self.assertEqual(2, result.returncode)
            self.assertEqual("", result.stdout)
            self.assertEqual("Invalid hubbleUrl\nURL validation failure evidence could not be saved\n", result.stderr)
            self.assertFalse(output.exists())
            self.assertEqual([], list(evidence.iterdir()))
            self.assertFalse(marker.exists())
            self.assert_no_secret(result.stdout + result.stderr)

    def test_unwritable_destination_reports_a_fixed_diagnostic_without_echoing_path(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            env, marker = self.guarded_environment(root)
            blocker = root / self.SENTINEL
            blocker.write_text("not a directory", encoding="utf-8")
            result = subprocess.run([sys.executable, str(SCRIPT), "unused.tar.gz", "--server-url",
                                     self.invalid_urls()[0], "--json-output", str(blocker / "report.json")],
                                    env=env, capture_output=True, text=True, check=False)
            self.assertEqual(2, result.returncode)
            self.assertEqual("", result.stdout)
            self.assertEqual("Invalid serverUrl\nURL validation failure evidence could not be saved\n", result.stderr)
            self.assert_no_secret(result.stdout + result.stderr)
            self.assertFalse(marker.exists())

    def test_valid_preflight_preserves_existing_evidence_without_runtime_actions(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            env, marker = self.guarded_environment(root)
            env.update(HUBBLE_URL="https://example.invalid/prefix/hubble/",
                       HUBBLE_SMOKE_SERVER_URL="https://example.invalid/prefix/server/")
            output = root / "existing.json"
            original = '{"status":"passed","historical":true}'
            output.write_text(original, encoding="utf-8")
            result = subprocess.run([sys.executable, str(SCRIPT), "unused.tar.gz", "--validate-urls",
                                     "--json-output", str(output)], env=env, capture_output=True,
                                    text=True, check=False)
            self.assertEqual(0, result.returncode)
            self.assertEqual("", result.stdout + result.stderr)
            self.assertEqual(original, output.read_text())
            self.assertFalse(marker.exists())

    def test_shell_preflight_rejects_secrets_and_invalidates_old_success_before_startup(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            env, marker = self.guarded_environment(root)
            tarball = root / "candidate.tar.gz"
            tarball.write_bytes(b"not used by preflight")
            runtime = root / "runtime"
            runtime.mkdir()
            node = root / "forbidden-node"
            node.write_text("#!/bin/sh\nprintf 'attempted' > \"$TEST_ACTION_MARKER\"\nexit 91\n", encoding="utf-8")
            node.chmod(0o755)
            env.update(PYTHON=sys.executable, NODE=str(node), TMPDIR=str(runtime))
            wrapper = SCRIPT.with_name("verify-hubble-issue-694.sh")
            for field in ("hubble", "server"):
                for index, url in enumerate(self.invalid_urls()):
                    with self.subTest(field=field, case=index):
                        evidence = root / f"evidence-{field}-{index}"
                        evidence.mkdir()
                        manifest = evidence / "manifest.json"
                        manifest.write_text('{"status":"passed"}', encoding="utf-8")
                        manifest.chmod(0o444)
                        (evidence / "live-hubble-smoke.json").write_text('{"status":"passed"}', encoding="utf-8")
                        invocation_env = {**env, "HUBBLE_694_EVIDENCE_DIR": str(evidence)}
                        server = "http://127.0.0.1:8080"
                        if field == "hubble":
                            invocation_env["HUBBLE_URL"] = url
                        else:
                            server = url
                        result = subprocess.run(["bash", str(wrapper), str(tarball), server],
                                                env=invocation_env, capture_output=True, text=True, check=False)
                        self.assertEqual(2, result.returncode)
                        self.assertEqual("", result.stdout)
                        self.assertEqual(f"Invalid {field}Url\n", result.stderr)
                        report = json.loads(manifest.read_text())
                        self.assertEqual("failed", report["status"])
                        self.assertEqual("url-validation", report["stage"])
                        self.assertEqual([], list(runtime.iterdir()))
                        self.assertFalse((evidence / "ui").exists())
                        self.assertFalse((evidence / "logs").exists())
                        self.assertFalse(marker.exists())
                        for text in [result.stdout, result.stderr] + [p.read_text() for p in evidence.glob("*.json")]:
                            self.assert_no_secret(text)


class SchemaWriteEvidenceTest(unittest.TestCase):

    def test_permission_evidence_distinguishes_created_from_conflict(self):
        for conflicted in (False, True):
            with self.subTest(conflicted=conflicted):
                response = None if conflicted else {"status": 200, "data": {}}
                with mock.patch.object(SMOKE, "ignore_conflict", return_value=response), \
                        mock.patch.object(SMOKE, "server_json", return_value={"name": "proof_person"}):
                    checks, _ = SMOKE.create_schema("http://hubble", "http://server", "DEFAULT", "g", "proof")
                by_name = {check["name"]: check for check in checks}
                self.assertEqual(0 if conflicted else 3, by_name["hubble-schema-propertykeys"]["created_count"])
                self.assertEqual(not conflicted, by_name["hubble-schema-vertexlabel"]["created"])
                self.assertEqual(not conflicted, by_name["hubble-schema-edgelabel"]["created"])

    def test_rejected_business_write_does_not_publish_created_evidence(self):
        with mock.patch.object(SMOKE, "ignore_conflict", return_value={"status": 403, "message": "denied"}):
            with self.assertRaises(RuntimeError):
                SMOKE.create_schema("http://hubble", "http://server", "DEFAULT", "g", "proof")


class ArchiveExtractionTest(unittest.TestCase):

    def test_missing_data_filter_fails_before_opening_archive(self):
        with mock.patch.object(tarfile, "data_filter", None, create=True), \
                mock.patch.object(tarfile, "open") as open_archive:
            with self.assertRaisesRegex(RuntimeError, "tarfile.data_filter"):
                SMOKE.extract_archive(Path("hubble.tar.gz"), Path("runtime"))
            open_archive.assert_not_called()

    def test_cli_rejects_unsupported_python_before_runtime_setup(self):
        with mock.patch.object(tarfile, "data_filter", None, create=True), \
                mock.patch("sys.argv", [str(SCRIPT), "hubble.tar.gz"]), \
                mock.patch.object(SMOKE, "is_healthy") as is_healthy, \
                mock.patch.object(tempfile, "mkdtemp") as create_work_dir, \
                mock.patch("sys.stderr", new_callable=io.StringIO) as stderr:
            with self.assertRaises(SystemExit) as error:
                SMOKE.main()
            self.assertEqual(2, error.exception.code)
            self.assertIn("upgrade to the latest security patch", stderr.getvalue())
            is_healthy.assert_not_called()
            create_work_dir.assert_not_called()

    def test_sequential_symlink_escape_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            work = root / "runtime"
            work.mkdir()
            tarball = root / "escape.tar.gz"
            with tarfile.open(tarball, "w:gz") as archive:
                for name, target in (("pivot", "."), ("pivot/escape", "..")):
                    member = tarfile.TarInfo(name)
                    member.type = tarfile.SYMTYPE
                    member.linkname = target
                    archive.addfile(member)
                member = tarfile.TarInfo("pivot/escape/escaped")
                member.size = 7
                archive.addfile(member, io.BytesIO(b"outside"))
            # Each entry passes the old preflight before any links exist.
            with tarfile.open(tarball) as archive:
                for member in archive.getmembers():
                    SMOKE.assert_safe_tar_member(member, work)

            with self.assertRaises(tarfile.FilterError):
                SMOKE.extract_archive(tarball, work)
            self.assertFalse((root / "escaped").exists())

    def test_internal_symbolic_and_hard_links_are_preserved(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            work = root / "runtime"
            work.mkdir()
            tarball = root / "hubble.tar.gz"
            home_name = "apache-hugegraph-hubble-fixture"
            with tarfile.open(tarball, "w:gz") as archive:
                member = tarfile.TarInfo(f"{home_name}/lib/payload")
                member.size = 7
                archive.addfile(member, io.BytesIO(b"fixture"))
                for name, target, kind in (
                        ("current", "lib", tarfile.SYMTYPE),
                        ("copy", f"{home_name}/lib/payload", tarfile.LNKTYPE)):
                    member = tarfile.TarInfo(f"{home_name}/{name}")
                    member.type = kind
                    member.linkname = target
                    archive.addfile(member)

            home = SMOKE.extract_tarball(tarball, work)
            self.assertEqual(work / home_name, home)
            self.assertTrue((home / "current").is_symlink())
            self.assertEqual(b"fixture", (home / "current/payload").read_bytes())
            self.assertEqual(b"fixture", (home / "copy").read_bytes())


class HubbleReadinessTest(unittest.TestCase):

    def test_configure_endpoint_uses_registered_server_options(self):
        with tempfile.TemporaryDirectory() as directory:
            home = Path(directory)
            conf_dir = home / "conf"
            conf_dir.mkdir()
            conf = conf_dir / "hugegraph-hubble.properties"
            conf.write_text(
                "server.host=localhost\n"
                "server.port=8088\n"
                "pd.enabled=true\n",
                encoding="utf-8"
            )

            SMOKE.configure_hubble_endpoint(
                home,
                "http://127.0.0.1:19088",
                "0.0.0.0",
                "http://127.0.0.1:18080"
            )

            configured = conf.read_text(encoding="utf-8")
            self.assertIn("server.host=0.0.0.0", configured)
            self.assertIn("server.port=19088", configured)
            self.assertNotIn("hubble.host=", configured)
            self.assertNotIn("hubble.port=", configured)
            self.assertNotIn("server.address=", configured)

    def test_readiness_rejects_spa_html(self):
        self.assertFalse(SMOKE.is_hubble_readiness_response(
            '<div id="root"></div>'))

    def test_readiness_requires_about_response_shape(self):
        self.assertFalse(SMOKE.is_hubble_readiness_response(
            {"status": 200, "data": {"name": "hugegraph-hubble"}}))
        self.assertFalse(SMOKE.is_hubble_readiness_response({
            "status": 200,
            "data": {"name": "HugeGraph-Hubble", "version": "3.0.0"}
        }))
        self.assertTrue(SMOKE.is_hubble_readiness_response({
            "status": 200,
            "data": {"name": "hugegraph-hubble", "version": "3.0.0"}
        }))

    @mock.patch.object(SMOKE, "request")
    def test_preflight_uses_strict_about_api(self, request):
        request.return_value = '<div id="root"></div>'

        self.assertFalse(SMOKE.is_healthy("http://127.0.0.1:8088"))
        request.assert_called_once_with(
            "GET", "http://127.0.0.1:8088/about", timeout=2)


class LoginIdentityEvidenceTest(unittest.TestCase):

    def test_identity_mismatch_does_not_disclose_login_response_token(self):
        token = "LOGIN_RESPONSE_TOKEN_SENTINEL_9x"
        with mock.patch.object(SMOKE, "request", return_value={
                "status": 200, "data": {"user_name": "wrong-account", "token": token}}) as request:
            with self.assertRaisesRegex(RuntimeError, "identity does not match") as error:
                SMOKE.run_login_checks("http://hubble", "requested", "test-secret")
        self.assertNotIn(token, str(error.exception))
        self.assertNotIn("wrong-account", str(error.exception))
        request.assert_called_once()


class BoundaryStatusEvidenceTest(unittest.TestCase):

    def test_hubble_success_requires_integer_envelope_status(self):
        for response in ["<html>SPA</html>", [], None, {"vertices": []}, {},
                         {"status": None}, {"status": "200"}, {"status": True}]:
            with self.subTest(response=response), mock.patch.object(SMOKE, "request", return_value=response):
                with self.assertRaisesRegex(RuntimeError, "lacks an integer status"):
                    SMOKE.get_json_status("GET", "http://hubble/api/v1.3/cypher")
        for status in (200, 400, 503):
            with mock.patch.object(SMOKE, "request", return_value={"status": status, "data": {}}):
                self.assertEqual(status, SMOKE.get_json_status("GET", "http://hubble/api/v1.3/cypher"))

    def test_malformed_success_payload_fails_without_echoing_payload(self):
        sentinel = "BOUNDARY_RESPONSE_SECRET_7x"
        for error in (SMOKE.json.JSONDecodeError("bad JSON", sentinel, 0),
                      UnicodeDecodeError("utf8", sentinel.encode(), 0, 1, "invalid")):
            with mock.patch.object(SMOKE, "request", side_effect=error):
                with self.assertRaisesRegex(RuntimeError, "not valid JSON") as failure:
                    SMOKE.get_json_status("GET", "http://hubble/api/v1.3/cypher")
            self.assertNotIn(sentinel, str(failure.exception))

    def test_transport_failure_cannot_be_promoted_by_body_to_success(self):
        for code, body in ((503, b'{"status":200}'), (400, b'legacy error'), (401, b'[]')):
            error = SMOKE.urllib.error.HTTPError("http://hubble", code, "test", {}, io.BytesIO(body))
            with mock.patch.object(SMOKE, "request", side_effect=error):
                self.assertEqual(code, SMOKE.get_json_status("GET", "http://hubble/api/v1.3/cypher"))

    def test_direct_server_raw_json_is_not_required_to_have_hubble_envelope(self):
        raw = {"versions": {"version": "1.7.0"}}
        with mock.patch.object(SMOKE, "request", return_value=raw) as request:
            self.assertIs(raw, SMOKE.server_json("GET", "http://server", "/versions"))
        self.assertEqual(("GET", "http://server/versions", None), request.call_args.args)


class AnalysisBoundaryTest(unittest.TestCase):

    @mock.patch.object(SMOKE, "get_json_status", side_effect=[400, 400])
    def test_environment_dependent_boundaries_are_skipped(self, _status):
        checks = SMOKE.run_analysis_boundary_checks(
            "http://127.0.0.1:8088", "DEFAULT", "hugegraph")

        self.assertEqual(["skipped", "skipped"],
                         [check["status"] for check in checks])
        self.assertEqual([400, 400],
                         [check["http_or_business_status"]
                          for check in checks])

    @mock.patch.object(SMOKE, "get_json_status", side_effect=[200, 503])
    def test_service_failure_is_not_skipped(self, _status):
        with self.assertRaisesRegex(RuntimeError,
                                    "analysis-olap-boundary failed: status 503"):
            SMOKE.run_analysis_boundary_checks(
                "http://127.0.0.1:8088", "DEFAULT", "hugegraph")

    @mock.patch.object(SMOKE, "get_json_status", return_value=200)
    def test_available_boundaries_pass(self, _status):
        checks = SMOKE.run_analysis_boundary_checks(
            "http://127.0.0.1:8088", "DEFAULT", "hugegraph")

        self.assertEqual(["passed", "passed"],
                         [check["status"] for check in checks])


class LoaderFixtureDataTest(unittest.TestCase):

    def test_repeated_vertex_writes_have_identical_properties(self):
        schema = {"vl_person": "smoke_person", "el_knows": "smoke_knows",
                  "pk_name": "smoke_name", "pk_rank": "smoke_rank"}
        mappings = []
        rows = []

        def multipart(_fields, _file_field, _file_name, content):
            rows.extend(csv.DictReader(io.StringIO(content.decode())))
            return b"multipart", "multipart/form-data"

        def request(method, url, body=None, headers=None):
            if url.endswith("/job-manager"):
                data = {"id": 1}
            elif "/upload-file/token?" in url:
                data = {"smoke_edges.csv": "token"}
            elif url.endswith("/upload-file"):
                data = {"id": 2}
            elif url.endswith("/file-setting"):
                data = {"file_setting": {"column_names": list(rows[0])}}
            else:
                data = {}
            if url.endswith("/vertex-mappings"):
                mappings.append(body)
            return {"status": 200, "data": data}

        with mock.patch.object(SMOKE, "request", side_effect=request), \
                mock.patch.object(SMOKE, "encode_multipart", side_effect=multipart):
            SMOKE.upload_csv("http://hubble", "DEFAULT", "graph", "smoke", schema)
            SMOKE.configure_mapping("http://hubble", "DEFAULT", "graph", 1, 2, schema)

        vertices = {}
        for mapping in mappings:
            for row in rows:
                vertex_id = row[mapping["id_fields"][0]]
                properties = {m["mapped_name"]: row[m["column_name"]] for m in mapping["field_mapping"]}
                if vertex_id in vertices:
                    self.assertEqual(vertices[vertex_id], properties, vertex_id)
                vertices[vertex_id] = properties
        self.assertEqual({f"smoke_{name.lower()}": {"smoke_name": name, "smoke_rank": str(rank)}
                          for rank, name in enumerate(("Alice", "Bob", "Carol"), 1)}, vertices)


class QuerySemanticsTest(unittest.TestCase):

    def setUp(self):
        self.schema = {"vl_person": "smoke_person", "el_knows": "smoke_knows",
                       "pk_name": "smoke_name", "pk_rank": "smoke_rank"}
        self.queries = []
        self.cypher_enabled = True
        self.vertices = [
            {"id": "smoke_alice", "label": "smoke_person",
             "properties": {"smoke_name": "Alice", "smoke_rank": 1}},
            {"id": "smoke_bob", "label": "smoke_person",
             "properties": {"smoke_name": "Bob", "smoke_rank": 2}},
            {"id": "smoke_carol", "label": "smoke_person", "properties": {}}]
        self.edges = [{"id": "edge1", "label": "smoke_knows", "source": "smoke_alice", "target": "smoke_bob"},
                      {"id": "edge2", "label": "smoke_knows", "source": "smoke_bob", "target": "smoke_carol"}]

    def request(self, method, url, body=None):
        if url.endswith("/config"):
            data = {"server_capabilities_verified": True, "cypher_enabled": self.cypher_enabled}
        elif url.endswith("/gremlin-query"):
            self.assertEqual("POST", method)
            count = 3 if "g.V()" in body["content"] else 2
            data = {"table_view": {"rows": [{"result": count}]}}
        elif "/cypher?" in url:
            self.assertEqual("GET", method)
            query = SMOKE.urllib.parse.parse_qs(SMOKE.urllib.parse.urlsplit(url).query)["cypher"][0]
            self.queries.append(query)
            if query == "RETURN 1 AS value":
                data = {"json_view": {"data": [{"value": 1}]},
                        "table_view": {"rows": [{"result": {"value": 1}}]}}
            else:
                vertex = query.endswith("RETURN n")
                elements = self.vertices if vertex else self.edges
                data = {"type": "VERTEX" if vertex else "EDGE",
                        "json_view": {"data": elements}, "table_view": {"rows": elements},
                        "graph_view": {"vertices": self.vertices, "edges": self.edges}}
        elif "/execute-histories?" in url:
            data = {"records": [{"content": q, "status": "SUCCESS"} for q in self.queries]}
        else:
            self.fail(f"Unexpected query request: {method} {url}")
        return {"status": 200, "data": data}

    def run_checks(self, request=None):
        with mock.patch.object(SMOKE, "request", side_effect=request or self.request):
            return SMOKE.run_hubble_query_checks("http://hubble", "http://server", "DEFAULT", "graph",
                                                 self.schema, "smoke")

    def test_query_routes_validate_all_views_and_history(self):
        checks = self.run_checks()
        self.assertEqual(6, len(checks))
        self.assertTrue(all(c["status"] == "passed" for c in checks))
        self.assertEqual(["RETURN 1 AS value", "MATCH (n:`smoke_person`) RETURN n",
                          "MATCH ()-[e:`smoke_knows`]->() RETURN e"], self.queries)

    def test_disabled_server_skips_only_cypher(self):
        self.cypher_enabled = False
        versions = {"versions": {"core": "1.7.0"}}
        with mock.patch.object(SMOKE, "server_json", return_value=versions) as server_json:
            checks = self.run_checks()
        server_json.assert_called_once_with("GET", "http://server", "/versions")
        self.assertEqual(["passed", "passed", "skipped"], [c["status"] for c in checks])
        self.assertEqual(versions, checks[-1]["server_versions"])
        self.assertEqual([], self.queries)

    def test_enabled_cypher_failure_is_not_skipped(self):
        def request(method, url, body=None):
            if "/cypher?" in url:
                raise RuntimeError("Server query failed")
            return self.request(method, url, body)
        with self.assertRaisesRegex(RuntimeError, "Server query failed"):
            self.run_checks(request)

    def test_unverified_capability_is_not_skipped(self):
        for capabilities in ({"server_capabilities_verified": False, "cypher_enabled": False},
                             {"server_capabilities_verified": True}):
            with self.subTest(capabilities=capabilities):
                def request(method, url, body=None):
                    if url.endswith("/config"):
                        return {"status": 200, "data": capabilities}
                    return self.request(method, url, body)
                with self.assertRaisesRegex(RuntimeError, "unverified"):
                    self.run_checks(request)

    def test_failed_history_is_not_accepted(self):
        def request(method, url, body=None):
            response = self.request(method, url, body)
            if "/execute-histories?" in url:
                response["data"]["records"][0]["status"] = "FAILED"
            return response
        with self.assertRaisesRegex(RuntimeError, "execution history mismatch"):
            self.run_checks(request)

    def test_wrong_gremlin_count_fails(self):
        with mock.patch.object(SMOKE, "run_hubble_gremlin", return_value={"table_view": {"rows": [0]}}):
            with self.assertRaisesRegex(RuntimeError, "Gremlin vertices count"):
                self.run_checks()

    def test_missing_graph_conversion_fails(self):
        result = {"type": "VERTEX", "json_view": {"data": self.vertices},
                  "table_view": {"rows": self.vertices}, "graph_view": {"vertices": []}}
        with self.assertRaisesRegex(RuntimeError, "missing graph view"):
            SMOKE.check_cypher_graph_views(result, self.schema, "smoke", "VERTEX")

    def test_duplicate_graph_rows_fail(self):
        for kind, elements, resource in [("VERTEX", self.vertices, "vertices"),
                                          ("EDGE", self.edges, "edges")]:
            for section, key in [("json_view", "data"), ("table_view", "rows"), ("graph_view", resource)]:
                with self.subTest(kind=kind, view=section):
                    result = {"type": kind, "json_view": {"data": list(elements)},
                              "table_view": {"rows": list(elements)},
                              "graph_view": {resource: list(elements)}}
                    result[section][key].append(elements[0])
                    with self.assertRaisesRegex(RuntimeError, "count mismatch"):
                        SMOKE.check_cypher_graph_views(result, self.schema, "smoke", kind)

    def test_missing_complementary_graph_elements_fail(self):
        for kind, elements, complementary in [("VERTEX", self.vertices, "edges"),
                                               ("EDGE", self.edges, "vertices")]:
            with self.subTest(kind=kind):
                result = {"type": kind, "json_view": {"data": elements},
                          "table_view": {"rows": elements},
                          "graph_view": {"vertices": self.vertices, "edges": self.edges}}
                result["graph_view"][complementary] = []
                with self.assertRaisesRegex(RuntimeError, "graph enrichment"):
                    SMOKE.check_cypher_graph_views(result, self.schema, "smoke", kind)

    def test_loader_flow_invokes_hubble_query_checks(self):
        shortest_path = {"path": ["smoke_alice", "smoke_bob", "smoke_carol"]}
        with mock.patch.object(SMOKE, "create_schema", return_value=([], self.schema)), \
                mock.patch.object(SMOKE, "upload_csv", return_value=(1, 2)), \
                mock.patch.object(SMOKE, "configure_mapping"), \
                mock.patch.object(SMOKE, "wait_for_load_task", return_value=(3, {"status": "SUCCEED"}, {})), \
                mock.patch.object(SMOKE, "run_server_label_count", side_effect=[3, 2]), \
                mock.patch.object(SMOKE, "server_json", return_value=shortest_path), \
                mock.patch.object(SMOKE, "run_hubble_query_checks", return_value=[]) as query_checks:
            SMOKE.run_live_function_flow("http://hubble", "http://server", "DEFAULT", "graph", "smoke")
        query_checks.assert_called_once_with("http://hubble", "http://server", "DEFAULT", "graph",
                                             self.schema, "smoke")


if __name__ == "__main__":
    unittest.main()
