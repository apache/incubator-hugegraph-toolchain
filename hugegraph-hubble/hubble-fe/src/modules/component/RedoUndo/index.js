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
 * @file  前进后退 RedoUndo
 */

import {useContext} from 'react';
import {useTranslation} from 'react-i18next';
import {Button, Tooltip} from 'antd';
import {ArrowLeftOutlined, ArrowRightOutlined} from '@ant-design/icons';
import {GraphContext} from '../Context';

const RedoUndo = () => {
    const {t} = useTranslation();
    const {controller, snapshot, run} = useContext(GraphContext);
    const ready = Boolean(controller && snapshot?.status === 'ready');
    return (
        <>
            <Tooltip title={t('analysis.canvas.toolbar.undo')} placement="bottom">
                <Button
                    disabled={!ready || !snapshot.canUndo}
                    type="text"
                    aria-label={t('analysis.canvas.toolbar.undo')}
                    onClick={() => run(() => controller.undo())}
                    icon={<ArrowLeftOutlined />}
                />
            </Tooltip>
            <Tooltip title={t('analysis.canvas.toolbar.redo')} placement="bottom">
                <Button
                    disabled={!ready || !snapshot.canRedo}
                    type="text"
                    aria-label={t('analysis.canvas.toolbar.redo')}
                    onClick={() => run(() => controller.redo())}
                    icon={<ArrowRightOutlined />}
                />
            </Tooltip>
        </>
    );
};

export default RedoUndo;
