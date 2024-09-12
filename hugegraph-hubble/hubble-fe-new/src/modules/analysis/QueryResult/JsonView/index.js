/**
 * @file Gremlin语法分析 JsonView
 * @author anxiaojie@
 */

import React, {useCallback, useMemo} from 'react';
import ReactJsonView from 'react-json-view';
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


const JsonView = props => {
    const {
        jsonViewContent,
        queryStatus,
        isQueryMode,
        queryMessage,
        asyncTaskId,
    } = props;

    const renderSuccessView = useCallback(
        () => {
            if (isQueryMode) {
                if (_.isEmpty(jsonViewContent)) {
                    return (
                        <GraphStatusView status={SUCCESS} message={'无Json结果，请查看图或表格数据'} />
                    );
                }
                return (
                    <div className={c.jsonWrapper}>
                        <ReactJsonView
                            src={jsonViewContent}
                            name={false}
                            displayObjectSize={false}
                            displayDataTypes={false}
                            groupArraysAfterLength={50}
                        />
                    </div>
                );
            }
            return <TaskNavigateView message={'提交成功'} taskId={asyncTaskId} />;
        },
        [asyncTaskId, isQueryMode, jsonViewContent]
    );

    const statusMessage = useMemo(
        () => ({
            [STANDBY]: '暂无数据结果',
            [LOADING]: isQueryMode ? '数据加载中...' : '提交异步任务中...',
            [FAILED]: queryMessage || '提交失败',
            [UPLOAD_FAILED]: queryMessage || '导入失败',
        }),
        [isQueryMode, queryMessage]
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

export default React.memo(JsonView);