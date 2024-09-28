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

import {Typography, Form, Radio, Input, Space, Button, Select} from 'antd';
import {useState} from 'react';
import * as rules from '../../../utils/rules';

const ScheduleForm = ({prev, visible, datasource, loading}) => {
    const [scheduleForm] = Form.useForm();
    const [syncType, setSyncType] = useState(0);
    // console.log(datasource);
    const datasourceType = datasource?.datasource_config?.type;
    const scheduleOptions = datasourceType === 'KAFKA'
        ? [{label: '实时执行', value: 'REALTIME'}]
        : [
            {label: '执行一次', value: 'ONCE'},
            {label: '实时执行', value: 'REALTIME', disabled: true},
            {label: '周期执行', value: 'CRON'},
        ];

    return (
        <div style={{display: visible ? '' : 'none'}}>
            <Form
                form={scheduleForm}
                name='schedule_form'
                initialValues={{
                    task_schedule_type: 'ONCE',
                    task_schedule_status: 'ENABLE',
                    task_load_type: 'FULL',
                }}
            >
                <Typography.Title level={5}>同步方式</Typography.Title>
                <Form.Item label='同步方式' required name='task_schedule_type'>
                    <Radio.Group
                        options={scheduleOptions}
                        onChange={e => {
                            setSyncType(e.target.value);
                        }}
                    />
                </Form.Item>
                {syncType === 'CRON'
                && (
                    <>
                        <Form.Item
                            wrapperCol={{offset: 4, span: 14}}
                            extra='quartz表达式：秒 分钟 小时 天 月 星期 年(选填)'
                            name='task_schedule_extend'
                            rules={[rules.required('请输入调度信息'), rules.isCron]}
                        >
                            <Input placeholder='定时 * 5 * * * * (*)' />
                        </Form.Item>
                    </>
                )
                }
                {/* <Typography.Title level={5}>调度信息</Typography.Title> */}
                {/* <Form.Item label='失败重试次数' name={['ingestion_option', 'max_read_errors']}>
                    <InputNumber min={0} max={10} />
                </Form.Item>
                <Form.Item label='超时时间' name='timeout'>
                    <InputNumber min={0} formatter={value => `${value}秒`} />
                </Form.Item> */}
                {(syncType === 'CRON' && ['KAFKA, JDBC'].includes(datasource?.datasource_config?.type)) ? (
                    <Form.Item label='同步方式' name='task_load_type' wrapperCol={{span: 4}}>
                        <Select options={[{label: '全量', value: 'FULL'}, {label: '增量', value: 'INCREMENTAL'}]} />
                    </Form.Item>
                ) : <Form.Item name='task_load_type' hidden />}
                <Form.Item name='task_schedule_status' hidden />

                <Form.Item wrapperCol={{offset: 4}}>
                    <Space>
                        <Button onClick={prev}>上一步</Button>
                        <Button type='primary' htmlType='submit' loading={loading}>确定</Button>
                    </Space>
                </Form.Item>
            </Form>
        </div>
    );
};

export default ScheduleForm;
