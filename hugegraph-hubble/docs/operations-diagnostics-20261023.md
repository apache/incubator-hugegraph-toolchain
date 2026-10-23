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

## Access and endpoint matching

In authenticated Server mode, operations health, topology and metric capabilities
are granted only to the global `ADMIN` level. GraphSpace administrators and
ordinary users receive HTTP 403 from the overview and node endpoints before the
data service runs. Configured/member origins are intentional administrator
diagnostics; credentials, URL parameters and base-path contents are not exposed.
In Server non-auth mode these read endpoints are available without a login, as
with the existing global operations view; Hubble does not invent a user role.

Endpoint matching lowercases the scheme and host, fills the standard HTTP/HTTPS
port, normalizes path dot segments and ignores one optional trailing slash. A
member address without a scheme inherits the configured scheme. Proxy base paths
must otherwise match exactly, including case and escaped path spelling. A proxy
or load-balancer origin is not treated as an alias of its backend member.
Bracketed IPv6 literals are supported and host case is normalized; equivalent
compressed/expanded IPv6 spellings are not resolved to guess a member identity.

A missing, null or non-array `pdList` is a malformed `/v1/cluster` response. The
collector preserves that source diagnosis and skips PD metric matching, rather
than trying to recover a topology from `pdLeader` alone. Other healthy sources
remain available. The strict parser is retained so malformed data cannot appear
as a successful but empty PD topology.

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
