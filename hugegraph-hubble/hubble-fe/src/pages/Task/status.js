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

import {Tag} from 'antd';
import {useTranslation} from 'react-i18next';
import style from './index.module.scss';

const TASK_STATUS_CONFIG = {
    DEFAULT: {key: 'pending'},
    NEW: {key: 'pending'},
    PENDING: {key: 'pending'},
    WAITING: {key: 'pending'},
    UPLOADING: {key: 'running', color: 'processing'},
    MAPPING: {key: 'running', color: 'processing'},
    SETTING: {key: 'running', color: 'processing'},
    LOADING: {key: 'running', color: 'processing'},
    RUNNING: {key: 'running', color: 'processing'},
    EXECUTING: {key: 'running', color: 'processing'},
    SUCCEED: {key: 'success', color: 'success'},
    SUCCESS: {key: 'success', color: 'success'},
    FAILED: {key: 'failed', color: 'error'},
    FAILURE: {key: 'failed', color: 'error'},
    PAUSED: {key: 'paused', color: 'warning'},
    STOPPED: {key: 'stopped'},
    INIT: {key: 'initializing'},
    CANCELLING: {key: 'cancelling', color: 'warning'},
    CANCELLED: {key: 'cancelled'},
    CANCELED: {key: 'cancelled'},
};

const getTaskStatus = status => {
    const value = status && typeof status === 'object' ? status.status : status;
    const normalized = value ? String(value).toUpperCase() : '';
    return TASK_STATUS_CONFIG[normalized] ?? {key: 'unknown'};
};

const TaskStatus = ({status}) => {
    const {t} = useTranslation();
    const config = getTaskStatus(status);

    return (
        <Tag className={style.task_status} color={config.color}>
            {t(`task.status.${config.key}`)}
        </Tag>
    );
};

export {getTaskStatus, TaskStatus};
