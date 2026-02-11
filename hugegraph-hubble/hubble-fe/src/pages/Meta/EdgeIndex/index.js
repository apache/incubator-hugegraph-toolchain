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
import {useEffect, useState, useCallback} from 'react';
import * as api from '../../../api';
import {useParams} from 'react-router-dom';
import {indexTypeOptions} from '../common/config.js';

const EdgeIndexTable = () => {
    const [data, setData] = useState([]);
    const [pagination, setPagination] = useState({current: 1, total: 10});
    const {graphspace, graph} = useParams();

    const handleTable = useCallback(newPagination => {
        setPagination(newPagination);
    }, []);

    const columns = [
        {
            title: '边类型名称',
            dataIndex: 'owner',
        },
        {
            title: '索引名称',
            dataIndex: 'name',
        },
        {
            title: '索引类型',
            dataIndex: 'type',
            render: val => indexTypeOptions.find(item => item.value === val)?.label || val,
        },
        {
            title: '属性',
            dataIndex: 'fields',
            render: val => val.join(','),
        },
    ];

    useEffect(() => {
        api.manage.getMetaEdgeIndexList(graphspace, graph, {
            page_no: pagination.current,
        }).then(res => {
            if (res.status === 200) {
                setData(res.data.records);
                setPagination({...pagination, total: res.data.total});
            }
        });
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [pagination.current, graph, graphspace]);

    return (
        <>
            <Table
                columns={columns}
                dataSource={data}
                pagination={pagination}
                onChange={handleTable}
            />
        </>
    );
};

export default EdgeIndexTable;
