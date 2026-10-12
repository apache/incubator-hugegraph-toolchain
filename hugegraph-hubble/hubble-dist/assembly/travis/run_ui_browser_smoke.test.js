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
const test = require('node:test');

const {
  apiEntryPassed,
  matchesRequiredApi,
  waitForRequiredResponse
} = require('./run_ui_browser_smoke');

function response(url, options = {}) {
  const method = options.method || 'GET';
  const httpStatus = options.httpStatus || 200;
  const body = options.body === undefined ? {status: 200} : options.body;
  return {
    request: () => ({method: () => method}),
    url: () => url,
    status: () => httpStatus,
    ok: () => httpStatus >= 200 && httpStatus < 300,
    json: async () => {
      if (options.jsonError) {
        throw new Error(options.jsonError);
      }
      return body;
    }
  };
}

test('matches the exact page-level required GET request', () => {
  const requiredRequest = {
    path: '/api/v1.3/graphspaces/DEFAULT/graphs',
    query: {page_no: '1', page_size: '11'}
  };
  assert.equal(matchesRequiredApi(
    response(`http://hubble${requiredRequest.path}?page_no=1&page_size=11`),
    requiredRequest
  ), true);
  assert.equal(matchesRequiredApi(
    response(`http://hubble${requiredRequest.path}?page_no=1&page_size=-1`),
    requiredRequest
  ), false);
  assert.equal(matchesRequiredApi(
    response(
      `http://hubble${requiredRequest.path}?page_no=1&page_size=11`,
      {method: 'POST'}
    ), requiredRequest
  ), false);
});

test('waits for and validates the required API response body', async () => {
  const requiredRequest = {
    path: '/api/v1.3/graphspaces/DEFAULT/graphs',
    query: {page_no: '1', page_size: '11'}
  };
  const apiResponse = response(
    `http://hubble${requiredRequest.path}?page_no=1&page_size=11`,
    {
    body: {status: 200, data: {records: []}}
    }
  );
  const page = {
    waitForResponse: async (predicate, options) => {
      assert.deepEqual(options, {timeout: 30000});
      assert.equal(predicate(apiResponse), true);
      return apiResponse;
    }
  };

  const result = await waitForRequiredResponse(page, requiredRequest);
  assert.equal(result.error, null);
  assert.deepEqual(result.entry, {
    method: 'GET',
    url: `http://hubble${requiredRequest.path}?page_no=1&page_size=11`,
    httpStatus: 200,
    ok: true,
    businessStatus: 200
  });
  assert.equal(apiEntryPassed(result.entry), true);
});

test('keeps a missing required API fail-closed', async () => {
  const page = {
    waitForResponse: async () => {
      throw new Error('required API timeout');
    }
  };

  const result = await waitForRequiredResponse(
    page, {path: '/api/v1.3/graphspaces/DEFAULT/graphs'}
  );
  assert.equal(result.entry, null);
  assert.equal(result.error, 'required API timeout');
  assert.equal(apiEntryPassed(result.entry), false);
});

test('rejects unsuccessful or malformed required API responses', async () => {
  const failures = [
    response('http://hubble/api/v1.3/graphspaces', {
      httpStatus: 500,
      body: {status: 500}
    }),
    response('http://hubble/api/v1.3/graphspaces', {
      body: {status: 500}
    }),
    response('http://hubble/api/v1.3/graphspaces', {
      body: {data: {records: []}}
    }),
    response('http://hubble/api/v1.3/graphspaces', {
      jsonError: 'invalid JSON'
    })
  ];

  for (const apiResponse of failures) {
    const page = {
      waitForResponse: async predicate => {
        assert.equal(predicate(apiResponse), true);
        return apiResponse;
      }
    };
    const result = await waitForRequiredResponse(
      page, {path: '/api/v1.3/graphspaces'}
    );
    assert.equal(apiEntryPassed(result.entry), false);
  }
});
