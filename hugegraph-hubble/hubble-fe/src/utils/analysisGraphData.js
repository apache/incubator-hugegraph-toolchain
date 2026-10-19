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

import {adaptHubbleGraph, hubbleRenderId} from '../visualization/hubble/adapter';
import {mutableCopy} from '../visualization/model/ownership';
import {ALGORITHM_NAME, getCanonicalAlgorithmName} from './constants';

const indexMeta = entries => new Map((entries || []).map(entry => [entry.name, entry]));
const endpointIds = values => new Set((values || []).map(id => hubbleRenderId('node', id)));
const highlightedStyle = style => ({
    ...style,
    stroke: '#d48806',
    lineWidth: Math.max(Number(style?.lineWidth) || 0, 3),
});

/** Keep algorithm semantics at the Hubble boundary, with no engine or combo objects. */
export const adaptAnalysisGraph = (raw, meta = {}, savedStyles = {}, options = {}, algorithmName,
    normalizeOptions = {}) => {
    const data = adaptHubbleGraph(raw, meta, savedStyles, normalizeOptions);
    const {startId, endPointsId} = options || {};
    const start = startId === undefined || startId === null ? undefined : hubbleRenderId('node', startId);
    const starts = endpointIds(endPointsId?.startNodes);
    const ends = endpointIds(endPointsId?.endNodes);
    const name = getCanonicalAlgorithmName(algorithmName);
    const shortest = name === ALGORITHM_NAME.SHORTEST_PATH;
    const endpointsHighlighted = name === ALGORITHM_NAME.SAME_NEIGHBORS || name === ALGORITHM_NAME.ALLPATHS;
    const pathsHighlighted = shortest || endpointsHighlighted;
    return {
        nodes: data.nodes.map(node => {
            const highlighted = shortest || node.id === start
                || (endpointsHighlighted && (starts.has(node.id) || ends.has(node.id)));
            return {
                ...node,
                data: {...node.data,
                    ...(endPointsId ? {group: ends.has(node.id) ? 'end' : starts.has(node.id) ? 'start' : 'middle'} : {}),
                    ...(highlighted ? {highlighted: true} : {})},
                ...(highlighted ? {style: highlightedStyle(node.style)} : {}),
            };
        }),
        edges: pathsHighlighted ? data.edges.map(edge => ({...edge,
            data: {...edge.data, highlighted: true}, style: highlightedStyle(edge.style)})) : data.edges,
    };
};

/** Group by an unordered pair without concatenation collisions or an all-edge nested scan. */
const add3DGeometry = edges => {
    const pairs = new Map();
    for (const edge of edges) {
        const [first, second] = edge.source <= edge.target ? [edge.source, edge.target] : [edge.target, edge.source];
        let targets = pairs.get(first);
        if (!targets) { targets = new Map(); pairs.set(first, targets); }
        let group = targets.get(second);
        if (!group) { group = []; targets.set(second, group); }
        group.push(edge);
    }
    for (const targets of pairs.values()) {
        for (const group of targets.values()) {
            const first = group[0];
            group.forEach((edge, index) => {
                if (edge.source === edge.target) {
                    edge.curvature = 0.5 + Math.floor(index / 8) * 0.2;
                    edge.rotation = index % 8 * Math.PI / 4;
                }
                else if (group.length > 1) {
                    // Reverse endpoints reverse the bend plane; compensate to keep lanes distinct.
                    const direction = edge.source === first.source ? 1 : -1;
                    edge.curvature = (0.5 + Math.floor(index / 8) * 0.4) * direction;
                    edge.rotation = (group.length > 8 ? index % 8 * Math.PI / 4
                        : index / group.length * Math.PI * 2) * direction;
                }
            });
        }
    }
    return edges;
};

/** The independent 3D engine receives detached writable records and stable endpoint IDs. */
export const toCanvas3DData = (graphData, meta = {}) => {
    const vertices = indexMeta(meta.vertexMeta);
    const edgeLabels = indexMeta(meta.edgeMeta);
    const visibleNodes = graphData.nodes.filter(node => node.style?.visibility !== 'hidden');
    const visibleIds = new Set(visibleNodes.map(node => node.id));
    const nodes = visibleNodes.map(node => {
        const raw = node.data.raw;
        return mutableCopy({
            id: node.id,
            rawId: raw.id,
            properties: raw.properties || {},
            statistics: raw.statistics,
            itemType: raw.label,
            label: node.style?.labelText ?? String(raw.id),
            type: node.data.shape || 'circle',
            size: node.style?.size ?? 30,
            style: node.style || {},
            metaConfig: vertices.get(raw.label),
        });
    });
    const edges = graphData.edges.filter(edge => edge.style?.visibility !== 'hidden'
        && visibleIds.has(edge.source) && visibleIds.has(edge.target)).map(edge => {
        const raw = edge.data.raw;
        const metadata = edgeLabels.get(raw.label) || {};
        return mutableCopy({
            id: edge.id,
            rawId: raw.id,
            source: edge.source,
            target: edge.target,
            rawSource: raw.source,
            rawTarget: raw.target,
            label: edge.style?.labelText ?? raw.label,
            rawLabel: raw.label,
            itemType: raw.label,
            type: 'line',
            properties: raw.properties || {},
            style: edge.style || {},
            metaConfig: {...metadata, style: {...metadata.style,
                ...(edge.style?.endArrow === undefined ? {} : {with_arrow: Boolean(edge.style.endArrow)})}},
            curvature: 0,
            rotation: 0,
        });
    });
    return {nodes, edges: add3DGeometry(edges)};
};

/** Drawer/API fields use business IDs. Rendering identity is carried separately. */
export const toElementDetails = (event, meta = {}) => {
    if (!event?.data?.raw || !['node', 'edge'].includes(event.kind)) return null;
    const {raw} = event.data;
    const metadata = indexMeta(event.kind === 'edge' ? meta.edgeMeta : meta.vertexMeta).get(raw.label);
    return {
        kind: event.kind,
        renderingId: event.id,
        id: raw.id,
        label: raw.label,
        rawLabel: raw.label,
        itemType: raw.label,
        properties: raw.properties || {},
        statistics: raw.statistics,
        source: raw.source,
        target: raw.target,
        type: event.kind === 'edge' ? 'line' : event.data.shape || 'circle',
        metaConfig: metadata,
        raw,
    };
};
