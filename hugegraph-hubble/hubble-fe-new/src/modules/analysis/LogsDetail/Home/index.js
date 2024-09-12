/**
 * @file gremlin表格 Home
 * @author
 */

import React, {useCallback, useContext} from 'react';
import {Tabs, message} from 'antd';
import ExecuteLog from '../ExecuteLog';
import Favorite from '../Favorite';
import GraphAnalysisContext from '../../../Context';
import * as api from '../../../../api/index';
import c from './index.module.scss';

const FAVORITE_TYPE  = {
    Gremlin: 'GREMLIN',
    Algorithms: 'ALGORITHM',
    Cypher: 'CYPHER',
};

const LogsDetail = props => {
    const {
        executionLogsData,
        favoriteQueriesData,
        isLoading,
        pageExecute,
        pageFavorite,
        pageSize,
        onExecutePageChange,
        onFavoritePageChange,
        onChangeSearchValue,
        onSortChange,
        onRefresh,
        analysisMode,
        onClickLoadContent,
    } = props;

    const context = useContext(GraphAnalysisContext);

    const {records: executeLogsDataRecords, total: executeLogsDataTotal} = executionLogsData;
    const {records: favoriteQueriesDataRecords, total: favoriteQueriesDataTotal} = favoriteQueriesData;
    const {graphSpace: currentGraphSpace, graph: currentGraph} = context;

    const addItemByName = useCallback(
        (content, favoriteName) => {
            const params = {
                'content': content,
                'name': favoriteName,
                'type': FAVORITE_TYPE[analysisMode],
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
                });
        },
        [analysisMode, currentGraph, currentGraphSpace, onRefresh]
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
                });
        },
        [currentGraph, currentGraphSpace, onRefresh]
    );

    const editItemByRow =  useCallback(
        (rowData, favoriteName) => {
            const params = {
                id: rowData.id,
                content: rowData.content,
                name: favoriteName,
                'type': FAVORITE_TYPE[analysisMode],
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
                });
        },
        [analysisMode, currentGraph, currentGraphSpace, onRefresh]
    );

    const onAddHandler = useCallback(
        (content, favoriteName) => {
            addItemByName(content, favoriteName);
        },
        [addItemByName]
    );

    const onLoadHandler = useCallback(
        content => {
            onClickLoadContent(content);
        },
        [onClickLoadContent]
    );
    const onDelHandler = useCallback(
        id => {
            delItemByRowId(id);
        },
        [delItemByRowId]
    );

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
                    executeLogsDataRecords={executeLogsDataRecords}
                    executeLogsDataTotal={executeLogsDataTotal}
                    isLoading={isLoading}
                    pageExecute={pageExecute}
                    pageSize={pageSize}
                    onExecutePageChange={onExecutePageChange}
                    onAddCollection={onAddHandler}
                    onLoadContent={onLoadHandler}
                />),
        },
        {
            label: '收藏的查询',
            key: 'favorites',
            children: (
                <Favorite
                    favoriteQueriesDataRecords={favoriteQueriesDataRecords}
                    favoriteQueriesDataTotal={favoriteQueriesDataTotal}
                    isLoading={isLoading}
                    pageFavorite={pageFavorite}
                    pageSize={pageSize}
                    onFavoritePageChange={onFavoritePageChange}
                    onChangeSearchValue={onChangeSearchValue}
                    onSortChange={onSortChange}
                    onDel={onDelHandler}
                    onEditCollection={onEditHandler}
                    onLoadContent={onLoadHandler}
                />),
        },
    ];

    return (
        <div className={c.footerTabs}>
            <Tabs type='card' items={tabItems} />
        </div>
    );
};

export default LogsDetail;