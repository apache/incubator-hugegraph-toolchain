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

import {Button, Form, Input, Select, Segmented, DatePicker} from 'antd';
import {useState, useCallback} from 'react';
import {ruleOptions, typeToOption} from './config';
import style from './index.module.scss';
import * as rules from '../../../../utils/rules';

const VertexTypeOptions = [
    {label: '点属性', value: 'property'},
    {label: '点ID', value: 'id'},
    {label: '点类型', value: 'label'},
];

const EdgeTypeOptions = [
    {label: '边属性', value: 'property'},
    {label: '边ID', value: 'id'},
    {label: '边类型', value: 'label'},
];

const DateRangeForm = ({name}) => {

    return (
        <>
            <Form.Item
                name={[name, 0]}
                // getValueFromEvent={getFromEvent}
                // getValueProps={getFromProps}
                rules={[rules.required()]}
            >
                <DatePicker
                    format={'YYYY-MM-DD'}
                    style={{
                        width: '100%',
                    }}
                />
            </Form.Item>
            <Form.Item
                name={[name, 1]}
                // getValueFromEvent={getFromEvent}
                // getValueProps={getFromProps}
                rules={[rules.required()]}
            >
                <DatePicker
                    format={'YYYY-MM-DD'}
                    style={{
                        width: '100%',
                    }}
                />
            </Form.Item>
        </>
    );
};

const InputRangeForm = ({name}) => (
    <>
        <Form.Item name={[name, 0]} rules={[rules.required()]}>
            <Input placeholder='值' />
        </Form.Item>
        <Form.Item name={[name, 1]} rules={[rules.required()]}>
            <Input placeholder='值' />
        </Form.Item>
    </>
);

const FilterForm = ({index, remove, propertyList}) => {
    const [form] = Form.useForm();
    const [type, setType] = useState('vertex');
    const [opType, setOpType] = useState('property');
    const [ruleType, setRuleType] = useState('');
    const [rule, setRule] = useState('');
    // const [propertyList, setPropertyList] = useState([]);

    const handleType = useCallback(value => {
        setType(value);
        form.resetFields();
        form.setFieldValue('type', value);
    }, [form]);

    const handleOpType = useCallback(value => {
        const dataType = (value === 'id' || value === 'label') ? 'TEXT' : '';

        setOpType(value);
        setRuleType(dataType);
        setRule('');
        form.setFieldValue('data_type', dataType);
        form.resetFields(['rule', 'value', 'property']);
    }, [form]);

    const handleRemove = useCallback(() => remove(index), [remove, index]);

    const handleProperty = useCallback(value => {
        const dataType = propertyList.find(item => item.name === value).data_type;

        setRuleType(dataType);
        setRule('');
        form.setFieldValue('data_type', dataType);
        form.resetFields(['rule', 'value']);
    }, [propertyList, form]);

    const handleRule = useCallback(value => {
        setRule(value);
        form.resetFields(['value']);
    }, [form]);

    // const getFromEvent = useCallback(value => moment(value).format('YYYY-MM-DD'), []);
    // const getFromProps = useCallback(value => ({value: value ? moment(value) : ''}), []);

    return (
        <Form
            form={form}
            name={`form_${index}`}
            layout='vertical'
            className={style.form}
            initialValues={{
                op_type: 'property',
                type,
            }}
        >
            <Form.Item label='筛选类型'>
                <Segmented
                    block
                    options={[{label: '点', value: 'vertex'}, {label: '边', value: 'edge'}]}
                    onChange={handleType}
                />
            </Form.Item>
            <Form.Item name='type' hidden><Input /></Form.Item>
            <Form.Item label='操作类型' name='op_type'>
                <Select
                    options={type === 'vertex' ? VertexTypeOptions : EdgeTypeOptions}
                    placeholder='操作类型'
                    onChange={handleOpType}
                />
            </Form.Item>
            {opType === 'property' && (
                <Form.Item label='属性' name='property' rules={[rules.required()]}>
                    <Select
                        options={propertyList.map(item => ({
                            label: item.name,
                            value: item.name,
                            disabled: !typeToOption[item.data_type],
                        }))}
                        placeholder='属性'
                        onChange={handleProperty}
                    />
                </Form.Item>
            )}
            <Form.Item label='规则' name='rule' rules={[rules.required()]}>
                <Select
                    options={ruleOptions.filter(item => (
                        ruleType
                        && typeToOption[ruleType]
                        && typeToOption[ruleType].includes(item.value)
                    ))}
                    placeholder='规则'
                    onChange={handleRule}
                />
            </Form.Item>
            <Form.Item label='值'>
                {['ltlt', 'ltlte', 'ltelt', 'ltelte'].includes(rule)
                    ? (
                        ruleType === 'DATE'
                            ? <DateRangeForm name='value' />
                            : <InputRangeForm name='value' />
                    )
                    : (
                        ruleType === 'BOOLEN'
                            ? (
                                <Form.Item noStyle name='value' rules={[rules.required()]}>
                                    <Select options={[
                                        {label: 'true', value: true},
                                        {label: 'false', value: false},
                                    ]}
                                    />
                                </Form.Item>
                            )
                            : (ruleType === 'DATE' ? (
                                <Form.Item
                                    noStyle
                                    name='value'
                                    rules={[rules.required()]}
                                >
                                    <DatePicker
                                        format={'YYYY-MM-DD'}
                                        allowClear={false}
                                        style={{
                                            width: '100%',
                                        }}
                                    />
                                </Form.Item>
                            ) : (
                                <Form.Item noStyle name='value' rules={[rules.required()]}>
                                    <Input placeholder='值' />
                                </Form.Item>
                            ))
                    )}
            </Form.Item>
            <Form.Item name='data_type'>
                <Input hidden />
            </Form.Item>
            {/* <Select
                options={(edgeList).map(item =>
                    ({label: item.name, value: item.name}))}
                placeholder='值'
            /> */}
            <Form.Item>
                <Button block danger onClick={handleRemove}>删除表达式</Button>
            </Form.Item>
        </Form>
    );
};

export default FilterForm;
