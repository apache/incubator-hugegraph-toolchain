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

import * as api from '../api';

export const GRAPH_OPTION_PAGE_SIZE = 100;

const optionsPage = (response, pageNo, selected) => {
    if (response?.status !== 200 || !Array.isArray(response.data?.records)) {
        const error = new Error('Graph options unavailable');
        error.status = response?.status;
        throw error;
    }
    const {records} = response.data;
    return {
        records,
        selected: Object.hasOwn(response.data, 'selected')
            ? response.data.selected : records.find(item => item.name === selected) ?? null,
        hasMore: pageNo * GRAPH_OPTION_PAGE_SIZE < Number(response.data.total ?? records.length),
    };
};

export const loadGraphOptions = async (graphspace, {pageNo = 1, query = '', selected = ''} = {}, config = {}) => {
    const response = await api.manage.getGraphList(graphspace, {
        brief: true,
        page_no: pageNo,
        page_size: GRAPH_OPTION_PAGE_SIZE,
        query,
        selected,
    }, config);
    return optionsPage(response, pageNo, selected);
};

export const loadGraphspaceOptions = async ({pageNo = 1, query = '', selected = ''} = {}, config = {}) => {
    const response = await api.manage.getGraphSpaceList({
        brief: true,
        page_no: pageNo,
        page_size: GRAPH_OPTION_PAGE_SIZE,
        query,
        selected,
    }, config);
    return optionsPage(response, pageNo, selected);
};

export const mergeGraphOptions = (records, selected) => {
    const unique = new Map(records.map(item => [item.name, item]));
    if (selected && !unique.has(selected.name)) {
        return [selected, ...unique.values()];
    }
    return [...unique.values()];
};
