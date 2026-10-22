/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import {loadGraphOptions, mergeGraphOptions} from './graphOptions';
import * as api from '../api';

jest.mock('../api', () => ({manage: {getGraphList: jest.fn()}}));

beforeEach(() => jest.clearAllMocks());

test('loads one bounded page and its late selected option without aggregating all pages', async () => {
    const records = Array.from({length: 100}, (_, index) => ({name: `graph_${index}`}));
    const selected = {name: 'graph_10000', nickname: 'Late graph'};
    api.manage.getGraphList.mockResolvedValue({
        status: 200, data: {records, total: 10001, selected},
    });
    const config = {signal: new AbortController().signal, suppressBusinessErrorToast: true};

    const page = await loadGraphOptions('space', {query: 'Late', selected: selected.name}, config);

    expect(api.manage.getGraphList).toHaveBeenCalledTimes(1);
    expect(api.manage.getGraphList).toHaveBeenCalledWith('space', {
        brief: true, page_no: 1, page_size: 100, query: 'Late', selected: 'graph_10000',
    }, config);
    expect(page).toEqual({records, selected, hasMore: true});
    expect(mergeGraphOptions(records, selected)).toHaveLength(101);
});

test('merges a selected graph only once when its page is reached', () => {
    const selected = {name: 'late', nickname: 'Late graph'};
    expect(mergeGraphOptions([selected, {name: 'first'}, selected], selected))
        .toEqual([selected, {name: 'first'}]);
});

test('preserves forbidden status for the context permission boundary', async () => {
    api.manage.getGraphList.mockResolvedValue({status: 403});
    await expect(loadGraphOptions('space')).rejects.toMatchObject({status: 403});
});
