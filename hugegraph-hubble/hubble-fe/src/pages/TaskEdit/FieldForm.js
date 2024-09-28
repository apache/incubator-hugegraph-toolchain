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

import {Form, Input, Transfer, Space, Button, Typography, message, Tree, Popconfirm} from 'antd';
import {PlusOutlined, MinusSquareOutlined} from '@ant-design/icons';
import {useEffect, useState} from 'react';
import * as api from '../../api';

// const formatData = field => {
//     const {vertex, edge} = field;
//     const data = [];

//     for (let item of vertex) {
//         data.push({})
//     }

// }

const FieldForm = ({visible, prev, next, datasource, setTargetField, setHeader}) => {
    const [targetKeys, setTargetKeys] = useState([]);
    const [data, setData] = useState([]);
    const [inputData, setInputData] = useState('');
    const [status, setStatus] = useState('');
    const [transferStatus, setTransferStatus] = useState('');

    const checkNext = () => {
        if (targetKeys.length === 0) {
            setTransferStatus('error');
            return;
        }
        setTargetField(targetKeys);
        setHeader(data.map(item => item.key));
        next();
    };

    const setKey = val => {
        setTargetKeys(val);
        if (val.length > 0) {
            setTransferStatus('');
        }
    };

    const addField = () => {
        if (inputData && status !== 'error') {
            setData([...data, {key: inputData}]);
            setInputData('');
        }
    };

    const isChecked = (selectedKeys, eventKey) => selectedKeys.includes(eventKey);

    const handleDelete = key => {
        const index = data.findIndex(item => item.key === key);
        const tmp = [...data];
        tmp.splice(index, 1);

        setData(tmp);
    };

    const handleInputData = e => {
        const value = e.target.value;
        if (/[^a-zA-Z0-9\-\_]/.test(value)) {
            setStatus('error');
        }
        else {
            setStatus('');
        }

        setInputData(value);
    };

    const generateTree = (treeNodes = [], checkedKeys = []) =>
        treeNodes.map(({children, ...props}) => ({
            ...props,
            title: (
                <Space>
                    {props.key}
                    <Popconfirm
                        title="确定要删除这个字段吗？"
                        onConfirm={() => handleDelete(props.key)}
                        okText="Yes"
                        cancelText="No"
                    >
                        <MinusSquareOutlined />
                    </Popconfirm>
                </Space>
            ),
            disabled: checkedKeys.includes(props.key),
        }));

    const footer = (_, {direction}) => {
        return (
            direction === 'left'
            && (
                <div style={{margin: 5}}>
                    <Input
                        value={inputData}
                        addonAfter={<PlusOutlined onClick={addField} />}
                        placeholder='添加字段,仅允许字母、数字、-、_'
                        onChange={handleInputData}
                        status={status}
                    />
                </div>
            )
        );
    };

    useEffect(() => {
        if (!datasource.datasource_id) {
            return;
        }

        api.manage.getDatasourceSchema(datasource.datasource_id.toString()).then(res => {
            if (res.status === 200) {
                setData(res.data.map(item => ({key: item})));
                return;
            }

            message.error(res.message);
        });
    }, [datasource]);

    return (
        <div style={{display: visible ? '' : 'none'}}>
            <Typography.Title level={5}>选择源端字段</Typography.Title>
            <Form.Item>
                <Transfer
                    dataSource={data}
                    titles={['源端可选字段', '已选字段']}
                    listStyle={{width: 400, height: 400}}
                    render={item => item.key}
                    targetKeys={targetKeys}
                    status={transferStatus}
                    onChange={setKey}
                    footer={footer}
                    oneWay
                >
                    {({direction, onItemSelect, selectedKeys}) => {
                        if (direction === 'left') {
                            const checkedKeys = [...selectedKeys, ...targetKeys];
                            return (
                                <Tree
                                    blockNode
                                    checkable
                                    checkStrictly
                                    defaultExpandAll
                                    checkedKeys={checkedKeys}
                                    treeData={generateTree(data, targetKeys)}
                                    onCheck={(_, {node: {key}}) => {
                                        onItemSelect(key, !isChecked(checkedKeys, key));
                                    }}
                                    // onSelect={(_, {node: {key}}) => {
                                    //     onItemSelect(key, !isChecked(checkedKeys, key));
                                    // }}
                                />
                            );
                        }
                    }}
                </Transfer>
            </Form.Item>
            <Form.Item>
                <Space>
                    <Button onClick={prev}>上一步</Button>
                    <Button type='primary' onClick={checkNext}>下一步</Button>
                </Space>
            </Form.Item>
        </div>
    );
};

export default FieldForm;
