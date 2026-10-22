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
