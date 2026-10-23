# Schema deletion recovery: 2026-10-23

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

## Browser evidence

Screenshots stay outside Git at the user's request; the evidence destination is the
[PR #90 description](https://github.com/hugegraph/hugegraph-toolchain/pull/90).
The README links this report and its [machine-readable results](assets/schema-delete-20261023/browser-report.json).

The initial failure was reproduced before editing the source. Chromium 154.0.8037.57,
1440×1000, exercised an isolated checkout of `origin/master`
(`eef5afd29762f394667494b726afdce1b938214d`) plus only this schema fix.
All APIs use explicit in-memory fixtures, including injected HTTP 500 and lost responses.
No production graphs or credentials were used. This verifies UI interactions, not
live-cluster acceptance, backend persistence or server cache consistency.

The browser checks cover:

- Authenticated GraphSpace/account visibility and switching GraphSpace and graph through
  the header controls, with the subsequent query verified against the selected API scope.
- Required-name validation and keyboard property creation; cancel sends no DELETE.
- Failed property, vertex-label and edge-label deletion preserves the target and dialog;
  explicit retry removes the corresponding row.
- Each schema type survives a response lost after removal without repeating its DELETE.
- A partially applied property batch retries only remaining names; a new usage reference
  blocks retry until released, and requests use `skip_using=false`.
- Accepted and response-lost async deletions remain unconfirmed; checking status sends no
  DELETE, while explicit retry revalidates and completes.
- Non-authenticated GraphSpace visibility, hidden account/GraphSpace administration, and
  actual property creation/deletion in the browser.
- Canvas vertex creation, property editing/readback, empty and failed query recovery,
  and repeated 100/300-vertex rendering.

All 15 browser checks passed with zero page errors and unexpected API routes.
Canvas removal is a view operation; persistent vertex/edge removal is not claimed.

## Validation and reproduction

The three focused suites passed 36 tests, including failed/incomplete listings, lost
responses, partial batches, usage changes, asynchronous pending outcomes and duplicate
submission. Full ESLint, locale validation, TypeScript and the production Vite build passed.
The existing large-chunk build warning remains.

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


## Review follow-up: trusted schema snapshots

Review `5630665125` identified that a throwing completion callback could leave an
already completed confirmation open; commit `cc3540f9` fixed that behavior. The
follow-up also rejects duplicated or missing schema identities before interpreting
absence or sending a deletion. Missing totals, truncated lists and failed requests
remain untrusted. This avoids treating a malformed full-list response as proof that
a selected target has already disappeared.

The focused review run passed 17 tests in three suites, targeted ESLint, locale
validation, TypeScript and the production build. The schema-only Chromium rerun
passed 13 scenarios, including malformed-list rejection followed by a successful
explicit retry, with zero page errors or unexpected API routes. The final run is
recorded in [the review receipt](assets/schema-delete-20261023/review-followup-browser-report.json).
These 13 scenarios do not rerun the broader canvas/performance cases above.
Screenshots remain outside Git; the review evidence is intended for the PR description.
