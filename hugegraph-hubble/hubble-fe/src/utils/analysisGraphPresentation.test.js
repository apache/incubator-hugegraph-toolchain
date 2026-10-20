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

import {analysisLayout, normalizeLayoutSettings, getAnalysisStyles, applyAnalysisStyles} from './analysisGraphPresentation';
import {adaptAnalysisGraph} from './analysisGraphData';
import {hubbleRenderId} from '../visualization/hubble/adapter';
import {immutableCopy} from '../visualization/model/ownership';

const fixture = () => adaptAnalysisGraph({
    vertices: [{id: 0, label: 'person', properties: {nested: ['Alice']}},
        {id: '0', label: 'city', properties: {nested: ['Paris']}}],
    edges: [{id: 'knows', source: 0, target: '0', label: 'knows', properties: {nested: [1]}},
        {id: 'visits', source: '0', target: 0, label: 'visits', properties: {}}],
}, {vertexMeta: [{name: 'person', style: {icon: 'user', color: '#008800'}}]});

test.each([
    ['force', 'd3-force'], ['force2', 'd3-force'], ['relationship', 'columns'], ['forceAtlas', 'force-atlas2'],
    ['dagre', 'dagre'], ['grid', 'grid'], ['customGrid', 'grid'], ['radial', 'radial'],
    ['circular', 'circular'], ['concentric', 'concentric'],
])('maps business layout %s to %s', (selection, type) => {
    expect(analysisLayout(selection)).toEqual({type});
    expect(analysisLayout({layout: selection})).toEqual({type});
});

test('radial selection preserves zero, strings and bigint in a stable typed focus ID', () => {
    for (const startId of [0, '0', 12345678901234567890n]) {
        expect(analysisLayout({layout: 'radial', startId})).toEqual({type: 'radial',
            options: {focusNode: hubbleRenderId('node', startId)}});
    }
    expect(analysisLayout(undefined)).toEqual({type: 'd3-force'});
});

test('converts force2 form values to official D3 forces, retaining zero and negative strength semantics', () => {
    const input = {type: 'force2', linkDistance: 0, nodeStrength: 300, nodeSize: [80, 60], nodeSpacing: 20,
        preventOverlap: true, maxIteration: 0, relayoutAtChangeData: false, pipes: [{type: 'grid'}],
        onTick: () => {}, nodeSpacingCallback: () => 10};
    const result = normalizeLayoutSettings(input);
    expect(result).toEqual({type: 'd3-force', options: {
        link: {distance: 0}, manyBody: {strength: -300}, collide: {radius: 50}, iterations: 0,
    }});
    expect(input.nodeStrength).toBe(300);
    expect(normalizeLayoutSettings({type: 'force', nodeStrength: -40, preventOverlap: false,
        nodeSize: 100, nodeSpacing: 25})).toEqual({type: 'd3-force',
        options: {manyBody: {strength: -40}, collide: false}});
    expect(normalizeLayoutSettings({type: 'force2', nodeStrength: 0})).toEqual({type: 'd3-force',
        options: {manyBody: {strength: -0}}});
});

test('canonical D3 false forces and data options survive without mutation or callback leakage', () => {
    const input = immutableCopy({type: 'd3-force', options: {link: false, manyBody: false, collide: false,
        iterations: 300, x: {strength: 0, x: 0}, alphaDecay: 0.01}});
    expect(normalizeLayoutSettings(input)).toEqual(input);
    expect(normalizeLayoutSettings({type: 'd3-force', options: {
        link: {distance: () => 10, strength: 0.2}, randomSource: () => 0.5,
    }})).toEqual({type: 'd3-force', options: {link: {strength: 0.2}}});
});

test('an explicit overlap form edit can enable a previously disabled collision force', () => {
    expect(normalizeLayoutSettings({type: 'd3-force', options: {
        collide: false, preventOverlap: true, nodeSize: 70, nodeSpacing: 40,
    }})).toEqual({type: 'd3-force', options: {collide: {radius: 55}}});
});

test('all six form layouts retain valid settings and remove old engine values', () => {
    expect(normalizeLayoutSettings({type: 'customGrid', rows: '', cols: 0, preventOverlap: false,
        position: () => ({row: 1})})).toEqual({type: 'grid', options: {preventOverlap: false}});
    expect(normalizeLayoutSettings({type: 'grid', rows: 2, cols: 3})).toEqual({type: 'grid', options: {rows: 2, cols: 3}});
    expect(normalizeLayoutSettings({type: 'circular', startRadius: 0, endRadius: 300, clockwise: false,
        ordering: null})).toEqual({type: 'circular',
        options: {startRadius: 0, endRadius: 300, clockwise: false, ordering: null}});
    expect(normalizeLayoutSettings({type: 'concentric', nodeSize: 30, nodeSpacing: 80,
        preventOverlap: true, equidistant: false})).toEqual({type: 'concentric',
        options: {nodeSize: 30, nodeSpacing: 80, preventOverlap: true, equidistant: false}});
    expect(normalizeLayoutSettings({type: 'dagre', rankdir: 'LR', nodesep: 0, ranksep: 50,
        relayoutAtChangeData: true})).toEqual({type: 'dagre', options: {rankdir: 'LR', nodesep: 0, ranksep: 50}});
    expect(normalizeLayoutSettings({type: 'radial', focusNode: 0, unitRadius: 0, strictRadial: false}))
        .toEqual({type: 'radial', options: {focusNode: hubbleRenderId('node', 0), unitRadius: 0, strictRadial: false}});
    expect(normalizeLayoutSettings({type: 'radial', options: {focusNode: hubbleRenderId('node', 0)}}))
        .toEqual({type: 'radial', options: {focusNode: hubbleRenderId('node', 0)}});
    expect(normalizeLayoutSettings({type: 'radial', focusNode: ''})).toEqual({type: 'radial', options: {}});
});

test('style projection creates writable drafts without raw data or shared style arrays', () => {
    const graph = fixture();
    graph.nodes[0].style.size = [40, 50];
    graph.edges[0].style.lineDash = [4, 2];
    const frozen = immutableCopy(graph);
    const styles = getAnalysisStyles(frozen);
    expect(styles.nodes.person).toMatchObject({type: 'circle', size: [40, 50],
        style: {fill: '#008800'}, icon: {_iconName: 'UserOutlined', show: true}});
    expect(styles.edges.knows.style).toMatchObject({lineDash: [4, 2], endArrow: false});
    expect(styles.nodes.person.raw).toBeUndefined();
    expect(styles.nodes.person.properties).toBeUndefined();
    styles.nodes.person.size.push(99);
    styles.edges.knows.style.lineDash.push(99);
    expect(frozen.nodes[0].style.size).toEqual([40, 50]);
    expect(frozen.edges[0].style.lineDash).toEqual([4, 2]);
});

test('round-tripping unchanged style drafts or an empty partial produces no element writes', () => {
    const graph = immutableCopy(fixture());
    expect(applyAnalysisStyles(graph, getAnalysisStyles(graph))).toEqual({nodes: [], edges: []});
    expect(applyAnalysisStyles(graph, {nodes: {person: {style: {fill: '#008800'}}}}))
        .toEqual({nodes: [], edges: []});
    expect(applyAnalysisStyles(graph)).toEqual({nodes: [], edges: []});
});

test('returns only changed elements while preserving raw, coordinates, pin, group, highlight and visibility', () => {
    const input = fixture();
    input.nodes[0].data = {...input.nodes[0].data, fixed: true, highlighted: true, group: 'start'};
    input.nodes[0].style = {...input.nodes[0].style, x: 12, y: 34, visibility: 'hidden'};
    input.edges[0].style = {...input.edges[0].style, visibility: 'hidden', controlPoints: [[1, 2], [3, 4]]};
    const graph = immutableCopy(input);
    const changed = applyAnalysisStyles(graph, {
        nodes: {person: {style: {fillOpacity: 0, strokeOpacity: 0, lineWidth: 0},
            labelCfg: {position: 'right', style: {fontSize: 0, fill: '#ff0000'}}}},
        edges: {knows: {style: {lineWidth: 0, lineDash: [0, 2], endArrow: false, opacity: 0}}},
    });
    expect(changed.nodes).toHaveLength(1);
    expect(changed.edges).toHaveLength(1);
    expect(changed.nodes[0].data.raw).toBe(graph.nodes[0].data.raw);
    expect(changed.nodes[0].data).toMatchObject({fixed: true, highlighted: true, group: 'start'});
    expect(changed.nodes[0].style).toMatchObject({x: 12, y: 34, visibility: 'hidden',
        fillOpacity: 0, strokeOpacity: 0, lineWidth: 0, labelFontSize: 0, labelFill: '#ff0000', labelPlacement: 'right'});
    expect(changed.edges[0].style).toMatchObject({lineWidth: 0, lineDash: [0, 2], endArrow: false,
        opacity: 0, visibility: 'hidden', controlPoints: [[1, 2], [3, 4]]});
    expect(changed.edges[0].data.raw).toBe(graph.edges[0].data.raw);
    expect(graph.nodes[0].style.fillOpacity).toBe(0.4);
});

test('shape and icon changes share one element patch, and hiding clears the old engine icon', () => {
    const graph = immutableCopy(fixture());
    const updates = applyAnalysisStyles(graph, {nodes: {person: {type: 'diamond',
        icon: {_iconName: 'HomeOutlined', show: true, fontSize: 24, fill: '#123456'}}}});
    expect(updates.nodes[0].data).toMatchObject({shape: 'diamond', icon: 'HomeOutlined'});
    expect(updates.nodes[0].style).toMatchObject({iconFontSize: 24, iconWidth: 24, iconHeight: 24,
        iconFill: '#123456'});
    expect(updates.nodes[0].style.iconSrc).toContain('data:image/svg+xml,');
    expect(updates.nodes[0].style.iconSrc).not.toBe(graph.nodes[0].style.iconSrc);
    const hidden = applyAnalysisStyles(graph, {nodes: {person: {icon: {show: false}}}});
    expect(hidden.nodes[0].data.icon).toBeUndefined();
    expect(hidden.nodes[0].style).toMatchObject({icon: false, iconSrc: ''});
    const restored = applyAnalysisStyles({nodes: hidden.nodes, edges: []}, {nodes: {person: {
        icon: {show: true, _iconName: 'UserOutlined'},
    }}});
    expect(restored.nodes[0].style.icon).toBe(true);
    expect(restored.nodes[0].data.icon).toBe('UserOutlined');
});
