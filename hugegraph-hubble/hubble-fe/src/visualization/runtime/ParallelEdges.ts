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

import {BaseTransform} from '@antv/g6';
import type {DrawData} from '@antv/g6';

const DISTANCE = 24;
const MAX_CURVE_OFFSET = 480;
const LOOP_PLACEMENTS = ['top', 'top-right', 'right', 'right-bottom',
    'bottom', 'bottom-left', 'left', 'left-top'] as const;
interface Geometry {
    curveOffset?: number;
    loopPlacement?: typeof LOOP_PLACEMENTS[number];
    loopDist?: number;
}
interface DerivedEdge {
    source: string;
    target: string;
    geometry: Geometry;
}
const sameGeometry = (previous: Geometry | undefined, next: Geometry): boolean => Boolean(previous)
    && previous?.curveOffset === next.curveOffset && previous?.loopPlacement === next.loopPlacement
    && previous?.loopDist === next.loopDist;

/**
 * Bundle the module's explicit-ID, node-to-node edges in linear time. The cache
 * contains only endpoints and derived geometry; business data stays in G6.
 * Non-loop fans intentionally cap offsets at 480 instead of extending the
 * native 24-unit spacing without limit. Self-loop spread remains unchanged.
 * Required G6 transforms have already expanded node movement into affected
 * update.edges. Combo collapsing and merge mode are outside this data contract.
 */
export class ParallelEdges extends BaseTransform {
    private derived = new Map<string, DerivedEdge>();
    private topologyInitialized = false;

    private topologyChanged(input: DrawData): boolean {
        if (!this.topologyInitialized || input.add.edges.size || input.remove.edges.size) return true;
        for (const [id, edge] of input.update.edges) {
            const previous = this.derived.get(id);
            if (!previous || previous.source !== edge.source || previous.target !== edge.target) return true;
        }
        return false;
    }

    private rebuild(): Map<string, DerivedEdge> {
        const next = new Map<string, DerivedEdge>();
        // Nested maps avoid delimiter collisions and retain the model's stable
        // edge order, including last-value/first-position duplicate semantics.
        const groups = new Map<string, Map<string, string[]>>();
        for (const edge of this.context.graph.getEdgeData()) {
            if (typeof edge.id !== 'string') throw new Error('Parallel edges require explicit string IDs');
            if (next.has(edge.id)) {
                // The public G6 model is unique by ID. Reject a malformed port
                // rather than count an edge twice or fabricate a missing edge.
                throw new Error(`Duplicate edge ID in the graph model: ${edge.id}`);
            }
            next.set(edge.id, {source: edge.source, target: edge.target, geometry: {}});
            const first = edge.source <= edge.target ? edge.source : edge.target;
            const second = edge.source <= edge.target ? edge.target : edge.source;
            let targets = groups.get(first);
            if (!targets) { targets = new Map(); groups.set(first, targets); }
            let ids = targets.get(second);
            if (!ids) { ids = []; targets.set(second, ids); }
            ids.push(edge.id);
        }
        for (const targets of groups.values()) {
            for (const ids of targets.values()) {
                const first = next.get(ids[0])!;
                // Preserve native spacing for small groups. Large non-loop
                // fans intentionally compress into a fixed band so their
                // outer lanes do not obscure the rest of the graph.
                const distance = ids.length > 1
                    ? Math.min(DISTANCE, MAX_CURVE_OFFSET / (ids.length - 1)) : DISTANCE;
                ids.forEach((id, index) => {
                    const edge = next.get(id)!;
                    if (edge.source === edge.target) {
                        edge.geometry = {loopPlacement: LOOP_PLACEMENTS[index % LOOP_PLACEMENTS.length],
                            loopDist: 50 + Math.floor(index / LOOP_PLACEMENTS.length) * DISTANCE};
                    }
                    else {
                        const reverse = edge.source === first.target && edge.target === first.source;
                        const sign = (index % 2 === 0 ? 1 : -1) * (reverse ? -1 : 1);
                        const lane = ids.length % 2 === 1 ? Math.ceil(index / 2) * 2 : Math.floor(index / 2) * 2 + 1;
                        // Clamp floating-point roundoff at the outer lane;
                        // each lane inside the band retains a distinct offset.
                        edge.geometry = {curveOffset: ids.length === 1 ? 0
                            : sign * Math.min(MAX_CURVE_OFFSET, lane * distance)};
                    }
                });
            }
        }
        return next;
    }

    override beforeDraw(input: DrawData): DrawData {
        if (this.destroyed) return input;
        const affected = new Set([...input.add.edges.keys(), ...input.update.edges.keys()]);
        if (this.topologyChanged(input)) {
            const next = this.rebuild();
            for (const [id, edge] of next) {
                if (!sameGeometry(this.derived.get(id)?.geometry, edge.geometry)) affected.add(id);
            }
            this.derived = next;
            this.topologyInitialized = true;
        }
        for (const id of affected) {
            const cached = this.derived.get(id);
            if (!cached) continue;
            const adding = input.add.edges.has(id);
            if (!adding && input.remove.edges.has(id)) continue;
            const edge = input.add.edges.get(id) ?? input.update.edges.get(id) ?? this.context.graph.getEdgeData(id);
            const projected = {...edge, type: 'quadratic', style: {...edge.style, ...cached.geometry}};
            if (adding && input.remove.edges.has(id)) {
                // G6 schedules removal until after animation. Keeping both
                // entries would skip creation while the old shape exists,
                // then remove it. Updating uses the complete replacement and
                // lets G6 restore the shape type without delayed destruction.
                input.add.edges.delete(id);
                input.remove.edges.delete(id);
                input.update.edges.set(id, projected);
            }
            else if (adding) {
                input.add.edges.set(id, projected);
                // update-related-edge can also mark a newly added edge as an
                // update. Do not overwrite its complete creation projection.
                input.update.edges.delete(id);
            }
            else input.update.edges.set(id, projected);
        }
        return input;
    }

    override destroy(): void {
        this.derived.clear();
        this.topologyInitialized = false;
        super.destroy();
    }
}
