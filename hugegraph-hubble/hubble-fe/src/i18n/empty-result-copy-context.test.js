/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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

import fs from 'fs';
import path from 'path';

import enAnalysis from './resources/en-US/modules/analysis.json';
import zhAnalysis from './resources/zh-CN/modules/analysis.json';

const readSource = relativePath => fs.readFileSync(
    path.resolve(__dirname, '..', relativePath),
    'utf8'
);

describe('empty graph result copy stays scoped to its product flow', () => {
    it.each([
        ['English', enAnalysis.analysis],
        ['Chinese', zhAnalysis.analysis],
    ])('keeps real %s query and algorithm copy distinct', (name, analysis) => {
        expect(analysis.query_result.no_graph_result).toMatch(/Table|表格/);
        expect(analysis.query_result.no_graph_result).toMatch(/JSON/);
        expect(analysis.algorithm.result.no_graph_result).not.toMatch(/Table|表格|JSON/);
    });

    it('uses the algorithm-only key in both algorithm empty-result views', () => {
        const graphResult = readSource('modules/algorithm/GraphResult/Home/index.js');
        const rankResult = readSource(
            'modules/algorithm/GraphResult/RankApiView/index.js'
        );

        expect(graphResult).toContain(
            "t('analysis.algorithm.result.no_graph_result')"
        );
        expect(rankResult).toContain(
            "t('analysis.algorithm.result.no_graph_result')"
        );
    });

    it('keeps the query key in the Gremlin empty-graph view', () => {
        const queryResult = readSource(
            'modules/analysis/QueryResult/GraphResult/Home/index.js'
        );

        expect(queryResult).toContain(
            "t('analysis.query_result.no_graph_result')"
        );
        expect(queryResult).not.toContain(
            "t('analysis.algorithm.result.no_graph_result')"
        );
    });
});
