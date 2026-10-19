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

const assert = require('node:assert/strict');
const test = require('node:test');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const {spawnSync} = require('node:child_process');
const {insecureHttpTarget, runHttpSmoke} = require('./run_ui_http_smoke');

test('exercises loopback services through an insecure HTTP name', () => {
  for (const hostname of ['127.0.0.1', '127.0.0.2', 'localhost', 'hubble.localhost', '[::1]']) {
    const target = insecureHttpTarget(`http://${hostname}:8088/login?from=home`);
    assert.equal(target.url, 'http://hubble-http.test:8088/login?from=home');
    assert.deepEqual(target.args, [
      `--host-resolver-rules=MAP hubble-http.test ${hostname}`,
      '--proxy-bypass-list=hubble-http.test'
    ]);
    assert.equal(target.skipReason, undefined);
  }
});

test('preserves ordinary remote HTTP routing', () => {
  for (const hostname of ['hubble.example.org', '192.168.1.10', 'localhost.example.org']) {
    assert.deepEqual(insecureHttpTarget(`http://${hostname}:8088/hubble/`), {
      url: `http://${hostname}:8088/hubble/`, args: []
    });
  }
});

test('reports HTTPS as untested instead of silently treating it as insecure HTTP', async () => {
  const report = await runHttpSmoke(undefined, 'https://hubble.example.org/', '/unused');
  assert.equal(report.status, 'skipped');
  assert.equal(report.isSecureContext, null);
  assert.equal(report.screenshot, null);
  assert.match(report.reason, /use an HTTP Hubble URL/);
});

test('rejects unsupported target protocols', () => {
  assert.throws(() => insecureHttpTarget('file:///tmp/index.html'), /requires an HTTP or HTTPS/);
});

test('rejects sensitive HTTP and HTTPS targets before browser or report creation', async () => {
  for (const target of ['http://user:private-value@example.org/',
    'https://user:private-value@example.org/', 'http://example.org/?token=private-value',
    'https://example.org/?token=private-value', 'http://example.org/#private-value']) {
    await assert.rejects(runHttpSmoke(undefined, target, '/unused'), error =>
      error.message === 'Invalid hubbleUrl');
  }
});

test('standalone CLI rejects sensitive URLs without persisting or printing their values', t => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'hubble-http-safe-'));
  t.after(() => fs.rmSync(directory, {recursive: true, force: true}));
  const report = path.join(directory, 'report.json');
  for (const target of ['http://user:private-value@example.org/',
    'https://example.org/?token=private-value']) {
    const result = spawnSync(process.execPath, [path.join(__dirname, 'run_ui_http_smoke.js'),
      '--hubble-url', target, '--output-dir', directory, '--json-output', report], {encoding: 'utf8'});
    assert.equal(result.status, 1);
    assert.equal(result.stdout, '');
    assert.equal(result.stderr.trim(), 'Invalid hubbleUrl');
    assert.equal(fs.existsSync(report), false);
  }
});
