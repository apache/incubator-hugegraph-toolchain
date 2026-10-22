/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {act, fireEvent, render, screen} from '@testing-library/react';
import {MemoryRouter, Route, Routes, useNavigate} from 'react-router-dom';
import GraphDetail from './index';
import * as api from '../../api';

jest.mock('../../api', () => ({
    manage: {
        getGraphSpace: jest.fn(),
        getGraph: jest.fn(),
        getGraphStatistic: jest.fn(),
        getGraphView: jest.fn(),
        updateGraphStatistic: jest.fn(),
    },
    analysis: {getGraphData: jest.fn()},
}));

jest.mock('antd', () => ({
    ...jest.requireActual('antd'),
    message: {
        error: jest.fn(),
        success: jest.fn(),
    },
}));

jest.mock('react-i18next', () => ({
    useTranslation: () => ({t: key => ({
        'graph.detail.title': 'Detail',
        'graph.detail.last_update': 'Last Updated: ',
        'graph.detail.update_data': 'Update Data',
        'graph.detail.vertex_total': 'Total vertices',
        'graph.detail.edge_total': 'Total edges',
        'graph.detail.vertex_type': 'Vertex type',
        'graph.detail.edge_type': 'Edge type',
        'graph.detail.count': 'Count',
        'graph.detail.statistics_unavailable': 'Statistics are unavailable. Retry later.',
        'graph.detail.unavailable': 'Graph details are unavailable. Check the server and retry.',
        'graph.detail.overview': 'Overview',
        'graph.detail.schema': 'Schema',
        'graph.detail.query': 'Query this graph',
        'graph.detail.data_status': 'Data status',
        'graph.detail.available': 'Available',
        'graph.detail.partial': 'Partial',
        'graph.detail.loading': 'Loading statistics',
        'graph.detail.not_collected': 'Not collected',
        'graph.detail.collect_statistics': 'Collect statistics',
        'graph.detail.retry_page': 'Retry graph details',
        'graph.detail.back_to_graphs': 'Back to graphs',
        'graph.detail.retry_statistics': 'Retry statistics',
        'graph.detail.update_failed': 'Data update failed. Retry later.',
        'graph.detail.empty_title': 'This graph has no schema or data yet',
        'graph.detail.empty_description': 'Create schema, prepare data, or run a safe query.',
        'graph.detail.create_schema': 'Create schema',
        'graph.detail.prepare_data': 'Prepare data',
    })[key] || key}),
}));

beforeEach(() => {
    jest.resetAllMocks();
    api.manage.getGraphView.mockResolvedValue({status: 200, data: null});
    Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: query => ({
            matches: false,
            media: query,
            addListener: jest.fn(),
            removeListener: jest.fn(),
        }),
    });
});

const deferred = () => {
    let resolve;
    const promise = new Promise(done => { resolve = done; });
    return {promise, resolve};
};

const Harness = () => {
    const navigate = useNavigate();
    return <>
        <button onClick={() => navigate('/graphspace/B/graph/b/detail')}>Switch graph</button>
        <Routes><Route path='/graphspace/:graphspace/graph/:graph/detail' element={<GraphDetail />} /></Routes>
    </>;
};

const renderDetail = () => render(<MemoryRouter initialEntries={['/graphspace/A/graph/a/detail']}>
    <Harness />
</MemoryRouter>);

const readyCore = () => {
    api.manage.getGraphSpace.mockResolvedValue({status: 200, data: {name: 'A'}});
    api.manage.getGraph.mockResolvedValue({status: 200, data: {name: 'a'}});
};

test('renders core graph navigation while the snapshot read is still pending', async () => {
    readyCore();
    const snapshot = deferred();
    api.analysis.getGraphData.mockReturnValue(snapshot.promise);
    renderDetail();
    expect(await screen.findByText('Detail')).toBeInTheDocument();
    expect(screen.getByRole('link', {name: 'Schema'})).toHaveAttribute('href', '/graphspace/A/graph/a/meta');
    expect(screen.getByRole('button', {name: /Query this graph/})).toBeEnabled();
    expect(screen.getByText('Loading statistics')).toBeInTheDocument();
    expect(api.manage.getGraphStatistic).not.toHaveBeenCalled();
    await act(async () => { snapshot.resolve({status: 200, data: {vertexcount: null, edgecount: null}}); });
    expect(screen.getByText('Not collected')).toBeInTheDocument();
    expect(screen.getAllByText('--')).toHaveLength(2);
});

test('uses a recorded snapshot without running full graph statistics', async () => {
    readyCore();
    api.analysis.getGraphData.mockResolvedValue({status: 200, data: {vertexcount: 312, edgecount: 608}});
    renderDetail();
    expect(await screen.findByText('312')).toBeInTheDocument();
    expect(screen.getByText('608')).toBeInTheDocument();
    expect(screen.getByText('Available')).toBeInTheDocument();
    expect(api.manage.getGraphStatistic).not.toHaveBeenCalled();
    expect(api.manage.getGraphView).not.toHaveBeenCalled();
    expect(api.manage.updateGraphStatistic).not.toHaveBeenCalled();
});

test('runs a full count only after an explicit action and keeps core navigation usable', async () => {
    readyCore();
    api.analysis.getGraphData.mockResolvedValue({status: 200, data: {vertexcount: null, edgecount: null}});
    const full = deferred();
    api.manage.getGraphStatistic.mockReturnValue(full.promise);
    renderDetail();
    expect(await screen.findByText('Not collected')).toBeInTheDocument();
    expect(api.manage.getGraphStatistic).not.toHaveBeenCalled();
    await act(async () => { fireEvent.click(screen.getByRole('button', {name: /Collect statistics/})); });
    expect(api.manage.getGraphStatistic).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Loading statistics')).toBeInTheDocument();
    expect(screen.getByRole('button', {name: /Query this graph/})).toBeEnabled();
    expect(screen.getByRole('link', {name: 'Schema'})).toHaveAttribute('href', '/graphspace/A/graph/a/meta');
    await act(async () => {
        full.resolve({status: 200, data: {vertex_count: 12, edge_count: 20, vertices: {person: 12}, edges: {knows: 20}, approximate: true, scan_limit: 100000}});
    });
    expect(screen.getByText('≈12')).toBeInTheDocument();
    expect(screen.getByText('≈20')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByText('20')).toBeInTheDocument();
    fireEvent.mouseEnter(screen.getByText('Available'));
    expect(await screen.findByText('graph.detail.bounded_statistics_hint')).toBeInTheDocument();
});

test('shows a failed statistic read without hiding core metadata or inventing zeros', async () => {
    readyCore();
    api.analysis.getGraphData.mockResolvedValue({status: 503});
    api.manage.getGraphStatistic.mockResolvedValue({status: 200, data: {vertex_count: 2, edge_count: 3}});
    renderDetail();
    expect(await screen.findByText('Statistics are unavailable. Retry later.')).toBeInTheDocument();
    expect(screen.queryByText('0')).not.toBeInTheDocument();
    expect(screen.getByText('Detail')).toBeInTheDocument();
    await act(async () => { fireEvent.click(screen.getByRole('button', {name: 'Retry statistics'})); });
    expect(await screen.findByText('2')).toBeInTheDocument();
    expect(api.manage.getGraphSpace).toHaveBeenCalledTimes(1);
    expect(api.manage.getGraph).toHaveBeenCalledTimes(1);
});

test('ignores full statistics from the previous graph after navigation', async () => {
    readyCore();
    api.analysis.getGraphData.mockImplementation((space) => Promise.resolve({status: 200,
        data: space === 'A' ? {vertexcount: null, edgecount: null} : {vertexcount: 22, edgecount: 33}}));
    const fullA = deferred();
    api.manage.getGraphStatistic.mockReturnValue(fullA.promise);
    renderDetail();
    expect(await screen.findByText('Not collected')).toBeInTheDocument();
    await act(async () => { fireEvent.click(screen.getByRole('button', {name: /Collect statistics/})); });
    await act(async () => { fireEvent.click(screen.getByRole('button', {name: 'Switch graph'})); });
    expect(await screen.findByText('22')).toBeInTheDocument();
    await act(async () => { fullA.resolve({status: 200, data: {vertex_count: 999, edge_count: 888}}); });
    expect(screen.queryByText('999')).not.toBeInTheDocument();
    expect(screen.getByText('22')).toBeInTheDocument();
    expect(screen.getByRole('link', {name: 'Schema'})).toHaveAttribute('href', '/graphspace/B/graph/b/meta');
});

test('renders one actionable core metadata error', async () => {
    api.manage.getGraphSpace.mockRejectedValue(new Error('connection refused'));
    api.manage.getGraph.mockRejectedValue(new Error('connection refused'));
    api.analysis.getGraphData.mockResolvedValue({status: 200, data: {vertexcount: null, edgecount: null}});
    renderDetail();
    expect(await screen.findByText('Graph details are unavailable. Check the server and retry.')).toBeInTheDocument();
    expect(screen.getAllByRole('alert')).toHaveLength(1);
    expect(screen.getByRole('link', {name: 'Back to graphs'})).toHaveAttribute('href', '/graphspace/A');
});


test('zero recorded counts keep the normal page while schema loads and when schema already exists', async () => {
    readyCore();
    api.analysis.getGraphData.mockResolvedValue({status: 200, data: {vertexcount: '0', edgecount: '0'}});
    const schema = deferred();
    api.manage.getGraphView.mockReturnValue(schema.promise);
    renderDetail();
    expect(await screen.findByText('Total vertices')).toBeInTheDocument();
    expect(screen.queryByText('This graph has no schema or data yet')).not.toBeInTheDocument();
    expect(screen.getByRole('button', {name: /Query this graph/})).toBeEnabled();
    await act(async () => {
        schema.resolve({status: 200, data: {vertices: [{id: 'person'}], edges: []}});
    });
    expect(screen.getByText('Total vertices')).toBeInTheDocument();
    expect(screen.queryByText('This graph has no schema or data yet')).not.toBeInTheDocument();
    expect(api.manage.getGraphStatistic).not.toHaveBeenCalled();
});

test('shows empty graph guidance only after schema is confirmed empty', async () => {
    readyCore();
    api.analysis.getGraphData.mockResolvedValue({status: 200, data: {vertexcount: '0', edgecount: '0'}});
    const schema = deferred();
    api.manage.getGraphView.mockReturnValue(schema.promise);
    renderDetail();
    expect(await screen.findByText('Total vertices')).toBeInTheDocument();
    expect(screen.queryByText('This graph has no schema or data yet')).not.toBeInTheDocument();
    await act(async () => {
        schema.resolve({status: 200, data: {vertices: [], edges: []}});
    });
    expect(screen.getByText('This graph has no schema or data yet')).toBeInTheDocument();
    expect(api.manage.getGraphStatistic).not.toHaveBeenCalled();
});


test('marks bounded snapshot totals consistently with graph cards and canvas counts', async () => {
    readyCore();
    api.analysis.getGraphData.mockResolvedValue({status: 200, data: {
        vertexcount: '100000', edgecount: '20', approximate: true, scan_limit: 100000,
    }});
    renderDetail();
    expect(await screen.findByText('≥100000')).toBeInTheDocument();
    expect(screen.getByText('≈20')).toBeInTheDocument();
    expect(api.manage.getGraphStatistic).not.toHaveBeenCalled();
    expect(api.manage.getGraphView).not.toHaveBeenCalled();
});

test('an approximate zero remains visible without declaring the current graph empty', async () => {
    readyCore();
    api.analysis.getGraphData.mockResolvedValue({status: 200, data: {
        vertexcount: '0', edgecount: '0', approximate: true, scan_limit: 100000,
    }});
    api.manage.getGraphView.mockResolvedValue({status: 200, data: {vertices: [], edges: []}});
    renderDetail();

    expect(await screen.findAllByText('≈0')).toHaveLength(2);
    expect(screen.getByText('Total vertices')).toBeInTheDocument();
    expect(screen.queryByText('This graph has no schema or data yet')).not.toBeInTheDocument();
    expect(screen.getByRole('button', {name: /Query this graph/})).toBeEnabled();
    expect(api.manage.getGraphView).not.toHaveBeenCalled();
    expect(api.manage.getGraphStatistic).not.toHaveBeenCalled();
});
