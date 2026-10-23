# Schema deletion recovery: 2026-10-23

Deleting a property key, vertex label or edge label previously closed the confirmation
when the request failed. The row remained, leaving only a temporary error message and
requiring the user to reopen the confirmation to retry.

This Hubble-only fix keeps the confirmation open on failure, restores its actions for
retry or cancellation, and closes it only after success. The selected GraphSpace/graph
and names are visible for both individual and batch deletion. While the request is in
flight, a local guard prevents duplicate submission and the dialog cannot be canceled.
Errors retain a single localized message; raw backend responses are not displayed.

## Browser evidence

Screenshots are kept outside Git; the evidence destination is the
[PR #90 description](https://github.com/hugegraph/hugegraph-toolchain/pull/90).

The reproduction was captured before editing the source. Final screenshots were inspected
from Chromium 154.0.8037.57, 1440×1000 viewport, against an isolated checkout of
`origin/master` (`eef5afd29762f394667494b726afdce1b938214d`) plus only this schema fix.
API calls were intercepted with explicit in-memory fixtures, including injected DELETE
500 responses. No production graph or credentials were used. This verifies browser
interaction and rendering; it is not live-cluster acceptance or backend persistence.

1. **Before:** a failed property deletion dismissed the confirmation.

2. **Property:** required-name validation blocks creation; keyboard submission creates
   one row. Canceling deletion sends no DELETE. A failed DELETE preserves the target
   and confirmation; retry succeeds and the row disappears.

3. **Vertex label:** injected failure preserves `DEFAULT/hugegraph` and `AuditVertex`;
   retry succeeds and removes that row.

4. **Edge label:** injected failure preserves `DEFAULT/hugegraph` and `AuditEdge`;
   retry succeeds and removes that row.

The same runner additionally exercised canvas vertex creation and property editing,
verified the edited value after another query, and exercised empty results and query
failure/recovery. All seven checks passed with zero browser page errors and zero
unexpected API routes. Canvas removal is a view operation; persistent vertex/edge
removal is not claimed by this audit. See the [machine-readable report](assets/schema-delete-20261023/browser-report.json).

## Validation and reproduction

Focused Jest coverage verifies business and transport failures, one error notification,
duplicate-submission prevention, and a successful retry for all three schema tables.
Six tests passed; ESLint passed for the changed application and test files.

With frontend dependencies installed and Vite running:

```bash
cd hugegraph-hubble/hubble-fe
pnpm test:ci src/pages/Meta/delete-recovery.test.js src/pages/Meta/delete-error-owner.test.js
cd ../..
SCHEMA_ONLY=1 HUBBLE_URL=http://127.0.0.1:3000 \
  node hugegraph-hubble/hubble-dist/assembly/travis/run_ui_data_crud.js
```

Omit `SCHEMA_ONLY` to run the broader UI audit. Set `PERF_ONLY=1` for warm repeated
100/300-vertex query rendering. The default evidence directory is temporary and stays outside Git. Set
`HUBBLE_UI_EVIDENCE_DIR` to preserve each run.
Performance samples use five repeats per size and measure clicking Execute through
the updated count, idle canvas and two animation frames. They include Playwright and
browser rendering overhead against in-memory API responses; they do not measure
Server query performance, storage I/O or cluster throughput.
