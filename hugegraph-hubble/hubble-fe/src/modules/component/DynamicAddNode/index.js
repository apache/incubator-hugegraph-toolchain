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
 */

import React, {useCallback, useEffect, useState, useContext} from 'react';
import {Drawer, Select, Input, Button, message, Form, Row, Col} from 'antd';
import {useTranslation} from 'react-i18next';
import GraphAnalysisContext from '../../Context';
import * as rules from '../../../utils/rules';
import _ from 'lodash';
import c from './index.module.scss';
import * as api from '../../../api';

const DynamicAddNode = props => {
    const {
        open,
        onCancel,
        onOK,
        drawerInfo: vertexLists,
    } = props;

    const {t} = useTranslation();
    const {graphSpace: currentGraphSpace, graph: currentGraph} = useContext(GraphAnalysisContext);
    const [form] = Form.useForm();
    const idStrategyMappings = {
        PRIMARY_KEY: t('analysis.canvas.dynamic_add.primary_key_id'),
        AUTOMATIC: t('analysis.canvas.dynamic_add.automatic'),
        CUSTOMIZE_STRING: t('analysis.canvas.dynamic_add.customize_string'),
        CUSTOMIZE_NUMBER: t('analysis.canvas.dynamic_add.customize_number'),
        CUSTOMIZE_UUID: 'UUID',
    };

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
                        message.success(t('analysis.canvas.dynamic_add.add_success'));
                        onOK(data);
                    }
                });
                setSelectedVertexLabel();
                form.resetFields();
                onCancel();
            }).catch(e => {});
        },
        [currentGraph, currentGraphSpace, form, onCancel, onOK, t]
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
                    idRules = [rules.required(t('analysis.canvas.dynamic_add.required')), rules.isInt()];
                    break;
                case 'CUSTOMIZE_UUID':
                    idRules = [rules.required(t('analysis.canvas.dynamic_add.required')), rules.isUUID()];
                    break;
                case 'CUSTOMIZE_STRING':
                    idRules = [rules.required(t('analysis.canvas.dynamic_add.required'))];
                    break;
            }
            return idRules;
        },
        [t]
    );

    const renderForm = (formArr, isAllowNull) => {
        return (
            <>
                <Form.Item>
                    <Row>
                        <Col span={6} justify='end'>
                            {isAllowNull
                                ? `${t('analysis.canvas.dynamic_add.nullable_property')}:`
                                : `${t('analysis.canvas.dynamic_add.non_nullable_property')}:`}
                        </Col>
                        <Col span={9} justify='end'>{t('analysis.canvas.dynamic_add.property')}</Col>
                        <Col span={9} justify='end'>{t('analysis.canvas.dynamic_add.property_value')}</Col>
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
                                rules={!isAllowNull
                                    ? [rules.required(t('analysis.canvas.dynamic_add.required'))]
                                    : ''}
                            >
                                <Input
                                    placeholder={t('analysis.canvas.dynamic_add.property_placeholder')}
                                    maxLength={40}
                                />
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
            title={t('analysis.canvas.dynamic_add.add_vertex')}
            onClose={onDrawerClose}
            open={open}
            footer={[
                <Button
                    type="primary"
                    size="medium"
                    onClick={onAddNode}
                    key="add"
                >
                    {t('analysis.canvas.dynamic_add.add')}
                </Button>,
                <Button
                    size="medium"
                    onClick={onDrawerClose}
                    key="close"
                >
                    {t('analysis.canvas.dynamic_add.cancel')}
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
                    label={`${t('analysis.canvas.dynamic_add.vertex_type')}:`}
                    rules={[rules.required(t('analysis.canvas.dynamic_add.required'))]}
                >
                    <Select
                        size="medium"
                        trigger="click"
                        placeholder={t('analysis.canvas.dynamic_add.select_vertex_type')}
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
                                <Col span={6}>{t('analysis.canvas.dynamic_add.id_strategy')}:</Col>
                                <Col>  {selectedVertexLabel.id_strategy === 'PRIMARY_KEY'
                                    ? `${t('analysis.canvas.dynamic_add.primary_key')}-${
                                        selectedVertexLabel.primary_keys.join(',')
                                    }`
                                    : idStrategyMappings[selectedVertexLabel.id_strategy]}
                                </Col>
                            </Row>
                        </Form.Item>
                        {shouldRevealId && (
                            <Form.Item
                                label={`${t('analysis.canvas.dynamic_add.id_value')}:`}
                                name='id'
                                rules={validateIdField(selectedVertexLabel.id_strategy)}
                            >
                                <Input placeholder={t('analysis.canvas.dynamic_add.id_placeholder')} />
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
