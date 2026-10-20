/** @jest-environment node */
/*
 *
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

const {createDevProxy} = require('./dev-proxy.cjs');

const options = env => createDevProxy(env)['/api/v1.3'];

test('proxies only the backend API path using the local backend by default', () => {
    expect(Object.keys(createDevProxy({}))).toEqual(['/api/v1.3']);
    expect(options({})).toEqual({
        target: 'http://127.0.0.1:8088',
        changeOrigin: true,
        headers: {origin: 'http://127.0.0.1:8088'},
    });
});

test('supports the legacy development proxy setting', () => {
    expect(options({HUBBLE_API_PROXY: 'http://127.0.0.1:19000'}))
        .toMatchObject({target: 'http://127.0.0.1:19000'});
});

test('uses the preferred backend origin for both target and Origin', () => {
    expect(options({HUBBLE_API_TARGET: 'http://127.0.0.1:18080'}))
        .toMatchObject({
            target: 'http://127.0.0.1:18080',
            headers: {origin: 'http://127.0.0.1:18080'},
        });
});

test('prefers HUBBLE_API_TARGET when both development settings are present', () => {
    expect(options({
        HUBBLE_API_TARGET: 'http://127.0.0.1:18080',
        HUBBLE_API_PROXY: 'http://127.0.0.1:19000',
    })).toMatchObject({target: 'http://127.0.0.1:18080'});
});
