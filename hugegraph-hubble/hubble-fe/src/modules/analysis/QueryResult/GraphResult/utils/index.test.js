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

jest.mock('@antv/graphin-icons', () => () => ({glyphs: []}));
jest.mock('antd', () => ({message: {error: jest.fn(), warning: jest.fn()}}));
jest.mock('../../../../../api/index', () => ({
    analysis: {
        putExecutionQuery: jest.fn(),
    },
}));

import {handleExpandGraph} from './index';

const metaData = {
    vertexMeta: [{name: 'person', style: {display_fields: ['~id']}}],
    edgeMeta: [{name: 'knows', style: {display_fields: ['~id']}}],
};

test('does not append duplicate nodes or edges while expanding graph result', () => {
    const graphInstance = {
        save: () => ({
            nodes: [{
                id: 'v1',
                label: 'v1',
                itemType: 'person',
                legendType: 'person',
                properties: {},
            }],
            edges: [{
                id: 'e1',
                label: 'e1',
                rawLabel: 'knows',
                itemType: 'knows',
                legendType: 'knows',
                source: 'v1',
                target: 'v1',
                properties: {},
            }],
        }),
        changeData: jest.fn(),
        refresh: jest.fn(),
    };

    const result = handleExpandGraph({
        vertices: [{id: 'v1', label: 'person', properties: {}}],
        edges: [{id: 'e1', label: 'knows', source: 'v1', target: 'v1', properties: {}}],
    }, metaData, {}, graphInstance);

    expect(result.nodes).toHaveLength(1);
    expect(result.edges).toHaveLength(1);
    expect(graphInstance.changeData).toHaveBeenCalledWith({
        nodes: result.nodes,
        edges: result.edges,
    }, true);
});
