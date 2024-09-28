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
 * @file  画布数据统计
 * @author
 */

import React from 'react';
import c from './index.module.scss';
import classnames from 'classnames';

const NumberCard = props => {
    const {pathNum, data, hasPadding} = props;
    const {currentGraphNodesNum, currentGraphEdgesNum, allGraphNodesNum, allGraphEdgesNum} = data;

    let showLoading = false;
    if (currentGraphNodesNum < 0 || currentGraphEdgesNum < 0 || +allGraphNodesNum < 0 || +allGraphEdgesNum < 0) {
        showLoading = true;
    }

    const allGraphNodes = showLoading ? 'loading...' : allGraphNodesNum;
    const allGraphEdges = showLoading ? 'loading...' : allGraphEdgesNum;

    const graphClassName = classnames(
        c.numberCard,
        {[c.numberCardWithLayoutPanel]: hasPadding}
    );

    return (
        <div className={graphClassName}>
            {pathNum && (
                <div className={c.numberCardItem}>
                    <div className={c.numberCardTitle}>Paths</div>
                    <div className={c.numberCardInfo}>
                        <span className={c.numberCur}>{pathNum}</span>
                    </div>
                </div>
            )}
            <div className={c.numberCardItem}>
                <div className={c.numberCardTitle}>Nodes</div>
                <div className={c.numberCardInfo}>
                    <span className={c.numberCur}>{currentGraphNodesNum}</span>
                    <span>/</span>
                    <span className={c.numberAll}>
                        {allGraphNodes}
                    </span>
                </div>
            </div>
            <div className={c.numberCardItem}>
                <div className={c.numberCardTitle}>Edges</div>
                <div className={c.numberCardInfo}>
                    <span className={c.numberCur}>{currentGraphEdgesNum}</span>
                    <span>/</span>
                    <span className={c.numberAll}>
                        {allGraphEdges}
                    </span>
                </div>
            </div>
        </div>
    );
};

export default NumberCard;
