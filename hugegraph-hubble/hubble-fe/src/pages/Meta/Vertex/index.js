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

import {Table, Space, Button, message} from 'antd';
import {useState, useEffect, useCallback} from 'react';
import {useParams} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import RowActionButton from '../../../components/RowActionButton';
import useMetaTable from '../common/useMetaTable';
import MetaTableStatus from '../common/MetaTableStatus';
import confirmSchemaDeletion from '../common/confirmSchemaDeletion';
import {EditVertexLayer} from './EditLayer';
import TableHeader from '../../../components/TableHeader';
import * as api from '../../../api';


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
        confirmSchemaDeletion({
            graphspace, graph, names, t,
            title: t('schema.vertex.delete_confirm'),
            warning: <><div>{t('schema.delete_irreversible')}</div><div>{t('schema.delete_task_hint')}</div></>,
            list: api.manage.getMetaVertexList,
            remove: api.manage.delMetaVertex,
            checkUsage: api.manage.checkMetaVertex,
            inUseKey: 'schema.vertex.in_use',
            asyncDeletion: true,
            onRefresh: () => setRefresh(value => !value),
            onComplete: () => {
                if (isBatch) setSelectedItems([]);
                setRefresh(value => !value);
            },
        });
    }, [graph, graphspace, t]);

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
