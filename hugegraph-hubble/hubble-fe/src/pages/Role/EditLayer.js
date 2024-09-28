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

import {Modal, Input, Form, Select, Transfer, message, Spin} from 'antd';
import {useCallback, useEffect, useState} from 'react';
import * as api from '../../api';
import * as rules from '../../utils/rules';
import _ from 'lodash';
import style from './index.module.scss';

const DetailLayer = ({visible, onCancel, detail}) => {
    const [data, setData] = useState({});
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (!visible || !detail.user_id) {
            return;
        }

        setLoading(true);
        api.auth.getUserInfo(detail.user_id).then(res => {
            setLoading(false);
            if (res.status === 200) {
                setData(res.data);
                return;
            }

            message.error(res.message);
        });
    }, [visible, detail.user_id]);

    return (
        <Modal
            title={'账号详情'}
            onCancel={onCancel}
            open={visible}
            footer={null}
            width={600}
        >
            <Spin spinning={loading}>
                <Form
                    labelCol={{span: 6}}
                    initialValues={data}
                >
                    <Form.Item label='账号名' className={style.item}>{data.user_nickname}</Form.Item>
                    <Form.Item label='账号ID' className={style.item}>{data.user_name}</Form.Item>
                    <Form.Item label='备注' className={style.item}>{data.user_description}</Form.Item>
                    <Form.Item label='管理权限' className={style.item}>
                        {data.adminSpaces ? data.adminSpaces.join(',') : ''}
                    </Form.Item>
                </Form>
            </Spin>
        </Modal>
    );
};

const AddAccountLayer = ({visible, onCancel, role, graphspace, refresh}) => {
    const [targetKeys, setTargetKeys] = useState([]);
    const [oldKeys, setOldKeys] = useState([]);
    const [userList, setUserList] = useState([]);
    const [isChange, setIsChange] = useState(false);

    const onChange = useCallback(nextTargetKeys => {
        setTargetKeys(nextTargetKeys);
        setIsChange(true);
    }, []);

    const renderItem = useCallback(item => item.id, []);

    const onFinish = useCallback(() => {
        const newArr = _.difference(targetKeys, oldKeys);
        const removeArr = _.difference(oldKeys, targetKeys).map(item => `${item}->ur->${role.id}`);

        api.auth.delRoleUserBatch(graphspace, {ids: removeArr}).then(() => {
            api.auth.addRoleUserBatch(graphspace, {
                role_id: role.id,
                user_ids: newArr,
            }).then(res => {
                if (res.status === 200) {
                    message.success('添加成功');
                    onCancel();
                    refresh();
                    return;
                }

                message.error(res.message);
            });
        });

        // if (newArr.length > 0) {
        //     api.auth.addRoleUserBatch(graphspace, {
        //         role_id: role.id,
        //         user_ids: newArr,
        //     }).then(res => {
        //         if (res.status === 200) {
        //             message.success('添加成功');
        //             onCancel();
        //             refresh();
        //             return;
        //         }

        //         message.error(res.message);
        //     });
        // }
    }, [role.id, graphspace, targetKeys, oldKeys, onCancel, refresh]);

    // const onSelectChange = (sourceSelectedKeys, targetSelectedKeys) => {
    //     setSelectedKeys([...sourceSelectedKeys, ...targetSelectedKeys]);
    // };

    useEffect(() => {
        if (!visible || !role.id) {
            return;
        }
        setIsChange(false);

        api.auth.getUserList().then(res => {
            if (res.status === 200) {
                setUserList(res.data.users.map(item => ({...item, key: item.id})));
            }
        });

        api.auth.getRoleUser(graphspace, {
            role_id: role.id,
            page_size: -1,
        }).then(res => {
            if (res.status === 200) {
                const keys = res.data.records.map(item => item.user_id);
                setTargetKeys(keys);
                setOldKeys(keys);
            }
        });
    }, [role.id, visible, graphspace]);

    return (
        <Modal
            title={'账号详情'}
            onCancel={onCancel}
            open={visible}
            onOk={onFinish}
            okButtonProps={{disabled: !isChange}}
            destroyOnClose
            width={600}
        >
            <div>
                <Transfer
                    dataSource={userList}
                    titles={['账号列表', '已选择账号']}
                    targetKeys={targetKeys}
                    showSearch
                    onChange={onChange}
                    // onSelectChange={onSelectChange}
                    render={renderItem}
                    listStyle={{margin: 'auto', height: 300}}
                />
            </div>
        </Modal>
    );
};

const AddRoleLayer = ({visible, onCancel, refresh, graphspace}) => {
    const [form] = Form.useForm();

    const onFinish = useCallback(() => {
        form.validateFields().then(values => {
            api.auth.addRole(graphspace, values).then(res => {
                if (res.status === 200) {
                    message.success('添加成功');
                    onCancel();
                    refresh();

                    return;
                }
                message.error(res.message);
            });
        });
    }, [form, graphspace, onCancel, refresh]);

    return (
        <Modal
            title={'创建角色'}
            onCancel={onCancel}
            open={visible}
            onOk={onFinish}
            destroyOnClose
            width={600}
        >
            <Form form={form} labelCol={{span: 4}} preserve={false}>
                <Form.Item
                    label='角色名'
                    rules={[rules.required()]}
                    name='role_name'
                >
                    <Input placeholder='请输入角色名' />
                </Form.Item>
                <Form.Item label='备注' name='role_description'>
                    <Input placeholder='请输入备注' />
                </Form.Item>
            </Form>
        </Modal>
    );
};

const AddResourceLayer = ({visible, onCancel, refresh, graphspace, role}) => {
    const [form] = Form.useForm();
    const [resourceList, setResourceList] = useState([]);

    const onFinish = useCallback(() => {
        form.validateFields().then(values => {
            api.auth.addRoleResource(graphspace, {
                role_id: role,
                target_id: values.target_id,
                permissions: ['READ', 'WRITE', 'DELETE', 'EXECUTE'],
            }).then(res => {
                if (res.status === 200) {
                    message.success('添加成功');
                    onCancel();
                    refresh();
                    return;
                }
                message.error(res.message);
            });
        });
    }, [form, graphspace, onCancel, refresh, role]);

    useEffect(() => {
        if (!visible) {
            return;
        }

        api.auth.getResourceList(graphspace, {page_size: 99999}).then(res => {
            if (res.status === 200) {
                setResourceList(res.data.records);
                return;
            }

            message.error(res.message);
        });
    }, [graphspace, visible]);

    return (
        <Modal
            title={'添加资源'}
            onCancel={onCancel}
            open={visible}
            onOk={onFinish}
            destroyOnClose
            width={600}
        >
            <Form form={form} labelCol={{span: 4}} preserve={false}>
                <Form.Item
                    label='资源名'
                    rules={[rules.required()]}
                    name='target_id'
                >
                    <Select
                        options={resourceList.map(item => ({label: item.target_name, value: item.id}))}
                        placeholder='请选择资源'
                    />
                </Form.Item>
            </Form>
        </Modal>
    );
};

const EditRoleLayer = ({visible, detail, onCancel, graphspace, refresh}) => {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);

    const onFinish = useCallback(() => {
        form.validateFields().then(values => {
            setLoading(true);
            api.auth.updateRole(graphspace, detail.id, {group_name: values.new_group_name}).then(res => {
                setLoading(false);
                if (res.status === 200) {
                    message.success('更改成功');
                    refresh();
                    onCancel();

                    return;
                }

                message.error(res.message);
            });
        });
    }, [detail.id, form, graphspace, onCancel, refresh]);

    useEffect(() => {
        if (!visible) {
            return;
        }

        form.setFieldsValue(detail);
    }, [visible, detail, form]);

    return (
        <Modal
            title='更改角色名'
            onCancel={onCancel}
            onOk={onFinish}
            open={visible}
            destroyOnClose
            confirmLoading={loading}
            width={600}
        >
            <Form form={form} labelCol={{span: 4}} preserve={false}>
                <Form.Item label='原名称' name='group_name'><Input disabled /></Form.Item>
                <Form.Item label='新名称' name='new_group_name' rules={[rules.required()]}><Input /></Form.Item>
            </Form>
        </Modal>
    );
};

export {DetailLayer, AddAccountLayer, AddRoleLayer, AddResourceLayer, EditRoleLayer};
