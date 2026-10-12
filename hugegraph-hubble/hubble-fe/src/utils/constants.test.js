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
    ALGORITHM_NAME,
    Algorithm_Layout,
    Async_Task_Manipulations,
    Async_Task_Type,
    Async_Taskt_Status_Name,
    Filter_Task_Status,
    getAlgorithmDisplayName,
    getCanonicalAlgorithmName,
    getTranslatedAsyncTaskConstants,
    isAlgorithmNameMatched,
} from './constants';
import enAnalysis from '../i18n/resources/en-US/modules/analysis.json';
import zhAnalysis from '../i18n/resources/zh-CN/modules/analysis.json';

const t = key => key.split('.').reduce((value, part) => value && value[part], enAnalysis);
const zhT = key => key.split('.').reduce((value, part) => value && value[part], zhAnalysis);
const asyncTaskMaps = [
    Async_Task_Type,
    Async_Taskt_Status_Name,
    Filter_Task_Status,
    Async_Task_Manipulations,
];

describe('isAlgorithmNameMatched', () => {
    it('keeps algorithm constants free of hard-coded Chinese display text', () => {
        Object.values(ALGORITHM_NAME).forEach(value => {
            expect(value).not.toMatch(/[\u4E00-\u9FFF]/);
        });
    });

    it('keeps legacy Chinese algorithm names translatable for saved task state', () => {
        expect(getAlgorithmDisplayName('查找最短路径', t)).toBe('Find Shortest Path');
        expect(getAlgorithmDisplayName('查找最短路径', zhT)).toBe('查找最短路径');
        expect(isAlgorithmNameMatched('查找所有路径（GET，基础版）', 'Basic', t)).toBe(true);
    });

    it('keeps old exact KOUT_POST display text as a legacy alias', () => {
        const legacyName = 'K-out API(POST, 高级版)';

        expect(getAlgorithmDisplayName(legacyName, t)).toBe('K-out API (POST, Advanced)');
        expect(isAlgorithmNameMatched(legacyName, 'Advanced', t)).toBe(true);
        expect(isAlgorithmNameMatched(legacyName, 'POST', t)).toBe(true);
    });

    it('normalizes localized or legacy names before semantic map lookup', () => {
        const legacyName = 'K-out API(POST, 高级版)';
        const canonicalName = getCanonicalAlgorithmName(legacyName, t);

        expect(canonicalName).toBe(ALGORITHM_NAME.KOUT_POST);
        expect(Algorithm_Layout[canonicalName]).toBe('force');
        expect(Algorithm_Layout[zhT('analysis.algorithm.olap.item.KOUT_POST')]).toBeUndefined();
    });

    it('matches translated display names when raw algorithm names are localized', () => {
        expect(isAlgorithmNameMatched(ALGORITHM_NAME.FINDSHORTESTPATH, 'Find', t)).toBe(true);
        expect(isAlgorithmNameMatched(ALGORITHM_NAME.PATHS, 'Basic', t)).toBe(true);
    });

    it('keeps matching raw algorithm names as a fallback', () => {
        expect(isAlgorithmNameMatched(ALGORITHM_NAME.FINDSHORTESTPATH, '查找', t)).toBe(true);
    });

    it('keeps raw fallback when callers pass translated algorithm names', () => {
        const translatedName = t('analysis.algorithm.olap.item.FINDSHORTESTPATH');

        expect(isAlgorithmNameMatched(translatedName, '查找', t)).toBe(true);
    });
});

describe('async task constants i18n keys', () => {
    it('keeps async task display maps as i18n keys instead of hard-coded text', () => {
        const values = asyncTaskMaps.flatMap(map => Object.values(map));

        values.forEach(value => {
            expect(value).toMatch(/^analysis\.async_task\./);
            expect(value).not.toMatch(/[\u4E00-\u9FFF]/);
        });
    });

    it('resolves every async task i18n key in English and Chinese resources', () => {
        const values = asyncTaskMaps.flatMap(map => Object.values(map));

        values.forEach(value => {
            expect(t(value)).toBeTruthy();
            expect(t(value)).not.toBe(value);
            expect(zhT(value)).toBeTruthy();
            expect(zhT(value)).not.toBe(value);
        });
    });

    it('translates async task display constants through shared resources', () => {
        const enConstants = getTranslatedAsyncTaskConstants(t);
        const zhConstants = getTranslatedAsyncTaskConstants(zhT);

        expect(enConstants.taskTypeNames.gremlin).toBe('Gremlin Task');
        expect(enConstants.taskStatusNames.queued).toBe('Queued');
        expect(enConstants.taskManipulations.check_result).toBe('View Result');

        expect(zhConstants.taskTypeNames.gremlin).toBe('Gremlin任务');
        expect(zhConstants.taskStatusNames.queued).toBe('排队中');
        expect(zhConstants.taskManipulations.check_result).toBe('查看结果');
    });
});
