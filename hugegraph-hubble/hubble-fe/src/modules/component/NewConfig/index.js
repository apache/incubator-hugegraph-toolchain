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
 * @file 新建按钮
 * @author
 */

import React, {useCallback} from 'react';
import {Button, Tooltip, Dropdown, Menu} from 'antd';
import {PlusSquareOutlined} from '@ant-design/icons';

const NewConfig = props => {
    const {
        buttonEnable,
        onClickAddNode,
        onClickAddEdge,
        tooltip,
    } = props;

    const handleClickNewNode = useCallback(
        () => {
            onClickAddNode();
        },
        [onClickAddNode]
    );

    const handleClickNewEdge = useCallback(
        isOut => {
            onClickAddEdge(isOut);
        },
        [onClickAddEdge]
    );

    const newMenu = (
        <Menu
            items={[
                {
                    key: '1',
                    label: (<a onClick={handleClickNewNode}>添加顶点</a>),
                },
                {
                    key: '2',
                    label: (<a onClick={() => handleClickNewEdge(false)}>添加入边</a>),
                },
                {
                    key: '3',
                    label: (<a onClick={() => handleClickNewEdge(true)}>添加出边</a>),
                },
            ]}
        />
    );

    return (
        <Dropdown overlay={newMenu} placement="bottomLeft" disabled={!buttonEnable}>
            <Tooltip placement="bottom" title={!buttonEnable ? tooltip : ''}>
                <Button
                    type='text'
                    icon={<PlusSquareOutlined />}
                    disabled={!buttonEnable}
                >
                    新建
                </Button>
            </Tooltip>
        </Dropdown>
    );
};

export default NewConfig;
