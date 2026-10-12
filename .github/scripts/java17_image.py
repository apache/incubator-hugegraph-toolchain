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
"""Candidate provenance and real container behavior gates; no registry publishing."""

import argparse
import hashlib
import importlib.util
import json
import os
import re
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import uuid

from verify_candidate_image_sdk import source_commit, validate_sdk
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HELPER = ROOT / "hugegraph-hubble/hubble-dist/assembly/travis/run_live_hubble_smoke.py"
SPEC = importlib.util.spec_from_file_location("hubble_smoke", HELPER)
HTTP = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(HTTP)
SERVER = "http://127.0.0.1:8080"
HUBBLE = "http://127.0.0.1:8088"
REQUIRED = ("hugegraph-common", "hg-pd-client", "hg-pd-grpc")
MEMORY = "_JAVA_OPTIONS=-Xms128m -Xmx512m -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8"


def run(args, log=None, check=True, **kwargs):
    result = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            text=True, encoding="utf-8", errors="replace",
                            timeout=kwargs.pop("timeout", 180), **kwargs)
    if log:
        Path(log).write_text(result.stdout, encoding="utf-8")
    if check and result.returncode:
        raise RuntimeError(f"{args[0]} failed with exit {result.returncode}; log={log}")
    return result.stdout.strip()


def digest(path):
    checksum = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            checksum.update(chunk)
    return checksum.hexdigest()


def prepare(module, commit, repository, server_archive, archive_sha, work, evidence):
    sdk, hashes = validate_sdk(repository)
    if commit != source_commit() or sdk["commit"] != commit:
        raise RuntimeError("Image source does not match the locked SDK commit")
    if not server_archive.is_file() or digest(server_archive) != archive_sha:
        raise RuntimeError("Missing or changed same-source server archive")
    archive = work / "server.tar.gz"
    shutil.copy2(server_archive, archive)
    shutil.copy2(repository / "candidate-sdk-manifest.json", evidence / "candidate-sdk-manifest.json")
    manifest = {"module": module, "server_commit": commit,
                "server_repository": sdk["repository"],
                "toolchain_commit": run(["git", "-C", str(ROOT), "rev-parse", "HEAD"]),
                "image": f"hugegraph-java17-{module}:ci", "server_archive": str(archive),
                "server_archive_sha256": digest(archive), "candidate_jars": hashes}
    (evidence / "manifest.json").write_text(json.dumps(manifest, indent=2))


def verify_jars(module, expected, actual):
    required = REQUIRED + (("hugegraph-core",) if module == "hubble" else ())
    for artifact in required:
        matches = [name for name in actual if name.startswith(artifact + "-")]
        if len(matches) != 1 or matches[0] not in expected:
            raise RuntimeError(f"Missing or ambiguous candidate library: {artifact}")
    verified = {}
    for name, checksum in actual.items():
        own_tool = name.startswith(("hugegraph-client-", "hugegraph-loader-"))
        if name.startswith(("hugegraph-", "hg-")) and not own_tool:
            if name not in expected or checksum != expected[name]:
                raise RuntimeError(f"Candidate hash mismatch: {name}")
            verified[name] = checksum
    return verified


def require_java17(output):
    if not re.search(r'version "17[.\"]', output):
        raise RuntimeError("Container did not execute Java 17")


def configure(path, values, remove=()):
    lines = [line for line in path.read_text().splitlines()
             if line.split("=", 1)[0].strip() not in set(values) | set(remove)]
    path.write_text("\n".join(lines + [f"{k}={v}" for k, v in values.items()]) + "\n")


def wait_for_graph():
    deadline = time.monotonic() + 90
    while time.monotonic() < deadline:
        try:
            if HTTP.request("GET", SERVER + "/graphspaces/DEFAULT/graphs/hugegraph",
                            timeout=3).get("name") == "hugegraph":
                return
        except (OSError, ValueError):
            pass
        time.sleep(1)
    raise RuntimeError("Candidate graph was not ready within 90 seconds")


def start_server(manifest, runtime, evidence):
    archive = Path(manifest["server_archive"])
    if digest(archive) != manifest["server_archive_sha256"]:
        raise RuntimeError("Server archive changed after candidate build")
    HTTP.extract_archive(archive, runtime)
    homes = list(runtime.glob("apache-hugegraph-*"))
    if len(homes) != 1 or not homes[0].is_dir():
        raise RuntimeError("Invalid server archive root")
    home = homes[0]
    configure(home / "conf/rest-server.properties",
              {"restserver.url": SERVER, "usePD": "false"}, ("auth.authenticator",))
    configure(home / "conf/graphs/hugegraph.properties",
              {"gremlin.graph": "org.apache.hugegraph.HugeFactory", "backend": "rocksdb"})
    # This is a fresh anonymous fixture, not an alteration of an authenticated service.
    version = run([str(Path(os.environ["JAVA_HOME"]) / "bin/java"), "-version"],
                  evidence / "server-java-version.log")
    require_java17(version)
    environment = dict(os.environ, _JAVA_OPTIONS="-Xms128m -Xmx768m -XX:ActiveProcessorCount=2")
    run(["bash", "bin/init-store.sh"], evidence / "server-init.log", cwd=home,
        env=environment, stdin=subprocess.DEVNULL)
    run(["bash", "bin/start-hugegraph.sh"], evidence / "server-start.log", cwd=home,
        env=environment)
    wait_for_graph()
    return home


def check_loader(container, evidence):
    command = ["docker", "exec", "-e", MEMORY, container, "bin/hugegraph-loader.sh"]
    run(command + ["--help"], evidence / "loader-help.log")
    run(command + ["-g", "hugegraph", "-f", "example/file/struct.json", "-s",
                   "example/file/schema.groovy", "-h", "127.0.0.1", "-p", "8080"],
        evidence / "loader-import.log")
    counts = {}
    for resource, label, expected in (("vertices", "person", 6), ("vertices", "software", 2),
                                      ("edges", "knows", 2), ("edges", "created", 4)):
        response = HTTP.request("GET", SERVER + "/graphspaces/DEFAULT/graphs/hugegraph/graph/" +
                                f"{resource}?label={label}&limit=100")
        values = response.get(resource)
        if not isinstance(values, list) or len(values) != expected:
            raise RuntimeError(f"Loader readback mismatch for {label}")
        counts[label] = len(values)
        if label == "person":
            marko = next((v for v in values if v["properties"].get("name") == "marko"), None)
            if marko is None:
                raise RuntimeError("Loader readback is missing vertex marko")
            if marko["properties"].get("age") != 29 or marko["properties"].get("city") != "Beijing":
                raise RuntimeError("Loader did not preserve example property values")
    return counts


def api(method, path, body=None):
    return HTTP.unwrap(HTTP.request(method, HUBBLE + "/api/v1.3/" + path, body), path)


def check_hubble_before_restart():
    HTTP.wait_for_health(HUBBLE, 90)
    checks = HTTP.run_hubble_only_checks(HUBBLE)
    if api("GET", "auth/context").get("mode") != "NON_AUTH":
        raise RuntimeError("Hubble did not detect the anonymous candidate fixture")
    graph = "java17_image_smoke"
    base = "graphspaces/DEFAULT/graphs/" + graph
    api("POST", base, {"nickname": graph})
    graph_info = HTTP.request("GET", SERVER + "/" + base)
    if graph_info.get("name") != graph:
        raise RuntimeError("Created graph is not usable on the candidate server")
    checks.extend(HTTP.run_server_checks(HUBBLE, SERVER, "DEFAULT", graph))
    path = base + "/gremlin-collections"
    favorite = api("POST", path, {"name": "image_persistence", "type": "GREMLIN",
                                  "content": "g.V().count()"})
    saved_path = path + "/" + str(favorite["id"])
    api("PUT", saved_path, {"id": favorite["id"], "content": "g.E().count()"})
    if api("GET", saved_path).get("content") != "g.E().count()":
        raise RuntimeError("H2 metadata update was not readable")
    return saved_path, checks


def smoke(evidence):
    manifest = json.loads((evidence / "manifest.json").read_text())
    module, image = manifest["module"], manifest["image"]
    name = "java17-image-" + uuid.uuid4().hex[:10]
    volume = name + "-data"
    report = {"status": "failed", "server_commit": manifest["server_commit"],
              "toolchain_commit": manifest["toolchain_commit"], "container_ids": []}
    server_home = None
    volume_created = False
    failure_types = set()
    with tempfile.TemporaryDirectory(prefix="java17-image-runtime-") as directory:
        try:
            report["image_id"] = run(["docker", "image", "inspect", "--format", "{{.Id}}", image])
            labels = run(["docker", "image", "inspect", "--format",
                          '{{index .Config.Labels "org.apache.hugegraph.server.revision"}} ' +
                          '{{index .Config.Labels "org.opencontainers.image.revision"}}', image])
            if labels.split() != [manifest["server_commit"], manifest["toolchain_commit"]]:
                raise RuntimeError("Image labels do not match the tested source revisions")
            for port in ((8080, 8088) if module == "hubble" else (8080,)):
                with socket.socket() as probe:
                    probe.bind(("127.0.0.1", port))
            server_home = start_server(manifest, Path(directory), evidence)
            options = ["--network", "host", "--memory=1g", "--cpus=2", "-e", MEMORY]
            if module == "hubble":
                run(["docker", "volume", "create", volume])
                volume_created = True
                options += ["-v", volume + ":/hubble/data"]
            start = ["docker", "run", "-d", "--name", name, *options, image]
            report["container_ids"].append(run(start))
            version = run(["docker", "exec", name, "java", "-version"], evidence / "java-version.log")
            require_java17(version)
            sums = run(["docker", "exec", name, "sh", "-c", f"sha256sum /{module}/lib/*.jar"])
            actual = {Path(line.split(maxsplit=1)[1]).name: line.split()[0] for line in sums.splitlines()}
            report["verified_candidate_jars"] = verify_jars(module, manifest["candidate_jars"], actual)
            if module == "loader":
                report["example_readback"] = check_loader(name, evidence)
            else:
                saved_path, report["checks"] = check_hubble_before_restart()
                run(["docker", "exec", name, "test", "-s", "/hubble/data/hubble-v2.mv.db"])
                run(["docker", "stop", "--time", "30", name])
                run(["docker", "logs", name], evidence / "hubble-first.log")
                run(["docker", "rm", name])
                report["container_ids"].append(run(start))
                HTTP.wait_for_health(HUBBLE, 90)
                favorite = api("GET", saved_path)
                if favorite.get("content") != "g.E().count()" or favorite.get("name") != "image_persistence":
                    raise RuntimeError("H2 metadata was not retained across container recreation")
                if report["container_ids"][0] == report["container_ids"][1]:
                    raise RuntimeError("Persistence check did not recreate the container")
                report["h2_recreated_container_readback"] = "passed"
            report["status"] = "passed"
        except Exception as error:
            report["error"] = "smoke: " + type(error).__name__
            failure_types.add("smoke: " + type(error).__name__)
        finally:
            def cleanup(args, **kwargs):
                try:
                    run(args, check=False, **kwargs)
                except Exception as error:
                    report["status"] = "failed"
                    report.setdefault("cleanup_errors", []).append("cleanup: " + type(error).__name__)
                    failure_types.add("cleanup: " + type(error).__name__)

            cleanup(["docker", "logs", name], log=evidence / "container.log")
            cleanup(["docker", "rm", "-fv", name])
            if volume_created:
                cleanup(["docker", "volume", "rm", volume])
            # Also stop a partially initialized fixture if startup failed before returning.
            homes = [server_home] if server_home else list(Path(directory).glob("apache-hugegraph-*"))
            for home in homes:
                cleanup(["bash", "bin/stop-hugegraph.sh"], log=evidence / "server-stop.log", cwd=home)
                for log in (home / "logs").glob("*.log"):
                    try:
                        shutil.copy2(log, evidence / ("server-" + log.name))
                    except OSError as error:
                        report["status"] = "failed"
                        report.setdefault("cleanup_errors", []).append("cleanup: " + type(error).__name__)
                        failure_types.add("cleanup: " + type(error).__name__)
            (evidence / "report.json").write_text(json.dumps(report, indent=2))
    if report["status"] != "passed":
        # Exception details may contain credentials, URLs or full commands; report only failure types.
        summary = ", ".join(sorted(failure_types))[:1024]
        message = f"Java 17 image smoke failed ({summary}); report={evidence / 'report.json'}"
        print(message.encode("utf-8")[:7168].decode("utf-8", errors="ignore"), file=sys.stderr)
    return report["status"] == "passed"


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    prepare_args = commands.add_parser("prepare")
    prepare_args.add_argument("module", choices=("loader", "hubble"))
    prepare_args.add_argument("commit")
    prepare_args.add_argument("repository", type=Path)
    prepare_args.add_argument("server_archive", type=Path)
    prepare_args.add_argument("archive_sha")
    prepare_args.add_argument("work", type=Path)
    prepare_args.add_argument("evidence", type=Path)
    smoke_args = commands.add_parser("smoke")
    smoke_args.add_argument("evidence", type=Path)
    args = parser.parse_args()
    if args.command == "prepare":
        prepare(args.module, args.commit, args.repository, args.server_archive, args.archive_sha, args.work, args.evidence)
    else:
        raise SystemExit(0 if smoke(args.evidence) else 1)
