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

const {authenticateUi, logoutUi} = require('./ui_auth');

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
        get: async (url, options) => {
          calls.push({method: 'GET', url, options});
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
    response(200, {status: 200, data: {username: 'admin', role: 'SUPERADMIN'}}),
    response(200, {status: 200, data: config})
  ]);
  const result = await authenticateUi(item.context, item.page,
                                      'http://hubble/', 'admin', 'pa');

  assert.deepEqual(result, {user, level: 'ADMIN', pdEnabled: false, username: 'admin', role: 'SUPERADMIN'});
  assert.equal(item.calls[0].url, 'http://hubble/api/v1.3/auth/login');
  assert.ok(item.calls.every(call => call.options.timeout === 5000));
  assert.deepEqual(item.calls[0].options.data, {
    user_name: 'admin',
    user_password: 'pa'
  });
  assert.equal(item.calls[1].url, 'http://hubble/api/v1.3/auth/status');
  assert.equal(item.calls[2].url, 'http://hubble/api/v1.3/auth/context');
  assert.equal(item.calls[3].url, 'http://hubble/api/v1.3/config');
  assert.equal(item.initScripts.length, 1);
  assert.deepEqual(item.initScripts[0].value, {user, config});
});

test('requires runtime config before opening authenticated routes', async () => {
  const item = fixture([
    response(200, {status: 200, data: {id: 7, user_name: 'admin'}}),
    response(200, {status: 200, data: {level: 'ADMIN'}}),
    response(200, {status: 200, data: {username: 'admin', role: 'SUPERADMIN'}}),
    response(200, {status: 500, message: 'config unavailable'})
  ]);

  await assert.rejects(
    authenticateUi(item.context, item.page, 'http://hubble', 'admin', 'pa'),
    /config failed: config unavailable/
  );
  assert.equal(item.initScripts.length, 0);
});


test('rejects a login user or actual session identity different from the requested actor', async () => {
  for (const wrongLogin of [true, false]) {
    const item = fixture([
      response(200, {status: 200, data: {user_name: wrongLogin ? 'other' : 'admin'}}),
      response(200, {status: 200, data: {level: 'ADMIN'}}),
      response(200, {status: 200, data: {username: 'other', role: 'SUPERADMIN'}})
    ]);
    await assert.rejects(authenticateUi(item.context, item.page, 'http://hubble', 'admin', 'pa'),
      /did not match requested username/);
    assert.equal(item.initScripts.length, 0);
  }
});

test('records a real reader role and rejects absent or unsupported roles', async () => {
  for (const role of ['USER', undefined, 'ANONYMOUS', 'unexpected']) {
    const item = fixture([
      response(200, {status: 200, data: {user_name: 'reader'}}),
      response(200, {status: 200, data: {level: 'USER'}}),
      response(200, {status: 200, data: {username: 'reader', role}}),
      response(200, {status: 200, data: {pd_enabled: true}})
    ]);
    const run = authenticateUi(item.context, item.page, 'http://hubble', 'reader', 'pa');
    if (role === 'USER') {
      const result = await run;
      assert.equal(result.username, 'reader');
      assert.equal(result.role, 'USER');
    }
    else await assert.rejects(run, /supported authenticated role/);
  }
});

test('logout proves same cookie jar becomes unauthenticated, with either actual 401 contract', async () => {
  for (const denied of [response(401, null), response(200, {status: 401})]) {
    const item = fixture([response(200, {status: 200}), denied]);
    await logoutUi(item.context, 'http://hubble/');
    assert.deepEqual(item.calls.map(call => call.url), [
      'http://hubble/api/v1.3/auth/logout', 'http://hubble/api/v1.3/auth/status'
    ]);
    assert.deepEqual(item.calls.map(call => call.method), ['GET', 'GET']);
    assert.deepEqual(item.calls.map(call => call.options), [{timeout: 5000}, {timeout: 5000}]);
  }
});

test('logout fails if its endpoint fails or the session remains accepted', async () => {
  for (const replies of [
    [response(503, {})],
    [response(200, {status: 500})],
    [response(200, {status: 200}), response(200, {status: 200, data: {level: 'ADMIN'}})],
    [response(200, {status: 200}), response(403, {status: 403})],
    [response(200, {status: 200}), response(200, {status: '401'})],
    [response(200, {status: 200}), {status: () => 200, json: async () => {throw new Error('invalid');}}]
  ]) {
    await assert.rejects(logoutUi(fixture(replies).context, 'http://hubble'), /logout|Logout/);
  }
});


test('interruption at each completed authentication request prevents the next request or init script', async () => {
  for (const stopAt of [1, 2, 3, 4]) {
    const item = fixture([
      response(200, {status: 200, data: {user_name: 'reader'}}),
      response(200, {status: 200, data: {level: 'USER'}}),
      response(200, {status: 200, data: {username: 'reader', role: 'USER'}}),
      response(200, {status: 200, data: {pd_enabled: true}})
    ]);
    let completed = 0;
    let interrupted = false;
    for (const method of ['post', 'get']) {
      const request = item.context.request[method];
      item.context.request[method] = async (...args) => {
        const result = await request(...args);
        if (++completed === stopAt) interrupted = true;
        return result;
      };
    }
    const assertRunning = () => {if (interrupted) throw new Error('unit interruption');};
    await assert.rejects(authenticateUi(item.context, item.page,
      'http://hubble', 'reader', 'pa', assertRunning), /unit interruption/);
    assert.equal(item.calls.length, stopAt);
    assert.ok(item.calls.every(call => call.options.timeout === 5000));
    assert.equal(item.initScripts.length, 0);
  }
});
