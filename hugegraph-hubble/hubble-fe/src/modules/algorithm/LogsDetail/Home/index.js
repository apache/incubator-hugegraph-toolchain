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
 * @file 图算法表格 Home
 * @author
 */

import React, {useCallback, useContext} from 'react';
import GraphAnalysisContext from '../../../Context';
import {Tabs, message} from 'antd';
import ExecuteLog from '../ExecuteLog';
import Favorite from '../Favorite';
import * as api from '../../../../api/index';
import c from './index.module.scss';

const LogsDetail = props => {
    const {
        isLoading,
        pageExecute,
        pageFavorite,
        pageSize,
        onExecutePageChange,
        onFavoritePageChange,
        onChangeFavorSearch,
        onSortChange,
        onRefresh,
        favoriteQueriesData,
        executionLogsData,
    } = props;

    const {graphSpace: currentGraphSpace, graph: currentGraph} = useContext(GraphAnalysisContext);
    const {records: favoriteQueriesDataRecords, total: favoriteQueriesDataTotal} = favoriteQueriesData;
    const {records: executionLogsDataRecords, total: executionLogsDataTotal} = executionLogsData;

    const addItemByName = useCallback(
        (content, favoriteName) => {
            const params = {
                'content': content,
                'name': favoriteName,
                'type': 'ALGORITHM',
            };
            api.analysis.addFavoriate(currentGraphSpace, currentGraph, params)
                .then(res => {
                    const {status, message: errMsg} = res;
                    if (status === 200) {
                        message.success('收藏成功');
                        onRefresh();
                    }
                    else {
                        !errMsg && message.error('收藏失败');
                    }
                }).catch(err => {
                    console.error(err);
                });
        }, [currentGraph, currentGraphSpace, onRefresh]);

    const onAddHandler = useCallback(
        (content, favoriteName) => {
            addItemByName(content, favoriteName);
        },
        [addItemByName]
    );

    const delItemByRowId = useCallback(
        favoriteId => {
            api.analysis.deleteQueryCollection(currentGraphSpace, currentGraph, favoriteId)
                .then(res => {
                    const {status, message: errMsg} = res;
                    if (status === 200) {
                        message.success('删除成功');
                        onRefresh();
                    }
                    else {
                        !errMsg && message.error('删除失败');
                    }
                }).catch(err => {
                    console.error(err);
                });
        }, [currentGraph, currentGraphSpace, onRefresh]);

    const onDelHandler = useCallback(
        id => {
            delItemByRowId(id);
        },
        [delItemByRowId]
    );

    const editItemByRow =  useCallback(
        (rowData, favoriteName) => {
            const params = {
                id: rowData.id,
                content: rowData.content,
                name: favoriteName,
                'type': 'ALGORITHM',
            };
            api.analysis.editQueryCollection(currentGraphSpace, currentGraph, params)
                .then(res => {
                    const {status, message: errMsg} = res;
                    if (status === 200) {
                        message.success('修改成功');
                        onRefresh();
                    }
                    else {
                        !errMsg && message.error('修改失败');
                    }
                }).catch(err => {
                    console.error(err);
                });
        }, [currentGraph, currentGraphSpace, onRefresh]);

    const onEditHandler = useCallback(
        (rowData, favoriteName) => {
            editItemByRow(rowData, favoriteName);
        },
        [editItemByRow]
    );

    const tabItems = [
        {
            label: '执行记录',
            key: 'excutes',
            children: (
                <ExecuteLog
                    isLoading={isLoading}
                    pageExecute={pageExecute}
                    pageSize={pageSize}
                    onExecutePageChange={onExecutePageChange}
                    onAddCollection={onAddHandler}
                    executionLogsDataRecords={executionLogsDataRecords}
                    executionLogsDataTotal={executionLogsDataTotal}
                />
            ),
        },
        {
            label: '收藏',
            key: 'favorites',
            children: (
                <Favorite
                    isLoading={isLoading}
                    pageFavorite={pageFavorite}
                    pageSize={pageSize}
                    onFavoritePageChange={onFavoritePageChange}
                    onChangeFavorSearch={onChangeFavorSearch}
                    onSortChange={onSortChange}
                    onDel={onDelHandler}
                    onEditCollection={onEditHandler}
                    favoriteQueriesDataRecords={favoriteQueriesDataRecords}
                    favoriteQueriesDataTotal={favoriteQueriesDataTotal}
                />
            ),
        },
    ];

    return (
        <div className={c.footerTabs}>
            <Tabs type='card' items={tabItems} />
        </div>
    );
};

export default LogsDetail;
