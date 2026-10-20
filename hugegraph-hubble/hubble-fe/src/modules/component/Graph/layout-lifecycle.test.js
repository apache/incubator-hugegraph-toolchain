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
import KeyboardAction from '../../../components/KeyboardAction';

const mockController = {getSnapshot: jest.fn(), zoomBy: jest.fn(), fitView: jest.fn()};
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
const rightClick = (kind, {button = 2, ctrlKey = false, menuButton = button, suppressed = true} = {}) => {
    const surface = screen.getByTestId('graph-surface');
    const client = {x: 60, y: 90};
    const input = {button, ctrlKey, bubbles: true, cancelable: true, clientX: client.x, clientY: client.y};
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
        const nativeMenu = new MouseEvent('contextmenu', {...input, button: menuButton});
        fireEvent(surface, nativeMenu);
        expect(nativeMenu.defaultPrevented).toBe(suppressed);
        return mouseDown;
    }
    finally { behavior.destroy(); }
};

beforeEach(() => { jest.clearAllMocks(); jest.useFakeTimers(); mockSnapshot.status = 'ready'; });
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

test.each([0, 2])('allows the native Ctrl+click menu with contextmenu button %s when G6 did not open a menu', menuButton => {
    render(<Graph data={{nodes: [], edges: []}}><Probe /><Menu onClickAddNode={() => {}} /></Graph>);
    rightClick('node');
    expect(mockContext.contextTarget.kind).toBe('node');
    const mouseDown = rightClick('node', {button: 0, ctrlKey: true, menuButton, suppressed: false});
    expect(mouseDown.defaultPrevented).toBe(false);
    expect(mockContext.contextTarget).toBeNull();
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
});

test('does not suppress a later native menu without a new button-2 pointerdown', () => {
    render(<Graph data={{nodes: [], edges: []}}><Probe /></Graph>);
    rightClick('node');
    const nativeMenu = new MouseEvent('contextmenu', {button: 2, bubbles: true, cancelable: true});
    fireEvent(screen.getByTestId('graph-surface'), nativeMenu);
    expect(nativeMenu.defaultPrevented).toBe(false);
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

test('focused graph keys use the toolbar zoom factors and fit without handling F or browser zoom', () => {
    render(<Graph data={{nodes: [], edges: []}}><Probe /></Graph>);
    const container = mockContext.containerRef.current;
    container.focus();
    const press = (key, options = {}) => {
        const event = new KeyboardEvent('keydown', {key, bubbles: true, cancelable: true, ...options});
        fireEvent(container, event);
        return event;
    };
    for (const [key, options] of [['+', {shiftKey: true}], ['=', {}], ['-', {}], ['0', {}]]) {
        expect(press(key, options).defaultPrevented).toBe(true);
    }
    expect(mockController.zoomBy.mock.calls).toEqual([[1.25], [1.25], [0.8]]);
    expect(mockController.fitView).toHaveBeenCalledTimes(1);
    for (const options of [{ctrlKey: true}, {metaKey: true}, {altKey: true},
        {isComposing: true}, {keyCode: 229}]) {
        expect(press('=', options).defaultPrevented).toBe(false);
    }
    expect(press('f').defaultPrevented).toBe(false);
    expect(mockController.zoomBy).toHaveBeenCalledTimes(3);
    expect(mockController.fitView).toHaveBeenCalledTimes(1);
});

test.each([
    ['input', <input key='input' aria-label='editing' />],
    ['textarea', <textarea key='textarea' aria-label='editing' />],
    ['select', <select key='select' aria-label='editing' />],
    ['button', <button key='button' aria-label='editing'>Action</button>],
    ['role button', <KeyboardAction key='role-button' onAction={() => {}} aria-label='editing'>Action</KeyboardAction>],
    ['contenteditable', <div key='contenteditable' aria-label='editing' contentEditable tabIndex={0} />],
    ['CM6', <div key='cm6' className='cm-editor'><div aria-label='editing' tabIndex={0} /></div>],
    ['textbox', <div key='textbox' role='textbox' aria-label='editing' tabIndex={0} />],
])('does not zoom while editing or using a %s control', (_name, editor) => {
    render(<Graph data={{nodes: [], edges: []}}>{editor}</Graph>);
    const target = screen.getByLabelText('editing');
    target.focus();
    const event = new KeyboardEvent('keydown', {key: '=', bubbles: true, cancelable: true});
    fireEvent(target, event);
    expect(event.defaultPrevented).toBe(false);
    expect(mockController.zoomBy).not.toHaveBeenCalled();
});

test('does not zoom from a focused KeyboardAction descendant', () => {
    render(<Graph data={{nodes: [], edges: []}}>
        <KeyboardAction onAction={() => {}} aria-label='panel action'><span data-testid='action-label'>Panel</span></KeyboardAction>
    </Graph>);
    screen.getByRole('button', {name: 'panel action'}).focus();
    for (const key of ['+', '=', '-', '0']) {
        const event = new KeyboardEvent('keydown', {key, bubbles: true, cancelable: true});
        fireEvent(screen.getByTestId('action-label'), event);
        expect(event.defaultPrevented).toBe(false);
    }
    expect(mockController.zoomBy).not.toHaveBeenCalled();
    expect(mockController.fitView).not.toHaveBeenCalled();
});

test('ignores prevented events, portal dialogs, unfocused graphs and not-ready controllers', () => {
    const portal = <div role='dialog' aria-label='portal dialog' tabIndex={0} />;
    const data = {nodes: [], edges: []};
    const children = <><div tabIndex={0} data-testid='handled-key' onKeyDown={event => event.preventDefault()} />
        {createPortal(portal, document.body)}</>;
    const view = render(<Graph data={data}><Probe />{children}</Graph>);
    const container = mockContext.containerRef.current;
    fireEvent.keyDown(container, {key: '='});
    const handled = screen.getByTestId('handled-key');
    handled.focus();
    fireEvent.keyDown(handled, {key: '='});
    const dialog = screen.getByRole('dialog');
    dialog.focus();
    fireEvent.keyDown(dialog, {key: '='});
    container.focus();
    fireEvent.keyDown(document.body, {key: '='});
    mockSnapshot.status = 'updating';
    view.rerender(<Graph data={data}><Probe />{children}</Graph>);
    fireEvent.keyDown(container, {key: '='});
    expect(mockController.zoomBy).not.toHaveBeenCalled();
});

test('a focused nested graph handles its key once without triggering the parent canvas', () => {
    const view = render(<Graph data={{nodes: [], edges: []}}>
        <Graph data={{nodes: [], edges: []}} />
    </Graph>);
    const [outer, inner] = view.container.querySelectorAll('[data-hubble-graph]');
    inner.focus();
    fireEvent.keyDown(inner, {key: '='});
    expect(mockController.zoomBy.mock.calls).toEqual([[1.25]]);
    outer.focus();
    fireEvent.keyDown(outer, {key: '0'});
    expect(mockController.fitView).toHaveBeenCalledTimes(1);
});
