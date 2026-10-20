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
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const {spawnSync} = require('node:child_process');
const {options, fixtureFromLiveReport, prepareGraphArgs, run, aggregateStatus, main, exitCode} = require('./run_ui_full_acceptance');
const {ALL_CHECKS} = require('./run_ui_graph_acceptance');

function config(directory, args = []) {
  return options(['node', 'wrapper', '--output-dir', directory, ...args],
    {HUBBLE_USERNAME: 'writer', HUBBLE_PASSWORD: 'private-secret'});
}
function liveReport() {
  return {status: 'passed', hubble_url: 'http://127.0.0.1:8088', graph: 'hugegraph',
    graphspace: 'DEFAULT', data_prefix: 'real_123', checks: [
      {name: 'hubble-login', status: 'passed', user: 'writer'},
      {name: 'server-loader-rest-count', status: 'passed', vertices: 3, edges: 2},
      {name: 'hubble-schema-vertexlabel', status: 'passed', label: 'real_123_person', created: true},
      {name: 'hubble-schema-edgelabel', status: 'passed', label: 'real_123_knows', created: true}
    ]};
}
function directory(t) {
  const output = fs.mkdtempSync(path.join(os.tmpdir(), 'hubble-ui-wrapper-test-'));
  t.after(() => fs.rmSync(output, {recursive: true, force: true}));
  return output;
}
function childRunner(status = 'passed', code = 0, observe = () => {}) {
  return (command, args, settings) => {
    observe(command, args, settings);
    const graph = args[0].endsWith('run_ui_graph_acceptance.js');
    const report = args[args.indexOf('--json-output') + 1];
    const requested = graph ? args[args.indexOf('--checks') + 1].split(',') : [];
    fs.writeFileSync(report, JSON.stringify({status: graph ? status : 'passed',
      ...(graph ? {requestedChecks: requested, checks: requested.map(name => ({name, status})),
        fullRequestedMatrix: status === 'passed' && ALL_CHECKS.every(name => requested.includes(name)),
        remainingMatrix: ['Separate real auth-mode matrix']} : {})}));
    return {status: graph ? code : 0, stdout: 'private-secret', stderr: ''};
  };
}

test('default wrapper requires every real graph check, with only explicit subset selection', () => {
  assert.deepEqual(config('/tmp/output').graphChecks, ALL_CHECKS);
  assert.deepEqual(config('/tmp/output', ['--graph-checks', 'example,query']).graphChecks, ['example', 'query']);
  assert.throws(() => config('/tmp/output', ['--graph-checks', '']), /Unknown/);
  assert.throws(() => config('/tmp/output', ['--graph-checks', 'typo']), /Unknown/);
});

test('fixture permission comes from exact successful schema writes, never generic passed/conflict checks', () => {
  const cfg = config('/tmp/output');
  assert.equal(fixtureFromLiveReport(liveReport(), cfg).canWriteSchema, true);
  for (const value of [false, undefined]) {
    const report = liveReport();
    report.checks[2].created = value;
    assert.equal(fixtureFromLiveReport(report, cfg).canWriteSchema, undefined);
  }
  const report = liveReport();
  report.checks[3].label = 'unrelated';
  assert.equal(fixtureFromLiveReport(report, cfg).canWriteSchema, undefined);
});

test('live fixture rejects wrong server, graph, GraphSpace, actor and missing real Loader proof', () => {
  for (const mutate of [
    value => {value.hubble_url = 'http://different:8088';},
    value => {value.graph = 'other';}, value => {delete value.graphspace;},
    value => {value.checks[0].user = 'another';}, value => {value.checks[1].vertices = 0;},
    value => {value.status = 'failed';}
  ]) {
    const report = liveReport(); mutate(report);
    assert.throws(() => fixtureFromLiveReport(report, config('/tmp/output')), {name: 'NeedsInput'});
  }
});

test('wrapper always runs graph gate, carries missing-input exit 2 and never writes credentials', t => {
  const cfg = config(directory(t));
  const calls = [];
  const report = main(cfg, childRunner('needs input', 2, (command, args, settings) => {
    calls.push(args[0]);
    assert.ok(!args.includes(cfg.password));
    assert.equal(settings.env.HUBBLE_PASSWORD, cfg.password);
  }));
  assert.equal(calls.length, 4);
  assert.ok(calls[0].endsWith('run_ui_http_smoke.js'));
  assert.ok(calls[3].endsWith('run_ui_graph_acceptance.js'));
  assert.equal(report.status, 'needs input');
  assert.equal(report.requestedSmokeStatus, 'needs input');
  assert.equal(report.overallAcceptanceStatus, 'needs input');
  assert.equal(exitCode(report.status), 2);
  assert.equal(report.fullRequestedMatrix, false);
  assert.ok(!fs.readFileSync(cfg.jsonOutput, 'utf8').includes(cfg.password));
});

test('explicit standalone subset stays a subset in aggregate even when every requested child passes', t => {
  const cfg = config(directory(t), ['--graph-checks', 'example,authentication,query,schema,cards,algorithm']);
  const liveFile = path.join(cfg.outputDir, 'live.json');
  fs.writeFileSync(liveFile, JSON.stringify(liveReport())); cfg.liveReport = liveFile;
  const report = main(cfg, childRunner());
  assert.equal(report.status, 'passed');
  assert.equal(report.requestedSmokeStatus, 'passed');
  assert.equal(report.overallAcceptanceStatus, 'incomplete');
  assert.equal(report.fullRequestedMatrix, false);
  assert.equal(report.graphChecksComplete, false);
  assert.match(report.remainingAcceptance[0], /Graph checks not requested: scope/);
  assert.ok(report.remainingAcceptance.some(item => /authenticated and anonymous/.test(item)));
  const fixture = JSON.parse(fs.readFileSync(path.join(cfg.outputDir, 'graph-fixture.json'), 'utf8'));
  assert.equal(fixture.canWriteSchema, true);
  assert.deepEqual(fixture.expected.vertexIds, ['real_123_alice', 'real_123_bob', 'real_123_carol']);
  assert.equal(fixture.graphspace, 'DEFAULT');
  assert.deepEqual(report.checks[3].remainingMatrix, ['Separate real auth-mode matrix']);
});

test('all graph checks may pass without proving the overall acceptance matrix', t => {
  const report = main(config(directory(t)), childRunner());
  assert.equal(report.status, 'passed');
  assert.equal(exitCode(report.status), 0);
  assert.equal(report.requestedSmokeStatus, 'passed');
  assert.equal(report.checks[3].fullRequestedMatrix, true);
  assert.equal(report.graphChecksComplete, true);
  assert.equal(report.fullRequestedMatrix, false);
  assert.equal(report.overallAcceptanceStatus, 'incomplete');
  assert.equal(report.remainingAcceptance.length, 3);
  for (const required of [/authenticated and anonymous/, /PD\/non-PD/,
    /GraphSpace\/account permission and role combinations/, /memory\/resource release/]) {
    assert.ok(report.remainingAcceptance.some(item => required.test(item)));
  }
});

test('independent graph completion survives a failed browser or i18n smoke', t => {
  for (const failedScript of ['run_ui_browser_smoke.js', 'run_ui_i18n_switch_smoke.js']) {
    const cfg = config(directory(t));
    const child = childRunner();
    const report = main(cfg, (command, args, settings) => {
      const result = child(command, args, settings);
      if (!args[0].endsWith(failedScript)) return result;
      const output = args[args.indexOf('--json-output') + 1];
      fs.writeFileSync(output, JSON.stringify({status: 'failed'}));
      return {...result, status: 1};
    });
    assert.equal(report.checks[3].status, 'passed');
    assert.equal(report.checks[3].fullRequestedMatrix, true);
    assert.equal(report.graphChecksComplete, true);
    assert.equal(report.status, 'failed');
    assert.equal(report.requestedSmokeStatus, 'failed');
    assert.equal(report.overallAcceptanceStatus, 'failed');
    assert.equal(report.fullRequestedMatrix, false);
    assert.equal(exitCode(report.status), 1);
  }
});

test('failed or inconsistent child cannot pass using a stale report or a nominal zero exit', t => {
  const cfg = config(directory(t)), file = path.join(cfg.outputDir, 'child.json');
  fs.writeFileSync(file, '{"status":"passed"}');
  const missing = run('child', [], file, cfg, () => ({status: 0, stdout: '', stderr: ''}));
  assert.equal(missing.status, 'failed');
  const signaled = run('child', [], file, cfg, () => ({status: null, signal: 'SIGTERM'}));
  assert.equal(signaled.status, 'failed');
  assert.equal(signaled.signal, 'SIGTERM');
  const report = main(cfg, childRunner('failed', 0));
  assert.equal(report.status, 'failed');
  assert.equal(exitCode(report.status), 1);
});

test('unusable live report is recorded as a graph missing-input failure without a green skip', t => {
  const cfg = config(directory(t));
  cfg.liveReport = path.join(cfg.outputDir, 'live.json');
  fs.writeFileSync(cfg.liveReport, JSON.stringify({...liveReport(), graph: 'wrong'}));
  const report = main(cfg, childRunner());
  assert.equal(report.checks.length, 4);
  assert.equal(report.checks[3].name, 'ui-graph-acceptance');
  assert.equal(report.status, 'needs input');
});

// Exercise the actual shell orchestration with fixture commands, never a service/browser.
test('distribution wrapper preserves requested smoke status and incomplete acceptance across cleanup', t => {
  const output = directory(t), evidence = path.join(output, 'evidence');
  fs.mkdirSync(evidence);
  fs.writeFileSync(path.join(evidence, 'manifest.json'), '{"status":"passed"}');
  const tarball = path.join(output, 'candidate.tar.gz'); fs.writeFileSync(tarball, 'test-only');
  const fixtureCommand = path.join(output, 'fixture-command');
  const invocationFile = path.join(output, 'invocations.jsonl');
  fs.writeFileSync(fixtureCommand, `#!/usr/bin/env node
const fs = require('node:fs'), path = require('node:path'), cp = require('node:child_process');
const args = process.argv.slice(2);
if (args.includes('--validate-urls')) {
  const result = cp.spawnSync('python3', args, {stdio: 'inherit'});
  process.exit(result.status);
}
if (args[0] === '-') {
  const result = cp.spawnSync(process.execPath, args, {stdio: 'inherit'});
  process.exit(result.status);
}
fs.appendFileSync(process.env.TEST_INVOCATIONS, JSON.stringify(args) + '\\n');
if (process.env.HUBBLE_PASSWORD !== 'private-secret') process.exit(99);
const file = args[args.indexOf('--json-output') + 1];
const ui = args[0].endsWith('run_ui_full_acceptance.js');
const mode = process.env.TEST_UI_MODE;
const status = mode === 'needs input' ? 'needs input' : mode === 'graph-full-smoke-failed' ? 'failed' : 'passed';
const graphComplete = mode === 'full' || mode === 'graph-full-smoke-failed';
if (process.env.HUBBLE_URL !== 'https://example.invalid/deploy/hubble') process.exit(98);
if (!ui && process.env.HUBBLE_SMOKE_SERVER_URL !== 'https://example.invalid/deploy/server') process.exit(98);
fs.mkdirSync(path.dirname(file), {recursive: true});
fs.writeFileSync(file, JSON.stringify(ui ? {status, fullRequestedMatrix: graphComplete,
  graphChecksComplete: graphComplete, graphRequestedChecks: graphComplete
    ? ${JSON.stringify(ALL_CHECKS)} : ['example', 'scope']}
  : {status: 'passed', hubble_url: process.env.HUBBLE_URL,
  server_url: process.env.HUBBLE_SMOKE_SERVER_URL, graph: 'g', graphspace: 'DEFAULT'}));
process.exit(ui ? status === 'needs input' ? 2 : status === 'failed' ? 1 : 0 : 0);
`, {mode: 0o755});
  for (const mode of ['needs input', 'subset', 'full', 'graph-full-smoke-failed']) {
    const expectedStatus = mode === 'needs input' ? 'needs input'
      : mode === 'graph-full-smoke-failed' ? 'failed' : 'passed';
    const graphComplete = mode === 'full' || mode === 'graph-full-smoke-failed';
    const result = spawnSync('bash', [path.join(__dirname, 'verify-hubble-issue-694.sh'), tarball,
      'https://example.invalid/deploy/server'], {
      encoding: 'utf8', env: {...process.env, PYTHON: fixtureCommand, NODE: fixtureCommand,
        HUBBLE_USERNAME: 'writer', HUBBLE_PASSWORD: 'private-secret',
        TEST_UI_MODE: mode,
        HUBBLE_URL: 'https://example.invalid/deploy/hubble',
        HUBBLE_694_EVIDENCE_DIR: evidence, TEST_INVOCATIONS: invocationFile}
    });
    assert.equal(result.status, exitCode(expectedStatus), result.stderr);
    const manifest = JSON.parse(fs.readFileSync(path.join(evidence, 'manifest.json'), 'utf8'));
    assert.equal(manifest.status, expectedStatus);
    assert.equal(manifest.requestedSmokeStatus, manifest.status);
    assert.equal(manifest.overallAcceptanceStatus, expectedStatus === 'passed' ? 'incomplete' : expectedStatus);
    assert.equal(manifest.fullRequestedMatrix, false);
    assert.equal(manifest.graphChecksComplete, graphComplete);
    assert.ok(manifest.remainingAcceptance.some(item => /PD\/non-PD/.test(item)));
    assert.ok(manifest.remainingAcceptance.some(item =>
      /GraphSpace\/account permission and role combinations/.test(item)));
    assert.equal(manifest.hubbleUrl, 'https://example.invalid/deploy/hubble');
    assert.equal(manifest.serverUrl, 'https://example.invalid/deploy/server');
    assert.deepEqual(manifest.graphRequestedChecks, graphComplete ? ALL_CHECKS : ['example', 'scope']);
    assert.equal(result.stdout.includes('checks passed'), expectedStatus === 'passed');
    assert.ok(!fs.readFileSync(invocationFile, 'utf8').includes('private-secret'));
    assert.doesNotMatch(fs.readFileSync(invocationFile, 'utf8'), /https:\/\/example\.invalid\/deploy\/(?:hubble|server)/);
    assert.ok(!JSON.stringify(manifest).includes('private-secret'));
  }
});

test('graph child cannot claim success with an empty proof or mislabel a subset as full', t => {
  const cfg = config(directory(t), ['--graph-checks', 'example']);
  const report = path.join(cfg.outputDir, 'graph.json');
  for (const body of [
    {status: 'passed', fullRequestedMatrix: false, checks: []},
    {status: 'passed', fullRequestedMatrix: true, checks: [{name: 'example', status: 'passed'}]}
  ]) {
    const result = run('ui-graph-acceptance', [], report, cfg, () => {
      fs.writeFileSync(report, JSON.stringify(body)); return {status: 0};
    });
    assert.equal(result.status, 'failed');
    assert.match(result.error, /does not prove/);
  }
});

test('issue wrapper rejects stale, missing, invalid and failed-process UI success reports', t => {
  for (const mode of ['missing', 'invalid', 'null', 'forged-success', 'invalid-zero-exit']) {
    const output = directory(t), evidence = path.join(output, 'evidence');
    fs.mkdirSync(evidence);
    const oldReport = path.join(evidence, 'ui-full-acceptance.json');
    fs.writeFileSync(oldReport, JSON.stringify({status: 'passed', fullRequestedMatrix: true}));
    const tarball = path.join(output, 'candidate.tar.gz'); fs.writeFileSync(tarball, 'test-only');
    const command = path.join(output, 'fixture-command');
    fs.writeFileSync(command, `#!/usr/bin/env node
const fs = require('node:fs'), path = require('node:path'), cp = require('node:child_process');
const args = process.argv.slice(2);
if (args.includes('--validate-urls')) process.exit(cp.spawnSync('python3', args, {stdio: 'inherit'}).status);
if (args[0] === '-') process.exit(cp.spawnSync(process.execPath, args, {stdio: 'inherit'}).status);
const file = args[args.indexOf('--json-output') + 1];
fs.mkdirSync(path.dirname(file), {recursive: true});
if (args[0].endsWith('run_ui_full_acceptance.js')) {
  if (process.env.TEST_UI_MODE !== 'missing') fs.writeFileSync(file,
    process.env.TEST_UI_MODE === 'null' ? 'null' : process.env.TEST_UI_MODE === 'forged-success'
      ? JSON.stringify({status: 'passed', fullRequestedMatrix: true}) : '{broken');
  process.exit(process.env.TEST_UI_MODE === 'invalid-zero-exit' ? 0 : 1);
}
fs.writeFileSync(file, JSON.stringify({status: 'passed', hubble_url: process.env.HUBBLE_URL,
  server_url: process.env.HUBBLE_SMOKE_SERVER_URL, graph: 'g', graphspace: 'DEFAULT'}));
`, {mode: 0o755});
    const result = spawnSync('bash', [path.join(__dirname, 'verify-hubble-issue-694.sh'), tarball], {
      encoding: 'utf8', env: {...process.env, PYTHON: command, NODE: command, TEST_UI_MODE: mode,
        HUBBLE_694_EVIDENCE_DIR: evidence}
    });
    assert.equal(result.status, 1, `${mode}: ${result.stderr}`);
    const manifest = JSON.parse(fs.readFileSync(path.join(evidence, 'manifest.json'), 'utf8'));
    assert.equal(manifest.status, 'failed', mode);
    assert.equal(manifest.fullRequestedMatrix, false, mode);
    assert.ok(!result.stdout.includes('checks passed'), mode);
    if (mode === 'missing') assert.equal(fs.existsSync(oldReport), false);
  }
});

test('explicit permission/scope fixture stays intact and a prefix never invents write permission', t => {
  const cfg = config(directory(t));
  cfg.fixtureFile = path.join(cfg.outputDir, 'existing-fixture.json');
  const contents = JSON.stringify({canWriteSchema: false, nextScope: {graphspace: 'OTHER', graph: 'g2'}});
  fs.writeFileSync(cfg.fixtureFile, contents);
  cfg.liveReport = 'missing-live-report';
  const args = prepareGraphArgs(cfg);
  assert.equal(args[args.indexOf('--fixture-json') + 1], cfg.fixtureFile);
  assert.equal(fs.readFileSync(cfg.fixtureFile, 'utf8'), contents);
  cfg.fixtureFile = ''; cfg.liveReport = ''; cfg.dataPrefix = 'existing_123';
  assert.ok(prepareGraphArgs(cfg).includes('--data-prefix'));
  assert.equal(fs.existsSync(path.join(cfg.outputDir, 'graph-fixture.json')), false);
  cfg.fixtureFile = 'missing-fixture';
  assert.throws(() => prepareGraphArgs(cfg), {name: 'NeedsInput'});
});

test('wrapper preserves deployment prefix and forwards the independently validated example route', t => {
  const cfg = config(directory(t), ['--hubble-url', 'https://example.invalid/prefix/',
    '--example-url', 'https://example.invalid/prefix/visualization.html', '--graph-checks', 'example']);
  assert.equal(cfg.hubbleUrl, 'https://example.invalid/prefix');
  assert.equal(cfg.exampleUrl, 'https://example.invalid/prefix/visualization.html');
  main(cfg, childRunner('passed', 0, (_command, args) => {
    if (args[0].endsWith('run_ui_graph_acceptance.js')) {
      assert.equal(args[args.indexOf('--example-url') + 1], cfg.exampleUrl);
    }
  }));
});

test('both CLI gates reject encoded URL secrets before spawning a browser or writing URL evidence', t => {
  const output = directory(t), preload = path.join(output, 'no-browser.cjs');
  fs.writeFileSync(preload, `
require(${JSON.stringify(path.join(__dirname, 'graph_acceptance_helpers.js'))}).loadPlaywright = () => {
  throw new Error('Browser launch forbidden in this unit test');
};
require('node:child_process').spawnSync = () => {throw new Error('Child browser forbidden in this unit test');};
`);
  const sentinel = 'URL_INPUT_SECRET_7aQ';
  const encoded = [...sentinel].map(value => '%' + value.charCodeAt(0).toString(16)).join('');
  let index = 0;
  for (const script of ['run_ui_graph_acceptance.js', 'run_ui_full_acceptance.js']) {
    for (const field of ['hubble', 'example']) {
      for (const url of [
        `https://user:${encoded}@example.invalid/prefix`,
        `https://example.invalid/prefix?token=${encoded}`, `https://example.invalid/prefix#${encoded}`
      ]) {
        const report = path.join(output, `${index++}.json`);
        const result = spawnSync(process.execPath, ['--require', preload, path.join(__dirname, script),
          `--${field}-url`, url, '--output-dir', path.join(output, 'browser-output'), '--json-output', report],
          {encoding: 'utf8', env: {...process.env, HUBBLE_PASSWORD: 'different-login-secret'}});
        assert.equal(result.status, 1);
        assert.equal(result.stdout, '');
        assert.match(result.stderr, new RegExp(`Invalid ${field}Url`));
        const evidence = fs.existsSync(report) ? fs.readFileSync(report, 'utf8') : '';
        for (const value of [result.stdout, result.stderr, evidence]) {
          assert.ok(!value.includes(sentinel) && !value.includes(encoded));
          assert.ok(!value.includes('Browser launch forbidden') && !value.includes('Child browser forbidden'));
        }
      }
    }
  }
  for (const field of ['hubbleUrl', 'exampleUrl']) {
    const script = `
const api = require(${JSON.stringify(path.join(__dirname, 'run_ui_graph_acceptance.js'))});
const config = api.options(['node', 'gate', '--output-dir', ${JSON.stringify(path.join(output, 'browser-output'))}], {});
config[${JSON.stringify(field)}] = ${JSON.stringify(`https://user:${encoded}@example.invalid/prefix`)};
api.main(config).catch(error => {console.error(error.message); process.exitCode = 1;});
`;
    const result = spawnSync(process.execPath, ['--require', preload, '-e', script], {encoding: 'utf8'});
    assert.equal(result.status, 1);
    assert.equal(result.stdout, '');
    assert.equal(result.stderr.trim(), `Invalid ${field}`);
    assert.ok(!result.stderr.includes(sentinel) && !result.stderr.includes(encoded));
  }
  assert.equal(fs.existsSync(path.join(output, 'browser-output')), false);
});

test('programmatic wrapper configuration cannot bypass URL validation', t => {
  const cfg = config(directory(t));
  for (const field of ['hubbleUrl', 'exampleUrl']) {
    const invalid = {...cfg, [field]: 'https://user:URL_INPUT_SECRET_7aQ@example.invalid/'};
    assert.throws(() => main(invalid, () => {throw new Error('Unexpected child spawn');}),
      error => error.message === `Invalid ${field}`);
  }
});

function childReport(t, report, code = 0, name = 'ui-insecure-http-smoke') {
  const cfg = config(directory(t));
  const reportPath = path.join(cfg.outputDir, 'report.json');
  return run(name, [
    '-e',
    'require("fs").writeFileSync(process.argv[1], process.argv[2]);' +
      'process.exitCode = Number(process.argv[3]);',
    reportPath, JSON.stringify(report), String(code)
  ], reportPath, cfg);
}

test('retains the HTTPS skip and reason in the full acceptance check', t => {
  const reason = 'HTTPS cannot exercise insecure HTTP; use an HTTP Hubble URL for this gate.';
  const check = childReport(t, {status: 'skipped', reason});
  assert.equal(check.status, 'skipped');
  assert.equal(check.reason, reason);
  const cfg = config(directory(t));
  const graphRunner = childRunner('needs input', 2);
  const wrapper = main(cfg, (command, args, settings) => {
    if (!args[0].endsWith('run_ui_http_smoke.js')) return graphRunner(command, args, settings);
    const report = args[args.indexOf('--json-output') + 1];
    fs.writeFileSync(report, JSON.stringify({status: 'skipped', reason}));
    return {status: 0};
  });
  assert.equal(wrapper.checks[0].status, 'skipped');
  assert.equal(wrapper.checks[0].reason, reason);
  assert.equal(wrapper.status, 'needs input');
  assert.equal(wrapper.fullRequestedMatrix, false);
  assert.equal(aggregateStatus([
    check,
    {name: 'ui-browser-smoke', status: 'passed'},
    {name: 'ui-i18n-switch-smoke', status: 'passed'},
    {name: 'ui-graph-acceptance', status: 'passed'}
  ]), 'passed');
});

test('keeps child failures and malformed reports fail-closed', t => {
  assert.equal(childReport(t, {status: 'failed'}).status, 'failed');
  assert.equal(childReport(t, {status: 'skipped'}, 1).status, 'failed');
  assert.equal(childReport(t, {}).status, 'failed');
});

test('allows skips only for the optional insecure HTTP check', t => {
  assert.equal(aggregateStatus([
    {name: 'ui-insecure-http-smoke', status: 'skipped'},
    {name: 'ui-browser-smoke', status: 'failed'}
  ]), 'failed');
  assert.equal(aggregateStatus([
    {name: 'ui-insecure-http-smoke', status: 'passed'},
    {name: 'ui-i18n-switch-smoke', status: 'skipped'}
  ]), 'failed');
  assert.equal(aggregateStatus([
    {name: 'ui-insecure-http-smoke', status: 'skipped'},
    {name: 'ui-graph-acceptance', status: 'needs input'}
  ]), 'needs input');
  assert.equal(childReport(t, {status: 'skipped'}, 0, 'ui-graph-acceptance').status, 'failed');
  assert.equal(aggregateStatus([{name: 'ui-browser-smoke', status: 'unknown'}]), 'failed');
});

test('existing child checks retain nonzero exit failure and require a current report', t => {
  for (const name of ['ui-browser-smoke', 'ui-i18n-switch-smoke']) {
    assert.equal(childReport(t, {status: 'passed'}, 0, name).status, 'passed');
    assert.equal(childReport(t, {status: 'passed'}, 1, name).status, 'failed');
  }
  const cfg = config(directory(t));
  const missing = run('ui-browser-smoke', ['-e', ''], path.join(cfg.outputDir, 'missing.json'), cfg);
  assert.equal(missing.status, 'failed');
});
