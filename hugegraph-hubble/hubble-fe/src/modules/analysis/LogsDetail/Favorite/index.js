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
 * @file gremlin表格 收藏
 * @author
 */

import React, {useState, useCallback} from 'react';
import {Table, Input, Popconfirm, Modal} from 'antd';
import ExecutionContent from '../../../../components/ExecutionContent';
import Highlighter from 'react-highlight-words';
import c from './index.module.scss';

const Favorite = props => {
    const {
        favoriteQueriesDataRecords,
        favoriteQueriesDataTotal,
        isLoading,
        pageFavorite,
        pageSize,
        onFavoritePageChange,
        onChangeSearchValue,
        onSortChange,
        onDel,
        onEditCollection,
        onLoadContent,
    } = props;

    const [favoriteName, setFavoriteName] = useState();
    const [searchCache, setSearchCache] = useState('');
    const [search, setSearch] = useState('');
    const [isDisabledName, setDisabledName]  = useState(false);

    const loadStatements = useCallback(
        (content, index) => {
            onLoadContent(content);
            const headerTabNode = document.getElementById('queryBar');
            window.scrollTo(0, headerTabNode.offsetTop);
        },
        [onLoadContent]
    );

    const changeGremCollection = useCallback(
        rowData => {
            onEditCollection(rowData, favoriteName);
        },
        [favoriteName, onEditCollection]
    );

    const onSaveEditFavorite = useCallback(
        rowData => {
            setFavoriteName('');
            changeGremCollection(rowData);
        },
        [changeGremCollection]
    );

    const onEditFavorite = useCallback(
        rowData => {
            const {name} = rowData;
            setFavoriteName(name);
        },
        []
    );

    const onChangeFavoraiteName = useCallback(
        e => {
            setFavoriteName(e.target.value);
            e.target.value ? setDisabledName(false) : setDisabledName(true);
        },
        []
    );

    const onConfirm = id => {
        Modal.confirm({
            title: '确认删除',
            content: '是否确认删除该条收藏语句？',
            okText: '确定',
            cancelText: '取消',
            onOk: () => onDel(id),
        });
    };

    const editFavoriteForm = (
        <div>
            <div style={{marginBottom: '16px'}}>修改名称</div>
            <Input
                style={{marginBottom: '18px'}}
                placeholder="请输入收藏名称"
                showCount
                maxLength={48}
                value={favoriteName}
                onChange={onChangeFavoraiteName}
            />
        </div>
    );

    const queryFavoriteColumns = [
        {
            title: '时间',
            dataIndex: 'create_time',
            width: '25%',
            sorter: true,
        },
        {
            title: '名称',
            dataIndex: 'name',
            width: '15%',
            sorter: true,
            render: text => {
                return (
                    <Highlighter
                        highlightClassName={c.highlight}
                        searchWords={[search]}
                        autoEscape
                        textToHighlight={text}
                    />
                );
            },
        },
        {
            title: '收藏语句',
            dataIndex: 'content',
            width: '40%',
            render(text, rowData) {
                return text.split('\n')[1] ? (
                    <ExecutionContent
                        content={text}
                        highlightText={search}
                    />
                ) : (
                    <div className={c.breakWord}>
                        <Highlighter
                            highlightClassName={c.highlight}
                            searchWords={[search]}
                            autoEscape
                            textToHighlight={text}
                        />
                    </div>
                );
            },
        },
        {
            title: '操作',
            dataIndex: 'manipulation',
            width: '20%',
            render(_, rowData, index) {
                const {content, id} = rowData;
                return (
                    <div className={c.manipulation}>
                        <a
                            style={{marginLeft: '8px'}}
                            onClick={() => loadStatements(content, index)}
                        >
                            加载语句
                        </a>
                        <Popconfirm
                            placement="left"
                            className={c.favoriteModel}
                            title={editFavoriteForm}
                            onConfirm={() => onSaveEditFavorite(rowData)}
                            okText="保存"
                            okButtonProps={{disabled: isDisabledName}}
                            cancelText="取消"
                        >
                            <a
                                style={{marginLeft: '8px'}}
                                onClick={() => onEditFavorite(rowData)}
                            >
                                修改名称
                            </a>
                        </Popconfirm>
                        <a style={{marginLeft: '8px'}} onClick={() => onConfirm(id)}>删除</a>
                    </div>
                );
            },
        },
    ];

    const onSearchChange = useCallback(
        e => {
            const value = e.target.value;
            setSearchCache(value);
            if (!value) {
                setSearch(value);
            }
            onChangeSearchValue(value);
        },
        [onChangeSearchValue]
    );

    const onSearch = useCallback(
        () => {
            if (searchCache !== search) {
                setSearch(searchCache);
            }
            onChangeSearchValue(searchCache);
        },
        [search, searchCache, onChangeSearchValue]
    );

    return (
        <>
            <div className={c.searchBar}>
                <Input.Search
                    value={searchCache}
                    onChange={onSearchChange}
                    onSearch={onSearch}
                    placeholder='搜索收藏名称或语句'
                    allowClear
                    style={{width: '215px'}}
                />
            </div>
            <Table
                columns={queryFavoriteColumns}
                dataSource={favoriteQueriesDataRecords}
                rowKey={item => item.id}
                onChange={onSortChange}
                pagination={{
                    onChange: onFavoritePageChange,
                    position: ['bottomRight'],
                    total: favoriteQueriesDataTotal,
                    showSizeChanger: favoriteQueriesDataTotal > 10,
                    current: pageFavorite,
                    pageSize: pageSize,
                }}
                loading={isLoading}
            />
        </>
    );
};

export default Favorite;
