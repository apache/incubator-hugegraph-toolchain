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

import {formatResourceLimit, formatStorageLimit, formatStorageUsed,
    storageUsagePercent, formatComputeLimit} from './storageUsage';

test.each([
    [1, '1 KB'],
    [512, '512 KB'],
    [1024, '1 MB'],
    [512 * 1024, '512 MB'],
    [1024 * 1024, '1 GB'],
    [1536 * 1024, '1.5 GB'],
    [1024 * 1024 * 1024, '1 TB'],
    [0, '0 KB'],
])('formats %s KB without truncating sub-GB usage', (kb, expected) => {
    expect(formatStorageUsed({storage_used_kb: kb, storage_used: 0})).toBe(expected);
});

test('uses legacy GB when the server does not supply precise usage', () => {
    expect(formatStorageUsed({storage_used: 2})).toBe('2 GB');
    expect(formatStorageUsed({storage_used_kb: null, storage_used: 2})).toBe('2 GB');
    expect(formatStorageUsed({storage_used_kb: '524288', storage_used: 3})).toBe('3 GB');
});

test('keeps finite storage quotas numeric with the same unit style as usage', () => {
    expect(formatStorageLimit(10240, 'Unlimited')).toBe('10240 GB');
    expect(formatStorageLimit(2147483647, 'Unlimited')).toBe('Unlimited');
    expect(formatStorageLimit(2147483647, 'Unlimited', '--')).toBe('--');
});

test('finite quotas above 99999 stay numeric and legacy sentinel stays unlimited', () => {
    expect(formatResourceLimit(10240, ' GB', 'Unlimited')).toBe('10240 GB');
    expect(formatResourceLimit(1000000, ' GB', 'Unlimited')).toBe('1000000 GB');
    expect(formatResourceLimit(2147483647, ' GB', 'Unlimited')).toBe('Unlimited');
    expect(formatResourceLimit(2147483647, ' GB', 'Unlimited', '--')).toBe('--');
});

test('derives small percentages from KB even when the server rounded its ratio to zero', () => {
    expect(storageUsagePercent({storage_used_kb: 1024, storage_limit: 10240, storage_percent: 0}))
        .toBeCloseTo(0.0000095367431640625, 15);
    expect(storageUsagePercent({storage_used_kb: 0, storage_limit: 100, storage_percent: 0.5})).toBe(0);
    expect(storageUsagePercent({storage_used_kb: 2 * 1024 * 1024, storage_limit: 1})).toBe(200);
});

test('uses legacy ratios when precise usage or a positive limit is unavailable', () => {
    for (const item of [
        {storage_percent: 0.25},
        {storage_used_kb: null, storage_limit: 100, storage_percent: 0.25},
        {storage_used_kb: -1, storage_limit: 100, storage_percent: 0.25},
        {storage_used_kb: 1024, storage_limit: 0, storage_percent: 0.25},
    ]) {
        expect(storageUsagePercent(item)).toBe(25);
    }
});

test('compute zero continuously inherits the current graph-service limit', () => {
    const t = (key, options) => key === 'graphspace.unit.inherited'
        ? `Inherited (${options.value})` : 'Unlimited';
    expect(formatComputeLimit(0, 4, ' vCPU', t)).toBe('Inherited (4 vCPU)');
    expect(formatComputeLimit(0, 16, ' vCPU', t)).toBe('Inherited (16 vCPU)');
    expect(formatComputeLimit(0, 8, ' GB', t)).toBe('Inherited (8 GB)');
    expect(formatComputeLimit(6, 4, ' vCPU', t)).toBe('6 vCPU');
    expect(formatComputeLimit(0, undefined, ' GB', t)).toBe('Inherited (--)');
});

test.each([1024, undefined])('does not calculate a percentage of an unlimited quota (%s KB)', usedKb => {
    expect(storageUsagePercent({
        storage_limit: 2147483647, storage_used_kb: usedKb, storage_percent: 1,
    })).toBeNull();
});
