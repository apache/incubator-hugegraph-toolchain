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

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


class LoaderLauncherTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="loader-java-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name).resolve()
        self.tools = self.root / "tools"
        self.tools.mkdir()
        for name in ("dirname", "readlink", "find", "sort", "tr", "awk"):
            executable = shutil.which(name)
            self.assertIsNotNone(executable)
            (self.tools / name).symlink_to(executable)
        self.bash = shutil.which("bash")
        self.app = self.root / "loader"
        for name in ("bin", "lib", "conf"):
            (self.app / name).mkdir(parents=True)
        source = Path(__file__).resolve().parents[3] / "assembly/static/bin/hugegraph-loader.sh"
        self.launcher = self.app / "bin/hugegraph-loader.sh"
        shutil.copyfile(source, self.launcher)
        self.record = self.root / "java-arguments.txt"

    def run_launcher(self, java_home=None, arguments=None, extra_env=None, shell=None):
        env = dict(os.environ, PATH=str(self.tools), JAVA_RECORD=str(self.record),
                   JVM_OPTS="", CLASSPATH="")
        env.update(extra_env or {})
        env.pop("JAVA_HOME", None)
        env.pop("JAVA", None)
        if java_home is not None:
            env["JAVA_HOME"] = str(java_home)
        command = [shell or self.bash, str(self.launcher),
                   *(arguments if arguments is not None else ["--help"])]
        return subprocess.run(command, env=env, cwd=self.root, text=True, stdout=subprocess.PIPE,
                              stderr=subprocess.PIPE, timeout=10)

    def install_java(self, directory, major):
        directory.mkdir(parents=True, exist_ok=True)
        java = directory / "java"
        java.write_text("#!/bin/bash\n"
                        "if [[ $1 == -version ]]; then\n"
                        f"  printf 'openjdk version \"{major}.0.1\"\\n' >&2\n"
                        "  exit 0\n"
                        "fi\n"
                        "printf '%s\\0' \"$@\" > \"$JAVA_RECORD\"\n")
        java.chmod(0o755)

    def read_arguments(self):
        return self.record.read_bytes().decode().split("\0")[:-1]

    def test_missing_java_on_path_has_actionable_diagnostic(self):
        result = self.run_launcher()
        self.assertEqual(1, result.returncode)
        self.assertIn("Unable to find java executable", result.stderr)
        self.assertIn("JAVA_HOME and PATH", result.stderr)
        self.assertFalse(self.record.exists())

    def test_invalid_java_home_has_actionable_diagnostic(self):
        result = self.run_launcher(self.root / "missing-jdk")
        self.assertEqual(1, result.returncode)
        self.assertIn("Unable to find java executable", result.stderr)
        self.assertFalse(self.record.exists())

    def test_path_java_11_preserves_launch_without_module_opening(self):
        self.install_java(self.tools, 11)
        result = self.run_launcher()
        self.assertEqual(0, result.returncode, result.stderr)
        arguments = self.read_arguments()
        self.assertNotIn("--add-opens=java.base/java.net=ALL-UNNAMED", arguments)
        self.assertIn("org.apache.hugegraph.loader.HugeGraphLoader", arguments)

    def test_java_home_17_preserves_module_opening(self):
        home = self.root / "jdk"
        self.install_java(home / "bin", 17)
        result = self.run_launcher(home)
        self.assertEqual(0, result.returncode, result.stderr)
        arguments = self.read_arguments()
        self.assertIn("--add-opens=java.base/java.net=ALL-UNNAMED", arguments)
        self.assertIn("org.apache.hugegraph.loader.HugeGraphLoader", arguments)

    def test_application_arguments_keep_boundaries_and_literals(self):
        self.install_java(self.tools, 17)
        for name in ("data-a.csv", "data-b.csv"):
            (self.root / name).touch()
        expected = ["--file", "mapping with spaces.json", "--password", "",
                    "--file", "data-*.csv", "--token", "fake token with spaces"]
        for shell in (self.bash, shutil.which("sh")):
            with self.subTest(shell=shell):
                result = self.run_launcher(arguments=expected, shell=shell)
                self.assertEqual(0, result.returncode, result.stderr)
                arguments = self.read_arguments()
                main = arguments.index("org.apache.hugegraph.loader.HugeGraphLoader")
                self.assertEqual(expected, arguments[main + 1:])

    def test_release_paths_with_spaces_preserve_properties_and_classpath(self):
        self.app = self.app.rename(self.root / "loader release with spaces")
        self.launcher = self.app / "bin/hugegraph-loader.sh"
        home = self.root / "java home with spaces"
        self.install_java(home / "bin", 17)
        included = ["log4j-slf4j-impl-fixture.jar", "hugegraph-client.jar",
                    "dependency with spaces.jar"]
        shaded = "apache-hugegraph-loader-fixture-shaded.jar"
        for name in included + [shaded]:
            (self.app / "lib" / name).touch()
        for shell in (self.bash, shutil.which("sh")):
            with self.subTest(shell=shell):
                result = self.run_launcher(home, shell=shell)
                self.assertEqual(0, result.returncode, result.stderr)
                arguments = self.read_arguments()
                self.assertIn("-Dloader.home.path=" + str(self.app), arguments)
                self.assertIn("-Dlog4j.configurationFile=" + str(self.app / "conf/log4j2.xml"), arguments)
                self.assertIn("-Djava.library.path=" + str(self.app / "native"), arguments)
                cp = arguments.index("-cp")
                self.assertEqual("org.apache.hugegraph.loader.HugeGraphLoader", arguments[cp + 2])
                classpath = arguments[cp + 1].split(":")
                expected = [str(self.app / "lib" / name) for name in included]
                self.assertEqual(expected, [entry for entry in classpath if entry])
                self.assertNotIn(str(self.app / "lib" / shaded), classpath)

    def test_external_classpath_with_spaces_stays_one_argument(self):
        self.install_java(self.tools, 17)
        external = str(self.root / "external jars/custom.jar")
        result = self.run_launcher(extra_env={"CLASSPATH": external,
                                             "JVM_OPTS": "-Xms16m -Dprobe=value"})
        self.assertEqual(0, result.returncode, result.stderr)
        arguments = self.read_arguments()
        self.assertIn("-Xms16m", arguments)
        self.assertIn("-Dprobe=value", arguments)
        cp = arguments.index("-cp")
        self.assertTrue(arguments[cp + 1].startswith(external + ":"), arguments)
        self.assertEqual("org.apache.hugegraph.loader.HugeGraphLoader", arguments[cp + 2])

    def test_default_max_heap_is_preserved(self):
        self.install_java(self.tools, 17)
        result = self.run_launcher()
        self.assertEqual(0, result.returncode, result.stderr)
        heaps = [arg for arg in self.read_arguments() if arg.startswith("-Xmx")]
        self.assertEqual(["-Xmx10g"], heaps)

    def test_caller_max_heap_takes_precedence(self):
        for major in (11, 17):
            self.install_java(self.tools, major)
            for shell in (self.bash, shutil.which("sh")):
                with self.subTest(major=major, shell=shell):
                    result = self.run_launcher(extra_env={"JVM_OPTS": "-Xms16m -Xmx128m"},
                                               shell=shell)
                    self.assertEqual(0, result.returncode, result.stderr)
                    arguments = self.read_arguments()
                    heaps = [arg for arg in arguments if arg.startswith("-Xmx")]
                    self.assertEqual(["-Xmx10g", "-Xmx128m"], heaps)
                    self.assertIn("-Xms16m", arguments)

    def test_last_caller_max_heap_remains_effective(self):
        self.install_java(self.tools, 17)
        result = self.run_launcher(extra_env={"JVM_OPTS": "-Xmx128m -Xmx256m"})
        self.assertEqual(0, result.returncode, result.stderr)
        heaps = [arg for arg in self.read_arguments() if arg.startswith("-Xmx")]
        self.assertEqual(["-Xmx10g", "-Xmx128m", "-Xmx256m"], heaps)


if __name__ == "__main__":
    unittest.main()
