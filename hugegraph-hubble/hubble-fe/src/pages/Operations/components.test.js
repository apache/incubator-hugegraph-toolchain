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

test.each([
    ['UP', 'AVAILABLE', false, 'up', 'UP'],
    ['UP', 'PARTIAL', false, 'degraded', 'Attention'],
    ['UP', 'UNAVAILABLE', false, 'degraded', 'Attention'],
    ['UP', 'MALFORMED', false, 'degraded', 'Attention'],
    ['UP', 'AVAILABLE', true, 'degraded', 'Attention'],
    ...['AVAILABLE', 'PARTIAL', 'UNAVAILABLE', 'MALFORMED', 'UNSUPPORTED'].flatMap(availability => [
        ['DEGRADED', availability, false, 'degraded', 'Attention'],
        ['DOWN', availability, false, 'down', 'DOWN'],
    ]),
    ['UNKNOWN', 'UNAVAILABLE', false, 'unknown', 'UNKNOWN'],
    ['UNKNOWN', 'PARTIAL', false, 'degraded', 'Attention'],
    ['UNKNOWN', 'UNSUPPORTED', false, 'unknown', 'UNKNOWN'],
])('combines topology %s, collection %s and stale=%s in source health', (
    status, availability, stale, colorStatus, label
) => {
    render(
        <SourceStrip
            sources={{stores: {status, availability, stale}}}
            sourceNames={['stores']}
        />
    );

    const source = screen.getByText('Store').closest('.operations-source');
    const badge = source.querySelector('.operations-health');
    expect(badge).toHaveClass(`status-${colorStatus}`);
    expect(badge).toHaveTextContent(label);
    const topologyLabel = status === 'DEGRADED' ? 'Attention' : status;
    expect(source).toHaveAccessibleName(new RegExp(`Node status: ${topologyLabel}`));
    if (status === 'DOWN') {
        expect(source).toHaveAccessibleName(/Abnormal - Unavailable/);
        expect(source).not.toHaveTextContent('Not applicable in this deployment');
    }
    if (status === 'DEGRADED') {
        expect(source).not.toHaveTextContent('Not applicable in this deployment');
    }
    if (colorStatus !== 'up') {
        expect(badge).not.toHaveClass('status-up');
        expect(within(badge).queryByText('UP', {exact: true})).not.toBeInTheDocument();
    }
});

test('keeps node UP as secondary evidence when collection is partially available', () => {
    render(
        <SourceStrip
            detailed
            sources={{stores: {
                status: 'UP', availability: 'PARTIAL', reason: 'response_too_large',
            }}}
            sourceNames={['stores']}
        />
    );

    const source = screen.getByText('Store').closest('.operations-source');
    expect(source.querySelector('.operations-health')).toHaveClass('status-degraded');
    expect(source).toHaveTextContent('Node status: UP');
    expect(source).toHaveTextContent('Metrics collection: Partially available');
    expect(source).toHaveTextContent(i18n.t('operations.reason_response_too_large'));
});

test('does not show a green UP beside the Chinese partial-availability warning', async () => {
    await i18n.changeLanguage('zh-CN');
    render(
        <SourceStrip
            sources={{stores: {status: 'UP', availability: 'PARTIAL'}}}
            sourceNames={['stores']}
        />
    );

    const source = screen.getByText('Store').closest('.operations-source');
    expect(within(source).getByText('需关注')).toBeInTheDocument();
    expect(within(source).getByText('异常 - 部分可用')).toBeInTheDocument();
    expect(source.querySelector('.operations-health')).toHaveClass('status-degraded');
    expect(within(source).queryByText('UP', {exact: true})).not.toBeInTheDocument();
    expect(source).toHaveAccessibleName(/节点状态: UP/);
});

test.each([
    ['en-US', 'Cluster state', 'Data state', 'PD reports a data fault'],
    ['zh-CN', '集群状态', '数据状态', 'PD 上报数据故障'],
])('shows raw cluster and data health in %s', async (
    language, clusterLabel, dataLabel, dataReason
) => {
    await i18n.changeLanguage(language);
    render(
        <SourceStrip
            detailed
            sourceNames={['pd']}
            sources={{pd: {
                status: 'UP',
                availability: 'PARTIAL',
                cluster_state: 'Cluster_OK',
                data_state: 'PState_Fault',
                reason: 'data_fault',
            }}}
        />
    );

    const source = screen.getByText('PD').closest('.operations-source');
    expect(source).toHaveTextContent(`${clusterLabel}: Cluster_OK`);
    expect(source).toHaveTextContent(`${dataLabel}: PState_Fault`);
    expect(source).toHaveTextContent(dataReason);
    expect(source.querySelector('.operations-health')).not.toHaveClass('status-down');
});

test.each([
    ['en-US', 'Some graph backend metrics failed'],
    ['zh-CN', '部分图的后端指标获取失败'],
])('localizes backend collection failures in %s', async (language, message) => {
    await i18n.changeLanguage(language);
    render(<HealthStatus status='DEGRADED' reason='backend_metrics_failed' size='large' />);

    expect(screen.getByText(new RegExp(message))).toBeInTheDocument();
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
                    name: 'Server 123abc',
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

test('preserves custom server names in the topology', () => {
    render(
        <MemoryRouter future={{v7_startTransition: true, v7_relativeSplatPath: true}}>
            <ClusterTopology nodes={[
                {id: 'server-custom', name: 'HugeGraph Server Production', type: 'SERVER', status: 'UP'},
            ]} />
        </MemoryRouter>
    );

    expect(screen.getByText('HugeGraph Server Production')).toBeInTheDocument();
    expect(screen.queryByText('Server Production')).not.toBeInTheDocument();
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
