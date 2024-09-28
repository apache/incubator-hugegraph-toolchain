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
 * @file 图分析组件 层次布局
 * @author
 */

import React from 'react';
import {Form, Select} from 'antd';
import SliderComponent from '../../../../components/SlideComponent';
import _ from 'lodash';

const rankdirOptions = [
    {value: 'TB', label: '从上至下'},
    {value: 'BT', label: '从下至上'},
    {value: 'LR', label: '从左至右'},
    {value: 'RL', label: '从右至左'},
];

const alignOptions = [
    {value: null, label: '中间对齐'},
    {value: 'UL', label: '对齐到左上角'},
    {value: 'UR', label: '对齐到右上角'},
    {value: 'DL', label: '对齐到左下角'},
    {value: 'DR', label: '对齐到右下角'},
];

const description = {
    rankdir: '布局的方向',
    align: '节点对齐方式',
    ranksep: `层间距（px）。在rankdir 为 'TB' 或 'BT' 时是竖直方向相邻层间距；
    在rankdir 为 'LR' 或 'RL' 时代表水平方向相邻层间距`,
    nodesep: `节点间距（px）。在rankdir 为 'TB' 或 'BT' 时是节点的水平间距；在rankdir 
    为 'LR' 或 'RL' 时代表节点的竖直方向间距`,
};

const DagreLayoutForm = props => {
    const {handleFormChange, initialValues} = props;
    const {useForm} = Form;
    const [dagreLayoutForm] = useForm();

    return (
        <Form
            form={dagreLayoutForm}
            onValuesChange={_.debounce(handleFormChange, 100)}
            initialValues={initialValues}
            labelCol={{span: 24}}
        >
            <Form.Item
                name='rankdir'
                label='布局方向'
                tooltip={description.rankdir}
            >
                <Select options={rankdirOptions} />
            </Form.Item>
            <Form.Item
                name='align'
                label='对齐'
                tooltip={description.align}
            >
                <Select options={alignOptions} />
            </Form.Item>
            <Form.Item
                name='ranksep'
                label='层间距'
                tooltip={description.ranksep}
            >
                <SliderComponent max={1000} />
            </Form.Item>
            <Form.Item
                name='nodesep'
                label='点间距'
                tooltip={description.nodesep}
            >
                <SliderComponent max={1000} />
            </Form.Item>
        </Form>
    );
};

export default DagreLayoutForm;
