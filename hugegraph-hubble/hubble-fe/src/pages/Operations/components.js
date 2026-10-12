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

import {
    CheckCircleFilled,
    ClockCircleFilled,
    CloseCircleFilled,
    CrownOutlined,
    DatabaseOutlined,
    DeploymentUnitOutlined,
    ExclamationCircleFilled,
    QuestionCircleOutlined,
    ReloadOutlined,
    SafetyCertificateOutlined,
} from '@ant-design/icons';
import {Button, Tooltip} from 'antd';
import {Link} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import {
    formatObservedAge,
    formatObservedAt,
    hasStaleMetrics,
    metricIssueReason,
    selectTierNodes,
    storeLeaderCount,
} from './topology';

const STATUS_ICON = {
    UP: CheckCircleFilled,
    DEGRADED: ExclamationCircleFilled,
    DOWN: CloseCircleFilled,
    UNKNOWN: QuestionCircleOutlined,
};

const formatReason = (reason, t) => {
    return reason ? t(`operations.reason_${reason}`, {
        defaultValue: reason.replaceAll('_', ' '),
    }) : null;
};

const NODE_TYPE_LABELS = {
    SERVER: 'Server',
    PD: 'PD',
    STORE: 'Store',
};

const displayNodeType = type => NODE_TYPE_LABELS[type] ?? type ?? '—';

const displayTopologyNodeName = node => {
    if (node?.type !== 'SERVER') {
        return node?.name;
    }
    return node.name?.replace(/^HugeGraph Server\b/, 'Server');
};

const displayHealthStatus = (status, t) => (
    status === 'DEGRADED' ? t('operations.status_degraded') : status
);

const sourceHealthSummary = (source, t) => {
    const status = source.status ?? 'UNKNOWN';
    const availability = source.availability ?? 'UNSUPPORTED';
    if (availability === 'UNSUPPORTED') {
        return t('operations.source_health_not_applicable');
    }
    if (status === 'UP' && availability === 'AVAILABLE') {
        return t('operations.source_health_normal');
    }
    if (status === 'DOWN' || ['UNAVAILABLE', 'MALFORMED'].includes(availability)) {
        return t('operations.source_health_unavailable');
    }
    return t('operations.source_health_partial');
};

const HealthStatus = ({status = 'UNKNOWN', reason, stale = false, size = 'normal'}) => {
    const {t} = useTranslation();
    const normalized = STATUS_ICON[status] ? status : 'UNKNOWN';
    const Icon = STATUS_ICON[normalized];
    const details = [
        normalized === 'UNKNOWN' ? t('operations.status_unknown') : null,
        normalized === 'DEGRADED' ? t('operations.status_degraded_help') : null,
        reason ? formatReason(reason, t) : null,
        stale ? t('operations.stale_help') : null,
    ].filter(Boolean);
    const icon = details.length > 0 ? (
        <Tooltip title={details.join(' · ')}>
            <span
                role='img'
                aria-label={details.join(' · ')}
                className='operations-health-info'
            >
                <Icon aria-hidden='true' />
            </span>
        </Tooltip>
    ) : <Icon aria-hidden='true' />;
    return (
        <span className={`operations-health status-${normalized.toLowerCase()} is-${size}`}>
            {icon}
            <span>
                {displayHealthStatus(normalized, t)}
            </span>
            {stale && (
                <span className='operations-health-stale'>
                    <ClockCircleFilled aria-hidden='true' /> {t('operations.stale')}
                </span>
            )}
            {reason && size === 'large' && (
                <span className='operations-health-reason'>{formatReason(reason, t)}</span>
            )}
        </span>
    );
};

const SourceStrip = ({sources = {}, detailed = false,
    sourceNames = ['server', 'pd', 'stores']}) => {
    const {t, i18n} = useTranslation();
    return (
        <section
            className={`operations-source-strip${detailed ? ' is-detailed' : ''}`}
            aria-label={t('operations.sources')}
        >
            {sourceNames.map(name => {
                const source = sources[name] ?? {};
                const lastObservedAt = source.stale
                    ? source.last_success_at ?? source.observed_at
                    : source.observed_at ?? source.last_success_at;
                const age = lastObservedAt ? formatObservedAge(
                    lastObservedAt,
                    i18n.language,
                    t('operations.unavailable')
                ) : null;
                const observed = lastObservedAt ? formatObservedAt(
                    lastObservedAt,
                    i18n.language,
                    t('operations.unavailable')
                ) : null;
                const availability = t(`operations.availability_${(
                    source.availability ?? 'UNSUPPORTED'
                ).toLowerCase()}`, {
                    defaultValue: source.availability ?? 'UNSUPPORTED',
                });
                const healthSummary = sourceHealthSummary(source, t);
                const sourceDetails = [
                    healthSummary,
                    `${t('operations.source_topology_status')}: ${
                        displayHealthStatus(source.status ?? 'UNKNOWN', t)
                    }`,
                    `${t('operations.source_collection_status')}: ${availability}`,
                    observed
                        ? `${t('operations.last_observed')}: ${observed}` : null,
                    source.stale ? t('operations.stale') : null,
                    source.reason ? formatReason(source.reason, t) : null,
                ].filter(Boolean).join(' · ');
                const sourceLabel = displayNodeType(name === 'stores'
                    ? 'STORE' : name.toUpperCase());
                const sourceSummary = `${sourceLabel} ${
                    displayHealthStatus(source.status ?? 'UNKNOWN', t)
                } · ${sourceDetails}`;
                return (
                    <Tooltip key={name} title={detailed ? null : sourceDetails}>
                        <div
                            className='operations-source'
                            tabIndex={detailed ? undefined : 0}
                            aria-label={detailed ? undefined : sourceSummary}
                        >
                            <strong>{sourceLabel}</strong>
                            <HealthStatus
                                status={source.status}
                                reason={detailed ? source.reason : undefined}
                            />
                            {!detailed && (
                                <span className='operations-source-summary'>
                                    {healthSummary}
                                    {age ? ` · ${age}` : ''}
                                    {source.stale
                                        ? ` · ${t('operations.stale')}` : ''}
                                </span>
                            )}
                            {detailed && (
                                <span className='operations-source-state'>
                                    {healthSummary}
                                    {` · ${t('operations.source_topology_status')}: ${
                                        displayHealthStatus(
                                            source.status ?? 'UNKNOWN', t
                                        )
                                    }`}
                                    {` · ${t('operations.source_collection_status')}: ${
                                        availability
                                    }`}
                                    {observed
                                        ? ` · ${t('operations.last_observed')}: ${
                                            observed
                                        }`
                                        : (age ? ` · ${age}` : '')}
                                    {source.stale ? ` · ${t('operations.stale')}` : ''}
                                    {source.reason
                                        ? ` · ${formatReason(source.reason, t)}` : ''}
                                </span>
                            )}
                        </div>
                    </Tooltip>
                );
            })}
        </section>
    );
};

const TIER_ICONS = {
    SERVER: DeploymentUnitOutlined,
    PD: SafetyCertificateOutlined,
    STORE: DatabaseOutlined,
};

const TierIcon = ({type}) => {
    const Icon = TIER_ICONS[type] ?? DeploymentUnitOutlined;
    return (
        <span className={`operations-node-icon type-${type?.toLowerCase()}`}>
            <Icon aria-label={`${type} icon`} role='img' />
        </span>
    );
};

const RefreshButton = ({loading = false, onClick}) => {
    const {t} = useTranslation();
    const label = t('operations.refresh');
    return (
        <Tooltip title={label}>
            <Button
                className='operations-refresh-button'
                type='text'
                shape='circle'
                icon={<ReloadOutlined />}
                loading={loading}
                onClick={onClick}
                aria-label={label}
            />
        </Tooltip>
    );
};

const nodeRoleLabel = (node, t) => {
    if (node?.role) {
        if (node.role === 'LEADER') {
            return t('operations.leader');
        }
        if (node.role === 'FOLLOWER') {
            return t('operations.follower');
        }
        return node.role;
    }
    const leaders = storeLeaderCount(node);
    return leaders === null ? '—' : t(
        leaders === 1 ? 'operations.leader_shard' : 'operations.leader_shards',
        {count: leaders}
    );
};

const TierNode = ({node, returnState}) => {
    const {t} = useTranslation();
    const name = displayTopologyNodeName(node);
    return (
        <Link
            className={[
                'operations-topology-node',
                `status-${node.status?.toLowerCase()}`,
                node.type === 'PD' && node.role === 'LEADER'
                    ? 'is-axis-node' : '',
            ].filter(Boolean).join(' ')}
            to={`/operations/nodes/${node.id}`}
            state={returnState}
            aria-label={`${node.type} ${name} ${nodeRoleLabel(node, t)} ${
                displayHealthStatus(node.status, t)}`}
        >
            <TierIcon type={node.type} />
            <span className='operations-node-copy'>
                <strong>{name}</strong>
                <span>
                    {node.type === 'PD' && node.role === 'LEADER' && (
                        <CrownOutlined
                            aria-label={t('operations.leader_role')}
                            role='img'
                        />
                    )}
                    {nodeRoleLabel(node, t) === '—'
                        ? (node.version ?? '—') : nodeRoleLabel(node, t)}
                </span>
            </span>
            <HealthStatus
                status={node.status}
                reason={metricIssueReason(node)}
                stale={hasStaleMetrics(node)}
            />
        </Link>
    );
};

const TopologyTier = ({type, nodes, returnState}) => {
    const {t} = useTranslation();
    const tier = selectTierNodes(nodes, type);
    return (
        <section className={`operations-tier tier-${type.toLowerCase()}`}>
            <div className='operations-tier-label'>
                <Link className='operations-tier-title' to={`/operations/nodes?type=${type}`}>
                    {displayNodeType(type)} {t('operations.tier')}
                </Link>
                <span>
                    {t('operations.node_count', {
                        count: nodes.filter(node => node.type === type).length,
                    })}
                </span>
            </div>
            <div className='operations-tier-branch' data-node-count={tier.visible.length}>
                {tier.visible.map((node, index) => (
                    <TierNode
                        key={node.id}
                        node={node}
                        returnState={returnState}
                        index={index}
                    />
                ))}
                {tier.overflow > 0 && (
                    <Link
                        className='operations-topology-overflow'
                        to={`/operations/nodes?type=${type}`}
                        aria-label={t('operations.more_nodes', {count: tier.overflow})}
                    >
                        +{tier.overflow}
                    </Link>
                )}
            </div>
        </section>
    );
};

const ClusterTopology = ({nodes = [], returnState}) => {
    const {t} = useTranslation();
    return (
        <div className='operations-topology' aria-label={t('operations.topology_label')}>
            {['SERVER', 'PD', 'STORE'].map(type => (
                <TopologyTier
                    key={type}
                    type={type}
                    nodes={Array.isArray(nodes) ? nodes : []}
                    returnState={returnState}
                />
            ))}
        </div>
    );
};

export {
    HealthStatus,
    nodeRoleLabel,
    SourceStrip,
    ClusterTopology,
    TierIcon,
    RefreshButton,
    formatReason,
    displayHealthStatus,
    displayNodeType,
};
