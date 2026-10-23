#!/usr/bin/env node
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

// This gate observes real DOM, screenshots, browser input, public HTTP responses,
// and downloaded artifacts. It never reads React fibers or a G6/controller instance.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {authenticateUi, logoutUi} = require('./ui_auth');
const {
  NeedsInput, argument, redact, publicHttpUrl, sha256, typedId, assertExportMatches, assertFixtureGraph,
  uniqueDisplayId, gremlinIdLiteral, connectedPair, assertExpandedGraph, hasCurrentGraphCount, assertVisibleGraphStats, assertVisibleGraphPanel,
  assertOverallPng, assertOffscreenPan, assertOverallPanInvariant,
  decodeScreenshot, screenshotAxes, screenshotOverlayRect, screenshotFringeRects, assertPngMatchesCanvas, dragTarget, dragEndpoint, assertLiveDragPixels,
  fixtureFromPrefix, loadPlaywright, chromePath, businessPayload, scopePath,
  pngDimensions, pixelEvidence, screenshot, canvasReady, findNode,
} = require('./graph_acceptance_helpers');

const ALL_CHECKS = ['example', 'authentication', 'query', 'schema', 'cards', 'algorithm', 'scope'];
const USAGE = `Usage: node run_ui_graph_acceptance.js [options]
  --hubble-url URL             Existing candidate Hubble; no service is started
  --example-url URL            Defaults to Hubble /visualization.html
  --output-dir DIR --json-output FILE --chromium-executable FILE
  --fixture-json FILE          Existing public dataset/permission contract below
  --data-prefix PREFIX         Reuse run_live_hubble_smoke.py's existing Loader data
  --graphspace NAME --graph NAME
  --checks example,authentication,query,schema,cards,algorithm,scope
  --username VALUE --password VALUE  Also accepts HUBBLE_USERNAME/HUBBLE_PASSWORD;
                                    prefer environment inputs to avoid shell history.

Fixture JSON: {graphspace, graph, query, expected: {vertexIds, edgeIds?, edgeCount?},
  schemaLabels, canWriteSchema, graphDisplayName?,
  algorithm: {source, target, label?, sourceInput?, targetInput?},
  nextScope?: {graphspace, graph, query, expected, graphDisplayName?, graphspaceDisplayName?}}
All records/scopes must already exist on the real Server. No fixture is created,
mocked, or inferred from a fake browser user. Missing required evidence produces
needs-input (exit 2), never a green skip. A subset run is labeled as a subset;
it cannot satisfy fullRequestedMatrix or the separate auth/runtime matrix.
`;
function options(argv = process.argv, environment = process.env) {
  const read = (name, fallback) => argument(argv, name, fallback);
  const hubbleUrl = publicHttpUrl(read('--hubble-url',
    environment.HUBBLE_URL || 'http://127.0.0.1:8088'), 'hubbleUrl').replace(/\/$/, '');
  const exampleUrl = publicHttpUrl(read('--example-url', `${hubbleUrl}/visualization.html`), 'exampleUrl');
  const outputDir = path.resolve(read('--output-dir', 'target/hubble-graph-acceptance'));
  const requested = read('--checks', ALL_CHECKS.join(',')).split(',');
  assert.ok(requested.length > 0 && requested.every(name => ALL_CHECKS.includes(name)), 'Unknown --checks value');
  const graphspace = read('--graphspace', environment.HUGEGRAPH_GRAPHSPACE || 'DEFAULT');
  const graph = read('--graph', environment.HUGEGRAPH_GRAPH || 'hugegraph');
  const fixtureFile = read('--fixture-json', environment.HUBBLE_GRAPH_FIXTURE || '');
  const prefix = read('--data-prefix', environment.HUBBLE_GRAPH_DATA_PREFIX || '');
  const fixture = fixtureFile ? JSON.parse(fs.readFileSync(path.resolve(fixtureFile), 'utf8'))
    : prefix ? fixtureFromPrefix(prefix, graphspace, graph) : null;
  if (fixture) {fixture.graphspace ??= graphspace; fixture.graph ??= graph;}
  return {hubbleUrl, exampleUrl, outputDir, requested, fixture,
    jsonOutput: path.resolve(read('--json-output', path.join(outputDir, 'ui-graph-acceptance.json'))),
    executablePath: chromePath(read('--chromium-executable',
      environment.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || environment.CHROME_PATH || '')),
    username: read('--username', environment.HUBBLE_USERNAME || 'admin'),
    password: read('--password', environment.HUBBLE_PASSWORD || 'pa')};
}
async function eventually(page, description, predicate, timeout = 12000) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    if (await predicate()) return;
    await page.waitForTimeout(80);
  }
  throw new Error(`Timed out: ${description}`);
}
function graphRegion(page) { return page.getByRole('region', {name: 'Graph canvas', exact: true}); }
// The Hubble menu is a sibling of GraphCanvas within its public graph wrapper.
// Primary navigation elsewhere in the page also has role=menu.
function graphContextMenu(page) { return graphRegion(page).locator('..').getByRole('menu'); }
async function closeUiSession(context, browser, hubbleUrl, loginAttempted, report, secrets) {
  try {
    if (loginAttempted) {
      try {
        await logoutUi(context, hubbleUrl);
        report.sessionCleanup = {status: 'passed'};
      }
      catch (error) {
        report.sessionCleanup = {status: 'failed', error: redact(error.message, secrets)};
      }
    }
  }
  finally {await browser.close();}
}
async function blankPointer(page, region) {
  const box = await region.boundingBox();
  await page.mouse.move(box.x + 6, box.y + 6);
  await page.waitForTimeout(120);
}
async function changedPixels(page, region, before, evidenceFile) {
  await blankPointer(page, region);
  const after = await screenshot(page, region, evidenceFile);
  assert.notEqual(after.sha256, before.sha256, 'Real graph pixels did not change after browser interaction');
  return after;
}
async function screenshotInteraction(page, region, file, action) {
  await blankPointer(page, region);
  const before = await screenshot(page, region, `${file}-before.png`);
  await action();
  const after = await changedPixels(page, region, before, `${file}-after.png`);
  return {before, after};
}
async function exerciseCamera(page, region, directory, prefix, zoomButtons = false) {
  const box = await region.boundingBox();
  const pan = await screenshotInteraction(page, region, path.join(directory, `${prefix}-pan`), async () => {
    await page.mouse.move(box.x + 8, box.y + 8);
    await page.mouse.down();
    await page.mouse.move(box.x + 72, box.y + 48, {steps: 12});
    await page.mouse.up();
  });
  const wheel = await screenshotInteraction(page, region, path.join(directory, `${prefix}-wheel`), async () => {
    await page.mouse.move(box.x + box.width * .5, box.y + box.height * .5);
    await page.mouse.wheel(0, -60);
    await page.waitForTimeout(180);
  });
  if (zoomButtons) {
    await page.getByRole('button', {name: 'Fit Center', exact: true}).click();
    await screenshotInteraction(page, region, path.join(directory, `${prefix}-zoom-button`),
      () => page.getByRole('button', {name: 'Zoom In', exact: true}).click());
    await page.getByRole('button', {name: 'Zoom Out', exact: true}).click();
    await page.getByRole('button', {name: 'Fit Center', exact: true}).click();
  }
  return {pan, wheel};
}
async function saveDownload(download, file) {
  assert.ok(download && typeof download.saveAs === 'function', 'Missing actual browser download');
  assert.equal(await download.failure(), null, 'Browser download failed');
  await download.saveAs(file);
  return {file, suggestedFilename: download.suggestedFilename(), sha256: sha256(fs.readFileSync(file))};
}
async function downloadBusiness(page, directory, prefix, kind) {
  const exportButton = page.getByRole('button').filter({hasText: /^Export$/});
  assert.equal(await exportButton.count(), 1, 'Expected the actual visible graph Export button');
  await exportButton.click();
  const name = kind === 'json' ? 'Export JSON' : 'Export image';
  await page.getByRole('menuitem', {name, exact: true}).click();
  const dialog = page.getByRole('dialog', {name, exact: true});
  await dialog.getByLabel('File Name', {exact: true}).fill(`${prefix}-${kind}`.slice(0, 12));
  const pending = page.waitForEvent('download');
  await dialog.getByRole('button', {name: 'OK', exact: true}).click();
  const download = await saveDownload(await pending, path.join(directory, `${prefix}-export.${kind}`));
  await dialog.waitFor({state: 'hidden'});
  return download;
}
async function captureGraphBody(page, region, file) {
  await eventually(page, 'graph is idle before public capture', async () => await region.getAttribute('aria-busy') !== 'true');
  await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
  await blankPointer(page, region);
  const capture = await screenshot(page, region, file);
  const bounds = await region.boundingBox();
  const image = await decodeScreenshot(page, fs.readFileSync(capture.file));
  const pixelRatio = await page.evaluate(() => window.devicePixelRatio);
  const axes = screenshotAxes(bounds, {...image, pixelRatio});
  const overlays = await visibleGraphOverlays(page, region);
  const excluded = [...overlays.map(rect => screenshotOverlayRect(rect, axes)),
    ...screenshotFringeRects(bounds, {...image, pixelRatio})];
  return {capture, image, bounds, pixelRatio, axes, overlays, excluded};
}
function offscreenPanPath(capture) {
  const {bounds, axes, image, excluded} = capture;
  assert.ok(bounds.width > 100 && bounds.height > 50, 'Graph is too small for safe offscreen pan');
  const path = {from: {x: bounds.x + 8, y: bounds.y + 4},
    to: {x: bounds.x + bounds.width - 8, y: bounds.y + 4}};
  const strip = screenshotOverlayRect({x: path.from.x - 2, y: path.from.y - 2,
    width: path.to.x - path.from.x + 4, height: 4}, axes);
  assert.ok(!excluded.some(rect => strip.x < rect.x + rect.width && strip.x + strip.width > rect.x
    && strip.y < rect.y + rect.height && strip.y + strip.height > rect.y),
  'Predeclared top canvas pan path intersects public UI');
  for (let y = strip.y; y < strip.y + strip.height; y++) for (let x = strip.x; x < strip.x + strip.width; x++) {
    const i = (y * image.width + x) * 4;
    const colors = [image.data[i], image.data[i + 1], image.data[i + 2]];
    assert.ok(Math.max(...colors) - Math.min(...colors) <= 32 && Math.min(...colors) >= 100,
      'Predeclared top canvas pan path intersects actual graph ink');
  }
  return path;
}
async function validateOverallDownload(page, download) {
  const bytes = fs.readFileSync(download.file);
  download.dimensions = pngDimensions(bytes);
  download.contract = assertOverallPng(await decodeScreenshot(page, bytes),
    await page.evaluate(() => window.devicePixelRatio));
  return download;
}
async function overallExportWithPan(page, region, directory, prefix, downloadPng, fit) {
  const evidence = {contract: 'overall', status: 'running', paths: [],
    strictPixelFixture: 'Separate mandatory public fixed-fixture Chrome DPR 1/2 gate; these camera checks do not establish per-ID pixel fidelity'};
  try {
    await fit();
    await page.evaluate(() => document.fonts.ready);
    await fit();
    const preflight = await captureGraphBody(page, region, path.join(directory, `${prefix}-overall-pan-preflight.png`));
    const blank = offscreenPanPath(preflight).from;
    // Clear selection through the actual blank canvas before both compared exports.
    // Otherwise a canvas drag may also dismiss a previously selected node's style.
    await page.mouse.click(blank.x, blank.y);
    const before = await captureGraphBody(page, region, path.join(directory, `${prefix}-overall-before-pan.png`));
    evidence.before = before.capture;
    evidence.geometry = {before: {bounds: before.bounds, overlays: before.overlays, axes: before.axes}};
    evidence.initial = await validateOverallDownload(page, await downloadPng(`${prefix}-before-pan`));
    // Four fixed full-width drags, each preceded by public screenshot/path safety checks.
    // No renderer/controller coordinates or private state are inspected.
    for (let index = 0; index < 4; index++) {
      const current = index === 0 ? before : await captureGraphBody(page, region,
        path.join(directory, `${prefix}-pan-${index}.png`));
      const pan = offscreenPanPath(current);
      evidence.paths.push(pan);
      await page.mouse.move(pan.from.x, pan.from.y);
      await page.mouse.down();
      try {await page.mouse.move(pan.to.x, pan.to.y, {steps: 24});}
      finally {await page.mouse.up();}
    }
    const after = await captureGraphBody(page, region, path.join(directory, `${prefix}-overall-offscreen.png`));
    evidence.after = after.capture;
    evidence.geometry.after = {bounds: after.bounds, overlays: after.overlays, axes: after.axes};
    evidence.panProof = assertOffscreenPan(before.image, after.image, before.excluded, after.excluded);
    evidence.panned = await validateOverallDownload(page, await downloadPng(`${prefix}-offscreen`));
    evidence.invariant = assertOverallPanInvariant(evidence.initial, evidence.panned, evidence.panProof);
    evidence.status = 'passed';
    return evidence;
  }
  catch (error) {evidence.status = 'failed'; throw error;}
  finally {
    try {await fit();}
    finally {fs.writeFileSync(path.join(directory, `${prefix}-overall-export-evidence.json`), JSON.stringify(evidence, null, 2) + '\n');}
  }
}
async function exportBusiness(page, region, raw, directory, prefix) {
  const json = await downloadBusiness(page, directory, prefix, 'json');
  assertExportMatches(JSON.parse(fs.readFileSync(json.file, 'utf8')), raw);
  const png = await overallExportWithPan(page, region, directory, prefix,
    name => downloadBusiness(page, directory, name, 'png'),
    () => page.getByRole('button', {name: 'Fit Center', exact: true}).click());
  return {json, png};
}
async function executeQuery(page, config, fixture, prefix, navigate = true) {
  if (navigate) await page.goto(`${config.hubbleUrl}/gremlin/${encodeURIComponent(fixture.graphspace)}/${encodeURIComponent(fixture.graph)}`);
  const editor = page.getByLabel('Gremlin query editor', {exact: true});
  await editor.waitFor({state: 'visible'});
  const run = page.getByRole('button', {name: 'Run Query', exact: true});
  await eventually(page, 'query graph scope is ready before editing', () => run.isEnabled());
  // CodeMirror must observe a real editing transaction, not a contenteditable DOM replacement.
  await editor.click();
  await page.keyboard.press('ControlOrMeta+A');
  await page.keyboard.insertText(fixture.query);
  assert.equal(await editor.innerText(), fixture.query,
    'Actual editor did not retain the exact requested Gremlin query string after input');
  const endpoint = `${scopePath(fixture)}/gremlin-query`;
  await eventually(page, 'query graph scope remains ready before execution', () => run.isEnabled());
  assert.equal(await editor.innerText(), fixture.query,
    'Actual editor did not retain the exact requested Gremlin query string before Run');
  const pending = page.waitForRequest(request => new URL(request.url()).pathname === endpoint
    && request.method() === 'POST');
  await run.click();
  const request = await pending;
  assert.deepEqual(request.postDataJSON(), {content: fixture.query, preview: true},
    'Actual browser POST did not submit the exact requested Gremlin query string with preview enabled');
  const response = await request.response();
  assert.ok(response, 'Actual browser Gremlin request did not receive a response');
  const data = await businessPayload(response, 'Real browser Gremlin query');
  const raw = data?.graph_view;
  assertFixtureGraph(raw, fixture);
  const region = graphRegion(page);
  const pixels = await canvasReady(page, region, path.join(config.outputDir, `${prefix}-ready.png`));
  const counts = await page.getByRole('group').allTextContents();
  const descriptions = await page.getByRole('group').evaluateAll(elements => elements.map(element => element.getAttribute('aria-label') || ''));
  assert.ok(hasCurrentGraphCount(descriptions, 'Nodes', raw.vertices.length), 'Graph model node count does not match real response');
  assert.ok(hasCurrentGraphCount(descriptions, 'Edges', raw.edges.length), 'Graph model edge count does not match real response');
  return {raw, region, evidence: {route: page.url(), endpoint, method: 'POST',
    httpStatus: response.status(), vertexIds: raw.vertices.map(v => v.id), edgeIds: raw.edges.map(e => e.id), pixels, counts}};
}
async function selectVisibleScopeOption(page, combobox, label) {
  await combobox.click();
  // AntD's role=option accessibility proxy is hidden; use the actual painted option row.
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option')
    .getByText(label, {exact: true}).click();
}
async function canvasPanelVisibility(page, region, config, prefix) {
  const wrapper = region.locator('..');
  const graphElement = await region.elementHandle();
  const wrapperElement = await wrapper.elementHandle();
  const before = await wrapper.evaluate(element => {
    const scrolls = [];
    for (let current = element; current; current = current.parentElement) {
      scrolls.push({top: current.scrollTop, left: current.scrollLeft});
    }
    return scrolls;
  });
  const panels = [
    {name: 'Settings', toggle: /\bSettings$/, control: page.getByRole('combobox', {name: /Node Label Property/})},
    {name: 'Layout', toggle: /^(?:deployment-unit )?Layout$/, control: page.getByRole('combobox', {name: 'Layout Type', exact: true})},
    {name: 'Statistics', toggle: /\bStatistics$/, control: page.getByRole('radiogroup', {name: 'Statistics view', exact: true})},
  ];
  const results = [];
  for (const {name, toggle, control} of panels) {
    const button = page.getByRole('button', {name: toggle});
    for (const opening of ['first', 'reopen']) {
      await button.click();
      await control.waitFor({state: 'visible'});
      // Read geometry before interacting with any panel input: locator.click()
      // would scroll clipped controls into view and hide a positioning regression.
      const geometry = await control.evaluate((element, {graphElement, wrapperElement, before, name}) => {
        const rect = value => {
          if (!value) return null;
          const box = value.getBoundingClientRect();
          return {x: box.x, y: box.y, width: box.width, height: box.height};
        };
        let panel = element.parentElement;
        while (panel && !(getComputedStyle(panel).position === 'absolute' && panel.offsetParent === wrapperElement)) {
          panel = panel.parentElement;
        }
        const clips = [];
        for (let current = element.parentElement; current; current = current.parentElement) {
          const style = getComputedStyle(current);
          const x = /hidden|clip|auto|scroll/.test(style.overflowX);
          const y = /hidden|clip|auto|scroll/.test(style.overflowY);
          if (x || y) clips.push({rect: rect(current), x, y});
        }
        const scrolls = [];
        for (let current = wrapperElement; current; current = current.parentElement) {
          const baseline = before[scrolls.length];
          scrolls.push({top: current.scrollTop, left: current.scrollLeft,
            beforeTop: baseline?.top, beforeLeft: baseline?.left});
        }
        return {name, viewport: {x: 0, y: 0, width: innerWidth, height: innerHeight},
          graph: rect(graphElement), panel: rect(panel), control: rect(element), clips, scrolls};
      }, {graphElement, wrapperElement, before, name});
      const evidence = {name, opening, geometry,
        screenshot: await screenshot(page, page, path.join(config.outputDir, `${prefix}-${name.toLowerCase()}-${opening}.png`))};
      try {assertVisibleGraphPanel(geometry); evidence.status = 'passed';}
      catch (error) {evidence.status = 'failed'; evidence.error = error.message;}
      results.push(evidence);
      await button.click();
      await control.waitFor({state: 'hidden'});
    }
  }
  const file = path.join(config.outputDir, `${prefix}-panel-visibility.json`);
  fs.writeFileSync(file, JSON.stringify(results, null, 2) + '\n');
  assert.ok(results.every(result => result.status === 'passed'),
    `Canvas panel controls must be visible without scrolling on first open and reopen; see ${file}`);
  return results;
}
function nodeTooltip(id, vertices) {
  if (!vertices || !uniqueDisplayId(vertices, id)) {
    throw new NeedsInput('Actual node tooltip cannot uniquely distinguish this business ID from another displayed ID');
  }
  return text => text.split(/\r?\n/).some(line => line === `Node ID: ${String(id)}`);
}
function collectGraphOverlayRects(wrapper) {
  const document = wrapper.ownerDocument;
  const view = document.defaultView;
  const bounds = wrapper.querySelector('[role="region"]').getBoundingClientRect();
  const elements = [...wrapper.querySelectorAll('[role="tooltip"], [role="menu"], [aria-label="Legend"]'),
    ...document.querySelectorAll('[role="dialog"], [role="menu"]')];
  // The minimap's canvas and viewport border can overflow its border-box parent.
  // Only include these immediate public UI children, not renderer descendants.
  for (const minimap of wrapper.querySelectorAll('.g6-minimap, .minimap')) {
    elements.push(minimap, ...[...minimap.children].filter(child => ['CANVAS', 'DIV'].includes(child.tagName)));
  }
  for (const toolbar of wrapper.querySelectorAll('[class*="toolBarTop"], [class*="toolBarWithPadding"]')) {
    elements.push(...toolbar.querySelectorAll('button, select, input, [role="button"], [role="switch"], [role="combobox"]'));
  }
  for (const group of document.querySelectorAll('[role="group"][aria-label]')) {
    if (/^(Nodes|Edges): /.test(group.getAttribute('aria-label'))) elements.push(group.parentElement.parentElement);
  }
  return [...new Set(elements)].filter(element => element.getClientRects().length
    && view.getComputedStyle(element).visibility !== 'hidden' && view.getComputedStyle(element).opacity !== '0')
    .map(element => {
      const box = element.getBoundingClientRect();
      return {x: box.x, y: box.y, width: box.width, height: box.height};
    }).filter(rect => rect.width > 0 && rect.height > 0 && rect.x < bounds.right && rect.y < bounds.bottom
      && rect.x + rect.width > bounds.left && rect.y + rect.height > bounds.top);
}
async function visibleGraphOverlays(page, region) {
  return page.evaluate(collectGraphOverlayRects, await region.locator('..').elementHandle());
}
function assertPngWithGeometry(actual, exported, geometry, geometryFile) {
  // Keep current public geometry available even when a subsequent pixel assertion fails.
  fs.writeFileSync(geometryFile, JSON.stringify(geometry, null, 2) + '\n');
  return assertPngMatchesCanvas(actual, exported,
    [...geometry.overlays.map(overlay => overlay.pixels), ...geometry.fringeRects]);
}

async function graphStatistics(page, region, config, prefix) {
  const data = await page.getByRole('group').evaluateAll(elements => {
    const rect = element => {
      const box = element.getBoundingClientRect();
      return {x: box.x, y: box.y, width: box.width, height: box.height};
    };
    const counts = elements.filter(element => /^(Nodes|Edges): /.test(element.getAttribute('aria-label') || ''));
    return {viewport: {x: 0, y: 0, width: innerWidth, height: innerHeight},
      counts: counts.map(element => {
        const card = element.parentElement;
        const clips = [];
        for (let ancestor = card.parentElement; ancestor; ancestor = ancestor.parentElement) {
          const style = getComputedStyle(ancestor);
          const x = /hidden|clip|auto|scroll/.test(style.overflowX);
          const y = /hidden|clip|auto|scroll/.test(style.overflowY);
          if (x || y) clips.push({rect: rect(ancestor), x, y});
        }
        return {label: element.getAttribute('aria-label'), rect: rect(card), clips,
          pane: card.closest('[role="tabpanel"]') ? rect(card.closest('[role="tabpanel"]')) : null};
      })};
  });
  data.minimaps = await region.locator('.g6-minimap, .minimap').evaluateAll(elements => elements
    .filter(element => element.getClientRects().length && getComputedStyle(element).visibility !== 'hidden')
    .map(element => {
      const rect = value => {
        const box = value.getBoundingClientRect();
        return {x: box.x, y: box.y, width: box.width, height: box.height};
      };
      const clips = [];
      for (let ancestor = element.parentElement; ancestor; ancestor = ancestor.parentElement) {
        const style = getComputedStyle(ancestor);
        const x = /hidden|clip|auto|scroll/.test(style.overflowX);
        const y = /hidden|clip|auto|scroll/.test(style.overflowY);
        if (x || y) clips.push({rect: rect(ancestor), x, y});
      }
      return {...rect(element), clips};
    }));
  data.canvas = await region.boundingBox();
  assertVisibleGraphStats(data);
  return {...data, screenshot: await screenshot(page, page, path.join(config.outputDir, `${prefix}-visible-statistics.png`))};
}
async function doubleClickExpansion(page, config, fixture, original) {
  const {source} = connectedPair(original);
  const single = {...fixture, query: `g.V(${gremlinIdLiteral(source.id)})`,
    expected: {vertexIds: [source.id], edgeCount: 0}};
  const before = await executeQuery(page, config, single, 'query-single-node', false);
  const point = await findNode(page, before.region, nodeTooltip(source.id, before.raw.vertices),
    path.join(config.outputDir, 'query-double-click-target.png'));
  const endpoint = `${scopePath(fixture)}/gremlin-query`;
  const pending = page.waitForResponse(response => new URL(response.url()).pathname === endpoint
    && response.request().method() === 'PUT');
  await page.mouse.dblclick(point.x, point.y);
  const response = await pending;
  const request = response.request().postDataJSON();
  assert.equal(typedId(request.vertex_id), typedId(source.id), 'Expand request changed the business ID type/value');
  assert.equal(request.vertex_label, source.label, 'Expand request changed the actual vertex label');
  const incoming = (await businessPayload(response, 'Real double-click expansion'))?.graph_view;
  const expansion = assertExpandedGraph(before.raw, incoming, source.id);
  const expanded = expansion.graph;
  const region = before.region;
  const pixels = await canvasReady(page, region, path.join(config.outputDir, 'query-double-click-expanded.png'));
  await eventually(page, 'real expanded node/edge counts', async () => {
    const labels = await page.getByRole('group').evaluateAll(elements => elements.map(element => element.getAttribute('aria-label') || ''));
    return hasCurrentGraphCount(labels, 'Nodes', expanded.vertices.length)
      && hasCurrentGraphCount(labels, 'Edges', expanded.edges.length);
  });
  // The click timer is 300 ms; observe beyond it before checking for the old masking drawer.
  await page.waitForTimeout(400);
  assert.equal(await page.getByRole('dialog', {name: 'Data Details', exact: true}).isVisible(), false,
    'Double-click opened the single-click Data Details drawer');
  const added = expansion.neighbors.find(vertex => uniqueDisplayId(expanded.vertices, vertex.id));
  if (!added) throw new NeedsInput('New neighbors have ambiguous displayed IDs; provide a fixture with a uniquely identifiable actual neighbor tooltip');
  const neighbor = await findNode(page, region, nodeTooltip(added.id, expanded.vertices),
    path.join(config.outputDir, 'query-expanded-neighbor-hit.png'));
  const exports = await exportBusiness(page, region, expanded, config.outputDir, 'query-expanded');
  const statistics = await graphStatistics(page, region, config, 'query-expanded');
  await executeQuery(page, config, fixture, 'query-after-expand-restore', false);
  return {endpoint, method: 'PUT', selectedId: source.id, selectedIdType: typeof source.id,
    addedVertexIds: expansion.addedVertexIds,
    addedEdgeIds: expansion.addedEdgeIds, detailsDrawerOpened: false,
    pixels, actualNewNeighborTooltip: neighbor.tooltip, exports, statistics};
}

function dragTargetWithEvidence(point, neighbor, bounds, obstacles, beforePixels, file) {
  const diagnostics = {};
  try {return dragTarget(point, neighbor, bounds, obstacles, beforePixels, diagnostics);}
  finally {fs.writeFileSync(file, JSON.stringify(diagnostics, null, 2) + '\n');}
}
async function fixedHistory(page, region, config, prefix, raw) {
  const pair = connectedPair(raw);
  let point, neighbor;
  // Use the real public camera control to make the edge's pixel samples readable.
  for (let attempt = 0; attempt < 5; attempt++) {
    neighbor = await findNode(page, region, nodeTooltip(pair.target.id, raw.vertices),
      path.join(config.outputDir, `${prefix}-neighbor-candidates.png`));
    point = await findNode(page, region, nodeTooltip(pair.source.id, raw.vertices),
      path.join(config.outputDir, `${prefix}-node-candidates.png`));
    if (Math.hypot(neighbor.x - point.x, neighbor.y - point.y) >= 130 || attempt === 4) break;
    await page.getByRole('button', {name: 'Zoom In', exact: true}).click();
    await blankPointer(page, region);
  }
  await blankPointer(page, region);
  const endpointEvidence = path.join(config.outputDir, `${prefix}-drag-endpoint-geometry.json`);
  const endpointScreenshot = await screenshot(page, region,
    path.join(config.outputDir, `${prefix}-drag-endpoint-before.png`));
  const endpointPixels = await decodeScreenshot(page, fs.readFileSync(endpointScreenshot.file));
  const endpointDiagnostics = {};
  try {
    ({point, neighbor} = dragEndpoint(point, neighbor, await region.boundingBox(),
      await visibleGraphOverlays(page, region), endpointPixels, endpointDiagnostics));
  }
  finally {fs.writeFileSync(endpointEvidence, JSON.stringify(endpointDiagnostics, null, 2) + '\n');}
  await page.mouse.click(point.x, point.y);
  const details = page.getByRole('dialog', {name: 'Data Details', exact: true});
  await details.waitFor({state: 'visible'});
  const selectedDetails = await details.innerText();
  await details.getByLabel('Close', {exact: true}).click();
  await details.waitFor({state: 'hidden'});
  const fix = page.getByRole('button', {name: 'Fix Node', exact: true});
  await eventually(page, 'selected node enables Fix Node', () => fix.isEnabled());
  await fix.click();
  await eventually(page, 'pin state is visible', async () => await fix.getAttribute('aria-pressed') === 'true');
  const undo = page.getByRole('button', {name: 'Undo', exact: true});
  const redo = page.getByRole('button', {name: 'Redo', exact: true});
  assert.ok(await undo.isEnabled(), 'Pin operation did not create an undo command');
  await undo.click();
  await eventually(page, 'undo restores unpinned state', async () => await fix.getAttribute('aria-pressed') === 'false');
  await redo.click();
  await eventually(page, 'redo restores pinned state', async () => await fix.getAttribute('aria-pressed') === 'true');
  await blankPointer(page, region);
  const bounds = await region.boundingBox();
  const obstacles = await visibleGraphOverlays(page, region);
  const before = await screenshot(page, region, path.join(config.outputDir, `${prefix}-node-drag-before.png`));
  const beforePixels = await decodeScreenshot(page, fs.readFileSync(before.file));
  const targetEvidence = path.join(config.outputDir, `${prefix}-drag-target-geometry.json`);
  const target = dragTargetWithEvidence(point, neighbor, bounds, obstacles, beforePixels, targetEvidence);
  const local = value => ({x: (value.x - bounds.x) * beforePixels.width / bounds.width,
    y: (value.y - bounds.y) * beforePixels.height / bounds.height});
  let held, heldPixels;
  try {
    await page.mouse.move(point.x, point.y);
    await page.mouse.down();
    await page.mouse.move(target.x, target.y, {steps: 16});
    await page.waitForTimeout(120);
    held = await screenshot(page, region, path.join(config.outputDir, `${prefix}-node-drag-held.png`));
    const image = await decodeScreenshot(page, fs.readFileSync(held.file));
    heldPixels = assertLiveDragPixels(beforePixels, image,
      {point: local(point), target: local(target), neighbor: local(neighbor)});
  }
  finally {await page.mouse.up();}
  await page.waitForTimeout(100);
  if (await details.isVisible()) {
    await details.getByLabel('Close', {exact: true}).click();
    await details.waitFor({state: 'hidden'});
  }
  const drag = {before, held, heldPixels, targetEvidence, endpointEvidence, endpointScreenshot, edgeId: pair.edge.id,
    after: await changedPixels(page, region, before, path.join(config.outputDir, `${prefix}-node-drag-after.png`))};
  await eventually(page, 'real drag completes without runtime alert', async () => await region.getByRole('alert').count() === 0);
  return {selectedTooltip: point.tooltip, selectedDetails, pinnedAfterRedo: true, drag,
    screenshot: await screenshot(page, region, path.join(config.outputDir, `${prefix}-fixed-history.png`))};
}
async function exampleCheck(page, config) {
  await page.goto(config.exampleUrl);
  const main = page.getByRole('region', {name: 'Interactive graph', exact: true});
  const preview = page.getByRole('region', {name: 'Preview graph', exact: true});
  const thumbnail = page.getByRole('region', {name: 'Thumbnail graph', exact: true});
  await canvasReady(page, main, path.join(config.outputDir, 'example-ready.png'));
  await canvasReady(page, preview, path.join(config.outputDir, 'example-preview.png'));
  await canvasReady(page, thumbnail, path.join(config.outputDir, 'example-thumbnail.png'));
  await page.getByLabel('Layout', {exact: true}).selectOption('grid');
  await blankPointer(page, main);
  const previewBefore = await screenshot(page, preview, path.join(config.outputDir, 'example-preview-before.png'));
  const thumbnailBefore = await screenshot(page, thumbnail, path.join(config.outputDir, 'example-thumbnail-before.png'));
  const point = await findNode(page, main, text => /^node: /.test(text), path.join(config.outputDir, 'example-candidates.png'));
  await page.mouse.click(point.x, point.y);
  const position = page.locator('dt').filter({hasText: /^Position$/}).locator('xpath=following-sibling::dd[1]');
  await position.waitFor({state: 'visible'});
  const positionBefore = await position.innerText();
  await page.mouse.move(point.x, point.y);
  await page.mouse.down();
  await page.mouse.move(point.x + 46, point.y + 29, {steps: 14});
  await page.mouse.up();
  await eventually(page, 'public Position changes after actual node drag', async () => await position.innerText() !== positionBefore);
  await page.getByRole('button', {name: 'Pin selected', exact: true}).click();
  const fixed = page.locator('dt').filter({hasText: /^Fixed$/}).locator('xpath=following-sibling::dd[1]');
  await eventually(page, 'public fixed state', async () => await fixed.innerText() === 'yes');
  const pinnedPosition = await position.innerText();
  await page.getByLabel('Layout', {exact: true}).selectOption('dagre');
  await eventually(page, 'layout finished', async () => await main.getAttribute('aria-busy') !== 'true');
  assert.equal(await position.innerText(), pinnedPosition, 'Pinned world position moved during layout');
  await page.getByRole('button', {name: 'Undo', exact: true}).click();
  await eventually(page, 'example undo', async () => await fixed.innerText() === 'no');
  await page.getByRole('button', {name: 'Redo', exact: true}).click();
  await eventually(page, 'example redo', async () => await fixed.innerText() === 'yes');
  await page.getByRole('button', {name: 'Fit view', exact: true}).click();
  await exerciseCamera(page, main, config.outputDir, 'example');
  await page.getByRole('button', {name: 'Fit view', exact: true}).click();
  await blankPointer(page, main);
  const png = await overallExportWithPan(page, main, config.outputDir, 'example', async name => {
    const pending = page.waitForEvent('download');
    await page.getByRole('button', {name: 'Export PNG', exact: true}).click();
    return saveDownload(await pending, path.join(config.outputDir, `${name}-export.png`));
  }, () => page.getByRole('button', {name: 'Fit view', exact: true}).click());
  assert.equal((await screenshot(page, preview, path.join(config.outputDir, 'example-preview-after.png'))).sha256,
               previewBefore.sha256, 'Interactive commands changed the independent preview');
  assert.equal((await screenshot(page, thumbnail, path.join(config.outputDir, 'example-thumbnail-after.png'))).sha256,
               thumbnailBefore.sha256, 'Interactive commands changed the independent thumbnail');
  await page.getByRole('button', {name: 'Merge neighbor', exact: true}).click();
  await eventually(page, 'merged node is rendered in public status', async () => (await page.getByRole('status').innerText()).includes('4 nodes'));
  await page.getByRole('button', {name: 'Reset dataset', exact: true}).click();
  await eventually(page, 'new dataset clears history', async () => await page.getByRole('button', {name: 'Undo', exact: true}).isDisabled());
  await page.getByRole('button', {name: 'Empty graph', exact: true}).click();
  await eventually(page, 'empty public status', async () => (await page.getByRole('status').innerText()).includes('0 nodes · 0 edges'));
  assert.equal(await preview.getByRole('alert').count(), 0);
  return {route: config.exampleUrl, dragChangedPosition: true, fixedAcrossLayout: true,
    history: true, independentInstances: true, emptyDataset: true, png};
}

async function main(configuration = options()) {
  const config = {...configuration,
    hubbleUrl: publicHttpUrl(configuration.hubbleUrl, 'hubbleUrl').replace(/\/$/, ''),
    exampleUrl: publicHttpUrl(configuration.exampleUrl, 'exampleUrl')};
  fs.mkdirSync(config.outputDir, {recursive: true});
  fs.mkdirSync(path.dirname(config.jsonOutput), {recursive: true});
  const secrets = [config.password];
  const {chromium} = loadPlaywright();
  // Cooperative interruption must retain this cookie jar until logout verifies 401.
  let browser;
  let context;
  let page;
  let interrupted = false;
  const assertRunning = () => {if (interrupted) throw new Error('Acceptance interrupted');};
  const interrupt = () => {
    if (interrupted) return;
    interrupted = true;
    // Closing only the page interrupts pending DOM waits without disposing request cookies.
    if (page) void page.close().catch(() => {});
  };
  const signals = ['SIGTERM', 'SIGINT', 'SIGHUP'];
  for (const signal of signals) process.on(signal, interrupt);
  const errors = [];
  const report = {hubbleUrl: config.hubbleUrl, requestedChecks: config.requested,
    scope: config.fixture ? {graphspace: config.fixture.graphspace, graph: config.fixture.graph} : null,
    browser: null, viewport: {width: 1440, height: 900, dpr: 1},
    evidenceModel: 'Real DOM, screenshot pixels, browser input, live public HTTP and downloads; no private React/G6 state',
    checks: [], remainingMatrix: ['Run both real authenticated and anonymous environments',
      'PD/non-PD capabilities, account visibility and connection resolver modes require their actual environments',
      'Rapid concurrent query response ordering and memory/resource release need additional targeted evidence']};
  let authenticated = false;
  let loginAttempted = false;
  const requireBusiness = () => {
    if (!authenticated) throw new NeedsInput('Actual authentication/capability setup did not pass');
    if (!config.fixture) throw new NeedsInput('Provide --fixture-json or --data-prefix from an existing real Server fixture');
    if (typeof config.fixture.query !== 'string' || !config.fixture.query.trim()
        || !config.fixture.expected?.vertexIds?.length) {
      throw new NeedsInput('Fixture requires a real read-only query and expected.vertexIds');
    }
    return config.fixture;
  };
  async function check(name, run) {
    if (!config.requested.includes(name)) return;
    const start = errors.length;
    const result = {name, startedAt: new Date().toISOString(), status: 'failed'};
    try {
      assertRunning();
      result.evidence = await run();
      assertRunning();
      assert.deepEqual(errors.slice(start), [], 'Actual browser emitted errors during the check');
      result.status = 'passed';
    }
    catch (error) {
      result.status = error instanceof NeedsInput ? 'needs input' : 'failed';
      result.error = redact(error.message, secrets);
      result.browserErrors = errors.slice(start);
      try {await screenshot(page, page, path.join(config.outputDir, `${name}-failure.png`));}
      catch { /* The primary failure remains authoritative. */ }
    }
    report.checks.push(result);
    fs.writeFileSync(config.jsonOutput, JSON.stringify(report, null, 2) + '\n');
  }
  try {
    browser = await chromium.launch({headless: true, executablePath: config.executablePath,
      handleSIGTERM: false, handleSIGINT: false, handleSIGHUP: false});
    assertRunning();
    context = await browser.newContext({viewport: {width: 1440, height: 900}, deviceScaleFactor: 1,
      locale: 'en-US', acceptDownloads: true});
    page = await context.newPage();
    page.setDefaultTimeout(20000);
    await page.addInitScript(() => window.localStorage.setItem('languageType', 'en-US'));
    page.on('pageerror', error => errors.push({kind: 'pageerror', message: redact(error.message, secrets)}));
    page.on('console', message => {
      if (message.type() === 'error') errors.push({kind: 'console', message: redact(message.text(), secrets)});
    });
    report.browser = await browser.version();
    await check('example', () => exampleCheck(page, config));
    // Authentication is a prerequisite even when --checks selects a business subset.
    const authRequested = config.requested.includes('authentication');
    if (!authRequested && config.requested.some(name => !['example', 'authentication'].includes(name))) {
      config.requested = [...config.requested, 'authentication'];
    }
    await check('authentication', async () => {
      const capabilities = await businessPayload(await context.request.get(
        `${config.hubbleUrl}/api/v1.3/config`, {timeout: 5000}), 'config');
      assertRunning();
      if (capabilities.server_capabilities_verified !== true) throw new NeedsInput('Server capabilities must be verified by the actual Hubble backend');
      const anonymous = capabilities.auth_enabled === false;
      let identity;
      if (!anonymous) {
        // Login can set a cookie before a subsequent identity/config check fails.
        loginAttempted = true;
        identity = await authenticateUi(context, page, config.hubbleUrl, config.username, config.password, assertRunning);
      }
      else await page.addInitScript(() => window.sessionStorage.removeItem('user_'));
      await page.goto(`${config.hubbleUrl}/gremlin`);
      if (anonymous) {
        assert.equal(await page.locator('a[href="/account"], a[href="/profile"]').count(), 0,
                     'Anonymous UI exposes account/profile navigation');
        assert.equal(await page.locator('input[type="password"]:visible').count(), 0, 'Anonymous route redirected to login');
      }
      authenticated = true;
      report.authentication = {mode: anonymous ? 'anonymous' : 'authenticated',
        pdEnabled: capabilities.pd_enabled, serverCapabilitiesVerified: true,
        ...(identity ? {username: identity.username, role: identity.role} : {})};
      return report.authentication;
    });
    await check('query', async () => {
      const fixture = requireBusiness();
      const {raw, region, evidence} = await executeQuery(page, config, fixture, 'query');
      evidence.panels = await canvasPanelVisibility(page, region, config, 'query');
      evidence.statistics = await graphStatistics(page, region, config, 'query');
      evidence.doubleClickExpansion = await doubleClickExpansion(page, config, fixture, raw);
      evidence.camera = await exerciseCamera(page, region, config.outputDir, 'query', true);
      evidence.fixedHistory = await fixedHistory(page, region, config, 'query', raw);
      evidence.exports = await exportBusiness(page, region, raw, config.outputDir, 'query');
      const before = await region.locator('canvas').first().elementHandle();
      await executeQuery(page, config, fixture, 'query-rerun', false);
      await eventually(page, 'new query clears Undo and Redo', async () =>
        await page.getByRole('button', {name: 'Undo', exact: true}).isDisabled()
          && await page.getByRole('button', {name: 'Redo', exact: true}).isDisabled());
      const retained = await before.evaluate(element => element.isConnected);
      assert.ok(retained, 'Same-scope new query replaced the existing canvas instance');
      evidence.rerun = {historyCleared: true, oldCanvasStillConnected: retained};
      evidence.page = await screenshot(page, page, path.join(config.outputDir, 'query-page.png'));
      return evidence;
    });
    await check('schema', async () => {
      const fixture = requireBusiness();
      if (!fixture.schemaLabels?.length) throw new NeedsInput('fixture.schemaLabels must name actual schema records');
      const endpoint = `${scopePath(fixture)}/schema/graphview`;
      const response = page.waitForResponse(item => new URL(item.url()).pathname === endpoint && item.request().method() === 'GET');
      await page.goto(`${config.hubbleUrl}/graphspace/${encodeURIComponent(fixture.graphspace)}/graph/${encodeURIComponent(fixture.graph)}/meta`);
      const raw = await businessPayload(await response, 'Real schema graphview');
      const labels = [...(raw.vertices || []), ...(raw.edges || [])].map(item => item.label || item.id);
      for (const label of fixture.schemaLabels) assert.ok(labels.includes(label), 'Actual schema response lacks fixture label');
      const region = page.getByRole('region', {name: 'Metadata Management', exact: true});
      const pixels = await canvasReady(page, region, path.join(config.outputDir, 'schema-ready.png'));
      const hover = await findNode(page, region, text => fixture.schemaLabels.some(label => text.includes(label)),
        path.join(config.outputDir, 'schema-candidates.png'));
      await page.mouse.dblclick(hover.x, hover.y);
      if (fixture.canWriteSchema === true) await page.getByRole('dialog').waitFor({state: 'visible'});
      else if (fixture.canWriteSchema === false) {
        await page.waitForTimeout(250);
        assert.equal(await page.getByRole('dialog').count(), 0, 'Read-only schema opened an editor');
      }
      else throw new NeedsInput('Set fixture.canWriteSchema from the actual account/GraphSpace permission to close double-click permission gate');
      const editor = await screenshot(page, page, path.join(config.outputDir, 'schema-double-click.png'));
      await page.keyboard.press('Escape');
      return {endpoint, labels, pixels, hover: hover.tooltip, canWriteSchema: fixture.canWriteSchema, editor};
    });
    await check('cards', async () => {
      const fixture = requireBusiness();
      await page.goto(`${config.hubbleUrl}/graphspace/${encodeURIComponent(fixture.graphspace)}`);
      const cardView = page.getByRole('radio', {name: 'Card View', exact: true});
      if (await cardView.count()) await cardView.check();
      const name = `View Schema for ${fixture.graphDisplayName || fixture.graph}`;
      const region = page.getByRole('region', {name, exact: true});
      const pixels = await canvasReady(page, region, path.join(config.outputDir, 'card-ready.png'));
      const cardPage = await screenshot(page, page, path.join(config.outputDir, 'cards-page.png'));
      await page.getByRole('button', {name, exact: true}).click();
      await page.waitForURL(url => url.pathname === `/graphspace/${encodeURIComponent(fixture.graphspace)}/graph/${encodeURIComponent(fixture.graph)}/meta`);
      const schema = page.getByRole('region', {name: 'Metadata Management', exact: true});
      await canvasReady(page, schema, path.join(config.outputDir, 'card-destination-schema.png'));
      return {cardLabel: name, pixels, cardPage, destination: page.url()};
    });
    await check('algorithm', async () => {
      const fixture = requireBusiness();
      if (fixture.algorithm?.source === undefined || fixture.algorithm.source === null
          || fixture.algorithm?.target === undefined || fixture.algorithm.target === null) {
        throw new NeedsInput('fixture.algorithm needs real source/target IDs');
      }
      await page.goto(`${config.hubbleUrl}/algorithms/${encodeURIComponent(fixture.graphspace)}/${encodeURIComponent(fixture.graph)}`);
      await page.getByPlaceholder('Algorithm Query', {exact: true}).fill('Find Shortest Path');
      await page.waitForTimeout(350); // Existing search input debounces its public filter for 200ms.
      const title = page.locator('.ant-collapse-header .ant-typography').filter({hasText: /^Find Shortest Path$/});
      await title.click();
      const panel = title.locator('xpath=ancestor::div[contains(concat(" ", normalize-space(@class), " "), " ant-collapse-item ")][1]');
      assert.equal(await panel.count(), 1, 'Algorithm title did not identify a unique public panel');
      await panel.getByLabel('source', {exact: true}).fill(fixture.algorithm.sourceInput ?? String(fixture.algorithm.source));
      await panel.getByLabel('target', {exact: true}).fill(fixture.algorithm.targetInput ?? String(fixture.algorithm.target));
      if (fixture.algorithm.label) await panel.getByLabel('label', {exact: true}).fill(fixture.algorithm.label);
      const run = panel.locator(':scope > .ant-collapse-header button');
      assert.equal(await run.count(), 1, 'Algorithm public header needs an unambiguous Run button');
      await eventually(page, 'valid real algorithm form enables run', () => run.isEnabled());
      const endpoint = `${scopePath(fixture)}/algorithms/oltp/allshortestpaths`;
      const pending = page.waitForResponse(response => new URL(response.url()).pathname === endpoint && response.request().method() === 'POST');
      await run.click();
      const payload = await businessPayload(await pending, 'Real shortest-path algorithm');
      const raw = payload.graph_view;
      assert.ok(raw?.vertices?.length > 0 && raw?.edges?.length > 0, 'Real algorithm returned no graph path');
      assert.ok(raw.vertices.some(vertex => vertex.id === fixture.algorithm.source));
      assert.ok(raw.vertices.some(vertex => vertex.id === fixture.algorithm.target));
      const region = graphRegion(page);
      const pixels = await canvasReady(page, region, path.join(config.outputDir, 'algorithm-ready.png'));
      const hover = await findNode(page, region, text => text.includes('Node ID:'), path.join(config.outputDir, 'algorithm-candidates.png'));
      const exports = await exportBusiness(page, region, raw, config.outputDir, 'algorithm');
      return {endpoint, vertices: raw.vertices.map(vertex => vertex.id), edges: raw.edges.map(edge => edge.id),
        pixels, hover: hover.tooltip, exports,
        page: await screenshot(page, page, path.join(config.outputDir, 'algorithm-page.png'))};
    });
    await check('scope', async () => {
      const fixture = requireBusiness();
      const next = fixture.nextScope;
      if (!next) throw new NeedsInput('Provide fixture.nextScope with a second existing real graph/query/expected IDs');
      if (!next.graphspace || !next.graph || !next.query || !next.expected?.vertexIds?.length) {
        throw new NeedsInput('nextScope requires graphspace, graph, query and expected.vertexIds');
      }
      const previous = await executeQuery(page, config, fixture, 'scope-old');
      const oldCanvas = await previous.region.locator('canvas').first().elementHandle();
      const node = await findNode(page, previous.region, text => text.includes('Node ID:'),
        path.join(config.outputDir, 'scope-old-menu-target.png'));
      await page.mouse.click(node.x, node.y, {button: 'right'});
      await graphContextMenu(page).waitFor({state: 'visible'});
      await graphContextMenu(page).getByRole('menuitem', {name: 'Expand', exact: true}).waitFor({state: 'visible'});
      assert.equal(await graphContextMenu(page).getByRole('menuitem', {name: 'Add Vertex', exact: true}).count(), 0,
        'Right-clicking a node opened the blank canvas menu');
      const oldMenu = await graphContextMenu(page).elementHandle();
      if (next.graphspace !== fixture.graphspace) {
        if (!report.authentication.pdEnabled) throw new NeedsInput('Cross-GraphSpace switching needs a real PD fixture');
        await selectVisibleScopeOption(page, page.getByRole('combobox', {name: 'Current GraphSpace', exact: true}),
          next.graphspaceDisplayName || next.graphspace);
      }
      await selectVisibleScopeOption(page, page.getByRole('combobox', {name: 'Current graph', exact: true}),
        next.graphDisplayName || next.graph);
      await page.waitForURL(url => url.pathname === `/gremlin/${encodeURIComponent(next.graphspace)}/${encodeURIComponent(next.graph)}`);
      await eventually(page, 'scope switch detaches old canvas', async () => !await oldCanvas.evaluate(element => element.isConnected));
      assert.equal(await oldMenu.evaluate(element => element.isConnected), false,
                   'Old graph context menu survived scope switch');
      assert.equal(await graphContextMenu(page).count(), 0, 'New graph retained a context menu after scope switch');
      const current = await executeQuery(page, config, next, 'scope-new', false);
      const exclusiveOldIds = fixture.expected.vertexIds.filter(id => !next.expected.vertexIds.includes(id));
      assert.ok(!current.raw.vertices.some(vertex => exclusiveOldIds.includes(vertex.id)), 'Old fixture record survived into new scope');
      const exports = await exportBusiness(page, current.region, current.raw, config.outputDir, 'scope-new');
      assert.ok(await page.getByRole('button', {name: 'Undo', exact: true}).isDisabled());
      assert.ok(await page.getByRole('button', {name: 'Redo', exact: true}).isDisabled());
      await page.waitForTimeout(700);
      assert.equal(await current.region.getByRole('alert').count(), 0);
      return {from: {graphspace: fixture.graphspace, graph: fixture.graph},
        to: {graphspace: next.graphspace, graph: next.graph}, oldCanvasDetached: true,
        publicContextSwitcher: true, oldContextMenuDetached: true, exclusiveOldIdsChecked: exclusiveOldIds, exports};
    });
  }
  catch (error) {report.startupError = redact(error.message, secrets);}
  finally {
    try {if (browser) await closeUiSession(context, browser, config.hubbleUrl, loginAttempted, report, secrets);}
    finally {for (const signal of signals) process.removeListener(signal, interrupt);}
  }
  if (interrupted) report.interrupted = true;
  report.status = interrupted || report.startupError || report.sessionCleanup?.status === 'failed' ? 'failed'
    : report.checks.every(result => result.status === 'passed') ? 'passed'
    : report.checks.some(result => result.status === 'failed') ? 'failed' : 'needs input';
  report.fullRequestedMatrix = report.status === 'passed' && ALL_CHECKS.every(name => report.checks.some(result => result.name === name && result.status === 'passed'));
  fs.mkdirSync(path.dirname(config.jsonOutput), {recursive: true});
  fs.writeFileSync(config.jsonOutput, JSON.stringify(report, null, 2) + '\n');
  console.log(JSON.stringify({status: report.status, fullRequestedMatrix: report.fullRequestedMatrix,
    checks: report.checks.map(({name, status, error}) => ({name, status, error})), jsonOutput: config.jsonOutput}, null, 2));
  return report;
}
if (require.main === module) {
  if (process.argv.includes('--help')) console.log(USAGE);
  else main().then(report => {process.exitCode = report.status === 'passed' ? 0 : report.status === 'needs input' ? 2 : 1;})
    .catch(error => {
      const message = redact(error.message, [process.env.HUBBLE_PASSWORD,
        argument(process.argv, '--password', '')]);
      const output = path.resolve(argument(process.argv, '--json-output',
        path.join(argument(process.argv, '--output-dir', 'target/hubble-graph-acceptance'), 'ui-graph-acceptance.json')));
      try {
        fs.mkdirSync(path.dirname(output), {recursive: true});
        fs.writeFileSync(output, JSON.stringify({status: 'failed', startupError: message, checks: []}, null, 2) + '\n');
      }
      catch { /* Preserve the original startup failure when evidence storage also fails. */ }
      console.error(message);
      process.exitCode = 1;
    });
}
module.exports = {ALL_CHECKS, USAGE, options, main, graphContextMenu, closeUiSession, nodeTooltip, executeQuery,
  selectVisibleScopeOption, canvasPanelVisibility, collectGraphOverlayRects, assertPngWithGeometry, dragTargetWithEvidence, offscreenPanPath, overallExportWithPan, saveDownload};
