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
 * @file  添加节点
 * @author
 */

import React, {useCallback, useEffect, useState, useContext} from 'react';
import {Drawer, Select, Input, Button, message, Form, Row, Col} from 'antd';
import GraphAnalysisContext from '../../Context';
import * as rules from '../../../utils/rules';
import _ from 'lodash';
import c from './index.module.scss';
import * as api from '../../../api';

const IDStrategyMappings = {
    PRIMARY_KEY: '主键ID',
    AUTOMATIC: '自动生成',
    CUSTOMIZE_STRING: '自定义字符串',
    CUSTOMIZE_NUMBER: '自定义数字',
    CUSTOMIZE_UUID: '自定义UUID',
};

const DynamicAddNode = props => {
    const {
        open,
        onCancel,
        onOK,
        drawerInfo: vertexLists,
    } = props;

    const {graphSpace: currentGraphSpace, graph: currentGraph} = useContext(GraphAnalysisContext);
    const [form] = Form.useForm();

    const [selectedVertexLabel, setSelectedVertexLabel] = useState();
    const [nonNullableProperties, setNonNullableProperties] = useState();
    const [nullableProperties, setNullableProperties] = useState();

    const shouldRevealId = selectedVertexLabel
    && selectedVertexLabel.id_strategy !== 'PRIMARY_KEY'
    && selectedVertexLabel.id_strategy !== 'AUTOMATIC';

    useEffect(
        () => {
            const nullableProperties = [];
            const nonNullableProperties = [];
            if (!_.isUndefined(selectedVertexLabel)) {
                selectedVertexLabel?.properties.forEach(
                    item => {
                        if (item.nullable) {
                            nullableProperties.push(item.name);
                        }
                        else {
                            nonNullableProperties.push(item.name);
                        }
                    }
                );
            }
            setNonNullableProperties(nonNullableProperties);
            setNullableProperties(nullableProperties);
        },
        [selectedVertexLabel]
    );

    const onDrawerClose = useCallback(
        () => {
            setSelectedVertexLabel();
            form.resetFields();
            onCancel();
        },
        [form, onCancel]
    );

    const onAddNode = useCallback(
        () => {
            form.validateFields().then(values => {
                const params = {
                    properties: values.properties || {},
                    ...values,
                };
                api.analysis.addGraphNode(currentGraphSpace, currentGraph, params).then(res => {
                    const {status, data} = res;
                    if (status === 200) {
                        message.success('添加成功');
                        onOK(data);
                    }
                });
                setSelectedVertexLabel();
                form.resetFields();
                onCancel();
            }).catch(e => {});
        },
        [currentGraph, currentGraphSpace, form, onCancel, onOK]
    );

    const onVertexTypeChange = useCallback(
        value => {
            const selectedVertexLabel = _.find(vertexLists, item => {
                return item.name === value;
            });
            setSelectedVertexLabel({...selectedVertexLabel});
            form.resetFields();
            form.setFieldValue('label', value);
        },
        [form, vertexLists]
    );

    const validateIdField = useCallback(
        idStrategy => {
            let idRules;
            switch (idStrategy) {
                case 'CUSTOMIZE_NUMBER':
                    idRules = [rules.required(), rules.isInt()];
                    break;
                case 'CUSTOMIZE_UUID':
                    idRules = [rules.required(), rules.isUUID()];
                    break;
                case 'CUSTOMIZE_STRING':
                    idRules = [rules.required()];
                    break;
            }
            return idRules;
        },
        []
    );

    const renderForm = (formArr, isAllowNull) => {
        return (
            <>
                <Form.Item>
                    <Row>
                        <Col span={6} justify='end'>{isAllowNull ? '可空属性:' : '不可空属性:'}</Col>
                        <Col span={9} justify='end'>属性</Col>
                        <Col span={9} justify='end'>属性值</Col>
                    </Row>
                </Form.Item>
                {
                    formArr.map(item => {
                        return (
                            <Form.Item
                                key={item}
                                name={['properties', item]}
                                label={item}
                                labelCol={{span: 9, offset: 6}}
                                rules={!isAllowNull ? [rules.required()] : ''}
                            >
                                <Input placeholder='请输入属性值' maxLength={40} />
                            </Form.Item>
                        );
                    })
                }
            </>
        );
    };

    return (
        <Drawer
            className={c.addNodeDrawer}
            title="添加顶点"
            onClose={onDrawerClose}
            open={open}
            footer={[
                <Button
                    type="primary"
                    size="medium"
                    onClick={onAddNode}
                    key="add"
                >
                    添加
                </Button>,
                <Button
                    size="medium"
                    onClick={onDrawerClose}
                    key="close"
                >
                    取消
                </Button>,
            ]}
        >
            <Form
                form={form}
                labelAlign='left'
                labelCol={{span: 6}}
                colon={false}
                labelWrap
            >
                <Form.Item
                    name='label'
                    label='顶点类型:'
                    rules={[rules.required()]}
                >
                    <Select
                        size="medium"
                        trigger="click"
                        placeholder='请选择顶点类型'
                        options={vertexLists.map(
                            ({name}) => {
                                return {
                                    label: name,
                                    value: name,
                                };
                            }
                        )}
                        onChange={onVertexTypeChange}
                    />
                </Form.Item>
                {selectedVertexLabel && (
                    <>
                        <Form.Item>
                            <Row>
                                <Col span={6}>ID策略：</Col>
                                <Col>  {selectedVertexLabel.id_strategy === 'PRIMARY_KEY'
                                    ? `主键属性-${selectedVertexLabel.primary_keys.join('，')}`
                                    : IDStrategyMappings[selectedVertexLabel.id_strategy]}
                                </Col>
                            </Row>
                        </Form.Item>
                        {shouldRevealId && (
                            <Form.Item
                                label='ID值:'
                                placeholder="请输入ID值"
                                name='id'
                                rules={validateIdField(selectedVertexLabel.id_strategy)}
                            >
                                <Input />
                            </Form.Item>
                        )}
                        {!_.isEmpty(nullableProperties) && renderForm(nullableProperties, true)}
                        {!_.isEmpty(nonNullableProperties) && renderForm(nonNullableProperties, false)}
                    </>
                )
                }
            </Form>
        </Drawer>
    );
};

export default DynamicAddNode;
