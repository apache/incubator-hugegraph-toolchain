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

import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import unittest


class SparkLauncherTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="spark launcher ")
        self.addCleanup(self.temp.cleanup)
        root = Path(self.temp.name)
        self.app = root / "loader package"
        self.lib = self.app / "lib"
        self.lib.mkdir(parents=True)
        (self.app / "bin").mkdir()
        source = Path(__file__).resolve().parents[3] / "assembly/static/bin"
        for name in ("get-params.sh", "hugegraph-spark-loader.sh"):
            shutil.copyfile(source / name, self.app / "bin" / name)
        self.shaded = self.lib / "apache-hugegraph-loader-test-shaded.jar"
        for path in (self.shaded, self.lib / "hugegraph-loader-test.jar",
                     self.lib / "dependency one.jar"):
            path.touch()
        self.spark = root / "spark home"
        (self.spark / "bin").mkdir(parents=True)
        submit = self.spark / "bin/spark-submit"
        submit.write_text("#!/usr/bin/env python3\nimport json, os, sys\n"
                          "with open(os.environ['SPARK_ARGV_FILE'], 'w') as output:\n"
                          "    json.dump(sys.argv[1:], output)\n")
        submit.chmod(0o755)
        self.argv = root / "argv.json"

    def run_launcher(self, *args):
        env = dict(os.environ, SPARK_HOME=str(self.spark), SPARK_ARGV_FILE=str(self.argv))
        return subprocess.run(["bash", str(self.app / "bin/hugegraph-spark-loader.sh"), *args],
                              env=env, text=True, stdout=subprocess.PIPE,
                              stderr=subprocess.PIPE, timeout=10)

    def arguments(self):
        return json.loads(self.argv.read_text())

    def load_option_parameters(self):
        source = Path(__file__).resolve().parents[2] / "main/java/org/apache/hugegraph/loader/executor/LoadOptions.java"
        text = source.read_text()
        parameters = re.findall(r"@Parameter\((.*?)\)\s*public\s+([\w<>]+)\s+\w+", text, re.DOTALL)
        self.assertEqual(text.count("@Parameter("), len(parameters))
        self.assertNotIn("@ParametersDelegate", text)
        self.assertNotRegex(text, r"class LoadOptions\s+extends")
        for annotation, kind in parameters:
            names = re.search(r'names\s*=\s*(\{[^}]*\}|"[^"]*")', annotation)
            self.assertIsNotNone(names)
            arity = re.search(r"arity\s*=\s*(\d+)", annotation)
            count = int(arity.group(1)) if arity else (0 if kind in ("boolean", "Boolean") else 1)
            self.assertNotIn("variableArity", annotation)
            self.assertIn(count, (0, 1))
            for name in re.findall(r'"([^"]+)"', names.group(1)):
                yield name, count

    def test_every_load_options_annotation_and_alias_routes_after_jar(self):
        for name, arity in self.load_option_parameters():
            with self.subTest(name=name):
                values = ["value with spaces"] if arity else []
                result = self.run_launcher("--master", "local[1]", name, *values)
                self.assertEqual(0, result.returncode, result.stderr)
                args = self.arguments()
                expected_name = "--file" if name in ("-f", "--file") else name
                self.assertEqual([expected_name] + values, args[args.index(str(self.shaded)) + 1:])
                self.assertEqual("local[1]", args[args.index("--master") + 1])

    def test_equals_forms_of_all_value_options_preserve_value_and_routing(self):
        for name, arity in self.load_option_parameters():
            if arity != 1:
                continue
            with self.subTest(name=name):
                value = "value with spaces=second part"
                result = self.run_launcher("--master", "local[1]", name + "=" + value)
                self.assertEqual(0, result.returncode, result.stderr)
                args = self.arguments()
                expected_name = "--file" if name in ("-f", "--file") else name
                self.assertEqual([expected_name, value], args[args.index(str(self.shaded)) + 1:])

    def test_every_value_option_requires_its_annotated_argument(self):
        for name, arity in self.load_option_parameters():
            if arity != 1:
                continue
            with self.subTest(name=name):
                result = self.run_launcher(name)
                self.assertEqual(2, result.returncode)
                self.assertIn("Missing value for " + name, result.stderr)
                self.assertFalse(self.argv.exists())

    def test_empty_equals_values_and_repeated_list_options_are_not_dropped(self):
        result = self.run_launcher("--password=", "--short-id", "first,second",
                                   "--short-id=third=fourth", "--file=")
        self.assertEqual(0, result.returncode, result.stderr)
        args = self.arguments()
        self.assertEqual(["--password", "", "--short-id", "first,second", "--short-id", "third=fourth",
                          "--file", ""], args[args.index(str(self.shaded)) + 1:])

    def test_cluster_files_merge_preserves_all_resources_with_last_wins_parser(self):
        caller = ["hdfs://namenode/data.csv#records", "/tmp/library directory/config.json",
                  "s3a://bucket/settings.json#settings", "file:/tmp/more%20data.csv#more"]
        mapping = "/tmp/mapping directory/input.json"
        result = self.run_launcher("--files", caller[0] + "," + caller[1],
                                   "--files=" + caller[2], "--files", caller[3],
                                   "--deploy-mode", "cluster", "--master", "yarn",
                                   "--file", mapping)
        self.assertEqual(0, result.returncode, result.stderr)
        args = self.arguments()
        engine = args[:args.index(str(self.shaded))]
        effective_files = None
        occurrences = 0
        # SparkSubmitArguments assigns files for each occurrence: the last value wins.
        for index, value in enumerate(engine):
            if value == "--files":
                effective_files = engine[index + 1]
                occurrences += 1
            elif value.startswith("--files="):
                effective_files = value.split("=", 1)[1]
                occurrences += 1
        self.assertEqual(caller + [mapping], effective_files.split(","))
        self.assertEqual(1, occurrences)
        self.assertEqual(["--file", "input.json"], args[args.index(str(self.shaded)) + 1:])

    def test_client_mode_files_merge_does_not_ship_the_mapping(self):
        result = self.run_launcher("--files=first.json#first", "--files", "second path.json#second",
                                   "--file", "/tmp/local mapping.json")
        self.assertEqual(0, result.returncode, result.stderr)
        args = self.arguments()
        jar = args.index(str(self.shaded))
        self.assertEqual(["--files", "first.json#first,second path.json#second",
                          "--deploy-mode", "client"], args[2:jar])
        self.assertEqual(["--file", "/tmp/local mapping.json"], args[jar + 1:])

    def test_conf_cluster_mode_ships_mapping_in_all_option_forms(self):
        mapping = "/tmp/mapping directory/input.json"
        variants = [
            ["--conf", "spark.submit.deployMode=cluster"],
            ["--conf=spark.submit.deployMode=cluster"],
            ["-c", "spark.submit.deployMode=cluster"],
            ["--conf", "spark.submit.deployMode=client",
             "--conf", "spark.submit.deployMode=cluster"],
        ]
        for mode in variants:
            with self.subTest(mode=mode):
                result = self.run_launcher("--file", mapping, "--master", "yarn",
                                           "--files", "config.json", *mode)
                self.assertEqual(0, result.returncode, result.stderr)
                args = self.arguments()
                jar = args.index(str(self.shaded))
                self.assertEqual("config.json," + mapping, args[args.index("--files") + 1])
                self.assertEqual(["--file", "input.json"], args[jar + 1:])

    def test_explicit_deploy_mode_controls_mapping_over_conf_in_any_order(self):
        mapping = "/tmp/mapping directory/input.json"
        for explicit, configured in (("client", "cluster"), ("cluster", "client")):
            options = [["--deploy-mode", explicit],
                       ["--conf", "spark.submit.deployMode=" + configured]]
            for order in (options, options[::-1]):
                with self.subTest(explicit=explicit, order=order):
                    result = self.run_launcher("--master", "yarn", *order[0], *order[1],
                                               "--file", mapping)
                    self.assertEqual(0, result.returncode, result.stderr)
                    args = self.arguments()
                    jar = args.index(str(self.shaded))
                    expected = "input.json" if explicit == "cluster" else mapping
                    self.assertEqual(["--file", expected], args[jar + 1:])
                    self.assertEqual(explicit == "cluster", "--files" in args[:jar])

    def test_explicit_jdbc_jar_is_shipped_but_other_lib_jars_are_not(self):
        driver = self.lib / "mysql driver.jar"
        driver.touch()
        result = self.run_launcher("--master", "local[1]", "--jars", str(driver),
                                   "--file", "jdbc-mapping.json")
        self.assertEqual(0, result.returncode, result.stderr)
        args = self.arguments()
        self.assertEqual(str(driver), args[args.index("--jars") + 1])
        self.assertNotIn(str(self.lib / "dependency one.jar"), args)

    def test_standalone_cluster_is_rejected_before_submission(self):
        variants = [
            ["--master", "spark://example:7077", "--deploy-mode", "cluster"],
            ["--deploy-mode=cluster", "--master=spark://example:7077"],
            ["--conf", "spark.master=spark://example:7077", "--deploy-mode", "cluster"],
            ["--master", "spark://example:7077", "--conf=spark.submit.deployMode=cluster"],
        ]
        for engine in variants:
            with self.subTest(engine=engine):
                result = self.run_launcher(*engine, "--file", "mapping.json")
                self.assertEqual(2, result.returncode)
                self.assertIn("Standalone cluster", result.stderr)
                self.assertFalse(self.argv.exists())

    def test_standalone_client_preserves_mapping_and_opaque_engine_values(self):
        result = self.run_launcher("--master", "spark://example:7077", "--deploy-mode", "client",
                                   "--name", "--deploy-mode", "--file", "/tmp/mapping.json")
        self.assertEqual(0, result.returncode, result.stderr)
        args = self.arguments()
        self.assertEqual(["--file", "/tmp/mapping.json"], args[args.index(str(self.shaded)) + 1:])

    def test_explicit_client_mode_takes_priority_over_conf(self):
        result = self.run_launcher("--master", "spark://example:7077", "--deploy-mode", "client",
                                   "--conf", "spark.submit.deployMode=cluster", "--file", "mapping.json")
        self.assertEqual(0, result.returncode, result.stderr)

    def test_cluster_mapping_fragment_fails_before_submission_without_echoing_path(self):
        mapping = self.app / "valid mapping#private-alias.json"
        mapping.write_text("{}")
        for option in (["--file", str(mapping)], ["--file=" + str(mapping)], ["-f", str(mapping)]):
            with self.subTest(option=option[0]):
                result = self.run_launcher("--deploy-mode=cluster", "--files=hdfs://namenode/data.csv#records",
                                           *option)
                self.assertEqual(2, result.returncode)
                self.assertIn("--file", result.stderr)
                self.assertNotIn(str(mapping), result.stdout + result.stderr)
                self.assertNotIn("private-alias", result.stdout + result.stderr)
                self.assertFalse(self.argv.exists())

    def test_cluster_mapping_comma_is_rejected_without_echoing_path(self):
        mapping = str(self.app / "private,mapping.json")
        for option in (["--file", mapping], ["--file=" + mapping], ["-f", mapping]):
            with self.subTest(option=option[0]):
                result = self.run_launcher("--master", "yarn", "--deploy-mode", "cluster", *option)
                self.assertEqual(2, result.returncode)
                self.assertIn("commas", result.stderr)
                self.assertNotIn(mapping, result.stdout + result.stderr)
                self.assertFalse(self.argv.exists())

    def test_default_client_overrides_properties_and_preserves_local_mapping(self):
        config = self.spark / "conf"
        config.mkdir()
        (config / "spark-defaults.conf").write_text("spark.submit.deployMode cluster\nspark.master yarn\n")
        properties = self.app / "spark.properties"
        properties.write_text("spark.submit.deployMode cluster\nspark.master yarn\n")
        mapping = str(self.app / "mapping,local#part.json")
        for options in ([], ["--properties-file", str(properties)]):
            with self.subTest(options=options):
                result = self.run_launcher(*options, "--file", mapping)
                self.assertEqual(0, result.returncode, result.stderr)
                args = self.arguments()
                jar = args.index(str(self.shaded))
                self.assertEqual("client", args[args.index("--deploy-mode") + 1])
                self.assertNotIn("--files", args[:jar])
                self.assertEqual(["--file", mapping], args[jar + 1:])

    def test_cluster_requires_a_command_line_master(self):
        properties = self.app / "spark.properties"
        properties.write_text("spark.master yarn\n")
        result = self.run_launcher("--properties-file", str(properties),
                                   "--deploy-mode", "cluster", "--file", "mapping.json")
        self.assertEqual(2, result.returncode)
        self.assertIn("requires --master", result.stderr)
        self.assertFalse(self.argv.exists())

    def test_cluster_accepts_command_line_master_conf(self):
        for master in (["--conf", "spark.master=yarn"], ["--conf=spark.master=yarn"],
                       ["-c", "spark.master=yarn"]):
            with self.subTest(master=master):
                result = self.run_launcher(*master, "--deploy-mode", "cluster", "--file", "mapping.json")
                self.assertEqual(0, result.returncode, result.stderr)
                args = self.arguments()
                self.assertEqual("mapping.json", args[args.index("--files") + 1])

    def test_client_mapping_literal_hash_remains_a_local_path(self):
        mapping = self.app / "valid mapping#part.json"
        mapping.write_text("{}")
        result = self.run_launcher("--file", str(mapping), "--files=hdfs://namenode/data.csv#records")
        self.assertEqual(0, result.returncode, result.stderr)
        args = self.arguments()
        self.assertEqual(["--file", str(mapping)], args[args.index(str(self.shaded)) + 1:])

    def test_engine_values_are_not_reclassified_as_loader_options(self):
        engine = ["-c", "--create-graph=true", "--driver-java-options", "--direct=false",
                  "--files", "--file=engine-owned.json", "--jars=/tmp/library with spaces.jar"]
        result = self.run_launcher(*engine, "--master", "yarn",
                                   "--deploy-mode=cluster", "--file=/tmp/input with spaces.json",
                                   "--direct=false", "--batch-failure-fallback=true")
        self.assertEqual(0, result.returncode, result.stderr)
        args = self.arguments()
        jar = args.index(str(self.shaded))
        self.assertEqual(engine[:4] + engine[6:] + ["--master", "yarn", "--deploy-mode", "cluster", "--files",
                         "--file=engine-owned.json,/tmp/input with spaces.json"], args[2:jar])
        self.assertEqual(["--direct", "false", "--batch-failure-fallback", "true",
                          "--file", "input with spaces.json"], args[jar + 1:])

    def test_help_equals_value_is_rejected_without_echoing_value(self):
        result = self.run_launcher("--help=not-a-real-secret")
        self.assertEqual(2, result.returncode)
        self.assertNotIn("not-a-real-secret", result.stdout + result.stderr)
        self.assertFalse(self.argv.exists())

    def test_recognized_engine_value_option_requires_an_argument(self):
        for name in ("--conf", "-c", "--driver-memory"):
            with self.subTest(name=name):
                result = self.run_launcher(name)
                self.assertEqual(2, result.returncode)
                self.assertFalse(self.argv.exists())

    def test_password_and_paths_preserve_boundaries_without_echo(self):
        password = "not-a-real-secret with spaces"
        mapping = str(self.app / "mapping with spaces.json")
        result = self.run_launcher("--master", "local[1]", "--file", mapping,
                                   "--username", "test-user", "--password", password)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertNotIn(password, result.stdout + result.stderr)
        args = self.arguments()
        jar = args.index(str(self.shaded))
        self.assertEqual(["--username", "test-user", "--password", password,
                          "--file", mapping], args[jar + 1:])
        self.assertNotIn(str(self.lib / "hugegraph-loader-test.jar"), args)
        self.assertNotIn(str(self.lib / "dependency one.jar"), args)

    def test_cluster_files_and_engine_parameters(self):
        mapping = "/tmp/mapping directory/input.json"
        result = self.run_launcher("--deploy-mode", "cluster", "--file", mapping,
                                   "--conf", "spark.app.name=two words", "--master", "yarn",
                                   "--jars", "/tmp/custom library.jar")
        self.assertEqual(0, result.returncode, result.stderr)
        args = self.arguments()
        jar = args.index(str(self.shaded))
        self.assertIn("spark.app.name=two words", args[:jar])
        self.assertEqual("/tmp/custom library.jar", args[args.index("--jars") + 1])
        self.assertEqual(mapping, args[args.index("--files") + 1])
        self.assertEqual(["--file", "input.json"], args[jar + 1:])

    def test_hbase_parameters_are_application_arguments(self):
        params = ["--hbase-zk-quorum", "localhost", "--hbase-zk-port", "2181",
                  "--hbase-zk-parent", "/hbase", "--vertex-table-name", "graph:v",
                  "--edge-table-name", "graph:e", "--sink-type", "false"]
        result = self.run_launcher("--master", "local[1]", *params, "--file", "mapping.json")
        self.assertEqual(0, result.returncode, result.stderr)
        args = self.arguments()
        self.assertEqual(params + ["--file", "mapping.json"], args[args.index(str(self.shaded)) + 1:])

    def test_routing_options_and_aliases_are_application_arguments(self):
        params = ["--pd-peers", "localhost:8686", "--pd-token", "test-token",
                  "--meta-endpoints", "localhost:2379", "--route-type", "pd",
                  "--cluster", "test-cluster", "--graphspace", "test-space",
                  "-g", "test-graph", "-s", "schema.json", "-h", "localhost", "-p", "8080"]
        result = self.run_launcher("--master", "local[1]", *params, "-f", "mapping.json")
        self.assertEqual(0, result.returncode, result.stderr)
        args = self.arguments()
        self.assertEqual(params + ["--file", "mapping.json"], args[args.index(str(self.shaded)) + 1:])

    def test_shared_legacy_string_outputs_remain_available(self):
        result = subprocess.run(
            ["bash", "-c", 'source "$1"; shift; get_params "$@"; '
             'printf "%s\\n%s\\n" "$ENGINE_PARAMS" "$HUGEGRAPH_PARAMS"',
             "bash", str(self.app / "bin/get-params.sh"), "--master", "local[1]",
             "--file", "mapping.json", "--batch-size", "10"],
            text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=10)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(["--master local[1]", "--batch-size 10 --file mapping.json"],
                         result.stdout.splitlines())

    def test_missing_value_fails_before_submit(self):
        for option in ("--password", "--file", "--batch-size"):
            with self.subTest(option=option):
                result = self.run_launcher(option)
                self.assertEqual(2, result.returncode)
                self.assertIn("Missing value for " + option, result.stderr)
                self.assertFalse(self.argv.exists())

    def test_help_does_not_consume_an_engine_parameter(self):
        result = self.run_launcher("--help", "--master", "local[1]")
        self.assertEqual(0, result.returncode, result.stderr)
        args = self.arguments()
        self.assertEqual(["--help"], args[args.index(str(self.shaded)) + 1:])
        self.assertEqual("local[1]", args[args.index("--master") + 1])


if __name__ == "__main__":
    unittest.main()
