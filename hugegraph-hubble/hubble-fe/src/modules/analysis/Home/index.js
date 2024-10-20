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
 * @file 图分析 Home
 * @author
 */

import React, {useState, useCallback, useEffect, useContext} from 'react';
import GraphAnalysisContext from '../../Context';
import QueryBar from '../QueryBar/Home';
import QueryResult from '../QueryResult/Home';
import LogsDetail from '../LogsDetail/Home';
import {GREMLIN_EXECUTES_MODE, ANALYSIS_TYPE, GRAPH_STATUS, PANEL_TYPE,
    FAVORITE_TYPE, EXECUTION_LOGS_TYPE, GRAPH_RENDER_MODE} from '../../../utils/constants';
import * as api from '../../../api';
import _ from 'lodash';

const {STANDBY, LOADING, SUCCESS, FAILED} = GRAPH_STATUS;
const {QUERY} = GREMLIN_EXECUTES_MODE;
const {GREMLIN, ASYNC_CYPHER, ASYNC_GREMLIN} = ANALYSIS_TYPE;
const {CLOSED} = PANEL_TYPE;
const {CANVAS2D} = GRAPH_RENDER_MODE;
const defaultPageParams = {page: 1, pageSize: 10};

const AnalysisHome = () => {
    const {graphSpace, graph} = useContext(GraphAnalysisContext);
    const [queryStatus, setQueryStatus] = useState(STANDBY);
    const [queryMessage, setQueryMessage] = useState();
    const [isQueryMode, setQueryMode] = useState(true);
    const [queryResult, setQueryResult] = useState();
    const [asyncTaskResult, setAsyncTaskResult] = useState();
    const [metaData, setMetaData] = useState();
    const [propertyKeysRecords, setPropertyKeysRecords] = useState();
    const [graphNums, setGraphNums] = useState({vertexCount: -1, edgeCount: -1});
    const [executeMode, setExecuteMode] = useState(QUERY);
    const [analysisMode, setAnalysisMode] = useState(GREMLIN);
    const [panelType, setPanelType] = useState(CLOSED);
    const [isLoading, setLoading] = useState(false);
    const [favoriteQueriesData, setFavoriteQueriesData] = useState({});
    const [executionLogsData, setExecutionLogsData] = useState({});
    const [codeEditorContent, setCodeEditorContent] = useState('');
    const [pageExecute, setExecutePage] = useState(defaultPageParams.page);
    const [pageFavorite, setFavoritePage] = useState(defaultPageParams.page);
    const [pageSize, setPageSize] = useState(defaultPageParams.pageSize);
    const [search, setSearch] = useState();
    const [sortMode, setSortMode] = useState();
    const [graphRenderMode, setGraphRenderMode] = useState(CANVAS2D);

    const getExecutionLogsList = useCallback(
        async () => {
            const params = {'page_size': pageSize, 'page_no': pageExecute, 'type': EXECUTION_LOGS_TYPE[analysisMode]};
            setLoading(true);
            const response = await api.analysis.getExecutionLogs(graphSpace, graph, params);
            const {status, data = {}} = response;
            if (status === 200) {
                setExecutionLogsData({records: data.records, total: data.total});
            }
            setLoading(false);
        },
        [graphSpace, graph, pageSize, pageExecute, analysisMode]
    );

    const getFavoriteQueriesList = useCallback(
        async () => {
            const params = {
                page_size: pageSize,
                page_no: pageFavorite,
                content: search,
                time_order: sortMode,
                type: FAVORITE_TYPE[analysisMode],
            };
            setLoading(true);
            const response = await api.analysis.fetchFavoriteQueries(graphSpace, graph, params);
            const {status, data = {}} = response;
            if (status === 200) {
                setFavoriteQueriesData({records: data.records, total: data.total});
            }
            setLoading(false);
        },
        [graphSpace, graph, pageSize, pageFavorite, search, sortMode, analysisMode]
    );

    const onFavoriteRefresh = useCallback(
        () => {
            getFavoriteQueriesList();
        },
        [getFavoriteQueriesList]
    );

    const onExeLogsRefresh = useCallback(
        () => {
            getExecutionLogsList();
        },
        [getExecutionLogsList]
    );

    const initQueryResult = useCallback(
        () => {
            setQueryStatus(STANDBY);
            setQueryMessage();
            setQueryResult({});
            setPanelType(CLOSED);
        },
        []
    );

    const getMetaData = useCallback(
        async () => {
            let edgeMeta;
            let vertexMeta;
            const edgeMetaResponse = await api.manage.getMetaEdgeList(graphSpace, graph, {page_size: -1});
            if (edgeMetaResponse.status === 200) {
                edgeMeta = edgeMetaResponse.data.records;
            }
            const vertexMetaResponse = await api.manage.getMetaVertexList(graphSpace, graph, {page_size: -1});
            if (vertexMetaResponse.status === 200) {
                vertexMeta = vertexMetaResponse.data.records;
            }
            setMetaData({edgeMeta, vertexMeta});
        },
        [graph, graphSpace]
    );

    const getPropertykeys = useCallback(
        async () => {
            const response = await api.manage.getMetaPropertyList(graphSpace, graph, {page_size: -1});
            if (response.status === 200) {
                const propertykeysRecords = response?.data?.records ?? [];
                setPropertyKeysRecords(propertykeysRecords);
            }
        },
        [graph, graphSpace]
    );

    const getGraphNumsInfo = useCallback(
        async () => {
            const response = await api.analysis.getGraphData(graphSpace, graph);
            const {status, data} = response || {};
            if (status === 200) {
                const {vertexcount, edgecount} = data || {};
                const numsInfo = {vertexCount: vertexcount, edgeCount: edgecount};
                setGraphNums(numsInfo);
            }
        },
        [graph, graphSpace]
    );

    const onResetPage = useCallback(
        () => {
            setExecutePage(defaultPageParams.page);
            setFavoritePage(defaultPageParams.page);
        }, []);

    const onAnalysisModeChange = useCallback(
        queryType => {
            setCodeEditorContent('');
            setAnalysisMode(queryType);
            onResetPage();
        },
        [onResetPage]
    );

    const resetGraphInfo = useCallback(
        () => {
            getMetaData();
            getPropertykeys();
            getGraphNumsInfo();
        },
        [getGraphNumsInfo, getMetaData, getPropertykeys]
    );

    useEffect(() => {
        if (graphSpace && graph) {
            resetGraphInfo();
        }
        initQueryResult();
        onResetPage();
    }, [graph, graphSpace, initQueryResult, onResetPage, resetGraphInfo]);

    const onExecuteModeChange = useCallback(
        mode => {
            initQueryResult();
            setExecuteMode(mode);
        },
        [initQueryResult]
    );

    useEffect(
        () => {
            if (pageFavorite > 1 && _.isEmpty(favoriteQueriesData.records)) {
                setFavoritePage(pageFavorite - 1);
            }
        },
        [favoriteQueriesData, pageFavorite, pageSize]
    );

    const getExecuteAsyncTask = useCallback(
        async taskType => {
            const params = {type: EXECUTION_LOGS_TYPE[taskType]};
            setLoading(true);
            const response = await api.analysis.getExecuteAsyncTaskList(graphSpace, graph, params);
            setLoading(false);
            const {status, data} = response;
            if (status === 200) {
                const {async_id} = data.records[0];
                setAsyncTaskResult(async_id);
            }
        },
        [graph, graphSpace]
    );

    const onExecuteQuery = useCallback(
        async tabKey => {
            setQueryMode(true);
            setQueryStatus(LOADING);
            setPanelType(CLOSED);
            setGraphRenderMode(CANVAS2D);
            let response;
            if (tabKey === GREMLIN) {
                response = await api.analysis.getExecutionQuery(graphSpace, graph, codeEditorContent);
            }
            else {
                response = await api.analysis.getCypherExecutionQuery(graphSpace, graph, {cypher: codeEditorContent});
            }
            const {status, data, message} = response || {};
            setQueryResult(data);
            setAsyncTaskResult();
            setQueryMessage(message);
            if (status === 200) {
                setQueryStatus(SUCCESS);
            }
            else {
                setQueryStatus(FAILED);
            }
            onExeLogsRefresh();
            onFavoriteRefresh();
            resetGraphInfo();
        },
        [graph, graphSpace, codeEditorContent, onExeLogsRefresh, onFavoriteRefresh, resetGraphInfo]
    );

    const onExecuteTask = useCallback(
        async tabKey => {
            setQueryMode(false);
            setQueryStatus(LOADING);
            setPanelType(CLOSED);
            let response;
            if (tabKey === GREMLIN) {
                response = await api.analysis.getExecutionTask(graphSpace, graph, {content: codeEditorContent});
            }
            else {
                response = await api.analysis.getCypherTask(graphSpace, graph, {content: codeEditorContent});
            }
            const {status, message, data} = response || {};
            setQueryMessage(message);
            if (status === 200) {
                setQueryStatus(SUCCESS);
                // getExecuteAsyncTask(tabKey === GREMLIN ? ASYNC_GREMLIN : ASYNC_CYPHER);
                setAsyncTaskResult(data?.task_id || 0);
                setQueryResult();
                onExeLogsRefresh();
                onFavoriteRefresh();
            }
            else {
                setQueryStatus(FAILED);
            }
        },
        [codeEditorContent, graph, graphSpace, onExeLogsRefresh, onFavoriteRefresh]
    );

    const onExecute = useCallback(
        tabKey => {
            if (executeMode === QUERY) {
                onExecuteQuery(tabKey);
            }
            else {
                onExecuteTask(tabKey);
            }
        },
        [executeMode, onExecuteQuery, onExecuteTask]
    );

    useEffect(
        () => {
            getExecutionLogsList();
            getFavoriteQueriesList();
        },
        [getExecutionLogsList, getFavoriteQueriesList]
    );

    const resetGraphStatus = useCallback(
        (status, message, data) => {
            setPanelType(CLOSED);
            setQueryMode(true);
            status && setQueryStatus(status);
            message && setQueryMessage(message);
            data && setQueryResult(data);
        },
        []
    );

    const resetCodeEditorContent = useCallback(
        content => {
            setCodeEditorContent(content);
        },
        []
    );

    const updatePanelType = useCallback(
        type => {
            setPanelType(type);
        },
        []
    );

    const onExecutePageChange = useCallback(
        (page, pageSize) => {
            setExecutePage(page);
            setPageSize(pageSize);
        },
        []
    );

    const onFavoritePageChange = useCallback(
        (page, pageSize) => {
            setFavoritePage(page);
            setPageSize(pageSize);
        },
        []
    );

    const onChangeFavorSearch = useCallback(
        values => {
            setSearch(values);
        },
        []
    );

    const onSortChange = useCallback((pagination, filters, sort) => {
        setSortMode(sort.order === 'ascend' ? 'asc' : 'desc');
    }, []);

    const handleClickLoadContent = useCallback(content => {
        setCodeEditorContent(content);
    }, []);

    const onGraphRenderModeChange = useCallback(
        value => {
            setGraphRenderMode(value);
        },
        []
    );

    return (
        <>
            <QueryBar
                codeEditorContent={codeEditorContent}
                setCodeEditorContent={resetCodeEditorContent}
                executeMode={executeMode}
                onExecuteModeChange={onExecuteModeChange}
                activeTab={analysisMode}
                onTabsChange={onAnalysisModeChange}
                onExecute={onExecute}
                onRefresh={onFavoriteRefresh}
            />
            <QueryResult
                queryResult={queryResult}
                asyncTaskResult={asyncTaskResult}
                isQueryMode={isQueryMode}
                metaData={metaData}
                queryStatus={queryStatus}
                queryMessage={queryMessage}
                resetGraphStatus={resetGraphStatus}
                panelType={panelType}
                updatePanelType={updatePanelType}
                propertyKeysRecords={propertyKeysRecords}
                graphNums={graphNums}
                graphRenderMode={graphRenderMode}
                onGraphRenderModeChange={onGraphRenderModeChange}
            />
            <LogsDetail
                executionLogsData={executionLogsData}
                favoriteQueriesData={favoriteQueriesData}
                pageExecute={pageExecute}
                onClickLoadContent={handleClickLoadContent}
                analysisMode={analysisMode}
                isLoading={isLoading}
                pageSize={pageSize}
                pageFavorite={pageFavorite}
                onExecutePageChange={onExecutePageChange}
                onFavoritePageChange={onFavoritePageChange}
                onRefresh={onFavoriteRefresh}
                onChangeSearchValue={onChangeFavorSearch}
                onSortChange={onSortChange}
            />
        </>
    );
};

export default AnalysisHome;
