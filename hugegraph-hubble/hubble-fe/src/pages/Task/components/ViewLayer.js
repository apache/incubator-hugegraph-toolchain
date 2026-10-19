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
    Alert,
    Button,
    Collapse,
    Descriptions,
    Modal,
    Spin,
} from 'antd';
import {useCallback, useEffect, useRef, useState} from 'react';
import * as api from '../../../api';
import JsonViewer from '../../../components/JsonViewer';
import {useTranslation} from 'react-i18next';
import {TaskStatus} from '../status';
import style from './index.module.scss';


const displayValue = (value, fallback) => (
    value === null || value === undefined || value === '' ? fallback : value
);

const ViewLayer = ({visible, onCancel, task_id}) => {
    const {t} = useTranslation();
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(false);
    const detailRequest = useRef(null);
    const unavailable = t('task.view.unavailable_value');

    const onFinish = useCallback(() => {
        onCancel();
    }, [onCancel]);

    const loadDetail = useCallback(async () => {
        const token = Symbol('task-view-detail');
        detailRequest.current = token;
        setData(null);
        setError(false);
        setLoading(true);
        try {
            const res = await api.manage.getTaskDetail(task_id, {
                suppressBusinessErrorToast: true,
            });
            if (detailRequest.current !== token) {
                return;
            }
            if (res.status === 200) {
                setData(JSON.parse(JSON.stringify(res.data)));
                return;
            }
            setError(true);
        }
        catch {
            if (detailRequest.current === token) {
                setError(true);
            }
        }
        finally {
            if (detailRequest.current === token) {
                setLoading(false);
            }
        }
    }, [task_id]);

    useEffect(() => {
        if (!visible) {
            detailRequest.current = null;
            return undefined;
        }
        loadDetail();
        return () => {
            detailRequest.current = null;
        };
    }, [visible, task_id, loadDetail]);

    return (
        <Modal
            title={t('task.view.title')}
            onCancel={onCancel}
            open={visible}
            width={600}
            onOk={onFinish}
            destroyOnHidden
        >
            <Spin spinning={loading}>
                {error && (
                    <Alert
                        type='error'
                        showIcon
                        title={t('task.view.unavailable')}
                        action={(
                            <Button size='small' onClick={loadDetail}>
                                {t('task.view.retry')}
                            </Button>
                        )}
                    />
                )}
                {data && (
                    <>
                        <Descriptions bordered size='small' column={1} items={[
                            {
                                key: 'name', label: t('task.view.name'),
                                children: displayValue(data.job_name ?? data.name, unavailable),
                            },
                            {
                                key: 'space', label: t('task.view.target_space'),
                                children: displayValue(data.graphspace, unavailable),
                            },
                            {
                                key: 'graph', label: t('task.view.target_graph'),
                                children: displayValue(data.graph, unavailable),
                            },
                            {
                                key: 'status', label: t('task.view.status'),
                                children: <TaskStatus status={data.job_status ?? data.status} />,
                            },
                            {
                                key: 'size', label: t('task.view.data_size'),
                                children: displayValue(data.job_size, unavailable),
                            },
                            {
                                key: 'duration', label: t('task.view.duration'),
                                children: displayValue(data.job_duration, unavailable),
                            },
                            {
                                key: 'created', label: t('task.view.create_time'),
                                children: displayValue(data.create_time, unavailable),
                            },
                            {
                                key: 'updated', label: t('task.view.update_time'),
                                children: displayValue(data.update_time, unavailable),
                            },
                        ]} />
                        <Collapse className={style.technical_details} ghost items={[{
                            key: 'technical', label: t('task.view.technical_details'),
                            children: <div className={style.raw_json}><JsonViewer value={data} /></div>,
                        }]} />
                    </>
                )}
            </Spin>
        </Modal>
    );
};

export default ViewLayer;
