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

export type RawId = string | number;
export type ElementKind = 'node' | 'edge';
export type JsonValue = string | number | boolean | null | JsonValue[] | {[key: string]: JsonValue};
/**
 * Presentation attributes. Edge zIndex is reserved by the runtime: edges render
 * below nodes so crossing edges cannot block node interaction. Supplied edge
 * zIndex values remain in model snapshots but do not control drawing order.
 */
export interface GraphStyle {
    [key: string]: JsonValue | undefined;
    x?: number;
    y?: number;
}
export interface ElementData {
    /** Complete input record. Backend requests and downloads use this, never the rendering ID. */
    raw: Record<string, unknown>;
    /** Business label; display text is style.labelText. */
    label?: string;
    legend?: string;
    group?: string;
    shape?: string;
    icon?: string;
    highlighted?: boolean;
}
export interface NodeData extends ElementData {
    /** Controller-owned pin state. Replacement and merge cannot reset an existing pin. */
    fixed?: boolean;
}
export interface GraphNode {
    id: string;
    data: NodeData;
    style?: GraphStyle;
}
export interface GraphEdge {
    id: string;
    source: string;
    target: string;
    data: ElementData;
    style?: GraphStyle;
}
export interface GraphData {
    nodes: GraphNode[];
    edges: GraphEdge[];
}
export interface GraphEvent {
    id: string;
    kind: ElementKind;
    data: NodeData | ElementData;
}
export interface NormalizeOptions {
    /** Used for partial neighbour responses. Missing endpoints still fail after merge. */
    existingNodeIds?: ReadonlySet<string>;
}
export interface MergeResult {
    data: GraphData;
    changed: boolean;
    addedNodeIds: string[];
    addedEdgeIds: string[];
    updatedNodeIds: string[];
    updatedEdgeIds: string[];
}
