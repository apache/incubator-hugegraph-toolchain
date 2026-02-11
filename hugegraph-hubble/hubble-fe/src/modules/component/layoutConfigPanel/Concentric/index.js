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
 * @author
 */

import React from 'react';
import {Form, Switch} from 'antd';
import SliderComponent from '../../../../components/SlideComponent';
import _ from 'lodash';

const ConcentricLayoutForm = props => {
    const {handleFormChange, initialValues} = props;
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
                label='节点大小'
                tooltip='节点大小（直径），用于防止节点重叠时的碰撞检测'
            >
                <SliderComponent min={1} max={100} />
            </Form.Item>
            <Form.Item
                name='sweep'
                label='弧度差'
                tooltip='第一个节点与最后一个节点之间的弧度差。'
            >
                <SliderComponent />
            </Form.Item>
            <Form.Item
                name='startAngle'
                label='起始弧度'
                tooltip='开始方式节点的弧度'
            >
                <SliderComponent />
            </Form.Item>
            <Form.Item
                name='preventOverlap'
                label='是否防止重叠'
                valuePropName="checked"
                labelCol={{span: 20}}
                labelAlign='left'
            >
                <Switch />
            </Form.Item>
            <Form.Item
                name='nodeSpacing'
                label='节点间距'
                tooltip='Prevent Overlap为true时生效，节点边缘间距的最小值，以防止重叠'
            >
                <SliderComponent min={0} max={1000} />
            </Form.Item>
            <Form.Item
                name='equidistant'
                label='环与环距离是否相等'
                valuePropName="checked"
                labelCol={{span: 20}}
                labelAlign='left'
            >
                <Switch />
            </Form.Item>
            <Form.Item
                name='clockwise'
                label='是否按照顺时针排列'
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
