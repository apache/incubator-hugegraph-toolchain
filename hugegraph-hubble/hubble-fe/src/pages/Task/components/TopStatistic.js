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

import {Row, Col} from 'antd';
import style from './index.module.scss';

const CardBox = ({title, listItems}) => {

    return (
        <div className={style.cardbox}>
            <div className={style.title}>{title}</div>
            <Row justify='space-around'>
                {listItems.map(item => {
                    return (
                        <Col key={item.label}>
                            <div className={style.value}>{item.value ?? 0}</div>
                            <div className={style.label}>{item.label}</div>
                        </Col>
                    );
                })}
            </Row>
        </div>
    );
};

const TopStatistic = ({data}) => {

    return (
        <Row className={style.row}>
            <Col span={5}>
                <CardBox
                    title='实时任务'
                    listItems={[
                        {label: '最大并发', value: data.total_realtime_size},
                    ]}
                />
            </Col>
            <Col span={5}>
                <CardBox
                    title='非实时任务'
                    listItems={[
                        {label: '最大并发', value: data.total_other_size},
                    ]}
                />
            </Col>
            <Col span={7}>
                <CardBox
                    title='待执行'
                    listItems={[
                        {label: '一次性任务', value: data?.todo?.ONCE},
                        {label: '周期任务', value: data?.todo?.CRON},
                        {label: '实时任务', value: data?.todo?.REALTIME},
                    ]}
                />
            </Col>
            <Col span={7} className={style.last}>
                <CardBox
                    title='正在执行'
                    listItems={[
                        {label: '一次性任务', value: data?.running?.ONCE},
                        {label: '周期任务', value: data?.running?.CRON},
                        {label: '实时任务', value: data?.running?.REALTIME},
                    ]}
                />
            </Col>
        </Row>
    );
};

export default TopStatistic;
