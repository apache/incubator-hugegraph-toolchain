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
