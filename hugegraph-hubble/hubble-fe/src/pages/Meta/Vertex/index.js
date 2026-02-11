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

import {Table, Space, Button, message, Modal} from 'antd';
import {useState, useEffect, useCallback} from 'react';
import {useParams} from 'react-router-dom';
import {EditVertexLayer} from './EditLayer';
import TableHeader from '../../../components/TableHeader';
import * as api from '../../../api';

const VertexTable = () => {
    const [editLayerVisible, setEditLayerVisible] = useState(false);
    const [data, setData] = useState([]);
    const [pagination, setPagination] = useState({current: 1, total: 10});
    const [refresh, setRefresh] = useState(false);
    const [selectedItems, setSelectedItems] = useState([]);
    const [vertexName, setVertexName] = useState('');
    const [propertyList, setPropertyList] = useState([]);
    const {graphspace, graph} = useParams();

    const handleTable = useCallback(newPagination => {
        setPagination(newPagination);
    }, []);

    const removeVertex = useCallback((names, isBatch) => {
        api.manage.checkMetaVertex(graphspace, graph, {names}).then(res => {
            if (res.status !== 200) {
                message.error(res.message);
                return;
            }

            const inUse = names.filter(name => res.data[name] === true);
            if (inUse.length > 0) {
                message.error(`顶点数据 ${inUse.join(',')} 正在使用中，不可删除`);
                return;
            }

            Modal.confirm({
                title: '确认删除此顶点类型？',
                content: (
                    <><div>删除后无法恢复，请谨慎操作</div>
                        <div>删除元数据耗时较久，详情可在任务管理中查看</div>
                    </>),
                onOk: () => {
                    api.manage.delMetaVertex(graphspace, graph, {names}).then(res => {
                        if (res.status !== 200) {
                            message.error(res.message);
                            return;
                        }

                        if (isBatch) {
                            setSelectedItems([]);
                        }

                        message.success('删除成功');
                        setRefresh(!refresh);
                    });
                },
            });
        });
    }, [graph, graphspace, refresh]);

    const handleDelete = useCallback(row => {
        removeVertex([row.name]);
    }, [removeVertex]);

    const handleEdit = useCallback(row => {
        setVertexName(row.name);
        setEditLayerVisible(true);
    }, []);

    const handleCreate = useCallback(() => {
        setVertexName('');
        setEditLayerVisible(true);
    }, []);

    const handleRefresh = useCallback(() => {
        setRefresh(!refresh);
    }, [refresh]);

    const handleHideLayer = useCallback(() => {
        setEditLayerVisible(false);
    }, []);

    const rowKey = useCallback(item => item.name, []);

    const handleDeleteBatch = useCallback(() => {
        if (selectedItems.length === 0) {
            message.error('请至少选择一项');
            return;
        }

        removeVertex(selectedItems, true);
    }, [selectedItems, removeVertex]);

    const columns = [
        {
            title: '顶点类型名称',
            dataIndex: 'name',
        },
        {
            title: '关联属性',
            dataIndex: 'properties',
            ellipsis: true,
            render: val => val.map(item => item.name).join(';'),
        },
        {
            title: 'ID策略',
            dataIndex: 'id_strategy',
        },
        {
            title: '主键属性',
            dataIndex: 'primary_keys',
            render: val => val.join(','),
        },
        {
            title: '类型索引',
            dataIndex: 'open_label_index',
            render: val => (val ? '是' : '否'),
        },
        {
            title: '属性索引',
            dataIndex: 'property_indexes',
            ellipsis: true,
            render: val => val.map(item => item.name).join(';'),
        },
        {
            title: '操作',
            align: 'center',
            render: val => (
                <Space>
                    <a onClick={() => handleEdit(val)}>编辑</a>
                    <a onClick={() => handleDelete(val)}>删除</a>
                </Space>
            ),
        },
    ];

    useEffect(() => {
        api.manage.getMetaVertexList(graphspace, graph, {
            page_no: pagination.current,
        }).then(res => {
            if (res.status === 200) {
                setData(res.data.records);
                setPagination({...pagination, total: res.data.total});
            }
        });
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [refresh, pagination.current]);

    useEffect(() => {
        api.manage.getMetaPropertyList(graphspace, graph, {page_size: -1}).then(res => {
            if (res.status === 200) {
                setPropertyList(res.data.records.map(item => ({
                    lable: item.name,
                    value: item.name,
                    data_type: item.data_type,
                })));
            }
        });
    }, [graph, graphspace]);

    return (
        <>
            <TableHeader>
                <Space>
                    <Button type='primary' onClick={handleCreate}>创建</Button>
                    <Button onClick={handleRefresh}>刷新</Button>
                    <Button onClick={handleDeleteBatch}>批量删除</Button>
                </Space>
            </TableHeader>

            <Table
                columns={columns}
                dataSource={data}
                rowSelection={{
                    type: 'checkbox',
                    onChange: selectedRowKeys => {
                        setSelectedItems(selectedRowKeys);
                    },
                }}
                pagination={pagination}
                onChange={handleTable}
                rowKey={rowKey}
            />

            <EditVertexLayer
                visible={editLayerVisible}
                onCancle={handleHideLayer}
                graph={graph}
                graphspace={graphspace}
                refresh={handleRefresh}
                name={vertexName}
                propertyList={propertyList}
            />
        </>
    );
};

export default VertexTable;
