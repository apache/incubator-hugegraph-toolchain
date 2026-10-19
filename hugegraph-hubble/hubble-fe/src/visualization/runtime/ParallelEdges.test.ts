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

import {BaseTransform, Graph, ProcessParallelEdges} from '@antv/g6';
import type {DrawData, EdgeData, NodeData} from '@antv/g6';
import {ParallelEdges} from './ParallelEdges';

const edge = (id: string, source = 'a', target = 'b'): EdgeData => ({id, source, target,
    data: {raw: Object.freeze({id, label: 'knows'})}});
const draw = ({add = [], update = [], remove = [], nodes = []}: {
    add?: EdgeData[]; update?: EdgeData[]; remove?: EdgeData[]; nodes?: NodeData[];
} = {}): DrawData => ({
    add: {nodes: new Map(), edges: new Map(add.map(value => [value.id!, value])), combos: new Map()},
    update: {nodes: new Map(nodes.map(value => [value.id, value])),
        edges: new Map(update.map(value => [value.id!, value])), combos: new Map()},
    remove: {nodes: new Map(), edges: new Map(remove.map(value => [value.id!, value])), combos: new Map()},
});
const fixture = (edges: EdgeData[]) => {
    let records = new Map(edges.map(value => [value.id!, value]));
    const getEdgeData = jest.fn((id?: string) => id === undefined ? [...records.values()] : records.get(id)!);
    const context = {graph: {getEdgeData}} as unknown as ConstructorParameters<typeof ParallelEdges>[0];
    const transform = new ParallelEdges(context, {type: 'parallel-edges'});
    return {transform, getEdgeData, set: (values: EdgeData[]) => { records = new Map(values.map(value => [value.id!, value])); }};
};
const offsets = (data: DrawData) => [...data.add.edges.values(), ...data.update.edges.values()]
    .map(value => [value.id, value.style?.curveOffset]);
const fullReads = (get: ReturnType<typeof fixture>['getEdgeData']) => get.mock.calls.filter(args => !args.length).length;

test('uses the installed public BaseTransform and preserves odd/even bundle geometry and reverse direction', () => {
    const odd = [edge('one'), edge('two', 'b', 'a'), edge('three')];
    const first = fixture(odd);
    expect(first.transform).toBeInstanceOf(BaseTransform);
    expect(offsets(first.transform.beforeDraw(draw({add: odd})))).toEqual([['one', 0], ['two', 48], ['three', 48]]);
    const even = [edge('one'), edge('two'), edge('three', 'b', 'a'), edge('four')];
    const second = fixture(even);
    expect(offsets(second.transform.beforeDraw(draw({add: even})))).toEqual([
        ['one', 24], ['two', -24], ['three', -72], ['four', -72],
    ]);
    expect(fullReads(first.getEdgeData)).toBe(1);
});

test('groups endpoints without string delimiter collisions and spreads self loops across eight positions', () => {
    const loops = Array.from({length: 10}, (_, index) => edge(`loop-${index}`, 'self', 'self'));
    const edges = [edge('a', 'a-b', 'c'), edge('b', 'a', 'b-c'), edge('c', 'a-b', 'c'), ...loops];
    const value = fixture(edges).transform.beforeDraw(draw({add: edges}));
    expect(value.add.edges.get('a')?.style?.curveOffset).toBe(24);
    expect(value.add.edges.get('b')?.style?.curveOffset).toBe(0);
    expect(value.add.edges.get('c')?.style?.curveOffset).toBe(-24);
    expect(loops.map(item => value.add.edges.get(item.id!)?.style?.loopPlacement)).toEqual([
        'top', 'top-right', 'right', 'right-bottom', 'bottom', 'bottom-left', 'left', 'left-top', 'top', 'top-right',
    ]);
    expect(loops.map(item => value.add.edges.get(item.id!)?.style?.loopDist)).toEqual([50, 50, 50, 50, 50, 50, 50, 50, 74, 74]);
});

test('recomputes surviving lanes after deletion, clear, undo and redo without removing visible edges', () => {
    const edges = [edge('one'), edge('two'), edge('three')];
    const {transform, set, getEdgeData} = fixture(edges);
    transform.beforeDraw(draw({add: edges}));
    getEdgeData.mockClear();
    set([edges[0], edges[2]]);
    const deleted = transform.beforeDraw(draw({remove: [edges[1]]}));
    expect([...deleted.remove.edges.keys()]).toEqual(['two']);
    expect(offsets(deleted)).toEqual([['one', 24], ['three', -24]]);
    expect(fullReads(getEdgeData)).toBe(1);
    set(edges);
    const undo = transform.beforeDraw(draw({add: [edges[1]]}));
    expect(undo.add.edges.size + undo.update.edges.size).toBe(3);
    expect(undo.update.edges.get('one')?.style?.curveOffset).toBe(0);
    set([edges[0], edges[2]]);
    const redo = transform.beforeDraw(draw({remove: [edges[1]]}));
    expect(redo.update.edges.get('one')?.style?.curveOffset).toBe(24);
    set([]);
    const cleared = transform.beforeDraw(draw({remove: [edges[0], edges[2]]}));
    expect(cleared.add.edges.size + cleared.update.edges.size).toBe(0);
    set([edges[0]]);
    const replaced = transform.beforeDraw(draw({add: [edges[0]]}));
    expect(replaced.add.edges.get('one')?.style?.curveOffset).toBe(0);
});

test('updates both endpoint groups after a move and normalizes same-ID replacement to one update', () => {
    const edges = [edge('one'), edge('two'), edge('three', 'c', 'd')];
    const {transform, set, getEdgeData} = fixture(edges);
    transform.beforeDraw(draw({add: edges}));
    getEdgeData.mockClear();
    const moved = {...edges[1], source: 'c', target: 'd'};
    set([edges[0], moved, edges[2]]);
    const next = transform.beforeDraw(draw({update: [moved]}));
    expect(next.update.edges.get('one')?.style?.curveOffset).toBe(0);
    expect(next.update.edges.get('two')?.style?.curveOffset).toBe(24);
    expect(next.update.edges.get('three')?.style?.curveOffset).toBe(-24);
    expect(fullReads(getEdgeData)).toBe(1);
    set([moved, edges[2], edges[0]]);
    const readded = transform.beforeDraw(draw({remove: [edges[0]], add: [edges[0]], update: [edges[0]]}));
    expect(readded.remove.edges.has('one')).toBe(false);
    expect(readded.add.edges.has('one')).toBe(false);
    expect(readded.update.edges.get('one')?.style?.curveOffset).toBe(0);
});

test('projects only affected style/state/position changes with no full graph reads or raw/model mutation', () => {
    const edges = Array.from({length: 200}, (_, index) => edge(`e${index}`, `a${index}`, `b${index}`));
    const {transform, getEdgeData} = fixture(edges);
    transform.beforeDraw(draw({add: edges}));
    getEdgeData.mockClear();
    const style = Object.freeze({stroke: '#f00', curveOffset: 99});
    const styled = Object.freeze({...edges[17], style, states: ['selected']});
    const input = draw({update: [styled], nodes: [{id: 'a17', style: {x: 50, y: 80}}]});
    const changed = transform.beforeDraw(input);
    expect(getEdgeData).not.toHaveBeenCalled();
    expect(changed.update.edges.size).toBe(1);
    expect(changed.update.edges.get('e17')).toMatchObject({type: 'quadratic', style: {stroke: '#f00', curveOffset: 0},
        states: ['selected']});
    expect(changed.update.edges.get('e17')?.data?.raw).toBe(styled.data?.raw);
    expect(styled.style.curveOffset).toBe(99);
    expect(edges[17].style).toBeUndefined();
});

test('keeps duplicate-ID stable order, replaces business records without a cache copy and isolates instances', () => {
    const first = edge('one');
    const last = {...first, data: {raw: {id: 'new business record'}}};
    const edges = [first, edge('two'), last];
    const a = fixture(edges);
    const initial = a.transform.beforeDraw(draw({add: edges}));
    expect([...initial.add.edges.keys()]).toEqual(['one', 'two']);
    expect(initial.add.edges.get('one')?.data).toBe(last.data);
    const b = fixture([edge('one')]);
    expect(b.transform.beforeDraw(draw({add: [edge('one')]})).add.edges.get('one')?.style?.curveOffset).toBe(0);
    a.getEdgeData.mockClear();
    const replacement = {...last, data: {raw: {id: 'third record'}}};
    a.set([replacement, edges[1]]);
    const updated = a.transform.beforeDraw(draw({update: [replacement]}));
    expect(updated.update.edges.get('one')?.data).toBe(replacement.data);
    expect(updated.update.edges.get('one')?.style?.curveOffset).toBe(24);
    expect(a.getEdgeData).not.toHaveBeenCalled();
    a.transform.destroy();
    expect(a.transform.destroyed).toBe(true);
    expect(b.transform.beforeDraw(draw({update: [edge('one')]})).update.edges.get('one')?.style?.curveOffset).toBe(0);
});

test('reads endpoints linearly and only redraws the group changed by added topology', () => {
    let endpointReads = 0;
    const edges = Array.from({length: 2000}, (_, index): EdgeData => ({id: `edge-${index}`,
        get source() { endpointReads++; return `source-${index}`; },
        get target() { endpointReads++; return `target-${index}`; },
    }));
    const {transform, set, getEdgeData} = fixture(edges);
    const initial = transform.beforeDraw(draw({add: edges}));
    expect(initial.add.edges.size).toBe(2000);
    expect(endpointReads).toBeLessThanOrEqual(12 * edges.length);
    getEdgeData.mockClear();
    const extra = edge('extra', 'source-17', 'target-17');
    set([...edges, extra]);
    const added = transform.beforeDraw(draw({add: [extra]}));
    expect([...added.add.edges.keys()]).toEqual(['extra']);
    expect([...added.update.edges.keys()]).toEqual(['edge-17']);
    expect(added.update.edges.get('edge-17')?.style?.curveOffset).toBe(24);
    expect(added.add.edges.get('extra')?.style?.curveOffset).toBe(-24);
    expect(fullReads(getEdgeData)).toBe(1);
    expect(getEdgeData.mock.calls.filter(args => args.length)).toEqual([['edge-17']]);
});

test('initializes an empty graph, recognizes unknown updated edges and ignores draws after destruction', () => {
    const {transform, set, getEdgeData} = fixture([]);
    transform.beforeDraw(draw({nodes: [{id: 'node'}]}));
    getEdgeData.mockClear();
    const added = edge('new');
    set([added]);
    expect(transform.beforeDraw(draw({update: [added]})).update.edges.get('new')?.style?.curveOffset).toBe(0);
    expect(fullReads(getEdgeData)).toBe(1);
    transform.destroy();
    getEdgeData.mockClear();
    const input = draw({update: [added]});
    expect(transform.beforeDraw(input)).toBe(input);
    expect(getEdgeData).not.toHaveBeenCalled();
});

test('does not leave a second stale update after required G6 transforms also mark an added edge related', () => {
    const edges = [edge('one'), edge('two')];
    const {transform} = fixture(edges);
    const input = draw({add: edges, update: edges});
    const result = transform.beforeDraw(input);
    expect(result.add.edges.size).toBe(2);
    expect(result.update.edges.size).toBe(0);
    expect(offsets(result)).toEqual([['one', 24], ['two', -24]]);
});

test('same-ID endpoint and loop replacements use the new complete datum without delayed removal', () => {
    const old = edge('changed');
    const partner = edge('partner');
    const target = edge('target', 'c', 'd');
    const {transform, set, getEdgeData} = fixture([old, partner, target]);
    transform.beforeDraw(draw({add: [old, partner, target]}));
    const raw = Object.freeze({id: 'replacement business ID'});
    const moved = Object.freeze({...old, source: 'c', target: 'd', type: 'line', data: Object.freeze({raw})});
    set([partner, target, moved]);
    getEdgeData.mockClear();
    const replacement = transform.beforeDraw(draw({add: [moved], remove: [old]}));
    expect(replacement.add.edges.has('changed')).toBe(false);
    expect(replacement.remove.edges.has('changed')).toBe(false);
    expect(replacement.update.edges.get('changed')).toMatchObject({source: 'c', target: 'd', type: 'quadratic',
        style: {curveOffset: -24}});
    expect(replacement.update.edges.get('changed')?.data?.raw).toBe(raw);
    expect(replacement.update.edges.get('partner')?.style?.curveOffset).toBe(0);
    expect(replacement.update.edges.get('target')?.style?.curveOffset).toBe(24);
    expect(fullReads(getEdgeData)).toBe(1);
    const loop = Object.freeze({...moved, source: 'c', target: 'c'});
    set([partner, target, loop]);
    const loopReplacement = transform.beforeDraw(draw({add: [loop], remove: [moved]}));
    expect(loopReplacement.add.edges.has('changed')).toBe(false);
    expect(loopReplacement.remove.edges.has('changed')).toBe(false);
    expect(loopReplacement.update.edges.get('changed')).toMatchObject({source: 'c', target: 'c', type: 'quadratic',
        style: {loopPlacement: 'top', loopDist: 50}});
    expect(loopReplacement.update.edges.get('target')?.style?.curveOffset).toBe(0);
    set([partner, target, moved]);
    const restored = transform.beforeDraw(draw({add: [moved], remove: [loop]}));
    expect(restored.update.edges.get('changed')).toMatchObject({source: 'c', target: 'd', type: 'quadratic',
        style: {curveOffset: -24}});
    expect(restored.add.edges.has('changed')).toBe(false);
    expect(restored.remove.edges.has('changed')).toBe(false);
    expect(old.source).toBe('a');
    expect(moved.type).toBe('line');
    expect(loop.style).toBeUndefined();
});

const nativeGeometry = (edges: EdgeData[]) => {
    const nodeIds = new Set(edges.flatMap(value => [value.source, value.target]));
    const graph = new Graph({autoResize: false, data: {nodes: [...nodeIds].map(id => ({id})), edges}});
    // Supply the extension context through real public Graph data operations.
    // No canvas/runtime is created; the installed public transform still runs
    // its actual grouping, direction, spread and DrawData reassignment code.
    const model = {
        getEdgeData: () => graph.getEdgeData(),
        getRelatedEdgesData: (id: string) => graph.getRelatedEdgesData(id),
        getElementDataById: (id: string) => graph.getElementData(id),
        getParentData: (id: string) => graph.getParentData(id, 'combo'),
        getChanges: () => [],
    };
    const context = {graph, model} as unknown as ConstructorParameters<typeof ProcessParallelEdges>[0];
    const native = new ProcessParallelEdges(context, {type: 'process-parallel-edges',
        mode: 'bundle', distance: 24, loopMode: 'spread'});
    try { return geometryOf(native.beforeDraw(draw({add: graph.getEdgeData()}))); }
    finally { native.destroy(); graph.destroy(); }
};
const geometryOf = (input: DrawData) => [...input.add.edges.values(), ...input.update.edges.values()]
    .map(value => ({id: value.id, type: value.type, curveOffset: value.style?.curveOffset,
        loopDist: value.style?.loopDist, loopPlacement: value.style?.loopPlacement}));

test.each([1, 2, 3, 4, 5, 6])('matches the installed public bundle transform at distance 24 for %i directed lanes', count => {
    const edges = [
        ...Array.from({length: count}, (_, index) => edge(`lane-${index}`, index % 3 === 1 ? 'b' : 'a',
            index % 3 === 1 ? 'a' : 'b')),
        ...Array.from({length: 10}, (_, index) => edge(`loop-${index}`, 'self', 'self')),
        edge('single', 'x', 'y'),
    ];
    const {transform} = fixture(edges);
    const projected = transform.beforeDraw(draw({add: edges}));
    expect(geometryOf(projected)).toEqual(nativeGeometry(edges));
    edges.forEach(value => expect(value.style).toBeUndefined());
});

// Reverse endpoints reverse the path's bend direction. Compare lanes relative
// to the first edge so distinct physical curves do not get mistaken for equal
// numeric curveOffset values on oppositely directed edges.
const relativeOffsets = (values: EdgeData[]) => {
    const source = values[0].source;
    return values.map(value => (value.style?.curveOffset as number) * (value.source === source ? 1 : -1));
};
test.each([22, 28, 134, 534])('bounds %i parallel lanes without merging, hiding or changing direction/order', count => {
    const direct = Array.from({length: count}, (_, index) => Object.freeze(edge(`lane-${index}`)));
    const mixed = direct.map((value, index) => Object.freeze(index % 3 === 1
        ? {...value, source: 'b', target: 'a'} : value));
    const reversed = mixed.map(value => Object.freeze({...value, source: value.target, target: value.source}));
    const expected = relativeOffsets([...fixture(direct).transform.beforeDraw(draw({add: direct})).add.edges.values()]);
    for (const input of [mixed, reversed]) {
        const {transform, getEdgeData} = fixture(input);
        const projected = transform.beforeDraw(draw({add: input}));
        const values = [...projected.add.edges.values()];
        expect(values.map(value => value.id)).toEqual(input.map(value => value.id));
        expect(projected.update.edges.size + projected.remove.edges.size).toBe(0);
        const offsets = relativeOffsets(values);
        expect(new Set(offsets).size).toBe(count);
        expect(Math.max(...offsets.map(Math.abs))).toBeLessThanOrEqual(480);
        expect(Math.max(...offsets.map(Math.abs))).toBeCloseTo(480, 10);
        expect(offsets).toEqual(expected);
        values.forEach((value, index) => {
            expect(value.source).toBe(input[index].source);
            expect(value.target).toBe(input[index].target);
            expect(value.data?.raw).toBe(input[index].data?.raw);
            expect(value.style?.visibility).toBeUndefined();
            expect(input[index].style).toBeUndefined();
        });
        getEdgeData.mockClear();
        const styled = {...input[17], style: {stroke: '#f00'}, states: ['selected']};
        const changed = transform.beforeDraw(draw({update: [styled]}));
        expect(getEdgeData).not.toHaveBeenCalled();
        expect([...changed.update.edges.keys()]).toEqual(['lane-17']);
        expect(changed.update.edges.get('lane-17')?.style?.curveOffset).toBe(values[17].style?.curveOffset);
    }
});

test('crosses the 21-to-22-lane limit and restores undo geometry, updating only that group', () => {
    const lanes = Array.from({length: 21}, (_, index) => edge(`lane-${index}`));
    const unaffected = [edge('other-one', 'c', 'd'), edge('other-two', 'd', 'c'),
        ...Array.from({length: 10}, (_, index) => edge(`self-${index}`, 'self', 'self'))];
    const initial = [...lanes, ...unaffected];
    const {transform, set, getEdgeData} = fixture(initial);
    const before = transform.beforeDraw(draw({add: initial}));
    const originalOffsets = lanes.map(value => before.add.edges.get(value.id!)?.style?.curveOffset);
    expect(geometryOf(before).slice(0, lanes.length)).toEqual(nativeGeometry(lanes));
    expect(Math.max(...originalOffsets.map(value => Math.abs(value as number)))).toBe(480);
    const extra = edge('lane-21');
    set([...initial, extra]);
    getEdgeData.mockClear();
    const added = transform.beforeDraw(draw({add: [extra]}));
    expect([...added.add.edges.keys()]).toEqual(['lane-21']);
    // The positive outer lane stays at +480 on both sides of the threshold;
    // only the other twenty existing geometries need an engine update.
    const changedLanes = lanes.slice(0, -1);
    expect([...added.update.edges.keys()]).toEqual(changedLanes.map(value => value.id));
    expect(Math.abs(added.add.edges.get('lane-21')?.style?.curveOffset as number)).toBe(480);
    expect(fullReads(getEdgeData)).toBe(1);
    expect(getEdgeData.mock.calls.filter(args => args.length).length).toBe(20);
    set(initial);
    getEdgeData.mockClear();
    const undo = transform.beforeDraw(draw({remove: [extra]}));
    expect([...undo.remove.edges.keys()]).toEqual(['lane-21']);
    expect([...undo.update.edges.keys()]).toEqual(changedLanes.map(value => value.id));
    expect(lanes.map(value => (undo.update.edges.get(value.id!) ?? before.add.edges.get(value.id!))
        ?.style?.curveOffset)).toEqual(originalOffsets);
    expect(fullReads(getEdgeData)).toBe(1);
    set([...initial, extra]);
    const redo = transform.beforeDraw(draw({add: [extra]}));
    expect(offsets(redo)).toEqual(offsets(added));
    unaffected.forEach(value => {
        expect(added.update.edges.has(value.id!)).toBe(false);
        expect(undo.update.edges.has(value.id!)).toBe(false);
        expect(redo.update.edges.has(value.id!)).toBe(false);
    });
});

test('large parallel fan endpoint reads remain linear and loop distances remain unbounded by the fan cap', () => {
    let reads = 0;
    const lanes = Array.from({length: 534}, (_, index): EdgeData => ({id: `lane-${index}`,
        get source() { reads++; return 'a'; }, get target() { reads++; return 'b'; }}));
    const loops = Array.from({length: 154}, (_, index) => edge(`loop-${index}`, 'self', 'self'));
    const {transform, getEdgeData} = fixture([...lanes, ...loops]);
    const projected = transform.beforeDraw(draw({add: [...lanes, ...loops]}));
    expect(projected.add.edges.size).toBe(lanes.length + loops.length);
    expect(reads).toBeLessThanOrEqual(12 * lanes.length);
    expect(fullReads(getEdgeData)).toBe(1);
    expect(projected.add.edges.get('loop-153')?.style?.loopDist).toBe(50 + 19 * 24);
    expect(projected.add.edges.get('loop-153')?.style?.loopDist).toBeGreaterThan(480);
});
