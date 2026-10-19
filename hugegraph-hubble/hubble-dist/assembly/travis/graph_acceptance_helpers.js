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
        if (density >= 28) candidates.push({x, y, density});
      }
    }
    candidates.sort((a, b) => b.density - a.density);
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
async function screenshot(page, target, file) {
  const graphOnly = target !== page;
  const minimaps = graphOnly ? target.locator('.g6-minimap, .minimap') : null;
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
module.exports = {NeedsInput, argument, redact, publicHttpUrl, sha256, typedId, canonicalGraph,
  assertExportMatches, assertFixtureGraph, fixtureFromPrefix, loadPlaywright, chromePath,
  businessPayload, scopePath, pngDimensions, pixelEvidence, screenshot, canvasReady, findNode};
