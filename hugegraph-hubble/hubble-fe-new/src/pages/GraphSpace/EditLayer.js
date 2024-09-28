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

import {Modal, Form, Input, Select, message, Switch, InputNumber, Space, Typography} from 'antd';
import {useState, useEffect, useMemo, useCallback} from 'react';
import * as api from '../../api/index';
import * as rules from '../../utils/rules';

const {Option} = Select;

const MyFormItem = ({label, children}) => {
    return (
        <Form.Item label={label} colon={false} required>
            <Space>
                {children}
                <span className='spanFontSize'>{label === 'cpu资源' ? '核' : 'G'}</span>
            </Space>
        </Form.Item>
    );
};

const EditLayer = ({visible, detail, onCancel, refresh}) => {
    const [form] = Form.useForm();
    const [userList, setUserList] = useState([]);
    const [loading, setLoading] = useState(false);

    const defaultValues = {
        max_graph_number: 100,
        max_role_number: 100,
        cpu_limit: 100,
        compute_cpu_limit: 100,
        memory_limit: 1000,
        compute_memory_limit: 1000,
        storage_limit: 1000000,
        description: '',
    };

    // 是否禁用
    const isDisabled = useMemo(() => {
        if (Object.keys(detail).length !== 0) {
            return true;
        }
        return false;
    }, [detail]);

    // 完成提交
    const onFinish = useCallback(() => {
        form.validateFields().then(values => {
            setLoading(true);

            const thenCallBack = res => {
                setLoading(false);
                if (res && res.status === 200) {
                    message.success('操作成功');
                    refresh();
                    onCancel();
                }
            };

            if (isDisabled) {
                api.manage.updateGraphSpace(detail.name, values).then(thenCallBack);
            }
            else {
                api.manage.addGraphSpace(values).then(thenCallBack);
            }
        });
    }, [detail.name, isDisabled, onCancel, refresh, form]);

    // 验证
    const serviceValidator = (_, value) => {
        if (value === 'DEFAULT') {
            return Promise.resolve();
        }
        let res = /^[a-z][a-z0-9_]{0,47}$/.test(value);
        if (!res) {
            return Promise.reject('以字母开头,只能包含小写字母、数字、_, 最长48位');
        }

        return Promise.resolve();
    };

    // 验证
    const k8sValidator = (_, value) => {
        let res = /^[a-z][a-z0-9\-]{0,47}$/.test(value);
        if (value && !res) {
            return Promise.reject('以字母开头,只能包含小写字母、数字、-,最长48位');
        }

        return Promise.resolve();
    };

    // 管理员
    const userSelect = useMemo(
        () => userList.map(item => (<Option key={item.id}>{item.user_name}</Option>)),
        [userList]
    );

    useEffect(() => {
        if (!visible) {
            return;
        }
        form.resetFields();

        api.auth.getUserList().then(res => {
            if (res && res.status === 200) {
                setUserList(res.data.users);
            }
        });

        if (detail.name) {
            api.manage.getGraphSpace(detail.name).then(res => {
                if (res.status === 200) {
                    form.setFieldsValue(res.data);
                }
            });
        }
    }, [visible, form, detail.name]);

    return (
        <Modal
            title={isDisabled ? '编辑图空间' : '创建图空间'}
            open={visible}
            onCancel={onCancel}
            okText={Object.keys(detail).length === 0 ? '创建' : '保存'}
            loading={loading}
            onOk={onFinish}
            width={600}
            destroyOnClose
            maskClosable={false}
            // forceRender
        >
            <Form
                labelCol={{span: 6}}
                form={form}
                name="control-hooks"
                // onFinish={onFinish}
                // preserve={false}
                initialValues={defaultValues}
            >

                <Form.Item
                    name="name"
                    label="图空间ID"
                    rules={
                        [
                            {required: true, message: '此项为必填项'},
                            {max: 48, message: '字符长度最多48位'},
                            {validator: serviceValidator},
                        ]
                    }
                >
                    <Input disabled={isDisabled} placeholder='以字母开头,只能包含小写字母、数字、_, 最长48位' />
                </Form.Item>

                <Form.Item
                    name="nickname"
                    label="图空间名称"
                    rules={
                        [
                            {required: true, message: '此项为必填项'},
                            {max: 12, message: '字符长度最多12位'},
                            rules.isPropertyName,
                        ]
                    }
                >
                    <Input placeholder='只能包含中文、字母、数字、_，最长12位' />
                </Form.Item>

                <Form.Item
                    label="是否开启鉴权"
                    name="auth"
                    valuePropName="checked"
                >
                    <Switch disabled={isDisabled} />
                </Form.Item>

                <Form.Item
                    name="max_graph_number"
                    label="最大图数"
                    rules={
                        [
                            {required: true, message: '此项为必填项'},
                        ]
                    }
                >
                    <InputNumber precision={0} min={1} />
                </Form.Item>

                {/* <Form.Item
                    name="max_role_number"
                    label="最大角色数"
                    rules={
                        [
                            {required: true, message: '此项为必填项'},
                        ]
                    }
                >
                    <InputNumber precision={0} min={1} />
                </Form.Item> */}

                <Typography.Title level={5}>图服务：</Typography.Title>
                <MyFormItem label='cpu资源:'>
                    <Form.Item
                        name="cpu_limit"
                        noStyle
                        rules={
                            [
                                {required: true, message: '此项为必填项'},
                            ]
                        }
                    >
                        <InputNumber placeholder='核' precision={0} min={1} />
                    </Form.Item>
                </MyFormItem>

                <MyFormItem label="内存资源:">
                    <Form.Item
                        name="memory_limit"
                        noStyle
                        rules={
                            [
                                {required: true, message: '此项为必填项'},
                            ]
                        }
                    >
                        <InputNumber placeholder='G' precision={0} min={1} />
                    </Form.Item>
                </MyFormItem>

                <Form.Item
                    name="oltp_namespace"
                    label="k8s命名空间"
                    rules={
                        [
                            {validator: k8sValidator},
                        ]
                    }
                >
                    <Input disabled={isDisabled} placeholder='以字母开头,只能包含小写字母、数字、-,最长48位' />
                </Form.Item>

                <Typography.Title level={5}>计算任务：</Typography.Title>
                <MyFormItem label="cpu资源:">
                    <Form.Item
                        name="compute_cpu_limit"
                        noStyle
                        rules={
                            [
                                {required: true, message: '此项为必填项'},
                            ]
                        }
                    >
                        <InputNumber placeholder='核' precision={0} min={1} />
                    </Form.Item>
                </MyFormItem>

                <MyFormItem label="内存资源:">
                    <Form.Item
                        name="compute_memory_limit"
                        noStyle
                        rules={
                            [
                                {required: true, message: '此项为必填项'},
                            ]
                        }
                    >
                        <InputNumber placeholder='G' precision={0} min={1} />
                    </Form.Item>
                </MyFormItem>

                <Form.Item
                    name="olap_namespace"
                    label="k8s命名空间"
                    rules={
                        [
                            {validator: k8sValidator},
                        ]
                    }
                >
                    <Input disabled={isDisabled} placeholder='以字母开头,只能包含小写字母、数字、-,最长48位' />
                </Form.Item>

                <Form.Item
                    name="operator_image_path"
                    label="Operator镜像地址"
                    rules={[
                        {
                            pattern: /^[a-zA-Z0-9\-\.]+\/[a-zA-Z0-9\-_]+\/[a-zA-Z0-9\-_]+(\:[a-z0-9\.]+)*$/,
                            message: '请输入正确的镜像地址格式(ie: example.com/org_1/xx_img:1.0.0)!',
                        },
                    ]}
                    extra='ie: example.com/org_1/xx_img:1.0.0'
                >
                    <Input />
                </Form.Item>

                <Form.Item
                    name="internal_algorithm_image_url"
                    label="算法镜像地址"
                    extra='ie: example.com/org_1/xx_img:1.0.0'
                    rules={[
                        {
                            pattern: /^[a-zA-Z0-9\-\.]+\/[a-zA-Z0-9\-_]+\/[a-zA-Z0-9\-_]+(\:[a-z0-9\.]+)*$/,
                            message: '请输入正确的镜像地址格式(ie: example.com/org_1/xx_img:1.0.0)!',
                        },
                    ]}
                >
                    <Input />
                </Form.Item>

                <Typography.Title level={5}>存储服务：</Typography.Title>
                <MyFormItem label="最大存储空间限制:">
                    <Form.Item
                        name="storage_limit"
                        noStyle
                        rules={
                            [
                                {required: true, message: '此项为必填项'},
                            ]
                        }
                    >
                        <InputNumber style={{width: 200}} placeholder='G' precision={0} min={1} />
                    </Form.Item>
                </MyFormItem>

                <hr />

                <Form.Item
                    name="graphspace_admin"
                    label="图空间管理员"
                >
                    <Select
                        mode="multiple"
                        allowClear
                        style={{width: '100%'}}
                        placeholder="选择图空间管理员"
                    >
                        {userList.length ? userSelect : null}
                    </Select>
                </Form.Item>

                <Form.Item
                    name="description"
                    label="描述"
                    rules={
                        [
                            {max: 128, message: '最多字符128位'},
                        ]
                    }
                >
                    <Input.TextArea placeholder='图空间描述，可选' />
                </Form.Item>
                {/* <Form.Item {...tailLayout}>
                    <Space>
                        <Button type="primary" htmlType="submit" loading={loading}>
                            {Object.keys(detail).length === 0 ? '创建' : '保存'}
                        </Button>
                        <Button htmlType="button" onClick={onCancel}>
                            取消
                        </Button>
                    </Space>
                </Form.Item> */}
            </Form>
        </Modal>
    );
};

export {EditLayer};
