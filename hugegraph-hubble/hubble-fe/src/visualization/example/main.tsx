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

import {useState} from 'react';
import {createRoot} from 'react-dom/client';
import {GraphCanvas, useGraphSnapshot} from '../react';
import type {GraphControllerHandle} from '../react';
import type {CanvasEvent, GraphLayout} from '../runtime/types';
import type {GraphData} from '../model/types';
import './style.css';

const sample: GraphData = {
    nodes: ['Alice', 'Bob', 'Carol'].map((name, index) => ({
        id: name, data: {raw: {id: name, name, team: index < 2 ? 'Research' : 'Platform'}},
        style: {labelText: name, fill: ['#627eea', '#37b7a1', '#edb652'][index], size: 42},
    })),
    edges: [
        {id: 'knows-1', source: 'Alice', target: 'Bob', data: {raw: {relationship: 'knows'}}, style: {labelText: 'knows'}},
        {id: 'knows-2', source: 'Alice', target: 'Bob', data: {raw: {relationship: 'works with'}}, style: {labelText: 'works with'}},
        {id: 'loop', source: 'Carol', target: 'Carol', data: {raw: {relationship: 'reviews own work'}}, style: {labelText: 'reviews'}},
        {id: 'collaborates', source: 'Bob', target: 'Carol', data: {raw: {relationship: 'collaborates'}}},
    ],
};
const empty: GraphData = {nodes: [], edges: []};
const previewLayout: GraphLayout = {type: 'dagre', options: {rankdir: 'LR'}};
const tooltip = (event: CanvasEvent) => <><strong>{event.kind}: {event.id}</strong>
    <pre>{JSON.stringify(event.data.raw, null, 2)}</pre></>;

function Example() {
    const [controller, setController] = useState<GraphControllerHandle | null>(null);
    const [data, setData] = useState(sample);
    const [datasetKey, setDatasetKey] = useState(0);
    const [layout, setLayout] = useState<GraphLayout>({type: 'd3-force'});
    const [error, setError] = useState('');
    const [selected, setSelected] = useState<CanvasEvent | null>(null);
    const [theme, setTheme] = useState<'light' | 'dark'>('light');
    const snapshot = useGraphSnapshot(controller);
    const run = (action: (graph: GraphControllerHandle) => Promise<unknown>) => {
        if (controller) {
            setError('');
            void action(controller).catch(failure => setError(String(failure)));
        }
    };
    const replace = (next: GraphData) => {setData(next); setDatasetKey(key => key + 1); setSelected(null);};
    const selectedIds = snapshot.selectedIds;
    const selectedElement = [...snapshot.data.nodes, ...snapshot.data.edges].find(item => selectedIds.includes(item.id));
    return <main>
        <header><p className='eyebrow'>PUBLIC GRAPH MODULE</p><h1>A canvas with a clear boundary</h1>
            <p>Independent React instances, typed graph data and an imperative controller. No Hubble services required.</p></header>
        <section className='workspace'>
            <div className='toolbar' aria-label='Graph controls'>
                <button onClick={() => replace(sample)}>Reset dataset</button>
                <button onClick={() => replace(empty)}>Empty graph</button>
                <button onClick={() => run(graph => graph.fitView())}>Fit view</button>
                <button onClick={() => run(graph => graph.zoomBy(1.2))}>Zoom in</button>
                <button onClick={() => run(graph => graph.zoomBy(1 / 1.2))}>Zoom out</button>
                <label htmlFor='example-layout'>Layout</label><select id='example-layout' value={layout.type} onChange={event => setLayout({type: event.target.value as GraphLayout['type']})}>
                    {['d3-force', 'dagre', 'circular', 'concentric', 'grid', 'radial', 'force-atlas2', 'columns'].map(type =>
                        <option key={type}>{type}</option>)}
                </select>
                <button onClick={() => setTheme(value => value === 'light' ? 'dark' : 'light')}>Toggle theme</button>
                <button disabled={!controller} onClick={() => run(graph => graph.mergeData({
                    nodes: [{id: 'Dana', data: {raw: {id: 'Dana', name: 'Dana'}}, style: {labelText: 'Dana', fill: '#cf79c5'}}],
                    edges: graph.getSnapshot().data.nodes.some(node => node.id === 'Alice')
                        ? [{id: 'new-neighbor', source: 'Alice', target: 'Dana', data: {raw: {relationship: 'new neighbor'}}}] : [],
                }))}>Merge neighbor</button>
                <button disabled={!selectedIds.length} onClick={() => run(graph => graph.pin([...selectedIds], true))}>Pin selected</button>
                <button disabled={!selectedIds.length} onClick={() => run(graph => graph.pin([...selectedIds], false))}>Unpin selected</button>
                <button disabled={!selectedIds.length} onClick={() => run(graph => graph.applyStyle(selectedIds.map(id => ({
                    id, kind: graph.getSnapshot().data.nodes.some(node => node.id === id) ? 'node' : 'edge',
                    style: {stroke: '#e05454', lineWidth: 4},
                }))))}>Style selected</button>
                <button disabled={!selectedIds.length} onClick={() => run(graph => graph.hide([...selectedIds], true))}>Hide selected</button>
                <button onClick={() => run(graph => graph.hide([...graph.getSnapshot().data.nodes, ...graph.getSnapshot().data.edges].map(item => item.id), false))}>Show all</button>
                <button disabled={!snapshot.canUndo} onClick={() => run(graph => graph.undo())}>Undo</button>
                <button disabled={!snapshot.canRedo} onClick={() => run(graph => graph.redo())}>Redo</button>
                <button onClick={() => run(async graph => {
                    const result = await graph.exportPng();
                    if (result.status !== 'applied') return;
                    const anchor = document.createElement('a');
                    anchor.href = result.value; anchor.download = 'graph.png'; anchor.click();
                })}>Export PNG</button>
            </div>
            <GraphCanvas data={data} datasetKey={datasetKey} layout={layout} theme={theme} minimap
                onReady={setController} onClick={setSelected} tooltip={tooltip}
                onError={failure => setError(String(failure))} style={{height: 520}} aria-label='Interactive graph' />
            <footer><span role='status'>{snapshot.status} · {snapshot.data.nodes.length} nodes · {snapshot.data.edges.length} edges</span>
                <span>Click to inspect. Drag to move; use the wheel to zoom.</span></footer>
        </section>
        {error && <p role='alert' className='failure'>{error}</p>}
        <div className='companions'>
            <section><h2>Preview</h2><p>Independent Dagre layout with parallel edges and a self loop.</p>
                <GraphCanvas data={sample} preset='preview' layout={previewLayout} tooltip={tooltip}
                    aria-label='Preview graph' style={{height: 260}} /></section>
            <section><h2>Thumbnail</h2><p>Fits its own viewport when its container resizes.</p>
                <GraphCanvas data={sample} preset='thumbnail' layout={previewLayout}
                    aria-label='Thumbnail graph' style={{height: 260}} /></section>
            <section><h2>Raw record</h2><p>Event data remains separate from rendering IDs and styles.</p>
                {selectedElement && <dl>
                    <dt>Rendering ID</dt><dd>{selectedElement.id}</dd>
                    <dt>Fixed</dt><dd>{'fixed' in selectedElement.data && selectedElement.data.fixed ? 'yes' : 'no'}</dd>
                    <dt>Position</dt><dd>x: {String(selectedElement.style?.x ?? '—')}, y: {String(selectedElement.style?.y ?? '—')}</dd>
                </dl>}
                <pre>{selectedElement ? JSON.stringify(selectedElement.data.raw, null, 2)
                    : selected ? 'Selection cleared.' : 'Select an element in the interactive graph.'}</pre></section>
        </div>
    </main>;
}
const container = document.getElementById('root');
if (!container) throw new Error('Missing visualization root');
createRoot(container).render(<Example />);
