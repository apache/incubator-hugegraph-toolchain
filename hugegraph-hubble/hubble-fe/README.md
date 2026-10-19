# Hubble frontend

The frontend uses React, TypeScript and Vite. Use the Node version in `.node-version`
and the exact pnpm version in `package.json`; Java 17 is required for the Hubble
backend and distribution build. Commit dependency changes with the single
`pnpm-lock.yaml` lockfile.

Run these commands from this directory:

```sh
pnpm install --frozen-lockfile
pnpm dev
```

The development server listens on <http://127.0.0.1:3000>. Its API proxy targets
`http://127.0.0.1:8088` by default; set `HUBBLE_API_TARGET` to use another Hubble
backend. This server-only setting does not expose environment variables to the
client. The independent graph example at `/visualization.html` needs no Hubble
or HugeGraph Server.

## Validation and production output

```sh
pnpm test:ci
pnpm lint
pnpm i18n:check
pnpm typecheck
pnpm build
```

`test:ci` runs Jest once with bounded concurrency. `build` checks translations
and TypeScript before creating production assets in `build/`. Normal production
output excludes source maps; `pnpm build:diagnostic` includes them for diagnosis.
`pnpm preview` serves the production assets locally.

Use the [Hubble build instructions](../README.md) to build the backend and full
distribution. Maven's frontend helper reuses outputs only when its recorded
inputs and output contents match. Frontend source, configuration, lockfile,
dependency patches and tool versions participate in invalidation. A frontend
build alone does not validate the backend, distribution or browser workflows.

The browser PNG gate uses pinned Pixelmatch tooling to recognize anti-aliased
edge samples. These still require nearby graph pixels in the other image;
missing nodes or edges, solid colors and opacity remain checked. Public DOM
overlays and fractional screenshot fringes are recorded separately.

## G6 rendering patch

G6 is pinned to 5.1.1. `patches/@antv__g6@5.1.1.patch`, registered in
`pnpm-workspace.yaml`, makes PNG export destroy its temporary offscreen Canvas
after success or failure and reject export errors instead of leaving the caller
pending. A 10-second deadline also covers initialization and a missing render
event, so failed rendering releases the temporary canvas. pnpm applies the patch
during dependency installation; do not edit
`node_modules` manually.

The patch also refreshes Minimap geometry after layout changes. PNG export copies
the existing display children into plain groups, preserving transforms, labels,
markers and opacity without rerunning custom shape constructors and duplicating
their children.

The patch updates the published TypeScript, ESM and CommonJS implementations
together. When upgrading G6, check whether upstream includes the fix, repeat
successful and failing exports in a real browser, and remove the patch and its
lockfile registration once they are no longer needed.
