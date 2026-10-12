# Hubble contributor guide

## Authentication and connection boundary

The `1.8/master` path is the source of truth. The backend detects authentication mode from HugeGraph Server and uses one connection resolver. The resolver chooses either a direct server URL or an address discovered from PD; callers must not reimplement `usePD` or infer connection state from page-local flags. In PD mode the server address returned by discovery is authoritative, so a manual server URL is not required.

Use the unauthenticated HugeGraph client for anonymous mode. Do not manufacture an empty token or an administrator session. Anonymous mode has no account context and account/permission routes are hidden or rejected at the capability boundary.

## Compatibility policy

Compatibility is intentionally one-way:

- `1.8/master`: modern GraphSpace/auth contracts and the complete UI.
- `1.7`: thin fallback for the legacy response shape; keep the core workflow usable without adding version branches to controllers or React pages.
- `1.5` standalone: core graph/schema/data operations only. GraphSpace management is unsupported and should degrade with an explicit capability response. Do not add a PD variant for 1.5.

Version checks belong in the client compatibility adapter and connection resolver. New code should consume capabilities, not compare literal versions. When an old image cannot satisfy a capability, mark the test as `needs input` or `skipped` with the exact image tag and reason.

## Verification

For UI changes, use Chrome to exercise login/non-auth mode, connection switching, and account/GraphSpace visibility. Static inspection and unit tests are not a substitute for this interaction check. Keep screenshots collected from the running UI in the documentation assets referenced by `README.md`.
