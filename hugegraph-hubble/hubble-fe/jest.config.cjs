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

module.exports = {
    roots: ['<rootDir>/src', '<rootDir>/scripts'],
    testEnvironment: 'jsdom',
    testMatch: ['**/?(*.)+(test).[jt]s?(x)'],
    setupFiles: ['<rootDir>/scripts/test-globals.cjs'],
    setupFilesAfterEnv: ['<rootDir>/src/setupTests.js'],
    transform: {
        '^.+\\.tsx?$': ['@swc/jest', {
            jsc: {target: 'es2022', parser: {syntax: 'typescript', tsx: true},
                transform: {react: {runtime: 'automatic'}}},
        }],
        '^.+\\.[cm]?jsx?$': ['@swc/jest', {
            jsc: {target: 'es2022', parser: {syntax: 'ecmascript', jsx: true},
                transform: {
                    react: {runtime: 'automatic'},
                    // Vite supplies the build mode; Jest runs these modules in test mode.
                    optimizer: {globals: {vars: {'import.meta.env.MODE': '"test"'}}},
                }},
        }],
    },
    moduleNameMapper: {
        '\\.(css|less|scss|sass)$': 'identity-obj-proxy',
        '\\.(gif|png|jpe?g|webp|svg|woff2?|ttf|eot|mp4)$': '<rootDir>/scripts/test-asset.cjs',
    },
    // Only ESM dependencies used in the test graph need a test transform. pnpm stores
    // scoped packages as scope+name inside .pnpm.
    transformIgnorePatterns: [
        'node_modules/(?!\\.pnpm/|lodash-es/|screenfull/|@ant-design/|@antv/|d3-[^/]+/|internmap/)',
        'node_modules/\\.pnpm/(?!(lodash-es|screenfull|@ant-design\\+[^@]+|@antv\\+[^@]+|d3-[^@]+|internmap)@)',
    ],
    // React 19 act schedules microtasks; fake only the application timers.
    fakeTimers: {doNotFake: ['queueMicrotask']},
    clearMocks: true,
    maxWorkers: 2,
    cacheDirectory: '<rootDir>/node_modules/.cache/jest',
};
