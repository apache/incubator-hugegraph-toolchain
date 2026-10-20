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

import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import GraphStatistics from './index';
import {GraphContext} from '../../../Context';
import {hubbleRenderId} from '../../../../../visualization/hubble/adapter';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
jest.mock('../../BarChartComponent', () => ({__esModule: true, default: ({data}) => <pre>{JSON.stringify(data)}</pre>}));
const nodeId = hubbleRenderId('node', 90071992547409930000n);
const peerId = hubbleRenderId('node', 'peer');
const edgeId = hubbleRenderId('edge', 'edge');
const fixture = () => ({controller: {select: jest.fn(async () => ({})), hide: jest.fn(async () => ({}))},
    snapshot: {status: 'ready', selectedIds: [nodeId], hoveredIds: [], data: {
        nodes: [{id: nodeId, data: {raw: {id: 90071992547409930000n, statistics: {incidence_vertices: ['peer']}}}},
            {id: peerId, data: {raw: {id: 'peer'}}}], edges: [{id: edgeId, data: {raw: {id: 'edge'}}}]}},
    run: operation => Promise.resolve().then(operation)});
const statistics = {isolated_vertices: [90071992547409930000n], isolated_edges: ['edge']};

test('maps backend statistic IDs to rendering IDs and uses selected raw node incidence', async () => {
    const value = fixture();
    render(<GraphContext.Provider value={value}><GraphStatistics statistics={statistics} /></GraphContext.Provider>);
    const highlight = screen.getAllByRole('button', {name: 'analysis.canvas.statistics_panel.highlight'});
    const hide = screen.getAllByRole('button', {name: 'analysis.canvas.statistics_panel.hide'});
    fireEvent.click(highlight[0]);
    fireEvent.click(highlight[1]);
    fireEvent.click(highlight[2]);
    fireEvent.click(hide[1]);
    await waitFor(() => expect(value.controller.hide).toHaveBeenCalledWith([edgeId], true));
    expect(value.controller.select.mock.calls).toEqual([[[]], [[edgeId]], [[peerId]]]);
    expect(screen.getByText(/90071992547409930000/)).toBeInTheDocument();
});

test('restores hidden statistic elements and disables unavailable incidence', async () => {
    const value = fixture();
    value.snapshot.data.edges[0].style = {visibility: 'hidden'};
    value.snapshot.selectedIds = [];
    render(<GraphContext.Provider value={value}><GraphStatistics statistics={statistics} /></GraphContext.Provider>);
    const hide = screen.getAllByRole('button', {name: 'analysis.canvas.statistics_panel.hide'});
    fireEvent.click(hide[1]);
    await waitFor(() => expect(value.controller.hide).toHaveBeenCalledWith([edgeId], false));
    expect(hide[2]).toBeDisabled();
});
