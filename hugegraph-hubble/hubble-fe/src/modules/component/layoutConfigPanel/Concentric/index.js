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
 * @file 切换层次布局组件 Concentric
 */

import React from 'react';
import {Form, Switch} from 'antd';
import {useTranslation} from 'react-i18next';
import SliderComponent from '../../../../components/SlideComponent';
import _ from 'lodash';

const ConcentricLayoutForm = props => {
    const {handleFormChange, initialValues} = props;
    const {t} = useTranslation();
    const {useForm} = Form;
    const [concentricLayoutForm] = useForm();

    return (
        <Form
            form={concentricLayoutForm}
            onValuesChange={_.debounce(handleFormChange, 100)}
            labelCol={{span: 24}}
            initialValues={initialValues}
        >
            <Form.Item
                name='nodeSize'
                label={t('analysis.canvas.layout_panel.node_size')}
                tooltip={t('analysis.canvas.layout_panel.node_size_tooltip')}
            >
                <SliderComponent min={1} max={100} />
            </Form.Item>
            <Form.Item
                name='sweep'
                label={t('analysis.canvas.layout_panel.sweep')}
                tooltip={t('analysis.canvas.layout_panel.sweep_tooltip')}
            >
                <SliderComponent />
            </Form.Item>
            <Form.Item
                name='startAngle'
                label={t('analysis.canvas.layout_panel.start_angle')}
                tooltip={t('analysis.canvas.layout_panel.start_angle_tooltip')}
            >
                <SliderComponent />
            </Form.Item>
            <Form.Item
                name='preventOverlap'
                label={t('analysis.canvas.layout_panel.prevent_overlap')}
                valuePropName="checked"
                labelCol={{span: 20}}
                labelAlign='left'
            >
                <Switch />
            </Form.Item>
            <Form.Item
                name='nodeSpacing'
                label={t('analysis.canvas.layout_panel.node_spacing')}
                tooltip={t('analysis.canvas.layout_panel.node_spacing_tooltip')}
            >
                <SliderComponent min={0} max={1000} />
            </Form.Item>
            <Form.Item
                name='equidistant'
                label={t('analysis.canvas.layout_panel.equidistant')}
                valuePropName="checked"
                labelCol={{span: 20}}
                labelAlign='left'
            >
                <Switch />
            </Form.Item>
            <Form.Item
                name='clockwise'
                label={t('analysis.canvas.layout_panel.clockwise')}
                valuePropName="checked"
                labelCol={{span: 20}}
                labelAlign='left'
            >
                <Switch />
            </Form.Item>
        </Form>
    );
};

export default ConcentricLayoutForm;
