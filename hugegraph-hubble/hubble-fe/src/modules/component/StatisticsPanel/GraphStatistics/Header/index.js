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
 * @file  图统计标题头
 */

import React from 'react';
import {Button, Tooltip} from 'antd';
import {QuestionCircleOutlined} from '@ant-design/icons';
import {useTranslation} from 'react-i18next';
import c from './index.module.scss';

const GraphStatisticsHeader = props => {

    const {t} = useTranslation();
    const {name, description, highlightFunc, hideFunc, highlightFuncDisable, hideFuncDisable} = props;

    return (
        <div className={c.graphStatisticsHeader}>
            <div>
                <span className={c.name}>{name}</span>
                <Tooltip placement="right" title={description}><QuestionCircleOutlined /></Tooltip>
            </div>
            <div className={c.buttom}>
                <Button type="text" size='small' onClick={highlightFunc} disabled={highlightFuncDisable}>
                    {t('analysis.canvas.statistics_panel.highlight')}
                </Button>
                <Button type="text" size='small' onClick={hideFunc} disabled={hideFuncDisable}>
                    {t('analysis.canvas.statistics_panel.hide')}
                </Button>
            </div>
        </div>
    );
};

export default GraphStatisticsHeader;
