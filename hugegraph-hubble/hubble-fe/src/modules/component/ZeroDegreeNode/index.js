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
 * @file  孤立点检测
 */

import {useContext, useMemo} from 'react';
import {useTranslation} from 'react-i18next';
import {Button, Tooltip} from 'antd';
import {UngroupOutlined} from '@ant-design/icons';
import {GraphContext} from '../Context';

const ZeroDegreeNodeSearch = () => {
    const {t} = useTranslation();
    const {controller, snapshot, run} = useContext(GraphContext);
    const connectedNodes = useMemo(() => {
        const endpoints = new Set();
        snapshot?.data.edges.forEach(edge => {
            endpoints.add(edge.source);
            endpoints.add(edge.target);
        });
        return snapshot?.data.nodes.filter(node => endpoints.has(node.id)) || [];
    }, [snapshot?.data]);
    const isolatedNodesMode = connectedNodes.length > 0
        && connectedNodes.every(node => node.style?.visibility === 'hidden');
    return (
        <Tooltip title={t('analysis.canvas.toolbar.isolated_nodes')} placement="bottom">
            <Button
                disabled={!controller || snapshot?.status !== 'ready' || connectedNodes.length === 0}
                type="text"
                aria-label={t('analysis.canvas.toolbar.isolated_nodes')}
                aria-pressed={isolatedNodesMode}
                onClick={() => run(() => controller.hide(connectedNodes.map(node => node.id), !isolatedNodesMode))}
                icon={<UngroupOutlined />}
            />
        </Tooltip>
    );
};

export default ZeroDegreeNodeSearch;
