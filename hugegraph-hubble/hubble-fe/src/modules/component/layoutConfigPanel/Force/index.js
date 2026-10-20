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
 * @file 切换力导布局组件 force
 */

import {useId} from 'react';
import {Form, Switch, InputNumber} from 'antd';
import {useTranslation} from 'react-i18next';

const ForceLayoutForm = props => {
    const {handleFormChange, initialValues, disabled} = props;
    const {t} = useTranslation();
    const instanceId = useId();
    const {useForm} = Form;
    const [forceLayoutForm] = useForm();

    return (
        <Form
            name={instanceId}
            form={forceLayoutForm}
            onValuesChange={handleFormChange}
            disabled={disabled}
            initialValues={initialValues}
            labelCol={{span: 24}}
        >
            <Form.Item
                name='nodeSize'
                label={t('analysis.canvas.layout_panel.node_size')}
                tooltip={t('analysis.canvas.layout_panel.node_size_tooltip')}
            >
                <InputNumber aria-label={t('analysis.canvas.layout_panel.node_size')} min={1} style={{width: '100%'}} />
            </Form.Item>
            <Form.Item
                name='linkDistance'
                label={t('analysis.canvas.layout_panel.link_distance')}
            >
                <InputNumber aria-label={t('analysis.canvas.layout_panel.link_distance')} min={0} style={{width: '100%'}} max={1000} />
            </Form.Item>
            <Form.Item
                name='nodeStrength'
                label={t('analysis.canvas.layout_panel.node_strength')}
                tooltip={t('analysis.canvas.layout_panel.node_strength_tooltip')}
            >
                <InputNumber aria-label={t('analysis.canvas.layout_panel.node_strength')} style={{width: '100%'}} min={-5000} max={5000} />
            </Form.Item>
            <Form.Item
                name='preventOverlap'
                label={t('analysis.canvas.layout_panel.prevent_overlap')}
                valuePropName='checked'
                labelCol={{span: 20}}
                labelAlign='left'
            >
                <Switch aria-label={t('analysis.canvas.layout_panel.prevent_overlap')} />
            </Form.Item>
            <Form.Item
                name='nodeSpacing'
                label={t('analysis.canvas.layout_panel.node_spacing')}
                tooltip={t('analysis.canvas.layout_panel.node_spacing_tooltip')}
            >
                <InputNumber aria-label={t('analysis.canvas.layout_panel.node_spacing')} min={0} style={{width: '100%'}} />
            </Form.Item>
        </Form>
    );
};

export default ForceLayoutForm;
