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
 * @file Gremlin语法分析 Header
 * @author huangqiuyu
 */

import React, {useCallback, useState, useContext} from 'react';
import {Button, Menu, Tooltip, Dropdown, Input, Popover, message, Space} from 'antd';
import {UpOutlined, DownOutlined, QuestionCircleOutlined} from '@ant-design/icons';
import {GREMLIN_EXECUTES_MODE} from '../../../../utils/constants';
import GraphAnalysisContext from '../../../Context';
import classnames from 'classnames';
import c from './index.module.scss';
import * as api from '../../../../api/index';

const FAVORITE_TYPE  = {
    Gremlin: 'GREMLIN',
    Algorithms: 'ALGORITHM',
    Cypher: 'CYPHER',
};

const {QUERY, TASK} = GREMLIN_EXECUTES_MODE;

const ContentCommon = props => {
    const {
        codeEditorContent,
        setCodeEditorContent,
        executeMode,
        onExecuteModeChange,
        activeTab,
        onExecute,
        onRefresh,
        isEmptyQuery,
        favoriteCardVisible,
        setFavoriteCardVisible,
    } = props;

    const context = useContext(GraphAnalysisContext);
    const isQueryMode = executeMode === QUERY;

    const [isShowMore, setShowMore] = useState(true);
    const [favoriteName, setFavoriteName] = useState();
    const [disabledFavorite, setDisabledFavorite]  = useState(true);

    const queryDesc = '查询模式适合30秒内可返回结果的小规模分析，任务模式适合较长时间返回结果的大规模分析，任务详情可在任务管理中查看';
    const emptyDesc = '查询语句不能为空';

    const onToggleCollapse = useCallback(
        () => {
            setShowMore(prev => !prev);
        },
        []
    );

    const renderCollapseHeader = () => {
        const icon = isShowMore ? <UpOutlined /> : <DownOutlined />;
        const iconName = isShowMore ? <span>收起</span> : <span>展开</span>;
        return (
            <div>{icon}{iconName}</div>
        );
    };

    const onClear = useCallback(
        () => {
            setCodeEditorContent('');
        },
        [setCodeEditorContent]
    );

    const onFavoriteCard = useCallback(
        () => {
            setFavoriteCardVisible(true);
            setDisabledFavorite(true);
            setFavoriteName('');
        },
        [setFavoriteCardVisible]
    );

    const saveFavoriteList = useCallback(
        () => {
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
                        message.success('收藏成功');
                        onRefresh();
                    }
                    else {
                        !errMsg && message.error('收藏失败');
                    }
                });
        },
        [activeTab, codeEditorContent, context, favoriteName, onRefresh]
    );

    const onOkFavorite = useCallback(
        () => {
            saveFavoriteList();
            setFavoriteCardVisible(false);
        },
        [saveFavoriteList, setFavoriteCardVisible]
    );

    const onHideFavorite = useCallback(
        () => {
            setFavoriteCardVisible(false);
        },
        [setFavoriteCardVisible]
    );

    const onChangeFavoraiteName = useCallback(
        e => {
            const favoriteName = e.target.value;
            setFavoriteName(favoriteName);
            favoriteName ? setDisabledFavorite(false) : setDisabledFavorite(true);
        },
        []
    );

    const favoriteContent = (
        <>
            <Input
                placeholder="请输入收藏名称"
                showCount
                maxLength={48}
                value={favoriteName}
                onChange={onChangeFavoraiteName}
            />
            <Space style={{marginTop: '24px'}}>
                <Button type='primary' onClick={onOkFavorite} disabled={disabledFavorite}>收藏</Button>
                <Button onClick={onHideFavorite}>取消</Button>
            </Space>
        </>
    );

    const tabClassName = classnames(
        c.tabContent,
        {[c.tabContentCollpased]: !isShowMore}
    );

    const onSwitchExecuteMenu = useCallback(
        e => {
            if (e.key === QUERY) {
                onExecuteModeChange(QUERY);
            }
            else {
                onExecuteModeChange(TASK);
            }
        },
        [onExecuteModeChange]
    );

    const onExecution = useCallback(
        () => {
            onExecute(activeTab);
        },
        [activeTab, onExecute]
    );

    const executeMenu = (
        <Menu
            onClick={onSwitchExecuteMenu}
            items={[
                {label: '执行查询', key: QUERY},
                {label: '执行任务', key: TASK},
            ]}
        />
    );

    return (
        <div className={tabClassName}>
            <div className={c.leftHeader}>
                {props.children}
                <div className={c.btnGroup}>
                    <Tooltip placement="bottom" title={isEmptyQuery ? emptyDesc : ''}>
                        <Dropdown.Button
                            overlay={executeMenu}
                            disabled={isEmptyQuery}
                            onClick={onExecution}
                            size='small'
                        >
                            {isQueryMode ? '执行查询' : '执行任务'}
                        </Dropdown.Button>
                    </Tooltip>
                    <Tooltip placement="bottom" title={queryDesc} className={c.questionCircleIcon}>
                        <QuestionCircleOutlined />
                    </Tooltip>
                    <Popover
                        placement="bottom"
                        trigger='click'
                        overlayClassName={c.favoriteModel}
                        title={'收藏语句'}
                        content={favoriteContent}
                        open={favoriteCardVisible}
                    >
                        <Tooltip placement="bottom" title={isEmptyQuery ? emptyDesc : ''}>
                            <Button
                                className={c.btn}
                                disabled={isEmptyQuery}
                                onClick={onFavoriteCard}
                                size='small'
                            >
                                收藏
                            </Button>
                        </Tooltip>
                    </Popover>
                    <Button className={c.btn} onClick={onClear} size='small'>清空</Button>
                </div>
            </div>
            <div className={c.showMoreButton} onClick={onToggleCollapse}>
                {renderCollapseHeader()}
            </div>
        </div>
    );
};

export default ContentCommon;
