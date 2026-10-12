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

import {
    Modal,
    Form,
    Input,
    Radio,
    message,
} from 'antd';
import {useEffect, useState, useCallback, useRef} from 'react';
import {useTranslation} from 'react-i18next';
import * as rules from '../../../utils/rules';
import * as api from '../../../api';

const EditLayer = ({visible, onCancel, data, refresh}) => {
    const {t} = useTranslation();
    const [form] = Form.useForm();
    const [syncType, setSyncType] = useState('');
    const [loading, setLoading] = useState(false);
    const updateRequest = useRef(null);
    const datasourceType = data?.ingestion_mapping?.structs[0]?.input?.type;
    const scheduleOptions = datasourceType === 'KAFKA'
        ? [{label: t('task.sync.realtime'), value: 'REALTIME'}]
        : [
            {label: t('task.sync.once'), value: 'ONCE'},
            {label: t('task.sync.realtime'), value: 'REALTIME', disabled: true},
            {label: t('task.sync.cron'), value: 'CRON'},
        ];

    const handleSyncType = useCallback(e => {
        setSyncType(e.target.value);
    }, []);

    const onFinish = useCallback(() => {
        form.validateFields().then(values => {
            if (syncType !== 'CRON') {
                delete values.task_schedule_extend;
            }

            const request = Symbol('task-update');
            updateRequest.current = request;
            setLoading(true);
            // The OK spinner must clear on every outcome, including a rejected
            // request, otherwise the button stays locked with the modal open.
            api.manage.updateTask(data.task_id, values).then(res => {
                if (updateRequest.current !== request) {
                    return;
                }
                updateRequest.current = null;
                setLoading(false);

                if (res?.status === 200) {
                    message.success(t('task.edit.update_success'));
                    onCancel();
                    refresh();
                    return;
                }

                message.error(res?.message || t('common.msg.operation_failed'));
            }).catch(() => {
                if (updateRequest.current !== request) {
                    return;
                }
                updateRequest.current = null;
                setLoading(false);
                message.error(t('common.msg.operation_failed'));
            });
        }).catch(error => {
            if (!error?.errorFields) {
                message.error(t('common.msg.operation_failed'));
            }
        });
    }, [data.task_id, form, onCancel, refresh, syncType, t]);

    useEffect(() => {
        updateRequest.current = null;
        setLoading(false);
        if (!visible) {
            return;
        }
        api.manage.getTaskDetail(data.task_id).then(res => {
            if (res.status === 200) {
                setSyncType(res.data.task_schedule_type);
                form.setFieldsValue(res.data);

                return;
            }
            message.error(res.message);
        });

        return () => {
            updateRequest.current = null;
        };
    }, [visible, data.task_id, form]);

    const handleCancel = useCallback(() => {
        updateRequest.current = null;
        setLoading(false);
        onCancel();
    }, [onCancel]);

    return (
        <Modal
            title={t('task.edit_title_edit')}
            onCancel={handleCancel}
            open={visible}
            onOk={onFinish}
            confirmLoading={loading}
            destroyOnClose
        >
            <Form
                labelCol={{span: 4}}
                form={form}
                // initialValues={data}
                preserve={false}
            >
                <Form.Item label={t('task.edit.name')} name='task_name' rules={[rules.required()]}>
                    <Input placeholder={t('task.edit.name_placeholder')} showCount maxLength={20} />
                </Form.Item>
                <Form.Item
                    label={t('task.edit.sync_type')}
                    name='task_schedule_type'
                    rules={[rules.required()]}
                >
                    <Radio.Group
                        options={scheduleOptions}
                        onChange={handleSyncType}
                    />
                </Form.Item>

                {syncType === 'CRON' && (
                    <Form.Item
                        wrapperCol={{offset: 4, span: 14}}
                        extra={t('task.edit.cron_extra')}
                        name='task_schedule_extend'
                        rules={[rules.required(t('task.edit.cron_required')), rules.isCron]}
                        hidden={syncType !== 'CRON'}
                    >
                        <Input placeholder={t('task.edit.cron_placeholder')} />
                    </Form.Item>
                )}
            </Form>
        </Modal>
    );
};

export default EditLayer;
