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

import {isEqual} from 'lodash';
import {hubbleRenderId} from '../visualization/hubble/adapter';
import {edgeStyle, nodeShape, nodeStyle, normalizeIcon} from '../visualization/hubble/styles';
import {hubbleIconSource} from '../visualization/hubble/icons';
import {mutableCopy} from '../visualization/model/ownership';

const layoutTypes = new Map([
    ['force', 'd3-force'], ['force2', 'd3-force'], ['d3-force', 'd3-force'],
    ['relationship', 'columns'], ['columns', 'columns'], ['forceAtlas', 'force-atlas2'],
    ['forceAtlas2', 'force-atlas2'], ['force-atlas2', 'force-atlas2'], ['customGrid', 'grid'],
    ...['circular', 'concentric', 'dagre', 'grid', 'radial'].map(type => [type, type]),
]);

export const analysisLayout = selection => {
    const value = typeof selection === 'string' ? {layout: selection} : selection || {};
    const type = layoutTypes.get(value.layout || value.type) || 'd3-force';
    return type === 'radial' && value.startId !== undefined && value.startId !== null
        ? {type, options: {focusNode: hubbleRenderId('node', value.startId)}} : {type};
};

const optionKeys = {
    circular: ['radius', 'startRadius', 'endRadius', 'clockwise', 'divisions', 'ordering',
        'angleRatio', 'startAngle', 'endAngle'],
    concentric: ['preventOverlap', 'sweep', 'equidistant', 'startAngle', 'clockwise', 'maxLevelDiff', 'sortBy'],
    grid: ['begin', 'preventOverlap', 'condense', 'rows', 'cols', 'sortBy'],
    radial: ['linkDistance', 'focusNode', 'unitRadius', 'preventOverlap', 'maxPreventOverlapIteration',
        'strictRadial', 'sortBy', 'sortStrength', 'maxIteration'],
    dagre: ['rankdir', 'ranker', 'align', 'nodesep', 'ranksep', 'directed', 'compound', 'multigraph',
        'edgeMinLen', 'edgeWeight', 'edgeLabelSize', 'edgeLabelPos', 'edgeLabelOffset'],
    'force-atlas2': ['kr', 'kg', 'ks', 'ksmax', 'tao', 'mode', 'preventOverlap', 'dissuadeHubs',
        'barnesHut', 'prune', 'maxIteration', 'minMovement'],
    columns: ['columnSpacing', 'nodeSpacing'],
    'd3-force': ['center', 'centerX', 'centerY', 'centerStrength', 'alpha', 'alphaMin', 'alphaDecay',
        'alphaTarget', 'velocityDecay', 'iterations', 'link', 'manyBody', 'collide', 'x', 'y', 'radial'],
};
const cleanValue = value => {
    if (value === null || typeof value === 'boolean' || typeof value === 'string') return value;
    if (typeof value === 'number') return Number.isFinite(value) ? value : undefined;
    if (Array.isArray(value)) {
        const items = value.map(cleanValue);
        return items.every(item => item !== undefined) ? items : undefined;
    }
    if (value && Object.getPrototypeOf(value) === Object.prototype) {
        return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, cleanValue(item)])
            .filter(([, item]) => item !== undefined));
    }
    return undefined;
};
const finite = value => typeof value === 'number' && Number.isFinite(value);
const maxSize = value => Array.isArray(value) && value.length && value.every(finite)
    ? Math.max(...value) : finite(value) ? value : undefined;

/** Form fields are converted once to the installed layout 2 force options, with data-only options. */
export const normalizeLayoutSettings = value => {
    const input = typeof value === 'string' ? {type: value} : value || {};
    const type = analysisLayout(input).type;
    const source = {...input, ...input.options};
    const keys = [...optionKeys[type], 'width', 'height', ...(type === 'd3-force' ? [] : ['center', 'nodeSize', 'nodeSpacing'])];
    const options = Object.fromEntries(keys.filter(key => source[key] !== undefined)
        .map(key => [key, cleanValue(source[key])]).filter(([key, item]) => item !== undefined
            && key !== 'rows' && key !== 'cols'));
    // Rows and columns must be positive integers; empty form values must not override automatic sizing.
    for (const key of ['rows', 'cols']) {
        if (type === 'grid' && Number.isInteger(source[key]) && source[key] > 0) options[key] = source[key];
    }
    if (type === 'radial') {
        if (source.focusNode === '' || source.focusNode === null) delete options.focusNode;
        else if (source.focusNode !== undefined && input.options?.focusNode === undefined) {
            options.focusNode = hubbleRenderId('node', source.focusNode);
        }
        else if (source.startId !== undefined && source.startId !== null && source.focusNode === undefined) {
            options.focusNode = hubbleRenderId('node', source.startId);
        }
    }
    if (type === 'd3-force') {
        if (finite(source.linkDistance) && options.link !== false) {
            options.link = {...options.link, distance: source.linkDistance};
        }
        if (finite(source.nodeStrength) && options.manyBody !== false) {
            options.manyBody = {...options.manyBody, strength: -Math.abs(source.nodeStrength)};
        }
        if (finite(source.maxIteration) && source.iterations === undefined) options.iterations = source.maxIteration;
        if (source.preventOverlap === false || (source.preventOverlap === undefined && options.collide === false)) {
            options.collide = false;
        }
        else {
            const size = maxSize(source.nodeSize);
            const spacing = maxSize(source.nodeSpacing);
            if (source.preventOverlap === true || size !== undefined || spacing !== undefined) {
                // Official formatNodeSizeFn adds spacing to diameter, then collision takes half.
                options.collide = {...options.collide,
                    radius: ((size ?? 10) + (spacing ?? 0)) / 2};
            }
        }
    }
    return {type, options};
};

const labelConfig = style => ({
    position: style.labelPlacement || 'bottom',
    style: Object.fromEntries([['fill', style.labelFill], ['fontSize', style.labelFontSize],
        ['fontFamily', style.labelFontFamily], ['fontWeight', style.labelFontWeight]]
        .filter(([, value]) => value !== undefined)),
});
const selectedStyle = (style, keys) => Object.fromEntries(keys.filter(key => style[key] !== undefined)
    .map(key => [key, mutableCopy(style[key])]));
const nodeConfig = node => {
    const style = node.style || {};
    return {
        type: node.data.shape || 'circle',
        size: mutableCopy(style.size ?? 30),
        style: selectedStyle(style, ['fill', 'stroke', 'fillOpacity', 'strokeOpacity', 'lineWidth']),
        labelCfg: labelConfig(style),
        icon: {show: Boolean(node.data.icon) && style.icon !== false, _iconName: node.data.icon || '',
            fontSize: style.iconFontSize ?? 12, ...(style.iconFill === undefined ? {} : {fill: style.iconFill})},
    };
};
const edgeConfig = edge => ({
    type: 'line',
    style: {...selectedStyle(edge.style || {}, ['stroke', 'lineWidth', 'strokeOpacity', 'opacity', 'endArrow']),
        lineDash: edge.style?.lineDash?.length ? mutableCopy(edge.style.lineDash) : false},
    labelCfg: labelConfig(edge.style || {}),
});

/** Project only presentation fields; configuration drafts never carry or clone raw business records. */
export const getAnalysisStyles = data => {
    const nodes = new Map();
    const edges = new Map();
    for (const node of data.nodes) {
        const label = node.data.label ?? node.data.raw.label;
        if (!nodes.has(label)) nodes.set(label, nodeConfig(node));
    }
    for (const edge of data.edges) {
        const label = edge.data.label ?? edge.data.raw.label;
        if (!edges.has(label)) edges.set(label, edgeConfig(edge));
    }
    return {nodes: Object.fromEntries(nodes), edges: Object.fromEntries(edges)};
};
const mergedConfig = (current, next) => ({...current, ...next,
    style: {...current.style, ...next.style},
    labelCfg: {...current.labelCfg, ...next.labelCfg,
        style: {...current.labelCfg.style, ...next.labelCfg?.style}},
    ...(current.icon ? {icon: {...current.icon, ...next.icon}} : {}),
});

/** Changed elements only: controller updates preserve coordinates, pin state, visibility and raw identity. */
export const applyAnalysisStyles = (data, savedStyles = {}) => {
    const nodes = [];
    const edges = [];
    for (const node of data.nodes) {
        const label = node.data.label ?? node.data.raw.label;
        if (!Object.hasOwn(savedStyles.nodes || {}, label)) continue;
        const current = nodeConfig(node);
        const config = mergedConfig(current, savedStyles.nodes[label]);
        if (isEqual(current, config)) continue;
        const icon = config.icon.show === false ? undefined : normalizeIcon(config.icon._iconName);
        const style = {...node.style, ...nodeStyle({}, config, node.style?.labelText || '')};
        if (icon) {
            if (node.style?.icon === false || !node.data.icon) style.icon = true;
            style.iconSrc = hubbleIconSource(icon, style.iconFill);
            style.iconWidth = style.iconFontSize;
            style.iconHeight = style.iconFontSize;
        }
        else if (node.data.icon || node.style?.iconSrc) {
            style.icon = false;
            style.iconSrc = '';
        }
        const nextData = {...node.data, shape: nodeShape(config.type)};
        if (icon !== undefined || Object.hasOwn(node.data, 'icon')) nextData.icon = icon;
        const changed = {...node, data: nextData, style};
        if (!isEqual(node, changed)) nodes.push(changed);
    }
    for (const edge of data.edges) {
        const label = edge.data.label ?? edge.data.raw.label;
        if (!Object.hasOwn(savedStyles.edges || {}, label)) continue;
        const current = edgeConfig(edge);
        const config = mergedConfig(current, savedStyles.edges[label]);
        if (isEqual(current, config)) continue;
        const changed = {...edge, style: {...edge.style, ...edgeStyle({}, config, edge.style?.labelText || ''),
            ...selectedStyle(config.style, ['strokeOpacity', 'opacity'])}};
        if (!isEqual(edge, changed)) edges.push(changed);
    }
    return {nodes, edges};
};
