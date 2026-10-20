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
 * @file 图分析组件 外观设置弹窗
 */

import {useState} from 'react';
import {Modal, Form, Select, Divider, Switch} from 'antd';
import {useTranslation} from 'react-i18next';
import {cloneDeep} from 'lodash';
import {InputColorSelect} from '../../../../components/ColorSelect';
import IconSelect from '../../../../components/IconSelect';
import {resolveIconName} from '../../../../utils/iconNames';
import c from './index.module.scss';

const numericOptions = values => values.map(value => ({value, label: String(value)}));
const sizeOptions = numericOptions([20, 25, 30, 35, 40, 45, 50]);
const lineWidthOptions = numericOptions([0, 0.5, 1, 1.5, 2, 2.5, 3]);
const strokeWidthOptions = numericOptions([0, 1, 2, 3, 4, 5]);
const fontSizeOptions = numericOptions([12, 14, 16, 18, 20, 22, 24, 26]);
const opacityOptions = numericOptions([0, 0.2, 0.4, 0.6, 0.8, 1]);
const meanSize = size => Array.isArray(size) ? size.reduce((sum, value) => sum + value, 0) / size.length : size;
const ellipseSize = size => [size + 10, Math.max(0, size - 10)];

const StyleEditor = ({style, onOk, onCancel}) => {
    const {t} = useTranslation();
    // This component mounts only when the user opens the dialog. Live graph snapshots
    // must not overwrite a draft while the user is editing it.
    const [draft, setDraft] = useState(() => cloneDeep(style || {nodes: {}, edges: {}}));
    const [formType, setFormType] = useState('nodes');
    const [selected, setSelected] = useState({nodes: undefined, edges: undefined});
    const nodeForm = formType === 'nodes';
    const selectedLabel = selected[formType];
    const current = draft[formType]?.[selectedLabel] || {};
    const disabled = selectedLabel === undefined;
    const placeholder = t('style_config.select_placeholder');
    const formTypeOptions = [
        {value: 'nodes', label: t('style_config.vertex_appearance')},
        {value: 'edges', label: t('style_config.edge_appearance')},
    ];
    const shapesOptions = ['circle', 'diamond', 'triangle', 'star', 'ellipse'].map(value => ({
        value, label: t(`style_config.shape_${value}`),
    }));
    const lineTypeOptions = [
        {value: 'line', label: t('style_config.line_solid')},
        {value: 'dashed', label: t('style_config.line_dashed')},
    ];
    const changeStyle = changes => setDraft(previous => {
        const old = previous[formType]?.[selectedLabel];
        if (!old) return previous;
        const next = {...old, style: {...old.style}, labelCfg: {...old.labelCfg, style: {...old.labelCfg?.style}}};
        if (nodeForm) {
            next.icon = {...old.icon};
            if ('shape' in changes) {
                next.type = changes.shape;
                const size = meanSize(old.size) ?? 30;
                next.size = next.type === 'ellipse' ? ellipseSize(size) : size;
            }
            if ('size' in changes) {
                next.size = next.type === 'ellipse' ? ellipseSize(changes.size) : changes.size;
                next.icon.fontSize = changes.size * 0.6;
            }
            for (const [field, key] of [['strokeColor', 'stroke'], ['lineWidth', 'lineWidth'],
                ['fillColor', 'fill'], ['opacity', 'fillOpacity']]) {
                if (field in changes) next.style[key] = changes[field];
            }
            if ('iconShow' in changes) next.icon.show = changes.iconShow;
            if ('iconType' in changes) {
                next.icon._iconName = resolveIconName(changes.iconType, '');
                next.icon.show = Boolean(next.icon._iconName);
            }
            if ('iconColor' in changes) next.icon.fill = changes.iconColor;
        }
        else {
            if ('type' in changes) next.style.lineDash = changes.type === 'dashed'
                ? [Math.max(1, (next.style.lineWidth ?? 1) * 3)] : false;
            if ('width' in changes) next.style.lineWidth = changes.width;
            if ('color' in changes) next.style.stroke = changes.color;
            if ('arrow' in changes) next.style.endArrow = changes.arrow;
        }
        if ('labelSize' in changes) next.labelCfg.style.fontSize = changes.labelSize;
        if ('labelColor' in changes) next.labelCfg.style.fill = changes.labelColor;
        return {...previous, [formType]: {...previous[formType], [selectedLabel]: next}};
    });
    const values = nodeForm ? {
        shape: current.type, size: meanSize(current.size), strokeColor: current.style?.stroke,
        lineWidth: current.style?.lineWidth, fillColor: current.style?.fill, opacity: current.style?.fillOpacity,
        iconType: current.icon?._iconName, iconColor: current.icon?.fill, iconShow: current.icon?.show !== false,
        labelSize: current.labelCfg?.style?.fontSize, labelColor: current.labelCfg?.style?.fill,
    } : {
        type: current.style?.lineDash?.length ? 'dashed' : 'line', width: current.style?.lineWidth,
        color: current.style?.stroke, arrow: Boolean(current.style?.endArrow),
        labelSize: current.labelCfg?.style?.fontSize, labelColor: current.labelCfg?.style?.fill,
    };
    const select = options => <Select options={options} disabled={disabled} placeholder={placeholder} />;
    const color = () => <InputColorSelect disable={disabled} />;

    return (
        <Modal title={t('style_config.title')} open onOk={() => onOk(draft)} onCancel={onCancel}
            width={600} className={c.styleConfigModal}>
            <div className={c.title}>{t('style_config.basic_properties')}</div>
            <Form labelCol={{span: 6}} wrapperCol={{span: 18}}>
                <Form.Item label={t('style_config.appearance_type')}>
                    <Select aria-label={t('style_config.appearance_type')} options={formTypeOptions}
                        value={formType} onChange={setFormType} />
                </Form.Item>
                <Form.Item label={t('style_config.type')}>
                    <Select aria-label={t('style_config.type')} value={selectedLabel} placeholder={placeholder}
                        options={Object.keys(draft[formType] || {}).map(value => ({value, label: value}))}
                        onChange={value => setSelected(previous => ({...previous, [formType]: value}))} />
                </Form.Item>
            </Form>
            <Divider />
            <div className={c.title}>{t('style_config.appearance_config')}</div>
            <Form key={`${formType}:${selectedLabel}`} initialValues={values} onValuesChange={changeStyle}
                labelCol={{span: 6}} wrapperCol={{span: 18}}>
                {nodeForm ? <>
                    <Form.Item name='shape' label={t('style_config.shape')}>{select(shapesOptions)}</Form.Item>
                    <Form.Item name='size' label={t('style_config.size')}>{select(sizeOptions)}</Form.Item>
                    <Form.Item name='strokeColor' label={t('style_config.border_color')}>{color()}</Form.Item>
                    <Form.Item name='lineWidth' label={t('style_config.border_width')}>{select(strokeWidthOptions)}</Form.Item>
                    <Form.Item name='fillColor' label={t('style_config.fill_color')}>{color()}</Form.Item>
                    <Form.Item name='iconShow' label={t('style_config.icon')} valuePropName='checked'>
                        <Switch disabled={disabled} />
                    </Form.Item>
                    <Form.Item name='iconType' label={t('style_config.icon_style')}><IconSelect disabled={disabled} /></Form.Item>
                    <Form.Item name='iconColor' label={t('style_config.icon_color')}>{color()}</Form.Item>
                    <Form.Item name='opacity' label={t('style_config.node_opacity')}>{select(opacityOptions)}</Form.Item>
                </> : <>
                    <Form.Item name='type' label={t('style_config.type')}>{select(lineTypeOptions)}</Form.Item>
                    <Form.Item name='width' label={t('style_config.edge_width')}>{select(lineWidthOptions)}</Form.Item>
                    <Form.Item name='color' label={t('style_config.edge_color')}>{color()}</Form.Item>
                    <Form.Item name='arrow' label={t('style_config.arrow')} valuePropName='checked'>
                        <Switch disabled={disabled} />
                    </Form.Item>
                </>}
                <Form.Item name='labelSize' label={t('style_config.label_size')}>{select(fontSizeOptions)}</Form.Item>
                <Form.Item name='labelColor' label={t('style_config.label_color')}>{color()}</Form.Item>
            </Form>
        </Modal>
    );
};

const ConfigModal = ({visible, ...props}) => visible ? <StyleEditor {...props} /> : null;
export default ConfigModal;
