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

import {useCallback} from 'react';
import style from './index.module.scss';
import {useTranslation} from 'react-i18next';

const StatusField = ({status}) => {
    const {t} = useTranslation();
    const lower = status ? status.toLowerCase() : 'undefined';
    const config = {
        new: t('common.status.new'),
        running: t('common.status.running'),
        success: t('common.status.success'),
        cancelling: t('common.status.cancelling'),
        cancelled: t('common.status.cancelled'),
        failed: t('common.status.failed'),
        undefined: t('common.status.undefined'),
    };

    return (
        <span className={`${style.state} ${config[lower] ? style[lower] : ''}`}>
            {config[lower] ? config[lower] : status}
        </span>
    );
};

const StatusA = ({onClick, disable, children}) => {

    return (
        <a
            onClick={disable ? null : onClick}
            className={disable ? style.disable : ''}
        >
            {children}
        </a>
    );
};

const StatusText = ({onClick, data, disable, children}) => {
    const handleClick = useCallback(() => {
        onClick(data);
    }, [onClick, data]);

    return (
        disable ? (
            <a className={style.disable}>{children}</a>
        ) : (
            <a onClick={handleClick}>{children}</a>
        )
    );
};

export {StatusField, StatusA, StatusText};
