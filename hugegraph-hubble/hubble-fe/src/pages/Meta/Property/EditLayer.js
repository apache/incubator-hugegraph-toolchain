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

import {Modal, Form, Input, Select, message} from 'antd';
import {useCallback, useState} from 'react';
import * as api from '../../../api';
import * as rules from '../../../utils/rules';
import {
    dataTypeOptions,
    cardinalityOptions,
} from '../common/config';


const EditPropertyLayer = ({visible, onCancle, graphspace, graph, refresh}) => {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);

    const onFinish = useCallback(() => {
        form.validateFields().then(values => {
            setLoading(true);

            api.manage.addMetaProperty(graphspace, graph, values).then(res => {
                setLoading(false);
                if (res.status === 200) {
                    refresh();
                    onCancle();
                    message.success('添加成功');
                    return;
                }

                message.error(res.message);
            });
        });
    }, [form, graph, graphspace, onCancle, refresh]);

    return (
        <Modal
            title='创建'
            open={visible}
            onCancel={onCancle}
            onOk={onFinish}
            confirmLoading={loading}
            destroyOnClose
            width={600}
        >
            <Form
                form={form}
                labelCol={{span: 6}}
                preserve={false}
                initialValues={{data_type: 'TEXT', cardinality: 'SINGLE'}}
            >
                <Form.Item
                    label='属性名称'
                    name='name'
                    rules={[rules.required(), rules.isPropertyName, {type: 'string', max: 128}]}
                >
                    <Input placeholder='允许出现中英文、数字、下划线' max={128} />
                </Form.Item>
                <Form.Item label='数据类型' name='data_type' rules={[rules.required()]} wrapperCol={{span: 6}}>
                    <Select options={dataTypeOptions} />
                </Form.Item>
                <Form.Item label='基数' name='cardinality' rules={[rules.required()]} wrapperCol={{span: 6}}>
                    <Select options={cardinalityOptions} />
                </Form.Item>
            </Form>
        </Modal>
    );
};


export {EditPropertyLayer};
