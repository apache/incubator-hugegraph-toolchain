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
import {useTranslation} from 'react-i18next';
import useMetaTable from '../common/useMetaTable';
import MetaTableStatus from '../common/MetaTableStatus';
import RowActionButton from '../../../components/RowActionButton';
import * as api from '../../../api';

const DELETE_REQUEST_CONFIG = {suppressBusinessErrorToast: true};

const EdgeTable = ({canWrite = true}) => {
    const [editLayerVisible, setEditLayerVisible] = useState(false);
    const [refresh, setRefresh] = useState(false);
    const [selectedItems, setSelectedItems] = useState([]);
    const [edgeName, setEdgeName] = useState('');
    const [propertyList, setPropertyList] = useState([]);
    const [vertexList, setVertexList] = useState([]);
    const {graphspace, graph} = useParams();
    const {t} = useTranslation();

    const fetchPage = useCallback(params => api.manage.getMetaEdgeList(
        graphspace, graph, params
    ), [graphspace, graph]);
    const {data, pagination, loading, error, retry, handleTable} = useMetaTable(
        fetchPage, {identityKey: `${graphspace}:${graph}`, refreshKey: refresh}
    );

    const removeEdge = useCallback((names, isBatch) => {
        Modal.confirm({
            title: t('schema.edge.delete_confirm'),
            content: (
                <><div>{t('schema.delete_irreversible')}</div>
                    <div>{t('schema.delete_task_hint')}</div>
                </>),
            onOk: () => api.manage.delMetaEdge(
                graphspace, graph, {names}, DELETE_REQUEST_CONFIG
            ).then(res => {
                if (res.status !== 200) {
                    message.error(t('schema.delete_failed'));
                    return;
                }

                if (isBatch) {
                    setSelectedItems([]);
                }

                message.success(t('common.delete_success'));
                setRefresh(!refresh);
            }).catch(() => {
                message.error(t('schema.delete_failed'));
            }),
        });
    }, [graph, graphspace, refresh, t]);

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
            message.error(t('common.select_at_least_one'));
            return;
        }

        removeEdge(selectedItems, true);
    }, [removeEdge, selectedItems, t]);

    const handleRefresh = useCallback(() => {
        setRefresh(!refresh);
    }, [refresh]);

    const rowKey = useCallback(item => item.name, []);

    const columns = [
        {
            title: t('schema.edge.col.name'),
            dataIndex: 'name',
        },
        {
            title: t('schema.edge.col.type'),
            dataIndex: 'edgelabel_type',
            render: (val, row) => ({
                NORMAL: t('schema.edge.type.normal'),
                PARENT: t('schema.edge.type.parent'),
                SUB: (
                    <Tooltip title={t('schema.edge.parent_hint', {name: row.parent_label})}>
                        {t('schema.edge.type.sub')}
                    </Tooltip>
                ),
            }[val]),
        },
        {
            title: t('schema.edge.col.source'),
            dataIndex: 'source_label',
        },
        {
            title: t('schema.edge.col.target'),
            dataIndex: 'target_label',
        },
        {
            title: t('schema.col.properties'),
            dataIndex: 'properties',
            ellipsis: true,
            render: val => val.map(item => item.name).join(';'),
        },
        {
            title: t('schema.edge.col.sort_keys'),
            dataIndex: 'sort_keys',
        },
        {
            title: t('schema.col.label_index'),
            dataIndex: 'open_label_index',
            render: val => (val ? t('common.yes') : t('common.no')),
        },
        {
            title: t('schema.col.property_indexes'),
            dataIndex: 'property_indexes',
            ellipsis: true,
            render: val => val.map(item => item.name).join(';'),
        },
        canWrite && {
            title: t('common.operation'),
            align: 'center',
            render: row => (
                <Space>
                    <RowActionButton onAction={handleEdit} value={row}>
                        {t('common.edit')}
                    </RowActionButton>
                    <RowActionButton onAction={handleDelete} value={row}>
                        {t('common.delete')}
                    </RowActionButton>
                </Space>
            ),
        },
    ].filter(Boolean);

    useEffect(() => {
        api.manage.getMetaPropertyList(graphspace, graph, {page_size: -1}).then(res => {
            if (res.status === 200) {
                setPropertyList(res.data.records.map(item => ({
                    label: item.name,
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
                        {canWrite && (
                            <Button type='primary' onClick={handleCreate}>
                                {t('common.create')}
                            </Button>
                        )}
                        <Button onClick={handleRefresh}>{t('common.refresh')}</Button>
                        {canWrite && (
                            <Button onClick={handleDeleteBatch}>
                                {t('common.batch_delete')}
                            </Button>
                        )}
                    </Space>
                </Col>
            </Row>
            <br />

            <MetaTableStatus error={error} onRetry={retry} />
            <Table
                columns={columns}
                dataSource={data}
                rowSelection={canWrite ? {
                    type: 'checkbox',
                    onChange: selectedRowKeys => {
                        setSelectedItems(selectedRowKeys);
                    },
                } : null}
                pagination={pagination}
                onChange={handleTable}
                rowKey={rowKey}
                showExpandColumn={false}
                loading={loading}
                scroll={{x: 'max-content'}}
            />

            {canWrite && <EditEdgeLayer
                visible={editLayerVisible}
                graphspace={graphspace}
                graph={graph}
                onCancle={handleHideLayer}
                refresh={handleRefresh}
                name={edgeName}
                propertyList={propertyList}
                vertexList={vertexList}
            />}
        </>
    );
};

export default EdgeTable;
