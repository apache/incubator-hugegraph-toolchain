/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import {useContext, useEffect} from 'react';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {message} from 'antd';
import Graph from './index';
import {GraphContext} from '../Context';

const mockController = {getSnapshot: jest.fn()};
const mockSnapshot = {status: 'ready', data: {nodes: [], edges: []},
    selectedIds: [], hoveredIds: [], canUndo: false, canRedo: false};
let mockCanvasProps;
let mockContext;
const mockEvent = {id: 'node:number:0', kind: 'node', data: {raw: {id: 0, label: 'person'}}};

jest.mock('../../../visualization/react', () => ({
    useGraphSnapshot: () => mockSnapshot,
    GraphCanvas: props => {
        mockCanvasProps = props;
        const {onReady} = props;
        useEffect(() => {
            onReady(mockController);
            return () => onReady(null);
        }, [onReady]);
        return <>
            <canvas data-testid='graph-surface' />
            <button onClick={() => props.onClick?.(mockEvent)}>Inspect node</button>
        </>;
    },
}));
jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
jest.mock('antd', () => ({message: {error: jest.fn()}}));
const Probe = () => {
    mockContext = useContext(GraphContext);
    return null;
};

beforeEach(() => { jest.clearAllMocks(); jest.useFakeTimers(); });
afterEach(() => { jest.useRealTimers(); });

test('shares the controller snapshot without exposing an engine or rewriting result data', () => {
    const onReady = jest.fn();
    const onClick = jest.fn();
    const data = {nodes: [], edges: []};
    const layout = {type: 'grid'};
    const view = render(<Graph data={data} datasetKey='query-1' layout={layout}
        onReady={onReady} onClick={onClick}><Probe /></Graph>);
    expect(mockContext.controller).toBe(mockController);
    expect(mockContext.snapshot).toBe(mockSnapshot);
    expect(mockContext).not.toHaveProperty('graph');
    expect(mockCanvasProps.data).toBe(data);
    expect(mockCanvasProps.layout).toBe(layout);
    expect(mockCanvasProps.datasetKey).toBe('query-1');
    fireEvent.click(screen.getByRole('button', {name: 'Inspect node'}));
    expect(onClick).not.toHaveBeenCalled();
    act(() => { jest.advanceTimersByTime(300); });
    expect(onClick).toHaveBeenCalledWith(mockEvent);
    expect(mockContext.containerRef.current).toContainElement(screen.getByTestId('graph-surface'));
    view.unmount();
    expect(onReady).toHaveBeenLastCalledWith(null);
});

test('catches command failures once and preserves successful command results', async () => {
    render(<Graph data={{nodes: [], edges: []}}><Probe /></Graph>);
    const result = {status: 'applied', value: true};
    await expect(mockContext.run(async () => result)).resolves.toBe(result);
    await expect(mockContext.run(async () => { throw new Error('draw failed'); })).resolves.toBeUndefined();
    expect(message.error).toHaveBeenCalledTimes(1);
    expect(message.error).toHaveBeenCalledWith('draw failed');
});

test('canvas menus are owned by the current instance and reset at a dataset boundary', () => {
    const data = {nodes: [], edges: []};
    const view = render(<Graph data={data} datasetKey='query-1'><Probe /></Graph>);
    fireEvent.contextMenu(screen.getByTestId('graph-surface'), {clientX: 60, clientY: 90});
    expect(mockContext.contextTarget).toEqual({kind: 'canvas', client: {x: 60, y: 90}});
    act(() => mockCanvasProps.onContextMenu({...mockEvent, client: {x: 60, y: 90}}));
    expect(mockContext.contextTarget.kind).toBe('node');
    view.rerender(<Graph data={data} datasetKey='query-2'><Probe /></Graph>);
    expect(mockContext.contextTarget).toBeNull();
});

test('tooltip fields remain React text and preserve long IDs without executing markup', () => {
    render(<Graph data={{nodes: [], edges: []}} />);
    render(mockCanvasProps.tooltip({kind: 'node', data: {raw: {
        id: 900719925474099312345n, label: '<script>unsafe</script>',
        properties: {payload: '<img src=x onerror=alert(1)>', nested: {value: 3n}},
    }}}));
    expect(screen.getByText(/900719925474099312345/)).toBeInTheDocument();
    expect(screen.getByText(/<script>unsafe<\/script>/)).toBeInTheDocument();
    expect(document.querySelector('script, img')).toBeNull();
    expect(screen.getByText(/nested:.*"value":"3"/)).toBeInTheDocument();
});


test('double-click expands immediately without opening a single-click details drawer', () => {
    const onClick = jest.fn(); const onDoubleClick = jest.fn();
    render(<Graph data={{nodes: [], edges: []}} onClick={onClick} onDoubleClick={onDoubleClick} />);
    act(() => { mockCanvasProps.onClick(mockEvent); });
    act(() => { jest.advanceTimersByTime(120); mockCanvasProps.onClick(mockEvent); mockCanvasProps.onDoubleClick(mockEvent); });
    expect(onDoubleClick).toHaveBeenCalledTimes(1);
    expect(onDoubleClick).toHaveBeenCalledWith(mockEvent);
    act(() => { jest.advanceTimersByTime(500); });
    expect(onClick).not.toHaveBeenCalled();
});

test('pending node inspection cannot run after a dataset change or unmount', () => {
    const onClick = jest.fn(); const data = {nodes: [], edges: []};
    const view = render(<Graph data={data} datasetKey='old' onClick={onClick} />);
    act(() => { mockCanvasProps.onClick(mockEvent); });
    view.rerender(<Graph data={data} datasetKey='new' onClick={onClick} />);
    act(() => { jest.advanceTimersByTime(300); });
    expect(onClick).not.toHaveBeenCalled();
    act(() => { mockCanvasProps.onClick(mockEvent); });
    view.unmount();
    act(() => { jest.advanceTimersByTime(300); });
    expect(onClick).not.toHaveBeenCalled();
});

test('edge inspection remains immediate and delayed node callback failures are reported', async () => {
    const onClick = jest.fn();
    render(<Graph data={{nodes: [], edges: []}} onClick={onClick} />);
    const edge = {...mockEvent, kind: 'edge'};
    act(() => { mockCanvasProps.onClick(edge); });
    expect(onClick).toHaveBeenCalledWith(edge);
    onClick.mockImplementationOnce(() => { throw new Error('inspection failed'); });
    await act(async () => { mockCanvasProps.onClick(mockEvent); jest.advanceTimersByTime(300); });
    expect(message.error).toHaveBeenCalledTimes(1);
    expect(message.error).toHaveBeenCalledWith('inspection failed');
});


test('a newer target owns the details drawer without a stale node inspection', () => {
    const onClick = jest.fn();
    render(<Graph data={{nodes: [], edges: []}} onClick={onClick} />);
    const edge = {...mockEvent, kind: 'edge', id: 'edge:ab'};
    act(() => { mockCanvasProps.onClick(mockEvent); jest.advanceTimersByTime(100); mockCanvasProps.onClick(edge); });
    act(() => { jest.advanceTimersByTime(300); });
    expect(onClick.mock.calls).toEqual([[edge]]);
    onClick.mockClear();
    const other = {...mockEvent, id: 'node:b'};
    act(() => { mockCanvasProps.onClick(mockEvent); jest.advanceTimersByTime(100); mockCanvasProps.onClick(other); });
    expect(onClick).not.toHaveBeenCalled();
    act(() => { jest.advanceTimersByTime(300); });
    expect(onClick.mock.calls).toEqual([[other]]);
});


test('same-dataset handler refresh keeps the pending inspection and calls the latest handler', () => {
    const original = jest.fn(); const latest = jest.fn(); const data = {nodes: [], edges: []};
    const view = render(<Graph data={data} datasetKey='same' onClick={original} />);
    act(() => { mockCanvasProps.onClick(mockEvent); jest.advanceTimersByTime(100); });
    view.rerender(<Graph data={data} datasetKey='same' onClick={latest} onDoubleClick={() => {}} />);
    act(() => { jest.advanceTimersByTime(200); });
    expect(original).not.toHaveBeenCalled();
    expect(latest).toHaveBeenCalledTimes(1);
    expect(latest).toHaveBeenCalledWith(mockEvent);
});
