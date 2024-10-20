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

import React from 'react';
import c from './index.module.scss';

const JaccRankView = props => {
    const {
        nodeInfo,
        title,
        value,
    } = props;

    const {label, color} = nodeInfo || {};
    const nodeStyle = `linear-gradient(-70deg, ${color},70%,#fff)`;

    const formatedValue = Math.floor(value * 1000000000000000) / 1000000000000000;

    return (
        <div className={c.jaccViewContent}>
            {nodeInfo && (
                <div className={c.circleContent}>
                    <div className={c.singleCircle} style={{backgroundImage: nodeStyle}}></div>
                    <div className={c.circleLabel} style={{color: color}}>{label}</div>
                </div>
            )}
            <div>
                <div className={c.jaccardCanvasTitle}>{title}</div>
                <div className={c.jaccardCanvasData}>{formatedValue}</div>
            </div>
        </div>
    );

};

export default JaccRankView;
