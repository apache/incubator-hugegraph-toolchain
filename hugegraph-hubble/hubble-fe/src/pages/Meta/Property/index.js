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

import {Table, Row, Col, Space, Button, message, Modal} from 'antd';
import {useState, useEffect, useCallback} from 'react';
import {EditPropertyLayer} from './EditLayer';
import * as api from '../../../api';
import {useParams} from 'react-router-dom';

const PropertyTable = ({noHeader, forceRefresh}) => {
    const [editLayerVisible, setEditLayerVisible] = useState(false);
    const [refresh, setRefresh] = useState(false);
    const [data, setData] = useState([]);
    const [pagination, setPagination] = useState({current: 1, total: 10});
    const [selectedItems, setSelectedItems] = useState([]);
    const {graphspace, graph} = useParams();

    const removeProperty = useCallback((names, isBatch) => {
        api.manage.checkMetaProperty(graphspace, graph, {names}).then(res => {
            if (res.status !== 200) {
                message.error(res.message);
                return;
            }

            const inUse = names.filter(name => res.data[name] === true);
            if (inUse.length > 0) {
                message.error(`属性数据 ${inUse.join(',')} 正在使用中，不可删除`);
                return;
            }

            Modal.confirm({
                title: '确认删除此属性？',
                content: (
                    <div>删除后无法恢复，请谨慎操作。</div>
                ),
                onOk: () => {
                    api.manage.delMetaProperty(graphspace, graph, {names: names}).then(res => {
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

    const handleTable = useCallback(newPagination => {
        setPagination(newPagination);
    }, []);

    const handleDelete = useCallback(row => {
        removeProperty([row.name]);
    }, [removeProperty]);

    const handleRefresh = useCallback(() => {
        setRefresh(!refresh);
    }, [refresh]);

    const handleShowLayer = useCallback(() => {
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

        removeProperty(selectedItems, true);
    }, [removeProperty, selectedItems]);

    const rowKey = useCallback(item => item.name, []);

    const columns = [
        {
            title: '属性名称',
            dataIndex: 'name',
            render: val => <span style={{wordBreak: 'break-all'}}>{val}</span>,
        },
        {
            title: '数据类型',
            dataIndex: 'data_type',
            width: 120,
            align: 'center',
            render: val => (val === 'TEXT' ? 'string' : val.toLocaleLowerCase()),
        },
        {
            title: '基数',
            dataIndex: 'cardinality',
            width: 120,
            align: 'center',
            render: val => val.toLocaleLowerCase(),
        },
        {
            title: '操作',
            width: 120,
            align: 'center',
            render: val => <a onClick={() => handleDelete(val)}>删除</a>,
        },
    ];

    useEffect(() => {
        api.manage.getMetaPropertyList(graphspace, graph, {
            page_no: pagination.current,
        }).then(res => {
            if (res.status === 200) {
                setData(res.data.records);
                setPagination({...pagination, total: res.data.total});
            }
        });
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [refresh, forceRefresh, pagination.current]);

    return (
        <>
            {!noHeader
            && (
                <Row>
                    <Col>
                        <Space>
                            <Button type='primary' onClick={handleShowLayer}>创建</Button>
                            <Button onClick={handleRefresh}>刷新</Button>
                            <Button onClick={handleDeleteBatch}>批量删除</Button>
                        </Space>
                    </Col>
                </Row>
            )}
            <br />{noHeader}

            <Table
                columns={columns}
                dataSource={data}
                rowSelection={noHeader ? null : {
                    type: 'checkbox',
                    onChange: selectedRowKeys => {
                        setSelectedItems(selectedRowKeys);
                    },
                }}
                rowKey={rowKey}
                pagination={pagination}
                onChange={handleTable}
            />

            {!noHeader
            && (
                <EditPropertyLayer
                    visible={editLayerVisible}
                    onCancle={handleHideLayer}
                    graphspace={graphspace}
                    graph={graph}
                    refresh={handleRefresh}
                />
            )}
        </>
    );
};

export default PropertyTable;
