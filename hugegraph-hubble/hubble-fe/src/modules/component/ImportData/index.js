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
 * @file  导入数据
 */

import React, {useCallback, useContext} from 'react';
import {useTranslation} from 'react-i18next';
import {Button, Tooltip, Upload, message} from 'antd';
import {UploadOutlined} from '@ant-design/icons';
import GraphAnalysisContext from '../../Context';
import {GRAPH_STATUS} from '../../../utils/constants';
import * as api from '../../../api';

const {
    LOADING,
    SUCCESS,
    UPLOAD_FAILED,
} = GRAPH_STATUS;

const ImportData = props => {
    const {t} = useTranslation();
    const {
        buttonEnable,
        onUploadChange,
        tooltip,
    } = props;

    const {graphSpace, graph} = useContext(GraphAnalysisContext);

    const handleUploadChange = useCallback(
        info => {
            const {status, response} = info.file;

            if (status === 'uploading') {
                onUploadChange(LOADING);
                return;
            }

            // antd emits 'error' on HTTP/network failure. Without this branch the
            // canvas stays in LOADING forever. An error page body is not JSON, so
            // `response.message` may be missing entirely.
            if (status === 'error') {
                const failure = response?.message || t('analysis.canvas.import_failed');
                onUploadChange(UPLOAD_FAILED, failure);
                message.error(failure);
                return;
            }

            if (status === 'done') {
                if (response?.status === 200) {
                    onUploadChange(SUCCESS, t('analysis.canvas.import_success'), response.data);
                    message.success(t('analysis.canvas.import_success'));
                    return;
                }

                const failure = response?.message || t('analysis.canvas.import_failed');
                onUploadChange(UPLOAD_FAILED, failure);
            }
        },
        [onUploadChange, t]
    );

    return (
        <>
            <Upload
                action={api.analysis.getUploadList(graphSpace, graph)}
                onChange={handleUploadChange}
                accept="application/json"
                maxCount={1}
                showUploadList={false}
                disabled={!buttonEnable}
            >
                <Tooltip placement="bottom" title={buttonEnable ? '' : tooltip}>
                    <Button
                        icon={<UploadOutlined />}
                        type={'text'}
                        disabled={!buttonEnable}
                    >
                        {t('analysis.canvas.import')}
                    </Button>
                </Tooltip>
            </Upload>
        </>
    );
};

export default ImportData;
