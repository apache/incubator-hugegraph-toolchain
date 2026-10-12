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

const {authenticateUi} = require('./ui_auth');

function response(httpStatus, body) {
  return {
    ok: () => httpStatus >= 200 && httpStatus < 300,
    status: () => httpStatus,
    json: async () => body
  };
}

function fixture(responses) {
  const calls = [];
  const initScripts = [];
  return {
    context: {
      request: {
        post: async (url, options) => {
          calls.push({method: 'POST', url, options});
          return responses.shift();
        },
        get: async url => {
          calls.push({method: 'GET', url});
          return responses.shift();
        }
      }
    },
    page: {
      addInitScript: async (fn, value) => initScripts.push({fn, value})
    },
    calls,
    initScripts
  };
}

test('rejects an HTTP 401 login response', async () => {
  const item = fixture([response(401, {status: 401})]);
  await assert.rejects(
    authenticateUi(item.context, item.page, 'http://hubble', 'admin', 'bad'),
    /login failed: HTTP 401/
  );
});

test('rejects a non-success login business status', async () => {
  const item = fixture([response(200, {status: 401, message: 'denied'})]);
  await assert.rejects(
    authenticateUi(item.context, item.page, 'http://hubble', 'admin', 'bad'),
    /login failed: denied/
  );
});

test('requires an authenticated status with a level', async () => {
  const item = fixture([
    response(200, {status: 200, data: {user_name: 'admin'}}),
    response(200, {status: 200, data: {}})
  ]);
  await assert.rejects(
    authenticateUi(item.context, item.page, 'http://hubble', 'admin', 'pa'),
    /auth status response did not include level/
  );
});

test('uses the server user and shared browser context session', async () => {
  const user = {id: 7, user_name: 'admin', is_superadmin: true};
  const config = {pd_enabled: false, server_url: 'http://127.0.0.1:8080'};
  const item = fixture([
    response(200, {status: 200, data: user}),
    response(200, {status: 200, data: {level: 'ADMIN'}}),
    response(200, {status: 200, data: config})
  ]);
  const result = await authenticateUi(item.context, item.page,
                                      'http://hubble/', 'admin', 'pa');

  assert.deepEqual(result, {user, level: 'ADMIN', pdEnabled: false});
  assert.equal(item.calls[0].url, 'http://hubble/api/v1.3/auth/login');
  assert.deepEqual(item.calls[0].options.data, {
    user_name: 'admin',
    user_password: 'pa'
  });
  assert.equal(item.calls[1].url, 'http://hubble/api/v1.3/auth/status');
  assert.equal(item.calls[2].url, 'http://hubble/api/v1.3/config');
  assert.equal(item.initScripts.length, 1);
  assert.deepEqual(item.initScripts[0].value, {user, config});
});

test('requires runtime config before opening authenticated routes', async () => {
  const item = fixture([
    response(200, {status: 200, data: {id: 7, user_name: 'admin'}}),
    response(200, {status: 200, data: {level: 'ADMIN'}}),
    response(200, {status: 500, message: 'config unavailable'})
  ]);

  await assert.rejects(
    authenticateUi(item.context, item.page, 'http://hubble', 'admin', 'pa'),
    /config failed: config unavailable/
  );
  assert.equal(item.initScripts.length, 0);
});
