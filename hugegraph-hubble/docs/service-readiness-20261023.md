# Service readiness in operations

Registration health and service readiness answer different questions. PD may
report a node as `UP` while its local Raft quorum is unavailable, or while a Store
is still starting. Hubble now keeps `node.status` unchanged and reports a separate
`node.readiness` object with status, observation time, an allowlisted service
state, a safe reason, and request-path/HTTP evidence.

- PD: `GET /v1/ready` must return an explicit boolean `ready` and a known Raft state.
  A valid HTTP 503 response preserves the Raft failure state instead of losing the
  body. HTTP 200 alone is insufficient: older PD images may return a 200
  authentication-error envelope for this unknown endpoint. Unknown/malformed
  responses remain `UNKNOWN`, and HTTP 404 is `UNSUPPORTED`.
- Store: `GET /-/state` requires `status: 0` and `text: ONLINE`, `STARTING` or
  `STOPPING`. Only `ONLINE` is `READY`. The request uses the existing matched,
  trusted Store target. Probes run in a separate bounded executor alongside metrics
  and share the overall waiting cutoff; a slow probe does not occupy a metric worker.
- Hubble probes only the PD member that matches the configured endpoint exactly.
  Other members are explicitly `UNKNOWN` / `readiness_not_probed`; an ambiguous
  or unmapped configured endpoint is never assigned to a member by guesswork.
- Readiness is collected with metrics and uses the existing snapshot/cache cycle.
  There is no new timer, poller or Server API. A failed refresh marks retained
  readiness observations stale; the UI says a refresh is needed rather than
  showing an old successful probe as current readiness.

The topology and node list use small secondary labels. Only explicit, current
`NOT_READY` results enter the attention list. Node details contain the reported
state, request, HTTP status and a specific next step: quorum/peer checks for PD
elections, storage and snapshot logs for fatal Raft errors, or Store startup and
shutdown checks. Unknown and unsupported probes do not create warning banners.

## Verification

The backend regressions cover real HTTP 503 bodies, strict response parsing,
HTTP 200 authentication errors, trusted origins and response-size limits,
registration/readiness independence, failed-refresh staleness, and a Store probe
that exceeds the existing overall collection budget. Frontend regressions cover
concrete recovery guidance, understated unknown states and stale observations.

Run the Chromium fixture against a running frontend:

```bash
HUBBLE_URL=http://127.0.0.1:3000 \
HUBBLE_UI_EVIDENCE_DIR=/tmp/hubble-readiness-evidence \
node hubble-dist/assembly/travis/run_ui_readiness.js
```

It checks PD/Store details, node list and overview in both anonymous mode and an
administrator session. Authenticated scenarios enter credentials through the
actual login form, establish a fixture HttpOnly session cookie, and verify the
subsequent context/session requests; no user or authentication state is preseeded.
The runner also switches GraphSpace and graph using the header selectors and
checks route context, identity-scoped persistence and scoped API requests. Account
and GraphSpace controls are checked for anonymous, administrator and ordinary
read-only users, including the restricted account deep link. The JSON report
records the request paths, HTTP statuses and session presence without credentials.
These are Chromium client checks against deterministic API fixtures, not a
live-backend authentication test or deployed-cluster acceptance. Set
`HUBBLE_UI_REVISION` to record the frontend revision and whether it is a combined
integration build; the report also records `HUBBLE_URL` and does not infer an
isolated-PR exact-head result. Before/after screenshots are kept outside Git and
linked from the PR to the regression record. Actual endpoint availability,
credentials, proxy routing and recovery need deployment validation.
