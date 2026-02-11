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
 * @file  ClearGraph 清空画布
 * @author
 */

import React, {useCallback, useContext, useState} from 'react';
import {Button, Tooltip, Modal} from 'antd';
import {CopyrightOutlined} from '@ant-design/icons';
import {GraphContext} from '../Context';

const ClearGraph = props => {
    const {enable, onChange} = props;
    const {graph} = useContext(GraphContext);
    const [clearModalVisible, setClearModalVisible] = useState(false);

    const handleClear = useCallback(
        () => {
            setClearModalVisible(true);
        },
        []
    );

    const handleClearModalOk = useCallback(
        () => {
            graph?.clear();
            setClearModalVisible(false);
            onChange();
        },
        [graph, onChange]
    );

    const handleClearModalCancel = useCallback(
        () => {
            setClearModalVisible(false);
        },
        []
    );

    return (
        <>
            <Tooltip title="清空画布" placement='bottom'>
                <Button disabled={!enable} type="text" onClick={handleClear} icon={<CopyrightOutlined />} />
            </Tooltip>
            <Modal
                width={600}
                title="是否清除当前画布"
                open={clearModalVisible}
                onOk={handleClearModalOk}
                onCancel={handleClearModalCancel}
                okText="确认"
                cancelText="取消"
            >
                <div>清除当前画布，此次清除操作后不可以恢复</div>
            </Modal>
        </>
    );
};

export default ClearGraph;
