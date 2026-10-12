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
 * @file  NeighborRankApi算法展示
 */

import React from 'react';
import {useTranslation} from 'react-i18next';
import {colors} from '../../../../utils/constants';
import JaccRankView from '../../../component/JaccRankView';
import _ from 'lodash';
import c from './index.module.scss';

const NeighborRankApiView = props => {
    const {t} = useTranslation();
    const {rankArray} = props;
    const colorsNum = colors.length;
    return (
        <div className={c.noneGraphContent}>
            {rankArray.map((item, index) => {
                return (
                    <div key={_.uniqueId()}>
                        <div className={c.noneGraphContentTitle}>
                            {t('analysis.algorithm.result.category', {index: index + 1})}
                        </div>
                        {_.isEmpty(item) ? (
                            <div className={c.emptyDesc}>
                                {t('analysis.algorithm.result.no_neighbor_at_degree', {
                                    index: index + 1,
                                })}
                            </div>
                        ) : (Object.entries(item)?.map(item2 => {
                            const [key, value] = item2;
                            return (
                                <JaccRankView
                                    nodeInfo={{
                                        label: key,
                                        color: colors[index % colorsNum],
                                    }}
                                    key={key}
                                    title={t('analysis.algorithm.result.rank_score')}
                                    value={value?.toString()}
                                />
                            );
                        })
                        )}
                    </div>
                );
            })}
        </div>
    );
};

export default NeighborRankApiView;
