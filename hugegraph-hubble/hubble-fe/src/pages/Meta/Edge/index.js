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

import {Table, Row, Col, Space, Button, message, Modal, Tooltip} from 'antd';
import {EditEdgeLayer} from './EditLayer';
import {useState, useEffect, useCallback} from 'react';
import {useParams} from 'react-router-dom';
import * as api from '../../../api';

const EdgeTable = () => {
    const [editLayerVisible, setEditLayerVisible] = useState(false);
    const [data, setData] = useState([]);
    const [refresh, setRefresh] = useState(false);
    const [pagination, setPagination] = useState({current: 1, total: 10});
    const [selectedItems, setSelectedItems] = useState([]);
    const [edgeName, setEdgeName] = useState('');
    const [propertyList, setPropertyList] = useState([]);
    const [vertexList, setVertexList] = useState([]);
    const {graphspace, graph} = useParams();

    const handleTable = useCallback(newPagination => {
        setPagination(newPagination);
    }, []);

    const removeEdge = useCallback((names, isBatch) => {
        Modal.confirm({
            title: '确认删除此边类型？',
            content: (
                <><div>删除后无法恢复，请谨慎操作</div>
                    <div>删除元数据耗时较久，详情可在任务管理中查看</div>
                </>),
            onOk: () => {
                api.manage.delMetaEdge(graphspace, graph, {names}).then(res => {
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
    }, [graph, graphspace, refresh]);

    const handleDelete = useCallback(row => {
        removeEdge([row.name]);
    }, [removeEdge]);

    const handleEdit = useCallback(row => {
        setEdgeName(row.name);
        setEditLayerVisible(true);
    }, []);

    const handleCreate = useCallback(() => {
        setEdgeName('');
        setEditLayerVisible(true);
    }, []);

    const handleHideLayer = useCallback(() => {
        setEditLayerVisible(false);
    }, []);

    const handleDeleteBatch = useCallback(() => {
        if (selectedItems.length === 0) {
            message.error('请至少选择一项');
            return;
        }

        removeEdge(selectedItems, true);
    }, [removeEdge, selectedItems]);

    const handleRefresh = useCallback(() => {
        setRefresh(!refresh);
    }, [refresh]);

    const rowKey = useCallback(item => item.name, []);

    const columns = [
        {
            title: '边类型名称',
            dataIndex: 'name',
        },
        {
            title: '边类型',
            dataIndex: 'edgelabel_type',
            render: (val, row) => ({
                NORMAL: '普通类型',
                PARENT: '父类型',
                SUB: <Tooltip title={'父边：' + row.parent_label}>子类型</Tooltip>,
            }[val]),
        },
        {
            title: '起点类型',
            dataIndex: 'source_label',
        },
        {
            title: '终点类型',
            dataIndex: 'target_label',
        },
        {
            title: '关联属性',
            dataIndex: 'properties',
            ellipsis: true,
            render: val => val.map(item => item.name).join(';'),
        },
        {
            title: '区分键',
            dataIndex: 'sort_keys',
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
            render: row => (
                <Space>
                    <a onClick={() => handleEdit(row)}>编辑</a>
                    <a onClick={() => handleDelete(row)}>删除</a>
                </Space>
            ),
        },
    ];

    useEffect(() => {
        api.manage.getMetaEdgeList(graphspace, graph, {
            page_no: pagination.current,
        }).then(res => {
            if (res.status === 200) {
                setData(res.data.records);
                setPagination({...pagination, total: res.data.total});
            }
        });
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [refresh, graphspace, graph, pagination.current]);

    useEffect(() => {
        api.manage.getMetaPropertyList(graphspace, graph).then(res => {
            if (res.status === 200) {
                setPropertyList(res.data.records.map(item => ({
                    lable: item.name,
                    value: item.name,
                    data_type: item.data_type,
                })));
            }
        });

        api.manage.getMetaVertexList(graphspace, graph, {page_size: -1}).then(res => {
            if (res.status === 200) {
                setVertexList(res.data.records.map(item => ({label: item.name, value: item.name})));
            }
        });
    }, [refresh, graph, graphspace]);


    return (
        <>
            <Row>
                <Col>
                    <Space>
                        <Button type='primary' onClick={handleCreate}>创建</Button>
                        <Button onClick={handleRefresh}>刷新</Button>
                        <Button onClick={handleDeleteBatch}>批量删除</Button>
                    </Space>
                </Col>
            </Row>
            <br />

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
                showExpandColumn={false}
            />

            <EditEdgeLayer
                visible={editLayerVisible}
                graphspace={graphspace}
                graph={graph}
                onCancle={handleHideLayer}
                refresh={handleRefresh}
                name={edgeName}
                propertyList={propertyList}
                vertexList={vertexList}
            />
        </>
    );
};

export default EdgeTable;
