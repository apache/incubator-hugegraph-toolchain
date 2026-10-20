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

import {useState, useCallback, useEffect, useContext, useRef, useId} from 'react';
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
    const formId = useId();
    const {graphSpace: currentGraphSpace, graph: currentGraph} = useContext(GraphAnalysisContext);

    const [relatedGraphEdges, setRelatedEdges] = useState([]);
    const [edgeProperties, setEdgeProperties] = useState([]);
    const {vertices} = graphData || {};
    const nodesDataArr = vertices;
    const {vertexId, vertexLabel} = drawerInfo || {};

    const [isShowMore, setShowMore] = useState(false);

    const [addForm] = Form.useForm();
    const generation = useRef(0);
    const liveScope = useRef(null);
    liveScope.current = {currentGraph, currentGraphSpace, open, vertexId, vertexLabel, isClickNew, isOutEdge};
    const linksRequest = useRef(0);
    const propertiesRequest = useRef(0);
    const [pending, setPending] = useState(false);
    const submitting = useRef(false);
    useEffect(() => {
        generation.current += 1;
        submitting.current = false;
        setPending(false);
        setRelatedEdges([]);
        setEdgeProperties([]);
        setShowMore(false);
        if (open) addForm.resetFields();
        return () => { generation.current += 1; };
    }, [addForm, currentGraph, currentGraphSpace, open, vertexId, vertexLabel, isClickNew, isOutEdge]);

    const resetStatus = useCallback(() => setShowMore(false), []);

    const finishClose = useCallback(
        ({writeCompleted = false} = {}) => {
            generation.current += 1;
            // A completed write stays locked until the owner closes/reopens
            // this drawer, even if applying it to the local canvas failed.
            if (!writeCompleted) {
                submitting.current = false;
                setPending(false);
            }
            addForm.resetFields();
            resetStatus();
            return onCancel?.();
        },
        [addForm, onCancel, resetStatus]
    );

    const onCloseDrawer = useCallback(() => {
        // Closing a drawer does not undo a submitted server write. Keep its
        // completion callback attached until success or failure is known.
        if (!submitting.current) return finishClose();
    }, [finishClose]);

    const getVertexlinks = useCallback(async label => {
        if (!open) return;
        const token = generation.current;
        const owner = liveScope.current;
        const request = ++linksRequest.current;
        const current = () => token === generation.current && request === linksRequest.current
            && Object.keys(owner).every(key => owner[key] === liveScope.current[key]);
        try {
            const {data, status, message: errMsg} = await api.analysis.fetchVertexlinks(
                currentGraphSpace, currentGraph, label);
            if (!current()) return;
            if (status === 200) setRelatedEdges(data);
            else if (!errMsg) message.error(t('analysis.canvas.dynamic_add.get_edge_type_failed'));
        }
        catch { if (current()) message.error(t('analysis.canvas.dynamic_add.get_edge_type_failed')); }
    }, [currentGraph, currentGraphSpace, open, t]);

    useEffect(
        () => {
            open && !isClickNew && getVertexlinks(vertexLabel);
        },
        [getVertexlinks, isClickNew, open, vertexLabel]
    );

    const getEdgeLabels = useCallback(async label => {
        if (!open) return;
        const token = generation.current;
        const owner = liveScope.current;
        const request = ++propertiesRequest.current;
        const current = () => token === generation.current && request === propertiesRequest.current
            && Object.keys(owner).every(key => owner[key] === liveScope.current[key]);
        try {
            const {data, status, message: errMsg} = await api.analysis.fetchEdgeLabels(
                currentGraphSpace, currentGraph, label);
            if (!current()) return;
            if (status === 200) setEdgeProperties(data.properties);
            else if (!errMsg) message.error(t('analysis.canvas.dynamic_add.get_edge_property_failed'));
        }
        catch { if (current()) message.error(t('analysis.canvas.dynamic_add.get_edge_property_failed')); }
    }, [currentGraph, currentGraphSpace, open, t]);

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

    const getSelectOptions = () => relatedGraphEdges.map(value => ({value, label: value}));

    const getSelectNodeOptions = () => nodesDataArr?.map(item => ({value: item.id, label: item.id}));

    const createEdge = useCallback(
        async value => {
            if (!open || submitting.current) return;
            const token = generation.current;
            const owner = liveScope.current;
            const current = () => token === generation.current
                && Object.keys(owner).every(key => owner[key] === liveScope.current[key]);
            submitting.current = true;
            setPending(true);
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
            try {
                const {data, status, message: errMsg} = await api.analysis.addEdge(
                    currentGraphSpace, currentGraph, edgeParams);
                if (!current()) return;
                if (status === 200) {
                    try {
                        const result = await onOk?.(data);
                        if (result && typeof result === 'object' && 'status' in result && result.status !== 'applied') {
                            throw new Error('Canvas update was not applied');
                        }
                    }
                    catch {
                        if (current()) {
                            message.warning(t('analysis.canvas.dynamic_add.sync_failed'));
                            await finishClose({writeCompleted: true});
                        }
                        return;
                    }
                    if (!current()) return;
                    message.success(t('analysis.canvas.dynamic_add.save_success'));
                    await finishClose({writeCompleted: true});
                }
                else if (!errMsg) message.error(t('analysis.canvas.dynamic_add.save_failed'));
            }
            catch { if (current()) message.error(t('analysis.canvas.dynamic_add.save_failed')); }
            finally {
                if (current()) { submitting.current = false; setPending(false); }
            }
        }, [currentGraph, currentGraphSpace, onOk, isClickNew, isOutEdge, vertexId, t,
            finishClose, open]
    );

    const onFormFinish = useCallback(value => createEdge(value), [createEdge]);

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
            closable={{disabled: pending}}
            mask={{closable: !pending}}
            keyboard={!pending}
            open={open}
            footer={[
                <Button
                    type="primary"
                    size="medium"
                    style={{width: 60}}
                    onClick={onSaveEdit}
                    key="save"
                    disabled={pending}
                    loading={pending}
                    aria-label={t('analysis.canvas.dynamic_add.add')}
                >
                    {t('analysis.canvas.dynamic_add.add')}
                </Button>,
                <Button
                    size="medium"
                    style={{width: 60}}
                    onClick={onCloseDrawer}
                    disabled={pending}
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
                name={`add-edge-${formId}`}
                form={addForm}
                disabled={pending}
                onFinish={onFormFinish}
                labelAlign='left'
                labelCol={{span: 6}}
                labelWrap
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
                            options={getSelectNodeOptions()}
                        />
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
                        options={getSelectOptions()}
                    />
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
