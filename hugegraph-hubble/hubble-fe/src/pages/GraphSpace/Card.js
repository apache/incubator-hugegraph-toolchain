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

import {ExclamationCircleOutlined, InfoOutlined} from '@ant-design/icons';
import {Menu, Typography, Dropdown, Row, Col, Space, Progress, Card, Tooltip} from 'antd';
import {Link, useNavigate} from 'react-router-dom';
import moment from 'moment';
import style from './index.module.scss';
import React, {useCallback} from 'react';

const showText = (val, suffix, empty) => (val > 99999 ? (empty === undefined ? '未限制' : empty) : `${val}${suffix}`);

const formatPercent = percent => {
    return percent >= 100 ? <InfoOutlined /> : `${percent}%`;
};

const TitleField = ({item, onClick}) => (
    <>
        <Typography.Text
            style={{maxWidth: 244}}
            ellipsis={{ellipsis: true}}
            title={`${item.nickname}`}
            onClick={onClick}
        >{item.nickname}
        </Typography.Text>
        <div className={style.subtitle}>
            {item.default && <span className={style.default}>默认图空间</span>}
            {moment(item.create_time).format('YYYY-MM-DD')}创建
        </div>
    </>
);

const GraphSpaceCard = ({item, editGraphspace, deleteGraphspace, handleSetDefault, handleInit}) => {
    const navigate = useNavigate();

    const handleGotoGraph = useCallback(() => {
        navigate(`/graphspace/${item.name}`);
    }, [item, navigate]);

    const handleEdit = useCallback(() => {
        editGraphspace(item);
    }, [item, editGraphspace]);

    const handleDelete = useCallback(() => {
        deleteGraphspace(item.name);
    }, [deleteGraphspace, item]);

    const handleSet = useCallback(() => {
        handleSetDefault(item.name);
    }, [handleSetDefault, item]);

    const Overlay = ({item}) => (
        item.name === 'neizhianli' ? (
            <Menu
                items={[
                    {
                        key: '1',
                        label: <Link to={`/graphspace/${item.name}/schema`}>schema模版管理</Link>,
                    },
                    {
                        key: '2',
                        label: <a onClick={handleInit}>初始化</a>,
                    },
                ]}
            />
        ) : (
            <Menu
                items={[
                    {
                        key: '1',
                        label: <Link to={`/graphspace/${item.name}/schema`}>schema模版管理</Link>,
                    },
                    {
                        key: '2',
                        label: <a onClick={handleEdit}>编辑</a>,
                    },
                    {
                        key: '3',
                        label: (item.default)
                            ? <span className={style.disable}>删除</span>
                            : <a onClick={handleDelete}>删除</a>,
                    },
                    {
                        key: '4',
                        label: (item.default)
                            ? <span className={style.disable}>设为默认</span>
                            : <a onClick={handleSet}>设为默认</a>,
                    },
                ]}
            />
        )
    );

    return (
        <Card
            className={style.card}
            title={<TitleField item={item} onClick={handleGotoGraph} />}
            headStyle={{
                backgroundImage: 'linear-gradient(180deg, '
                    + 'rgba(51,136,255,0.10) 0%, rgba(51,136,255,0.00) 100%)',
                borderBottom: 0,
                height: 93,
                paddingLeft: 20,
            }}
            bodyStyle={{
                padding: '0 24px 24px 20px',
            }}
            extra={(
                <Dropdown.Button
                    overlay={<Overlay item={item} />}
                    trigger={['click']}
                >
                    <Link to={`/graphspace/${item.name}`}>进入图空间</Link>
                </Dropdown.Button>
            )}
            actions={
                [
                    <Space key="1">
                        <span>顶点：{item.statistic?.vertex}</span>
                        <span>边：{item.statistic?.edge}</span>
                        <span>
                            <Tooltip title={<><div>点边数每日更新</div><div>本次数据更新于 {item.statistic.date}</div></>}>
                                <ExclamationCircleOutlined />
                            </Tooltip>
                        </span>
                    </Space>,
                ]
            }
        >
            <div className={style.card_content} onClick={handleGotoGraph}>
                <Row justify='space-between'>
                    <Col span={14}>
                        <ul className={style.list}>
                            <li>图ID：{item.name}</li>
                            <li>是否鉴权：{item.auth ? '是' : '否'}</li>
                            <li>
                                最大图数：{showText(item.max_graph_number, '')}
                            </li>
                            <li>cpu资源：{showText(item.cpu_limit, '核')}</li>
                            <li>内存资源：{showText(item.memory_limit, 'G')}</li>
                            <li>最大存储空间限制：
                                {showText(item.storage_limit, 'G')}
                            </li>
                        </ul>
                    </Col>
                    <Col span={8}>
                        <Space direction='vertical'>
                            <Progress
                                type='circle'
                                width={96}
                                percent={item.storage_percent * 100}
                                format={formatPercent}
                                status={item.storage_percent >= 1
                                    ? 'exception' : 'normal'}
                            />
                            <div
                                style={{textAlign: 'center', fontSize: '12px'}}
                            >
                                <div>已使用：</div>
                                <div>
                                    {item.storage_used}G
                                    {' / '}
                                    {showText(item.storage_limit, 'G', '--')}
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
