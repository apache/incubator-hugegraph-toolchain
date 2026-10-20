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

/** Labels are camera-derived view state; they never change the saved graph data. */
export const labelVisibility = (itemCount: number, zoom: number): {nodeLabels: boolean; edgeLabels: boolean} => {
    if (itemCount < 200) return {nodeLabels: true, edgeLabels: true};
    // Medium graphs retain node names for orientation. Edge names are
    // available on hover/selection or when zoomed in, rather than covering
    // the overview with hundreds of overlapping labels and backgrounds.
    if (itemCount < 800) return {nodeLabels: true, edgeLabels: zoom >= 2};
    return {nodeLabels: zoom >= 2, edgeLabels: zoom >= 3};
};

/** Half-octave buckets keep zoomed labels near their original screen size.
 * Below 1x preserve the overview; refresh styles only when a bucket changes.
 */
export const labelFontScale = (zoom: number): number =>
    Number.isFinite(zoom) && zoom > 1 ? 2 ** (Math.round(Math.log2(zoom) * 2) / 2) : 1;
