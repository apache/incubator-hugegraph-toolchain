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

import dayjs from 'dayjs';
import {filterData} from './filter';

const matches = (left, right, rule) => filterData({
    vertices: [{id: 'v1', properties: {created: left}}], edges: [],
}, {vertex: [{op_type: 'property', property: 'created', data_type: 'DATE', value: right, rule}], edge: []}, 'and')
    .vertices.length === 1;

const start = dayjs('2026-10-01');
const end = dayjs('2026-10-03');

test.each([
    ['eq', '2026-10-01', true], ['eq', '2026-10-01T12:00:00', false],
    ['neq', '2026-10-01', false], ['neq', '2026-10-02', true],
    ['gt', '2026-10-01', false], ['gt', '2026-10-01T12:00:00', true],
    ['gte', '2026-10-01', true], ['gte', '2026-09-30', false],
    ['lt', '2026-10-01', false], ['lt', '2026-09-30', true],
    ['lte', '2026-10-01', true], ['lte', '2026-10-02', false],
])('preserves DATE %s timestamp precision for %s', (rule, left, expected) => {
    expect(matches(left, start, rule)).toBe(expected);
});

test.each([
    ['ltlt', false, false], ['ltelt', true, false],
    ['ltlte', false, true], ['ltelte', true, true],
])('preserves DATE interval %s boundary inclusivity', (rule, includeStart, includeEnd) => {
    expect(matches('2026-10-01', [start, end], rule)).toBe(includeStart);
    expect(matches('2026-10-02', [start, end], rule)).toBe(true);
    expect(matches('2026-10-03', [start, end], rule)).toBe(includeEnd);
    expect(matches('2026-09-30', [start, end], rule)).toBe(false);
    expect(matches('2026-10-04', [start, end], rule)).toBe(false);
});
