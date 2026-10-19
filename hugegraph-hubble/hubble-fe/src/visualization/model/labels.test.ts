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

import {labelFontScale, labelVisibility} from './labels';

test('small graphs keep labels; medium graphs reveal edge names on zoom; dense graphs reveal nodes then edges', () => {
    expect(labelVisibility(199, 0.03)).toEqual({nodeLabels: true, edgeLabels: true});
    expect(labelVisibility(200, 0.03)).toEqual({nodeLabels: true, edgeLabels: false});
    expect(labelVisibility(500, 1.99)).toEqual({nodeLabels: true, edgeLabels: false});
    expect(labelVisibility(500, 2)).toEqual({nodeLabels: true, edgeLabels: true});
    expect(labelVisibility(799, 0.03)).toEqual({nodeLabels: true, edgeLabels: false});
    expect(labelVisibility(800, 1.99)).toEqual({nodeLabels: false, edgeLabels: false});
    expect(labelVisibility(800, 2)).toEqual({nodeLabels: true, edgeLabels: false});
    expect(labelVisibility(800, 2.99)).toEqual({nodeLabels: true, edgeLabels: false});
    expect(labelVisibility(800, 3)).toEqual({nodeLabels: true, edgeLabels: true});
});

test('zoomed fonts remain near their base screen size with bounded bucket refreshes', () => {
    expect([0, 0.03, 0.5, 1, NaN, Infinity].map(labelFontScale)).toEqual([1, 1, 1, 1, 1, 1]);
    const buckets = new Set<number>();
    for (let zoom = 1; zoom <= 8; zoom += 0.01) {
        const scale = labelFontScale(zoom);
        buckets.add(scale);
        expect(zoom / scale).toBeGreaterThan(0.84);
        expect(zoom / scale).toBeLessThan(1.2);
    }
    expect(buckets.size).toBe(7);
    expect(labelFontScale(4)).toBe(4);
    expect(labelFontScale(8)).toBe(8);
});
