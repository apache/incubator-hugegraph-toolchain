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

import {useContext, useMemo} from 'react';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {message} from 'antd';
import QueryGraphResult from '../analysis/QueryResult/GraphResult/Home';
import AlgorithmGraphResult from '../algorithm/GraphResult/Home';
import GraphAnalysisContext from '../Context';
import {GraphContext} from './Context';
import {adaptAnalysisGraph} from '../../utils/analysisGraphData';
import {GRAPH_STATUS, GRAPH_RENDER_MODE, PANEL_TYPE} from '../../utils/constants';
import {USER_CHANGE_EVENT} from '../../utils/user';
import * as api from '../../api';

jest.mock('react-i18next', () => {
    const t = key => key;
    return {...jest.requireActual('react-i18next'), useTranslation: () => ({t})};
});
jest.mock('../../api', () => ({analysis: {
    getVertexProperties: jest.fn(), getEdgeProperties: jest.fn(),
    updateVertexProperties: jest.fn(), updateEdgeProperties: jest.fn(),
    fetchVertexlinks: jest.fn(), fetchEdgeLabels: jest.fn(), addEdge: jest.fn(),
}, manage: {getMetaVertexLink: jest.fn(), getMetaEdge: jest.fn()}}));

// Keep both real parent state machines and target drawers. Replace the canvas
// engine and decorative controls only; scope changes still remount its subtree.
jest.mock('./Graph/useAnalysisGraph', () => ({useAnalysisGraph: ({data, graphSpaceInfo}) => {
    const scopeKey = `${graphSpaceInfo.graphSpace}/${graphSpaceInfo.graph}`;
    const controller = useMemo(() => ({scopeKey, getSnapshot: () => ({status: 'ready'})}), [scopeKey]);
    const graphData = adaptAnalysisGraph(data);
    return {scopeKey, datasetKey: scopeKey, controller, graphData, initialData: graphData,
        currentRawData: data, styleConfigData: {nodes: {}, edges: {}},
        editElement: mockApply, addElements: mockApply, expand: mockExpand};
}}));
jest.mock('./Graph', () => function Canvas({data, onClick, children}) {
    const scope = useContext(GraphAnalysisContext);
    const controller = useMemo(() => ({graphSpace: scope.graphSpace, graph: scope.graph}), [scope.graphSpace, scope.graph]);
    return <GraphContext.Provider value={{controller, run: operation => Promise.resolve().then(operation)}}>
        <button onClick={() => onClick({kind: 'node', ...data.nodes[0]})}>Inspect node</button>
        <button onClick={() => onClick({kind: 'edge', ...data.edges[0]})}>Inspect edge</button>
        {children}
    </GraphContext.Provider>;
});
jest.mock('./Menu', () => function TargetMenu({onClickSearch, onClickAddEdge}) {
    return <>
        <button onClick={() => onClickSearch({vertexId: 'shared', vertexLabel: 'person'})}>Search target</button>
        <button onClick={() => onClickAddEdge({vertexId: 'shared', vertexLabel: 'person'}, true)}>Add target edge</button>
    </>;
});
jest.mock('./Legend', () => () => null);
jest.mock('./MiniMap', () => () => null);
jest.mock('./NumberCard', () => () => null);
jest.mock('./SettingConfigPanel', () => () => null);
jest.mock('./ClosePanelButton', () => () => null);
jest.mock('./DynamicAddNode', () => () => null);
jest.mock('./StatisticsPanel/Home', () => () => null);
jest.mock('./GraphStatusView', () => () => null);
jest.mock('./TaskNavigateView', () => () => null);
jest.mock('./Canvas3D', () => () => null);
jest.mock('./layoutConfigPanel/Home', () => () => null);
jest.mock('../analysis/QueryResult/GraphResult/GraphMenubar', () => () => null);
jest.mock('../analysis/QueryResult/GraphResult/GraphToolBar', () => () => null);
jest.mock('../algorithm/GraphResult/GraphMenuBar', () => () => null);
jest.mock('../algorithm/GraphResult/GraphToolBar', () => () => null);
jest.mock('../algorithm/GraphResult/NeighborRankView', () => () => null);
jest.mock('../algorithm/GraphResult/RankApiView', () => () => null);
jest.mock('../algorithm/GraphResult/JaccView', () => () => null);

const mockApply = jest.fn().mockResolvedValue({status: 'applied'});
const mockExpand = jest.fn();
const scopeA = {graphSpace: 'DEFAULT', graph: 'A'};
const scopeB = {graphSpace: 'DEFAULT', graph: 'B'};
const dataFor = source => ({
    vertices: [{id: 'shared', label: 'person', properties: {name: `${source}-node`}},
        {id: 'other', label: 'person', properties: {name: `${source}-other`}}],
    edges: [{id: 'shared-edge', label: 'knows', source: 'shared', target: 'other',
        properties: {name: `${source}-edge`}}],
});
const metaData = {vertexMeta: [{name: 'person'}], edgeMeta: [{name: 'knows'}]};
const scene = (Component, scope) => <GraphAnalysisContext.Provider value={scope}>
    <Component data={dataFor(scope.graph)} metaData={metaData} isQueryMode
        queryStatus={GRAPH_STATUS.SUCCESS} panelType={PANEL_TYPE.CLOSED}
        graphNums={{vertexCount: 1, edgeCount: 1}} propertyKeysRecords={[]}
        graphRenderMode={GRAPH_RENDER_MODE.CANVAS2D} updatePanelType={jest.fn()} />
</GraphAnalysisContext.Provider>;

beforeAll(() => {
    window.matchMedia = query => ({matches: false, media: query, addListener: jest.fn(), removeListener: jest.fn(),
        addEventListener: jest.fn(), removeEventListener: jest.fn(), dispatchEvent: jest.fn()});
});
beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(message, 'success').mockImplementation(() => {});
    jest.spyOn(message, 'warning').mockImplementation(() => {});
    jest.spyOn(message, 'error').mockImplementation(() => {});
    const schema = {status: 200, data: {nullableProps: ['name'], nonNullableProps: [], primaryKeys: []}};
    api.analysis.getVertexProperties.mockResolvedValue(schema);
    api.analysis.getEdgeProperties.mockResolvedValue(schema);
    api.analysis.updateVertexProperties.mockResolvedValue({status: 200});
    api.analysis.updateEdgeProperties.mockResolvedValue({status: 200});
    api.analysis.fetchVertexlinks.mockResolvedValue({status: 200, data: ['knows']});
    api.manage.getMetaVertexLink.mockResolvedValue({status: 200, data: ['knows']});
});
afterEach(() => jest.restoreAllMocks());

const transitions = ['A to B', 'A to B to A', 'account change'];
const targets = ['node', 'edge', 'search', 'addEdge'];
const cases = transitions.flatMap(transition => targets.map(target => [transition, target]));

describe.each([['query', QueryGraphResult], ['algorithm', AlgorithmGraphResult]])('%s graph target ownership', (_name, Component) => {
    test.each(cases)('%s closes and clears the old %s target', async (transition, target) => {
        const view = render(scene(Component, scopeA));
        const action = {node: 'Inspect node', edge: 'Inspect edge', search: 'Search target', addEdge: 'Add target edge'}[target];
        fireEvent.click(screen.getByRole('button', {name: action}));
        expect(await screen.findByRole('dialog')).toBeInTheDocument();
        if (target === 'search') await waitFor(() => expect(api.manage.getMetaVertexLink).toHaveBeenCalled());
        if (target === 'addEdge') await waitFor(() => expect(api.analysis.fetchVertexlinks).toHaveBeenCalled());

        if (transition === 'account change') act(() => window.dispatchEvent(new Event(USER_CHANGE_EVENT)));
        else {
            view.rerender(scene(Component, scopeB));
            if (transition === 'A to B to A') view.rerender(scene(Component, scopeA));
        }
        const staleDialog = screen.queryByRole('dialog');
        if (staleDialog && ['node', 'edge'].includes(target)) {
            // On the broken source the real drawer remounts with A's details
            // under B's context. Exercise its real form and capture the PUT.
            fireEvent.click(screen.getByRole('button', {name: 'analysis.canvas.edit_element.edit'}));
            const input = await screen.findByRole('textbox', {name: 'name'});
            expect(input).toHaveValue(`A-${target}`);
            fireEvent.click(screen.getByRole('button', {name: 'analysis.canvas.edit_element.save'}));
            const update = target === 'node' ? api.analysis.updateVertexProperties : api.analysis.updateEdgeProperties;
            await waitFor(() => expect(update).toHaveBeenCalled());
        }
        expect(api.analysis.updateVertexProperties).not.toHaveBeenCalled();
        expect(api.analysis.updateEdgeProperties).not.toHaveBeenCalled();
        expect(staleDialog).toBeNull();
        expect(mockExpand).not.toHaveBeenCalled();
        expect(api.analysis.addEdge).not.toHaveBeenCalled();

        // The same ID/label can be intentionally selected again, but its details
        // must now come from the current graph rather than a retained A target.
        if (target === 'node' || target === 'edge') {
            fireEvent.click(screen.getByRole('button', {name: action}));
            fireEvent.click(await screen.findByRole('button', {name: 'analysis.canvas.edit_element.edit'}));
            const currentGraph = transition === 'A to B' ? 'B' : 'A';
            expect(await screen.findByRole('textbox', {name: 'name'})).toHaveValue(`${currentGraph}-${target}`);
        }
    });
});
