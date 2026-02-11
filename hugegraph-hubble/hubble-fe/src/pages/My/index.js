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

import {PageHeader, Button, Form, Input, Space, message, Spin} from 'antd';
import {useEffect, useState, useCallback} from 'react';
import style from './index.module.scss';
import EditLayer from './EditLayer';
import * as api from '../../api';
import * as rules from '../../utils/rules';

const My = () => {
    const [editLayerVisible, setEditLayerVisible] = useState(false);
    const [changePass, setChangePass] = useState(false);
    const [refresh, setRefresh] = useState(false);
    const [data, setData] = useState({});
    const [loading, setLoading] = useState(false);
    const [spinning, setSpinning] = useState(true);
    const [form] = Form.useForm();

    const handleForm = useCallback(() => {
        setLoading(true);
        form.validateFields().then(values => {
            const {old_password, user_password} = values;
            api.auth.updatePwd(data.user_name, old_password, user_password).then(res => {
                if (res.status === 200) {
                    message.success('更改成功');
                    setChangePass(false);
                    return;
                }

                message.error(res.message);
            });
        });
    }, [data.user_name, form]);

    const handleChange = useCallback(() => {
        setChangePass(true);
        form.resetFields();
    }, [form]);

    const handleShowLayer = useCallback(() => {
        setEditLayerVisible(true);
    }, []);

    const handleHideLayer = useCallback(() => {
        setEditLayerVisible(false);
    }, []);

    const handleRefresh = useCallback(() => {
        setRefresh(!refresh);
    }, [refresh]);

    const handleShowAccount = useCallback(() => {
        setChangePass(false);
    }, []);

    useEffect(() => {
        api.auth.getPersonal().then(res => {
            setSpinning(false);
            if (res.status === 200) {
                setData(res.data);
                return;
            }

            message.error(res.message);
        });
    }, [refresh]);

    return (
        <>
            <PageHeader
                ghost={false}
                onBack={false}
                title={'个人中心'}
                extra={[
                    <Button key='1' onClick={handleShowLayer}>编辑信息</Button>,
                    <Button key='2' onClick={handleChange}>更改密码</Button>,
                ]}
            />

            <div className='container'>
                <Form
                    className={style.form}
                    labelCol={{span: 6}}
                    initialValues={{user_name: data.user_name}}
                    form={form}
                >
                    {changePass === false
                        ? (
                            <Spin spinning={spinning}>
                                <Form.Item label='账号ID' className={style.item}>{data.user_name}</Form.Item>
                                <Form.Item label='账号名' className={style.item}>{data.user_nickname}</Form.Item>
                                <Form.Item label='备注' className={style.item}>{data.user_description}</Form.Item>
                                <Form.Item label='权限及角色' className={style.item}>
                                    {data.adminSpaces && data.adminSpaces.join(',')}
                                </Form.Item>
                                <Form.Item label='创建时间' className={style.item}>{data.user_create}</Form.Item>
                            </Spin>
                        )
                        : (
                            <>
                                <Form.Item label='账号名' name='user_name'>
                                    <Input disabled />
                                </Form.Item>
                                <Form.Item label='旧密码' name='old_password'>
                                    <Input.Password autoComplete='new-password' placeholder='请输入旧密码' />
                                </Form.Item>
                                <Form.Item
                                    label='新密码'
                                    name='user_password'
                                    rules={[rules.required(), {type: 'string', min: 5, max: 16}]}
                                >
                                    <Input.Password placeholder='请输入新密码' />
                                </Form.Item>
                                <Form.Item
                                    label='确认密码'
                                    name='repeat_password'
                                    dependencies={['user_password']}
                                    hasFeedback
                                    rules={[
                                        rules.required(),
                                        ({getFieldValue}) => ({
                                            validator(_, value) {
                                                if (!value || getFieldValue('user_password') === value) {
                                                    return Promise.resolve();
                                                }

                                                return Promise.reject(new Error('两次密码不一致'));
                                            },
                                        }),
                                    ]}
                                >
                                    <Input.Password placeholder='请再次输入密码' />
                                </Form.Item>
                                <Form.Item wrapperCol={{offset: 6}}>
                                    <Space>
                                        <Button onClick={handleShowAccount}>取消</Button>
                                        <Button type='primary' onClick={handleForm} loading={loading}>确定</Button>
                                    </Space>
                                    {/* <div className={Style.desc}>上次更改密码时间：2022-06-24 10:44:22</div> */}
                                </Form.Item>
                            </>
                        )}
                </Form>
            </div>

            <EditLayer
                visible={editLayerVisible}
                onCancel={handleHideLayer}
                data={data}
                refresh={handleRefresh}
            />
        </>
    );
};

export default My;
