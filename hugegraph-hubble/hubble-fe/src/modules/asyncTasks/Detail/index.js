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
 * @file 任务管理 table页面
 * @author zhanghao14@
 */

import React, {useState, useCallback, useContext} from 'react';
import {Table, Tag, Spin, message, Button, Typography, Modal} from 'antd';
import GraphAnalysisContext from '../../Context';
import {CloseOutlined} from '@ant-design/icons';
import * as api from '../../../api/index';
import formatTimeDuration from '../../../utils/formatTimeDuration';
import {
    Async_Task_Type,
    Async_Taskt_Status,
    Async_Taskt_Status_Name,
    Filter_Task_Status,
    Async_Task_Manipulations,
    Status_Color,
} from '../../../utils/constants';
import {intersection, size} from 'lodash-es';
import {format} from 'date-fns';
import c from './index.module.scss';
const {Text} = Typography;

const {FAILED, SUCCESS, DELETING, CANCELLING} = Async_Taskt_Status;

const AsyncTaskDetail = props => {
    const {
        page,
        pageSize,
        onPageChange,
        getAsynTaskList,
        asyncManageTaskData,
    } = props;

    const {graphSpace: currentGraphSpace, graph: currentGraph, isVermeer} = useContext(GraphAnalysisContext);
    const {records: asyncManageTaskDataRecords, total: asyncManageTaskDataTotal} = asyncManageTaskData || {};
    const [selectedRowKeys, setSelectedRowKeys] = useState([]);

    const onSelectChange = (rowKey, selectedRows) => {
        setSelectedRowKeys(rowKey);
    };

    const rowSelection = {
        selectedRowKeys,
        onChange: onSelectChange,
        getCheckboxProps: record => {
            const checkboxProps = ['scheduling', 'scheduled', 'queued', 'running', 'restoring', 'deleting'];
            return {
                disabled: checkboxProps.includes(record.task_status),
                task_status: record.task_status,
            };

        },
    };

    const currentSelectedRowKeys = intersection(
        selectedRowKeys,
        asyncManageTaskDataRecords?.map(({id}) => id)
    );

    const onRefresh = useCallback(() => {
        getAsynTaskList();
    }, [getAsynTaskList]);

    const renderTaskTypeFilters = () => {
        const res = [];
        const keys = Object.keys(Async_Task_Type);
        for (let i = 0; i < keys.length; i++) {
            const item = keys[i];
            const text = Async_Task_Type[item];
            if (!text.includes('vermeer')) {
                res.push({text: Async_Task_Type[item], value: item});
            }
            else if (isVermeer) {
                res.push({text: Async_Task_Type[item], value: item});
            }
        }
        return res;
    };

    const renderTaskStatusFilters = () => {
        const res = [];
        const keys = Object.keys(Filter_Task_Status);
        for (let i = 0; i < keys.length; i++) {
            const item = keys[i];
            res.push({text: Filter_Task_Status[item], value: item});
        }
        return res;
    };

    const viewResult = useCallback(
        (text, rowData, index) => {
            window.open(`/asyncTasks/result/${currentGraphSpace}/${currentGraph}/${rowData.id}`);
        }, [currentGraph, currentGraphSpace]);

    const deleteTaskByIds = useCallback(
        taskIdArr => {
            const parmas  = {ids: taskIdArr};
            api.analysis.deleteAsyncTask(currentGraphSpace, currentGraph, parmas)
                .then(res => {
                    const {status, message: errMsg} = res;
                    if (status === 200) {
                        onRefresh();
                    }
                    else {
                        !errMsg && message.error('删除失败');
                    }
                });
        }, [currentGraph, currentGraphSpace, onRefresh]);

    const abortAsyncTaskById = useCallback(
        async taskId => {
            const response  = await api.analysis.abortAsyncTask(currentGraphSpace, currentGraph, taskId);
            const {status, message: abortAsyncTaskMessage} = response || {};
            if (status === 200) {
                onRefresh();
            }
            else {
                !abortAsyncTaskMessage && message.error('终止失败');
            }
        }, [currentGraphSpace, currentGraph, onRefresh]);

    const onAbortTaskHandler = useCallback(taskId => {
        abortAsyncTaskById(taskId);
    }, [abortAsyncTaskById]);

    const onDeleteConfirm = id => {
        Modal.confirm({
            title: '删除确任',
            content: '确认删除该任务？删除后无法恢复，请谨慎操作',
            okText: '确定',
            cancelText: '取消',
            onOk: () => deleteTaskByIds([id]),
        });
    };

    const onMassDeleteConfirm = () => {
        Modal.confirm({
            title: '批量删除',
            content: '确认删除以下任务？删除后无法恢复，请谨慎操作',
            okText: '确定',
            cancelText: '取消',
            onOk: () => deleteTaskByIds(currentSelectedRowKeys),
        });
    };

    const columns = [
        {
            title: '任务ID',
            dataIndex: 'id',
            fixed: 'left',
        },
        {
            title: '任务名称',
            dataIndex: 'task_name',
            render: (task_name, rowData, index) => {
                return (<Text ellipsis={{tooltip: task_name}}>{task_name}</Text>);
            },
        },
        {
            title: '任务类型',
            dataIndex: 'task_type',
            filters: renderTaskTypeFilters(),
            filterMultiple: false,
            render: (task_type, rowData, index) => {
                return (<>{Async_Task_Type[task_type] || task_type}</>);
            },
        },
        {
            title: '创建时间',
            dataIndex: 'task_create',
            render: (task_create, rowData, index) => {
                const convertedDate = format(new Date(task_create), 'yyyy-MM-dd H:m:ss');
                return (<>{convertedDate}</>);
            },
        },
        {
            title: '耗时',
            dataIndex: 'task_progress',
            render: (task_progress, rowData, index) => {
                const {task_update, task_create} = rowData;
                const duration = formatTimeDuration(task_create, task_update);
                return <div style={{whiteSpace: 'nowrap'}}>{duration}</div>;
            },
        },
        {
            title: '状态',
            dataIndex: 'task_status',
            filterMultiple: false,
            filters: renderTaskStatusFilters(),
            render: (task_status, rowData, index) => {
                return <Tag color={Status_Color[task_status]}>{Async_Taskt_Status_Name[task_status]}</Tag>;
            },
        },
        {
            title: '操作',
            dataIndex: 'manipulation',
            render: (result, rowData, index) => {
                const {'task_status': status, 'task_type': type, id: taskId}  = rowData;
                const allowCheckResTypeArr = ['gremlin', 'computer-dis', 'cypher'];
                const isAllowCheckRes = status === SUCCESS && allowCheckResTypeArr.includes(type);
                const allowAbortStatusArr = ['scheduling', 'scheduled', 'queued', 'running', 'restoring'];
                const isAllowAbort = allowAbortStatusArr.includes(status);
                const {
                    'check_reason': reason,
                    'check_result': resultText,
                    'delete': delText,
                    abort,
                    aborting,
                } = Async_Task_Manipulations;
                return (
                    <div style={{whiteSpace: 'nowrap'}}>
                        {status === FAILED && (
                            <a
                                style={{margin: '10px'}}
                                href={`/asyncTasks/result/${currentGraphSpace}/${currentGraph}/${taskId}`}
                            >
                                {reason}
                            </a>

                        )}
                        {isAllowCheckRes && (
                            <a style={{margin: '10px'}} onClick={() => viewResult(result, rowData, index)}>
                                {resultText}
                            </a>
                        )}
                        {!isAllowAbort && (
                            status === DELETING
                                ? <Spin type="strong" />
                                : <a style={{margin: '10px'}} onClick={() => onDeleteConfirm(taskId)}>{delText}</a>
                        )}
                        {isAllowAbort && (
                            <a style={{margin: '10px'}} onClick={() => onAbortTaskHandler(taskId)}>
                                {abort}
                            </a>
                        )}
                        {status === CANCELLING && (
                            <div><a style={{margin: '10px'}}>{aborting}</a></div>
                        )}
                    </div>
                );
            },
        },
    ];

    return (
        <div className={c.gremlinAsyncTaskDetail}>
            {size(currentSelectedRowKeys) !== 0 && (
                <div className={c.massDelete}>
                    <div className={c.left}>
                        <span style={{marginRight: '12px'}}>已选{size(currentSelectedRowKeys)}项</span>
                        <Button onClick={onMassDeleteConfirm}>批量删除</Button>
                    </div>
                    <CloseOutlined onClick={() => setSelectedRowKeys([])} />
                </div>
            )}
            <Table
                rowKey='id'
                scroll={{x: 1000}}
                rowSelection={rowSelection}
                columns={columns}
                dataSource={asyncManageTaskDataRecords}
                onChange={onPageChange}
                pagination={{
                    position: ['bottomRight'],
                    total: asyncManageTaskDataTotal,
                    showSizeChanger: asyncManageTaskDataTotal > 10,
                    current: page,
                    pageSize: pageSize,
                }}
            />
        </div>
    );
};

export default AsyncTaskDetail;
