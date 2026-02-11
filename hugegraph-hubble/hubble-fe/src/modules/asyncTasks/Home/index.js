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

/**
 * @file 任务管理 首页
 * @author zhanghao14@
 */

import React, {useCallback, useContext, useEffect, useRef, useState} from 'react';
import GraphAnalysisContext from '../../Context';
import {useParams} from 'react-router-dom';
import {message, Input} from 'antd';
import AsyncTaskDetail from '../Detail';
import * as api from '../../../api/index';
import _ from 'lodash';
import c from './index.module.scss';

const defaultPageParams = {page: 1, pageSize: 10};

const AsyncTaskHome = () => {
    const {taskId} = useParams();
    const {graphSpace, graph} = useContext(GraphAnalysisContext);
    const [page, setPage] = useState(defaultPageParams.page);
    const [pageSize, setPageSize] = useState(defaultPageParams.pageSize);
    const [searchCache, setSearchCache] = useState(taskId);
    const [search, setSearch] = useState(taskId);
    const [filters, setFilters] = useState({});
    const [asyncManageTaskData, setAsyncManageTaskData] = useState({});
    let timer  = useRef();

    const onPageChange = useCallback(
        (pagination, filters) => {
            setFilters(filters);
            setPage(pagination.current);
            setPageSize(pagination.pageSize);
        }, []
    );

    const getAsynTaskList = useCallback(
        async () => {
            const {task_type, task_status} = filters;
            const params = {
                'page_size': pageSize,
                'page_no': page,
                content: search,
                status: task_status && task_status[0].toUpperCase(),
                type: task_type && task_type[0],
            };
            const response  = await api.analysis.fetchManageTaskList(graphSpace, graph, params);
            const {status, message: asyncManageTaskListMessage, data} = response;
            setAsyncManageTaskData({records: data.records, total: data.total});
            if (status !== 200) {
                clearInterval(timer.current);
                message.error(asyncManageTaskListMessage || '获取异步任务失败');
            }
        },
        [filters, pageSize, page, search, graphSpace, graph]
    );

    useEffect(
        () => {
            setAsyncManageTaskData({});
            if (graphSpace && graph) {
                getAsynTaskList();
            }
        },
        [getAsynTaskList, graph, graphSpace]
    );

    useEffect(() => {
        if (graphSpace && graph) {
            timer.current = setInterval(() => {
                getAsynTaskList();
            }, 5000);
        }
        return () => clearInterval(timer.current);
    }, [getAsynTaskList, graph, graphSpace]);

    const onSearchChange = useCallback(
        e => {
            const value = e.target.value;
            setSearchCache(value);
            if (!value) {
                setSearch(value);
            }
        },
        []
    );

    const onSearch = useCallback(
        () => {
            if (searchCache !== search) {
                setSearch(searchCache);
            }
        },
        [search, searchCache]
    );

    useEffect(
        () => {
            if (page > 1 && asyncManageTaskData?.records?.length === 0) {
                setPage(defaultPageParams.page);
            }
        },
        [asyncManageTaskData, page]
    );

    useEffect(
        () => {
            setPage(defaultPageParams.page);
        },
        [graphSpace, graph]
    );

    return (
        <div className={c.gremlinAsyncTask}>
            <div className={c.content}>
                <div className={c.queryBar}>
                    <Input.Search
                        value={searchCache}
                        onChange={onSearchChange}
                        onSearch={onSearch}
                        placeholder='请输入任务ID或名称'
                        allowClear
                        style={{width: '215px'}}
                    />
                </div>
                <AsyncTaskDetail
                    onPageChange={onPageChange}
                    asyncManageTaskData={asyncManageTaskData}
                    getAsynTaskList={getAsynTaskList}
                    page={page}
                    pageSize={pageSize}
                />
            </div>
        </div>
    );
};

export default AsyncTaskHome;
