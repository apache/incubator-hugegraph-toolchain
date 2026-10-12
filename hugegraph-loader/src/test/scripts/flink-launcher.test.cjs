/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

const test = require('node:test');
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const { mkdtempSync, mkdirSync, copyFileSync, writeFileSync, readFileSync, realpathSync, rmSync } = require('node:fs');
const { tmpdir } = require('node:os');
const { join, resolve } = require('node:path');

test('CDC launcher keeps argument boundaries and does not expose arguments', t => {
  const root = mkdtempSync(join(tmpdir(), 'flink-launcher-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  mkdirSync(join(root, 'loader/bin'), { recursive: true });
  mkdirSync(join(root, 'loader/lib'));
  mkdirSync(join(root, 'flink/bin'), { recursive: true });
  const script = join(root, 'loader/bin/hugegraph-flinkcdc-loader.sh');
  copyFileSync(resolve(__dirname, '../../../assembly/static/bin/hugegraph-flinkcdc-loader.sh'), script);
  const argsFile = join(root, 'args');
  writeFileSync(join(root, 'flink/bin/flink'), '#!/bin/bash\nprintf "%s\\0" "$@" > "$PROBE_ARGS"\n' +
    'exit "${PROBE_EXIT_CODE:-0}"\n', { mode: 0o755 });
  const jar = join(root, 'loader/lib/apache-hugegraph-loader-1.8.0-shaded.jar');
  const run = (args, extra = {}) => spawnSync('bash', [script, ...args], {
    env: { ...process.env, FLINK_HOME: join(root, 'flink'), PROBE_ARGS: argsFile, ...extra }, encoding: 'utf8'
  });
  assert.notEqual(run(['--file', 'mapping.json']).status, 0);
  writeFileSync(jar, 'fixture');
  const result = run(['-p', '2', '--', '--file', 'path with spaces.json', '--token', 'fake-token',
    '--cdc-sink-parallelism', '2']);
  assert.equal(result.status, 0, result.stderr);
  assert.equal(result.stdout, '');
  assert.equal(result.stderr, '');
  assert.deepEqual(readFileSync(argsFile, 'utf8').split('\0').slice(0, -1),
    ['run', '-p', '2', '-c', 'org.apache.hugegraph.loader.flink.HugeGraphFlinkCDCLoader', jar,
      '--file', 'path with spaces.json', '--token', 'fake-token', '--cdc-sink-parallelism', '2']);
  const direct = run(['--file', 'mapping.json', '--graph', 'fixture']);
  assert.equal(direct.status, 0, direct.stderr);
  assert.deepEqual(readFileSync(argsFile, 'utf8').split('\0').slice(0, -1),
    ['run', '-c', 'org.apache.hugegraph.loader.flink.HugeGraphFlinkCDCLoader', jar,
      '--file', 'mapping.json', '--graph', 'fixture']);
  const rejected = run(['--file'], { PROBE_EXIT_CODE: '2' });
  assert.equal(rejected.status, 2, rejected.stderr);
  assert.equal(rejected.stdout, '');
  assert.notEqual(run([], { FLINK_HOME: '' }).status, 0);
  writeFileSync(join(root, 'loader/lib/apache-hugegraph-loader-extra-shaded.jar'), 'fixture');
  assert.notEqual(run([]).status, 0);
});

test('ordinary Loader excludes the shared engine job and keeps Kafka clients', t => {
  const root = realpathSync(mkdtempSync(join(tmpdir(), 'loader-classpath-')));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  mkdirSync(join(root, 'loader/bin'), { recursive: true });
  mkdirSync(join(root, 'loader/lib'));
  mkdirSync(join(root, 'java/bin'), { recursive: true });
  const script = join(root, 'loader/bin/hugegraph-loader.sh');
  copyFileSync(resolve(__dirname, '../../../assembly/static/bin/hugegraph-loader.sh'), script);
  const argsFile = join(root, 'args');
  writeFileSync(join(root, 'java/bin/java'), '#!/bin/bash\n' +
    'if [[ "$1" == "-version" ]]; then echo \'openjdk version "17.0.1"\' >&2; exit 0; fi\n' +
    'printf "%s\\0" "$@" > "$PROBE_ARGS"\n', { mode: 0o755 });
  for (const name of ['apache-hugegraph-loader-1.8.0.jar', 'apache-hugegraph-loader-1.8.0-shaded.jar',
    'kafka-clients-3.2.0.jar', 'zstd-jni-1.5.0-4.jar', 'log4j-slf4j-impl-2.25.5.jar', 'custom-shaded.jar']) {
    writeFileSync(join(root, 'loader/lib', name), 'fixture');
  }
  const result = spawnSync('bash', [script, '--file', 'mapping.json'], {
    env: { ...process.env, JAVA_HOME: join(root, 'java'), PROBE_ARGS: argsFile,
      CLASSPATH: '', JVM_OPTS: '' }, encoding: 'utf8'
  });
  assert.equal(result.status, 0, result.stderr);
  const args = readFileSync(argsFile, 'utf8').split('\0').slice(0, -1);
  const classpath = args[args.indexOf('-cp') + 1].split(':');
  assert(!classpath.includes(join(root, 'loader/lib/apache-hugegraph-loader-1.8.0-shaded.jar')));
  for (const name of ['apache-hugegraph-loader-1.8.0.jar', 'kafka-clients-3.2.0.jar',
    'zstd-jni-1.5.0-4.jar', 'log4j-slf4j-impl-2.25.5.jar', 'custom-shaded.jar']) {
    assert(classpath.includes(join(root, 'loader/lib', name)), name);
  }
});
