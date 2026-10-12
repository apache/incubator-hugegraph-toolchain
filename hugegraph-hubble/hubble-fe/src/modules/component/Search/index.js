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
 * @file  搜索
 */

import {useState, useEffect, useCallback, useContext} from 'react';
import {Button, Drawer, Form, Input, Select, Row, Col, Divider, InputNumber, message} from 'antd';
import {useTranslation} from 'react-i18next';
import GraphAnalysisContext from '../../Context';
import * as api from '../../../api';
import {
    getRuleOptionLabelKey,
    getRuleOptions,
    normalizeFilterCondition,
} from './utils';

const directionOptions = [
    {label: 'IN', value: 'IN'},
    {label: 'OUT', value: 'OUT'},
    {label: 'BOTH', value: 'BOTH'},
];

const FormList = ({properties, map, field, t}) => {
    const [ruleType, setRuleType] = useState('');
    const isBooleanRule = ruleType?.toLowerCase() === 'boolean';

    const handleKey = useCallback(value => {
        setRuleType(map[value]);
    }, [map]);

    const getValueForm = () => {
        const type = ruleType?.toLowerCase();

        if (['float', 'double'].includes(type)) {
            return <Input placeholder={t('analysis.canvas.filter_drawer.number_placeholder')} />;
        }

        if (['byte', 'int', 'long'].includes(type)) {
            return (
                <InputNumber
                    placeholder={t('analysis.canvas.filter_drawer.number_placeholder')}
                    style={{width: '100%'}}
                />
            );
        }

        if (['date'].includes(type)) {
            // return <DatePicker />;
            return <Input placeholder={t('analysis.canvas.filter_drawer.string_placeholder')} />;
        }

        if (['object', 'text', 'blob', 'uuid'].includes(type)) {
            return <Input placeholder={t('analysis.canvas.filter_drawer.string_placeholder')} />;
            // return <DatePicker style={{width: '100%'}} showTime />;
        }

        if (['boolean'].includes(type)) {
            return <div style={{lineHeight: '32px'}}>/</div>;
        }

        return <Input disabled placeholder={t('analysis.canvas.filter_drawer.placeholder')} />;
    };

    return (
        <>
            <Col span={4}>
                <Form.Item
                    label={t('analysis.canvas.filter_drawer.property')}
                    name={[field.name, 'key']}
                    rules={[{required: true}]}
                >
                    <Select
                        onChange={handleKey}
                        options={properties.map(item => ({label: item.name, value: item.name}))}
                    />
                </Form.Item>
            </Col>
            <Col span={4} offset={1}>
                <Form.Item
                    label={t('analysis.canvas.filter_drawer.rule')}
                    name={[field.name, 'operator']}
                    rules={[{required: true}]}
                >
                    <Select
                        options={getRuleOptions(ruleType).map(item => ({
                            label: t(`analysis.canvas.filter_drawer.rule_option.${getRuleOptionLabelKey(item)}`),
                            value: item,
                        }))}
                    />
                </Form.Item>
            </Col>
            <Col offset={1} span={5}>
                <Form.Item
                    label={t('analysis.canvas.filter_drawer.value')}
                    name={[field.name, 'value']}
                    rules={isBooleanRule ? [] : [{required: true}]}
                >
                    {getValueForm()}
                </Form.Item>
            </Col>
        </>
    );
};

const RemovePropertyButton = ({index, remove, label}) => {
    const handleRemove = useCallback(
        () => {
            remove(index);
        },
        [index, remove]
    );

    return (
        <Button
            type='link'
            onClick={handleRemove}
        >
            {label}
        </Button>
    );
};

const Search = ({
    onClose,
    open,
    vertexLabel,
    vertexId,
    propertykeys,
    onChange,
}) => {

    const {t} = useTranslation();
    const {graphSpace, graph} = useContext(GraphAnalysisContext);
    const [edgeList, setEdgeList] = useState([]);
    const [properties, setProperties] = useState([]);
    const [form] = Form.useForm();
    const propertykeysMap = {};

    if (propertykeys.length > 0) {
        propertykeys?.map(item => {
            propertykeysMap[item.name] = item.data_type;
        });
    }

    const checkDuplicate = () => ({
        validator(_, value) {
            const existName = [];
            if (value === undefined) {
                return Promise.resolve();
            }

            for (let item of value) {
                if (item === undefined || !item.key) {
                    return Promise.resolve();
                }

                if (existName.includes(item.key)) {
                    return Promise.reject(new Error(t('analysis.canvas.filter_drawer.duplicate_property')));
                }

                existName.push(item.key);
            }

            return Promise.resolve();
        },
    });

    const handleEdge = useCallback(value => {
        api.manage.getMetaEdge(graphSpace, graph, value).then(res => {
            if (res.status !== 200) {
                message.error(t('analysis.canvas.filter_drawer.get_edge_failed'));
                return;
            }

            setProperties(res.data.properties);
        });
    }, [graphSpace, graph, t]);


    const handleFinish = useCallback(
        () => {
            form.validateFields().then(values => {
                const tmp = values.conditions ?? [];
                const newConditions = [];

                for (let item of tmp) {
                    const condition = normalizeFilterCondition(item);

                    if (condition) {
                        newConditions.push(condition);
                    }
                }
                values.conditions = newConditions;
                onChange({...values, vertex_id: vertexId, vertex_label: vertexLabel});
            });
        },
        [form, onChange, vertexId, vertexLabel]
    );

    useEffect(() => {
        if (!open || !vertexLabel || !graphSpace || !graph) {
            return;
        }
        form.resetFields();

        api.manage.getMetaVertexLink(graphSpace, graph, vertexLabel).then(res => {
            if (res.status !== 200) {
                message.error(t('analysis.canvas.filter_drawer.get_adjacent_edge_failed'));
                return;
            }
            setEdgeList(res.data);
        });
    }, [form, vertexLabel, graphSpace, graph, open, t]);

    return (
        <Drawer
            title={t('analysis.canvas.filter_drawer.title')}
            placement="top"
            // closable={false}
            mask={false}
            onClose={onClose}
            open={open}
            getContainer={false}
            extra={[
                <Button key='1' type='primary' onClick={handleFinish}>
                    {t('analysis.canvas.filter_drawer.filter')}
                </Button>,
            ]}
            style={{
                position: 'absolute',
            }}
        >
            <Form form={form}>
                <Row>
                    <Col span={4}>
                        <Form.Item
                            label={t('analysis.canvas.filter_drawer.edge_label')}
                            name={'edge_label'}
                            rules={[{required: true}]}
                        >
                            <Select
                                options={edgeList.map(item => ({label: item, value: item}))}
                                onChange={handleEdge}
                            />
                        </Form.Item>
                    </Col>
                    <Col span={4} offset={1}>
                        <Form.Item
                            label={t('analysis.canvas.filter_drawer.edge_direction')}
                            name={'direction'}
                            rules={[{required: true}]}
                        >
                            <Select options={directionOptions} />
                        </Form.Item>
                    </Col>
                </Row>
                <Divider style={{marginTop: 0}} />
                <Form.List name='conditions' rules={[checkDuplicate()]}>
                    {(fields, {remove, add}, {errors}) => (
                        <>
                            {fields.map((field, index) => (
                                <Row key={field.key}>
                                    <FormList
                                        map={propertykeysMap}
                                        properties={properties}
                                        field={field}
                                        t={t}
                                    />
                                    <RemovePropertyButton
                                        index={index}
                                        remove={remove}
                                        label={t('analysis.canvas.filter_drawer.delete')}
                                    />
                                </Row>
                            ))}
                            <Form.ErrorList errors={errors} />
                            <Row>
                                <Col span={15}>
                                    <Button
                                        type='dashed'
                                        block
                                        onClick={add}
                                        disabled={properties.length === 0}
                                    >
                                        {t('analysis.canvas.filter_drawer.add_property')}
                                    </Button>
                                </Col>
                            </Row>
                        </>
                    )}
                </Form.List>
            </Form>
        </Drawer>
    );
};

export default Search;
