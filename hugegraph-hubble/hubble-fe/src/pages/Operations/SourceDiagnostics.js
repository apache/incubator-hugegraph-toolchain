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

import {Alert, Descriptions} from 'antd';
import {useEffect, useRef} from 'react';
import {useTranslation} from 'react-i18next';
import {displayNodeType, formatReason, HealthStatus} from './components';
import {formatBytes, formatObservedAt, selectAttentionSources} from './topology';
import {sourceHealthStatus} from './sourceHealth';

const SOURCE_TYPES = {server: 'SERVER', pd: 'PD', stores: 'STORE'};
const DIAGNOSTICS_ID = 'operations-source-diagnostics';

const DiagnosticRequest = ({diagnostics = {}}) => {
    const {t, i18n} = useTranslation();
    const bytes = value => {
        const formatted = formatBytes(value);
        return formatted ? `${formatted} (${new Intl.NumberFormat(i18n.language).format(value)} B)` : null;
    };
    const responseBytes = bytes(diagnostics.response_bytes);
    const limit = bytes(diagnostics.max_response_bytes);
    if (!diagnostics.request_path && !diagnostics.http_status && !responseBytes && !limit) {
        return null;
    }
    return (
        <Descriptions size='small' column={{xs: 1, sm: 2, lg: 3}} items={[
            diagnostics.request_path && {
                key: 'request', label: t('operations.diagnostic_request'),
                children: <code>{diagnostics.request_path}</code>,
            },
            diagnostics.http_status && {
                key: 'http', label: t('operations.diagnostic_http_status'), children: diagnostics.http_status,
            },
            responseBytes && {
                key: 'size', label: t('operations.diagnostic_response_size'),
                children: diagnostics.response_size_exact === false
                    ? t('operations.diagnostic_size_at_least', {size: responseBytes}) : responseBytes,
            },
            limit && {key: 'limit', label: t('operations.diagnostic_response_limit'), children: limit},
        ].filter(Boolean)} />
    );
};

const SourceDiagnostics = ({sources = {}, sourceName, nodes = [], focus = false, complete = false}) => {
    const {t, i18n} = useTranslation();
    const panel = useRef(null);
    const selected = sourceName && SOURCE_TYPES[sourceName]
        ? (sources[sourceName] ? [{name: sourceName, ...sources[sourceName]}] : [])
        : selectAttentionSources(sources);
    useEffect(() => {
        if (focus && panel.current) {
            panel.current.focus({preventScroll: true});
            panel.current.scrollIntoView?.({block: 'start'});
        }
    }, [focus, sourceName, sources]);
    if (!selected.length) {
        return null;
    }
    const time = value => formatObservedAt(value, i18n.language, t('operations.unavailable'));
    return (
        <section id={DIAGNOSTICS_ID} ref={panel} tabIndex={-1}
            className='operations-surface operations-source-diagnostics'
            aria-label={t('operations.source_diagnostics')}>
            <h3>{t('operations.source_diagnostics')}</h3>
            <p>{t('operations.source_diagnostics_help')}</p>
            {selected.map(source => {
                const type = SOURCE_TYPES[source.name];
                const recovered = sourceHealthStatus(source) === 'UP';
                const failures = nodes.filter(node => node.type === type).flatMap(node => (
                    Object.entries(node.metric_statuses ?? {}).filter(([, status]) => (
                        ['UNAVAILABLE', 'MALFORMED'].includes(status.availability)
                            || status.stale && status.availability !== 'UNSUPPORTED'
                            || status.availability === 'UNSUPPORTED'
                                && status.reason === 'unsupported_version'
                                && (source.availability === 'PARTIAL'
                                    || ['DOWN', 'DEGRADED'].includes(source.status))
                    )).map(([group, status]) => ({node, group, status}))
                ));
                return (
                    <div key={source.name}>
                        <h4>{displayNodeType(type)} <HealthStatus status={sourceHealthStatus(source)} /></h4>
                        <Alert showIcon type={recovered ? 'success' : 'warning'}
                            title={recovered ? t('operations.diagnostic_recovered')
                                : formatReason(source.reason, t) || t('operations.diagnostic_reason_unavailable')}
                            description={t(recovered ? 'operations.diagnostic_recovered_help'
                                : source.reason?.includes('response_too_large')
                                    ? 'operations.diagnostic_large_response_action'
                                    : 'operations.diagnostic_check_source_action')} />
                        <DiagnosticRequest diagnostics={source.diagnostics} />
                        {source.diagnostics?.affected_node_count > 0 && (
                            <p>{t('operations.diagnostic_affected_nodes')}: {source.diagnostics.affected_node_count}
                                {' · '}{t('operations.diagnostic_failed_groups')}: {
                                    source.diagnostics.failed_metric_groups?.join(', ')
                                        || t('operations.unavailable')}
                            </p>
                        )}
                        <Descriptions size='small' column={{xs: 1, sm: 2, lg: 3}} items={[
                            {key: 'health', label: t('operations.source_topology_status'),
                                children: source.status ?? t('operations.status_unknown')},
                            {key: 'collection', label: t('operations.source_collection_status'),
                                children: t(`operations.availability_${(source.availability ?? 'UNKNOWN').toLowerCase()}`,
                                    {defaultValue: source.availability ?? t('operations.unavailable')})},
                            {key: 'observed', label: t('operations.last_observed'), children: time(source.observed_at)},
                            {key: 'success', label: t('operations.last_success'), children: time(source.last_success_at)},
                            {key: 'fresh', label: t('operations.diagnostic_freshness'),
                                children: t(source.stale ? 'operations.stale' : source.fresh
                                    ? 'operations.diagnostic_fresh' : 'operations.unavailable')},
                        ]} />
                        {failures.length > 0 ? (
                            <div>
                                <strong>{t(complete ? 'operations.diagnostic_affected_metrics_snapshot'
                                    : 'operations.diagnostic_affected_metrics')}</strong>
                                <ul>{failures.map(({node, group, status}) => (
                                    <li key={`${node.id}-${group}`}>
                                        {node.name} · {group}: {formatReason(status.reason, t)
                                            || t('operations.unavailable')}
                                        <DiagnosticRequest diagnostics={status.diagnostics} />
                                    </li>
                                ))}</ul>
                            </div>
                        ) : !recovered && <p>{t(complete ? 'operations.diagnostic_no_node_details_snapshot'
                            : 'operations.diagnostic_no_node_details')}</p>}
                    </div>
                );
            })}
        </section>
    );
};

export {DIAGNOSTICS_ID, SOURCE_TYPES};
export default SourceDiagnostics;
