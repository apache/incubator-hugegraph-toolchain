#!/usr/bin/env python3
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

"""Linux-only, test-owned Flink/MySQL/server acceptance. Default is a no-I/O plan."""

import argparse
import gzip
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import signal
import socket
import subprocess
import sys
import tempfile
import time
import urllib.request
from urllib.parse import urlsplit
import uuid
import xml.etree.ElementTree as ET

FLINK_IMAGE = "flink:2.2.1-java17"
MYSQL_IMAGE = "mysql:8.0.44"
JDBC_COORDINATE = "mysql:mysql-connector-java:8.0.28"
JDBC_SHA256 = "a00ccdf537ff50e50067b989108c2235197ffb65e197149bbb669db843cd1c3e"
GRAPHS = ("flink_initial", "flink_rebuild")
SERVER_URL = "http://127.0.0.1:38080"
FLINK_URL = "http://127.0.0.1:18081"


def validate_jdbc_driver(path):
    digest = hashlib.sha256(Path(path).read_bytes()).hexdigest()
    if digest != JDBC_SHA256:
        raise AssertionError("External test JDBC driver does not match pinned " + JDBC_COORDINATE)
    return {"coordinate": JDBC_COORDINATE, "sha256": digest, "distribution": "external-test-runtime-only"}


def validate_candidate_sdk(path, source_commit):
    manifest = json.loads(Path(path).read_text())
    if manifest.get("commit") != source_commit:
        raise AssertionError("Candidate SDK manifest does not match the selected server source")
    return manifest


def redact(text):
    return re.sub(r"(?i)((?:password|token|authorization)[\"']?\s*[=:]\s*)[^\s,;]+",
                  r"\1[REDACTED]", text)


def normalize(rows, graph=False):
    result = {}
    for row in rows:
        if graph and type(row["id"]) is not int:
            raise AssertionError("Expected numeric graph ID")
        key = str(row["id"])
        if key in result:
            raise AssertionError("Duplicate graph/source ID: " + key)
        props = row["properties"] if graph else row
        if graph and set(props) - {"name", "age", "city"}:
            raise AssertionError("Unexpected graph properties")
        result[key] = {name: props.get(name) for name in ("name", "age", "city")}
    return result


def request(url, body=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=10) as response:
        body = response.read()
        if response.headers.get("Content-Encoding", "").lower() == "gzip":
            body = gzip.decompress(body)
        return json.loads(body)


def wait_for(description, probe, seconds=180):
    deadline = time.monotonic() + seconds
    last_error = "condition not satisfied"
    while time.monotonic() < deadline:
        try:
            value = probe()
            if value:
                return value
        except (OSError, ValueError, RuntimeError) as error:
            last_error = str(error)
        time.sleep(1)
    raise TimeoutError(description + ": " + redact(last_error))


def set_property(path, key, value):
    text = path.read_text()
    pattern = re.compile(r"^\s*" + re.escape(key) + r"\s*=.*$", re.MULTILINE)
    entry = key + "=" + value
    path.write_text(pattern.sub(entry, text) if pattern.search(text) else text + "\n" + entry + "\n")


class Acceptance:
    def __init__(self, args):
        self.args = args
        self.work = Path(tempfile.mkdtemp(prefix="hg-flink-", dir=args.work_root))
        self.evidence = Path(args.evidence).resolve()
        self.evidence.mkdir(parents=True, exist_ok=True)
        self.prefix = "hg-flink-" + uuid.uuid4().hex[:10]
        self.containers = []
        self.server_process = None
        self.server_log = None
        self.report = {"status": "running", "phases": {}, "server_sha": args.server_sha,
                       "toolchain_sha": args.toolchain_sha, "work": str(self.work)}

    def run(self, command, *, cwd=None, env=None, timeout=300, input_text=None):
        result = subprocess.run(command, cwd=cwd, env=env, input=input_text,
                                text=True, capture_output=True, timeout=timeout)
        if result.returncode:
            raise RuntimeError(f"{command[0]} exited {result.returncode}: " +
                               redact((result.stdout + result.stderr)[-6000:]))
        return result.stdout

    def save(self, name, value):
        (self.evidence / (name + ".json")).write_text(json.dumps(value, indent=2) + "\n")

    def phase(self, name, value):
        self.report["phases"][name] = value
        self.save("result", self.report)

    def start_server(self):
        extracted = self.work / "server"
        extracted.mkdir()
        self.run(["tar", "xzf", str(Path(self.args.server_archive).resolve()), "-C", str(extracted)])
        roots = list(extracted.glob("apache-hugegraph-*/"))
        if len(roots) != 1:
            raise AssertionError("Expected one server distribution")
        self.server = roots[0]
        conf = self.server / "conf"
        template = (conf / "graphs/hugegraph.properties").read_text()
        # This is a newly extracted distribution inside this run's private directory.
        for old in (conf / "graphs").glob("*.properties"):
            old.unlink()
        for graph in GRAPHS:
            props = conf / "graphs" / (graph + ".properties")
            props.write_text(template)
            set_property(props, "store", graph)
            set_property(props, "backend", "rocksdb")
            set_property(props, "rocksdb.data_path", str(self.work / (graph + "-data")))
            set_property(props, "rocksdb.wal_path", str(self.work / (graph + "-wal")))
        set_property(conf / "rest-server.properties", "restserver.url", SERVER_URL)
        set_property(conf / "rest-server.properties", "gremlinserver.url", "http://127.0.0.1:18182")
        yaml = conf / "gremlin-server.yaml"
        yaml.write_text(re.sub(r"^#?port:.*$", "port: 18182", yaml.read_text(), flags=re.MULTILINE))
        env = dict(os.environ, JAVA_TOOL_OPTIONS="-Xmx512m -XX:ActiveProcessorCount=2")
        self.run(["bash", "bin/init-store.sh"], cwd=self.server, env=env)
        self.server_log = (self.work / "server-console.log").open("w")
        self.server_process = subprocess.Popen(
            ["bash", "bin/start-hugegraph.sh", "-d", "false", "-j",
             "-Xms256m -Xmx512m -XX:ActiveProcessorCount=2", "-t", "120"],
            cwd=self.server, env=env, stdout=self.server_log, stderr=subprocess.STDOUT,
            start_new_session=True)
        wait_for("server graphs", lambda: request(self.graph_url(GRAPHS[0]) + "/schema/propertykeys"))
        for graph in GRAPHS:
            url = self.graph_url(graph) + "/schema/"
            for name, kind in (("name", "TEXT"), ("age", "INT"), ("city", "TEXT")):
                request(url + "propertykeys", {"name": name, "data_type": kind, "cardinality": "SINGLE"})
            request(url + "vertexlabels", {"name": "cdc_person", "id_strategy": "CUSTOMIZE_NUMBER",
                                            "properties": ["name", "age", "city"], "nullable_keys": ["city"]})
        self.phase("server_ready", {"graphs": GRAPHS, "java": self.java_version()})

    @staticmethod
    def java_version():
        result = subprocess.run([str(Path(os.environ["JAVA_HOME"]) / "bin/java"), "-version"],
                                text=True, capture_output=True, check=True)
        if not re.search(r'version "17[.\"]', result.stderr):
            raise AssertionError("Server JVM must be Java 17")
        return result.stderr.strip()

    @staticmethod
    def graph_url(graph):
        return SERVER_URL + "/graphspaces/DEFAULT/graphs/" + graph

    def verify_identity_api(self):
        # Use only the freshly extracted Server owned by this acceptance run.
        root = Path(__file__).resolve().parents[4]
        suffix = self.prefix + "-identity"
        port = urlsplit(SERVER_URL).port
        test = "org.apache.hugegraph.loader.flink.HugeGraphIdentityIntegrationTest"
        output = self.run(["mvn", "-o", "test", "-pl", "hugegraph-client,hugegraph-loader", "-am", "-ntp",
                           "-Dmaven.repo.local=" + str(Path(self.args.maven_repo).resolve()),
                           "-Dtest=HugeGraphIdentityIntegrationTest", "-Dsurefire.failIfNoSpecifiedTests=false",
                           "-DforkCount=0", "-Dflink.identity.integration=true",
                           "-Dflink.identity.port=" + str(port), "-Dflink.identity.graph=" + GRAPHS[0],
                           "-Dsurefire.reportNameSuffix=" + suffix], cwd=root, timeout=300)
        (self.evidence / "identity-api-maven.log").write_text(redact(output[-60000:]))
        report = root / "hugegraph-loader/target/surefire-reports" / ("TEST-" + test + "-" + suffix + ".xml")
        suite = ET.parse(report).getroot()
        counts = {name: int(suite.get(name, "0")) for name in ("tests", "failures", "errors", "skipped")}
        if counts["tests"] != 1 or any(counts[name] for name in ("failures", "errors", "skipped")) or \
                not any(case.get("name") == "testIdentityChangesDeletesAndReplay" for case in suite.findall("testcase")):
            raise AssertionError("Identity API probe must execute its test without failures or skips")
        self.phase("identity_api_probe", {"test": test, "counts": counts, "port": port, "graph": GRAPHS[0],
                                           "report_suffix": suffix, "scope": "SourceRecord-to-Graph-API; not Flink edge E2E"})

    def container(self, suffix, image, arguments, environment, mounts=(), memory="1200m"):
        name = self.prefix + "-" + suffix
        command = ["docker", "run", "-d", "--name", name, "--network", "host", "--memory", memory,
                   "--label", "hugegraph.flink.acceptance=" + self.prefix]
        for key, value in environment.items():
            command.extend(["-e", key + "=" + value])
        for mount in mounts:
            command.extend(["-v", mount])
        # Record first so a partially successful Docker invocation is still cleaned up.
        self.containers.append(name)
        self.run(command + [image] + arguments)
        return name

    def mysql(self, sql):
        return self.run(["docker", "exec", "-i", self.mysql_name, "mysql", "-uroot", "--protocol=TCP",
                         "-h127.0.0.1", "-P13306", "--raw", "--batch", "--skip-column-names"],
                        input_text=sql)

    def start_flink_mysql(self):
        for image in (FLINK_IMAGE, MYSQL_IMAGE):
            self.run(["docker", "pull", image], timeout=600)
        self.save("images", {image: json.loads(self.run(["docker", "image", "inspect", image,
                             "--format", "{{json .RepoDigests}} "])) for image in (FLINK_IMAGE, MYSQL_IMAGE)})
        self.mysql_name = self.container("mysql", MYSQL_IMAGE,
            ["--port=13306", "--bind-address=127.0.0.1", "--server-id=1", "--log-bin=mysql-bin",
             "--binlog-format=ROW", "--binlog-row-image=FULL"], {"MYSQL_ALLOW_EMPTY_PASSWORD": "yes"},
            memory="768m")
        wait_for("MySQL ready", lambda: self.mysql("SELECT 1;"))
        settings = self.mysql("SELECT @@version, @@binlog_format, @@binlog_row_image, @@log_bin;").strip()
        self.save("mysql-binlog", {"settings": settings})
        if settings.split("\t")[-3:] != ["ROW", "FULL", "1"]:
            raise AssertionError("MySQL must have ROW/FULL binary logging enabled")
        self.mysql("CREATE DATABASE flink_fixture; CREATE TABLE flink_fixture.person "
                   "(id INT PRIMARY KEY, name VARCHAR(50) NOT NULL, age INT NOT NULL, city VARCHAR(50)); "
                   "INSERT INTO flink_fixture.person VALUES (1,'one',21,'a'),(2,'two',22,'b'),(3,'three',23,NULL);")
        job = self.work / "job"
        (job / "lib").mkdir(parents=True)
        (job / "bin").mkdir()
        jar = Path(self.args.job_jar).resolve()
        destination = job / "lib" / jar.name
        try:
            os.link(jar, destination)
        except OSError:
            shutil.copyfile(jar, destination)
        with jar.open("rb") as stream:
            self.report["job_sha256"] = hashlib.file_digest(stream, "sha256").hexdigest()
        shutil.copyfile(Path(__file__).parents[2] / "static/bin/hugegraph-flinkcdc-loader.sh",
                        job / "bin/hugegraph-flinkcdc-loader.sh")
        mapping = {"version": "2.0", "structs": [{"id": "mysql", "input": {
            "type": "jdbc", "vendor": "mysql", "url": "jdbc:mysql://127.0.0.1:13306",
            "database": "flink_fixture", "table": "person", "username": "root", "password": "",
            "header": ["id", "name", "age", "city"]}, "vertices": [{"label": "cdc_person", "id": "id",
            "update_strategies": {"name": "OVERRIDE", "age": "OVERRIDE", "city": "OVERRIDE"}}]}]}
        (job / "mapping.json").write_text(json.dumps(mapping))
        checkpoints = self.work / "checkpoints"
        checkpoints.mkdir(mode=0o777)
        checkpoints.chmod(0o777)
        config = "\n".join(["jobmanager.rpc.address: 127.0.0.1", "jobmanager.rpc.port: 16123",
            "rest.address: 127.0.0.1", "rest.bind-address: 127.0.0.1", "rest.port: 18081",
            "blob.server.port: 16124", "taskmanager.bind-host: 127.0.0.1", "taskmanager.host: 127.0.0.1",
            "taskmanager.rpc.port: 16122", "taskmanager.numberOfTaskSlots: 1", "parallelism.default: 1",
            "jobmanager.memory.process.size: 1024m", "taskmanager.memory.process.size: 1536m",
            "state.checkpoints.dir: file:///checkpoints", "state.savepoints.dir: file:///checkpoints",
            "execution.checkpointing.externalized-checkpoint-retention: RETAIN_ON_CANCELLATION",
            "restart-strategy.type: fixed-delay", "restart-strategy.fixed-delay.attempts: 5",
            "restart-strategy.fixed-delay.delay: 1 s"])
        environment = {"FLINK_PROPERTIES": config, "JAVA_TOOL_OPTIONS": "-XX:ActiveProcessorCount=2"}
        shared = [str(checkpoints) + ":/checkpoints",
                  str(Path(self.args.jdbc_driver).resolve()) + ":/opt/flink/lib/mysql-connector-java-8.0.28.jar:ro"]
        self.jm = self.container("jm", FLINK_IMAGE, ["jobmanager"], environment,
                                 shared + [str(job) + ":/job:ro"])
        # Worker deliberately has no mapping-file or Loader JAR mount: Flink must ship the job.
        self.tm = self.container("tm", FLINK_IMAGE, ["taskmanager"], environment, shared, memory="1800m")
        wait_for("TaskManager registered", lambda: request(FLINK_URL + "/taskmanagers")["taskmanagers"])
        for name in (self.jm, self.tm):
            result = self.run(["docker", "exec", name, "sh", "-c", "java -version 2>&1"])
            if not re.search(r'version "17[.\"]', result):
                raise AssertionError("Flink JVM is not Java 17")
            self.phase(name.rsplit("-", 1)[-1] + "_java", result.strip())

    def submit(self, graph, savepoint=None):
        options = ["-d", "-p", "1"]
        if savepoint:
            options += ["-s", savepoint]
        output = self.run(["docker", "exec", self.jm, "bash", "/job/bin/hugegraph-flinkcdc-loader.sh"] +
            options + ["--", "--file", "/job/mapping.json", "--graph", graph, "--host", "127.0.0.1",
                       "--port", "38080", "--cdc-sink-parallelism", "1", "--cdc-flush-interval", "0"])
        match = re.search(r"JobID\s+([0-9a-f]{32})", output)
        if not match:
            raise AssertionError("Flink did not return a submitted JobID: " + redact(output))
        job = match.group(1)
        def running():
            state = request(FLINK_URL + "/jobs/" + job)["state"]
            if state in ("FAILED", "CANCELED", "FINISHED"):
                raise AssertionError("CDC job unexpectedly " + state)
            return state == "RUNNING"
        wait_for("job running", running)
        self.save("job-" + job, request(FLINK_URL + "/jobs/" + job))
        return job

    def source_rows(self):
        lines = self.mysql("SELECT JSON_OBJECT('id',id,'name',name,'age',age,'city',city) "
                           "FROM flink_fixture.person ORDER BY id;").splitlines()
        return normalize([json.loads(line) for line in lines])

    def graph_rows(self, graph):
        data = request(self.graph_url(graph) + "/graph/vertices?label=cdc_person&limit=100")
        return normalize(data["vertices"], graph=True)

    def compare(self, phase, graph, job):
        expected = self.source_rows()
        observed = {}

        def probe():
            nonlocal observed
            state = request(FLINK_URL + "/jobs/" + job)["state"]
            if state in ("FAILED", "CANCELED", "FINISHED"):
                raise AssertionError("CDC job unexpectedly " + state)
            observed = self.graph_rows(graph)
            return observed == expected

        try:
            wait_for(phase, probe)
        finally:
            self.save(phase + "-rows", {"expected": expected, "actual": observed, "job": job})
        self.phase(phase, {"job": job, "rows": len(expected)})

    def checkpoint(self, job, after=-1):
        def probe():
            state = request(FLINK_URL + "/jobs/" + job + "/checkpoints")
            completed = state.get("latest", {}).get("completed")
            return completed if completed and completed["id"] > after else None
        return wait_for("completed checkpoint", probe)

    def cancel(self, job):
        self.run(["docker", "exec", self.jm, "/opt/flink/bin/flink", "cancel", job])
        wait_for("job canceled", lambda: request(FLINK_URL + "/jobs/" + job)["state"] == "CANCELED")

    def verify(self):
        self.report["external_jdbc_driver"] = validate_jdbc_driver(self.args.jdbc_driver)
        manifest = validate_candidate_sdk(self.args.sdk_manifest, self.args.server_sha)
        self.save("candidate-sdk", manifest)
        with Path(self.args.sdk_manifest).open("rb") as stream:
            self.report["sdk_manifest_sha256"] = hashlib.file_digest(stream, "sha256").hexdigest()
        with Path(self.args.server_archive).open("rb") as stream:
            self.report["server_archive_sha256"] = hashlib.file_digest(stream, "sha256").hexdigest()
        if self.report["server_archive_sha256"] != self.args.server_archive_sha256:
            raise AssertionError("Server archive does not match the same-source SDK action output")
        for port in (38080, 18182, 13306, 18081, 16123, 16124, 16122):
            with socket.socket() as probe:
                probe.bind(("127.0.0.1", port))
        self.start_server()
        self.verify_identity_api()
        self.start_flink_mysql()
        job = self.submit(GRAPHS[0])
        self.compare("snapshot", GRAPHS[0], job)
        checkpoint = self.checkpoint(job)
        savepoint_output = self.run(["docker", "exec", self.jm, "/opt/flink/bin/flink", "savepoint", job,
                                    "file:///checkpoints"])
        match = re.search(r"(file:/+checkpoints/savepoint-[^\s]+)", savepoint_output)
        if not match:
            raise AssertionError("Flink did not return a savepoint path")
        savepoint = match.group(1).rstrip(".")
        self.phase("snapshot_checkpoint", {"checkpoint": checkpoint, "savepoint": savepoint})
        self.mysql("INSERT INTO flink_fixture.person VALUES(4,'four',24,'d'); "
                   "UPDATE flink_fixture.person SET name='updated',age=42,city=NULL WHERE id=2; "
                   "DELETE FROM flink_fixture.person WHERE id=1;")
        self.compare("changes_and_null", GRAPHS[0], job)
        previous = request(FLINK_URL + "/taskmanagers")["taskmanagers"][0]["id"]
        self.run(["docker", "restart", self.tm])
        self.mysql("INSERT INTO flink_fixture.person VALUES(5,'five',25,'e'); "
                   "UPDATE flink_fixture.person SET city='after-restart' WHERE id=3;")
        registered = wait_for("replacement TaskManager", lambda: [tm for tm in
            request(FLINK_URL + "/taskmanagers")["taskmanagers"] if tm["id"] != previous])
        self.compare("taskmanager_restart", GRAPHS[0], job)
        self.phase("restart_checkpoint", {"previous_taskmanager": previous, "replacement": registered,
                                           "checkpoint": self.checkpoint(job, checkpoint["id"])})
        # Debezium emits DELETE + CREATE for a changed database primary key.
        self.mysql("UPDATE flink_fixture.person SET id=30 WHERE id=3;")
        self.compare("primary_key_change", GRAPHS[0], job)
        # Force deterministic replay, including an already-applied delete, from the older savepoint.
        self.cancel(job)
        job = self.submit(GRAPHS[0], savepoint)
        # A new binlog marker prevents already-correct graph data from passing before replay catches up.
        self.mysql("UPDATE flink_fixture.person SET name='after-replay' WHERE id=5;")
        self.compare("savepoint_replay", GRAPHS[0], job)
        self.phase("replay_checkpoint", self.checkpoint(job))
        self.cancel(job)
        self.mysql("DELETE FROM flink_fixture.person WHERE id=2; "
                   "INSERT INTO flink_fixture.person VALUES(6,'six',26,NULL); "
                   "UPDATE flink_fixture.person SET name='during-downtime' WHERE id=4;")
        if self.graph_rows(GRAPHS[1]):
            raise AssertionError("Rebuild target must start empty")
        job = self.submit(GRAPHS[1])
        self.compare("fresh_target_rebuild", GRAPHS[1], job)
        self.phase("rebuild_checkpoint", self.checkpoint(job))
        self.cancel(job)
        self.report["status"] = "passed"

    def cleanup(self):
        for name in self.containers:
            owner = subprocess.run(["docker", "inspect", "--format",
                '{{index .Config.Labels "hugegraph.flink.acceptance"}}', name], text=True, capture_output=True)
            if owner.returncode or owner.stdout.strip() != self.prefix:
                continue
            logs = subprocess.run(["docker", "logs", "--tail", "500", name], text=True, capture_output=True)
            (self.evidence / (name.rsplit("-", 1)[-1] + ".log")).write_text(redact(logs.stdout + logs.stderr))
            subprocess.run(["docker", "rm", "-f", name], capture_output=True)
        if self.server_process and self.server_process.poll() is None:
            os.killpg(self.server_process.pid, signal.SIGTERM)
            try:
                self.server_process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                os.killpg(self.server_process.pid, signal.SIGKILL)
        if self.server_log:
            self.server_log.close()
            (self.evidence / "server.log").write_text(redact((self.work / "server-console.log").read_text()[-60000:]))
        self.save("result", self.report)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run", action="store_true", help="start test-owned Linux runtime resources")
    parser.add_argument("--server-archive")
    parser.add_argument("--server-archive-sha256", help="archive hash from the same-source SDK action")
    parser.add_argument("--sdk-manifest", help="manifest from the same-source SDK action")
    parser.add_argument("--maven-repo", help="isolated repository from the same-source SDK action, for the API probe")
    parser.add_argument("--job-jar")
    parser.add_argument("--jdbc-driver", help="external mysql-connector-java 8.0.28 JAR; never bundled in the job")
    parser.add_argument("--server-sha", default="unknown")
    parser.add_argument("--toolchain-sha", default="unknown")
    parser.add_argument("--work-root", default=tempfile.gettempdir())
    parser.add_argument("--evidence", default="flink-cdc-evidence")
    args = parser.parse_args()
    if not args.run:
        print(json.dumps({"mode": "plan", "images": [FLINK_IMAGE, MYSQL_IMAGE], "graphs": GRAPHS,
                          "phases": ["identity_api_probe", "snapshot", "changes_and_null", "taskmanager_restart", "primary_key_change",
                                     "savepoint_replay", "fresh_target_rebuild"]}, indent=2))
        return
    if sys.platform != "linux" or not all((args.server_archive, args.server_archive_sha256,
                                           args.sdk_manifest, args.maven_repo, args.job_jar, args.jdbc_driver)):
        parser.error("--run requires Linux, --server-archive, --server-archive-sha256, "
                     "--sdk-manifest, --maven-repo, --job-jar and --jdbc-driver")
    if not Path(args.maven_repo).is_dir():
        parser.error("--maven-repo must be an existing isolated repository")
    if not re.fullmatch(r"[0-9a-f]{64}", args.server_archive_sha256):
        parser.error("--run requires a full server archive SHA-256")
    if not all(re.fullmatch(r"[0-9a-fA-F]{40}", sha) for sha in (args.server_sha, args.toolchain_sha)):
        parser.error("--run requires full server and Toolchain commit SHAs")
    acceptance = Acceptance(args)
    try:
        acceptance.verify()
    except Exception as error:
        acceptance.report.update(status="failed", error=redact(str(error)))
        raise
    finally:
        acceptance.cleanup()


if __name__ == "__main__":
    main()
