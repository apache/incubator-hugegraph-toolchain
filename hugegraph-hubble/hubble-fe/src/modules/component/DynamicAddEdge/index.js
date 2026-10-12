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
 * @file  添加出入边
 */

import React, {useState, useCallback, useEffect, useContext} from 'react';
import {Drawer, Input, Button, message, Form, Select, Col, Row} from 'antd';
import {useTranslation} from 'react-i18next';
import GraphAnalysisContext from '../../Context';
import * as api from '../../../api/index';
import _ from 'lodash';
import c from './index.module.scss';

const AddEdgeDrawer = props => {
    const {
        open,
        onCancel,
        onOk,
        graphData,
        drawerInfo,
        isClickNew,
        isOutEdge,
    } =  props;

    const {t} = useTranslation();
    const {graphSpace: currentGraphSpace, graph: currentGraph} = useContext(GraphAnalysisContext);

    const [relatedGraphEdges, setRelatedEdges] = useState([]);
    const [edgeProperties, setEdgeProperties] = useState([]);
    const {vertices} = graphData || {};
    const nodesDataArr = _.cloneDeep(vertices);
    const {vertexId, vertexLabel} = drawerInfo || {};

    const [isShowMore, setShowMore] = useState(false);
    const [isAddDisabled, setIsAddDisabled] = useState(true);

    const [addForm] = Form.useForm();

    const resetStatus = useCallback(
        () => {
            setShowMore(false);
            setIsAddDisabled(true);
        },
        []
    );

    const onCloseDrawer = useCallback(
        () => {
            onCancel();
            addForm.resetFields();
            resetStatus();
        },
        [addForm, onCancel, resetStatus]
    );

    const getVertexlinks = useCallback(
        label => {
            api.analysis.fetchVertexlinks(currentGraphSpace, currentGraph, label)
                .then(res => {
                    const {data, status, message: errMsg} = res;
                    if (status === 200) {
                        setRelatedEdges(data);
                    }
                    if (status !== 200) {
                        !errMsg && message.error(t('analysis.canvas.dynamic_add.get_edge_type_failed'));
                    }
                });
        }, [currentGraph, currentGraphSpace, t]
    );

    useEffect(
        () => {
            open && !isClickNew && getVertexlinks(vertexLabel);
        },
        [getVertexlinks, isClickNew, open, vertexLabel]
    );

    const getEdgeLabels = useCallback(
        label => {
            api.analysis.fetchEdgeLabels(currentGraphSpace, currentGraph, label)
                .then(res => {
                    const {data, status, message: errMsg} = res;
                    if (status === 200) {
                        const {properties} = data;
                        setEdgeProperties(properties);
                    }
                    else {
                        !errMsg && message.error(t('analysis.canvas.dynamic_add.get_edge_property_failed'));
                    }
                });
        }, [currentGraph, currentGraphSpace, t]
    );

    const onChangeEdgeLabel  = useCallback(
        selectedEdge => {
            getEdgeLabels(selectedEdge);
            setShowMore(true);
        },
        [getEdgeLabels]
    );

    const onChangeNode = useCallback(
        id => {
            const label = isOutEdge ? 'source' : 'target';
            const selectedItem = nodesDataArr?.filter(
                item => (item.id === id)
            );
            getVertexlinks(selectedItem[0].label);
            setShowMore(false);
            addForm.resetFields();
            addForm.setFieldValue(label, id);
        },
        [addForm, getVertexlinks, isOutEdge, nodesDataArr]
    );

    const getSelectOptions = () => {
        return relatedGraphEdges.map(value => {
            return (
                <Select.Option
                    key={value}
                    value={value}
                >
                    {value}
                </Select.Option>
            );
        });
    };

    const getSelectNodeOptions = () => {
        return nodesDataArr?.map(item => {
            return (
                <Select.Option
                    key={item.id}
                    value={item.id}
                >
                    {item.id}
                </Select.Option>
            );
        });
    };

    const createEdge = useCallback(
        value => {
            let copyObj = _.clone(value);
            delete copyObj.label;
            let source;
            let target;
            if (isClickNew) {
                delete copyObj.target;
                delete copyObj.source;
                source = value.source;
                target = value.target;
            }
            else {
                isOutEdge ? delete copyObj.target : delete copyObj.source;
                source = isOutEdge ? vertexId : value.source;
                target = isOutEdge ? value.target : vertexId;
            }
            const edgeParams = {
                'label': value.label,
                'source': source,
                'target': target,
                'properties': copyObj,
            };
            api.analysis.addEdge(currentGraphSpace, currentGraph, edgeParams)
                .then(res => {
                    const {data, status, message: errMsg} = res;
                    if (status === 200) {
                        message.success(t('analysis.canvas.dynamic_add.save_success'));
                        onOk(data);
                    }
                    else {
                        !errMsg && message.error(t('analysis.canvas.dynamic_add.save_failed'));
                    }
                });
        }, [currentGraph, currentGraphSpace, onOk, isClickNew, isOutEdge, vertexId, t]
    );

    const onFormFinish = useCallback(
        value => {
            createEdge(value);
            onCloseDrawer();
        },
        [createEdge, onCloseDrawer]
    );

    const onValuesChange = useCallback(
        () => {
            addForm.validateFields()
                .then(() => {
                    setIsAddDisabled(false);
                })
                .catch(() => {
                    setIsAddDisabled(true);
                });
        },
        [addForm]
    );

    const onSaveEdit = useCallback(
        () => {
            addForm.submit();
        },
        [addForm]
    );

    const renderItem = (arr, isRequired) => {
        return arr.map(item => {
            return (
                <div key={item.name}>
                    <Form.Item
                        label={item.name}
                        colon
                        name={item.name}
                        rules={[{required: isRequired, message: t('analysis.canvas.dynamic_add.required')}]}
                        labelCol={{span: 9, offset: 6}}
                    >
                        <Input
                            placeholder={t('analysis.canvas.dynamic_add.property_placeholder')}
                            maxLength={40}
                        />
                    </Form.Item>
                </div>
            );
        });
    };

    const renderContent = (arr, isAllowNull) => {
        if (!_.isEmpty(arr)) {
            return (
                <>
                    <Row style={{marginBottom: '20px'}}>
                        <Col span={6} justify='end'>
                            {isAllowNull
                                ? `${t('analysis.canvas.dynamic_add.nullable_property')}:`
                                : `${t('analysis.canvas.dynamic_add.non_nullable_property')}:`}
                        </Col>
                        <Col span={9} justify='end'>{t('analysis.canvas.dynamic_add.property')}</Col>
                        <Col span={9} justify='end'>{t('analysis.canvas.dynamic_add.property_value')}</Col>
                    </Row>
                    {renderItem(arr, !isAllowNull)}
                </>
            );
        }
    };

    const renderPropsFormItem = () => {
        let allowNullArr = edgeProperties.filter(item => {
            return item.nullable;
        });
        let requiredArr = edgeProperties.filter(item => {
            return !item.nullable;
        });
        return (
            <>
                {renderContent(requiredArr, false)}
                {renderContent(allowNullArr, true)}
            </>
        );

    };

    return (
        <Drawer
            className={c.addEdgeDrawer}
            title={isOutEdge
                ? t('analysis.canvas.dynamic_add.add_out_edge')
                : t('analysis.canvas.dynamic_add.add_in_edge')}
            placement="right"
            onClose={onCloseDrawer}
            open={open}
            footer={[
                <Button
                    type="primary"
                    size="medium"
                    style={{width: 60}}
                    onClick={onSaveEdit}
                    key="save"
                    disabled={isAddDisabled}
                >
                    {t('analysis.canvas.dynamic_add.add')}
                </Button>,
                <Button
                    size="medium"
                    style={{width: 60}}
                    onClick={onCloseDrawer}
                    key="close"
                >
                    {t('analysis.canvas.dynamic_add.cancel')}
                </Button>,
            ]}
        >
            {!isClickNew && (
                <div
                    className={c.fields}
                    style={{marginBottom: 24}}
                >
                    <div>
                        {isOutEdge
                            ? t('analysis.canvas.dynamic_add.source')
                            : t('analysis.canvas.dynamic_add.target')}:
                    </div>
                    <div>{vertexId}</div>
                </div>)
            }

            <Form
                form={addForm}
                onFinish={onFormFinish}
                labelAlign='left'
                labelCol={{span: 6}}
                labelWrap
                onValuesChange={_.debounce(onValuesChange, 300)}
            >
                {isClickNew && (
                    <Form.Item
                        label={isOutEdge
                            ? `${t('analysis.canvas.dynamic_add.source')}:`
                            : `${t('analysis.canvas.dynamic_add.target')}:`}
                        name={isOutEdge ? 'source' : 'target'}
                        rules={[{required: true, message: t('analysis.canvas.dynamic_add.required')}]}
                    >
                        <Select
                            showSearch
                            placeholder={isOutEdge
                                ? t('analysis.canvas.dynamic_add.select_source')
                                : t('analysis.canvas.dynamic_add.select_target')}
                            onChange={onChangeNode}
                        >
                            {getSelectNodeOptions()}
                        </Select>
                    </Form.Item>)
                }
                <Form.Item
                    label={t('analysis.canvas.dynamic_add.edge_type')}
                    name={'label'}
                    rules={[{required: true, message: t('analysis.canvas.dynamic_add.select_edge_type')}]}
                >
                    <Select
                        placeholder={t('analysis.canvas.dynamic_add.select_edge_type')}
                        onChange={onChangeEdgeLabel}
                    >
                        {getSelectOptions()}
                    </Select>
                </Form.Item>
                {(isShowMore && (
                    <>
                        <Form.Item
                            label={isOutEdge
                                ? `${t('analysis.canvas.dynamic_add.target')}:`
                                : `${t('analysis.canvas.dynamic_add.source')}:`}
                            name={isOutEdge ? 'target' : 'source'}
                            rules={[{required: true, message: t('analysis.canvas.dynamic_add.required')}]}
                        >
                            <Input
                                placeholder={isOutEdge
                                    ? t('analysis.canvas.dynamic_add.select_target')
                                    : t('analysis.canvas.dynamic_add.select_source')}
                            />
                        </Form.Item>
                    </>
                ))}
                {isShowMore && renderPropsFormItem()}
            </Form>
        </Drawer>
    );
};

export default AddEdgeDrawer;
