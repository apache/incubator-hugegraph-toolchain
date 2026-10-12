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

import importlib.util
import io
import tarfile
import tempfile
import unittest
from pathlib import Path
from unittest import mock


SCRIPT = Path(__file__).with_name("run_live_hubble_smoke.py")
SPEC = importlib.util.spec_from_file_location("run_live_hubble_smoke", SCRIPT)
SMOKE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SMOKE)


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


if __name__ == "__main__":
    unittest.main()
