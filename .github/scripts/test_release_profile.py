#!/usr/bin/env python3
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with this
# work for additional information regarding copyright ownership. The ASF
# licenses this file to You under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
"""Opt-in check of the apache-release profile as Maven merges it with the Apache parent."""

import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET

NS = "{http://maven.apache.org/POM/4.0.0}"
SOURCE_GOALS = {"jar", "jar-no-fork"}
TEST_SOURCE_GOALS = {"test-jar", "test-jar-no-fork"}


def executions(project, plugin):
    for node in project.findall(NS + "build/" + NS + "plugins/" + NS + "plugin"):
        if node.findtext(NS + "artifactId") == plugin:
            return [(execution.findtext(NS + "id"),
                     [goal.text for goal in execution.findall(NS + "goals/" + NS + "goal")])
                    for execution in node.findall(NS + "executions/" + NS + "execution")]
    return []


class ReleaseProfileTest(unittest.TestCase):
    @unittest.skipUnless(os.environ.get("RELEASE_PROFILE_MAVEN_TEST"),
                         "Set RELEASE_PROFILE_MAVEN_TEST to the mvn command for the release-profile check")
    def test_each_module_signs_and_attaches_once(self):
        # Merge the profile with Apache parent 23 the way a release build does, without running it
        with tempfile.TemporaryDirectory() as temporary:
            output = Path(temporary) / "effective-pom.xml"
            source = Path(__file__).resolve().parents[2]
            result = subprocess.run([os.environ["RELEASE_PROFILE_MAVEN_TEST"], "-f", str(source / "pom.xml"),
                                     "-ntp", "-B", "-Papache-release",
                                     "org.apache.maven.plugins:maven-help-plugin:3.2.0:effective-pom",
                                     "-Doutput=" + str(output)], capture_output=True, text=True, timeout=300)
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)
            projects = ET.parse(output).getroot().findall(NS + "project")
            self.assertTrue(projects)

            for project in projects:
                module = project.findtext(NS + "artifactId")
                with self.subTest(module=module):
                    signing = [goals for _, goals in executions(project, "maven-gpg-plugin") if "sign" in goals]
                    self.assertEqual(1, len(signing), "gpg:sign executions")

                    sources = [goals for _, goals in executions(project, "maven-source-plugin")
                               if SOURCE_GOALS & set(goals)]
                    self.assertEqual(1, len(sources), "source jar producers")
                    self.assertEqual(1, len(SOURCE_GOALS & set(sources[0])), "source jar goals")

                    javadocs = [goals for _, goals in executions(project, "maven-javadoc-plugin") if "jar" in goals]
                    self.assertEqual(1, len(javadocs), "javadoc jar producers")

                    test_sources = [goals for _, goals in executions(project, "maven-source-plugin")
                                    if TEST_SOURCE_GOALS & set(goals)]
                    expected = 1 if module == "hugegraph-tools" else 0
                    self.assertEqual(expected, len(test_sources), "test source jar producers")
                    for goals in test_sources:
                        self.assertEqual(1, len(TEST_SOURCE_GOALS & set(goals)), "test source jar goals")


if __name__ == "__main__":
    unittest.main()
