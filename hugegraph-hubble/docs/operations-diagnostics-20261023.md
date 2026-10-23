# Operations diagnostics: 2026-10-23

The review covers the path from Cluster Overview to Server/Store node details and
PD source diagnostics. The goal is to identify what failed, inspect concrete
evidence, and find the next recovery step without treating a collection failure
as a process outage.

Screenshots were captured from the running frontend in Chromium with deterministic
API fixtures. They are UI evidence, not observations of the user's deployed cluster.
Before/after image evidence is linked from [PR #87](https://github.com/hugegraph/hugegraph-toolchain/pull/87)
rather than stored in the repository.

## Flow and findings

1. **Cluster Overview — clearer collection failures.**
   The previous view repeated large warning blocks and showed green UP rows without
   naming the missing metrics. Source issues now use compact rows with direct
   diagnostic links; the attention table names the affected metric groups while
   preserving the node's actual registration status.
2. **Server details — failure count before supporting details.**
   The page now shows only the selected node's source. The current failed-graph
   count remains visible, while repeated context and recovery instructions are
   available through a disclosure. Stale metric timestamps are labeled as the
   last successful collection, not the latest observation.
3. **Store details — actionable request evidence.**
   The failure reason is localized. `/metrics/system` and HTTP 503 remain visible
   before expanding supporting details; the node stays UP when registration is
   healthy. Collection and node health remain separate.
4. **PD diagnostics — concise status with address evidence on demand.**
   The summary distinguishes online nodes, missing system metrics and
   `Cluster_Not_Ready`. The disclosure shows the configured origin, member origins,
   origin/path match results and matched/discovered counts. It names the relevant
   `pd.server` / `server.protocol` settings and restart/refresh steps. Endpoint
   ambiguity is preserved; metrics are never assigned to a leader by guesswork.

## Verification and limits

- Backend collector/parser tests cover unmatched identities, canonical matches,
  ambiguous identities, three healthy PD nodes with unavailable metrics, bounded
  diagnostic membership, and redaction of URL credentials, parameters and paths.
- Frontend tests cover disclosures, visible request evidence and failed-graph
  counts, independent topology health, current-source scoping and timestamp labels.
- A single failed metric shown before disclosure includes its owning node and
  metric group. With multiple failures, every request/status or failed-graph count
  stays inside its own node/group row; no arbitrary first value appears source-wide.
- [Browser report](assets/operations-diagnostics-20261023/after/browser-report.json)
  and [reproducible runner](../hubble-dist/assembly/travis/run_ui_operations_diagnostics.js)
  cover the four flows above and multiple Server/Store diagnostic ownership.
  The native disclosure controls support keyboard
  interaction; these checks do not establish complete accessibility conformance.
- Actual PD connection addresses, cluster readiness, authentication enforcement,
  upstream recovery and multi-Server behavior still require deployment validation.
  A screenshot showing `pd_metrics_unmapped` does not identify which address differs;
  the new diagnostic response exposes that comparison safely.
