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

import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {
    MemoryRouter,
    Route,
    Routes,
    useLocation,
    useNavigate,
} from 'react-router-dom';
import Overview from './Overview';
import Nodes from './Nodes';
import NodeDetail from './NodeDetail';
import {getNode, getNodes, getOverview} from '../../api/operations';
import {getDashboard} from '../../api/auth';
import '../../i18n';

jest.mock('../../api/operations');
jest.mock('../../api/auth');

const OVERVIEW = {
    status: 'UP',
    observed_at: 1000,
    sources: {},
    facts: {},
    nodes: [{id: 'store-safe', name: 'Store A', type: 'STORE', status: 'UP'}],
};

const DETAIL = {
    node: {
        id: 'store-safe',
        name: 'Store A',
        type: 'STORE',
        status: 'UP',
        version: '1.5.0',
        metrics: {},
    },
    observed_at: 1000,
    sources: {},
};

const HistoryControls = () => {
    const location = useLocation();
    const navigate = useNavigate();
    return (
        <>
            <button type='button' onClick={() => navigate(-1)}>browser back</button>
            <button type='button' onClick={() => navigate(1)}>browser forward</button>
            <output aria-label='current location'>
                {location.pathname}{location.search}
            </output>
        </>
    );
};

const Journey = () => (
    <>
        <HistoryControls />
        <Routes>
            <Route path='/operations/overview' element={<Overview />} />
            <Route path='/operations/nodes/:nodeId' element={<NodeDetail />} />
            <Route path='/operations/nodes' element={<Nodes />} />
        </Routes>
    </>
);

beforeEach(() => {
    sessionStorage.clear();
    sessionStorage.setItem('hubble_config_', JSON.stringify({pd_enabled: true}));
    window.matchMedia = () => ({
        matches: false,
        addListener: () => {},
        removeListener: () => {},
        addEventListener: () => {},
        removeEventListener: () => {},
    });
    getOverview.mockResolvedValue(OVERVIEW);
    getNodes.mockResolvedValue({
        items: OVERVIEW.nodes,
        total: OVERVIEW.nodes.length,
        observed_at: OVERVIEW.observed_at,
        stale: false,
    });
    getNode.mockResolvedValue(DETAIL);
    getDashboard.mockReturnValue(new Promise(() => {}));
});

afterEach(() => {
    jest.restoreAllMocks();
    jest.clearAllMocks();
});

test('keeps the overview node list across detail and browser history', async () => {
    render(
        <MemoryRouter
            initialEntries={['/operations/overview']}
            future={{v7_startTransition: true, v7_relativeSplatPath: true}}
        >
            <Journey />
        </MemoryRouter>
    );

    expect(await screen.findByRole('radio', {name: 'Topology'})).toBeChecked();
    fireEvent.click(screen.getByRole('radio', {name: 'Node list'}));
    expect(screen.getByRole('radio', {name: 'Node list'})).toBeChecked();
    expect(screen.getByLabelText('current location'))
        .toHaveTextContent('/operations/overview?view=nodes');

    fireEvent.click(screen.getByRole('link', {name: 'Store A'}));
    expect(await screen.findByRole('heading', {name: 'Store A'})).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', {name: 'browser back'}));
    expect(await screen.findByRole('radio', {name: 'Node list'})).toBeChecked();

    fireEvent.click(screen.getByRole('button', {name: 'browser forward'}));
    expect(await screen.findByRole('heading', {name: 'Store A'})).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', {name: 'arrow-left Back'}));
    expect(await screen.findByRole('radio', {name: 'Node list'})).toBeChecked();
    expect(screen.getByLabelText('current location'))
        .toHaveTextContent('/operations/overview?view=nodes');
});

test('returns a topology node detail to the current overview query', async () => {
    render(
        <MemoryRouter
            initialEntries={['/operations/overview?source=topology']}
            future={{v7_startTransition: true, v7_relativeSplatPath: true}}
        >
            <Journey />
        </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('link', {name: /STORE Store A.*UP/}));
    expect(await screen.findByRole('heading', {name: 'Store A'})).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'arrow-left Back'}));

    expect(await screen.findByRole('radio', {name: 'Topology'})).toBeChecked();
    expect(screen.getByLabelText('current location'))
        .toHaveTextContent('/operations/overview?source=topology');
});

test('returns an attention detail to the overview query that opened it', async () => {
    getOverview.mockResolvedValue({
        ...OVERVIEW,
        status: 'DEGRADED',
        nodes: [{...OVERVIEW.nodes[0], status: 'DOWN'}],
    });

    render(
        <MemoryRouter
            initialEntries={['/operations/overview?source=attention']}
            future={{v7_startTransition: true, v7_relativeSplatPath: true}}
        >
            <Journey />
        </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('link', {name: 'View details'}));
    expect(await screen.findByRole('heading', {name: 'Store A'})).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'arrow-left Back'}));

    expect(await screen.findByRole('link', {name: 'View details'})).toBeInTheDocument();
    expect(screen.getByLabelText('current location'))
        .toHaveTextContent('/operations/overview?source=attention');
});

test('uses the node list fallback for a direct or unsafe detail entry', async () => {
    render(
        <MemoryRouter
            initialEntries={[{
                pathname: '/operations/nodes/store-safe',
                state: {operationsReturnTo: '//example.invalid/operations/overview?view=nodes'},
            }]}
            future={{v7_startTransition: true, v7_relativeSplatPath: true}}
        >
            <Journey />
        </MemoryRouter>
    );

    expect(await screen.findByRole('heading', {name: 'Store A'})).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'arrow-left Back'}));

    expect(await screen.findByRole('link', {name: /Store A/})).toBeInTheDocument();
    expect(screen.getByLabelText('current location')).toHaveTextContent('/operations/nodes');
});

test('recovers a failed detail to the overview node-list view', async () => {
    getNode.mockRejectedValue(new Error('down'));

    render(
        <MemoryRouter
            initialEntries={['/operations/overview?view=nodes']}
            future={{v7_startTransition: true, v7_relativeSplatPath: true}}
        >
            <Journey />
        </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('link', {name: 'Store A'}));
    expect(await screen.findByRole('alert')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'Back to nodes'}));

    expect(await screen.findByRole('radio', {name: 'Node list'})).toBeChecked();
    expect(screen.getByLabelText('current location'))
        .toHaveTextContent('/operations/overview?view=nodes');
});

test('recovers a failed detail to the filtered node list', async () => {
    getNode.mockRejectedValue(new Error('down'));

    render(
        <MemoryRouter
            initialEntries={['/operations/nodes?query=Store&page=2']}
            future={{v7_startTransition: true, v7_relativeSplatPath: true}}
        >
            <Journey />
        </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('link', {name: /Store A/}));
    expect(await screen.findByRole('alert')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'Back to nodes'}));

    expect(await screen.findByRole('link', {name: /Store A/})).toBeInTheDocument();
    expect(screen.getByLabelText('current location'))
        .toHaveTextContent('/operations/nodes?query=Store&page=2');
});

test('preserves node-list filters when its detail returns', async () => {
    render(
        <MemoryRouter
            initialEntries={['/operations/nodes?query=Store&page=2']}
            future={{v7_startTransition: true, v7_relativeSplatPath: true}}
        >
            <Journey />
        </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('link', {name: /Store A/}));
    expect(await screen.findByRole('heading', {name: 'Store A'})).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'arrow-left Back'}));

    expect(await screen.findByRole('link', {name: /Store A/})).toBeInTheDocument();
    await waitFor(() => expect(screen.getByLabelText('current location'))
        .toHaveTextContent('/operations/nodes?query=Store&page=2'));
});
