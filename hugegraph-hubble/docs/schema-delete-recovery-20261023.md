# Schema deletion recovery

Deleting a property key, vertex label or edge label previously closed the confirmation
when the request failed. The row remained, leaving only a temporary error message and
requiring the user to reopen the confirmation to retry.

This Hubble-only fix keeps the selected GraphSpace/graph and names visible. It prevents
duplicate submission and cancellation during an attempt, and reports completion only
when a complete schema listing confirms the targets are absent. Before every deletion
attempt, it filters out already removed names and repeats property/vertex usage checks.
Batch deletion explicitly disables silently skipping used targets.

A lost response is reconciled before retrying. Async vertex/edge deletion can be accepted
before its response is lost, or before a later batch item fails. If targets still exist,
the dialog shows that the result is unconfirmed. **Check status** only reads the schema;
**Retry deletion** explicitly repeats usage checks and submits remaining targets. This
keeps failed async tasks recoverable without silently resubmitting while checking status.
Errors share a localized notification key. No Server/PD/Store code or cache protocol changes.

## Trusted schema snapshots

A throwing completion callback does not leave an already completed confirmation open.
Duplicated or missing schema identities are rejected before interpreting absence or
sending a deletion. Missing totals, truncated lists and failed requests remain
untrusted. This avoids treating a malformed full-list response as proof that a
selected target has already disappeared.

## Running browser checks

With frontend dependencies installed and Vite running:

```bash
cd hugegraph-hubble/hubble-fe
pnpm test:ci src/pages/Meta/delete-recovery.test.js src/pages/Meta/delete-error-owner.test.js src/api/manage-contract.test.js
cd ../..
SCHEMA_ONLY=1 HUBBLE_URL=http://127.0.0.1:3000 \
  node hugegraph-hubble/hubble-dist/assembly/travis/run_ui_data_crud.js
```

Omit `SCHEMA_ONLY` for the broader UI audit. Set `PERF_ONLY=1` for warm repeated
100/300-vertex rendering. The default screenshot directory is temporary and outside Git;
set `HUBBLE_UI_EVIDENCE_DIR` to preserve a run. Performance uses five repeats per size
and waits for the current query/canvas busy-to-idle cycle, response completion and two
animation frames. It includes Playwright and browser rendering overhead against in-memory responses.
It does not measure Server query performance, storage I/O or cluster throughput.
