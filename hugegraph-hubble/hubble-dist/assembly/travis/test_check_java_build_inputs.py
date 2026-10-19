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
"""Exercise external-input invalidation without invoking a second compiler."""

import importlib.util
import json
import os
from pathlib import Path
import tempfile
import subprocess
import sys
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).with_name("check-java-build-inputs.py")
SPEC = importlib.util.spec_from_file_location("java_inputs", SCRIPT)
guard = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(guard)


class JavaInputTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.project = self.root / "hugegraph-hubble/hubble-be"
        self.project.mkdir(parents=True)
        for directory in (self.project, self.project.parent, self.root):
            (directory / "pom.xml").write_text("<project/>")
        self.jar = self.root / "dependency.jar"
        self.jar.write_bytes(b"version one")
        self.sdk = {"manifest": "same-source-fixture", "jars": {"sdk.jar": "one"}}
        self.java = ["/jdk17", "17.0.20+1"]

    def identity(self, classpath=None, sdk=None, java=None, arguments=()):
        return guard.input_identity(self.project, classpath or [self.jar], sdk or self.sdk,
                                    java or self.java, arguments)

    def test_only_external_inputs_invalidate_and_native_outputs_are_retained_when_unchanged(self):
        identity = self.identity()
        self.assertTrue(guard.invalidate(self.project, identity))
        target = self.project / "target"
        for directory in guard.OUTPUTS:
            (target / directory).mkdir()
            (target / directory / "output").write_text("native")
        (target / "hubble-be.jar").write_text("previous package")
        self.assertFalse(guard.invalidate(self.project, self.identity([self.jar, target / "classes"])))
        (self.project / "src").mkdir()
        source = self.project / "src/Changed.java"
        source.write_text("source is handled by native Maven")
        self.assertEqual(self.identity(), identity)
        source.unlink()
        self.assertEqual(self.identity(), identity)
        timestamp = self.jar.stat().st_mtime_ns
        self.jar.write_bytes(b"version two")
        os.utime(self.jar, ns=(timestamp, timestamp))
        changed = self.identity()
        self.assertNotEqual(changed, identity)
        self.assertTrue(guard.invalidate(self.project, changed))
        self.assertTrue(all(not (target / directory).exists() for directory in guard.OUTPUTS))
        self.assertEqual((target / "hubble-be.jar").read_text(), "previous package")
        self.assertFalse(guard.invalidate(self.project, changed))

    def test_manifest_jdk_poms_and_compiler_parameters_change_identity(self):
        identity = self.identity()
        self.assertNotEqual(self.identity(sdk={"manifest": "new-source", "jars": self.sdk["jars"]}), identity)
        self.assertNotEqual(self.identity(java=["/jdk17", "17.0.21+1"]), identity)
        self.assertNotEqual(self.identity(arguments=["-Dmaven.compiler.release=21"]), identity)
        (self.project.parent / "pom.xml").write_text("<project>changed</project>")
        self.assertNotEqual(self.identity(), identity)

    def test_reactor_classes_and_classpath_additions_deletions_are_external_inputs(self):
        reactor = self.root / "hugegraph-client/target/classes"
        reactor.mkdir(parents=True)
        file = reactor / "Client.class"
        file.write_bytes(b"one")
        first = self.identity([self.jar, reactor])
        self.assertNotEqual(first, self.identity())
        file.write_bytes(b"two")
        self.assertNotEqual(first, self.identity([self.jar, reactor]))
        (reactor / "Removed.class").write_bytes(b"three")
        added = self.identity([self.jar, reactor])
        (reactor / "Removed.class").unlink()
        self.assertNotEqual(added, self.identity([self.jar, reactor]))
        self.jar.unlink()
        with self.assertRaisesRegex(RuntimeError, "Missing resolved"):
            self.identity()

    def test_compiler_arguments_ignore_execution_only_flags_and_keep_profile_and_model_changes(self):
        arguments = guard.compiler_arguments("package -DskipTests -Dmaven.javadoc.skip=true -P unit-test "
                                             "-Dmaven.compiler.release=17 -D revision=2")
        self.assertEqual(arguments, ["-Dmaven.compiler.release=17", "-Drevision=2", "-Punit-test"])
        self.assertEqual(guard.compiler_arguments("-Xmx3g -Dmaven.compiler.release=17"),
                         ["-Dmaven.compiler.release=17"])
        self.assertEqual(guard.compiler_arguments(
            "compile -DskipTests=false -Dmaven.compiler.showCompilationChanges=true"), [])
        self.assertEqual(guard.compiler_arguments(
            "test -D test=OneTest -Dit.test=OtherIT -Dgroups=fast -DexcludedGroups=slow -DskipITs "
            "-Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.rerunFailingTestsCount=1 "
            "-Dmaven.surefire.plugin.version=3.5.6 -Dmaven.compiler.testRelease=17 -Drevision=2"),
            ["-Dmaven.compiler.testRelease=17", "-Dmaven.surefire.plugin.version=3.5.6", "-Drevision=2"])

    def test_unsupported_python_reports_requirement_before_parsing_or_touching_inputs(self):
        with patch.object(guard.sys, "version_info", (3, 8, 20)), \
                patch.object(guard, "sdk_identity") as sdk:
            with self.assertRaisesRegex(RuntimeError, "requires Python 3.9 or newer"):
                guard.main()
        sdk.assert_not_called()

    def test_bad_stamp_invalidates_and_symlink_target_is_never_traversed(self):
        target = self.project / "target"
        target.mkdir()
        (target / "external-build-inputs.json").write_text("invalid json")
        self.assertTrue(guard.invalidate(self.project, self.identity()))
        outside = self.root / "outside"
        outside.mkdir()
        (outside / "classes").mkdir()
        import shutil
        shutil.rmtree(target)
        target.symlink_to(outside, target_is_directory=True)
        with self.assertRaisesRegex(RuntimeError, "symlinked"):
            guard.invalidate(self.project, self.identity())
        self.assertTrue((outside / "classes").is_dir())


class SdkIdentityTest(unittest.TestCase):
    setUp = JavaInputTest.setUp
    identity = JavaInputTest.identity

    def candidate_fixture(self):
        verifier_file = SCRIPT.resolve().parents[4] / \
            "hugegraph-client/assembly/travis/verify_candidate_image_sdk.py"
        spec = importlib.util.spec_from_file_location("sdk_fixture", verifier_file)
        sdk = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(sdk)
        repository = self.root / "m2"
        modules, artifacts = [], []
        for source in sorted(sdk.REQUIRED_MODULES):
            artifact = Path(source).parent.name if source != "pom.xml" else "hugegraph"
            parent = source in {"pom.xml", "hugegraph-commons/pom.xml", "hugegraph-server/pom.xml",
                                "hugegraph-pd/pom.xml", "hugegraph-store/pom.xml"}
            files = []
            for extension in (["pom"] if parent else ["pom", "jar"]):
                name = f"{artifact}-1.8.0.{extension}"
                file = repository / "org/apache/hugegraph" / artifact / "1.8.0" / name
                file.parent.mkdir(parents=True, exist_ok=True)
                file.write_text(name)
                with (file.parent / "_remote.repositories").open("a") as origins:
                    origins.write(name + ">=\n")
                relative = file.relative_to(repository).as_posix()
                files.append(relative)
                artifacts.append({"path": relative, "sha256": guard.digest(file), "source_reactor_install": True})
            modules.append({"source_pom": source, "artifact_id": artifact, "group_id": "org.apache.hugegraph",
                            "version": "1.8.0", "packaging": "pom" if parent else "jar", "files": files})
        manifest = {"repository": sdk.REPOSITORY, "commit": sdk.source_commit(), "source_revision": "1.8.0",
                    "java_version": "17", "required_sdk_modules": modules, "artifacts": artifacts}
        (repository / "candidate-sdk-manifest.json").write_text(json.dumps(manifest))
        return repository, manifest

    def test_candidate_manifest_changes_invalidate_and_tampered_sdk_fails_before_touching_outputs(self):
        repository, manifest = self.candidate_fixture()
        before = guard.sdk_identity(repository, "candidate", "1.8.0")
        first = self.identity(sdk=before)
        manifest["test_fixture_metadata"] = "manifest changed without modifying source identity"
        (repository / "candidate-sdk-manifest.json").write_text(json.dumps(manifest))
        after = guard.sdk_identity(repository, "candidate", "1.8.0")
        self.assertNotEqual(first, self.identity(sdk=after))
        guard.invalidate(self.project, first)
        classes = self.project / "target/classes"
        classes.mkdir()
        (classes / "Retained.class").write_bytes(b"not deleted by failed SDK verification")
        jar = next(repository.rglob("*.jar"))
        jar.write_bytes(b"tampered")
        result = subprocess.run([sys.executable, str(SCRIPT), "--project", str(self.project),
                                 "--repository", str(repository), "--sdk-mode", "candidate",
                                 "--sdk-version", "1.8.0", "--java-home", "/jdk17",
                                 "--java-version", "17.0.20", "--classpath", str(self.jar)],
                                capture_output=True, text=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertTrue((classes / "Retained.class").is_file())

    def test_guard_cli_retains_classes_for_test_selection_but_invalidates_compiler_and_model_changes(self):
        repository, _ = self.candidate_fixture()
        command = [sys.executable, str(SCRIPT), "--project", str(self.project),
                   "--repository", str(repository), "--sdk-mode", "candidate", "--sdk-version", "1.8.0",
                   "--java-home", "/jdk17", "--java-version", "17.0.20", "--classpath", str(self.jar)]
        model = "-Dmaven.compiler.release=17 -Drevision=1 -Punit-test"

        def run(arguments):
            environment = dict(os.environ, MAVEN_CMD_LINE_ARGS=arguments, MAVEN_OPTS="")
            result = subprocess.run(command, env=environment, capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            return result.stdout

        self.assertIn("invalidated", run(model + " -Dtest=OneTest"))
        classes = self.project / "target/classes"
        classes.mkdir()
        retained = classes / "Retained.class"
        retained.write_bytes(b"native compiler output")
        for selection in ("-Dtest=OtherTest", "-D test=ThirdTest -Dit.test=OtherIT",
                          "-Dsurefire.includes=**/*Test.java -Dfailsafe.failIfNoSpecifiedTests=false",
                          "-Dgroups=fast -DexcludedGroups=slow -DskipITs"):
            with self.subTest(selection=selection):
                self.assertIn("keeping native compiler state", run(model + " " + selection))
                self.assertEqual(retained.read_bytes(), b"native compiler output")
        for changed in (model.replace("release=17", "release=21"),
                        model.replace("revision=1", "revision=2"),
                        model.replace("-Punit-test", "-Pother"),
                        model + " -Dmaven.surefire.plugin.version=3.5.7"):
            with self.subTest(model=changed):
                run(model)
                classes.mkdir(exist_ok=True)
                retained.write_bytes(b"native compiler output")
                self.assertIn("invalidated", run(changed + " -Dtest=OtherTest"))
                self.assertFalse(classes.exists())


if __name__ == "__main__":
    unittest.main()
