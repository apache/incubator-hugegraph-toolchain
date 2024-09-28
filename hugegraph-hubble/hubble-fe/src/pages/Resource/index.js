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
    PageHeader,
    Row,
    Col,
    Button,
    Input,
    Space,
    Table,
    Divider,
    Modal,
    message,
    Select,
} from 'antd';
import {useCallback, useEffect, useState} from 'react';
import {EditLayer} from './EditLayer';
import {StatusText} from '../../components/Status';
import ListButton from '../../components/ListButton';
import TableHeader from '../../components/TableHeader';
import * as api from '../../api';
import * as user from '../../utils/user';

const Resource = () => {
    const [editLayerVisible, setEditLayerVisible] = useState(false);
    const [graphspace, setGraphspace] = useState(user.getDefaultGraphspace());
    const [graphspaceList, setGraphspaceList] = useState([]);
    const [refresh, setRefresh] = useState(false);
    const [query, setQuery] = useState('');
    const [op, setOp] = useState('detail');
    const [detail, setDetail] = useState({});
    const [data, setData] = useState([]);
    const [pagination, setPagination] = useState({current: 1, pageSize: 10});

    const handleEdit = useCallback(row => {
        setDetail(row);
        setOp('edit');
        setEditLayerVisible(true);
    }, []);

    const handleView = useCallback(row => {
        setDetail(row);
        setOp('view');
        setEditLayerVisible(true);
    }, []);

    const handleSearch = useCallback(value => {
        setQuery(value);
        setPagination({...pagination, current: 1});
    }, [pagination]);

    const handleDelete = useCallback(id => {
        Modal.confirm({
            title: `确定要删除${id}吗`,
            onOk: () => {
                api.auth.delResource(graphspace, id).then(res => {
                    if (res.status === 200) {
                        message.success('删除成功');
                        setRefresh(!refresh);

                        return;
                    }
                    message.error(res.message);
                });
            },
        });
    }, [graphspace, refresh]);

    const handleGraphspace = useCallback(data => {
        setGraphspace(data);
    }, []);

    const handleCreate = useCallback(() => {
        setOp('create');
        setEditLayerVisible(true);
    }, []);

    const handleTable = useCallback(newPagination => {
        setPagination(newPagination);
    }, []);

    const handleHideLayer = useCallback(() => {
        setEditLayerVisible(false);
    }, []);

    const handleRefresh = useCallback(() => {
        setRefresh(!refresh);
    }, [refresh]);

    const isDefault = name => /.*DEFAULT_SPACE_TARGET$/.test(name);

    const columns = [
        {
            title: '资源ID',
            dataIndex: 'id',
        },
        {
            title: '资源名',
            dataIndex: 'target_name',
        },
        {
            title: '图',
            dataIndex: 'target_graph',
        },
        {
            title: '资源备注',
            dataIndex: 'target_description',
        },
        {
            title: '创建时间',
            align: 'center',
            dataIndex: 'target_create',
        },
        {
            title: '操作',
            align: 'center',
            render: row => (
                <Space>
                    <StatusText onClick={handleEdit} data={row} disable={isDefault(row.target_name)}>编辑</StatusText>
                    <StatusText onClick={handleView} data={row}>详情</StatusText>
                    <StatusText onClick={handleDelete} data={row.id} disable={isDefault(row.target_name)}>
                        删除
                    </StatusText>
                </Space>
            ),
        },
    ];

    useEffect(() => {
        const userInfo = user.getUser();
        if (userInfo.is_superadmin) {
            api.manage.getGraphSpaceList({page_size: 99999}).then(res => {
                if (res.status === 200) {
                    setGraphspaceList(res.data.records);
                }
            });
        }
        else if (userInfo.resSpaces && userInfo.resSpaces.length > 0) {
            setGraphspaceList(userInfo.resSpaces);
        }
    }, []);

    useEffect(() => {
        api.auth.getResourceList(graphspace, {query, page_no: pagination.current}).then(res => {
            if (res.status === 200) {
                setData(res.data.records);
                setPagination({...pagination, total: res.data.total});
            }
        });
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [graphspace, refresh, pagination.current, query]);

    return (
        <>
            <PageHeader
                ghost={false}
                onBack={false}
                title={'资源管理'}
                extra={[
                    <Input.Search
                        key={1}
                        placeholder='请输入资源ID或资源名检索内容'
                        onSearch={handleSearch}
                        allowClear
                    />,
                ]}
            >
                <span>当前图空间:</span>
                <Select
                    onChange={handleGraphspace}
                    options={graphspaceList.map(item => ({label: item.name, value: item.name}))}
                    style={{width: 120}}
                    bordered={false}
                    defaultValue={graphspace}
                />
            </PageHeader>

            <div className='container'>
                <TableHeader>
                    <Button onClick={handleCreate} type='primary'>创建资源</Button>
                </TableHeader>
                <Table
                    columns={columns}
                    dataSource={data}
                    pagination={pagination}
                    onChange={handleTable}
                />
            </div>

            <EditLayer
                visible={editLayerVisible}
                detail={detail}
                onCancel={handleHideLayer}
                graphspace={graphspace}
                refresh={handleRefresh}
                op={op}
            />
        </>
    );
};

export default Resource;
