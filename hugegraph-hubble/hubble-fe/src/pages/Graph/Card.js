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

import {useCallback, useEffect, useMemo, useState} from 'react';
import {Button, Card, Dropdown, Space, Typography, Tooltip} from 'antd';
import {UnorderedListOutlined} from '@ant-design/icons';
import {Link, useNavigate} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import {GraphCanvas} from '../../visualization/react';
import dayjs from 'dayjs';
import _ from 'lodash';
import {adaptHubbleSchema} from '../../visualization/hubble/adapter';
import {isPdEnabled} from '../../utils/config';
import style from './index.module.scss';
import {byteConvert} from '../../utils/format';
import {getResourceDisplayName} from '../../utils/displayName';
import * as api from '../../api';
import {formatRecordedCount} from '../../utils/graphStatistics';

const EMPTY_SCHEMA = {vertices: [], edges: []};
const CARD_LAYOUT = {type: 'dagre', options: {rankdir: 'LR', nodesep: 30, ranksep: 60}};

const TitleField = ({item, onClick, onKeyDown, schemaTypeCounts}) => {
    const {t} = useTranslation();
    const pdMode = isPdEnabled();
    const graphName = getResourceDisplayName(item.name, item.nickname);
    const graphspaceName = item.graphspace === 'DEFAULT'
        ? t('graphspace.default_name')
        : getResourceDisplayName(item.graphspace, item.graphspace_nickname);
    const displayName = pdMode
        ? `${_.truncate(graphspaceName, {length: 12})}-${_.truncate(graphName, {length: 12})}`
        : graphName;
    const fullTitle = pdMode
        ? `${graphspaceName}-${graphName}`
        : graphName;
    const createdAt = item.create_time
        ? dayjs(item.create_time).format('YYYY-MM-DD')
        : '--';

    return (
        <>
            <Typography.Text
                style={{maxWidth: 244}}
                ellipsis={{ellipsis: true}}
                title={fullTitle}
                onClick={onClick}
                onKeyDown={onKeyDown}
                role='button'
                tabIndex={0}
            >
                {displayName}
            </Typography.Text>
            {item.default && <span className={style.default}>{t('common.label.default')}</span>}
            <div className={style.subtitle}>
                {t('graph.card.storage')}: {item.storage >= 0 ? byteConvert(item.storage) : '--'}
                {' / '}
                {t('graph.col.create_time')}: {createdAt}
                {' / '}
                {schemaTypeCounts ? t('graph.card.schema_types', schemaTypeCounts) : t('graph.card.schema_info')}
            </div>
        </>
    );
};

const GraphCard = ({item, menus, canMutate = true}) => {
    const {t} = useTranslation();
    const navigate = useNavigate();
    const graphName = getResourceDisplayName(item.name, item.nickname);
    const graphspace = item.graphspace || 'DEFAULT';
    const key = `${graphspace}/${item.name}`;
    const [schema, setSchema] = useState(null);
    const [snapshot, setSnapshot] = useState(null);
    const schemaView = item.schemaview || (schema?.key === key ? schema.data : null);
    const statistic = item.statistic?.vertex != null && item.statistic?.edge != null
        ? item.statistic : snapshot?.key === key ? snapshot.data : null;
    useEffect(() => {
        let active = true;
        if (!item.schemaview) {
            api.manage.getGraphView(graphspace, item.name, {suppressBusinessErrorToast: true}).then(res => {
                if (active) setSchema({key, data: res.status === 200 ? res.data : null, loaded: true});
            }).catch(() => {
                if (active) setSchema({key, data: null, loaded: true});
            });
        }
        if (item.statistic?.vertex == null || item.statistic?.edge == null) {
            api.analysis.getGraphData(graphspace, item.name, {suppressBusinessErrorToast: true}).then(res => {
                if (active) setSnapshot({key, data: res.status === 200 ? {
                    ...res.data, vertex: res.data?.vertexcount, edge: res.data?.edgecount,
                } : null, error: res.status !== 200});
            }).catch(() => {
                if (active) setSnapshot({key, data: null, error: true});
            });
        }
        return () => { active = false; };
    }, [graphspace, item.name, item.schemaview, item.statistic, key]);
    const prepared = useMemo(() => {
        try {
            return {data: adaptHubbleSchema(schemaView || EMPTY_SCHEMA), error: null};
        }
        catch (error) {
            return {data: {nodes: [], edges: []}, error};
        }
    }, [schemaView]);
    const schemaIsEmpty = !prepared.error && prepared.data.nodes.length === 0 && prepared.data.edges.length === 0;
    const hasElementCounts = statistic?.vertex != null && statistic?.edge != null
        && Number.isFinite(Number(statistic.vertex)) && Number.isFinite(Number(statistic.edge));
    const overviewPath = `/graphspace/${graphspace}/graph/${item.name}/detail`;
    const schemaPath = `/graphspace/${graphspace}/graph/${item.name}/meta`;

    const handleGotoAnalysis = useCallback(() => {
        navigate(`/gremlin/${item.graphspace || 'DEFAULT'}/${item.name}`);
    }, [item, navigate]);

    const handleGotoSchema = useCallback(() => {
        navigate(schemaPath);
    }, [navigate, schemaPath]);

    const handleSchemaKeyDown = useCallback(event => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            handleGotoSchema();
        }
    }, [handleGotoSchema]);

    return (
        <Card styles={{header: {
                paddingLeft: 20,
            }}}
            className={style.card}
            title={(
                <TitleField
                    item={item}
                    onClick={handleGotoSchema}
                    onKeyDown={handleSchemaKeyDown}
                    schemaTypeCounts={schemaView ? {
                        vertices: schemaView.vertices?.length || 0,
                        edges: schemaView.edges?.length || 0,
                    } : null}
                />
            )}

            extra={(
                <Dropdown
                    menu={{items: menus}}
                    trigger={['click']}
                >
                    <Button
                        type='text'
                        icon={<UnorderedListOutlined />}
                        aria-label={t('graph.card.more_actions', {graph: graphName})}
                    />
                </Dropdown>
            )}
            actions={[
                hasElementCounts ? (
                    <Tooltip key='counts' title={t(statistic.scan_limit > 0
                        ? 'graph.detail.bounded_statistics_hint' : 'graph.card.counts_tooltip',
                        {limit: statistic.scan_limit})}>
                        <span className={style.element_counts}>
                            {t('graph.card.element_counts', {
                                vertices: formatRecordedCount(statistic.vertex, statistic),
                                edges: formatRecordedCount(statistic.edge, statistic),
                            })}
                        </span>
                    </Tooltip>
                ) : <span key='counts'>{t(snapshot?.key === key
                    ? snapshot.error ? 'graph.detail.statistics_unavailable' : 'graph.detail.not_collected'
                    : 'graph.detail.loading')}</span>,
                <Link key='overview' to={overviewPath}>{t('graph.card.overview')}</Link>,
                <Link key='query' to={`/gremlin/${graphspace}/${item.name}`}>
                    {t('graph.card.query_graph')}
                </Link>,
            ].filter(Boolean)}
        >
            {!schemaView ? (
                <div className={style.empty_schema}>
                    <span>{t(schema?.key === key && schema.loaded
                        ? 'graph.card.schema_unavailable' : 'graph.card.schema_loading')}</span>
                    <Button size='small' onClick={handleGotoSchema}>{t('graph.menu.view_schema')}</Button>
                </div>
            ) : prepared.error ? (
                <div role='alert'>{t('schema.image_view.load_failed')}</div>
            ) : schemaIsEmpty ? (
                <div className={style.empty_schema}>
                    <strong>{t('graph.card.empty_schema')}</strong>
                    <span>{t('graph.card.empty_schema_help')}</span>
                    <Space>
                        <Button
                            type={canMutate ? 'primary' : 'default'}
                            size='small'
                            onClick={handleGotoSchema}
                        >
                            {canMutate
                                ? t('graph.card.open_schema')
                                : t('graph.menu.view_schema')}
                        </Button>
                        <Button size='small' onClick={handleGotoAnalysis}>
                            {t('graph.card.query_graph')}
                        </Button>
                    </Space>
                </div>
            ) : (
                <div
                    className={style.card_content}
                    onClick={handleGotoSchema}
                    onKeyDown={handleSchemaKeyDown}
                    role='button'
                    tabIndex={0}
                    aria-label={t('graph.card.view_schema', {graph: graphName})}
                >
                    <GraphCanvas
                        key={JSON.stringify([graphspace, item.name])}
                        data={prepared.data}
                        preset='thumbnail'
                        layout={CARD_LAYOUT}
                        style={{height: 153}}
                        aria-label={t('graph.card.view_schema', {graph: graphName})}
                    />
                </div>
            )}
        </Card>
    );
};

export default GraphCard;
