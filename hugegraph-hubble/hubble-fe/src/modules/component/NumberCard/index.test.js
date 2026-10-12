/*
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

import {render, screen} from '@testing-library/react';
import NumberCard from './index';
import enAnalysis from '../../../i18n/resources/en-US/modules/analysis.json';
import zhAnalysis from '../../../i18n/resources/zh-CN/modules/analysis.json';

let mockLanguage = 'en';
const mockResources = {
    en: enAnalysis.analysis.canvas.number_card,
    zh: zhAnalysis.analysis.canvas.number_card,
};

const mockInterpolate = (value, values = {}) => Object.entries(values).reduce(
    (text, [key, replacement]) => text.replace(`{{${key}}}`, replacement),
    value
);

jest.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key, values) => {
            const numberCard = mockResources[mockLanguage];
            const value = numberCard[key.split('.').pop()] ?? key;
            return mockInterpolate(value, values);
        },
    }),
}));

const counts = {
    currentGraphNodesNum: 1,
    currentGraphEdgesNum: 2,
    allGraphNodesNum: 3,
    allGraphEdgesNum: 8,
};

beforeEach(() => {
    mockLanguage = 'en';
});

test('explains current-result and full-graph counts in natural English', () => {
    render(<NumberCard pathNum={4} data={counts} />);

    expect(screen.getByText('Paths')).toBeInTheDocument();
    expect(screen.getByText('Nodes')).toBeInTheDocument();
    expect(screen.getByText('Edges')).toBeInTheDocument();
    expect(screen.getByRole('group', {
        name: 'Nodes: 1 in this result, 3 in the full graph',
    })).toHaveAttribute('title', 'Nodes: 1 in this result, 3 in the full graph');
    expect(screen.getByRole('group', {
        name: 'Edges: 2 in this result, 8 in the full graph',
    })).toBeInTheDocument();
});

test('localizes labels and count semantics in Chinese', () => {
    mockLanguage = 'zh';
    render(<NumberCard data={counts} />);

    expect(screen.getByText('节点')).toBeInTheDocument();
    expect(screen.getByText('边')).toBeInTheDocument();
    expect(screen.getByRole('group', {
        name: '节点：当前结果 1，全图 3',
    })).toBeInTheDocument();
    expect(screen.getByRole('group', {
        name: '边：当前结果 2，全图 8',
    })).toBeInTheDocument();
});

test('keeps current-result counts while full-graph totals are loading', () => {
    render(
        <NumberCard
            data={{
                currentGraphNodesNum: 1,
                currentGraphEdgesNum: 2,
                allGraphNodesNum: -1,
                allGraphEdgesNum: -1,
            }}
        />
    );

    expect(screen.getAllByText('Loading')).toHaveLength(2);
    expect(screen.getByText('1')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.queryByText('-1')).not.toBeInTheDocument();
    expect(screen.getByRole('group', {
        name: 'Nodes: 1 in this result, Loading in the full graph',
    })).toBeInTheDocument();
    expect(screen.getByRole('group', {
        name: 'Edges: 2 in this result, Loading in the full graph',
    })).toBeInTheDocument();
});

test('describes each loading sentinel independently in Chinese', () => {
    mockLanguage = 'zh';
    render(
        <NumberCard
            data={{
                currentGraphNodesNum: -1,
                currentGraphEdgesNum: 2,
                allGraphNodesNum: 3,
                allGraphEdgesNum: -1,
            }}
        />
    );

    expect(screen.getByRole('group', {
        name: '节点：当前结果 加载中，全图 3',
    })).toBeInTheDocument();
    expect(screen.getByRole('group', {
        name: '边：当前结果 2，全图 加载中',
    })).toBeInTheDocument();
});

test('marks missing and invalid counts unavailable instead of rendering blanks', () => {
    render(
        <NumberCard
            data={{
                currentGraphNodesNum: undefined,
                currentGraphEdgesNum: 'invalid',
                allGraphNodesNum: null,
                allGraphEdgesNum: '',
            }}
        />
    );

    expect(screen.getAllByText('Unavailable')).toHaveLength(4);
    expect(screen.getByRole('group', {
        name: 'Nodes: Unavailable in this result, Unavailable in the full graph',
    })).toBeInTheDocument();
    expect(screen.getByRole('group', {
        name: 'Edges: Unavailable in this result, Unavailable in the full graph',
    })).toBeInTheDocument();
});

test('ships symmetric Chinese and English NumberCard copy', () => {
    expect(zhAnalysis.analysis.canvas.number_card).toEqual({
        paths: '路径',
        nodes: '节点',
        edges: '边',
        loading: '加载中',
        unavailable: '不可用',
        node_summary: '节点：当前结果 {{current}}，全图 {{total}}',
        edge_summary: '边：当前结果 {{current}}，全图 {{total}}',
    });
    expect(enAnalysis.analysis.canvas.number_card).toEqual({
        paths: 'Paths',
        nodes: 'Nodes',
        edges: 'Edges',
        loading: 'Loading',
        unavailable: 'Unavailable',
        node_summary: 'Nodes: {{current}} in this result, {{total}} in the full graph',
        edge_summary: 'Edges: {{current}} in this result, {{total}} in the full graph',
    });
});
