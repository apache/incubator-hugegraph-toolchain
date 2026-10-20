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
 * @file 画布公共组件
 */

import {useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState} from 'react';
import {message} from 'antd';
import {useTranslation} from 'react-i18next';
import classnames from 'classnames';
import {GraphCanvas, useGraphSnapshot} from '../../../visualization/react';
import {GraphContext} from '../Context';
import c from './index.module.scss';
import './index.css';

const DEFAULT_LAYOUT = {type: 'd3-force'};
const text = value => {
    if (value === null || value === undefined) {
        return '';
    }
    if (typeof value === 'object') {
        try {
            return JSON.stringify(value, (_key, item) => typeof item === 'bigint' ? item.toString() : item);
        }
        catch {
            return String(value);
        }
    }
    return String(value);
};

/** Hubble owns business UI; the public canvas owns every engine update and its lifecycle. */
const Graph = ({data, datasetKey, layout = DEFAULT_LAYOUT, onReady, onClick, onDoubleClick,
    isPanelEnable, clearGraph, children}) => {
    const {t} = useTranslation();
    const containerRef = useRef(null);
    const nodeClick = useRef(null);
    const nativeCanvasMenuPending = useRef(false);
    const handlers = useRef({onClick, onDoubleClick});
    useLayoutEffect(() => { handlers.current = {onClick, onDoubleClick}; }, [onClick, onDoubleClick]);
    const [controller, setController] = useState(null);
    const [contextTarget, setContextTarget] = useState(null);
    const [showLegend, setShowLegend] = useState(true);
    const [showGrid, setShowGrid] = useState(true);
    const [showMinimap, setShowMinimap] = useState(true);
    const snapshot = useGraphSnapshot(controller);
    const reportError = useCallback(error => {
        message.error(error instanceof Error ? error.message : String(error));
    }, []);
    const run = useCallback(async operation => {
        try {
            return await operation();
        }
        catch (error) {
            reportError(error);
            return undefined;
        }
    }, [reportError]);
    const cancelNodeClick = useCallback(() => {
        if (nodeClick.current !== null) clearTimeout(nodeClick.current);
        nodeClick.current = null;
    }, []);
    useLayoutEffect(() => cancelNodeClick, [cancelNodeClick, datasetKey]);
    const click = useCallback(event => {
        // Only the latest inspection opens a drawer; stale targets must not intercept a new double-click.
        cancelNodeClick();
        if (!handlers.current.onClick) return;
        if (event.kind !== 'node') return run(() => handlers.current.onClick?.(event));
        // Opening the details drawer immediately would intercept the second click.
        // Preserve the original 300ms single/double-click distinction at the Hubble boundary.
        nodeClick.current = setTimeout(() => {
            nodeClick.current = null;
            void run(() => handlers.current.onClick?.(event));
        }, 300);
    }, [cancelNodeClick, run]);
    const doubleClick = useCallback(event => {
        cancelNodeClick();
        return run(() => handlers.current.onDoubleClick?.(event));
    }, [cancelNodeClick, run]);
    const ready = useCallback(value => {
        setController(value);
        return onReady?.(value);
    }, [onReady]);
    useEffect(() => { setContextTarget(null); }, [datasetKey]);
    const context = useMemo(() => ({controller, snapshot, containerRef, layout, run, clearGraph,
        contextTarget, setContextTarget, showLegend, setShowLegend, showGrid, setShowGrid,
        showMinimap, setShowMinimap}),
    [controller, snapshot, layout, run, clearGraph, contextTarget, showLegend, showGrid, showMinimap]);
    const tooltip = useCallback(event => {
        const raw = event.data.raw;
        const element = t(event.kind === 'node'
            ? 'analysis.canvas.element_tooltip.node' : 'analysis.canvas.element_tooltip.edge');
        return <>
            <div>{element} {t('analysis.canvas.element_tooltip.type')}: {text(raw.label)}</div>
            <div>{element} ID: {text(raw.id)}</div>
            {Object.entries(raw.properties ?? {}).map(([key, value]) =>
                <div key={key}>{key}: {text(value)}</div>)}
        </>;
    }, [t]);
    const focus = useCallback(event => {
        // A right-button pointerdown already focused the menu's first action.
        // Keep the browser's later default focus from moving it back to canvas.
        if (event.target.tagName === 'CANVAS' && event.button === 2) {
            event.preventDefault();
            return;
        }
        if (!event.target.closest?.('button, input, textarea, select, a, [contenteditable="true"]')) {
            event.currentTarget.focus({preventScroll: true});
        }
    }, []);
    const pointerDown = useCallback(event => {
        if (!event.target.closest?.('[role="menu"]')) {
            // G6 resolves node/edge menus during pointerdown. Establish the blank
            // canvas fallback in capture, before that hit-tested callback runs.
            nativeCanvasMenuPending.current = event.target.tagName === 'CANVAS' && event.button === 2;
            setContextTarget(nativeCanvasMenuPending.current
                ? {kind: 'canvas', client: {x: event.clientX, y: event.clientY}} : null);
        }
    }, []);
    const canvasMenu = useCallback(event => {
        if (event.target.tagName !== 'CANVAS') return;
        // macOS Ctrl+click can produce a native menu without G6's button-2
        // pointerdown. Suppress it only when Hubble already opened its own menu.
        if (nativeCanvasMenuPending.current) event.preventDefault();
        nativeCanvasMenuPending.current = false;
    }, []);
    const keyDown = useCallback(event => {
        const container = event.currentTarget;
        const target = event.target;
        if (!controller || snapshot.status !== 'ready' || event.defaultPrevented
            || event.ctrlKey || event.metaKey || event.altKey || event.isComposing
            || event.nativeEvent.isComposing || event.keyCode === 229
            || !(target instanceof Element) || !container.contains(target)
            || target.closest('[data-hubble-graph]') !== container
            || document.activeElement?.closest('[data-hubble-graph]') !== container
            || target.closest('input, textarea, select, button, [role="button"], a, [contenteditable], '
                + '[role="textbox"], [role="menu"], [role="dialog"], .cm-editor, .CodeMirror')) return;
        const operation = event.key === '+' || event.key === '=' ? () => controller.zoomBy(1.25)
            : event.key === '-' ? () => controller.zoomBy(0.8)
                : event.key === '0' ? () => controller.fitView() : null;
        if (!operation) return;
        event.preventDefault();
        void run(operation);
    }, [controller, run, snapshot.status]);
    return <GraphContext.Provider value={context}>
        <div ref={containerRef} className={classnames(c.graph, {[c.layoutPanelOpen]: isPanelEnable, hiddenGrid: !showGrid})}
            data-hubble-graph tabIndex={0} aria-label={t('analysis.canvas.graph_canvas')} onMouseDown={focus}
            onKeyDown={keyDown}
            onPointerDownCapture={pointerDown} onContextMenuCapture={canvasMenu}>
            <GraphCanvas data={data} datasetKey={datasetKey} layout={layout} preset='interactive'
                style={{height: '100%'}} onReady={ready} onClick={click} onDoubleClick={doubleClick}
                onContextMenu={setContextTarget} onError={reportError} tooltip={tooltip}
                aria-label={t('analysis.canvas.graph_canvas')} />
            {children}
        </div>
    </GraphContext.Provider>;
};

export default Graph;
