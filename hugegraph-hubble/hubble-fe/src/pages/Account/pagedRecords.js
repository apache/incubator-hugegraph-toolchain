/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.
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

const PAGE_ERROR_CONFIG = {suppressBusinessErrorToast: true};
const PAGE_SIZE = 500;
const MAX_RECORDS = 10_000;

const loadAllPages = async (request, {params = {}, config = PAGE_ERROR_CONFIG} = {}) => {
    const records = [];
    let pageNo = 1;
    let total;

    while (records.length <= MAX_RECORDS) {
        const response = await request({
            ...params,
            page_no: pageNo,
            page_size: PAGE_SIZE,
        }, config);
        if (response?.status !== 200) {
            return response;
        }
        const pageRecords = response?.data?.records ?? [];
        if (records.length + pageRecords.length > MAX_RECORDS) {
            throw new Error(`Record list exceeds ${MAX_RECORDS}`);
        }
        records.push(...pageRecords);

        const rawTotal = response.data?.total;
        const normalizedTotal = typeof rawTotal === 'string'
            ? rawTotal.trim()
            : rawTotal;
        if (normalizedTotal !== undefined
            && normalizedTotal !== null
            && normalizedTotal !== '') {
            const declaredTotal = Number(normalizedTotal);
            if (Number.isFinite(declaredTotal) && declaredTotal >= 0) {
                if (declaredTotal > MAX_RECORDS) {
                    throw new Error(`Record list exceeds ${MAX_RECORDS}`);
                }
                total = declaredTotal;
            }
        }
        if (total !== undefined && records.length < total
            && pageRecords.length === 0) {
            throw new Error('Record list ended before its declared total');
        }
        if ((total !== undefined && records.length >= total)
            || (total === undefined && pageRecords.length < PAGE_SIZE)) {
            return {
                status: 200,
                data: {records, total: total ?? records.length},
            };
        }
        pageNo += 1;
    }
    throw new Error(`Record list exceeds ${MAX_RECORDS}`);
};

export {loadAllPages, PAGE_ERROR_CONFIG};
