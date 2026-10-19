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

const assert = require('node:assert/strict');
const childProcess = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const {NeedsInput, argument, fixtureFromPrefix, redact, publicHttpUrl} = require('./graph_acceptance_helpers');
const {ALL_CHECKS} = require('./run_ui_graph_acceptance');

function options(argv = process.argv, env = process.env) {
  const read = (name, fallback) => argument(argv, name, fallback);
  const hubbleUrl = publicHttpUrl(read('--hubble-url',
    env.HUBBLE_URL || 'http://127.0.0.1:8088'), 'hubbleUrl').replace(/\/$/, '');
  const exampleUrl = publicHttpUrl(read('--example-url', `${hubbleUrl}/visualization.html`), 'exampleUrl');
  const outputDir = path.resolve(read('--output-dir', '.workflow/hubble-v2-issue-694/evidence/ui'));
  const graphChecks = read('--graph-checks', env.HUBBLE_GRAPH_CHECKS || ALL_CHECKS.join(',')).split(',');
  assert.ok(graphChecks.length && graphChecks.every(name => ALL_CHECKS.includes(name)), 'Unknown --graph-checks value');
  return {
    outputDir, graphChecks, hubbleUrl, exampleUrl,
    connId: read('--conn-id', env.HUBBLE_CONN_ID || '1'),
    graphspace: read('--graphspace', env.HUGEGRAPH_GRAPHSPACE || 'DEFAULT'),
    graph: read('--graph', env.HUGEGRAPH_GRAPH || 'hugegraph'),
    fixtureFile: read('--graph-fixture', env.HUBBLE_GRAPH_FIXTURE || ''),
    dataPrefix: read('--data-prefix', env.HUBBLE_GRAPH_DATA_PREFIX || ''),
    liveReport: read('--live-report', ''),
    username: env.HUBBLE_USERNAME || 'admin',
    password: env.HUBBLE_PASSWORD || 'pa',
    chromiumExecutable: read('--chromium-executable',
      env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || env.CHROME_PATH || ''),
    jsonOutput: path.resolve(read('--json-output', path.join(outputDir, 'ui-full-acceptance.json')))
  };
}

function fixtureFromLiveReport(report, config) {
  const needs = message => {throw new NeedsInput(message);};
  if (report.status !== 'passed') needs('Live fixture report must have passed');
  if (report.hubble_url?.replace(/\/$/, '') !== config.hubbleUrl || report.graph !== config.graph) {
    needs('Live fixture report does not match this Hubble URL and graph');
  }
  if (report.graphspace !== config.graphspace) needs('Live fixture report must identify the same GraphSpace');
  const checks = report.checks || [];
  const passed = name => checks.find(check => check.name === name && check.status === 'passed');
  if (passed('hubble-login')?.user !== config.username) needs('Live fixture report belongs to a different account');
  const counts = passed('server-loader-rest-count');
  if (counts?.vertices !== 3 || counts?.edges !== 2) needs('Live report lacks the successful real Loader fixture');
  if (!report.data_prefix) needs('Live report lacks the actual Loader data prefix');
  const fixture = fixtureFromPrefix(report.data_prefix, config.graphspace, config.graph);
  // A generic passed schema smoke may have swallowed an existing-label conflict.
  // Only successful writes for this account and these exact labels prove permission.
  const vertex = passed('hubble-schema-vertexlabel'), edge = passed('hubble-schema-edgelabel');
  if (vertex?.created === true && vertex.label === fixture.schemaLabels[0]
      && edge?.created === true && edge.label === fixture.schemaLabels[1]) fixture.canWriteSchema = true;
  return fixture;
}

function prepareGraphArgs(config) {
  const args = ['--example-url', config.exampleUrl,
    '--graphspace', config.graphspace, '--graph', config.graph,
    '--checks', config.graphChecks.join(',')];
  if (config.fixtureFile) {
    if (!fs.existsSync(path.resolve(config.fixtureFile))) throw new NeedsInput('Configured graph fixture file is missing');
    return [...args, '--fixture-json', path.resolve(config.fixtureFile)];
  }
  if (config.liveReport) {
    if (!fs.existsSync(path.resolve(config.liveReport))) throw new NeedsInput('Live fixture report is missing');
    const fixture = fixtureFromLiveReport(JSON.parse(fs.readFileSync(path.resolve(config.liveReport), 'utf8')), config);
    const fixtureFile = path.join(config.outputDir, 'graph-fixture.json');
    fs.writeFileSync(fixtureFile, JSON.stringify(fixture, null, 2) + '\n');
    return [...args, '--fixture-json', fixtureFile];
  }
  return config.dataPrefix ? [...args, '--data-prefix', config.dataPrefix] : args;
}

function run(name, args, jsonOutput, config, spawn = childProcess.spawnSync) {
  // Remove only this invocation's report so stale evidence cannot make a failed child green.
  fs.rmSync(jsonOutput, {force: true});
  const startedAt = new Date().toISOString();
  const result = spawn(process.execPath, args, {
    cwd: path.resolve(__dirname, '../../../..'), encoding: 'utf8', maxBuffer: 32 * 1024 * 1024,
    env: {...process.env, HUBBLE_USERNAME: config.username, HUBBLE_PASSWORD: config.password}
  });
  let evidence, reportError;
  try { evidence = JSON.parse(fs.readFileSync(jsonOutput, 'utf8')); }
  catch (error) { reportError = error.message; }
  let graphEvidenceValid = true;
  if (name === 'ui-graph-acceptance' && evidence?.status === 'passed') {
    const passed = check => evidence.checks?.some(item => item.name === check && item.status === 'passed');
    const full = ALL_CHECKS.every(passed);
    graphEvidenceValid = config.graphChecks.every(passed) && evidence.fullRequestedMatrix === full;
    if (!graphEvidenceValid) reportError = 'Graph report does not prove every requested check or its declared coverage';
  }
  const status = result.status === 0 && evidence?.status === 'passed' && graphEvidenceValid ? 'passed'
    : result.status === 0 && name === 'ui-insecure-http-smoke' && evidence?.status === 'skipped' ? 'skipped'
    : result.status === 2 && evidence?.status === 'needs input' ? 'needs input' : 'failed';
  return {name, startedAt, status, exitCode: result.status, signal: result.signal || undefined,
    reason: evidence?.reason ? redact(evidence.reason, [config.password]) : undefined,
    report: jsonOutput, fullRequestedMatrix: evidence?.fullRequestedMatrix,
    requestedChecks: evidence?.requestedChecks, remainingMatrix: evidence?.remainingMatrix,
    stdout: redact(result.stdout || '', [config.password]),
    stderr: redact(result.stderr || '', [config.password]),
    error: result.error ? redact(result.error.message, [config.password]) : reportError};
}
function aggregateStatus(checks) {
  return checks.some(check => !['passed', 'needs input'].includes(check.status)
      && !(check.name === 'ui-insecure-http-smoke' && check.status === 'skipped')) ? 'failed'
    : checks.some(check => check.status === 'needs input') ? 'needs input' : 'passed';
}
function exitCode(status) { return status === 'passed' ? 0 : status === 'needs input' ? 2 : 1; }

function acceptanceSummary(status, graphChecksComplete, requestedChecks) {
  // Passing this runner proves its requested smoke only. Even all graph checks
  // do not supply the deployment/auth/PD and runtime evidence listed below.
  const missingGraphChecks = ALL_CHECKS.filter(name => !requestedChecks.includes(name));
  return {
    requestedSmokeStatus: status,
    overallAcceptanceStatus: status === 'passed' ? 'incomplete' : status,
    fullRequestedMatrix: false,
    graphChecksComplete: graphChecksComplete === true,
    remainingAcceptance: [
      ...(missingGraphChecks.length ? [`Graph checks not requested: ${missingGraphChecks.join(', ')}`] : []),
      'Not proved by this runner: both real authenticated and anonymous deployment environments',
      'Not proved by this runner: PD/non-PD capabilities, GraphSpace/account permission and role combinations, '
        + 'account visibility and connection resolver modes',
      'Not proved by this runner: concurrent response ordering and memory/resource release'
    ]
  };
}

function main(configuration = options(), spawn = childProcess.spawnSync) {
  const config = {...configuration,
    hubbleUrl: publicHttpUrl(configuration.hubbleUrl, 'hubbleUrl').replace(/\/$/, ''),
    exampleUrl: publicHttpUrl(configuration.exampleUrl, 'exampleUrl')};
  fs.mkdirSync(config.outputDir, {recursive: true});
  const common = ['--hubble-url', config.hubbleUrl, '--output-dir', config.outputDir,
    '--chromium-executable', config.chromiumExecutable];
  const checks = [];
  for (const [name, script, extra] of [
    ['ui-insecure-http-smoke', 'run_ui_http_smoke.js', []],
    ['ui-browser-smoke', 'run_ui_browser_smoke.js', ['--conn-id', config.connId]],
    ['ui-i18n-switch-smoke', 'run_ui_i18n_switch_smoke.js', []]
  ]) {
    const report = path.join(config.outputDir, `${name}.json`);
    checks.push(run(name, [path.join(__dirname, script),
      ...common, ...extra, '--json-output', report], report, config, spawn));
  }
  const graphReport = path.join(config.outputDir, 'ui-graph-acceptance.json');
  fs.rmSync(graphReport, {force: true});
  try {
    const args = prepareGraphArgs(config);
    checks.push(run('ui-graph-acceptance', [path.join(__dirname, 'run_ui_graph_acceptance.js'),
      ...common, ...args, '--json-output', graphReport], graphReport, config, spawn));
  }
  catch (error) {
    checks.push({name: 'ui-graph-acceptance', status: error instanceof NeedsInput ? 'needs input' : 'failed',
      fullRequestedMatrix: false, error: redact(error.message, [config.password])});
  }
  const status = aggregateStatus(checks);
  const graphChecksComplete = checks.some(check => check.name === 'ui-graph-acceptance'
    && check.status === 'passed' && check.fullRequestedMatrix === true);
  const report = {hubbleUrl: config.hubbleUrl, connId: config.connId, outputDir: config.outputDir,
    graphRequestedChecks: config.graphChecks, checks, status,
    ...acceptanceSummary(status, graphChecksComplete, config.graphChecks),
    coverage: 'Requested HTTP/route/i18n and real graph smoke only; overall acceptance is not proved'};
  fs.mkdirSync(path.dirname(config.jsonOutput), {recursive: true});
  fs.writeFileSync(config.jsonOutput, JSON.stringify(report, null, 2) + '\n');
  return report;
}

if (require.main === module) {
  try {
    const report = main();
    console.log(JSON.stringify(report, null, 2));
    process.exitCode = exitCode(report.status);
  }
  catch (error) {console.error(redact(error.message, [process.env.HUBBLE_PASSWORD])); process.exitCode = 1;}
}
module.exports = {options, fixtureFromLiveReport, prepareGraphArgs, run, aggregateStatus, exitCode,
  acceptanceSummary, main};
