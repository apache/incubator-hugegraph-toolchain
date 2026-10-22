# Hubble and Vermeer development environment

This environment runs Hubble against the Go module in
`/workspace/hugegraph-computer/vermeer`. It retains editable source, the Go build
cache, and a development binary. The Java Computer/Kubernetes stack is not needed
for this workflow.

The prepared components are Go 1.23.7, JDK 17, the existing candidate HugeGraph SDK
at `86c05ef9c5d417bd37eab21d4ecd5af82bab17ef`, and separately running PD, Store,
Server, Vermeer Master/Worker, and Hubble. The development helper pins all three
database images by digest. Vermeer was checked out from
`hugegraph/hugegraph-computer` at
`051c6acc5679db6942ca8142754fc048d6d9d127` ([Computer PR #19](https://github.com/hugegraph/hugegraph-computer/pull/19)); local edits are retained when refreshing
the environment.

Run these commands from `/workspace/hugegraph-toolchain`:

```bash
# Refresh/build the retained environment after the base Java/frontend install.
python3 hugegraph-hubble/hubble-dist/assembly/travis/vermeer_dev.py prepare

# Start services and verify API access and Worker registration.
python3 hugegraph-hubble/hubble-dist/assembly/travis/vermeer_dev.py start

# Seed a dedicated graph, load it, run PageRank, and verify values and task filters.
python3 hugegraph-hubble/hubble-dist/assembly/travis/vermeer_dev.py smoke
```

The smoke test uses `DEFAULT/vermeer_demo`: six vertices and seven directed edges.
It creates the graph through the Server API so its configuration and schema are
available to Store via PD. It can be repeated; it reloads an existing Vermeer graph.
The fixture is separate from the default `hugegraph` graph. Do not change a live
cluster's metadata namespace while reusing its data; old schema IDs can make raw
Store scans fail even when another graph's REST requests work.

After Go edits:

```bash
source /workspace/.onboarding/vermeer-env.sh
cd /workspace/hugegraph-computer/vermeer
go test ./apps/common -run TestPDAuthority -count=1
cd /workspace/hugegraph-toolchain
python3 hugegraph-hubble/hubble-dist/assembly/travis/vermeer_dev.py build --component vermeer
python3 hugegraph-hubble/hubble-dist/assembly/travis/vermeer_dev.py stop --component vermeer
python3 hugegraph-hubble/hubble-dist/assembly/travis/vermeer_dev.py start
python3 hugegraph-hubble/hubble-dist/assembly/travis/vermeer_dev.py smoke
```

For Java edits use `build --component hubble`, then `stop --component hubble` and
`start`. The helper uses the existing Maven wrapper, isolated candidate SDK
repository, and `sdk.validation.mode=candidate`; it does not substitute released
SDK artifacts. The Go development build uses `-tags=dev`, so embedded Vermeer UI
asset generation is unnecessary for Hubble/API integration.

Activate the frontend with `source /workspace/.onboarding/frontend-env.sh`, then
run `pnpm dev` from `hugegraph-hubble/hubble-fe`. Its API proxy targets Hubble on
port 8088. In Hubble select `DEFAULT` and `vermeer_demo`, switch to batch analysis,
load/reload into Vermeer, and run PageRank. Open the async task's result to inspect
the vertex values. The verification artifacts are under
`/workspace/.onboarding/integration/`.

![Vermeer computation results in Hubble](images/vermeer/async-results.png)

Live processes do not survive environment publication. Run `start` again in each
restored environment; rerun `smoke` to produce current queryable computation
results. Master/Worker in-memory result caches are temporary. Database data,
Vermeer metadata, and local output files are retained, but historical task status
alone does not guarantee its in-memory query results remain available.

## Connection and task contracts

Set `vermeer.url` in Hubble's properties to the Master HTTP origin. If Master uses
token authentication, put its separate service token in a protected file and set
`vermeer.token_file`. Hubble does not reuse its HugeGraph login token for Vermeer.
An empty `vermeer.url` retains the existing Dashboard/Server-proxy path and its
token-output capability restriction.

The direct adapter uses the routes implemented by Vermeer:

| Operation | Vermeer route |
| --- | --- |
| Load/compute | `POST /api/v1/tasks/create` |
| Task status | `GET /api/v1/task/{id}` |
| Result values | `GET /api/v1/tasks/value/{id}?cursor=0&limit=1000` |
| Graph status | `GET /api/v1/graphs` |
| Cancel | `GET /api/v1/task/cancel/{id}` |

Hubble's graph name is encoded as `graphspace-graph` within the configured Vermeer
service space. Direct task IDs are negative in Hubble; native Vermeer IDs remain
positive. Server task IDs retain their original meaning. All direct task and
result operations check the selected graph. Because raw Store scans bypass
Server's per-label permissions, authenticated direct access requires a HugeGraph
super administrator; anonymous mode follows the Server's anonymous graph access.

The Hubble direct adapter supports the nine algorithms shown by its Vermeer
forms. Results are queried through the task API and are not written back to
HugeGraph properties. The task result view returns up to 1000 values with a
cursor; additional pages are available at Hubble's
`GET /api/v1.3/graphspaces/{space}/graphs/{graph}/async-tasks/{negative-id}/values?cursor=N&limit=1000`.
Hubble provides next/previous controls for these result pages. Native Vermeer does not expose task-history deletion, so Hubble disables that action for direct tasks.

PD, Store and Server use the metadata cluster name `hg`. Both Vermeer processes
read PD's shared secret from `VERMEER_PD_PASSWORD_FILE` or `pd_password_file`,
outside task parameters. For an authenticated Server, also set
`VERMEER_HUGEGRAPH_CREDENTIALS_FILE` (or Worker config `hugegraph_credentials_file`)
to a protected JSON file containing a dedicated Schema reader's `username` and
`password`. This deployment identity needs Schema read access to the selected
graph; Hubble does not copy the caller's login into task history. An explicitly
configured unreadable or malformed credential file fails the load. The helper
generates a private PD deployment file once
and reuses it. It also allocates Worker group `default` to service space
`$DEFAULT`; without that allocation submitted tasks remain queued.

## Retained configuration

Environment variables may set `VERMEER_SOURCE_DIR`, `VERMEER_URL`,
`VERMEER_PD_PASSWORD_FILE`, `VERMEER_GRAPH`, `GOPATH`, `GOMODCACHE`, and `GOCACHE`.
The helper defaults to paths under `/workspace/.onboarding`. Go archive checksum,
module checksums, TLS verification, and the existing SDK provenance checks remain
enabled. Large Go proxy archives may require `storage.googleapis.com` in the
environment allowlist. The prepared module cache includes checksum-verified
dependencies fetched from their official GitHub source where object-storage
access was unavailable.


## Browser verification

After starting the frontend, run the checked-in browser acceptance script with
an installed Chromium (or omit the executable path to use Playwright's browser):

```bash
source /workspace/.onboarding/frontend-env.sh
BROWSER_EXECUTABLE_PATH=/usr/bin/chromium \
  node hugegraph-hubble/hubble-dist/assembly/travis/vermeer_browser_smoke.cjs
```

The script checks actual PageRank submission, terminal task state, result values,
graph switching and account visibility. It expects the six-vertex `vermeer_demo`
fixture and writes screenshots plus a JSON report under
`/tmp/vermeer-browser-evidence` (override with `VERMEER_BROWSER_ARTIFACTS`).
For a deliberate slow-load check on this dedicated development Worker, set
`VERMEER_WORKER_PID_FILE` to its PID file. The script pauses that Worker for
4.5 seconds and always resumes it in `finally`; never use this option against a
shared deployment.

To verify a separately prepared authenticated fixture, set
`VERMEER_TEST_CREDENTIALS` to a protected JSON file containing `username`,
`password`, `observer` and `observer_password`. The observer must have graph read
access. The script signs both users in through the UI, checks that only the
administrator can use direct Vermeer, verifies that the observer's Server task
list remains usable, and checks rejection of a direct compute request. It does
not create accounts or change permissions.

Recorded verification uses the rebuilt local services, with no mocked API
responses: all nine supported algorithms returned six vertex values; Chrome
verified anonymous/admin/observer flows and a delayed Worker load. The final complete Hubble backend suite passed 869 tests, including the direct
Vermeer permission and task regressions. The focused
frontend suites, lint, i18n, TypeScript and production build passed. The separate
Go regression includes deterministic storage-initialization failure and retry.

![Slow Worker load remains visible until the submitted task finishes](images/vermeer/slow-load.png)

![Authenticated administrator inspecting computation results](images/vermeer/auth-admin-results.png)

![Observer retains the Server task page without direct Vermeer actions](images/vermeer/auth-observer-tasks.png)


Authenticated fresh-load verification used a separate observer identity for
Worker Schema reads. A dedicated 1001-vertex graph loaded through PD/Store;
its degree results returned 1000 values then one distinct value, and Chromium
verified both next and previous page controls without page errors.

![Final page of a real 1001-vertex computation](images/vermeer/paged-results.png)
