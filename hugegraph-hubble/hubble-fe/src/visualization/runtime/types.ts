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

import type {ElementKind, GraphData, GraphEdge, GraphEvent, GraphNode, GraphStyle, JsonValue} from '../model/types';

export type GraphPreset = 'interactive' | 'preview' | 'thumbnail';
export interface GraphLayout {
    type: 'd3-force' | 'circular' | 'concentric' | 'dagre' | 'grid' | 'radial' | 'force-atlas2' | 'columns';
    options?: Record<string, JsonValue>;
}
export interface StylePatch {
    id: string;
    kind: ElementKind;
    style: GraphStyle;
}
export interface RuntimeEvent {
    type: 'click' | 'doubleClick' | 'hover' | 'leave' | 'canvasClick' | 'dragEnd' | 'contextMenu' | 'viewport';
    id?: string;
    kind?: ElementKind;
    client?: {x: number; y: number};
    /** Shift, Control or Meta click toggles an element in the selection. */
    additiveSelection?: boolean;
}
export type DeepReadonly<T> = T extends object ? {readonly [K in keyof T]: DeepReadonly<T[K]>} : T;
export type CanvasEvent = DeepReadonly<GraphEvent & {client?: {x: number; y: number}}>;

export interface ControllerOptions {
    preset?: GraphPreset;
    layout?: GraphLayout;
    theme?: 'light' | 'dark';
    minimap?: boolean;
    onClick?: (event: CanvasEvent) => void;
    onDoubleClick?: (event: CanvasEvent) => void;
    onContextMenu?: (event: CanvasEvent) => void;
    onHover?: (event: CanvasEvent | null) => void;
    onError?: (error: unknown) => void;
}
export interface InternalControllerSnapshot {
    status: 'idle' | 'updating' | 'ready' | 'error' | 'disposed';
    /** Read-only snapshot. Requests and downloads use data.raw, never rendering IDs. */
    data: GraphData;
    canUndo: boolean;
    canRedo: boolean;
    selectedIds: string[];
    hoveredIds: string[];
    error?: unknown;
}
// Spell the recursive JSON view directly so TypeScript need not expand a mapped
// type over JsonValue indefinitely when checking GraphData assignability.
type ReadonlyJson = string | number | boolean | null | readonly ReadonlyJson[] | {readonly [key: string]: ReadonlyJson};
interface ReadonlyStyle {
    readonly [key: string]: ReadonlyJson | undefined;
    readonly x?: number;
    readonly y?: number;
}
type ReadonlyNode = Readonly<Omit<GraphNode, 'data' | 'style'>> & {
    readonly data: DeepReadonly<GraphNode['data']>;
    readonly style?: ReadonlyStyle;
};
type ReadonlyEdge = Readonly<Omit<GraphEdge, 'data' | 'style'>> & {
    readonly data: DeepReadonly<GraphEdge['data']>;
    readonly style?: ReadonlyStyle;
};
export interface ControllerSnapshot {
    readonly status: InternalControllerSnapshot['status'];
    readonly data: {readonly nodes: readonly ReadonlyNode[]; readonly edges: readonly ReadonlyEdge[]};
    readonly canUndo: boolean;
    readonly canRedo: boolean;
    readonly selectedIds: readonly string[];
    readonly hoveredIds: readonly string[];
    readonly error?: unknown;
}
/** Internal port: the controller owns scheduling; only G6Runtime touches the engine. */
export interface GraphRuntime {
    idleInteraction(): Promise<void>;
    onEvent(handler: (event: RuntimeEvent) => void): void;
    replace(data: GraphData, layout: GraphLayout | null): Promise<void>;
    patch(data: Partial<GraphData>, record: boolean): Promise<void>;
    layout(layout: GraphLayout): Promise<void>;
    remove(ids: {nodes: string[]; edges: string[]}): Promise<void>;
    setMinimap(visible: boolean): void;
    read(): GraphData;
    readElement(kind: 'node', id: string): GraphNode;
    readElement(kind: 'edge', id: string): GraphEdge;
    states(states: Record<string, string[]>): Promise<void>;
    refreshLabelVisibility(itemCount: number): Promise<void>;
    visibility(ids: string[], visible: boolean): Promise<void>;
    fitView(): Promise<void>;
    zoomBy(factor: number): Promise<void>;
    resize(width: number, height: number): void;
    exportPng(): Promise<string>;
    history(action: 'undo' | 'redo' | 'clear'): Promise<void>;
    historyState(): {canUndo: boolean; canRedo: boolean};
    syncPins(ids?: string[]): void;
    releasePins(ids: string[]): void;
    stop(): void;
    destroy(): void;
}
