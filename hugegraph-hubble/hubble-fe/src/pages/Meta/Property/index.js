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

import {Table, Row, Col, Space, Button, message} from 'antd';
import {useState, useCallback} from 'react';
import {EditPropertyLayer} from './EditLayer';
import * as api from '../../../api';
import {useParams} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import RowActionButton from '../../../components/RowActionButton';
import useMetaTable from '../common/useMetaTable';
import MetaTableStatus from '../common/MetaTableStatus';
import confirmSchemaDeletion from '../common/confirmSchemaDeletion';


const PropertyTable = ({noHeader, forceRefresh, canWrite = true}) => {
    const [editLayerVisible, setEditLayerVisible] = useState(false);
    const [refresh, setRefresh] = useState(false);
    const [selectedItems, setSelectedItems] = useState([]);
    const {graphspace, graph} = useParams();
    const {t} = useTranslation();

    const removeProperty = useCallback((names, isBatch) => {
        confirmSchemaDeletion({
            graphspace, graph, names, t,
            title: t('schema.property.delete_confirm'),
            warning: <div>{t('schema.common.delete_warning')}</div>,
            list: api.manage.getMetaPropertyList,
            remove: api.manage.delMetaProperty,
            checkUsage: api.manage.checkMetaProperty,
            inUseKey: 'schema.property.in_use',
            onRefresh: () => setRefresh(value => !value),
            onComplete: () => {
                if (isBatch) setSelectedItems([]);
                setRefresh(value => !value);
            },
        });
    }, [graph, graphspace, t]);

    const fetchPage = useCallback(params => api.manage.getMetaPropertyList(
        graphspace, graph, params
    ), [graphspace, graph]);
    const {data, pagination, loading, error, retry, handleTable} = useMetaTable(
        fetchPage, {
            identityKey: `${graphspace}:${graph}`,
            refreshKey: `${refresh}:${forceRefresh}`,
        }
    );

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
            message.error(t('common.msg.select_one'));
            return;
        }

        removeProperty(selectedItems, true);
    }, [removeProperty, selectedItems, t]);

    const rowKey = useCallback(item => item.name, []);

    const columns = [
        {
            title: t('schema.property.col.name'),
            dataIndex: 'name',
            render: val => <span style={{wordBreak: 'break-all'}}>{val}</span>,
        },
        {
            title: t('schema.property.col.type'),
            dataIndex: 'data_type',
            width: 120,
            align: 'center',
            render: val => (val === 'TEXT' ? 'string' : val.toLocaleLowerCase()),
        },
        {
            title: t('schema.property.col.cardinality'),
            dataIndex: 'cardinality',
            width: 120,
            align: 'center',
            render: val => val.toLocaleLowerCase(),
        },
        canWrite && {
            title: t('schema.property.col.operation'),
            width: 120,
            align: 'center',
            render: val => (
                <RowActionButton onAction={handleDelete} value={val}>
                    {t('common.action.delete')}
                </RowActionButton>
            ),
        },
    ].filter(Boolean);

    return (
        <>
            {!noHeader
            && (
                <Row>
                    <Col>
                        <Space>
                            {canWrite && (
                                <Button type='primary' onClick={handleShowLayer}>
                                    {t('common.action.create')}
                                </Button>
                            )}
                            <Button onClick={handleRefresh}>{t('common.action.refresh')}</Button>
                            {canWrite && (
                                <Button onClick={handleDeleteBatch}>
                                    {t('schema.common.batch_delete')}
                                </Button>
                            )}
                        </Space>
                    </Col>
                </Row>
            )}
            <br />{noHeader}

            <MetaTableStatus error={error} onRetry={retry} />

            <Table
                columns={columns}
                dataSource={data}
                rowSelection={noHeader || !canWrite ? null : {
                    type: 'checkbox',
                    onChange: selectedRowKeys => {
                        setSelectedItems(selectedRowKeys);
                    },
                }}
                rowKey={rowKey}
                pagination={pagination}
                onChange={handleTable}
                loading={loading}
            />

            {!noHeader && canWrite
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
