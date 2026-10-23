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

import type {GraphLayout} from './types';

/** Shared adaptive defaults; omitted user options remain owned by the layout runtime. */
export const adaptiveLayoutDefaults = (layout: GraphLayout,
    nodes: readonly {style?: {size?: unknown}}[]): NonNullable<GraphLayout['options']> => {
    if (layout.type === 'd3-force') {
        const small = nodes.length <= 20;
        // Static force ticks run on the main thread. Keep small graph quality while
        // bounding the default work for larger previews; explicit options override this.
        const iterations = Math.max(50, Math.round(30000 / Math.max(100, nodes.length)));
        return {link: {distance: small ? 180 : 100}, manyBody: {strength: small ? -800 : -250},
            collide: {radius: small ? 40 : 28}, iterations,
            // Axis attraction contains disconnected nodes without pulling separate groups together.
            x: {strength: 0.05}, y: {strength: 0.05}};
    }
    if (layout.type !== 'radial') return {};
    const largestSize = (size: unknown) => Math.max(0, ...(Array.isArray(size) ? size : [size])
        .filter((value): value is number => typeof value === 'number' && Number.isFinite(value)));
    const size = Math.max(48, ...nodes.map(node => largestSize(node.style?.size)));
    const spacing = layout.options?.nodeSpacing === undefined ? 24 : largestSize(layout.options.nodeSpacing);
    return {preventOverlap: true, strictRadial: false, nodeSize: size, nodeSpacing: 24,
        unitRadius: Math.max(100, Math.max(size, largestSize(layout.options?.nodeSize)) + spacing)};
};
