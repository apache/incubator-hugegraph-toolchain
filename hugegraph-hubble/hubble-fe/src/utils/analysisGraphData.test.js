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

import JSONbig from 'json-bigint';
import {adaptAnalysisGraph, toCanvas3DData, toElementDetails} from './analysisGraphData';
import {hubbleRenderId} from '../visualization/hubble/adapter';
import {immutableCopy} from '../visualization/model/ownership';
import {ALGORITHM_NAME} from './constants';

const fixture = () => ({
    vertices: [
        {id: 0, label: 'person', properties: {names: ['Alice']}, statistics: {degree: 2}, extra: {enabled: true}},
        {id: '0', label: 'person', properties: {names: ['Bob']}},
        {id: 2, label: 'person', properties: {}},
    ],
    edges: [{id: 0, source: 0, target: '0', label: 'knows', properties: {weights: [1, 2]}}],
});
const metadata = () => ({
    vertexMeta: [{name: 'person', style: {color: '#001122', size: 'BIG'}}],
    edgeMeta: [{name: 'knows', edgelabel_type: 'SUB', style: {with_arrow: true}}],
});

test('keeps raw records, typed IDs, zero start and existing saved style at the boundary', () => {
    const input = fixture();
    const saved = {nodes: {person: {type: 'diamond', style: {fillOpacity: 0}}}};
    const graph = adaptAnalysisGraph(input, metadata(), saved, {startId: 0});
    expect(graph.nodes[0]).toMatchObject({id: hubbleRenderId('node', 0),
        data: {shape: 'diamond', highlighted: true}, style: {fill: '#001122', fillOpacity: 0, lineWidth: 3}});
    expect(graph.nodes[1].data.highlighted).toBeUndefined();
    expect(graph.nodes[0].id).not.toBe(graph.nodes[1].id);
    expect(graph.nodes[0].data.raw).toBe(input.vertices[0]);
    expect(graph.edges[0].data.raw).toBe(input.edges[0]);
    expect(input.vertices[0].extra).toEqual({enabled: true});
    expect(saved.nodes.person.style).toEqual({fillOpacity: 0});
});

test('maps endpoint groups with typed IDs, preserving end precedence and middle nodes without combos', () => {
    const graph = adaptAnalysisGraph(fixture(), {}, {}, {endPointsId: {startNodes: [0], endNodes: ['0']}});
    expect(graph.nodes.map(node => node.data.group)).toEqual(['start', 'end', 'middle']);
    expect(graph.combos).toBeUndefined();
    expect(graph.nodes.every(node => node.comboId === undefined && node.tag === undefined)).toBe(true);
    const overlap = adaptAnalysisGraph(fixture(), {}, {}, {endPointsId: {startNodes: [0], endNodes: [0]}});
    expect(overlap.nodes[0].data.group).toBe('end');
});

test.each([ALGORITHM_NAME.SAME_NEIGHBORS, ALGORITHM_NAME.ALLPATHS])(
    '%s emphasizes endpoints and all result edges while leaving middle node styles intact', name => {
        const graph = adaptAnalysisGraph(fixture(), {}, {},
            {endPointsId: {startNodes: [0], endNodes: ['0']}}, name);
        expect(graph.nodes.map(node => Boolean(node.data.highlighted))).toEqual([true, true, false]);
        expect(graph.edges[0]).toMatchObject({data: {highlighted: true}, style: {stroke: '#d48806', lineWidth: 3}});
        expect(graph.nodes[2].style.lineWidth).toBe(1);
    }
);

test('shortest path highlights every returned node and edge, including zero-valued start', () => {
    const graph = adaptAnalysisGraph(fixture(), {}, {}, {startId: 0}, ALGORITHM_NAME.SHORTEST_PATH);
    expect([...graph.nodes, ...graph.edges].every(element => element.data.highlighted)).toBe(true);
    expect([...graph.nodes, ...graph.edges].every(element => element.style.lineWidth >= 3)).toBe(true);
});

test('keeps lossless numbers, bigint and strings separate for endpoints and selection details', () => {
    const parsed = JSONbig.parse('{"vertices":[{"id":900719925474099312345,"label":"person"}],"edges":[]}');
    const bigId = parsed.vertices[0].id;
    parsed.vertices.push({id: bigId.toFixed(), label: 'person'}, {id: 900719925474099312345n, label: 'person'});
    parsed.edges.push({id: bigId, source: bigId, target: bigId.toFixed(), label: 'knows', properties: {}});
    const graph = adaptAnalysisGraph(parsed, {}, {}, {endPointsId: {startNodes: [bigId], endNodes: [bigId.toFixed()]}});
    expect(new Set(graph.nodes.map(node => node.id)).size).toBe(3);
    expect(graph.nodes.map(node => node.data.group)).toEqual(['start', 'end', 'middle']);
    expect(graph.edges[0].source).toBe(graph.nodes[0].id);
    expect(graph.edges[0].target).toBe(graph.nodes[1].id);
    const details = toElementDetails({kind: 'edge', id: graph.edges[0].id, data: graph.edges[0].data});
    expect(details.id).toBe(bigId);
    expect(details.source).toBe(bigId);
    expect(details.id.toFixed()).toBe('900719925474099312345');
    expect(details.raw).toBe(parsed.edges[0]);
});

test('forwards normalizeOptions for an edge-only expansion rather than silently dropping it', () => {
    const full = adaptAnalysisGraph(fixture());
    const partial = {vertices: [], edges: [{id: 'new-edge', source: '0', target: 2, label: 'knows'}]};
    const graph = adaptAnalysisGraph(partial, {}, {}, {}, undefined,
        {existingNodeIds: new Set(full.nodes.map(node => node.id))});
    expect(graph.nodes).toEqual([]);
    expect(graph.edges).toHaveLength(1);
    expect(graph.edges[0].data.raw).toBe(partial.edges[0]);
    expect(() => adaptAnalysisGraph(partial)).toThrow();
});

test('3D adapter detaches every mutable nested property, size and style from frozen 2D data and metadata', () => {
    const input = fixture();
    const meta = immutableCopy(metadata());
    const graph = immutableCopy(adaptAnalysisGraph(input, meta,
        {nodes: {person: {size: [40, 50]}}, edges: {knows: {style: {endArrow: false, lineDash: [3, 2]}}}}));
    const output = toCanvas3DData(graph, meta);
    expect(output.nodes[0]).toMatchObject({id: graph.nodes[0].id, rawId: 0, itemType: 'person', size: [40, 50]});
    expect(output.edges[0]).toMatchObject({id: graph.edges[0].id, rawId: 0,
        source: output.nodes[0].id, target: output.nodes[1].id,
        metaConfig: {edgelabel_type: 'SUB', style: {with_arrow: false}}});
    output.nodes[0].x = 123;
    output.nodes[0].properties.names.push('changed');
    output.nodes[0].size[0] = 999;
    output.nodes[0].style.fill = '#ffffff';
    output.nodes[0].metaConfig.style.color = 'red';
    output.edges[0].source = output.nodes[0];
    output.edges[0].properties.weights.push(9);
    output.edges[0].style.lineDash.push(10);
    output.edges[0].metaConfig.style.with_arrow = true;
    expect(graph.nodes[0].style).toMatchObject({fill: '#001122', size: [40, 50]});
    expect(graph.nodes[0].data.raw.properties.names).toEqual(['Alice']);
    expect(graph.edges[0].data.raw.properties.weights).toEqual([1, 2]);
    expect(graph.edges[0].style.lineDash).toEqual([3, 2]);
    expect(meta.vertexMeta[0].style.color).toBe('#001122');
    expect(input.edges[0].source).toBe(0);
});

test('3D raw IDs retain exact BigNumber prototypes and native bigint', () => {
    const raw = JSONbig.parse('{"vertices":[{"id":900719925474099312345,"label":"person"}],"edges":[]}');
    raw.vertices.push({id: 900719925474099312345n, label: 'person'});
    const graph = immutableCopy(adaptAnalysisGraph(raw));
    const output = toCanvas3DData(graph);
    expect(output.nodes[0].rawId).not.toBe(graph.nodes[0].data.raw.id);
    expect(output.nodes[0].rawId.toFixed()).toBe('900719925474099312345');
    expect(typeof output.nodes[1].rawId).toBe('bigint');
    expect(output.nodes[0].id).not.toBe(output.nodes[1].id);
});

test('3D projection retains the current filter and excludes edges with hidden endpoints', () => {
    const raw = fixture();
    raw.edges.push({id: 'visible', source: '0', target: 2, label: 'knows'},
        {id: 'hidden-edge', source: '0', target: 2, label: 'knows'});
    const graph = adaptAnalysisGraph(raw);
    graph.nodes[0].style.visibility = 'hidden';
    graph.edges[2].style.visibility = 'hidden';
    const source = immutableCopy(graph);
    const output = toCanvas3DData(source);
    expect(output.nodes.map(node => node.rawId)).toEqual(['0', 2]);
    expect(output.edges.map(edge => edge.rawId)).toEqual(['visible']);
    expect(source.nodes).toHaveLength(3);
    expect(source.edges).toHaveLength(3);
    expect(output.edges[0]).toMatchObject({curvature: 0, rotation: 0});
});

test('3D geometry groups parallel, reversed and more than eight self-loop edges in linear passes', () => {
    const raw = fixture();
    raw.edges = [
        {id: 'single', source: '0', target: 2, label: 'knows'},
        ...Array.from({length: 10}, (_, index) => ({id: `parallel-${index}`,
            source: index % 2 ? '0' : 0, target: index % 2 ? 0 : '0', label: 'knows'})),
        ...Array.from({length: 10}, (_, index) => ({id: `loop-${index}`, source: 0, target: 0, label: 'knows'})),
    ];
    const data = toCanvas3DData(adaptAnalysisGraph(raw));
    expect(data.edges[0]).toMatchObject({curvature: 0, rotation: 0});
    expect(new Set(data.edges.slice(1, 11).map(edge => `${edge.curvature}:${edge.rotation}`)).size).toBe(10);
    expect(data.edges[2].curvature).toBeLessThan(0);
    expect(data.edges[11]).toMatchObject({curvature: 0.5, rotation: 0});
    expect(data.edges[19]).toMatchObject({curvature: 0.7, rotation: 0});
    expect(raw.edges.every(edge => edge.curvature === undefined)).toBe(true);
});

test('drawers receive complete raw values, separate rendering IDs and canonical metadata', () => {
    const raw = fixture();
    const meta = metadata();
    const graph = adaptAnalysisGraph(raw, meta, {nodes: {person: {type: 'diamond'}}});
    const node = graph.nodes[0];
    expect(toElementDetails({kind: 'node', id: node.id, data: node.data}, meta)).toEqual({
        kind: 'node', renderingId: node.id, id: 0, label: 'person', rawLabel: 'person', itemType: 'person',
        properties: raw.vertices[0].properties, statistics: raw.vertices[0].statistics,
        source: undefined, target: undefined, type: 'diamond', metaConfig: meta.vertexMeta[0], raw: raw.vertices[0],
    });
    const edge = graph.edges[0];
    expect(toElementDetails({kind: 'edge', id: edge.id, data: edge.data}, meta)).toMatchObject({
        type: 'line', source: 0, target: '0', id: 0, metaConfig: meta.edgeMeta[0],
    });
    expect(toElementDetails(null)).toBeNull();
});
