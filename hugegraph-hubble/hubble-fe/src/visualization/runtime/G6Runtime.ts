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

import {Graph, GraphEvent} from '@antv/g6';
import type {Command, EdgeData as EngineEdge, GraphData as EngineData, GraphOptions, History, IPointerEvent,
    NodeData as EngineNode} from '@antv/g6';
import type {GraphData, GraphEdge, GraphNode, NodeData, GraphStyle} from '../model/types';
import {immutableCopy, mutableCopy} from '../model/ownership';
import {labelFontScale, labelVisibility} from '../model/labels';
import {PinBridge, registerExtensions} from './extensions';
import type {ControllerOptions, GraphLayout, GraphRuntime, RuntimeEvent} from './types';

const toEngineData = (data: Partial<GraphData>): EngineData => ({
    ...(data.nodes ? {nodes: data.nodes.map(node => ({...node, data: {...immutableCopy(node.data)}, style: mutableCopy(node.style)}))} : {}),
    ...(data.edges ? {edges: data.edges.map(edge => ({...edge, data: {...immutableCopy(edge.data)}, style: mutableCopy(edge.style)}))} : {}),
});
const readNodeData = (data: Record<string, unknown> | undefined): NodeData => {
    if (!data?.raw || typeof data.raw !== 'object' || Array.isArray(data.raw)) {
        throw new Error('Engine element lost its original data record');
    }
    return {...data, raw: data.raw as Record<string, unknown>};
};
const hasChanges = (command: Command): boolean => Object.values(command.current)
    .some((data: EngineData) => [data.nodes, data.edges, data.combos].some(items => Boolean(items?.length)));
// G6's default-style callback wins over DrawData. Leave automatic lane/loop
// geometry to the transform so stale model geometry cannot override its result.
const edgePresentation = (style: EngineEdge['style'] = {}) => {
    const {curveOffset: _curveOffset, loopPlacement: _loopPlacement, loopDist: _loopDist, ...presentation} = style;
    return presentation;
};

/** Owns one real G6 instance. No application caller receives the Graph. */
export class G6Runtime implements GraphRuntime {
    private readonly graph: Graph;
    private initialized = false;
    private firstDataDrawPending = true;
    private readonly historyEnabled: boolean;
    private readonly fitZoomLimit: number;
    private readonly semanticZoom: boolean;
    private labels = {nodeLabels: true, edgeLabels: true};
    private fontScale = 1;
    private alive = true;
    private busy = false;
    private recording = false;
    private replaying = false;
    private handler: (event: RuntimeEvent) => void = () => {};
    private readonly bindings: Array<[string, (event: IPointerEvent) => void]> = [];
    private dragIdle: Promise<void> | null = null;
    private finishDrag?: () => void;

    constructor(container: HTMLElement, options: ControllerOptions = {}) {
        registerExtensions();
        const preset = options.preset ?? 'interactive';
        const interactive = preset === 'interactive';
        this.historyEnabled = interactive;
        this.semanticZoom = interactive;
        this.fitZoomLimit = interactive ? 1.35 : 1.5;
        // G6 5.1.1 forwards this public G Canvas option, omitted from its narrowed type.
        // Pure camera frames still draw in endFrame; skip the redundant per-element hooks.
        const canvas: NonNullable<GraphOptions['canvas']> & {disableRenderHooks: boolean} = {disableRenderHooks: true};
        this.graph = new Graph({
            container, width: Math.max(container.clientWidth, 1), height: Math.max(container.clientHeight, 1),
            canvas, autoResize: false, animation: false,
            padding: preset === 'thumbnail' ? 12 : options.minimap ? [36, 36, 36, 220] : 36,
            theme: options.theme ?? 'light', zoomRange: [0.03, 8], data: {nodes: [], edges: []},
            node: {
                type: datum => typeof datum.data?.shape === 'string' ? datum.data.shape : 'circle',
                style: datum => ({size: 36, fill: '#5C73E6', stroke: '#5C73E6', lineWidth: 1,
                    labelPlacement: 'bottom', labelFill: options.theme === 'dark' ? '#e8e8ec' : '#242938',
                    labelBackground: true, labelBackgroundFill: options.theme === 'dark' ? '#171923' : '#ffffff',
                    labelBackgroundOpacity: 0.85, cursor: preset === 'thumbnail' ? 'pointer' : 'grab',
                    ...datum.style, ...this.labelTypography(datum.style, 12, [2, 4]),
                    label: datum.style?.label !== false && this.labels.nodeLabels}),
                state: {selected: datum => ({halo: false, lineWidth: 3, stroke: '#F59E0B',
                    ...this.labelTypography(datum.style, 12, [2, 4]), label: datum.style?.label !== false}),
                    active: {halo: false, lineWidth: 3, stroke: '#2563EB'},
                    focused: datum => ({label: datum.style?.label !== false})},
            },
            edge: {type: 'quadratic',
                style: datum => ({stroke: '#8D97B0', lineWidth: 1, endArrow: true,
                labelFill: options.theme === 'dark' ? '#e8e8ec' : '#49536A',
                labelBackground: true, labelBackgroundFill: options.theme === 'dark' ? '#171923' : '#ffffff',
                labelBackgroundOpacity: 0.85, loop: true, ...edgePresentation(datum.style),
                ...this.labelTypography(datum.style, 11, [1, 3]),
                label: datum.style?.label !== false && this.labels.edgeLabels}),
            state: {selected: datum => ({halo: false, lineWidth: 3, stroke: '#F59E0B',
                ...this.labelTypography(datum.style, 11, [1, 3]), label: datum.style?.label !== false}),
                active: {halo: false, lineWidth: 2, stroke: '#2563EB'},
                focused: datum => ({label: datum.style?.label !== false})}},
            transforms: [{type: 'hubble-parallel-edges'}],
            behaviors: preset === 'thumbnail' ? [] : [
                'drag-canvas', 'zoom-canvas',
                ...(interactive ? [{type: 'drag-element', shadow: false, hideEdge: 'none', animation: false,
                    enable: (event: IPointerEvent) => {
                        if (!this.alive || this.busy || event.targetType !== 'node') return false;
                        this.dragIdle = new Promise(resolve => { this.finishDrag = resolve; });
                        return true;
                    },
                    onFinish: () => {
                        this.finishDrag?.(); this.finishDrag = undefined; this.dragIdle = null;
                        if (this.alive) this.handler({type: 'dragEnd'});
                    }}] : []),
            ],
            plugins: [
                {type: 'hubble-pin-bridge', key: 'pins'},
                ...(interactive ? [{type: 'history', key: 'history', stackSize: 20,
                    beforeAddCommand: (command: Command) => this.recording && hasChanges(command),
                    afterAddCommand: () => {
                        if (!this.replaying) this.getHistory()?.redoStack.splice(0);
                    }}] : []),
                ...(interactive && options.minimap ? [{type: 'minimap', key: 'minimap', size: [180, 120], position: 'left-bottom'}] : []),
            ],
        });
        if (preset !== 'thumbnail') this.bindEvents();
    }

    async idleInteraction(): Promise<void> { if (this.dragIdle) await this.dragIdle; }
    onEvent(handler: (event: RuntimeEvent) => void): void { this.handler = handler; }

    private labelTypography(style: EngineNode['style'] | EngineEdge['style'], defaultSize: number, defaultPadding: number[]): {
        labelFontSize: number; labelLineHeight: number; labelPadding: number | number[];
    } {
        const nonnegative = (value: unknown): value is number =>
            typeof value === 'number' && Number.isFinite(value) && value >= 0;
        const size = nonnegative(style?.labelFontSize) ? style.labelFontSize : defaultSize;
        const lineHeight = nonnegative(style?.labelLineHeight) ? style.labelLineHeight : size * 4 / 3;
        const padding = style?.labelPadding;
        return {labelFontSize: size / this.fontScale, labelLineHeight: lineHeight / this.fontScale,
            labelPadding: nonnegative(padding) ? padding / this.fontScale
                : (Array.isArray(padding) && padding.every(nonnegative) ? padding : defaultPadding)
                    .map(value => value / this.fontScale)};
    }

    private bindEvents(): void {
        for (const kind of ['node', 'edge'] as const) {
            for (const [name, type] of [['click', 'click'], ['dblclick', 'doubleClick'],
                ['pointerenter', 'hover'], ['pointerleave', 'leave'], ['contextmenu', 'contextMenu']] as const) {
                const handler = (event: IPointerEvent) => {
                    if (!this.alive) return;
                    if (type === 'contextMenu') event.preventDefault();
                    const id = 'id' in event.target ? String(event.target.id) : undefined;
                    this.handler({type, id, kind, client: {x: event.client.x, y: event.client.y}});
                };
                this.graph.on(`${kind}:${name}`, handler);
                this.bindings.push([`${kind}:${name}`, handler]);
            }
        }
        const canvasClick = () => { if (this.alive) this.handler({type: 'canvasClick'}); };
        this.graph.on('canvas:click', canvasClick);
        this.bindings.push(['canvas:click', canvasClick]);
        const viewport = () => { if (this.alive) this.handler({type: 'viewport'}); };
        this.graph.on(GraphEvent.AFTER_TRANSFORM, viewport);
        this.bindings.push([GraphEvent.AFTER_TRANSFORM, viewport]);
    }

    private getHistory(): History | undefined {
        return this.initialized && this.historyEnabled ? this.graph.getPluginInstance<History>('history') : undefined;
    }
    private getPins(): PinBridge | undefined {
        return this.initialized ? this.graph.getPluginInstance<PinBridge>('pins') : undefined;
    }

    private async commit(record: boolean, run: () => Promise<void>): Promise<void> {
        if (this.dragIdle) await this.dragIdle;
        if (!this.alive) return;
        const history = this.getHistory();
        // G6 5.1.1 trims a full stack BEFORE consulting beforeAddCommand.
        // Preserve the plugin's public stacks around excluded system draws.
        const saved = !record && history ? {undo: [...history.undoStack], redo: [...history.redoStack]} : null;
        this.busy = true;
        this.recording = record;
        try { await run(); }
        finally {
            if (saved && history) { history.undoStack = saved.undo; history.redoStack = saved.redo; }
            this.recording = true;
            this.busy = false;
        }
    }

    async replace(data: GraphData, layout: GraphLayout | null): Promise<void> {
        await this.commit(false, async () => {
            // Compute before the first draw, so large datasets never create a full set of labels first.
            const zoom = this.initialized ? this.graph.getZoom() : 1;
            this.labels = this.semanticZoom
                ? labelVisibility(data.nodes.length + data.edges.length, zoom)
                : {nodeLabels: true, edgeLabels: true};
            this.fontScale = this.semanticZoom ? labelFontScale(zoom) : 1;
            this.graph.setData(toEngineData(data));
            // Native pre-layout computes positions before constructing shapes.
            // An empty initial draw must not consume the first real dataset.
            const preLayout = this.firstDataDrawPending && data.nodes.length > 0 && layout && layout.type !== 'columns';
            if (data.nodes.length) this.firstDataDrawPending = false;
            if (preLayout) await this.runLayout(layout, true);
            else {
                await this.graph.draw();
                this.initialized = true;
                if (this.alive && layout) await this.runLayout(layout);
            }
        });
    }

    async patch(data: Partial<GraphData>, record: boolean): Promise<void> {
        await this.commit(record, async () => {
            const incoming = toEngineData(data);
            const add: EngineData = {nodes: [], edges: []};
            const update: EngineData = {nodes: [], edges: []};
            for (const node of incoming.nodes ?? []) {
                (this.graph.hasNode(node.id) ? update.nodes! : add.nodes!).push(node);
            }
            for (const edge of incoming.edges ?? []) {
                (this.graph.hasEdge(edge.id!) ? update.edges! : add.edges!).push(edge);
            }
            this.graph.addData(add);
            this.graph.updateData(update);
            // A manual add can be the first nonempty display after an empty
            // query. Its existing shapes must use post-layout on later replace.
            if (add.nodes!.length) this.firstDataDrawPending = false;
            await this.graph.draw();
        });
    }

    layout(layout: GraphLayout): Promise<void> { return this.commit(false, async () => this.runLayout(layout)); }
    private async runLayout(layout: GraphLayout, preLayout = false): Promise<void> {
        const fixed = this.graph.getNodeData().filter(node => node.data?.fixed)
            .map(node => ({id: node.id, style: {x: node.style?.x, y: node.style?.y}}));
        if (layout.type === 'columns') {
            const data = this.graph.getNodeData();
            const groups = new Map<string, EngineNode[]>();
            for (const node of data) {
                const group = typeof node.data?.group === 'string' ? node.data.group : 'middle';
                const nodes = groups.get(group) ?? []; nodes.push(node); groups.set(group, nodes);
            }
            const names = ['start', ...[...groups.keys()].filter(name => name !== 'start' && name !== 'end'), 'end'];
            const positions = names.flatMap((name, column) => (groups.get(name) ?? []).map((node, row) => ({id: node.id,
                style: {x: column * 240, y: row * 90 - ((groups.get(name)?.length ?? 1) - 1) * 45}})));
            this.graph.updateNodeData(positions);
            await this.graph.draw();
        }
        else {
            const small = this.graph.getNodeData().length <= 20;
            const defaults = layout.type === 'd3-force'
                ? {link: {distance: small ? 180 : 100}, manyBody: {strength: small ? -800 : -250},
                    collide: {radius: small ? 40 : 28}, iterations: 300,
                    // Center only translates the centroid. Axis attraction also
                    // keeps disconnected nodes from expanding the fit bounds.
                    x: {strength: 0.2}, y: {strength: 0.2}}
                : layout.type === 'dagre' ? {rankdir: 'LR', ranksep: 100, nodesep: 60}
                : layout.type === 'circular' ? {nodeSpacing: 0,
                    radius: Math.max(120, this.graph.getNodeData().length * 100 / (2 * Math.PI))}
                : layout.type === 'grid' ? {preventOverlap: true, nodeSize: 48, nodeSpacing: 36}
                : {preventOverlap: true, nodeSize: 48, nodeSpacing: 24};
            const type = layout.type === 'd3-force' ? 'hubble-pinned-d3-force' : layout.type;
            // Post-layout includes hidden rendered nodes; keep the same layout
            // input when simulating before the initial shapes exist.
            const options = {...defaults, ...layout.options, type, animation: false, enableWorker: false, preLayout,
                ...(preLayout ? {isLayoutInvisibleNodes: true} : {})};
            this.graph.setLayout(options);
            if (preLayout) {
                await this.graph.render();
                this.initialized = true;
            }
            else await this.graph.layout(options);
        }
        if (!this.alive) return;
        const restore = fixed.filter(node => {
            const current = this.graph.getNodeData(node.id).style;
            return current?.x !== node.style.x || current?.y !== node.style.y;
        });
        if (restore.length) { this.graph.updateNodeData(restore); await this.graph.draw(); }
        if (this.alive) this.syncPins();
    }

    remove(ids: {nodes: string[]; edges: string[]}): Promise<void> {
        return this.commit(false, async () => { this.graph.removeData(ids); await this.graph.draw(); });
    }
    setMinimap(visible: boolean): void {
        this.graph.setOptions({padding: visible ? [36, 36, 36, 220] : 36});
        this.graph.setPlugins(previous => [
            ...previous.filter(plugin => typeof plugin !== 'object' || plugin.key !== 'minimap'),
            ...(visible ? [{type: 'minimap', key: 'minimap', size: [180, 120], position: 'left-bottom'}] : []),
        ]);
    }

    read(): GraphData {
        const data = this.graph.getData();
        return {nodes: data.nodes.map(node => ({id: node.id, data: readNodeData(node.data),
            ...(node.style ? {style: node.style as GraphStyle} : {})})),
        edges: data.edges.map(edge => ({id: edge.id!, source: edge.source, target: edge.target,
            data: readNodeData(edge.data), ...(edge.style ? {style: edge.style as GraphStyle} : {})}))};
    }
    readElement(kind: 'node', id: string): GraphNode;
    readElement(kind: 'edge', id: string): GraphEdge;
    readElement(kind: 'node' | 'edge', id: string): GraphNode | GraphEdge {
        if (kind === 'node') {
            const node = this.graph.getNodeData(id);
            return {id: node.id, data: readNodeData(node.data), ...(node.style ? {style: node.style as GraphStyle} : {})};
        }
        const edge = this.graph.getEdgeData(id);
        return {id: edge.id!, source: edge.source, target: edge.target, data: readNodeData(edge.data),
            ...(edge.style ? {style: edge.style as GraphStyle} : {})};
    }
    states(states: Record<string, string[]>): Promise<void> {
        return this.commit(false, async () => { await this.graph.setElementState(states, false); });
    }
    async refreshLabelVisibility(itemCount: number): Promise<void> {
        if (!this.alive || !this.initialized || !this.semanticZoom) return;
        const zoom = this.graph.getZoom();
        const next = labelVisibility(itemCount, zoom);
        const fontScale = labelFontScale(zoom);
        if (next.nodeLabels === this.labels.nodeLabels && next.edgeLabels === this.labels.edgeLabels
            && fontScale === this.fontScale) return;
        await this.commit(false, async () => {
            const previous = this.labels;
            const previousScale = this.fontScale;
            this.labels = next;
            this.fontScale = fontScale;
            try {
                const options = this.graph.getOptions();
                // Camera-derived styles change only at density/font buckets, never the saved data or layout.
                if ((previous.nodeLabels !== next.nodeLabels || previousScale !== fontScale) && options.node) {
                    this.graph.setNode(options.node);
                }
                if ((previous.edgeLabels !== next.edgeLabels || previousScale !== fontScale) && options.edge) {
                    this.graph.setEdge(options.edge);
                }
                await this.graph.draw();
            }
            catch (error) { this.labels = previous; this.fontScale = previousScale; throw error; }
        });
    }
    visibility(ids: string[], visible: boolean): Promise<void> {
        return this.commit(false, async () => {
            if (ids.length) await this.graph.setElementVisibility(
                Object.fromEntries(ids.map(id => [id, visible ? 'visible' as const : 'hidden' as const])), false);
        });
    }
    async fitView(): Promise<void> {
        if (!this.initialized || !this.graph.getNodeData().length) return;
        await this.graph.fitView({}, false);
        // A tiny graph should be centered without enlarging labels to fill the entire viewport.
        if (this.alive && this.graph.getZoom() > this.fitZoomLimit) await this.graph.zoomTo(this.fitZoomLimit, false);
    }
    async zoomBy(factor: number): Promise<void> { if (this.initialized) await this.graph.zoomBy(factor, false); }
    resize(width: number, height: number): void { this.graph.resize(width, height); }
    async exportPng(): Promise<string> { return this.graph.toDataURL({type: 'image/png'}); }
    async history(action: 'undo' | 'redo' | 'clear'): Promise<void> {
        const history = this.getHistory();
        if (!history) return;
        if (action === 'clear') { history.clear(); return; }
        this.replaying = true;
        this.recording = true;
        try {
            history[action]();
            await this.commit(false, async () => { await this.graph.draw(); });
        }
        finally { this.replaying = false; }
    }
    historyState(): {canUndo: boolean; canRedo: boolean} {
        const history = this.getHistory();
        return {canUndo: history?.canUndo() ?? false, canRedo: history?.canRedo() ?? false};
    }
    syncPins(ids?: string[]): void { this.getPins()?.sync(ids); }
    releasePins(ids: string[]): void { this.getPins()?.release(ids); }
    stop(): void {
        this.alive = false;
        this.handler = () => {};
        this.finishDrag?.(); this.finishDrag = undefined; this.dragIdle = null;
        if (this.initialized) this.graph.stopLayout();
    }
    destroy(): void {
        this.bindings.forEach(([name, handler]) => this.graph.off(name, handler));
        this.bindings.length = 0;
        this.graph.destroy();
    }
}
