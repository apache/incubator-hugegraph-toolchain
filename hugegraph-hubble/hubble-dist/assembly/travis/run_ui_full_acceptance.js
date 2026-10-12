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

const childProcess = require('child_process');
const fs = require('fs');
const path = require('path');

function argValue(name, fallback) {
  const index = process.argv.indexOf(name);
  if (index >= 0 && index + 1 < process.argv.length) {
    return process.argv[index + 1];
  }
  return fallback;
}

function run(name, command, args) {
  const startedAt = new Date().toISOString();
  const result = childProcess.spawnSync(command, args, {
    cwd: path.resolve(__dirname, '../../../..'),
    encoding: 'utf-8'
  });
  return {
    name,
    command: [command, ...args].join(' '),
    startedAt,
    status: result.status === 0 ? 'passed' : 'failed',
    stdout: result.stdout,
    stderr: result.stderr,
    error: result.error ? result.error.message : undefined
  };
}

function main() {
  const scriptDir = __dirname;
  const outputDir = path.resolve(argValue('--output-dir',
                                          '.workflow/hubble-v2-issue-694/evidence/ui'));
  const hubbleUrl = argValue('--hubble-url', process.env.HUBBLE_URL ||
                             'http://127.0.0.1:8088');
  const connId = argValue('--conn-id', process.env.HUBBLE_CONN_ID || '1');
  const chromiumExecutable = argValue('--chromium-executable',
                                      process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
                                      process.env.CHROME_PATH || '');
  const jsonOutput = argValue('--json-output',
                              path.join(outputDir, 'ui-full-acceptance.json'));
  fs.mkdirSync(outputDir, { recursive: true });

  const checks = [
    run('ui-browser-smoke', process.execPath, [
      path.join(scriptDir, 'run_ui_browser_smoke.js'),
      '--hubble-url', hubbleUrl,
      '--conn-id', connId,
      '--output-dir', outputDir,
      '--json-output', path.join(outputDir, 'ui-browser-smoke.json'),
      '--chromium-executable', chromiumExecutable
    ]),
    run('ui-i18n-switch-smoke', process.execPath, [
      path.join(scriptDir, 'run_ui_i18n_switch_smoke.js'),
      '--hubble-url', hubbleUrl,
      '--output-dir', outputDir,
      '--json-output', path.join(outputDir, 'ui-i18n-switch-smoke.json'),
      '--chromium-executable', chromiumExecutable
    ])
  ];

  const report = {
    hubbleUrl,
    connId,
    outputDir,
    checks,
    status: checks.every((check) => check.status === 'passed') ? 'passed' : 'failed'
  };
  fs.writeFileSync(path.resolve(jsonOutput), JSON.stringify(report, null, 2) + '\n');
  console.log(JSON.stringify(report, null, 2));
  if (report.status !== 'passed') {
    process.exit(1);
  }
}

main();
