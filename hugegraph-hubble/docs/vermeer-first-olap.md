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

## Verification

Based on remote master `eef5afd29762f394667494b726afdce1b938214d`.
Product code verified at `b1dc8380deb5e5967e568e7318546a2ce54aea48`:

- Four focused Jest suites / 56 tests passed, including graph A→B→A opt-in reset,
  late A capability after B opted in, all nine engine-specific documentation URLs,
  supported algorithm sets, and locale copy rules.
- TypeScript and targeted ESLint passed. Initial implementation also passed six
  suites / 47 tests, locale consistency, and the Vite production build.
- Chromium exercised 10 checks with 12 captures: authenticated login, anonymous
  account/GraphSpace visibility, graph context switching, loaded and unloaded
  Vermeer, PageRank submission route and payload, unsupported Louvain, collapsed
  Computer opt-in and unavailable capability, return-to-graph opt-in reset, and
  both task engine labels.
  Uncaught page errors, caught error toasts and unexpected API routes were zero.

The browser uses explicit API fixtures against the actual running frontend.
It verifies interaction and routing, not live-cluster authentication, actual
Vermeer computation or result persistence. This UI has graph/GraphSpace selectors,
not a Server deployment selector; real deployment switching remains unverified.
The [structured receipt](vermeer-first-browser-report.json) records the checks.
Screenshot files stay outside Git; see [PR #91](https://github.com/hugegraph/hugegraph-toolchain/pull/91)
for screenshot evidence status.

To reproduce, start the frontend with `node node_modules/vite/bin/vite.js --port 3004`
from `hubble-fe`, then run from the repository root:

```sh
node hugegraph-hubble/hubble-dist/assembly/travis/vermeer_first_browser.cjs
```

The runner writes screenshots and its report to `/tmp/hubble-vermeer-first-evidence`.
`HUBBLE_UI_BASE`, `HUBBLE_UI_EVIDENCE_DIR`, and `CHROMIUM_PATH` can override the frontend
URL, output directory, and Chromium executable. The report records the checked-out commit.
