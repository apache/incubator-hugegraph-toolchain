<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Spark Loader on Java 17

The API sink (`--sink-type true`) uses Spark 3.5.8 / Scala 2.12 and requires Java 17 on both the driver and executors. Validate vertex and edge writes with a separate executor JVM and Java Client readback against Server 1.7 before deployment. The HBase bulkload path (`--sink-type false`) is outside this Java 17 support boundary.

Use the generated distribution's `bin/hugegraph-spark-loader.sh`. It submits the distribution's shaded Loader jar, which contains the application dependencies. Spark supplies the engine libraries and Java 17 module options. Set `SPARK_HOME` and `JAVA_HOME` for the matching engine and JVM before starting the launcher. Extra JARs placed in `lib/` are not shipped automatically. Pass JDBC drivers and other additional dependencies explicitly with `--jars /path/to/mysql-driver.jar`. Before creating a Spark session or submitting any partitions, the driver checks every CSV and TEXT mapping, including FILE and HDFS sources. These mappings require an explicit `header` and headerless data; a null or omitted `header`, or `has_header: true`, is rejected with a pointer to this document. The Spark reader does not implement the ordinary Loader's per-file header detection or skip physical header rows. Use headerless CSV and TEXT with an explicit mapping header. CSV, JSON and TEXT use Spark's default UTF-8 readers. For both FILE and HDFS, the driver rejects other mapping charsets before creating a Spark session or submitting any partitions. UTF-8 aliases such as `utf8` remain supported; convert non-UTF-8 data to UTF-8 before importing. CSV parsing retains Spark's permissive behavior. Validate input column counts before submission: Spark can truncate nonempty extra fields before mapping validation. This is a known limitation of the current reader. Standalone (`--master spark://...`) supports client deploy mode only; cluster deploy mode is rejected because the remote driver's mapping is needed before Spark initializes file localization. This restriction does not establish validation of YARN or Kubernetes cluster deployment. The launcher explicitly defaults to client deploy mode, overriding deployment mode settings in `--properties-file` and `spark-defaults.conf`. Select cluster mode and its master on the command line using `--deploy-mode` / `--master` or the equivalent `--conf spark.submit.deployMode=cluster` / `--conf spark.master=...` options. `--file` requires a local mapping path without commas or `#` fragments in cluster mode; client mapping paths retain these characters, and caller `--files` URI aliases remain supported. In cluster mode, the mapping file's basename must be unique among localized resources from caller `--files` and other Spark file resources, including URI aliases. Spark's native resource localization reports collisions; the launcher does not prevalidate these URIs.

Create the target schema before loading. Data paths must be readable by the Spark executors. Load vertices before edges when the edges require those vertices. The current API path does not update the distributed insertion counters, so check the imported vertices and edges through the target graph rather than using `insertSuccessCnt` as the result count. Spark does not maintain resumable input offsets: `--incremental-mode true` and `--failure-mode true` are rejected by the driver before creating a Spark session. Executor-local progress files do not represent a cluster checkpoint; rerunning a Spark import reads the input again. For example, from the Loader distribution directory:

```bash
bin/hugegraph-spark-loader.sh \
  --master 'local[2]' \
  --driver-memory 1g \
  --file /path/to/vertex-mapping.json \
  --host 127.0.0.1 --port 8080 --graph hugegraph \
  --username "$HUGEGRAPH_USERNAME" --password "$HUGEGRAPH_PASSWORD" \
  --sink-type true
```

`--password` is the Basic authentication password; `--token` selects token authentication. Engine arguments such as `--master`, `--conf`, and `--jars` are passed to Spark. Loader arguments are passed after the application jar, with spaces and other argument characters preserved. The launcher does not print credentials or the full command. A failed load returns a nonzero exit code.

The ordinary Maven build retains Spark 3.2.2 as a `provided` compile-time API dependency. Spark 3.5.8 supplies the runtime engine through `SPARK_HOME`; it is not bundled in the shaded Loader jar. This keeps the ordinary Loader's default dependency graph unchanged. No global Spark version override is needed to build the API launcher.

HBase bulkload is unsupported on Spark 3.5.8 / Java 17: the `HBaseDirectLoader.buildVertexAndEdge` path fails while Spark serializes its actual partition closure with `NotSerializableException: LoadOptions`. The API partition writer does not cover this separate bulkload closure.

HFile generation, HBase import, and backend readback still need their own validation before this path can be supported. Existing bulkload deployments should retain their previously validated released Loader package and matching JVM/Spark/HBase setup in a separate installation. Validate that installation against its own supported environment before use.

## Regression checks

The ordinary Loader `UnitTestSuite` includes the Spark partition serialization and driver header preflight checks. Loader CI also runs the standalone launcher argument tests. The separate `spark-loader-ci` workflow builds with the default provided compile dependency and runs the shaded distribution through an official Spark 3.5.8 runtime against both the locked candidate server and official Server 1.7. Its independent executor gate checks UTF-8 vertex/edge data, graph readback, failed-load exit status, and cleanup; it does not exercise HBase bulkload.
