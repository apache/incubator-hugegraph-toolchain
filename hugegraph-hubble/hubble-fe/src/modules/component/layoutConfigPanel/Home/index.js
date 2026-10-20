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

import {useState, useMemo, useEffect, useContext, useImperativeHandle, useRef} from 'react';
import {Form, Select} from 'antd';
import {useTranslation} from 'react-i18next';
import debounce from 'lodash/debounce';
import {GraphContext} from '../../Context';
import ForceLayoutForm from '../Force';
import CircularLayoutForm from '../Circular';
import ConcentricLayoutForm from '../Concentric';
import DagreLayoutForm from '../Dagre';
import GridLayoutForm from '../Grid';
import RadialLayoutForm from '../Radial';
import forceIcon from '../../../../assets/layout_force.svg';
import circularIcon from '../../../../assets/layout_circular.svg';
import concentricIcon from '../../../../assets/layout_concentric.svg';
import dagreIcon from '../../../../assets/layout_dagre.svg';
import gridIcon from '../../../../assets/layout_grid.svg';
import radialIcon from '../../../../assets/layout_radial.svg';
import {normalizeLayoutSettings} from '../../../../utils/analysisGraphPresentation';
import {adaptiveLayoutDefaults} from '../../../../visualization/runtime/layoutDefaults';
import c from './index.module.scss';

const forms = {'d3-force': ForceLayoutForm, circular: CircularLayoutForm, concentric: ConcentricLayoutForm,
    dagre: DagreLayoutForm, grid: GridLayoutForm, radial: RadialLayoutForm};
const icons = {'d3-force': forceIcon, circular: circularIcon, concentric: concentricIcon,
    dagre: dagreIcon, grid: gridIcon, radial: radialIcon};
const adaptive = type => type === 'd3-force' || type === 'radial';
const defaults = (type, data, options = {}) => {
    if (adaptive(type)) {
        const automatic = adaptiveLayoutDefaults({type, options}, data.nodes);
        const effective = {...automatic, ...normalizeLayoutSettings({type, options}).options};
        if (type === 'radial') return effective;
        // The force form's size describes the collision diameter, not the rendered node size.
        const collision = effective.collide || automatic.collide;
        // A supplied partial native collision object uses D3's default radius of 5.
        return {nodeSize: options.nodeSize ?? (collision.radius ?? 5) * 2 - (options.nodeSpacing ?? 0),
            nodeSpacing: options.nodeSpacing ?? 0,
            linkDistance: options.linkDistance ?? effective.link?.distance,
            nodeStrength: options.nodeStrength ?? Math.abs(effective.manyBody?.strength ?? 0),
            preventOverlap: options.preventOverlap ?? effective.collide !== false};
    }
    const count = data.nodes.length;
    return ({
    circular: {startRadius: Math.max(120, count * 100 / (2 * Math.PI)),
        endRadius: Math.max(120, count * 100 / (2 * Math.PI)), divisions: 1, angleRatio: 1,
        clockwise: true, ordering: undefined},
    concentric: {nodeSize: 30, preventOverlap: true, startAngle: 0, nodeSpacing: 24,
        equidistant: false, clockwise: true},
    dagre: {rankdir: 'TB', align: undefined, ranksep: 80, nodesep: 40},
    grid: {},
    }[type] || {});
};
const OpenLayout = ({layout, data, onChange, ref}) => {
    const {t} = useTranslation();
    const {controller, snapshot, run} = useContext(GraphContext);
    const [type, setType] = useState(layout?.type || 'd3-force');
    const draft = useRef({});
    const currentLayout = useRef(layout);
    useEffect(() => {currentLayout.current = layout;}, [layout]);
    const [initial] = useState(() => ({...defaults(type, data, layout?.options), ...layout?.options}));
    const scheduled = useMemo(() => debounce(value => {
        draft.current = {};
        return run(() => onChange({type: value.type, options: {
            ...(value.type === currentLayout.current?.type ? currentLayout.current.options : {}), ...value.options,
        }}));
    }, 100), [onChange, run]);
    useEffect(() => () => scheduled.cancel(), [scheduled]);
    useImperativeHandle(ref, () => ({flush: () => scheduled.flush(), cancel: () => scheduled.cancel()}), [scheduled]);
    const ready = Boolean(controller && snapshot?.status === 'ready');
    const names = {'d3-force': 'force', circular: 'circular', concentric: 'concentric', dagre: 'dagre',
        grid: 'grid', radial: 'radial'};
    const options = Object.keys(forms).map(value => ({value, label: (
        <span className={c.assetsItem}>
            <span className={c.assetsTitle} aria-hidden="true">
                <img className={c.changeLayoutTypeSelectIcon} src={icons[value]} alt="" />
            </span>
            <span className={c.changeLayoutTypeSelectName}>{t(`analysis.canvas.layout_panel.${names[value]}`)}</span>
        </span>
    )}));
    if (!forms[type]) options.push({value: type, label: type});
    const LayoutForm = forms[type];
    const changedType = next => {
        scheduled.cancel();
        setType(next);
        const options = adaptive(next) ? {} : defaults(next, data);
        draft.current = {};
        run(() => onChange({type: next, options}));
    };
    const changedOptions = (changed, values) => {
        // Radial's selector carries rendering IDs, converted from the raw IDs at its boundary.
        const options = adaptive(type) ? {...changed} : {...values};
        // Collision controls are one diameter/spacing pair. Resolve their displayed siblings
        // only when that group is explicitly edited, never for an unrelated link/strength edit.
        if (type === 'd3-force' && ['nodeSize', 'nodeSpacing', 'preventOverlap'].some(key => key in changed)) {
            Object.assign(options, {nodeSize: values.nodeSize, nodeSpacing: values.nodeSpacing,
                preventOverlap: values.preventOverlap});
        }
        if (type === 'dagre' && options.align === 'center') options.align = undefined;
        if (type === 'circular' && options.ordering === 'data') options.ordering = undefined;
        draft.current = {...draft.current, ...options};
        scheduled({type, options: draft.current});
    };
    return (
        <div className={c.changeLayout}>
            <div className={c.changeLayoutTitle}>{t('analysis.canvas.layout_panel.title')}</div>
            <Form disabled={!ready} colon={false} className={c.layoutTypeForm}>
                <Form.Item label={t('analysis.canvas.layout_panel.layout_type')} labelCol={{span: 24}}>
                    <Select value={type} options={options} onChange={changedType}
                        aria-label={t('analysis.canvas.layout_panel.layout_type')} />
                </Form.Item>
            </Form>
            {LayoutForm && <LayoutForm key={type} initialValues={type === layout?.type ? initial
                : defaults(type, data)} handleFormChange={changedOptions} data={data} disabled={!ready} />}
        </div>
    );
};
const LayoutConfigPanel = ({open, ref, ...props}) => {
    return open ? <OpenLayout key={props.layout?.type} ref={ref} {...props} /> : null;
};
export default LayoutConfigPanel;
