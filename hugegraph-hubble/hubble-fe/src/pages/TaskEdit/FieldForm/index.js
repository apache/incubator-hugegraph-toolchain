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
import * as api from '../../../api';
import * as rules from '../../../utils/rules';
import style from '../index.module.scss';

// const formatData = field => {
//     const {vertex, edge} = field;
//     const data = [];

//     for (let item of vertex) {
//         data.push({})
//     }

// }

const FieldForm = ({visible, prev, datasourceID}) => {
    const [targetKeys, setTargetKeys] = useState([]);
    const [data, setData] = useState([]);
    const [inputData, setInputData] = useState('');
    const [status, setStatus] = useState('');
    const [transferStatus, setTransferStatus] = useState('');
    const [fieldForm] = Form.useForm();

    const setSourceData = data => {
        setData(data);
        fieldForm.setFieldValue('source_keys', data);
    };

    const setKey = val => {
        setTargetKeys(val);
        if (val.length > 0) {
            setTransferStatus('');
        }
    };

    const addField = () => {
        if (inputData && status !== 'error') {
            setSourceData([...data, {key: inputData}]);
            setInputData('');
        }
    };

    const isChecked = (selectedKeys, eventKey) => selectedKeys.includes(eventKey);

    const handleDelete = key => {
        const index = data.findIndex(item => item.key === key);
        const tmp = [...data];
        tmp.splice(index, 1);

        setSourceData(tmp);
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
        if (!datasourceID) {
            return;
        }

        api.manage.getDatasourceSchema(datasourceID).then(res => {
            if (res.status === 200) {
                setSourceData(res.data.map(item => ({key: item})));
                return;
            }

            message.error(res.message);
        });
    }, [datasourceID]);

    return (
        <div style={{display: visible ? '' : 'none'}} className={style.transfer}>
            <Form form={fieldForm} name='field_form'>
                <Typography.Title level={5}>选择源端字段</Typography.Title>
                <Form.Item name='target_keys' rules={[rules.required('请选择源端字段')]}>
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
                <Form.Item name='source_keys' hidden />
                <Form.Item>
                    <Space>
                        <Button onClick={prev}>上一步</Button>
                        <Button type='primary' htmlType='submit'>下一步</Button>
                    </Space>
                </Form.Item>
            </Form>
        </div>
    );
};

export default FieldForm;
