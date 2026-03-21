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
import {useEffect, useState, useCallback} from 'react';
import * as rules from '../../../utils/rules';
import * as api from '../../../api';

const EditLayer = ({visible, onCancel, data, refresh}) => {
    const [form] = Form.useForm();
    const [syncType, setSyncType] = useState('');
    const datasourceType = data?.ingestion_mapping?.structs[0]?.input?.type;
    const scheduleOptions = datasourceType === 'KAFKA'
        ? [{label: '实时执行', value: 'REALTIME'}]
        : [
            {label: '执行一次', value: 'ONCE'},
            {label: '实时执行', value: 'REALTIME', disabled: true},
            {label: '周期执行', value: 'CRON'},
        ];

    const handleSyncType = useCallback(e => {
        setSyncType(e.target.value);
    }, []);

    const onFinish = useCallback(() => {
        form.validateFields().then(values => {
            if (syncType !== 'CRON') {
                delete values.task_schedule_extend;
            }

            api.manage.updateTask(data.task_id, values).then(res => {
                if (res.status === 200) {
                    message.success('修改成功');
                    onCancel();
                    refresh();
                    return;
                }

                message.error(res.message);
            });
        });
    }, [data.task_id, form, onCancel, refresh, syncType]);

    useEffect(() => {
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

    }, [visible, data.task_id, form]);

    return (
        <Modal
            title='编辑任务'
            onCancel={onCancel}
            open={visible}
            onOk={onFinish}
            destroyOnClose
        >
            <Form
                labelCol={{span: 4}}
                form={form}
                // initialValues={data}
                preserve={false}
            >
                <Form.Item label='任务名称' name='task_name' rules={[rules.required()]}>
                    <Input placeholder='请输入任务名称' showCount maxLength={20} />
                </Form.Item>
                <Form.Item label='同步方式' name='task_schedule_type' rules={[rules.required()]}>
                    <Radio.Group
                        options={scheduleOptions}
                        onChange={handleSyncType}
                    />
                </Form.Item>

                {syncType === 'CRON' && (
                    <Form.Item
                        wrapperCol={{offset: 4, span: 14}}
                        extra='quartz表达式：秒 分钟 小时 天 月 星期 年(选填)'
                        name='task_schedule_extend'
                        rules={[rules.required('请输入调度信息'), rules.isCron]}
                        hidden={syncType !== 'CRON'}
                    >
                        <Input placeholder='定时 * 5 * * * * (*)' />
                    </Form.Item>
                )}
            </Form>
        </Modal>
    );
};

export default EditLayer;
