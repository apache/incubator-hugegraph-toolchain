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

import {Modal, Form, Input, Select, Row, Col, Checkbox, message, Spin, Radio} from 'antd';
import {useCallback, useEffect, useState} from 'react';
import * as api from '../../../api';
import * as rules from '../../../utils/rules';
import {
    edgeSizeSchemas,
    edgeShapeSchemas,
} from '../common/config';
import {InputColorSelect} from '../../../components/ColorSelect';
import RelateProperty from '../common/RelateProperty';
import RelatePropertyIndex from '../common/RelatePropertyIndex';

const defaultDisplayFields = {label: '边类型', value: '~id'};

const EditEdgeLayer = ({visible, onCancle, graphspace, graph, refresh, name, propertyList, vertexList}) => {
    const [form] = Form.useForm();
    const [linkMulti, setLinkMulti] = useState(false);
    const [selectedPropertyList, setSelectedPropertyList] = useState([]);
    const [existProperties, setExistProperties] = useState([]);
    const [existPropertyIndex, setExistPropertyIndex] = useState([]);
    const [spinning, setSpinning] = useState(false);
    const [loading, setLoading] = useState(false);
    const [edgeLabelType, setEdgeLabelType] = useState('NORMAL');
    const [parentEdgeLabelList, setParentEdgeLabelList] = useState([]);

    const selectProperty = useCallback(() => {
        const attr = form.getFieldValue('properties');
        const tmp = [];
        const exist = [];
        form.validateFields(['properties']);
        for (let item of attr) {
            if (item !== undefined && item.name && !exist.includes(item.name)) {
                tmp.push({...item, label: item.name, value: item.name});
            }
            exist.push(item.name);
        }
        setSelectedPropertyList(tmp);
    }, [form]);

    const removeProperty = useCallback(() => {
        // const tmp = [...selectedPropertyList];
        // tmp.splice(index, 1);
        const {attr} = form.getFieldValue('properties');
        const tmp = [];
        for (let item of attr) {
            tmp.push({...item, label: item.name, value: item.name});
        }
        setSelectedPropertyList(tmp);
    }, [form]);

    const addEdge = useCallback(data => {

        setLoading(true);
        api.manage.addMetaEdge(graphspace, graph, data).then(res => {
            setLoading(false);
            if (res.status === 200) {
                message.success('添加成功');
                onCancle();
                refresh();
                return;
            }

            message.error(res.message);
        });
    }, [graphspace, graph, onCancle, refresh]);

    const updateEdge = useCallback((name, data) => {
        const {style, append_properties, remove_property_indexes, append_property_indexes} = data;

        setLoading(true);
        api.manage.updateMetaEdge(graphspace, graph, name, {
            style,
            append_properties,
            remove_property_indexes,
            append_property_indexes,
        }).then(res => {
            setLoading(false);
            if (res.status === 200) {
                message.success('更新成功');
                onCancle();
                refresh();
                return;
            }

            message.error(res.message);
        });
    }, [graph, graphspace, onCancle, refresh]);

    const handleEdgeLabelType = useCallback(e => {
        setEdgeLabelType(e.target.value);
    }, []);

    const handleLinkMulti = useCallback(e => setLinkMulti(e.target.checked), []);

    const onFinish = useCallback(() => {
        form.validateFields().then(values => {
            // submitForm();
            if (name) {
                updateEdge(name, values);
                return;
            }

            addEdge(values);
        }).catch(e => {
            return;
        });
    }, [form, name, updateEdge, addEdge]);

    useEffect(() => {
        if (!visible) {
            return;
        }

        form.resetFields();
        setLinkMulti(false);
        setSelectedPropertyList([]);
        setExistProperties([]);
        setExistPropertyIndex([]);
        setParentEdgeLabelList([]);
        setEdgeLabelType('NORMAL');

        if (!name) {
            return;
        }

        setSpinning(true);
        api.manage.getMetaEdge(graphspace, graph, name).then(res => {
            if (res.status === 200) {
                const {properties, property_indexes, link_multi_times, edgelabel_type} = res.data;

                form.setFieldsValue(res.data);
                // form.setFields([{name: 'properties', value: properties}]);

                setSelectedPropertyList(properties.map(item => ({
                    ...item,
                    label: item.name,
                    value: item.name,
                })));
                setLinkMulti(link_multi_times);
                setExistProperties(properties);
                setExistPropertyIndex(property_indexes);
                setSpinning(false);
                setEdgeLabelType(edgelabel_type);
            }
        });
    }, [visible, name, form, graph, graphspace]);

    useEffect(() => {
        if (!visible) {
            return;
        }

        api.manage.getMetaEdgeList(graphspace, graph, {page_size: -1, edgelabel_type: 'PARENT'}).then(res => {
            if (res.status === 200) {
                const {records} = res.data;
                setParentEdgeLabelList(records);
            }
        });
    }, [visible, graph, graphspace]);

    return (
        <Modal
            title={name ? '编辑边' : '创建边'}
            open={visible}
            onCancel={onCancle}
            onClose={onCancle}
            onOk={onFinish}
            confirmLoading={loading}
            width={600}
            destroyOnClose
        >
            <Spin spinning={spinning}>
                <Form
                    form={form}
                    labelCol={{span: 6}}
                    initialValues={{
                        open_label_index: false,
                        link_multi_times: false,
                        style: {
                            color: '#5c73e6',
                            thickness: 'NORMAL',
                            with_arrow: true,
                            display_fields: ['~id'],
                        },
                        edgelabel_type: 'NORMAL',
                    }}
                >
                    <Form.Item label='边类型名称：' name='name' rules={[rules.required()]}>
                        <Input placeholder='允许出现中英文、数字、下划线' disabled={!!name} />
                    </Form.Item>
                    <Form.Item label='类型：' name='edgelabel_type' rules={[rules.required()]}>
                        <Radio.Group
                            options={[
                                {label: '普通类型', value: 'NORMAL'},
                                {label: '父边类型', value: 'PARENT'},
                                {label: '子边类型', value: 'SUB'},
                            ]}
                            optionType='button'
                            onChange={handleEdgeLabelType}
                            disabled={!!name}
                        />
                    </Form.Item>
                    {edgeLabelType === 'SUB' && (
                        <Form.Item
                            label='父边类型：'
                            name='parent_label'
                            rules={[rules.required()]}
                            wrapperCol={{span: 8}}
                        >
                            <Select
                                placeholder='请选择父边'
                                options={parentEdgeLabelList.map(item => ({
                                    label: item.name,
                                    value: item.name,
                                }))}
                                disabled={!!name}
                            />
                        </Form.Item>
                    )}
                    {edgeLabelType !== 'PARENT' && (
                        <>
                            <Form.Item label='顶点样式：'>
                                <Row gutter={[12, 24]}>
                                    <Col>
                                        <Form.Item
                                            wrapperCol={{span: 10}}
                                            name={['style', 'color']}
                                            rules={[rules.required()]}
                                            style={{marginBottom: 0}}
                                        >
                                            <InputColorSelect />
                                        </Form.Item>
                                    </Col>
                                    <Col>
                                        <Form.Item
                                            wrapperCol={{span: 10}}
                                            name={['style', 'with_arrow']}
                                            rules={[rules.required()]}
                                            style={{marginBottom: 0}}
                                        >
                                            <Select
                                                style={{width: 66}}
                                                size="medium"
                                            >
                                                {edgeShapeSchemas.map(item => {
                                                    return (
                                                        <Select.Option key={item.blackicon} value={item.flag}>
                                                            <img src={item.blackicon} />
                                                        </Select.Option>
                                                    );
                                                })}
                                            </Select>
                                        </Form.Item>
                                    </Col>
                                    <Col>
                                        <Form.Item
                                            wrapperCol={{span: 10}}
                                            name={['style', 'thickness']}
                                            rules={[rules.required()]}
                                            style={{marginBottom: 0}}
                                        >
                                            <Select
                                                style={{width: 66}}
                                                size="medium"
                                                options={edgeSizeSchemas}
                                            />
                                        </Form.Item>
                                    </Col>
                                </Row>
                            </Form.Item>
                            <Form.Item label='起点类型' name='source_label' rules={[rules.required()]}>
                                <Select options={vertexList} disabled={!!name} />
                            </Form.Item>
                            <Form.Item label='终点类型' name='target_label' rules={[rules.required()]}>
                                <Select options={vertexList} disabled={!!name} />
                            </Form.Item>
                            <Form.Item label='允许多次连接' name='link_multi_times' valuePropName='checked'>
                                <Checkbox onChange={handleLinkMulti} disabled={!!name} />
                            </Form.Item>
                            <Form.Item label='关联属性' required={linkMulti === 'PRIMARY_KEY'}>
                                <RelateProperty
                                    propertyList={propertyList}
                                    selectProperty={selectProperty}
                                    removeProperty={removeProperty}
                                    exist={existProperties}
                                    isEdit={!!name}
                                />
                            </Form.Item>
                            {linkMulti && (
                                <Form.Item label='区分键' name='sort_keys' rules={[rules.required()]}>
                                    <Select
                                        options={selectedPropertyList.filter(item => !item.nullable)}
                                        mode='multiple'
                                        disabled={!!name}
                                    />
                                </Form.Item>
                            )}
                            <Form.Item label='边展示内容' name={['style', 'display_fields']} rules={[rules.required()]}>
                                <Select
                                    // eslint-disable-next-line max-len
                                    options={selectedPropertyList.filter(item => !item.nullable).concat(defaultDisplayFields)}
                                    mode='multiple'
                                />
                            </Form.Item>
                            <Form.Item label='类型索引' name='open_label_index' valuePropName='checked'>
                                <Checkbox disabled={!!name} />
                            </Form.Item>
                            <Form.Item label='属性索引'>
                                <RelatePropertyIndex
                                    selectedPropertyList={selectedPropertyList}
                                    propertyList={propertyList}
                                    exist={existPropertyIndex}
                                    isEdit={!!name}
                                />
                            </Form.Item>
                        </>
                    )}
                </Form>
            </Spin>
        </Modal>
    );
};

export {EditEdgeLayer};
