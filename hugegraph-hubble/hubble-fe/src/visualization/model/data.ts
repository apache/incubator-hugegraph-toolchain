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

import type {ElementKind, GraphData, GraphEdge, GraphNode, GraphStyle, MergeResult,
    NormalizeOptions, RawId} from './types';

export class GraphDataError extends Error {
    constructor(message: string) {
        super(message);
        this.name = 'GraphDataError';
    }
}

export const renderId = (kind: ElementKind, id: RawId): string => {
    if (typeof id === 'number' && Number.isFinite(id)) {
        return `${kind}:number:${Object.is(id, -0) ? '-0' : id}`;
    }
    if (typeof id === 'string') {
        return `${kind}:string:${encodeURIComponent(id)}`;
    }
    throw new GraphDataError(`${kind} ID must be a string or finite number`);
};

const assertJson = (value: unknown, path: string, ancestors = new Set<object>()): void => {
    if (value === null || typeof value === 'string' || typeof value === 'boolean'
        || (typeof value === 'number' && Number.isFinite(value))) {
        return;
    }
    if (typeof value !== 'object') {
        throw new GraphDataError(`${path} must contain only finite JSON values`);
    }
    if (ancestors.has(value)) {
        throw new GraphDataError(`${path} contains a cycle`);
    }
    const prototype: unknown = Object.getPrototypeOf(value);
    if (!Array.isArray(value) && prototype !== Object.prototype && prototype !== null) {
        throw new GraphDataError(`${path} must contain plain JSON objects`);
    }
    ancestors.add(value);
    for (const [key, entry] of Object.entries(value)) {
        assertJson(entry, `${path}.${key}`, ancestors);
    }
    ancestors.delete(value);
};

export const assertGraphStyle = (style: GraphStyle, path = 'Graph style'): void => {
    if (style === null || typeof style !== 'object' || Array.isArray(style)) {
        throw new GraphDataError(`${path} must be a JSON object`);
    }
    assertJson(style, path);
    for (const axis of ['x', 'y'] as const) {
        if (axis in style && (typeof style[axis] !== 'number' || !Number.isFinite(style[axis]))) {
            throw new GraphDataError(`${path}.${axis} must be a finite coordinate`);
        }
    }
};

const assertElement = (item: GraphNode | GraphEdge, kind: ElementKind): void => {
    if (!item || typeof item.id !== 'string' || !item.id) {
        throw new GraphDataError(`${kind} requires a nonempty rendering ID`);
    }
    if (!item.data || !item.data.raw || typeof item.data.raw !== 'object' || Array.isArray(item.data.raw)) {
        throw new GraphDataError(`${kind} ${item.id} requires its raw input record`);
    }
    if ('fixed' in item.data && item.data.fixed !== undefined && typeof item.data.fixed !== 'boolean') {
        throw new GraphDataError(`Node ${item.id} fixed state must be a boolean`);
    }
    if (item.style !== undefined) {
        assertGraphStyle(item.style, `${kind} ${item.id}.style`);
    }
};

/** Last duplicate wins, while first occurrence controls stable display order. No edge is dropped. */
export const normalizeGraphData = (input: GraphData, options: NormalizeOptions = {}): GraphData => {
    if (!input || !Array.isArray(input.nodes) || !Array.isArray(input.edges)) {
        throw new GraphDataError('Graph data requires nodes and edges arrays');
    }
    const nodes = new Map<string, GraphNode>();
    const edges = new Map<string, GraphEdge>();
    for (const node of input.nodes) {
        assertElement(node, 'node');
        nodes.set(node.id, node);
    }
    for (const edge of input.edges) {
        assertElement(edge, 'edge');
        if (nodes.has(edge.id) || options.existingNodeIds?.has(edge.id)) {
            throw new GraphDataError(`Node and edge rendering IDs collide: ${edge.id}`);
        }
        const duplicate = edges.get(edge.id);
        if (duplicate && (duplicate.source !== edge.source || duplicate.target !== edge.target)) {
            throw new GraphDataError(`Conflicting endpoints for duplicate edge ${edge.id}`);
        }
        for (const endpoint of [edge.source, edge.target]) {
            if (typeof endpoint !== 'string' || (!nodes.has(endpoint) && !options.existingNodeIds?.has(endpoint))) {
                throw new GraphDataError(`Edge ${edge.id} references missing node ${String(endpoint)}`);
            }
        }
        edges.set(edge.id, edge);
    }
    return {nodes: [...nodes.values()], edges: [...edges.values()]};
};

export const sameTopology = (left: GraphData, right: GraphData): boolean => {
    if (left.nodes.length !== right.nodes.length || left.edges.length !== right.edges.length) {
        return false;
    }
    const nodes = new Set(left.nodes.map(node => node.id));
    if (nodes.size !== left.nodes.length || new Set(right.nodes.map(node => node.id)).size !== right.nodes.length
        || right.nodes.some(node => !nodes.has(node.id))) {
        return false;
    }
    const edges = new Map(left.edges.map(edge => [edge.id, edge]));
    if (edges.size !== left.edges.length || new Set(right.edges.map(edge => edge.id)).size !== right.edges.length) {
        return false;
    }
    return right.edges.every(edge => {
        const previous = edges.get(edge.id);
        return previous?.source === edge.source && previous.target === edge.target;
    });
};

const preserveNode = (previous: GraphNode, next: GraphNode): GraphNode => {
    const style: GraphStyle = {...previous.style, ...next.style};
    if (previous.style?.x !== undefined) {
        style.x = previous.style.x;
    }
    if (previous.style?.y !== undefined) {
        style.y = previous.style.y;
    }
    const data = {...previous.data, ...next.data};
    if (previous.data.fixed !== undefined) {
        data.fixed = previous.data.fixed;
    }
    return {...next, data, ...(previous.style || next.style ? {style} : {})};
};

/** Only a refresh of the same topology preserves placement. Explicit pin changes belong to the controller. */
export const preserveNodePositions = (previous: GraphData, next: GraphData): GraphData => {
    if (!sameTopology(previous, next)) {
        return next;
    }
    const nodes = new Map(previous.nodes.map(node => [node.id, node]));
    return {...next, nodes: next.nodes.map(node => preserveNode(nodes.get(node.id)!, node))};
};

const equalValue = (left: unknown, right: unknown): boolean => {
    if (Object.is(left, right)) {
        return true;
    }
    if (left === null || right === null || typeof left !== 'object' || typeof right !== 'object') {
        return false;
    }
    if (Object.getPrototypeOf(left) !== Object.getPrototypeOf(right)) {
        return false;
    }
    const leftEntries = Object.entries(left);
    const rightRecord = right as Record<string, unknown>;
    return leftEntries.length === Object.keys(right).length && leftEntries.every(([key, value]) =>
        Object.prototype.hasOwnProperty.call(right, key) && equalValue(value, rightRecord[key]));
};

export interface MergeOptions {
    /** Neighbour expansion updates raw records without resetting the existing rendered appearance. */
    preserveAppearance?: boolean;
}

const preserveAppearance = <T extends GraphNode | GraphEdge>(previous: T, next: T): T => {
    const result = {...next, data: {...previous.data, raw: next.data.raw}};
    if (previous.style === undefined) delete result.style;
    else result.style = previous.style;
    return result;
};

/** Linear in the number of graph elements and their record fields. Edge-only expansion is a change. */
export const mergeGraphData = (current: GraphData, incoming: GraphData, options: MergeOptions = {}): MergeResult => {
    const normalized = normalizeGraphData(current);
    const partial = normalizeGraphData(incoming, {existingNodeIds: new Set(normalized.nodes.map(node => node.id))});
    const nodes = new Map(normalized.nodes.map(node => [node.id, node]));
    const edges = new Map(normalized.edges.map(edge => [edge.id, edge]));
    const addedNodeIds: string[] = [];
    const addedEdgeIds: string[] = [];
    const updatedNodeIds: string[] = [];
    const updatedEdgeIds: string[] = [];
    for (const node of partial.nodes) {
        const previous = nodes.get(node.id);
        const next = previous ? (options.preserveAppearance ? preserveAppearance(previous, node)
            : preserveNode(previous, node)) : node;
        if (!previous) {
            addedNodeIds.push(node.id);
            nodes.set(node.id, next);
        }
        else if (!equalValue(previous, next)) {
            updatedNodeIds.push(node.id);
            nodes.set(node.id, next);
        }
    }
    for (const edge of partial.edges) {
        const previous = edges.get(edge.id);
        const next = previous ? (options.preserveAppearance ? preserveAppearance(previous, edge)
            : {...edge, data: {...previous.data, ...edge.data},
                ...(previous.style || edge.style ? {style: {...previous.style, ...edge.style}} : {})}) : edge;
        if (!previous) {
            addedEdgeIds.push(edge.id);
            edges.set(edge.id, next);
        }
        else if (!equalValue(previous, next)) {
            updatedEdgeIds.push(edge.id);
            edges.set(edge.id, next);
        }
    }
    const changed = addedNodeIds.length + addedEdgeIds.length + updatedNodeIds.length + updatedEdgeIds.length > 0;
    const data = changed ? normalizeGraphData({nodes: [...nodes.values()], edges: [...edges.values()]}) : current;
    return {data, changed, addedNodeIds, addedEdgeIds, updatedNodeIds, updatedEdgeIds};
};
