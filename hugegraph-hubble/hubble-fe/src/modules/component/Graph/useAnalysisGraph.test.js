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
import {useAnalysisGraph} from './useAnalysisGraph';
import {adaptAnalysisGraph} from '../../../utils/analysisGraphData';
import * as api from '../../../api';

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
        mergeData: jest.fn(async () => ({status: 'applied', value: true})),
        updateElements: jest.fn(async () => ({status: 'applied'})),
        applyStyle: jest.fn(async () => ({status: 'applied'})),
        hide: jest.fn(async () => ({status: 'applied'})),
    };
};

beforeEach(() => jest.clearAllMocks());

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
    const next = {...createController(), getSnapshot: () => ({status: 'ready', data: final})};
    act(() => view.result.current.onReady(next));
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
    ]}));
    expect(message.warning).not.toHaveBeenCalled();
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
