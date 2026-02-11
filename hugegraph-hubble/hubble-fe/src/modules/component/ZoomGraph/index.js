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
 * @file  ZoomGraph 放大缩小
 * @author
 */

import React, {useCallback, useContext} from 'react';
import {Button, Tooltip} from 'antd';
import {ZoomOutOutlined, ZoomInOutlined} from '@ant-design/icons';
import {ToolBarContext} from '../Context';

const ZoomGraph = () => {
    const toolBarInstance = useContext(ToolBarContext);

    const handleZoomIn = useCallback(
        () => {
            toolBarInstance.zoomIn();
        },
        [toolBarInstance]
    );

    const handleZoomOut = useCallback(
        () => {
            toolBarInstance.zoomOut();
        },
        [toolBarInstance]
    );

    return (
        <>
            <Tooltip title="缩小" placement='bottom'>
                <Button type="text" onClick={handleZoomIn} icon={<ZoomOutOutlined />} />
            </Tooltip>
            <Tooltip title="放大" placement='bottom'>
                <Button type="text" onClick={handleZoomOut} icon={<ZoomInOutlined />} />
            </Tooltip>
        </>
    );
};

export default ZoomGraph;
