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

const test = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const {NeedsInput, redact, publicHttpUrl, typedId, assertExportMatches, assertFixtureGraph,
  fixtureFromPrefix, pngDimensions, scopePath} = require('./graph_acceptance_helpers');

const raw = {
  vertices: [{id: 7, label: 'person', properties: {name: '甲', score: 0}, statistics: {degree: 2}},
    {id: '7', label: 'person', properties: {name: '乙'}}],
  edges: [{id: 'edge', source: 7, target: '7', label: 'knows', properties: {weight: .25}}],
};

test('external screenshot decoding preserves packed RGBA bytes across transfer chunks', async () => {
  const {decodeScreenshot} = require('./graph_acceptance_helpers');
  const pixels = Uint8ClampedArray.from({length: 8301 * 4}, (_, index) => index % 256);
  const input = Buffer.from('external screenshot');
  const page = {evaluate: async (fn, encoded) => {
    assert.equal(encoded, input.toString('base64'));
    return vm.runInNewContext(`(${fn.toString()})(encoded)`, {
      encoded, Image: class {
        naturalWidth = 8301;
        naturalHeight = 1;
        async decode() {}
      },
      document: {createElement: () => ({getContext: () => ({
        drawImage() {}, getImageData: () => ({data: pixels}),
      })})},
      btoa: value => Buffer.from(value, 'binary').toString('base64'),
    });
  }};
  const result = await decodeScreenshot(page, input);
  assert.equal(result.width, 8301);
  assert.equal(result.height, 1);
  assert.deepEqual(result.data, Buffer.from(pixels));
  await assert.rejects(decodeScreenshot({evaluate: async () => ({width: 2, height: 2, rgba: 'AA=='})}, input),
    /incomplete RGBA/);
});

test('node candidates are captured only after the previous real tooltip has disappeared', async () => {
  const events = [];
  const tooltip = {
    waitFor: async ({state}) => {events.push(state);},
    innerText: async () => 'Node ID: next-node',
  };
  const page = {
    mouse: {move: async (x, y) => {events.push([x, y]);}},
    locator: () => ({}),
    evaluate: async (_callback, value) => typeof value === 'string'
      ? {width: 100, height: 100, coloredPixels: 80, candidates: [{x: 50, y: 50}]}
      : events.push('frame'),
  };
  const region = {
    getByRole: role => {assert.equal(role, 'tooltip'); return tooltip;},
    locator: () => ({count: async () => 1}),
    boundingBox: async () => ({x: 100, y: 200, width: 100, height: 100}),
    screenshot: async () => {
      assert.deepEqual(events, [[-1, -1], 'hidden', 'frame']);
      events.push('capture');
      return Buffer.from('external screenshot');
    },
  };
  const {findNode} = require('./graph_acceptance_helpers');
  const result = await findNode(page, region, text => text.includes('next-node'), '/unused/unit-test.png');
  assert.equal(result.tooltip, 'Node ID: next-node');
  assert.deepEqual(events.slice(3), ['capture', [150, 250], 'frame', 'visible']);
});
test('raw export comparison accepts record order but preserves complete typed records', () => {
  assertExportMatches({...raw, vertices: [...raw.vertices].reverse()}, raw);
  const changedId = structuredClone(raw);
  changedId.vertices[0].id = '7';
  assert.throws(() => assertExportMatches(changedId, raw));
  const changedEndpoint = structuredClone(raw);
  changedEndpoint.edges[0].source = '7';
  assert.throws(() => assertExportMatches(changedEndpoint, raw));
  const omittedStatistic = structuredClone(raw);
  delete omittedStatistic.vertices[0].statistics;
  assert.throws(() => assertExportMatches(omittedStatistic, raw));
});
test('fixture gate rejects empty, unrelated, and imprecise numeric ID evidence', () => {
  assert.throws(() => assertFixtureGraph(raw, {}), NeedsInput);
  assertFixtureGraph(raw, {expected: {vertexIds: [7, '7'], edgeCount: 1}});
  assert.throws(() => assertFixtureGraph(raw, {expected: {vertexIds: ['wrong'], edgeCount: 1}}));
  assert.throws(() => typedId(Number.MAX_SAFE_INTEGER + 1), /cannot be verified exactly/);
});
test('existing public Loader fixture prefix creates a read-only query, never a new fixture', () => {
  const fixture = fixtureFromPrefix('existing_123', 'DEFAULT', 'g');
  assert.equal(fixture.query, "g.V().hasLabel('existing_123_person').bothE().dedup()");
  assert.equal(fixture.expected.edgeCount, 2);
  assert.deepEqual(fixture.expected.vertexIds, ['existing_123_alice', 'existing_123_bob', 'existing_123_carol']);
  assert.throws(() => fixtureFromPrefix("bad');drop()", 'DEFAULT', 'g'));
  assert.equal(scopePath({graphspace: 'A B', graph: 'g/x'}), '/api/v1.3/graphspaces/A%20B/graphs/g%2Fx');
});
test('redaction removes configured credentials and URL userinfo', () => {
  const output = redact('failed secret-one https://u:secret-two@example.invalid/api', ['secret-one']);
  assert.equal(output, 'failed [REDACTED] https://[REDACTED]@example.invalid/api');
  assert.ok(!output.includes('secret-one') && !output.includes('secret-two'));
});
test('PNG validation rejects arbitrary download bytes and invalid dimensions', () => {
  assert.throws(() => pngDimensions(Buffer.from('not an image')));
  const header = Buffer.alloc(24);
  Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]).copy(header);
  header.write('IHDR', 12, 'ascii');
  header.writeUInt32BE(800, 16);
  header.writeUInt32BE(600, 20);
  assert.deepEqual(pngDimensions(header), {width: 800, height: 600});
  header.writeUInt32BE(0, 16);
  assert.throws(() => pngDimensions(header));
});

test('public acceptance URLs preserve HTTP(S) origins and deployment paths', () => {
  for (const value of ['http://127.0.0.1:8088', 'https://example.invalid/deployment/hubble/',
    'https://example.invalid/deployment/visualization.html', 'http://[::1]:8088/prefix%20name']) {
    assert.equal(publicHttpUrl(value, 'hubbleUrl'), value);
  }
});
test('URL validation rejects credential/query/fragment inputs without reflecting secrets', () => {
  const sentinel = 'URL_INPUT_SECRET_7aQ';
  const encoded = [...sentinel].map(value => `%${value.charCodeAt(0).toString(16)}`).join('');
  for (const value of [
    `https://user:${sentinel}@example.invalid/`, `https://${encoded}@example.invalid/`,
    `https://user:${encoded}@example.invalid/`, `https://example.invalid/?token=${encoded}`,
    `https://example.invalid/#${encoded}`, 'https://@example.invalid/',
    'https://example.invalid/?', 'https://example.invalid/#', 'file:///tmp/example',
    'https:example.invalid', `https://example.invalid/\n${sentinel}`, `not-a-url-${sentinel}`
  ]) {
    assert.throws(() => publicHttpUrl(value, 'exampleUrl'), error => {
      assert.equal(error.message, 'Invalid exampleUrl');
      assert.ok(!String(error).includes(sentinel) && !String(error).includes(encoded));
      return true;
    });
  }
});

test('single-node queries preserve numeric IDs and safely quote string IDs without Groovy interpolation', () => {
  const {gremlinIdLiteral, connectedPair, assertExpandedGraph} = require('./graph_acceptance_helpers');
  assert.equal(gremlinIdLiteral(7), '7');
  assert.equal(gremlinIdLiteral('7'), "'7'");
  assert.equal(gremlinIdLiteral("a'\\\n${danger}"), "'a\\'\\\\\\u000a${danger}'");
  assert.throws(() => connectedPair(raw), NeedsInput);
  const unambiguous = {vertices: [raw.vertices[0], {...raw.vertices[1], id: '8'}],
    edges: [{...raw.edges[0], target: '8'}]};
  const pair = connectedPair(unambiguous);
  assert.equal(pair.source.id, 7);
  assert.equal(pair.target.id, '8');
  const single = {vertices: [raw.vertices[0]], edges: []};
  assertExportMatches(assertExpandedGraph(single, raw, 7).graph, raw);
  assert.throws(() => assertExpandedGraph(single, single, 7), /no neighbor/);
  assert.throws(() => assertExpandedGraph(single, {...raw, edges: []}, 7), /no edge/);
  assert.throws(() => connectedPair({vertices: [raw.vertices[0]], edges: []}), NeedsInput);
});

test('current graph counts accept known and unavailable totals without accepting a different count', () => {
  const {hasCurrentGraphCount} = require('./graph_acceptance_helpers');
  for (const suffix of [', 10 in the full graph', '; full-graph total not obtained']) {
    const labels = [`Nodes: 3 in this result${suffix}`, `Edges: 2 in this result${suffix}`];
    assert.ok(hasCurrentGraphCount(labels, 'Nodes', 3));
    assert.ok(hasCurrentGraphCount(labels, 'Edges', 2));
    assert.equal(hasCurrentGraphCount(labels, 'Nodes', 2), false);
    assert.equal(hasCurrentGraphCount(labels, 'Nodes', 30), false);
    assert.equal(hasCurrentGraphCount(labels, 'Edges', 3), false);
  }
  assert.equal(hasCurrentGraphCount(['Nodes: 3 in this result0'], 'Nodes', 3), false);
});

function statsFixture() {
  return {viewport: {x: 0, y: 0, width: 1000, height: 700}, canvas: {x: 100, y: 300, width: 900, height: 400},
    minimaps: [{x: 100, y: 600, width: 160, height: 100}],
    counts: ['Nodes', 'Edges'].map((label, index) => ({label,
      rect: {x: 800 + index * 60, y: 650, width: 50, height: 40},
      pane: {x: 100, y: 300, width: 900, height: 400}, clips: []}))};
}
test('statistics evidence rejects the real clipping and minimap overlap regressions', () => {
  const {assertVisibleGraphStats} = require('./graph_acceptance_helpers');
  assertVisibleGraphStats(statsFixture());
  for (const change of [
    value => {value.counts[0].rect.y = 680;},
    value => {value.counts[0].pane.height = 360;},
    value => {value.counts[0].clips = [{rect: {x: 100, y: 300, width: 900, height: 370}, x: false, y: true}];},
    value => {value.minimaps[0].x = 790;},
    value => {value.minimaps = [];},
    value => {value.minimaps[0].width = 0;},
    value => {value.minimaps[0].height = 0;},
    value => {value.minimaps[0].x = -200;},
    value => {value.minimaps[0].y = 290;},
    value => {value.minimaps[0].clips = [{rect: {x: 100, y: 600, width: 100, height: 100}, x: true, y: false}];},
    value => {value.counts.pop();},
    value => {value.minimaps[0] = {x: 700, y: 400, width: 100, height: 100};}
  ]) {
    const data = statsFixture(); change(data);
    assert.throws(() => assertVisibleGraphStats(data));
  }
});
function panelGeometry() {
  return {name: 'Settings', viewport: {x: 0, y: 0, width: 1600, height: 1080},
    graph: {x: 334, y: 462, width: 1246, height: 564},
    panel: {x: 1230, y: 462, width: 350, height: 564},
    control: {x: 1254, y: 612, width: 302, height: 32}, clips: [],
    scrolls: [{beforeTop: 0, beforeLeft: 0, top: 0, left: 0}]};
}
test('canvas panel geometry accepts controls contained in the graph and viewport', () => {
  const {assertVisibleGraphPanel} = require('./graph_acceptance_helpers');
  for (const name of ['Settings', 'Layout', 'Statistics']) {
    assertVisibleGraphPanel({...panelGeometry(), name});
  }
});
test('canvas panel geometry rejects controls pushed below the full-height canvas', () => {
  const {assertVisibleGraphPanel} = require('./graph_acceptance_helpers');
  const geometry = panelGeometry();
  geometry.panel.y += geometry.graph.height;
  geometry.control.y += geometry.graph.height;
  assert.throws(() => assertVisibleGraphPanel(geometry), /clipped outside/);
});
test('canvas panel geometry rejects auto-scroll that would conceal the clipping regression', () => {
  const {assertVisibleGraphPanel} = require('./graph_acceptance_helpers');
  const geometry = panelGeometry();
  geometry.scrolls[0].top = 447;
  assert.throws(() => assertVisibleGraphPanel(geometry), /changed ancestor scrollTop/);
  geometry.scrolls[0].beforeTop = 447;
  assert.throws(() => assertVisibleGraphPanel(geometry), /already scrolled/);
});
test('canvas panel geometry checks intervening clipping bounds and scroll evidence', () => {
  const {assertVisibleGraphPanel} = require('./graph_acceptance_helpers');
  const geometry = panelGeometry();
  geometry.clips.push({rect: {...geometry.panel, height: 160}, x: false, y: true});
  assert.throws(() => assertVisibleGraphPanel(geometry), /vertically clipped by an ancestor/);
  geometry.clips = [];
  geometry.scrolls = [];
  assert.throws(() => assertVisibleGraphPanel(geometry), /missing ancestor scroll evidence/);
});
function graphPixels({nodeAt = {x: 80, y: 100}, edgeFrom = nodeAt, ghost = false, edge = true} = {}) {
  const width = 500, height = 350;
  const data = new Array(width * height * 4).fill(255);
  const set = (x, y, rgb) => {
    const offset = (Math.round(y) * width + Math.round(x)) * 4;
    for (let index = 0; index < 3; index++) data[offset + index] = rgb[index];
  };
  const neighbor = {x: 420, y: 100};
  for (let step = 0; edge && step <= 1000; step++) {
    const t = step / 1000;
    set(edgeFrom.x + (neighbor.x - edgeFrom.x) * t, edgeFrom.y + (neighbor.y - edgeFrom.y) * t, [141, 151, 176]);
  }
  for (const center of [nodeAt, neighbor]) {
    for (let y = -14; y <= 14; y++) for (let x = -14; x <= 14; x++) {
      if (x * x + y * y <= 196) set(center.x + x, center.y + y, [92, 115, 230]);
    }
  }
  if (ghost) for (let y = -16; y <= 16; y++) for (let x = -16; x <= 16; x++) {
    if (Math.abs(x) === 16 || Math.abs(y) === 16) set(80 + x, 210 + y, [92, 115, 230]);
  }
  return {width, height, data};
}
test('held-drag evidence requires moved solid nodes and reconnected edge pixels before release', () => {
  const {assertLiveDragPixels, dragTarget} = require('./graph_acceptance_helpers');
  const geometry = {point: {x: 80, y: 100}, target: {x: 80, y: 210}, neighbor: {x: 420, y: 100}};
  const before = graphPixels();
  const real = graphPixels({nodeAt: geometry.target});
  const evidence = assertLiveDragPixels(before, real, geometry);
  assert.equal(evidence.mouseReleased, false);
  assert.ok(evidence.edgeBands.every(band => band.oldEdgeInkRemoved >= 2 && band.newEdgeInkAdded >= 2));
  assert.throws(() => assertLiveDragPixels(before, graphPixels({ghost: true}), geometry), /real filled node/);
  assert.throws(() => assertLiveDragPixels(before,
    graphPixels({nodeAt: geometry.target, edgeFrom: geometry.point}), geometry), /reconnect/);
  assert.deepEqual(dragTarget(geometry.point, geometry.neighbor, {x: 0, y: 0, width: 500, height: 350}, [], before), geometry.target);
  assert.throws(() => dragTarget({x: 1, y: 1}, {x: 2, y: 2}, {x: 0, y: 0, width: 5, height: 5}), NeedsInput);
});

test('held-drag color differences remain signed when screenshot bytes are packed', () => {
  const {assertLiveDragPixels} = require('./graph_acceptance_helpers');
  const geometry = {point: {x: 80, y: 100}, target: {x: 80, y: 210}, neighbor: {x: 420, y: 100}};
  const blackEdges = image => {
    for (let offset = 0; offset < image.data.length; offset += 4) {
      if (image.data[offset] === 141 && image.data[offset + 1] === 151 && image.data[offset + 2] === 176) {
        image.data[offset] = image.data[offset + 1] = image.data[offset + 2] = 0;
      }
    }
    return image;
  };
  const before = blackEdges(graphPixels()), held = blackEdges(graphPixels({nodeAt: geometry.target}));
  const expected = assertLiveDragPixels(before, held, geometry);
  const packed = image => ({...image, data: Buffer.from(image.data)});
  assert.deepEqual(assertLiveDragPixels(packed(before), packed(held), geometry), expected);
});


test('expansion rejects disconnected new branches and missing edge endpoints, and reports only added edges', () => {
  const {assertExpandedGraph} = require('./graph_acceptance_helpers');
  const before = {vertices: [{id: 'clicked'}, {id: 'known'}],
    edges: [{id: 'existing', source: 'clicked', target: 'known'}]};
  const incoming = {vertices: [{id: 'new-a'}, {id: 'new-b'}],
    edges: [{id: 'disconnected', source: 'new-a', target: 'new-b'}]};
  assert.throws(() => assertExpandedGraph(before, incoming, 'clicked'), /connecting the clicked/);
  assert.throws(() => assertExpandedGraph(before,
    {...incoming, edges: [{id: 'dangling', source: 'clicked', target: 'missing'}]}, 'clicked'), /absent/);
  const result = assertExpandedGraph(before, {...incoming,
    edges: [{id: 'existing', source: 'clicked', target: 'known'},
      {id: 'actual-neighbor', source: 'new-a', target: 'clicked'}]}, 'clicked');
  assert.deepEqual(result.addedEdgeIds, ['actual-neighbor']);
  assert.deepEqual(result.neighbors.map(vertex => vertex.id), ['new-a']);
});

test('drag target rejects actual overlay intersections along the path and tries the other direction', () => {
  const {dragTarget} = require('./graph_acceptance_helpers');
  const point = {x: 80, y: 200}, neighbor = {x: 420, y: 200}, bounds = {x: 0, y: 0, width: 500, height: 350};
  const before = graphPixels({nodeAt: point});
  const floating = {x: 60, y: 250, width: 40, height: 1};
  assert.deepEqual(dragTarget(point, neighbor, bounds, [floating], before), {x: 80, y: 90});
  assert.throws(() => dragTarget(point, neighbor, bounds, [floating,
    {x: 50, y: 40, width: 80, height: 100}], before), NeedsInput);
});

test('viewport PNG comparison rejects stale single-node and locally omitted edge images', () => {
  const {assertPngMatchesCanvas} = require('./graph_acceptance_helpers');
  const actual = graphPixels();
  const exported = structuredClone(actual);
  const evidence = assertPngMatchesCanvas(actual, exported);
  assert.equal(evidence.screenToExport.ratio, 1);
  assert.equal(evidence.exportToScreen.ratio, 1);
  // Transparent export background and opaque Chrome background describe the same graph.
  for (let offset = 0; offset < exported.data.length; offset += 4) {
    if (exported.data[offset] === 255 && exported.data[offset + 1] === 255 && exported.data[offset + 2] === 255) {
      exported.data[offset + 3] = 0;
    }
  }
  assertPngMatchesCanvas(actual, exported);
  const stale = graphPixels({edge: false});
  for (let y = 80; y <= 120; y++) for (let x = 400; x <= 440; x++) {
    const offset = (y * stale.width + x) * 4;
    stale.data[offset] = stale.data[offset + 1] = stale.data[offset + 2] = 255;
  }
  assert.throws(() => assertPngMatchesCanvas(actual, stale), /graph pixel positions/);
  const missingSegment = structuredClone(actual);
  for (let x = 220; x <= 235; x++) {
    const offset = (100 * missingSegment.width + x) * 4;
    missingSegment.data[offset] = missingSegment.data[offset + 1] = missingSegment.data[offset + 2] = 255;
  }
  assert.throws(() => assertPngMatchesCanvas(actual, missingSegment), /localized/);
  assert.throws(() => assertPngMatchesCanvas(actual, {...actual, width: actual.width + 2}), /dimensions/);
});

test('PNG matching allows only one-pixel capture rounding and excludes actual overlay rectangles symmetrically', () => {
  const {assertPngMatchesCanvas} = require('./graph_acceptance_helpers');
  const actual = graphPixels();
  const shifted = {width: actual.width, height: actual.height, data: new Array(actual.data.length).fill(255)};
  for (let y = 0; y < actual.height; y++) for (let x = 0; x < actual.width - 1; x++) {
    const from = (y * actual.width + x) * 4, to = (y * actual.width + x + 1) * 4;
    for (let channel = 0; channel < 4; channel++) shifted.data[to + channel] = actual.data[from + channel];
  }
  assertPngMatchesCanvas(actual, shifted);
  const overlay = structuredClone(actual);
  for (let y = 200; y < 210; y++) for (let x = 250; x < 260; x++) {
    const offset = (y * overlay.width + x) * 4;
    overlay.data[offset] = 240; overlay.data[offset + 1] = 120; overlay.data[offset + 2] = 10;
  }
  assert.throws(() => assertPngMatchesCanvas(overlay, actual));
  assertPngMatchesCanvas(overlay, actual, [{x: 250, y: 200, width: 10, height: 10}]);
  assert.throws(() => assertPngMatchesCanvas(overlay, actual,
    [{x: 0, y: 0, width: actual.width, height: actual.height}]), /no unoccluded/);
});


test('PNG matching rejects a hue change at unchanged graph positions', () => {
  const {assertPngMatchesCanvas} = require('./graph_acceptance_helpers');
  const actual = graphPixels(), hueChanged = structuredClone(actual);
  for (let offset = 0; offset < hueChanged.data.length; offset += 4) {
    if (hueChanged.data[offset] === 92 && hueChanged.data[offset + 1] === 115 && hueChanged.data[offset + 2] === 230) {
      hueChanged.data[offset] = 230; hueChanged.data[offset + 2] = 92;
    }
  }
  assert.throws(() => assertPngMatchesCanvas(actual, hueChanged), /graph pixel positions/);
});

test('drag target checks actual before pixels to avoid a same-color third node', () => {
  const {dragTarget} = require('./graph_acceptance_helpers');
  const point = {x: 80, y: 200}, neighbor = {x: 420, y: 200};
  const bounds = {x: 0, y: 0, width: 500, height: 350};
  const before = graphPixels({nodeAt: point});
  const paintNode = center => {
    for (let y = -14; y <= 14; y++) for (let x = -14; x <= 14; x++) {
      if (x * x + y * y > 196) continue;
      const offset = ((center.y + y) * before.width + center.x + x) * 4;
      before.data[offset] = 92; before.data[offset + 1] = 115; before.data[offset + 2] = 230;
    }
  };
  paintNode({x: 80, y: 310});
  assert.deepEqual(dragTarget(point, neighbor, bounds, [], before), {x: 80, y: 90});
  paintNode({x: 80, y: 90});
  assert.deepEqual(dragTarget(point, neighbor, bounds, [], before), {x: 80, y: 290});
  paintNode({x: 80, y: 290});
  paintNode({x: 80, y: 110});
  assert.throws(() => dragTarget(point, neighbor, bounds, [], before), NeedsInput);
});


test('viewport PNG comparison includes neutral gray edges and rejects their omission', () => {
  const {assertPngMatchesCanvas} = require('./graph_acceptance_helpers');
  const actual = graphPixels();
  for (let offset = 0; offset < actual.data.length; offset += 4) {
    if (actual.data[offset] === 141 && actual.data[offset + 1] === 151 && actual.data[offset + 2] === 176) {
      actual.data[offset] = actual.data[offset + 1] = actual.data[offset + 2] = 150;
    }
  }
  assertPngMatchesCanvas(actual, structuredClone(actual));
  const missingEdge = structuredClone(actual);
  for (let offset = 0; offset < missingEdge.data.length; offset += 4) {
    if (missingEdge.data[offset] === 150 && missingEdge.data[offset + 1] === 150 && missingEdge.data[offset + 2] === 150) {
      missingEdge.data[offset] = missingEdge.data[offset + 1] = missingEdge.data[offset + 2] = 255;
    }
  }
  assert.throws(() => assertPngMatchesCanvas(actual, missingEdge), /graph pixel positions/);
});

test('viewport PNG comparison rejects a black background behind otherwise unchanged graph pixels', () => {
  const {assertPngMatchesCanvas} = require('./graph_acceptance_helpers');
  const actual = graphPixels(), darkBackground = structuredClone(actual);
  for (let offset = 0; offset < darkBackground.data.length; offset += 4) {
    if (darkBackground.data[offset] === 255 && darkBackground.data[offset + 1] === 255 && darkBackground.data[offset + 2] === 255) {
      darkBackground.data[offset] = darkBackground.data[offset + 1] = darkBackground.data[offset + 2] = 0;
    }
  }
  assert.throws(() => assertPngMatchesCanvas(actual, darkBackground), /graph pixel positions/);
});

test('actual pale CSS grid colors remain background rather than graph foreground', () => {
  const {assertPngMatchesCanvas} = require('./graph_acceptance_helpers');
  const exported = graphPixels(), withGrid = structuredClone(exported);
  for (let y = 0; y < withGrid.height; y += 16) for (let x = 0; x < withGrid.width; x += 16) {
    const offset = (y * withGrid.width + x) * 4;
    if (withGrid.data[offset] !== 255 || withGrid.data[offset + 1] !== 255 || withGrid.data[offset + 2] !== 255) continue;
    const color = (x + y) % 32 ? [237, 241, 244] : [238, 242, 244];
    for (let channel = 0; channel < 3; channel++) withGrid.data[offset + channel] = color[channel];
  }
  const evidence = assertPngMatchesCanvas(withGrid, exported);
  assert.equal(evidence.screenToExport.ratio, 1);
  assert.equal(evidence.exportToScreen.ratio, 1);
});


test('public fractional screenshot bounds exclude only the enclosing CSS border sample', () => {
  const {screenshotFringeRects, assertPngMatchesCanvas} = require('./graph_acceptance_helpers');
  assert.deepEqual(screenshotFringeRects({x: 332.546875, y: 463, width: 1088.453125, height: 433},
    {width: 1089, height: 433, pixelRatio: 1}), [{x: 0, y: 0, width: 1, height: 433}]);
  const actual = graphPixels(), exported = structuredClone(actual);
  for (let y = 0; y < actual.height; y++) {
    const offset = y * actual.width * 4;
    actual.data[offset] = actual.data[offset + 1] = actual.data[offset + 2] = 217;
  }
  assert.throws(() => assertPngMatchesCanvas(actual, exported));
  const fringe = screenshotFringeRects({x: 10.5, y: 20, width: 499.5, height: 350},
    {width: actual.width, height: actual.height, pixelRatio: 1});
  assertPngMatchesCanvas(actual, exported, fringe);
});

test('screenshot fringe honors actual DPR and cannot exclude integral sample boundaries', () => {
  const {screenshotFringeRects} = require('./graph_acceptance_helpers');
  assert.deepEqual(screenshotFringeRects({x: 10, y: 20, width: 100, height: 50},
    {width: 200, height: 100, pixelRatio: 2}), []);
  assert.deepEqual(screenshotFringeRects({x: 10.25, y: 20.75, width: 100.5, height: 50.5},
    {width: 202, height: 102, pixelRatio: 2}), [
    {x: 0, y: 0, width: 1, height: 102}, {x: 201, y: 0, width: 1, height: 102},
    {x: 0, y: 0, width: 202, height: 1}, {x: 0, y: 101, width: 202, height: 1}
  ]);
  assert.throws(() => screenshotFringeRects({x: 10, y: 20, width: 100, height: 50},
    {width: 220, height: 100, pixelRatio: 2}), {name: 'NeedsInput',
    message: 'PNG size does not match outward rounding of the public region bounds'});
});

test('fractional screenshot masking still rejects integral border ink, missing interior edges and hue changes', () => {
  const {screenshotFringeRects, assertPngMatchesCanvas} = require('./graph_acceptance_helpers');
  const actual = graphPixels();
  const fringe = screenshotFringeRects({x: 10.5, y: 20, width: 499.5, height: 350},
    {width: actual.width, height: actual.height, pixelRatio: 1});
  const changed = structuredClone(actual);
  for (let x = 220; x <= 235; x++) {
    const offset = (100 * changed.width + x) * 4;
    changed.data[offset] = changed.data[offset + 1] = changed.data[offset + 2] = 255;
  }
  assert.throws(() => assertPngMatchesCanvas(actual, changed, fringe), /localized/);
  const hue = structuredClone(actual);
  for (let offset = 0; offset < hue.data.length; offset += 4) {
    if (hue.data[offset] === 92 && hue.data[offset + 1] === 115 && hue.data[offset + 2] === 230) {
      hue.data[offset] = 230; hue.data[offset + 2] = 92;
    }
  }
  assert.throws(() => assertPngMatchesCanvas(actual, hue, fringe), /graph pixel positions/);
  const integerBorder = structuredClone(actual);
  for (let y = 0; y < integerBorder.height; y++) {
    const offset = y * integerBorder.width * 4;
    integerBorder.data[offset] = integerBorder.data[offset + 1] = integerBorder.data[offset + 2] = 217;
  }
  const integral = screenshotFringeRects({x: 10, y: 20, width: 500, height: 350},
    {width: actual.width, height: actual.height, pixelRatio: 1});
  assert.deepEqual(integral, []);
  assert.throws(() => assertPngMatchesCanvas(integerBorder, actual, integral));
});

test('CSS-first enclosure uses the actual PNG size when device-pixel enclosure differs', () => {
  const {screenshotFringeRects} = require('./graph_acceptance_helpers');
  assert.deepEqual(screenshotFringeRects({x: 10.75, y: 20.75, width: 100.5, height: 50.5},
    {width: 204, height: 104, pixelRatio: 2}), [
    {x: 0, y: 0, width: 2, height: 104}, {x: 202, y: 0, width: 2, height: 104},
    {x: 0, y: 0, width: 204, height: 2}, {x: 0, y: 102, width: 204, height: 2}
  ]);
});

function antialiasedCircle(phase) {
  const width = 64, height = 64, data = new Array(width * height * 4).fill(255);
  // Two subpixel phases of one solid circle, rasterized by ordinary 8x8 area
  // sampling. This generates input images, not an anti-alias classification rule.
  for (let y = 16; y < 48; y++) for (let x = 16; x < 48; x++) {
    let covered = 0;
    for (let sy = 0; sy < 8; sy++) for (let sx = 0; sx < 8; sx++) {
      if (Math.hypot(x + (sx + .5) / 8 - 32 - phase, y + (sy + .5) / 8 - 32) < 14) covered++;
    }
    const alpha = covered / 64;
    for (let channel = 0; channel < 3; channel++) {
      data[(y * width + x) * 4 + channel] = Math.round([92, 115, 230][channel] * alpha + 255 * (1 - alpha));
    }
  }
  return {width, height, data};
}
test('standard pixelmatch AA reconciles subpixel circle phases only beside real target foreground', () => {
  const {assertPngMatchesCanvas} = require('./graph_acceptance_helpers');
  const evidence = assertPngMatchesCanvas(antialiasedCircle(0), antialiasedCircle(.35));
  assert.equal(evidence.antiAliasing.library, 'pixelmatch');
  assert.equal(evidence.antiAliasing.version, '8.0.0');
  assert.deepEqual(evidence.antiAliasing.options, {threshold: .1, includeAA: false, alpha: 0,
    aaColor: [0, 255, 0], diffColor: [255, 0, 0], checkerboard: false, diffMask: false});
  assert.ok(evidence.antiAliasing.aaPixels > 0);
  for (const direction of [evidence.screenToExport, evidence.exportToScreen]) {
    assert.ok(direction.rgbUnmatched > 0);
    assert.ok(direction.aaConsidered > 0);
    assert.ok(direction.aaAccepted > 0 && direction.aaAccepted <= direction.aaConsidered);
    assert.ok(direction.ratio >= .98 && direction.worstTile >= .9);
  }
  const unpadded = antialiasedCircle(0);
  const padded = {width: 65, height: 64, data: new Array(65 * 64 * 4).fill(255)};
  for (let y = 0; y < 64; y++) for (let x = 0; x < 64; x++) {
    const from = (y * 64 + x) * 4, to = (y * 65 + x) * 4;
    for (let channel = 0; channel < 4; channel++) padded.data[to + channel] = unpadded.data[from + channel];
  }
  assert.deepEqual(assertPngMatchesCanvas(unpadded, padded).antiAliasing.padded, {width: 65, height: 64});
  const blank = {width: 64, height: 64, data: new Array(64 * 64 * 4).fill(255)};
  assert.throws(() => assertPngMatchesCanvas(antialiasedCircle(0), blank), /graph pixel positions/);
});

test('AA cannot excuse a thin edge hue change or an incorrect solid fill opacity', () => {
  const {assertPngMatchesCanvas} = require('./graph_acceptance_helpers');
  const original = graphPixels(), hue = structuredClone(original);
  for (let offset = 0; offset < original.data.length; offset += 4) {
    if (original.data[offset] === 141 && original.data[offset + 1] === 151 && original.data[offset + 2] === 176) {
      original.data[offset] = 0; original.data[offset + 1] = 0; original.data[offset + 2] = 255;
      hue.data[offset] = 255; hue.data[offset + 1] = 0; hue.data[offset + 2] = 0;
    }
  }
  assert.throws(() => assertPngMatchesCanvas(original, hue), /graph pixel positions/);
  const translucent = graphPixels(), doubled = structuredClone(translucent);
  for (let offset = 0; offset < translucent.data.length; offset += 4) {
    if (translucent.data[offset] === 92 && translucent.data[offset + 1] === 115 && translucent.data[offset + 2] === 230) {
      translucent.data[offset + 3] = 102; // 0.4 opacity
      doubled.data[offset + 3] = 163; // Accidental double drawing produces about 0.64 opacity.
    }
  }
  assert.throws(() => assertPngMatchesCanvas(translucent, doubled), /graph pixel positions/);
});

test('missing installed frontend pixelmatch requests input without installing dependencies', t => {
  const fs = require('node:fs'), os = require('node:os'), path = require('node:path');
  const {spawnSync} = require('node:child_process');
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'hubble-pixelmatch-missing-'));
  t.after(() => fs.rmSync(directory, {recursive: true, force: true}));
  const helper = path.join(directory, 'graph_acceptance_helpers.js');
  fs.copyFileSync(path.join(__dirname, 'graph_acceptance_helpers.js'), helper);
  const result = spawnSync(process.execPath, ['-e', `
    const assert = require('node:assert/strict');
    const {assertPngMatchesCanvas} = require(${JSON.stringify(helper)});
    const image = {width: 64, height: 64, data: new Array(64 * 64 * 4).fill(255)};
    assert.throws(() => assertPngMatchesCanvas(image, image), {name: 'NeedsInput'});
  `], {encoding: 'utf8', env: {...process.env, NODE_PATH: '',
    PLAYWRIGHT_NODE_MODULES: path.join(directory, 'missing-frontend', 'node_modules')}});
  assert.equal(result.status, 0, result.stderr);
});

test('actual Legend DOM rectangle projects to screenshot column 12 without a pseudo-DPR ratio', () => {
  const {screenshotAxes, screenshotOverlayRect} = require('./graph_acceptance_helpers');
  const axes = screenshotAxes({x: 332.546875, y: 463, width: 1088.453125, height: 433},
    {width: 1089, height: 433, pixelRatio: 1});
  assert.equal(axes.horizontal.origin, 332);
  assert.equal(axes.vertical.origin, 463);
  assert.deepEqual(screenshotOverlayRect({x: 344.546875, y: 483.25, width: 396.5, height: 20.25}, axes),
    {x: 12, y: 20, width: 398, height: 21});
});

test('DOM overlay projection uses the proven DPR2 origin for either screenshot rounding model', () => {
  const {screenshotAxes, screenshotOverlayRect} = require('./graph_acceptance_helpers');
  const bounds = {x: 10.75, y: 20.75, width: 100.5, height: 50.5};
  const overlay = {x: 12.125, y: 24.125, width: 3.5, height: 4.5};
  const device = screenshotAxes(bounds, {width: 202, height: 102, pixelRatio: 2});
  assert.equal(device.horizontal.model, 'device-pixel');
  assert.equal(device.horizontal.origin, 21);
  assert.equal(device.vertical.origin, 41);
  assert.deepEqual(screenshotOverlayRect(overlay, device), {x: 3, y: 7, width: 8, height: 10});
  const css = screenshotAxes(bounds, {width: 204, height: 104, pixelRatio: 2});
  assert.equal(css.horizontal.model, 'css-first');
  assert.equal(css.horizontal.origin, 20);
  assert.equal(css.vertical.origin, 40);
  assert.deepEqual(screenshotOverlayRect(overlay, css), {x: 4, y: 8, width: 8, height: 10});
  assert.throws(() => screenshotAxes(bounds, {width: 205, height: 104, pixelRatio: 2}), {name: 'NeedsInput'});
});

test('outward DOM projection masks the Legend first column but preserves interior graph checks', () => {
  const {screenshotAxes, screenshotOverlayRect, assertPngMatchesCanvas} = require('./graph_acceptance_helpers');
  const actual = graphPixels(), exported = structuredClone(actual);
  const axes = screenshotAxes({x: 332.546875, y: 463, width: 499.453125, height: 350},
    {width: 500, height: 350, pixelRatio: 1});
  const mask = screenshotOverlayRect({x: 344.546875, y: 483.25, width: 396.5, height: 20.25}, axes);
  for (let y = 20; y < 41; y++) for (let x = 12; x < 410; x++) {
    const offset = (y * actual.width + x) * 4;
    actual.data[offset] = actual.data[offset + 1] = actual.data[offset + 2] = 217;
  }
  assert.throws(() => assertPngMatchesCanvas(actual, exported));
  assertPngMatchesCanvas(actual, exported, [mask]);
  const missingEdge = structuredClone(exported);
  for (let x = 220; x <= 235; x++) {
    const offset = (100 * missingEdge.width + x) * 4;
    missingEdge.data[offset] = missingEdge.data[offset + 1] = missingEdge.data[offset + 2] = 255;
  }
  assert.throws(() => assertPngMatchesCanvas(actual, missingEdge, [mask]), /localized/);
});


function overallPixels(dpr = 1) {
  const width = 120 * dpr, height = 100 * dpr;
  const image = {width, height, data: new Array(width * height * 4).fill(255)};
  for (let y = 35 * dpr; y < 65 * dpr; y++) for (let x = 35 * dpr; x < 85 * dpr; x++) {
    const i = (y * width + x) * 4;
    image.data[i] = 20; image.data[i + 1] = 80; image.data[i + 2] = 200;
  }
  return image;
}
test('overall PNG checks all alpha pixels, DPR white padding and actual nonempty body', () => {
  const {assertOverallPng} = require('./graph_acceptance_helpers');
  for (const dpr of [1, 2]) {
    const image = overallPixels(dpr), proof = assertOverallPng(image, dpr);
    assert.equal(proof.contract, 'overall');
    assert.equal(proof.requiredWhiteBorderPixels, 30 * dpr);
    const transparent = structuredClone(image); transparent.data[transparent.data.length - 1] = 254;
    assert.throws(() => assertOverallPng(transparent, dpr), /transparent/);
    const blank = structuredClone(image); blank.data.fill(255);
    assert.throws(() => assertOverallPng(blank, dpr), /nonempty/);
    const wrongMargin = structuredClone(image);
    wrongMargin.data[((40 * dpr) * image.width + 15 * dpr) * 4] = 0;
    assert.throws(() => assertOverallPng(wrongMargin, dpr), /border/);
  }
});
test('overall camera proof rejects unperformed pans, remaining graph body and absent downloads', () => {
  const {assertOffscreenPan, assertOverallPanInvariant, assertOverallPng} = require('./graph_acceptance_helpers');
  const before = overallPixels(), offscreen = structuredClone(before); offscreen.data.fill(255);
  const proof = assertOffscreenPan(before, offscreen);
  assert.equal(proof.remainingRatio, 0);
  assert.throws(() => assertOffscreenPan(before, before), /offscreen/);
  assert.throws(() => assertOffscreenPan(offscreen, offscreen), /before input/);
  const contract = assertOverallPng(before, 1);
  const artifact = {file: '/actual/download.png', sha256: 'a'.repeat(64),
    dimensions: {width: before.width, height: before.height}, contract};
  assert.equal(assertOverallPanInvariant(artifact, artifact, proof).identicalDownloadBytes, true);
  assert.throws(() => assertOverallPanInvariant(artifact, artifact), /pan pixel proof/);
  assert.throws(() => assertOverallPanInvariant(artifact, undefined, proof), /actual download/);
  assert.throws(() => assertOverallPanInvariant(artifact, {...artifact, sha256: 'b'.repeat(64)}, proof), /bytes changed/);
  assert.throws(() => assertOverallPanInvariant(artifact, {...artifact, dimensions: {width: 40, height: 40}}, proof), /actual download/);
  assert.throws(() => assertOverallPanInvariant(artifact, artifact,
    {...proof, afterColoredPixels: -1, remainingRatio: -1 / proof.beforeColoredPixels}), /pan pixel proof/);
});
test('overall offscreen proof excludes only public overlays and rejects masking the original body', () => {
  const {assertOffscreenPan} = require('./graph_acceptance_helpers');
  const before = overallPixels(), offscreen = structuredClone(before); offscreen.data.fill(255);
  for (const image of [before, offscreen]) for (let y = 2; y < 10; y++) for (let x = 2; x < 10; x++) {
    const i = (y * image.width + x) * 4;
    image.data[i] = 20; image.data[i + 1] = 150; image.data[i + 2] = 20;
  }
  assert.throws(() => assertOffscreenPan(before, offscreen), /offscreen/);
  const overlay = {x: 2, y: 2, width: 8, height: 8};
  assert.equal(assertOffscreenPan(before, offscreen, [overlay], [overlay]).afterColoredPixels, 0);
  assert.throws(() => assertOffscreenPan(before, offscreen,
    [{x: 0, y: 0, width: before.width, height: before.height}], [overlay]), /before input/);
});


test('drag target retains 110px priority and uses bounded 90px alternatives for the actual narrow viewport', () => {
  const {dragTarget} = require('./graph_acceptance_helpers');
  // Public geometry recorded from the real production query canvas. The positive
  // 110px target intersects edge ink; its negative target misses the 32px margin.
  const point = {x: 844.289758379247, y: 603}, neighbor = {x: 1084.169234963269, y: 585};
  const bounds = {x: 332.546875, y: 463, width: 1088.453125, height: 433};
  const image = {width: 1089, height: 433, data: Buffer.alloc(1089 * 433 * 4, 255)};
  const length = Math.hypot(neighbor.x - point.x, neighbor.y - point.y);
  const target = distance => ({x: point.x - (neighbor.y - point.y) / length * distance,
    y: point.y + (neighbor.x - point.x) / length * distance});
  // The original screenshot contains 42/81 and 26/81 dark samples at the two
  // positive targets. Reproduce those occupancies without depending on local files.
  for (const [distance, ink] of [[110, 42], [90, 26]]) {
    const blocked = target(distance);
    const x = Math.round((blocked.x - bounds.x) * image.width / bounds.width);
    const y = Math.round((blocked.y - bounds.y) * image.height / bounds.height);
    for (let sample = 0; sample < ink; sample++) {
      const i = ((y - 4 + Math.floor(sample / 9)) * image.width + x - 4 + sample % 9) * 4;
      image.data[i] = image.data[i + 1] = image.data[i + 2] = 140;
    }
  }
  const diagnostics = {};
  assert.deepEqual(dragTarget(point, neighbor, bounds, [], image, diagnostics), target(-90));
  assert.deepEqual(diagnostics.distances, [110, 90]);
  assert.deepEqual(diagnostics.candidates.map(candidate => candidate.rejected),
    [['occupied-target'], ['viewport-margin'], ['occupied-target'], []]);
  assert.equal(diagnostics.candidates[0].pixels.inkPixels, 42);
  assert.equal(diagnostics.candidates[2].pixels.inkPixels, 26);
  assert.equal(diagnostics.candidates[3].accepted, true);
  const empty = {...image, data: Buffer.alloc(image.data.length, 255)};
  assert.deepEqual(dragTarget(point, neighbor, bounds, [], empty), target(110));
});
test('bounded drag alternatives fail closed when all paths hit public UI or screenshot ink', () => {
  const {dragTarget} = require('./graph_acceptance_helpers');
  const point = {x: 80, y: 200}, neighbor = {x: 420, y: 200};
  const bounds = {x: 0, y: 0, width: 500, height: 350};
  const image = graphPixels({nodeAt: point});
  const obstacles = [{x: 40, y: 230, width: 80, height: 10}, {x: 40, y: 150, width: 80, height: 10}];
  const diagnostics = {};
  assert.throws(() => dragTarget(point, neighbor, bounds, obstacles, image, diagnostics), NeedsInput);
  assert.equal(diagnostics.candidates.length, 4);
  assert.ok(diagnostics.candidates.every(candidate => candidate.rejected.includes('public-overlay')));
  assert.deepEqual(diagnostics.obstacles, obstacles);
  assert.match(diagnostics.failure, /no unobstructed/);
});

test('live drag chooses the free observed endpoint of the same edge before pinning', () => {
  const {dragTarget, dragEndpoint} = require('./graph_acceptance_helpers');
  // Recorded release-1.7 query geometry. IDs are anonymized; the source is next
  // to the public toolbar, while the opposite endpoint has a clear drag path.
  const point = {x: 963.3873566513762, y: 507, tooltip: 'Node ID: source'};
  const neighbor = {x: 975.3772075688073, y: 669, tooltip: 'Node ID: target'};
  const bounds = {x: 331.921875, y: 463, width: 1089.078125, height: 433};
  const obstacles = [
    {x: 343.921875, y: 471, width: 512.21875, height: 28},
    {x: 330.921875, y: 776, width: 180, height: 120},
    {x: 331.921875, y: 777, width: 180, height: 120},
    {x: 331.921875, y: 777.015625, width: 180, height: 119.9375},
    ...Array.from({length: 10}, (_, index) => ({x: 1071 + 32 * index, y: 472, width: 32, height: 32})),
    {x: 1321.421875, y: 833.015625, width: 80.578125, height: 43.984375},
  ];
  // Recorded source candidate ink counts were 0, 1, 0, 4 /81, all below the
  // existing occupancy threshold. Empty synthetic pixels isolate the geometry.
  const pixels = {width: 1090, height: 433, data: Buffer.alloc(1090 * 433 * 4, 255)};
  const blocked = {};
  assert.throws(() => dragTarget(point, neighbor, bounds, obstacles, pixels, blocked), NeedsInput);
  assert.equal(blocked.candidates.length, 4);
  assert.ok(blocked.candidates.every(candidate => candidate.rejected.includes('public-overlay')));
  const diagnostics = {};
  const selected = dragEndpoint(point, neighbor, bounds, obstacles, pixels, diagnostics);
  assert.equal(selected.point, neighbor);
  assert.equal(selected.neighbor, point);
  assert.deepEqual(selected.target, dragTarget(neighbor, point, bounds, obstacles, pixels));
  assert.deepEqual(diagnostics.attempts[0], blocked);
  assert.equal(diagnostics.attempts.length, 2);
  assert.deepEqual(diagnostics.attempts[1].distances, [110, 90]);
  assert.equal(diagnostics.attempts[1].candidates[0].distance, 110);
  assert.equal(diagnostics.attempts[1].candidates[0].accepted, true);
  assert.equal(diagnostics.selectedEndpoint, 1);

  const bothBlocked = {};
  assert.throws(() => dragEndpoint(point, neighbor, bounds,
    [...obstacles, {x: neighbor.x - 140, y: neighbor.y - 20, width: 280, height: 40}],
    pixels, bothBlocked), NeedsInput);
  assert.equal(bothBlocked.attempts.length, 2);
  assert.ok(bothBlocked.attempts.every(attempt => attempt.candidates.length === 4
    && attempt.candidates.every(candidate => candidate.rejected.includes('public-overlay'))));
  assert.match(bothBlocked.failure, /Neither observed endpoint/);

  const preferred = {};
  assert.equal(dragEndpoint(neighbor, point, bounds, obstacles, pixels, preferred).point, neighbor);
  assert.equal(preferred.attempts.length, 1, 'A valid source must not search another endpoint');
});


async function candidatePixels(image) {
  const vm = require('node:vm');
  const {pixelEvidence} = require('./graph_acceptance_helpers');
  // Execute the real detached-browser analysis against synthetic external RGBA;
  // isolated globals avoid touching a production canvas or another test's realm.
  const page = {evaluate: callback => vm.runInNewContext(`(${callback.toString()})('synthetic-png')`, {
    Image: class {naturalWidth = image.width; naturalHeight = image.height; async decode() {}},
    document: {createElement: () => ({getContext: () => ({drawImage() {}, getImageData: () => ({data: image.data})})})}
  })};
  const evidence = await pixelEvidence(page, Buffer.from('synthetic external screenshot'));
  return JSON.parse(JSON.stringify(evidence));
}
function selectedCirclePixels(stroke) {
  const image = {width: 100, height: 100, data: Buffer.alloc(100 * 100 * 4, 255)};
  for (let y = 0; y < image.height; y++) for (let x = 0; x < image.width; x++) {
    const radius = Math.hypot(x - 50, y - 50);
    if (radius > 23) continue;
    const rgb = radius <= 20 ? [190, 199, 245] : stroke;
    const i = (y * image.width + x) * 4;
    for (let channel = 0; channel < 3; channel++) image.data[i + channel] = rgb[channel];
  }
  return image;
}
test('node candidates prefer uniform fill cores over equally dense blue or selected orange outlines', async () => {
  for (const stroke of [[92, 115, 230], [255, 165, 0]]) {
    const image = selectedCirclePixels(stroke);
    const evidence = await candidatePixels(image);
    assert.ok(evidence.coloredPixels > 30);
    const first = evidence.candidates[0];
    assert.equal(first.density, 121);
    // The original scan-order choice has the same maximum density, but mixes
    // outline pixels into its 11×11 patch. A higher score must beat that tie.
    let originalDensity = 0, originalUniform = 0;
    for (let y = 33; y <= 43; y++) for (let x = 39; x <= 49; x++) {
      const i = (y * image.width + x) * 4, rgb = [...image.data.subarray(i, i + 3)];
      if (Math.max(...rgb) - Math.min(...rgb) > 32 && Math.min(...rgb) < 245) originalDensity++;
      if (Math.hypot(...rgb.map((value, channel) => value - [190, 199, 245][channel])) <= 24) originalUniform++;
    }
    assert.equal(originalDensity, 121);
    assert.ok(originalUniform < first.uniformPixels);
    for (let y = first.y - 5; y <= first.y + 5; y++) for (let x = first.x - 5; x <= first.x + 5; x++) {
      const i = (y * image.width + x) * 4;
      assert.deepEqual([...image.data.subarray(i, i + 3)], [190, 199, 245]);
    }
  }
});
test('candidate centers must themselves satisfy the existing chromatic predicate', async () => {
  const image = selectedCirclePixels([92, 115, 230]);
  // Make every other sampled grid point white while retaining a dense colored neighborhood.
  for (let y = 8; y < 92; y += 12) for (let x = 8; x < 92; x += 12) {
    const i = (y * image.width + x) * 4;
    image.data[i] = image.data[i + 1] = image.data[i + 2] = 255;
  }
  const evidence = await candidatePixels(image);
  for (const candidate of evidence.candidates) {
    const i = (candidate.y * image.width + candidate.x) * 4;
    const channels = [...image.data.subarray(i, i + 3)];
    assert.ok(Math.max(...channels) - Math.min(...channels) > 32 && Math.min(...channels) < 245);
  }
});
