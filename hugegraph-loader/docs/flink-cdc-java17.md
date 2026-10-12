# Flink CDC on Java 17

This integration targets Flink 2.2.1 and MySQL CDC 3.6.0-2.2 on Java 17. CI first checks rejection of vertex and edge identity changes, deletion and replay through a Graph API probe against its test-owned, same-source candidate Server. It then checks a bounded vertex job against real MySQL and Flink, covering snapshots, insert/update/NULL/delete, primary-key changes, TaskManager restart, savepoint replay and fresh-target rebuild. The API probe does not verify edge delivery through Flink/MySQL; broader mappings and edge engine coverage require separate validation. These bounded checks do not establish supported production deployment.

## Building

The root POM pins the Flink and CDC versions. Compile and test the adaptation with:

```bash
mvn test -pl hugegraph-loader \
  -Dtest=FlinkSinkSerializationTest,HugeGraphSinkTest,HugeGraphDeserializationTest
node --test hugegraph-loader/src/test/scripts/flink-launcher.test.cjs
```

Build and install the matching Java 17 Client and its server-side dependencies first. The Flink runtime remains provided by the cluster. The CDC connector is a runtime part of the job and is included in the Loader shading inputs. CI validates the packaged vertex job with the external JDBC driver and matching Client in its pinned Flink 2.2.1 Java 17 image. CDC's Flink 2 compatibility jar includes duplicate Flink API classes. The POM places the provided Flink core before CDC so compilation and tests use the public 2.2 Sink interface. The packaged class-loading probe checks that the CDC coordinator and Guava 31 load from the job JAR, while Guava 33 and Flink's `Source` API load from the cluster's `flink-dist-2.2.1.jar`. Clusters with different dependencies or class-loading settings require separate validation; passing the compile-time contract test alone is insufficient.

## Launch arguments

Set `FLINK_HOME` to the selected Flink installation. The submission JVM, JobManager and TaskManager must all run Java 17. Preserve the distribution's `env.java.opts.all` module-access options; do not replace them with a shorter list. The launcher selects exactly one `*hugegraph-loader*-shaded.jar` in `lib`. Spark retains the same shared job artifact path. The ordinary Loader launcher excludes this shaded job from its classpath, and the assembly omits CDC-only loose dependencies. Kafka clients and their compression dependency remain available to the ordinary Kafka reader; common dependency versions are retained for the shared shaded job.

CDC rejects `--dry-run true` and `--use-prefilter true` before creating a sink or opening a client. It does not implement preview writes or ingestion deduplication. UPDATE requires explicit non-empty mapping `update_strategies` and an unchanged graph identity. Identity-changing UPDATE events fail before any mapping is buffered or written; remote failures in supported writes still require replay/recovery.

Without a separator, all arguments are Loader options:

```bash
bin/hugegraph-flinkcdc-loader.sh --file mapping.json --graph hugegraph
```

Place Flink options before `--` and Loader options after it. Paths with spaces remain individual arguments; the launcher does not print argument values.

```bash
bin/hugegraph-flinkcdc-loader.sh -p 1 -- \
    --file "my mappings/mysql.json" --graph hugegraph --cdc-sink-parallelism 1
```

Each mapping uses one source reader and one sink writer to preserve mutation order. `--cdc-sink-parallelism` defaults to 1 and other values are rejected before building the pipeline. Both operators explicitly use parallelism 1, including when Flink's global parallelism is unset or `-p 2` is supplied before `--`. This serial topology does not support parallel CDC writers. After `--`, Loader's `-p` is the HugeGraph server port, not Flink parallelism. The launcher and configuration serialization can transport other numeric values; the CDC entry point enforces the supported topology when it receives them. Both Flink submission and direct JVM invocation report invalid sink settings as failures.

The submitting process validates the mapping file and Loader options. Sink configuration is serialized to workers without requiring that file to exist on each worker. The sink uses the public Sink V2 writer lifecycle. Checkpoint flush delegates to the existing graph output logic; this does not add transactional graph writes or an exactly-once guarantee.

## Remaining runtime coverage

Edge CDC, broader mapping configurations and custom cluster environments still need runtime acceptance beyond the bounded vertex fixture. For those cases, compare complete IDs and property values through snapshot, mutations, worker recovery and replay against real MySQL and HugeGraph instances.

A fresh snapshot into an existing graph does not by itself remove graph records deleted during downtime: controlled rebuild needs an explicit fresh-target or reconciliation procedure. Do not delete user graph data as an implicit migration step.


## Reproducible Linux acceptance

The `flink-cdc-java17-ci` workflow uses the shared same-source SDK action aligned with ASF Server master. It installs the matching Common/Core/PD/Store SDK and Server distribution dependency closure in a fresh Maven repository, then packages the Loader job. Its runtime stage is separate from compilation and unit-test success.

To inspect the test plan without touching Docker or starting services:

```bash
python3 hugegraph-loader/assembly/travis/flink-cdc/verify.py
```

Actual execution requires an isolated Linux host, Docker, Python 3.11+, Maven and Java 17. Provide the same-source server archive, its populated isolated Maven repository and packaged shaded Loader JAR. The API probe uses offline Maven against that repository:

```bash
python3 hugegraph-loader/assembly/travis/flink-cdc/verify.py --run \
    --server-archive /path/to/apache-hugegraph.tar.gz \
    --server-archive-sha256 <same-source-sdk-archive-sha256> \
    --sdk-manifest /path/to/candidate-sdk-manifest.json \
    --maven-repo /path/to/candidate-m2 \
    --job-jar /path/to/hugegraph-loader-shaded.jar \
    --jdbc-driver /path/to/mysql-connector-java-8.0.28.jar \
    --server-sha <server-commit> --toolchain-sha <toolchain-commit> \
    --evidence /path/to/test-evidence
```

The harness reserves loopback ports 38080/18182 (Server), 13306 (MySQL), and 18081/16123/16124/16122 (Flink), refusing occupied ports. It creates a new Server directory and two test graphs, plus uniquely labelled containers. MySQL uses an empty test-only root password, binds to loopback, and never points at an existing database. Cleanup removes only the containers carrying this run's ownership label and stops its own server process group. Do not use production data or hosts.

JobManager and TaskManager run in separate Java 17 containers. Only JobManager mounts the mapping file and packaged job; TaskManager must receive the actual job through Flink. The test compares numeric vertex IDs and all mapped properties against MySQL after snapshot, insert/update/NULL/delete, SQL primary-key changes, TaskManager restart, forced replay from an earlier savepoint, and a fresh-target rebuild after downtime. A new binlog marker after restore prevents pre-existing correct graph data from passing before replay has caught up. Successful checkpoints are required.

The harness rejects a manifest from another source commit or an archive differing from the SDK action's SHA-256 before starting runtime resources. Evidence includes the SDK manifest, source commits, archive/job hashes, image digests, JVM versions, checkpoint/savepoint metadata, row comparisons and sanitized logs. A failed or unexecuted runtime stage is not compatibility evidence. The current acceptance fixture covers vertex CDC; edge CDC and broader mapping configurations still need separate runtime coverage before declaring the entire mode supported.

## Identity and delete behavior

UPDATE events retain both row images. Configure MySQL with `binlog_row_image=FULL`; a missing before image fails explicitly. The sink derives PRIMARY_KEY vertex IDs through the same Loader builder path used for edge endpoints. It resolves edge IDs through the public adjacency API using source, label and sort keys, then matches the target. Queries preserve literal sort-key strings and paginate when needed. An already absent element is accepted when a delete is replayed.

UPDATE supports only unchanged vertex IDs, edge endpoints and sort keys, including identities added or removed by unfolded mappings. The sink checks all non-skipped mappings before any write and rejects changes with an explicit error, preserving the old elements and incident edges. Atomic identity migration is tracked in [#793](https://github.com/apache/hugegraph-toolchain/issues/793). Separate DELETE/CREATE events retain their existing non-transactional semantics; this guard does not make Debezium database primary-key changes atomic. Source-to-graph identity mappings must be unambiguous; independent rows must not compete for one graph identity.

CDC converts non-null source fields to strings before Loader mapping. Configure `null_values` with string markers such as `"0"` or `"<NULL>"`; a numeric marker such as `0` does not match the source string `"0"`. Nullable property updates follow the Loader mapping's selected fields, ignored fields and null markers.

Run the opt-in REST integration probe against a test-only Server:

```bash
mvn test -pl hugegraph-client,hugegraph-loader -am \
  -Dtest=HugeGraphIdentityIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false \
  -Dflink.identity.integration=true -Dflink.identity.port=8080 -Dflink.identity.graph=hugegraph
```

The probe uses UUID-prefixed schemas in the selected `DEFAULT` graph (default `hugegraph`) and removes only those fixtures. CI selects `flink_initial` on its owned Server at port 38080 and requires a fresh, uniquely suffixed Surefire report with one passing test and zero skips before starting Flink/MySQL. A probe failure fails the acceptance run. It passes real Debezium `SourceRecord` objects through the deserializer and sink. It checks rejection of CUSTOMIZE_NUMBER and PRIMARY_KEY vertex identity changes, preservation of incident edges, UUID vertex delete, rejection of edge endpoint/sort-key changes, same-identity updates, duplicate replay and deletion, and deletion beyond a 100-edge query page, including a literal `P.gt(1)` sort key. This verifies the Graph API contract, not delivery through a running Flink/MySQL cluster.

The candidate Server's existing edge-create API still rejects UUID endpoints when `check_vertex=true`: JSON UUID IDs arrive as strings and are looked up without converting the endpoint schema type. UUID edge ingestion is therefore not verified by this probe; this separate Server API limitation must be resolved before claiming that mapping supported.

## External driver and bundled component licenses

MySQL Connector/J remains an external, provided dependency. Install a compatible driver in the Flink cluster's `lib` directory on both JobManager and TaskManager; it is not included in the Loader shaded JAR or release archive. The test harness uses `mysql:mysql-connector-java:8.0.28` only as an external test-runtime file, checks its pinned SHA-256 before starting services, and mounts it read-only into both containers. CI downloads this file separately and does not upload it with the released job or evidence.

The Loader's Flink CDC dependency aggregate includes [Activation 1.1.1](https://javaee.github.io/activation/) under CDDL 1.0; [HK2 2.6.1](https://eclipse-ee4j.github.io/glassfish-hk2/) (including its repackaged AOP Alliance and Jakarta Inject components), [JAX-RS APIs 2.1.1/2.1.6](https://github.com/jakartaee/rest), and [Jersey Servlet 2.34/3.1.11 and HK2 integration 2.34](https://eclipse-ee4j.github.io/jersey/) under their EPL 2.0 license option. See the distribution `LICENSE` and `NOTICE` for the full terms and additional embedded-component notices.
