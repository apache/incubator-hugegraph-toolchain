/*
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

import {fireEvent, render, screen} from '@testing-library/react';
import QueryResult from './index';
import {
    getGraphViewLimitStatus,
    getBoundedGraphView,
    getBoundedJsonPreview,
    getJsonViewContent,
    getQueryResultStandbyMessage,
} from './utils';
import JSONbig from 'json-bigint';
import {adaptHubbleGraph, exportHubbleGraph} from '../../../../visualization/hubble/adapter';
import {toElementDetails} from '../../../../utils/analysisGraphData';
import enAnalysis from '../../../../i18n/resources/en-US/modules/analysis.json';
import zhAnalysis from '../../../../i18n/resources/zh-CN/modules/analysis.json';

jest.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key, options) => {
            const text = {
                'analysis.query_result.graph': 'Graph',
                'analysis.query_result.table': 'Table',
                'analysis.query_result.graph_limit_title': 'Graph limit',
                'analysis.query_result.graph_limit_description':
                    `${options?.nodes}/${options?.nodeLimit}`,
                'analysis.query_result.non_graph_title': 'Query result preview',
                'analysis.query_result.non_graph_description':
                    'This result cannot be rendered as a graph; a JSON preview is shown.',
                'analysis.query_result.view_full_json': 'View JSON preview',
                'analysis.query_result.empty_success':
                    'Query succeeded, result is empty',
            };
            return text[key] || key;
        },
    }),
}));
jest.mock('../GraphResult/Home', () => ({queryProgress, onRefreshGraphNums, previewNotice}) => (
    <div>graph canvas {previewNotice}{queryProgress && <span>{queryProgress.phase}</span>}
        <button onClick={() => onRefreshGraphNums?.()}>Refresh saved graph counts</button>
    </div>
));
jest.mock('../TableView', () => () => <div>table result</div>);
jest.mock('../JsonView', () => () => <div>json result</div>);

it('projects BigInt, production BigNumber, and null-prototype values safely', () => {
    const bigNumber = JSONbig.parse('{"id":9007199254740993}').id;
    const nullPrototype = Object.create(null);
    nullPrototype.name = 'alice';
    const data = [{native: 9007199254740993n, parsed: bigNumber}, nullPrototype];
    const projected = getJsonViewContent({data});

    expect(projected).not.toBe(data);
    expect(projected[0]).toEqual({
        native: '9007199254740993',
        parsed: '9007199254740993',
    });
    expect(Object.getPrototypeOf(projected[1])).toBe(Object.prototype);
    expect(projected[1]).toEqual({name: 'alice'});
    expect(getJsonViewContent()).toEqual([]);
    expect(getJsonViewContent({data: false})).toEqual({value: false});
});

describe('graph view limits', () => {
    it.each([
        [1000, 500, false],
        [1001, 0, true],
        [0, 501, true],
    ])('evaluates %i nodes and %i edges', (nodeCount, edgeCount, exceeded) => {
        expect(getGraphViewLimitStatus({
            vertices: new Array(nodeCount),
            edges: new Array(edgeCount),
        })).toEqual({nodeCount, edgeCount, exceeded});
    });

    it('handles a missing graph result without collapsing', () => {
        expect(getGraphViewLimitStatus()).toEqual({
            nodeCount: 0,
            edgeCount: 0,
            exceeded: false,
        });
    });

    it('keeps a bounded graph preview selected when a new result exceeds the limit', () => {
        const result = nodeCount => ({
            graph_view: {vertices: new Array(nodeCount), edges: []},
            json_view: {data: []},
            table_view: {header: [], rows: []},
        });
        const view = render(<QueryResult queryResult={result(300)} />);
        expect(screen.getByRole('tab', {name: 'Graph'})).toHaveAttribute(
            'aria-selected', 'true'
        );

        view.rerender(<QueryResult queryResult={result(1001)} />);

        expect(screen.getByRole('tab', {name: 'Graph'})).toHaveAttribute(
            'aria-selected', 'true'
        );
    });
});

it('labels the structured result tab as JSON', () => {
    render(<QueryResult queryResult={{}} />);

    expect(screen.getByRole('tab', {name: 'JSON'})).toBeInTheDocument();
    expect(screen.queryByRole('tab', {name: 'Json'})).not.toBeInTheDocument();
});

it('uses a distinct prompt before the first query across result views', () => {
    const t = jest.fn(key => key);

    expect(getQueryResultStandbyMessage(t, true))
        .toBe('analysis.query_result.not_started');
    expect(getQueryResultStandbyMessage(t, false))
        .toBe('analysis.query_result.task_not_started');
    expect(t).not.toHaveBeenCalledWith('analysis.query_result.no_data');
});

it('previews a small scalar result instead of calling it an empty graph', () => {
    render(<QueryResult
        queryStatus='success'
        isQueryMode
        queryResult={{
            graph_view: {vertices: [], edges: []},
            table_view: {header: ['count'], rows: [{count: 42}]},
            json_view: {data: 42},
        }}
    />);

    expect(screen.getByText('Query result preview')).toBeInTheDocument();
    expect(screen.getByText(/42/)).toBeInTheDocument();
    expect(screen.queryByText('analysis.query_result.no_graph_result'))
        .not.toBeInTheDocument();
});

it('labels a completely empty successful result precisely', () => {
    render(<QueryResult
        queryStatus='success'
        isQueryMode
        queryResult={{
            graph_view: {vertices: [], edges: []},
            table_view: {header: [], rows: []},
            json_view: {data: []},
        }}
    />);

    expect(screen.getByText('Query succeeded, result is empty')).toBeInTheDocument();
    expect(zhAnalysis.analysis.query_result.empty_success).toBe('查询成功，结果为空');
    expect(enAnalysis.analysis.query_result.empty_success)
        .toBe('Query succeeded, result is empty');
});

it('keeps a non-empty JSON-only result in the JSON preview path', () => {
    render(<QueryResult
        queryStatus='success'
        isQueryMode
        queryResult={{
            graph_view: {vertices: [], edges: []},
            table_view: {header: [], rows: []},
            json_view: {data: {count: 42}},
        }}
    />);

    expect(screen.getByText('Query result preview')).toBeInTheDocument();
    expect(screen.getByText(/"count": 42/)).toBeInTheDocument();
    expect(screen.queryByText('Query succeeded, result is empty'))
        .not.toBeInTheDocument();
});

it('offers a direct JSON path when a non-graph preview is truncated', () => {
    render(<QueryResult
        queryStatus='success'
        isQueryMode
        queryResult={{
            graph_view: {vertices: [], edges: []},
            table_view: {
                header: ['name'],
                rows: Array.from({length: 8}, (_, index) => ({name: `node-${index}`})),
            },
            json_view: {data: Array.from({length: 8}, (_, index) => ({name: `node-${index}`}))},
        }}
    />);

    expect(screen.getByText(/node-0/)).toBeInTheDocument();
    expect(screen.queryByText(/node-7/)).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'View JSON preview'}));
    expect(screen.getByText('json result')).toBeInTheDocument();
});

it('marks circular display values without throwing', () => {
    const circular = {name: 'loop'};
    circular.self = circular;

    expect(getJsonViewContent({data: circular})).toEqual({
        name: 'loop',
        self: '[Circular]',
    });
});

it('keeps __proto__ as display data without changing the projected prototype', () => {
    const source = Object.create(null);
    Object.defineProperty(source, '__proto__', {
        enumerable: true,
        value: 'safe',
    });
    const projected = getJsonViewContent({data: source});

    expect(Object.getPrototypeOf(projected)).toBe(Object.prototype);
    expect(Object.prototype.hasOwnProperty.call(projected, '__proto__')).toBe(true);
    expect(Reflect.get(projected, '__proto__')).toBe('safe');
    expect(Object.prototype.safe).toBeUndefined();
});

it('does not collapse objects that only mimic a BigNumber constructor name', () => {
    const mimic = {
        constructor: {name: 'BigNumber'},
        toString: () => '[misleading value]',
        value: 7,
    };

    expect(getJsonViewContent({data: mimic})).toEqual({
        constructor: {name: 'BigNumber'},
        toString: mimic.toString,
        value: 7,
    });
});


it('routes Text2GQL progress to the graph result view without affecting ordinary result tabs', () => {
    const {rerender} = render(<QueryResult queryStatus='standby' isQueryMode />);
    fireEvent.click(screen.getByRole('tab', {name: 'JSON'}));
    expect(screen.getByText('json result')).toBeInTheDocument();
    rerender(<QueryResult queryStatus='standby' isQueryMode queryProgress={{phase: 'waiting'}} />);
    expect(screen.getByRole('tab', {name: 'Graph'})).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByText('waiting')).toBeInTheDocument();
    rerender(<QueryResult queryStatus='standby' isQueryMode queryProgress={{phase: 'streaming'}} />);
    expect(screen.getByText('streaming')).toBeInTheDocument();
    rerender(<QueryResult queryStatus='standby' isQueryMode />);
    expect(screen.queryByText('streaming')).not.toBeInTheDocument();
});

it('keeps the requested result slot height when switching views', () => {
    const view = render(<QueryResult height={320} />);
    const result = view.container.firstChild;
    expect(result.style.height).toBe('320px');
    expect(result.style.getPropertyValue('--analysis-result-height')).toBe('320px');
    fireEvent.click(screen.getByRole('tab', {name: 'Table'}));
    expect(view.container.firstChild).toBe(result);
    expect(result.style.height).toBe('320px');
    fireEvent.click(screen.getByRole('tab', {name: 'JSON'}));
    expect(result.style.height).toBe('320px');
});

it('passes the server-count refresh callback through to the query graph result', () => {
    const onRefreshGraphNums = jest.fn();
    render(<QueryResult queryStatus='success'
        queryResult={{graph_view: {vertices: [{id: 'existing'}], edges: []}}}
        onRefreshGraphNums={onRefreshGraphNums} />);
    fireEvent.click(screen.getByRole('button', {name: 'Refresh saved graph counts'}));
    expect(onRefreshGraphNums).toHaveBeenCalledTimes(1);
    expect(onRefreshGraphNums).toHaveBeenCalledWith();
});

it('bounds graph preview by unique exact IDs and retains only connected edges', () => {
    const vertices = Array.from({length: 1001}, (_, id) => ({id}));
    vertices.unshift({id: 9007199254740993n}, {id: 9007199254740993n});
    const preview = getBoundedGraphView({vertices, edges: [
        {id: 'large', source: 9007199254740993n, target: 0},
        {id: 'outside', source: 0, target: 1000},
        {id: 'distinct-string', source: '0', target: 1},
    ]});
    expect(preview.vertices).toHaveLength(1000);
    expect(preview.vertices[0].id).toBe(9007199254740993n);
    expect(preview.edges.map(edge => edge.id)).toEqual(['large']);
});

it('stops projecting JSON before touching omitted rows and truncates large properties', () => {
    const rows = [{payload: 'x'.repeat(1000000)}, {id: 2}, {id: 3}];
    Object.defineProperty(rows, 3, {enumerable: true, get: () => {throw new Error('omitted row read');}});
    const preview = getBoundedJsonPreview(rows, {items: 3, characters: 1200});
    expect(preview.truncated).toBe(true);
    expect(preview.content[0].payload.length).toBeLessThanOrEqual(1001);
    expect(preview.content).toHaveLength(3);
});

it('omits oversized JSON keys before reading their values without renaming entries', () => {
    const read = jest.fn(() => {throw new Error('omitted property read');});
    const prefix = 'x'.repeat(1024 * 1024);
    const source = {before: 1};
    Object.defineProperty(source, `${prefix}a`, {enumerable: true, get: read});
    Object.defineProperty(source, `${prefix}b`, {enumerable: true, get: read});
    source.after = 2;
    const preview = getBoundedJsonPreview(source);
    expect(preview.content).toEqual({before: 1, after: 2});
    expect(preview.truncated).toBe(true);
    expect(read).not.toHaveBeenCalled();
    expect(JSON.stringify(preview.content).length).toBeLessThan(100);
    expect(Object.getOwnPropertyDescriptor(source, `${prefix}a`).get).toBe(read);
});

it('charges JSON keys to the remaining character budget before accessing their values', () => {
    const source = {ordinary: 1};
    const read = jest.fn(() => {throw new Error('over-budget property read');});
    Object.defineProperty(source, 'x'.repeat(100), {enumerable: true, get: read});
    const preview = getBoundedJsonPreview(source, {characters: 100});
    expect(preview.content).toEqual({ordinary: 1});
    expect(preview.truncated).toBe(true);
    expect(read).not.toHaveBeenCalled();
});

it('does not traverse raw JSON before its tab is selected for a graph result', () => {
    const read = jest.fn(() => 7);
    const raw = {};
    Object.defineProperty(raw, 'lazy', {enumerable: true, get: read});
    render(<QueryResult isQueryMode queryStatus='success' queryResult={{
        graph_view: {vertices: [{id: 1}], edges: []}, json_view: {data: raw},
    }} />);
    expect(read).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('tab', {name: 'JSON'}));
    expect(read).toHaveBeenCalledTimes(1);
});

it('shows quiet preview status with optional details instead of stacked alerts', () => {
    render(<QueryResult isQueryMode queryStatus='success' queryResult={{
        default_limit_applied: true, default_limit: 300, graph_truncated: true,
        graph_view: {vertices: [{id: 1}], edges: []},
    }} />);
    expect(screen.getByText(/analysis.query_result.default_limit_applied/)).toBeInTheDocument();
    expect(screen.getByText(/Graph limit/)).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'analysis.query_result.preview_details'})).toBeInTheDocument();
    expect(screen.getByText(/graph canvas/)).toBeInTheDocument();
});

it('persists read-only property preview markers through graph export and reload', () => {
    const source = {vertices: [{id: 1, label: 'person', properties: {payload: 'prefix… [preview 256/1000000 chars]'}}],
        edges: [{id: 'loop', source: 1, target: 1, properties: {payload: 'prefix…'}}]};
    const preview = getBoundedGraphView(source, 500, {
        vertexIds: [1], edgeIds: ['loop'],
    });
    expect(source.vertices[0]).not.toHaveProperty('~hubble_properties_preview');
    const saved = exportHubbleGraph(adaptHubbleGraph(preview));
    const restored = adaptHubbleGraph(saved);
    expect(saved.vertices[0]['~hubble_properties_preview']).toBe(true);
    expect(saved.edges[0]['~hubble_properties_preview']).toBe(true);
    expect(toElementDetails({kind: 'node', id: restored.nodes[0].id, data: restored.nodes[0].data})
        .raw['~hubble_properties_preview']).toBe(true);
});

it('marks only the truncated vertex in an ordinary 300-vertex query', () => {
    const vertices = Array.from({length: 300}, (_, id) => ({id, label: 'person', properties: {name: 'Alice'}}));
    vertices[143].properties.name = 'x'.repeat(256) + '… [preview 256/257 chars]';
    const preview = getBoundedGraphView({vertices, edges: []}, 500, {
        vertexIds: [143], edgeIds: [],
    });
    expect(preview.vertices.filter(vertex => vertex['~hubble_properties_preview'])).toEqual([
        expect.objectContaining({id: 143}),
    ]);
    expect(preview.vertices[0]).toBe(vertices[0]);
    expect(vertices[143]).not.toHaveProperty('~hubble_properties_preview');
});

it('matches every partial ID precisely and keeps vertices separate from edges', () => {
    const parsed = JSONbig.parse('{"vertices":[{"id":"9007199254740993"},{"id":"shared"},{"id":"7"}],'
        + '"vertexIds":["9007199254740993","7"]}');
    const preview = getBoundedGraphView({vertices: parsed.vertices,
        edges: [{id: 'shared', source: 'shared', target: '7'}]}, 500, {
        vertexIds: parsed.vertexIds, edgeIds: ['shared'],
    });
    expect(preview.vertices.map(vertex => !!vertex['~hubble_properties_preview'])).toEqual([true, false, true]);
    expect(preview.edges[0]['~hubble_properties_preview']).toBe(true);
});

it('keeps elements editable when no IDs are marked partial', () => {
    const source = {vertices: [{id: 1}], edges: [{id: 'loop', source: 1, target: 1}]};
    const complete = getBoundedGraphView(source, 500, {
        vertexIds: [], edgeIds: [],
    });
    expect(complete.vertices[0]).not.toHaveProperty('~hubble_properties_preview');
    expect(complete.edges[0]).not.toHaveProperty('~hubble_properties_preview');
});

it('shows the server property truncation notice alongside bounded result views', () => {
    render(<QueryResult isQueryMode queryStatus='success' queryResult={{
        properties_truncated: true, property_preview_limit: 256,
        graph_view: {vertices: [{id: 1}], edges: []},
    }} />);
    expect(screen.getByText('analysis.query_result.properties_preview_summary')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.queryByText('analysis.query_result.properties_preview')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'analysis.query_result.preview_details'}));
    expect(screen.getByText('analysis.query_result.properties_preview')).toBeInTheDocument();
});


it('keeps routine automatic limits as grey canvas text without alerts or extra details', () => {
    render(<QueryResult isQueryMode queryStatus='success' queryResult={{
        default_limit_applied: true, default_limit: 300,
        graph_view: {vertices: [{id: 1}], edges: []},
    }} />);
    expect(screen.getByText('analysis.query_result.default_limit_applied').parentElement).toHaveClass('previewStatus');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'analysis.query_result.preview_details'})).not.toBeInTheDocument();
});

it('scopes graph truncation details to the graph view', () => {
    render(<QueryResult isQueryMode queryStatus='success' queryResult={{
        graph_truncated: true,
        graph_view: {vertices: [{id: 1}], edges: []},
    }} />);
    expect(screen.getByText('Graph limit')).toBeVisible();
    fireEvent.click(screen.getByRole('tab', {name: 'Table'}));
    expect(screen.queryByText('Graph limit')).not.toBeVisible();
    expect(screen.getByText('table result')).toBeVisible();
    fireEvent.click(screen.getByRole('tab', {name: 'JSON'}));
    expect(screen.queryByText('Graph limit')).not.toBeVisible();
    expect(screen.getByText('json result')).toBeVisible();
});

it.each([
    {isQueryMode: true, queryStatus: 'failed'},
    {isQueryMode: true, queryStatus: 'loading'},
    {isQueryMode: false, queryStatus: 'success'},
    {isQueryMode: true, queryStatus: 'success', queryProgress: {phase: 'waiting'}},
])('hides stale preview status outside completed synchronous queries: %j', props => {
    render(<QueryResult {...props} queryResult={{
        properties_truncated: true, result_truncated: true, graph_truncated: true,
        default_limit_applied: true,
        graph_view: {vertices: [{id: 1}], edges: []},
    }} />);
    expect(screen.queryByText(/analysis.query_result.properties_preview_summary/)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'analysis.query_result.preview_details'})).not.toBeInTheDocument();
});
