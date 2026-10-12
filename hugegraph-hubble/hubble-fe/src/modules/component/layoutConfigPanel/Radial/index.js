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
 * @file  径向布局
 */

import React from 'react';
import {Form, InputNumber, Input, Switch} from 'antd';
import {useTranslation} from 'react-i18next';
import _ from 'lodash';

const RadialForm = props => {
    const {handleFormChange, initialValues} = props;
    const {t} = useTranslation();
    const {useForm} = Form;
    const [form] = useForm();

    return (
        <Form
            form={form}
            onValuesChange={_.debounce(handleFormChange, 100)}
            initialValues={initialValues}
            labelCol={{span: 24}}
        >
            <Form.Item
                name='unitRadius'
                label={t('analysis.canvas.layout_panel.unit_radius')}
                tooltip={t('analysis.canvas.layout_panel.unit_radius_tooltip')}
            >
                <InputNumber style={{width: '100%'}} min={1} />
            </Form.Item>
            <Form.Item
                name='linkDistance'
                label={t('analysis.canvas.layout_panel.link_distance_short')}
                tooltip={t('analysis.canvas.layout_panel.link_distance')}
            >
                <InputNumber style={{width: '100%'}} min={1} />
            </Form.Item>
            <Form.Item
                name='nodeSize'
                label={t('analysis.canvas.layout_panel.node_size')}
                tooltip={t('analysis.canvas.layout_panel.node_size_tooltip')}
            >
                <InputNumber style={{width: '100%'}} min={1} />
            </Form.Item>
            <Form.Item
                name='focusNode'
                label={t('analysis.canvas.layout_panel.focus_node')}
                tooltip={t('analysis.canvas.layout_panel.focus_node_tooltip')}
            >
                <Input style={{width: '100%'}} />
            </Form.Item>
            <Form.Item
                name='nodeSpacing'
                label={t('analysis.canvas.layout_panel.node_spacing')}
                tooltip={t('analysis.canvas.layout_panel.radial_node_spacing_tooltip')}
            >
                <InputNumber style={{width: '100%'}} min={1} />
            </Form.Item>
            <Form.Item
                name='preventOverlap'
                label={t('analysis.canvas.layout_panel.prevent_overlap')}
                valuePropName='checked'
                labelAlign='left'
                labelCol={{span: 20}}
                tooltip={t('analysis.canvas.layout_panel.radial_prevent_overlap_tooltip')}
            >
                <Switch />
            </Form.Item>
            <Form.Item
                name='strictRadial'
                label={t('analysis.canvas.layout_panel.strict_radial')}
                valuePropName='checked'
                labelAlign='left'
                labelCol={{span: 20}}
                tooltip={t('analysis.canvas.layout_panel.strict_radial_tooltip')}
            >
                <Switch />
            </Form.Item>
        </Form>
    );
};

export default RadialForm;
