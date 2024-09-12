/**
 * @file 异步任务结果
 * @author
 */

import React, {useCallback, useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';
import * as api from '../../../api/index';
import ReactJsonView from 'react-json-view';
import convertStringToJSON from '../../../utils/convertStringToJSON';
import c from './index.module.scss';

const AsyncTaskResult = () => {
    const {
        graphspace,
        graph,
        taskId,
    } = useParams();

    const [asyncTaskResultJson, setAsyncTaskResultJson] = useState();

    const getResult = useCallback(
        async () => {
            const response = await api.analysis.fetchAsyncTaskResult(graphspace, graph, taskId);
            setAsyncTaskResultJson(response.data?.task_result);
        },
        [graph, graphspace, taskId]
    );

    useEffect(
        () => {
            getResult();
        },
        [getResult, graph, graphspace, taskId]
    );
    const resultForJSON = convertStringToJSON(asyncTaskResultJson);

    return (
        <div className={c.asyncTaskResult}>
            {
                resultForJSON === null ? (
                    asyncTaskResultJson
                ) : (
                    <ReactJsonView
                        src={resultForJSON}
                        name={false}
                        displayObjectSize={false}
                        displayDataTypes={false}
                        groupArraysAfterLength={50}
                    />
                )
            }
        </div>
    );
};

export default AsyncTaskResult;
