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
 * @author
 */

import React from 'react';
import {Form, InputNumber, Input, Switch} from 'antd';
import _ from 'lodash';

const description = {
    unitRadius: '每一圈距离上一圈的距离。默认填充整个画布，即根据图的大小决定',
    linkDistance: '边长度',
    nodeSize: '节点大小（直径）。用于防止节点重叠时的碰撞检测',
    focusNode: '辐射的中心点，默认为数据中第一个节点。可以传入节点 id 或节点本身',
    nodeSpacing: 'preventOverlap 为 true 时生效, 防止重叠时节点边缘间距的最小值',
    preventOverlap: '是否防止重叠，必须配合下面属性 nodeSize，只有设置了与当前图节点大小相同的 nodeSize 值，才能够进行节点重叠的碰撞检测',
    strictRadial: '是否必须是严格的 radial 布局',
};

const RadialForm = props => {
    const {handleFormChange, initialValues} = props;
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
                label='层级距离'
                tooltip={description.unitRadius}
            >
                <InputNumber style={{width: '100%'}} min={1} />
            </Form.Item>
            <Form.Item
                name='linkDistance'
                label='边长'
                tooltip={description.linkDistance}
            >
                <InputNumber style={{width: '100%'}} min={1} />
            </Form.Item>
            <Form.Item
                name='nodeSize'
                label='节点大小'
                tooltip={description.nodeSize}
            >
                <InputNumber style={{width: '100%'}} min={1} />
            </Form.Item>
            <Form.Item
                name='focusNode'
                label='中心节点'
                tooltip={description.focusNode}
            >
                <Input style={{width: '100%'}} />
            </Form.Item>
            <Form.Item
                name='nodeSpacing'
                label='节点间距'
                tooltip={description.nodeSpacing}
            >
                <InputNumber style={{width: '100%'}} min={1} />
            </Form.Item>
            <Form.Item
                name='preventOverlap'
                label='防止重叠'
                valuePropName='checked'
                labelAlign='left'
                labelCol={{span: 20}}
                tooltip={description.preventOverlap}
            >
                <Switch />
            </Form.Item>
            <Form.Item
                name='strictRadial'
                label='严格辐射'
                valuePropName='checked'
                labelAlign='left'
                labelCol={{span: 20}}
                tooltip={description.strictRadial}
            >
                <Switch />
            </Form.Item>
        </Form>
    );
};

export default RadialForm;
