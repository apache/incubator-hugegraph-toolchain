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
