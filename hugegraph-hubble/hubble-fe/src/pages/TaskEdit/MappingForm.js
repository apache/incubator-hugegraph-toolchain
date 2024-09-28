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

import {Space, Button, Form, Select, Input, Collapse, Typography} from 'antd';
import {useState} from 'react';
import Style from './index.module.scss';

const fieldMapping = list => {
    const obj = {};
    if (!list) {
        return obj;
    }

    list.map(item => {
        if (!item || !item.key) {
            return;
        }

        obj[item.key] = item.val;
    });

    return obj;
};

const valueMapping = list => {
    const obj = {};
    if (!list) {
        return obj;
    }

    list.map(item => {
        if (!item || !item.key) {
            return;
        }

        if (!obj[item.key]) {
            obj[item.key] = {};
        }
        obj[item.key][item.origin] = item.replace;
    });

    return obj;
};

const VertexForm = ({visible, onCancel, form, sourceField, targetField}) => {
    const [selectLabel, setSelectLabel] = useState({});
    const [selectID, setSelectID] = useState('');
    const [errorList, setErrorList] = useState({});

    const attrFormList = (fields, {add, remove}) => (
        <>
            {fields.map((field, index) => (
                <div key={field.key}>
                    <Form.Item
                        className={Style.form_attr_select}
                        name={[field.name, 'key']}
                    >
                        <Select
                            options={targetField.map(item => ({label: item, value: item}))}
                            placeholder='请选择schema字段'
                        />
                    </Form.Item>
                    <span className={Style.form_attr_split}>-</span>
                    <Form.Item
                        className={Style.form_attr_select}
                        name={[field.name, 'val']}
                    >
                        <Select
                            options={(selectLabel?.properties ?? []).map(item =>
                                ({label: item.name, value: item.name}))}
                            placeholder='请选择映射字段'
                        />
                    </Form.Item>
                    <a onClick={() => remove(index)}> 删除</a>
                </div>
            )

            )}
            <div className={Style.form_attr_add} onClick={() => add()}>+新增</div>
        </>
    );

    const valueFormList = (fields, {add, remove}) => (
        <>
            {fields.map((field, index) => (
                <div key={field.key}>
                    <Form.Item
                        className={Style.form_attr_select}
                        name={[field.name, 'key']}
                    >
                        <Select
                            options={targetField.map(item => ({label: item, value: item}))}
                        />
                    </Form.Item>
                    <span className={Style.form_attr_split}>:</span>
                    <Form.Item
                        className={Style.form_attr_val}
                        name={[field.name, 'origin']}
                    >
                        <Input />
                    </Form.Item>
                    <span className={Style.form_attr_split}>{'->'}</span>
                    <Form.Item
                        className={Style.form_attr_val}
                        name={[field.name, 'replace']}
                    >
                        <Input />
                    </Form.Item>
                    <a onClick={() => remove(index)}> 删除</a>
                </div>
            )

            )}
            <div className={Style.form_attr_add} onClick={() => add()}>+新增</div>
        </>
    );

    const handleLabel = (_, option) => {
        setSelectLabel(option.info);
        setErrorList({...errorList, label: ''});
    };

    const handleID = value => {
        setSelectID(value);
        setErrorList({...errorList, id: ''});
    };

    const handleCancel = () => {
        setErrorList({});
        setSelectLabel({});
        form.resetFields(['vertex_form']);
        onCancel();
    };

    const addVertex = () => {
        if (!selectLabel.name) {
            setErrorList({...errorList, label: 'error'});
            return;
        }

        if (selectLabel.id_strategy !== 'PRIMARY_KEY' && !selectID) {
            setErrorList({...errorList, id: 'error'});
            return;
        }

        setErrorList({});

        const info = form.getFieldValue('vertex_form') || {};
        const vertices = form.getFieldValue('vertices') || [];
        const field_mapping = fieldMapping(info.attr);
        const value_mapping = valueMapping(info.value);
        const id = selectLabel.id_strategy === 'PRIMARY_KEY' ? null : info.id;
        const selected = Object.keys(field_mapping);

        vertices.push({
            label: info.label,
            skip: false,
            id,
            unfold: false,
            field_mapping,
            value_mapping,
            selected: id ? selected.concat(id) : selected,
            ignored: [],
            null_values: [''],
            update_strategies: {},
        });

        form.setFieldsValue({vertices: vertices});
        form.resetFields(['vertex_form']);
        setSelectLabel({});
        onCancel();
    };

    return (
        <div className={Style.form_attr_border} style={{display: visible ? '' : 'none'}}>
            <Form.Item label='顶点类型' required name={['vertex_form', 'label']} validateStatus={errorList.label}>
                <Select
                    options={sourceField.map(item => ({label: item.name, value: item.name, info: item}))}
                    onChange={handleLabel}
                />
            </Form.Item>
            <Form.Item
                required={selectLabel.id_strategy !== 'PRIMARY_KEY'}
                label='ID列'
                name={['vertex_form', 'id']}
                validateStatus={errorList.id}
            >
                <Select
                    disabled={selectLabel.id_strategy === 'PRIMARY_KEY' || !selectLabel.id_strategy}
                    options={targetField.map(item => ({label: item, value: item}))}
                    onChange={handleID}
                />
            </Form.Item>
            <Form.Item label='属性映射'>
                <Form.List name={['vertex_form', 'attr']}>
                    {attrFormList}
                </Form.List>
            </Form.Item>
            <Form.Item label='值映射'>
                <Form.List name={['vertex_form', 'value']}>
                    {valueFormList}
                </Form.List>
            </Form.Item>
            <Form.Item wrapperCol={{offset: 4}}>
                <Space>
                    <Button type='primary' onClick={addVertex}>确定</Button>
                    <Button onClick={handleCancel}>取消</Button>
                </Space>
            </Form.Item>
        </div>
    );
};

const EdgeForm = ({visible, onCancel, form, sourceField, targetField}) => {
    const [selectLabel, setSelectLabel] = useState({});
    const [errorList, setErrorList] = useState({});

    const attrFormList = (fields, {add, remove}) => (
        <>
            {fields.map((field, index) => (
                <div key={field.key}>
                    <Form.Item
                        className={Style.form_attr_select}
                        name={[field.name, 'key']}
                        placeholder='请选择schema字段'
                    >
                        <Select options={targetField.map(item => ({label: item, value: item}))} />
                    </Form.Item>
                    <span className={Style.form_attr_split}>-</span>
                    <Form.Item
                        className={Style.form_attr_select}
                        name={[field.name, 'val']}
                    >
                        <Select
                            options={(selectLabel?.properties ?? []).map(item =>
                                ({label: item.name, value: item.name}))}
                            placeholder='请选择映射字段'
                        />
                    </Form.Item>
                    <a onClick={() => remove(index)}> 删除</a>
                </div>
            )

            )}
            <div className={Style.form_attr_add} onClick={() => add()}>+新增</div>
        </>
    );

    const valueFormList = (fields, {add, remove}) => (
        <>
            {fields.map((field, index) => (
                <div key={field.key}>
                    <Form.Item
                        className={Style.form_attr_select}
                        name={[field.name, 'key']}
                    >
                        <Select options={targetField.map(item => ({label: item, value: item}))} />
                    </Form.Item>
                    <span className={Style.form_attr_split}>:</span>
                    <Form.Item
                        className={Style.form_attr_val}
                        name={[field.name, 'origin']}
                    >
                        <Input />
                    </Form.Item>
                    <span className={Style.form_attr_split}>{'->'}</span>
                    <Form.Item
                        className={Style.form_attr_val}
                        name={[field.name, 'replace']}
                    >
                        <Input />
                    </Form.Item>
                    <a onClick={() => remove(index)}> 删除</a>
                </div>
            )

            )}
            <div className={Style.form_attr_add} onClick={() => add()}>+新增</div>
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
        form.resetFields(['edge_form']);
        setErrorList({});
        setSelectLabel({});
        onCancel();
    };

    const addEdge = () => {
        const info = form.getFieldValue('edge_form') || {};
        const edges = form.getFieldValue('edges') || [];
        const error = {};
        let flag = true;
        if (!info.label) {
            error.label = 'error';
            flag = false;
        }

        if (!info.source) {
            error.source = 'error';
            flag = false;
        }

        if (!info.target) {
            error.target = 'error';
            flag = false;
        }

        if (!flag) {
            setErrorList({...errorList, ...error});
            return;
        }
        setErrorList({});

        const field_mapping = fieldMapping(info.attr);
        const value_mapping = valueMapping(info.value);

        edges.push({
            label: info.label,
            skip: false,
            source: [info.source],
            unfold_source: false,
            target: [info.target],
            unfold_target: false,
            field_mapping,
            value_mapping,
            selected: Object.keys(field_mapping).concat(info.source, info.target),
            ignored: [],
            null_values: [''],
            update_strategies: {},
        });

        form.setFieldsValue({edges: edges});
        form.resetFields(['edge_form']);
        onCancel();
    };

    return (
        <div className={Style.form_attr_border} style={{display: visible ? '' : 'none'}}>
            <Form.Item
                required
                label='边类型'
                name={['edge_form', 'label']}
                validateStatus={errorList.label}
            >
                <Select
                    options={sourceField.map(item => ({label: item.name, value: item.name, info: item}))}
                    onChange={handleLabel}
                />
            </Form.Item>
            <Form.Item
                required
                label='起点ID'
                name={['edge_form', 'source']}
                validateStatus={errorList.source}
            >
                <Select
                    options={targetField.map(item => ({label: item, value: item}))}
                    onChange={handleSource}
                />
            </Form.Item>
            <Form.Item
                required
                label='终点ID'
                name={['edge_form', 'target']}
                validateStatus={errorList.target}
            >
                <Select
                    options={targetField.map(item => ({label: item, value: item}))}
                    onChange={handleTarget}
                />
            </Form.Item>
            <Form.Item label='属性映射'>
                <Form.List name={['edge_form', 'attr']}>
                    {attrFormList}
                </Form.List>
            </Form.Item>
            <Form.Item label='值映射'>
                <Form.List name={['edge_form', 'value']}>
                    {valueFormList}
                </Form.List>
            </Form.Item>
            <Form.Item wrapperCol={{offset: 4}}>
                <Space>
                    <Button type='primary' onClick={addEdge}>确定</Button>
                    <Button onClick={handleCancel}>取消</Button>
                </Space>
            </Form.Item>
        </div>
    );
};

const MappingForm = ({prev, next, visible, form, targetField, vertex, edge}) => {
    const [mappingForm, setMappingForm] = useState('');

    const checkNext = () => {
        next();
    };

    const removeVertices = (e, index) => {
        e.stopPropagation();
        const vertices = form.getFieldValue('vertices');
        vertices.splice(index, 1);
        form.setFieldsValue({vertices: vertices});
    };

    const removeEdges = (e, index) => {
        e.stopPropagation();
        const edges = form.getFieldValue('edges');
        edges.splice(index, 1);
        form.setFieldsValue({edges: edges});
    };

    return (
        <div style={{display: visible ? '' : 'none'}}>
            <Typography.Title level={5}>映射字段</Typography.Title>
            <Space className={Style.form_attr_button}>
                <Button onClick={() => setMappingForm(mappingForm === 'vertex' ? '' : 'vertex')}>
                    新增顶点映射
                </Button>
                <Button onClick={() => setMappingForm(mappingForm === 'edge' ? '' : 'edge')}>
                    新增边映射
                </Button>
            </Space>
            <VertexForm
                visible={mappingForm === 'vertex'}
                onCancel={() => setMappingForm('')}
                form={form}
                targetField={targetField}
                sourceField={vertex || []}
            />
            <EdgeForm
                visible={mappingForm === 'edge'}
                onCancel={() => setMappingForm('')}
                form={form}
                targetField={targetField}
                sourceField={edge || []}
            />
            <Form.Item shouldUpdate>
                {() => {
                    const vertices = form.getFieldValue('vertices');
                    const edges = form.getFieldValue('edges');

                    return (
                        <Collapse className={Style.form_attr_table}>
                            {vertices && vertices.map((item, index) => {
                                return (
                                    <Collapse.Panel
                                        header={<Space><span>类型：顶点</span><span>名称：{item.label}</span></Space>}
                                        extra={<a onClick={e => removeVertices(e, index)}>删除</a>}
                                        key={item.label}
                                    >
                                        <Form.Item label='顶点类型'>{item.label}</Form.Item>
                                        <Form.Item label='ID列'>{item.id}</Form.Item>
                                        <Form.Item label='属性映射'>
                                            {item.field_mapping && Object.keys(item.field_mapping).map(key => {
                                                return (
                                                    <div key={key}>
                                                        {key} {'->'} {item.field_mapping[key]}
                                                    </div>
                                                );
                                            })}
                                        </Form.Item>
                                        <Form.Item label='值映射'>
                                            {item.value_mapping && Object.keys(item.value_mapping).map(key => {
                                                return Object.keys(item.value_mapping[key]).map(k => {
                                                    return (
                                                        <div key={k}>
                                                            {key}：{k} {'->'} {item.value_mapping[key][k]}
                                                        </div>
                                                    );
                                                });
                                            })}
                                        </Form.Item>
                                    </Collapse.Panel>
                                );
                            })}
                            {edges && edges.map((item, index) => {
                                return (
                                    <Collapse.Panel
                                        header={<Space><span>类型：边</span><span>名称：{item.label}</span></Space>}
                                        extra={<a onClick={e => removeEdges(e, index)}>删除</a>}
                                        key={item.label}
                                    >
                                        <Form.Item label='顶点类型'>{item.label}</Form.Item>
                                        <Form.Item label='起点ID'>{item.source ? item.source[0] : ''}</Form.Item>
                                        <Form.Item label='终点ID'>{item.target ? item.target[0] : ''}</Form.Item>
                                        <Form.Item label='属性映射'>
                                            {item.field_mapping && Object.keys(item.field_mapping).map(key => {
                                                return (
                                                    <div key={key}>
                                                        {key} {'->'} {item.field_mapping[key]}
                                                    </div>
                                                );
                                            })}
                                        </Form.Item>
                                        <Form.Item label='值映射'>
                                            {item.value_mapping && Object.keys(item.value_mapping).map(key => {
                                                return Object.keys(item.value_mapping[key]).map(k => {
                                                    return (
                                                        <div key={k}>
                                                            {key}：{k} {'->'} {item.value_mapping[key][k]}
                                                        </div>
                                                    );
                                                });
                                            })}
                                        </Form.Item>
                                    </Collapse.Panel>
                                );
                            })}
                        </Collapse>
                    );
                }}
            </Form.Item>
            <Form.Item name='vertices' hidden />
            <Form.Item name='edges' hidden />

            <Space>
                <Button onClick={prev}>上一步</Button>
                <Button type='primary' onClick={checkNext}>下一步</Button>
            </Space>
        </div>
    );
};

export default MappingForm;
