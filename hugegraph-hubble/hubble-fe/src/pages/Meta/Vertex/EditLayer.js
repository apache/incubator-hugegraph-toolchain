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

import {Alert, Button, Modal, Form, Input, Select, Row, Col, Checkbox, message, Spin} from 'antd';
import {useEffect, useState, useCallback, useRef} from 'react';
import {useTranslation} from 'react-i18next';
import * as api from '../../../api';
import * as rules from '../../../utils/rules';
import {
    idOptions,
    vertexSizeSchemas,
} from '../common/config';
// import {SelectColorbox} from '../common/Colorbox';
import IconSelect from '../../../components/IconSelect';
import {InputColorSelect} from '../../../components/ColorSelect';
import RelateProperty from '../common/RelateProperty';
import RelatePropertyIndex from '../common/RelatePropertyIndex';

const EditVertexLayer = ({visible, onCancle, graphspace, graph, refresh, name, propertyList}) => {
    const {t} = useTranslation();
    const defaultDisplayFields = {label: t('schema.vertex.id'), value: '~id'};
    const [selectedPropertyList, setSelectedPropertyList] = useState([]);
    const [idStrategy, setIdStrategy] = useState('PRIMARY_KEY');
    const [existProperties, setExistProperties] = useState([]);
    const [existPropertyIndex, setExistPropertyIndex] = useState([]);
    const [primaryKeys, setPrimaryKeys] = useState([]);
    const [loading, setLoading] = useState(false);
    const [spinning, setSpinning] = useState(false);
    const [detailError, setDetailError] = useState(false);
    const detailRequest = useRef(null);
    const [form] = Form.useForm();

    const loadDetail = useCallback(() => {
        const token = Symbol('vertex-detail');
        detailRequest.current = token;
        setDetailError(false);
        setSpinning(true);
        api.manage.getMetaVertex(graphspace, graph, name).then(res => {
            if (detailRequest.current !== token) {
                return;
            }
            if (res.status !== 200) {
                setDetailError(true);
                return;
            }
            const {properties, property_indexes, id_strategy, primaryKeys} = res.data;
            form.setFieldsValue(res.data);
            setSelectedPropertyList(properties.map(item => ({
                ...item, label: item.name, value: item.name,
            })));
            setExistProperties(properties);
            setExistPropertyIndex(property_indexes);
            setIdStrategy(id_strategy);
            setPrimaryKeys(primaryKeys);
        }).catch(() => {
            if (detailRequest.current === token) {
                setDetailError(true);
            }
        }).finally(() => {
            if (detailRequest.current === token) {
                setSpinning(false);
            }
        });
    }, [form, graph, graphspace, name]);

    const handleIDStrategy = useCallback(value => {
        setIdStrategy(value);
    }, []);

    const handlePrimaryKeys = useCallback(values => {
        setPrimaryKeys(values);
    }, []);

    const selectProperty = useCallback(() => {
        // const tmp = [...selectedPropertyList];
        // tmp.push({label: val, value: val});
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
        const attr = form.getFieldValue('properties');
        const tmp = [];
        for (let item of attr) {
            tmp.push({...item, label: item.name, value: item.name});
        }
        setSelectedPropertyList(tmp);
    }, [form]);

    const addVertex = useCallback(data => {
        setLoading(true);
        api.manage.addMetaVertex(graphspace, graph, data).then(res => {
            setLoading(false);
            if (res.status === 200) {
                message.success(t('common.add_success'));
                onCancle();
                refresh();
                return;
            }

            message.error(res.message);
        });
    }, [graph, graphspace, onCancle, refresh, t]);

    const updateVertex = useCallback((name, data) => {
        const {style, append_properties, remove_property_indexes, append_property_indexes} = data;

        setLoading(true);
        api.manage.updateMetaVertex(graphspace, graph, name, {
            style,
            append_properties,
            remove_property_indexes,
            append_property_indexes,
        }).then(res => {
            setLoading(false);
            if (res.status === 200) {
                message.success(t('common.update_success'));
                onCancle();
                refresh();
                return;
            }

            message.error(res.message);
        });
    }, [graph, graphspace, onCancle, refresh, t]);

    const onFinish = useCallback(() => {
        form.validateFields().then(values => {
            // submitForm(values);
            if (name) {
                updateVertex(name, values);
                return;
            }

            addVertex(values);
        }).catch(e => {});
    }, [form, name, addVertex, updateVertex]);

    useEffect(() => {
        if (!visible) {
            return;
        }

        form.resetFields();
        setExistProperties([]);
        setExistPropertyIndex([]);
        setSelectedPropertyList([]);
        setIdStrategy('PRIMARY_KEY');
        setPrimaryKeys([]);

        if (!name) {
            return;
        }

        loadDetail();
        return () => {
            detailRequest.current = null;
        };
    }, [visible, name, form, graph, graphspace, loadDetail]);

    return (
        <Modal
            title={name ? t('schema.vertex.edit') : t('schema.vertex.create')}
            open={visible}
            onCancel={onCancle}
            width={600}
            onOk={onFinish}
            confirmLoading={loading}
            okButtonProps={{disabled: spinning || detailError}}
            destroyOnClose
        >
            {detailError && (
                <Alert
                    type='error'
                    showIcon
                    message={t('schema.vertex.detail_failed')}
                    action={<Button size='small' onClick={loadDetail}>{t('schema.retry')}</Button>}
                />
            )}
            <Spin spinning={spinning}>
                <Form
                    form={form}
                    labelCol={{span: 6}}
                    initialValues={{
                        open_label_index: false,
                        style: {
                            color: '#5c73e6',
                            size: 'NORMAL',
                            display_fields: ['~id'],
                        },
                        id_strategy: 'PRIMARY_KEY',
                        remove_property_indexes: [],
                    }}
                    // preserve={false}
                >
                    <Form.Item label={t('schema.vertex.col.name')} name='name' rules={[rules.required()]}>
                        <Input placeholder={t('schema.name_placeholder')} disabled={!!name} />
                    </Form.Item>
                    <Form.Item label={t('schema.vertex.style')}>
                        <Row gutter={[12, 0]}>
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
                                    name={['style', 'size']}
                                    rules={[rules.required()]}
                                    style={{marginBottom: 0}}
                                >
                                    <Select
                                        style={{width: 66}}
                                        // size="small"
                                        options={vertexSizeSchemas}
                                    />
                                </Form.Item>
                            </Col>
                            <Col>
                                <Form.Item
                                    wrapperCol={{span: 10}}
                                    name={['style', 'icon']}
                                    rules={[rules.required()]}
                                    style={{marginBottom: 0}}
                                >
                                    <IconSelect />
                                </Form.Item>
                            </Col>
                        </Row>
                    </Form.Item>
                    <Form.Item
                        label={t('schema.vertex.col.id_strategy')}
                        wrapperCol={{span: 6}}
                        name='id_strategy'
                        rules={[rules.required()]}
                    >
                        <Select
                            options={idOptions}
                            onChange={handleIDStrategy}
                            disabled={!!name}
                        />
                    </Form.Item>
                    <Form.Item label={t('schema.col.properties')} required={idStrategy === 'PRIMARY_KEY'}>
                        <RelateProperty
                            propertyList={propertyList}
                            selectProperty={selectProperty}
                            removeProperty={removeProperty}
                            exist={existProperties}
                            isEdit={!!name}
                        />
                    </Form.Item>
                    {idStrategy === 'PRIMARY_KEY' && (
                        <Form.Item
                            label={t('schema.vertex.col.primary_keys')}
                            name='primary_keys'
                            rules={[rules.required()]}
                        >
                            <Select
                                placeholder={t('schema.vertex.select_properties_first')}
                                mode='multiple'
                                options={selectedPropertyList.map(item => ({
                                    ...item,
                                    disabled: item.nullable,
                                }))}
                                disabled={!!name}
                                onChange={handlePrimaryKeys}
                            />
                        </Form.Item>
                    )}
                    <Form.Item
                        label={t('schema.vertex.display_fields')}
                        name={['style', 'display_fields']}
                        rules={[rules.required()]}
                    >
                        <Select
                            options={selectedPropertyList.filter(item => !item.nullable).concat(defaultDisplayFields)}
                            mode='multiple'
                        />
                    </Form.Item>
                    <Form.Item label={t('schema.col.label_index')} valuePropName='checked' name='open_label_index'>
                        <Checkbox disabled={!!name} />
                    </Form.Item>
                    <Form.Item label={t('schema.col.property_indexes')}>
                        <RelatePropertyIndex
                            selectedPropertyList={selectedPropertyList}
                            propertyList={propertyList}
                            exist={existPropertyIndex}
                            isEdit={!!name}
                            primaryKeys={primaryKeys}
                        />
                        {/* <Form.Item name='remove_property_indexes' hidden /> */}
                    </Form.Item>
                </Form>
            </Spin>
        </Modal>
    );
};

export {EditVertexLayer};
