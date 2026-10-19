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

import type {Graph as NativeGraph, GraphOptions, GraphData as EngineData, NodeData, EdgeData} from '@antv/g6';
import {CircularLayout} from '@antv/layout';
import {G6Runtime} from './G6Runtime';
import {GraphController} from './GraphController';
import type {GraphData} from '../model/types';
import type {GraphLayout} from './types';

let mockZoom = 1;
let mockOptions: GraphOptions;
let mockData: {nodes: NodeData[]; edges: EdgeData[]};
let mockViewport: {width: number; height: number; x: number; y: number};
const mockHandlers = new Map<string, unknown>();
const mockEngine = {
    draw: jest.fn(async () => {}),
    render: jest.fn(async () => {}),
    layout: jest.fn(async () => {}),
    setLayout: jest.fn(),
    setData: jest.fn((data: EngineData) => {mockData = {nodes: data.nodes ?? [], edges: data.edges ?? []};}),
    getNodeData: jest.fn((id?: string) => id === undefined ? mockData.nodes : mockData.nodes.find(node => node.id === id)!),
    updateNodeData: jest.fn((nodes: NodeData[]) => {
        nodes.forEach(node => {
            const current = mockData.nodes.find(item => item.id === node.id)!;
            current.style = {...current.style, ...node.style};
        });
    }),
    getData: jest.fn(() => mockData),
    hasNode: jest.fn((id: string) => mockData.nodes.some(node => node.id === id)),
    hasEdge: jest.fn((id: string) => mockData.edges.some(edge => edge.id === id)),
    addData: jest.fn((incoming: EngineData) => {
        mockData.nodes.push(...(incoming.nodes ?? []));
        mockData.edges.push(...(incoming.edges ?? []));
    }),
    updateData: jest.fn((incoming: EngineData) => {
        for (const kind of ['nodes', 'edges'] as const) {
            for (const update of incoming[kind] ?? []) {
                const index = mockData[kind].findIndex(item => item.id === update.id);
                if (index >= 0) Object.assign(mockData[kind][index], update);
            }
        }
    }),
    getZoom: jest.fn(() => mockZoom),
    getSize: jest.fn(() => [mockViewport.width, mockViewport.height]),
    getCanvasByViewport: jest.fn(([x, y]: number[]) =>
        [(x - mockViewport.x) / mockZoom, (y - mockViewport.y) / mockZoom]),
    getViewportByCanvas: jest.fn(([x, y]: number[]) =>
        [x * mockZoom + mockViewport.x, y * mockZoom + mockViewport.y]),
    getOptions: jest.fn(() => mockOptions),
    setNode: jest.fn(), setEdge: jest.fn(),
    getPluginInstance: jest.fn(() => undefined),
    toDataURL: jest.fn(async () => 'data:image/png;base64,engine'),
    on: jest.fn((name: string, handler: unknown) => {mockHandlers.set(name, handler);}),
    off: jest.fn(), stopLayout: jest.fn(), destroy: jest.fn(),
};
jest.mock('@antv/g6', () => ({
    Graph: jest.fn((options: GraphOptions) => {mockOptions = options; return mockEngine;}),
    GraphEvent: {AFTER_TRANSFORM: 'aftertransform'},
}));
jest.mock('./extensions', () => ({registerExtensions: jest.fn()}));
const data = (count: number): GraphData => ({nodes: Array.from({length: count}, (_, index) => ({
    id: String(index), data: {raw: {id: index}}, style: {labelText: `Node ${index}`},
})), edges: []});

test.each(['shiftKey', 'ctrlKey', 'metaKey'] as const)('forwards %s as additive click intent', modifier => {
    const runtime = new G6Runtime(document.createElement('div'));
    const events = jest.fn(); runtime.onEvent(events);
    const click = mockHandlers.get('node:click') as (event: unknown) => void;
    const event = {target: {id: '0'}, client: {x: 20, y: 30}};
    try {
        click({...event, [modifier]: true});
        expect(events.mock.calls.at(-1)?.[0]).toMatchObject({type: 'click', id: '0', additiveSelection: true});
        click(event);
        expect(events.mock.calls.at(-1)?.[0].additiveSelection).not.toBe(true);
    } finally { runtime.stop(); runtime.destroy(); }
});

test('node dragging dismisses the tooltip and suppresses hover until release', async () => {
    const runtime = new G6Runtime(document.createElement('div'));
    const events = jest.fn();
    runtime.onEvent(events);
    const enter = mockHandlers.get('node:pointerenter') as (event: unknown) => void;
    const edgeEnter = mockHandlers.get('edge:pointerenter') as (event: unknown) => void;
    const event = {target: {id: '0'}, targetType: 'node', client: {x: 20, y: 30}};
    const drag = (mockOptions.behaviors as Array<{type?: string; enable?: (event: unknown) => boolean;
        onFinish?: () => void}>).find(item => item.type === 'drag-element')!;
    try {
        enter(event);
        expect(events.mock.calls.at(-1)?.[0].type).toBe('hover');
        expect(drag.enable!(event)).toBe(true);
        expect(events.mock.calls.at(-1)?.[0].type).toBe('leave');
        events.mockClear();
        let released = false;
        const idle = runtime.idleInteraction().then(() => { released = true; });
        enter(event);
        edgeEnter(event);
        await Promise.resolve();
        expect(events).not.toHaveBeenCalled();
        expect(released).toBe(false);
        drag.onFinish!();
        await idle;
        expect(released).toBe(true);
        expect(events.mock.calls.at(-1)?.[0].type).toBe('dragEnd');
        enter(event);
        expect(events.mock.calls.at(-1)?.[0].type).toBe('hover');
    }
    finally { runtime.stop(); runtime.destroy(); }
});

test.each([1, 2])('PNG exports the whole graph on white with a 30px margin at DPR %s', async pixelRatio => {
    const runtime = new G6Runtime(document.createElement('div'));
    const image = document.createElement('img');
    const decode = jest.fn(async () => {});
    Object.defineProperties(image, {
        naturalWidth: {value: 240}, naturalHeight: {value: 120}, decode: {value: decode},
    });
    const context = {fillStyle: '', fillRect: jest.fn(), drawImage: jest.fn()};
    const imageSpy = jest.spyOn(globalThis, 'Image').mockImplementation(() => image);
    const contextSpy = jest.spyOn(HTMLCanvasElement.prototype, 'getContext')
        .mockReturnValue(context as unknown as CanvasRenderingContext2D);
    const dataSpy = jest.spyOn(HTMLCanvasElement.prototype, 'toDataURL')
        .mockReturnValue('data:image/png;base64,white-and-padded');
    const ratio = jest.replaceProperty(globalThis, 'devicePixelRatio', pixelRatio);
    try {
        expect(await runtime.exportPng()).toBe('data:image/png;base64,white-and-padded');
        expect(mockEngine.toDataURL).toHaveBeenCalledWith({type: 'image/png', mode: 'overall'});
        expect(decode).toHaveBeenCalledTimes(1);
        expect(context.fillStyle).toBe('#ffffff');
        expect(context.fillRect).toHaveBeenCalledWith(0, 0, 240 + 60 * pixelRatio, 120 + 60 * pixelRatio);
        expect(context.drawImage).toHaveBeenCalledWith(image, 30 * pixelRatio, 30 * pixelRatio);
        expect(dataSpy).toHaveBeenCalledWith('image/png');
    }
    finally {
        ratio.restore(); dataSpy.mockRestore(); contextSpy.mockRestore(); imageSpy.mockRestore();
        runtime.stop(); runtime.destroy();
    }
});
const nodeStyle = (datum: NodeData) => {
    const mapper = mockOptions.node?.style;
    if (typeof mapper !== 'function') throw new Error('Expected node style mapper');
    return mapper.call({} as NativeGraph, datum);
};
const nodeState = (state: string, datum: NodeData) => {
    const mapper = mockOptions.node?.state?.[state];
    if (typeof mapper !== 'function') throw new Error('Expected state style mapper');
    return mapper.call({} as NativeGraph, datum);
};
beforeEach(() => {
    jest.clearAllMocks(); mockZoom = 1; mockHandlers.clear();
    mockViewport = {width: 800, height: 600, x: 0, y: 0};
});

test.each<{name: string; options: GraphLayout['options']; first: number; last: number}>([
    {name: 'interpolated endpoints', options: {startRadius: 80, endRadius: 160}, first: 80, last: 160},
    {name: 'start only', options: {startRadius: 80}, first: 80, last: 80},
    {name: 'end only', options: {endRadius: 160}, first: 160, last: 160},
    {name: 'explicit radius', options: {radius: 240, startRadius: 80, endRadius: 160}, first: 240, last: 240},
    {name: 'zero radius', options: {radius: 0, startRadius: 80, endRadius: 160}, first: 80, last: 160},
    {name: 'null radius', options: {radius: null, startRadius: 80, endRadius: 160}, first: 80, last: 160},
    {name: 'default radius', options: {}, first: 120, last: 120},
])('circular positions honor $name in the installed layout implementation', async ({options, first, last}) => {
    const runtime = new G6Runtime(document.createElement('div'));
    let algorithm: CircularLayout | undefined;
    try {
        await runtime.replace(data(6), {type: 'circular', options});
        algorithm = new CircularLayout({...mockEngine.setLayout.mock.calls.at(-1)![0], center: [0, 0]});
        await algorithm.execute({nodes: data(6).nodes});
        const radii: number[] = [];
        algorithm.forEachNode(node => radii.push(Math.hypot(node.x, node.y)));
        expect(radii).toHaveLength(6);
        expect(radii[0]).toBeCloseTo(first);
        expect(radii.at(-1)).toBeCloseTo(last);
    }
    finally {
        algorithm?.destroy();
        runtime.stop(); runtime.destroy();
    }
});

test('dense first draw suppresses labels, while hover/selection retain details without overriding explicit opt-out', async () => {
    const runtime = new G6Runtime(document.createElement('div'));
    const input = data(800);
    await runtime.replace(input, null);
    expect(nodeStyle(mockData.nodes[0]).label).toBe(false);
    expect(mockOptions.node?.state?.active).not.toHaveProperty('label');
    expect(mockOptions.node?.state?.active).toHaveProperty('halo', false);
    expect(nodeState('focused', mockData.nodes[0]).label).toBe(true);
    expect(nodeState('selected', mockData.nodes[0]).label).toBe(true);
    expect(nodeState('focused', {...mockData.nodes[0], style: {label: false}}).label).toBe(false);
    expect(mockEngine.draw).toHaveBeenCalledTimes(1);
    expect(runtime.read()).toEqual(input);
    runtime.stop(); runtime.destroy();
});

test('only density or font bucket changes refresh public mappers without overwriting raw data', async () => {
    const runtime = new G6Runtime(document.createElement('div'));
    const input = data(800);
    await runtime.replace(input, null);
    mockEngine.draw.mockClear();
    for (const zoom of [0.1, 0.5, 1, 1.1]) {mockZoom = zoom; await runtime.refreshLabelVisibility(800);}
    expect(mockEngine.draw).not.toHaveBeenCalled();
    mockZoom = 2;
    await runtime.refreshLabelVisibility(800);
    expect(mockEngine.setNode).toHaveBeenCalledTimes(1);
    expect(mockEngine.setEdge).toHaveBeenCalledTimes(1);
    expect(nodeStyle(mockData.nodes[0]).label).toBe(true);
    mockZoom = 2.1;
    await runtime.refreshLabelVisibility(800);
    expect(mockEngine.draw).toHaveBeenCalledTimes(1);
    mockZoom = 3;
    await runtime.refreshLabelVisibility(800);
    expect(mockEngine.setEdge).toHaveBeenCalledTimes(2);
    expect(mockEngine.draw).toHaveBeenCalledTimes(2);
    mockZoom = 0.1;
    await runtime.refreshLabelVisibility(199);
    expect(mockEngine.draw).toHaveBeenCalledTimes(3);
    expect(runtime.read()).toEqual(input);
    expect(mockEngine.setData).toHaveBeenCalledTimes(1);
    runtime.stop(); runtime.destroy();
});

test('zoom scales default and custom fonts only in rendering, including focused and selected labels', async () => {
    const runtime = new G6Runtime(document.createElement('div'));
    const input = data(2);
    input.nodes[1].style = {labelText: 'custom', labelFontSize: 24, labelLineHeight: 28, labelPadding: [4, 8, 4, 8]};
    input.edges = [{id: 'edge', source: '0', target: '1', data: {raw: {id: 'edge'}},
        style: {labelText: 'knows', labelFontSize: 22}}];
    await runtime.replace(input, null);
    mockEngine.draw.mockClear();
    mockZoom = 4;
    await runtime.refreshLabelVisibility(3);
    expect(nodeStyle(mockData.nodes[0]).labelFontSize).toBe(3);
    expect(nodeStyle(mockData.nodes[1])).toMatchObject({labelFontSize: 6, labelLineHeight: 7, labelPadding: [1, 2, 1, 2]});
    expect(nodeStyle({...mockData.nodes[1], style: {labelFontSize: 0}}).labelFontSize).toBe(0);
    expect(nodeStyle({...mockData.nodes[1], style: {labelPadding: 8}}).labelPadding).toBe(2);
    const edgeStyle = mockOptions.edge?.style;
    if (typeof edgeStyle !== 'function') throw new Error('Expected public edge style mapper');
    expect(edgeStyle.call({} as NativeGraph, mockData.edges[0]).labelFontSize).toBe(5.5);
    expect(nodeState('focused', mockData.nodes[1])).toEqual({label: true});
    expect(nodeState('selected', mockData.nodes[1])).toMatchObject({halo: false, labelFontSize: 6, labelLineHeight: 7,
        labelPadding: [1, 2, 1, 2]});
    const selected = mockOptions.edge?.state?.selected;
    if (typeof selected !== 'function') throw new Error('Expected public edge state mapper');
    expect(selected.call({} as NativeGraph, mockData.edges[0])).toMatchObject({halo: false, labelFontSize: 5.5});
    mockZoom = 4.1;
    await runtime.refreshLabelVisibility(3);
    expect(mockEngine.draw).toHaveBeenCalledTimes(1);
    mockZoom = 1;
    await runtime.refreshLabelVisibility(3);
    expect(nodeStyle(mockData.nodes[1]).labelFontSize).toBe(24);
    expect(runtime.read()).toEqual(input);
    expect(mockEngine.setData).toHaveBeenCalledTimes(1);
    expect(mockEngine.updateNodeData).not.toHaveBeenCalled();
    expect(mockEngine.layout).not.toHaveBeenCalled();
    runtime.stop(); runtime.destroy();
});

test('medium overview retains node names and all data; edge hover, selection and zoom expose names', async () => {
    const runtime = new G6Runtime(document.createElement('div'));
    const edge = {id: 'edge', source: '0', target: '1', data: {raw: {id: 'edge'}}, style: {labelText: 'knows'}};
    const input = {...data(199), edges: [edge]};
    await runtime.replace(input, null);
    const style = mockOptions.edge?.style;
    const focused = mockOptions.edge?.state?.focused;
    const selected = mockOptions.edge?.state?.selected;
    if (typeof style !== 'function' || typeof focused !== 'function' || typeof selected !== 'function') {
        throw new Error('Expected public edge style and state mappers');
    }
    const graph = {} as NativeGraph;
    expect(nodeStyle(mockData.nodes[0]).label).toBe(true);
    expect(style.call(graph, edge)).toMatchObject({label: false, labelText: 'knows'});
    expect(mockOptions.edge?.state?.active).not.toHaveProperty('label');
    expect(mockOptions.edge?.state?.active).toHaveProperty('halo', false);
    expect(focused.call(graph, edge).label).toBe(true);
    expect(selected.call(graph, edge).label).toBe(true);
    expect(focused.call(graph, {...edge, style: {label: false}}).label).toBe(false);
    mockEngine.draw.mockClear();
    mockZoom = 1.1;
    await runtime.refreshLabelVisibility(200);
    expect(mockEngine.draw).not.toHaveBeenCalled();
    mockZoom = 2;
    await runtime.refreshLabelVisibility(200);
    expect(style.call(graph, edge).label).toBe(true);
    expect(mockEngine.setEdge).toHaveBeenCalledTimes(1);
    expect(mockEngine.setNode).toHaveBeenCalledTimes(1);
    expect(mockEngine.layout).not.toHaveBeenCalled();
    expect(runtime.read()).toEqual(input);
    expect(mockEngine.setData).toHaveBeenCalledTimes(1);
    runtime.stop(); runtime.destroy();
});

test('preview labels are stable and viewport events stop with the owning runtime', async () => {
    const runtime = new G6Runtime(document.createElement('div'), {preset: 'preview'});
    const callback = jest.fn();
    runtime.onEvent(callback);
    await runtime.replace(data(800), null);
    const handler = mockHandlers.get('aftertransform');
    if (typeof handler !== 'function') throw new Error('Missing viewport listener');
    handler();
    expect(callback).toHaveBeenCalledWith({type: 'viewport'});
    mockZoom = 0.03;
    await runtime.refreshLabelVisibility(800);
    expect(nodeStyle(mockData.nodes[0]).label).toBe(true);
    mockZoom = 4;
    await runtime.refreshLabelVisibility(800);
    expect(nodeStyle(mockData.nodes[0]).labelFontSize).toBe(12);
    expect(mockEngine.setNode).not.toHaveBeenCalled();
    runtime.stop(); handler(); runtime.destroy();
    expect(callback).toHaveBeenCalledTimes(1);
    expect(mockEngine.off).toHaveBeenCalledWith('aftertransform', handler);
});

test('failed density draw can be retried and never certifies a stale visibility policy', async () => {
    const runtime = new G6Runtime(document.createElement('div'));
    await runtime.replace(data(800), null);
    mockZoom = 2;
    mockEngine.draw.mockRejectedValueOnce(new Error('draw failed'));
    await expect(runtime.refreshLabelVisibility(800)).rejects.toThrow('draw failed');
    expect(nodeStyle(mockData.nodes[0]).labelFontSize).toBe(12);
    expect(nodeStyle(mockData.nodes[0]).label).toBe(false);
    await runtime.refreshLabelVisibility(800);
    expect(nodeStyle(mockData.nodes[0]).label).toBe(true);
    expect(nodeStyle(mockData.nodes[0]).labelFontSize).toBe(6);
    expect(mockEngine.setNode).toHaveBeenCalledTimes(2);
    runtime.stop(); runtime.destroy();
});

test('automatic transform geometry is not overwritten by stale fields in the default-style callback', async () => {
    const runtime = new G6Runtime(document.createElement('div'));
    const input = {...data(2), edges: [{id: 'edge', source: '0', target: '1', data: {raw: {id: 'edge'}},
        style: {stroke: '#f00', curveOffset: 999, loopPlacement: 'bottom', loopDist: 999}}]};
    await runtime.replace(input, null);
    const mapper = mockOptions.edge?.style;
    if (typeof mapper !== 'function') throw new Error('Expected edge style mapper');
    const style = mapper.call({} as NativeGraph, mockData.edges[0]);
    expect(style).toMatchObject({stroke: '#f00', label: true});
    expect(style).not.toHaveProperty('curveOffset');
    expect(style).not.toHaveProperty('loopPlacement');
    expect(style).not.toHaveProperty('loopDist');
    expect(mockOptions.transforms).toEqual([{type: 'hubble-parallel-edges'}]);
    expect(runtime.read()).toEqual(input);
    runtime.stop(); runtime.destroy();
});

test('first nonempty dataset uses one native pre-layout render; subsequent data and styles do not repeat it', async () => {
    const runtime = new G6Runtime(document.createElement('div'));
    const input = data(100);
    await runtime.replace(input, {type: 'd3-force'});
    expect(mockEngine.render).toHaveBeenCalledTimes(1);
    expect(mockEngine.draw).not.toHaveBeenCalled();
    expect(mockEngine.layout).not.toHaveBeenCalled();
    expect(mockEngine.setLayout).toHaveBeenLastCalledWith(expect.objectContaining({
        type: 'hubble-pinned-d3-force', preLayout: true, isLayoutInvisibleNodes: true, animation: false, iterations: 300,
        link: {distance: 100}, manyBody: {strength: -250}, collide: {radius: 28},
        x: {strength: 0.2}, y: {strength: 0.2},
    }));
    expect(runtime.read()).toEqual(input);
    await runtime.replace(input, {type: 'd3-force'});
    expect(mockEngine.render).toHaveBeenCalledTimes(1);
    expect(mockEngine.draw).toHaveBeenCalledTimes(1);
    expect(mockEngine.layout).toHaveBeenCalledTimes(1);
    expect(mockEngine.setLayout).toHaveBeenLastCalledWith(expect.objectContaining({preLayout: false}));
    await runtime.replace(input, null);
    expect(mockEngine.draw).toHaveBeenCalledTimes(2);
    expect(mockEngine.layout).toHaveBeenCalledTimes(1);
    runtime.stop(); runtime.destroy();
});

test('empty initial data preserves pre-layout for the first asynchronous real result', async () => {
    const runtime = new G6Runtime(document.createElement('div'));
    await runtime.replace(data(0), {type: 'grid'});
    expect(mockEngine.render).not.toHaveBeenCalled();
    mockEngine.draw.mockClear(); mockEngine.layout.mockClear();
    await runtime.replace(data(3), {type: 'grid'});
    expect(mockEngine.render).toHaveBeenCalledTimes(1);
    expect(mockEngine.draw).not.toHaveBeenCalled();
    expect(mockEngine.layout).not.toHaveBeenCalled();
    expect(mockEngine.setLayout).toHaveBeenLastCalledWith(expect.objectContaining({type: 'grid', preLayout: true}));
    runtime.stop(); runtime.destroy();
});

test('manual addition after empty data consumes first pre-layout before replacing already drawn nodes', async () => {
    const runtime = new G6Runtime(document.createElement('div'));
    await runtime.replace(data(0), {type: 'grid'});
    await runtime.patch(data(2), true);
    expect(mockEngine.render).not.toHaveBeenCalled();
    mockEngine.layout.mockClear();
    await runtime.replace(data(2), {type: 'grid'});
    expect(mockEngine.render).not.toHaveBeenCalled();
    expect(mockEngine.layout).toHaveBeenCalledTimes(1);
    expect(mockEngine.setLayout).toHaveBeenLastCalledWith(expect.objectContaining({preLayout: false}));
    runtime.stop(); runtime.destroy();
});

test('first nonempty draw without a layout cannot later pre-layout existing shapes', async () => {
    const runtime = new G6Runtime(document.createElement('div'));
    await runtime.replace(data(2), null);
    await runtime.replace(data(2), {type: 'grid'});
    expect(mockEngine.render).not.toHaveBeenCalled();
    expect(mockEngine.layout).toHaveBeenCalledTimes(1);
    expect(mockEngine.setLayout).toHaveBeenLastCalledWith(expect.objectContaining({preLayout: false}));
    runtime.stop(); runtime.destroy();
});

test('column layout uses its explicit positions and does not invoke native pre-layout', async () => {
    const runtime = new G6Runtime(document.createElement('div'));
    await runtime.replace(data(3), {type: 'columns'});
    expect(mockEngine.render).not.toHaveBeenCalled();
    expect(mockEngine.layout).not.toHaveBeenCalled();
    expect(mockData.nodes.map(node => node.style?.y)).toEqual([-90, 0, 90]);
    await runtime.replace(data(3), {type: 'grid'});
    expect(mockEngine.render).not.toHaveBeenCalled();
    expect(mockEngine.layout).toHaveBeenCalledTimes(1);
    runtime.stop(); runtime.destroy();
});

test('initial non-force layout restores saved pinned positions without mutating the incoming data', async () => {
    const runtime = new G6Runtime(document.createElement('div'));
    const input = data(2);
    input.nodes[0] = {...input.nodes[0], data: {...input.nodes[0].data, fixed: true}, style: {x: 17, y: 23}};
    mockEngine.render.mockImplementationOnce(async () => {
        mockData.nodes[0].style = {x: 300, y: 400};
    });
    await runtime.replace(input, {type: 'dagre'});
    expect(mockEngine.updateNodeData).toHaveBeenCalledWith([{id: '0', style: {x: 17, y: 23}}]);
    expect(mockEngine.draw).toHaveBeenCalledTimes(1);
    expect(runtime.read().nodes[0]).toEqual(input.nodes[0]);
    expect(input.nodes[0].style).toEqual({x: 17, y: 23});
    runtime.stop(); runtime.destroy();
});

test('a failed native first render surfaces the error and retries through the established update path', async () => {
    const runtime = new G6Runtime(document.createElement('div'));
    mockEngine.render.mockRejectedValueOnce(new Error('first render failed'));
    await expect(runtime.replace(data(2), {type: 'grid'})).rejects.toThrow('first render failed');
    await runtime.replace(data(2), {type: 'grid'});
    expect(mockEngine.render).toHaveBeenCalledTimes(1);
    expect(mockEngine.draw).toHaveBeenCalledTimes(1);
    expect(mockEngine.layout).toHaveBeenCalledTimes(1);
    runtime.stop(); runtime.destroy();
});

test.each([0.5, 2])('remote additions receive distinct visible positions at zoom %s without moving the saved graph', async zoom => {
    const runtime = new G6Runtime(document.createElement('div'));
    const controller = new GraphController(runtime);
    mockZoom = zoom; mockViewport.x = -1200; mockViewport.y = 250;
    const initial = data(1);
    initial.nodes[0].style = {x: (400 - mockViewport.x) / zoom, y: (300 - mockViewport.y) / zoom, fill: 'red'};
    initial.nodes[0].data.fixed = true;
    await controller.replaceData(initial, {layout: false, fit: false});
    const before = controller.getSnapshot().data.nodes[0];
    const viewport = {...mockViewport};
    const additions = ['one', 'two', 'three'].map(id => ({id, data: {raw: {id}}, style: {size: 40}}));
    try {
        await controller.updateElements({nodes: additions.slice(0, 2)});
        await controller.updateElements({nodes: additions.slice(2)});
        const actual = controller.getSnapshot().data.nodes;
        const points = actual.slice(1).map(node => mockEngine.getViewportByCanvas([node.style!.x!, node.style!.y!]));
        for (const [x, y] of points) {
            expect(x).toBeGreaterThan(40 * zoom / 2);
            expect(x).toBeLessThan(viewport.width - 40 * zoom / 2);
            expect(y).toBeGreaterThan(40 * zoom / 2);
            expect(y).toBeLessThan(viewport.height - 40 * zoom / 2);
        }
        expect(new Set(points.map(point => point.join(','))).size).toBe(3);
        expect(actual[0]).toEqual(before);
        expect(mockViewport).toEqual(viewport); expect(mockZoom).toBe(zoom);
        expect(mockEngine.layout).not.toHaveBeenCalled(); expect(mockEngine.setLayout).not.toHaveBeenCalled();
        expect(additions.every(node => !('x' in node.style) && !('y' in node.style))).toBe(true);
    }
    finally { await controller.dispose(); }
});

test('new explicit coordinates survive while only missing axes are filled, and edits or edges do not reposition', async () => {
    const runtime = new G6Runtime(document.createElement('div'));
    const controller = new GraphController(runtime);
    mockViewport.x = -1000; mockViewport.y = -700; mockZoom = 2;
    const initial = data(1); initial.nodes[0].style = {x: 600, y: 500};
    await controller.replaceData(initial, {layout: false, fit: false});
    try {
        await controller.updateElements({nodes: [
            {id: 'explicit', data: {raw: {}}, style: {x: 0, y: 0}},
            {id: 'partial', data: {raw: {}}, style: {x: 710}},
        ]});
        expect(controller.getSnapshot().data.nodes.find(node => node.id === 'explicit')?.style).toEqual({x: 0, y: 0});
        const partial = controller.getSnapshot().data.nodes.find(node => node.id === 'partial')!;
        expect(partial.style?.x).toBe(710);
        const [, y] = mockEngine.getViewportByCanvas([partial.style!.x!, partial.style!.y!]);
        expect(y).toBeGreaterThan(0); expect(y).toBeLessThan(mockViewport.height);
        mockEngine.getSize.mockClear(); mockEngine.getCanvasByViewport.mockClear();
        const positions = controller.getSnapshot().data.nodes.map(node => [node.id, node.style?.x, node.style?.y]);
        await controller.updateElements({nodes: [{id: '0', data: {raw: {name: 'edited'}}, style: {fill: 'blue'}}]});
        await controller.updateElements({edges: [{id: 'link', source: '0', target: 'partial', data: {raw: {}}}]});
        expect(controller.getSnapshot().data.nodes.map(node => [node.id, node.style?.x, node.style?.y])).toEqual(positions);
        expect(mockEngine.getSize).not.toHaveBeenCalled(); expect(mockEngine.getCanvasByViewport).not.toHaveBeenCalled();
        expect(mockEngine.layout).not.toHaveBeenCalled();
    }
    finally { await controller.dispose(); }
});

test('an unavailable viewport or a disposed runtime never generates invalid positions', async () => {
    const runtime = new G6Runtime(document.createElement('div'));
    await runtime.replace(data(0), null);
    mockViewport.width = 0;
    await runtime.patch(data(1), false);
    expect(runtime.read().nodes[0].style).toEqual({labelText: 'Node 0'});
    expect(mockEngine.getCanvasByViewport).not.toHaveBeenCalled();
    runtime.stop();
    mockEngine.addData.mockClear(); mockEngine.getSize.mockClear();
    await runtime.patch(data(2), false);
    expect(mockEngine.addData).not.toHaveBeenCalled(); expect(mockEngine.getSize).not.toHaveBeenCalled();
    runtime.destroy();
});
