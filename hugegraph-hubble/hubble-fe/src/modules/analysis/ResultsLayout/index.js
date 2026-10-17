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

import React, {useEffect, useId, useRef, useState} from 'react';
import ResizeObserver from 'resize-observer-polyfill';
import {useTranslation} from 'react-i18next';
import ResizeHandle from '../../../components/ResizeHandle';
import c from './index.module.scss';

const MIN_RESULT_HEIGHT = 240;
const MIN_HISTORY_HEIGHT = 150;
const DEFAULT_HISTORY_HEIGHT = 210;
const HANDLE_HEIGHT = 14;
const clampResultHeight = (height, total) => Math.max(
    MIN_RESULT_HEIGHT, Math.min(height, total - MIN_HISTORY_HEIGHT - HANDLE_HEIGHT)
);

// Keep a stable result slot while switching result/history tabs or resizing either pane.
const ResultsLayout = ({renderResult, history}) => {
    const {t} = useTranslation();
    const id = useId();
    const layoutRef = useRef(null);
    const [totalHeight, setTotalHeight] = useState(560);
    const [preferredHeight, setPreferredHeight] = useState(null);
    const resultHeight = clampResultHeight(
        preferredHeight ?? totalHeight - DEFAULT_HISTORY_HEIGHT - HANDLE_HEIGHT,
        totalHeight
    );

    useEffect(() => {
        const element = layoutRef.current;
        const updateHeight = () => setTotalHeight(element.getBoundingClientRect().height || 560);
        updateHeight();
        const observer = new ResizeObserver(updateHeight);
        observer.observe(element);
        return () => observer.disconnect();
    }, []);

    return (
        <div
            ref={layoutRef}
            className={c.layout}
            style={{gridTemplateRows: `${resultHeight}px ${HANDLE_HEIGHT}px minmax(0, 1fr)`}}
        >
            <div id={`${id}-result`} className={c.result}>
                {renderResult(resultHeight)}
            </div>
            <ResizeHandle
                value={resultHeight}
                min={MIN_RESULT_HEIGHT}
                max={totalHeight - MIN_HISTORY_HEIGHT - HANDLE_HEIGHT}
                onChange={setPreferredHeight}
                label={t('analysis.query.resize_results')}
                controls={`${id}-result ${id}-history`}
                title={t('analysis.query.resize_results')}
            />
            <div id={`${id}-history`} className={c.history}>{history}</div>
        </div>
    );
};

export default ResultsLayout;
