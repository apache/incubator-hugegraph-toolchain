# GraphSpace interaction and CRUD audit — 2026-10-23

This change starts from Toolchain `origin/master`
`eef5afd29762f394667494b726afdce1b938214d`. It fixes GraphSpace card navigation,
editor request races, mutation recovery and pagination. Existing quotas and
creation defaults are preserved; this frontend change does not resolve
inconsistent configuration returned by different Server caches.

## Evidence and limits

The real Hubble frontend ran in Chromium at 1440 × 1000, in Chinese, with stateful
mocked APIs. Fixtures explicitly simulate authenticated capabilities, successful
CRUD, HTTP 503 failures, a delayed save and multi-page results. Each screenshot
was captured and inspected before proceeding. All 20 checks passed with zero
uncaught page errors and zero unexpected API routes. The four successful writes
were one create, one update and two deletes; failed attempts did not mutate the
fixture. See the [machine-readable report](assets/graphspace-crud-20261023/report.json).
Screenshots are attached to the [PR description](https://github.com/hugegraph/hugegraph-toolchain/pull/88),
not stored in the repository; the filenames below identify those captures.

These are browser interaction checks, not live-cluster acceptance or a production
performance test. Login, anonymous mode, graph-context switching and account /
GraphSpace visibility also passed in the separate combined-worktree 10-scenario
deployment runner; its general evidence is maintained with that runner. The local
unauthenticated backend did not advertise GraphSpace management, so this audit
does not claim live GraphSpace CRUD coverage. Deployment-specific quota bouncing
still requires checking the actual Server responses and routing.

## Confirmed findings

1. **Card header has a navigation dead zone (fixed, medium).** Only the title and
   body handled clicks, leaving the date and header whitespace inactive. Card
   navigation now covers that area while excluding buttons and dropdown menus.
   Focused tests check each surface and verify that menu actions do not navigate;
   the combined deployment browser runner checks the header interaction.
2. **Old editor requests can affect a newer editing session (fixed, high).**
   Detail responses previously wrote into the shared form without checking the
   current session. Request tokens now ignore old detail/user responses and save
   completions. Fields remain disabled until the current detail loads. Deferred
   response tests cover switching spaces, closing then creating, and an old save
   completing while a newer editor is open. This guards frontend races; it does
   not conceal stale values supplied by a Server.
3. **Saving replaces the form and collapses advanced fields on failure (fixed,
   medium).** Ant Design's `Modal.loading` rendered a skeleton instead of the
   form. [Before (before/06-save-failure.png)](https://github.com/hugegraph/hugegraph-toolchain/pull/88), the
   resource section collapsed after a rejected save. `confirmLoading` keeps the
   form mounted; [after (06-save-failure.png)](https://github.com/hugegraph/hugegraph-toolchain/pull/88), quota
   25 remains visible and retryable. A submission lock starts before validation;
   [pending controls (07a-saving.png)](https://github.com/hugegraph/hugegraph-toolchain/pull/88) prevent edits,
   duplicate saves and dismissal while the write is pending.
4. **Delete failure closes the confirmation and loses its context (fixed,
   medium).** The original callback did not retain the dialog; simply returning
   a rejected promise additionally produces an uncaught error in this installed
   Ant Design version. Manual close handling now keeps the
   [failed confirmation (13-delete-failure.png)](https://github.com/hugegraph/hugegraph-toolchain/pull/88)
   open, restores controls for retry and closes only on success. The confirmation
   also names the target space. Compare
   [before (before/13-delete-failure.png)](https://github.com/hugegraph/hugegraph-toolchain/pull/88).
5. **Searching from page two falsely reports no matches (fixed, high).** The old
   query retained page two after the result shrank to one record:
   [before (before/16-search-page-two.png)](https://github.com/hugegraph/hugegraph-toolchain/pull/88).
   Search/date changes now reset page one. Responses also clamp an invalid page
   after deletion, so deleting the last record on page two recovers the previous
   page. See [search recovery (16-search-resets-page.png)](https://github.com/hugegraph/hugegraph-toolchain/pull/88)
   and [deletion recovery (19-delete-last-page.png)](https://github.com/hugegraph/hugegraph-toolchain/pull/88).
6. **Table filtering gives an ambiguous empty state (fixed, low).** Table mode
   showed generic “暂无数据” even when a filter hid existing spaces. It now shares
   the card view's explicit no-matches / no-spaces wording and suppresses an empty
   message while loading or in error. Compare
   [before (before/10-filter-empty.png)](https://github.com/hugegraph/hugegraph-toolchain/pull/88) and
   [after (10-filter-empty.png)](https://github.com/hugegraph/hugegraph-toolchain/pull/88).

## Browser walkthrough

| Step | Observed result | Screenshot |
| --- | --- | --- |
| 01 | Authenticated cards and visible keyboard focus on Create | [01-list.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 02 | Invalid name blocks creation, inline explanation and `aria-invalid=true` | [02-invalid-create.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 03 | Failed create keeps entered values and allows retry | [03-create-failure.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 04 | Valid retry creates one space, preserving the existing 10240 GB default | [04-created.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 05 | Alias and advanced quota can be edited; keyboard focus remains visible | [05-edit-resources.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 06 | Failed save keeps quota 25 and advanced section expanded | [06-save-failure.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 07a | Pending save keeps the form mounted; repeated click submits once | [07a-saving.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 07b | Saved card shows the new alias and 25 GB | [07-saved.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 08 | Cancelled edit leaves saved alias and quota unchanged | [08-cancel-edit.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 09 | Table mode shows the same saved values | [09-table-mode.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 10 | No-match filter shows specific empty-state wording | [10-filter-empty.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 11 | Clearing filter and switching to cards restores records | [11-filter-recovery.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 12 | Confirmation names the target; cancel preserves it | [12-delete-cancel.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 13 | Failed delete shows one error, retains dialog and record | [13-delete-failure.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 14 | Retrying the same confirmation successfully deletes the target | [14-deleted.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 15 | Second page with one record is reachable | [15-page-two.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 16 | Search from page two returns the first-page matching record | [16-search-resets-page.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 17 | List failure has a distinct error state and retry action | [17-list-failure.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 18 | Retry restores the card list | [18-list-retry.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |
| 19 | Delete the only page-two record and recover page one | [19-delete-last-page.png](https://github.com/hugegraph/hugegraph-toolchain/pull/88) |

## Responsiveness and accessibility observations

The grouped interactions completed in 72–1706 ms, excluding screenshot capture
and deliberate inspection pauses. The initial authenticated-list group took
1390 ms and the reload/page-two group 1706 ms; these contain several automation
actions and assertions. The final document recorded eight long tasks of
63–149 ms. No persistent spinner, lost save fields or duplicate write was
observed. Development mode, mocked network latency and this small fixture cannot
establish production latency or large-dataset scalability.

Visible focus, Enter-based search, inline invalid-state semantics and disabled
mutation controls were checked. Remaining audit scope includes screen-reader
announcements, complete keyboard traversal, contrast measurement, mobile / zoom
layouts and production network conditions; these checks are not accessibility
certification. Full-page captures include the fixed navigation/footer as rendered
by Chromium. They are not redesigned mockups.

## Reproduce

Run the Hubble frontend locally with its normal development command, then:

```sh
HUBBLE_URL=http://127.0.0.1:3000 \
  node hugegraph-hubble/hubble-dist/assembly/travis/run_ui_graphspace_crud.js
```

The runner requires the frontend's Playwright dependency and `/usr/bin/chromium`.
It writes evidence to the system temporary directory's `hubble-graphspace-crud`
folder by default. Set `HUBBLE_UI_EVIDENCE_DIR` to choose another output folder. Set
`HUBBLE_AUDIT_INTERACTIVE=1` to pause after each capture for inspection and press
Enter to continue. It never sends CRUD requests to a real backend.

Five focused GraphSpace Jest suites passed (68 tests); GraphSpace ESLint also
passed. An independent agent reviewed the six changed source/test files,
including stale request handling, validation locking, pagination and Ant Design
manual confirmation behavior, and reran those suites with no actionable finding.
