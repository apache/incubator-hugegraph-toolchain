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
 * @file TaskNavigateView
 */

import React, {useCallback} from 'react';
import {useTranslation} from 'react-i18next';
import FinishedIcon from '../../../assets/ic_done_144.svg';
import {useNavigate} from 'react-router-dom';
import c from './index.module.scss';
import {Button} from 'antd';

const TaskNavigateView = props => {
    const {t} = useTranslation();
    const {
        message,
        taskId,
    } = props;


    const navigate = useNavigate();

    const onClickDetail = useCallback(
        () => {
            navigate(`/asyncTasks/${taskId}`);
        },
        [navigate, taskId]
    );

    return (
        <div className={c.graphView}>
            <img
                src={FinishedIcon}
                alt={t('analysis.canvas.task_navigation.success_alt')}
            />
            <span>{message}</span>
            <span>{t('analysis.canvas.task_navigation.task_id')}: {taskId}</span>
            <span>
                <Button type='link' onClick={onClickDetail}>
                    {t('analysis.canvas.task_navigation.view')}
                </Button>
            </span>
        </div>
    );
};

export default TaskNavigateView;
