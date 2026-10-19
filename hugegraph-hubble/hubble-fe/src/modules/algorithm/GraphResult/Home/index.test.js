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

import {act, fireEvent, render, screen} from '@testing-library/react';
import GraphResult from './index';
import GraphAnalysisContext from '../../../Context';
import {GRAPH_STATUS, GRAPH_RENDER_MODE, PANEL_TYPE} from '../../../../utils/constants';

let mockModel;
const mockEdgeDrawer = jest.fn();
const mockNodeDrawer = jest.fn();
const mockCounts = jest.fn();
jest.mock('../../../component/Graph/useAnalysisGraph', () => ({useAnalysisGraph: () => mockModel}));

jest.mock('../../../component/Graph', () => ({children}) => <div>{children}</div>);
jest.mock('../../../component/Legend', () => () => null);
jest.mock('../../../component/MiniMap', () => () => null);
jest.mock('../GraphMenuBar', () => () => <div>graph menu</div>);
jest.mock('../GraphToolBar', () => () => null);
jest.mock('../../../component/Menu', () => props => <button onClick={() => props.onClickAddEdge({}, true)}>Add edge</button>);
jest.mock('../../../component/NumberCard', () => props => { mockCounts(props); return null; });
jest.mock('../../../component/EditElement', () => () => null);
jest.mock('../../../component/Search', () => () => null);
jest.mock('../../../component/SettingConfigPanel', () => () => null);
jest.mock('../../../component/layoutConfigPanel/Home', () => () => null);
jest.mock('../../../component/ClosePanelButton', () => () => null);
jest.mock('../../../component/DynamicAddNode', () => props => { mockNodeDrawer(props); return null; });
jest.mock('../../../component/DynamicAddEdge', () => props => {
    mockEdgeDrawer(props);
    return props.open ? <button onClick={props.onCancel}>Close edge drawer</button> : null;
});
jest.mock('../NeighborRankView', () => () => null);
jest.mock('../../../component/StatisticsPanel/Home', () => () => null);
jest.mock('../RankApiView', () => () => null);
jest.mock('../JaccView', () => () => null);
jest.mock('../../../component/GraphStatusView', () => props => <div>{props.message}</div>);
jest.mock('../../../component/TaskNavigateView', () => () => null);
jest.mock('../../../component/Canvas3D', () => () => null);
jest.mock('react-router-dom', () => ({
    Link: ({to, children}) => <a href={to}>{children}</a>,
}));
jest.mock('react-i18next', () => ({
    useTranslation: () => ({t: key => key}),
}));
beforeEach(() => {
    jest.clearAllMocks();
    mockModel = {graphData: {nodes: [], edges: []}, initialData: {nodes: [], edges: []},
        styleConfigData: {nodes: {}, edges: {}}};
});

test('places empty-graph recovery actions in the graph result area', () => {
    render(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
            <GraphResult
                data={{}}
                metaData={{edgeMeta: [], vertexMeta: []}}
                graphNums={{vertexCount: 0, edgeCount: 0}}
                queryStatus={GRAPH_STATUS.STANDBY}
                panelType={PANEL_TYPE.CLOSED}
                graphRenderMode={GRAPH_RENDER_MODE.CANVAS2D}
                onGraphRenderModeChange={jest.fn()}
                updatePanelType={jest.fn()}
            />
        </GraphAnalysisContext.Provider>
    );

    expect(screen.getByText('analysis.algorithm.empty_graph_title')).toBeInTheDocument();
    expect(screen.getByRole('link', {name: 'analysis.algorithm.create_schema'}))
        .toHaveAttribute('href', '/graphspace/DEFAULT/graph/hugegraph/meta');
    expect(screen.getByRole('link', {name: 'analysis.algorithm.prepare_data'}))
        .toHaveAttribute('href', '/source');
});

test.each([null, undefined])('unavailable graph counts (%s) do not imply an empty database', count => {
    render(<GraphResult data={{}} metaData={{vertexMeta: [{name: 'person'}], edgeMeta: [{name: 'knows'}]}}
        graphNums={{vertexCount: count, edgeCount: count}} queryStatus={GRAPH_STATUS.STANDBY}
        panelType={PANEL_TYPE.CLOSED} graphRenderMode={GRAPH_RENDER_MODE.CANVAS2D}
        updatePanelType={jest.fn()} />);
    expect(screen.queryByText('analysis.algorithm.empty_graph_title')).not.toBeInTheDocument();
    expect(mockCounts.mock.lastCall[0].data).toMatchObject({allGraphNodesNum: count, allGraphEdgesNum: count});
});

test('only projects raw node choices while the edge drawer is visible', () => {
    const raw = {id: 'alice', label: 'person'};
    const reads = jest.fn(() => raw);
    mockModel = {...mockModel, graphData: {nodes: [{id: 'node:alice', data: {get raw() { return reads(); }}}], edges: []}};
    const props = {data: {vertices: [raw], edges: []}, isQueryMode: true,
        queryStatus: GRAPH_STATUS.SUCCESS, panelType: PANEL_TYPE.CLOSED,
        graphRenderMode: GRAPH_RENDER_MODE.CANVAS2D, updatePanelType: jest.fn()};
    const result = render(<GraphResult {...props} />);
    expect(reads).not.toHaveBeenCalled();
    expect(mockEdgeDrawer).toHaveBeenLastCalledWith(expect.objectContaining({graphData: null}));
    fireEvent.click(screen.getByRole('button', {name: 'Add edge'}));
    expect(reads).toHaveBeenCalledTimes(1);
    expect(mockEdgeDrawer.mock.lastCall[0].graphData.vertices[0]).toBe(raw);
    result.rerender(<GraphResult {...props} graphNums={{vertexCount: 1, edgeCount: 0}} />);
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
