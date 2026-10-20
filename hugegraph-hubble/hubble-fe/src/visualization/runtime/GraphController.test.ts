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

import {GraphController} from './GraphController';
import type {GraphData, GraphNode, GraphEdge} from '../model/types';
import type {GraphLayout, GraphRuntime, RuntimeEvent} from './types';

const node = (id: string, x = 0): GraphNode => ({id, data: {raw: {id, properties: {name: id}}}, style: {x, y: 10}});
const input = (): GraphData => ({nodes: [node('a', 15), node('b', 90), node('unrelated', 180)],
    edges: [{id: 'ab', source: 'a', target: 'b', data: {raw: {id: 'ab', source: 'a', target: 'b'}}}]});
const copy = (data: GraphData): GraphData => ({
    nodes: data.nodes.map(item => ({...item, data: {...item.data}, style: {...item.style}})),
    edges: data.edges.map(item => ({...item, data: {...item.data}, style: {...item.style}})),
});
const deferred = () => {
    let resolve!: () => void;
    const promise = new Promise<void>(done => { resolve = done; });
    return {promise, resolve};
};
const createRuntime = () => {
    let data: GraphData = {nodes: [], edges: []};
    let eventHandler: (event: RuntimeEvent) => void = () => {};
    function readElement(kind: 'node', id: string): GraphNode;
    function readElement(kind: 'edge', id: string): GraphEdge;
    function readElement(kind: 'node' | 'edge', id: string): GraphNode | GraphEdge {
        const item = (kind === 'node' ? data.nodes : data.edges).find(element => element.id === id);
        if (!item) throw new Error('Unknown element');
        return {...item, data: {...item.data}, style: {...item.style}};
    }
    const runtime = {
        readElement,
        idleInteraction: jest.fn(async () => {}),
        onEvent: jest.fn((handler: (event: RuntimeEvent) => void) => { eventHandler = handler; }),
        replace: jest.fn(async (next: GraphData, _layout: GraphLayout | null) => { data = copy(next); }),
        patch: jest.fn(async (patch: Partial<GraphData>, _record: boolean) => {
            const nodes = new Map(data.nodes.map(item => [item.id, item]));
            const edges = new Map(data.edges.map(item => [item.id, item]));
            patch.nodes?.forEach(item => nodes.set(item.id, {...item, data: {...item.data}, style: {...item.style}}));
            patch.edges?.forEach(item => edges.set(item.id, {...item, data: {...item.data}, style: {...item.style}}));
            data = {nodes: [...nodes.values()], edges: [...edges.values()]};
        }),
        layout: jest.fn(async (_layout: GraphLayout) => {}),
        remove: jest.fn(async (ids: {nodes: string[]; edges: string[]}) => {
            data = {nodes: data.nodes.filter(item => !ids.nodes.includes(item.id)),
                edges: data.edges.filter(item => !ids.edges.includes(item.id))};
        }),
        read: jest.fn(() => copy(data)),
        setMinimap: jest.fn((_visible: boolean) => {}),
        states: jest.fn(async (_states: Record<string, string[]>) => {}),
        refreshLabelVisibility: jest.fn(async (_itemCount: number) => {}),
        visibility: jest.fn(async (ids: string[], visible: boolean) => {
            const apply = <T extends GraphNode | GraphEdge>(item: T): T => ids.includes(item.id)
                ? {...item, style: {...item.style, visibility: visible ? 'visible' : 'hidden'}} : item;
            data = {nodes: data.nodes.map(apply), edges: data.edges.map(apply)};
        }),
        fitView: jest.fn(async () => {}), zoomBy: jest.fn(async (_factor: number) => {}),
        resize: jest.fn((_width: number, _height: number) => {}), exportPng: jest.fn(async () => 'data:image/png;base64,ok'),
        history: jest.fn(async (_action: 'undo' | 'redo' | 'clear') => {}),
        historyState: jest.fn(() => ({canUndo: false, canRedo: false})),
        syncPins: jest.fn(), releasePins: jest.fn((_ids: string[]) => {}), stop: jest.fn(), destroy: jest.fn(),
    } satisfies GraphRuntime;
    return {runtime, emit: (event: RuntimeEvent) => eventHandler(event), setData: (next: GraphData) => { data = copy(next); }};
};

test('same-result refresh keeps positions and pins; style updates combine without another layout', async () => {
    const {runtime, setData} = createRuntime();
    const controller = new GraphController(runtime, {preset: 'interactive'});
    await controller.replaceData(input());
    const moved = runtime.read(); moved.nodes[0].style = {x: 250, y: 80}; moved.nodes[0].data.fixed = true;
    setData(moved);
    await controller.replaceData(input(), {newDataset: false});
    expect(runtime.replace.mock.calls[1][1]).toBeNull();
    expect(controller.getSnapshot().data.nodes[0]).toMatchObject({style: {x: 250, y: 80}, data: {fixed: true}});
    const first = controller.applyStyle([{kind: 'node', id: 'a', style: {fill: '#123456'}}]);
    const second = controller.applyStyle([{kind: 'node', id: 'b', style: {size: 60}}]);
    const third = controller.applyStyle([{kind: 'node', id: 'a', style: {lineWidth: 3}}]);
    await Promise.all([first, second, third]);
    expect(runtime.patch).toHaveBeenCalledTimes(1);
    expect(runtime.patch.mock.calls[0][0].nodes).toHaveLength(2);
    expect(controller.getSnapshot().data.nodes[0].style).toEqual({x: 250, y: 80, fill: '#123456', lineWidth: 3});
    expect(runtime.layout).not.toHaveBeenCalled();
    expect(runtime.history.mock.calls).toEqual([['clear']]);
    await controller.dispose();
});

test('layout is a style batching barrier; a new dataset uses the latest pending layout exactly once', async () => {
    const {runtime} = createRuntime();
    const controller = new GraphController(runtime);
    const obsolete = controller.setLayout({type: 'circular'});
    const latest = controller.setLayout({type: 'grid'});
    await controller.replaceData(input());
    expect((await obsolete).status).toBe('superseded');
    expect((await latest).status).toBe('superseded');
    expect(runtime.replace.mock.calls[0][1]).toEqual({type: 'grid'});
    expect(runtime.layout).not.toHaveBeenCalled();
    const style1 = controller.applyStyle([{id: 'a', kind: 'node', style: {size: 40}}]);
    const layout = controller.setLayout({type: 'dagre'});
    const style2 = controller.applyStyle([{id: 'a', kind: 'node', style: {fill: '#abc'}}]);
    await Promise.all([style1, layout, style2]);
    expect(runtime.patch).toHaveBeenCalledTimes(2);
    expect(runtime.layout).toHaveBeenCalledTimes(1);
    await controller.dispose();
});

test('layout changes fit their final positions only after the layout finishes', async () => {
    const {runtime} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    runtime.fitView.mockClear();
    const started = deferred(); const finish = deferred();
    runtime.layout.mockImplementationOnce(async () => {started.resolve(); await finish.promise;});
    const update = controller.setLayout({type: 'd3-force'});
    await started.promise;
    expect(runtime.fitView).not.toHaveBeenCalled();
    finish.resolve();
    await update;
    expect(runtime.fitView).toHaveBeenCalledTimes(1);
    expect(controller.getSnapshot().status).toBe('ready');
    await controller.dispose();
});

test('a newer layout request prevents the older layout from fitting the camera', async () => {
    const {runtime} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    runtime.fitView.mockClear();
    const firstStarted = deferred(); const firstFinish = deferred();
    const nextStarted = deferred(); const nextFinish = deferred();
    runtime.layout.mockImplementationOnce(async () => {firstStarted.resolve(); await firstFinish.promise;})
        .mockImplementationOnce(async () => {nextStarted.resolve(); await nextFinish.promise;});
    const first = controller.setLayout({type: 'radial'});
    await firstStarted.promise;
    const next = controller.setLayout({type: 'd3-force'});
    firstFinish.resolve();
    await nextStarted.promise;
    expect(runtime.fitView).not.toHaveBeenCalled();
    nextFinish.resolve();
    await Promise.all([first, next]);
    expect(runtime.fitView).toHaveBeenCalledTimes(1);
    await controller.dispose();
});

test('an obsolete in-flight layout cannot fit the replacement dataset', async () => {
    const {runtime} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    runtime.fitView.mockClear();
    const started = deferred(); const finish = deferred();
    runtime.layout.mockImplementationOnce(async () => {started.resolve(); await finish.promise;});
    const old = controller.setLayout({type: 'grid'});
    await started.promise;
    const replacement = controller.replaceData(input(), {fit: false});
    finish.resolve();
    await Promise.all([old, replacement]);
    expect(runtime.fitView).not.toHaveBeenCalled();
    await controller.dispose();
});

test('edge-only expansion is committed and duplicate expansion is a no-op', async () => {
    const {runtime} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    await controller.pin(['a'], true);
    const partial = {nodes: [], edges: [{id: 'ba', source: 'b', target: 'a', data: {raw: {id: 'ba'}}}]};
    expect(await controller.mergeData(partial)).toEqual({status: 'applied', value: true});
    expect(controller.getSnapshot().data.edges.map(item => item.id)).toEqual(['ab', 'ba']);
    expect(controller.getSnapshot().data.nodes[0]).toMatchObject({data: {fixed: true}, style: {x: 15, y: 10}});
    expect(await controller.mergeData(partial)).toEqual({status: 'applied', value: false});
    expect(runtime.layout).toHaveBeenCalledTimes(1);
    await controller.dispose();
});

test('modifier clicks toggle selection while normal clicks and blank canvas reset it', async () => {
    const {runtime, emit} = createRuntime();
    const onClick = jest.fn();
    const controller = new GraphController(runtime, {onClick});
    await controller.replaceData(input());
    const click = async (id: string, additiveSelection = false) => {
        emit({type: 'click', kind: id === 'ab' ? 'edge' : 'node', id, additiveSelection});
        await controller.idle();
    };
    await click('a');
    await click('b', true);
    expect(controller.getSnapshot().selectedIds).toEqual(['a', 'b']);
    emit({type: 'leave'}); await controller.idle();
    emit({type: 'dragEnd'}); await controller.idle();
    expect(controller.getSnapshot().selectedIds).toEqual(['a', 'b']);
    await click('a', true);
    expect(controller.getSnapshot().selectedIds).toEqual(['b']);
    await click('ab', true);
    expect(controller.getSnapshot().selectedIds).toEqual(['b', 'ab']);
    await click('a');
    expect(controller.getSnapshot().selectedIds).toEqual(['a']);
    await click('a', true);
    expect(controller.getSnapshot().selectedIds).toEqual([]);
    await click('b');
    emit({type: 'canvasClick'}); await controller.idle();
    expect(controller.getSnapshot().selectedIds).toEqual([]);
    expect(controller.getSnapshot().canUndo).toBe(false);
    expect(onClick).toHaveBeenCalledTimes(7);
    expect(runtime.patch).not.toHaveBeenCalled();
    await controller.dispose();
});

test('hover touches the previous and next neighborhood, keeps selection, and never reads the full graph', async () => {
    const {runtime, emit} = createRuntime();
    const onClick = jest.fn();
    const controller = new GraphController(runtime, {onClick});
    await controller.replaceData(input());
    runtime.read.mockClear();
    emit({type: 'click', kind: 'node', id: 'a'});
    emit({type: 'hover', kind: 'node', id: 'b'});
    await controller.idle();
    expect(controller.getSnapshot().selectedIds).toEqual(['a']);
    expect(runtime.states.mock.calls.at(-1)?.[0]).toEqual({a: ['selected', 'active'], b: ['active', 'focused'], ab: ['active']});
    expect(runtime.read).not.toHaveBeenCalled();
    expect(onClick.mock.calls[0][0].data.raw.id).toBe('a');
    emit({type: 'leave', kind: 'node', id: 'b'});
    await controller.idle();
    expect(runtime.states.mock.calls.at(-1)?.[0]).toEqual({a: ['selected'], b: [], ab: []});
    expect(controller.getSnapshot().hoveredIds).toEqual([]);
    await controller.dispose();
});

test('high-degree hover focuses only its target; edge hover, legend and dataset boundaries clear old focus', async () => {
    const {runtime, emit} = createRuntime();
    const controller = new GraphController(runtime);
    const nodes = Array.from({length: 21}, (_, i) => ({id: String(i), data: {raw: {id: i}}}));
    const edges = nodes.slice(1).map(node => ({id: `e${node.id}`, source: '0', target: node.id,
        data: {raw: {id: `e${node.id}`}}}));
    await controller.replaceData({nodes, edges});
    runtime.read.mockClear();
    emit({type: 'hover', kind: 'node', id: '0'});
    await controller.idle();
    const neighborhood = runtime.states.mock.calls.at(-1)![0];
    expect(Object.keys(neighborhood)).toHaveLength(41);
    expect(Object.entries(neighborhood).filter(([, states]) => states.includes('focused'))).toEqual([
        ['0', ['active', 'focused']],
    ]);
    emit({type: 'hover', kind: 'edge', id: 'e1'});
    await controller.idle();
    const edgeFocus = runtime.states.mock.calls.at(-1)![0];
    expect(edgeFocus.e1).toEqual(['active', 'focused']);
    expect(edgeFocus['0']).toEqual(['active']);
    expect(edgeFocus.e2).toEqual([]);
    await controller.select(['1']);
    expect(runtime.states.mock.calls.at(-1)![0].e1).toContain('focused');
    await controller.highlight(nodes.map(node => node.id));
    expect(Object.values(runtime.states.mock.calls.at(-1)![0]).some(states => states.includes('focused'))).toBe(false);
    expect(controller.getSnapshot().selectedIds).toEqual(['1']);
    expect(runtime.read).not.toHaveBeenCalled();
    emit({type: 'hover', kind: 'node', id: '0'});
    await controller.idle();
    await controller.replaceData({nodes, edges});
    await controller.select(['1']);
    expect(runtime.states.mock.calls.at(-1)![0]).toEqual({'1': ['selected']});
    emit({type: 'hover', kind: 'node', id: '0'});
    await controller.idle();
    await controller.removeElements(['0']);
    await controller.highlight([]);
    expect(Object.values(runtime.states.mock.calls.at(-1)![0]).some(states => states.includes('focused'))).toBe(false);
    await controller.dispose();
});

test('viewport density checks coalesce in the update queue without graph reads or a new layout', async () => {
    const {runtime, emit} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    runtime.read.mockClear(); runtime.refreshLabelVisibility.mockClear();
    emit({type: 'viewport'}); emit({type: 'viewport'}); emit({type: 'viewport'});
    await controller.idle();
    expect(runtime.refreshLabelVisibility).toHaveBeenCalledTimes(1);
    expect(runtime.refreshLabelVisibility).toHaveBeenCalledWith(4);
    expect(runtime.read).not.toHaveBeenCalled();
    expect(runtime.layout).not.toHaveBeenCalled();
    expect(runtime.patch).not.toHaveBeenCalled();
    await controller.dispose();
    emit({type: 'viewport'});
    expect(runtime.refreshLabelVisibility).toHaveBeenCalledTimes(1);
});

test.each(['layout-leave', 'layout-canvasClick', 'replace-leave'] as const)(
    '%s clears stale hover after an in-flight update without restoring a previous dataset selection', async mode => {
        const {runtime, emit} = createRuntime();
        const controller = new GraphController(runtime);
        await controller.replaceData(input());
        await controller.select(['a']);
        emit({type: 'hover', kind: 'node', id: 'b'});
        await controller.idle();
        const started = deferred(); const finish = deferred();
        if (mode === 'replace-leave') {
            const replace = runtime.replace.getMockImplementation()!;
            runtime.replace.mockImplementationOnce(async (...args) => {
                started.resolve(); await finish.promise; await replace(...args);
            });
        }
        else runtime.layout.mockImplementationOnce(async () => {started.resolve(); await finish.promise;});
        const update = mode === 'replace-leave' ? controller.replaceData(input()) : controller.setLayout({type: 'grid'});
        await started.promise;
        expect(controller.getSnapshot().status).toBe('updating');
        emit({type: mode === 'layout-canvasClick' ? 'canvasClick' : 'leave'});
        finish.resolve();
        await update;
        await controller.idle();
        expect(controller.getSnapshot().hoveredIds).toEqual([]);
        expect(controller.getSnapshot().selectedIds).toEqual(mode === 'layout-leave' ? ['a'] : []);
        const states = runtime.states.mock.calls.at(-1)![0];
        expect(Object.values(states).flat()).not.toContain('active');
        expect(Object.values(states).flat()).not.toContain('focused');
        expect(Object.values(states).flat()).toEqual(mode === 'layout-leave' ? ['selected'] : []);
        await controller.dispose();
    });

test('invalid style fails before engine mutation, reports the error once, and the next command recovers', async () => {
    const {runtime} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    await expect(controller.applyStyle([{id: 'a', kind: 'node', style: {size: Number.NaN}}]))
        .rejects.toThrow('finite JSON');
    expect(runtime.patch).not.toHaveBeenCalled();
    expect(controller.getSnapshot().status).toBe('error');
    await controller.applyStyle([{id: 'a', kind: 'node', style: {size: 50}}]);
    expect(runtime.patch).toHaveBeenCalledTimes(1);
    expect(controller.getSnapshot().status).toBe('ready');
    await controller.dispose();
});

test('superseded renders cannot publish old data or fit the old result', async () => {
    const {runtime, setData} = createRuntime();
    const started = deferred(); const finish = deferred();
    runtime.replace.mockImplementationOnce(async next => { started.resolve(); await finish.promise; setData(next); });
    const controller = new GraphController(runtime);
    const published: string[] = [];
    controller.subscribe(() => { controller.getSnapshot().data.nodes.forEach(item => published.push(item.id)); });
    const old = controller.replaceData(input());
    await started.promise;
    const latest = controller.replaceData({nodes: [node('latest')], edges: []});
    finish.resolve();
    expect((await old).status).toBe('superseded');
    expect((await latest).status).toBe('applied');
    expect(published).not.toContain('a');
    expect(controller.getSnapshot().data.nodes[0].id).toBe('latest');
    expect(runtime.fitView).toHaveBeenCalledTimes(1);
    await controller.dispose();
});

test('waits for a drag before reading style input and disposes before an active draw can publish', async () => {
    const {runtime, setData, emit} = createRuntime();
    const onClick = jest.fn(); const onHover = jest.fn();
    const controller = new GraphController(runtime, {onClick, onHover});
    await controller.replaceData(input());
    const drag = deferred();
    runtime.idleInteraction.mockImplementationOnce(() => drag.promise);
    const style = controller.applyStyle([{kind: 'node', id: 'a', style: {fill: '#eee'}}]);
    await Promise.resolve();
    const moved = input(); moved.nodes[0].style = {x: 700, y: 350}; setData(moved);
    drag.resolve(); await style;
    expect(controller.getSnapshot().data.nodes[0].style).toEqual({x: 700, y: 350, fill: '#eee'});
    const draw = deferred(); const started = deferred();
    runtime.patch.mockImplementationOnce(async () => { started.resolve(); await draw.promise; });
    const active = controller.applyStyle([{kind: 'node', id: 'a', style: {size: 80}}]);
    await started.promise;
    const notified = jest.fn(); controller.subscribe(notified);
    const disposed = controller.dispose();
    expect(runtime.stop).toHaveBeenCalledTimes(1);
    expect(runtime.destroy).not.toHaveBeenCalled();
    emit({type: 'click', kind: 'node', id: 'a'});
    draw.resolve(); await disposed;
    expect((await active).status).toBe('disposed');
    expect(notified).not.toHaveBeenCalled();
    expect(onClick).not.toHaveBeenCalled();
    expect(runtime.destroy).toHaveBeenCalledTimes(1);
    expect(controller.dispose()).toBe(disposed);
});

test('remote record updates preserve pin/position and reset local history; deletion includes incident edges', async () => {
    const {runtime} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input()); await controller.pin(['a'], true);
    await controller.updateElements({nodes: [{...node('a'), data: {raw: {id: 'a', properties: {name: 'updated'}}}}]});
    expect(controller.getSnapshot().data.nodes[0]).toMatchObject({style: {x: 15, y: 10},
        data: {fixed: true, raw: {properties: {name: 'updated'}}}});
    expect(runtime.patch.mock.calls.at(-1)?.[1]).toBe(false);
    await controller.select(['a']);
    await controller.removeElements(['a']);
    expect(runtime.remove).toHaveBeenCalledWith({nodes: ['a'], edges: ['ab']});
    expect(controller.getSnapshot().data.nodes.map(item => item.id)).toEqual(['b', 'unrelated']);
    expect(controller.getSnapshot().selectedIds).toEqual([]);
    expect(runtime.history.mock.calls).toEqual([['clear'], ['clear'], ['clear']]);
    await controller.dispose();
});


test('node hiding includes incident edges and never reveals an explicitly hidden edge', async () => {
    const {runtime} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    await controller.hide(['a']);
    expect(runtime.visibility).toHaveBeenLastCalledWith(['a', 'ab'], false);
    await controller.hide(['ab']);
    await controller.hide(['a'], false);
    expect(runtime.visibility.mock.calls.slice(-2)).toEqual([[['ab'], false], [['a'], true]]);
    await controller.hide(['ab'], false);
    expect(runtime.visibility).toHaveBeenLastCalledWith(['ab'], true);
    await controller.dispose();
    expect(controller.getSnapshot().status).toBe('disposed');
});


test('a same-topology new dataset resets placement/pins and applies the selected layout', async () => {
    const {runtime, setData} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    const previous = runtime.read(); previous.nodes[0].style = {x: 800, y: 400}; previous.nodes[0].data.fixed = true;
    setData(previous);
    await controller.replaceData(input(), {newDataset: true});
    expect(runtime.replace.mock.calls.at(-1)?.[1]).toEqual({type: 'd3-force'});
    expect(controller.getSnapshot().data.nodes[0]).toMatchObject({style: {x: 15, y: 10}});
    expect(controller.getSnapshot().data.nodes[0].data.fixed).toBeUndefined();
    await controller.dispose();
});

test('a pending layout survives a same-topology refresh while unchanged layout refresh does not relayout', async () => {
    const {runtime} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    const layout = controller.setLayout({type: 'circular'});
    await controller.replaceData(input(), {newDataset: false});
    expect((await layout).status).toBe('superseded');
    expect(runtime.replace.mock.calls.at(-1)?.[1]).toEqual({type: 'circular'});
    await controller.replaceData(input(), {newDataset: false});
    expect(runtime.replace.mock.calls.at(-1)?.[1]).toBeNull();
    await controller.dispose();
});

test('hidden endpoints stay hidden for new edges from merge and same-dataset replacement', async () => {
    const {runtime} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    await controller.hide(['a']);
    const edge = {id: 'ac', source: 'a', target: 'unrelated', data: {raw: {id: 'ac'}}};
    await controller.mergeData({nodes: [], edges: [edge]});
    expect(runtime.patch.mock.calls.at(-1)?.[0]).toMatchObject({nodes: [], edges: [{id: 'ac', style: {visibility: 'hidden'}}]});
    const refreshed = input(); refreshed.edges.push({...edge, id: 'ac-fresh'});
    await controller.replaceData(refreshed, {newDataset: false});
    expect(runtime.replace.mock.calls.at(-1)?.[0].edges.at(-1)?.style?.visibility).toBe('hidden');
    await controller.dispose();
});

test('cascade deletion prunes selected and hovered incident edge IDs', async () => {
    const {runtime} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    await controller.select(['a', 'ab']); await controller.highlight(['ab', 'b']);
    await controller.removeElements(['a']);
    expect(controller.getSnapshot().selectedIds).toEqual([]);
    expect(controller.getSnapshot().hoveredIds).toEqual(['b']);
    await controller.dispose();
});

test('remote mutations preserve hidden nodes and hide newly added incident edges before drawing', async () => {
    const {runtime} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    await controller.hide(['a']);
    await controller.updateElements({
        nodes: [{...node('a'), style: {visibility: 'visible'}}],
        edges: [{id: 'ac-remote', source: 'a', target: 'unrelated', data: {raw: {id: 'ac-remote'}}}],
    });
    expect(runtime.patch.mock.calls.at(-1)?.[0]).toMatchObject({
        nodes: [{id: 'a', style: {visibility: 'hidden'}}],
        edges: [{id: 'ac-remote', style: {visibility: 'hidden'}}],
    });
    await controller.hide(['a'], false);
    expect(runtime.visibility.mock.calls.at(-1)).toEqual([expect.arrayContaining(['a', 'ab', 'ac-remote']), true]);
    expect(runtime.layout).not.toHaveBeenCalled();
    await controller.dispose();
});

test('hidden view state overrides node and edge style patches until explicitly shown', async () => {
    const {runtime} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    await controller.hide(['a']);
    await controller.applyStyle([
        {id: 'a', kind: 'node', style: {fill: '#f00', visibility: 'visible'}},
        {id: 'ab', kind: 'edge', style: {stroke: '#f00', visibility: 'visible'}},
    ]);
    expect(runtime.patch.mock.calls.at(-1)?.[0]).toMatchObject({
        nodes: [{id: 'a', style: {fill: '#f00', visibility: 'hidden'}}],
        edges: [{id: 'ab', style: {stroke: '#f00', visibility: 'hidden'}}],
    });
    await controller.hide(['a'], false);
    await controller.applyStyle([{id: 'a', kind: 'node', style: {visibility: 'hidden'}}]);
    expect(runtime.patch.mock.calls.at(-1)?.[0].nodes?.[0].style?.visibility).toBe('visible');
    expect(runtime.layout).not.toHaveBeenCalled();
    await controller.dispose();
});

test('undo and redo restore current view visibility without recording another command', async () => {
    const {runtime, setData} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    await controller.hide(['a']);
    runtime.history.mockImplementationOnce(async () => { setData(input()); });
    await controller.undo();
    expect(runtime.patch.mock.calls.at(-1)).toEqual([expect.objectContaining({
        nodes: [expect.objectContaining({id: 'a', style: expect.objectContaining({visibility: 'hidden'})})],
        edges: [expect.objectContaining({id: 'ab', style: expect.objectContaining({visibility: 'hidden'})})],
    }), false]);
    await controller.hide(['a'], false);
    const oldView = input();
    oldView.edges.push({id: 'ac', source: 'a', target: 'unrelated', data: {raw: {id: 'ac'}},
        style: {visibility: 'hidden'}});
    runtime.history.mockImplementationOnce(async () => { setData(oldView); });
    await controller.redo();
    expect(runtime.patch.mock.calls.at(-1)).toEqual([{nodes: [], edges: [expect.objectContaining({
        id: 'ac', style: {visibility: 'visible'},
    })]}, false]);
    expect(controller.getSnapshot().data.edges.at(-1)?.style?.visibility).toBe('visible');
    expect(runtime.layout).not.toHaveBeenCalled();
    await controller.dispose();
});

test('history without visibility differences adopts its existing full read once', async () => {
    const {runtime} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    runtime.read.mockClear();
    await controller.undo();
    expect(runtime.read).toHaveBeenCalledTimes(1);
    expect(runtime.patch).not.toHaveBeenCalled();
    runtime.read.mockClear();
    await controller.redo();
    expect(runtime.read).toHaveBeenCalledTimes(1);
    expect(runtime.patch).not.toHaveBeenCalled();
    await controller.dispose();
});

test('style reads and submits only changed elements, without a full graph read or topology validation', async () => {
    const {runtime} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    runtime.read.mockClear();
    const reads = jest.spyOn(runtime, 'readElement');
    await controller.applyStyle([{id: 'a', kind: 'node', style: {fill: '#123'}}]);
    expect(runtime.read).not.toHaveBeenCalled();
    expect(reads).toHaveBeenCalledTimes(2); // One fresh input, one committed result.
    expect(runtime.patch.mock.calls.at(-1)?.[0].nodes?.map(item => item.id)).toEqual(['a']);
    expect(runtime.syncPins).not.toHaveBeenCalled();
    await controller.dispose();
});

test('input, snapshot and event records cannot mutate controller truth or another instance', async () => {
    const {runtime, emit} = createRuntime(); const other = createRuntime();
    const clicked = jest.fn();
    const controller = new GraphController(runtime, {onClick: clicked});
    const second = new GraphController(other.runtime);
    const original = input();
    await Promise.all([controller.replaceData(original), second.replaceData(original)]);
    original.nodes[0].style!.x = 900;
    (original.nodes[0].data.raw.properties as Record<string, unknown>).name = 'caller changed';
    const snapshot = controller.getSnapshot();
    expect(Reflect.set(snapshot.data.nodes[0].style!, 'x', 700)).toBe(false);
    expect(Reflect.set(snapshot.data.nodes[0].data.raw.properties as object, 'name', 'snapshot changed')).toBe(false);
    emit({type: 'click', kind: 'node', id: 'a'}); await controller.idle();
    expect(Reflect.set(clicked.mock.calls[0][0].data.raw.properties, 'name', 'event changed')).toBe(false);
    expect(controller.getSnapshot().data.nodes[0]).toMatchObject({style: {x: 15}, data: {raw: {properties: {name: 'a'}}}});
    expect(second.getSnapshot().data.nodes[0].data.raw.properties).toEqual({name: 'a'});
    await Promise.all([controller.dispose(), second.dispose()]);
});

test('immutable ownership preserves json-bigint ID precision and its number prototype', async () => {
    const json = jest.requireActual('json-bigint') as (options: {alwaysParseAsBig: boolean}) => {
        parse(text: string): Record<string, unknown>; stringify(value: unknown): string;
    };
    const lossless = json({alwaysParseAsBig: true});
    const raw = lossless.parse('{"id":900719925474099312345,"properties":{"weight":900719925474099312346}}');
    const {runtime} = createRuntime(); const controller = new GraphController(runtime);
    await controller.replaceData({nodes: [{id: 'typed-id', data: {raw}}], edges: []});
    const copied = controller.getSnapshot().data.nodes[0].data.raw;
    expect(copied).not.toBe(raw);
    expect(lossless.stringify(copied)).toBe('{"id":900719925474099312345,"properties":{"weight":900719925474099312346}}');
    expect(Object.getPrototypeOf(copied.id)).toBe(Object.getPrototypeOf(raw.id));
    await controller.dispose();
});


test('async event callback failures report once and async error callbacks cannot create unhandled rejections', async () => {
    const {runtime, emit} = createRuntime();
    const failure = new Error('host click failed');
    const onError = jest.fn(async () => { throw new Error('host error handler failed'); });
    const logged = jest.spyOn(console, 'error').mockImplementation(() => {});
    const controller = new GraphController(runtime, {onClick: async () => { throw failure; }, onError});
    try {
        await controller.replaceData(input());
        emit({type: 'click', kind: 'node', id: 'a'});
        await controller.idle(); await Promise.resolve();
        expect(onError).toHaveBeenCalledTimes(1);
        expect(onError).toHaveBeenCalledWith(failure);
        expect(logged).toHaveBeenCalledTimes(1);
        expect(logged).toHaveBeenCalledWith('Graph error callback failed', expect.any(Error));
        expect(controller.getSnapshot().error).toBe(failure);
        await controller.dispose();
        emit({type: 'click', kind: 'node', id: 'a'});
        await Promise.resolve();
        expect(onError).toHaveBeenCalledTimes(1);
    }
    finally { logged.mockRestore(); await controller.dispose(); }
});

test.each(['leave', 'hover'] as const)('consecutive pointer intents waiting on a drag retain only final %s', async final => {
    const {runtime, emit} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    await controller.select(['unrelated']);
    runtime.states.mockClear();
    const drag = deferred(); const entered = deferred();
    runtime.idleInteraction.mockImplementationOnce(async () => { entered.resolve(); await drag.promise; });
    emit({type: 'hover', kind: 'node', id: 'a'});
    await entered.promise;
    emit({type: 'hover', kind: 'node', id: 'unrelated'});
    emit({type: final, ...(final === 'hover' ? {kind: 'node' as const, id: 'b'} : {})});
    drag.resolve();
    await controller.idle();
    expect(runtime.states).toHaveBeenCalledTimes(1);
    const states = runtime.states.mock.calls[0][0];
    expect(states.unrelated).toContain('selected');
    expect(controller.getSnapshot().selectedIds).toEqual(['unrelated']);
    if (final === 'leave') {
        expect(controller.getSnapshot().hoveredIds).toEqual([]);
        expect(states).toEqual({unrelated: ['selected']});
    }
    else {
        expect(states.b).toEqual(['active', 'focused']);
        expect(new Set(controller.getSnapshot().hoveredIds)).toEqual(new Set(['a', 'b', 'ab']));
    }
    await controller.dispose();
});

test('export between pointer intents preserves the previous hover before the export', async () => {
    const {runtime, emit} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    const order: string[] = [];
    runtime.states.mockImplementation(async states => { order.push(states.a?.includes('focused') ? 'a' : 'b'); });
    runtime.exportPng.mockImplementation(async () => { order.push('export'); return 'data:image/png;base64,ok'; });
    const drag = deferred(); const entered = deferred();
    runtime.idleInteraction.mockImplementationOnce(async () => { entered.resolve(); await drag.promise; });
    emit({type: 'hover', kind: 'node', id: 'a'});
    await entered.promise;
    const exported = controller.exportPng();
    emit({type: 'hover', kind: 'node', id: 'b'});
    drag.resolve();
    await exported; await controller.idle();
    expect(order).toEqual(['a', 'export', 'b']);
    await controller.dispose();
});

test('pointer movement during an engine draw still adopts its committed states before clearing', async () => {
    const {runtime, emit} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    const drawing = deferred(); const entered = deferred();
    runtime.states.mockImplementationOnce(async () => { entered.resolve(); await drawing.promise; });
    emit({type: 'hover', kind: 'node', id: 'a'});
    await entered.promise;
    emit({type: 'leave'});
    drawing.resolve(); await controller.idle();
    expect(runtime.states).toHaveBeenCalledTimes(2);
    expect(runtime.states.mock.calls[1][0]).toEqual({a: [], b: [], ab: []});
    expect(controller.getSnapshot().hoveredIds).toEqual([]);
    await controller.dispose();
});

test('failed hover draw can be retried by the next valid pointer intent', async () => {
    const {runtime, emit} = createRuntime(); const onError = jest.fn();
    const controller = new GraphController(runtime, {onError});
    await controller.replaceData(input());
    runtime.states.mockRejectedValueOnce(new Error('draw failed'));
    emit({type: 'hover', kind: 'node', id: 'a'});
    await controller.idle();
    expect(onError).toHaveBeenCalledTimes(1);
    emit({type: 'hover', kind: 'node', id: 'a'});
    await controller.idle();
    expect(runtime.states).toHaveBeenCalledTimes(2);
    expect(runtime.states.mock.calls[1][0].a).toEqual(['active', 'focused']);
    expect(new Set(controller.getSnapshot().hoveredIds)).toEqual(new Set(['a', 'b', 'ab']));
    await controller.dispose();
});

test.each(['replace', 'dispose'] as const)('waiting hover cannot publish across %s', async action => {
    const {runtime, emit} = createRuntime();
    const controller = new GraphController(runtime);
    await controller.replaceData(input());
    const drag = deferred(); const entered = deferred();
    runtime.idleInteraction.mockImplementationOnce(async () => { entered.resolve(); await drag.promise; });
    emit({type: 'hover', kind: 'node', id: 'a'});
    await entered.promise;
    const next = action === 'replace' ? controller.replaceData({nodes: [node('new')], edges: []}) : controller.dispose();
    drag.resolve(); await next; await controller.idle();
    expect(runtime.states).not.toHaveBeenCalled();
    expect(controller.getSnapshot().hoveredIds).toEqual([]);
    if (action === 'replace') expect(controller.getSnapshot().data.nodes.map(item => item.id)).toEqual(['new']);
    else expect(runtime.destroy).toHaveBeenCalledTimes(1);
    await controller.dispose();
});


test('expansion preserves the appearance committed by a preceding queued style command', async () => {
    const {runtime} = createRuntime();
    const controller = new GraphController(runtime);
    const initial = input(); initial.nodes[0].data = {...initial.nodes[0].data, fixed: true, shape: 'diamond', icon: 'star', group: 'saved'};
    await controller.replaceData(initial);
    const style = controller.applyStyle([{id: 'a', kind: 'node', style: {fill: 'red', size: 80}},
        {id: 'ab', kind: 'edge', style: {stroke: 'purple', lineWidth: 5}}]);
    const expansion = {nodes: [{...node('a'), data: {raw: {id: 'a', changed: true}, shape: 'circle', fixed: false},
        style: {fill: 'blue', size: 20}}, {...node('c'), style: {fill: 'green'}}], edges: [
        {id: 'ab', source: 'a', target: 'b', data: {raw: {id: 'ab', changed: true}}, style: {stroke: 'black'}},
        {id: 'ac', source: 'a', target: 'c', data: {raw: {id: 'ac'}}, style: {stroke: 'green'}},
    ]};
    const merged = controller.mergeData(expansion, {preserveAppearance: true});
    await Promise.all([style, merged]);
    expect(controller.getSnapshot().data.nodes[0]).toMatchObject({data: {fixed: true, shape: 'diamond', icon: 'star', group: 'saved',
        raw: {changed: true}}, style: {x: 15, y: 10, fill: 'red', size: 80}});
    expect(controller.getSnapshot().data.edges[0]).toMatchObject({data: {raw: {changed: true}}, style: {stroke: 'purple', lineWidth: 5}});
    expect(controller.getSnapshot().data.nodes.at(-1)?.style?.fill).toBe('green');
    expect(controller.getSnapshot().data.edges.at(-1)?.style?.stroke).toBe('green');
    runtime.patch.mockClear(); runtime.layout.mockClear();
    expect(await controller.mergeData(expansion, {preserveAppearance: true})).toEqual({status: 'applied', value: false});
    expect(runtime.patch).not.toHaveBeenCalled(); expect(runtime.layout).not.toHaveBeenCalled();
    await controller.updateElements({nodes: [{...node('a'), style: {fill: 'orange'}}]});
    expect(controller.getSnapshot().data.nodes[0].style?.fill).toBe('orange');
    await controller.mergeData({nodes: [{...node('a'), style: {fill: 'blue'}}], edges: []});
    expect(controller.getSnapshot().data.nodes[0].style?.fill).toBe('blue');
    await controller.dispose();
});

test('named filter masks replace atomically without clearing overlapping manual hides', async () => {
    const {runtime} = createRuntime(); const controller = new GraphController(runtime);
    await controller.replaceData(input());
    runtime.history.mockClear(); runtime.replace.mockClear();
    await controller.hide(['a', 'ab']);
    await controller.setHiddenIds('filter', ['a', 'b']);
    await controller.setHiddenIds('filter', ['b']);
    expect(runtime.read().nodes.find(node => node.id === 'a')?.style?.visibility).toBe('hidden');
    await controller.hide(['a'], false);
    expect(runtime.read().nodes.find(node => node.id === 'a')?.style?.visibility).toBe('visible');
    expect(runtime.read().edges[0].style?.visibility).toBe('hidden');
    await controller.setHiddenIds('filter', []);
    expect(runtime.read().nodes.find(node => node.id === 'b')?.style?.visibility).toBe('visible');
    expect(runtime.read().edges[0].style?.visibility).toBe('hidden');
    await controller.hide(['ab'], false);
    expect(runtime.read().edges[0].style?.visibility).toBe('visible');
    await controller.setHiddenIds('filter', ['a']);
    await controller.setHiddenIds('category', ['a']);
    await controller.setHiddenIds('filter', []);
    expect(runtime.read().nodes[0].style?.visibility).toBe('hidden');
    await controller.setHiddenIds('category', []);
    expect(runtime.read().nodes[0].style?.visibility).toBe('visible');
    expect(runtime.history).not.toHaveBeenCalled(); expect(runtime.patch).not.toHaveBeenCalled();
    expect(runtime.layout).not.toHaveBeenCalled(); expect(runtime.replace).not.toHaveBeenCalled();
    await controller.dispose();
});

test('visibility sources can be read independently without exposing mutable controller state', async () => {
    const {runtime} = createRuntime(); const controller = new GraphController(runtime);
    await controller.replaceData(input());
    await controller.hide(['a']);
    await controller.setHiddenIds('filter', ['a', 'b']);
    const manual = controller.getHiddenIds();
    const filtered = controller.getHiddenIds('filter');
    expect(manual).toEqual(['a']);
    expect(filtered).toEqual(['a', 'b']);
    manual.push('unrelated'); filtered.length = 0;
    expect(controller.getHiddenIds()).toEqual(['a']);
    expect(controller.getHiddenIds('filter')).toEqual(['a', 'b']);
    await controller.setHiddenIds('filter', []);
    expect(runtime.read().nodes[0].style?.visibility).toBe('hidden');
    expect(runtime.read().nodes[1].style?.visibility).toBe('visible');
    await controller.dispose();
});

test('named masks survive merge, styles and history, prune absent IDs and clear for new datasets', async () => {
    const {runtime, setData} = createRuntime(); const controller = new GraphController(runtime);
    await controller.replaceData(input());
    await controller.hide(['b']); await controller.setHiddenIds('filter', ['a', 'missing']);
    const added = {id: 'ac', source: 'a', target: 'unrelated', data: {raw: {id: 'ac'}}};
    await controller.mergeData({nodes: [], edges: [added]});
    expect(runtime.read().edges.at(-1)?.style?.visibility).toBe('hidden');
    await controller.applyStyle([{id: 'a', kind: 'node', style: {fill: 'red', visibility: 'visible'}}]);
    expect(runtime.read().nodes[0].style?.visibility).toBe('hidden');
    for (const action of ['undo', 'redo'] as const) {
        runtime.history.mockImplementationOnce(async () => {setData(input());});
        await controller[action]();
        expect(runtime.read().nodes.slice(0, 2).map(node => node.style?.visibility)).toEqual(['hidden', 'hidden']);
        expect(runtime.read().edges[0].style?.visibility).toBe('hidden');
        expect(runtime.patch.mock.calls.at(-1)?.[1]).toBe(false);
    }
    await controller.replaceData(input(), {newDataset: false});
    expect(runtime.read().nodes.slice(0, 2).map(node => node.style?.visibility)).toEqual(['hidden', 'hidden']);
    await controller.removeElements(['a']);
    await controller.mergeData({nodes: [node('a'), node('missing')], edges: []});
    expect(runtime.read().nodes.find(node => node.id === 'a')?.style?.visibility).not.toBe('hidden');
    expect(runtime.read().nodes.find(node => node.id === 'missing')?.style?.visibility).not.toBe('hidden');
    await controller.replaceData(input(), {newDataset: true});
    expect(runtime.read().nodes.map(node => node.style?.visibility)).not.toContain('hidden');
    expect(runtime.layout).toHaveBeenCalledTimes(2);
    await controller.dispose();
});
