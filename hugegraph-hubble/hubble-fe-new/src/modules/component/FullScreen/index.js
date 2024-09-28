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
 * @file  FullScreen 全屏
 * @author
 */

import React, {useCallback, useContext, useEffect} from 'react';
import {Button, Tooltip} from 'antd';
import {CompressOutlined} from '@ant-design/icons';
import screenfull from 'screenfull';
import {GraphContext} from '../Context';

const FullScreen = props => {
    const {onChange} = props;
    const {graph} = useContext(GraphContext);
    const handleFullScreenState = useCallback(
        () => {
            onChange();
        },
        [onChange]
    );

    useEffect(
        () => {
            document.addEventListener('fullscreenchange', handleFullScreenState, false);
            return () => {
                document.removeEventListener('fullscreenchange', handleFullScreenState, false);
            };
        },
        [handleFullScreenState]
    );

    const handleFullScreen = useCallback(
        () => {
            const container = graph?.getContainer();
            if (screenfull.isEnabled) {
                if (screenfull.isFullscreen) {
                    screenfull.exit();
                }
                else {
                    screenfull.request(container);
                }
            }
        },
        [graph]
    );

    return (
        <Tooltip title="全屏" placement='bottom'>
            <Button type="text" onClick={handleFullScreen} icon={<CompressOutlined />} />
        </Tooltip>
    );
};

export default FullScreen;
