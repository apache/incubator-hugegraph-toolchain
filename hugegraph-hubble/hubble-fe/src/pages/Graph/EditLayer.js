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
import {useState, useEffect, useCallback} from 'react';
import * as api from '../../api/index';
import * as rules from '../../utils/rules';
import {byteConvert, timeConvert} from '../../utils/format';
import style from './index.module.scss';

const EditLayer = ({visible, onCancel, refresh, graphspace, graph}) => {
    const [schemaList, setSchemaList] = useState([]);
    const [loading, setLoading] = useState(false);
    const [form] = Form.useForm();

    const onFinish = useCallback(() => {
        form.validateFields().then(values => {
            setLoading(true);

            if (graph) {
                // 编辑
                api.manage.updateGraph(graphspace, graph, {nickname: values.nickname}).then(res => {
                    setLoading(false);
                    if (res.status === 200) {
                        message.success('添加成功');
                        onCancel();
                        refresh();
                        return;
                    }
                    message.error(res.message);
                });
                return;
            }

            // 新增
            api.manage.addGraph(graphspace, {...values, auth: false, graphspace}).then(res => {
                setLoading(false);
                if (res.status === 200) {
                    message.success('添加成功');
                    onCancel();
                    refresh();
                    return;
                }
                message.error(res.message);
            });
        });
    }, [form, graphspace, graph, refresh, onCancel]);

    useEffect(() => {
        if (!visible) {
            return;
        }

        api.manage.getSchemaList(graphspace).then(res => {
            if (res.status === 200) {
                setSchemaList(res.data.records);
                return;
            }

            message.error(res.message);
        });

        if (graph) {
            api.manage.getGraph(graphspace, graph).then(res => {
                if (res.status === 200) {
                    form.setFieldsValue({...res.data, graph: res.data.name});
                }
            });
        }
    }, [visible, graph, form, graphspace]);

    return (
        <Modal
            open={visible}
            onCancel={onCancel}
            title={graph ? '编辑' : '创建'}
            destroyOnClose
            width={600}
            onOk={onFinish}
            confirmLoading={loading}
            maskClosable={false}
        >
            <Form
                form={form}
                labelCol={{span: 6}}
                preserve={false}
            >
                <Form.Item
                    label='图ID'
                    rules={[rules.required(), rules.isName, {type: 'string', max: 48}]}
                    name='graph'
                >
                    <Input placeholder='只能包含小写字母、数字、_，最长48位' disabled={graph} />
                </Form.Item>
                <Form.Item
                    label='图名'
                    rules={[rules.required(), rules.isPropertyName, {type: 'string', max: 12}]}
                    name='nickname'
                >
                    <Input placeholder='只能包含中文、字母、数字、_，最长12位' />
                </Form.Item>
                {!graph && (
                    <Form.Item
                        label='schema'
                        name='schema'
                    >
                        <Select
                            placeholder='请选择schema'
                            options={schemaList.map(item => ({label: item.name, value: item.name}))}
                        />
                    </Form.Item>
                )}
            </Form>
        </Modal>
    );
};

const ViewLayer = ({visible, onCancel, graphspace, graph}) => {
    const [info, setInfo] = useState('');

    useEffect(() => {
        if (!visible) {
            return;
        }

        api.manage.getGraphSchema(graphspace, graph).then(res => {
            if (res.status === 200) {
                setInfo(res.data.schema);
                return;
            }
            message.error(res.message);
        });
    }, [visible, graphspace, graph]);

    return (
        <Modal
            open={visible}
            onCancel={onCancel}
            title='查看schema'
            width={600}
            onOk={onCancel}
            footer={null}
            maskClosable={false}
        >
            {info}
        </Modal>
    );
};

const CloneLayer = ({open, onCancel, refresh, graphspace, graph}) => {
    const [form] = Form.useForm();
    const [graphspaceList, setGraphspaceList] = useState([]);
    const [detail, setDetail] = useState({});
    const [loading, setLoading] = useState(false);

    // 不等
    const notEq = (str, msg) => ({
        validator(_, value) {
            if (value !== str) {
                return Promise.resolve();
            }

            return Promise.reject(new Error(msg));
        },
    });

    const handleClone = useCallback(() => {
        form.validateFields().then(res => {
            setLoading(true);
            api.manage.cloneGraph(graphspace, graph, {
                graphspace: res.graphspace,
                nickname: res.nickname,
                name: res.name,
                load_data: res.load_data,
            }).then(res => {
                setLoading(false);
                if (res.status === 200) {
                    message.success('克隆成功');
                    onCancel();
                    refresh();
                    return;
                }

                message.error(res.message);
            });
        });
    }, [form, graphspace, graph, onCancel]);

    useEffect(() => {
        if (!open) {
            return;
        }

        api.manage.getGraph(graphspace, graph).then(res => {
            if (res.status === 200) {
                setDetail(res.data);
                form.setFieldsValue({
                    name: res.data.name,
                    nickname: `${res.data.nickname}复制`,
                });
            }
        });

        api.manage.getGraphSpaceList({page_size: -1}).then(res => {
            if (res.status === 200) {
                setGraphspaceList(res.data.records);
                return;
            }
            message.error(res.message);
        });
    }, [open, graphspace, graph, form]);

    return (
        <Modal
            open={open}
            onCancel={onCancel}
            title='克隆图'
            width={600}
            onOk={handleClone}
            maskClosable={false}
            confirmLoading={loading}
            destroyOnClose
        >
            <Form
                form={form}
                labelCol={{span: 6}}
                preserve={false}
                initialValues={{
                    graphspace,
                    load_data: 0,
                }}
            >
                <Form.Item
                    label='克隆图ID'
                    rules={[
                        rules.required(),
                        rules.isName,
                        {type: 'string', max: 48},
                        notEq(graph, '不可以和原图ID重复'),
                    ]}
                    name='name'
                >
                    <Input placeholder='不可以和原图ID重复' />
                </Form.Item>
                <Form.Item
                    label='克隆图名称'
                    rules={[
                        rules.required(),
                        rules.isPropertyName,
                        {type: 'string', max: 48},
                        notEq(detail.nickname, '不可以和原图名称重复'),
                    ]}
                    name='nickname'
                >
                    <Input placeholder='不可以和原图名称重复' />
                </Form.Item>
                <Form.Item
                    label='存储图空间'
                    rules={[rules.required()]}
                    name='graphspace'
                >
                    <Select options={graphspaceList.map(item => ({label: item.nickname, value: item.name}))} />
                </Form.Item>
                <Form.Item
                    label='克隆内容'
                    name='load_data'
                    required
                >
                    <Select
                        options={[
                            {label: '克隆schema', value: 0},
                            {label: '克隆schema+数据', value: 1},
                        ]}
                    />
                </Form.Item>
                <Form.Item label='所需硬盘为' className={style.form_item}>{byteConvert(detail.storage)}</Form.Item>
                {/* <Form.Item label='所需内存为' className={style.form_item}>111</Form.Item> */}
                {/* 复制时间按4G/h估算 */}
                <Form.Item label='预计复制时间' className={style.form_item}>
                    {timeConvert(detail.storage * 3600 / (4 * 1024 * 1024))}
                </Form.Item>
            </Form>
        </Modal>
    );
};

export {EditLayer, ViewLayer, CloneLayer};
