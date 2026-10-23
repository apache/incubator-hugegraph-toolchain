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

import {fireEvent, render, screen, within} from '@testing-library/react';
import SourceDiagnostics from './SourceDiagnostics';
import i18n from '../../i18n';

beforeEach(() => {
    i18n.changeLanguage('en-US');
    window.matchMedia = () => ({matches: false, addListener() {}, removeListener() {}});
});

test('keeps source diagnostics available without discovered nodes and reports bounded response evidence', () => {
    render(<SourceDiagnostics sourceName='stores' sources={{stores: {
        status: 'UP', availability: 'PARTIAL', reason: 'response_too_large',
        observed_at: 2000, last_success_at: 1000, fresh: false, stale: true,
        diagnostics: {request_path: '/v1/stores', max_response_bytes: 1048576,
            response_bytes: 1048577, response_size_exact: false},
    }}} />);
    const panel = screen.getByRole('region', {name: 'Source diagnostics'});
    expect(within(panel).getByText('/v1/stores')).toBeInTheDocument();
    expect(within(panel).getByText('At least 1 MB (1,048,577 B)')).toBeInTheDocument();
    expect(within(panel).getByText('1 MB (1,048,576 B)', {exact: true})).toBeInTheDocument();
    expect(within(panel).getByText('Partially available')).toBeInTheDocument();
    expect(within(panel).getByText('UP', {exact: true})).toBeInTheDocument();
    expect(within(panel).getByText('Last success')).toBeInTheDocument();
    expect(within(panel).getByText(/does not mean every node is healthy/)).toBeInTheDocument();
    expect(within(panel).getByText(/metrics or discovery response exceeded/)).toBeInTheDocument();
});

test('shows per-node failed metric requests while keeping successful groups out of the failure list', () => {
    render(<SourceDiagnostics sourceName='stores' sources={{stores: {
        status: 'UP', availability: 'PARTIAL', reason: 'store_metrics_partial',
        diagnostics: {affected_node_count: 25, failed_metric_groups: ['backend']},
    }}} nodes={[{id: 'store-1', type: 'STORE', name: 'Store A', metric_statuses: {
        system: {availability: 'AVAILABLE'},
        backend: {availability: 'UNAVAILABLE', reason: 'response_too_large',
            diagnostics: {request_path: '/metrics/backend', http_status: 503}},
    }}]} />);
    expect(screen.getByText(/Store A · backend/)).toBeInTheDocument();
    expect(screen.getByText('/metrics/backend')).toBeInTheDocument();
    expect(screen.getByText('503')).toBeInTheDocument();
    expect(screen.getByText(/Affected nodes across this snapshot.*25/)).toBeInTheDocument();
    expect(screen.queryByText(/Store A · system/)).not.toBeInTheDocument();
});

test('reports recovery when a previously selected failed source is healthy again', () => {
    render(<SourceDiagnostics sourceName='stores' sources={{stores: {
        status: 'UP', availability: 'AVAILABLE', fresh: true,
    }}} />);
    expect(screen.getByText('Source collection has recovered')).toBeInTheDocument();
    expect(screen.queryByText('No source error details were reported')).not.toBeInTheDocument();
});

test('does not synthesize a selected source without a received snapshot', () => {
    render(<SourceDiagnostics sourceName='stores' sources={{}} />);
    expect(screen.queryByRole('region', {name: 'Source diagnostics'})).not.toBeInTheDocument();
});

test.each([true, false])('describes missing node details at the correct scope (complete=%s)', complete => {
    render(<SourceDiagnostics sourceName='stores' complete={complete} sources={{stores: {
        status: 'UNKNOWN', availability: 'MALFORMED', reason: 'malformed_response',
    }}} />);

    expect(screen.getByText(complete ? /No per-node failure details were reported in this snapshot/
        : /No per-node failure details were reported on this page/)).toBeInTheDocument();
});

test.each([
    ['UP', 'PARTIAL', true],
    ['DOWN', 'UNSUPPORTED', true],
    ['DEGRADED', 'UNSUPPORTED', true],
    ['UP', 'UNSUPPORTED', false],
])(
    'shows unsupported request details when source topology requires attention (%s/%s)',
    (status, availability, showDetails) => {
        render(<SourceDiagnostics sourceName='stores' sources={{stores: {
            status, availability, reason: 'unsupported_version',
        }}} nodes={[{id: 'store-1', type: 'STORE', name: 'Store A', metric_statuses: {
            system: {availability: 'AVAILABLE'},
            raft: {availability: 'UNSUPPORTED', reason: 'unsupported_version',
                diagnostics: {request_path: '/metrics/raft', http_status: 404}},
        }}]} />);

        if (showDetails) {
            expect(screen.getByText(/Store A · raft/)).toBeInTheDocument();
            expect(screen.getByText('/metrics/raft')).toBeInTheDocument();
            expect(screen.getByText('404')).toBeInTheDocument();
            expect(screen.queryByText(/No per-node failure details/)).not.toBeInTheDocument();
        }
        else {
            expect(screen.queryByText('/metrics/raft')).not.toBeInTheDocument();
        }
    }
);

test.each([false, true])('excludes intentionally uncollected PD followers (stale=%s)', stale => {
    render(<SourceDiagnostics sourceName='pd' sources={{pd: {
        status: 'UP', availability: 'PARTIAL', reason: 'upstream_rejected',
    }}} nodes={[
        {id: 'leader', type: 'PD', name: 'PD leader', metric_statuses: {system: {
            availability: 'UNAVAILABLE', reason: 'upstream_rejected',
            diagnostics: {request_path: '/actuator/prometheus', http_status: 503},
        }}},
        {id: 'follower', type: 'PD', name: 'PD follower', metric_statuses: {system: {
            availability: 'UNSUPPORTED', reason: 'metrics_not_collected', stale,
        }}},
    ]} />);
    expect(screen.getByText(/PD leader · system/)).toBeInTheDocument();
    expect(screen.getByText('/actuator/prometheus')).toBeInTheDocument();
    expect(screen.getByText('503')).toBeInTheDocument();
    expect(screen.queryByText(/PD follower/)).not.toBeInTheDocument();
});

test.each(['UNAVAILABLE', 'UNSUPPORTED'])(
    'explains unmapped PD metrics and shows discovered node details (%s)', availability => {
        render(<SourceDiagnostics sourceName='pd' complete sources={{pd: {
            status: 'UP', availability: 'PARTIAL', reason: 'pd_metrics_unmapped',
            observed_at: 2000, last_success_at: 2000, fresh: false, stale: false,
        }}} nodes={[1, 2, 3].map(id => ({id: `pd-${id}`, type: 'PD', name: `PD ${id}`, metric_statuses: {
            system: {availability, reason: 'pd_metrics_unmapped'},
        }}))} />);

        expect(screen.getByText('PD nodes are online; system metrics could not be attributed'))
            .toBeInTheDocument();
        expect(screen.getByText(/PD topology was discovered.*restUrl in the PD member list/)).not.toBeVisible();
        fireEvent.click(screen.getByText('Check addresses and how to fix'));
        expect(screen.getByText(/PD topology was discovered.*restUrl in the PD member list/)).toBeInTheDocument();
        expect(screen.getByText('Last topology success')).toBeInTheDocument();
        expect(screen.getByText('System metrics not collected')).toBeInTheDocument();
        for (const id of [1, 2, 3]) {
            expect(screen.getByText(new RegExp(`PD ${id} · system`))).toBeVisible();
        }
        expect(screen.queryByText(/No per-node failure details/)).not.toBeInTheDocument();
    }
);

test('localizes PD endpoint identity guidance without claiming a discovery failure', () => {
    i18n.changeLanguage('zh-CN');
    render(<SourceDiagnostics sourceName='pd' sources={{pd: {
        status: 'UP', availability: 'PARTIAL', reason: 'pd_metrics_unmapped',
    }}} nodes={[{id: 'pd-1', type: 'PD', name: 'PD 1', metric_statuses: {
        system: {availability: 'UNAVAILABLE', reason: 'pd_metrics_unmapped'},
    }}]} />);

    expect(screen.getByText('PD 节点在线，系统指标尚未匹配到节点')).toBeVisible();
    fireEvent.click(screen.getByText('检查地址与修复方法'));
    expect(screen.getByText(/已发现 PD 拓扑.*restUrl/)).toBeInTheDocument();
    expect(screen.getByText('最近拓扑采集成功')).toBeInTheDocument();
    expect(screen.queryByText(/来源可能在发现节点前/)).not.toBeInTheDocument();
});

test('shows the actual PD endpoint comparison and independent cluster readiness', () => {
    render(<SourceDiagnostics sourceName='pd' sources={{pd: {
        status: 'UP', availability: 'PARTIAL', reason: 'pd_metrics_unmapped',
        cluster_state: 'Cluster_Not_Ready', data_state: 'PState_Normal',
        diagnostics: {pd_metric_mapping: {
            configured_origin: 'http://pd-service:8620', status: 'no_match',
            matched_nodes: 0, discovered_nodes: 3, omitted_members: 1,
            members: [
                {node_id: 'pd-1', origin: 'http://pd-node:8620', endpoint_valid: true,
                    origin_matches: false, base_path_matches: true},
                {node_id: 'pd-2', origin: 'http://pd-service:8620', endpoint_valid: true,
                    origin_matches: true, base_path_matches: false},
            ],
        }},
    }}} nodes={[{id: 'pd-1', name: 'PD leader', type: 'PD'}]} />);

    expect(screen.getByText('No matching node · 0 of 3 discovered nodes match the configured address')).toBeVisible();
    expect(screen.getByText('Cluster readiness: PD still reports Cluster_Not_Ready')).toBeVisible();
    const addressTable = screen.getByText('http://pd-node:8620').closest('table');
    expect(addressTable).not.toBeVisible();
    fireEvent.click(screen.getByText('Check addresses and how to fix'));
    expect(addressTable).toBeVisible();
    expect(screen.getByText('No matching node', {exact: true})).toBeVisible();
    expect(screen.getByText('0 / 3')).toBeInTheDocument();
    expect(screen.getByText('http://pd-node:8620')).toBeInTheDocument();
    expect(screen.getAllByText('http://pd-service:8620')).toHaveLength(2);
    expect(screen.getByText('PD leader')).toBeInTheDocument();
    expect(screen.getAllByText('Differs')).toHaveLength(2);
    expect(screen.getAllByText('Matches')).toHaveLength(2);
    expect(screen.getByText('1 more members are omitted from this comparison')).toBeInTheDocument();
    expect(screen.getByText('Cluster_Not_Ready', {exact: true})).toBeInTheDocument();
    expect(screen.getByText('PState_Normal', {exact: true})).toBeInTheDocument();
    expect(screen.getByText(/PD reports Cluster_Not_Ready.*initial-store-count/)).toBeInTheDocument();
});

test('does not call PD nodes online when topology is degraded', () => {
    render(<SourceDiagnostics sourceName='pd' sources={{pd: {
        status: 'DEGRADED', availability: 'PARTIAL', reason: 'pd_metrics_unmapped',
    }}} />);
    expect(screen.getByText('PD system metrics could not be attributed to a node')).toBeVisible();
    expect(screen.queryByText('PD nodes are online; system metrics could not be attributed'))
        .not.toBeInTheDocument();
});

test.each([false, true])('excludes unavailable topology placeholders from Store failures (stale=%s)', stale => {
    render(<SourceDiagnostics sourceName='stores' sources={{stores: {
        status: 'UP', availability: 'PARTIAL', reason: 'unsupported_version',
    }}} nodes={[
        {id: 'failed', type: 'STORE', name: 'Store failed', metric_statuses: {raft: {
            availability: 'UNSUPPORTED', reason: 'unsupported_version',
            diagnostics: {request_path: '/metrics/raft', http_status: 404},
        }}},
        {id: 'healthy', type: 'STORE', name: 'Store healthy', metric_statuses: {backend: {
            availability: 'UNSUPPORTED', reason: 'topology_fields_unavailable', stale,
        }}},
    ]} />);
    expect(screen.getByText(/Store failed · raft/)).toBeInTheDocument();
    expect(screen.getByText('/metrics/raft')).toBeInTheDocument();
    expect(screen.getByText('404')).toBeInTheDocument();
    expect(screen.queryByText(/Store healthy/)).not.toBeInTheDocument();
});

test('localizes the collection status and response size lower bound in Chinese', () => {
    i18n.changeLanguage('zh-CN');
    render(<SourceDiagnostics sourceName='stores' sources={{stores: {
        status: 'UP', availability: 'PARTIAL', reason: 'response_too_large',
        diagnostics: {response_bytes: 1048577, response_size_exact: false},
    }}} />);
    expect(screen.getByText('部分可用', {exact: true})).toBeInTheDocument();
    expect(screen.getByText('至少 1 MB (1,048,577 B)')).toBeInTheDocument();
});

test('keeps the failed request visible while disclosing supporting collection details on demand', () => {
    i18n.changeLanguage('zh-CN');
    render(<SourceDiagnostics sourceName='stores' sources={{stores: {
        status: 'UP', availability: 'PARTIAL', reason: 'upstream_rejected',
    }}} nodes={[{id: 'store-1', type: 'STORE', name: 'Store 1', metric_statuses: {
        system: {availability: 'UNAVAILABLE', reason: 'upstream_rejected',
            diagnostics: {request_path: '/metrics/system', http_status: 503}},
    }}]} />);
    expect(screen.getAllByText('指标接口返回错误响应').filter(element => {
        return !element.closest('details');
    })).toHaveLength(1);
    expect(screen.getByText('影响 1 个节点 · 指标：系统')).toBeVisible();
    expect(screen.getByText('/metrics/system')).toBeVisible();
    expect(screen.getByText('Store 1 · 系统', {exact: true})).toBeVisible();
    const nodeDetails = screen.getByText(/Store 1 · system/);
    expect(nodeDetails).not.toBeVisible();
    fireEvent.click(screen.getByText('采集详情与修复方法'));
    expect(nodeDetails).toBeVisible();
});

test('shows the current failed-graph count before expanding Server diagnostics', () => {
    render(<SourceDiagnostics sourceName='server' sources={{server: {
        status: 'UP', availability: 'PARTIAL', reason: 'backend_metrics_failed',
    }}} nodes={[{id: 'server-1', type: 'SERVER', name: 'Server 1', metric_statuses: {
        backend: {availability: 'UNAVAILABLE', reason: 'backend_metrics_failed', diagnostics: {failed_graphs: 2}},
    }}]} />);
    expect(screen.getByText('Graphs with failed backend metrics')).toBeVisible();
    expect(screen.getByText('2', {exact: true})).toBeVisible();
    expect(screen.getByText('Server 1 · Backend', {exact: true})).toBeVisible();
    expect(screen.getByText(/Server 1 · backend/)).not.toBeVisible();
});

test('keeps multiple Server failed-graph counts attached to their owning nodes', () => {
    render(<SourceDiagnostics sourceName='server' sources={{server: {
        status: 'UP', availability: 'PARTIAL', reason: 'backend_metrics_failed',
    }}} nodes={[['server-1', 'Server A', 2], ['server-2', 'Server B', 7]].map(([id, name, count]) => ({
        id, name, type: 'SERVER', metric_statuses: {backend: {
            availability: 'UNAVAILABLE', reason: 'backend_metrics_failed', diagnostics: {failed_graphs: count},
        }},
    }))} />);

    expect(screen.getByText('2', {exact: true})).not.toBeVisible();
    expect(screen.getByText('7', {exact: true})).not.toBeVisible();
    fireEvent.click(screen.getByText('Collection details and how to fix'));
    for (const [name, count] of [['Server A', '2'], ['Server B', '7']]) {
        const row = screen.getByText(new RegExp(`${name} · backend`)).closest('li');
        expect(within(row).getByText(count, {exact: true})).toBeVisible();
    }
});

test('keeps multiple Store request paths and HTTP statuses attached to their metric groups', () => {
    render(<SourceDiagnostics sourceName='stores' sources={{stores: {
        status: 'UP', availability: 'PARTIAL', reason: 'store_metrics_partial',
    }}} nodes={[
        {id: 'store-1', name: 'Store A', type: 'STORE', metric_statuses: {system: {
            availability: 'UNAVAILABLE', reason: 'upstream_rejected',
            diagnostics: {request_path: '/metrics/system', http_status: 503},
        }}},
        {id: 'store-2', name: 'Store B', type: 'STORE', metric_statuses: {raft: {
            availability: 'UNAVAILABLE', reason: 'upstream_rejected',
            diagnostics: {request_path: '/metrics/raft', http_status: 502},
        }}},
    ]} />);

    expect(screen.getByText('/metrics/system')).not.toBeVisible();
    expect(screen.getByText('/metrics/raft')).not.toBeVisible();
    fireEvent.click(screen.getByText('Collection details and how to fix'));
    const first = screen.getByText(/Store A · system/).closest('li');
    const second = screen.getByText(/Store B · raft/).closest('li');
    expect(within(first).getByText('/metrics/system')).toBeVisible();
    expect(within(first).getByText('503', {exact: true})).toBeVisible();
    expect(within(first).queryByText('/metrics/raft')).not.toBeInTheDocument();
    expect(within(second).getByText('/metrics/raft')).toBeVisible();
    expect(within(second).getByText('502', {exact: true})).toBeVisible();
    expect(within(second).queryByText('/metrics/system')).not.toBeInTheDocument();
});
