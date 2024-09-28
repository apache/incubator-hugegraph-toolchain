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

import {Space, Button, Form, Select, Input, Drawer} from 'antd';
import {useState, useEffect, useCallback} from 'react';
import * as rules from '../../../utils/rules';

const VertexForm = ({open, onCancel, sourceField, targetField, vertexList, index}) => {
    const [selectLabel, setSelectLabel] = useState({});
    const [errorList, setErrorList] = useState({});
    const [vertexForm] = Form.useForm();

    const autoSelect = () => {
        const list = vertexForm.getFieldValue('attr') ?? [];
        const existKeys = list.map(item => item?.key);
        const enableKeys = selectLabel.properties ? selectLabel.properties.map(item => item.name) : [];

        const addRows = [];
        targetField.map(item => {
            if (!existKeys.includes(item) && enableKeys.includes(item)) {
                addRows.push({key: item, val: item});
            }
        });
        vertexForm.setFieldValue('attr', [...list, ...addRows]);
    };

    const checkDuplicate = () => ({
        validator(_, value) {
            const existName = [];
            if (value === undefined) {
                return Promise.resolve();
            }

            for (let item of value) {
                if (item === undefined || !item.key) {
                    return Promise.resolve();
                }

                if (existName.includes(item.key)) {
                    return Promise.reject(new Error('属性不可重复'));
                }

                existName.push(item.key);
            }

            return Promise.resolve();
        },
    });

    const attrFormList = (fields, {add, remove}, {errors}) => (
        <>
            {fields.map((field, index) => (
                <div key={field.key}>
                    <Form.Item
                        className="form_attr_select"
                        name={[field.name, 'key']}
                    >
                        <Select
                            options={targetField.map(item => ({label: item, value: item}))}
                            placeholder='请选择schema字段'
                        />
                    </Form.Item>
                    <span className={'form_attr_split'}>-</span>
                    <Form.Item
                        className="form_attr_select"
                        name={[field.name, 'val']}
                    >
                        <Select
                            options={(selectLabel?.properties ?? []).map(item =>
                                ({label: item.name, value: item.name}))}
                            placeholder='请选择映射字段'
                        />
                    </Form.Item>
                    <Space>
                        <Button type='link' onClick={() => remove(index)}> 删除</Button>
                    </Space>
                </div>
            ))}
            <Form.ErrorList errors={errors} />
            <Button onClick={() => autoSelect()} block style={{marginBottom: 8}}>自动匹配</Button>
            <Button type='dashed' onClick={() => add()} block>+新增</Button>
        </>
    );

    const valueFormList = (fields, {add, remove}) => (
        <>
            {fields.map((field, index) => (
                <div key={field.key}>
                    <Form.Item
                        className="form_attr_select"
                        name={[field.name, 'key']}
                    >
                        <Select
                            options={targetField.map(item => ({label: item, value: item}))}
                        />
                    </Form.Item>
                    <span className={'form_attr_split'}>:</span>
                    <Form.Item
                        className="form_attr_val"
                        name={[field.name, 'origin']}
                    >
                        <Input />
                    </Form.Item>
                    <span className={'form_attr_split'}>{'->'}</span>
                    <Form.Item
                        className="form_attr_val"
                        name={[field.name, 'replace']}
                    >
                        <Input />
                    </Form.Item>
                    <a onClick={() => remove(index)}> 删除</a>
                </div>
            )

            )}
            <div className={'form_attr_add'} onClick={() => add()}>+新增</div>
        </>
    );

    const handleLabel = useCallback((_, option) => {
        setSelectLabel(option.info);
        if (['PRIMARY_KEY', 'AUTOMATIC'].includes(option.info.id_strategy)) {
            vertexForm.resetFields(['id']);
        }
        setErrorList({...errorList, label: ''});
    }, [errorList]);

    const handleCancel = () => {
        setErrorList({});
        setSelectLabel({});
        vertexForm.resetFields();
        onCancel();
    };

    const onFinish = () => {

        vertexForm.validateFields().then(() => {
            vertexForm.submit();
            onCancel();
        });
    };

    useEffect(() => {
        if (!open) {
            return;
        }

        vertexForm.resetFields();
        setSelectLabel({});

        if (index >= 0) {
            (new Promise(resolve => resolve())).then(() => {
                vertexForm.setFieldsValue({...vertexList[index], index});
                setSelectLabel(sourceField.find(item => item.name === vertexList[index].label));
            });
        }
    }, [open, index]);

    return (
        <Drawer
            title={index >= 0 ? '编辑顶点' : '创建顶点'}
            placement="right"
            onClose={onCancel}
            width={580}
            open={open}
        >
            <Form form={vertexForm} labelCol={{span: 4}} name='vertex_form'>
                <Form.Item label='顶点类型' required name={'label'} rules={[rules.required()]}>
                    <Select
                        options={sourceField.map(item => ({label: item.name, value: item.name, info: item}))}
                        onChange={handleLabel}
                    />
                </Form.Item>
                <Form.Item
                    // required={selectLabel.id_strategy !== 'PRIMARY_KEY'}
                    label='ID列'
                    name={'id'}
                    rules={[!['PRIMARY_KEY', 'AUTOMATIC'].includes(selectLabel.id_strategy) ? rules.required() : null]}
                >
                    <Select
                        disabled={['PRIMARY_KEY', 'AUTOMATIC'].includes(selectLabel.id_strategy)
                        || !selectLabel.id_strategy}
                        options={targetField.map(item => ({label: item, value: item}))}
                        onChange={value => (['PRIMARY_KEY', 'AUTOMATIC'].includes(selectLabel.id_strategy)
                            ? null : value)}
                    />
                </Form.Item>
                <Form.Item label='属性映射'>
                    <Form.List name={'attr'} rules={[checkDuplicate]}>
                        {attrFormList}
                    </Form.List>
                </Form.Item>
                <Form.Item label='值映射'>
                    <Form.List name={'value'}>
                        {valueFormList}
                    </Form.List>
                </Form.Item>
                <Form.Item name='index' hidden />
                <Form.Item wrapperCol={{offset: 4}}>
                    <Space>
                        <Button type='primary' onClick={onFinish}>确定</Button>
                        <Button onClick={handleCancel}>取消</Button>
                    </Space>
                </Form.Item>
            </Form>
        </Drawer>
    );
};

export default VertexForm;
