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
 */

import React, {useCallback} from 'react';
import {Form, InputNumber} from 'antd';
import {useTranslation} from 'react-i18next';
import _ from 'lodash';

const GridForm = props => {
    const {handleFormChange, initialValues} = props;
    const {t} = useTranslation();
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
                label={t('analysis.canvas.layout_panel.rows')}
                tooltip={t('analysis.canvas.layout_panel.rows_tooltip')}
            >
                <InputNumber onChange={onRowsChange} min={1} style={{width: '100%'}} />
            </Form.Item>
            <Form.Item
                name='cols'
                label={t('analysis.canvas.layout_panel.cols')}
                tooltip={t('analysis.canvas.layout_panel.cols_tooltip')}
            >
                <InputNumber onChange={onColsChange} min={1} style={{width: '100%'}} />
            </Form.Item>
        </Form>
    );
};

export default GridForm;
