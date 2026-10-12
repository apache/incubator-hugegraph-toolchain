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
 * @file  点边属性编辑
 */

import React, {useState, useCallback, useContext} from 'react';
import {Drawer, Input, Button, message, Form, Row, Col, Tag} from 'antd';
import {UpOutlined, DownOutlined} from '@ant-design/icons';
import {useTranslation} from 'react-i18next';
import {GraphContext} from '../Context';
import GraphAnalysisContext from '../../Context';
import {EDGE_TYPE, EDGELABEL_TYPE} from '../../../utils/constants';
import * as api from '../../../api/index';
import _ from 'lodash';
import c from './index.module.scss';
import classnames from 'classnames';
import KeyboardAction from '../../../components/KeyboardAction';

const SELECTED_TYPE = {EDGE: 'EDGE', VERTEX: 'VERTEX'};

const EditElement = props => {
    const {
        show,
        cancel,
        drawerInfo,
        edgeMeta,
        onChange,
    } =  props;

    const {t} = useTranslation();
    const {graph} = useContext(GraphContext);
    const {graphSpace: currentGraphSpace, graph: currentGraph} = useContext(GraphAnalysisContext);
    const {type, id, itemType, properties, metaConfig} = drawerInfo || {};
    const {edgelabel_type, parent_label} = metaConfig || {};
    const {children} = _.find(edgeMeta, {name: parent_label}) || {};

    const elementType = EDGE_TYPE.includes(type) ? SELECTED_TYPE.EDGE : SELECTED_TYPE.VERTEX;
    const elementTypeLabel = elementType === SELECTED_TYPE.EDGE
        ? t('analysis.canvas.edit_element.edge')
        : t('analysis.canvas.edit_element.vertex');
    const showEdgeLabelExpand =  elementType === SELECTED_TYPE.EDGE && !_.isEmpty(children);

    const [vertexNullableProps, setVertexNullableProps] = useState();
    const [vertexNonNullableProps, setVertexNonNullableProps] = useState();
    const [vertexPrimaryKeys, setVertexPrimaryKeys] = useState();
    const [edgeNullableProps, setEdgeNullableProps] = useState();
    const [edgeNonNullableProps, setEdgeNonNullableProps] = useState();
    const [isExpandEdgeLabelType, setExpandEdgeLabelType] = useState(false);
    const [isEdit, setSwitchEdit] = useState(false);

    const [editForm] = Form.useForm();
    const graphInfoItemClassName = classnames(
        c.detailItem,
        {[c.disabled]: isEdit}
    );

    const handleDrawerClose = useCallback(
        () => {
            editForm.resetFields();
            setSwitchEdit(cancel);
        },
        [cancel, editForm]
    );

    const updateEdgePropertReq = useCallback(
        value => {
            const edgeParams = {
                id: id,
                label: itemType,
                properties: value,
                source: drawerInfo?.source,
                target: drawerInfo?.target,
            };
            api.analysis.updateEdgeProperties(currentGraphSpace, currentGraph, edgeParams)
                .then(res => {
                    const {status, message: errMsg} = res;
                    if (status === 200) {
                        message.success(t('analysis.canvas.edit_element.save_success'));
                        const edgeItem  = graph.findById(id);
                        graph.updateItem(edgeItem, edgeParams, false);
                        onChange('edges', edgeItem, edgeParams);
                    }
                    else {
                        !errMsg && message.error(t('analysis.canvas.edit_element.save_failed'));
                    }
                });
        },
        [currentGraph, currentGraphSpace, drawerInfo?.source, drawerInfo?.target, graph, id, itemType, onChange, t]
    );

    const updateVertexPropertReq = useCallback(
        value => {
            const vertexParams = {
                id: id,
                label: itemType,
                properties: value,
            };
            api.analysis.updateVertexProperties(currentGraphSpace, currentGraph, vertexParams)
                .then(res => {
                    const {status, message: errMsg} = res;
                    if (status === 200) {
                        message.success(t('analysis.canvas.edit_element.save_success'));
                        const nodeItem  = graph.findById(id);
                        const vertexModel = Object.assign({}, vertexParams, {label: id});
                        graph.updateItem(nodeItem, vertexModel, false);
                        const itemProperties = Object.assign({}, vertexParams.properties,
                            {[vertexPrimaryKeys]: properties[vertexPrimaryKeys]});
                        const itemParams =  Object.assign({}, vertexParams, {properties: itemProperties});
                        onChange('nodes', nodeItem, itemParams);
                    }
                    else {
                        !errMsg && message.error(t('analysis.canvas.edit_element.save_failed'));
                    }
                });
        },
        [currentGraph, currentGraphSpace, graph, id, itemType, onChange, properties, t, vertexPrimaryKeys]
    );

    const onFormFinish = useCallback(
        value => {
            if (elementType === SELECTED_TYPE.EDGE) {
                updateEdgePropertReq(value);
            }
            else {
                updateVertexPropertReq(value);
            }
            isEdit && handleDrawerClose();
        },
        [elementType, isEdit, handleDrawerClose, updateEdgePropertReq, updateVertexPropertReq]
    );

    const handleChangeVisible = useCallback(() => {
        setExpandEdgeLabelType(pre => !pre);
    }, [setExpandEdgeLabelType]);

    const renderDrawer = () => {
        const propertyMap = Object.entries(properties);
        return (
            <>
                <div className={c.fields}>
                    <div> {elementTypeLabel} {t('analysis.canvas.edit_element.type')}:</div>
                    <div>{itemType}</div>
                </div>
                {edgelabel_type === EDGELABEL_TYPE.SUB && (
                    <>
                        <div className={c.fields}>
                            <div> {t('analysis.canvas.edit_element.edge_relation')}:</div>
                            <KeyboardAction
                                onAction={handleChangeVisible}
                                aria-expanded={isExpandEdgeLabelType}
                            >
                                {showEdgeLabelExpand && (isExpandEdgeLabelType ? <UpOutlined /> : <DownOutlined />)}
                                {parent_label}
                                <Tag color='blue' style={{marginLeft: '10px'}}>
                                    {t('analysis.canvas.edit_element.parent_edge')}
                                </Tag>
                            </KeyboardAction>
                        </div>
                        <div className={c.fieldWithoutName}>
                            {isExpandEdgeLabelType && children?.map(item =>
                                (
                                    <div key={item}>{item}
                                        <Tag color='gold' style={{marginLeft: '10px'}}>
                                            {t('analysis.canvas.edit_element.child_edge')}
                                        </Tag>
                                    </div>
                                ))}
                        </div>
                    </>
                )}
                <div className={c.fields}>
                    <div> {elementTypeLabel} {t('analysis.canvas.edit_element.id')}:</div>
                    <div>{id} </div>
                </div>
                {elementType === SELECTED_TYPE.EDGE && (
                    <>
                        <div className={c.fields}>
                            <div>{t('analysis.canvas.edit_element.source')}</div>
                            <div>{drawerInfo.source} </div>
                        </div>
                        <div className={c.fields}>
                            <div>{t('analysis.canvas.edit_element.target')}</div>
                            <div>{drawerInfo.target} </div>
                        </div>
                    </>
                )}
                {propertyMap.map(item => {
                    const [key, value] = item;
                    let labelText;
                    if (elementType === SELECTED_TYPE.VERTEX) {
                        labelText = vertexPrimaryKeys?.includes(key)
                            ? `${key}(${t('analysis.canvas.edit_element.primary_key')})`
                            : key;
                    }
                    else {
                        labelText = key;
                    }
                    return (
                        <div className={c.fields} key={_.uniqueId()}>
                            <div>{labelText}:</div>
                            <div>{value?.toString()}</div>
                        </div>
                    );
                })}
            </>
        );
    };

    const fetchEdgePropertReq = useCallback(
        async () => {
            const response = await api.analysis.getEdgeProperties(currentGraphSpace, currentGraph, itemType);
            const {
                message: edgePropertiessMessage,
                data,
                status,
            } = response;
            if (status === 200) {
                const {nullableProps, nonNullableProps} = data;
                setEdgeNonNullableProps(nonNullableProps);
                setEdgeNullableProps(nullableProps);
            }
            if (status !== 200 && !edgePropertiessMessage) {
                message.error(t('analysis.canvas.edit_element.get_edge_property_failed'));
            }
        },
        [currentGraph, currentGraphSpace, itemType, t]
    );


    const fetchVertexPropertReq = useCallback(
        async () => {
            const response = await api.analysis.getVertexProperties(currentGraphSpace, currentGraph, itemType);
            const {
                message: vertexPropertiessMessage,
                data,
                status,
            } = response;
            if (status === 200) {
                const {nullableProps, nonNullableProps, primaryKeys} = data;
                setVertexPrimaryKeys(primaryKeys);
                setVertexNullableProps(nullableProps);
                setVertexNonNullableProps(nonNullableProps);
            }
            if (status !== 200 && !vertexPropertiessMessage) {
                message.error(t('analysis.canvas.edit_element.get_vertex_property_failed'));
            }
        },
        [currentGraph, currentGraphSpace, itemType, t]
    );

    const getVertexorEdgeProperties = useCallback(
        () => {
            if (elementType === SELECTED_TYPE.EDGE) {
                fetchEdgePropertReq();
            }
            else {
                fetchVertexPropertReq();
            }
        },
        [fetchEdgePropertReq, fetchVertexPropertReq, elementType]
    );

    const onSaveEdit = useCallback(
        async () => {
            if (isEdit) {
                editForm.submit();
            }
            else {
                setSwitchEdit(true);
                getVertexorEdgeProperties();
            }
        },
        [editForm, getVertexorEdgeProperties, isEdit]
    );

    const renderItem = (propsArr, isRequired) => {
        return Object.entries(properties)
            .map(([key, value]) => {
                let itemDom;
                if (propsArr.includes(key)) {
                    if (elementType === SELECTED_TYPE.VERTEX && vertexPrimaryKeys?.includes(key)) {
                        itemDom = (
                            <Row className={graphInfoItemClassName} key={key}>
                                <Col span={9}>{key}({t('analysis.canvas.edit_element.primary_key')}):</Col>
                                <Col>{value?.toString()}</Col>
                            </Row>
                        );
                    }
                    else {
                        editForm.setFieldValue(key, value);
                        itemDom = (
                            <Form.Item
                                key={key}
                                label={key}
                                colon
                                name={key}
                                rules={[{required: isRequired, message: t('analysis.canvas.edit_element.required')}]}
                                labelCol={{span: 9}}
                            >
                                <Input
                                    placeholder={t('analysis.canvas.edit_element.property_placeholder')}
                                    maxLength={40}
                                />
                            </Form.Item>
                        );
                    }
                    return itemDom;
                }
            });
    };

    const getIntersection  = (obj, arr) => {
        return _.intersection(Object.keys(obj), arr);
    };

    const renderForm = (arr, isAllowNull) => {
        if (_.size(arr) > 0 && !_.isEmpty(getIntersection(properties, arr))) {
            return (
                <>
                    <div style={{margin: '8px 0'}}>
                        {isAllowNull
                            ? t('analysis.canvas.edit_element.nullable_property')
                            : t('analysis.canvas.edit_element.non_nullable_property')}:
                    </div>
                    {renderItem(arr, !isAllowNull)}
                </>
            );
        }
    };

    const renderEditForm = () => {
        return (
            <>
                {elementType === SELECTED_TYPE.VERTEX && (
                    <>
                        {renderForm(vertexNullableProps, true)}
                        {renderForm(vertexNonNullableProps, false)}
                    </>
                )}
                {elementType === SELECTED_TYPE.EDGE && (
                    <>
                        {renderForm(edgeNullableProps, true)}
                        {renderForm(edgeNonNullableProps, false)}
                    </>
                )}
            </>
        );
    };

    return (
        <Drawer
            title={isEdit
                ? t('analysis.canvas.edit_element.edit_details')
                : t('analysis.canvas.edit_element.data_details')}
            placement="right"
            onClose={handleDrawerClose}
            open={show}
            footer={[
                <Button
                    type="primary"
                    size="medium"
                    style={{width: 60}}
                    onClick={onSaveEdit}
                    key="save"
                >
                    {isEdit
                        ? t('analysis.canvas.edit_element.save')
                        : t('analysis.canvas.edit_element.edit')}
                </Button>,
                <Button
                    size="medium"
                    style={{width: 60}}
                    onClick={handleDrawerClose}
                    key="close"
                >
                    {t('analysis.canvas.edit_element.close')}
                </Button>,
            ]}
        >
            {!_.isEmpty(drawerInfo) && !isEdit && renderDrawer()}
            <div className={c.graphNodeInfo}>
                {isEdit && (
                    <>
                        {elementType === SELECTED_TYPE.EDGE ? (
                            <>
                                <Row className={graphInfoItemClassName}>
                                    <Col span={9}>{t('analysis.canvas.edit_element.edge_type')}:</Col>
                                    <Col>{itemType}</Col>
                                </Row>
                                {edgelabel_type === EDGELABEL_TYPE.SUB && (
                                    <Row className={graphInfoItemClassName}>
                                        <Col span={9}>{t('analysis.canvas.edit_element.edge_relation')}:</Col>
                                        <Col>{showEdgeLabelExpand && <UpOutlined />}</Col>
                                        <Col>
                                            <span>{parent_label}
                                                <Tag color='blue' style={{marginLeft: '10px'}}>
                                                    {t('analysis.canvas.edit_element.parent_edge')}
                                                </Tag>
                                            </span>
                                        </Col>
                                    </Row>
                                )
                                }
                                {children?.map(item => (
                                    <Row key={item} className={graphInfoItemClassName}>
                                        <Col offset={9}>{item}
                                            <Tag color={'gold'} style={{marginLeft: '10px'}}>
                                                {t('analysis.canvas.edit_element.child_edge')}
                                            </Tag>
                                        </Col>
                                    </Row>
                                ))}
                                <Row className={graphInfoItemClassName}>
                                    <Col span={9}>{t('analysis.canvas.edit_element.edge_id')}:</Col>
                                    <Col>{id}</Col>
                                </Row>
                                <Row className={graphInfoItemClassName}>
                                    <Col span={9}>{t('analysis.canvas.edit_element.source')}:</Col>
                                    <Col>{drawerInfo.source}</Col>
                                </Row>
                                <Row className={graphInfoItemClassName}>
                                    <Col span={9}>{t('analysis.canvas.edit_element.target')}:</Col>
                                    <Col>{drawerInfo.target}</Col>
                                </Row>
                            </>
                        ) : (
                            <>
                                <Row className={graphInfoItemClassName}>
                                    <Col span={9}>{t('analysis.canvas.edit_element.vertex_type')}:</Col>
                                    <Col>{itemType}</Col>
                                </Row>
                                <Row className={graphInfoItemClassName}>
                                    <Col span={9}>{t('analysis.canvas.edit_element.vertex_id')}:</Col>
                                    <Col>{id}</Col>
                                </Row>
                            </>
                        )}
                    </>
                )}
                {isEdit && (
                    <>
                        <Form
                            className={c.editForm}
                            form={editForm}
                            onFinish={onFormFinish}
                            labelAlign='left'
                            labelWrap
                        >
                            {renderEditForm()}
                        </Form>
                    </>)
                }
            </div>
        </Drawer>
    );
};

export default EditElement;
