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

import {render, screen, within} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {ClusterTopology, HealthStatus, SourceStrip} from './components';
import i18n from '../../i18n';

afterEach(() => i18n.changeLanguage('en-US'));

test('localizes the topology accessible name', async () => {
    await i18n.changeLanguage('zh-CN');

    render(
        <MemoryRouter future={{v7_startTransition: true, v7_relativeSplatPath: true}}>
            <ClusterTopology nodes={[]} />
        </MemoryRouter>
    );

    expect(screen.getByLabelText('HugeGraph 集群拓扑')).toBeInTheDocument();
    expect(screen.queryByLabelText('Server PD Store topology')).not.toBeInTheDocument();
});

test('localizes the standalone deployment reason code', async () => {
    await i18n.changeLanguage('zh-CN');

    render(
        <SourceStrip
            sources={{
                pd: {
                    status: 'UNKNOWN',
                    availability: 'UNSUPPORTED',
                    reason: 'deployment_mode_unsupported',
                    observed_at: 1000,
                    last_success_at: 500,
                },
            }}
        />
    );

    expect(screen.queryByText(/当前部署模式不支持/)).not.toBeInTheDocument();
    expect(screen.getByLabelText(/PD.*当前部署不适用.*当前部署模式不支持/))
        .toHaveAccessibleName(/当前部署模式不支持/);
    expect(screen.queryByText(/deployment mode unsupported/))
        .not.toBeInTheDocument();
});

test('uses the last successful data time for a stale source', () => {
    render(
        <SourceStrip
            detailed
            sources={{
                stores: {
                    status: 'UP',
                    availability: 'PARTIAL',
                    stale: true,
                    observed_at: 2000,
                    last_success_at: 1000,
                },
            }}
            sourceNames={['stores']}
        />
    );

    const source = screen.getByText('Store').closest('.operations-source');
    const formatter = new Intl.DateTimeFormat('en-US', {
        dateStyle: 'medium',
        timeStyle: 'medium',
    });
    expect(source).toHaveTextContent(`Last observed: ${formatter.format(new Date(1000))}`);
    expect(source).not.toHaveTextContent(formatter.format(new Date(2000)));
});

test('uses a concise Attention label and explains the degraded state', () => {
    render(<HealthStatus status='DEGRADED' reason='refresh_failed' />);

    expect(screen.getByText('Attention')).toBeInTheDocument();
    expect(screen.getByRole('img', {name: /Some sources or metrics are unhealthy/}))
        .toHaveAccessibleName(/Refresh failed/);
    expect(screen.queryByText('DEGRADED')).not.toBeInTheDocument();
});

test('uses semantic tier icons and keeps the PD leader on the visual axis', () => {
    render(
        <MemoryRouter future={{v7_startTransition: true, v7_relativeSplatPath: true}}>
            <ClusterTopology nodes={[
                {
                    id: 'server-1',
                    name: 'HugeGraph Server 123abc',
                    type: 'SERVER',
                    status: 'UP',
                },
                {id: 'pd-2', name: 'pd-2', type: 'PD', status: 'UP', role: 'FOLLOWER'},
                {id: 'pd-1', name: 'pd-1', type: 'PD', status: 'UP', role: 'LEADER'},
                {id: 'store-1', name: 'store-1', type: 'STORE', status: 'UP'},
            ]}
            />
        </MemoryRouter>
    );

    expect(screen.getByLabelText('SERVER icon')).toBeInTheDocument();
    expect(screen.getByText('Server 123abc')).toBeInTheDocument();
    expect(screen.queryByText('HugeGraph Server 123abc')).not.toBeInTheDocument();
    expect(screen.getAllByLabelText('PD icon')).toHaveLength(2);
    expect(screen.getByLabelText('STORE icon')).toBeInTheDocument();
    expect(screen.getByText('pd-1').closest('a')).toHaveClass('is-axis-node');
    expect(within(screen.getByText('pd-1').closest('a'))
        .getByLabelText('Leader role')).toBeInTheDocument();
    expect(screen.getByText('pd-2').closest('a')).not.toHaveClass('is-axis-node');
    expect(screen.getByRole('link', {name: 'Server tier'})).toBeInTheDocument();
    expect(screen.getByRole('link', {name: 'Store tier'})).toBeInTheDocument();
    expect(screen.queryByRole('link', {name: 'SERVER tier'})).not.toBeInTheDocument();
});

test('shows stale metrics beside an up topology status', () => {
    render(
        <MemoryRouter future={{v7_startTransition: true, v7_relativeSplatPath: true}}>
            <ClusterTopology nodes={[{
                id: 'store-stale',
                name: 'store-stale',
                type: 'STORE',
                status: 'UP',
                metric_statuses: {system: {availability: 'UNAVAILABLE', stale: true}},
            }]}
            />
        </MemoryRouter>
    );

    const card = screen.getByText('store-stale').closest('a');
    expect(within(card).getByText('UP')).toBeInTheDocument();
    expect(within(card).getByText('Stale')).toBeInTheDocument();
});

test('explains unavailable Store metrics on the topology card', () => {
    render(
        <MemoryRouter future={{v7_startTransition: true, v7_relativeSplatPath: true}}>
            <ClusterTopology nodes={[{
                id: 'store-unavailable',
                name: 'store-unavailable',
                type: 'STORE',
                status: 'UP',
                metric_statuses: {
                    system: {
                        availability: 'UNAVAILABLE',
                        stale: false,
                        reason: 'metrics_target_untrusted',
                    },
                },
            }]}
            />
        </MemoryRouter>
    );

    const card = screen.getByText('store-unavailable').closest('a');
    expect(within(card).getByRole('img', {name: /Store metrics origin is not trusted/}))
        .toBeInTheDocument();
    expect(within(card).queryByText('Stale')).not.toBeInTheDocument();
});
