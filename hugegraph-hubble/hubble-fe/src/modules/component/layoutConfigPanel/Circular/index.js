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
 * @file 切换环形布局组件 circular
 */

import React, {useMemo} from 'react';
import {Form, Switch, Segmented} from 'antd';
import {useTranslation} from 'react-i18next';
import SliderComponent from '../../../../components/SlideComponent';
import _ from 'lodash';
import c from './index.module.scss';

const CircularLayoutForm = props => {
    const {handleFormChange, initialValues} = props;
    const {t} = useTranslation();
    const {useForm} = Form;
    const [circularLayoutForm] = useForm();
    const orderingOptions = useMemo(
        () => [
            {value: null, label: t('analysis.canvas.layout_panel.ordering_data')},
            {value: 'topology', label: t('analysis.canvas.layout_panel.ordering_topology')},
            {value: 'degree', label: t('analysis.canvas.layout_panel.ordering_degree')},
        ],
        [t]
    );

    return (
        <Form
            form={circularLayoutForm}
            onValuesChange={_.debounce(handleFormChange, 100)}
            labelCol={{span: 24}}
            initialValues={initialValues}
            className={c.circularLayoutForm}
        >
            <Form.Item
                name='startRadius'
                label={t('analysis.canvas.layout_panel.start_radius')}
                tooltip={t('analysis.canvas.layout_panel.start_radius_tooltip')}
            >
                <SliderComponent min={30} max={1000} />
            </Form.Item>
            <Form.Item
                name='endRadius'
                label={t('analysis.canvas.layout_panel.end_radius')}
                tooltip={t('analysis.canvas.layout_panel.end_radius_tooltip')}
            >
                <SliderComponent min={30} max={1000} />
            </Form.Item>
            <Form.Item
                name='divisions'
                label={t('analysis.canvas.layout_panel.divisions')}
                tooltip={t('analysis.canvas.layout_panel.divisions_tooltip')}
            >
                <SliderComponent min={1} max={100} />
            </Form.Item>
            <Form.Item
                name='angleRatio'
                label={t('analysis.canvas.layout_panel.angle_ratio')}
                tooltip={t('analysis.canvas.layout_panel.angle_ratio_tooltip')}
            >
                <SliderComponent min={1} max={100} />
            </Form.Item>
            <Form.Item
                name='ordering'
                label={t('analysis.canvas.layout_panel.ordering')}
                tooltip={t('analysis.canvas.layout_panel.ordering_tooltip')}
            >
                <Segmented options={orderingOptions} />
            </Form.Item>
            <Form.Item
                name='clockwise'
                label={t('analysis.canvas.layout_panel.clockwise')}
                valuePropName='checked'
                labelCol={{span: 20}}
                labelAlign='left'
            >
                <Switch />
            </Form.Item>
        </Form>
    );
};

export default CircularLayoutForm;
