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

import {StrictMode, useContext} from 'react';
import {act, render, screen} from '@testing-library/react';
import Graph from './index';
import {useAnalysisGraph} from './useAnalysisGraph';
import {GraphContext} from '../Context';
import {createGraphController} from '../../../visualization/runtime';
import {GraphController} from '../../../visualization/runtime/GraphController';
import {adaptAnalysisGraph, toCanvas3DData} from '../../../utils/analysisGraphData';
import {exportHubbleGraph} from '../../../visualization/hubble/adapter';

jest.mock('../../../visualization/runtime', () => ({createGraphController: jest.fn()}));
jest.mock('../../../api', () => ({analysis: {putExecutionQuery: jest.fn()}}));
jest.mock('antd', () => ({message: {error: jest.fn(), warning: jest.fn()}}));
jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));

const first = {vertices: [{id: 'old', label: 'person'}], edges: []};
const second = {vertices: [{id: 'new', label: 'person'}], edges: []};
let controllers;
let renders;
const makeController = () => {
    let snapshot = {status: 'idle', data: {nodes: [], edges: []},
        canUndo: false, canRedo: false, selectedIds: [], hoveredIds: []};
    const listeners = new Set();
    const publish = data => {
        snapshot = {...snapshot, status: 'ready', data};
        listeners.forEach(listener => listener());
    };
    return {getSnapshot: () => snapshot, getHiddenIds: () => [], listeners, publish,
        subscribe: listener => { listeners.add(listener); return () => listeners.delete(listener); },
        replaceData: jest.fn(async data => { publish(data); return {status: 'applied'}; }),
        resize: jest.fn(async () => ({status: 'applied'})),
        fitView: jest.fn(async () => ({status: 'applied'})),
        dispose: jest.fn(async () => ({status: 'applied'})),
    };
};
const Probe = () => {
    const {snapshot} = useContext(GraphContext);
    return <output data-testid='canvas-data'>{JSON.stringify(exportHubbleGraph(snapshot.data))}</output>;
};
const Host = ({graph, data, mode = '2d', queryStatus = 'success'}) => {
    const model = useAnalysisGraph({data, graphSpaceInfo: {graphSpace: 'DEFAULT', graph}, queryStatus});
    renders.push({graph, exported: exportHubbleGraph(model.currentData()), data: model.graphData});
    return <>
        <output data-testid='export-data'>{JSON.stringify(exportHubbleGraph(model.graphData))}</output>
        {mode === '2d' && <Graph key={model.scopeKey} data={model.initialData} datasetKey={model.datasetKey}
            onReady={model.onReady} layout={model.layout}><Probe /></Graph>}
    </>;
};
const flush = async () => { await act(async () => {}); };
beforeEach(() => {
    controllers = []; renders = [];
    createGraphController.mockImplementation(() => {
        const controller = makeController();
        controllers.push(controller);
        return controller;
    });
    global.ResizeObserver = class { observe() {} disconnect() {} };
});

test('real hook, wrapper and canvas replace the scope owner and preserve only the new scope snapshot across 3D', async () => {
    const view = render(<Host graph='one' data={first} />);
    await flush();
    const old = controllers[0];
    expect(screen.getByTestId('canvas-data')).toHaveTextContent('old');
    view.rerender(<Host graph='two' data={first} queryStatus='loading' />);
    await flush();
    expect(controllers).toHaveLength(2);
    expect(old.dispose).toHaveBeenCalledTimes(1);
    expect(old.listeners.size).toBe(0);
    expect(screen.getByTestId('canvas-data')).not.toHaveTextContent('old');
    expect(screen.getByTestId('export-data')).not.toHaveTextContent('old');
    for (const observed of renders.filter(item => item.graph === 'two')) {
        expect(observed.exported.vertices).toEqual([]);
        expect(observed.data.nodes).toEqual([]);
    }
    view.rerender(<Host graph='two' data={second} />);
    await flush();
    const next = controllers[1];
    const final = adaptAnalysisGraph({vertices: [...second.vertices, {id: 'expanded', label: 'person'}], edges: []});
    final.nodes[0].style = {...final.nodes[0].style, x: 31, y: 57, fill: '#f00'};
    final.nodes[0].data.fixed = true;
    act(() => next.publish(final));
    expect(screen.getByTestId('export-data')).toHaveTextContent('expanded');
    view.rerender(<Host graph='two' data={second} mode='3d' />);
    await flush();
    expect(next.dispose).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId('export-data')).toHaveTextContent('expanded');
    view.rerender(<Host graph='two' data={second} />);
    await flush();
    expect(controllers).toHaveLength(3);
    expect(controllers[2].replaceData).toHaveBeenCalledWith(final, expect.any(Object));
    expect(controllers[2].getSnapshot().data.nodes[0]).toMatchObject({
        data: {fixed: true}, style: {x: 31, y: 57, fill: '#f00'},
    });
});

test('StrictMode effect replay establishes a live controller instead of retaining a disposed owner', async () => {
    const view = render(<StrictMode><Host graph='one' data={first} /></StrictMode>);
    await flush();
    const last = controllers.at(-1);
    expect(controllers.length).toBeGreaterThan(1);
    expect(last.dispose).not.toHaveBeenCalled();
    expect(screen.getByTestId('canvas-data')).toHaveTextContent('old');
    expect(screen.getByTestId('export-data')).toHaveTextContent('old');
    view.unmount();
    expect(last.dispose).toHaveBeenCalledTimes(1);
    expect(last.listeners.size).toBe(0);
});

test.each([false, true])('real controller restores independent masks after 3D (manual overlap: %s)', async manual => {
    const copy = value => JSON.parse(JSON.stringify(value));
    createGraphController.mockImplementation(() => {
        let data = {nodes: [], edges: []};
        const noop = async () => {};
        const runtime = {
            onEvent() {}, idleInteraction: noop,
            replace: async next => { data = copy(next); },
            read: () => copy(data),
            visibility: async (ids, visible) => {
                [...data.nodes, ...data.edges].forEach(item => {
                    if (ids.includes(item.id)) item.style = {...item.style, visibility: visible ? 'visible' : 'hidden'};
                });
            },
            states: noop, refreshLabelVisibility: noop, fitView: noop, resize() {},
            history: noop, historyState: () => ({canUndo: false, canRedo: false}),
            stop() {}, destroy() {},
        };
        const controller = new GraphController(runtime);
        controllers.push(controller);
        return controller;
    });
    const raw = {vertices: [{id: 'keep', label: 'person'}, {id: 'hidden', label: 'person'}],
        edges: [{id: 'link', source: 'keep', target: 'hidden', label: 'knows'}]};
    const filter = {filter: {rules: {vertex: [{op_type: 'id', rule: 'eq', value: 'keep'}], edge: []}, logic: 'and'}};
    let model;
    const FilterHost = ({input = raw, mode = '2d', graph = 'one'}) => {
        model = useAnalysisGraph({data: input, graphSpaceInfo: {graphSpace: 'DEFAULT', graph},
            queryStatus: 'success'});
        return mode === '2d' && <Graph key={model.scopeKey} data={model.initialData} datasetKey={model.datasetKey}
            onReady={model.onReady} layout={model.layout} />;
    };
    const hiddenIds = () => [...model.graphData.nodes, ...model.graphData.edges]
        .filter(item => item.style?.visibility === 'hidden').map(item => item.data.raw.id);
    const view = render(<FilterHost />);
    await flush();
    await act(async () => { await model.filter(filter); });
    expect(hiddenIds()).toEqual(['hidden', 'link']);
    if (manual) await act(async () => {
        await model.controller.hide(['node:string:hidden', 'edge:string:link']);
    });
    view.rerender(<FilterHost mode='3d' />);
    await flush();
    expect(hiddenIds()).toEqual(['hidden', 'link']);
    expect(toCanvas3DData(model.graphData).nodes.map(node => node.rawId)).toEqual(['keep']);
    expect(toCanvas3DData(model.graphData).edges).toEqual([]);
    view.rerender(<FilterHost />);
    await flush();
    expect(controllers).toHaveLength(2);
    expect(hiddenIds()).toEqual(['hidden', 'link']);
    await act(async () => { await model.filter(filter); });
    expect(hiddenIds()).toEqual(['hidden', 'link']);
    await act(async () => { await model.filter({}); });
    if (manual) {
        expect(hiddenIds()).toEqual(['hidden', 'link']);
        await act(async () => { await model.controller.hide(['node:string:hidden'], false); });
        expect(hiddenIds()).toEqual(['link']);
        await act(async () => { await model.controller.hide(['edge:string:link'], false); });
    }
    expect(hiddenIds()).toEqual([]);
    await act(async () => { await model.filter(filter); });
    expect(hiddenIds()).toEqual(['hidden', 'link']);
    const newQuery = copy(raw);
    view.rerender(<FilterHost input={newQuery} />);
    await flush();
    expect(hiddenIds()).toEqual([]);
    await act(async () => { await model.filter(filter); });
    view.rerender(<FilterHost input={newQuery} mode='3d' />);
    await flush();
    expect(hiddenIds()).toEqual(['hidden', 'link']);
    // A new query while the 2D owner is absent must discard the retained masks.
    const refreshed = copy(raw);
    view.rerender(<FilterHost input={refreshed} mode='3d' />);
    await flush();
    expect(hiddenIds()).toEqual([]);
    view.rerender(<FilterHost input={refreshed} />);
    await flush();
    expect(hiddenIds()).toEqual([]);
    await act(async () => { await model.filter(filter); });
    view.rerender(<FilterHost input={refreshed} mode='3d' />);
    await flush();
    const otherScope = copy(raw);
    view.rerender(<FilterHost input={otherScope} mode='3d' graph='two' />);
    await flush();
    expect(hiddenIds()).toEqual([]);
    view.rerender(<FilterHost input={otherScope} graph='two' />);
    await flush();
    expect(hiddenIds()).toEqual([]);
});

test.each([false, true])('a new query owns the 3D handoff even while its replacement is queued: %s', async hold => {
    const old = {vertices: [{id: 'shared', label: 'person', properties: {name: 'old'}}], edges: []};
    const fresh = {vertices: [{id: 'shared', label: 'person', properties: {name: 'new'}}], edges: []};
    const copy = value => JSON.parse(JSON.stringify(value));
    let model;
    let blocked = false;
    let release;
    const barrier = new Promise(resolve => { release = resolve; });
    createGraphController.mockImplementation(() => {
        let data = {nodes: [], edges: []};
        const noop = async () => {};
        const controller = new GraphController({
            onEvent() {}, idleInteraction: async () => { if (blocked) await barrier; },
            replace: async next => { data = copy(next); }, read: () => copy(data),
            visibility: async (ids, visible) => {
                data.nodes.forEach(node => {
                    if (ids.includes(node.id)) node.style = {...node.style, visibility: visible ? 'visible' : 'hidden'};
                });
            },
            states: noop, refreshLabelVisibility: noop, fitView: noop, resize() {},
            history: noop, historyState: () => ({canUndo: false, canRedo: false}),
            stop: () => release(), destroy() {},
        });
        controllers.push(controller);
        return controller;
    });
    const QueryHost = ({input, mode = '2d'}) => {
        model = useAnalysisGraph({data: input, graphSpaceInfo: {graphSpace: 'DEFAULT', graph: 'one'},
            queryStatus: 'success'});
        return mode === '2d' && <Graph data={model.initialData} datasetKey={model.datasetKey}
            onReady={model.onReady} layout={model.layout} />;
    };
    const view = render(<QueryHost input={old} />);
    await flush();
    const controller = controllers[0];
    await act(async () => { await controller.hide(['node:string:shared']); });
    blocked = hold;
    view.rerender(<QueryHost input={fresh} />);
    await flush();
    expect(model.currentRawData()).toEqual(fresh);
    expect(controller.getSnapshot().data.nodes[0].data.raw.properties.name).toBe(hold ? 'old' : 'new');
    view.rerender(<QueryHost input={fresh} mode='3d' />);
    await flush();
    try {
        expect(model.currentRawData()).toEqual(fresh);
        expect(model.graphData.nodes[0].style?.visibility).not.toBe('hidden');
        view.rerender(<QueryHost input={fresh} />);
        await flush();
        expect(model.currentRawData()).toEqual(fresh);
        expect(model.graphData.nodes[0].style?.visibility).not.toBe('hidden');
    }
    finally { release(); view.unmount(); await Promise.all(controllers.map(owner => owner.dispose())); }
});
