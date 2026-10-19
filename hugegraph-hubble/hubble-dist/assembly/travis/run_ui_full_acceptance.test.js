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
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const test = require('node:test');
const {acceptanceStatus, run} = require('./run_ui_full_acceptance');

function childReport(t, report, exitCode = 0) {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'hubble-ui-report-'));
  t.after(() => fs.rmSync(directory, {recursive: true, force: true}));
  const reportPath = path.join(directory, 'report.json');
  return run('ui-insecure-http-smoke', process.execPath, [
    '-e',
    'require("fs").writeFileSync(process.argv[1], process.argv[2]);' +
      'process.exitCode = Number(process.argv[3]);',
    reportPath, JSON.stringify(report), String(exitCode)
  ], reportPath);
}

test('retains the HTTPS skip and reason in the full acceptance check', t => {
  const reason = 'HTTPS cannot exercise insecure HTTP; use an HTTP Hubble URL for this gate.';
  const check = childReport(t, {status: 'skipped', reason});
  assert.equal(check.status, 'skipped');
  assert.equal(check.reason, reason);
  assert.equal(acceptanceStatus([
    check,
    {name: 'ui-browser-smoke', status: 'passed'},
    {name: 'ui-i18n-switch-smoke', status: 'passed'}
  ]), 'passed');
});

test('keeps child failures and malformed reports fail-closed', t => {
  assert.equal(childReport(t, {status: 'failed'}).status, 'failed');
  assert.equal(childReport(t, {status: 'skipped'}, 1).status, 'failed');
  assert.equal(childReport(t, {}).status, 'failed');
});

test('allows skips only for the optional insecure HTTP check', () => {
  assert.equal(acceptanceStatus([
    {name: 'ui-insecure-http-smoke', status: 'skipped'},
    {name: 'ui-browser-smoke', status: 'failed'}
  ]), 'failed');
  assert.equal(acceptanceStatus([
    {name: 'ui-insecure-http-smoke', status: 'passed'},
    {name: 'ui-i18n-switch-smoke', status: 'skipped'}
  ]), 'failed');
  assert.equal(acceptanceStatus([
    {name: 'ui-insecure-http-smoke', status: 'passed'},
    {name: 'ui-browser-smoke', status: 'passed'},
    {name: 'ui-i18n-switch-smoke', status: 'passed'}
  ]), 'passed');
});

test('preserves exit-status behavior for existing checks without a child report', () => {
  assert.equal(run('ui-browser-smoke', process.execPath, ['-e', '']).status, 'passed');
  assert.equal(run('ui-i18n-switch-smoke', process.execPath,
    ['-e', 'process.exitCode = 1']).status, 'failed');
});
