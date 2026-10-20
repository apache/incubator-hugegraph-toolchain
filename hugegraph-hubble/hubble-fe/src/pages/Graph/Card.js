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

import {useCallback, useMemo} from 'react';
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
                {t('graph.card.schema_types', schemaTypeCounts)}
            </div>
        </>
    );
};

const GraphCard = ({item, menus, canMutate = true}) => {
    const {t} = useTranslation();
    const navigate = useNavigate();
    const graphName = getResourceDisplayName(item.name, item.nickname);
    const schemaView = item.schemaview || EMPTY_SCHEMA;
    const prepared = useMemo(() => {
        try {
            return {data: adaptHubbleSchema(schemaView), error: null};
        }
        catch (error) {
            return {data: {nodes: [], edges: []}, error};
        }
    }, [schemaView]);
    const schemaIsEmpty = !prepared.error && prepared.data.nodes.length === 0 && prepared.data.edges.length === 0;
    const hasElementCounts = item.statistic
        && item.statistic.vertex !== null
        && item.statistic.vertex !== undefined
        && item.statistic.edge !== null
        && item.statistic.edge !== undefined
        && Number.isFinite(Number(item.statistic.vertex))
        && Number.isFinite(Number(item.statistic.edge));
    const graphspace = item.graphspace || 'DEFAULT';
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
                    schemaTypeCounts={{
                        vertices: schemaView.vertices?.length || 0,
                        edges: schemaView.edges?.length || 0,
                    }}
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
                hasElementCounts && (
                    <Tooltip key='counts' title={t('graph.card.counts_tooltip')}>
                        <span className={style.element_counts}>
                            {t('graph.card.element_counts', {
                                vertices: item.statistic.vertex,
                                edges: item.statistic.edge,
                            })}
                        </span>
                    </Tooltip>
                ),
                <Link key='overview' to={overviewPath}>{t('graph.card.overview')}</Link>,
                <Link key='query' to={`/gremlin/${graphspace}/${item.name}`}>
                    {t('graph.card.query_graph')}
                </Link>,
            ].filter(Boolean)}
        >
            {prepared.error ? (
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
