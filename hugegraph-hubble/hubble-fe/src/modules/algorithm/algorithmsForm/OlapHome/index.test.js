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

import {render, screen, waitFor} from '@testing-library/react';
import GraphAnalysisContext from '../../../Context';
import OlapFormHome from '.';
import * as api from '../../../../api';

jest.mock('../../../../api', () => ({
    analysis: {
        getOlapCapability: jest.fn(),
    },
}));

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

const renderHome = (context = {
    isVermeer: false,
    graphStatus: 'LOADED',
}) => render(
    <GraphAnalysisContext.Provider
        value={context}
    >
        <OlapFormHome
            graphSpace="DEFAULT"
            graph="hugegraph"
            search=""
            currentAlgorithm=""
            updateCurrentAlgorithm={jest.fn()}
            onOlapFormSubmit={jest.fn()}
            canRunLouvain
        />
    </GraphAnalysisContext.Provider>
);

beforeEach(() => {
    jest.clearAllMocks();
});

test('does not render runnable Computer algorithms when capability is unavailable',
    async () => {
        api.analysis.getOlapCapability.mockResolvedValue({
            status: 200,
            data: {available: false},
        });

        renderHome();

        expect(await screen.findByText(
            'analysis.algorithm.computer_unavailable_title'
        )).toBeInTheDocument();
        expect(screen.queryByTestId('olap-item')).not.toBeInTheDocument();
    });

test('renders Computer algorithms only after capability succeeds', async () => {
    api.analysis.getOlapCapability.mockResolvedValue({
        status: 200,
        data: {available: true},
    });

    renderHome();

    await waitFor(() => {
        expect(screen.getAllByTestId('olap-item').length).toBeGreaterThan(0);
    });
    expect(api.analysis.getOlapCapability).toHaveBeenCalledWith(
        'DEFAULT',
        'hugegraph',
        {suppressBusinessErrorToast: true}
    );
});

test('renders Vermeer algorithms without probing Computer', () => {
    renderHome({isVermeer: true, graphStatus: 'LOADED'});

    expect(screen.getAllByTestId('olap-item').length).toBeGreaterThan(0);
    expect(api.analysis.getOlapCapability).not.toHaveBeenCalled();
    expect(screen.queryByText(
        'analysis.algorithm.vermeer_unavailable_title'
    )).not.toBeInTheDocument();
});
