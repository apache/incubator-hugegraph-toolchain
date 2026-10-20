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

import type {GraphStyle} from '../model/types';
import {resolveIconName} from '../../utils/iconNames';

export type HubbleStyle = Record<string, unknown>;
const nodeSizes = new Map([['TINY', 10], ['SMALL', 20], ['NORMAL', 30], ['BIG', 40], ['HUGE', 50]]);
const edgeSizes = new Map([['THICK', 0.8], ['NORMAL', 1.6], ['FINE', 2.4]]);
export const normalizeIcon = (value: unknown): string | undefined => resolveIconName(value, '') || undefined;
export const asRecord = (value: unknown): Record<string, unknown> =>
    value !== null && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {};
const number = (value: unknown, fallback: number): number =>
    typeof value === 'number' && Number.isFinite(value) ? value : fallback;
const text = (value: unknown, fallback: string): string => typeof value === 'string' ? value : fallback;
const sizes = (value: unknown, fallback: number): number | number[] => {
    if (Array.isArray(value) && value.length > 0 && value.every(item => typeof item === 'number' && Number.isFinite(item))) {
        return value as number[];
    }
    return number(value, fallback);
};
const copyLabelStyle = (source: Record<string, unknown>, target: GraphStyle): void => {
    for (const [oldKey, newKey] of [['fill', 'labelFill'], ['fontSize', 'labelFontSize'],
        ['fontFamily', 'labelFontFamily'], ['fontWeight', 'labelFontWeight']] as const) {
        const value = source[oldKey];
        if (typeof value === 'string' || (typeof value === 'number' && Number.isFinite(value))) {
            target[newKey] = value;
        }
    }
};

/** Converts persisted Hubble/G6 4 presentation fields once, without editing the persisted record. */
export const nodeStyle = (persisted: HubbleStyle, customized: HubbleStyle, label: string): GraphStyle => {
    const oldStyle = asRecord(customized.style);
    const labelConfig = asRecord(customized.labelCfg);
    const icon = asRecord(customized.icon);
    const color = text(persisted.color, '#5c73e6');
    const style: GraphStyle = {
        size: sizes(customized.size, nodeSizes.get(text(persisted.size, 'NORMAL')) ?? 30),
        fill: text(oldStyle.fill, color),
        stroke: text(oldStyle.stroke, color),
        lineWidth: number(oldStyle.lineWidth, 1),
        fillOpacity: number(oldStyle.fillOpacity, 0.4),
        strokeOpacity: number(oldStyle.strokeOpacity, 1),
        labelText: label,
        labelPlacement: text(labelConfig.position, 'bottom'),
        labelFill: '#343434',
        labelFontSize: 12,
        iconFontSize: number(icon.fontSize, 12),
    };
    if (typeof icon.fill === 'string') {
        style.iconFill = icon.fill;
    }
    copyLabelStyle(asRecord(labelConfig.style), style);
    return style;
};

export const edgeStyle = (persisted: HubbleStyle, customized: HubbleStyle, label: string): GraphStyle => {
    const oldStyle = asRecord(customized.style);
    const labelConfig = asRecord(customized.labelCfg);
    const width = edgeSizes.get(text(persisted.thickness, 'NORMAL')) ?? 1.6;
    const arrow = oldStyle.endArrow;
    const style: GraphStyle = {
        stroke: text(oldStyle.stroke, text(persisted.color, '#5c73e6')),
        lineWidth: number(oldStyle.lineWidth, width),
        endArrow: arrow === undefined ? persisted.with_arrow === true : arrow !== false && arrow !== null,
        labelText: label,
        labelFill: '#000000',
        labelFontSize: 12,
        labelAutoRotate: true,
        labelBackground: true,
        labelBackgroundFill: '#ffffff',
        labelPadding: [2, 2, 2, 2],
    };
    const dash = oldStyle.lineDash;
    if (Array.isArray(dash) && dash.every(item => typeof item === 'number' && Number.isFinite(item))) {
        style.lineDash = dash as number[];
    }
    else if (dash === false) {
        style.lineDash = [];
    }
    else if (persisted.line_type === 'DASHED' || persisted.line_type === 'DASH') {
        style.lineDash = [5, 5];
    }
    copyLabelStyle(asRecord(labelConfig.style), style);
    return style;
};

const nodeShapes = new Set(['circle', 'rect', 'diamond', 'triangle', 'star', 'ellipse', 'hexagon']);
export const nodeShape = (value: unknown): string => typeof value === 'string' && nodeShapes.has(value) ? value : 'circle';
