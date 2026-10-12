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

"""Exercise the packaged Spark Loader entry point against an isolated test graph."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import signal
import subprocess
import sys
from urllib.parse import urlsplit


def run(command, log, env, cwd, expected=0, timeout=120):
    with log.open("w") as output:
        process = subprocess.Popen(command, env=env, cwd=cwd, stdout=output,
                                   stderr=subprocess.STDOUT, start_new_session=True)
        try:
            code = process.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            try:
                os.killpg(process.pid, signal.SIGTERM)
            except ProcessLookupError:
                pass
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
                process.wait()
            raise RuntimeError(log.name + " timed out") from None
    if code != expected:
        raise RuntimeError(log.name + " exited " + str(code) + ", expected " + str(expected))


def redact(text, env):
    for key in ("HUGEGRAPH_TEST_USERNAME", "HUGEGRAPH_TEST_PASSWORD"):
        secret = env.get(key)
        if not secret:
            continue
        for value in {secret, json.dumps(secret)[1:-1]}:
            text = re.sub(r"(?<![A-Za-z0-9_])" + re.escape(value) + r"(?![A-Za-z0-9_])",
                          "<redacted>", text)
    return text


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--loader-home", type=Path, required=True)
    parser.add_argument("--spark-home", type=Path, required=True)
    parser.add_argument("--work-dir", type=Path, required=True)
    parser.add_argument("--evidence-dir", type=Path, required=True)
    parser.add_argument("--server-baseline", required=True)
    parser.add_argument("--auth-mode", choices=("basic", "anonymous"), default="basic")
    parser.add_argument("--backend", default="rocksdb")
    args = parser.parse_args()
    env = os.environ.copy()
    required = ["JAVA_HOME", "HUGEGRAPH_TEST_URL", "HUGEGRAPH_TEST_GRAPH"]
    if args.auth_mode == "basic":
        required.extend(("HUGEGRAPH_TEST_USERNAME", "HUGEGRAPH_TEST_PASSWORD"))
    env["HUGEGRAPH_TEST_AUTH_MODE"] = args.auth_mode
    for key in required:
        if not env.get(key):
            raise RuntimeError("Missing environment variable: " + key)
    endpoint = urlsplit(env["HUGEGRAPH_TEST_URL"])
    if endpoint.scheme not in ("http", "https") or not endpoint.hostname or endpoint.path not in ("", "/"):
        raise RuntimeError("HUGEGRAPH_TEST_URL must be an HTTP origin")
    if endpoint.username or endpoint.password or endpoint.query or endpoint.fragment:
        raise RuntimeError("Use separate authentication environment variables")
    home = args.loader_home.resolve()
    spark = args.spark_home.resolve()
    work = args.work_dir.resolve()
    evidence = args.evidence_dir.resolve()
    if work.exists() and any(work.iterdir()):
        raise RuntimeError("Use a new work directory for each artifact run")
    work.mkdir(parents=True, exist_ok=True)
    evidence.mkdir(parents=True, exist_ok=True)
    jars = list((home / "lib").glob("apache-hugegraph-loader-*-shaded.jar"))
    if len(jars) != 1:
        raise RuntimeError("Expected one packaged shaded application jar")
    jar = jars[0]
    digest = hashlib.sha256()
    with jar.open("rb") as source:
        for chunk in iter(lambda: source.read(1048576), b""):
            digest.update(chunk)
    env.update(SPARK_HOME=str(spark), SPARK_LOCAL_IP="127.0.0.1", SPARK_SCALA_VERSION="2.12",
               LANG="C", LC_ALL="C")
    env.pop("SPARK_PRINT_LAUNCH_COMMAND", None)
    java = Path(env["JAVA_HOME"]) / "bin/java"
    fixture_source = Path(__file__).resolve().parents[1] / "java/org/apache/hugegraph/loader/spark/SparkLoaderArtifactFixture.java"
    fixture_classes = work / "fixture-classes"
    fixture_classes.mkdir()
    try:
        run([str(java.with_name("javac")), "--release", "17", "-cp", str(jar), "-d", str(fixture_classes),
             str(fixture_source)], work / "fixture-compile.log", env, work)
    except Exception:
        (evidence / "fixture-compile.log").write_text(
            redact((work / "fixture-compile.log").read_text(errors="replace"), env), encoding="utf-8")
        raise

    def fixture(mode):
        run([str(java), "-Xmx256m", "-XX:ActiveProcessorCount=2", "-cp",
             str(fixture_classes) + os.pathsep + str(jar),
             "org.apache.hugegraph.loader.spark.SparkLoaderArtifactFixture", mode, str(work)],
            work / ("fixture-" + mode + ".log"), env, work, timeout=60)

    log4j = "-Dlog4j.configurationFile=" + (home / "conf/log4j2.xml").as_uri()
    base = [str(home / "bin/hugegraph-spark-loader.sh"), "--master", "local-cluster[1,1,512]",
            "--driver-memory", "512m", "--conf", "spark.executor.memory=512m",
            "--conf", "spark.driver.host=127.0.0.1", "--conf", "spark.driver.bindAddress=127.0.0.1",
            "--conf", "spark.ui.enabled=false", "--conf",
            "spark.executor.extraJavaOptions=-XX:ActiveProcessorCount=2 -XshowSettings:properties " + log4j]
    app = ["--host", endpoint.hostname, "--port", str(endpoint.port or (443 if endpoint.scheme == "https" else 80)),
           "--protocol", endpoint.scheme, "--graph", env["HUGEGRAPH_TEST_GRAPH"],
           "--sink-type", "true", "--batch-insert-threads", "1", "--single-insert-threads", "1",
           "--max-conn", "4", "--max-conn-per-route", "2"]
    if args.auth_mode == "basic":
        app += ["--username", env["HUGEGRAPH_TEST_USERNAME"], "--password", env["HUGEGRAPH_TEST_PASSWORD"]]
    previous = set((spark / "work").glob("app-*"))
    errors = []
    cleanup = False
    try:
        fixture("prepare")
        for phase in ("vertex", "edge"):
            classlog = work / (phase + "-classes.log")
            options = "-XX:ActiveProcessorCount=2 " + log4j + " " + shlex.quote(
                "-Xlog:class+load=info:file=" + str(classlog))
            run(base + ["--conf", "spark.driver.extraJavaOptions=" + options,
                        "--file", str(work / (phase + "-struct.json"))] + app,
                work / (phase + "-launcher.log"), env, work)
            loaded = [line for line in classlog.read_text().splitlines()
                      if "org.apache.hugegraph.loader.spark.HugeGraphSparkLoader source:" in line]
            if len(loaded) != 1 or jar.name not in loaded[0]:
                raise RuntimeError("Loader main was not loaded from the packaged shaded jar")
            fixture("verify-vertices" if phase == "vertex" else "verify-all")
        invalid = work / "invalid.json"
        invalid.write_text('{"vertices":', encoding="utf-8")
        run(base + ["--file", str(invalid)] + app, work / "failure-exit.log", env, work, expected=1)
        runtime_log = (work / "logs/hugegraph-loader.log").read_text()
        versions = re.findall(r"Running Spark version ([^\s]+)", runtime_log)
        if len(versions) < 2 or set(versions) != {"3.5.8"}:
            raise RuntimeError("Unexpected Spark runtime version")
        if len(re.findall(r"Spark Loader runtime: Java 17[^\n]*default charset US-ASCII", runtime_log)) != 2:
            raise RuntimeError("Missing Java 17 / US-ASCII driver evidence")
    except Exception as error:
        errors.append(redact(str(error), env))
    finally:
        if (work / "prefix.txt").exists():
            try:
                fixture("cleanup")
                cleanup = True
            except Exception as error:
                errors.append(redact(str(error), env))

    if not cleanup:
        errors.append("Fixture cleanup was not verified")
    applications = sorted(set((spark / "work").glob("app-*")) - previous)
    if len(applications) != 2:
        errors.append("Expected two independent-executor applications")
    for index, application in enumerate(applications):
        executors = list(application.glob("*/logs/hugegraph-loader.log"))
        if not executors:
            errors.append("Missing executor log for " + application.name)
        for executor_log in executors:
            text = executor_log.read_text(errors="replace")
            settings = (executor_log.parents[1] / "stderr").read_text(errors="replace")
            if "Starting executor ID 0" not in text or "Java version 17." not in text or jar.name not in text:
                errors.append("Missing Java 17 / shaded jar executor evidence")
            if not re.search(r"file.encoding = (US-ASCII|ANSI_X3.4-1968|ASCII)(?:\n|$)", settings):
                errors.append("Unexpected executor default charset")
            (evidence / ("executor-" + str(index) + ".log")).write_text(redact(text, env), encoding="utf-8")
            selected = "\n".join(line for line in settings.splitlines()
                                  if re.match(r"\s*(java.version|java.home|file.encoding) =", line))
            (evidence / ("executor-" + str(index) + "-jvm.log")).write_text(selected, encoding="utf-8")
    for path in list(work.glob("*.log")) + list((work / "logs").glob("*.log")):
        (evidence / path.name).write_text(redact(path.read_text(errors="replace"), env), encoding="utf-8")
    summary = {"server_baseline": args.server_baseline, "backend": args.backend,
               "auth_mode": args.auth_mode, "artifact_sha256": digest.hexdigest(),
               "source_commit": env.get("SOURCE_COMMIT"), "candidate_dependency_commit": env.get("SERVER_COMMIT"),
               "expected_vertices": 2, "expected_edges": 1,
               "vertices_verified": (work / "verify-vertices.json").exists(),
               "edges_and_unicode_id_verified": (work / "verify-all.json").exists(),
               "cleanup_verified": cleanup,
               "independent_applications": len(applications), "passed": not errors, "errors": errors}
    (evidence / "result.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    if errors:
        raise RuntimeError("; ".join(errors))
    print("Packaged Spark Loader passed: Java 17 driver/executors, UTF-8 data, vertex/edge readback, cleanup")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print("Spark Loader artifact check failed: " + str(error), file=sys.stderr)
        sys.exit(1)
