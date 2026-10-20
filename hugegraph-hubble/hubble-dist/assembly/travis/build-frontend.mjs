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

import {createHash} from 'node:crypto';
import {spawnSync} from 'node:child_process';
import {lstat, mkdir, readFile, readdir, rename, rm, writeFile} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath, pathToFileURL} from 'node:url';
import {parseArgs} from 'node:util';

const sourceFile = fileURLToPath(import.meta.url);
const defaultFrontend = path.resolve(path.dirname(sourceFile), '../../../hubble-fe');
const generated = new Set(['node_modules', 'node', '.node', '.cache', '.pnpm-store', '.vite',
                           'build', 'dist', 'coverage', 'target', '.git']);
const digest = value => createHash('sha256').update(value).digest('hex');

// Content, names, additions and deletions all matter. Never follow a source or
// output symlink into another workspace (nor accept a partial output inventory).
export async function inventory(directory, excludeGenerated = false) {
  const entries = {};
  async function walk(current, relative = '') {
    const children = await readdir(current, {withFileTypes: true});
    children.sort((a, b) => a.name.localeCompare(b.name, 'en'));
    for (const child of children) {
      if (excludeGenerated && !relative && generated.has(child.name)) continue;
      const name = relative ? `${relative}/${child.name}` : child.name;
      const location = path.join(current, child.name);
      if (child.isSymbolicLink()) throw new Error(`Build input/output must not be a symlink: ${name}`);
      if (child.isDirectory()) await walk(location, name);
      else if (child.isFile()) entries[name] = digest(await readFile(location));
      else throw new Error(`Unsupported build input/output: ${name}`);
    }
  }
  if (!(await lstat(directory)).isDirectory()) throw new Error(`Expected directory: ${directory}`);
  await walk(directory);
  return entries;
}

export function buildEnvironment(environment = process.env) {
  // Hash only environment read by the frontend tools. Do not save values or
  // fingerprint unrelated credentials, GitHub tokens, Maven paths, or SDK data.
  const result = {CI: 'true', NODE_ENV: 'production', TZ: 'UTC',
    NODE_OPTIONS: environment.NODE_OPTIONS || '--max-old-space-size=4096'};
  for (const name of Object.keys(environment).sort()) {
    const toolOption = ['BABEL_ENV', 'BROWSERSLIST_ENV', 'NODE_PATH', 'SOURCE_DATE_EPOCH'].includes(name);
    if (name.startsWith('VITE_') || toolOption) {
      result[name] = environment[name];
    }
  }
  return result;
}

export async function fingerprint(frontend, mode, nodeVersion, pnpmVersion, environment = process.env) {
  const inputs = await inventory(frontend, true);
  for (const required of ['package.json', 'pnpm-lock.yaml', '.node-version', 'index.html']) {
    if (!inputs[required]) throw new Error(`Missing frontend build input: ${required}`);
  }
  return digest(JSON.stringify({schema: 1, inputs, mode, nodeVersion, pnpmVersion,
    platform: process.platform, architecture: process.arch,
    sourceRoot: mode === 'diagnostic' ? path.resolve(frontend) : null,
    environment: buildEnvironment(environment), helper: digest(await readFile(sourceFile))}));
}

export async function validOutput(frontend, inputHash, mode) {
  try {
    const manifest = JSON.parse(await readFile(path.join(frontend, '.cache/frontend-build.json'), 'utf8'));
    if (manifest.schema !== 1 || manifest.inputHash !== inputHash || manifest.mode !== mode) return false;
    const files = await inventory(path.join(frontend, 'build'));
    return Boolean(files['index.html']) && Object.keys(files).length > 1 &&
      JSON.stringify(files) === JSON.stringify(manifest.files);
  } catch (error) {
    if (error.code === 'ENOENT' || error instanceof SyntaxError) return false;
    throw error;
  }
}

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {stdio: 'inherit', ...options});
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error(`${command} ${args.join(' ')} failed (${result.signal || result.status})`);
}

// Only stable releases within the maintained major are supported. Keep the
// minimum in .node-version; later patch/minor releases need no source edit.
export function supportedNodeVersion(actual, minimum) {
  const stable = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/;
  if (!stable.test(actual) || !stable.test(minimum)) return false;
  const [major, minor, patch] = actual.split('.').map(Number);
  const [requiredMajor, requiredMinor, requiredPatch] = minimum.split('.').map(Number);
  return major === requiredMajor &&
    (minor > requiredMinor || (minor === requiredMinor && patch >= requiredPatch));
}

export async function buildFrontend({frontend = defaultFrontend, mode = 'production', force = false} = {}) {
  if (!['production', 'diagnostic'].includes(mode)) throw new Error(`Unknown frontend build mode: ${mode}`);
  const minimumNode = (await readFile(path.join(frontend, '.node-version'), 'utf8')).trim().replace(/^v/, '');
  const nodeVersion = process.versions.node;
  if (!supportedNodeVersion(nodeVersion, minimumNode)) {
    throw new Error(`Hubble requires stable Node >=${minimumNode} in the same major; found ${nodeVersion}. ` +
      'See hubble-fe/.node-version.');
  }
  const {packageManager} = JSON.parse(await readFile(path.join(frontend, 'package.json'), 'utf8'));
  if (!/^pnpm@\d+\.\d+\.\d+$/.test(packageManager)) throw new Error('packageManager must pin a stable pnpm version');
  const pnpmVersion = packageManager.slice(5);
  const pnpm = process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm';
  const version = spawnSync(pnpm, ['--version'], {cwd: frontend, encoding: 'utf8'});
  if (version.error || version.status !== 0 || version.stdout.trim() !== pnpmVersion) {
    throw new Error(`Hubble requires ${packageManager} on PATH; run corepack enable and pnpm --version in hubble-fe.`);
  }
  const cacheDirectory = path.join(frontend, '.cache');
  try {
    await mkdir(cacheDirectory);
  } catch (error) {
    if (error.code !== 'EEXIST') throw error;
  }
  // Reject a pre-existing link before any lock or manifest operation can
  // follow it outside this frontend. The lock coordinates cooperating builds.
  if (!(await lstat(cacheDirectory)).isDirectory()) {
    throw new Error(`Frontend cache must be a real directory, not a symlink or file: ${cacheDirectory}`);
  }
  const lock = path.join(cacheDirectory, 'frontend-build.lock');
  try {
    await mkdir(lock);
  } catch (error) {
    if (error.code === 'EEXIST') {
      throw new Error('Frontend build already locked. If its process stopped, remove .cache/frontend-build.lock.');
    }
    throw error;
  }
  try {
    const inputHash = await fingerprint(frontend, mode, nodeVersion, pnpmVersion);
    if (!force && await validOutput(frontend, inputHash, mode)) {
      console.log(`Frontend unchanged: verified complete ${mode} output; reusing build/.`);
      return {reused: true, inputHash};
    }
    const manifest = path.join(cacheDirectory, 'frontend-build.json');
    // Invalidate before touching build/: a failure cannot certify old output.
    await rm(manifest, {force: true});
    const environment = {...process.env, ...buildEnvironment()};
    run(pnpm, ['install', '--frozen-lockfile', '--prefer-offline'], {cwd: frontend, env: environment});
    await rm(path.join(frontend, 'build'), {recursive: true, force: true});
    run(pnpm, ['run', mode === 'diagnostic' ? 'build:diagnostic' : 'build'], {cwd: frontend, env: environment});
    if (inputHash !== await fingerprint(frontend, mode, nodeVersion, pnpmVersion)) {
      throw new Error('Frontend inputs changed during build; rerun after edits finish. Output was not certified.');
    }
    const files = await inventory(path.join(frontend, 'build'));
    if (!files['index.html'] || Object.keys(files).length < 2) {
      throw new Error('Frontend build produced incomplete output');
    }
    const temporary = `${manifest}.${process.pid}.tmp`;
    await writeFile(temporary, `${JSON.stringify({schema: 1, inputHash, mode, files}, null, 2)}\n`);
    await rename(temporary, manifest);
    console.log(`Frontend ${mode} output verified: ${Object.keys(files).length} files.`);
    return {reused: false, inputHash};
  } finally {
    await rm(lock, {recursive: true, force: true});
  }
}

if (process.argv[1] && pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url) {
  const {values} = parseArgs({options: {frontend: {type: 'string'}, mode: {type: 'string'}, force: {type: 'boolean'}}});
  buildFrontend(values).catch(error => {console.error(error.message); process.exitCode = 1;});
}
