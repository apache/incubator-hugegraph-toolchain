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

import {Modal, Input, Form, message} from 'antd';
import * as rules from '../../utils/rules';
import * as api from '../../api';
import {useState, useCallback} from 'react';
import {useTranslation} from 'react-i18next';

const EditLayer = ({visible, onCancel, data, refresh}) => {
    const {t} = useTranslation();
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);

    const onFinish = useCallback(() => {
        form.validateFields().then(values => {
            setLoading(true);
            api.auth.updatePersonal({
                // user_name: values.user_name,
                nickname: values.user_nickname,
                description: values.user_description,
            }).then(res => {
                setLoading(false);
                if (res.status === 200) {
                    onCancel();
                    refresh();
                    return;
                }

                message.error(res.message);
            }).catch(() => {
                setLoading(false);
            });
        });
    }, [form, onCancel, refresh]);

    return (
        <Modal
            title={t('my.edit_profile_title')}
            onCancel={onCancel}
            open={visible}
            onOk={onFinish}
            confirmLoading={loading}
            width={600}
        >
            <Form
                labelCol={{span: 6}}
                initialValues={data}
                form={form}
            >
                <Form.Item label={t('my.col.id')} name='user_name'><Input disabled /></Form.Item>
                <Form.Item
                    label={t('my.col.name')}
                    name='user_nickname'
                    rules={[rules.required(), rules.isAccountName(t('my.edit.account_name_rule'))]}
                >
                    <Input />
                </Form.Item>
                <Form.Item label={t('my.col.remark')} name='user_description'><Input /></Form.Item>
            </Form>
        </Modal>
    );
};

export default EditLayer;
