# Apache HugeGraph-Hubble

[![License](https://img.shields.io/badge/license-Apache%202-0E78BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![hugegraph-hubble-ci](https://github.com/apache/hugegraph-toolchain/actions/workflows/hubble-ci.yml/badge.svg?branch=master)](https://github.com/apache/hugegraph-toolchain/actions/workflows/hubble-ci.yml)
[![CodeQL](https://github.com/apache/hugegraph-toolchain/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/apache/hugegraph-toolchain/actions/workflows/codeql-analysis.yml)

hugegraph-hubble is a graph management and analysis platform that provides features:
graph data load, schema management, graph relationship analysis, and graphical display.

## Product Tour

### Graph Data Workbench

Start from one workspace for graph discovery, import, query, account management, and cluster operations.

![Hubble graph data workbench](docs/images/showcase/home-workbench.jpg)

### GQL Traversal

Run Gremlin queries and inspect their graph, table, or JSON results without leaving the workbench.

![Hubble GQL traversal and graph visualization](docs/images/showcase/gql-traversal.jpg)

### Schema Templates

Prepare reusable schemas before configuring data sources and import tasks.

![Hubble schema template management](docs/images/showcase/schema-templates.jpg)

### Asynchronous Tasks

Track asynchronous queries and algorithms, then expand compact results inline.

![Hubble asynchronous task results](docs/images/showcase/async-tasks.jpg)

## Authentication, connections, and compatibility

Hubble uses one capability-driven connection boundary for `1.8/master`. Authentication mode is detected from the connected HugeGraph Server; Hubble does not maintain a separate authentication switch. Account and permission entry points are hidden when Server allows anonymous access. Connection switching always goes through the backend resolver. In PD mode, a valid server address returned by discovery is sufficient; a manually configured server URL is not required.

The UI presents four stable permission meanings: super administrator, GraphSpace read-only, GraphSpace read-write, and GraphSpace administrator. The last one means member management plus read/write within that GraphSpace; low-level `role`, `target`, `access`, and `belong` fields are not exposed.

The compatibility boundary is deliberately small:

| HugeGraph Server / PD | Deployment | Hubble compatibility | Scope and limitations |
|---|---|---|---|
| Server 1.5.x | Standalone, normally without authentication | Minimum compatibility | Basic graph, schema, data, and Gremlin workflows only. GraphSpace, account permissions, PD/Store topology, cluster operations, and newer algorithms are unavailable. |
| Server 1.7.x with matching PD/Store 1.7.x | Standalone or distributed | Minimum compatibility through legacy adapters | Core management and query workflows remain usable, but legacy REST/Gremlin authentication, permission semantics, metrics, and algorithm capabilities may provide a reduced experience. |
| Server, PD, and Store 1.8.x or later | Distributed deployment recommended | Full and recommended experience | Current GraphSpace, account permission presets, cluster operations, async tasks, and algorithm capability handling are designed and validated against this generation. |

Use matching Server, PD, and Store minor versions in a distributed cluster. **Server/PD 1.8 or later is strongly recommended for the best Hubble experience.** Support for 1.5 and 1.7 is intentionally limited to minimum usability and does not imply feature parity with the current release. Version checks stay in the client adapter/resolver rather than being scattered through controllers or pages. See [`AGENTS.md`](AGENTS.md) for verification rules.

## Import task lifecycle

Import tasks finish their Loader shutdown before reporting completion. Shutdown
also persists progress and closes the task's clients and worker pools; a shutdown
failure marks the task as failed, including after a pause or stop request. The
synchronous Loader example also shuts down before returning. A task that cannot
be saved, whose transaction rolls back, or whose async dispatch is rejected closes
its Loader without starting a run. Pause and stop wait for the running Loader and
its final database callback. Within one Hubble service, pause, stop, resume, retry
and initial task publication are serialized for a task, including the database
read and final stop persistence; other requests for that task wait for admission.
The bounded weak lock stripes can also serialize unrelated task IDs that collide.
Retry and resume reject an owner whose workers have not closed. An accepted
pause or stop also prevents delayed initial dispatch after commit; the unused
Loader closes without overwriting the accepted status. A delayed callback also
closes only its unused Loader if the task was deleted, finished, or replaced; it
does not change the newer task or job state. Task deletion rechecks state and
closed resources under admission. The Job task create/start endpoints and Job
management deletion share Job admission, so deletion enumerates tasks started before it and later
starts recheck that the Job still exists. Job deletion commits physical stops before
atomically deleting task and job metadata; a metadata rollback leaves those
tasks stopped. The Ingest task deletion route uses this same scoped Job deletion.
Graph deletion keeps its remote-first order: if the Server refuses the drop, local
Job/task metadata and owners are preserved. After a successful drop, Hubble
enumerates that graph's existing Jobs and applies the same deletion to each one,
waiting for its Loaders to close before deleting its metadata. A cleanup failure
leaves that Job and its task rows recoverable; earlier Jobs have already committed
their cleanup, and the remote graph drop cannot be rolled back. This enumeration
does not prevent new Jobs from being created, so Jobs outside the snapshot are
preserved. It provides no graph-wide admission barrier.

If delayed initial publication cannot read task metadata, it closes
its unused Loader and retains that closed owner for the existing progress poll.
The poll marks only a still-RUNNING row as failed and retires the owner after commit;
a missing or advanced row is preserved. A rejected dispatch whose failure cannot
be committed also retains its closed owner. The poll retries the conditional
Task-to-FAILED and Job-to-FAILED changes in one transaction, preserving a task row
that has since advanced or disappeared. Each owner's polling write uses an H2
savepoint: a write failure rolls back that owner's changes and leaves it for a
later poll while other owners can finish persistence. Owners are retired only
after the polling transaction commits; a deleted final task row retires its closed
owner without recreating metadata.
Finished tasks release their native JVM shutdown hook once their context closes.
If shutdown cannot terminate the task's workers, Hubble retains the failed owner
and rejects retry/resume until its context is actually closed; restart Hubble to
finish recovery when cleanup remains incomplete. This cleanup applies when load
exits and reaches shutdown; an ongoing backend call can still block existing
Loader waits and controls queued on the same stripe. The existing progress poll retries final metadata writes for
finished, closed owners and releases an owner only after its final metadata
transaction commits. Closing an owner before load starts preserves its old
checkpoint. A pause/stop using separately restored task state, or a resume/retry,
rejects a conflicting persisted status change; read the current state before
retrying the request. Incremental pause/resume also keeps offsets for inputs not yet read
and already completed counts. This cleanup keeps the existing graph-mode
behavior and does not coordinate concurrent imports from separate processes or
Server instances.

The Loader [startup mode ownership guard](https://github.com/apache/hugegraph-toolchain/pull/812)
is a prerequisite: without it, cleanup after startup fails can reset another
importer's `LOADING` mode. Multi-file jobs start a Loader for each file, so shared
graph-mode ownership also requires the matching Server, Client and Loader ownership
protocol 1 support. The paired [Server change](https://github.com/apache/hugegraph/pull/3331)
and [website documentation](https://github.com/apache/hugegraph-doc/pull/525) describe
that protocol; the Client and Loader must use it too. The startup guard alone does
not keep `LOADING` until every sibling Loader finishes. This lifecycle cleanup does
not implement that protocol, and multi-file validation requires their combined build.

## Local development feedback loop

Run the frontend with third-party source-map noise disabled:

```bash
cd hubble-fe
yarn dev
```

Run the backend incrementally with Java 17 and the Maven daemon:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvnd -pl hubble-be -DskipTests compile dependency:build-classpath \
  -Dmdep.outputFile=/tmp/hubble-be-classpath
mkdir -p /tmp/hubble-dev-home
cd hubble-be
"$JAVA_HOME/bin/java" -Dfile.encoding=UTF-8 \
  --add-opens=java.base/java.net=ALL-UNNAMED \
  -Dhubble.home.path=/tmp/hubble-dev-home \
  -cp "target/classes:$(</tmp/hubble-be-classpath)" \
  org.apache.hugegraph.HugeGraphHubble
```

Stop the previous backend process before restarting after Java changes. Changes
under `hubble-be/src/main/resources` are copied by the next `compile`. The POM
does not configure `spring-boot:run`, so that command is not a supported local
shortcut. Build
the release package with `mvnd package -DskipTests`; these development commands
do not change packaged runtime behavior.

Native Store metrics use an operator-managed exact-origin allowlist in addition
to PD topology and metrics-target discovery. The packaged default
`operations.store.allowed_targets=[http://127.0.0.1:8520,http://[::1]:8520]`
is only for local testing. Production deployments must explicitly list every
trusted Store scheme, hostname or literal address, and port; discovery cannot
add origins to this allowlist. HTTPS origins keep their configured hostname for
TLS SNI and certificate hostname verification.

## Functional Modules Overview

```mermaid
graph TD
    Hubble["HugeGraph-Hubble Platform"]

    Hubble --> Conn["1. Workspace Management<br>(Multi-Graph Connections)"]
    Hubble --> Schema["2. Visual Schema Designer<br>(Vertex, Edge & Index Types)"]
    Hubble --> Load["3. Guided Data Importer<br>(Source Mapping & Task Monitor)"]
    Hubble --> Analyze["4. Graph Analysis & Visualization<br>(Gremlin Console & Visual Exploration)"]
    Hubble --> System["5. System Administration<br>(Async Tasks & Access Control)"]
```

<details>
<summary>ASCII diagram (for terminals/editors)</summary>

```
             ┌──────────────────────────────────────────┐
             │       HugeGraph-Hubble Platform          │
             └────────────────────┬─────────────────────┘
                                  │
      ┌───────────────────────────┼───────────────────────────┐
      ▼                           ▼                           ▼
┌──────────────┐            ┌──────────────┐            ┌──────────────┐
│  Workspace   │            │Visual Schema │            │ Guided Data  │
│  Management  │            │   Designer   │            │   Importer   │
│  - Connect   │            │  - Vertex    │            │  - Sources   │
│  - Switch    │            │  - Edge      │            │  - Mapping   │
│  - Card View │            │  - Index     │            │  - Monitor   │
└──────────────┘            └──────────────┘            └──────────────┘
      │                                                       │
      └───────────────────────────┬───────────────────────────┘
                                  │
                                  ▼
                    ┌──────────────────────────┐
                    │Graph Analysis & Explorer │
                    │ - Gremlin Console        │
                    │ - Algorithm Execution    │
                    │ - Topology Exploration   │
                    └──────────────────────────┘
```
</details>

## Features

- Graph connection management, supporting to easily switch graph to operate
- Graph data load, supporting to load large amounts of data from files into hugegraph-server
- Schema management, supporting to easily perform schema manipulation and display
- Graph analysis and graphical display, supporting to build a query via the gremlin or algorithms with a little effort then will get cool graphical results

## Quick Start

There are three ways to get HugeGraph-Hubble:

- Download the Toolchain binary package
- Source code compilation
- Use Docker image (Convenient for Test/Dev)

And you can find more details in the [doc](https://hugegraph.apache.org/docs/quickstart/hugegraph-hubble/#2-deploy)

### 1. Download the Toolchain binary package

`hubble` is in the `toolchain` project. First, download the binary tar tarball

```bash
wget https://downloads.apache.org/hugegraph/{version}/apache-hugegraph-toolchain-{version}.tar.gz
tar -xvf apache-hugegraph-toolchain-{version}.tar.gz
cd apache-hugegraph-toolchain-{version}/apache-hugegraph-hubble-{version}
```

Run `hubble`:

```
bin/start-hubble.sh
```

Then use a web browser to access `ip:8088` and you can see the `Hubble` page. You can stop the service using bin/stop-hubble.sh.

### 2. Clone source code then compile and install

> Note: Compiling Hubble requires the user's local environment to have Node.js V18.20.8 and yarn installed.

```bash
apt install curl build-essential
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.1/install.sh | bash
source ~/.bashrc
nvm install 18.20.8
```

Then, verify that the installed Node.js version is 18.20.8.

```bash
node -v
```

install `yarn` by the command below:

```bash
npm install -g yarn
```

Download the toolchain source code.

```bash
git clone https://github.com/apache/hugegraph-toolchain.git
```

For the Java 17 candidate, prepare the locked SDK using the [bootstrap instructions](../docs/java17-migration.md#build-the-locked-candidate). Build Client and Loader first, then Hubble with the same isolated Maven repository:

```bash
cd hugegraph-toolchain
python3 -m pip install -r hugegraph-hubble/hubble-dist/assembly/travis/requirements.txt
mvn -Dmaven.repo.local="$candidate_dir/m2" -Dsdk.validation.mode=candidate install -pl hugegraph-client,hugegraph-loader -am -Dmaven.javadoc.skip=true -DskipTests -ntp
(cd hugegraph-hubble && mvn -Dmaven.repo.local="$candidate_dir/m2" -Dsdk.validation.mode=candidate package -Dmaven.javadoc.skip=true -DskipTests -ntp)
cd hugegraph-hubble/apache-hugegraph-hubble-*
```

Run `hubble`

```bash
bin/start-hubble.sh -d
```

### 3. User docker image (Convenient for Test/Dev)

To build the current checkout, run from the Toolchain repository root:

```bash
docker build -f hugegraph-hubble/Dockerfile -t hugegraph/hugegraph-hubble:latest .
```

We can use `docker run -itd --name=hubble -p 8088:8088 hugegraph/hubble` to quickly start [hubble](https://hub.docker.com/r/hugegraph/hubble). An you can visit [hubble deploy doc](https://hugegraph.apache.org/docs/quickstart/hugegraph-hubble/#2-deploy) for more details.

Then we should follow the [hubble workflow doc](https://hugegraph.apache.org/docs/quickstart/hugegraph-hubble/#3platform-workflow) to create the graph.

> Note: 
> 1. The docker image of hugegraph-hubble is a convenience release, but not **official distribution** artifacts. You can find more details from [ASF Release Distribution Policy](https://infra.apache.org/release-distribution.html#dockerhub).
> 
> 2. Recommand to use `release tag`(like `1.0.0`) for the stable version. Use `latest` tag to experience the newest functions in development.

## Doc

[The hubble homepage](https://hugegraph.apache.org/docs/quickstart/hugegraph-hubble/) contains more information about it.

## License

hugegraph-hubble is licensed under Apache 2.0 License.

## Notice

The `hubble-fe` folder contains the frontend code, including all related source code for the frontend.

The `hubble-be` folder contains the backend code, including all related source code for the backend.

The `hubble-dist/assembly` folder contains distribution resources. Packaging the frontend and backend produces the deployment archive under `target/`.

## Runtime and metadata

Hubble 1.8 requires Java 17 and uses Spring Boot 3 with H2 2.x for metadata. MySQL remains available as a Loader source. When upgrading, use a new metadata database and retain the previous database with its matching Hubble release; automatic metadata migration is not supported. See [metadata storage](docs/metadata-storage.md) for configuration and troubleshooting.

The startup script includes `--add-opens=java.base/java.net=ALL-UNNAMED` for the embedded Loader's Hive ORC reader. Include this option when using a custom Java launch command. Packaged archives are generated under `target/` and exclude local runtime data.

## Graph switching

Query execution waits until the graph context matches the current route, including keyboard shortcuts during a graph switch. Standalone Server 1.5 supports core graph, schema and data operations; GraphSpace management and PD mode require a newer server.
