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
"""Run the real POM cleanup and UI copy targets against generated and runtime files."""

import argparse
import copy
from pathlib import Path
import subprocess
import tempfile
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--maven-repo", type=Path, required=True)
    args = parser.parse_args()
    source = Path(__file__).resolve().parents[2] / "pom.xml"
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    tree = ET.parse(source)
    execution = next(element for element in tree.findall(".//m:execution", namespace)
                     if element.findtext("m:id", namespaces=namespace) == "clear-old-distribution-outputs")
    execution = copy.deepcopy(execution)
    for element in execution.iter():
        element.tag = element.tag.rsplit("}", 1)[-1]
    archive = next(element for element in tree.findall(".//m:execution", namespace)
                   if element.findtext("m:id", namespaces=namespace) == "archive-and-audit-hubble")
    ui_copy = copy.deepcopy(archive.find("m:configuration/m:target/m:copy", namespace))
    for element in ui_copy.iter():
        element.tag = element.tag.rsplit("}", 1)[-1]
    with tempfile.TemporaryDirectory(prefix="hubble-release-cleanup-") as directory:
        base = Path(directory)
        release = base / "release"
        generated_dirs = ("bin", "conf", "lib", "ui", "licenses")
        metadata = ("LICENSE", "NOTICE", "README.md", "candidate-sdk-manifest.json")
        for name in generated_dirs:
            output = release / name / "old-subdirectory"
            output.mkdir(parents=True)
            (output / "removed-file").write_text("stale generated content")
        for name in metadata:
            (release / name).write_text("old release metadata")
        protected = {"data/metadata.mv.db": b"database bytes", "logs/hubble.log": b"runtime log",
                     "upload-files/input.csv": b"source data", "operator-notes.md": b"operator notes"}
        for name, contents in protected.items():
            file = release / name
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_bytes(contents)
        frontend = base / "hubble-fe"
        assets = {"index.html": b"<html>diagnostic build</html>", "assets/app.js": b"application code"}
        source_maps = {"app.js.map": b"root source map", "assets/app.js.map": b"nested source map"}
        for name, contents in {**assets, **source_maps}.items():
            file = frontend / "build" / name
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_bytes(contents)
        project = ET.Element("project")
        for name, value in (("modelVersion", "4.0.0"), ("groupId", "hubble.build.fixture"),
                            ("artifactId", "release-cleanup"), ("version", "1")):
            ET.SubElement(project, name).text = value
        properties = ET.SubElement(project, "properties")
        ET.SubElement(properties, "top.level.dir").text = str(base)
        ET.SubElement(properties, "final.name").text = "release"
        ET.SubElement(properties, "hubble-fe.dir").text = str(frontend)
        plugin = ET.SubElement(ET.SubElement(ET.SubElement(project, "build"), "plugins"), "plugin")
        for name, value in (("groupId", "org.apache.maven.plugins"), ("artifactId", "maven-antrun-plugin"),
                            ("version", "1.8")):
            ET.SubElement(plugin, name).text = value
        executions = ET.SubElement(plugin, "executions")
        executions.append(execution)
        copy_execution = ET.SubElement(executions, "execution")
        ET.SubElement(copy_execution, "id").text = "copy-diagnostic-ui"
        ET.SubElement(ET.SubElement(copy_execution, "goals"), "goal").text = "run"
        ET.SubElement(ET.SubElement(copy_execution, "configuration"), "target").append(ui_copy)
        pom = base / "pom.xml"
        ET.ElementTree(project).write(pom, encoding="utf-8", xml_declaration=True)
        command = ["mvn", "-o", "-f", str(pom), "-Dmaven.repo.local=" + str(args.maven_repo),
                   "org.apache.maven.plugins:maven-antrun-plugin:1.8:run@clear-old-distribution-outputs", "-ntp"]
        for repetition in range(2):
            result = subprocess.run(command, capture_output=True, text=True)
            if result.returncode:
                raise RuntimeError(result.stdout + result.stderr)
            assert all(not (release / name).exists() for name in generated_dirs + metadata), repetition
            assert all((release / name).read_bytes() == contents for name, contents in protected.items()), repetition
        print("Real Ant cleanup is idempotent; stale generated files removed, database/logs/uploads/notes preserved.")
        command[-2] = "org.apache.maven.plugins:maven-antrun-plugin:1.8:run@copy-diagnostic-ui"
        result = subprocess.run(command, capture_output=True, text=True)
        if result.returncode:
            raise RuntimeError(result.stdout + result.stderr)
        assert all((release / "ui" / name).read_bytes() == contents for name, contents in assets.items())
        assert not list((release / "ui").rglob("*.map")), "Diagnostic source maps leaked into the release UI"
        assert all((frontend / "build" / name).read_bytes() == contents for name, contents in source_maps.items())
        print("Real Ant UI copy retains runtime assets, excludes source maps, and preserves diagnostic build maps.")


if __name__ == "__main__":
    main()
