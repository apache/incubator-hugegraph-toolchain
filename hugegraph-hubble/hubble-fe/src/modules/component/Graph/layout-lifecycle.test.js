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
import {createPortal} from 'react-dom';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {message} from 'antd';
import {BaseEdge, BaseNode} from '@antv/g6';
import {BehaviorController} from '@antv/g6/lib/runtime/behavior';
import Graph from './index';
import {GraphContext} from '../Context';
import Menu from '../Menu';

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

// G6 emits its hit-tested menu during pointerdown, before the browser's
// mousedown and contextmenu. Use the installed forwarder, not a later mock callback.
const rightClick = kind => {
    const surface = screen.getByTestId('graph-surface');
    const client = {x: 60, y: 90};
    const input = {button: 2, bubbles: true, cancelable: true, clientX: client.x, clientY: client.y};
    const element = kind && Object.create(kind === 'node' ? BaseNode.prototype : BaseEdge.prototype, {
        id: {value: mockEvent.id}, type: {value: kind}, destroyed: {value: false},
    });
    const behavior = new BehaviorController({
        options: {},
        graph: {emit: (name, event) => {
            if (kind && name === `${kind}:contextmenu`) {
                event.preventDefault();
                mockCanvasProps.onContextMenu({...mockEvent, kind, client});
            }
        }},
        canvas: {getContainer: () => surface,
            document: {addEventListener: jest.fn(), removeAllEventListeners: jest.fn()}},
    });
    surface.addEventListener('pointerdown', event => {
        behavior.forwardCanvasEvents({type: event.type, button: event.button, client, target: element});
    }, {once: true});
    try {
        fireEvent(surface, new MouseEvent('pointerdown', input));
        const mouseDown = new MouseEvent('mousedown', input);
        fireEvent(surface, mouseDown);
        const nativeMenu = new MouseEvent('contextmenu', input);
        fireEvent(surface, nativeMenu);
        expect(nativeMenu.defaultPrevented).toBe(true);
        return mouseDown;
    }
    finally { behavior.destroy(); }
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
    rightClick();
    expect(mockContext.contextTarget).toEqual({kind: 'canvas', client: {x: 60, y: 90}});
    rightClick('node');
    expect(mockContext.contextTarget.kind).toBe('node');
    view.rerender(<Graph data={data} datasetKey='query-2'><Probe /></Graph>);
    expect(mockContext.contextTarget).toBeNull();
});

test.each(['node', 'edge'])('retains the G6 %s hit through later native mouse events', kind => {
    render(<Graph data={{nodes: [], edges: []}}><Probe /><Menu onClickAddNode={() => {}} /></Graph>);
    rightClick(kind);
    expect(mockContext.contextTarget).toMatchObject({...mockEvent, kind});
    expect(screen.getByRole('menuitem', {name: 'analysis.canvas.context_menu.hide'})).toBeInTheDocument();
    expect(screen.queryByRole('menuitem', {name: 'analysis.canvas.context_menu.add_vertex'})).not.toBeInTheDocument();
    expect(screen.getAllByRole('menuitem')[0]).toHaveFocus();
});

test('a new right-click replaces an open menu before dismissal and keeps menu actions usable', () => {
    const add = jest.fn();
    render(<Graph data={{nodes: [], edges: []}}><Probe /><Menu onClickAddNode={add} /></Graph>);
    rightClick('node');
    expect(mockContext.contextTarget.kind).toBe('node');
    rightClick('edge');
    expect(mockContext.contextTarget.kind).toBe('edge');
    rightClick();
    expect(mockContext.contextTarget.kind).toBe('canvas');
    const action = screen.getByRole('menuitem', {name: 'analysis.canvas.context_menu.add_vertex'});
    expect(action).toHaveFocus();
    fireEvent(action, new MouseEvent('pointerdown', {button: 0, bubbles: true}));
    fireEvent.mouseDown(action);
    expect(action).toBeInTheDocument();
    fireEvent.click(action);
    expect(add).toHaveBeenCalledTimes(1);
    expect(mockContext.contextTarget).toBeNull();
});

test('right mouse input cancels default canvas focus after the menu has focused its first action', () => {
    render(<Graph data={{nodes: [], edges: []}}><Probe /><Menu onClickAddNode={() => {}} /></Graph>);
    for (const kind of ['node', 'edge', undefined]) {
        expect(rightClick(kind).defaultPrevented).toBe(true);
        expect(screen.getAllByRole('menuitem')[0]).toHaveFocus();
    }
    const leftMouseDown = new MouseEvent('mousedown', {button: 0, bubbles: true, cancelable: true});
    fireEvent(screen.getByTestId('graph-surface'), leftMouseDown);
    expect(leftMouseDown.defaultPrevented).toBe(false);
});

test('left pointer input dismisses menus and focuses the canvas without stealing control focus', () => {
    render(<Graph data={{nodes: [], edges: []}}><Probe /><input aria-label='Graph input' /></Graph>);
    rightClick('node');
    const surface = screen.getByTestId('graph-surface');
    const input = screen.getByRole('textbox', {name: 'Graph input'});
    input.focus();
    fireEvent(surface, new MouseEvent('pointerdown', {button: 0, bubbles: true}));
    expect(input).toHaveFocus();
    fireEvent.mouseDown(surface);
    expect(mockContext.contextTarget).toBeNull();
    expect(mockContext.containerRef.current).toHaveFocus();
    input.focus();
    fireEvent(input, new MouseEvent('pointerdown', {button: 0, bubbles: true}));
    fireEvent.mouseDown(input);
    expect(input).toHaveFocus();
});

test('portal option pointer handlers run before the wrapper changes input focus', () => {
    let focusedDuringOptionPointer;
    const optionPointer = jest.fn(() => { focusedDuringOptionPointer = document.activeElement; });
    render(<Graph data={{nodes: [], edges: []}}><Probe /><input aria-label='Portal select input' />
        {createPortal(<div role='option' onPointerDown={optionPointer}>Choose graph field</div>, document.body)}
    </Graph>);
    const input = screen.getByRole('textbox', {name: 'Portal select input'});
    input.focus();
    fireEvent(screen.getByRole('option'), new MouseEvent('pointerdown', {button: 0, bubbles: true}));
    expect(optionPointer).toHaveBeenCalledTimes(1);
    expect(focusedDuringOptionPointer).toBe(input);
    expect(input).toHaveFocus();
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
