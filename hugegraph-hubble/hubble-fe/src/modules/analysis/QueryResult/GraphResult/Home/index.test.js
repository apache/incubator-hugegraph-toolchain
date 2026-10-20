/*
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

import {act, fireEvent, render, screen} from '@testing-library/react';
import GraphResult from './index';
import {USER_CHANGE_EVENT} from '../../../../../utils/user';
import GraphAnalysisContext from '../../../../Context';
import {GRAPH_STATUS, GRAPH_RENDER_MODE, PANEL_TYPE} from '../../../../../utils/constants';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: (key, values) =>
    values?.seconds === undefined ? key : `${key} ${values.seconds}`})}));
jest.mock('../../../../component/Graph/useAnalysisGraph', () => ({useAnalysisGraph: () => mockModel}));
jest.mock('../../../../component/Graph', () => {
    const {useEffect} = require('react');
    return {__esModule: true, default: function MockGraph({children}) {
        useEffect(() => { mockMount2D(); return mockUnmount2D; }, []);
        return <div data-testid="canvas-2d">{children}</div>;
    }};
});
jest.mock('../../../../component/Canvas3D', () => {
    const {useEffect} = require('react');
    return {__esModule: true, default: function MockCanvas3D({onExit, children}) {
        useEffect(() => { mockMount3D(); return mockUnmount3D; }, []);
        return <div data-testid="canvas-3d"><button onClick={onExit}>Return to 2D</button>{children}</div>;
    }};
});
jest.mock('../../../../component/Legend', () => () => null);
jest.mock('../../../../component/Menu', () => props => <button onClick={() => props.onClickAddEdge({}, true)}>Add edge</button>);
jest.mock('../../../../component/Search', () => () => null);
jest.mock('../../../../component/DynamicAddNode', () => props => { mockNodeDrawer(props); return null; });
jest.mock('../../../../component/DynamicAddEdge', () => props => {
    mockEdgeDrawer(props);
    return props.open ? <button onClick={props.onCancel}>Close edge drawer</button> : null;
});
jest.mock('../../../../component/layoutConfigPanel/Home', () => {
    const {useImperativeHandle} = require('react');
    return function MockLayoutPanel({ref, ...props}) {
        useImperativeHandle(ref, () => ({flush: mockFlushLayout, cancel: mockCancelLayout}));
        mockLayoutPanel(props);
        return null;
    };
});
jest.mock('../../../../component/SettingConfigPanel', () => props => { mockSettingsPanel(props); return null; });
jest.mock('../../../../component/ClosePanelButton', () => props => props.show ? <button onClick={props.onClick}>Close panel</button> : null);
jest.mock('../../../../component/StatisticsPanel/Home', () => () => null);
jest.mock('../../../../component/MiniMap', () => () => null);
jest.mock('../../../../component/NumberCard', () => props => { mockCounts(props); return null; });
jest.mock('../../../../component/TaskNavigateView', () => () => <div>task result</div>);
jest.mock('../../../../component/EditElement', () => () => null);
jest.mock('../GraphToolBar', () => props => <>
    <button onClick={() => props.updatePanelType(0)}>Toolbar close</button>
    <button onClick={props.handleClearGraph}>Clear graph</button>
</>);
jest.mock('../GraphMenubar', () => props => { mockMenuBar(props); return <>
    <div>graph menu</div>
    <button onClick={() => props.handleTogglePanel(1)}>Toggle layout</button>
    <button onClick={() => props.handleTogglePanel(2)}>Open settings</button>
</>; });

const mockMount2D = jest.fn();
const mockUnmount2D = jest.fn();
const mockMount3D = jest.fn();
const mockUnmount3D = jest.fn();
const mockEdgeDrawer = jest.fn();
const mockNodeDrawer = jest.fn();
const mockCounts = jest.fn();
const mockSettingsPanel = jest.fn();
const mockMenuBar = jest.fn();
const mockFlushLayout = jest.fn();
const mockCancelLayout = jest.fn();
const mockLayoutPanel = jest.fn();
let mockModel;
const raw = {vertices: [{id: 'alice', label: 'person', properties: {}}], edges: []};
const canonical = {nodes: [{id: 'node:alice', data: {raw: raw.vertices[0]}}], edges: []};
const props = () => ({data: raw, isQueryMode: true, queryStatus: GRAPH_STATUS.SUCCESS,
    graphRenderMode: GRAPH_RENDER_MODE.CANVAS2D, panelType: PANEL_TYPE.CLOSED,
    updatePanelType: jest.fn(), onGraphRenderModeChange: jest.fn()});
beforeEach(() => {
    jest.clearAllMocks();
    mockModel = {graphData: canonical, initialData: canonical, styleConfigData: {nodes: {}, edges: {}},
        datasetKey: 1, layout: {type: 'd3-force'}};
});

test('keeps the existing canvas through progress phases and cancellation, with accessible status and elapsed time', () => {
    const initial = props();
    const result = render(<GraphResult {...initial} />);
    const canvas = screen.getByTestId('canvas-2d');
    const cancel = jest.fn();
    result.rerender(<GraphResult {...initial} queryProgress={{phase: 'waiting', elapsedSeconds: 1, cancel}} />);
    expect(canvas).toContainElement(screen.getByRole('region', {name: 'analysis.query_result.loading'}));
    expect(screen.getByRole('status')).toHaveTextContent('analysis.query.phase_waiting');
    expect(screen.getByText('analysis.query.elapsed_seconds 1')).toHaveAttribute('aria-live', 'off');
    result.rerender(<GraphResult {...initial} queryProgress={{phase: 'repair_streaming', elapsedSeconds: 72,
        modelSlow: true, cancel}} />);
    expect(screen.getByText('analysis.query.phase_repair_streaming')).toBeInTheDocument();
    expect(screen.getByText('analysis.query.model_slow')).toHaveAttribute('role', 'status');
    result.rerender(<GraphResult {...initial} queryStatus={GRAPH_STATUS.LOADING}
        queryProgress={{phase: 'execution', elapsedSeconds: 75, cancel}} />);
    expect(screen.getByText('analysis.query.execution_timeout_hint')).toBeInTheDocument();
    expect(screen.queryByText('analysis.query.model_timeout_hint')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'analysis.query.cancel_generation'}));
    expect(cancel).toHaveBeenCalledTimes(1);
    result.rerender(<GraphResult {...initial} />);
    expect(screen.queryByRole('region', {name: 'analysis.query_result.loading'})).not.toBeInTheDocument();
    expect(screen.getByTestId('canvas-2d')).toBe(canvas);
    expect(mockMount2D).toHaveBeenCalledTimes(1);
    expect(mockUnmount2D).not.toHaveBeenCalled();
});

test('keeps progress while the existing 3D canvas is mounted', () => {
    const initial = {...props(), graphRenderMode: GRAPH_RENDER_MODE.CANVAS3D};
    const result = render(<GraphResult {...initial} />);
    const canvas = screen.getByTestId('canvas-3d');
    result.rerender(<GraphResult {...initial} queryStatus={GRAPH_STATUS.LOADING}
        queryProgress={{phase: 'execution', elapsedSeconds: 4, cancel: jest.fn()}} />);
    expect(screen.getByRole('status')).toHaveTextContent('analysis.query.phase_execution');
    result.rerender(<GraphResult {...initial} />);
    expect(screen.getByTestId('canvas-3d')).toBe(canvas);
    expect(mockMount3D).toHaveBeenCalledTimes(1);
    expect(mockUnmount3D).not.toHaveBeenCalled();
});

test('keeps the no-graph progress status page and exposes cancellation', () => {
    mockModel = {...mockModel, graphData: {nodes: [], edges: []}, initialData: {nodes: [], edges: []}};
    const cancel = jest.fn();
    const result = render(<GraphResult {...props()} data={{vertices: [], edges: []}}
        queryStatus={GRAPH_STATUS.STANDBY} queryProgress={{phase: 'waiting', elapsedSeconds: 2, cancel}} />);
    expect(screen.getByRole('status')).toHaveTextContent('analysis.query.phase_waiting');
    expect(screen.queryByTestId('canvas-2d')).not.toBeInTheDocument();
    expect(screen.queryByRole('region', {name: 'analysis.query_result.loading'})).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'analysis.query.cancel_generation'}));
    expect(cancel).toHaveBeenCalledTimes(1);
    result.rerender(<GraphResult {...props()} data={{vertices: [], edges: []}} queryStatus={GRAPH_STATUS.STANDBY} />);
    expect(screen.queryByText('analysis.query.phase_waiting')).not.toBeInTheDocument();
    expect(mockMount2D).not.toHaveBeenCalled();
});

test.each([GRAPH_STATUS.FAILED, GRAPH_STATUS.UPLOAD_FAILED])('terminal %s overrides stale progress and retained data', status => {
    const initial = props();
    const result = render(<GraphResult {...initial} />);
    result.rerender(<GraphResult {...initial} queryStatus={status} queryMessage="actual failure"
        queryProgress={{phase: 'waiting', elapsedSeconds: 99, cancel: jest.fn()}} />);
    expect(screen.getByText('actual failure')).toBeInTheDocument();
    expect(screen.queryByText('analysis.query.phase_waiting')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'analysis.query.cancel_generation'})).not.toBeInTheDocument();
    expect(screen.queryByTestId('canvas-2d')).not.toBeInTheDocument();
    expect(mockUnmount2D).toHaveBeenCalledTimes(1);
});

test('does not turn a task result into progress when the query-only progress prop arrives late', () => {
    render(<GraphResult {...props()} isQueryMode={false}
        queryProgress={{phase: 'waiting', elapsedSeconds: 99, cancel: jest.fn()}} />);
    expect(screen.getByText('task result')).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(screen.queryByTestId('canvas-2d')).not.toBeInTheDocument();
});

test('only projects raw node choices while the edge drawer is visible and keeps raw records by reference', () => {
    const reads = jest.fn(() => raw.vertices[0]);
    const node = {id: 'node:alice', data: {get raw() { return reads(); }}};
    mockModel = {...mockModel, graphData: {nodes: [node], edges: []}};
    const initial = props();
    const result = render(<GraphResult {...initial} />);
    expect(reads).not.toHaveBeenCalled();
    expect(mockEdgeDrawer).toHaveBeenLastCalledWith(expect.objectContaining({graphData: null}));
    fireEvent.click(screen.getByRole('button', {name: 'Add edge'}));
    expect(reads).toHaveBeenCalledTimes(1);
    const choices = mockEdgeDrawer.mock.lastCall[0].graphData;
    expect(choices).toEqual({vertices: raw.vertices});
    expect(choices.vertices[0]).toBe(raw.vertices[0]);
    result.rerender(<GraphResult {...initial} queryProgress={{phase: 'waiting', elapsedSeconds: 1}} />);
    expect(reads).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByRole('button', {name: 'Close edge drawer'}));
    expect(mockEdgeDrawer).toHaveBeenLastCalledWith(expect.objectContaining({graphData: null}));
    expect(reads).toHaveBeenCalledTimes(1);
});


test.each(['node', 'edge'])('returns %s write-apply failures to the drawer without changing totals', async kind => {
    const error = new Error('local apply failed');
    const onRefreshGraphNums = jest.fn().mockResolvedValue();
    mockModel = {...mockModel, graphData: {nodes: [{id: 'existing', data: {raw: {id: 'existing'}}}], edges: []},
        addElements: jest.fn().mockRejectedValue(error)};
    render(<GraphResult data={{vertices: [{id: 'existing', label: 'person'}], edges: []}} isQueryMode
        queryStatus={GRAPH_STATUS.SUCCESS} panelType={PANEL_TYPE.CLOSED}
        graphNums={{vertexCount: 7, edgeCount: 9}} onRefreshGraphNums={onRefreshGraphNums}
        graphRenderMode={GRAPH_RENDER_MODE.CANVAS2D} updatePanelType={jest.fn()} />);
    const callback = kind === 'node' ? mockNodeDrawer.mock.lastCall[0].onOK : mockEdgeDrawer.mock.lastCall[0].onOk;
    await act(async () => { await expect(callback({vertices: [{id: 'saved'}], edges: [{id: 'saved-edge'}]})).rejects.toBe(error); });
    expect(onRefreshGraphNums).toHaveBeenCalledTimes(1);
    expect(mockCounts.mock.lastCall[0].data).toMatchObject({allGraphNodesNum: 7, allGraphEdgesNum: 9});
});

test.each(['applied', 'superseded', 'disposed', undefined])('preserves %s write-apply outcome while refreshing server totals independently', async status => {
    const outcome = status ? {status} : undefined;
    const onRefreshGraphNums = jest.fn().mockResolvedValue();
    mockModel = {...mockModel, graphData: {nodes: [{id: 'existing', data: {raw: {id: 'existing'}}}], edges: []},
        addElements: jest.fn().mockResolvedValue(outcome)};
    render(<GraphResult data={{vertices: [{id: 'existing', label: 'person'}], edges: []}} isQueryMode
        queryStatus={GRAPH_STATUS.SUCCESS} panelType={PANEL_TYPE.CLOSED}
        graphNums={{vertexCount: 7, edgeCount: 9}} onRefreshGraphNums={onRefreshGraphNums}
        graphRenderMode={GRAPH_RENDER_MODE.CANVAS2D} updatePanelType={jest.fn()} />);
    await act(async () => {
        await expect(mockNodeDrawer.mock.lastCall[0].onOK({vertices: [{id: 'saved'}]}))
            .resolves.toEqual(outcome ?? {status: 'disposed'});
        await expect(mockEdgeDrawer.mock.lastCall[0].onOk({edges: [{id: 'saved-edge'}]}))
            .resolves.toEqual(outcome ?? {status: 'disposed'});
    });
    expect(mockCounts.mock.lastCall[0].data).toMatchObject({
        allGraphNodesNum: 7, allGraphEdgesNum: 9,
    });
    expect(onRefreshGraphNums).toHaveBeenCalledTimes(2);
});

const countedGraphProps = () => ({data: {vertices: [{id: 'existing', label: 'person'}], edges: []},
    isQueryMode: true, queryStatus: GRAPH_STATUS.SUCCESS, panelType: PANEL_TYPE.CLOSED,
    graphNums: {vertexCount: 7, edgeCount: 9}, graphRenderMode: GRAPH_RENDER_MODE.CANVAS2D,
    updatePanelType: jest.fn()});

test.each([
    ['node', {vertices: [{id: 'existing'}, {id: 'existing'}]}],
    ['node', {vertices: [{id: 'already-in-db-but-not-on-canvas'}]}],
    ['edge', {vertices: [{id: 'existing'}, {id: 'already-in-db-but-not-on-canvas'}], edges: [{id: 'saved-edge'}]}],
])('refreshes authoritative totals after a saved %s without inferring inserts from returned IDs', async (kind, saved) => {
    mockModel = {...mockModel, addElements: jest.fn().mockResolvedValue({status: 'applied'})};
    const onRefreshGraphNums = jest.fn().mockResolvedValue();
    const initial = {...countedGraphProps(), onRefreshGraphNums};
    const view = render(<GraphResult {...initial} />);
    const callback = kind === 'node' ? mockNodeDrawer.mock.lastCall[0].onOK : mockEdgeDrawer.mock.lastCall[0].onOk;
    await act(async () => { await expect(callback(saved)).resolves.toEqual({status: 'applied'}); });
    expect(mockModel.addElements).toHaveBeenCalledWith(saved);
    expect(onRefreshGraphNums).toHaveBeenCalledTimes(1);
    expect(mockCounts.mock.lastCall[0].data).toMatchObject({allGraphNodesNum: 7, allGraphEdgesNum: 9});
    view.rerender(<GraphResult {...initial} graphNums={{vertexCount: 40, edgeCount: 60}} />);
    expect(mockCounts.mock.lastCall[0].data).toMatchObject({allGraphNodesNum: 40, allGraphEdgesNum: 60});
});

test.each(['node', 'edge'])('a failed statistics refresh never rejects a saved %s canvas application', async kind => {
    mockModel = {...mockModel, addElements: jest.fn().mockResolvedValue({status: 'applied'})};
    const onRefreshGraphNums = jest.fn().mockRejectedValue(new Error('statistics unavailable'));
    render(<GraphResult {...countedGraphProps()} onRefreshGraphNums={onRefreshGraphNums} />);
    const callback = kind === 'node' ? mockNodeDrawer.mock.lastCall[0].onOK : mockEdgeDrawer.mock.lastCall[0].onOk;
    await act(async () => { await expect(callback({vertices: [{id: 'saved'}]})).resolves.toEqual({status: 'applied'}); });
    expect(onRefreshGraphNums).toHaveBeenCalledTimes(1);
    expect(mockCounts.mock.lastCall[0].data).toMatchObject({allGraphNodesNum: 7, allGraphEdgesNum: 9});
});

test('a pending statistics refresh does not delay returning the canvas application outcome', async () => {
    mockModel = {...mockModel, addElements: jest.fn().mockResolvedValue({status: 'applied'})};
    const onRefreshGraphNums = jest.fn(() => new Promise(() => {}));
    render(<GraphResult {...countedGraphProps()} onRefreshGraphNums={onRefreshGraphNums} />);
    await act(async () => {
        await expect(mockNodeDrawer.mock.lastCall[0].onOK({vertices: [{id: 'saved'}]}))
            .resolves.toEqual({status: 'applied'});
    });
    expect(onRefreshGraphNums).toHaveBeenCalledTimes(1);
});


test.each([
    ['Close panel', PANEL_TYPE.CLOSED],
    ['Toggle layout', PANEL_TYPE.CLOSED],
    ['Open settings', PANEL_TYPE.SETTING],
    ['Toolbar close', PANEL_TYPE.CLOSED],
])('flushes the layout edit before the explicit %s transition', (action, nextPanel) => {
    mockModel = {...mockModel, graphData: {nodes: [{id: 'node:alice', data: {raw: {id: 'alice'}}}], edges: []},
        initialData: {nodes: [], edges: []}, layout: {type: 'd3-force', options: {collide: {radius: 55}}},
        layoutSettings: {type: 'd3-force', options: {nodeSize: 70, nodeSpacing: 40}}};
    const updatePanelType = jest.fn(() => expect(mockFlushLayout).toHaveBeenCalledTimes(1));
    render(<GraphResult data={{vertices: [{id: 'alice'}], edges: []}} isQueryMode
        queryStatus={GRAPH_STATUS.SUCCESS} panelType={PANEL_TYPE.LAYOUT}
        updatePanelType={updatePanelType} graphRenderMode={GRAPH_RENDER_MODE.CANVAS2D} />);
    expect(mockLayoutPanel.mock.lastCall[0].layout).toBe(mockModel.layoutSettings);
    fireEvent.click(screen.getByRole('button', {name: action}));
    expect(updatePanelType).toHaveBeenCalledWith(nextPanel);
    expect(mockFlushLayout).toHaveBeenCalledTimes(1);
    expect(mockCancelLayout).not.toHaveBeenCalled();
});

test.each(['Clear graph', 'Switch renderer'])('cancels pending layout edits for %s', action => {
    mockModel = {...mockModel, graphData: {nodes: [{id: 'node:alice', data: {raw: {id: 'alice'}}}], edges: []}};
    render(<GraphResult data={{vertices: [{id: 'alice'}], edges: []}} isQueryMode
        queryStatus={GRAPH_STATUS.SUCCESS} panelType={PANEL_TYPE.LAYOUT}
        updatePanelType={jest.fn()} onGraphRenderModeChange={jest.fn()}
        graphRenderMode={GRAPH_RENDER_MODE.CANVAS2D} />);
    if (action === 'Switch renderer') act(() => mockSettingsPanel.mock.lastCall[0].onRenderModeChange(GRAPH_RENDER_MODE.CANVAS3D));
    else fireEvent.click(screen.getByRole('button', {name: action}));
    expect(mockCancelLayout).toHaveBeenCalledTimes(1);
    expect(mockFlushLayout).not.toHaveBeenCalled();
});

test('direct result unmount does not flush a layout edit', () => {
    mockModel = {...mockModel, graphData: {nodes: [{id: 'node:alice', data: {raw: {id: 'alice'}}}], edges: []}};
    const view = render(<GraphResult data={{vertices: [{id: 'alice'}], edges: []}} isQueryMode
        queryStatus={GRAPH_STATUS.SUCCESS} panelType={PANEL_TYPE.LAYOUT}
        updatePanelType={jest.fn()} graphRenderMode={GRAPH_RENDER_MODE.CANVAS2D} />);
    view.unmount();
    expect(mockFlushLayout).not.toHaveBeenCalled();
});

test('returns from unavailable 3D through the normal query renderer switch', () => {
    const initial = {...props(), graphRenderMode: GRAPH_RENDER_MODE.CANVAS3D};
    render(<GraphResult {...initial} />);
    fireEvent.click(screen.getByRole('button', {name: 'Return to 2D'}));
    expect(initial.onGraphRenderModeChange).toHaveBeenCalledWith(GRAPH_RENDER_MODE.CANVAS2D);
    expect(initial.updatePanelType).toHaveBeenCalledWith(PANEL_TYPE.CLOSED);
    expect(screen.getByText('graph menu')).toBeInTheDocument();
});

test('passes dataset-owned label choices to the 2D settings panel', () => {
    const fields = {current: {node: new Map(), edge: new Map()}};
    mockModel.labelFields = fields;
    render(<GraphResult {...props()} />);
    expect(mockSettingsPanel.mock.lastCall[0].selectedFields).toBe(fields);
});

test('keeps renderer settings reachable in 3D and switches back through the owned handler', () => {
    const onGraphRenderModeChange = jest.fn();
    const initial = {...props(), graphRenderMode: GRAPH_RENDER_MODE.CANVAS3D,
        panelType: PANEL_TYPE.SETTING, onGraphRenderModeChange};
    render(<GraphResult {...initial} />);
    const settings = mockSettingsPanel.mock.lastCall[0];
    expect(settings.open).toBe(true);
    expect(settings.renderMode).toBe(GRAPH_RENDER_MODE.CANVAS3D);
    act(() => settings.onRenderModeChange(GRAPH_RENDER_MODE.CANVAS2D));
    expect(onGraphRenderModeChange).toHaveBeenCalledWith(GRAPH_RENDER_MODE.CANVAS2D);
    expect(initial.updatePanelType).toHaveBeenCalledWith(PANEL_TYPE.CLOSED);
});

test('quick layout cancels pending panel edits and uses the existing layout update path', () => {
    const changeLayout = jest.fn();
    mockModel = {...mockModel, changeLayout, layoutSettings: {type: 'grid'}, defaultLayout: {type: 'columns'},
        graphData: {nodes: [{id: 'node:alice', data: {raw: {id: 'alice'}}}], edges: []}};
    const initial = {...props(), panelType: PANEL_TYPE.LAYOUT};
    const view = render(<GraphResult {...initial} />);
    expect(mockMenuBar.mock.lastCall[0].layout).toBe(mockModel.layoutSettings);
    expect(mockMenuBar.mock.lastCall[0].defaultLayout).toBe(mockModel.defaultLayout);
    act(() => mockMenuBar.mock.lastCall[0].handleQuickLayoutChange({type: 'radial'}));
    expect(mockCancelLayout).toHaveBeenCalledTimes(1);
    expect(mockFlushLayout).not.toHaveBeenCalled();
    expect(changeLayout).toHaveBeenCalledTimes(1);
    expect(changeLayout).toHaveBeenCalledWith({type: 'radial'});
    mockModel.layoutSettings = {type: 'radial'};
    view.rerender(<GraphResult {...initial} />);
    expect(mockLayoutPanel.mock.lastCall[0].layout).toBe(mockModel.layoutSettings);
    expect(initial.updatePanelType).not.toHaveBeenCalled();
});


test.each(['node', 'edge'].flatMap(kind => ['graph', 'account'].map(change => [kind, change])))(
    'keeps late %s drawer closes idempotent after a %s change', (kind, change) => {
        const initial = props();
        const scene = graph => <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph}}>
            <GraphResult {...initial} />
        </GraphAnalysisContext.Provider>;
        const view = render(scene('A'));
        const drawer = kind === 'node' ? mockNodeDrawer : mockEdgeDrawer;
        act(() => {
            const menu = mockMenuBar.mock.lastCall[0];
            if (kind === 'node') menu.handleClickNewAddNode();
            else menu.handleClickNewAddEdge(true);
        });
        expect(drawer.mock.lastCall[0].open).toBe(true);
        const lateClose = drawer.mock.lastCall[0].onCancel;
        if (change === 'graph') view.rerender(scene('B'));
        else act(() => window.dispatchEvent(new Event(USER_CHANGE_EVENT)));
        expect(drawer.mock.lastCall[0].open).toBe(false);
        // A completed write may still deliver its old close callback.
        act(() => lateClose());
        expect(drawer.mock.lastCall[0].open).toBe(false);
    }
);
