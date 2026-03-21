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
 * @file 3D/2D 切换按钮
 * @author gouzixing
 */

import React from 'react';
import {Tooltip, Segmented} from 'antd';
import {GRAPH_RENDER_MODE} from '../../../utils/constants';
import c from './index.module.scss';

const RenderModeSwitcher = props => {

    const {buttonEnable, onClick, value, tooltip} = props;

    return (
        <Tooltip placement="bottom" title={!buttonEnable ? tooltip : ''}>
            <div className={c.threeModeSegment}>
                <Segmented
                    disabled={!buttonEnable}
                    options={Object.values(GRAPH_RENDER_MODE)}
                    onChange={onClick}
                    value={value}
                />
            </div>
        </Tooltip>
    );
};

export default RenderModeSwitcher;
