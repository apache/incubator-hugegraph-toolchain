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

/**
 * @file  设置表单
 */

import {useContext, useState, useId, useRef} from 'react';
import {Form, Select, Switch, Cascader, Slider} from 'antd';
import {useTranslation} from 'react-i18next';
import {GraphContext} from '../Context';
import RenderModeSwitcher from '../GraphRenderModeSwitcher';
import {GRAPH_RENDER_MODE} from '../../../utils/constants';
import c from './index.module.scss';

const createFields = (elements, metadata = [], selectedFields) => {
    const groups = new Map();
    for (const element of elements) {
        const raw = element.data.raw;
        if (!groups.has(raw.label)) groups.set(raw.label, new Set());
        Object.keys(raw.properties || {}).forEach(key => groups.get(raw.label).add(key));
    }
    const meta = new Map(metadata.map(entry => [entry.name, entry]));
    return {
        options: [...groups].map(([label, keys]) => ({value: label, label,
            children: ['~id', ...keys].map(key => ({value: key, label: key}))})),
        selected: [...groups.keys()].flatMap(label => selectedFields?.get(label)
            ?? (meta.get(label)?.style?.display_fields ?? ['~id']).map(field => [label, field])),
    };
};
const labelText = (element, fields, kind) => {
    const raw = element.data.raw;
    const keys = new Set();
    for (const path of fields || []) {
        if (path[0] !== raw.label) continue;
        if (path.length === 1) ['~id', ...Object.keys(raw.properties || {})].forEach(key => keys.add(key));
        else keys.add(path[1]);
    }
    return [...keys].map(key => key === '~id' ? kind === 'node' ? raw.id : raw.label : raw.properties?.[key])
        .filter(value => value !== undefined && value !== null).map(String).join('\n');
};
const RenderSettings = ({renderMode, onRenderModeChange}) => {
    const {t} = useTranslation();
    return (
        <details>
            <summary>{t('analysis.canvas.render_mode')}</summary>
            <RenderModeSwitcher buttonEnable value={renderMode} onClick={onRenderModeChange} />
        </details>
    );
};

const OpenSettings = ({data, metaData, onChange, selectedFields, renderMode, onRenderModeChange}) => {
    const {t} = useTranslation();
    const {controller, snapshot, run, showLegend = true, setShowLegend,
        showGrid = true, setShowGrid, showMinimap = true, setShowMinimap} = useContext(GraphContext);
    const [form] = Form.useForm();
    const instanceId = useId();
    const [draft] = useState(() => {
        const nodes = createFields(data.nodes, metaData?.vertexMeta, selectedFields.current.node);
        const edges = createFields(data.edges, metaData?.edgeMeta, selectedFields.current.edge);
        const firstNode = data.nodes[0]?.style;
        const firstEdge = data.edges[0]?.style;
        return {nodes, edges, values: {
            enableNodeLabel: !data.nodes.length || data.nodes.some(node => Boolean(node.style?.labelText)),
            enableEdgeLabel: !data.edges.length || data.edges.some(edge => Boolean(edge.style?.labelText)),
            nodeLabelPropertyName: nodes.selected, edgeLabelPropertyName: edges.selected,
            nodeLabelPos: firstNode?.labelPlacement || 'bottom',
            nodeLabelSize: firstNode?.labelFontSize ?? 12, edgeLabelSize: firstEdge?.labelFontSize ?? 12,
        }};
    });
    const ready = Boolean(controller && snapshot?.status === 'ready');
    const changeLabels = (changed, values) => {
        const key = Object.keys(changed)[0];
        const kind = key.includes('Node') || key.startsWith('node') ? 'node' : 'edge';
        const elements = kind === 'node' ? data.nodes : data.edges;
        if (key.endsWith('PropertyName')) {
            const options = kind === 'node' ? draft.nodes.options : draft.edges.options;
            for (const {value: label} of options) {
                selectedFields.current[kind].set(label, (values[key] || [])
                    .filter(path => path[0] === label).map(path => [...path]));
            }
        }
        const style = element => {
            if (key.endsWith('Size')) return {labelFontSize: changed[key]};
            if (key === 'nodeLabelPos') return {labelPlacement: changed[key]};
            const enabled = kind === 'node' ? values.enableNodeLabel : values.enableEdgeLabel;
            const fields = kind === 'node' ? values.nodeLabelPropertyName : values.edgeLabelPropertyName;
            const text = enabled ? labelText(element, fields, kind) : '';
            return {labelText: text, ...(kind === 'edge' ? {labelBackground: Boolean(text),
                labelBackgroundFill: '#fff', labelBackgroundPadding: text ? [2, 2] : 0,
                labelBackgroundRadius: 2} : {})};
        };
        // Emit the last user intent even if an earlier queued patch has not reached props yet.
        const patches = elements.map(element => ({id: element.id, kind, style: style(element)}));
        if (patches.length) run(() => onChange?.(patches));
    };
    const fieldLabel = key => t(`analysis.canvas.setting_panel.${key}`);
    return (
        <div className={c.canvasSetting}>
            <Form name={instanceId} form={form} initialValues={draft.values} onValuesChange={changeLabels} colon={false} disabled={!ready}>
                <div className={c.canvasSettingTitle}>{fieldLabel('title')}</div>
                <Form.Item name="enableNodeLabel" label={fieldLabel('node_label')} valuePropName="checked">
                    <Switch />
                </Form.Item>
                <Form.Item name="nodeLabelPropertyName" label={fieldLabel('node_label_property')} labelCol={{span: 24}}>
                    <Cascader options={draft.nodes.options.map(group => ({...group,
                        children: group.children.map(child => child.value === '~id'
                            ? {...child, label: fieldLabel('node_id')} : child)}))}
                        multiple maxTagCount={5} showSearch style={{width: '100%'}} />
                </Form.Item>
                <Form.Item name="nodeLabelPos" label={fieldLabel('node_label_position')} labelCol={{span: 24}}>
                    <Select options={['center', 'bottom'].map(value => ({value, label: fieldLabel(`node_${value}`)}))} />
                </Form.Item>
                <Form.Item name="nodeLabelSize" label={fieldLabel('node_label_size')} labelCol={{span: 24}}>
                    <Slider ariaLabelForHandle={fieldLabel('node_label_size')} min={6} max={48} />
                </Form.Item>
                <Form.Item name="enableEdgeLabel" label={fieldLabel('edge_label')} valuePropName="checked">
                    <Switch />
                </Form.Item>
                <Form.Item name="edgeLabelPropertyName" label={fieldLabel('edge_label_property')} labelCol={{span: 24}}>
                    <Cascader options={draft.edges.options.map(group => ({...group,
                        children: group.children.map(child => child.value === '~id'
                            ? {...child, label: fieldLabel('edge_type')} : child)}))}
                        multiple maxTagCount={5} showSearch style={{width: '100%'}} />
                </Form.Item>
                <Form.Item name="edgeLabelSize" label={fieldLabel('edge_label_size')} labelCol={{span: 24}}>
                    <Slider ariaLabelForHandle={fieldLabel('edge_label_size')} min={6} max={48} />
                </Form.Item>
            </Form>
            <div className={c.canvasSettingTitle}>{fieldLabel('components')}</div>
            <Form colon={false} disabled={!ready}>
                <Form.Item label={fieldLabel('enable_minimap')}>
                    <Switch aria-label={fieldLabel('enable_minimap')} checked={showMinimap} onChange={value => {
                        run(async () => {
                            const result = await controller.setMinimap(value);
                            if (result?.status === 'applied') setShowMinimap?.(value);
                        });
                    }} />
                </Form.Item>
                <Form.Item label={fieldLabel('enable_grid')}>
                    <Switch aria-label={fieldLabel('enable_grid')} checked={showGrid} onChange={setShowGrid} />
                </Form.Item>
                <Form.Item label={fieldLabel('enable_legend')}>
                    <Switch aria-label={fieldLabel('enable_legend')} checked={showLegend} onChange={setShowLegend} />
                </Form.Item>
            </Form>
            {onRenderModeChange && <RenderSettings renderMode={renderMode} onRenderModeChange={onRenderModeChange} />}
        </div>
    );
};

const SettingConfigPanel = ({open, selectedFields, renderMode = GRAPH_RENDER_MODE.CANVAS2D,
    onRenderModeChange, ...props}) => {
    const {t} = useTranslation();
    // Keep only the user's field choices through panel closure. Options, fonts
    // and positions are read from the current graph when the panel reopens.
    // The owner's dataset key resets these choices for a new query result.
    const localFields = useRef({node: new Map(), edge: new Map()});
    if (!open) return null;
    if (renderMode === GRAPH_RENDER_MODE.CANVAS3D) {
        return <div className={c.canvasSetting}>
            <div className={c.canvasSettingTitle}>{t('analysis.canvas.setting')}</div>
            <RenderSettings renderMode={renderMode} onRenderModeChange={onRenderModeChange} />
        </div>;
    }
    return <OpenSettings {...props} selectedFields={selectedFields ?? localFields}
        renderMode={renderMode} onRenderModeChange={onRenderModeChange} />;
};
export default SettingConfigPanel;
