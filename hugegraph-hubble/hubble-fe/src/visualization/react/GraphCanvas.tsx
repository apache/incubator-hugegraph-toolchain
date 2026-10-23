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

import {useCallback, useEffect, useLayoutEffect, useRef, useState, useSyncExternalStore} from 'react';
import type {CSSProperties, ReactNode} from 'react';
import {createGraphController} from '../runtime';
import type {GraphData} from '../model/types';
import type {CanvasEvent, ControllerSnapshot, GraphLayout, GraphPreset} from '../runtime/types';
import './GraphCanvas.css';

export type GraphControllerHandle = ReturnType<typeof createGraphController>;
export interface GraphCanvasProps {
    data: GraphData;
    datasetKey?: string | number;
    layout?: GraphLayout;
    preset?: GraphPreset;
    theme?: 'light' | 'dark';
    minimap?: boolean;
    tooltip?: (event: CanvasEvent) => ReactNode;
    onClick?: (event: CanvasEvent) => void;
    onDoubleClick?: (event: CanvasEvent) => void;
    onContextMenu?: (event: CanvasEvent) => void;
    onError?: (error: unknown) => void | Promise<void>;
    onReady?: (controller: GraphControllerHandle | null) => void | Promise<void>;
    style?: CSSProperties;
    className?: string;
    'aria-label'?: string;
}
const DEFAULT_LAYOUT: GraphLayout = {type: 'd3-force'};
const EMPTY_SNAPSHOT: ControllerSnapshot = {
    status: 'idle', data: {nodes: [], edges: []}, canUndo: false, canRedo: false, selectedIds: [], hoveredIds: [],
};
const noSubscription = () => () => {};

export const useGraphSnapshot = (controller: GraphControllerHandle | null): ControllerSnapshot => {
    const subscribe = useCallback((listener: () => void) => controller?.subscribe(listener)
        ?? noSubscription(), [controller]);
    const getSnapshot = useCallback(() => controller?.getSnapshot() ?? EMPTY_SNAPSHOT, [controller]);
    return useSyncExternalStore(subscribe, getSnapshot, getSnapshot);
};
interface Instance {
    controller: GraphControllerHandle;
    generation: number;
    data: GraphData;
    datasetKey: GraphCanvasProps['datasetKey'];
    layoutKey: string;
    hasDisplayedData: boolean;
    datasetGeneration: number;
}
const errorText = (error: unknown): string => error instanceof Error ? error.message : String(error);

export const GraphCanvas = (props: GraphCanvasProps) => {
    const {data, datasetKey, layout = DEFAULT_LAYOUT, preset = 'interactive', theme = 'light',
        minimap = false, tooltip, style, className = '', 'aria-label': ariaLabel = 'Graph visualization'} = props;
    const propsRef = useRef(props);
    propsRef.current = props;
    const containerRef = useRef<HTMLDivElement>(null);
    const surfaceRef = useRef<HTMLDivElement>(null);
    const tooltipRef = useRef<HTMLDivElement>(null);
    const generationRef = useRef(0);
    const instanceRef = useRef<Instance | null>(null);
    const lastError = useRef<unknown>(Symbol('no-error'));
    const [controller, setController] = useState<GraphControllerHandle | null>(null);
    const [hover, setHover] = useState<CanvasEvent | null>(null);
    const [position, setPosition] = useState({left: 8, top: 8});
    const [error, setError] = useState<unknown>(null);
    const snapshot = useGraphSnapshot(controller);
    const layoutKey = JSON.stringify(layout);

    const report = useCallback((failure: unknown, generation: number) => {
        if (generationRef.current !== generation || lastError.current === failure) {
            return;
        }
        lastError.current = failure;
        setError(failure);
        try {
            void Promise.resolve(propsRef.current.onError?.(failure)).catch(callbackError =>
                console.error('Graph error callback failed', callbackError));
        }
        catch (callbackError) {
            console.error('Graph error callback failed', callbackError);
        }
    }, []);
    const command = useCallback((run: () => Promise<unknown>, generation: number) => {
        try {
            void run().catch(failure => report(failure, generation));
        }
        catch (failure) {
            report(failure, generation);
        }
    }, [report]);

    const replaceData = useCallback((instance: Instance, nextData: GraphData, newDataset: boolean) => {
        if (newDataset) {
            ++instance.datasetGeneration;
            instance.hasDisplayedData = false;
        }
        const datasetGeneration = instance.datasetGeneration;
        const nonEmpty = nextData.nodes.length > 0 || nextData.edges.length > 0;
        command(async () => {
            const result = await instance.controller.replaceData(nextData, {
                newDataset, layout: true,
                fit: newDataset || preset === 'thumbnail' || (!instance.hasDisplayedData && nonEmpty),
            });
            // A queued non-empty update can be superseded before rendering. Only a committed
            // display consumes this dataset's initial fit; stale datasets or owners cannot change it.
            if (result.status === 'applied' && nonEmpty && instance.datasetGeneration === datasetGeneration
                && generationRef.current === instance.generation
                && instanceRef.current === instance) {
                instance.hasDisplayedData = true;
            }
        }, instance.generation);
    }, [command, preset]);

    useEffect(() => {
        const surface = surfaceRef.current;
        const container = containerRef.current;
        if (!surface || !container) {
            return;
        }
        const generation = ++generationRef.current;
        const current = () => generationRef.current === generation;
        let next: GraphControllerHandle | null = null;
        let observer: ResizeObserver | null = null;
        setError(null);
        setHover(null);
        lastError.current = Symbol('no-error');
        const emit = (kind: 'onClick' | 'onDoubleClick' | 'onContextMenu', event: CanvasEvent) => {
            if (!current()) {
                return;
            }
            try {
                void Promise.resolve(propsRef.current[kind]?.(event)).catch(failure => report(failure, generation));
            }
            catch (failure) {
                report(failure, generation);
            }
        };
        try {
            const initial = propsRef.current;
            next = createGraphController(surface, {
                preset, theme, minimap, layout: initial.layout ?? DEFAULT_LAYOUT,
                onClick: event => emit('onClick', event),
                onDoubleClick: event => emit('onDoubleClick', event),
                onContextMenu: event => emit('onContextMenu', event),
                onHover: event => {
                    if (current()) {
                        setHover(propsRef.current.tooltip ? event : null);
                    }
                },
                onError: failure => report(failure, generation),
            });
            const owned = next;
            const instance: Instance = {controller: owned, generation, data: initial.data,
                datasetKey: initial.datasetKey, layoutKey: JSON.stringify(initial.layout ?? DEFAULT_LAYOUT),
                hasDisplayedData: false, datasetGeneration: 0};
            instanceRef.current = instance;
            setController(owned);
            let hasPositiveHostSize = false;
            const resize = (width: number, height: number, fit = true) => {
                if (current() && width > 0 && height > 0) {
                    const recoveredFromHidden = !hasPositiveHostSize;
                    hasPositiveHostSize = true;
                    setHover(null);
                    const shouldFit = fit && (preset === 'thumbnail' || recoveredFromHidden);
                    command(async () => {
                        await owned.resize(Math.max(1, Math.round(width)), Math.max(1, Math.round(height)));
                        if (shouldFit) {
                            await owned.fitView();
                        }
                    }, generation);
                }
            };
            // G6 writes pixel dimensions to its surface. Observe the host so
            // those dimensions cannot hide later layout changes from us.
            const rect = container.getBoundingClientRect();
            resize(rect.width, rect.height, false);
            observer = new ResizeObserver(entries => {
                const entry = entries.find(item => item.target === container);
                if (entry) {
                    resize(entry.contentRect.width, entry.contentRect.height);
                }
            });
            observer.observe(container);
            replaceData(instance, initial.data, true);
            command(() => Promise.resolve(initial.onReady?.(owned)), generation);
        }
        catch (failure) {
            setController(null);
            report(failure, generation);
        }
        return () => {
            generationRef.current = generation + 1;
            observer?.disconnect();
            if (instanceRef.current?.generation === generation) {
                instanceRef.current = null;
            }
            // Notify ownership release synchronously. No deferred engine callback reaches the host after this point.
            try {
                void Promise.resolve(propsRef.current.onReady?.(null)).catch(failure =>
                    console.error('Graph ownership release callback failed', failure));
            }
            catch (failure) {
                console.error('Graph ownership release callback failed', failure);
            }
            try {
                void next?.dispose().catch(failure => console.error('Graph disposal failed', failure));
            }
            catch (failure) {
                console.error('Graph disposal failed', failure);
            }
        };
    }, [command, minimap, preset, replaceData, report, theme]);

    useEffect(() => {
        const instance = instanceRef.current;
        if (!instance || instance.controller !== controller) {
            return;
        }
        const layoutChanged = instance.layoutKey !== layoutKey;
        const dataChanged = instance.data !== data || instance.datasetKey !== datasetKey;
        if (!layoutChanged && !dataChanged) {
            return;
        }
        setHover(null);
        setError(null);
        lastError.current = Symbol('no-error');
        if (layoutChanged) {
            instance.layoutKey = layoutKey;
            command(() => instance.controller.setLayout(layout), instance.generation);
        }
        if (dataChanged) {
            const newDataset = instance.datasetKey !== datasetKey;
            instance.data = data;
            instance.datasetKey = datasetKey;
            replaceData(instance, data, newDataset);
        }
    }, [command, controller, data, datasetKey, layout, layoutKey, replaceData]);

    useLayoutEffect(() => {
        const container = containerRef.current;
        const popup = tooltipRef.current;
        if (!hover || !container || !popup) {
            return;
        }
        const rect = container.getBoundingClientRect();
        const size = popup.getBoundingClientRect();
        const x = hover.client ? hover.client.x - rect.left + 12 : 12;
        const y = hover.client ? hover.client.y - rect.top + 12 : 12;
        setPosition({
            left: Math.max(8, Math.min(x, rect.width - size.width - 8)),
            top: Math.max(8, Math.min(y, rect.height - size.height - 8)),
        });
    }, [hover]);

    const displayError = error ?? (snapshot.status === 'error' ? snapshot.error : null);
    return <div ref={containerRef} className={`graph-canvas ${className}`} data-theme={theme}
        role='region' aria-label={ariaLabel} aria-busy={snapshot.status === 'updating'}
        tabIndex={preset === 'interactive' ? 0 : undefined} style={{height: 400, ...style}}
        onPointerLeave={() => setHover(null)}>
        <div ref={surfaceRef} className='graph-canvas-surface' />
        {hover && tooltip && snapshot.status !== 'disposed' && <div ref={tooltipRef} role='tooltip' className='graph-canvas-tooltip' style={position}>
            {tooltip(hover)}
        </div>}
        {displayError !== null && displayError !== undefined && <div role='alert' className='graph-canvas-error'>
            {errorText(displayError)}
        </div>}
    </div>;
};
