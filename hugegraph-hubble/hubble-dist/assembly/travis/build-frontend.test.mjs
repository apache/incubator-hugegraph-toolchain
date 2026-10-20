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

import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {lstat, mkdtemp, mkdir, readFile, readdir, rm, symlink, writeFile} from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {fileURLToPath} from 'node:url';
import {buildEnvironment, fingerprint, inventory, supportedNodeVersion, validOutput} from './build-frontend.mjs';

const helper = fileURLToPath(new URL('./build-frontend.mjs', import.meta.url));
const pnpmVersion = '12.14.0';

async function fixture(t) {
  const temporary = await mkdtemp(path.join(os.tmpdir(), 'hubble-build-cache-'));
  t.after(() => rm(temporary, {recursive: true, force: true}));
  const frontend = path.join(temporary, 'frontend');
  const bin = path.join(temporary, 'bin');
  await mkdir(path.join(frontend, 'src'), {recursive: true});
  await mkdir(bin);
  const files = {
    '.node-version': process.versions.node,
    'package.json': JSON.stringify({packageManager: `pnpm@${pnpmVersion}`}),
    'pnpm-lock.yaml': 'lockfileVersion: 9.0\n',
    'index.html': '<html></html>',
    'src/main.js': 'export const value = 1;'
  };
  for (const [name, contents] of Object.entries(files)) await writeFile(path.join(frontend, name), contents);
  const calls = path.join(temporary, 'calls');
  const fake = `#!/usr/bin/env node
const fs = require('node:fs');
if (process.argv[2] === '--version') { console.log('${pnpmVersion}'); process.exit(); }
fs.appendFileSync(process.env.HUBBLE_TEST_CALLS, process.argv.slice(2).join(' ') + '\\n');
if (process.env.HUBBLE_TEST_FAIL === process.argv[2]) process.exit(2);
if (process.argv[2] === 'run') {
  fs.mkdirSync('build/assets', {recursive: true});
  fs.writeFileSync('build/index.html', '<html>build</html>');
  fs.writeFileSync('build/assets/app.js', fs.readFileSync('src/main.js'));
  if (process.env.HUBBLE_TEST_EDIT) fs.appendFileSync('src/main.js', '// edit during build');
}
`;
  await writeFile(path.join(bin, 'pnpm'), fake, {mode: 0o755});
  const environment = {...process.env, PATH: `${bin}${path.delimiter}${process.env.PATH}`, HUBBLE_TEST_CALLS: calls};
  return {frontend, calls, environment};
}

function run(f, args = [], environment = {}) {
  return spawnSync(process.execPath, [helper, '--frontend', f.frontend, ...args], {
    env: {...f.environment, ...environment}, encoding: 'utf8'});
}

function success(result) {
  assert.equal(result.status, 0, result.stderr);
}

test('cold build installs frozen inputs once; unchanged complete output needs no install or build', async t => {
  const f = await fixture(t);
  success(run(f));
  const calls = await readFile(f.calls, 'utf8');
  assert.match(calls, /^install --frozen-lockfile --prefer-offline\nrun build\n$/);
  success(run(f));
  assert.equal(await readFile(f.calls, 'utf8'), calls);
  assert.match(run(f).stdout, /verified complete production output/);
  // Cached static output can be packaged without restoring a node_modules tree.
  const manifest = JSON.parse(await readFile(path.join(f.frontend, '.cache/frontend-build.json')));
  assert.equal(await validOutput(f.frontend, manifest.inputHash, 'production'), true);
});

test('changed, missing and extra output files each invalidate the complete inventory', async t => {
  const f = await fixture(t);
  success(run(f));
  for (const mutation of [
    () => writeFile(path.join(f.frontend, 'build/assets/app.js'), 'corrupt'),
    () => rm(path.join(f.frontend, 'build/assets/app.js')),
    () => writeFile(path.join(f.frontend, 'build/stale.js'), 'old release')
  ]) {
    await mutation();
    const result = run(f);
    success(result);
    assert.doesNotMatch(result.stdout, /unchanged/);
  }
  await assert.rejects(readFile(path.join(f.frontend, 'build/stale.js')), {code: 'ENOENT'});
});

test('source, resource, configuration, lock, helper tool versions and mode invalidate inputs', async t => {
  const f = await fixture(t);
  const hash = (mode = 'production', node = process.versions.node, pnpm = pnpmVersion, env = {}) =>
    fingerprint(f.frontend, mode, node, pnpm, env);
  let previous = await hash();
  for (const name of ['src/main.js', 'public.svg', 'vite.config.mjs', '.env.production', 'pnpm-lock.yaml',
                       'package.json', '.node-version', 'pnpm-workspace.yaml']) {
    const before = await readFile(path.join(f.frontend, name), 'utf8').catch(() => '');
    await writeFile(path.join(f.frontend, name), `${before}\nchanged`);
    const next = await hash();
    assert.notEqual(previous, next, name);
    previous = next;
  }
  await writeFile(path.join(f.frontend, 'src/added.js'), 'new file');
  const added = await hash();
  assert.notEqual(added, previous);
  await rm(path.join(f.frontend, 'src/added.js'));
  assert.equal(await hash(), previous);
  assert.notEqual(await hash('diagnostic'), previous);
  assert.notEqual(await hash('production', '99.0.0'), previous);
  assert.notEqual(await hash('production', process.versions.node, '99.0.0'), previous);
  assert.notEqual(await hash('production', process.versions.node, pnpmVersion, {VITE_API_URL: 'changed'}), previous);
  assert.equal(await hash('production', process.versions.node, pnpmVersion, {GITHUB_TOKEN: 'secret'}), previous);
  assert.notEqual(await hash('production', process.versions.node, pnpmVersion,
    {NODE_OPTIONS: '--conditions=custom-build'}), previous);
});

test('dependency patch additions, edits and deletions invalidate cached build inputs', async t => {
  const f = await fixture(t);
  const hash = () => fingerprint(f.frontend, 'production', process.versions.node, pnpmVersion, {});
  const original = await hash();
  const patch = path.join(f.frontend, 'patches/g6.patch');
  await mkdir(path.dirname(patch), {recursive: true});
  await writeFile(patch, 'first dependency fix');
  const added = await hash();
  assert.notEqual(added, original);
  await writeFile(patch, 'corrected dependency fix');
  assert.notEqual(await hash(), added);
  await rm(patch);
  assert.equal(await hash(), original);
});

test('production output can move between build stages; diagnostic maps keep their source root identity', async t => {
  const first = await fixture(t);
  const second = await fixture(t);
  const hash = (directory, mode) => fingerprint(directory, mode, process.versions.node, pnpmVersion, {});
  assert.equal(await hash(first.frontend, 'production'), await hash(second.frontend, 'production'));
  assert.notEqual(await hash(first.frontend, 'diagnostic'), await hash(second.frontend, 'diagnostic'));
});

test('generated root directories do not invalidate inputs; nested source directories still do', async t => {
  const f = await fixture(t);
  const hash = () => fingerprint(f.frontend, 'production', process.versions.node, pnpmVersion, {});
  const before = await hash();
  for (const directory of ['node_modules', 'build', '.cache', 'coverage']) {
    await mkdir(path.join(f.frontend, directory), {recursive: true});
    await writeFile(path.join(f.frontend, directory, 'generated'), 'ignored');
  }
  assert.equal(await hash(), before);
  await mkdir(path.join(f.frontend, 'src/build'));
  await writeFile(path.join(f.frontend, 'src/build/index.js'), 'source');
  assert.notEqual(await hash(), before);
});

test('install/build failure and source edits during build never certify output', async t => {
  const f = await fixture(t);
  success(run(f));
  for (const environment of [{HUBBLE_TEST_FAIL: 'install'}, {HUBBLE_TEST_FAIL: 'run'}, {HUBBLE_TEST_EDIT: 'true'}]) {
    const result = run(f, ['--force'], environment);
    assert.notEqual(result.status, 0);
    await assert.rejects(readFile(path.join(f.frontend, '.cache/frontend-build.json')), {code: 'ENOENT'});
    await assert.rejects(readFile(path.join(f.frontend, '.cache/frontend-build.lock')), {code: 'ENOENT'});
  }
});

test('diagnostic builds have separate identities and invoke the explicit source-map script', async t => {
  const f = await fixture(t);
  success(run(f));
  success(run(f, ['--mode', 'diagnostic']));
  assert.match(await readFile(f.calls, 'utf8'), /run build:diagnostic/);
  const result = run(f);
  success(result);
  assert.doesNotMatch(result.stdout, /unchanged/);
});

test('wrong Node or pnpm version fails before installing; concurrent writers fail explicitly', async t => {
  const f = await fixture(t);
  await writeFile(path.join(f.frontend, '.node-version'), '99.0.0');
  assert.match(run(f).stderr, /requires stable Node >=99.0.0/);
  await writeFile(path.join(f.frontend, '.node-version'), process.versions.node);
  await writeFile(path.join(f.frontend, 'package.json'), JSON.stringify({packageManager: 'pnpm@99.0.0'}));
  assert.match(run(f).stderr, /requires pnpm@99.0.0/);
  await writeFile(path.join(f.frontend, 'package.json'), JSON.stringify({packageManager: `pnpm@${pnpmVersion}`}));
  await mkdir(path.join(f.frontend, '.cache/frontend-build.lock'), {recursive: true});
  assert.match(run(f).stderr, /already locked/);
  await assert.rejects(readFile(f.calls), {code: 'ENOENT'});
});

test('a symlinked cache root never touches the external manifest or lock', async t => {
  for (const existingLock of [false, true]) {
    const f = await fixture(t);
    const external = path.join(path.dirname(f.frontend), 'external-cache');
    const lock = path.join(external, 'frontend-build.lock');
    await mkdir(external);
    await writeFile(path.join(external, 'frontend-build.json'), 'external manifest must remain unchanged\n');
    if (existingLock) {
      await mkdir(lock);
      await writeFile(path.join(lock, 'owner'), 'external build owner\n');
    }
    const files = await inventory(external);
    const entries = await readdir(external);
    await symlink(external, path.join(f.frontend, '.cache'), 'dir');
    const result = run(f, ['--force']);
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /Frontend cache must be a real directory, not a symlink or file/);
    assert.deepEqual(await inventory(external), files);
    assert.deepEqual(await readdir(external), entries);
    if (!existingLock) await assert.rejects(lstat(lock), {code: 'ENOENT'});
    await assert.rejects(readFile(f.calls), {code: 'ENOENT'});
  }
});

test('a regular file at the cache root is rejected without changing its contents', async t => {
  const f = await fixture(t);
  const cache = path.join(f.frontend, '.cache');
  await writeFile(cache, 'not a cache directory\n');
  const result = run(f);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /Frontend cache must be a real directory, not a symlink or file/);
  assert.equal(await readFile(cache, 'utf8'), 'not a cache directory\n');
  await assert.rejects(readFile(f.calls), {code: 'ENOENT'});
});

test('symlinked inputs/outputs are rejected; environment values are never persisted in manifests', async t => {
  const f = await fixture(t);
  await symlink('main.js', path.join(f.frontend, 'src/link.js'));
  await assert.rejects(inventory(f.frontend, true), /symlink/);
  await rm(path.join(f.frontend, 'src/link.js'));
  success(run(f, [], {VITE_TEST_TOKEN: 'do-not-record-this-value', GITHUB_TOKEN: 'nor-this-value'}));
  const manifest = await readFile(path.join(f.frontend, '.cache/frontend-build.json'), 'utf8');
  assert.doesNotMatch(manifest, /do-not-record-this-value|nor-this-value/);
  await symlink('index.html', path.join(f.frontend, 'build/symlink'));
  await assert.rejects(inventory(path.join(f.frontend, 'build')), /symlink/);
  assert.deepEqual(buildEnvironment({GITHUB_TOKEN: 'secret', NODE_ENV: 'test'}),
    {CI: 'true', NODE_ENV: 'production', TZ: 'UTC', NODE_OPTIONS: '--max-old-space-size=4096'});
});

test('Node minimum allows maintained patches and minors but rejects older, next-major and prerelease builds', () => {
  for (const actual of ['24.22.0', '24.22.1', '24.23.0']) {
    assert.equal(supportedNodeVersion(actual, '24.22.0'), true, actual);
  }
  for (const actual of ['24.21.9', '23.99.0', '25.0.0', '24.23.0-rc.1', 'v24.22.0', '24.22', '24.022.0']) {
    assert.equal(supportedNodeVersion(actual, '24.22.0'), false, actual);
  }
  assert.equal(supportedNodeVersion('24.22.0', '24.22.0-rc.1'), false);
});

test('build cache fingerprints the running Node version when the minimum is older', async t => {
  const f = await fixture(t);
  const [major, minor, patch] = process.versions.node.split('.').map(Number);
  assert.ok(minor > 0 || patch > 0, 'fixture requires a maintained patch/minor Node release');
  const minimum = patch > 0 ? `${major}.${minor}.${patch - 1}` : `${major}.${minor - 1}.0`;
  await writeFile(path.join(f.frontend, '.node-version'), minimum);
  success(run(f));
  const manifest = JSON.parse(await readFile(path.join(f.frontend, '.cache/frontend-build.json')));
  assert.equal(manifest.inputHash, await fingerprint(f.frontend, 'production', process.versions.node, pnpmVersion,
    f.environment));
  assert.notEqual(manifest.inputHash, await fingerprint(f.frontend, 'production', minimum, pnpmVersion,
    f.environment));
});
