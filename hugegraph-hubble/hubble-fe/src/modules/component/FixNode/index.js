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
 * @file  FixNode 固定节点
 */

import {useContext} from 'react';
import {useTranslation} from 'react-i18next';
import {Button, Tooltip} from 'antd';
import {PushpinOutlined} from '@ant-design/icons';
import {GraphContext} from '../Context';

const FixNode = () => {
    const {t} = useTranslation();
    const {controller, snapshot, run} = useContext(GraphContext);
    const selected = new Set(snapshot?.selectedIds);
    const nodes = snapshot?.data.nodes.filter(node => selected.has(node.id)) || [];
    const fixed = nodes.length > 0 && nodes.every(node => node.data.fixed);
    const ready = Boolean(controller && snapshot?.status === 'ready');
    return (
        <Tooltip title={t('analysis.canvas.toolbar.fix_node')} placement="bottom">
            <Button
                disabled={!ready || nodes.length === 0}
                type="text"
                aria-label={t('analysis.canvas.toolbar.fix_node')}
                aria-pressed={fixed}
                onClick={() => run(() => controller.pin(nodes.map(node => node.id), !fixed))}
                icon={<PushpinOutlined />}
            />
        </Tooltip>
    );
};

export default FixNode;
