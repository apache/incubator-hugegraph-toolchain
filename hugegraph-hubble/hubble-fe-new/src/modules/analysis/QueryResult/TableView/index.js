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
 * @file Gremlin语法分析 TabelView
 * @author anxiaojie@
 */

import React, {useCallback, useMemo} from 'react';
import {Table} from 'antd';
import GraphStatusView from '../../../component/GraphStatusView';
import TaskNavigateView from '../../../component/TaskNavigateView';
import {GRAPH_STATUS} from '../../../../utils/constants';
import _ from 'lodash';
import c from './index.module.scss';

const {
    STANDBY,
    LOADING,
    SUCCESS,
    FAILED,
    UPLOAD_FAILED,
} = GRAPH_STATUS;

const TableView = props => {
    const {
        queryResultTable,
        queryStatus,
        isQueryMode,
        queryMessage,
        asyncTaskId,
    } = props;

    const tableColums = queryResultTable.header?.map(title => ({
        title,
        dataIndex: title,
        render(text) {
            return JSON.stringify(text);
        },
    }));

    const statusMessage = useMemo(
        () => ({
            [STANDBY]: '暂无数据结果',
            [LOADING]: isQueryMode ? '数据加载中...' : '提交异步任务中...',
            [FAILED]: queryMessage || '提交失败',
            [UPLOAD_FAILED]: queryMessage || '导入失败',
        }),
        [isQueryMode, queryMessage]
    );

    const renderSuccessView = useCallback(
        () => {
            if (isQueryMode) {
                if (_.isNull(queryResultTable?.rows)) {
                    return (
                        <GraphStatusView status={SUCCESS} message={'无表格结果，请查看图或Json数据'} />
                    );
                }
                return (
                    <div className={c.tableWrapper}>
                        <Table
                            rowKey="id"
                            dataSource={queryResultTable?.rows || []}
                            columns={tableColums}
                            pagination={{position: ['bottomCenter']}}
                        />
                    </div>
                );

            }
            return <TaskNavigateView message={'提交成功'} taskId={asyncTaskId} />;
        },
        [asyncTaskId, isQueryMode, queryResultTable?.rows, tableColums]
    );

    const renderJsonView = () => {
        if (queryStatus === SUCCESS) {
            return renderSuccessView();
        }
        return <GraphStatusView status={queryStatus} message={statusMessage[queryStatus]} />;
    };


    return (
        renderJsonView()
    );
};

export default React.memo(TableView);
