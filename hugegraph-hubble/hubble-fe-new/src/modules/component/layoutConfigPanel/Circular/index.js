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
 * @author
 */

import React from 'react';
import {Form, Switch, Segmented} from 'antd';
import SliderComponent from '../../../../components/SlideComponent';
import _ from 'lodash';
import c from './index.module.scss';

const orderingOptions = [
    {value: null, label: '数据'},
    {value: 'topology', label: '拓扑'},
    {value: 'degree', label: '度数'},
];

const CircularLayoutForm = props => {
    const {handleFormChange, initialValues} = props;
    const {useForm} = Form;
    const [circularLayoutForm] = useForm();
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
                label='起始半径'
                tooltip='螺旋状布局的起始半径'
            >
                <SliderComponent min={30} max={1000} />
            </Form.Item>
            <Form.Item
                name='endRadius'
                label='结束半径'
                tooltip='螺旋状布局的结束半径'
            >
                <SliderComponent min={30} max={1000} />
            </Form.Item>
            <Form.Item
                name='divisions'
                label='分段数'
                tooltip='节点在环上的分段数'
            >
                <SliderComponent min={1} max={100} />
            </Form.Item>
            <Form.Item
                name='angleRatio'
                label='角度比'
                tooltip='从第一个节点到最后一个节点相差多少个2 * PI'
            >
                <SliderComponent min={1} max={100} />
            </Form.Item>
            <Form.Item
                name='ordering'
                label='排序依据'
                tooltip='节点在环上排序的依据'
            >
                <Segmented options={orderingOptions} />
            </Form.Item>
            <Form.Item
                name='clockwise'
                label='是否顺时针排列'
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
