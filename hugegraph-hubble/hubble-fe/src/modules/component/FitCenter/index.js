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
 * @file  FitCenter 自适应
 * @author
 */

import React, {useCallback, useContext} from 'react';
import {Button, Tooltip} from 'antd';
import {OneToOneOutlined} from '@ant-design/icons';
import {GraphContext} from '../Context';
import {fitView} from '../../../utils/graph';

const FitCenter = () => {
    const {graph} = useContext(GraphContext);

    const handleFitCenter = useCallback(
        () => {
            if (graph) {
                graph.fitCenter();
                fitView(graph);
            }
        },
        [graph]
    );

    return (
        <Tooltip title="自适应" placement='bottom'>
            <Button type="text" onClick={handleFitCenter} icon={<OneToOneOutlined />} />
        </Tooltip>
    );
};

export default FitCenter;
