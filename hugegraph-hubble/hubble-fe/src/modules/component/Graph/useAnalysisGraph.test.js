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

import {act, renderHook} from '@testing-library/react';
import {message} from 'antd';
import JSONbig from 'json-bigint';
import {mergeGraphData} from '../../../visualization/model/data';
import {GraphController} from '../../../visualization/runtime/GraphController';
import {projectHubbleGraph} from '../../../visualization/hubble/adapter';
import {useAnalysisGraph} from './useAnalysisGraph';
import {adaptAnalysisGraph} from '../../../utils/analysisGraphData';
import * as api from '../../../api';
import {ALGORITHM_NAME} from '../../../utils/constants';
import {hubbleRenderId} from '../../../visualization/hubble/adapter';

const mockEmptySnapshot = {status: 'idle', data: {nodes: [], edges: []}};
jest.mock('../../../visualization/react', () => ({
    useGraphSnapshot: controller => controller?.getSnapshot() ?? mockEmptySnapshot,
}));
jest.mock('../../../api', () => ({analysis: {putExecutionQuery: jest.fn()}}));
jest.mock('antd', () => ({message: {error: jest.fn(), warning: jest.fn()}}));
jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
const raw = {vertices: [{id: 0, label: 'person'}, {id: '0', label: 'person'}], edges: []};
const initial = {data: raw, graphSpaceInfo: {graphSpace: 'DEFAULT', graph: 'one'}, queryStatus: 'success'};
const deferred = () => {
    let resolve;
    const promise = new Promise(done => { resolve = done; });
    return {promise, resolve};
};
const createController = () => {
    const data = adaptAnalysisGraph(raw);
    return {
        getSnapshot: () => ({status: 'ready', data}),
        getHiddenIds: () => [],
        mergeData: jest.fn(async () => ({status: 'applied', value: true})),
        updateElements: jest.fn(async () => ({status: 'applied'})),
        applyStyle: jest.fn(async () => ({status: 'applied'})),
        hide: jest.fn(async () => ({status: 'applied'})),
        setHiddenIds: jest.fn(async () => ({status: 'applied'})),
    };
};

beforeEach(() => jest.clearAllMocks());

test('retains one editable layout draft while projecting only engine options', () => {
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: initial});
    const draft = {type: 'd3-force', options: {nodeSize: 70, nodeSpacing: 40,
        preventOverlap: false, linkDistance: 225, nodeStrength: 180, uiOnly: 'not an engine option'}};
    act(() => view.result.current.changeLayout(draft));
    expect(view.result.current.layoutSettings).toEqual(draft);
    expect(view.result.current.layout).toEqual({type: 'd3-force', options: {
        collide: false, link: {distance: 225}, manyBody: {strength: -180},
    }});
    draft.options.nodeSize = 999;
    expect(view.result.current.layoutSettings.options.nodeSize).toBe(70);
    act(() => view.result.current.changeLayout({...view.result.current.layoutSettings,
        options: {...view.result.current.layoutSettings.options, preventOverlap: true}}));
    expect(view.result.current.layoutSettings.options).toMatchObject({nodeSize: 70, nodeSpacing: 40});
    expect(view.result.current.layout.options.collide).toEqual({radius: 55});
});

test('a stale layout callback cannot apply edits after leaving and returning to its graph', () => {
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: initial});
    const staleChange = view.result.current.changeLayout;
    view.rerender({...initial, graphSpaceInfo: {graphSpace: 'OTHER', graph: 'two'}});
    view.rerender(initial);
    const settings = view.result.current.layoutSettings;
    act(() => staleChange({type: 'concentric', options: {startAngle: 1}}));
    expect(view.result.current.layoutSettings).toBe(settings);
});

test('keeps the existing immutable input while a new query loads and resets at a new result', () => {
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: initial});
    const first = view.result.current.initialData;
    const revision = view.result.current.datasetKey;
    view.rerender({...initial, data: {vertices: [], edges: []}, queryStatus: 'loading'});
    expect(view.result.current.initialData).toBe(first);
    view.rerender({...initial, data: {...raw}});
    expect(view.result.current.datasetKey).toBe(revision + 1);
    expect(view.result.current.initialData.nodes).toHaveLength(2);
});

test('clears prior graph input on context switching even while the next query loads', () => {
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: initial});
    view.rerender({...initial, graphSpaceInfo: {graphSpace: 'OTHER', graph: 'two'}, queryStatus: 'loading'});
    expect(view.result.current.initialData).toEqual({nodes: [], edges: []});
});

test('never exposes the previous controller data during a scope render, including before effects', () => {
    const observations = [];
    const view = renderHook(props => {
        const result = useAnalysisGraph(props);
        observations.push({scope: props.graphSpaceInfo.graph, data: result.graphData,
            exported: result.currentData(), controller: result.controller});
        return result;
    }, {initialProps: initial});
    act(() => view.result.current.onReady(createController()));
    view.rerender({...initial, graphSpaceInfo: {graphSpace: 'OTHER', graph: 'two'}, queryStatus: 'loading'});
    const switched = observations.filter(item => item.scope === 'two');
    expect(switched.length).toBeGreaterThan(0);
    for (const item of switched) {
        expect(item.data).toEqual({nodes: [], edges: []});
        expect(item.exported).toEqual({nodes: [], edges: []});
        expect(item.controller).toBeNull();
    }
});

test('accepts a fresh result arriving with its new scope and quarantines unchanged prior input', () => {
    const second = {...initial, graphSpaceInfo: {graphSpace: 'OTHER', graph: 'two'}};
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: initial});
    view.rerender({...second, queryStatus: 'loading'});
    view.rerender({...second, metaData: {vertexMeta: []}});
    expect(view.result.current.graphData).toEqual({nodes: [], edges: []});
    const data = {vertices: [{id: 'new', label: 'person'}], edges: []};
    view.rerender({...initial, graphSpaceInfo: {graphSpace: 'THIRD', graph: 'three'}, data});
    expect(view.result.current.initialData.nodes.map(node => node.data.raw.id)).toEqual(['new']);
});

test('old scope callbacks cannot acquire the next owner, even after returning to the same graph', async () => {
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: initial});
    act(() => view.result.current.onReady(createController()));
    const old = view.result.current;
    view.rerender({...initial, graphSpaceInfo: {graphSpace: 'OTHER', graph: 'two'}, queryStatus: 'loading'});
    const next = createController();
    act(() => view.result.current.onReady(next));
    await act(async () => {
        await old.expand({vertex_id: 0});
        await old.addElements(raw);
        await old.changeSettings([]);
    });
    act(() => old.onReady(null));
    expect(view.result.current.controller).toBe(next);
    expect(old.currentData()).toEqual({nodes: [], edges: []});
    view.rerender({...initial, data: {...raw}});
    const returned = createController();
    act(() => view.result.current.onReady(returned));
    await act(async () => { await old.expand({vertex_id: 0}); await old.addElements(raw); });
    expect(api.analysis.putExecutionQuery).not.toHaveBeenCalled();
    expect(next.updateElements).not.toHaveBeenCalled();
    expect(next.applyStyle).not.toHaveBeenCalled();
    expect(returned.updateElements).not.toHaveBeenCalled();
});

test('the new scope preserves its controller-only changes when switching to the independent renderer', () => {
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: initial});
    act(() => view.result.current.onReady(createController()));
    view.rerender({...initial, graphSpaceInfo: {graphSpace: 'OTHER', graph: 'two'}, data: {...raw}});
    const final = adaptAnalysisGraph({vertices: [{id: 'new', label: 'person'}], edges: []});
    final.nodes[0].style = {x: 31, y: 57, fill: '#f00'};
    final.nodes[0].data.fixed = true;
    const next = {...createController(), getSnapshot: jest.fn(() => ({status: 'ready', data: adaptAnalysisGraph(raw)}))};
    act(() => view.result.current.onReady(next));
    // Commit the new query before simulating subsequent controller-only edits.
    next.getSnapshot.mockReturnValue({status: 'ready', data: final});
    act(() => view.result.current.onReady(null));
    expect(view.result.current.initialData).toBe(final);
    expect(view.result.current.currentData()).toBe(final);
});

test('expansion accepts an edge-only response and sends the raw business query unchanged', async () => {
    api.analysis.putExecutionQuery.mockResolvedValue({status: 200, data: {graph_view: {
        vertices: [], edges: [{id: 'edge', source: 0, target: '0', label: 'knows'}],
    }}});
    const owner = createController();
    const view = renderHook(() => useAnalysisGraph(initial));
    act(() => view.result.current.onReady(owner));
    const params = {vertex_id: 0, vertex_label: 'person'};
    await act(async () => { await view.result.current.expand(params); });
    expect(api.analysis.putExecutionQuery).toHaveBeenCalledWith('DEFAULT', 'one', params);
    expect(owner.mergeData).toHaveBeenCalledWith(expect.objectContaining({nodes: [], edges: [
        expect.objectContaining({source: 'node:number:0', target: 'node:string:0'}),
    ]}), {preserveAppearance: true});
    expect(message.warning).not.toHaveBeenCalled();
});

test('expansion marks only partial vertex and edge properties before merging', async () => {
    const responseGraph = {vertices: [
        {id: 'partial', label: 'person', properties: {name: 'prefix… [preview 256/300 chars]'}},
        {id: 'complete', label: 'person', properties: {name: 'Alice'}},
    ], edges: [
        {id: 'partial-edge', source: 0, target: 'partial', properties: {name: 'prefix'}},
        {id: 'complete-edge', source: 0, target: 'complete', properties: {weight: 1}},
    ]};
    api.analysis.putExecutionQuery.mockResolvedValue({status: 200, data: {
        graph_view: responseGraph, properties_truncated: true,
        property_preview_vertex_ids: ['partial'], property_preview_edge_ids: ['partial-edge'],
    }});
    const owner = createController();
    const view = renderHook(() => useAnalysisGraph(initial));
    act(() => view.result.current.onReady(owner));

    await act(async () => { await view.result.current.expand({vertex_id: 0, vertex_label: 'person'}); });

    const merged = owner.mergeData.mock.calls[0][0];
    expect(merged.nodes[0].data.raw).toMatchObject({'~hubble_properties_preview': true,
        properties: responseGraph.vertices[0].properties});
    expect(merged.edges[0].data.raw['~hubble_properties_preview']).toBe(true);
    expect(merged.nodes[1].data.raw['~hubble_properties_preview']).toBeUndefined();
    expect(merged.edges[1].data.raw['~hubble_properties_preview']).toBeUndefined();
    expect(responseGraph.vertices[0]['~hubble_properties_preview']).toBeUndefined();
    expect(responseGraph.edges[0]['~hubble_properties_preview']).toBeUndefined();
});

test('an expansion response from an earlier result never reaches the controller', async () => {
    const pending = deferred();
    api.analysis.putExecutionQuery.mockReturnValue(pending.promise);
    const owner = createController();
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: initial});
    act(() => view.result.current.onReady(owner));
    let request;
    act(() => { request = view.result.current.expand({vertex_id: 0}); });
    view.rerender({...initial, data: {...raw}});
    await act(async () => {
        pending.resolve({status: 200, data: {graph_view: raw}});
        await request;
    });
    expect(owner.mergeData).not.toHaveBeenCalled();
    expect(message.error).not.toHaveBeenCalled();
});

test('remote edits preserve IDs and properties while clearing backend-unsafe local history', async () => {
    const owner = createController();
    const view = renderHook(() => useAnalysisGraph(initial));
    act(() => view.result.current.onReady(owner));
    await act(async () => { await view.result.current.editElement('nodes', 'node:number:0', {properties: {name: 'updated'}}); });
    expect(owner.updateElements).toHaveBeenCalledWith(expect.objectContaining({nodes: [
        expect.objectContaining({id: 'node:number:0', data: expect.objectContaining({raw: {
            id: 0, label: 'person', properties: {name: 'updated'},
        }})}),
    ], edges: []}));
    expect(owner.applyStyle).not.toHaveBeenCalled();
});

test('reports active local write-apply failures to the submitting drawer without a false success or duplicate message', async () => {
    const owner = createController();
    const error = new Error('canvas apply failed after server commit');
    owner.updateElements.mockRejectedValueOnce(error);
    const view = renderHook(() => useAnalysisGraph(initial));
    act(() => view.result.current.onReady(owner));
    await act(async () => { await expect(view.result.current.addElements(raw)).rejects.toBe(error); });
    expect(message.error).not.toHaveBeenCalled();
    // Other toolbar commands still handle their own errors; they have no drawer.
    owner.applyStyle.mockRejectedValueOnce(new Error('toolbar failed'));
    await act(async () => { await view.result.current.changeSettings([]); });
    expect(message.error).toHaveBeenCalledWith('toolbar failed');
});

test('a late local write-apply failure cannot notify or write through a new scope', async () => {
    let reject;
    const pending = new Promise((_, fail) => { reject = fail; });
    const owner = createController();
    owner.updateElements.mockReturnValueOnce(pending);
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: initial});
    act(() => view.result.current.onReady(owner));
    let request;
    act(() => { request = view.result.current.addElements(raw); });
    view.rerender({...initial, graphSpaceInfo: {graphSpace: 'OTHER', graph: 'two'}});
    const next = createController();
    act(() => view.result.current.onReady(next));
    await act(async () => { reject(new Error('old owner failed')); await expect(request).resolves.toBeUndefined(); });
    expect(next.updateElements).not.toHaveBeenCalled();
    expect(message.error).not.toHaveBeenCalled();
});

test('a late applied result cannot increment totals in a new scope', async () => {
    const pending = deferred();
    const owner = createController();
    owner.updateElements.mockReturnValueOnce(pending.promise);
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: initial});
    act(() => view.result.current.onReady(owner));
    let request;
    act(() => { request = view.result.current.addElements(raw); });
    view.rerender({...initial, graphSpaceInfo: {graphSpace: 'OTHER', graph: 'two'}});
    const next = createController();
    act(() => view.result.current.onReady(next));
    await act(async () => {
        pending.resolve({status: 'applied'});
        await expect(request).resolves.toBeUndefined();
    });
    expect(next.updateElements).not.toHaveBeenCalled();
    expect(message.error).not.toHaveBeenCalled();
});

test('canvas settings submit style patches without feeding snapshot data back through props', async () => {
    const owner = createController();
    const view = renderHook(() => useAnalysisGraph(initial));
    const initialInput = view.result.current.initialData;
    act(() => view.result.current.onReady(owner));
    const patches = [{kind: 'node', id: 'node:number:0', style: {fill: '#f00'}}];
    await act(async () => { await view.result.current.changeSettings(patches); });
    expect(owner.applyStyle).toHaveBeenCalledWith(patches);
    expect(view.result.current.initialData).toBe(initialInput);
});

test('releasing a canvas preserves its final snapshot for the independent renderer', () => {
    const owner = createController();
    const view = renderHook(() => useAnalysisGraph(initial));
    act(() => view.result.current.onReady(owner));
    const final = owner.getSnapshot().data;
    act(() => view.result.current.onReady(null));
    expect(view.result.current.initialData).toBe(final);
    expect(view.result.current.controller).toBeNull();
});

test('releasing the previous graph cannot restore its snapshot after a context switch', () => {
    const owner = createController();
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: initial});
    act(() => view.result.current.onReady(owner));
    view.rerender({...initial, graphSpaceInfo: {graphSpace: 'OTHER', graph: 'two'}, queryStatus: 'loading'});
    act(() => view.result.current.onReady(null));
    expect(view.result.current.initialData).toEqual({nodes: [], edges: []});
    expect(view.result.current.controller).toBeNull();
});


const observableController = input => {
    let data = adaptAnalysisGraph(projectHubbleGraph(input).graph);
    const listeners = new Set();
    let selectedIds = [];
    const publish = next => { data = next; listeners.forEach(listener => listener()); };
    return {
        ...createController(),
        getSnapshot: () => ({status: 'ready', data, selectedIds}),
        select: ids => { selectedIds = ids; listeners.forEach(listener => listener()); },
        subscribe: listener => { listeners.add(listener); return () => listeners.delete(listener); },
        publish,
        mergeData: jest.fn(async next => {
            const merged = mergeGraphData(data, next);
            publish(merged.data);
            return {status: 'applied', value: merged.changed};
        }),
        replaceData: jest.fn(async next => { publish(next); return {status: 'applied'}; }),
    };
};
const partial = {vertices: [{id: 1, label: 'person'}], edges: [
    {id: 'waiting', source: 1, target: 'later', label: 'knows', properties: {kept: true}},
]};

test('partial expansions preserve complete records and complete responses upgrade partial typed IDs', async () => {
    const complete = {vertices: [
        {id: 1, label: 'person', properties: {name: 'Alice'}},
        {id: '1', label: 'person', properties: {name: 'old prefix'}, '~hubble_properties_preview': true},
    ], edges: [
        {id: 7, source: 1, target: '1', label: 'knows', properties: {weight: 42}},
        {id: '7', source: '1', target: 1, label: 'knows', properties: {weight: 'old prefix'},
            '~hubble_properties_preview': true},
    ]};
    const responseGraph = {vertices: [
        {id: 1, label: 'person', properties: {name: 'prefix'}},
        {id: '1', label: 'person', properties: {name: 'Bob'}},
    ], edges: [
        {id: 7, source: 1, target: '1', label: 'knows', properties: {weight: 'prefix'}},
        {id: '7', source: '1', target: 1, label: 'knows', properties: {weight: 99}},
    ]};
    api.analysis.putExecutionQuery.mockResolvedValue({status: 200, data: {
        graph_view: responseGraph, property_preview_vertex_ids: [1], property_preview_edge_ids: [7],
    }});
    const owner = observableController(complete);
    const view = renderHook(() => useAnalysisGraph({...initial, data: complete}));
    act(() => view.result.current.onReady(owner));

    await act(async () => { await view.result.current.expand({vertex_id: 1}); });

    const exported = view.result.current.currentRawData();
    expect(exported.vertices.find(vertex => vertex.id === 1)).toEqual(complete.vertices[0]);
    expect(exported.edges.find(edge => edge.id === 7)).toEqual(complete.edges[0]);
    expect(exported.vertices.find(vertex => vertex.id === '1')).toEqual(responseGraph.vertices[1]);
    expect(exported.edges.find(edge => edge.id === '7')).toEqual(responseGraph.edges[1]);
    expect(exported.vertices.every(vertex => !vertex['~hubble_properties_preview'])).toBe(true);
    expect(exported.edges.every(edge => !edge['~hubble_properties_preview'])).toBe(true);
    expect(responseGraph.vertices[0]['~hubble_properties_preview']).toBeUndefined();
});

test('renders the valid partial graph while preserving detached dangling raw edges in JSON exports', () => {
    const input = JSONbig.parse('{"vertices":[{"id":1},{"id":"1"},{"id":900719925474099312345}],'
        + '"edges":[{"id":1,"source":1,"target":"1"},{"id":"1","source":1,"target":"later"},'
        + '{"id":900719925474099312345,"source":900719925474099312345,"target":2}]}');
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: {...initial, data: input}});
    expect(view.result.current.graphData.nodes).toHaveLength(3);
    expect(view.result.current.graphData.edges).toHaveLength(1);
    expect(view.result.current.currentRawData()).toEqual(input);
    input.edges[1].target = 'mutated';
    expect(view.result.current.currentRawData().edges[1].target).toBe('later');
    expect(view.result.current.currentRawData().edges[2].id.toFixed()).toBe('900719925474099312345');
    // Metadata updates also traverse projection rather than throwing on missing endpoints.
    view.rerender({...initial, data: input, metaData: {vertexMeta: []}});
    expect(view.result.current.graphData.edges).toHaveLength(1);
    expect(view.result.current.currentRawData().edges[1].target).toBe('later');
});

test('retains missing edges through an incomplete expansion and promotes them only when the endpoint arrives', async () => {
    const owner = observableController(partial);
    const view = renderHook(() => useAnalysisGraph({...initial, data: partial}));
    act(() => view.result.current.onReady(owner));
    api.analysis.putExecutionQuery.mockResolvedValueOnce({status: 200, data: {graph_view: {
        vertices: [], edges: [{id: 'another', source: 1, target: 'future'}],
    }}});
    await act(async () => { await view.result.current.expand({vertex_id: 1}); });
    expect(owner.getSnapshot().data.edges).toHaveLength(0);
    expect(view.result.current.currentRawData().edges.map(edge => edge.id)).toEqual(['waiting', 'another']);
    api.analysis.putExecutionQuery.mockResolvedValueOnce({status: 200, data: {graph_view: {
        vertices: [{id: 'later', label: 'person'}], edges: [],
    }}});
    await act(async () => { await view.result.current.expand({vertex_id: 1}); });
    expect(owner.getSnapshot().data.edges.map(edge => edge.data.raw.id)).toEqual(['waiting']);
    expect(view.result.current.currentRawData().edges.map(edge => edge.id)).toEqual(['waiting', 'another']);
    expect(owner.mergeData).toHaveBeenLastCalledWith(expect.objectContaining({edges: [
        expect.objectContaining({id: 'edge:string:waiting'}),
    ]}), {preserveAppearance: true});
});

test('expansion retains a partial edge marker through pending export and endpoint promotion', async () => {
    const owner = observableController(partial);
    const view = renderHook(() => useAnalysisGraph({...initial, data: partial}));
    act(() => view.result.current.onReady(owner));
    api.analysis.putExecutionQuery.mockResolvedValueOnce({status: 200, data: {graph_view: {
        vertices: [], edges: [{id: 'partial-edge', source: 1, target: 'future', properties: {name: 'prefix'}}],
    }, property_preview_vertex_ids: [], property_preview_edge_ids: ['partial-edge']}});
    await act(async () => { await view.result.current.expand({vertex_id: 1}); });
    expect(view.result.current.currentRawData().edges.find(edge => edge.id === 'partial-edge'))
        .toMatchObject({'~hubble_properties_preview': true});

    api.analysis.putExecutionQuery.mockResolvedValueOnce({status: 200, data: {graph_view: {
        vertices: [{id: 'future', label: 'person', properties: {name: 'Alice'}}], edges: [],
    }}});
    await act(async () => { await view.result.current.expand({vertex_id: 1}); });
    expect(owner.getSnapshot().data.edges.find(edge => edge.data.raw.id === 'partial-edge').data.raw)
        .toMatchObject({'~hubble_properties_preview': true});
    const complete = owner.getSnapshot().data.nodes.find(node => node.data.raw.id === 'future').data.raw;
    expect(complete['~hubble_properties_preview']).toBeUndefined();
});

test('does not resurrect a pending edge after its known endpoint was deleted and then supplied again', async () => {
    const owner = observableController(partial);
    const view = renderHook(() => useAnalysisGraph({...initial, data: partial}));
    act(() => view.result.current.onReady(owner));
    // Both publications can occur before React paints: the subscription still observes the deletion.
    act(() => { owner.publish({nodes: [], edges: []}); owner.publish(adaptAnalysisGraph({vertices: [{id: 1}, {id: 'later'}], edges: []})); });
    api.analysis.putExecutionQuery.mockResolvedValueOnce({status: 200, data: {graph_view: {vertices: [], edges: []}}});
    await act(async () => { await view.result.current.expand({vertex_id: 1}); });
    expect(view.result.current.currentRawData().edges).toEqual([]);
    expect(owner.getSnapshot().data.edges).toEqual([]);
});

test('explicitly clears a pending-only result and invalidates its in-flight expansion', async () => {
    const input = {vertices: [], edges: [{id: 'only', source: 1, target: 2}]};
    const owner = observableController(input);
    const view = renderHook(() => useAnalysisGraph({...initial, data: input}));
    act(() => view.result.current.onReady(owner));
    expect(view.result.current.currentRawData().edges).toEqual(input.edges);
    const pending = deferred();
    api.analysis.putExecutionQuery.mockReturnValueOnce(pending.promise);
    let request;
    act(() => { request = view.result.current.expand({vertex_id: 1}); });
    await act(async () => { await view.result.current.clearGraph(); });
    expect(view.result.current.currentRawData()).toEqual({vertices: [], edges: []});
    await act(async () => {
        pending.resolve({status: 200, data: {graph_view: {vertices: [{id: 1}, {id: 2}], edges: input.edges}}});
        await request;
    });
    expect(owner.mergeData).not.toHaveBeenCalled();
    expect(owner.replaceData).toHaveBeenCalledWith({nodes: [], edges: []}, {newDataset: true});
    expect(view.result.current.currentRawData()).toEqual({vertices: [], edges: []});
});

test('pending exports do not survive a new result or scope A-B-A and stale callbacks remain empty', () => {
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: {...initial, data: partial}});
    const oldExport = view.result.current.currentRawData;
    view.rerender({...initial, graphSpaceInfo: {graphSpace: 'OTHER', graph: 'two'}, data: partial});
    expect(view.result.current.currentRawData()).toEqual({vertices: [], edges: []});
    expect(oldExport()).toEqual({vertices: [], edges: []});
    view.rerender({...initial, data: raw});
    expect(view.result.current.currentRawData()).toEqual(raw);
    expect(oldExport()).toEqual({vertices: [], edges: []});
    view.rerender({...initial, data: partial});
    expect(view.result.current.currentRawData().edges).toEqual(partial.edges);
    view.rerender({...initial, data: {vertices: [], edges: []}});
    expect(view.result.current.currentRawData()).toEqual({vertices: [], edges: []});
});

test('replaces the complete filter-owned hidden set on repeat and clears only that cause', async () => {
    const owner = createController();
    const view = renderHook(() => useAnalysisGraph(initial));
    act(() => view.result.current.onReady(owner));
    await act(async () => { await view.result.current.filter({filter: {rules: {vertex: [
        {op_type: 'id', value: 0, rule: 'eq', data_type: 'TEXT'},
    ], edge: []}, logic: 'and'}}); await view.result.current.filter({filter: {rules: {vertex: [
        {op_type: 'id', value: 0, rule: 'eq', data_type: 'TEXT'},
    ], edge: []}, logic: 'and'}}); await view.result.current.filter(); });
    expect(owner.setHiddenIds.mock.calls).toEqual([['filter', ['node:string:0']],
        ['filter', ['node:string:0']], ['filter', []]]);
    expect(owner.hide).not.toHaveBeenCalled();
});


test('a new pending-only result is not pruned against the preceding nonempty ready snapshot', () => {
    const owner = observableController(raw);
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: initial});
    act(() => view.result.current.onReady(owner));
    const next = {vertices: [], edges: [{id: 'next', source: 0, target: 'missing'}]};
    view.rerender({...initial, data: next});
    // GraphCanvas has not yet committed its queued replace.
    expect(view.result.current.currentRawData()).toEqual(next);
    act(() => owner.publish({nodes: [], edges: []}));
    expect(view.result.current.currentRawData()).toEqual(next);
});


test('exports complete input while a newly mounted controller is idle', () => {
    const input = {...partial, edges: [...partial.edges, {id: 'loop', source: 1, target: 1}]};
    const owner = {...createController(), getSnapshot: () => mockEmptySnapshot};
    const view = renderHook(() => useAnalysisGraph({...initial, data: input}));
    act(() => view.result.current.onReady(owner));
    expect(view.result.current.currentData()).toBe(view.result.current.initialData);
    expect(view.result.current.currentRawData()).toEqual({...input, edges: [input.edges[1], input.edges[0]]});
});


test('a failed clear retains pending raw records without allowing its earlier expansion to revive', async () => {
    const owner = observableController(partial);
    owner.replaceData.mockRejectedValueOnce(new Error('clear failed'));
    const view = renderHook(() => useAnalysisGraph({...initial, data: partial}));
    act(() => view.result.current.onReady(owner));
    const pending = deferred();
    api.analysis.putExecutionQuery.mockReturnValueOnce(pending.promise);
    let request;
    act(() => { request = view.result.current.expand({vertex_id: 1}); });
    await act(async () => { await view.result.current.clearGraph(); });
    expect(view.result.current.currentRawData()).toEqual(partial);
    expect(message.error).toHaveBeenCalledWith('clear failed');
    await act(async () => {
        pending.resolve({status: 200, data: {graph_view: {vertices: [{id: 'later'}], edges: []}}});
        await request;
    });
    expect(owner.mergeData).not.toHaveBeenCalled();
    expect(view.result.current.currentRawData()).toEqual(partial);
});


test('an old renderer publication cannot prune the replacement renderer pending projection', () => {
    const first = observableController(partial);
    const next = observableController(partial);
    const view = renderHook(() => useAnalysisGraph({...initial, data: partial}));
    act(() => view.result.current.onReady(first));
    act(() => view.result.current.onReady(null));
    act(() => view.result.current.onReady(next));
    act(() => first.publish({nodes: [], edges: []}));
    expect(view.result.current.currentRawData()).toEqual(partial);
});


test('a metadata refresh preserves current edits and deletions rather than replaying original query records', async () => {
    const input = {vertices: [{id: 1}, {id: 'kept'}], edges: [
        {id: 'waiting', source: 1, target: 'later'},
    ]};
    const owner = observableController(input);
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: {...initial, data: input}});
    act(() => view.result.current.onReady(owner));
    act(() => owner.publish(adaptAnalysisGraph({vertices: [{id: 'kept', properties: {edited: true}}], edges: []})));
    view.rerender({...initial, data: input, metaData: {vertexMeta: [{name: 'person', style: {color: '#f00'}}]}});
    expect(view.result.current.initialData.nodes.map(node => node.data.raw))
        .toEqual([{id: 'kept', properties: {edited: true}}]);
    expect(view.result.current.currentRawData().edges).toEqual([]);
    api.analysis.putExecutionQuery.mockResolvedValueOnce({status: 200, data: {graph_view: {
        vertices: [{id: 1}, {id: 'later'}], edges: [],
    }}});
    await act(async () => { await view.result.current.expand({vertex_id: 'kept'}); });
    expect(owner.getSnapshot().data.edges).toEqual([]);
});


test('does not silently discard repeated edge records within a neighbor response', async () => {
    const owner = observableController(partial);
    const view = renderHook(() => useAnalysisGraph({...initial, data: partial}));
    act(() => view.result.current.onReady(owner));
    api.analysis.putExecutionQuery.mockResolvedValueOnce({status: 200, data: {graph_view: {
        vertices: [], edges: [{id: 'duplicate', source: 1, target: 'a'}, {id: 'duplicate', source: 1, target: 'b'}],
    }}});
    await act(async () => { await view.result.current.expand({vertex_id: 1}); });
    expect(owner.mergeData).not.toHaveBeenCalled();
    expect(message.error).toHaveBeenCalledWith('Duplicate edge ID edge:string:duplicate');
    expect(view.result.current.currentRawData()).toEqual(partial);
});


test('exports only the new owned typed result while its canvas replacement is queued', () => {
    const old = {vertices: [{id: 0}, {id: '0'}], edges: [{id: 'old', source: 0, target: '0'}]};
    const owner = observableController(old);
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: {...initial, data: old}});
    act(() => view.result.current.onReady(owner));
    const next = JSONbig.parse('{"vertices":[{"id":1,"properties":{"name":"new numeric"}},'
        + '{"id":"1","properties":{"name":"new string"}},{"id":900719925474099312345}],'
        + '"edges":[{"id":1,"source":1,"target":"1","properties":{"new":true}},'
        + '{"id":"1","source":900719925474099312345,"target":"missing","extra":"pending"}]}');
    view.rerender({...initial, data: next});
    // The live controller still holds the old graph until its queued replacement commits.
    expect(owner.getSnapshot().data.edges[0].data.raw.id).toBe('old');
    const queuedExport = view.result.current.currentRawData();
    expect(queuedExport).toEqual(next);
    expect(queuedExport.vertices.map(vertex => typeof vertex.id)).toEqual(['number', 'string', 'object']);
    expect(queuedExport.vertices[2].id.toFixed()).toBe('900719925474099312345');
    expect(queuedExport.edges[1].source.toFixed()).toBe('900719925474099312345');
    act(() => owner.publish(adaptAnalysisGraph(projectHubbleGraph(next).graph)));
    expect(view.result.current.currentRawData()).toEqual(next);
});


test.each([true, false])('same node IDs do not activate an old business result (pending edges: %s)', pendingEdge => {
    const old = JSONbig.parse('{"vertices":[{"id":1,"properties":{"name":"old numeric"}},'
        + '{"id":"1","properties":{"name":"old string"}},{"id":900719925474099312345,"version":"old"}],'
        + '"edges":[{"id":"old-edge","source":1,"target":"1","properties":{"version":"old"}}]}');
    const next = JSONbig.parse('{"vertices":[{"id":1,"properties":{"name":"new numeric"}},'
        + '{"id":"1","properties":{"name":"new string"}},{"id":900719925474099312345,"version":"new"}],'
        + '"edges":[{"id":1,"source":"1","target":900719925474099312345,"properties":{"version":"new"}},'
        + '{"id":"1","source":1,"target":1,"properties":{"version":"new loop"}}]}');
    if (pendingEdge) next.edges.push({id: 'pending', source: next.vertices[2].id, target: 'missing', extra: 'new pending'});
    const owner = observableController(old);
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: {...initial, data: old}});
    act(() => view.result.current.onReady(owner));
    view.rerender({...initial, data: next});
    // Neither a selection-only publication nor a layout/style publication commits the new query data.
    act(() => {
        owner.select(['node:number:1']);
        const previous = owner.getSnapshot().data;
        owner.publish({...previous, nodes: previous.nodes.map(node => ({...node, style: {x: 99, fill: '#f00'}}))});
    });
    const queued = view.result.current.currentRawData();
    expect(queued).toEqual(next);
    expect(queued.vertices[2].id.toFixed()).toBe('900719925474099312345');
    expect(queued.edges[0].id).toBe(1);
    expect(queued.edges[1].id).toBe('1');
    // Engine order, positions, selection and display style are independent of business projection identity.
    const projection = adaptAnalysisGraph(projectHubbleGraph(next).graph);
    const committed = {nodes: [...projection.nodes].reverse().map(node => ({...node,
        data: {...node.data, fixed: true}, style: {x: 23, y: 57, fill: '#0f0'}})),
    edges: [...projection.edges].reverse().map(edge => ({...edge, style: {stroke: '#00f'}}))};
    act(() => owner.publish(committed));
    expect(view.result.current.currentRawData()).toEqual({vertices: [...next.vertices].reverse(),
        edges: [...next.edges.slice(0, 2)].reverse().concat(pendingEdge ? [next.edges[2]] : [])});
    // Once committed, exports follow the current snapshot rather than remaining stuck on query input.
    const edited = {...committed, nodes: committed.nodes.map((node, index) => index ? node
        : {...node, data: {...node.data, raw: {...node.data.raw, updated: true}}})};
    act(() => owner.publish(edited));
    expect(view.result.current.currentRawData().vertices[0]).toEqual({...next.vertices[2], updated: true});
});

const handoffRuntime = hold => {
    let data = {nodes: [], edges: []};
    const entered = deferred();
    return {
        entered: entered.promise,
        onEvent: jest.fn(), idleInteraction: async () => {},
        replace: async next => { entered.resolve(); if (hold) await hold.promise; data = next; },
        read: () => data,
        history: async () => {}, historyState: () => ({canUndo: false, canRedo: false}),
        fitView: async () => {}, refreshLabelVisibility: async () => {},
        visibility: async (ids, visible) => {
            const apply = item => ids.includes(item.id)
                ? {...item, style: {...item.style, visibility: visible ? 'visible' : 'hidden'}} : item;
            data = {nodes: data.nodes.map(apply), edges: data.edges.map(apply)};
        },
        stop: jest.fn(), destroy: jest.fn(),
    };
};

test.each(['idle', 'updating'])('a rapid renderer handoff preserves data and masks before first adoption (%s)', async phase => {
    const held = deferred();
    const first = new GraphController(handoffRuntime());
    const runtime = handoffRuntime(held);
    const remounted = new GraphController(runtime);
    const final = new GraphController(handoffRuntime());
    const view = renderHook(() => useAnalysisGraph(initial));
    let replacement;
    let restoration;
    try {
        await first.replaceData(view.result.current.initialData);
        act(() => view.result.current.onReady(first));
        await act(async () => {
            await first.hide(['node:number:0']);
            await first.setHiddenIds('filter', ['node:string:0']);
        });
        act(() => view.result.current.onReady(null));
        act(() => {
            replacement = remounted.replaceData(view.result.current.initialData);
            restoration = view.result.current.onReady(remounted);
        });
        if (phase === 'updating') await act(async () => { await runtime.entered; });
        expect(remounted.getSnapshot().status).toBe(phase);
        expect(remounted.getSnapshot().data.nodes).toHaveLength(0);
        expect(view.result.current.currentRawData()).toEqual(raw);
        expect(view.result.current.graphData.nodes).toHaveLength(2);
        act(() => view.result.current.onReady(null));
        expect(view.result.current.initialData.nodes).toHaveLength(2);
        expect(view.result.current.currentRawData()).toEqual(raw);
        const nextReplacement = final.replaceData(view.result.current.initialData);
        await act(async () => {
            const nextRestoration = view.result.current.onReady(final);
            await Promise.all([nextReplacement, nextRestoration]);
        });
        expect(final.getSnapshot().data.nodes).toHaveLength(2);
        expect(final.getHiddenIds()).toEqual(['node:number:0']);
        expect(final.getHiddenIds('filter')).toEqual(['node:string:0']);
    } finally {
        held.resolve();
        await Promise.allSettled([replacement, restoration]);
        view.unmount();
        await Promise.all([first.dispose(), remounted.dispose(), final.dispose()]);
    }
});

test('label choices survive renderer release and reset for each new dataset and scope', () => {
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: initial});
    const choices = view.result.current.labelFields;
    choices.current.node.set('person', [['person', 'name']]);
    act(() => view.result.current.onReady(createController()));
    act(() => view.result.current.onReady(null));
    act(() => view.result.current.onReady(createController()));
    expect(view.result.current.labelFields).toBe(choices);
    view.rerender({...initial, metaData: {vertexMeta: []}});
    expect(view.result.current.labelFields).toBe(choices);
    view.rerender({...initial, data: {...raw}});
    expect(view.result.current.labelFields).not.toBe(choices);
    expect(view.result.current.labelFields.current.node.size).toBe(0);
    const second = view.result.current.labelFields;
    view.rerender({...initial, graphSpaceInfo: {graphSpace: 'OTHER', graph: 'two'}});
    expect(view.result.current.labelFields).not.toBe(second);
    expect(view.result.current.labelFields.current.node.size).toBe(0);
});

test('an adopted controller retains local edits even while a later command is updating', () => {
    const owner = {...createController(), getSnapshot: jest.fn(createController().getSnapshot)};
    const view = renderHook(() => useAnalysisGraph(initial));
    act(() => view.result.current.onReady(owner));
    const edited = adaptAnalysisGraph({vertices: [{id: 0, label: 'person', properties: {name: 'edited'}}], edges: []});
    owner.getSnapshot.mockReturnValue({status: 'updating', data: edited});
    expect(view.result.current.currentRawData().vertices).toEqual([{id: 0, label: 'person', properties: {name: 'edited'}}]);
    act(() => view.result.current.onReady(null));
    expect(view.result.current.initialData).toBe(edited);
});

test('shares the existing algorithm default and only resets the active layout when its original inputs change', () => {
    const first = {...initial, algorithmName: ALGORITHM_NAME.K_OUT, options: {startId: 0}};
    const view = renderHook(props => useAnalysisGraph(props), {initialProps: first});
    const original = view.result.current.defaultLayout;
    expect(original).toEqual({type: 'radial', options: {focusNode: hubbleRenderId('node', 0)}});
    expect(view.result.current.layoutSettings).toEqual(original);
    act(() => view.result.current.changeLayout({type: 'grid', options: {rows: 3}}));
    const custom = view.result.current.layoutSettings;
    view.rerender({...first, options: {startId: 0, unrelated: true}, metaData: {vertexMeta: []}});
    expect(view.result.current.defaultLayout).toBe(original);
    expect(view.result.current.layoutSettings).toBe(custom);
    view.rerender({...first, options: {startId: '0'}});
    expect(view.result.current.defaultLayout).toEqual({type: 'radial', options: {focusNode: hubbleRenderId('node', '0')}});
    expect(view.result.current.layoutSettings).toEqual(view.result.current.defaultLayout);
    view.rerender({...first, algorithmName: ALGORITHM_NAME.CUSTOMIZEDPATHS});
    expect(view.result.current.defaultLayout).toEqual({type: 'force-atlas2'});
    expect(view.result.current.layoutSettings).toEqual({type: 'force-atlas2'});
});
