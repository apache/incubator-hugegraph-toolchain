/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const helpers = require('./graph_acceptance_helpers');
const {closeUiSession, graphContextMenu} = require('./run_ui_graph_acceptance');

function response(http, data, status = 200) {
  return {ok: () => http >= 200 && http < 300, status: () => http,
    json: async () => ({status, data})};
}
function gateWithBrowser(browser) {
  const original = helpers.loadPlaywright;
  const modulePath = require.resolve('./run_ui_graph_acceptance');
  delete require.cache[modulePath];
  try {
    helpers.loadPlaywright = () => ({chromium: {launch: async () => browser}});
    return require(modulePath).main;
  }
  finally {helpers.loadPlaywright = original; delete require.cache[modulePath];}
}
function fixture(t, {wrongIdentity = false, failedLogin = false, liveAfterLogout = false,
  interruptDuringLoad = false, initializationFailure = false, interruptDuringCleanup = false,
  interruptDuringLogin = false, interruptDuringConfig = false} = {}) {
  const outputDir = fs.mkdtempSync(path.join(os.tmpdir(), 'hubble-session-gate-test-'));
  t.after(() => fs.rmSync(outputDir, {recursive: true, force: true}));
  const calls = [];
  const requestOptions = [];
  let authCookie = false;
  let loggedOut = false;
  let finishLoad;
  const page = {setDefaultTimeout() {}, on() {}, addInitScript: async () => {},
    goto: async () => {
      if (interruptDuringLoad) await new Promise(resolve => {
        finishLoad = resolve;
        queueMicrotask(() => {process.emit('SIGTERM'); process.emit('SIGTERM');});
      });
    },
    close: async () => {calls.push('page.close'); finishLoad?.();},
    screenshot: async () => {throw new Error('No browser in unit test');}};
  const context = {newPage: async () => page, request: {
    post: async (url, options) => {
      calls.push(url);
      requestOptions.push(options);
      if (interruptDuringLogin) await new Promise(resolve => {
        queueMicrotask(() => {
          process.emit('SIGTERM');
          calls.push('signal-during-login');
          setImmediate(() => {authCookie = true; calls.push('late-cookie'); resolve();});
        });
      });
      else authCookie = !failedLogin;
      return failedLogin ? response(401, null, 401) : response(200, {user_name: 'reader'});
    },
    get: async (url, options) => {
      calls.push(url);
      requestOptions.push(options);
      if (url.endsWith('/auth/logout')) {
        if (interruptDuringCleanup) {process.emit('SIGTERM'); process.emit('SIGTERM');}
        if (interruptDuringLogin) assert.equal(authCookie, true, 'Logout preceded late login cookie');
        authCookie = false;
        loggedOut = true;
        return response(200, null);
      }
      if (url.endsWith('/auth/status')) {
        return loggedOut && !authCookie && !liveAfterLogout ? response(200, null, 401) : response(200, {level: 'USER'});
      }
      if (url.endsWith('/auth/context')) {
        return response(200, {username: wrongIdentity ? 'other' : 'reader', role: 'USER'});
      }
      if (url.endsWith('/config')) {
        if (interruptDuringConfig) await new Promise(resolve => {
          queueMicrotask(() => {process.emit('SIGTERM'); setImmediate(resolve);});
        });
        return response(200, {server_capabilities_verified: true, auth_enabled: true, pd_enabled: true});
      }
      throw new Error('Unexpected unit request');
    }
  }};
  const browser = {newContext: async () => {
    if (initializationFailure) throw new Error('Unit initialization failure');
    return context;
  }, version: async () => 'unit-only',
    close: async () => {calls.push('browser.close');}};
  return {main: gateWithBrowser(browser), calls, requestOptions, config: {
    hubbleUrl: 'http://hubble', exampleUrl: 'http://hubble/visualization.html', outputDir,
    jsonOutput: path.join(outputDir, 'report.json'), requested: ['authentication'],
    username: 'reader', password: 'unit-password', fixture: null
  }};
}

test('authenticated gate reports the requested real identity and cleans its jar before browser close', async t => {
  const item = fixture(t);
  const report = await item.main(item.config);
  assert.equal(report.status, 'passed');
  assert.deepEqual(report.authentication, {mode: 'authenticated', pdEnabled: true,
    serverCapabilitiesVerified: true, username: 'reader', role: 'USER'});
  assert.deepEqual(report.sessionCleanup, {status: 'passed'});
  assert.deepEqual(item.calls.slice(-3), [
    'http://hubble/api/v1.3/auth/logout', 'http://hubble/api/v1.3/auth/status', 'browser.close'
  ]);
  assert.ok(!fs.readFileSync(item.config.jsonOutput, 'utf8').includes(item.config.password));
  assert.ok(item.requestOptions.every(options => options.timeout === 5000));
});

test('failed login and post-login identity failure still invalidate the attempted browser session', async t => {
  for (const settings of [{failedLogin: true}, {wrongIdentity: true}]) {
    const item = fixture(t, settings);
    const report = await item.main(item.config);
    assert.equal(report.status, 'failed');
    assert.equal(report.checks[0].status, 'failed');
    assert.deepEqual(report.sessionCleanup, {status: 'passed'});
    assert.deepEqual(item.calls.slice(-3), [
      'http://hubble/api/v1.3/auth/logout', 'http://hubble/api/v1.3/auth/status', 'browser.close'
    ]);
  }
});

test('an otherwise passed gate fails if the actual session remains accepted after logout', async t => {
  const item = fixture(t, {liveAfterLogout: true});
  const report = await item.main(item.config);
  assert.equal(report.checks[0].status, 'passed');
  assert.equal(report.sessionCleanup.status, 'failed');
  assert.equal(report.status, 'failed');
  assert.equal(report.fullRequestedMatrix, false);
  assert.equal(item.calls.at(-1), 'browser.close');
});

test('browser still closes when logout fails, and anonymous/unused contexts do not invent cleanup proof', async () => {
  let closed = 0;
  const browser = {close: async () => {closed++;}};
  const context = {request: {get: async () => {throw new Error('unit-password');}}};
  const failed = {};
  await closeUiSession(context, browser, 'http://hubble', true, failed, ['unit-password']);
  assert.equal(failed.sessionCleanup.status, 'failed');
  assert.ok(!failed.sessionCleanup.error.includes('unit-password'));
  const unused = {};
  await closeUiSession(context, browser, 'http://hubble', false, unused, []);
  assert.equal(closed, 2);
  assert.equal(unused.sessionCleanup, undefined);
});

test('scope menu targets only the graph wrapper and leaves primary navigation outside the assertion', () => {
  const menu = {};
  const page = {getByRole(role, options) {
    assert.equal(role, 'region');
    assert.deepEqual(options, {name: 'Graph canvas', exact: true});
    return {locator(selector) {
      assert.equal(selector, '..');
      return {getByRole(innerRole) {assert.equal(innerRole, 'menu'); return menu;}};
    }};
  }};
  assert.equal(graphContextMenu(page), menu);
});


test('cooperative interruption closes DOM waits, retains the cookie jar for logout, and removes listeners', async t => {
  const signals = ['SIGTERM', 'SIGINT', 'SIGHUP'];
  const before = signals.map(signal => process.listenerCount(signal));
  for (const settings of [{interruptDuringLoad: true}, {interruptDuringCleanup: true}]) {
    const item = fixture(t, settings);
    const report = await item.main(item.config);
    assert.equal(report.status, 'failed');
    assert.equal(report.interrupted, true);
    assert.deepEqual(report.sessionCleanup, {status: 'passed'});
    assert.equal(item.calls.filter(call => call === 'page.close').length, 1);
    assert.equal(item.calls.at(-1), 'browser.close');
    assert.ok(item.calls.includes('http://hubble/api/v1.3/auth/logout'));
    assert.deepEqual(signals.map(signal => process.listenerCount(signal)), before);
  }
});

test('initialization failure still closes the launched browser and removes signal listeners', async t => {
  const signals = ['SIGTERM', 'SIGINT', 'SIGHUP'];
  const before = signals.map(signal => process.listenerCount(signal));
  const item = fixture(t, {initializationFailure: true});
  const report = await item.main(item.config);
  assert.equal(report.status, 'failed');
  assert.equal(report.startupError, 'Unit initialization failure');
  assert.equal(report.sessionCleanup, undefined);
  assert.equal(item.calls.at(-1), 'browser.close');
  assert.deepEqual(signals.map(signal => process.listenerCount(signal)), before);
});


test('signal during login waits for the late cookie before logout and sends no later auth requests', async t => {
  const item = fixture(t, {interruptDuringLogin: true});
  const report = await item.main(item.config);
  assert.equal(report.status, 'failed');
  assert.equal(report.interrupted, true);
  assert.equal(report.sessionCleanup.status, 'passed');
  assert.equal(report.authentication, undefined);
  assert.equal(item.calls.filter(call => call.endsWith('/auth/status')).length, 1,
    'Only the post-logout status verification is allowed');
  assert.ok(!item.calls.some(call => call.endsWith('/auth/context')));
  assert.ok(item.calls.indexOf('late-cookie') > item.calls.indexOf('signal-during-login'));
  assert.ok(item.calls.indexOf('http://hubble/api/v1.3/auth/logout') > item.calls.indexOf('late-cookie'));
  assert.equal(item.calls.at(-1), 'browser.close');
  assert.ok(item.requestOptions.every(options => options.timeout === 5000));
});

test('signal during initial config completion prevents starting a login', async t => {
  const item = fixture(t, {interruptDuringConfig: true});
  const report = await item.main(item.config);
  assert.equal(report.status, 'failed');
  assert.equal(report.interrupted, true);
  assert.equal(report.sessionCleanup, undefined);
  assert.ok(!item.calls.some(call => call.endsWith('/auth/login')));
  assert.deepEqual(item.requestOptions, [{timeout: 5000}]);
  assert.equal(item.calls.at(-1), 'browser.close');
});


test('actual node hit matching uses complete tooltip ID lines, not substring collisions', () => {
  const {nodeTooltip} = require('./run_ui_graph_acceptance');
  assert.equal(nodeTooltip('7', [{id: '7'}])('Node Type: person\nNode ID: 7\nname: value'), true);
  assert.equal(nodeTooltip('7', [{id: '7'}])('Node ID: 70'), false);
  assert.equal(nodeTooltip('7', [{id: '7'}])('Edge ID: 7'), false);
});


test('tooltip identity rejects typed ID display collisions rather than picking an arbitrary node', () => {
  const {nodeTooltip} = require('./run_ui_graph_acceptance');
  assert.throws(() => nodeTooltip(7, [{id: 7}, {id: '7'}]), {name: 'NeedsInput'});
  assert.throws(() => nodeTooltip('7', [{id: 7}]), {name: 'NeedsInput'});
  assert.throws(() => nodeTooltip('missing', [{id: 'known'}]), {name: 'NeedsInput'});
});


test('query input waits for ready scope, checks actual editor text and observes the first matching request', async () => {
  const {executeQuery} = require('./run_ui_graph_acceptance');
  const query = "g.V().hasLabel('person')\n.bothE()";
  const acceptedBody = {content: query, preview: true};
  for (const body of ["g.V('old')", {query}, query,
    {content: "g.V('old')", preview: true}, {content: query},
    {content: query, preview: false}, acceptedBody]) {
    const calls = [];
    let text = 'g.V().limit(10)';
    let readiness = 0;
    const editor = {waitFor: async () => {}, click: async () => {calls.push('editor.click');},
      innerText: async () => {calls.push('editor.text'); return text;},
      fill: async () => {throw new Error('CodeMirror fill is forbidden in this test');}};
    const request = {url: () => 'http://hubble/api/v1.3/graphspaces/S/graphs/g/gremlin-query',
      method: () => 'POST', postDataJSON: () => body,
      response: async () => {calls.push('request.response'); return {
        ok: () => true, status: () => 200,
        json: async () => {throw new Error('unit-stop-after-post-contract');}};}};
    const page = {getByLabel: () => editor,
      keyboard: {press: async key => {calls.push(key);}, insertText: async value => {calls.push(value); text = value;}},
      waitForRequest: async predicate => {
        calls.push('observe.request');
        assert.equal(predicate({...request, method: () => 'GET'}), false);
        assert.equal(predicate({...request, url: () => 'http://hubble/unrelated'}), false);
        assert.equal(predicate(request), true, 'Stale body must be observed and rejected, not filtered out');
        return request;
      },
      waitForResponse: () => {throw new Error('Observe the new Request, not an unrelated in-flight response');},
      waitForTimeout: async () => {calls.push('wait.ready');},
      getByRole: () => ({isEnabled: async () => {calls.push('run.enabled'); return ++readiness > 1;},
        click: async () => {calls.push('run.click');}})};
    await assert.rejects(executeQuery(page, {}, {graphspace: 'S', graph: 'g', query}, 'unit', false),
      body === acceptedBody ? /unit-stop-after-post-contract/ : /exact requested Gremlin query string/);
    assert.deepEqual(calls, ['run.enabled', 'wait.ready', 'run.enabled', 'editor.click', 'ControlOrMeta+A', query,
      'editor.text', 'run.enabled', 'editor.text', 'observe.request', 'run.click', ...(body === acceptedBody ? ['request.response'] : [])]);
  }
});

test('query input rejects text replacement after editing or immediately before clicking Run', async () => {
  const {executeQuery} = require('./run_ui_graph_acceptance');
  for (const texts of [['stale'], ['g.V()', 'stale']]) {
    let clicked = false;
    const editor = {waitFor: async () => {}, click: async () => {}, innerText: async () => texts.shift()};
    const page = {getByLabel: () => editor,
      keyboard: {press: async () => {}, insertText: async () => {}},
      getByRole: () => ({isEnabled: async () => true, click: async () => {clicked = true;}}),
      waitForRequest: () => new Promise(() => {})};
    await assert.rejects(executeQuery(page, {}, {graphspace: 'S', graph: 'g', query: 'g.V()'}, 'unit', false),
      /editor.*exact requested Gremlin query string/);
    assert.equal(clicked, false);
  }
});

test('scope selection clicks the real visible AntD option with literal exact text', async () => {
  const {selectVisibleScopeOption} = require('./run_ui_graph_acceptance');
  const calls = [];
  const label = 'Graph [2].*';
  const combobox = {click: async () => {calls.push('combobox');}};
  const page = {locator: selector => {
    assert.equal(selector, '.ant-select-dropdown:visible .ant-select-item-option');
    return {getByText: (text, options) => {
      assert.equal(text, label);
      assert.deepEqual(options, {exact: true});
      return {click: async options => {
        assert.equal(options, undefined, 'No force click or invisible accessibility proxy'); calls.push('visible.option');
      }};
    }};
  }};
  await selectVisibleScopeOption(page, combobox, label);
  assert.deepEqual(calls, ['combobox', 'visible.option']);
});

function publicOverlayFixture(t) {
  const {createRequire} = require('node:module');
  const requireFrontend = createRequire(path.resolve(__dirname, '../../../hubble-fe/package.json'));
  const {JSDOM} = createRequire(requireFrontend.resolve('jest-environment-jsdom/package.json'))('jsdom');
  const dom = new JSDOM(`<div id="wrapper">
    <div role="region" data-rect="[0,0,500,350]">
      <div id="minimap" class="g6-minimap" data-rect="[0,230,180,120]">
        <canvas id="minimap-canvas" data-rect="[1,231,180,120]"></canvas>
        <div id="minimap-viewport" data-rect="[1,231,180,120]">
          <svg data-rect="[0,0,500,350]"><path id="renderer-shape" data-rect="[0,0,500,350]"></path></svg>
        </div>
      </div>
    </div>
    <div id="toolbar" class="toolBarTop toolBarWithPadding" data-rect="[100,80,300,50]"
      style="background:transparent;padding-right:250px">
      <button data-rect="[100,89,32,32]">Fit</button><select data-rect="[136,89,32,32]"><option>A</option></select>
      <button data-rect="[172,89,32,32]" style="visibility:hidden">Hidden</button>
      <input data-rect="[208,89,32,32]" style="opacity:0">
    </div>
  </div>`);
  t.after(() => dom.window.close());
  for (const element of dom.window.document.querySelectorAll('[data-rect]')) {
    const [x, y, width, height] = JSON.parse(element.dataset.rect);
    const rect = {x, y, width, height, left: x, top: y, right: x + width, bottom: y + height};
    element.getBoundingClientRect = () => rect;
    element.getClientRects = () => [rect];
  }
  return {document: dom.window.document, wrapper: dom.window.document.getElementById('wrapper')};
}

test('minimap masks use actual immediate canvas/viewport overflow without renderer descendants', t => {
  const {collectGraphOverlayRects} = require('./run_ui_graph_acceptance');
  const {MINIMAP_PAINT_SELECTOR, assertPngMatchesCanvas} = require('./graph_acceptance_helpers');
  const {wrapper} = publicOverlayFixture(t);
  assert.deepEqual([...wrapper.querySelectorAll(MINIMAP_PAINT_SELECTOR)].map(element => element.id),
    ['minimap', 'minimap-canvas', 'minimap-viewport']);
  const masks = collectGraphOverlayRects(wrapper);
  assert.ok(masks.some(rect => rect.x === 1 && rect.y === 231 && rect.width === 180 && rect.height === 120));
  assert.ok(!masks.some(rect => rect.width === 500 && rect.height === 350), 'Renderer descendants must not mask the main graph');
  const actual = {width: 500, height: 350, data: new Array(500 * 350 * 4).fill(255)};
  for (let y = 150; y < 170; y++) for (let x = 300; x < 320; x++) {
    const offset = (y * actual.width + x) * 4;
    actual.data[offset] = 92; actual.data[offset + 1] = 115; actual.data[offset + 2] = 230;
  }
  const exported = structuredClone(actual);
  for (let y = 231; y < 350; y++) {
    const offset = (y * actual.width + 180) * 4;
    actual.data[offset] = actual.data[offset + 1] = actual.data[offset + 2] = 221;
  }
  assert.throws(() => assertPngMatchesCanvas(actual, exported, [{x: 0, y: 230, width: 180, height: 120}]));
  assertPngMatchesCanvas(actual, exported, masks);
});

test('transparent toolbar padding remains graph evidence while visible controls alone are excluded', t => {
  const {collectGraphOverlayRects} = require('./run_ui_graph_acceptance');
  const {assertPngMatchesCanvas} = require('./graph_acceptance_helpers');
  const {wrapper} = publicOverlayFixture(t);
  const masks = collectGraphOverlayRects(wrapper);
  assert.ok(!masks.some(rect => rect.x === 100 && rect.y === 80 && rect.width === 300 && rect.height === 50));
  assert.ok(masks.some(rect => rect.x === 100 && rect.y === 89 && rect.width === 32 && rect.height === 32));
  assert.ok(masks.some(rect => rect.x === 136 && rect.y === 89 && rect.width === 32 && rect.height === 32));
  assert.ok(!masks.some(rect => rect.x === 172 || rect.x === 208), 'Invisible controls must not hide graph pixels');
  const actual = {width: 500, height: 350, data: new Array(500 * 350 * 4).fill(255)};
  for (let x = 80; x <= 420; x++) {
    const offset = (100 * actual.width + x) * 4;
    actual.data[offset] = 92; actual.data[offset + 1] = 115; actual.data[offset + 2] = 230;
  }
  const missing = structuredClone(actual);
  for (let x = 220; x <= 235; x++) {
    const offset = (100 * missing.width + x) * 4;
    missing.data[offset] = missing.data[offset + 1] = missing.data[offset + 2] = 255;
  }
  assert.throws(() => assertPngMatchesCanvas(actual, missing, masks), /graph pixel positions|localized/);
});

test('failed PNG comparison preserves current geometry and identifies the actual worst tile', t => {
  const {assertPngWithGeometry} = require('./run_ui_graph_acceptance');
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'hubble-png-geometry-test-'));
  t.after(() => fs.rmSync(directory, {recursive: true, force: true}));
  const actual = {width: 100, height: 64, data: new Array(100 * 64 * 4).fill(255)};
  const paint = (x, y) => {
    const offset = (y * actual.width + x) * 4;
    actual.data[offset] = 92; actual.data[offset + 1] = 115; actual.data[offset + 2] = 230;
  };
  for (let y = 22; y < 42; y++) for (let x = 5; x < 25; x++) {paint(x, y); paint(x + 70, y);}
  for (let x = 25; x < 75; x++) paint(x, 32);
  const missing = structuredClone(actual);
  for (let x = 40; x < 50; x++) {
    const offset = (32 * missing.width + x) * 4;
    missing.data[offset] = missing.data[offset + 1] = missing.data[offset + 2] = 255;
  }
  const geometry = {bounds: {x: 0, y: 0, width: 100, height: 64}, pixelRatio: 1,
    screenshot: {width: 100, height: 64}, overlays: [], fringeRects: []};
  const file = path.join(directory, 'query-expanded-png-geometry.json');
  assert.throws(() => assertPngWithGeometry(actual, missing, geometry, file),
    /localized.*screenToExport tile 1:1 ratio 0\.\d{4} \(\d+\/\d+\)/);
  assert.deepEqual(JSON.parse(fs.readFileSync(file, 'utf8')), geometry);
});


test('overall download requires an actual successful browser download', async () => {
  const {saveDownload} = require('./run_ui_graph_acceptance');
  await assert.rejects(saveDownload(undefined, '/unused'), /Missing actual browser download/);
  await assert.rejects(saveDownload({saveAs() {}, failure: async () => 'canceled'}, '/unused'), /download failed/);
});
test('offscreen camera pan uses a fixed empty public top strip and rejects ink or UI hit paths', () => {
  const {offscreenPanPath} = require('./run_ui_graph_acceptance');
  const {screenshotAxes} = require('./graph_acceptance_helpers');
  const bounds = {x: 20, y: 40, width: 300, height: 200};
  const image = {width: 300, height: 200, data: new Array(300 * 200 * 4).fill(255)};
  const capture = {bounds, image, axes: screenshotAxes(bounds, {...image, pixelRatio: 1}), excluded: []};
  assert.deepEqual(offscreenPanPath(capture), {from: {x: 28, y: 44}, to: {x: 312, y: 44}});
  assert.throws(() => offscreenPanPath({...capture, excluded: [{x: 80, y: 0, width: 30, height: 30}]}), /public UI/);
  const i = (4 * 300 + 100) * 4;
  image.data[i] = image.data[i + 1] = image.data[i + 2] = 0;
  assert.throws(() => offscreenPanPath(capture), /graph ink/);
});


test('drag target geometry evidence survives successful selection and fail-closed rejection', t => {
  const {dragTargetWithEvidence} = require('./run_ui_graph_acceptance');
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'hubble-drag-geometry-test-'));
  t.after(() => fs.rmSync(directory, {recursive: true, force: true}));
  const point = {x: 80, y: 200}, neighbor = {x: 420, y: 200};
  const bounds = {x: 0, y: 0, width: 500, height: 350};
  const pixels = {width: 500, height: 350, data: Buffer.alloc(500 * 350 * 4, 255)};
  const file = path.join(directory, 'geometry.json');
  assert.deepEqual(dragTargetWithEvidence(point, neighbor, bounds, [], pixels, file), {x: 80, y: 310});
  const passed = JSON.parse(fs.readFileSync(file, 'utf8'));
  assert.deepEqual(passed.point, point);
  assert.deepEqual(passed.neighbor, neighbor);
  assert.equal(passed.candidates[0].accepted, true);
  const obstacles = [{x: 40, y: 230, width: 80, height: 10}, {x: 40, y: 150, width: 80, height: 10}];
  assert.throws(() => dragTargetWithEvidence(point, neighbor, bounds, obstacles, pixels, file), helpers.NeedsInput);
  const failed = JSON.parse(fs.readFileSync(file, 'utf8'));
  assert.deepEqual(failed.obstacles, obstacles);
  assert.deepEqual(failed.bounds, bounds);
  assert.equal(failed.candidates.length, 4);
  assert.ok(failed.candidates.every(candidate => candidate.rejected.includes('public-overlay')));
  assert.match(failed.failure, /no unobstructed/);
  assert.throws(() => dragTargetWithEvidence(point, neighbor, bounds, [], undefined, file), helpers.NeedsInput);
  assert.equal(JSON.parse(fs.readFileSync(file, 'utf8')).screenshot, null);
});
