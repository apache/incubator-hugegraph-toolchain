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

import {createGraphController} from '../src/visualization/runtime';
import type {GraphData} from '../src/visualization/model/types';

// The painted anchor bounds are declared in world units, independent of export.
// [30,30]-[670,270] plus the public PNG's 30px border is exactly 700x300.
const fixtureData = (complex: boolean): GraphData => ({nodes: [
    {id: 'left', data: {raw: {id: 'left'}}, style: {x: 56, y: 56, size: 40,
        fill: '#627eea', stroke: '#627eea', lineWidth: 12, labelText: 'A', labelPlacement: 'center'}},
    {id: 'middle', data: {raw: {id: 'middle'}}, style: {x: complex ? 350.25 : 350,
        y: complex ? 150.25 : 150, size: 40, fill: '#37b7a1', stroke: '#37b7a1',
        lineWidth: 0, labelText: 'B', labelPlacement: 'center'}},
    {id: 'right', data: {raw: {id: 'right'}}, style: {x: 644, y: 244, size: 40,
        fill: '#edb652', stroke: '#edb652', lineWidth: 12, labelText: 'C', labelPlacement: 'center'}},
], edges: [
    {id: 'one', source: 'left', target: 'middle', data: {raw: {id: 'one'}},
        style: {stroke: '#333333', lineWidth: 2, endArrow: true, labelText: 'edge'}},
    {id: 'two', source: 'middle', target: 'right', data: {raw: {id: 'two'}},
        style: {stroke: '#7256a0', lineWidth: 2, endArrow: true}},
    ...(complex ? [
        {id: 'parallel', source: 'left', target: 'middle', data: {raw: {id: 'parallel'}},
            style: {stroke: '#b23668', lineWidth: 2, endArrow: true, labelText: 'parallel'}},
        {id: 'loop', source: 'middle', target: 'middle', data: {raw: {id: 'loop'}},
            style: {stroke: '#375f9c', lineWidth: 2, endArrow: true, labelText: 'loop'}},
    ] : []),
]});

const controller = createGraphController(document.getElementById('graph')!, {preset: 'preview'});
const complex = new URL(location.href).searchParams.get('case') === 'parallel-loop-fractional';
const settle = async (): Promise<void> => {
    await document.fonts.ready;
    await new Promise<void>(resolve => requestAnimationFrame(() => requestAnimationFrame(() => resolve())));
};
const replace = async (mutation = '') => {
    const data = fixtureData(complex);
    if (mutation === 'missing-node') {
        data.nodes = data.nodes.filter(node => node.id !== 'middle');
        data.edges = [];
    }
    if (mutation === 'missing-edge') data.edges = data.edges.filter(edge => edge.id !== 'one');
    if (mutation === 'changed-color') data.nodes[1].style!.fill = '#d9367e';
    if (mutation === 'edge-style') data.edges.forEach((edge, index) => {
        edge.style!.stroke = ['#c62828', '#007d9c', '#db6400', '#6b3fa0'][index];
        edge.style!.lineWidth = 4;
    });
    const result = await controller.replaceData(data, {layout: false, fit: false});
    if (result.status !== 'applied') throw new Error(`Fixture data was not applied: ${result.status}`);
    await settle();
    return data;
};
// This fixture owns and calls the same PUBLIC controller available to applications.
// No Graph instance, React fiber, engine internals or production test hook is exposed.
Object.assign(window, {pngFixture: {
    ready: replace(), replace,
    exportPng: () => controller.exportPng(),
    hideMiddle: async (hidden: boolean) => { const result = await controller.hide(['middle'], hidden); await settle(); return result; },
    clear: async () => { const result = await controller.replaceData({nodes: [], edges: []}, {layout: false, fit: false}); await settle(); return result; },
    zoom: async () => { const result = await controller.zoomBy(1.5); await settle(); return result; },
    dispose: () => controller.dispose(),
}});
