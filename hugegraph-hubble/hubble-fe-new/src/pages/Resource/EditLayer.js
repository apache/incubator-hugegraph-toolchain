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

import {Modal, Form, Input, Space, Checkbox, AutoComplete, Tag, Row, Col, Card, message} from 'antd';
import {useCallback, useEffect, useState} from 'react';
import {PlusOutlined} from '@ant-design/icons';
import Style from './index.module.scss';
import * as api from '../../api';

const metaOptions = [
    {
        label: '属性类型',
        value: 'PROPERTY_KEY',
    },
    {
        label: '顶点类型',
        value: 'VERTEX_LABEL',
    },
    {
        label: '边类型',
        value: 'EDGE_LABEL',
    },
    {
        label: '索引类型',
        value: 'INDEX_LABEL',
    },
];

const cardGridOption = {style: {width: '100%'}, hoverable: false};

const AddFieldLayer = ({visible, onCancel, onChange, type}) => {
    const [form] = Form.useForm();

    const onFinish = useCallback(() => {
        form.validateFields().then(values => {
            onChange(type, values);
            onCancel();
        });
    }, [form, onCancel, onChange, type]);

    return (
        <Modal
            title='创建label'
            open={visible}
            onCancel={onCancel}
            onOk={onFinish}
            destroyOnClose
            width={600}
        >
            <Form
                form={form}
                labelCol={{span: 6}}
                preserve={false}
                className={Style.form_label}
            >
                <Form.Item label='label' name='label'>
                    <Input placeholder='请输入label' rules={[{required: true, message: '请输入label'}]} />
                </Form.Item>
                <Form.List name='properties'>
                    {(fields, {add, remove}) => (
                        <>
                            {fields.map((field, index) => (
                                <Form.Item label={'key & value'} key={field.key}>
                                    <Form.Item
                                        name={[field.name, 'key']}
                                        noStyle
                                    >
                                        <Input className={Style.form_input} />
                                    </Form.Item>
                                    <span className={Style.form_split}>=</span>
                                    <Form.Item
                                        name={[field.name, 'val']}
                                        noStyle
                                    >
                                        <Input className={Style.form_input} />
                                    </Form.Item>
                                    <a onClick={() => remove(index)}> 删除</a>
                                </Form.Item>
                            ))}
                            <Form.Item wrapperCol={{offset: 6}}>
                                <div className={Style.form_add} onClick={() => add()}>+新增</div>
                            </Form.Item>
                        </>
                    )}
                </Form.List>
            </Form>
        </Modal>
    );
};

const EditLayer = ({visible, onCancel, refresh, graphspace, op, detail}) => {
    const [form] = Form.useForm();
    const [addLayerVisible, setAddLayerVisible] = useState(false);
    const [labelType, setLabelType] = useState('vertex');
    const [graphspaceList, setGraphspaceList] = useState([]);

    const formatProperties = item => {
        if (!item.properties) {
            return item;
        }

        const tmp = {...item};
        const properties = {};
        for (let p of item.properties) {
            properties[p.key] = p.val;
        }

        tmp.properties = properties;

        return tmp;
    };

    const unformatProperties = item => {
        if (!item.properties) {
            return item;
        }

        const tmp = {...item};
        const properties = [];
        for (let k of Object.keys(item.properties)) {
            properties.push({key: k, val: item.properties[k]});
        }

        tmp.properties = properties;

        return tmp;
    };

    const onFinish = useCallback(() => {
        form.validateFields().then(values => {
            const {vertex, edge, meta, SCHEMA, VERTEX_ALL, EDGE_ALL,
                target_name, target_graph, target_description,
            } = values;

            const target_resources = [];
            if (VERTEX_ALL) {
                target_resources.push({type: 'VERTEX'});
            }
            else if (vertex) {
                vertex.map(item => target_resources.push({type: 'VERTEX', ...formatProperties(item)}));
            }

            if (EDGE_ALL) {
                target_resources.push({type: 'EDGE'});
            }
            else if (edge) {
                edge.map(item => target_resources.push({type: 'EDGE', ...formatProperties(item)}));
            }

            if (!SCHEMA && meta) {
                meta.map(item => target_resources.push({type: item}));
            }

            values.GREMLIN && target_resources.push({type: 'GREMLIN'});
            values.TASK && target_resources.push({type: 'TASK'});
            values.VAR && target_resources.push({type: 'VAR'});
            values.SCHEMA && target_resources.push({type: 'SCHEMA'});

            if (op === 'create') {
                api.auth.addResource(graphspace, {
                    target_name,
                    target_resources,
                    target_graph,
                    target_description,
                }).then(res => {
                    if (res.status === 200) {
                        message.success('添加成功');
                        onCancel();
                        refresh();

                        return;
                    }

                    message.error(res.message);
                });
            }

            if (op === 'edit') {
                api.auth.updateResource(graphspace, detail.id, {
                    target_resources,
                    target_description,
                }).then(res => {
                    if (res.status === 200) {
                        message.success('编辑成功');
                        onCancel();
                        refresh();

                        return;
                    }

                    message.error(res.message);
                });
            }
        });
    }, [form, op, detail.id, graphspace, onCancel, refresh]);

    const checkAll = useCallback(() => {
        const {SCHEMA, VERTEX_ALL, EDGE_ALL, VAR, GREMLIN, TASK} = form.getFieldsValue();

        form.setFieldsValue({ALL: SCHEMA && VERTEX_ALL && EDGE_ALL && VAR && GREMLIN && TASK});
    }, [form]);

    const handleSchema = useCallback(e => {
        form.setFieldsValue({meta: e.target.checked ? metaOptions.map(item => item.value) : []});
        checkAll();
    }, [form, checkAll]);

    const handleMeta = useCallback(list => {
        form.setFieldsValue({SCHEMA: list.length === 4});
        checkAll();
    }, [form, checkAll]);

    const handleAll = useCallback(e => {
        const checked = e.target.checked;
        form.setFieldsValue({
            meta: checked ? metaOptions.map(item => item.value) : [],
            SCHEMA: checked,
            VERTEX_ALL: checked,
            EDGE_ALL: checked,
            VAR: checked,
            GREMLIN: checked,
            TASK: checked,
        });
    }, [form]);

    const handleVar = useCallback(e => {
        checkAll();
    }, [checkAll]);

    const handleGremlin = useCallback(e => {
        checkAll();
    }, [checkAll]);

    const handleTask = useCallback(e => {
        checkAll();
    }, [checkAll]);

    const handleVertexAll = useCallback(e => {
        checkAll();
    }, [checkAll]);

    const handleEgeAll = useCallback(e => {
        checkAll();
    }, [checkAll]);

    const addLabel = useCallback((type, values) => {
        const list = form.getFieldValue(type) || [];

        list.push({...values});
        form.setFieldsValue({[type]: list});
    }, [form]);

    const removeLabel = (type, label, index) => {
        const list = form.getFieldValue(type);

        for (let i in list) {
            if (label === list[i].label) {
                if (index === undefined) {
                    list.splice(i, 1);
                }
                else {
                    list[i].splice(index, 1);
                }
                break;
            }
        }

        form.setFieldsValue({[type]: list});
    };

    const showAddVertex = () => {
        setAddLayerVisible(true);
        setLabelType('vertex');
    };

    const showAddEdge = () => {
        setAddLayerVisible(true);
        setLabelType('edge');
    };

    const checkClose = type => {
        if (op === 'view') {
            return false;
        }

        if (type === 'vertex' && form.getFieldValue('VERTEX_ALL')) {
            return false;
        }

        if (type === 'edge' && form.getFieldValue('EDGE_ALL')) {
            return false;
        }

        return true;
    };

    const filterAutoComplete = useCallback((input, option) => option.label.includes(input), []);

    const hideAddLayer = useCallback(() => setAddLayerVisible(false), []);

    const handleChangeLabel = useCallback((type, val) => addLabel(type, val), [addLabel]);

    useEffect(() => {
        if (!visible) {
            return;
        }

        api.manage.getGraphList(graphspace, {page_size: 99999}).then(res => {
            if (res.status === 200) {
                setGraphspaceList(res.data.records);
            }
        });
    }, [visible, graphspace]);

    useEffect(() => {
        if (!visible) {
            return;
        }

        form.resetFields();
        if (op === 'create') {
            // form.resetFields();
            return;
        }

        const {target_name, target_graph, target_description, target_resources} = detail;
        const meta = [];
        const vertex = [];
        const edge = [];

        form.setFieldsValue({
            target_graph,
            target_name,
            target_description,
        });

        for (let resource of target_resources) {
            if (resource.type === 'ALL') {
                form.setFieldsValue({
                    SCHEMA: true,
                    meta: ['PROPERTY_KEY', 'VERTEX_LABEL', 'EDGE_LABEL', 'INDEX_LABEL'],
                    ALL: true,
                    VAR: true,
                    GREMLIN: true,
                    TASK: true,
                    VERTEX_ALL: true,
                    EDGE_ALL: true,
                });
                return;
            }

            if (resource.type === 'SCHEMA') {
                form.setFieldsValue({
                    SCHEMA: true,
                });
                meta.push('PROPERTY_KEY', 'VERTEX_LABEL', 'EDGE_LABEL', 'INDEX_LABEL');
            }

            if (resource.type === 'VAR') {
                form.setFieldsValue({VAR: true});
            }

            if (resource.type === 'GREMLIN') {
                form.setFieldsValue({GREMLIN: true});
            }

            if (resource.type === 'TASK') {
                form.setFieldsValue({TASK: true});
            }

            if (['PROPERTY_KEY', 'VERTEX_LABEL', 'EDGE_LABEL', 'INDEX_LABEL'].includes(resource.type)) {
                meta.push(resource.type);
            }

            if (resource.type === 'VERTEX') {
                resource.label === '*'
                    ? form.setFieldsValue({VERTEX_ALL: true}) : vertex.push(unformatProperties(resource));
            }

            if (resource.type === 'EDGE') {
                resource.label === '*'
                    ? form.setFieldsValue({EDGE_ALL: true}) : edge.push(unformatProperties(resource));
            }
        }

        console.log(detail, vertex, edge);
        form.setFieldsValue({
            meta,
            vertex,
            edge,
        });
    }, [detail, visible, op, form]);

    return (
        <Modal
            title={{'edit': '编辑资源', 'create': '创建资源', 'view': '查看资源'}[op]}
            open={visible}
            onCancel={onCancel}
            onOk={onFinish}
            width={600}
        >
            <Form
                labelCol={{span: 6}}
                wrapperCol={{span: 16}}
                initialValues={{remember: true}}
                autoComplete="off"
                form={form}
                disabled={op === 'view'}
            >
                <Form.Item
                    label="资源名称"
                    name="target_name"
                    rules={[
                        {required: true, message: '请输入资源名称!'},
                        {pattern: /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/, message: '请输入正确的格式!'},
                        {max: 48, message: '最大长度为48个字符!'},
                    ]}
                >
                    <Input placeholder="请输入资源名称" disabled={op === 'edit'} />
                </Form.Item>
                <Form.Item
                    label="图"
                    name="target_graph"
                    rules={[{required: true, message: '请选择图!'}]}
                >
                    <AutoComplete
                        disabled={op === 'edit'}
                        placeholder="请选择图"
                        options={graphspaceList.map(item => ({label: item.nickname, value: item.nickname}))}
                        filterOption={filterAutoComplete}
                    />
                </Form.Item>
                <Form.Item label='备注' name='target_description'>
                    <Input />
                </Form.Item>
                <Form.Item wrapperCol={{offset: 6, span: 16}}>
                    <Card
                        headStyle={{backgroundColor: '#f0f0f0'}}
                        title={(
                            <Form.Item name='ALL' noStyle valuePropName='checked'>
                                <Checkbox onChange={handleAll}>所有资源</Checkbox>
                            </Form.Item>
                        )}
                    >
                        <Card.Grid {...cardGridOption}>
                            <Form.Item name='SCHEMA' noStyle valuePropName='checked'>
                                <Checkbox onChange={handleSchema}>元数据</Checkbox>
                            </Form.Item>
                            <Form.Item name='meta' noStyle>
                                <Checkbox.Group options={metaOptions} onChange={handleMeta} />
                            </Form.Item>
                        </Card.Grid>

                        <Card.Grid {...cardGridOption}>
                            <Form.Item name='vertex' hidden />
                            <Row gutter={[24, 12]}>
                                <Col span={24}>
                                    <Space>
                                        <Form.Item name={'VERTEX_ALL'} valuePropName='checked' noStyle>
                                            <Checkbox onChange={handleVertexAll}>全部顶点</Checkbox>
                                        </Form.Item>
                                        <Form.Item noStyle shouldUpdate>
                                            {({getFieldValue}) => {
                                                const val = getFieldValue('VERTEX_ALL');
                                                return !val ? <a onClick={showAddVertex}><PlusOutlined />新增</a> : null;
                                            }}
                                        </Form.Item>
                                    </Space>
                                </Col>
                                <Col span={24}>
                                    <Form.Item label='已选择顶点' style={{margin: 0}} shouldUpdate>
                                        {({getFieldValue}) => {
                                            const vertex = getFieldValue('vertex');

                                            return (
                                                <Row gutter={[4, 4]}>
                                                    {vertex && vertex.map(item => (
                                                        <Col span={24} key={item.label}>
                                                            <Tag
                                                                closable={checkClose('vertex')}
                                                                color="processing"
                                                                onClick={() => removeLabel('vertex', item.label)}
                                                            >
                                                                {item.label}
                                                            </Tag>
                                                            {item.properties && item.properties.map((property, k) => (
                                                                <Tag
                                                                    closable={checkClose('vertex')}
                                                                    color="default"
                                                                    key={property.key}
                                                                    onClick={() =>
                                                                        removeLabel('vertex', item.label, k)}
                                                                >
                                                                    {property.key}={property.val}
                                                                </Tag>
                                                            ))}
                                                        </Col>
                                                    ))}
                                                </Row>
                                            );
                                        }}
                                    </Form.Item>
                                </Col>
                            </Row>
                        </Card.Grid>

                        <Card.Grid {...cardGridOption}>
                            <Form.Item name='edge' hidden />
                            <Row gutter={[24, 12]}>
                                <Col span={24}>
                                    <Space>
                                        <Form.Item name='EDGE_ALL' noStyle valuePropName='checked'>
                                            <Checkbox onChange={handleEgeAll}>全部边</Checkbox>
                                        </Form.Item>
                                        <Form.Item noStyle shouldUpdate>
                                            {({getFieldValue}) => {
                                                const val = getFieldValue('EDGE_ALL');
                                                return !val ? <a onClick={showAddEdge}><PlusOutlined />新增</a> : null;
                                            }}
                                        </Form.Item>
                                    </Space>
                                </Col>
                                <Col span={24}>
                                    <Form.Item label='已选择顶点' style={{margin: 0}} shouldUpdate>
                                        {({getFieldValue}) => {
                                            const edge = getFieldValue('edge');

                                            return (
                                                <Row gutter={[4, 4]}>
                                                    {edge && edge.map(item => (
                                                        <Col span={24} key={item.label}>
                                                            <Tag
                                                                closable={checkClose('edge')}
                                                                color="processing"
                                                                onClick={() => removeLabel('edge', item.label)}
                                                            >
                                                                {item.label}
                                                            </Tag>
                                                            {item.properties && item.properties.map((property, k) => (
                                                                <Tag
                                                                    closable={checkClose('edge')}
                                                                    color="default"
                                                                    key={property.key}
                                                                    onClick={() =>
                                                                        removeLabel('edge', item.label, k)}
                                                                >
                                                                    {property.key}={property.val}
                                                                </Tag>
                                                            ))}
                                                        </Col>
                                                    ))}
                                                </Row>
                                            );
                                        }}
                                    </Form.Item>
                                </Col>
                            </Row>
                        </Card.Grid>

                        <Card.Grid {...cardGridOption}>
                            <Form.Item name={'VAR'} noStyle valuePropName='checked'>
                                <Checkbox onChange={handleVar}>变量</Checkbox>
                            </Form.Item>
                        </Card.Grid>
                        <Card.Grid {...cardGridOption}>
                            <Form.Item name={'GREMLIN'} valuePropName='checked' noStyle>
                                <Checkbox onChange={handleGremlin}>GREMLIN</Checkbox>
                            </Form.Item>
                        </Card.Grid>
                        <Card.Grid {...cardGridOption}>
                            <Form.Item name={'TASK'} valuePropName='checked' noStyle>
                                <Checkbox onChange={handleTask}>任务</Checkbox>
                            </Form.Item>
                        </Card.Grid>
                    </Card>
                </Form.Item>
            </Form>
            <AddFieldLayer
                visible={addLayerVisible}
                onCancel={hideAddLayer}
                type={labelType}
                onChange={handleChangeLabel}
            />
        </Modal>
    );
};

export {EditLayer};
