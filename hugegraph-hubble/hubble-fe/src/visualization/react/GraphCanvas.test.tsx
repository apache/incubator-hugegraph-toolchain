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

import '@testing-library/jest-dom';
import {StrictMode} from 'react';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {GraphCanvas} from './GraphCanvas';
import {createGraphController} from '../runtime';
import type {ControllerOptions, ControllerSnapshot} from '../runtime/types';
import type {GraphData} from '../model/types';

jest.mock('../runtime', () => ({createGraphController: jest.fn()}));
const factory = jest.mocked(createGraphController);
const data: GraphData = {nodes: [{id: 'a', data: {raw: {id: 'original', name: '<script>unsafe</script>'}}}], edges: []};
const layout = {type: 'dagre' as const};
const makeController = () => {
    let snapshot: ControllerSnapshot = {status: 'ready', data, canUndo: false, canRedo: false, selectedIds: [], hoveredIds: []};
    const listeners = new Set<() => void>();
    return {
        replaceData: jest.fn().mockResolvedValue({status: 'applied', value: undefined}), setLayout: jest.fn().mockResolvedValue(undefined),
        resize: jest.fn().mockResolvedValue(undefined), fitView: jest.fn().mockResolvedValue(undefined),
        dispose: jest.fn().mockResolvedValue(undefined),
        getSnapshot: () => snapshot,
        subscribe: jest.fn((listener: () => void) => {listeners.add(listener); return () => {listeners.delete(listener);};}),
        publish: (value: Partial<ControllerSnapshot>) => {snapshot = {...snapshot, ...value}; listeners.forEach(listener => listener());},
        listeners,
    };
};
type MockController = ReturnType<typeof makeController>;
let controllers: MockController[];
let options: ControllerOptions[];
let observers: {callback: ResizeObserverCallback; observe: jest.Mock; disconnect: jest.Mock}[];
const flush = async () => {await act(async () => {});};
beforeEach(() => {
    jest.clearAllMocks(); controllers = []; options = []; observers = [];
    factory.mockImplementation((_container, config = {}) => {
        const controller = makeController(); controllers.push(controller); options.push(config);
        return controller as unknown as ReturnType<typeof createGraphController>;
    });
    global.ResizeObserver = class {
        observe = jest.fn(); unobserve = jest.fn(); disconnect = jest.fn();
        constructor(callback: ResizeObserverCallback) {observers.push({callback, observe: this.observe, disconnect: this.disconnect});}
    } as unknown as typeof ResizeObserver;
});

test('initializes once and updates data through the existing controller with explicit dataset semantics', async () => {
    const ready = jest.fn();
    const {rerender} = render(<GraphCanvas data={data} layout={layout} datasetKey='query-1' onReady={ready} />);
    await flush();
    expect(factory).toHaveBeenCalledTimes(1);
    expect(controllers[0].replaceData).toHaveBeenCalledTimes(1);
    expect(controllers[0].replaceData).toHaveBeenLastCalledWith(data, {newDataset: true, layout: true, fit: true});
    expect(controllers[0].setLayout).not.toHaveBeenCalled();
    rerender(<GraphCanvas data={data} layout={{type: 'dagre'}} datasetKey='query-1' onReady={ready} />);
    expect(controllers[0].setLayout).not.toHaveBeenCalled();
    const next = {...data, nodes: [...data.nodes, {id: 'b', data: {raw: {id: 2}}}]};
    rerender(<GraphCanvas data={next} layout={layout} datasetKey='query-1' onReady={ready} />);
    expect(controllers[0].replaceData).toHaveBeenLastCalledWith(next, {newDataset: false, layout: true, fit: false});
    rerender(<GraphCanvas data={next} layout={{type: 'grid'}} datasetKey='query-2' onReady={ready} />);
    expect(controllers[0].setLayout).toHaveBeenCalledWith({type: 'grid'});
    expect(controllers[0].replaceData).toHaveBeenLastCalledWith(next, {newDataset: true, layout: true, fit: true});
    expect(controllers[0].setLayout.mock.invocationCallOrder[0]).toBeLessThan(controllers[0].replaceData.mock.invocationCallOrder[2]);
    expect(factory).toHaveBeenCalledTimes(1);
});

test('delivers current handlers and raw records while hiding the React tooltip on leave and new data', async () => {
    const click = jest.fn(); const context = jest.fn(); const replacement = jest.fn();
    const tooltip = (event: {data: {raw: Record<string, unknown>}}) => <span>{String(event.data.raw.name)}</span>;
    const {rerender} = render(<GraphCanvas data={data} onClick={click} onContextMenu={context} tooltip={tooltip} />);
    const event = {id: 'a', kind: 'node' as const, data: data.nodes[0].data, client: {x: 900, y: 900}};
    act(() => {options[0].onClick?.(event); options[0].onContextMenu?.(event); options[0].onHover?.(event);});
    expect(click).toHaveBeenCalledWith(event); expect(context).toHaveBeenCalledWith(event);
    expect(screen.getByRole('tooltip')).toHaveTextContent('<script>unsafe</script>');
    expect(screen.getByRole('tooltip').querySelector('script')).toBeNull();
    fireEvent.pointerLeave(screen.getByRole('region'));
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    rerender(<GraphCanvas data={data} onClick={replacement} tooltip={tooltip} />);
    act(() => {options[0].onClick?.(event); options[0].onHover?.(event);});
    expect(replacement).toHaveBeenCalledWith(event);
    rerender(<GraphCanvas data={{...data}} onClick={replacement} tooltip={tooltip} />);
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    await flush();
});

test.each(['interactive', 'preview', 'thumbnail'] as const)('%s owns its observer and only thumbnails fit on resize', async preset => {
    const {unmount} = render(<GraphCanvas data={data} preset={preset} />);
    const observer = observers[0]; const target = observer.observe.mock.calls[0][0];
    act(() => observer.callback([{target, contentRect: {width: 640, height: 240}}] as ResizeObserverEntry[], {} as ResizeObserver));
    expect(controllers[0].resize).toHaveBeenCalledWith(640, 240);
    expect(controllers[0].fitView).toHaveBeenCalledTimes(preset === 'thumbnail' ? 1 : 0);
    unmount();
    expect(observer.disconnect).toHaveBeenCalledTimes(1);
    expect(controllers[0].dispose).toHaveBeenCalledTimes(1);
    expect(controllers[0].listeners.size).toBe(0);
    await flush();
});

test('rebuilds on theme changes, rejects stale callbacks and isolates multiple instances', async () => {
    const click = jest.fn(); const error = jest.fn(); const ready = jest.fn();
    const {rerender, unmount} = render(<><GraphCanvas data={data} onClick={click} onError={error} onReady={ready} /><GraphCanvas data={data} /></>);
    const old = options[0];
    rerender(<><GraphCanvas data={data} theme='dark' onClick={click} onError={error} onReady={ready} /><GraphCanvas data={data} /></>);
    expect(factory).toHaveBeenCalledTimes(3);
    expect(controllers[0].dispose).toHaveBeenCalledTimes(1);
    expect(controllers[1].dispose).not.toHaveBeenCalled();
    act(() => {old.onClick?.({id: 'a', kind: 'node', data: data.nodes[0].data}); old.onError?.(new Error('stale'));});
    expect(click).not.toHaveBeenCalled(); expect(error).not.toHaveBeenCalled();
    expect(ready.mock.calls.map(call => call[0])).toEqual([controllers[0], null, controllers[2]]);
    unmount(); await flush();
    expect(controllers[1].dispose).toHaveBeenCalledTimes(1);
    expect(controllers[2].dispose).toHaveBeenCalledTimes(1);
});

test('shows command and snapshot errors locally even without an error consumer', async () => {
    const {rerender} = render(<GraphCanvas data={data} />);
    controllers[0].replaceData.mockRejectedValueOnce(new Error('render failed'));
    rerender(<GraphCanvas data={{...data}} />);
    await flush();
    expect(screen.getByRole('alert')).toHaveTextContent('render failed');
    rerender(<GraphCanvas data={{...data}} />); await flush();
    act(() => controllers[0].publish({status: 'error', error: new Error('engine failed')}));
    expect(screen.getByRole('alert')).toHaveTextContent('engine failed');
});

test('handles asynchronous rejection after unmount without delivering an error to the released host', async () => {
    let reject: (error: Error) => void = () => {};
    const onError = jest.fn();
    const {rerender, unmount} = render(<GraphCanvas data={data} onError={onError} />);
    controllers[0].replaceData.mockReturnValueOnce(new Promise((_resolve, fail) => {reject = fail;}));
    rerender(<GraphCanvas data={{...data}} onError={onError} />);
    unmount(); reject(new Error('finished after disposal')); await flush();
    expect(onError).not.toHaveBeenCalled();
});


test('survives React strict effect replay and keeps only the active subscription and observer', async () => {
    const onError = jest.fn();
    const {unmount} = render(<StrictMode><GraphCanvas data={data} onError={onError} /></StrictMode>);
    expect(factory).toHaveBeenCalledTimes(2);
    expect(controllers[0].dispose).toHaveBeenCalledTimes(1);
    expect(observers[0].disconnect).toHaveBeenCalledTimes(1);
    expect(controllers[0].listeners.size).toBe(0);
    expect(controllers[1].listeners.size).toBe(1);
    act(() => options[0].onError?.(new Error('strict replay stale event')));
    expect(onError).not.toHaveBeenCalled();
    act(() => controllers[1].publish({status: 'updating'}));
    expect(screen.getByRole('region')).toHaveAttribute('aria-busy', 'true');
    unmount(); await flush();
    expect(controllers[1].listeners.size).toBe(0);
    expect(observers[1].disconnect).toHaveBeenCalledTimes(1);
});

test('surfaces a synchronous factory failure and safely unmounts without an engine', async () => {
    factory.mockImplementationOnce(() => {throw new Error('renderer unavailable');});
    const error = jest.fn();
    const {unmount} = render(<GraphCanvas data={data} onError={error} />);
    expect(screen.getByRole('alert')).toHaveTextContent('renderer unavailable');
    expect(error).toHaveBeenCalledWith(expect.objectContaining({message: 'renderer unavailable'}));
    unmount(); await flush();
});


test('reports a rejected asynchronous ownership callback through the active error boundary', async () => {
    const failure = new Error('consumer initialization failed');
    const onError = jest.fn();
    const onReady = jest.fn(controller => controller ? Promise.reject(failure) : Promise.resolve());
    const {unmount} = render(<GraphCanvas data={data} onReady={onReady} onError={onError} />);
    await flush();
    expect(screen.getByRole('alert')).toHaveTextContent('consumer initialization failed');
    expect(onError).toHaveBeenCalledTimes(1);
    expect(onError).toHaveBeenCalledWith(failure);
    expect(controllers[0].listeners.size).toBe(1);
    unmount(); await flush();
    expect(controllers[0].dispose).toHaveBeenCalledTimes(1);
});

test('contains a rejected asynchronous release callback without notifying a released consumer', async () => {
    const failure = new Error('consumer release failed');
    const onError = jest.fn();
    const onReady = jest.fn(controller => controller ? Promise.resolve() : Promise.reject(failure));
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
    try {
        const {unmount} = render(<GraphCanvas data={data} onReady={onReady} onError={onError} />);
        await flush(); unmount(); await flush();
        expect(onReady.mock.calls.map(call => call[0])).toEqual([controllers[0], null]);
        expect(onError).not.toHaveBeenCalled();
        expect(consoleError).toHaveBeenCalledTimes(1);
        expect(consoleError).toHaveBeenCalledWith('Graph ownership release callback failed', failure);
        expect(controllers[0].dispose).toHaveBeenCalledTimes(1);
    }
    finally {
        consoleError.mockRestore();
    }
});


test('contains an asynchronous error callback rejection without recursively notifying the consumer', async () => {
    const failure = new Error('engine notification failed');
    const callbackFailure = new Error('consumer error handler failed');
    let reject: (reason: Error) => void = () => {};
    const onError = jest.fn(() => new Promise<void>((_resolve, fail) => {reject = fail;}));
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
    try {
        const {unmount} = render(<GraphCanvas data={data} onError={onError} />);
        act(() => options[0].onError?.(failure));
        expect(screen.getByRole('alert')).toHaveTextContent('engine notification failed');
        expect(onError).toHaveBeenCalledTimes(1);
        expect(onError).toHaveBeenCalledWith(failure);
        unmount(); reject(callbackFailure); await flush();
        expect(onError).toHaveBeenCalledTimes(1);
        expect(consoleError).toHaveBeenCalledTimes(1);
        expect(consoleError).toHaveBeenCalledWith('Graph error callback failed', callbackFailure);
    }
    finally {
        consoleError.mockRestore();
    }
});


test('fits the first applied non-empty preview after an empty mount and preserves the viewport on refresh', async () => {
    const empty: GraphData = {nodes: [], edges: []};
    const {rerender} = render(<GraphCanvas data={empty} preset='preview' layout={layout} />);
    await flush();
    rerender(<GraphCanvas data={data} preset='preview' layout={layout} />);
    expect(controllers[0].replaceData).toHaveBeenLastCalledWith(data, {newDataset: false, layout: true, fit: true});
    await flush();
    const refreshed = {...data};
    rerender(<GraphCanvas data={refreshed} preset='preview' layout={layout} />);
    expect(controllers[0].replaceData).toHaveBeenLastCalledWith(refreshed, {newDataset: false, layout: true, fit: false});
    const observer = observers[0]; const target = observer.observe.mock.calls[0][0];
    act(() => observer.callback([{target, contentRect: {width: 640, height: 240}}] as ResizeObserverEntry[], {} as ResizeObserver));
    expect(controllers[0].fitView).not.toHaveBeenCalled();
    await flush();
});

test('does not consume the initial fit when a pending non-empty preview is superseded', async () => {
    const empty: GraphData = {nodes: [], edges: []};
    const {rerender} = render(<GraphCanvas data={empty} preset='preview' />);
    await flush();
    let supersede: (result: {status: 'superseded'}) => void = () => {};
    controllers[0].replaceData.mockReturnValueOnce(new Promise(resolve => {supersede = resolve;}));
    rerender(<GraphCanvas data={data} preset='preview' />);
    expect(controllers[0].replaceData).toHaveBeenLastCalledWith(data, {newDataset: false, layout: true, fit: true});
    const newest = {...data, nodes: [...data.nodes, {id: 'b', data: {raw: {id: 2}}}]};
    rerender(<GraphCanvas data={newest} preset='preview' />);
    expect(controllers[0].replaceData).toHaveBeenLastCalledWith(newest, {newDataset: false, layout: true, fit: true});
    await act(async () => supersede({status: 'superseded'}));
    await flush();
    const refreshed = {...newest};
    rerender(<GraphCanvas data={refreshed} preset='preview' />);
    expect(controllers[0].replaceData).toHaveBeenLastCalledWith(refreshed, {newDataset: false, layout: true, fit: false});
    await flush();
});

test('a superseded initial non-empty mount does not suppress the next update initial fit', async () => {
    factory.mockImplementationOnce((_container, config = {}) => {
        const controller = makeController(); controllers.push(controller); options.push(config);
        controller.replaceData.mockResolvedValueOnce({status: 'superseded'});
        return controller as unknown as ReturnType<typeof createGraphController>;
    });
    const {rerender} = render(<GraphCanvas data={data} preset='preview' />);
    await flush();
    const next = {...data};
    rerender(<GraphCanvas data={next} preset='preview' />);
    expect(controllers[0].replaceData).toHaveBeenLastCalledWith(next, {newDataset: false, layout: true, fit: true});
    await flush();
});


test('fits the first non-empty data of a new dataset even when that dataset starts empty', async () => {
    const empty: GraphData = {nodes: [], edges: []};
    const {rerender} = render(<GraphCanvas data={data} datasetKey='A' preset='preview' />);
    await flush();
    rerender(<GraphCanvas data={empty} datasetKey='B' preset='preview' />);
    await flush();
    rerender(<GraphCanvas data={data} datasetKey='B' preset='preview' />);
    expect(controllers[0].replaceData).toHaveBeenLastCalledWith(data, {newDataset: false, layout: true, fit: true});
    await flush();
    const refresh = {...data};
    rerender(<GraphCanvas data={refresh} datasetKey='B' preset='preview' />);
    expect(controllers[0].replaceData).toHaveBeenLastCalledWith(refresh, {newDataset: false, layout: true, fit: false});
    await flush();
});

test('does not let an old dataset applied callback consume the new empty dataset initial fit', async () => {
    let completeOld: (result: {status: 'applied'; value: undefined}) => void = () => {};
    factory.mockImplementationOnce((_container, config = {}) => {
        const controller = makeController(); controllers.push(controller); options.push(config);
        controller.replaceData.mockReturnValueOnce(new Promise(resolve => {completeOld = resolve;}));
        return controller as unknown as ReturnType<typeof createGraphController>;
    });
    const {rerender} = render(<GraphCanvas data={data} datasetKey='A' preset='preview' />);
    const empty: GraphData = {nodes: [], edges: []};
    rerender(<GraphCanvas data={empty} datasetKey='B' preset='preview' />);
    await flush();
    await act(async () => completeOld({status: 'applied', value: undefined}));
    rerender(<GraphCanvas data={data} datasetKey='B' preset='preview' />);
    expect(controllers[0].replaceData).toHaveBeenLastCalledWith(data, {newDataset: false, layout: true, fit: true});
    await flush();
});
