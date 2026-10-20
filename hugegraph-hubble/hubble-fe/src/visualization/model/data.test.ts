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

import {describe, expect, test} from '@jest/globals';
import {GraphDataError, mergeGraphData, normalizeGraphData, preserveNodePositions, renderId, sameTopology} from './data';
import {buildAdjacencyIndex, relatedElements} from './adjacency';
import type {GraphData, GraphEdge, GraphNode, GraphStyle} from './types';

const node = (id: string, value = 1): GraphNode => ({id, data: {raw: {id, value}}});
const edge = (id: string, source: string, target: string): GraphEdge => ({id, source, target, data: {raw: {id}}});
const graph = (): GraphData => ({nodes: [node('a'), node('b'), node('c')], edges: [edge('ab', 'a', 'b')]});

describe('stable rendering identity and validation', () => {
    test('distinguishes numeric, string and element-kind IDs, including delimiters', () => {
        expect(new Set([renderId('node', 1), renderId('node', '1'), renderId('edge', 1),
            renderId('node', 'number:1'), renderId('node', '%3A')]).size).toBe(5);
        expect(renderId('node', 'a:b')).toBe('node:string:a%3Ab');
        expect(() => renderId('node', NaN)).toThrow(GraphDataError);
    });
    test('deduplicates repeated records deterministically without dropping parallel edges or self-loops', () => {
        const data = normalizeGraphData({nodes: [node('a'), node('b'), node('a', 2)],
            edges: [edge('ab', 'a', 'b'), edge('aa', 'a', 'a'), edge('ab2', 'a', 'b'), edge('ab', 'a', 'b')]});
        expect(data.nodes.map(item => item.id)).toEqual(['a', 'b']);
        expect(data.nodes[0].data.raw.value).toBe(2);
        expect(data.edges.map(item => item.id)).toEqual(['ab', 'aa', 'ab2']);
    });
    test('reports missing endpoints, conflicting duplicate endpoints and cross-kind collisions', () => {
        expect(() => normalizeGraphData({nodes: [node('a')], edges: [edge('ab', 'a', 'b')]})).toThrow('missing node b');
        expect(() => normalizeGraphData({nodes: graph().nodes,
            edges: [edge('ab', 'a', 'b'), edge('ab', 'a', 'c')]})).toThrow('Conflicting endpoints');
        expect(() => normalizeGraphData({nodes: [node('a')], edges: [edge('a', 'a', 'a')]})).toThrow('collide');
    });
    test.each([NaN, Infinity, () => 1, {nested: undefined}])('rejects non-JSON style fields', value => {
        const invalid = {...node('a'), style: {fill: value} as unknown as GraphStyle};
        expect(() => normalizeGraphData({nodes: [invalid], edges: []})).toThrow(GraphDataError);
    });
});

describe('shared graph merge', () => {
    test('accepts an edge-only expansion and makes repeated expansion a no-op', () => {
        const current = graph();
        const incoming = {nodes: [], edges: [edge('bc', 'b', 'c')]};
        const merged = mergeGraphData(current, incoming);
        expect(merged.changed).toBe(true);
        expect(merged.addedEdgeIds).toEqual(['bc']);
        expect(merged.data.edges).toHaveLength(2);
        const repeat = mergeGraphData(merged.data, incoming);
        expect(repeat.changed).toBe(false);
        expect(repeat.data).toBe(merged.data);
        expect(current.edges).toHaveLength(1);
    });
    test('updates properties and styles while retaining coordinates, pin and existing groups', () => {
        const current = graph();
        current.nodes[0] = {...current.nodes[0], data: {...current.nodes[0].data, fixed: true, group: 'start'},
            style: {x: 4, y: 5, fill: 'red'}};
        const incoming = {...node('a', 2), data: {...node('a', 2).data, fixed: false},
            style: {x: 99, y: 99, fill: 'blue'}};
        const merged = mergeGraphData(current, {nodes: [incoming], edges: []});
        expect(merged.updatedNodeIds).toEqual(['a']);
        expect(merged.data.nodes[0]).toMatchObject({data: {raw: {value: 2}, fixed: true, group: 'start'},
            style: {x: 4, y: 5, fill: 'blue'}});
        expect(current.nodes[0].style?.fill).toBe('red');
    });
    test('counts edge record updates and rejects dangling expansion atomically', () => {
        const current = graph();
        const updated = {...current.edges[0], data: {raw: {id: 'ab', properties: {weight: 2}}}};
        expect(mergeGraphData(current, {nodes: [], edges: [updated]}).updatedEdgeIds).toEqual(['ab']);
        expect(() => mergeGraphData(current, {nodes: [node('d')], edges: [edge('dx', 'd', 'missing')]}))
            .toThrow('missing node missing');
        expect(current.nodes).toHaveLength(3);
    });
    test('does not treat an identical unstyled node as an update', () => {
        expect(mergeGraphData(graph(), {nodes: [node('a')], edges: []}).changed).toBe(false);
    });
});

test('topology ignores ordering but detects changed endpoints, preserving placement only for a refresh', () => {
    const current = graph();
    current.nodes[0].style = {x: 12, y: 20};
    current.nodes[0].data.fixed = true;
    const refresh = {nodes: [...graph().nodes].reverse(), edges: graph().edges};
    expect(sameTopology(current, refresh)).toBe(true);
    expect(preserveNodePositions(current, refresh).nodes.find(item => item.id === 'a'))
        .toMatchObject({style: {x: 12, y: 20}, data: {fixed: true}});
    const changed = {...refresh, edges: [edge('ab', 'a', 'c')]};
    expect(sameTopology(current, changed)).toBe(false);
    expect(preserveNodePositions(current, changed)).toBe(changed);
});

test('adjacency lookup retains loops/parallel edges and only returns the selected neighborhood', () => {
    const data = graph();
    data.edges.push(edge('ab2', 'a', 'b'), edge('aa', 'a', 'a'));
    const index = buildAdjacencyIndex(data);
    expect(relatedElements(index, 'node', 'a')).toEqual({nodeIds: ['a', 'b'], edgeIds: ['ab', 'ab2', 'aa']});
    expect(relatedElements(index, 'edge', 'aa')).toEqual({nodeIds: ['a'], edgeIds: ['aa']});
    expect(relatedElements(index, 'node', 'c')).toEqual({nodeIds: ['c'], edgeIds: []});
    expect(relatedElements(index, 'node', 'missing')).toEqual({nodeIds: [], edgeIds: []});
});


test('repeated expansion retains renderer edge geometry without reporting a false data change', () => {
    const raw = {id: 'edge', label: 'knows'};
    const current = {nodes: [{id: 'a', data: {raw: {id: 'a'}}}], edges: [
        {id: 'edge', source: 'a', target: 'a', data: {raw}, style: {curveOffset: 24}},
    ]};
    expect(mergeGraphData(current, {nodes: [], edges: [
        {id: 'edge', source: 'a', target: 'a', data: {raw}},
    ]})).toMatchObject({changed: false, data: current});
});


test('expansion preserves existing appearance and display data while updating raw records', () => {
    const current = graph();
    current.nodes[0] = {...current.nodes[0], data: {...current.nodes[0].data,
        fixed: true, shape: 'diamond', icon: 'star', group: 'saved', label: 'old-label', legend: 'old-legend'},
    style: {x: 10, y: 20, fill: 'red', size: 80, labelText: 'saved'}};
    current.edges[0] = {...current.edges[0], data: {...current.edges[0].data, group: 'saved-edge'},
        style: {stroke: 'purple', lineWidth: 5, labelText: 'saved-edge'}};
    const incoming = {nodes: [{...node('a', 2), data: {...node('a', 2).data, fixed: false,
        shape: 'circle', icon: 'default', group: 'default', label: 'new-label', legend: 'new-legend'},
    style: {x: 99, y: 99, fill: 'blue', size: 20, labelText: 'default', stroke: 'black'}},
    {...node('d'), style: {fill: 'green'}}], edges: [
        {...edge('ab', 'a', 'b'), data: {raw: {id: 'ab', weight: 2}, group: 'default-edge'},
            style: {stroke: 'black', lineWidth: 1}}, {...edge('ad', 'a', 'd'), style: {stroke: 'green'}},
    ]};
    const merged = mergeGraphData(current, incoming, {preserveAppearance: true});
    expect(merged.data.nodes[0]).toEqual({...current.nodes[0], data: {...current.nodes[0].data, raw: incoming.nodes[0].data.raw}});
    expect(merged.data.edges[0]).toEqual({...current.edges[0], data: {...current.edges[0].data, raw: incoming.edges[0].data.raw}});
    expect(merged.data.nodes.at(-1)).toEqual(incoming.nodes[1]);
    expect(merged.data.edges.at(-1)).toEqual(incoming.edges[1]);
    expect(merged.updatedNodeIds).toEqual(['a']);
    expect(merged.updatedEdgeIds).toEqual(['ab']);
    expect(mergeGraphData(merged.data, incoming, {preserveAppearance: true})).toMatchObject({changed: false, data: merged.data});
    expect(current.nodes[0].data.raw.value).toBe(1);
});

test('appearance preservation does not introduce default appearance into an existing unstyled element', () => {
    const current = graph();
    const incoming = {nodes: [{...node('a'), data: {...node('a').data, shape: 'circle'}, style: {fill: 'blue'}}],
        edges: [{...edge('ab', 'a', 'b'), style: {stroke: 'blue'}}]};
    expect(mergeGraphData(current, incoming, {preserveAppearance: true})).toMatchObject({changed: false, data: current});
    const refresh = preserveNodePositions(current, {...current, nodes: [incoming.nodes[0], ...current.nodes.slice(1)]});
    expect(refresh.nodes[0].style?.fill).toBe('blue');
});
