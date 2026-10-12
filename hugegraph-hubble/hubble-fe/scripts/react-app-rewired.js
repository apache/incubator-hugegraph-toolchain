#!/usr/bin/env node

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

const { spawnSync } = require('child_process');

const script = process.argv[2];
const extraArgs = process.argv.slice(3);
const nodeMajorVersion = Number(process.versions.node.split('.')[0]);
const env = { ...process.env };

if (
  nodeMajorVersion >= 17 &&
  !hasNodeOption(env.NODE_OPTIONS, '--openssl-legacy-provider')
) {
  env.NODE_OPTIONS = appendNodeOption(
    env.NODE_OPTIONS,
    '--openssl-legacy-provider'
  );
}

if (script === 'build' && typeof env.CI === 'undefined') {
  env.CI = 'false';
}

const result = spawnSync(
  process.execPath,
  [require.resolve('react-app-rewired/bin/index.js'), script, ...extraArgs],
  {
    env,
    stdio: 'inherit'
  }
);

if (result.error) {
  throw result.error;
}

process.exit(result.status === null ? 1 : result.status);

function appendNodeOption(existingOptions, nextOption) {
  return existingOptions ? `${existingOptions} ${nextOption}` : nextOption;
}

function hasNodeOption(existingOptions, targetOption) {
  return typeof existingOptions === 'string' &&
         existingOptions.split(/\s+/).includes(targetOption);
}
