# Hubble frontend third-party notices

`inventory.json` identifies the 257 unique package/version pairs in the frozen
pnpm production dependency closure. `THIRD-PARTY.txt` preserves 297 license and
notice texts, including nested vendored D3/InternMap notices in `@antv/vendor`
and the ECharts NOTICE. It replaces the obsolete React 16/Create React App
inventory. Development-only tools are excluded. The production closure is
conservative: bundling may remove types, server adapters or other unused modules.
This inventory does not assert that every listed file is present in a JS chunk.

The declared package license and original grant text are kept separately.
No copyright holder, license grant or SPDX expression has been inferred to fill
a missing upstream text. This inventory is not an ASF legal approval.

## Regeneration

Use the pinned pnpm version with a frozen install in `hugegraph-hubble/hubble-fe`,
then run from the repository root:

```sh
python3 hugegraph-dist/release-docs/licenses/fe-licenses/generate.py
```

The generator uses `pnpm list --prod --depth Infinity --json`, deduplicates by
package name/version and sorts the output. It verifies the pinned pnpm version
and installed lock against the source dependency document. It records the lock SHA-256 and
package manifest/text hashes; it recursively copies LICENSE, COPYING, NOTICE
and CopyrightNotice files without changing their bytes. Three packages carry
their complete MIT grant in a README License section; those sections are preserved instead.
The generator does not install packages, fetch network data or modify the lock.
Run it twice and compare the inventory/text hashes to check determinism.

`@ant-design/icons-svg` omits its license from the published package. Its
verbatim project LICENSE is retained in `upstream/` from the exact publishing
commit recorded by the package registry:
[7f2516ac91226d2b41f93b35cb5197c8d94f7189](https://github.com/ant-design/ant-design-icons/blob/7f2516ac91226d2b41f93b35cb5197c8d94f7189/LICENSE).
No license from another version has been substituted silently.

When the lock changes, regenerate and inspect new, removed or changed notices.
Review the corresponding original license grants; the package `license` field
alone does not establish that all required attribution text has been included.

## Local G6 changes

Hubble patches `@antv/g6` 5.1.1 through
`hugegraph-hubble/hubble-fe/patches/@antv__g6@5.1.1.patch`.
The original MIT grant and `Copyright (c) 2018 Alipay.inc` are preserved.
The patch refreshes minimap shapes after layout and preserves existing display
children, opacity, visibility and export camera state when producing a PNG.
Whole-graph PNG bounds also include painted strokes and shadows and enclose
fractional bounds in device pixels to avoid cutting off boundary ink.
Arrow markers retain the current painted appearance and visibility while the
new path positions them from their original transform, preserving later updates.
It also calls `Shortcut.destroy()` from `DragElement.destroy()` to release window
keydown, keyup and focus listeners when a graph is destroyed. See the upstream
5.1.1 [DragElement](https://github.com/antvis/G6/blob/5.1.1/packages/g6/src/behaviors/drag-element.ts)
and [Shortcut](https://github.com/antvis/G6/blob/5.1.1/packages/g6/src/utils/shortcut.ts)
implementations. These behavior, minimap and canvas changes are applied to the
upstream `src`, `esm` and `lib` trees; they do not change the upstream license.

## Release follow-up: missing upstream texts

Five production packages have no standalone license or complete README grant.
They are explicitly marked `upstream-license-text-required` in the inventory;
these gaps must be resolved before release legal readiness is claimed.

| Package | Published declaration | Source evidence and remaining work |
| --- | --- | --- |
| `@antv/algorithm@0.1.26` | MIT | [Publishing commit](https://github.com/antvis/algorithm/tree/58b99ab9c937b8ec3e5e99b57624ad5a007682e5) has no LICENSE/COPYING/NOTICE. Obtain the applicable grant and attribution from upstream. |
| `@antv/event-emitter@0.1.3` | MIT | [Publishing commit](https://github.com/antvis/event-emitter/tree/f46ff7712f332ef1c8b7601eb88b5784ad4edaa1) has no LICENSE/COPYING/NOTICE. Obtain the applicable grant and attribution from upstream. |
| `@antv/g-lite@2.7.2` | MIT | Registry metadata supplies no publishing gitHead. The [source tree inspected](https://github.com/antvis/g/tree/89b360c95bbc367f96b876ef9c27424e4c565084/packages/g-lite) declares the same version but has no package LICENSE. Confirm version provenance and the applicable project license/attribution; a sibling package license has not been substituted. |
| `@antv/util@2.0.17` | ISC; README says MIT | Registry metadata supplies no publishing gitHead. The [source repository](https://github.com/antvis/util) has no exact 2.0.17 tag. Resolve the conflicting declarations and obtain the version's grant/attribution; the current 3.x MIT license has not been substituted. |
| `cron-expression-validator@1.0.20` | ISC; README says MIT | [Publishing commit](https://github.com/anushaihalapathirana/cron-expression-validator/tree/a796b1e1a28846194e1397c93713d5a3e88527dd) has no LICENSE/COPYING/NOTICE. Resolve the conflicting declarations and obtain the applicable grant/attribution. |

No non-frontend or Maven license notices are replaced by this inventory.
