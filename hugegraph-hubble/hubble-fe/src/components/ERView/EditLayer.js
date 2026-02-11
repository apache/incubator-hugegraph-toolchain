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

import {Form, Modal, Select} from 'antd';
import vertexData from './data/vertex.json';
import edgeData from './data/edge.json';
import {useCallback} from 'react';
import {useTranslation} from 'react-i18next';

const EditVertexLayer = ({open, onCancle: onCancel, onChange}) => {
    const {t} = useTranslation();
    const [form] = Form.useForm();

    const setVertex = useCallback((_, item) => {
        form.setFieldValue('vertex', item.info);
    }, [form]);

    const onFinish = useCallback(() => {
        console.log(form.getFieldsValue());
        onChange(form.getFieldValue('vertex'));
        onCancel();
    }, [form, onChange, onCancel]);

    return (
        <Modal
            title={t('ERView.vertex.create')}
            onCancel={onCancel}
            open={open}
            onOk={onFinish}
        >
            <Form form={form}>
                <Form.Item label={t('ERView.vertex.type')}>
                    <Select
                        options={vertexData.map(item => ({label: item.name, value: item.name, info: item}))}
                        onChange={setVertex}
                    />
                    <Form.Item name='vertex' />
                </Form.Item>
            </Form>
        </Modal>
    );
};

const EditEdgeLayer = ({open, onCancle, onChange}) => {
    const [form] = Form.useForm();
    const {t} = useTranslation();
    const setEdge = useCallback((_, item) => {
        form.setFieldValue('edge', item.info);
    }, [form]);

    const onFinish = useCallback(() => {
        onChange(form.getFieldValue('edge'));
        onCancle();
    }, [form, onChange, onCancle]);

    return (
        <Modal
            title={t('ERView.edge.create')}
            onCancel={onCancle}
            open={open}
            onOk={onFinish}
        >
            <Form form={form}>
                <Form.Item label={t('ERView.edge.type')}>
                    <Select
                        options={edgeData.map(item => ({label: item.name, value: item.name, info: item}))}
                        onChange={setEdge}
                    />
                    <Form.Item name='edge' />
                </Form.Item>
            </Form>
        </Modal>
    );
};

export {EditVertexLayer, EditEdgeLayer};
