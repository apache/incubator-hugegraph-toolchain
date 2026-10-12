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
 * @file  RankApi算法展示
 */

import React from 'react';
import {useTranslation} from 'react-i18next';
import {GRAPH_STATUS} from '../../../../utils/constants';
import JaccRankView from '../../../component/JaccRankView';
import GraphStatusView from '../../../component/GraphStatusView';
import _ from 'lodash';
import c from './index.module.scss';

const RankApiView = props => {
    const {t} = useTranslation();
    const {rankObj} = props;
    if (_.isEmpty(rankObj)) {
        return (
            <GraphStatusView
                status={GRAPH_STATUS.SUCCESS}
                message={t('analysis.algorithm.result.no_graph_result')}
            />
        );
    }
    return (
        <div className={c.noneGraphContent}>
            {
                Object.entries(rankObj)?.map(
                    item => {
                        const [key, value] = item;
                        return (
                            <JaccRankView
                                nodeInfo={{
                                    label: key,
                                    color: '#42B3E5',
                                }}
                                key={key}
                                title={t('analysis.algorithm.result.similarity_value')}
                                value={value?.toString()}
                            />
                        );
                    }
                )
            }
        </div>
    );

};

export default RankApiView;
