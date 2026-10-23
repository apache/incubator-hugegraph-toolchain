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
 */

import {useCallback, useEffect, useMemo, useState} from 'react';
import {useTranslation} from 'react-i18next';
import {Alert, Button, Popover, Tabs} from 'antd';
import JsonView from '../JsonView';
import GraphResult from '../GraphResult/Home';
import TableView from '../TableView';
import {
    GRAPH_EDGE_LIMIT,
    GRAPH_NODE_LIMIT,
    getGraphViewLimitStatus,
    getBoundedGraphView,
    getBoundedJsonPreview,
} from './utils';
import c from './index.module.scss';
import {GRAPH_STATUS} from '../../../../utils/constants';

const PREVIEW_ITEM_LIMIT = 3;
const PREVIEW_CHAR_LIMIT = 1200;

const getPreviewContent = (tableView, jsonView) => {
    const rows = Array.isArray(tableView?.rows) ? tableView.rows : [];
    return getBoundedJsonPreview(rows.length > 0 ? rows : jsonView?.data ?? [],
        {items: PREVIEW_ITEM_LIMIT, characters: PREVIEW_CHAR_LIMIT, depth: 4});
};

const serializePreview = preview => {
    const text = JSON.stringify(preview.content, null, 2) ?? '';
    return {
        text: text.length > PREVIEW_CHAR_LIMIT ? `${text.slice(0, PREVIEW_CHAR_LIMIT)}\n…` : text,
        truncated: preview.truncated || text.length > PREVIEW_CHAR_LIMIT,
    };
};

const hasPreviewContent = content => {
    if (Array.isArray(content)) {
        return content.length > 0;
    }
    if (content && typeof content === 'object') {
        return Object.keys(content).length > 0;
    }
    return content !== undefined && content !== null;
};

const QueryResult = props => {
    const {t} = useTranslation();
    const {
        queryResult,
        asyncTaskResult,
        queryStatus,
        queryMessage,
        queryProgress,
        isQueryMode,
        height,
        ...args
    } = props;

    const {
        graph_view: queryResultGraph = {},
        json_view: queryResultJson = {},
        table_view: queryResultTable = {},
    } = queryResult || {};

    const edgeLimit = Number.isInteger(queryResult?.graph_edge_limit) && queryResult.graph_edge_limit > 0
        ? queryResult.graph_edge_limit : GRAPH_EDGE_LIMIT;
    const graphLimit = useMemo(
        () => getGraphViewLimitStatus(queryResultGraph, edgeLimit),
        [queryResultGraph, edgeLimit]
    );
    const graphPreview = useMemo(
        () => getBoundedGraphView(queryResultGraph, edgeLimit, {
            vertexIds: queryResult?.property_preview_vertex_ids,
            edgeIds: queryResult?.property_preview_edge_ids,
        }),
        [queryResultGraph, edgeLimit,
            queryResult?.property_preview_vertex_ids, queryResult?.property_preview_edge_ids]
    );
    const graphTruncated = queryResult?.graph_truncated || graphLimit.exceeded;
    const [activeView, setActiveView] = useState(1);
    const handleViewFullJson = useCallback(() => setActiveView(3), []);
    useEffect(() => {
        setActiveView(1);
    }, [queryResult]);
    const jsonPreview = useMemo(() => String(activeView) === '3'
        ? getBoundedJsonPreview(queryResultJson?.data ?? []) : null,
    [activeView, queryResultJson]);
    const jsonViewContent = jsonPreview?.content !== null && typeof jsonPreview?.content === 'object'
        ? jsonPreview.content : {value: jsonPreview?.content};

    const GRAPH_VIEW = t('analysis.query_result.graph');
    const TABLE_VIEW = t('analysis.query_result.table');
    const JSON_VIEW = 'JSON';
    const preview = useMemo(
        () => graphLimit.nodeCount === 0 && graphLimit.edgeCount === 0
            ? getPreviewContent(queryResultTable, queryResultJson) : {content: [], truncated: false},
        [graphLimit.nodeCount, graphLimit.edgeCount, queryResultJson, queryResultTable]
    );
    const serializedPreview = useMemo(() => serializePreview(preview), [preview]);
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

    const nonGraphResult = queryStatus === GRAPH_STATUS.SUCCESS
        && isQueryMode
        && graphLimit.nodeCount === 0
        && graphLimit.edgeCount === 0;
    const nonGraphPreview = (
        <div className={c.nonGraphPreview}>
            <Alert
                showIcon
                type="success"
                title={hasPreviewContent(preview.content)
                    ? t('analysis.query_result.non_graph_title')
                    : t('analysis.query_result.empty_success')}
                description={hasPreviewContent(preview.content) ? (
                    <>
                        <p>{t('analysis.query_result.non_graph_description')}</p>
                        <pre className={c.previewContent}>
                            {serializedPreview.text}
                        </pre>
                        {serializedPreview.truncated && (
                            <p className={c.previewHint}>
                                {t('analysis.query_result.preview_truncated')}
                            </p>
                        )}
                        <Button type="link" onClick={handleViewFullJson}>
                            {t('analysis.query_result.view_full_json')}
                        </Button>
                    </>
                ) : t('analysis.query_result.empty_success_description')}
            />
        </div>
    );
    const renderPreviewNotice = view => {
        if (!isQueryMode || queryStatus !== GRAPH_STATUS.SUCCESS || queryProgress) return null;
        const summaries = [];
        const details = [];
        if (queryResult?.result_truncated) {
            summaries.push(t('analysis.query_result.result_preview_summary'));
            details.push(t('analysis.query_result.result_preview_truncated', {limit: queryResult.result_limit || 1000}));
        }
        if (queryResult?.properties_truncated) {
            summaries.push(t('analysis.query_result.properties_preview_summary'));
            details.push(t('analysis.query_result.properties_preview', {limit: queryResult.property_preview_limit || 256}));
        }
        if (view === 'graph' && graphTruncated) {
            summaries.push(t('analysis.query_result.graph_limit_title'));
            details.push(t('analysis.query_result.graph_limit_description', {nodeLimit: GRAPH_NODE_LIMIT, edgeLimit}));
        }
        if (view === 'json' && jsonPreview?.truncated) {
            summaries.push(t('analysis.query_result.json_preview_summary'));
            details.push(t('analysis.query_result.json_preview_truncated'));
        }
        if (queryResult?.default_limit_applied && !queryResult?.result_truncated) {
            summaries.push(t('analysis.query_result.default_limit_applied', {limit: queryResult.default_limit || 300}));
        }
        if (summaries.length === 0) return null;
        return (
            <div className={c.previewStatus}>
                <span>{summaries.join(' · ')}</span>
                {details.length > 0 && (
                    <Popover trigger="click" placement="topLeft" content={(
                        <div className={c.previewDetails}>
                            {details.map(detail => <p key={detail}>{detail}</p>)}
                            <p>{t('analysis.query_result.preview_limits_hint')}</p>
                        </div>
                    )}>
                        <Button type="text" size="small">{t('analysis.query_result.preview_details')}</Button>
                    </Popover>
                )}
            </div>
        );
    };
    const graphView = nonGraphResult && !queryProgress ? (
        <>{renderPreviewNotice('result')}{nonGraphPreview}</>
    ) : (
        <>
            <GraphResult
                fitHeight={height !== undefined}
                data={graphPreview}
                isQueryMode={isQueryMode}
                queryStatus={queryStatus}
                queryMessage={queryMessage}
                queryProgress={queryProgress}
                asyncTaskId={asyncTaskResult}
                previewNotice={renderPreviewNotice('graph')}
                {...args}
            />
        </>
    );

    return (
        <div
            className={`${c.queryResult} ${height !== undefined ? c.sized : ''}`}
            style={height !== undefined ? {height, '--analysis-result-height': `${height}px`} : undefined}
        >
            <Tabs
                tabPlacement="left"
                className={c.queryResultTabs}
                activeKey={queryProgress ? 1 : activeView}
                onChange={setActiveView}
                items={[
                    {
                        label: renderTab(GRAPH_VIEW),
                        key: 1,
                        children: graphView,
                    },
                    {
                        label: renderTab(TABLE_VIEW),
                        key: 2,
                        children: <>
                            {renderPreviewNotice('table')}
                            <TableView
                                queryResultTable={queryResultTable}
                                queryStatus={queryStatus}
                                isQueryMode={isQueryMode}
                                queryMessage={queryMessage}
                                asyncTaskId={asyncTaskResult}
                            />
                        </>,
                    },
                    {
                        label: renderTab(JSON_VIEW),
                        key: 3,
                        children: <>
                            {renderPreviewNotice('json')}
                            <JsonView
                                jsonViewContent={jsonViewContent}
                                queryStatus={queryStatus}
                                isQueryMode={isQueryMode}
                                queryMessage={queryMessage}
                                asyncTaskId={asyncTaskResult}
                            />
                        </>,
                    },
                ]}
            />
        </div>
    );
};

export default QueryResult;
