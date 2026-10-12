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
import {useTranslation} from 'react-i18next';
import RowActionButton from '../../../components/RowActionButton';
import useMetaTable from '../common/useMetaTable';
import MetaTableStatus from '../common/MetaTableStatus';
import {EditVertexLayer} from './EditLayer';
import TableHeader from '../../../components/TableHeader';
import * as api from '../../../api';

const VERTEX_IN_USE_KEY = 'schema.vertex.in_use';
const DELETE_REQUEST_CONFIG = {suppressBusinessErrorToast: true};

const VertexTable = ({canWrite = true}) => {
    const [editLayerVisible, setEditLayerVisible] = useState(false);
    const [refresh, setRefresh] = useState(false);
    const [selectedItems, setSelectedItems] = useState([]);
    const [vertexName, setVertexName] = useState('');
    const [propertyList, setPropertyList] = useState([]);
    const {graphspace, graph} = useParams();
    const {t} = useTranslation();

    const fetchPage = useCallback(params => api.manage.getMetaVertexList(
        graphspace, graph, params
    ), [graphspace, graph]);
    const {data, pagination, loading, error, retry, handleTable} = useMetaTable(
        fetchPage, {identityKey: `${graphspace}:${graph}`, refreshKey: refresh}
    );

    const removeVertex = useCallback((names, isBatch) => {
        api.manage.checkMetaVertex(graphspace, graph, {names}, DELETE_REQUEST_CONFIG).then(res => {
            if (res.status !== 200) {
                message.error(t('schema.delete_failed'));
                return;
            }

            const inUse = names.filter(name => res.data[name] === true);
            if (inUse.length > 0) {
                message.error(t(VERTEX_IN_USE_KEY, {names: inUse.join(',')}));
                return;
            }

            Modal.confirm({
                title: t('schema.vertex.delete_confirm'),
                content: (
                    <><div>{t('schema.delete_irreversible')}</div>
                        <div>{t('schema.delete_task_hint')}</div>
                    </>),
                onOk: () => api.manage.delMetaVertex(
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
        }).catch(() => {
            message.error(t('schema.delete_failed'));
        });
    }, [graph, graphspace, refresh, t]);

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
            message.error(t('common.select_at_least_one'));
            return;
        }

        removeVertex(selectedItems, true);
    }, [selectedItems, removeVertex, t]);

    const columns = [
        {
            title: t('schema.vertex.col.name'),
            dataIndex: 'name',
        },
        {
            title: t('schema.col.properties'),
            dataIndex: 'properties',
            ellipsis: true,
            render: val => val.map(item => item.name).join(';'),
        },
        {
            title: t('schema.vertex.col.id_strategy'),
            dataIndex: 'id_strategy',
        },
        {
            title: t('schema.vertex.col.primary_keys'),
            dataIndex: 'primary_keys',
            render: val => val.join(','),
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
            render: val => (
                <Space>
                    <RowActionButton onAction={handleEdit} value={val}>
                        {t('common.edit')}
                    </RowActionButton>
                    <RowActionButton onAction={handleDelete} value={val}>
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
    }, [graph, graphspace]);

    return (
        <>
            <TableHeader>
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
            </TableHeader>

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
                loading={loading}
                scroll={{x: 'max-content'}}
            />

            {canWrite && <EditVertexLayer
                visible={editLayerVisible}
                onCancle={handleHideLayer}
                graph={graph}
                graphspace={graphspace}
                refresh={handleRefresh}
                name={vertexName}
                propertyList={propertyList}
            />}
        </>
    );
};

export default VertexTable;
