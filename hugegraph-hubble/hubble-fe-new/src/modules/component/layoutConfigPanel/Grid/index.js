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
 * @file  网格布局
 * @author
 */

import React, {useCallback} from 'react';
import {Form, InputNumber} from 'antd';
import _ from 'lodash';

const GridForm = props => {
    const {handleFormChange, initialValues} = props;
    const {useForm} = Form;
    const [form] = useForm();

    const onRowsChange = useCallback(
        () => {
            form.resetFields(['cols']);
        },
        [form]
    );

    const onColsChange = useCallback(
        () => {
            form.resetFields(['rows']);
        },
        [form]
    );

    return (
        <Form
            form={form}
            onValuesChange={_.debounce(handleFormChange, 100)}
            initialValues={initialValues}
            labelCol={{span: 24}}
        >
            <Form.Item
                name='rows'
                label='网格行数'
                tooltip='网格的行数，为 undefined 时算法会根据节点数量、布局空间、cols（若指定）自动计算'
            >
                <InputNumber onChange={onRowsChange} min={1} style={{width: '100%'}} />
            </Form.Item>
            <Form.Item
                name='cols'
                label='网格列数'
                tooltip='网格的列数，为 undefined 时算法根据节点数量、布局空间、rows（若指定）自动计算'
            >
                <InputNumber onChange={onColsChange} min={1} style={{width: '100%'}} />
            </Form.Item>
        </Form>
    );
};

export default GridForm;
