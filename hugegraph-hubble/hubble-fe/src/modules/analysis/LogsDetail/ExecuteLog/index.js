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
 * @file gremlin表格 执行记录
 * @author
 */

import {useState, useCallback} from 'react';
import {Table, Space, Tag, Input, Popconfirm} from 'antd';
import ExecutionContent from '../../../../components/ExecutionContent';
import c from './index.module.scss';
const ExecuteLog = props => {
    const {
        executeLogsDataRecords,
        executeLogsDataTotal,
        isLoading,
        pageExecute,
        pageSize,
        onExecutePageChange,
        onAddCollection,
        onLoadContent,
    } = props;

    const [favoriteName, setFavoriteName] = useState();
    const [disabledFavorite, setDisabledFavorite]  = useState(true);

    const loadStatements = useCallback(
        (text, rowData, index) => {
            onLoadContent(rowData.content);
            const headerTabNode = document.getElementById('queryBar');
            window.scrollTo(0, headerTabNode.offsetTop);
        },
        [onLoadContent]
    );

    const onFavoraiteName = useCallback(
        e => {
            setFavoriteName(e.target.value);
            e.target.value ? setDisabledFavorite(false) : setDisabledFavorite(true);
        },
        []
    );

    const onFavoriteCard = useCallback(
        () => {
            setFavoriteName('');
            setDisabledFavorite(true);
        },
        []
    );

    const updateAddCollection = useCallback(
        content => {
            onAddCollection(content, favoriteName);
        },
        [favoriteName, onAddCollection]
    );

    const onAddFavorite = useCallback(
        content => {
            updateAddCollection(content);
        },
        [updateAddCollection]
    );

    const favoriteContent = rowData => (
        <>
            <div style={{marginBottom: '16px'}}>收藏语句</div>
            <Input
                style={{marginBottom: '18px'}}
                placeholder="请输入收藏名称"
                showCount
                maxLength={48}
                value={favoriteName}
                onChange={onFavoraiteName}
            />
        </>
    );

    const typeDesc =  {
        GREMLIN: 'GREMLIN查询',
        GREMLIN_ASYNC: 'GREMLIN任务',
        ALGORITHM: '算法任务',
        CYPHER: 'CYPHER查询',
    };

    const statusDesc =  {
        SUCCESS: '成功',
        ASYNC_TASK_SUCCESS: '提交成功',
        ASYNC_TASK_RUNNING: '提交运行中',
        RUNNING: '运行中',
        FAILED: '失败',
        ASYNC_TASK_FAILED: '提交失败',
    };

    const statusColor =  {
        SUCCESS: 'green',
        ASYNC_TASK_SUCCESS: 'green',
        RUNNING: 'geekblue',
        FAILED: 'volcano',
        ASYNC_TASK_FAILED: 'volcano',
    };

    const executeLogColumns = [
        {
            title: '时间',
            dataIndex: 'create_time',
            width: '20%',
        },
        {
            title: '执行类型',
            dataIndex: 'type',
            width: '15%',
            render: type => typeDesc[type] || type,
        },
        {
            title: '执行内容',
            dataIndex: 'content',
            width: '30%',
            render: (text, rowData, index) => {
                return text.split('\n')[1] ? (
                    <ExecutionContent
                        content={text}
                        highlightText=""
                    />
                ) : (
                    <div className={c.breakWord}>
                        {text}
                    </div>
                );
            },
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: '10%',
            render: status => {
                return (
                    <Space>
                        <Tag color={statusColor[status]} key={status}>
                            {statusDesc[status] || status}
                        </Tag>
                    </Space>
                );
            },
        },
        {
            title: '耗时',
            dataIndex: 'duration',
            width: '10%',
        },
        {
            title: '操作',
            dataIndex: 'manipulation',
            width: '15%',
            render: (text, rowData, index) => {
                return (
                    <div className={c.manipulation}>
                        <Popconfirm
                            placement="left"
                            title={favoriteContent(rowData)}
                            onConfirm={() => onAddFavorite(rowData.content)}
                            okButtonProps={{disabled: disabledFavorite}}
                            okText="收藏"
                            cancelText="取消"
                        >
                            <a onClick={onFavoriteCard}>收藏</a>
                        </Popconfirm>
                        <a
                            style={{marginLeft: '8px'}}
                            onClick={() => loadStatements(text, rowData, index)}
                        >
                            加载语句
                        </a>
                    </div>
                );
            },
        },
    ];


    return (
        <Table
            columns={executeLogColumns}
            dataSource={executeLogsDataRecords}
            rowKey={item => item.id}
            pagination={{
                onChange: onExecutePageChange,
                position: ['bottomRight'],
                total: executeLogsDataTotal,
                showSizeChanger: executeLogsDataTotal > 10,
                current: pageExecute,
                pageSize: pageSize,
            }}
            loading={isLoading}
        />
    );
};

export default ExecuteLog;
