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

import argparse
import base64
import gzip
import http.cookiejar
import json
import os
import re
import shutil
import subprocess
import tarfile
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path


COOKIE_JAR = http.cookiejar.CookieJar()
OPENER = urllib.request.build_opener(
    urllib.request.HTTPCookieProcessor(COOKIE_JAR)
)
SERVER_TOKEN = None


def request(method, url, body=None, headers=None, timeout=10):
    data = None
    merged_headers = {"Accept-Encoding": "identity", **(headers or {})}
    if body is not None:
        if isinstance(body, bytes):
            data = body
        else:
            data = json.dumps(body).encode("utf-8")
            merged_headers = {"Content-Type": "application/json",
                              **merged_headers}
    req = urllib.request.Request(url, data=data, headers=merged_headers,
                                 method=method)
    with OPENER.open(req, timeout=timeout) as response:
        raw_payload = response.read()
        if response.headers.get("Content-Encoding") == "gzip":
            raw_payload = gzip.decompress(raw_payload)
        payload = raw_payload.decode("utf-8")
        content_type = response.headers.get("Content-Type", "")
    if ("application/json" in content_type or "+json" in content_type) and payload:
        return json.loads(payload)
    return payload


def unwrap(response, name):
    if not isinstance(response, dict) or response.get("status") != 200:
        raise RuntimeError(f"{name} failed: {response}")
    return response.get("data")


def server_json(method, server_url, path, body=None, timeout=10):
    headers = {}
    if SERVER_TOKEN:
        headers["Authorization"] = f"Bearer {SERVER_TOKEN}"
    return request(method, f"{server_url}{path}", body, headers=headers,
                   timeout=timeout)


def is_hubble_readiness_response(response):
    if not isinstance(response, dict) or response.get("status") != 200:
        return False
    data = response.get("data")
    return (isinstance(data, dict) and
            data.get("name") == "hugegraph-hubble" and
            isinstance(data.get("version"), str) and bool(data["version"]))


def wait_for_health(hubble_url, deadline_seconds):
    deadline = time.time() + deadline_seconds
    last_error = None
    while time.time() < deadline:
        try:
            response = request("GET", f"{hubble_url}/about", timeout=3)
            if is_hubble_readiness_response(response):
                return response
            last_error = RuntimeError(f"Unexpected /about response: {response}")
        except Exception as exc:  # noqa: BLE001 - smoke tool should report final error
            last_error = exc
        time.sleep(1)
    raise RuntimeError(f"Hubble health did not become UP: {last_error}")


def is_healthy(hubble_url):
    try:
        response = request("GET", f"{hubble_url}/about", timeout=2)
        return is_hubble_readiness_response(response)
    except Exception:  # noqa: BLE001 - preflight only needs a boolean
        return False


def assert_safe_tar_member(member, work_dir):
    target = (work_dir / member.name).resolve()
    root = work_dir.resolve()
    if target != root and root not in target.parents:
        raise RuntimeError(f"Unsafe tar entry outside work dir: {member.name}")
    if member.islnk() or member.issym():
        link_target = (target.parent / member.linkname).resolve()
        if link_target != root and root not in link_target.parents:
            raise RuntimeError(f"Unsafe tar link outside work dir: {member.name}")


def require_safe_archive_extraction():
    if not callable(getattr(tarfile, "data_filter", None)):
        raise RuntimeError(
            "Safe archive extraction requires Python with tarfile.data_filter; "
            "upgrade to the latest security patch of Python 3.10 or newer "
            "(extraction filters first appeared in 3.9.17, 3.10.12 and 3.11.4). "
            "Unfiltered extraction is not supported."
        )


def extract_archive(tarball, work_dir):
    require_safe_archive_extraction()
    with tarfile.open(tarball) as archive:
        members = archive.getmembers()
        for member in members:
            assert_safe_tar_member(member, work_dir)
        # Recheck each member against links already written during extraction.
        archive.extractall(work_dir, members, filter="data")


def extract_tarball(tarball, work_dir):
    extract_archive(tarball, work_dir)
    homes = [path for path in work_dir.iterdir()
             if path.is_dir() and path.name.startswith("apache-hugegraph-hubble-")]
    if not homes:
        raise RuntimeError(f"Unable to find Hubble home under {work_dir}")
    return homes[0]


def configure_hubble_endpoint(hubble_home, hubble_url, bind_host, server_url):
    parsed = urllib.parse.urlparse(hubble_url)
    host = bind_host or parsed.hostname
    port = parsed.port
    conf = hubble_home / "conf" / "hugegraph-hubble.properties"
    text = conf.read_text(encoding="utf-8")
    lines = []
    replaced_host = False
    replaced_port = False
    replaced_pd_enabled = False
    replaced_direct_url = False
    for line in text.splitlines():
        if host and line.startswith("server.host="):
            lines.append(f"server.host={host}")
            replaced_host = True
        elif port and line.startswith("server.port="):
            lines.append(f"server.port={port}")
            replaced_port = True
        elif line.startswith("pd.enabled="):
            lines.append("pd.enabled=false")
            replaced_pd_enabled = True
        elif line.startswith("server.direct_url="):
            lines.append(f"server.direct_url={server_url}")
            replaced_direct_url = True
        else:
            lines.append(line)
    if host and not replaced_host:
        lines.append(f"server.host={host}")
    if port and not replaced_port:
        lines.append(f"server.port={port}")
    if not replaced_pd_enabled:
        lines.append("pd.enabled=false")
    if not replaced_direct_url:
        lines.append(f"server.direct_url={server_url}")
    conf.write_text("\n".join(lines) + "\n", encoding="utf-8")


def run_hubble_only_checks(hubble_url):
    checks = []
    readiness = request("GET", f"{hubble_url}/about")
    if not is_hubble_readiness_response(readiness):
        raise RuntimeError(f"Unexpected Hubble /about response: {readiness}")
    checks.append({"name": "hubble-readiness", "status": "passed",
                   "detail": readiness})
    root = request("GET", f"{hubble_url}/")
    if '<div id="root"></div>' not in root:
        raise RuntimeError("Hubble root did not serve React root")
    checks.append({"name": "hubble-ui-root", "status": "passed"})
    for route in (
        "/navigation",
        "/graphspace",
        "/gremlin",
        "/algorithms",
        "/asyncTasks",
    ):
        body = request("GET", f"{hubble_url}{route}")
        if '<div id="root"></div>' not in body:
            raise RuntimeError(f"Route did not serve React root: {route}")
        checks.append({"name": f"route:{route}", "status": "passed"})
    return checks


def run_login_checks(hubble_url, username, password):
    checks = []
    api_prefix = f"{hubble_url}/api/v1.3"
    user = unwrap(request("POST", f"{api_prefix}/auth/login", {
        "user_name": username,
        "user_password": password
    }), "hubble-login")
    user_name = (user.get("user_name") or user.get("name") or
                 user.get("id") or "")
    if str(user_name) != username:
        raise RuntimeError(f"Unexpected login user: {user}")
    checks.append({"name": "hubble-login", "status": "passed",
                   "user": username})

    auth_status = unwrap(request("GET", f"{api_prefix}/auth/status"),
                         "hubble-auth-status")
    if "level" not in auth_status:
        raise RuntimeError(f"Unexpected auth status response: {auth_status}")
    checks.append({"name": "hubble-auth-status", "status": "passed",
                   "level": auth_status.get("level")})
    return checks


def run_server_login(server_url, username, password):
    global SERVER_TOKEN
    auth = f"{username}:{password}"
    basic = base64.b64encode(auth.encode("utf-8")).decode("ascii")
    response = request("POST", f"{server_url}/auth/login", {
        "user_name": username,
        "user_password": password,
        "token_expire": 60 * 60 * 24 * 30
    }, headers={"Authorization": f"Basic {basic}"})
    token = response.get("token") if isinstance(response, dict) else None
    if not token:
        raise RuntimeError(f"Unexpected Server login response: {response}")
    SERVER_TOKEN = token
    return [{"name": "server-login", "status": "passed",
             "user": username}]


def run_server_checks(hubble_url, server_url, graph_space, graph_name):
    checks = []
    api_prefix = f"{hubble_url}/api/v1.3"
    for name, path in (
        ("graphspace-list", "graphspaces/list"),
        ("schema-graphview",
         f"graphspaces/{graph_space}/graphs/{graph_name}/schema/graphview"),
        ("job-manager-list",
         f"graphspaces/{graph_space}/graphs/{graph_name}/job-manager"
         "?page_no=1&page_size=10"),
        ("async-task-list",
         f"graphspaces/{graph_space}/graphs/{graph_name}/async-tasks"
         "?page_no=1&page_size=10"),
    ):
        response = request("GET", f"{api_prefix}/{path}")
        if response.get("status") != 200:
            raise RuntimeError(f"{name} failed: {response}")
        checks.append({"name": name, "status": "passed"})
    versions = request("GET", f"{server_url}/versions")
    checks.append({"name": "server-versions", "status": "passed",
                   "detail_type": type(versions).__name__})
    return checks


def get_json_status(method, url, body=None):
    try:
        response = request(method, url, body)
        if isinstance(response, dict):
            return response.get("status", 200)
        return 200
    except urllib.error.HTTPError as exc:
        try:
            payload = exc.read().decode("utf-8")
            response = json.loads(payload)
            return response.get("status", exc.code)
        except (UnicodeDecodeError, json.JSONDecodeError):
            return exc.code


def run_analysis_boundary_checks(hubble_url, graph_space, graph_name):
    base = f"{hubble_url}/api/v1.3/graphspaces/{graph_space}/graphs/{graph_name}"
    checks = []

    cypher_status = get_json_status(
        "GET",
        f"{base}/cypher?cypher={urllib.parse.quote('MATCH (n) RETURN n LIMIT 1')}"
    )
    if cypher_status not in (200, 400):
        raise RuntimeError(
            f"analysis-cypher-boundary failed: status {cypher_status}")
    checks.append({
        "name": "analysis-cypher-boundary",
        "status": "passed" if cypher_status == 200 else "skipped",
        "http_or_business_status": cypher_status,
        "classification": ("hubble-api-available"
                           if cypher_status == 200 else
                           "boundary-or-environment-dependent")
    })

    olap_status = get_json_status("POST", f"{base}/algorithms/olap", {
        "algorithm": "pagerank",
        "worker": 1,
        "params": {}
    })
    if olap_status not in (200, 400):
        raise RuntimeError(
            f"analysis-olap-boundary failed: status {olap_status}")
    checks.append({
        "name": "analysis-olap-boundary",
        "status": "passed" if olap_status == 200 else "skipped",
        "http_or_business_status": olap_status,
        "classification": ("hubble-api-available"
                           if olap_status == 200 else
                           "boundary-or-environment-dependent")
    })

    return checks


def ignore_conflict(call):
    try:
        return call()
    except urllib.error.HTTPError as exc:
        if exc.code == 400:
            return None
        raise


def create_schema(hubble_url, server_url, graph_space, graph_name, prefix):
    base = (f"{hubble_url}/api/v1.3/graphspaces/{graph_space}/graphs/"
            f"{graph_name}/schema")
    pk_id = f"{prefix}_id"
    pk_name = f"{prefix}_name"
    pk_rank = f"{prefix}_rank"
    vl_person = f"{prefix}_person"
    el_knows = f"{prefix}_knows"

    checks = []
    for name, data_type in ((pk_id, "TEXT"), (pk_name, "TEXT"),
                            (pk_rank, "INT")):
        body = {"name": name, "data_type": data_type, "cardinality": "SINGLE"}
        response = ignore_conflict(
            lambda body=body: request("POST", f"{base}/propertykeys", body)
        )
        if response is not None:
            unwrap(response, f"create property key {name}")
    checks.append({"name": "hubble-schema-propertykeys", "status": "passed"})

    vertex_body = {
        "name": vl_person,
        "id_strategy": "CUSTOMIZE_STRING",
        "properties": [
            {"name": pk_name, "nullable": True},
            {"name": pk_rank, "nullable": True}
        ],
        "primary_keys": [],
        "property_indexes": [],
        "open_label_index": True,
        "style": {"color": "#2B65FF", "icon": "user",
                  "display_fields": []}
    }
    response = ignore_conflict(
        lambda: request("POST", f"{base}/vertexlabels", vertex_body)
    )
    if response is not None:
        unwrap(response, f"create vertex label {vl_person}")
    checks.append({"name": "hubble-schema-vertexlabel", "status": "passed",
                   "label": vl_person})

    edge_body = {
        "name": el_knows,
        "edgelabel_type": "NORMAL",
        "source_label": vl_person,
        "target_label": vl_person,
        "link_multi_times": False,
        "properties": [],
        "sort_keys": [],
        "property_indexes": [],
        "open_label_index": True,
        "style": {"color": "#0EB880", "display_fields": []}
    }
    response = ignore_conflict(
        lambda: request("POST", f"{base}/edgelabels", edge_body)
    )
    if response is not None:
        unwrap(response, f"create edge label {el_knows}")
    checks.append({"name": "hubble-schema-edgelabel", "status": "passed",
                   "label": el_knows})

    direct = server_json("GET", server_url,
                         f"/graphs/{graph_name}/schema/vertexlabels/{vl_person}")
    if direct.get("name") != vl_person:
        raise RuntimeError(f"Direct Server schema check failed: {direct}")
    checks.append({"name": "server-direct-schema-vertexlabel",
                   "status": "passed", "label": vl_person})
    return checks, {
        "pk_id": pk_id,
        "pk_name": pk_name,
        "pk_rank": pk_rank,
        "vl_person": vl_person,
        "el_knows": el_knows
    }


def encode_multipart(fields, file_field, file_name, content):
    boundary = "----hubble694" + uuid.uuid4().hex
    parts = []
    for name, value in fields.items():
        parts.append(
            f"--{boundary}\r\n"
            f"Content-Disposition: form-data; name=\"{name}\"\r\n\r\n"
            f"{value}\r\n".encode("utf-8")
        )
    parts.append(
        f"--{boundary}\r\n"
        f"Content-Disposition: form-data; name=\"{file_field}\"; "
        f"filename=\"{file_name}\"\r\n"
        "Content-Type: text/csv\r\n\r\n".encode("utf-8")
    )
    parts.append(content)
    parts.append(f"\r\n--{boundary}--\r\n".encode("utf-8"))
    return b"".join(parts), f"multipart/form-data; boundary={boundary}"


def upload_csv(hubble_url, graph_space, graph_name, prefix, schema):
    job = unwrap(request("POST",
                         f"{hubble_url}/api/v1.3/graphspaces/{graph_space}/"
                         f"graphs/{graph_name}/job-manager",
                         {"job_name": f"{prefix}_job",
                          "job_remarks": "issue_694_live_loader_smoke"}),
                 "create loader job")
    job_id = job["id"]
    file_name = f"{prefix}_edges.csv"
    csv_text = (
        f"source,target,{schema['pk_name']},{schema['pk_rank']}\n"
        f"{prefix}_alice,{prefix}_bob,Alice,1\n"
        f"{prefix}_bob,{prefix}_carol,Bob,2\n"
    )
    token_map = unwrap(request(
        "GET",
        f"{hubble_url}/api/v1.3/graphspaces/{graph_space}/graphs/"
        f"{graph_name}/job-manager/"
        f"{job_id}/upload-file/token?names={urllib.parse.quote(file_name)}"
    ), "create upload token")
    token = token_map[file_name]
    multipart_body, content_type = encode_multipart(
        {
            "name": file_name,
            "size": str(len(csv_text.encode("utf-8"))),
            "token": token,
            "total": "1",
            "index": "0"
        },
        "file",
        file_name,
        csv_text.encode("utf-8")
    )
    upload = unwrap(request(
        "POST",
        f"{hubble_url}/api/v1.3/graphspaces/{graph_space}/graphs/"
        f"{graph_name}/job-manager/"
        f"{job_id}/upload-file",
        multipart_body,
        {"Content-Type": content_type}
    ), "upload loader csv")
    file_id = upload["id"]
    unwrap(request(
        "PUT",
        f"{hubble_url}/api/v1.3/graphspaces/{graph_space}/graphs/"
        f"{graph_name}/job-manager/"
        f"{job_id}/upload-file/next-step"
    ), "advance upload step")
    return job_id, file_id


def configure_mapping(hubble_url, graph_space, graph_name, job_id, file_id,
                      schema):
    base = (f"{hubble_url}/api/v1.3/graphspaces/{graph_space}/graphs/"
            f"{graph_name}/job-manager/{job_id}/file-mappings")
    mapping = unwrap(request("POST", f"{base}/{file_id}/file-setting", {
        "has_header": True,
        "format": "CSV",
        "delimiter": ",",
        "charset": "UTF-8",
        "date_format": "yyyy-MM-dd HH:mm:ss",
        "time_zone": "GMT+8",
        "skipped_line": "(^#|^//).*|"
    }), "configure file setting")
    if "source" not in mapping["file_setting"]["column_names"]:
        raise RuntimeError(f"File setting did not read CSV columns: {mapping}")

    null_values = {"checked": ["", "NULL", "null"], "customized": []}
    unwrap(request("POST", f"{base}/{file_id}/vertex-mappings", {
        "label": schema["vl_person"],
        "id_fields": ["source"],
        "field_mapping": [
            {"column_name": schema["pk_name"], "mapped_name": schema["pk_name"]},
            {"column_name": schema["pk_rank"], "mapped_name": schema["pk_rank"]}
        ],
        "value_mapping": [],
        "null_values": null_values
    }), "add source vertex mapping")
    unwrap(request("POST", f"{base}/{file_id}/vertex-mappings", {
        "label": schema["vl_person"],
        "id_fields": ["target"],
        "field_mapping": [],
        "value_mapping": [],
        "null_values": null_values
    }), "add target vertex mapping")
    unwrap(request("POST", f"{base}/{file_id}/edge-mappings", {
        "label": schema["el_knows"],
        "source_fields": ["source"],
        "target_fields": ["target"],
        "field_mapping": [],
        "value_mapping": [],
        "null_values": null_values
    }), "add edge mapping")
    unwrap(request("PUT", f"{base}/next-step"), "advance mapping step")
    unwrap(request("POST", f"{base}/load-parameter", {
        "check_vertex": False,
        "insert_timeout": 60,
        "max_parse_errors": 1,
        "max_insert_errors": 1,
        "retry_times": 1,
        "retry_interval": 1
    }), "configure load parameters")


def wait_for_load_task(hubble_url, graph_space, graph_name, job_id, file_id):
    base = (f"{hubble_url}/api/v1.3/graphspaces/{graph_space}/graphs/"
            f"{graph_name}/job-manager/{job_id}/load-tasks")
    tasks = unwrap(request("POST", f"{base}/start?file_mapping_ids={file_id}",
                           {}),
                   "start load task")
    if not tasks:
        raise RuntimeError("Load task start returned no tasks")
    task_id = tasks[0]["id"]
    deadline = time.time() + 90
    last_task = tasks[0]
    while time.time() < deadline:
        last_task = unwrap(request("GET", f"{base}/{task_id}"),
                           "poll load task")
        status = last_task.get("status")
        if status in ("SUCCEED", "FAILED", "STOPPED", "PAUSED"):
            break
        time.sleep(1)
    if last_task.get("status") != "SUCCEED":
        raise RuntimeError(f"Load task did not succeed: {last_task}")
    job = unwrap(request(
        "GET",
        f"{hubble_url}/api/v1.3/graphspaces/{graph_space}/graphs/"
        f"{graph_name}/job-manager/{job_id}"
    ), "poll loader job")
    return task_id, last_task, job


def first_table_value(gremlin_result):
    table = gremlin_result.get("table_view") or gremlin_result.get("tableView")
    if not table:
        raise RuntimeError(f"No table view in gremlin result: {gremlin_result}")
    rows = table.get("data") or table.get("rows") or []
    if not rows:
        raise RuntimeError(f"No rows in gremlin result: {gremlin_result}")
    row = rows[0]
    if isinstance(row, dict):
        return next(iter(row.values()))
    if isinstance(row, list):
        return row[0]
    return row


def run_hubble_gremlin(hubble_url, graph_space, graph_name, content):
    return unwrap(request(
        "POST",
        f"{hubble_url}/api/v1.3/graphspaces/{graph_space}/graphs/"
        f"{graph_name}/gremlin-query",
        {"content": content}
    ), f"Hubble Gremlin {content}")


def run_server_label_count(server_url, graph_name, resource, label):
    response = server_json(
        "GET",
        server_url,
        f"/graphs/{graph_name}/graph/{resource}"
        f"?label={urllib.parse.quote(label)}&limit=100"
    )
    values = response.get(resource)
    if not isinstance(values, list):
        raise RuntimeError(f"Unexpected Server {resource} response: {response}")
    return len(values)


def object_id(item):
    if isinstance(item, dict):
        return (item.get("id") or item.get("source") or item.get("target") or
                item.get("name"))
    return item


def normalize_vertices(vertices):
    return {str(object_id(vertex)) for vertex in vertices}


def normalize_edges(edges):
    normalized = set()
    for edge in edges:
        if not isinstance(edge, dict):
            match = re.match(r"^S(.+?)>.*>>S(.+)$", str(edge))
            if match:
                normalized.add((match.group(1), match.group(2), ""))
            else:
                normalized.add((str(edge), "", ""))
            continue
        source = (edge.get("source") or edge.get("sourceId") or
                  edge.get("outV") or edge.get("out"))
        target = (edge.get("target") or edge.get("targetId") or
                  edge.get("inV") or edge.get("in"))
        label = edge.get("label") or edge.get("name") or ""
        normalized.add((str(source), str(target), str(label)))
    return normalized


def edge_pairs_contain(actual_edges, expected_edges):
    for source, target, label in expected_edges:
        if not any(edge_source == source and edge_target == target and
                   (edge_label in ("", label))
                   for edge_source, edge_target, edge_label in actual_edges):
            return False
    return True


def run_live_function_flow(hubble_url, server_url, graph_space, graph_name,
                           prefix):
    checks = []
    schema_checks, schema = create_schema(hubble_url, server_url, graph_space,
                                          graph_name, prefix)
    checks.extend(schema_checks)
    job_id, file_id = upload_csv(hubble_url, graph_space, graph_name, prefix,
                                 schema)
    checks.append({"name": "hubble-upload-csv", "status": "passed",
                   "job_id": job_id, "file_mapping_id": file_id})
    configure_mapping(hubble_url, graph_space, graph_name, job_id, file_id,
                      schema)
    checks.append({"name": "hubble-file-mapping", "status": "passed",
                   "file_mapping_id": file_id})
    task_id, task, job = wait_for_load_task(hubble_url, graph_space,
                                            graph_name, job_id, file_id)
    checks.append({"name": "hubble-loader-task", "status": "passed",
                   "task_id": task_id, "task_status": task.get("status"),
                   "job_status": job.get("job_status")})

    d_vertices = run_server_label_count(server_url, graph_name, "vertices",
                                        schema["vl_person"])
    d_edges = run_server_label_count(server_url, graph_name, "edges",
                                     schema["el_knows"])
    expected_vertices = 3
    expected_edges = 2
    if (d_vertices, d_edges) != (expected_vertices, expected_edges):
        raise RuntimeError("Loader imported unexpected graph size: "
                           f"{(d_vertices, d_edges)} != "
                           f"{(expected_vertices, expected_edges)}")
    checks.append({"name": "server-loader-rest-count",
                   "status": "passed", "vertices": d_vertices,
                   "edges": d_edges, "expected_vertices": expected_vertices,
                   "expected_edges": expected_edges})

    direct_shortest = server_json(
        "GET", server_url,
        f"/graphs/{graph_name}/traversers/shortestpath"
        f"?source={urllib.parse.quote(json.dumps(prefix + '_alice'))}"
        f"&target={urllib.parse.quote(json.dumps(prefix + '_carol'))}"
        f"&direction=OUT&label={urllib.parse.quote(schema['el_knows'])}"
        f"&max_depth=3&max_degree=1000&skip_degree=0&capacity=10000"
    )
    expected_vertex_ids = {
        f"{prefix}_alice",
        f"{prefix}_bob",
        f"{prefix}_carol"
    }
    expected_edge_pairs = {
        (f"{prefix}_alice", f"{prefix}_bob", schema["el_knows"]),
        (f"{prefix}_bob", f"{prefix}_carol", schema["el_knows"])
    }
    direct_vertices = direct_shortest.get("vertices") or []
    direct_edges = direct_shortest.get("edges") or []
    direct_path = direct_shortest.get("path") or []
    direct_vertex_ids = normalize_vertices(direct_vertices)
    if direct_vertex_ids and direct_vertex_ids != expected_vertex_ids:
        raise RuntimeError("Server direct shortestPath vertices mismatch: "
                           f"{direct_vertex_ids} != {expected_vertex_ids}")
    direct_edge_set = normalize_edges(direct_edges)
    if direct_edge_set and not edge_pairs_contain(direct_edge_set,
                                                  expected_edge_pairs):
        raise RuntimeError("Server direct shortestPath edges mismatch: "
                           f"{direct_edge_set} missing {expected_edge_pairs}")
    if direct_path and [str(object_id(item)) for item in direct_path] != [
            f"{prefix}_alice", f"{prefix}_bob", f"{prefix}_carol"]:
        raise RuntimeError(f"Server direct shortestPath path mismatch: "
                           f"{direct_path}")
    if not direct_vertices and not direct_path:
        raise RuntimeError(f"Server direct shortestPath returned no path: "
                           f"{direct_shortest}")
    checks.append({"name": "server-direct-shortestpath",
                   "status": "passed", "server_vertices": len(direct_vertices),
                   "server_edges": len(direct_edges),
                   "server_path_objects": len(direct_path),
                   "expected_vertices": sorted(expected_vertex_ids),
                   "expected_edges": sorted(expected_edge_pairs)})
    checks.append({
        "name": "hubble-shortestpath-string-id-followup",
        "status": "skipped",
        "classification": "known-follow-up",
        "reason": ("Hubble shortestPath string-id handling is tracked outside "
                   "the loader auth smoke gate")
    })
    return checks


def log_tail(path):
    if path is None or not path.exists():
        return None
    return "\n".join(path.read_text(encoding="utf-8", errors="replace")
                    .splitlines()[-80:])


def main():
    parser = argparse.ArgumentParser(description="Run live Hubble issue #694 smoke")
    parser.add_argument("tarball", type=Path)
    parser.add_argument("--server-url", default="http://127.0.0.1:8080")
    parser.add_argument("--hubble-url", default=os.environ.get("HUBBLE_URL",
                                                              "http://127.0.0.1:8088"))
    parser.add_argument("--graph", default=os.environ.get("HUGEGRAPH_GRAPH", "hugegraph"))
    parser.add_argument("--work-dir", type=Path)
    parser.add_argument("--keep-work-dir", action="store_true")
    parser.add_argument("--skip-start", action="store_true")
    parser.add_argument("--foreground-start", action="store_true")
    parser.add_argument("--leave-running", action="store_true")
    parser.add_argument("--bind-host")
    parser.add_argument("--loader-flow", action="store_true")
    parser.add_argument("--analysis-boundary", action="store_true")
    parser.add_argument("--graphspace", default=os.environ.get("HUGEGRAPH_GRAPHSPACE",
                                                              "DEFAULT"))
    parser.add_argument("--connection-name")
    parser.add_argument("--data-prefix")
    parser.add_argument("--json-output", type=Path)
    parser.add_argument("--username", default=os.environ.get("HUBBLE_USERNAME",
                                                            "admin"))
    parser.add_argument("--password", default=os.environ.get("HUBBLE_PASSWORD",
                                                            "pa"))
    args = parser.parse_args()

    if not args.skip_start:
        try:
            require_safe_archive_extraction()
        except RuntimeError as exc:
            parser.error(str(exc))

    hubble_url = args.hubble_url.rstrip("/")
    server_url = args.server_url.rstrip("/")
    work_dir = args.work_dir or Path(tempfile.mkdtemp(prefix="hubble-694-live-"))
    created_work_dir = args.work_dir is None
    hubble_home = None
    process = None
    log_handle = None
    hubble_log = None
    app_log = None
    report = {
        "tarball": str(args.tarball),
        "hubble_url": hubble_url,
        "server_url": server_url,
        "graph": args.graph,
        "checks": [],
        "status": "failed"
    }
    prefix = args.data_prefix or f"issue_694_{int(time.time())}"
    connection_name = args.connection_name or f"{prefix}_conn"
    report["data_prefix"] = prefix
    report["connection_name"] = connection_name

    try:
        if not args.skip_start:
            if is_healthy(hubble_url):
                raise RuntimeError("Hubble URL is already healthy before "
                                   "candidate startup; refusing to validate an "
                                   "unknown existing process")
            work_dir.mkdir(parents=True, exist_ok=True)
            hubble_home = extract_tarball(args.tarball, work_dir)
            report["work_dir"] = str(work_dir)
            report["hubble_home"] = str(hubble_home)
            configure_hubble_endpoint(hubble_home, hubble_url, args.bind_host,
                                      server_url)
            hubble_log = work_dir / "hubble-live-smoke.log"
            app_log = hubble_home / "logs" / "hugegraph-hubble.log"
            log_handle = hubble_log.open("w", encoding="utf-8")
            if args.foreground_start:
                process = subprocess.Popen(
                    [str(hubble_home / "bin" / "start-hubble.sh"), "-f", "true"],
                    cwd=str(hubble_home),
                    stdout=log_handle,
                    stderr=subprocess.STDOUT,
                    text=True
                )
            else:
                result = subprocess.run(
                    [str(hubble_home / "bin" / "start-hubble.sh")],
                    cwd=str(hubble_home),
                    stdout=log_handle,
                    stderr=subprocess.STDOUT,
                    text=True,
                    check=False
                )
                if result.returncode != 0:
                    raise RuntimeError("Hubble start script failed with "
                                       f"exit code {result.returncode}")
            wait_for_health(hubble_url, 90)
        else:
            wait_for_health(hubble_url, 10)

        report["checks"].extend(run_hubble_only_checks(hubble_url))
        report["checks"].extend(run_login_checks(hubble_url, args.username,
                                                 args.password))
        report["checks"].extend(run_server_login(server_url, args.username,
                                                 args.password))
        report["checks"].extend(run_server_checks(hubble_url, server_url,
                                                  args.graphspace, args.graph))

        if args.loader_flow:
            report["checks"].extend(run_live_function_flow(hubble_url,
                                                           server_url,
                                                           args.graphspace,
                                                           args.graph,
                                                           prefix))
        if args.analysis_boundary:
            report["checks"].extend(run_analysis_boundary_checks(hubble_url,
                                                                 args.graphspace,
                                                                 args.graph))

        report["status"] = "passed"
    except (urllib.error.URLError, RuntimeError) as exc:
        report["error"] = str(exc)
        if isinstance(exc, urllib.error.HTTPError):
            try:
                error_payload = exc.read().decode("utf-8", errors="replace")
                report["http_error_body"] = error_payload
            except Exception as body_exc:  # noqa: BLE001 - evidence best effort
                report["http_error_body_error"] = str(body_exc)
        wrapper_tail = log_tail(hubble_log)
        app_tail = log_tail(app_log)
        if wrapper_tail is not None:
            report["hubble_wrapper_log"] = str(hubble_log)
            report["hubble_wrapper_log_tail"] = wrapper_tail
        if app_tail is not None:
            report["hubble_app_log"] = str(app_log)
            report["hubble_app_log_tail"] = app_tail
        raise SystemExit(json.dumps(report, indent=2, sort_keys=True))
    finally:
        if process is not None and not args.leave_running:
            process.terminate()
            try:
                process.wait(timeout=20)
            except subprocess.TimeoutExpired:
                process.kill()
        if log_handle is not None:
            log_handle.close()
        if hubble_home is not None and not args.leave_running:
            subprocess.run([str(hubble_home / "bin" / "stop-hubble.sh")],
                           stdout=subprocess.DEVNULL,
                           stderr=subprocess.DEVNULL,
                           check=False)
        if args.json_output:
            args.json_output.parent.mkdir(parents=True, exist_ok=True)
            args.json_output.write_text(json.dumps(report, indent=2,
                                                   sort_keys=True) + "\n",
                                        encoding="utf-8")
        if created_work_dir and not args.keep_work_dir and not args.leave_running:
            shutil.rmtree(work_dir, ignore_errors=True)

    print(json.dumps(report, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
