# hugegraph-loader

[![License](https://img.shields.io/badge/license-Apache%202-0E78BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Build Status](https://github.com/apache/hugegraph-toolchain/actions/workflows/loader-ci.yml/badge.svg)](https://github.com/apache/hugegraph-toolchain/actions/workflows/loader-ci.yml)
[![codecov](https://codecov.io/gh/hugegraph/hugegraph-loader/branch/master/graph/badge.svg)](https://codecov.io/gh/hugegraph/hugegraph-loader)
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.hugegraph/hugegraph-loader)](https://mvnrepository.com/artifact/org.apache.hugegraph/hugegraph-loader)

hugegraph-loader is a customizable command line utility for loading small to medium size graph datasets into the HugeGraph database from multiple data sources with various input formats.

Embedded callers must call `shutdown()` after `load()`, including failed imports. Successful shutdown waits for this Loader's
workers, closes its clients and unregisters its JVM shutdown hook. Closing a Loader whose `load()` was never called
leaves the existing import checkpoint unchanged.

## 1. Features

- Multiple data sources, such as local file(path), HDFS file(path), MySQL
- Various input formats, such as json, csv, and text with any delimiters.
- Diverse options, with which users can manage the data loading intuitively.
- Detecting schema from data automatically, reduce the complex work of schema management.
- Advanced customized operations with groovy script, users can configure how to construct vertices and edges by themselves.

## 2. Quick start 

There are three ways to get HugeGraph-Loader:

- Download the compiled tarball
- Clone source code then compile and install
- Use docker image (Convenient for Test/Dev)

And you can find more details in the [doc](https://hugegraph.apache.org/docs/quickstart/hugegraph-loader/#2-get-hugegraph-loader)

### 2.1 Download the compiled tarball

Download the latest version of the HugeGraph-Toolchain release package:

``` bash
wget https://downloads.apache.org/hugegraph/{version}/apache-hugegraph-toolchain-{version}.tar.gz
tar zxf *hugegraph*.tar.gz
```

### 2.2 Clone source code then compile and install

Clone the latest version of HugeGraph-Loader source package:

```bash
# 1. get from github
git clone https://github.com/apache/hugegraph-toolchain.git

# 2. get from direct  (e.g. here is 1.0.0, please choose the latest version)
wget https://downloads.apache.org/hugegraph/{version}/apache-hugegraph-toolchain-{version}-src.tar.gz
```

Due to the license limitation of the `Oracle OJDBC`, you need to manually install ojdbc to the local maven repository. Visit the [Oracle jdbc downloads page](https://www.oracle.com/database/technologies/appdev/jdbc-drivers-archive.html). Select Oracle Database 12c Release 2 (12.2.0.1) drivers, as shown in the following figure.

After opening the link, select "ojdbc8.jar".

Install ojdbc8 to the local maven repository, enter the directory where ojdbc8.jar is located, and execute the following command.

```bash
mvn install:install-file -Dfile=./ojdbc8.jar -DgroupId=com.oracle -DartifactId=ojdbc8 -Dversion=12.2.0.1 -Dpackaging=jar
```

For the Java 17 candidate, prepare the locked SDK using the [bootstrap instructions](../docs/java17-migration.md#build-the-locked-candidate), then compile from the Toolchain repository root with the same isolated Maven repository:

```bash
mvn -Dmaven.repo.local="$candidate_dir/m2" -Dsdk.validation.mode=candidate clean package -pl hugegraph-client,hugegraph-loader -am -DskipTests -ntp
```

### 2.3 Use docker image (Convenient for Test/Dev)

#### 2.3.1 Docker run

Use the command `docker run -itd --name loader hugegraph/loader` to start loader.

If you want to load your data, you can mount the data folder like `-v /path/to/data/file:/loader/file`


To build the current checkout, run from the Toolchain repository root:

```bash
docker build -f hugegraph-loader/Dockerfile -t hugegraph/hugegraph-loader:latest .
```

#### 2.3.2 Docker-compose

The example `docker-compose.yml` is [here](./docker/example/docker-compose.yml). Use the command `docker-compose up -d` to deploy `loader` with `server` and `hubble`.

> Note: 
> 1. The docker image of hugegraph-loader is a convenience release, not **official distribution** artifacts. You can find more details from [ASF Release Distribution Policy](https://infra.apache.org/release-distribution.html#dockerhub).
>
> 2. Recommend to use `release tag`(like `1.0.0`) for the stable version. Use `latest` tag to experience the newest functions in development.

## 3 Load data

### 3.1 Use loader directly

> notice: currently, version is `1.3.0`

Download and unzip the compiled archive

```bash
wget https://downloads.apache.org/hugegraph/{version}/apache-hugegraph-toolchain-{version}.tar.gz
tar zxf *hugegraph*.tar.gz
```

Then, load data with example file:

```bash
cd apache-hugegraph-toolchain-{version}
cd apache-hugegraph-loader-{version}
sh bin/hugegraph-loader.sh -g hugegraph -f example/file/struct.json -s example/file/schema.groovy
```

More details is in the [doc](https://hugegraph.apache.org/docs/quickstart/hugegraph-loader/)

#### Arguments from a file

The launcher accepts JCommander's `@file` syntax. Put each option and its value on separate lines:

```text
--graph
hugegraph
--file
example/file/struct.json
--schema
example/file/schema.groovy
```

```bash
bash bin/hugegraph-loader.sh @loader.args
```

Use `--parser-threads` to set read pipelines. The deprecated `--parallel-count` alias also works in argument files
and emits one warning after successful option validation.

Paths in the argument file resolve from the working directory. Protect files containing credentials with mode `0600`;
the credentials can then stay out of the process arguments. The mapping directory must be writable for progress output,
so copy a mapping mounted from a read-only Kubernetes ConfigMap to a writable volume before running Loader.

#### Graph mode during startup and cleanup

For protocol-1 Servers, see [Concurrent imports and graph mode](#concurrent-imports-and-graph-mode).

On Servers without the loading-owner endpoint, Loader resets `LOADING` mode only after that context sets it. A startup failure before the mode
change, a rejected mode request, or a restore run does not clear another run's `LOADING` mode. Cleanup preserves a
different current graph mode; successful cleanup is not repeated. Concurrent successful Loader runs on the same
graph still require external coordination; this startup safeguard does not provide shared ownership of graph mode.

A network failure can leave a mode change applied without a successful response. Loader preserves the original
failure and logs an error naming the graph and warning that it may still be in `LOADING` mode. Cleanup leaves the
unconfirmed mode unchanged. Inspect the graph mode and confirm there are no active imports before explicitly
restoring `NONE` mode; an unconfirmed request cannot be rolled back automatically.

#### Concurrent imports and graph mode

On a Server with loading-owner protocol 1, each ordinary import acquires its own
handle on the same live graph instance. After its workers stop, cleanup releases
only that handle; the graph stays in `LOADING` until the last import releases.
The protocol does not coordinate different Server processes or survive restart.
Keep the client graph binding unchanged for the lifetime of a Loader context.
To import into another graph, create a new Loader context instead of rebinding
the shared client during an import.

Servers without the ownership endpoint keep the existing LOADING and cleanup
behavior, with no concurrent-import guarantee. Permission errors, network failures
and invalid ownership responses do not fall back to legacy mode. Restore and
Graph-source imports retain their existing RESTORING behavior.

The paired Server change must be merged and released before the new guarantee is
available: [Server #3331](https://github.com/apache/hugegraph/pull/3331).

On protocol-1 Servers, an ordinary import requires `NONE` or `LOADING` held by
active owners. Restore and Graph-source imports leave `RESTORING` after shutdown,
as before. A subsequent ordinary import is rejected until that mode is manually
recovered; an enum-set `LOADING` or `MERGING` mode has the same prerequisite.

Before acquiring ownership, Loader logs `Preparing LOADING owner` with the
non-secret `instance/epoch` identity and recovery guidance. Owner tokens are not
logged. Owners have no automatic expiry, and cleanup failure still closes the
Loader context and clients; another `shutdown()` does not retry the release.
After an unclean exit or release failure, use the paired Server's administrator
recovery procedure below instead of looking for a token in Loader logs.

Hubble's existing task lifecycle can finish, stop or reconnect without calling
`Loader.shutdown()`. This SDK change alone can therefore leave active owners and
block mode changes, including restore. Hubble integration requires the paired
[task cleanup fix #814](https://github.com/apache/hugegraph-toolchain/pull/814) and
[application shutdown fix #818](https://github.com/apache/hugegraph-toolchain/pull/818);
deploy those fixes together. If owners are already stranded, use the recovery
steps below.

1. Stop all related imports and ordinary graph writes, and drain their in-flight
   requests. The Server recovery endpoint cannot perform this external drain.
2. As an administrator with `ADMIN` or `ROOT` permission, GET
   `/graphspaces/{graphspace}/graphs/{graph}/mode/owners` on the affected live
   Server instance. Read its current `instance`, `epoch` and non-secret `revision`
   immediately before recovery; do not reuse values from another instance or an
   earlier attempt.
3. PUT the following body to that same endpoint, replacing the placeholders with
   the values just read. This recovery action does not require an owner token.

   ```json
   {
     "action": "recover",
     "instance": "<current instance UUID>",
     "epoch": 0,
     "revision": 0,
     "confirm": "I'm sure all imports have stopped"
   }
   ```

   Replace both example `0` values with the GET response's current `epoch` and
   `revision`. Recovery requires both values to match; any owner or mode change
   after GET makes this request stale.
   Success returns `state: recovered`, `mode: NONE` and the incremented epoch,
   clears existing owners, and prevents old-epoch acquire/release requests from
   taking effect. A stale recovery request returns HTTP 409 so it cannot clear a
   newer owner, including one acquired within the same epoch. Ordinary graph-status
   write permission is insufficient (HTTP 403).
4. If the response is lost or the result is otherwise unknown, inspect the graph
   mode and capability. Do not blindly obtain a new epoch and repeat recovery,
   which could clear owners created after the first attempt.

This procedure also clears a manually set `RESTORING` or `MERGING` mode once all
related work has stopped and drained. It depends on the paired Server change
linked above; older Servers retain their legacy enum-mode recovery behavior.
While protocol-1 owners are active, the Server rejects every enum graph-mode PUT
for that graph, including `NONE`, `LOADING` and `RESTORING`. This also affects the
Spark connector and older Loaders that use enum mode setters. Avoid running those
clients or restore imports concurrently with owned imports. Stop and drain the
owned imports, release or recover their ownership as described above, then retry
using the client's existing workflow.

### 3.2 Load data with docker

> If the `loader` and `server` is in the same docker network (for example, you deploy `loader` and `server` with `docker-compose`), we can set `-h {server_container_name}`. In our example, the container name of `server` is `graph`
>
> If `loader` is deployed alone, the `-h` should be set to the ip of the host of `server`. See the [parameter description](https://hugegraph.apache.org/docs/quickstart/hugegraph-loader/#341-parameter-description)

Visit [doc](https://hugegraph.apache.org/docs/quickstart/hugegraph-loader/#45-use-docker-to-load-data) for more details.

```bash
docker exec -it loader bin/hugegraph-loader.sh -g hugegraph -f example/file/struct.json -s example/file/schema.groovy -h graph -p 8080
```


## 4. Doc

For Spark execution, see [Spark Loader on Java 17](docs/spark-java17.md) for the tested engine version, launcher usage, and the separate HBase bulkload boundary.

The [loader homepage](https://hugegraph.apache.org/docs/quickstart/hugegraph-loader/) contains more information about it.

Server 1.7 compatibility CI uses the pinned official ASF 1.7.0 binary archive. The fixture cache verifies the release version, official source URL, official SHA-512 and cached archive SHA-256 before startup. A failed download does not fall back to rebuilding the release tag. Unpublished candidate SDKs and their Server fixture continue to use the locked ASF source build.

See the [Flink CDC Java 17 validation](docs/flink-cdc-java17.md) for its current limits and launcher syntax.

## 5. License

hugegraph-loader is licensed under Apache 2.0 License.
