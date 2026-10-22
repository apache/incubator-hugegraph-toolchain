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

import {hubbleRenderId} from '../../../../visualization/hubble/adapter';

const isJsonBigNumber = value => {
    return value !== null
        && typeof value === 'object'
        && typeof value.constructor?.isBigNumber === 'function'
        && value.constructor.isBigNumber(value)
        && typeof value.toString === 'function';
};

const projectJsonValue = (value, ancestors = new WeakSet()) => {
    if (typeof value === 'bigint' || isJsonBigNumber(value)) {
        return value.toString();
    }
    if (value === null || typeof value !== 'object') {
        return value;
    }
    if (ancestors.has(value)) {
        return '[Circular]';
    }
    ancestors.add(value);
    const projected = Array.isArray(value) ? [] : {};
    Object.keys(value).forEach(key => {
        Object.defineProperty(projected, key, {
            configurable: true,
            enumerable: true,
            value: projectJsonValue(value[key], ancestors),
            writable: true,
        });
    });
    ancestors.delete(value);
    return projected;
};

const getJsonViewContent = jsonView => {
    const projected = projectJsonValue(jsonView?.data ?? []);
    if (projected !== null && typeof projected === 'object') {
        return projected;
    }
    return {value: projected};
};

const getQueryResultStandbyMessage = (t, isQueryMode) => {
    if (isQueryMode === false) {
        return t('analysis.query_result.task_not_started');
    }
    return t('analysis.query_result.not_started');
};

const GRAPH_NODE_LIMIT = 1000;
const GRAPH_EDGE_LIMIT = 500;

// Bound work before copying values or serializing them, including nested paths/properties.
const getBoundedJsonPreview = (value, {items = 100, characters = 20000, depth = 8} = {}) => {
    let remaining = characters;
    let remainingItems = 2000;
    let truncated = false;
    const ancestors = new WeakSet();
    const project = (input, level) => {
        if (remaining <= 0 || remainingItems-- <= 0) {
            truncated = true;
            return '…';
        }
        if (typeof input === 'bigint' || isJsonBigNumber(input)) {
            input = input.toString();
        }
        if (typeof input === 'string') {
            const length = Math.min(input.length, remaining, 1000);
            remaining -= length;
            truncated ||= length < input.length;
            return input.slice(0, length) + (length < input.length ? '…' : '');
        }
        if (input === null || typeof input !== 'object') {
            remaining -= 16;
            return input;
        }
        if (ancestors.has(input)) {
            return '[Circular]';
        }
        if (level >= depth) {
            truncated = true;
            return '…';
        }
        ancestors.add(input);
        const output = Array.isArray(input) ? [] : {};
        let count = 0;
        // Do not enumerate/copy the complete collection before applying its budget.
        for (const key in input) {
            if (!Object.prototype.hasOwnProperty.call(input, key)) {
                continue;
            }
            if (count++ >= items || remaining <= 0 || remainingItems <= 0) {
                truncated = true;
                break;
            }
            // Keys cannot be shortened without changing their meaning or
            // merging entries. Omit an unreadable key before accessing its
            // value or allocating a property that JSON.stringify must copy.
            if (key.length > Math.min(remaining, 1000)) {
                truncated = true;
                continue;
            }
            remaining -= key.length;
            Object.defineProperty(output, key, {enumerable: true, configurable: true,
                writable: true, value: project(input[key], level + 1)});
        }
        ancestors.delete(input);
        return output;
    };
    return {content: project(value, 0), truncated};
};

const getBoundedGraphView = (graphView = {}, edgeLimit = GRAPH_EDGE_LIMIT,
    {vertexIds, edgeIds} = {}) => {
    const partialVertices = new Set((vertexIds ?? []).map(id => hubbleRenderId('node', id)));
    const partialEdges = new Set((edgeIds ?? []).map(id => hubbleRenderId('edge', id)));
    // Keep the read-only marker in raw graph data, including saved/exported canvases.
    const preview = (element, partial) => partial ? {...element, '~hubble_properties_preview': true} : element;
    const vertices = [];
    const ids = new Set();
    for (const vertex of graphView.vertices || []) {
        if (!vertex || vertex.id === undefined || vertex.id === null) {
            continue;
        }
        const id = hubbleRenderId('node', vertex.id);
        if (!ids.has(id)) {
            ids.add(id);
            vertices.push(preview(vertex, partialVertices.has(id)));
        }
        if (vertices.length >= GRAPH_NODE_LIMIT) {
            break;
        }
    }
    const edges = [];
    const renderedEdgeIds = new Set();
    for (const edge of graphView.edges || []) {
        if (edges.length >= edgeLimit) {
            break;
        }
        if (!edge || edge.id === undefined || edge.source === undefined || edge.target === undefined) {
            continue;
        }
        const id = hubbleRenderId('edge', edge.id);
        if (!renderedEdgeIds.has(id) && ids.has(hubbleRenderId('node', edge.source))
            && ids.has(hubbleRenderId('node', edge.target))) {
            renderedEdgeIds.add(id);
            edges.push(preview(edge, partialEdges.has(id)));
        }
    }
    return {...graphView, vertices, edges};
};

const getGraphViewLimitStatus = (graphView = {}, edgeLimit = GRAPH_EDGE_LIMIT) => {
    const nodeCount = Array.isArray(graphView.vertices) ? graphView.vertices.length : 0;
    const edgeCount = Array.isArray(graphView.edges) ? graphView.edges.length : 0;
    return {
        nodeCount,
        edgeCount,
        exceeded: nodeCount > GRAPH_NODE_LIMIT || edgeCount > edgeLimit,
    };
};

export {
    GRAPH_EDGE_LIMIT,
    GRAPH_NODE_LIMIT,
    getBoundedGraphView,
    getBoundedJsonPreview,
    getGraphViewLimitStatus,
    getJsonViewContent,
    getQueryResultStandbyMessage,
    isJsonBigNumber,
    projectJsonValue,
};
