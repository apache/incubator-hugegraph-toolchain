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

import {BasePlugin, D3ForceLayout, ExtensionCategory, invokeLayoutMethod, register} from '@antv/g6';
import type {BasePluginOptions} from '@antv/g6';
import {ParallelEdges} from './ParallelEdges';

/** Seed fx/fy before D3 copies nodes. G6's adapter otherwise maps only id/x/y. */
class PinnedForceLayout extends D3ForceLayout {
    protected override layout(): Promise<void> {
        this.model.forEachNode(node => {
            const original = node._original as {data?: {fixed?: boolean}};
            if (original.data?.fixed && Number.isFinite(node.x) && Number.isFinite(node.y)) {
                node.fx = node.x;
                node.fy = node.y;
            }
        });
        return super.layout();
    }
}

/** Official extension context is the only route to the active layout instance. */
export class PinBridge extends BasePlugin<BasePluginOptions> {
    private pinned = new Set<string>();
    sync(ids?: string[]): void {
        const layout = this.context.layout?.getLayoutInstance().find(instance => instance.id === 'd3-force');
        if (!layout) return;
        const nodes = ids ? ids.filter(id => this.context.graph.hasNode(id)).map(id => this.context.graph.getNodeData(id))
            : this.context.graph.getNodeData();
        for (const node of nodes) {
            if (node.data?.fixed) {
                invokeLayoutMethod(layout, 'setFixedPosition', node.id, [node.style?.x ?? 0, node.style?.y ?? 0]);
                this.pinned.add(node.id);
            }
            else if (this.pinned.delete(node.id)) invokeLayoutMethod(layout, 'setFixedPosition', node.id, null);
        }
        if (!ids) {
            const existing = new Set(nodes.map(node => node.id));
            this.pinned.forEach(id => { if (!existing.has(id)) this.pinned.delete(id); });
        }
    }
    release(ids: string[]): void {
        const layout = this.context.layout?.getLayoutInstance().find(instance => instance.id === 'd3-force');
        if (!layout) return;
        ids.forEach(id => {
            invokeLayoutMethod(layout, 'setFixedPosition', id, null);
            this.pinned.delete(id);
        });
        // A static layout has no tick listener updating G6. Reheat its state,
        // then stop its timer; the next requested layout computes the new position.
        invokeLayoutMethod(layout, 'reheat');
        invokeLayoutMethod(layout, 'stop');
    }
}
let registered = false;
export const registerExtensions = (): void => {
    if (registered) return;
    register(ExtensionCategory.LAYOUT, 'hubble-pinned-d3-force', PinnedForceLayout);
    register(ExtensionCategory.PLUGIN, 'hubble-pin-bridge', PinBridge);
    register(ExtensionCategory.TRANSFORM, 'hubble-parallel-edges', ParallelEdges);
    registered = true;
};
