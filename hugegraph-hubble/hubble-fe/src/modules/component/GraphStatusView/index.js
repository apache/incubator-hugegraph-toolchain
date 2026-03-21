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
 * @file GraphStatusView
 * @author
 */

import React, {useCallback} from 'react';
import {GRAPH_STATUS} from '../../../utils/constants';
import EmptyIcon from '../../../assets/ic_sousuo_empty.svg';
import LoadingBackIcon from '../../../assets/ic_loading_back.svg';
import LoadingFrontIcon from '../../../assets/ic_loading_front.svg';
import FailedIcon from '../../../assets/ic_fail.svg';
import c from './index.module.scss';

const {
    STANDBY,
    LOADING,
    FAILED,
    UPLOAD_FAILED,
    SUCCESS,
} = GRAPH_STATUS;

const GraphStatusView = props => {
    const {
        status,
        message,
    } = props;

    const emptyContent = message => {
        const displayMessage = message || '暂无数据结果';
        return (
            <div className={c.noneGraphStatus}>
                <img
                    src={EmptyIcon}
                    alt={displayMessage}
                />
                <span>{displayMessage}</span>
            </div>
        );
    };

    const loadingContent = message => {
        const displayMessage = message || '程序运行中，请稍候';
        return (
            <div className={c.noneGraphStatus}>
                <div className={c.loadingContent}>
                    <img
                        className={c.loadingBackImage}
                        src={LoadingBackIcon}
                        alt={displayMessage}
                    />
                    <img
                        className={c.loadingFrontIamge}
                        src={LoadingFrontIcon}
                        alt={displayMessage}
                    />
                </div>
                <span className={c.loadingDesc}>
                    {displayMessage}
                </span>
            </div>
        );
    };

    const failedContent = message => {
        const displayMessage = message || '运行失败';
        return (
            <div className={c.noneGraphStatus}>
                <img
                    src={FailedIcon}
                    alt={'运行或提交失败'}
                />
                <span>{displayMessage}</span>
            </div>
        );
    };

    const uploadFailedContent = message => {
        const displayMessage = message || '导入失败';
        return (
            <div className={c.noneGraphStatus}>
                <img
                    src={FailedIcon}
                    alt={'导入失败'}
                />
                <span>{displayMessage}</span>
            </div>
        );
    };

    const succesEmptyContent = message => {
        const displayMessage = message || '无图结果';
        return (
            <div className={c.noneGraphStatus}>
                <img
                    src={EmptyIcon}
                    alt={displayMessage}
                />
                <span>{displayMessage}</span>
            </div>
        );
    };

    const renderContent = useCallback(
        (status, message) => {
            let res;
            switch (status) {
                case STANDBY:
                    res = emptyContent(message);
                    break;
                case LOADING:
                    res = loadingContent(message);
                    break;
                case FAILED:
                    res = failedContent(message);
                    break;
                case UPLOAD_FAILED:
                    res = uploadFailedContent(message);
                    break;
                case SUCCESS:
                    res = succesEmptyContent(message);
                    break;
                default:
                    res = emptyContent(message);
                    break;
            }
            return res;
        },
        []
    );

    return (
        <>
            {renderContent(status, message)}
        </>
    );
};

export default GraphStatusView;
