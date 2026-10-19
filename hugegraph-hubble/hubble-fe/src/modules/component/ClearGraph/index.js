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
 */

import {useContext, useState, useEffect, useRef} from 'react';
import {useTranslation} from 'react-i18next';
import {Button, Tooltip, Modal} from 'antd';
import {CopyrightOutlined} from '@ant-design/icons';
import {GraphContext} from '../Context';

const ClearGraph = ({enable, onChange}) => {
    const {t} = useTranslation();
    const {controller, snapshot, run, clearGraph} = useContext(GraphContext);
    const [clearModalVisible, setClearModalVisible] = useState(false);
    const activeController = useRef(null);
    useEffect(() => {
        activeController.current = controller;
        setClearModalVisible(false);
        return () => { activeController.current = null; };
    }, [controller]);
    const ready = Boolean(controller && snapshot?.status === 'ready');
    const handleClear = async () => {
        if (!ready) return;
        const result = clearGraph ? await clearGraph()
            : await run(() => controller.replaceData({nodes: [], edges: []}, {newDataset: true}));
        if (activeController.current !== controller || result?.status !== 'applied') return;
        setClearModalVisible(false);
        await run(() => onChange?.());
    };
    return (
        <>
            <Tooltip title={t('analysis.canvas.toolbar.clear_canvas')} placement="bottom">
                <Button
                    disabled={!enable || !ready}
                    type="text"
                    aria-label={t('analysis.canvas.toolbar.clear_canvas')}
                    onClick={() => setClearModalVisible(true)}
                    icon={<CopyrightOutlined />}
                />
            </Tooltip>
            <Modal
                width={600}
                title={t('analysis.canvas.clear_graph.title')}
                open={clearModalVisible}
                onOk={handleClear}
                onCancel={() => setClearModalVisible(false)}
                okButtonProps={{disabled: !ready}}
                okText={t('analysis.canvas.clear_graph.ok')}
                cancelText={t('analysis.canvas.clear_graph.cancel')}
            >
                <div>{t('analysis.canvas.clear_graph.description')}</div>
            </Modal>
        </>
    );
};

export default ClearGraph;
