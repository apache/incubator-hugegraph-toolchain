# Vermeer as the default OLAP entry

The algorithm sidebar explicitly identifies Vermeer as the default OLAP engine.
An available, loaded Vermeer graph exposes the existing nine supported algorithms:
PageRank, WCC, LPA, Degree Centrality, Closeness Centrality, Betweenness Centrality,
Triangle Count, K-Core and SSSP. This matches `DirectVermeerService` and does not
claim every native Vermeer algorithm is supported by Hubble. In particular,
Louvain is not added to the Vermeer interface.

When Vermeer is unavailable, the sidebar explains deployment configuration without
automatically falling back to Computer. The Computer compatibility entry starts
collapsed and checks that graph's capability only after expansion. An unavailable
Computer capability cannot expose runnable forms. Graph changes invalidate the
opt-in and ignore late responses from the previous graph. Existing deployments
can still use the compatibility entry; its documentation and task labels identify
Computer explicitly. Returning to a previously selected graph also requires a new
explicit opt-in; its earlier capability result is not reused.

Vermeer algorithm links point to Vermeer documentation. Interactive traversal
links keep their API-specific references. Task types name the actual backend.
See [connection and service-token configuration](vermeer-development.md#connection-and-task-contracts)
for `vermeer.url` and `vermeer.token_file`; this UI change does not manufacture a
configured or healthy Vermeer service.

## Browser checks

The browser runner uses explicit API fixtures against the running frontend to
check engine selection, graph scope, supported algorithms, task routing, and
authenticated or anonymous visibility. It checks UI interaction and routing; it
does not establish live-engine computation, production authentication or result
persistence. Graph and GraphSpace selectors do not switch Server deployments.

To reproduce, start the frontend with `node node_modules/vite/bin/vite.js --port 3004`
from `hubble-fe`, then run from the repository root:

```sh
node hugegraph-hubble/hubble-dist/assembly/travis/vermeer_first_browser.cjs
```

The runner writes screenshots and its report to `/tmp/hubble-vermeer-first-evidence`.
`HUBBLE_UI_BASE`, `HUBBLE_UI_EVIDENCE_DIR`, and `CHROMIUM_PATH` can override the frontend
URL, output directory, and Chromium executable. The report records the checked-out commit.
