/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const {createRequire} = require('node:module');

class NeedsInput extends Error {
  constructor(message) { super(message); this.name = 'NeedsInput'; }
}
function argument(argv, name, fallback) {
  const index = argv.indexOf(name);
  return index < 0 ? fallback : argv[index + 1];
}
function redact(value, secrets = []) {
  let result = String(value);
  for (const secret of secrets.filter(Boolean)) result = result.split(secret).join('[REDACTED]');
  return result.replace(/(https?:\/\/)[^\s/@]+:[^\s/@]+@/g, '$1[REDACTED]@');
}
function publicHttpUrl(value, field) {
  // URL parsing errors can contain the supplied input. Keep only the static field
  // name in diagnostics, before the value can reach a browser or evidence file.
  const invalid = () => {throw new TypeError(`Invalid ${field}`);};
  if (typeof value !== 'string' || !/^https?:\/\//i.test(value)
      || /[\u0000-\u0020\u007f]/.test(value) || value.includes('?') || value.includes('#')) invalid();
  let parsed;
  try { parsed = new URL(value); }
  catch { invalid(); }
  const authority = value.match(/^[^:]+:\/\/([^/?#]*)/)[1];
  if (!['http:', 'https:'].includes(parsed.protocol) || !parsed.hostname
      || parsed.username || parsed.password || authority.includes('@')) invalid();
  return value;
}

function sha256(value) { return crypto.createHash('sha256').update(value).digest('hex'); }
function typedId(id) {
  assert.ok(typeof id === 'string' || (typeof id === 'number' && Number.isSafeInteger(id)),
            'Fixture IDs must be strings or safe integers; unsafe JSON numeric IDs cannot be verified exactly');
  return `${typeof id}:${JSON.stringify(id)}`;
}
function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.keys(value).sort().map(key => [key, canonical(value[key])]));
  }
  return value;
}
function canonicalGraph(graph) {
  assert.ok(Array.isArray(graph?.vertices) && Array.isArray(graph?.edges),
            'Expected real graph_view vertices and edges');
  for (const key of ['vertices', 'edges']) {
    assert.equal(new Set(graph[key].map(item => typedId(item.id))).size, graph[key].length,
                 `Real ${key} contain duplicate IDs`);
  }
  return Object.fromEntries(['vertices', 'edges'].map(key => [key,
    graph[key].map(item => {typedId(item.id); return canonical(item);})
      .sort((a, b) => typedId(a.id).localeCompare(typedId(b.id)))]));
}
function assertExportMatches(actual, expected) {
  assert.deepEqual(canonicalGraph(actual), canonicalGraph(expected),
                   'Downloaded records must preserve full raw IDs, endpoints and properties');
}
function assertFixtureGraph(graph, fixture) {
  canonicalGraph(graph);
  const expected = fixture.expected;
  if (!Array.isArray(expected?.vertexIds) || !expected.vertexIds.length) {
    throw new NeedsInput('Supply fixture.expected.vertexIds for a known non-empty real Server dataset');
  }
  assert.deepEqual(graph.vertices.map(v => typedId(v.id)).sort(), expected.vertexIds.map(typedId).sort(),
                   'Real Server query did not return the configured fixture vertex IDs');
  if (expected.edgeIds) {
    assert.deepEqual(graph.edges.map(e => typedId(e.id)).sort(), expected.edgeIds.map(typedId).sort());
  }
  if (expected.edgeCount !== undefined) assert.equal(graph.edges.length, expected.edgeCount);
}
function fixtureFromPrefix(prefix, graphspace, graph) {
  assert.match(prefix, /^[a-zA-Z0-9_]+$/, 'Use the safe prefix from run_live_hubble_smoke.py');
  return {graphspace, graph,
    query: `g.V().hasLabel('${prefix}_person').bothE().dedup()`,
    expected: {vertexIds: [`${prefix}_alice`, `${prefix}_bob`, `${prefix}_carol`], edgeCount: 2},
    schemaLabels: [`${prefix}_person`, `${prefix}_knows`],
    algorithm: {source: `${prefix}_alice`, target: `${prefix}_carol`, label: `${prefix}_knows`}};
}
function loadPlaywright() {
  try { return require('playwright'); }
  catch {
    const candidates = [process.env.PLAYWRIGHT_NODE_MODULES,
      path.resolve(__dirname, '../../../hubble-fe/node_modules')].filter(Boolean);
    for (const directory of candidates) {
      try { return createRequire(path.join(directory, 'playwright/package.json'))('playwright'); }
      catch { /* Try the next existing installation; never install during acceptance. */ }
    }
    throw new NeedsInput('Existing Playwright installation unavailable; set PLAYWRIGHT_NODE_MODULES');
  }
}
function chromePath(configured) {
  if (configured) return configured;
  return ['/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    '/Applications/Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing']
    .find(candidate => fs.existsSync(candidate));
}
async function businessPayload(response, label) {
  assert.ok(response.ok(), `${label}: HTTP ${response.status()}`);
  const body = await response.json();
  assert.equal(body?.status, 200, `${label}: business response did not succeed`);
  return body.data;
}
function scopePath(fixture) {
  assert.ok(fixture?.graphspace && fixture?.graph, 'Fixture needs graphspace and graph');
  return `/api/v1.3/graphspaces/${encodeURIComponent(fixture.graphspace)}/graphs/${encodeURIComponent(fixture.graph)}`;
}
function pngDimensions(bytes) {
  assert.ok(bytes.length >= 24 && bytes.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10])),
            'Downloaded image is not PNG');
  assert.equal(bytes.toString('ascii', 12, 16), 'IHDR');
  const width = bytes.readUInt32BE(16), height = bytes.readUInt32BE(20);
  assert.ok(width > 0 && height > 0 && width < 30000 && height < 30000, 'Invalid PNG dimensions');
  return {width, height};
}
async function pixelEvidence(page, bytes) {
  // Decode only externally captured screenshot/download pixels into a detached standard
  // browser canvas. No production canvas internals, React state or engine API are read.
  return page.evaluate(async encoded => {
    const image = new Image();
    image.src = `data:image/png;base64,${encoded}`;
    await image.decode();
    const canvas = document.createElement('canvas');
    canvas.width = image.naturalWidth; canvas.height = image.naturalHeight;
    const context = canvas.getContext('2d', {willReadFrequently: true});
    context.drawImage(image, 0, 0);
    const {width, height} = canvas;
    const {data} = context.getImageData(0, 0, width, height);
    const stride = width + 1;
    const sum = new Uint32Array((width + 1) * (height + 1));
    let colored = 0;
    for (let y = 0; y < height; y++) {
      let row = 0;
      for (let x = 0; x < width; x++) {
        const i = (y * width + x) * 4;
        const maximum = Math.max(data[i], data[i + 1], data[i + 2]);
        const minimum = Math.min(data[i], data[i + 1], data[i + 2]);
        const hit = data[i + 3] > 40 && maximum - minimum > 32 && minimum < 245;
        if (hit) { row++; colored++; }
        sum[(y + 1) * stride + x + 1] = sum[y * stride + x + 1] + row;
      }
    }
    const candidates = [];
    for (let y = 8; y < height - 8; y += 6) {
      for (let x = 8; x < width - 8; x += 6) {
        const left = x - 5, right = x + 6, top = y - 5, bottom = y + 6;
        const density = sum[bottom * stride + right] - sum[top * stride + right]
          - sum[bottom * stride + left] + sum[top * stride + left];
        if (density < 28) continue;
        const center = (y * width + x) * 4;
        const red = data[center], green = data[center + 1], blue = data[center + 2];
        if (!(data[center + 3] > 40 && Math.max(red, green, blue) - Math.min(red, green, blue) > 32
          && Math.min(red, green, blue) < 245)) continue;
        // Rank using this before-screenshot patch alone. A dense mixed outline/fill
        // near a node edge is less reliable than its uniform filled interior.
        let uniformPixels = 0;
        for (let py = top; py < bottom; py++) for (let px = left; px < right; px++) {
          const i = (py * width + px) * 4;
          const dr = data[i] - red, dg = data[i + 1] - green, db = data[i + 2] - blue;
          if (data[i + 3] > 40 && Math.max(data[i], data[i + 1], data[i + 2])
              - Math.min(data[i], data[i + 1], data[i + 2]) > 32
              && Math.min(data[i], data[i + 1], data[i + 2]) < 245 && dr * dr + dg * dg + db * db <= 24 * 24) {
            uniformPixels++;
          }
        }
        candidates.push({x, y, density, uniformPixels});
      }
    }
    candidates.sort((a, b) => b.uniformPixels - a.uniformPixels || b.density - a.density);
    const selected = [];
    for (const candidate of candidates) {
      if (selected.every(other => Math.hypot(other.x - candidate.x, other.y - candidate.y) >= 20)) {
        selected.push(candidate);
        if (selected.length >= 100) break;
      }
    }
    return {width, height, coloredPixels: colored, candidates: selected};
  }, bytes.toString('base64'));
}
const MINIMAP_PAINT_SELECTOR = '.g6-minimap, .minimap, .g6-minimap > canvas, .g6-minimap > div, '
  + '.minimap > canvas, .minimap > div';
async function screenshot(page, target, file) {
  const graphOnly = target !== page;
  const minimaps = graphOnly ? target.locator(MINIMAP_PAINT_SELECTOR) : null;
  const tooltips = graphOnly ? target.locator('.graph-canvas-tooltip') : null;
  const maskedElements = graphOnly ? {minimaps: await minimaps.count(), tooltips: await tooltips.count()} : null;
  const bytes = await target.screenshot({path: file, animations: 'disabled',
    maskColor: '#000000',
    mask: [page.locator('input[type="password"]'),
      ...(graphOnly ? [minimaps, tooltips] : [])]});
  return {file, sha256: sha256(bytes), graphPixelCheckExcludesMinimapAndTooltip: graphOnly,
    maskedPublicDomElements: maskedElements,
    ...await pixelEvidence(page, bytes)};
}
async function canvasReady(page, region, screenshotFile) {
  await region.waitFor({state: 'visible'});
  await region.locator('canvas').first().waitFor({state: 'visible'});
  await page.waitForFunction(element => element.getAttribute('aria-busy') !== 'true', await region.elementHandle());
  assert.equal(await region.getByRole('alert').count(), 0, 'Canvas reported an actual runtime error');
  const bounds = await region.boundingBox();
  assert.ok(bounds && bounds.width > 80 && bounds.height > 80, 'Canvas has no usable size');
  await page.evaluate(async () => {
    await document.fonts.ready;
    await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
  });
  for (let attempt = 0; attempt < 30; attempt++) {
    const evidence = await screenshot(page, region, screenshotFile);
    if (evidence.coloredPixels > 30) return evidence;
    await page.waitForTimeout(150);
  }
  throw new Error('Canvas screenshot contains no graph-colored pixels after bounded readiness wait');
}
async function findNode(page, region, matches, screenshotFile) {
  // A tooltip over another node would mask that node out of the screenshot candidates.
  await page.mouse.move(-1, -1);
  await region.getByRole('tooltip').waitFor({state: 'hidden'});
  await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
  const image = await screenshot(page, region, screenshotFile);
  const bounds = await region.boundingBox();
  for (const point of image.candidates) {
    const x = bounds.x + point.x * bounds.width / image.width;
    const y = bounds.y + point.y * bounds.height / image.height;
    await page.mouse.move(x, y);
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
    try {
      const tooltip = region.getByRole('tooltip');
      await tooltip.waitFor({state: 'visible', timeout: 220});
      const text = await tooltip.innerText();
      if (matches(text)) return {x, y, tooltip: text};
    }
    catch { /* A pixel may belong to an edge; continue actual pointer hit testing. */ }
  }
  throw new Error('No actual node tooltip matched the expected raw record in screenshot-derived candidates');
}
// Gremlin single-quoted strings avoid Groovy interpolation of business IDs.
function gremlinIdLiteral(id) {
  typedId(id);
  if (typeof id === 'number') return String(id);
  return "'" + id.replace(/[\\'\u0000-\u001f\u2028\u2029]/g, value => {
    if (value === '\\' || value === "'") return '\\' + value;
    return '\\u' + value.charCodeAt(0).toString(16).padStart(4, '0');
  }) + "'";
}
function uniqueDisplayId(vertices, id) {
  return vertices.some(vertex => typedId(vertex.id) === typedId(id))
    && vertices.filter(vertex => String(vertex.id) === String(id)).length === 1;
}
function connectedPair(raw) {
  canonicalGraph(raw);
  const nodes = new Map(raw.vertices.map(vertex => [typedId(vertex.id), vertex]));
  for (const edge of raw.edges) {
    const source = nodes.get(typedId(edge.source)), target = nodes.get(typedId(edge.target));
    if (source && target && typedId(source.id) !== typedId(target.id)
        && uniqueDisplayId(raw.vertices, source.id) && uniqueDisplayId(raw.vertices, target.id)) return {source, target, edge};
  }
  throw new NeedsInput('Query fixture needs a non-self edge with two uniquely displayed endpoint IDs for expansion/live-drag evidence');
}
function mergeRawGraph(before, incoming) {
  canonicalGraph(before); canonicalGraph(incoming);
  return Object.fromEntries(['vertices', 'edges'].map(kind => [kind,
    [...new Map([...before[kind], ...incoming[kind]].map(record => [typedId(record.id), record])).values()]]));
}
function assertExpandedGraph(before, incoming, clickedId) {
  const merged = mergeRawGraph(before, incoming);
  const previousVertices = new Set(before.vertices.map(vertex => typedId(vertex.id)));
  const previousEdges = new Set(before.edges.map(edge => typedId(edge.id)));
  const vertices = new Set(merged.vertices.map(vertex => typedId(vertex.id)));
  const clicked = typedId(clickedId);
  assert.ok(previousVertices.has(clicked), 'Clicked expansion vertex is absent from the actual original response');
  const addedVertices = merged.vertices.filter(vertex => !previousVertices.has(typedId(vertex.id)));
  const addedEdges = merged.edges.filter(edge => !previousEdges.has(typedId(edge.id)));
  assert.ok(addedVertices.length > 0, 'Real expand response added no neighbor record');
  assert.ok(addedEdges.length > 0, 'Real expand response added no edge record');
  for (const edge of merged.edges) {
    assert.ok(vertices.has(typedId(edge.source)) && vertices.has(typedId(edge.target)),
      'Expanded edge references a vertex absent from the actual result');
  }
  const neighborIds = new Set(addedEdges.flatMap(edge => typedId(edge.source) === clicked ? [typedId(edge.target)]
    : typedId(edge.target) === clicked ? [typedId(edge.source)] : []));
  const neighbors = addedVertices.filter(vertex => neighborIds.has(typedId(vertex.id)));
  assert.ok(neighbors.length > 0, 'Expansion added no new edge connecting the clicked vertex to a new neighbor');
  return {graph: merged, addedVertexIds: addedVertices.map(vertex => vertex.id),
    addedEdgeIds: addedEdges.map(edge => edge.id), neighbors};
}
function hasCurrentGraphCount(labels, kind, count) {
  const current = `${kind}: ${count} in this result`;
  // Known totals use a comma; unavailable totals use the current-result semicolon.
  return labels.some(label => label.startsWith(`${current}, `) || label.startsWith(`${current}; `));
}
function assertVisibleGraphStats({viewport, counts, minimaps, canvas}) {
  const inside = (inner, outer, name) => {
    assert.ok(inner.width > 0 && inner.height > 0, `${name} has no visible size`);
    assert.ok(inner.x >= outer.x - 1 && inner.y >= outer.y - 1
      && inner.x + inner.width <= outer.x + outer.width + 1
      && inner.y + inner.height <= outer.y + outer.height + 1, `${name} is clipped outside its visible bounds`);
  };
  assert.equal(counts.length, 2, 'Both node and edge statistic cards must be present');
  for (const item of counts) {
    inside(item.rect, viewport, item.label);
    assert.ok(item.pane, 'Graph statistics have no visible tab panel');
    inside(item.rect, item.pane, item.label);
    for (const clip of item.clips) {
      const rect = item.rect;
      if (clip.x) assert.ok(rect.x >= clip.rect.x - 1 && rect.x + rect.width <= clip.rect.x + clip.rect.width + 1,
        `${item.label} is horizontally clipped by an ancestor`);
      if (clip.y) assert.ok(rect.y >= clip.rect.y - 1 && rect.y + rect.height <= clip.rect.y + clip.rect.height + 1,
        `${item.label} is vertically clipped by an ancestor`);
    }
    for (const minimap of minimaps) {
      assert.ok(item.rect.x + item.rect.width <= minimap.x || minimap.x + minimap.width <= item.rect.x
        || item.rect.y + item.rect.height <= minimap.y || minimap.y + minimap.height <= item.rect.y,
        'Minimap overlaps graph statistics');
    }
  }
  assert.ok(minimaps.length > 0, 'Default graph minimap is missing');
  for (const minimap of minimaps) {
    inside(minimap, viewport, 'Minimap');
    inside(minimap, canvas, 'Minimap');
    for (const item of counts) inside(minimap, item.pane, 'Minimap');
    for (const clip of minimap.clips || []) {
      if (clip.x) assert.ok(minimap.x >= clip.rect.x - 1
        && minimap.x + minimap.width <= clip.rect.x + clip.rect.width + 1, 'Minimap is horizontally clipped');
      if (clip.y) assert.ok(minimap.y >= clip.rect.y - 1
        && minimap.y + minimap.height <= clip.rect.y + clip.rect.height + 1, 'Minimap is vertically clipped');
    }
    assert.ok(minimap.x + minimap.width / 2 < canvas.x + canvas.width / 2, 'Minimap moved away from the left side');
  }
}
function assertVisibleGraphPanel({name, viewport, graph, panel, control, clips, scrolls}) {
  const inside = (inner, outer, bounds) => {
    assert.ok(inner && outer && inner.width > 0 && inner.height > 0,
      `${name} has no visible ${bounds}`);
    assert.ok(inner.x >= outer.x - 1 && inner.y >= outer.y - 1
      && inner.x + inner.width <= outer.x + outer.width + 1
      && inner.y + inner.height <= outer.y + outer.height + 1,
      `${name} control is clipped outside ${bounds}`);
  };
  inside(control, viewport, 'the viewport');
  inside(control, graph, 'the graph');
  inside(control, panel, 'its panel');
  for (const clip of clips) {
    if (clip.x) assert.ok(control.x >= clip.rect.x - 1
      && control.x + control.width <= clip.rect.x + clip.rect.width + 1,
      `${name} control is horizontally clipped by an ancestor`);
    if (clip.y) assert.ok(control.y >= clip.rect.y - 1
      && control.y + control.height <= clip.rect.y + clip.rect.height + 1,
      `${name} control is vertically clipped by an ancestor`);
  }
  assert.ok(scrolls.length > 0, `${name} is missing ancestor scroll evidence`);
  assert.equal(scrolls[0].beforeTop, 0, 'Graph wrapper was already scrolled before opening panels');
  assert.equal(scrolls[0].beforeLeft, 0, 'Graph wrapper was already scrolled before opening panels');
  for (const item of scrolls) {
    assert.equal(item.top, item.beforeTop, `${name} changed ancestor scrollTop`);
    assert.equal(item.left, item.beforeLeft, `${name} changed ancestor scrollLeft`);
  }
}
async function decodeScreenshot(page, bytes) {
  // The source is an external Chrome PNG, never the application's canvas object.
  const image = await page.evaluate(async encoded => {
    const image = new Image();
    image.src = `data:image/png;base64,${encoded}`;
    await image.decode();
    const canvas = document.createElement('canvas');
    canvas.width = image.naturalWidth; canvas.height = image.naturalHeight;
    const context = canvas.getContext('2d');
    context.drawImage(image, 0, 0);
    const rgba = context.getImageData(0, 0, canvas.width, canvas.height).data;
    // Packed bytes avoid serializing millions of boxed RGBA numbers through CDP.
    let binary = '';
    for (let offset = 0; offset < rgba.length; offset += 8192) {
      binary += String.fromCharCode(...rgba.subarray(offset, offset + 8192));
    }
    return {width: canvas.width, height: canvas.height, rgba: btoa(binary)};
  }, bytes.toString('base64'));
  const data = Buffer.from(image.rgba, 'base64');
  assert.equal(data.length, image.width * image.height * 4, 'Decoded screenshot has incomplete RGBA bytes');
  return {width: image.width, height: image.height, data};
}
function screenshotAxes(bounds, image) {
  const ratio = image.pixelRatio;
  assert.ok(Number.isFinite(ratio) && ratio > 0, 'Screenshot needs its actual public device pixel ratio');
  const epsilon = 1e-7;
  const axis = (start, length, pixels) => {
    assert.ok(Number.isFinite(start) && Number.isFinite(length) && length > 0
      && Number.isInteger(pixels) && pixels > 0, 'Invalid public screenshot geometry');
    const low = start * ratio, high = (start + length) * ratio;
    // Chromium/Playwright can enclose the clip in device pixels or enclose CSS
    // coordinates before applying DPR. Accept only the model the PNG size proves.
    let origin = Math.floor(low + epsilon), model = 'device-pixel';
    const deviceSize = Math.ceil(high - epsilon) - origin;
    if (pixels !== deviceSize) {
      model = 'css-first';
      origin = Math.floor(Math.floor(start + epsilon) * ratio + epsilon);
      const cssSize = Math.ceil(Math.ceil(start + length - epsilon) * ratio - epsilon) - origin;
      if (pixels !== cssSize) {
        throw new NeedsInput('PNG size does not match outward rounding of the public region bounds');
      }
    }
    return {origin, model, regionStart: low, regionEnd: high, imagePixels: pixels,
      leading: Math.max(0, Math.ceil(low - origin - epsilon)),
      trailing: Math.max(0, pixels - Math.floor(high - origin + epsilon))};
  };
  return {pixelRatio: ratio, horizontal: axis(bounds.x, bounds.width, image.width),
    vertical: axis(bounds.y, bounds.height, image.height)};
}
function screenshotOverlayRect(rect, axes) {
  assert.ok([rect.x, rect.y, rect.width, rect.height].every(Number.isFinite)
    && rect.width > 0 && rect.height > 0, 'Invalid public overlay rectangle');
  const ratio = axes.pixelRatio, epsilon = 1e-7;
  const left = Math.floor(rect.x * ratio + epsilon) - axes.horizontal.origin;
  const top = Math.floor(rect.y * ratio + epsilon) - axes.vertical.origin;
  const right = Math.ceil((rect.x + rect.width) * ratio - epsilon) - axes.horizontal.origin;
  const bottom = Math.ceil((rect.y + rect.height) * ratio - epsilon) - axes.vertical.origin;
  return {x: left, y: top, width: right - left, height: bottom - top};
}
function screenshotFringeRects(bounds, image) {
  const {horizontal, vertical} = screenshotAxes(bounds, image);
  const rects = [];
  if (horizontal.leading) rects.push({x: 0, y: 0, width: horizontal.leading, height: image.height});
  if (horizontal.trailing) rects.push({x: image.width - horizontal.trailing, y: 0,
    width: horizontal.trailing, height: image.height});
  if (vertical.leading) rects.push({x: 0, y: 0, width: image.width, height: vertical.leading});
  if (vertical.trailing) rects.push({x: 0, y: image.height - vertical.trailing,
    width: image.width, height: vertical.trailing});
  return rects;
}

function validatePixels(image) {
  assert.ok(Number.isInteger(image?.width) && image.width > 0
    && Number.isInteger(image.height) && image.height > 0
    && image.data?.length === image.width * image.height * 4, 'Missing decoded external image pixels');
}
function coloredBodyPixels(image, excludedRects = []) {
  validatePixels(image);
  let count = 0;
  for (let y = 0; y < image.height; y++) for (let x = 0; x < image.width; x++) {
    if (excludedRects.some(rect => x >= rect.x && x < rect.x + rect.width
      && y >= rect.y && y < rect.y + rect.height)) continue;
    const i = (y * image.width + x) * 4;
    const red = image.data[i], green = image.data[i + 1], blue = image.data[i + 2];
    if (image.data[i + 3] > 40 && Math.max(red, green, blue) - Math.min(red, green, blue) > 32
      && Math.min(red, green, blue) < 245) count++;
  }
  return count;
}
function assertOverallPng(image, pixelRatio) {
  validatePixels(image);
  assert.ok(Number.isFinite(pixelRatio) && pixelRatio > 0, 'Overall PNG requires actual DPR');
  const padding = Math.round(30 * pixelRatio);
  assert.ok(image.width > padding * 2 && image.height > padding * 2, 'Overall PNG has no graph body');
  for (let y = 0; y < image.height; y++) for (let x = 0; x < image.width; x++) {
    const i = (y * image.width + x) * 4;
    assert.equal(image.data[i + 3], 255, 'Overall PNG contains transparent pixels');
    if (x < padding || x >= image.width - padding || y < padding || y >= image.height - padding) {
      assert.ok(image.data[i] === 255 && image.data[i + 1] === 255 && image.data[i + 2] === 255,
        'Overall PNG does not preserve the required white 30 CSS pixel border');
    }
  }
  const coloredPixels = coloredBodyPixels(image);
  assert.ok(coloredPixels > 30, 'Overall PNG has no nonempty colored graph body');
  return {contract: 'overall', width: image.width, height: image.height, pixelRatio,
    opaque: true, requiredWhiteBorderPixels: padding, coloredPixels,
    coverage: 'opaque white border and nonempty body; individual node/edge pixels require the independent strict fixture'};
}
function assertOffscreenPan(before, after, excludedBefore = [], excludedAfter = []) {
  validatePixels(before); validatePixels(after);
  assert.equal(after.width, before.width, 'Pan changed screenshot width');
  assert.equal(after.height, before.height, 'Pan changed screenshot height');
  const beforePixels = coloredBodyPixels(before, excludedBefore), afterPixels = coloredBodyPixels(after, excludedAfter);
  assert.ok(beforePixels > 30, 'Offscreen pan has no visible colored node body before input');
  const remainingRatio = afterPixels / beforePixels;
  assert.ok(remainingRatio <= .01, 'Actual public pan did not move colored graph body offscreen');
  return {beforeColoredPixels: beforePixels, afterColoredPixels: afterPixels, remainingRatio,
    maximumRemainingRatio: .01};
}
function assertOverallPanInvariant(before, after, panProof) {
  assert.ok(panProof && Number.isInteger(panProof.beforeColoredPixels) && panProof.beforeColoredPixels > 30
    && Number.isInteger(panProof.afterColoredPixels) && panProof.afterColoredPixels >= 0
    && panProof.remainingRatio >= 0 && panProof.remainingRatio <= .01
    && panProof.afterColoredPixels / panProof.beforeColoredPixels === panProof.remainingRatio,
    'Overall export lacks actual offscreen pan pixel proof');
  for (const download of [before, after]) {
    assert.ok(download?.file && /^[a-f0-9]{64}$/.test(download.sha256)
      && download.contract?.contract === 'overall' && download.dimensions?.width === download.contract.width
      && download.dimensions?.height === download.contract.height, 'Overall export lacks a validated actual download');
  }
  assert.deepEqual(after.dimensions, before.dimensions, 'Overall PNG dimensions changed after pure pan');
  assert.equal(after.sha256, before.sha256, 'Overall PNG bytes changed after pure pan');
  return {contract: 'overall', purePanInvariant: true, identicalDownloadBytes: true, ...panProof,
    coverage: 'real downloads remain identical after public offscreen pan; per-ID pixel fidelity is a separate strict fixture'};
}

function loadPixelmatch() {
  const packages = [path.resolve(__dirname, '../../../hubble-fe/package.json')];
  if (process.env.PLAYWRIGHT_NODE_MODULES) {
    packages.push(path.resolve(process.env.PLAYWRIGHT_NODE_MODULES, '../package.json'));
  }
  for (const packageFile of packages) {
    try {
      const requireFrontend = createRequire(packageFile);
      const module = requireFrontend('pixelmatch');
      const compare = module.default || module;
      const version = requireFrontend('pixelmatch/package.json').version;
      if (typeof compare === 'function' && typeof version === 'string') return {compare, version};
    }
    catch { /* Only use an existing frontend installation; never install in acceptance. */ }
  }
  throw new NeedsInput('Installed frontend pixelmatch dependency unavailable; use Node 24 and existing frontend dependencies');
}

function assertPngMatchesCanvas(screen, exported, excludedRects = []) {
  assert.ok(Math.abs(screen.width - exported.width) <= 1 && Math.abs(screen.height - exported.height) <= 1,
    'Viewport PNG dimensions differ from the actual Chrome canvas screenshot');
  const ignored = (x, y) => excludedRects.some(rect => x >= rect.x && y >= rect.y
    && x < rect.x + rect.width && y < rect.y + rect.height);
  const composite = (image, x, y) => {
    const offset = (y * image.width + x) * 4, alpha = image.data[offset + 3] / 255;
    return [0, 1, 2].map(index => image.data[offset + index] * alpha + 255 * (1 - alpha));
  };
  const foreground = image => {
    const mask = new Uint8Array(image.width * image.height);
    for (let y = 0; y < image.height; y++) for (let x = 0; x < image.width; x++) {
      if (ignored(x, y)) continue;
      const color = composite(image, x, y);
      const spread = Math.max(...color) - Math.min(...color), minimum = Math.min(...color);
      // Include gray edges/black ink without counting Hubble's pale neutral CSS grid.
      mask[y * image.width + x] = (spread > 24 && minimum < 250) || (spread <= 24 && minimum < 225) ? 1 : 0;
    }
    return mask;
  };
  const {compare, version} = loadPixelmatch();
  const padded = {width: Math.max(screen.width, exported.width), height: Math.max(screen.height, exported.height)};
  const prepare = image => {
    const data = new Uint8ClampedArray(padded.width * padded.height * 4).fill(255);
    for (let y = 0; y < image.height; y++) for (let x = 0; x < image.width; x++) {
      if (ignored(x, y)) continue;
      const offset = (y * padded.width + x) * 4;
      const color = composite(image, x, y);
      for (let channel = 0; channel < 3; channel++) data[offset + channel] = color[channel];
    }
    return data;
  };
  const diff = new Uint8ClampedArray(padded.width * padded.height * 4);
  const aaOptions = {threshold: .1, includeAA: false, alpha: 0, aaColor: [0, 255, 0],
    diffColor: [255, 0, 0], checkerboard: false, diffMask: false};
  const differentPixels = compare(prepare(screen), prepare(exported), diff, padded.width, padded.height, aaOptions);
  const aaMask = new Uint8Array(padded.width * padded.height);
  let aaPixels = 0;
  for (let index = 0; index < aaMask.length; index++) {
    if (diff[index * 4] === 0 && diff[index * 4 + 1] === 255 && diff[index * 4 + 2] === 0) {
      aaMask[index] = 1; aaPixels++;
    }
  }
  const screenMask = foreground(screen), exportMask = foreground(exported);
  const coverage = (source, sourceMask, target, targetMask, direction) => {
    let colored = 0, matched = 0, rgbUnmatched = 0, aaConsidered = 0, aaAccepted = 0;
    const tiles = new Map();
    for (let y = 0; y < source.height; y++) for (let x = 0; x < source.width; x++) {
      if (!sourceMask[y * source.width + x]) continue;
      colored++;
      let hit = false, targetForeground = false;
      const sourceColor = composite(source, x, y);
      for (let dy = -1; dy <= 1 && !hit; dy++) for (let dx = -1; dx <= 1; dx++) {
        const nx = x + dx, ny = y + dy;
        if (nx >= 0 && ny >= 0 && nx < target.width && ny < target.height && targetMask[ny * target.width + nx]) {
          targetForeground = true;
          const targetColor = composite(target, nx, ny);
          if (Math.hypot(...sourceColor.map((value, index) => value - targetColor[index])) <= 45) {
            hit = true; break;
          }
        }
      }
      if (!hit) {
        rgbUnmatched++;
        if (aaMask[y * padded.width + x]) {
          aaConsidered++;
          // AA can reconcile edge intensity only where the other image actually
          // contains graph ink. It cannot excuse a missing node or edge in white space.
          if (targetForeground) {hit = true; aaAccepted++;}
        }
      }
      if (hit) matched++;
      const key = `${Math.floor(x / 32)}:${Math.floor(y / 32)}`;
      if (!tiles.has(key)) tiles.set(key, {colored: 0, matched: 0});
      const tile = tiles.get(key); tile.colored++; if (hit) tile.matched++;
    }
    assert.ok(colored > 30, 'PNG comparison has no unoccluded actual graph pixels');
    const ratio = matched / colored;
    assert.ok(ratio >= .98, 'Viewport PNG does not preserve the actual graph pixel positions');
    const significant = [...tiles].filter(([, tile]) => tile.colored >= 8);
    assert.ok(significant.length > 0, 'PNG comparison has no significant graph pixel region');
    const [worstTileCoordinate, worst] = significant.reduce((previous, current) =>
      current[1].matched / current[1].colored < previous[1].matched / previous[1].colored ? current : previous);
    const worstTile = worst.matched / worst.colored;
    assert.ok(worstTile >= .9, 'Viewport PNG omits or adds a localized graph node/edge segment: '
      + `${direction} tile ${worstTileCoordinate} ratio ${worstTile.toFixed(4)} (${worst.matched}/${worst.colored})`);
    return {colored, matched, ratio, worstTile, worstTileCoordinate, rgbUnmatched, aaConsidered, aaAccepted};
  };
  return {screenToExport: coverage(screen, screenMask, exported, exportMask, 'screenToExport'),
    exportToScreen: coverage(exported, exportMask, screen, screenMask, 'exportToScreen'),
    whiteComposite: true, tolerancePixels: 1, maxColorDistance: 45, excludedRects,
    antiAliasing: {library: 'pixelmatch', version, options: aaOptions, padded, aaPixels, differentPixels}};
}

function segmentHitsRect(start, end, rect, padding = 32) {
  let enter = 0, leave = 1;
  for (const [axis, size] of [['x', 'width'], ['y', 'height']]) {
    const low = rect[axis] - padding, high = rect[axis] + rect[size] + padding;
    const delta = end[axis] - start[axis];
    if (delta === 0) {
      if (start[axis] < low || start[axis] > high) return false;
    }
    else {
      const first = (low - start[axis]) / delta, second = (high - start[axis]) / delta;
      enter = Math.max(enter, Math.min(first, second));
      leave = Math.min(leave, Math.max(first, second));
      if (enter > leave) return false;
    }
  }
  return true;
}
function dragTarget(point, neighbor, bounds, obstacles = [], beforePixels, diagnostics = {}) {
  // Predeclared distances only; never fit a target to held pixels or weaken the
  // later filled-node/edge-reconnection assertions. Preserve both 110px choices
  // before trying 90px; the unchanged edge-band checks must still prove movement.
  Object.assign(diagnostics, {point, neighbor, bounds, obstacles, distances: [110, 90], candidates: [],
    screenshot: beforePixels ? {width: beforePixels.width, height: beforePixels.height} : null});
  delete diagnostics.failure;
  const fail = reason => {diagnostics.failure = reason; throw new NeedsInput(reason);};
  if (!beforePixels) fail('Real before-drag screenshot pixels are required to select an unoccupied target');
  const targetPixels = target => {
    const center = {x: Math.round((target.x - bounds.x) * beforePixels.width / bounds.width),
      y: Math.round((target.y - bounds.y) * beforePixels.height / bounds.height)};
    let ink = 0;
    for (let y = -4; y <= 4; y++) for (let x = -4; x <= 4; x++) {
      const px = center.x + x, py = center.y + y;
      if (px < 0 || py < 0 || px >= beforePixels.width || py >= beforePixels.height) {
        return {center, outsideScreenshot: true};
      }
      const offset = (py * beforePixels.width + px) * 4, alpha = beforePixels.data[offset + 3] / 255;
      const color = [0, 1, 2].map(index => beforePixels.data[offset + index] * alpha + 255 * (1 - alpha));
      if (Math.min(...color) < 240) ink++;
    }
    // A few background grid dots are harmless; an existing filled node is not.
    return {center, inkPixels: ink, sampledPixels: 81, maximumInkRatio: .15, empty: ink / 81 <= .15};
  };
  const length = Math.hypot(neighbor.x - point.x, neighbor.y - point.y);
  diagnostics.endpointDistance = length;
  if (length < 130) fail('Live-drag pixel evidence needs separated real endpoints (at least 130 CSS pixels)');
  for (const distance of diagnostics.distances) for (const sign of [1, -1]) {
    const target = {x: point.x - sign * (neighbor.y - point.y) / length * distance,
      y: point.y + sign * (neighbor.x - point.x) / length * distance};
    const rejected = [];
    if (!(target.x > bounds.x + 32 && target.y > bounds.y + 32
        && target.x < bounds.x + bounds.width - 32 && target.y < bounds.y + bounds.height - 32)) {
      rejected.push('viewport-margin');
    }
    const overlayIndices = obstacles.flatMap((rect, index) => segmentHitsRect(point, target, rect) ? [index] : []);
    if (overlayIndices.length) rejected.push('public-overlay');
    const pixels = targetPixels(target);
    if (pixels.outsideScreenshot) rejected.push('outside-screenshot');
    else if (!pixels.empty) rejected.push('occupied-target');
    diagnostics.candidates.push({distance, sign, target, rejected, overlayIndices, pixels, accepted: rejected.length === 0});
    if (!rejected.length) return target;
  }
  fail('Real graph viewport has no unobstructed, unoccupied perpendicular drag target');
}
function dragEndpoint(point, neighbor, bounds, obstacles, beforePixels, diagnostics = {}) {
  // Both positions have already been identified by actual public node tooltips.
  // Keep the same edge and all dragTarget constraints; only its movable end varies.
  diagnostics.attempts = [];
  for (const [index, endpoints] of [[point, neighbor], [neighbor, point]].entries()) {
    const attempt = {};
    diagnostics.attempts.push(attempt);
    try {
      const target = dragTarget(...endpoints, bounds, obstacles, beforePixels, attempt);
      diagnostics.selectedEndpoint = index;
      return {point: endpoints[0], neighbor: endpoints[1], target};
    }
    catch (error) {if (!(error instanceof NeedsInput)) throw error;}
  }
  diagnostics.failure = 'Neither observed endpoint of the real edge has an unobstructed, unoccupied drag target';
  throw new NeedsInput(diagnostics.failure);
}
function assertLiveDragPixels(before, held, {point, target, neighbor}) {
  assert.equal(held.width, before.width); assert.equal(held.height, before.height);
  const pixel = (image, x, y) => {
    x = Math.round(x); y = Math.round(y);
    assert.ok(x >= 0 && y >= 0 && x < image.width && y < image.height, 'Drag sample lies outside Chrome screenshot');
    const offset = (y * image.width + x) * 4;
    // Return numbers in a regular array: Buffer.map would wrap signed RGB differences.
    return [image.data[offset], image.data[offset + 1], image.data[offset + 2]];
  };
  const distance = (a, b) => Math.hypot(...a.map((value, index) => value - b[index]));
  const patch = (image, center) => {
    const values = [];
    for (let y = -2; y <= 2; y++) for (let x = -2; x <= 2; x++) values.push(pixel(image, center.x + x, center.y + y));
    return values;
  };
  const reference = patch(before, point).filter(color => Math.max(...color) - Math.min(...color) > 35);
  assert.ok(reference.length >= 15, 'Drag origin is not within a real filled node');
  const color = [0, 1, 2].map(index => reference.reduce((sum, item) => sum + item[index], 0) / reference.length);
  const density = (image, center) => patch(image, center).filter(value => distance(value, color) < 45).length / 25;
  const fills = {oldBefore: density(before, point), oldHeld: density(held, point),
    newBefore: density(before, target), newHeld: density(held, target)};
  assert.ok(fills.oldBefore >= .6 && fills.oldHeld <= .2 && fills.newBefore <= .2 && fills.newHeld >= .6,
    'Held drag must remove the real filled node at origin and render its filled body at the new location');
  // Read changes in separate old/new edge corridors, away from node bodies. A
  // ghost outline or moved node with stale edges cannot remove the old edge ink.
  const band = (start, fraction, removing) => {
    const dx = neighbor.x - start.x, dy = neighbor.y - start.y, length = Math.hypot(dx, dy);
    const center = {x: start.x + dx * fraction, y: start.y + dy * fraction};
    let changedInk = 0;
    for (let along = -4; along <= 4; along++) for (let across = -16; across <= 16; across++) {
      const sample = {x: center.x + dx / length * along - dy / length * across,
        y: center.y + dy / length * along + dx / length * across};
      if ([point, target, neighbor].some(node => Math.hypot(node.x - sample.x, node.y - sample.y) < 32)) continue;
      const oldColor = pixel(before, sample.x, sample.y), newColor = pixel(held, sample.x, sample.y);
      const ink = removing ? oldColor : newColor, background = removing ? newColor : oldColor;
      if (Math.min(...ink) < 220 && Math.max(...ink) < 250 && distance(ink, background) > 35
          && background.reduce((a, b) => a + b, 0) - ink.reduce((a, b) => a + b, 0) > 45) changedInk++;
    }
    return changedInk;
  };
  const bands = [.38, .5, .62].map(fraction => ({fraction,
    oldEdgeInkRemoved: band(point, fraction, true), newEdgeInkAdded: band(target, fraction, false)}));
  assert.ok(bands.filter(item => item.oldEdgeInkRemoved >= 2 && item.newEdgeInkAdded >= 2).length >= 2,
    'Held drag did not visibly reconnect the associated edge before mouse release');
  return {fills, edgeBands: bands, mouseReleased: false};
}

module.exports = {hasCurrentGraphCount, assertOverallPng, assertOffscreenPan, assertOverallPanInvariant, coloredBodyPixels, MINIMAP_PAINT_SELECTOR, uniqueDisplayId, gremlinIdLiteral, connectedPair, mergeRawGraph, assertExpandedGraph, assertVisibleGraphStats, assertVisibleGraphPanel,
  decodeScreenshot, screenshotAxes, screenshotOverlayRect, screenshotFringeRects, assertPngMatchesCanvas, dragTarget, dragEndpoint, assertLiveDragPixels, NeedsInput, argument, redact, publicHttpUrl, sha256, typedId, canonicalGraph,
  assertExportMatches, assertFixtureGraph, fixtureFromPrefix, loadPlaywright, chromePath,
  businessPayload, scopePath, pngDimensions, pixelEvidence, screenshot, canvasReady, findNode};
