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

import type {ElementKind, GraphData} from './types';

export interface AdjacencyIndex {
    /** Each self-loop appears once. Parallel edges retain their separate IDs. */
    incidentEdges: ReadonlyMap<string, ReadonlySet<string>>;
    neighbors: ReadonlyMap<string, ReadonlySet<string>>;
    endpoints: ReadonlyMap<string, readonly [string, string]>;
}
export interface RelatedElements {
    nodeIds: string[];
    edgeIds: string[];
}

export const buildAdjacencyIndex = (data: GraphData): AdjacencyIndex => {
    const incidentEdges = new Map<string, Set<string>>();
    const neighbors = new Map<string, Set<string>>();
    const endpoints = new Map<string, readonly [string, string]>();
    for (const node of data.nodes) {
        incidentEdges.set(node.id, new Set());
        neighbors.set(node.id, new Set());
    }
    for (const edge of data.edges) {
        incidentEdges.get(edge.source)?.add(edge.id);
        incidentEdges.get(edge.target)?.add(edge.id);
        neighbors.get(edge.source)?.add(edge.target);
        neighbors.get(edge.target)?.add(edge.source);
        endpoints.set(edge.id, [edge.source, edge.target]);
    }
    return {incidentEdges, neighbors, endpoints};
};

/** Hover lookup visits only the selected node's degree, never all graph elements. */
export const relatedElements = (index: AdjacencyIndex, kind: ElementKind, id: string): RelatedElements => {
    if (kind === 'edge') {
        const pair = index.endpoints.get(id);
        return pair ? {nodeIds: [...new Set(pair)], edgeIds: [id]} : {nodeIds: [], edgeIds: []};
    }
    const neighbors = index.neighbors.get(id);
    return neighbors
        ? {nodeIds: [...new Set([id, ...neighbors])], edgeIds: [...(index.incidentEdges.get(id) ?? [])]}
        : {nodeIds: [], edgeIds: []};
};
