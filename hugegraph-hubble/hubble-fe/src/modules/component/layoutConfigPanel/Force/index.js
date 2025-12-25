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
 * @author
 */

import React from 'react';
import {Form, Switch} from 'antd';
import SliderComponent from '../../../../components/SlideComponent';
import _ from 'lodash';

const ForceLayoutForm = props => {
    const {handleFormChange, initialValues} = props;
    const {useForm} = Form;
    const [forceLayoutForm] = useForm();

    return (
        <Form
            form={forceLayoutForm}
            onValuesChange={_.debounce(handleFormChange, 100)}
            initialValues={initialValues}
            labelCol={{span: 24}}
        >
            <Form.Item
                name='nodeSize'
                label='节点大小'
                tooltip='节点大小（直径），用于碰撞检测。'
            >
                <SliderComponent />
            </Form.Item>
            <Form.Item
                name='linkDistance'
                label='边长度'
            >
                <SliderComponent max={1000} />
            </Form.Item>
            <Form.Item
                name='nodeStrength'
                label='点作用力'
                tooltip='节点作用力，正数代表节点之间的引力作用，负数代表节点之间的斥力作用'
            >
                <SliderComponent min={-500} max={500} />
            </Form.Item>
            <Form.Item
                name='preventOverlap'
                label='是否防止重叠'
                valuePropName='checked'
                labelCol={{span: 20}}
                labelAlign='left'
            >
                <Switch />
            </Form.Item>
            <Form.Item
                name='nodeSpacing'
                label='节点间距'
                tooltip='preventOverlap开启时生效, 防止重叠时节点边缘间距的最小值。'
            >
                <SliderComponent />
            </Form.Item>
        </Form>
    );
};

export default ForceLayoutForm;
