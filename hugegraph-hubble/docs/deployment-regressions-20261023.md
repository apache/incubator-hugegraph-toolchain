# Deployment regressions reported on 2026-10-23

This follow-up starts from Toolchain `origin/master` at
`eef5afd29762f394667494b726afdce1b938214d`. The Server investigation uses
Apache HugeGraph `origin/master` at
`8338402826e5f9d05ed6b87784fe334659e381c8`. Both remote heads were fetched
and checked before implementation. These Hubble changes have not been deployed
to the reporting cluster. Per the requested scope, systemic Server cache work is
deferred to the existing [Schema cache synchronization design](https://chatgpt.com/space/page_327bed139ea08191bfc87a2d9b2de60a).
The separately authorized, bounded GraphSpace notification fix is proposed in
[Server PR #332](https://github.com/hugegraph/hugegraph/pull/332).

## Split delivery

- [Toolchain #86](https://github.com/hugegraph/hugegraph-toolchain/pull/86): query previews, counts, G6 data ownership and asynchronous task discovery/failures.
- [Toolchain #87](https://github.com/hugegraph/hugegraph-toolchain/pull/87): actionable PD/Server/Store monitoring diagnostics.
- [Toolchain #88](https://github.com/hugegraph/hugegraph-toolchain/pull/88): GraphSpace navigation, edit races and CRUD recovery.
- [Toolchain #89](https://github.com/hugegraph/hugegraph-toolchain/pull/89): Vermeer load permission configuration guidance.
- [Toolchain #90](https://github.com/hugegraph/hugegraph-toolchain/pull/90): recoverable property/vertex-label/edge-label deletion.
- [Toolchain #91](https://github.com/hugegraph/hugegraph-toolchain/pull/91): explicit Vermeer-first OLAP entry and optional Computer compatibility entry.
- [Toolchain #93](https://github.com/hugegraph/hugegraph-toolchain/pull/93): minimap bounds and viewport-mask synchronization after canvas resize.

Screenshots remain outside Git. Browser evidence is linked from the PR
descriptions and the [issue record](https://chatgpt.com/space/page_62cf6a23a284819196000f124fdd6990).
The runners write temporary output unless an evidence directory is supplied.
The combined acceptance runner requires the relevant split fixes together;
this documentation PR alone does not change application behavior.

## Findings and changes

| Issue | Confirmed cause | Change |
| --- | --- | --- |
| 23: GraphSpace card header does not open the space | Only the title text and body handled clicks; the date and header whitespace did not. | The card handles navigation consistently and excludes menus and independent controls. |
| 24: Quotas alternate after editing and refreshing | Server caches GraphSpace configuration. It emitted ADD/UPDATE notifications but subscribed only to REMOVE. Different Server instances could therefore return different cached quotas. | The bounded listener fix is a separate Server PR. The Hubble editor independently ignores stale detail responses after closing or changing spaces. The deployment's exact request path still needs verification. |
| 25: Very small utilization and suggested new defaults | The current creation default is 10240 GB. The screenshot's 917 GB is not that code default; its deployment provenance is unknown. | Preserve existing positive quotas and accurate percentages. No global default or existing cluster quota is changed by this fix. |
| 26: Counts show `100/Unavailable` | The numerator is the displayed result count; optional whole-graph statistics have not been collected. | Show the current count with a current-result label when the total is unknown. Keep known and approximate totals distinct. |
| 27: Query warnings consume the canvas | Four persistent alerts could stack. `graph_truncated` also covers incomplete link enrichment, not only a graph-size limit. | Use quiet grey text inside the canvas and on-demand details for actual truncation. Routine automatic limits do not produce banners. Scope notices to the relevant result view and hide them during loading, failure and async submission. |
| 28: A submitted task looks missing | Navigation dropped GraphSpace/graph context. The ordinary task list defaults to 100 entries and can lag the scheduler's metadata lookup. The page also waited for unrelated Vermeer capabilities. | Preserve graph context and the returned ID; immediately show discovery state and fetch metadata by ID every second, at most 30 missing responses. After discovery, refresh every five seconds. On prolonged delay or error, offer an explicit retry. Ordinary numeric searches also use an exact-ID fallback. |
| 29: Async query fails with a result-size exception | The screenshot reports 77,849,617 bytes against 16,777,216 bytes. Server's per-graph `task.result_size_limit` defaults to 16 MiB and checks compressed stored task results. | Explain failure, actual size and limit, and suggest reducing result rows or projecting fewer properties. Failed tasks are not presented as successful full results. Async execution does not bypass this limit. |
| 30: `pd metrics unmapped` has no useful diagnosis | A configured PD endpoint could not be uniquely matched to an advertised member. Missing system metrics were classified as unsupported and filtered out of node diagnostics. | Keep node registration status intact, classify collection as unavailable, list affected metrics and show safe endpoint comparisons with match counts and actionable configuration guidance. |
| 31: Re-querying the same IDs fails in G6 | Native history aligns fields by mutating an earlier raw record, which was frozen. | Copy writable engine data at the boundary; retain immutable controller snapshots. |
| 32: GraphSpace CRUD loses context | Failed saves replaced the form, failed deletes closed confirmation, and filters/deleting the last row could leave the wrong page selected. | Preserve form state and deletion targets, block duplicate mutation, keep failures retryable, and reset or retreat pagination. |
| 33: Schema deletion cannot retry in place | Failed DELETE requests closed the static confirmation. A lost response or partial batch can leave an uncertain result. | Re-read complete schema and dependencies, delete only remaining eligible targets, and retain the exact graph/names. Accepted or uncertain async deletion checks status first; only explicit retry resubmits. Report success only after absence is confirmed. |
| 34: OLAP engine is unclear | Vermeer and Computer were not clearly distinguished and optional Computer capability checks ran by default. | Make Vermeer the default entry, show actual task engine labels and expose Computer only after explicit opt-in. |
| 35: Minimap is clipped after a query or canvas resize | Reproduced on unchanged remote master: G6 writes fixed surface dimensions, hiding host layout changes from the surface observer; its minimap also retains an inline pixel top offset. | Observe the actual canvas host and anchor the minimap to its bottom. Reapply the current zoom through the public API to refresh its viewport mask without changing the camera. Keep the browser bounds assertion unchanged. |

## Why PD-backed data can still show inconsistent quotas

Hubble calls the Server GraphSpace API. The Server profile endpoint refreshes
**used storage** from PD, but obtains **configuration and storage limits** from
its local GraphSpace cache. It computes utilization from these two values.
Consequently, stable used bytes alongside alternating limits are consistent
with stale Server caches. The screenshot alone does not establish which
deployed Server returned each value.

A separate Server PR refreshes affected caches for both ADD and UPDATE events,
including quota REST updates that emit ADD. It passed focused cache tests and
does not change the broader cache synchronization architecture or promise
synchronous consistency. Existing Schema synchronization history
includes [ASF issue 3235](https://github.com/apache/hugegraph/issues/3235) and
[closed PR 3237](https://github.com/apache/hugegraph/pull/3237); GraphSpace and
Schema are distinct caches and require separate coverage in that workstream.
Hubble UI changes alone do not resolve inconsistent Server replicas.

## Reading PD health diagnostics

`UP` describes node registration/availability. A missing system-metric identity
does not prove that an online PD member is down. `Cluster_Not_Ready` is a separate
cluster readiness signal and is shown separately from collection status.

The diagnostic page compares the configured PD origin with member REST origins
from `/v1/cluster`, shows whether origin and base path match, and distinguishes
no match from multiple matching nodes. It displays at most ten members, prioritizing exact origin/base-path matches;
counts still use the complete deduplicated set. Each failed Server or Store
request retains its owning node, metric group, request path and HTTP status. It
does not expose userinfo, URL parameters or base-path contents.

Check `pd.server` in `hugegraph-hubble.properties` and `server.protocol` when no
scheme is specified. A load balancer or proxy address may not identify a single
member. Use a reachable, correctly advertised member endpoint or correct the
member's advertised `restUrl`; restart Hubble after changing its configuration
and refresh the diagnostic page. The reporting deployment's endpoint values
are not available in the screenshots, so this change does not claim to repair
that configuration automatically.

## Verification scope

Focused frontend tests cover card hit areas and menu isolation, stale editor
responses, query notices and counts, graph-scoped task navigation, bounded
discovery polling, stale task responses, failure details, and PD diagnostics.
Backend tests cover exact-ID lookup without downloading results and PD identity
comparisons, ambiguous matches, redaction and missing metric classification.

Server PR #332 passed all 38 reactor modules in a clean compile and 12
GraphManager cache tests, including two independent managers and delayed
notifications. These were in-process metadata fixtures, not live multi-Server
acceptance; the proposed fix has not been merged or deployed.

Browser screenshots and a repeatable Chromium run use explicit API fixtures;
they verify the running frontend rather than a deployed PD/Store/Server cluster.
See [the browser evidence](assets/deployment-regressions-20261023/browser-report.json).
Live cluster acceptance, image publication and deployment remain separate.

The combined source at `7d2fbf4e` passed all 216 frontend suites / 2,301 tests
and all 95 focused backend tests. Full frontend ESLint, locale validation,
TypeScript and production build passed; Vite retains its existing large-chunk
warning. The default backend suite also ran the newly registered Vermeer tests;
three unrelated GraphSpace controller failures reproduce on unchanged remote
master with the locally installed SDK. The full backend suite is therefore not
reported as green.

The combined Chromium run passed all ten login, anonymous-access, graph-context,
query, delayed-task, GraphSpace and PD-diagnostic scenarios. Separate browser
runners passed 20 GraphSpace CRUD steps, 15 schema/data scenarios, five monitoring
scenarios, nine Vermeer interactions and 12 localized OLAP state checks. Schema
coverage includes all three delete paths, lost responses, partial batches,
changed dependencies and explicit retry after uncertain asynchronous submission.
Canvas removal is a view operation; it is not described as persistent data deletion.

The acceptance runner also passed with a trailing slash in `HUBBLE_URL`.
It uses a configured Chromium executable, an available system Chromium, or the
Playwright-managed browser. A deliberately failed run preserved distinct
screenshots for two browser contexts instead of overwriting one failure image. Indexed diagnostic captures were
also verified with a visible error toast in both contexts; ordinary captures
still reject unexpected error toasts. The strengthened ten-scenario rerun at
`f4e79106` actually edits the expanded quota input, waits for the released stale
response and browser frames, verifies anonymous edit/delete controls in both
views, requires at least three exact-ID lookups while the task list stays empty,
and checks all three PD member origins plus the zero-match/affected-node counts.

After adding #93, combined commit `a6157fc1e7a0ba912368cd68555f5ad5f30cac98`
passed 119 focused visualization tests, production build, TypeScript and locale
validation. Chromium checked minimap bounds at seven stages: initial/repeated
query, panel toggles, settled layout, larger/smaller viewport and Table → Graph
navigation. The independent #93 branch also verified that a zoomed minimap mask
updates immediately after resize without changing the camera. The existing
minimap failure in #88/#89 CI reproduces on master and is addressed by #93;
those independent branches do not themselves contain #93.

## Bounded rendering comparison

Identical intercepted API fixtures ran against remote master `eef5afd2` and the
combined candidate `a6157fc1e7a0ba912368cd68555f5ad5f30cac98` in Chromium
154.0.8037.57. Each size used two interleaved rounds of five warm queries,
in baseline/candidate/candidate/baseline order. Timing waits for the exact
response, the current query/canvas busy-to-idle cycle, and two animation frames;
it excludes Server work.

The record pins both commits, frontend source trees, every split PR head and
the official runner's Git blob/SHA256. Every served source file was checked
against its commit before and after the run. The temporary execution copy only
replaces the Playwright import with the installed absolute dependency path;
its separate hash is recorded. These fresh measurements replace the earlier
comparison that lacked an exact candidate revision.

| Vertices | Master median | Candidate median | Master maximum | Candidate maximum |
| --- | --- | --- | --- | --- |
| 100 | 396.55 ms | 339.45 ms | 519.0 ms | 453.0 ms |
| 300 | 506.50 ms | 465.55 ms | 659.9 ms | 608.9 ms |

No rendering regression was observed in these bounded scenarios. Shared executor
load was uncontrolled, so these numbers do not establish a production speedup
or cluster-throughput guarantee. See the [comparison record](assets/deployment-regressions-20261023/performance/comparison.json).

## Vermeer branch compatibility

The adapter was checked against `hugegraph-computer` PR #19 at `241a6a5a`,
including PD peer precedence, scoped worker availability, load/reload, task
status/result pagination/cancellation and the nine algorithms currently exposed
by Hubble. Native Louvain support is not represented as a Hubble capability.

Token-authenticated HugeGraph load/reload requires a Vermeer administrator service
token. PR #89 recognizes that precise upstream failure and points to
`vermeer.token_file`; other upstream response bodies remain undisclosed.
The adapter/credential/client checks passed 26 Java tests. The current Vermeer
head passed the five affected Go packages with `-tags=dev -race`.

PR #19's existing live load/reload/PageRank browser receipt was verified, but it
belongs to Vermeer `273e1ecf` and Hubble `eef5afd2` in anonymous mode. It is not
relabelled as end-to-end acceptance of `241a6a5a` or token authentication.
