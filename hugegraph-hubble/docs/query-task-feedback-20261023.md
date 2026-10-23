# Query and submitted-task feedback regression verification

Date: 2026-10-23 (Asia/Taipei). Base: `eef5afd29762f394667494b726afdce1b938214d`.

## Behavior

- Submission links retain graph space, graph, and task ID. The task page immediately identifies an accepted task while status is synchronizing, without inventing a queued/running status.
- Metadata-only task discovery retries every second for at most 30 checks, then pauses with a retry action. Once found, normal five-second refresh resumes. Permission/network failures stop automatic discovery and remain visible; stale responses cannot overwrite a different graph.
- An exact task-ID lookup supplements ordinary list searches when a task is in the scheduler before its persisted list entry is visible. Name matching and status/type filters are preserved. Both ID lookup paths treat only a HugeGraph missing-task 404 naming a requested ID as not yet discovered. Missing graphs, unavailable routes, unrecognized 404 responses, permission errors and service errors still propagate. For a mixed batch, only the confirmed missing ID is excluded before querying the remaining metadata; each fallback shrinks the batch, and successful batches use one request.
- Metadata ID requests accept at most 32 IDs, including Vermeer IDs. Oversized batches are rejected before upstream calls, bounding missing-task fallback requests independently of caller input.
- Failed tasks show error details. Result-size failures explain the actual server-reported size/limit and recommend fewer records or selected properties. Server result-storage protection is unchanged. Recovery links select the actual Gremlin/Cypher task language and restore that language’s draft; unknown task types and Cypher tasks on a connection without Cypher support have no misleading query link.
- Routine query limits appear as a quiet canvas note; actual truncation uses one compact notice with details on demand. Unknown whole-graph counts are not displayed as denominators.
- G6 receives writable copies of raw nested data. Public controller snapshots stay immutable, preventing native history alignment from throwing when the same IDs are queried again.

## Validation

- The isolated frontend source at `f8dc78a9` passed 27 frontend suites / 575 tests covering query previews, async tasks, graph context, request contracts, language recovery and visualization ownership/runtime behavior.
- Backend: `AsyncTaskServiceTest` — fifteen passed after the mixed-ID follow-up; includes scheduler-before-list discovery, name/status preservation, missing IDs on both search and polling paths, propagation of unrelated 404 and non-404 failures, metadata-only Vermeer lookup, mixed/multiple missing IDs, bounded all-missing lookup and unrelated errors during fallback.
- The same isolated frontend source passed full frontend ESLint, locale-key validation, TypeScript checking and the production Vite build. The build retains the existing large-chunk warning.
- The scoped-404 follow-up also passed all 16 task-page tests and focused ESLint: a normalized empty result keeps discovery active, while business 400, permission 403 and service 503 stop polling with a visible failure. Frontend behavior code is unchanged.
- Chromium 154.0.8037.57 exercised the running UI against controlled API responses at 2026-10-23 20:39:31 Asia/Taipei. Scoped task discovery, delayed visibility, failed-result guidance, default-limit presentation, truncation details, graph switching, and same-ID re-query were checked. This is browser interaction evidence, not real-cluster acceptance or a deployment claim.

- Review follow-up: Chromium 154 completed four additional UI checks at 2026-10-23 21:13:42 Asia/Taipei. Clicking the recovery link selected the actual Cypher/Gremlin editor and restored its scoped draft; unknown and missing task types showed no recovery link. No page errors, unexpected API calls or error toasts occurred.

- CI follow-up: all 23 UI-copy punctuation checks passed after removing terminal periods from the five changed preview hints in both locales.

## Screenshots

Browser screenshots are linked from the [PR description](https://github.com/hugegraph/hugegraph-toolchain/pull/86) to the [regression Page](https://chatgpt.com/space/page_62cf6a23a284819196000f124fdd6990), which requires Page access. Per the explicit user requirement, these review artifacts are not stored in Git. GitHub-native inline image attachments remain unavailable through the current connector.
