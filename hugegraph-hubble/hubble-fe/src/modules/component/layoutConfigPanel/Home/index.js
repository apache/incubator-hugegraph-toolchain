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
 * @file LayoutConfigPanel
 */

import {useState, useMemo, useEffect, useContext} from 'react';
import {Form, Select} from 'antd';
import {useTranslation} from 'react-i18next';
import debounce from 'lodash/debounce';
import {GraphContext} from '../../Context';
import {normalizeLayoutSettings} from '../../../../utils/analysisGraphPresentation';
import ForceLayoutForm from '../Force';
import CircularLayoutForm from '../Circular';
import ConcentricLayoutForm from '../Concentric';
import DagreLayoutForm from '../Dagre';
import GridLayoutForm from '../Grid';
import RadialLayoutForm from '../Radial';
import c from './index.module.scss';

const forms = {'d3-force': ForceLayoutForm, circular: CircularLayoutForm, concentric: ConcentricLayoutForm,
    dagre: DagreLayoutForm, grid: GridLayoutForm, radial: RadialLayoutForm};
const defaults = (type, count) => ({
    'd3-force': {nodeSize: 30, linkDistance: 100, nodeStrength: 300, preventOverlap: true, nodeSpacing: 24},
    circular: {startRadius: Math.max(120, count * 100 / (2 * Math.PI)),
        endRadius: Math.max(120, count * 100 / (2 * Math.PI)), divisions: 1, angleRatio: 1,
        clockwise: true, ordering: undefined},
    concentric: {nodeSize: 30, preventOverlap: true, startAngle: 0, nodeSpacing: 24,
        equidistant: false, clockwise: true},
    dagre: {rankdir: 'TB', align: undefined, ranksep: 80, nodesep: 40},
    grid: {},
    radial: {unitRadius: 100, linkDistance: 100, nodeSize: 30, nodeSpacing: 24,
        preventOverlap: true, strictRadial: true},
}[type] || {});
const OpenLayout = ({layout, data, onChange}) => {
    const {t} = useTranslation();
    const {controller, snapshot, run} = useContext(GraphContext);
    const [type, setType] = useState(layout?.type || 'd3-force');
    const [initial] = useState(() => {
        const options = layout?.options || {};
        return {...defaults(type, data.nodes.length), ...options,
            ...(type === 'd3-force' ? {
                linkDistance: options.link?.distance ?? 100,
                nodeStrength: Math.abs(options.manyBody?.strength ?? -300),
                preventOverlap: options.collide !== false,
            } : {})};
    });
    const scheduled = useMemo(() => debounce(value => run(() => onChange(value)), 100), [onChange, run]);
    useEffect(() => () => scheduled.cancel(), [scheduled]);
    const ready = Boolean(controller && snapshot?.status === 'ready');
    const names = {'d3-force': 'force', circular: 'circular', concentric: 'concentric', dagre: 'dagre',
        grid: 'grid', radial: 'radial'};
    const options = Object.keys(forms).map(value => ({value, label: t(`analysis.canvas.layout_panel.${names[value]}`)}));
    if (!forms[type]) options.push({value: type, label: type});
    const LayoutForm = forms[type];
    const changedType = next => {
        scheduled.cancel();
        setType(next);
        run(() => onChange(normalizeLayoutSettings({type: next, ...defaults(next, data.nodes.length)})));
    };
    const changedOptions = (_changed, values) => {
        // Radial's selector carries rendering IDs, converted from the raw IDs at its boundary.
        const options = {...values};
        if (type === 'dagre' && options.align === 'center') options.align = undefined;
        if (type === 'circular' && options.ordering === 'data') options.ordering = undefined;
        scheduled(normalizeLayoutSettings({type, ...(type === layout?.type ? layout.options : {}), ...options,
            ...(type === 'radial' ? {options: {focusNode: options.focusNode}} : {})}));
    };
    return (
        <div className={c.changeLayout}>
            <div className={c.changeLayoutTitle}>{t('analysis.canvas.layout_panel.title')}</div>
            <Form disabled={!ready} colon={false}>
                <Form.Item label={t('analysis.canvas.layout_panel.layout_type')} labelCol={{span: 24}}>
                    <Select value={type} options={options} onChange={changedType}
                        aria-label={t('analysis.canvas.layout_panel.layout_type')} />
                </Form.Item>
            </Form>
            {LayoutForm && <LayoutForm key={type} initialValues={type === layout?.type ? initial
                : defaults(type, data.nodes.length)} handleFormChange={changedOptions} data={data} disabled={!ready} />}
        </div>
    );
};
const LayoutConfigPanel = ({open, ...props}) => open ? <OpenLayout key={props.layout?.type} {...props} /> : null;
export default LayoutConfigPanel;
