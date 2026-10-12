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

jest.mock('./request', () => ({
    __esModule: true,
    default: {
        post: jest.fn(),
    },
}));

const request = require('./request').default;
const analysis = require('./analysis');

describe('algorithm API failure contract', () => {
    beforeEach(() => {
        request.post.mockReset();
    });

    it.each([
        ['runOltpInfo', ['DEFAULT', 'g', {algorithmName: 'kneighbor'}]],
        ['postOlapInfo', ['DEFAULT', 'g', {algorithm: 'louvain'}]],
        ['runOlapVermeer', ['DEFAULT', 'g', {algorithm: 'pagerank'}]],
    ])('normalizes rejected %s requests for the shared form contract', async (name, args) => {
        request.post.mockRejectedValue({
            response: {
                status: 503,
                data: {status: 503, message: 'algorithm service unavailable'},
            },
        });

        await expect(analysis[name](...args)).resolves.toEqual({
            status: 503,
            data: null,
            message: 'algorithm service unavailable',
        });
    });

    it('preserves successful algorithm responses', async () => {
        const success = {status: 200, data: {task_id: 7}, message: 'Success'};
        request.post.mockResolvedValue(success);

        await expect(analysis.postOlapInfo('DEFAULT', 'g', {})).resolves.toBe(success);
    });

    it('preserves resolved business failures', async () => {
        const failure = {status: 400, data: null, message: 'invalid source'};
        request.post.mockResolvedValue(failure);

        await expect(analysis.runOltpInfo('DEFAULT', 'g', {
            algorithmName: 'kneighbor',
        })).resolves.toBe(failure);
    });

    it.each([
        [{data: {status: 401, message: 'expired'}}],
        [{response: {status: 401, data: {status: 401, message: 'expired'}}}],
    ])('keeps rejected 401 status after request-level redirect handling', async error => {
        request.post.mockRejectedValue(error);

        await expect(analysis.postOlapInfo('DEFAULT', 'g', {})).resolves.toEqual({
            status: 401,
            data: null,
            message: 'expired',
        });
    });
});
