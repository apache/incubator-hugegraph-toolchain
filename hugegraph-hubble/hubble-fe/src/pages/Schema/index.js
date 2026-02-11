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

import {useState, useEffect} from 'react';
import {Table, Space, PageHeader, Row, Col, Input, Button, message, Modal, Spin} from 'antd';
import EditLayer from './EditLayer';
import {useParams, useNavigate} from 'react-router-dom';
import * as api from '../../api/index';

const Schema = () => {
    const [data, setData] = useState([]);
    const [detail, setDetail] = useState({});
    const [mode, setMode] = useState('view');
    const [editLayer, setEditLayer] = useState(false);
    const [refresh, setRefresh] = useState(false);
    const [pagination, setPagination] = useState({current: 1, pageSize: 10});
    const [query, setQuery] = useState('');
    const [graphspaceInfo, setGraphspaceInfo] = useState({});
    const [loading, setLoading] = useState(false);
    const {graphspace} = useParams();
    const navigate = useNavigate();

    const viewSchema = data => {
        setMode('view');
        setDetail(data);
        setEditLayer(true);
    };

    const editSchema = data => {
        setMode('edit');
        setDetail(data);
        setEditLayer(true);
    };

    const createSchema = () => {
        setMode('create');
        setDetail({});
        setEditLayer(true);
    };

    const handleTable = newPagination => {
        setPagination(newPagination);
    };

    const onSearch = val => {
        setQuery(val);
    };

    const deleteSchema = row => {
        Modal.confirm({
            title: `确定要删除 ${row.name}吗？`,
            onOk: () => {
                api.manage.delSchema(graphspace, row.name).then(res => {
                    if (res.status === 200) {
                        message.success('删除成功');
                        setRefresh(!refresh);
                        return;
                    }

                    message.error('删除失败');
                });
            },
        });
    };

    const columns = [
        {
            title: 'schema模版名称',
            dataIndex: 'name',
        },
        {
            title: '创建时间',
            dataIndex: 'create_time',
        },
        {
            title: '更新时间',
            dataIndex: 'update_time',
        },
        {
            title: '创建人',
            dataIndex: 'creator',
        },
        {
            title: '操作',
            render: row => {
                return (
                    <Space>
                        <a onClick={() => viewSchema(row)}>查看</a>
                        <a onClick={() => editSchema(row)}>编辑</a>
                        <a onClick={() => deleteSchema(row)}>删除</a>
                    </Space>
                );
            },
        },
    ];

    useEffect(() => {
        api.manage.getGraphSpace(graphspace).then(res => {
            if (res.status === 200) {
                setGraphspaceInfo(res.data);
                return;
            }

            message.error(res.message);
        });
    }, []);

    useEffect(() => {
        api.manage.getSchemaList(graphspace, {
            query,
            page_no: pagination.current,
        }).then(res => {
            if (res.status === 200) {
                setData(res.data.records);
                setPagination({...pagination, total: res.data.total});
            }
        });
    }, [refresh, pagination.current, query]);

    return (
        <>
            <Spin spinning={loading}>
                <PageHeader
                    ghost={false}
                    onBack={() => navigate('/graphspace')}
                    title={`${graphspaceInfo.nickname} - Schema模版管理`}
                >
                    <Row justify='space-between'>
                        <Col>
                            <Space>
                                <Button type='primary' onClick={createSchema}>创建schema模版</Button>
                            </Space>
                        </Col>
                        <Col><Input.Search placeholder='请输入schema模版名称' onSearch={onSearch} /></Col>
                    </Row>
                </PageHeader>

                <div className='container'>
                    <Table
                        columns={columns}
                        dataSource={data}
                        bordered
                        size='small'
                        pagination={pagination}
                        onChange={handleTable}
                    />
                    <EditLayer
                        visible={editLayer}
                        detail={detail}
                        mode={mode}
                        onCancel={() => setEditLayer(false)}
                        graphspace={graphspace}
                        refresh={() => setRefresh(!refresh)}
                    />
                </div>
            </Spin>
        </>
    );
};

export default Schema;
