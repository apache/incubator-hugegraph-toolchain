/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const {createProxyMiddleware} = require('http-proxy-middleware');
const setupProxy = require('./setupProxy');

jest.mock('http-proxy-middleware', () => ({
    createProxyMiddleware: jest.fn(options => options),
}));

const originalTarget = process.env.HUBBLE_API_TARGET;
const originalProxy = process.env.HUBBLE_API_PROXY;

afterEach(() => {
    if (originalTarget === undefined) {
        delete process.env.HUBBLE_API_TARGET;
    }
    else {
        process.env.HUBBLE_API_TARGET = originalTarget;
    }
    if (originalProxy === undefined) {
        delete process.env.HUBBLE_API_PROXY;
    }
    else {
        process.env.HUBBLE_API_PROXY = originalProxy;
    }
    jest.clearAllMocks();
});

test('uses the backend default port for local development', () => {
    delete process.env.HUBBLE_API_TARGET;
    delete process.env.HUBBLE_API_PROXY;
    const app = {use: jest.fn()};

    setupProxy(app);

    expect(createProxyMiddleware).toHaveBeenCalledWith(expect.objectContaining({
        target: 'http://127.0.0.1:8088',
    }));
});

test('keeps the legacy proxy environment variable compatible', () => {
    delete process.env.HUBBLE_API_TARGET;
    process.env.HUBBLE_API_PROXY = 'http://127.0.0.1:19000';

    setupProxy({use: jest.fn()});

    expect(createProxyMiddleware).toHaveBeenCalledWith(expect.objectContaining({
        target: 'http://127.0.0.1:19000',
        headers: {origin: 'http://127.0.0.1:19000'},
    }));
});

test('uses HUBBLE_API_TARGET as the preferred backend origin', () => {
    process.env.HUBBLE_API_TARGET = 'http://127.0.0.1:18080';
    delete process.env.HUBBLE_API_PROXY;

    setupProxy({use: jest.fn()});

    expect(createProxyMiddleware).toHaveBeenCalledWith(expect.objectContaining({
        target: 'http://127.0.0.1:18080',
        headers: {origin: 'http://127.0.0.1:18080'},
    }));
});

test('prefers HUBBLE_API_TARGET over the legacy proxy setting', () => {
    process.env.HUBBLE_API_TARGET = 'http://127.0.0.1:18080';
    process.env.HUBBLE_API_PROXY = 'http://127.0.0.1:19000';

    setupProxy({use: jest.fn()});

    expect(createProxyMiddleware).toHaveBeenCalledWith(expect.objectContaining({
        target: 'http://127.0.0.1:18080',
        headers: {origin: 'http://127.0.0.1:18080'},
    }));
});
