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
 * @file  FullScreen 全屏
 */

import {useCallback, useContext, useEffect} from 'react';
import {useTranslation} from 'react-i18next';
import {Button, Tooltip} from 'antd';
import {CompressOutlined} from '@ant-design/icons';
import screenfull from 'screenfull';
import {GraphContext} from '../Context';

const FullScreen = ({onChange}) => {
    const {t} = useTranslation();
    const {controller, snapshot, containerRef, run} = useContext(GraphContext);
    const ready = Boolean(controller && snapshot?.status === 'ready');
    useEffect(() => {
        const container = containerRef?.current;
        if (!container) return undefined;
        let wasFullscreen = screenfull.element === container;
        const handleChange = () => {
            const isFullscreen = screenfull.element === container;
            if (wasFullscreen || isFullscreen) run(() => onChange?.());
            wasFullscreen = isFullscreen;
        };
        document.addEventListener('fullscreenchange', handleChange);
        return () => document.removeEventListener('fullscreenchange', handleChange);
    }, [containerRef, controller, onChange, run]);
    const toggleFullscreen = useCallback(() => {
        const container = containerRef?.current;
        if (!ready || !container || !screenfull.isEnabled) return;
        run(() => screenfull.element === container ? screenfull.exit() : screenfull.request(container));
    }, [containerRef, ready, run]);
    useEffect(() => {
        const container = containerRef?.current;
        if (!container || !ready) return undefined;
        const handleShortcut = event => {
            if (event.key?.toLowerCase() !== 'f' || event.metaKey || event.ctrlKey
                || event.altKey || event.isComposing
                || event.target.closest?.('input, textarea, select, button, [contenteditable]')) return;
            event.preventDefault();
            toggleFullscreen();
        };
        container.addEventListener('keydown', handleShortcut);
        return () => container.removeEventListener('keydown', handleShortcut);
    }, [containerRef, ready, toggleFullscreen]);
    const label = t('analysis.canvas.toolbar.full_screen_shortcut');
    return (
        <Tooltip title={label} placement="bottom">
            <Button
                disabled={!ready || !screenfull.isEnabled}
                type="text"
                aria-label={label}
                onClick={toggleFullscreen}
                icon={<CompressOutlined />}
            />
        </Tooltip>
    );
};

export default FullScreen;
