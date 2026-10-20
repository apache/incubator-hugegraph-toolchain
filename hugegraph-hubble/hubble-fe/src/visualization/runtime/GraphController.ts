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

import {buildAdjacencyIndex, relatedElements} from '../model/adjacency';
import {assertGraphStyle, mergeGraphData, normalizeGraphData, preserveNodePositions, sameTopology} from '../model/data';
import type {MergeOptions} from '../model/data';
import type {ElementKind, GraphData, GraphEdge, GraphNode, GraphStyle} from '../model/types';
import {immutableCopy} from '../model/ownership';
import {UpdateQueue} from './UpdateQueue';
import type {UpdateContext, UpdateResult} from './UpdateQueue';
import type {ControllerOptions, ControllerSnapshot, InternalControllerSnapshot, GraphLayout, GraphRuntime, RuntimeEvent, StylePatch} from './types';

interface ReplaceOptions {
    newDataset?: boolean;
    layout?: boolean;
    fit?: boolean;
}
interface StyleBatch {
    patches: Map<string, StylePatch>;
    result: Promise<UpdateResult<void>>;
}
const manualVisibility = Symbol('manual visibility');
const emptyGraph = (): GraphData => ({nodes: [], edges: []});

/** The only application write boundary. Engine state is read back after committed commands. */
export class GraphController {
    private readonly queue = new UpdateQueue();
    private alive = true;
    private initialized = false;
    private disposal?: Promise<void>;
    private layout: GraphLayout;
    private layoutRevision = 0;
    private appliedLayoutRevision = -1;
    private snapshot: InternalControllerSnapshot = immutableCopy({status: 'idle', data: emptyGraph(),
        canUndo: false, canRedo: false, selectedIds: [], hoveredIds: []});
    private readonly listeners = new Set<() => void>();
    private elements = new Map<string, {kind: ElementKind; datum: GraphNode | GraphEdge}>();
    private nodeIndices = new Map<string, number>();
    private edgeIndices = new Map<string, number>();
    private adjacency = buildAdjacencyIndex(this.snapshot.data);
    private selected = new Set<string>();
    private hovered = new Set<string>();
    private focused: string | null = null;
    private drawnStates = new Set<string>();
    private hidden = new Set<string>();
    private readonly hiddenReasons = new Map<string | symbol, Set<string>>();
    private styleTail: StyleBatch | null = null;
    private hoverTail: {current: boolean} | null = null;

    constructor(private readonly runtime: GraphRuntime, private readonly options: ControllerOptions = {}) {
        this.layout = immutableCopy(options.layout ?? {type: (options.preset ?? 'interactive') === 'interactive' ? 'd3-force' : 'dagre'});
        runtime.onEvent(event => this.handleEvent(event));
    }

    getSnapshot = (): ControllerSnapshot => this.snapshot;
    subscribe = (listener: () => void): (() => void) => {
        this.listeners.add(listener);
        return () => { this.listeners.delete(listener); };
    };
    idle = (): Promise<void> => this.queue.idle();

    private publish(update: Partial<InternalControllerSnapshot>): void {
        if (!this.alive) return;
        this.snapshot = Object.freeze({...this.snapshot, ...update,
            ...(update.data ? {data: immutableCopy(update.data)} : {}),
            ...(update.selectedIds ? {selectedIds: immutableCopy(update.selectedIds)} : {}),
            ...(update.hoveredIds ? {hoveredIds: immutableCopy(update.hoveredIds)} : {}),
        });
        this.listeners.forEach(listener => listener());
    }

    private adopt(input: GraphData = this.runtime.read()): void {
        const data = immutableCopy(input);
        const topologyChanged = !sameTopology(this.snapshot.data, data);
        this.elements = new Map<string, {kind: ElementKind; datum: GraphNode | GraphEdge}>([
            ...data.nodes.map(datum => [datum.id, {kind: 'node' as const, datum}] as const),
            ...data.edges.map(datum => [datum.id, {kind: 'edge' as const, datum}] as const),
        ]);
        this.nodeIndices = new Map(data.nodes.map((node, index) => [node.id, index]));
        this.edgeIndices = new Map(data.edges.map((edge, index) => [edge.id, index]));
        if (topologyChanged) this.adjacency = buildAdjacencyIndex(data);
        this.selected = new Set([...this.selected].filter(id => this.elements.has(id)));
        this.hovered = new Set([...this.hovered].filter(id => this.elements.has(id)));
        if (this.focused && !this.elements.has(this.focused)) this.focused = null;
        this.drawnStates = new Set([...this.drawnStates].filter(id => this.elements.has(id)));
        for (const [reason, ids] of this.hiddenReasons) {
            const remaining = new Set([...ids].filter(id => this.elements.has(id)));
            if (remaining.size) this.hiddenReasons.set(reason, remaining);
            else this.hiddenReasons.delete(reason);
        }
        this.refreshHidden();
        this.publish({data, ...this.runtime.historyState(), selectedIds: [...this.selected], hoveredIds: [...this.hovered]});
    }

    private adoptElements(changes: GraphData): void {
        const nodes = changes.nodes.length ? [...this.snapshot.data.nodes] : this.snapshot.data.nodes;
        const edges = changes.edges.length ? [...this.snapshot.data.edges] : this.snapshot.data.edges;
        for (const {id} of changes.nodes) {
            const datum = immutableCopy(this.runtime.readElement('node', id));
            nodes[this.nodeIndices.get(id)!] = datum;
            this.elements.set(id, {kind: 'node', datum});
        }
        for (const {id} of changes.edges) {
            const datum = immutableCopy(this.runtime.readElement('edge', id));
            edges[this.edgeIndices.get(id)!] = datum;
            this.elements.set(id, {kind: 'edge', datum});
        }
        this.publish({data: {nodes, edges}, ...this.runtime.historyState()});
    }

    private refreshHidden(): void {
        this.hidden = new Set([...this.hiddenReasons.values()].flatMap(ids => [...ids]));
    }

    private withVisibility(data: GraphData): GraphData {
        // Visibility is derived view state. Saved styles and History cannot override hide().
        const apply = <T extends GraphNode | GraphEdge>(datum: T, hidden: boolean): T => {
            const visibility = hidden ? 'hidden' : 'visible';
            if (datum.style?.visibility === visibility || (!hidden && datum.style?.visibility !== 'hidden')) return datum;
            return {...datum, style: {...datum.style, visibility}};
        };
        return {
            nodes: data.nodes.map(node => apply(node, this.hidden.has(node.id))),
            edges: data.edges.map(edge => apply(edge,
                this.hidden.has(edge.id) || this.hidden.has(edge.source) || this.hidden.has(edge.target))),
        };
    }

    private command<T>(run: (context: UpdateContext) => Promise<T>,
        version: number | null = this.queue.version, key?: string, observable = true): Promise<UpdateResult<T>> {
        this.styleTail = null;
        this.hoverTail = null;
        return this.queue.submit(async context => {
            await this.runtime.idleInteraction();
            if (!context.isCurrent()) throw new Error('Graph update was superseded');
            if (observable) this.publish({status: 'updating', error: undefined});
            try {
                const value = await run(context);
                if (context.isCurrent()) {
                    await this.runtime.refreshLabelVisibility(this.snapshot.data.nodes.length + this.snapshot.data.edges.length);
                }
                if (observable && context.isCurrent()) this.publish({status: 'ready', ...this.runtime.historyState()});
                return value;
            }
            catch (error) {
                if (context.isCurrent()) this.publish({status: 'error', error});
                throw error;
            }
        }, {version, key});
    }

    async replaceData(input: GraphData, options: ReplaceOptions = {}): Promise<UpdateResult<void>> {
        if (!this.alive) return {status: 'disposed'};
        const data = immutableCopy(normalizeGraphData(input));
        const version = this.queue.nextDataVersion();
        return this.command(async context => {
            this.notify(() => this.options.onHover?.(null));
            const previous = this.initialized ? this.runtime.read() : emptyGraph();
            const same = this.initialized && options.newDataset === false && sameTopology(previous, data);
            if (options.newDataset !== false) {
                this.hiddenReasons.clear();
                this.hidden.clear();
            }
            const next = this.withVisibility(same ? preserveNodePositions(previous, data) : data);
            const revision = this.layoutRevision;
            const runLayout = options.layout !== false && (!same || this.appliedLayoutRevision !== revision);
            await this.runtime.replace(next, runLayout ? this.layout : null);
            if (context.isCurrent() && runLayout) this.appliedLayoutRevision = revision;
            this.initialized = true;
            if (!context.isCurrent()) return;
            const existingIds = new Set([...next.nodes, ...next.edges].map(item => item.id));
            const states = Object.fromEntries([...this.drawnStates].filter(id => existingIds.has(id)).map(id => [id, []]));
            this.selected.clear();
            this.hovered.clear();
            this.focused = null;
            this.drawnStates.clear();
            if (Object.keys(states).length) await this.runtime.states(states);
            if (!context.isCurrent()) return;
            if (options.newDataset !== false) await this.runtime.history('clear');
            if (!context.isCurrent()) return;
            if (options.fit ?? !same) await this.runtime.fitView();
            if (context.isCurrent()) this.adopt();
        }, version);
    }

    async mergeData(input: GraphData, options: MergeOptions = {}): Promise<UpdateResult<boolean>> {
        const incoming = immutableCopy(input);
        const policy = immutableCopy(options);
        return this.command(async context => {
            const result = mergeGraphData(this.runtime.read(), incoming, policy);
            if (!result.changed) return false;
            // Expansion is recorded; the following system layout is not another user command.
            const nodeIds = new Set([...result.addedNodeIds, ...result.updatedNodeIds]);
            const edgeIds = new Set([...result.addedEdgeIds, ...result.updatedEdgeIds]);
            await this.runtime.patch(this.withVisibility({nodes: result.data.nodes.filter(node => nodeIds.has(node.id)),
                edges: result.data.edges.filter(edge => edgeIds.has(edge.id))}), true);
            if (!context.isCurrent()) return true;
            if (result.addedNodeIds.length || result.addedEdgeIds.length) await this.runtime.layout(this.layout);
            if (context.isCurrent()) this.adopt();
            return true;
        });
    }

    /** Merge only adjacent pending style commands; a layout/data/camera command is a barrier. */
    async applyStyle(patches: StylePatch[]): Promise<UpdateResult<void>> {
        if (!this.alive) return Promise.resolve({status: 'disposed'});
        if (this.styleTail) {
            this.addPatches(this.styleTail.patches, patches);
            return this.styleTail.result;
        }
        const batch: StyleBatch = {patches: new Map(), result: Promise.resolve({status: 'superseded'})};
        this.addPatches(batch.patches, patches);
        batch.result = this.command(async context => {
            if (this.styleTail === batch) this.styleTail = null;
            const data: GraphData = {nodes: [], edges: []};
            for (const patch of batch.patches.values()) {
                if (this.elements.get(patch.id)?.kind !== patch.kind) continue;
                if (patch.kind === 'node') {
                    const node = this.runtime.readElement('node', patch.id);
                    const style = {...node.style, ...patch.style};
                    assertGraphStyle(style);
                    data.nodes.push({...node, style});
                }
                else {
                    const edge = this.runtime.readElement('edge', patch.id);
                    const style = {...edge.style, ...patch.style};
                    assertGraphStyle(style);
                    data.edges.push({...edge, style});
                }
            }
            if (!data.nodes.length && !data.edges.length) return;
            await this.runtime.patch(this.withVisibility(data), true);
            if (context.isCurrent()) {
                const positions = [...batch.patches.values()].filter(patch => 'x' in patch.style || 'y' in patch.style)
                    .map(patch => patch.id);
                if (positions.length) this.runtime.syncPins(positions);
                this.adoptElements(data);
            }
        });
        this.styleTail = batch;
        return batch.result;
    }

    private addPatches(target: Map<string, StylePatch>, patches: StylePatch[]): void {
        for (const patch of patches) {
            const key = `${patch.kind}:${patch.id}`;
            const previous: GraphStyle = target.get(key)?.style ?? {};
            target.set(key, immutableCopy({...patch, style: {...previous, ...patch.style}}));
        }
    }

    setLayout(layout: GraphLayout): Promise<UpdateResult<void>> {
        this.layout = immutableCopy(layout);
        const target = this.layout;
        const revision = ++this.layoutRevision;
        return this.command(async context => {
            await this.runtime.layout(target);
            if (!context.isCurrent() || revision !== this.layoutRevision) return;
            await this.runtime.fitView();
            if (context.isCurrent() && revision === this.layoutRevision) {
                this.appliedLayoutRevision = revision; this.adopt();
            }
        }, this.queue.version, 'layout');
    }

    pin(ids: string[], fixed: boolean): Promise<UpdateResult<void>> {
        return this.command(async context => {
            const nodes = ids.filter(id => this.elements.get(id)?.kind === 'node')
                .map(id => this.runtime.readElement('node', id))
                .filter(node => Boolean(node.data.fixed) !== fixed)
                .map(node => ({...node, data: {...node.data, fixed}}));
            if (!nodes.length) return;
            await this.runtime.patch(this.withVisibility({nodes, edges: []}), true);
            if (!context.isCurrent()) return;
            const changed = nodes.map(node => node.id);
            if (fixed) this.runtime.syncPins(changed);
            else this.runtime.releasePins(changed);
            if (context.isCurrent()) this.adoptElements({nodes, edges: []});
        });
    }

    /** Manual hide state is independent of named masks such as filters. */
    getHiddenIds(reason?: string): string[] {
        return [...(this.hiddenReasons.get(reason ?? manualVisibility) ?? [])];
    }

    hide(ids: string[], hidden = true): Promise<UpdateResult<void>> {
        const requested = [...ids];
        return this.command(async context => {
            const manual = new Set(this.hiddenReasons.get(manualVisibility));
            const affected = requested.filter(id => this.elements.has(id));
            for (const id of affected) {
                if (hidden) manual.add(id); else manual.delete(id);
            }
            if (manual.size) this.hiddenReasons.set(manualVisibility, manual);
            else this.hiddenReasons.delete(manualVisibility);
            this.refreshHidden();
            await this.updateVisibility(affected, context);
        });
    }

    /** Atomically replace one view mask without changing any other source of hidden elements. */
    setHiddenIds(reason: string, ids: string[]): Promise<UpdateResult<void>> {
        const requested = [...ids];
        return this.command(async context => {
            const previous = this.hiddenReasons.get(reason) ?? new Set<string>();
            const next = new Set(requested.filter(id => this.elements.has(id)));
            if (next.size) this.hiddenReasons.set(reason, next);
            else this.hiddenReasons.delete(reason);
            this.refreshHidden();
            await this.updateVisibility(new Set([...previous, ...next]), context);
        });
    }

    private async updateVisibility(ids: Iterable<string>, context: UpdateContext): Promise<void> {
        const affected = new Set<string>();
        for (const id of ids) {
            if (!this.elements.has(id)) continue;
            affected.add(id);
            this.adjacency.incidentEdges.get(id)?.forEach(edge => affected.add(edge));
        }
        const visible: string[] = [];
        const invisible: string[] = [];
        for (const id of affected) {
            const endpoints = this.adjacency.endpoints.get(id);
            const isHidden = this.hidden.has(id) || Boolean(endpoints?.some(node => this.hidden.has(node)));
            (isHidden ? invisible : visible).push(id);
        }
        if (invisible.length) await this.runtime.visibility(invisible, false);
        if (!context.isCurrent()) return;
        if (visible.length) await this.runtime.visibility(visible, true);
        if (context.isCurrent()) this.adopt();
    }

    fitView = (): Promise<UpdateResult<void>> => this.command(async () => { await this.runtime.fitView(); }, null);
    zoomBy = (factor: number): Promise<UpdateResult<void>> => this.command(async () => {
        if (!Number.isFinite(factor) || factor <= 0) throw new Error('Zoom factor must be positive');
        await this.runtime.zoomBy(factor);
    }, null);
    resize = (width: number, height: number): Promise<UpdateResult<void>> => this.command(async () => {
        if (width > 0 && height > 0 && Number.isFinite(width) && Number.isFinite(height)) {
            this.runtime.resize(width, height);
        }
    }, null);
    exportPng = (): Promise<UpdateResult<string>> => this.command(async () => this.runtime.exportPng(), null);

    private replay(action: 'undo' | 'redo'): Promise<UpdateResult<void>> {
        return this.command(async context => {
            await this.runtime.history(action);
            if (!context.isCurrent()) return;
            // Official History replays complete styles, including visibility from an older view.
            const data = this.runtime.read();
            const visibleData = this.withVisibility(data);
            const nodes = visibleData.nodes.filter((node, index) => node !== data.nodes[index]);
            const edges = visibleData.edges.filter((edge, index) => edge !== data.edges[index]);
            const changed = nodes.length > 0 || edges.length > 0;
            if (changed) await this.runtime.patch({nodes, edges}, false);
            if (context.isCurrent()) {
                this.runtime.syncPins();
                this.adopt(changed ? this.runtime.read() : data);
            }
        });
    }
    undo = (): Promise<UpdateResult<void>> => this.replay('undo');
    redo = (): Promise<UpdateResult<void>> => this.replay('redo');

    private setInteraction(selected: Set<string>, hovered: Set<string>, focused = this.focused,
        hoverIntent?: {current: boolean}): Promise<UpdateResult<void>> {
        this.selected = selected;
        this.hovered = hovered;
        this.focused = focused;
        return this.command(async context => {
            // Coalesce only consecutive pointer intents that have not entered the engine.
            // Once states() starts, its result must still be adopted even if the pointer moves.
            if (hoverIntent && !hoverIntent.current) return;
            await this.drawInteraction(selected, hovered, focused, context);
        }, this.queue.version, 'interaction', false);
    }

    private async drawInteraction(selected: Set<string>, hovered: Set<string>, focused: string | null,
        context: UpdateContext): Promise<void> {
        const affected = new Set([...this.drawnStates, ...selected, ...hovered]);
        const states: Record<string, string[]> = {};
        for (const id of affected) {
            if (this.elements.has(id)) states[id] = [
                ...(selected.has(id) ? ['selected'] : []), ...(hovered.has(id) ? ['active'] : []),
                ...(focused === id ? ['focused'] : []),
            ];
        }
        await this.runtime.states(states);
        if (!context.isCurrent()) return;
        this.drawnStates = new Set([...selected, ...hovered]);
        this.publish({selectedIds: [...selected], hoveredIds: [...hovered]});
    }

    private clearInteractionAfterUpdate(clearSelection: boolean): Promise<UpdateResult<void>> {
        if (clearSelection) this.selected = new Set();
        this.hovered = new Set();
        this.focused = null;
        // Read selection at execution: a pending replacement may clear it.
        // Capturing the old selection here would restore it into a new dataset.
        return this.command(context => this.drawInteraction(this.selected, new Set(), null, context),
            this.queue.version, 'interaction', false);
    }

    select(ids: string[]): Promise<UpdateResult<void>> {
        return this.setInteraction(new Set(ids.filter(id => this.elements.has(id))), this.hovered);
    }
    highlight(ids: string[]): Promise<UpdateResult<void>> {
        // Legend/category emphasis must not expand every matching label.
        return this.setInteraction(this.selected, new Set(ids.filter(id => this.elements.has(id))), null);
    }
    setMinimap(visible: boolean): Promise<UpdateResult<void>> {
        return this.command(async () => { this.runtime.setMinimap(visible); }, null, undefined, false);
    }

    /** Remote mutations clear local undo so undo cannot restore stale backend records. */
    async updateElements(input: Partial<GraphData>, options: {record?: boolean; resetHistory?: boolean} = {}):
        Promise<UpdateResult<void>> {
        const incoming = immutableCopy({nodes: input.nodes ?? [], edges: input.edges ?? []});
        return this.command(async context => {
            const result = mergeGraphData(this.runtime.read(), incoming);
            if (!result.changed) return;
            const nodes = new Set([...result.addedNodeIds, ...result.updatedNodeIds]);
            const edges = new Set([...result.addedEdgeIds, ...result.updatedEdgeIds]);
            const changes = this.withVisibility({nodes: result.data.nodes.filter(node => nodes.has(node.id)),
                edges: result.data.edges.filter(edge => edges.has(edge.id))});
            await this.runtime.patch(changes, options.record ?? false);
            if (!context.isCurrent()) return;
            if (options.resetHistory !== false) await this.runtime.history('clear');
            if (context.isCurrent()) this.adopt();
        });
    }

    removeElements(ids: string[]): Promise<UpdateResult<void>> {
        return this.command(async context => {
            const nodes: string[] = [];
            const edges = new Set<string>();
            for (const id of ids) {
                if (this.elements.get(id)?.kind === 'node') {
                    nodes.push(id);
                    this.adjacency.incidentEdges.get(id)?.forEach(edge => edges.add(edge));
                }
                else if (this.elements.get(id)?.kind === 'edge') edges.add(id);
            }
            if (!nodes.length && !edges.size) return;
            await this.runtime.remove({nodes, edges: [...edges]});
            if (!context.isCurrent()) return;
            await this.runtime.history('clear');
            if (context.isCurrent()) {
                ids.forEach(id => { this.selected.delete(id); this.hovered.delete(id); });
                this.adopt();
            }
        });
    }

    private reportFailure(error: unknown, version = this.queue.version): void {
        if (!this.alive || this.queue.version !== version) return;
        this.publish({status: 'error', error});
        try {
            void Promise.resolve(this.options.onError?.(error))
                .catch(failure => console.error('Graph error callback failed', failure));
        }
        catch (failure) { console.error('Graph error callback failed', failure); }
    }
    private notify(callback: () => void): void {
        const version = this.queue.version;
        try { void Promise.resolve(callback()).catch(error => this.reportFailure(error, version)); }
        catch (error) { this.reportFailure(error, version); }
    }

    private handleEvent(event: RuntimeEvent): void {
        if (!this.alive) return;
        if (event.type === 'viewport') {
            // Camera events join the same queue; thresholds are checked against the latest camera at execution.
            void this.command(async () => {}, null, 'label-density', false)
                .catch(error => this.reportFailure(error));
            return;
        }
        if (this.snapshot.status === 'updating') {
            if (event.type === 'leave' || event.type === 'canvasClick') {
                this.notify(() => this.options.onHover?.(null));
                void this.clearInteractionAfterUpdate(event.type === 'canvasClick')
                    .catch(error => this.reportFailure(error));
            }
            return;
        }
        if (event.type === 'dragEnd') {
            void this.command(async context => { if (context.isCurrent()) this.adopt(); })
                .catch(error => this.reportFailure(error));
            return;
        }
        const item = event.id ? this.elements.get(event.id) : undefined;
        const value = item ? immutableCopy({id: event.id!, kind: item.kind, data: item.datum.data,
            ...(event.client ? {client: event.client} : {})}) : null;
        if (event.type === 'doubleClick' && value) this.notify(() => this.options.onDoubleClick?.(value));
        if (event.type === 'contextMenu' && value) this.notify(() => this.options.onContextMenu?.(value));
        if (event.type === 'hover') this.notify(() => this.options.onHover?.(value));
        if (event.type === 'leave' || event.type === 'canvasClick') this.notify(() => this.options.onHover?.(null));
        if (event.type === 'click' && value) this.notify(() => this.options.onClick?.(value));
        if (event.type === 'doubleClick' || event.type === 'contextMenu') return;
        let selected = event.type === 'canvasClick' ? new Set<string>() : this.selected;
        if (event.type === 'click' && value) {
            selected = event.additiveSelection ? new Set(this.selected) : new Set<string>();
            if (selected.has(value.id)) selected.delete(value.id);
            else selected.add(value.id);
        }
        const related = event.type === 'hover' && value ? relatedElements(this.adjacency, value.kind, value.id) : null;
        const hovered = related ? new Set([...related.nodeIds, ...related.edgeIds])
            : event.type === 'leave' || event.type === 'canvasClick' ? new Set<string>() : this.hovered;
        const focused = event.type === 'hover' ? value?.id ?? null
            : event.type === 'leave' || event.type === 'canvasClick' ? null : this.focused;
        const transient = event.type === 'hover' || event.type === 'leave';
        if (transient && this.hoverTail) this.hoverTail.current = false;
        const hoverIntent = transient ? {current: true} : undefined;
        const update = this.setInteraction(selected, hovered, focused, hoverIntent);
        if (hoverIntent) this.hoverTail = hoverIntent;
        void update.catch(error => this.reportFailure(error));
    }

    dispose(): Promise<void> {
        if (this.disposal) return this.disposal;
        this.alive = false;
        this.snapshot = Object.freeze({...this.snapshot, status: 'disposed'});
        this.styleTail = null;
        this.listeners.clear();
        const idle = this.queue.dispose();
        this.runtime.stop();
        this.disposal = idle.then(() => { this.runtime.destroy(); });
        return this.disposal;
    }
}
