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
 * @file  JACCARD_SIMILARITY等算法展示
 * @author
 */

import React from 'react';
import JaccRankView from '../../../component/JaccRankView';
import c from './index.module.scss';

const JaccView = props => {
    const {jaccardsimilarity} = props;
    return (
        <div className={c.noneGraphContent}>
            <JaccRankView
                title={'相似度的值'}
                value={jaccardsimilarity?.toString()}
            />
        </div>
    );

};

export default JaccView;
