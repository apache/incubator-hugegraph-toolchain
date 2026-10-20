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

/**
 * @file Gremlin query actions
 */

import {useCallback, useContext, useState} from 'react';
import {useTranslation} from 'react-i18next';
import {Button, Tooltip, Popover, message, Space} from 'antd';
import {ClockCircleOutlined, ThunderboltOutlined} from '@ant-design/icons';
import {GREMLIN_EXECUTES_MODE} from '../../../../utils/constants';
import GraphAnalysisContext from '../../../Context';
import {isValidFavoriteName} from '../../../../utils/rules';
import FavoriteNameInput from '../../../../components/FavoriteNameInput';
import c from './index.module.scss';
import * as api from '../../../../api/index';

const FAVORITE_TYPE = {
    Gremlin: 'GREMLIN',
    Algorithms: 'ALGORITHM',
    Cypher: 'CYPHER',
};

const {QUERY, TASK} = GREMLIN_EXECUTES_MODE;

const SecondaryActions = props => {
    const {t} = useTranslation();
    const {
        codeEditorContent,
        setCodeEditorContent,
        activeTab,
        onRefresh,
        isEmptyQuery,
        favoriteCardVisible,
        setFavoriteCardVisible,
        shortcutHint,
        onClear: clearQuery,
        allowFavorite = true,
        isGraphReady = true,
    } = props;
    const context = useContext(GraphAnalysisContext);
    const [favoriteName, setFavoriteName] = useState();
    const [disabledFavorite, setDisabledFavorite] = useState(true);
    const emptyDesc = t('analysis.query.empty_query');

    const onClear = useCallback(
        () => {
            if (!isGraphReady) {
                return;
            }
            if (clearQuery) {
                clearQuery();
            }
            else {
                setCodeEditorContent('');
            }
            setFavoriteCardVisible(false);
        },
        [clearQuery, isGraphReady, setCodeEditorContent, setFavoriteCardVisible]
    );

    const onFavoriteCard = useCallback(
        () => {
            if (!isGraphReady) {
                return;
            }
            setFavoriteCardVisible(true);
            setDisabledFavorite(true);
            setFavoriteName('');
        },
        [isGraphReady, setFavoriteCardVisible]
    );

    const saveFavoriteList = useCallback(
        () => {
            if (!isGraphReady) {
                return;
            }
            const {graphSpace, graph} = context;
            const params = {
                content: codeEditorContent,
                name: favoriteName,
                type: FAVORITE_TYPE[activeTab],
            };
            api.analysis.addFavoriate(graphSpace, graph, params)
                .then(res => {
                    const {status, message: errMsg} = res;
                    if (status === 200) {
                        message.success(t('analysis.query.favorite_success'));
                        onRefresh();
                    }
                    else {
                        !errMsg && message.error(t('analysis.query.favorite_failed'));
                    }
                });
        },
        [activeTab, codeEditorContent, context, favoriteName, isGraphReady, onRefresh, t]
    );

    const onOkFavorite = useCallback(
        () => {
            if (!isGraphReady) {
                return;
            }
            saveFavoriteList();
            setFavoriteCardVisible(false);
        },
        [isGraphReady, saveFavoriteList, setFavoriteCardVisible]
    );

    const onHideFavorite = useCallback(
        () => {
            setFavoriteCardVisible(false);
        },
        [setFavoriteCardVisible]
    );

    const onChangeFavoriteName = useCallback(
        event => {
            const name = event.target.value;
            setFavoriteName(name);
            setDisabledFavorite(!isValidFavoriteName(name));
        },
        []
    );

    const favoriteContent = (
        <>
            <FavoriteNameInput
                placeholder={t('analysis.query.favorite_name_placeholder')}
                value={favoriteName}
                onChange={onChangeFavoriteName}
            />
            <Space style={{marginTop: '24px'}}>
                <Button type='primary' onClick={onOkFavorite} disabled={!isGraphReady || disabledFavorite}>
                    {t('analysis.query.favorite')}
                </Button>
                <Button onClick={onHideFavorite}>{t('common.action.cancel')}</Button>
            </Space>
        </>
    );

    return (
        <div className={c.secondaryActions}>
            <Button className={c.btn} onClick={onClear} disabled={!isGraphReady} size='small'>
                {t('common.action.clear')}
            </Button>
            {allowFavorite && (
                <Popover classNames={{root: c.favoriteModel}}
                    placement='bottom'
                    trigger='click'

                    title={t('analysis.query.favorite_statement')}
                    content={favoriteContent}
                    open={favoriteCardVisible}
                >
                    <Tooltip placement='bottom' title={isEmptyQuery ? emptyDesc : ''}>
                        <Button
                            className={c.btn}
                            disabled={!isGraphReady || isEmptyQuery}
                            onClick={onFavoriteCard}
                            size='small'
                        >
                            {t('analysis.query.favorite')}
                        </Button>
                    </Tooltip>
                </Popover>
            )}
            {shortcutHint && (
                <span className={c.shortcutHint}>{shortcutHint}</span>
            )}
        </div>
    );
};

const PrimaryActions = props => {
    const {t} = useTranslation();
    const {
        executeMode,
        onExecuteModeChange,
        activeTab,
        onExecute,
        isEmptyQuery,
        isExecuting,
        isGraphReady = true,
        allowAsync = true,
    } = props;
    const isQueryMode = !allowAsync || executeMode === QUERY;
    const emptyDesc = t('analysis.query.empty_query');
    const executionHint = t(isQueryMode ? 'analysis.query.execute_shortcut' : 'analysis.query.submit_task_shortcut');

    const onSwitchExecuteMode = useCallback(
        () => onExecuteModeChange(isQueryMode ? TASK : QUERY),
        [isQueryMode, onExecuteModeChange]
    );

    const onExecution = useCallback(
        () => {
            if (isGraphReady && !isEmptyQuery && !isExecuting) {
                onExecute(activeTab);
            }
        },
        [activeTab, isEmptyQuery, isExecuting, isGraphReady, onExecute]
    );

    return (
        <div className={c.primaryActions}>
            <div className={c.executionControl}>
                {allowAsync && (
                    <Tooltip
                        placement='top'
                        title={t(isQueryMode
                            ? 'analysis.query.switch_async_task'
                            : 'analysis.query.switch_immediate_query')}
                    >
                        <Button
                            className={c.modeButton}
                            icon={isQueryMode
                                ? <ThunderboltOutlined />
                                : <ClockCircleOutlined />}
                            onClick={onSwitchExecuteMode}
                            aria-label={t(isQueryMode
                                ? 'analysis.query.switch_async_task'
                                : 'analysis.query.switch_immediate_query')}
                        >
                            {t(isQueryMode
                                ? 'analysis.query.execute_mode_immediate'
                                : 'analysis.query.execute_mode_async')}
                        </Button>
                    </Tooltip>
                )}
                <Tooltip
                    placement='top'
                    title={isEmptyQuery
                        ? emptyDesc
                        : executionHint}
                >
                    <Button
                        className={c.executeButton}
                        type='primary'
                        disabled={!isGraphReady || isEmptyQuery || isExecuting}
                        onClick={onExecution}
                        title={executionHint}
                    >
                        {isQueryMode
                            ? t('analysis.query.execute_query')
                            : t('analysis.query.execute_task')}
                    </Button>
                </Tooltip>
            </div>
        </div>
    );
};

export {PrimaryActions, SecondaryActions};

const ContentCommon = props => (
    <div className={c.actionBar}>
        <SecondaryActions {...props} />
        <PrimaryActions {...props} />
    </div>
);

export default ContentCommon;
