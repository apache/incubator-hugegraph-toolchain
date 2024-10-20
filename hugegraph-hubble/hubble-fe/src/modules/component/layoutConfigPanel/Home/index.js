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
 * @author
 */

import React, {useCallback, useState, useEffect} from 'react';
import {Form, Select, Switch} from 'antd';
import ForceLayoutForm from '../Force';
import CircularLayoutForm from '../Circular';
import ConcentricLayoutForm from '../Concentric';
import DagreLayoutForm from '../Dagre';
import GridLayoutForm from '../Grid';
import RadialLayoutForm from '../Radial';
import ForceLayoutIcon from '../../../../assets/layout_force.svg';
import ConcentricLayoutIcon from '../../../../assets/layout_concentric.svg';
import DagreLayoutIcon from '../../../../assets/layout_dagre.svg';
import CircularLayoutIcon from '../../../../assets/layout_circular.svg';
import GridLayoutIcon from '../../../../assets/layout_grid.svg';
import RadialLayoutIcon from '../../../../assets/layout_radial.svg';
import {SUPPORTED_LAYOUT_TYPE} from '../../../../utils/constants';
import classnames from 'classnames';
import _ from 'lodash';
import c from './index.module.scss';

const layoutTypeLabel = (icon, name) => {
    return (
        <div className={c.assetsItem}>
            <div className={c.assetsTitle}>
                <img className={c.changeLayoutTypeSelectIcon} src={icon}></img>
            </div>
            <span style={{marginLeft: '18px'}} className={c.changeLayoutTypeSelectName}>{name}</span>
        </div>
    );
};

const {FORCE, CIRCULAR, CONCENTRIC, DAGRE, CUSTOMGRID, RADIAL} = SUPPORTED_LAYOUT_TYPE;
const SUPPORTED_LAYOUT_TYPE_ARR = [FORCE, CIRCULAR, CONCENTRIC, DAGRE, CUSTOMGRID, RADIAL];

const layoutTypeOptions = [
    {value: FORCE, label: layoutTypeLabel(ForceLayoutIcon, '力导布局')},
    {value: CIRCULAR, label: layoutTypeLabel(CircularLayoutIcon, '环形布局')},
    {value: CONCENTRIC, label: layoutTypeLabel(ConcentricLayoutIcon, '同心圆布局')},
    {value: DAGRE, label: layoutTypeLabel(DagreLayoutIcon, '层次布局')},
    {value: CUSTOMGRID, label: layoutTypeLabel(GridLayoutIcon, '网格布局')},
    {value: RADIAL, label: layoutTypeLabel(RadialLayoutIcon, '径向布局')},
];

const initialLayoutValue = {
    [FORCE]: {
        nodeSize: 80,
        linkDistance: 100,
        nodeStrength: 300,
        preventOverlap: true,
        nodeSpacing: 15,
    },
    [CIRCULAR]: {
        startRadius: 200,
        endRadius: 200,
        divisions: 1,
        angleRatio: 1,
        clockwise: true,
        ordering: null,
    },
    [CONCENTRIC]: {
        nodeSize: 30,
        preventOverlap: true,
        startAngle: 5,
        nodeSpacing: 70,
        equidistant: false,
        clockwise: false,
    },
    [DAGRE]: {
        rankdir: 'TB',
        align: 'DL',
        ranksep: 50,
        nodesep: 50,
    },
    [RADIAL]: {
        unitRadius: 100,
        linkDistance: 50,
        nodeSize: 80,
        focusNode: '',
        nodeSpacing: 80,
        preventOverlap: true,
        strictRadial: true,
    },
};

const defaultLayoutValue = {
    nodeSize: 80,
    linkDistance: 100,
    nodeStrength: 300,
    preventOverlap: true,
    nodeSpacing: 15,
};

const LayoutConfigPanel = props => {
    const {
        layout,
        data,
        onChange,
        open,
    } = props;

    const {useForm} = Form;
    const [basicInfoForm] = useForm();
    const [defaultLayout, setDefaultLayout] = useState();
    const [layoutType, setLayoutType] = useState();
    const [layoutFormInfo, setLayoutFormInfo] = useState({type: FORCE, ...defaultLayoutValue});
    const [initialInfo, setInitialInfo] = useState(initialLayoutValue);

    const changeLayoutClassName = classnames(
        c.changeLayout,
        {[c.changeLayoutHidden]: !open}
    );

    const updateProps = (startRadius, endRadius) => {
        const props = {
            startRadius: startRadius,
            endRadius: endRadius,
            divisions: 1,
            angleRatio: 1,
            clockwise: true,
            ordering: null,
        };
        let newInitialProps = {...initialLayoutValue, 'circular': props};
        return newInitialProps;
    };

    useEffect(
        () => {
            const {edges, nodes} = data;
            const verticesNum = _.size(nodes);
            let newInitialProps;
            if (verticesNum <= 20) {
                newInitialProps = updateProps(200, 200);
            }
            else if (verticesNum > 20 && verticesNum <= 50) {
                newInitialProps = updateProps(402, 402);
            }
            else if (verticesNum > 50 && verticesNum <= 100) {
                newInitialProps = updateProps(606, 622);
            }
            else {
                newInitialProps = updateProps(1000, 1000);
            }
            setInitialInfo(newInitialProps);
        },
        [data]
    );

    useEffect(
        () => {
            const {type} = layout || {};
            const showLayoutType = SUPPORTED_LAYOUT_TYPE_ARR.includes(type) ? type : FORCE;
            layout && setLayoutType(showLayoutType);
            basicInfoForm.setFieldValue('type', showLayoutType);
            layout && setDefaultLayout(layout);
        },
        [basicInfoForm, layout]
    );

    const handleLayoutTypeChange = useCallback(
        value => {
            const enableLayout = basicInfoForm.getFieldValue('enableLayout');
            setLayoutType(value);
            setLayoutFormInfo({...initialInfo[value], type: value});
            if (enableLayout) {
                onChange({type: value, ...initialInfo[value]});
            }
        },
        [basicInfoForm, initialInfo, onChange]
    );

    const onFormValuesChange = useCallback(
        (changedValues, allValues) => {
            const {enableLayout, type} = basicInfoForm.getFieldsValue();
            let layoutInfo;
            if (type === CUSTOMGRID) {
                layoutInfo = {type: type, ...changedValues};
            }
            else {
                layoutInfo = {type: type, ...allValues};
            }
            setLayoutFormInfo(layoutInfo);
            if (enableLayout) {
                onChange(layoutInfo);
            }
        },
        [basicInfoForm, onChange]
    );

    const handleEnableLayout = useCallback(
        value => {
            if (!value) {
                onChange(defaultLayout, true);
            }
            else {
                onChange(layoutFormInfo);
            }
        },
        [defaultLayout, layoutFormInfo, onChange]
    );

    const switchLayout = useCallback(
        () => {
            const initialValues = initialInfo[layoutType] || defaultLayoutValue;
            switch (layoutType) {
                case FORCE:
                    return (
                        <ForceLayoutForm
                            handleFormChange={onFormValuesChange}
                            initialValues={initialValues}
                        />
                    );
                case CIRCULAR:
                    return (
                        <CircularLayoutForm
                            handleFormChange={onFormValuesChange}
                            initialValues={initialValues}
                        />
                    );
                case CONCENTRIC:
                    return (
                        <ConcentricLayoutForm
                            handleFormChange={onFormValuesChange}
                            initialValues={initialValues}
                        />
                    );
                case DAGRE:
                    return (
                        <DagreLayoutForm
                            handleFormChange={onFormValuesChange}
                            initialValues={initialValues}
                        />
                    );
                case CUSTOMGRID:
                    return (
                        <GridLayoutForm
                            handleFormChange={onFormValuesChange}
                            initialValues={initialValues}
                        />
                    );
                case RADIAL:
                    return (
                        <RadialLayoutForm
                            handleFormChange={onFormValuesChange}
                            initialValues={initialValues}
                        />
                    );
            }
        },
        [initialInfo, layoutType, onFormValuesChange]
    );

    return (
        <div className={changeLayoutClassName}>
            <div className={c.changeLayoutTitle}>布局</div>
            <Form
                form={basicInfoForm}
                colon={false}
            >
                <Form.Item
                    name='enableLayout'
                    label='启用布局'
                    valuePropName='checked'
                    labelCol={{span: 20}}
                    labelAlign='left'
                    tooltip='是否启用布局，关闭后布局将切换为默认布局'
                >
                    <Switch onChange={handleEnableLayout} />
                </Form.Item>
                <Form.Item
                    name='type'
                    label='布局方式'
                    labelCol={{span: 24}}
                    className={c.layoutTypeForm}
                >
                    <Select
                        className={c.layoutSelect}
                        options={layoutTypeOptions}
                        onChange={handleLayoutTypeChange}
                    />
                </Form.Item>
            </Form>
            {switchLayout()}
        </div>
    );
};

export default LayoutConfigPanel;
