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

import {Form, Input, Typography, Select, Space, Button, message} from 'antd';
import {useEffect, useState} from 'react';
import * as api from '../../api';
import * as rules from '../../utils/rules';

const BaseForm = ({cancel, next, visible, form, setSourceField, setDatasource}) => {
    const [datasourceOptions, setDatasourceOptions] = useState([]);
    const [graphspaceOptions, setGraphsapceOptions] = useState([]);
    const [graphOptions, setGraphOptions] = useState([]);
    const [selectGraphspace, setSelectGraphspace] = useState('');
    const [selectGraph, setSelectGraph] = useState('');
    const [selectDatasource, setSelectDatasource] = useState({});
    const [vertex, setVertex] = useState([]);
    const [edge, setEdge] = useState([]);

    const checkNext = () => {
        form.validateFields([
            'task_name',
            // 'datasource_id',
            ['ingestion_option', 'graphspace'],
            ['ingestion_option', 'graph'],
        ]).then(() => {
            setSourceField({vertex, edge});
            setDatasource(selectDatasource);
            next();
        });
    };

    useEffect(() => {
        api.manage.getDatasourceList({page_size: -1}).then(res => {
            if (res.status === 200) {
                setDatasourceOptions(res.data.records.map(item => ({
                    label: item.datasource_name,
                    value: BigInt(item.datasource_id.toString()),
                    info: item,
                })));

                return;
            }
            message.error(res.message);
        });

        api.manage.getGraphSpaceList({page_size: -1}).then(res => {
            if (res.status === 200) {
                setGraphsapceOptions(res.data.records.map(item => ({
                    label: item.nickname,
                    value: item.name,
                })));

                return;
            }
            message.error(res.message);
        });
    }, []);

    useEffect(() => {
        if (!selectGraphspace) {
            return;
        }

        api.manage.getGraphList(selectGraphspace, {page_size: -1}).then(res => {
            if (res.status === 200) {
                setGraphOptions(res.data.records.map(item => ({
                    label: item.nickname,
                    value: item.name,
                    disabled: (item.schemaview && item.schemaview.vertices.length === 0
                        && item.schemaview.edges.length === 0),
                })));

                return;
            }

            message.error(res.message);
        });

        setVertex([]);
        setEdge([]);
        form.resetFields([['ingestion_option', 'graph']]);
    }, [selectGraphspace]);

    useEffect(() => {
        if (!selectGraph) {
            return;
        }

        api.manage.getMetaVertexList(selectGraphspace, selectGraph).then(res => {
            if (res.status === 200) {
                setVertex(res.data.records);
                return;
            }

            message.error(res.message);
        });

        api.manage.getMetaEdgeList(selectGraphspace, selectGraph).then(res => {
            if (res.status === 200) {
                setEdge(res.data.records);
                return;
            }

            message.error(res.message);
        });
    }, [selectGraph]);

    return (
        <div style={{display: visible ? '' : 'none'}}>
            <Typography.Title level={5}>基本信息</Typography.Title>
            <Form.Item label='任务名称' name='task_name' rules={[rules.required()]}>
                <Input placeholder='请输入任务名称' showCount maxLength={20} />
            </Form.Item>
            <Form.Item label='数据源' wrapperCol={{span: 6}} name='datasource_id' rules={[rules.required()]}>
                <Select
                    options={datasourceOptions}
                    onChange={(_, options) => setSelectDatasource(options.info)}
                    placeholder='请选择数据源'
                />
            </Form.Item>
            <Form.Item label='目标' required>
                <Space>
                    <Form.Item name={['ingestion_option', 'graphspace']} rules={[rules.required('请选择图空间')]}>
                        <Select
                            placeholder='请选择图空间'
                            options={graphspaceOptions}
                            style={{width: 200}}
                            onChange={val => setSelectGraphspace(val)}
                        />
                    </Form.Item>
                    <Form.Item name={['ingestion_option', 'graph']} rules={[rules.required('请选择图')]}>
                        <Select
                            placeholder='请选择图'
                            options={graphOptions}
                            style={{width: 200}}
                            onChange={val => setSelectGraph(val)}
                        />
                    </Form.Item>
                </Space>
            </Form.Item>
            <Form.Item wrapperCol={{offset: 4}}>
                <Space>
                    <Button onClick={cancel}>取消</Button>
                    <Button type='primary' onClick={checkNext}>下一步</Button>
                </Space>
            </Form.Item>
        </div>
    );
};

export default BaseForm;
