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

import {Form, Select, Input, Space} from 'antd';
import {useState, useEffect, useCallback} from 'react';
import {indexTypeOptions} from './config';

const RelatePropertyIndex = ({selectedPropertyList, propertyList, exist, isEdit, primaryKeys}) => {
    const [plist, setPlist] = useState([]);
    const [showList, setShowList] = useState([]);
    const textEnum = ['TEXT', 'BOOLEAN', 'UUID', 'BLOB'];
    const propertyMap = {};

    for (let item of propertyList) {
        propertyMap[item.value] = item.data_type;
    }

    const setPropertyList = (index, value) => {
        plist[index] = value;
        setPlist([...plist]);
    };

    const getPropertyList = value => selectedPropertyList.filter(item => {
        if (primaryKeys && primaryKeys.includes(item.value)) {
            return false;
        }

        return value === 'SECONDARY'
            || value === 'SHARD'
            || value === 'UNIQUE'
            || (value === 'RANGE' && !textEnum.includes(propertyMap[item.value]))
            || (value === 'SEARCH' && propertyMap[item.value] === 'TEXT');
    });

    const checkDuplicate = () => ({
        validator(_, value) {
            const existName = showList.map(item => item.name);
            if (value === undefined) {
                return Promise.resolve();
            }

            for (let item of value) {
                if (item === undefined || !item.name) {
                    return Promise.resolve();
                }

                if (existName.includes(item.name)) {
                    return Promise.reject(new Error('索引名称不可重复'));
                }

                existName.push(item.name);
            }

            return Promise.resolve();
        },
    });

    useEffect(() => {
        setShowList(exist);
        setPlist([]);
    }, [exist, selectedPropertyList]);

    return (isEdit
        ? (
            <>
                <Form.Item noStyle>
                    {showList.map((item, index) => (
                        <Space key={item.name} align='baseline'>
                            <Form.Item
                                className="form_attr_select"
                            >
                                <Input placeholder='索引名' value={item.name} disabled />
                            </Form.Item>
                            <Form.Item
                                className="form_attr_select"
                            >
                                <Select
                                    options={indexTypeOptions}
                                    disabled
                                    value={item.type}
                                />
                            </Form.Item>
                            <Form.Item
                                className="form_attr_select"
                            >
                                <Select
                                    value={item.fields}
                                    mode='multiple'
                                    disabled
                                />
                            </Form.Item>
                            <Form.Item noStyle shouldUpdate>
                                {({getFieldValue, setFieldValue}) => {
                                    const handleRemove = (item, index) => {
                                        showList.splice(index, 1);
                                        setShowList([...showList]);
                                        // removePropertyIndex(item.name);
                                        setFieldValue('remove_property_indexes',
                                            [...(getFieldValue('remove_property_indexes') ?? []), item.name]);
                                    };

                                    return <a onClick={() => handleRemove(item, index)}>删除</a>;
                                }}
                            </Form.Item>
                        </Space>
                    ))}
                </Form.Item>
                <Form.List
                    name={'append_property_indexes'}
                    rules={[checkDuplicate]}
                >
                    {(fields, {add, remove}, {errors}) => (
                        <>
                            {fields.map((field, index) => {

                                return (
                                    <Space key={field.key} align='baseline'>
                                        <Form.Item
                                            className="form_attr_select"
                                            key={[field.key, 'name']}
                                            name={[field.name, 'name']}
                                        >
                                            <Input placeholder='索引名' />
                                        </Form.Item>
                                        <Form.Item
                                            className="form_attr_select"
                                            name={[field.name, 'type']}
                                        >
                                            <Select
                                                options={indexTypeOptions}
                                                onChange={val => setPropertyList(index, val)}
                                            />
                                        </Form.Item>
                                        <Form.Item
                                            className="form_attr_select"
                                            name={[field.name, 'fields']}
                                        >
                                            <Select
                                                options={getPropertyList(plist[index])}
                                                mode={plist[index] === 'UNIQUE' ? 'multiple' : 'multiple'}
                                            />
                                        </Form.Item>
                                        <a onClick={() => {
                                            remove(index);
                                        }}
                                        >
                                            删除
                                        </a>
                                    </Space>
                                );
                            }
                            )}
                            <Form.ErrorList errors={errors} />
                            <div className="form_attr_add" onClick={() => add()}>+新增</div>
                        </>
                    )}
                </Form.List>
                <Form.Item name='remove_property_indexes' hidden />
            </>
        )
        : (
            <Form.List
                name={'property_indexes'}
                rules={[checkDuplicate]}
            >
                {(fields, {add, remove}, {errors}) => (
                    <>
                        {fields.map((field, index) => {
                            return (
                                <Space key={field.key} align='baseline'>
                                    <Form.Item
                                        className="form_attr_select"
                                        key={[field.key, 'name']}
                                        name={[field.name, 'name']}
                                    >
                                        <Input placeholder='索引名' />
                                    </Form.Item>
                                    <Form.Item
                                        className="form_attr_select"
                                        name={[field.name, 'type']}
                                    >
                                        <Select
                                            options={indexTypeOptions}
                                            onChange={val => setPropertyList(index, val)}
                                        />
                                    </Form.Item>
                                    <Form.Item
                                        className="form_attr_select"
                                        name={[field.name, 'fields']}
                                    >
                                        <Select
                                            options={getPropertyList(plist[index])}
                                            mode={'multiple'}
                                            onChange={value => {
                                                if (plist[index] !== 'SECONDARY' && value?.length > 1) {
                                                    value.shift();
                                                }
                                            }}
                                        />
                                    </Form.Item>
                                    <a onClick={() => {
                                        remove(index);
                                    }}
                                    >
                                        删除
                                    </a>
                                </Space>
                            );
                        }
                        )}
                        <Form.ErrorList errors={errors} />
                        <div className="form_attr_add" onClick={() => add()}>+新增</div>
                    </>
                )}
            </Form.List>
        )
    );
};

export default RelatePropertyIndex;
