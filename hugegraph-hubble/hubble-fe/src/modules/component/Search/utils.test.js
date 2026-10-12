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

import {
    BOOLEAN_FALSE,
    BOOLEAN_TRUE,
    getRuleOptionLabelKey,
    getRuleOptions,
    normalizeFilterCondition,
} from './utils';

describe('Search filter utils', () => {
    it('normalizes boolean rules to explicit eq boolean conditions', () => {
        expect(getRuleOptions('boolean')).toEqual([BOOLEAN_TRUE, BOOLEAN_FALSE]);
        expect(getRuleOptionLabelKey(BOOLEAN_TRUE)).toBe('true');
        expect(getRuleOptionLabelKey(BOOLEAN_FALSE)).toBe('false');

        expect(normalizeFilterCondition({
            key: 'active',
            operator: BOOLEAN_TRUE,
        })).toEqual({
            key: 'active',
            operator: 'eq',
            value: true,
        });
        expect(normalizeFilterCondition({
            key: 'active',
            operator: BOOLEAN_FALSE,
        })).toEqual({
            key: 'active',
            operator: 'eq',
            value: false,
        });
    });

    it('keeps falsy but valid scalar values', () => {
        expect(normalizeFilterCondition({
            key: 'score',
            operator: 'eq',
            value: 0,
        })).toEqual({
            key: 'score',
            operator: 'eq',
            value: 0,
        });

        expect(normalizeFilterCondition({
            key: 'name',
            operator: 'eq',
            value: '',
        })).toBeNull();
    });
});
