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

import {message, PageHeader, Table} from 'antd';
import {useState, useEffect, useCallback} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import * as api from '../../api';
import {StatusField} from '../../components/Status';

const columns = [
    {
        title: '执行实例ID',
        dataIndex: 'job_id',
        render: val => val.toString(),
    },
    {
        title: '导入条数',
        dataIndex: 'job_metrics',
        align: 'right',
        render: val => val?.total_count,
    },
    {
        title: '创建时间',
        dataIndex: 'create_time',
        align: 'center',
    },
    {
        title: '平均速率',
        dataIndex: 'job_metrics',
        align: 'right',
        render: (val, row) => {
            if (val) {
                const rate = row.job_status.toLowerCase() === 'running' ? val.cur_rate : val.avg_rate;
                return `${rate} 条/s`;
            }

            return '-';
        },
    },
    {
        title: '导入时长',
        dataIndex: 'job_metrics',
        align: 'right',
        render: val => {
            if (val) {
                return `${val.total_time / 1000}s`;
            }

            return '-';
        },
    },
    {
        title: '状态',
        dataIndex: 'job_status',
        align: 'center',
        render: val => <StatusField status={val} />,
    },
    {
        title: '其它',
        width: 400,
        align: 'center',
        dataIndex: 'job_message',
        render: val => val ?? '-',
    },
];

const TaskDetail = () => {
    const [data, setData] = useState([]);
    const {taskid} = useParams();
    const navigate = useNavigate();

    const handleBack = useCallback(() => navigate('/task'), [navigate]);

    useEffect(() => {
        api.manage.getJobsList({taskid}).then(res => {
            if (res.status === 200) {
                setData(res.data.records);
                return;
            }

            message.error(res.message);
        });
    }, [taskid]);

    return (
        <>
            <PageHeader
                ghost={false}
                onBack={handleBack}
                title="任务详情"
            />

            <div className='container'>
                <Table
                    columns={columns}
                    dataSource={data}
                />
            </div>
        </>
    );
};

export default TaskDetail;
