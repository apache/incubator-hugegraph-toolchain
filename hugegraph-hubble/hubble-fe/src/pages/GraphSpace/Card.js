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

import {InfoCircleOutlined, InfoOutlined, EllipsisOutlined} from '@ant-design/icons';
import {Typography, Button, Dropdown, Row, Col, Space, Progress, Card, Tooltip} from 'antd';
import {Link, useNavigate} from 'react-router-dom';
import dayjs from 'dayjs';
import style from './index.module.scss';
import {useCallback} from 'react';
import {useTranslation} from 'react-i18next';
import {getResourceDisplayName} from '../../utils/displayName';
import {isKnownCount} from '../../utils/graphStatistics';
import {formatResourceLimit, formatStorageUsed, formatStorageLimit,
    storageUsagePercent, formatComputeLimit} from './storageUsage';

const formatPercent = percent => {
    if (percent >= 100) {
        return <InfoOutlined />;
    }
    if (percent > 0 && percent < 0.01) {
        return '<0.01%';
    }
    const rounded = Number(percent.toFixed(2));
    return rounded === 100 ? '<100%' : `${rounded}%`;
};

const formatUnlimitedPercent = () => '--';

const TitleField = ({item, onKeyDown}) => {
    const {t} = useTranslation();
    const displayName = item.name === 'DEFAULT'
        ? t('graphspace.default_name')
        : getResourceDisplayName(item.name, item.nickname);

    return (
        <div className={style.card_title}>
            <Typography.Text
                className={style.card_title_text}
                ellipsis={{tooltip: displayName}}
                title={`${displayName}`}
                aria-label={displayName}
                onKeyDown={onKeyDown}
                role='button'
                tabIndex={0}
            >{displayName}
            </Typography.Text>
            <div className={style.subtitle}>
                {item.default && (
                    <span className={style.default}>{t('graphspace.card.set_default')}</span>
                )}
                {item.create_time ? dayjs(item.create_time).format('YYYY-MM-DD') : '--'} {t('graphspace.card.created')}
            </div>
        </div>
    );
};

const GraphSpaceCard = ({
    item,
    editGraphspace,
    deleteGraphspace,
    handleInit,
    canUpdate = true,
    canDelete = true,
    canInit = true,
}) => {
    const navigate = useNavigate();
    const {t} = useTranslation();
    const storagePercent = storageUsagePercent(item);

    const handleGotoGraph = useCallback(() => {
        navigate(`/graphspace/${item.name}`);
    }, [item, navigate]);

    const handleCardClick = useCallback(event => {
        // Dropdown menus use a portal, but their clicks still bubble through
        // the card in React. Keep those actions separate from navigation.
        if (event.target.closest('a, button, [role="menu"], [role="menuitem"]')) {
            return;
        }
        handleGotoGraph();
    }, [handleGotoGraph]);

    const handleEdit = useCallback(() => {
        editGraphspace(item);
    }, [item, editGraphspace]);

    const handleDelete = useCallback(() => {
        deleteGraphspace(item.name);
    }, [deleteGraphspace, item]);

    const handleGotoGraphKeyDown = useCallback(event => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            handleGotoGraph();
        }
    }, [handleGotoGraph]);

    const getMenu = item => {
        const schema = {
            key: 'schema',
            label: (
                <Link to={`/graphspace/${item.name}/schema`}>
                    {t('common.action.schema_manage')}
                </Link>
            ),
        };
        if (item.name === 'neizhianli') {
            return {
                items: [schema, ...(canInit ? [{
                    key: 'init',
                    label: t('common.action.init'),
                    onClick: handleInit,
                }] : [])],
            };
        }
        return {
            items: [
                schema,
                ...(canUpdate ? [{
                    key: 'edit',
                    label: t('common.action.edit'),
                    onClick: handleEdit,
                }] : []),
                ...(canDelete ? [{
                    key: 'delete',
                    disabled: item.default,
                    label: (item.default)
                        ? <span className={style.disable}>{t('common.action.delete')}</span>
                        : t('common.action.delete'),
                    onClick: item.default ? undefined : handleDelete,
                }] : []),
            ],
        };
    };

    const unlimited = t('graphspace.unit.unlimited');
    const cpu = formatResourceLimit(item.cpu_limit, t('graphspace.unit.cpu'), unlimited);
    const memory = formatResourceLimit(item.memory_limit, t('graphspace.unit.memory'), unlimited);
    const storage = formatStorageLimit(item.storage_limit, unlimited);
    const statisticDate = item.statistic?.date;
    const hasStatistics = isKnownCount(item.statistic?.vertex) && isKnownCount(item.statistic?.edge);

    return (
        <Card styles={{header: {
                backgroundImage: 'linear-gradient(180deg, '
                    + 'rgba(51,136,255,0.10) 0%, rgba(51,136,255,0.00) 100%)',
                borderBottom: 0,
                minHeight: 93,
                paddingLeft: 20,
            }, body: {
                padding: '0 24px 24px 20px',
            }}}
            className={style.card}
            onClick={handleCardClick}
            title={(
                <TitleField
                    item={item}
                    onKeyDown={handleGotoGraphKeyDown}
                />
            )}


            extra={(
                <Space.Compact>
                    <Button onClick={handleGotoGraph}>{t('graphspace.card.enter')}</Button>
                    <Dropdown menu={getMenu(item)} trigger={['click']}>
                        <Button icon={<EllipsisOutlined />} aria-label={t('common.action.more')} />
                    </Dropdown>
                </Space.Compact>
            )}
            actions={hasStatistics ? [
                    <Space key="1" size={8} wrap>
                        <span>{t('graphspace.card.vertex')}: {item.statistic.vertex}</span>
                        <span>{t('graphspace.card.edge')}: {item.statistic.edge}</span>
                        <Typography.Text type='secondary'>
                            {statisticDate
                                ? t('graphspace.card.daily_snapshot', {date: statisticDate})
                                : t('graphspace.card.daily_update')}
                        </Typography.Text>
                        <Tooltip title={(
                            <>
                                <div>{t('graphspace.card.daily_update')}</div>
                                {statisticDate && (
                                    <div>{t('graphspace.card.last_update', {
                                        date: statisticDate,
                                    })}
                                    </div>
                                )}
                            </>
                        )}
                            trigger={['hover', 'focus', 'click']}
                        >
                            <Button
                                type='text'
                                size='small'
                                aria-label={t('graphspace.card.statistic_help')}
                                icon={<InfoCircleOutlined />}
                            />
                        </Tooltip>
                    </Space>,
                ] : undefined}
        >
            <div
                className={style.card_content}
                onKeyDown={handleGotoGraphKeyDown}
                role='button'
                tabIndex={0}
            >
                <Row justify='space-between'>
                    <Col span={14}>
                        <ul className={style.list}>
                            <li>{t('graphspace.card.graph_id', {name: item.name})}</li>
                            <li>
                                {t('graphspace.card.auth_label')}
                                {t(item.auth ? 'graphspace.auth_yes' : 'graphspace.auth_no')}
                            </li>
                            <li>
                                {t('graphspace.card.max_graph')}
                                {formatResourceLimit(item.max_graph_number, '', t('graphspace.unit.unlimited'))}
                            </li>
                            <li>{t('graphspace.card.cpu', {val: cpu})}</li>
                            <li>{t('graphspace.card.memory', {val: memory})}</li>
                            <li>{t('graphspace.card.compute')}{' '}
                                {formatComputeLimit(item.compute_cpu_limit, item.cpu_limit,
                                    t('graphspace.unit.cpu'), t)}
                                {' / '}
                                {formatComputeLimit(item.compute_memory_limit, item.memory_limit,
                                    t('graphspace.unit.memory'), t)}
                            </li>
                            <li>{t('graphspace.card.storage')}
                                {storage}
                            </li>
                        </ul>
                    </Col>
                    <Col span={8}>
                        <Space orientation='vertical'>
                            <Progress
                                type='circle'
                                width={96}
                                percent={storagePercent ?? 0}
                                format={storagePercent === null ? formatUnlimitedPercent : formatPercent}
                                status={storagePercent >= 100
                                    ? 'exception' : 'normal'}
                            />
                            <div
                                style={{textAlign: 'center', fontSize: '12px'}}
                            >
                                <div>{t('graphspace.card.used')}</div>
                                <div>
                                    {formatStorageUsed(item)}
                                    {' / '}
                                    {formatStorageLimit(
                                        item.storage_limit,
                                        t('graphspace.unit.unlimited'),
                                        '--'
                                    )}
                                </div>
                            </div>
                        </Space>
                    </Col>
                </Row>
            </div>
        </Card>
    );
};

export default GraphSpaceCard;
