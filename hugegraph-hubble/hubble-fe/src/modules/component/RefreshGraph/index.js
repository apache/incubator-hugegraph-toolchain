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
 * @file  RefreshGraph 刷新布局
 * @author
 */

import React, {useCallback, useContext} from 'react';
import {Button, Tooltip} from 'antd';
import {SyncOutlined} from '@ant-design/icons';
import {GraphContext} from '../Context';

const RefreshGraph = () => {
    const {graph} = useContext(GraphContext);

    const handleRefreshGraph = useCallback(
        () => {
            const {layout} = graph.cfg;
            if (layout) {
                graph.destroyLayout();
                graph.updateLayout(layout, 'center', undefined, false);
            }
        },
        [graph]
    );

    return (
        <Tooltip title="刷新布局" placement='bottom'>
            <Button type="text" onClick={handleRefreshGraph} icon={<SyncOutlined />} />
        </Tooltip>
    );
};

export default RefreshGraph;
