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
"""Exercise distribution gates without requiring a JDK or a Hubble build."""

import hashlib
import io
import json
import os
import pathlib
import shutil
import subprocess
import tarfile
import tempfile
import unittest
import zipfile


CHECKER = pathlib.Path(__file__).with_name("check-hubble-dist.sh")
BASH = shutil.which("bash")


class HubbleDistributionAuditTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = pathlib.Path(self.temp.name)
        self.root = self.base / "apache-hugegraph-hubble-test"
        for directory in ("bin", "conf", "lib", "ui", "licenses/fe-licenses"):
            (self.root / directory).mkdir(parents=True, exist_ok=True)
        for filename in ("bin/start-hubble.sh", "bin/stop-hubble.sh",
                         "bin/common_functions", "conf/hugegraph-hubble.properties",
                         "ui/index.html", "README.md", "LICENSE", "NOTICE",
                         "licenses/dependency.txt", "licenses/fe-licenses/dependency.txt"):
            (self.root / filename).write_text("audit fixture\n", encoding="utf-8")
        self.jar = self.root / "lib/dependency.jar"
        self.archive = self.base / "hubble.tar.gz"
        self.report = self.base / "report.json"

    def run_audit(self, env=None, options=(), extra_members=()):
        with tarfile.open(self.archive, "w:gz") as archive:
            archive.add(self.root, arcname=self.root.name)
            for member in extra_members:
                archive.addfile(member, io.BytesIO(b"x") if member.isfile() else None)
        return subprocess.run(
            [BASH, str(CHECKER), str(self.archive), "--json-output", str(self.report), *options],
            env=env, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
            check=False,
        )

    def valid_jar(self, path=None):
        with zipfile.ZipFile(path or self.jar, "w") as jar:
            jar.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n")
            jar.writestr("native/library.so", b"inventory fixture")
            for index in range(2048):
                jar.writestr(f"resources/entry-{index}.txt", "fixture")

    def audit_environment(self, include_python=True):
        tools = self.base / "tools"
        tools.mkdir()
        names = ["mktemp", "tar", "gzip", "grep", "awk", "sort", "wc",
                 "tr", "rm", "mkdir", "dirname", "sed"]
        if include_python:
            names.append("python3")
        for name in names:
            executable = shutil.which(name)
            self.assertIsNotNone(executable, name)
            (tools / name).symlink_to(executable)
        env = os.environ.copy()
        env["PATH"] = str(tools)
        return env

    def assert_rejected(self, message, **kwargs):
        result = self.run_audit(**kwargs)
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertIn(message, result.stderr)
        self.assertFalse(self.report.exists())

    def test_missing_python_cannot_report_success(self):
        self.valid_jar()
        self.assert_rejected("python3 is required", env=self.audit_environment(False))

    def test_all_jars_are_inspected_without_starting_jdk(self):
        self.valid_jar()
        for name in ("a.jar", "z.jar"):
            self.valid_jar(self.root / "lib" / name)
        env = self.audit_environment()
        marker = self.base / "jar-invoked"
        fake_jar = self.base / "tools/jar"
        fake_jar.write_text(f'#!/bin/sh\ntouch "{marker}"\nexit 87\n')
        fake_jar.chmod(0o755)
        result = self.run_audit(env)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse(marker.exists())
        report = json.loads(self.report.read_text())
        self.assertEqual(report["jar_count"], 3)
        self.assertEqual(report["native_jar_count"], 3)

    def test_corrupt_jar_cannot_report_success(self):
        self.jar.write_bytes(b"not a zip archive")
        result = self.run_audit()
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertIn("Unable to inspect packaged JAR", result.stderr)
        self.assertFalse(self.report.exists())

    def test_readable_jar_records_native_entries(self):
        self.valid_jar()
        result = self.run_audit()
        self.assertEqual(result.returncode, 0, result.stderr)
        report = json.loads(self.report.read_text())
        self.assertEqual(report["native_jar_count"], 1)
        self.assertEqual(report["native_jars"], [self.root.name + "/lib/dependency.jar"])

    def test_truncated_zip_directory_cannot_report_success(self):
        self.valid_jar()
        self.jar.write_bytes(self.jar.read_bytes()[:-40])
        self.assert_rejected("Unable to inspect packaged JAR")

    def test_invalid_jar_after_valid_jar_is_not_skipped(self):
        self.valid_jar()
        (self.root / "lib/z-broken.jar").write_bytes(b"not a jar")
        self.assert_rejected("Unable to inspect packaged JAR")

    def test_non_native_jars_are_not_classified_as_native(self):
        self.valid_jar()
        with zipfile.ZipFile(self.jar, "w") as jar:
            jar.writestr("Native.class", b"fixture")
            jar.writestr("docs/not-a-library.so.txt", b"fixture")
        result = self.run_audit()
        self.assertEqual(result.returncode, 0, result.stderr)
        report = json.loads(self.report.read_text())
        self.assertEqual(report["native_jar_count"], 0)
        self.assertEqual(report["native_jars"], [])

    def test_all_native_suffixes_are_recorded_once_per_jar(self):
        for index, suffix in enumerate(("so", "dylib", "dll", "jnilib")):
            path = self.jar if index == 0 else self.root / f"lib/native-{index}.jar"
            with zipfile.ZipFile(path, "w") as jar:
                jar.writestr(f"native/library.{suffix}", b"fixture")
                jar.writestr(f"native/another.{suffix}", b"fixture")
        result = self.run_audit()
        self.assertEqual(result.returncode, 0, result.stderr)
        report = json.loads(self.report.read_text())
        self.assertEqual(report["jar_count"], 4)
        self.assertEqual(report["native_jar_count"], 4)

    def test_category_x_dependency_is_rejected(self):
        self.valid_jar()
        self.jar.rename(self.root / "lib/truelicense-core-1.0.jar")
        self.assert_rejected("ASF Category-X dependency must not be packaged")

    def test_missing_dependency_and_frontend_licenses_are_rejected(self):
        self.valid_jar()
        (self.root / "licenses/dependency.txt").unlink()
        self.assert_rejected("No dependency license files found")
        (self.root / "licenses/dependency.txt").write_text("fixture")
        (self.root / "licenses/fe-licenses/dependency.txt").unlink()
        self.assert_rejected("No frontend license files found")

    def test_empty_legal_file_is_rejected(self):
        self.valid_jar()
        (self.root / "LICENSE").write_text(" \n\t")
        self.assert_rejected("Packaged LICENSE must not be empty")

    def test_unsafe_paths_are_rejected_before_extraction(self):
        self.valid_jar()
        for name in ("../outside", "/tmp/hubble-audit-absolute"):
            with self.subTest(name=name):
                member = tarfile.TarInfo(name)
                member.size = 1
                self.assert_rejected("Unsafe tarball path found", extra_members=(member,))
        self.assertFalse((self.base / "outside").exists())

    def test_links_are_rejected(self):
        self.valid_jar()
        for kind in (tarfile.SYMTYPE, tarfile.LNKTYPE):
            with self.subTest(kind=kind):
                member = tarfile.TarInfo(f"{self.root.name}/link")
                member.type = kind
                member.linkname = "lib/dependency.jar"
                self.assert_rejected("Tarball must not contain symbolic or hard links",
                                     extra_members=(member,))

    def test_runtime_and_source_map_residue_are_rejected(self):
        self.valid_jar()
        for relative, message in (
            ("logs/application.log", "Runtime residue must not be packaged"),
            ("lib/node_modules/package.json", "Blocked runtime/development artifact"),
            ("metadata.lock", "Blocked runtime/development file"),
            ("ui/main.js.map", "Frontend source maps must not be packaged"),
            ("lib/native.so", "Unknown outer binary/archive files found"),
        ):
            with self.subTest(relative=relative):
                path = self.root / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("fixture")
                self.assert_rejected(message)
                path.unlink()
                # The empty logs directory is also prohibited.
                if relative.startswith(("logs/", "lib/node_modules/")):
                    path.parent.rmdir()

    def test_required_sidecars_are_not_bypassed(self):
        self.valid_jar()
        self.assert_rejected("Missing required SHA-512 sidecar",
                             options=("--require-sidecars",))

    def test_bad_checksum_is_not_bypassed(self):
        self.valid_jar()
        pathlib.Path(str(self.archive) + ".sha512").write_text("bad-checksum")
        self.assert_rejected("SHA-512 checksum mismatch")

    def test_valid_checksum_is_reported(self):
        self.valid_jar()
        self.run_audit()
        digest = hashlib.sha512(self.archive.read_bytes()).hexdigest()
        pathlib.Path(str(self.archive) + ".sha512").write_text(digest + "  hubble.tar.gz\n")
        # Repacking can change gzip timestamps, so validate the exact saved input.
        result = subprocess.run(
            [BASH, str(CHECKER), str(self.archive), "--json-output", str(self.report)],
            text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(self.report.read_text())["checksum_status"], "passed")


if __name__ == "__main__":
    unittest.main()
