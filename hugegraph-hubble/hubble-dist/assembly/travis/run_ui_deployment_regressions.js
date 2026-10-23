#!/usr/bin/env node
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

const fs = require('fs');
const os = require('os');
const path = require('path');
const {spawnSync} = require('child_process');

const output = process.env.HUBBLE_UI_EVIDENCE_DIR ||
    fs.mkdtempSync(path.join(os.tmpdir(), 'hubble-deployment-ui-'));
fs.mkdirSync(output, {recursive: true});

const reportPath = path.join(output, 'browser-report.json');
// A reused evidence directory must never make this invocation look successful.
fs.rmSync(reportPath, {force: true});
const screenshotNames = new Set([
    'graphspace-authenticated', 'graphspace-edit', 'graph-context-switch',
    'query-routine-limit', 'query-truncated', 'query-preview-details',
    'async-task-submitted', 'async-task-result-limit', 'pd-unmapped-diagnostics',
    'graphspace-anonymous', 'failure',
].map(name => `${name}.png`));
for (const name of fs.readdirSync(output)) {
    if (screenshotNames.has(name) || /^failure-context-\d+-page-\d+\.png$/.test(name)) {
        fs.rmSync(path.join(output, name), {force: true});
    }
}

const implementation = path.join(__dirname, 'run_ui_deployment_regressions_impl.js');
const result = spawnSync(process.execPath, [implementation, ...process.argv.slice(2)], {
    cwd: process.cwd(),
    env: {...process.env, HUBBLE_UI_EVIDENCE_DIR: output},
    stdio: 'inherit',
});
const missingReport = !fs.existsSync(reportPath);

if (missingReport) {
    const reason = result.error?.message || (result.signal
        ? `Runner terminated by signal ${result.signal}`
        : `Runner exited before reporting with status ${result.status ?? 'unknown'}`);
    const report = {
        status: 'failed',
        observedAt: new Date().toISOString(),
        browserVersion: null,
        timezone: 'Asia/Taipei',
        scope: 'Deployment regression runner failed before producing its browser report',
        checks: [],
        unexpectedApi: [],
        pageErrors: [],
        failure: {stage: 'runner_startup', message: reason},
    };
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2) + '\n');
    console.error(`Deployment regression report written to ${reportPath}: ${reason}`);
}

if (result.error || result.signal) {
    process.exitCode = 1;
}
else {
    process.exitCode = missingReport && result.status === 0 ? 1 : (result.status ?? 1);
}
