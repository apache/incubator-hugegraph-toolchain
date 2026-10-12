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

import {Table} from 'antd';
import {useCallback} from 'react';
import * as api from '../../../api';
import {useParams} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import useMetaTable from '../common/useMetaTable';
import MetaTableStatus from '../common/MetaTableStatus';
import {indexTypeOptions} from '../common/config.js';

const EdgeIndexTable = () => {
    const {graphspace, graph} = useParams();
    const {t} = useTranslation();

    const fetchPage = useCallback(params => api.manage.getMetaEdgeIndexList(
        graphspace, graph, params
    ), [graphspace, graph]);
    const {data, pagination, loading, error, retry, handleTable} = useMetaTable(
        fetchPage, {identityKey: `${graphspace}:${graph}`}
    );

    const columns = [
        {
            title: t('schema.edge.col.name'),
            dataIndex: 'owner',
        },
        {
            title: t('schema.index.col.name'),
            dataIndex: 'name',
        },
        {
            title: t('schema.index.col.type'),
            dataIndex: 'type',
            render: val => indexTypeOptions.find(item => item.value === val)?.label || val,
        },
        {
            title: t('schema.index.col.fields'),
            dataIndex: 'fields',
            render: val => val.join(','),
        },
    ];

    return (
        <>
            <MetaTableStatus error={error} onRetry={retry} />
            <Table
                columns={columns}
                dataSource={data}
                pagination={pagination}
                onChange={handleTable}
                loading={loading}
            />
        </>
    );
};

export default EdgeIndexTable;
