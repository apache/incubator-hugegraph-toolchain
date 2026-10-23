/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import GraphAnalysisContext from '../../../Context';
import OlapFormHome from '.';
import * as api from '../../../../api';

jest.mock('../../../../api', () => ({
    analysis: {
        getOlapCapability: jest.fn(),
    },
}));

jest.mock('../algorithmDocs', () => ({COMPUTER_DOC: 'https://computer.example/docs'}));

jest.mock('../Olap/OlapItem', () => ({algorithmName}) => (
    <div data-testid="olap-item">{algorithmName}</div>
));

jest.mock('../../../../utils/constants', () => ({
    GRAPH_LOAD_STATUS: {LOADED: 'LOADED'},
    isAlgorithmNameMatched: () => true,
    TEXT_PATH: {ALGORITHM_COMMON: 'analysis.algorithm.common'},
    useTranslatedConstants: () => ({
        ALGORITHM_MODE: {OLAP: 'OLAP'},
        ALGORITHM_NAME: {
            PAGE_RANK: 'PageRank',
            WEAKLY_CONNECTED_COMPONENT: 'WCC',
            DEGREE_CENTRALIT: 'Degree',
            CLOSENESS_CENTRALITY: 'Closeness',
            TRIANGLE_COUNT: 'Triangle',
            RINGS_DETECTION: 'Rings',
            FILTERED_RINGS_DETECTION: 'FilteredRings',
            LINKS: 'Links',
            CLUSTER_COEFFICIENT: 'Cluster',
            BETWEENNESS_CENTRALITY: 'Betweenness',
            LABEL_PROPAGATION_ALGORITHM: 'LabelPropagation',
            LOUVAIN: 'Louvain',
            FILTER_SUBGRAPH_MATCHING: 'Subgraph',
            K_CORE: 'KCore',
            PERSONAL_PAGE_RANK: 'PersonalPageRank',
            SSSP: 'SSSP',
        },
    }),
}));

jest.mock('react-i18next', () => ({
    useTranslation: () => ({t: key => key}),
}));

const home = (context = {
    isVermeer: false,
    graphStatus: 'LOADED',
}, graph = 'hugegraph') => (
    <GraphAnalysisContext.Provider
        value={context}
    >
        <OlapFormHome
            graphSpace="DEFAULT"
            graph={graph}
            search=""
            currentAlgorithm=""
            updateCurrentAlgorithm={jest.fn()}
            onOlapFormSubmit={jest.fn()}
            canRunLouvain
        />
    </GraphAnalysisContext.Provider>
);

const renderHome = (context, graph) => render(home(context, graph));
const expandComputer = () => fireEvent.click(screen.getByRole('button', {
    name: 'analysis.algorithm.computer_legacy_entry',
}));

beforeEach(() => {
    jest.clearAllMocks();
});

test('defaults to Vermeer guidance without silently probing or enabling Computer', () => {
    renderHome();

    expect(screen.getByText('analysis.algorithm.vermeer_olap')).toBeInTheDocument();
    expect(screen.getByText('analysis.algorithm.vermeer_not_connected')).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'analysis.algorithm.computer_legacy_entry'}))
        .toHaveAttribute('aria-expanded', 'false');
    expect(api.analysis.getOlapCapability).not.toHaveBeenCalled();
    expect(screen.queryByTestId('olap-item')).not.toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
});

test('keeps unavailable Computer algorithms hidden after explicit expansion', async () => {
    api.analysis.getOlapCapability.mockResolvedValue({status: 200, data: {available: false}});
    renderHome();
    expandComputer();

    expect(await screen.findByText('analysis.algorithm.computer_legacy_unavailable'))
        .toBeInTheDocument();
    expect(screen.queryByTestId('olap-item')).not.toBeInTheDocument();
});

test('renders Computer algorithms only after explicit expansion and capability success', async () => {
    api.analysis.getOlapCapability.mockResolvedValue({status: 200, data: {available: true}});
    renderHome();
    expandComputer();

    await waitFor(() => expect(screen.getAllByTestId('olap-item')).toHaveLength(15));
    expect(screen.getByText('analysis.algorithm.computer_legacy_ready')).toBeInTheDocument();
    expect(screen.getByText('Louvain')).toBeInTheDocument();
    expect(screen.queryByText('SSSP')).not.toBeInTheDocument();
    expect(api.analysis.getOlapCapability).toHaveBeenCalledWith(
        'DEFAULT', 'hugegraph', {suppressBusinessErrorToast: true}
    );
    expandComputer();
    expect(screen.queryByTestId('olap-item')).not.toBeInTheDocument();
});

test('shows only the nine supported Vermeer algorithms without probing Computer', () => {
    renderHome({isVermeer: true, graphStatus: 'LOADED'});

    expect(screen.getAllByTestId('olap-item')).toHaveLength(9);
    expect(screen.getByText('analysis.algorithm.vermeer_ready')).toBeInTheDocument();
    expect(screen.getByText('SSSP')).toBeInTheDocument();
    expect(screen.queryByText('Louvain')).not.toBeInTheDocument();
    expect(screen.queryByText('analysis.algorithm.computer_legacy_entry')).not.toBeInTheDocument();
    expect(api.analysis.getOlapCapability).not.toHaveBeenCalled();
});

test('explains that Vermeer must load the graph before algorithms can run', () => {
    renderHome({isVermeer: true, graphStatus: 'created'});

    expect(screen.getByText('analysis.algorithm.vermeer_load_first')).toBeInTheDocument();
    expect(screen.queryByText('analysis.algorithm.vermeer_ready')).not.toBeInTheDocument();
});

test('does not carry Computer opt-in or a late capability response into another graph', async () => {
    let resolve;
    api.analysis.getOlapCapability.mockReturnValue(new Promise(done => { resolve = done; }));
    const view = renderHome();
    expandComputer();
    view.rerender(home({isVermeer: false, graphStatus: 'LOADED'}, 'other'));
    await act(async () => resolve({status: 200, data: {available: true}}));

    expect(screen.getByRole('button', {name: 'analysis.algorithm.computer_legacy_entry'}))
        .toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByTestId('olap-item')).not.toBeInTheDocument();
    expect(api.analysis.getOlapCapability).toHaveBeenCalledTimes(1);

    view.rerender(home());
    expect(screen.getByRole('button', {name: 'analysis.algorithm.computer_legacy_entry'}))
        .toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByTestId('olap-item')).not.toBeInTheDocument();
    expect(api.analysis.getOlapCapability).toHaveBeenCalledTimes(1);
});

test('returning to a previously enabled graph requires a new Computer opt-in', async () => {
    api.analysis.getOlapCapability.mockResolvedValue({status: 200, data: {available: true}});
    const view = renderHome();
    expandComputer();
    await screen.findByText('analysis.algorithm.computer_legacy_ready');

    view.rerender(home({isVermeer: false, graphStatus: 'LOADED'}, 'other'));
    view.rerender(home());

    expect(screen.getByRole('button', {name: 'analysis.algorithm.computer_legacy_entry'}))
        .toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByTestId('olap-item')).not.toBeInTheDocument();
    expect(api.analysis.getOlapCapability).toHaveBeenCalledTimes(1);
    expandComputer();
    await screen.findByText('analysis.algorithm.computer_legacy_ready');
    expect(api.analysis.getOlapCapability).toHaveBeenCalledTimes(2);
});

test('ignores graph A capability after graph B has explicitly opted in', async () => {
    let resolveA;
    api.analysis.getOlapCapability
        .mockReturnValueOnce(new Promise(done => { resolveA = done; }))
        .mockResolvedValueOnce({status: 200, data: {available: false}});
    const view = renderHome();
    expandComputer();
    view.rerender(home({isVermeer: false, graphStatus: 'LOADED'}, 'other'));
    expandComputer();
    await screen.findByText('analysis.algorithm.computer_legacy_unavailable');

    await act(async () => resolveA({status: 200, data: {available: true}}));

    expect(screen.getByText('analysis.algorithm.computer_legacy_unavailable')).toBeInTheDocument();
    expect(screen.queryByTestId('olap-item')).not.toBeInTheDocument();
    expect(api.analysis.getOlapCapability).toHaveBeenCalledTimes(2);
});
