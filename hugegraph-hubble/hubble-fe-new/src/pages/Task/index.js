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
    Button,
    Row,
    Col,
    PageHeader,
    Input,
    Modal,
    Table,
    Space,
    Tooltip,
    message,
    Badge,
    Spin,
} from 'antd';
import {
    EditOutlined,
    DeleteOutlined,
    FileTextOutlined,
    PauseOutlined,
    CaretRightOutlined,
    LineChartOutlined,
} from '@ant-design/icons';
import {useState, useEffect, useCallback} from 'react';
import style from './index.module.scss';
import EditLayer from './components/EditLayer';
import ViewLayer from './components/ViewLayer';
import TopStatistic from './components/TopStatistic';
import {useNavigate, Link} from 'react-router-dom';
import * as api from '../../api';
import {StatusField} from '../../components/Status';
import {sourceType, syncType} from './config';
import TableHeader from '../../components/TableHeader';

const DetailTip = ({row}) => {
    const {job_summary} = row;

    return (
        <Link to={`/task/detail/${row.task_id}`}>
            <Tooltip
                title={(
                    <div className={style.task_detail}>
                        <div>任务详情</div>
                        <Space>
                            <Badge status="success" text={<span>完成: {job_summary.success_count}</span>} />
                            <Badge status="error" text={<span>失败: {job_summary.failed_count}</span>} />
                            <Badge status="processing" text={<span>运行中: {job_summary.running_count}</span>} />
                        </Space>
                    </div>)
                }
            >
                <LineChartOutlined />
            </Tooltip>
        </Link>
    );
};

const RunningText = ({status, onClick, data}) => {
    const handleClick = useCallback(() => {
        onClick(data);
    }, [onClick, data]);

    return status === 'enable' ? (
        <Tooltip title='暂停'><PauseOutlined onClick={handleClick} /></Tooltip>
    ) : (
        <Tooltip title='执行'><CaretRightOutlined onClick={handleClick} /></Tooltip>
    );
};

const Task = () => {
    const [data, setData] = useState([]);
    const [searchName, setSearchName] = useState('');
    const [editLayer, setEditLayer] = useState(false);
    const [viewLayer, setViewLayer] = useState(false);
    const [refresh, setRefresh] = useState(false);
    const [detail, setDetail] = useState({});
    const [pagination, setPagination] = useState({pageSize: 10, current: 1});
    const [metricsData, setMetricsData] = useState({});
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const search = useCallback(val => {
        setSearchName(val);
        setPagination({...pagination, current: 1});
    }, [pagination]);

    const handleTable = useCallback(newPagination => {
        setPagination(newPagination);
    }, []);

    const enableTask = useCallback(id => {
        api.manage.enableTask(id).then(res => {
            if (res.status === 200) {
                setRefresh(!refresh);
                message.success('启动成功');

                return;
            }

            message.error('启动失败');
        });
    }, [refresh]);

    const disableTask = useCallback(id => {
        api.manage.disableTask(id).then(res => {
            if (res.status === 200) {
                setRefresh(!refresh);
                message.success('暂停成功');

                return;
            }

            message.error('暂停失败');
        });
    }, [refresh]);

    const deleteTask = id => {
        setLoading(true);
        api.manage.deleteTask(id).then(res => {
            setLoading(false);
            if (res.status === 200) {
                setRefresh(!refresh);
                message.success('删除成功');

                return;
            }

            message.error('删除失败');
        });
    };

    const editTask = row => {
        setDetail(row);
        setEditLayer(true);
    };

    const viewTask = row => {
        setDetail(row);
        setViewLayer(true);
    };

    const handleBack = useCallback(() => {
        navigate('/task/edit');
    }, [navigate]);

    const handleRefresh = useCallback(() => {
        setRefresh(!refresh);
    }, [refresh]);

    const handleHideEditLayer = useCallback(() => setEditLayer(false), []);

    const handleHideViewLayer = useCallback(() => setViewLayer(false), []);

    const rowKey = useCallback(record => record.task_id, []);

    const columns = [
        {
            title: '任务名称',
            dataIndex: 'task_name',
        },
        {
            title: '源数据类型',
            dataIndex: 'ingestion_mapping',
            render: val => {
                const {structs} = val;
                if (structs === null) {
                    return '未知';
                }
                const type = structs[0].input.type;

                return sourceType.find(item => item.value === type).label ?? '未知';
            },
        },
        {
            title: '目标图空间',
            dataIndex: 'ingestion_option',
            render: val => val.graphspace,
        },
        {
            title: '目标图',
            dataIndex: 'ingestion_option',
            render: val => val.graph,
        },
        {
            title: '导入时间',
            dataIndex: 'create_time',
        },
        {
            title: '创建人',
            dataIndex: 'creator',
        },
        {
            title: '最新状态',
            dataIndex: 'last_metrics',
            align: 'center',
            width: 120,
            render: val => <StatusField status={val} />,
        },
        {
            title: '同步策略',
            dataIndex: 'task_schedule_type',
            render: val => {
                return syncType.find(item => item.value === val).label;
            },
        },
        {
            title: '操作',
            align: 'center',
            width: 160,
            render: row => {
                return (
                    <Space>
                        <DetailTip row={row} />
                        <a onClick={() => viewTask(row)}><FileTextOutlined /></a>
                        {row.task_schedule_status  === 'DISABLE'
                            ? <a onClick={() => editTask(row)}><EditOutlined /></a>
                            : <EditOutlined style={{color: '#8c8c8c'}} />}
                        {/* <a>{row.task_schedule_status === 'ENABLE'
                            ? <Tooltip title='暂停'><PauseOutlined onClick={() => disableTask(row.task_id)} /></Tooltip>
                            : (
                                <Tooltip title='执行'>
                                    <CaretRightOutlined onClick={() => enableTask(row.task_id)} />
                                </Tooltip>)}
                        </a> */}
                        <a>{row.task_schedule_status === 'ENABLE'
                            ? <RunningText status='enable' data={row.task_id} onClick={disableTask} />
                            : <RunningText data={row.task_id} onClick={enableTask} />}
                        </a>
                        {row.task_schedule_status === 'DISABLE'
                            ? (
                                <a
                                    onClick={() => Modal.confirm({
                                        title: '删除数据源',
                                        content: '删除后，该数据源将从系统中消失。您确定要删除这个数据源吗？',
                                        onOk: () => deleteTask(row.task_id),
                                    })}
                                >
                                    <DeleteOutlined />
                                </a>
                            )
                            : <DeleteOutlined style={{color: '#8c8c8c'}} />}
                    </Space>
                );
            },
        },
    ];

    useEffect(() => {
        api.manage.getTaskList({
            query: searchName,
            page_no: pagination.current,
        }).then(res => {
            if (res.status === 200) {
                setData(res.data.records);
                setPagination({...pagination, total: res.data.total, pageSize: res.data.size});
                return;
            }

            message.error(res.message);
        });

        api.manage.getMetricsTask().then(res => {
            if (res.status === 200) {
                // console.log(res);
                setMetricsData(res.data);
                return;
            }

            message.error(res.message);
        });
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [searchName, refresh, pagination.current]);

    useEffect(() => {
        let id = setInterval(() => {
            setRefresh(val => !val);
        }, 12000);

        return () => clearInterval(id);
    }, []);

    return (
        <>
            <PageHeader
                ghost={false}
                onBack={false}
                title="数据导入"
            >
                <TopStatistic data={metricsData} />
                <Row justify='end' style={{paddingTop: 16}}>
                    <Col><Input.Search placeholder='请输入任务名称' onSearch={search} /></Col>
                </Row>
            </PageHeader>

            <div className='container'>
                <Spin spinning={loading}>
                    <TableHeader>
                        <Button type='primary' onClick={handleBack}>创建任务</Button>
                    </TableHeader>
                    <Table
                        columns={columns}
                        rowKey={rowKey}
                        dataSource={data}
                        pagination={pagination}
                        onChange={handleTable}
                    />
                </Spin>
                <EditLayer
                    visible={editLayer}
                    data={detail}
                    onCancel={handleHideEditLayer}
                    refresh={handleRefresh}
                />
                <ViewLayer
                    visible={viewLayer}
                    task_id={detail.task_id}
                    onCancel={handleHideViewLayer}
                />
            </div>
        </>
    );
};

export default Task;
