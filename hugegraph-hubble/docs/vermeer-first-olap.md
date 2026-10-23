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
Computer explicitly.

Vermeer algorithm links point to Vermeer documentation. Interactive traversal
links keep their API-specific references. Task types name the actual backend.
See [connection and service-token configuration](vermeer-development.md#connection-and-task-contracts)
for `vermeer.url` and `vermeer.token_file`; this UI change does not manufacture a
configured or healthy Vermeer service.

## Verification

Based on remote master `eef5afd29762f394667494b726afdce1b938214d`.
Product code verified at `4b2596d9188920d653686390ba80690a0f3e0295`:

- Six focused Jest suites / 47 tests passed, including capability opt-in, late
  response isolation, supported algorithm sets, submission and task type mapping.
- TypeScript, locale consistency, targeted ESLint and Vite production build passed.
- Independent code review found no actionable issues.
- Chromium exercised 9 checks with 11 captures: authenticated login, anonymous
  account/GraphSpace visibility, graph context switching, loaded and unloaded
  Vermeer, PageRank submission route and payload, unsupported Louvain, collapsed
  Computer opt-in and unavailable capability, and both task engine labels.
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
`HUBBLE_UI_BASE` and `CHROMIUM_PATH` can override the frontend URL and Chromium executable.
