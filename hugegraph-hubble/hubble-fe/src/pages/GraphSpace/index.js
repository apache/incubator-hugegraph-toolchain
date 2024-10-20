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
    Table,
    Space,
    Row,
    Col,
    PageHeader,
    Button,
    Input,
    Radio,
    DatePicker,
    Card,
    Menu,
    Dropdown,
    message,
    Modal,
    Typography,
    Pagination,
    Progress,
    Spin,
} from 'antd';
import {useState, useEffect, useCallback} from 'react';
import moment from 'moment';
import {EditLayer} from './EditLayer';
import TableHeader from '../../components/TableHeader';
import {Link, useNavigate} from 'react-router-dom';
import {PlusOutlined, InfoOutlined} from '@ant-design/icons';
import GraphSpaceCard from './Card';
import style from './index.module.scss';
import * as api from '../../api/index';

const showText = (val, suffix, empty) => (val > 99999 ? (empty === undefined ? '未限制' : empty) : `${val}${suffix}`);

const GraphSpace = () => {
    const [data, setData] = useState([]);
    const [detail, setDetail] = useState({});
    const [editLayer, setEditLayer] = useState(false);
    const [listType, setListType] = useState('image');
    const [refresh, setRefresh] = useState('false');
    const [dateData, setDateData] = useState('');
    const [graphspacename, setGraphspacename] = useState('');
    const [pagination, setPagination] = useState({toatal: 0, current: 1, pageSize: 11});
    const [loading, setLoading] = useState(false);

    const handleCreate = useCallback(() => {
        setEditLayer(true);
        setDetail({});
    }, []);

    const editGraphspace = useCallback(detail => {
        setDetail(detail);
        setEditLayer(true);
    }, []);

    const createGraphspace = useCallback(() => {
        setEditLayer(true);
        setDetail(false);
    }, []);

    const handleListType = useCallback(e => {
        setListType(e.target.value);
        setPagination({...pagination, current: 1, pageSize: e.target.value === 'image' ? 11 : 10});
    }, [pagination]);

    const setDefault = useCallback(graphspace => {
        setLoading(true);

        api.manage.setDefaultGraphSpace(graphspace).then(res => {
            setLoading(false);
            if (res.status === 200) {
                message.success('设置成功');
                setRefresh(!refresh);
                return;
            }
            message.error(res.message);
        });
    }, [refresh]);

    const handleSetDefault = useCallback(graphspace => {
        api.manage.getDefaultGraphSpace().then(res => {
            if (res.status !== 200) {
                message.error(res.message);
                return;
            }

            if (res.data.default_space) {
                Modal.confirm({
                    title: '确认更改图空间的默认设置?',
                    onOk: () => setDefault(graphspace),
                });

                return;
            }

            setDefault(graphspace);
        });
    }, [setDefault]);

    const deleteGraphspace = useCallback(graphspace => {
        Modal.confirm({
            title: '确定删除?',
            content: '删除空间后不可恢复',
            onOk: () => {
                api.manage.delGraphSpace(graphspace).then(res => {
                    if (res.status === 200) {
                        message.success('删除成功');
                        setRefresh(!refresh);
                        return;
                    }
                    message.error(res.message);
                });
            },
        });
    }, [refresh]);

    const handlePage = useCallback(current => {
        setPagination({...pagination, current});
    }, [pagination]);

    const handleTable = useCallback(pagination => {
        setPagination(pagination);
    }, []);

    const handleSearch = useCallback(value => {
        setGraphspacename(value);
        setRefresh(!refresh);
    }, [refresh]);

    const handleDatePickerChange = useCallback((_, val) => {
        setDateData(val);
        setRefresh(!refresh);
    }, [refresh]);

    const handleRefresh = useCallback(() => setRefresh(!refresh), [refresh]);

    const handleInit = useCallback(() => {
        api.manage.initBuiltin({init_space: true, init_hlm: true, init_covid19: true}).then(res => {
            if (res.status === 200) {
                message.success('初始化成功');
                setRefresh(!refresh);
                return;
            }
            message.error(res.message);
        });
    }, []);

    const hideEditLayer = useCallback(() => {
        setEditLayer(false);
    }, []);

    const columns = [
        {
            title: '名称',
            render: row => (
                <>
                    <Link to={`/graphspace/${row.name}`}>{row.nickname}</Link>
                    {row.default && <span className={style.default}>默认</span>}
                </>
            ),
        },
        {
            title: '是否开启鉴权',
            dataIndex: 'auth',
            width: 120,
            align: 'center',
            render: val => (val ? '是' : '否'),
        },
        {
            title: '描述',
            dataIndex: 'description',
            ellipsis: true,
        },
        {
            title: '图服务',
            dataIndex: 'cpu_limit',
            // eslint-disable-next-line max-len
            render: (_, row) => `${showText(row.cpu_limit, '核')}/${showText(row.memory_limit, 'G')}/${row.oltp_namespace}`,
            ellipsis: true,
        },
        {
            title: '计算任务',
            dataIndex: 'task',
            // eslint-disable-next-line max-len
            render: (_, row) => `${showText(row.compute_cpu_limit, '核')}/${showText(row.compute_memory_limit, 'G')}/${row.olap_namespace}`,
            ellipsis: true,
        },
        {
            title: '最大存储空间限制',
            dataIndex: 'storage_limit',
            width: 150,
            align: 'center',
            render: val => showText(val, 'G'),
        },
        {
            title: '操作',
            width: 280,
            align: 'center',
            render: row => {
                return (
                    row.name === 'neizhianli' ? (
                        <Space wrap>
                            <Link to={`/graphspace/${row.name}/schema`}>schema管理</Link>
                            <a onClick={handleInit}>初始化</a>
                        </Space>
                    ) : (
                        <Space wrap>
                            {(row.default)
                                ? <span className={style.disable}>编辑</span>
                                : <a onClick={() => editGraphspace(row)}>编辑</a>
                            }
                            {(row.default)
                                ? <span className={style.disable}>删除</span>
                                : <a onClick={() => deleteGraphspace(row.name)}>删除</a>
                            }
                            <Link to={`/graphspace/${row.name}/schema`}>schema管理</Link>
                            {(row.default)
                                ? <span className={style.disable}>设为默认</span>
                                : <a onClick={() => handleSetDefault(row.name)}>设为默认</a>}
                        </Space>
                    )
                );
            },
        },
    ];

    useEffect(() => {
        setLoading(true);

        api.manage.getGraphSpaceList({
            create_time: dateData,
            query: graphspacename,
            page_no: pagination.current,
            page_size: pagination.pageSize,
        }).then(res => {
            setLoading(false);
            if (res.status === 200) {
                setData(res.data.records);
                setPagination({...pagination, total: res.data.total});
            }
        });
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [refresh, listType, dateData, graphspacename, pagination.current]);

    return (
        <>
            <PageHeader
                ghost={false}
                onBack={false}
                title="图空间管理"
            >
                <Row justify='space-between'>
                    <Col>
                        <DatePicker onChange={handleDatePickerChange} />
                    </Col>
                    <Col>
                        <Space>
                            <Radio.Group
                                options={[{label: '图模式', value: 'image'}, {label: '列表模式', value: 'list'}]}
                                optionType='button'
                                buttonStyle='solid'
                                defaultValue={'image'}
                                onChange={handleListType}
                            />
                            <Input.Search
                                placeholder='请输入图空间名称'
                                onSearch={handleSearch}
                                allowClear
                            />
                        </Space>
                    </Col>
                </Row>
            </PageHeader>

            <div className='container'>
                <Spin spinning={loading}>
                    {listType === 'image'
                        ? (
                            <>
                                <Row gutter={[10, 10]} justify='start'>
                                    <Col span={8} key='add'>
                                        <Card className={style.add_card} onClick={handleCreate}>
                                            <Space><PlusOutlined />创建图空间</Space>
                                        </Card>
                                    </Col>

                                    {data.map(item => {
                                        return (
                                            <Col span={8} key={item.name}>
                                                <GraphSpaceCard
                                                    item={item}
                                                    deleteGraphspace={deleteGraphspace}
                                                    editGraphspace={editGraphspace}
                                                    handleSetDefault={handleSetDefault}
                                                    handleInit={handleInit}
                                                />
                                            </Col>
                                        );
                                    })}
                                </Row>
                                <br />
                                <Row justify='end'>
                                    <Col>
                                        <Pagination
                                            onChange={handlePage}
                                            total={pagination.total}
                                            pageSize={pagination.pageSize}
                                            current={pagination.current}
                                        />
                                    </Col>
                                </Row>
                            </>
                        ) : (
                            <>
                                <TableHeader>
                                    <Button onClick={createGraphspace} type='primary'>创建图空间</Button>
                                </TableHeader>
                                <Table
                                    columns={columns}
                                    dataSource={data}
                                    pagination={pagination}
                                    onChange={handleTable}
                                />
                            </>
                        )
                    }
                </Spin>
                <EditLayer
                    visible={editLayer}
                    onCancel={hideEditLayer}
                    detail={detail}
                    refresh={handleRefresh}
                />
            </div>
        </>
    );
};

export default GraphSpace;
