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

import {useId} from 'react';
import {Form, Switch, InputNumber} from 'antd';
import {useTranslation} from 'react-i18next';

const ConcentricLayoutForm = props => {
    const {handleFormChange, initialValues, disabled} = props;
    const {t} = useTranslation();
    const instanceId = useId();
    const {useForm} = Form;
    const [concentricLayoutForm] = useForm();

    return (
        <Form
            name={instanceId}
            form={concentricLayoutForm}
            onValuesChange={handleFormChange}
            disabled={disabled}
            labelCol={{span: 24}}
            initialValues={initialValues}
        >
            <Form.Item
                name='nodeSize'
                label={t('analysis.canvas.layout_panel.node_size')}
                tooltip={t('analysis.canvas.layout_panel.node_size_tooltip')}
            >
                <InputNumber aria-label={t('analysis.canvas.layout_panel.node_size')} style={{width: '100%'}} min={1} max={100} />
            </Form.Item>
            <Form.Item
                name='sweep'
                label={t('analysis.canvas.layout_panel.sweep')}
                tooltip={t('analysis.canvas.layout_panel.sweep_tooltip')}
            >
                <InputNumber aria-label={t('analysis.canvas.layout_panel.sweep')} style={{width: '100%'}} />
            </Form.Item>
            <Form.Item
                name='startAngle'
                label={t('analysis.canvas.layout_panel.start_angle')}
                tooltip={t('analysis.canvas.layout_panel.start_angle_tooltip')}
            >
                <InputNumber aria-label={t('analysis.canvas.layout_panel.start_angle')} style={{width: '100%'}} />
            </Form.Item>
            <Form.Item
                name='preventOverlap'
                label={t('analysis.canvas.layout_panel.prevent_overlap')}
                valuePropName="checked"
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
                <InputNumber aria-label={t('analysis.canvas.layout_panel.node_spacing')} style={{width: '100%'}} min={0} max={1000} />
            </Form.Item>
            <Form.Item
                name='equidistant'
                label={t('analysis.canvas.layout_panel.equidistant')}
                valuePropName="checked"
                labelCol={{span: 20}}
                labelAlign='left'
            >
                <Switch aria-label={t('analysis.canvas.layout_panel.equidistant')} />
            </Form.Item>
            <Form.Item
                name='clockwise'
                label={t('analysis.canvas.layout_panel.clockwise')}
                valuePropName="checked"
                labelCol={{span: 20}}
                labelAlign='left'
            >
                <Switch aria-label={t('analysis.canvas.layout_panel.clockwise')} />
            </Form.Item>
        </Form>
    );
};

export default ConcentricLayoutForm;
