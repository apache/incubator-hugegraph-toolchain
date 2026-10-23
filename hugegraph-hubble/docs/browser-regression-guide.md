# Hubble browser regression guide

## Run the browser fixture checks

The manual Chromium runner exercises the running frontend with controlled API
responses. It covers authenticated and anonymous visibility, GraphSpace
navigation and editing, graph switching, query notices and counts, delayed
task discovery, result-size guidance and PD diagnostics. It requires the
corresponding frontend features; fixtures do not establish acceptance of a live
PD/Store/Server cluster or replace the packaged distribution gate.

Install the frontend dependencies and start Hubble's frontend. Then, from
`hugegraph-hubble`, run:

```bash
HUBBLE_URL=http://127.0.0.1:3000 \
HUBBLE_UI_EVIDENCE_DIR=/tmp/hubble-deployment-ui \
  node hubble-dist/assembly/travis/run_ui_deployment_regressions.js
```

The runner accepts `HUBBLE_URL` with or without a trailing slash. It uses
`PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` when configured, an available system
Chromium, or Playwright's managed browser.

Keep the output directory outside Git. Each invocation replaces the previous
`browser-report.json` and known scenario/failure screenshots while preserving
unrelated files. Startup failures produce a fresh failed report; a child that
exits without a report is not treated as successful. Diagnostic screenshots
include context and page indices so one page cannot overwrite another.

Retain run reports, screenshots and performance samples in external CI artifacts
or PR attachments. Record the actual frontend revision, runner revision and
environment with that evidence. Do not commit one-time outputs or infer that a
fixture run verifies a production deployment.

## Understand GraphSpace quota data

Hubble calls the Server GraphSpace API. The Server profile endpoint refreshes
**used storage** from PD but obtains **configuration and storage limits** from
its local GraphSpace cache. Utilization combines these values. Stable used bytes
with alternating limits can indicate inconsistent Server caches; identify the
responding Server before attributing a deployment issue to the frontend.

Hubble must also ignore stale detail responses after an editor closes or switches
spaces. That protects the current form but cannot repair inconsistent Server
replicas. GraphSpace and Schema use distinct caches; broader synchronization
changes require separate coverage.

## Read PD health diagnostics

`UP` describes node registration/availability. Missing system metrics do not
prove that an online PD member is down. `Cluster_Not_Ready` is a separate
readiness signal and must be considered separately from collection status.

The diagnostic page compares the configured PD origin with member REST origins,
including origin and base-path matching. No match and multiple matches require
different diagnosis. The visible member list is bounded; counts use the complete
deduplicated set. Failed Server or Store requests retain their owning node,
metric group, request path and HTTP status without exposing credentials, URL
parameters or base-path contents.

Check `pd.server` in `hugegraph-hubble.properties` and `server.protocol` when no
scheme is specified. A load balancer or proxy address may not identify a single
member. Use a reachable, correctly advertised member endpoint or correct the
member's advertised `restUrl`. Restart Hubble after changing its configuration
and refresh the diagnostic page. Collection warnings alone do not identify the
actual endpoint configuration or repair it automatically.

## Diagnose asynchronous result limits

Asynchronous execution does not bypass Server's per-graph
`task.result_size_limit`. Its default is 16 MiB for compressed stored task
results. Reduce result rows or project fewer properties when a task exceeds that
limit. A failed task is not a successful full result; preserve its exact graph
and task ID when inspecting the failure or retrying discovery.

## Configure Vermeer load permissions

Token-authenticated HugeGraph load/reload requires a Vermeer administrator
service token. Configure `vermeer.token_file` as described in the
[Vermeer development guide](vermeer-development.md). Other upstream response
bodies must remain undisclosed. An anonymous-mode fixture does not establish
token-authenticated load/reload acceptance.

## Plan live deployment verification

Exercise authentication, anonymous access, graph switching and mutation recovery
against the intended deployment separately from the mocked browser runner.
Include PD discovery and multiple Server instances when investigating cache or
health problems. Removing a graph item from the canvas is a view operation;
verify persistent deletion through the data API when that is the intended action.

For rendering comparisons, pin both frontend revisions and the runner, use the
same fixtures and browser, and verify the served source. Separate unmeasured
warm-up queries from timed samples and interleave baseline/candidate runs to
reduce order effects. Small development-UI samples do not establish production
throughput or tail-latency guarantees.
