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
 */

import React, {useCallback} from 'react';
import {useTranslation} from 'react-i18next';
import {Button, Tooltip, Dropdown} from 'antd';
import {PlusSquareOutlined} from '@ant-design/icons';

const NewConfig = props => {
    const {t} = useTranslation();
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

    const newMenu = {
        items: [
            {
                key: '1',
                label: t('analysis.canvas.add_vertex'),
                onClick: handleClickNewNode,
            },
            {
                key: '2',
                label: t('analysis.canvas.add_in_edge'),
                onClick: () => handleClickNewEdge(false),
            },
            {
                key: '3',
                label: t('analysis.canvas.add_out_edge'),
                onClick: () => handleClickNewEdge(true),
            },
        ],
    };

    return (
        <Dropdown menu={newMenu} placement="bottomLeft" disabled={!buttonEnable}>
            <Tooltip placement="bottom" title={!buttonEnable ? tooltip : ''}>
                <Button
                    type='text'
                    icon={<PlusSquareOutlined />}
                    disabled={!buttonEnable}
                >
                    {t('analysis.canvas.new')}
                </Button>
            </Tooltip>
        </Dropdown>
    );
};

export default NewConfig;
