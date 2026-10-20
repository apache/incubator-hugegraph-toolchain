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

PNG downloads contain the whole graph on an opaque white background with a
30 CSS pixel margin, including graph content outside the current viewport.
Manually hidden and filtered elements are not painted. Their retained geometry
can leave additional blank space; export does not change the current camera.

The independent browser PNG fixture compares fixed world geometry against the
actual Chrome screenshot at DPR 1 and 2, with no image fitting or rescaling.
Its strict Pixelmatch checks retain position, color and localized ink coverage
assertions. The Hubble browser gate separately checks actual downloads and
camera independence; a whole-graph export and a fitted viewport use different
scales and cannot be compared as identically sized screenshots.

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
their children. It copies live marker appearance and visibility, then lets the
new path position each arrow from its original transform. Subsequent hiding and
style changes reach exports without applying the path angle twice.

Whole-graph export uses painted bounds, including boundary strokes and shadows,
instead of geometric bounds. It encloses fractional bounds in device pixels
before creating the export canvas, retaining one internal device pixel for an
empty graph. This change is limited to export and does not affect layout or
automatic fitting.

`DragElement.destroy()` also releases its `Shortcut` helper. This removes the
window keydown, keyup and focus listeners that otherwise retain destroyed graph
instances when a canvas is replaced. The upstream 5.1.1 implementations are
[DragElement](https://github.com/antvis/G6/blob/5.1.1/packages/g6/src/behaviors/drag-element.ts)
and [Shortcut](https://github.com/antvis/G6/blob/5.1.1/packages/g6/src/utils/shortcut.ts).
The other patched sources are
[Canvas](https://github.com/antvis/G6/blob/5.1.1/packages/g6/src/runtime/canvas.ts)
and [Minimap](https://github.com/antvis/G6/blob/5.1.1/packages/g6/src/plugins/minimap/index.ts).

The patch updates the published TypeScript, ESM and CommonJS implementations
together. When upgrading G6, check whether upstream includes the fix, repeat
successful and failing exports and graph replacement listener cleanup in a real
browser, and remove the patch and its lockfile registration once they are no
longer needed.
