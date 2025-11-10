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
import {useState, useEffect} from 'react';
import * as rules from '../../../utils/rules';

const EdgeForm = ({open, index, onCancel, sourceField, targetField, edgeList}) => {
    const [selectLabel, setSelectLabel] = useState({});
    const [errorList, setErrorList] = useState({});
    const [edgeForm] = Form.useForm();

    const autoSelect = () => {
        const list = edgeForm.getFieldValue('attr') ?? [];
        const existKeys = list.map(item => item?.key);
        const enableKeys = selectLabel.properties ? selectLabel.properties.map(item => item.name) : [];

        const addRows = [];
        targetField.map(item => {
            if (!existKeys.includes(item) && enableKeys.includes(item)) {
                addRows.push({key: item, val: item});
            }
        });
        edgeForm.setFieldValue('attr', [...list, ...addRows]);
        // const list = edgeForm.getFieldValue('attr');
        // const row = list[index];

        // if (row && selectLabel.properties && selectLabel.properties.find(item => item.name === row.key)) {
        //     edgeForm.setFieldValue(['attr', index, 'val'], row.key);
        // }
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
                        className={'form_attr_select'}
                        name={[field.name, 'key']}
                        placeholder='请选择schema字段'
                    >
                        <Select options={targetField.map(item => ({label: item, value: item}))} />
                    </Form.Item>
                    <span className={'form_attr_split'}>-</span>
                    <Form.Item
                        className={'form_attr_select'}
                        name={[field.name, 'val']}
                    >
                        <Select
                            options={(selectLabel?.properties ?? []).map(item =>
                                ({label: item.name, value: item.name}))}
                            placeholder='请选择映射字段'
                        />
                    </Form.Item>
                    <Button type='link' onClick={() => remove(index)}> 删除</Button>
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
                        className={'form_attr_select'}
                        name={[field.name, 'key']}
                    >
                        <Select options={targetField.map(item => ({label: item, value: item}))} />
                    </Form.Item>
                    <span className={'form_attr_split'}>:</span>
                    <Form.Item
                        className={'form_attr_val'}
                        name={[field.name, 'origin']}
                    >
                        <Input />
                    </Form.Item>
                    <span className={'form_attr_split'}>{'->'}</span>
                    <Form.Item
                        className={'form_attr_val'}
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

    const handleLabel = (_, option) => {
        setSelectLabel(option.info);
        setErrorList({...errorList, label: ''});
    };

    const handleSource = () => {
        setErrorList({...errorList, source: ''});
    };

    const handleTarget = () => {
        setErrorList({...errorList, target: ''});
    };

    const handleCancel = () => {
        setErrorList({});
        setSelectLabel({});
        onCancel();
    };

    const addEdge = () => {
        // const info = form.getFieldValue('edge_form') || {};
        // const edges = form.getFieldValue('edges') || [];
        // const error = {};
        // let flag = true;
        // if (!info.label) {
        //     error.label = 'error';
        //     flag = false;
        // }

        // if (!info.source) {
        //     error.source = 'error';
        //     flag = false;
        // }

        // if (!info.target) {
        //     error.target = 'error';
        //     flag = false;
        // }

        // if (!flag) {
        //     setErrorList({...errorList, ...error});
        //     return;
        // }
        // setErrorList({});

        // const field_mapping = fieldMapping(info.attr);
        // const value_mapping = valueMapping(info.value);

        // edges.push({
        //     label: info.label,
        //     skip: false,
        //     source: [info.source],
        //     unfold_source: false,
        //     target: [info.target],
        //     unfold_target: false,
        //     field_mapping,
        //     value_mapping,
        //     selected: Object.keys(field_mapping).concat(info.source, info.target),
        //     ignored: [],
        //     null_values: [''],
        //     update_strategies: {},
        // });

        // form.setFieldsValue({edges: edges});
        // form.resetFields(['edge_form']);
        // onCancel();
        edgeForm.validateFields().then(() => {
            edgeForm.submit();
            onCancel();
        });
    };

    useEffect(() => {
        if (!open) {
            return;
        }

        edgeForm.resetFields();
        setSelectLabel({});

        if (index >= 0) {
            (new Promise(resolve => resolve())).then(() => {
                edgeForm.setFieldsValue({...edgeList[index], index});
                setSelectLabel(sourceField.find(item => item.name === edgeList[index].label));
            });
        }
    }, [open, index]);

    return (
        <Drawer
            title={index >= 0 ? '编辑边' : '创建边'}
            placement="right"
            onClose={onCancel}
            width={580}
            open={open}
        >
            <Form form={edgeForm} name='edge_form' labelCol={{span: 4}}>
                <Form.Item
                    required
                    label='边类型'
                    name={['label']}
                    rules={[rules.required()]}
                >
                    <Select
                        options={sourceField.map(item => ({label: item.name, value: item.name, info: item}))}
                        onChange={handleLabel}
                    />
                </Form.Item>
                <Form.Item
                    required
                    label='起点ID'
                    name='source'
                    rules={[rules.required()]}
                >
                    <Select
                        options={targetField.map(item => ({label: item, value: item}))}
                        onChange={handleSource}
                    />
                </Form.Item>
                <Form.Item
                    required
                    label='终点ID'
                    name='target'
                    rules={[rules.required()]}
                >
                    <Select
                        options={targetField.map(item => ({label: item, value: item}))}
                        onChange={handleTarget}
                    />
                </Form.Item>
                <Form.Item label='属性映射'>
                    <Form.List name='attr' rules={[checkDuplicate]}>
                        {attrFormList}
                    </Form.List>
                </Form.Item>
                <Form.Item label='值映射'>
                    <Form.List name='value'>
                        {valueFormList}
                    </Form.List>
                </Form.Item>
                <Form.Item name='index' hidden />
                <Form.Item wrapperCol={{offset: 4}}>
                    <Space>
                        <Button type='primary' onClick={addEdge}>确定</Button>
                        <Button onClick={handleCancel}>取消</Button>
                    </Space>
                </Form.Item>
            </Form>
        </Drawer>
    );
};

export default EdgeForm;
