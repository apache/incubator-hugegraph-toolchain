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

import {Button, Row, Col, PageHeader, Input, Modal, Table, Space, message} from 'antd';
import {useState, useEffect, useCallback} from 'react';
import EditLayer from './EditLayer';
import TableHeader from '../../components/TableHeader';
import {sourceTypeOptions} from './config';
import * as api from '../../api';

const Datasource = () => {
    const [data, setData] = useState([]);
    const [selectedItems, setSelectedItems] = useState([]);
    const [editLayer, setEditLayer] = useState(false);
    const [refresh, setRefresh] = useState(false);
    const [query, setQuery] = useState('');
    const [pagination, setPagination] = useState({pageSize: 10, current: 1});

    const delDatasource = datasourceID => {
        api.manage.delDatasource(datasourceID).then(res => {
            if (res.status === 200) {
                message.success('删除成功');
                setRefresh(!refresh);
                return;
            }

            message.error(res.message);
        });
    };

    const delBatchDatasource = useCallback(list => {
        api.manage.delBatchDatasource(list).then(res => {
            if (res.status === 200) {
                message.success('删除成功');
                setRefresh(!refresh);

                return;
            }

            message.error(res.message);
        });
    }, [refresh]);

    const handleTable = useCallback(newPagination => {
        setPagination(newPagination);
    }, []);

    const handleSearch = useCallback(val => setQuery(val), []);

    const handleShowLayer = useCallback(() => setEditLayer(true), []);

    const handleHideLayer = useCallback(() => setEditLayer(false), []);

    const handleRefresh = useCallback(() => setRefresh(!refresh), [refresh]);

    const rowKey = useCallback(record => record.datasource_id, []);

    const columns = [
        {
            title: '数据源名称',
            dataIndex: 'datasource_name',
        },
        {
            title: '数据源类型',
            dataIndex: 'datasource_config',
            width: 200,
            align: 'center',
            render: config => sourceTypeOptions.find(item => item.value === config.type).label,
        },
        {
            title: '创建人',
            dataIndex: 'creator',
            align: 'center',
            width: 200,
        },
        {
            title: '创建时间',
            dataIndex: 'create_time',
            width: 240,
            align: 'center',
        },
        {
            title: '操作',
            dataIndex: 'datasource_id',
            align: 'center',
            width: 140,
            render: val => {
                return (
                    <Space>
                        <a onClick={() => Modal.confirm({
                            title: '删除数据源',
                            content: '删除后，该数据源将从系统中消失。您确定要删除这个数据源吗？',
                            onOk() {
                                delDatasource(val);
                            },
                        })}
                        >删除
                        </a>
                    </Space>
                );
            },
        },
    ];

    const delBatch = useCallback(() => {
        if (selectedItems.length === 0) {
            message.error('至少选择一项');
            return;
        }

        Modal.confirm({
            title: '删除数据源',
            content: '删除后，该数据源将从系统中消失。您确定要删除这个数据源吗？',
            onOk() {
                // delDatasource(val);
                delBatchDatasource(selectedItems);
                setSelectedItems([]);
            },
        });
    }, [delBatchDatasource, selectedItems]);

    useEffect(() => {
        api.manage.getDatasourceList({
            query,
            page_no: pagination.current,
        }).then(res => {
            if (res.status === 200) {
                setData(res.data.records);
                setPagination({...pagination, total: res.data.total});
            }
        });
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [refresh, query, pagination.current]);

    return (
        <>
            <PageHeader
                ghost={false}
                onBack={false}
                title="数据源管理"
            >
                <Row justify='end'>
                    <Col><Input.Search placeholder='请输入数据源名称' onSearch={handleSearch} /></Col>
                </Row>
            </PageHeader>

            <div className='container'>
                <TableHeader>
                    <Space>
                        <Button type='primary' onClick={handleShowLayer}>新增数据源</Button>
                        <Button onClick={delBatch}>删除数据源</Button>
                        <span>已选中{selectedItems.length}条/共{data.length}条</span>
                    </Space>
                </TableHeader>
                <Table
                    columns={columns}
                    rowKey={rowKey}
                    dataSource={data}
                    rowSelection={{
                        type: 'checkbox',
                        onChange: selectedRowKeys => {
                            setSelectedItems(selectedRowKeys);
                        },
                    }}
                    pagination={pagination}
                    onChange={handleTable}
                />
                <EditLayer
                    visible={editLayer}
                    onCancel={handleHideLayer}
                    refresh={handleRefresh}
                />
            </div>
        </>
    );
};

export default Datasource;
