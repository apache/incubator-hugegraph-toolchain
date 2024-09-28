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
 * @author wuxutao
 */

import {useState, useEffect, useCallback, useContext} from 'react';
import {Button, Drawer, Form, Input, Select, Row, Col, Divider, InputNumber, message} from 'antd';
import GraphAnalysisContext from '../../Context';
import * as api from '../../../api';

const directionOptions = [
    {label: 'IN', value: 'IN'},
    {label: 'OUT', value: 'OUT'},
    {label: 'BOTH', value: 'BOTH'},
];

const ruleMap = {
    大于: 'gt',
    大于等于: 'gte',
    等于: 'eq',
    小于: 'lt',
    小于等于: 'lte',
    True: 'eq',
    False: 'neq',
};

const getRuleOptions = ruleType => {
    switch (ruleType.toLowerCase()) {
        case 'float':
        case 'double':
        case 'byte':
        case 'int':
        case 'long':
        case 'date':
            return ['大于', '大于等于', '小于', '小于等于', '等于'];
        case 'object':
        case 'text':
        case 'blob':
        case 'uuid':
            return ['等于'];
        case 'boolean':
            return ['True', 'False'];
        default:
            return [];
    }
};


const FormList = ({properties, map, field}) => {
    const [ruleType, setRuleType] = useState('');

    const handleKey = useCallback(value => {
        setRuleType(map[value]);
    }, [map]);

    const getValueForm = () => {
        const type = ruleType?.toLowerCase();

        if (['float', 'double'].includes(type)) {
            return <Input placeholder='请输入数字' />;
        }

        if (['byte', 'int', 'long'].includes(type)) {
            return <InputNumber placeholder='请输入数字' style={{width: '100%'}} />;
        }

        if (['date'].includes(type)) {
            // return <DatePicker />;
            return <Input placeholder='请输入字符串' />;
        }

        if (['object', 'text', 'blob', 'uuuid'].includes(type)) {
            return <Input placeholder='请输入字符串' />;
            // return <DatePicker style={{width: '100%'}} showTime />;
        }

        if (['boolen'].includes(type)) {
            return <div style={{lineHeight: '32px'}}>/</div>;
        }

        return <Input disabled placeholder='请输入' />;
    };

    return (
        <>
            <Col span={4}>
                <Form.Item label='属性' name={[field.name, 'key']} rules={[{required: true}]}>
                    <Select
                        onChange={handleKey}
                        options={properties.map(item => ({label: item.name, value: item.name}))}
                    />
                </Form.Item>
            </Col>
            <Col span={4} offset={1}>
                <Form.Item label='规则' name={[field.name, 'operator']} rules={[{required: true}]}>
                    <Select
                        // options={properties.map(item => ({label: item.name, value: item.name}))}
                        options={getRuleOptions(ruleType).map(item => ({label: item, value: ruleMap[item]}))}
                    />
                </Form.Item>
            </Col>
            <Col offset={1} span={5}>
                <Form.Item label='值' name={[field.name, 'value']} rules={[{required: true}]}>
                    {getValueForm()}
                </Form.Item>
            </Col>
        </>
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
                    return Promise.reject(new Error('属性不可重复'));
                }

                existName.push(item.key);
            }

            return Promise.resolve();
        },
    });

    const handleEdge = useCallback(value => {
        api.manage.getMetaEdge(graphSpace, graph, value).then(res => {
            if (res.status !== 200) {
                message.error('获取边信息失败');
                return;
            }

            setProperties(res.data.properties);
        });
    }, [graphSpace, graph]);


    const handleFinish = useCallback(
        () => {
            form.validateFields().then(values => {
                const tmp = values.conditions ?? [];
                const newConditions = [];

                for (let item of tmp) {
                    const {key, value, operator} = item;
                    if (!key || !value || !operator) {
                        continue;
                    }

                    newConditions.push({
                        key,
                        operator,
                        value: value?._isAMomentObject ? value.format('YYYY-MM-DD HH:mm:ss') : value,
                    });
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
                message.error('获取邻边失败');
                return;
            }
            setEdgeList(res.data);
        });
    }, [vertexLabel, graphSpace, graph, open]);

    return (
        <Drawer
            title={'查询'}
            placement="top"
            // closable={false}
            mask={false}
            onClose={onClose}
            open={open}
            getContainer={false}
            extra={[
                <Button key='1' type='primary' onClick={handleFinish}>筛选</Button>,
            ]}
            style={{
                position: 'absolute',
            }}
        >
            <Form form={form}>
                <Row>
                    <Col span={4}>
                        <Form.Item label='边类型' name={'edge_label'} rules={[{required: true}]}>
                            <Select
                                options={edgeList.map(item => ({label: item, value: item}))}
                                onChange={handleEdge}
                            />
                        </Form.Item>
                    </Col>
                    <Col span={4} offset={1}>
                        <Form.Item label='边方向' name={'direction'} rules={[{required: true}]}>
                            <Select options={directionOptions} />
                        </Form.Item>
                    </Col>
                </Row>
                <Divider style={{marginTop: 0}} />
                <Form.List name='conditions' rules={[checkDuplicate]}>
                    {(fields, {remove, add}, {errors}) => (
                        <>
                            {fields.map((field, index) => (
                                <Row key={field.key}>
                                    <FormList
                                        map={propertykeysMap}
                                        properties={properties}
                                        field={field}
                                    />
                                    <Button
                                        type='link'
                                        onClick={() => remove(index)}
                                    >
                                        删除
                                    </Button>
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
                                        添加属性
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
