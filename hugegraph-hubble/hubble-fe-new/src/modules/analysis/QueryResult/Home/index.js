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
 * @file Gremlin语法分析 查询结果
 * @author anxiaojie@
 */

import React from 'react';
import {Tabs} from 'antd';
import JsonView from '../JsonView';
import GraphResult from '../GraphResult/Home';
import TableView from '../TableView';
import c from './index.module.scss';

const TABS = {
    GRAPH_VIEW: '图',
    TABLE_VIEW: '表格',
    JSON_VIEW: 'Json',
};

const QueryResult = props => {
    const {
        queryResult,
        asyncTaskResult,
        queryStatus,
        queryMessage,
        isQueryMode,
        ...args
    } = props;

    const {
        graph_view: queryResultGraph = {},
        json_view: queryResultJson = {},
        table_view: queryResultTable = {},
    } = queryResult || {};

    const jsonViewContent = JSON.parse(JSON.stringify(queryResultJson?.data || []));

    const {GRAPH_VIEW, TABLE_VIEW, JSON_VIEW} = TABS;
    const renderTab = type => {
        let iconClassName = '';
        switch (type) {
            case GRAPH_VIEW:
                iconClassName = c.graphIcon;
                break;
            case TABLE_VIEW:
                iconClassName = c.tableIcon;
                break;
            case JSON_VIEW:
                iconClassName = c.jsonIcon;
                break;
        }
        return (
            <div className={c.tab}>
                <i className={iconClassName} />
                <span>{type}</span>
            </div>
        );
    };

    const graphView = (
        <GraphResult
            data={queryResultGraph}
            isQueryMode={isQueryMode}
            queryStatus={queryStatus}
            queryMessage={queryMessage}
            asyncTaskId={asyncTaskResult}
            {...args}
        />
    );

    return (
        <div className={c.queryResult}>
            <Tabs
                tabPosition="left"
                className={c.queryResultTabs}
                items={[
                    {
                        label: renderTab(GRAPH_VIEW),
                        key: 1,
                        children: graphView,
                    },
                    {
                        label: renderTab(TABLE_VIEW),
                        key: 2,
                        children: <TableView
                            queryResultTable={queryResultTable}
                            queryStatus={queryStatus}
                            isQueryMode={isQueryMode}
                            queryMessage={queryMessage}
                            asyncTaskId={asyncTaskResult}
                        />,
                    },
                    {
                        label: renderTab(JSON_VIEW),
                        key: 3,
                        children: <JsonView
                            jsonViewContent={jsonViewContent}
                            queryStatus={queryStatus}
                            isQueryMode={isQueryMode}
                            queryMessage={queryMessage}
                            asyncTaskId={asyncTaskResult}
                        />,
                    },
                ]}
            />
        </div>
    );
};

export default QueryResult;
