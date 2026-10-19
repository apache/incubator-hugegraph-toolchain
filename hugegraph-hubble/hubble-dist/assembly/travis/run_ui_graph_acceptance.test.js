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
