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
    Modal,
    message,
    Pagination,
    Spin,
} from 'antd';
import {useState, useEffect, useCallback} from 'react';
import {EditLayer, ViewLayer, CloneLayer} from './EditLayer';
import {PlusOutlined} from '@ant-design/icons';
import {Link, useParams, useNavigate} from 'react-router-dom';
import style from './index.module.scss';
import * as api from '../../api';
import moment from 'moment';
import GraphCard from './Card';

const Graph = () => {
    const [data, setData] = useState([]);
    const [dateData, setDateData] = useState('');
    const [graphname, setGraphname] = useState('');
    const [graphspaceInfo, setGraphspaceInfo] = useState({});
    const [editLayer, setEditLayer] = useState(false);
    const [viewLayer, setViewLayer] = useState(false);
    const [cloneLayer, setCloneLayer] = useState(false);
    const [selectGraph, setSelectGraph] = useState('');
    const [listType, setListType] = useState('image');
    const [refresh, setRefresh] = useState(false);
    const [pagination, setPagination] = useState({current: 1, pageSize: 11});
    const [loading, setLoading] = useState(false);
    const {graphspace} = useParams();
    const navigate = useNavigate();

    const handlePagination = useCallback(current => {
        setPagination({...pagination, current});
    }, [pagination]);

    const handleTable = useCallback(pagination => {
        setPagination(pagination);
    }, []);

    const handleListType = useCallback(e => {
        setListType(e.target.value);
        setPagination({...pagination, current: 1, pageSize: e.target.value === 'image' ? 11 : 10});
    }, [pagination]);

    const handleSearch = useCallback(val => {
        setGraphname(val);
        setRefresh(!refresh);
    }, [refresh]);

    const showEditLayer = useCallback(() => {
        setEditLayer(true);
        setSelectGraph('');
    }, []);

    const editGraph = graph => {
        setSelectGraph(graph);
        setEditLayer(true);
    };

    const clearData = graph => {
        Modal.confirm({
            title: '确定清除数据？',
            content: '清除后无法恢复',
            onOk: () => {
                api.manage.clearGraphData(graphspace, graph).then(res => {
                    if (res.status === 200) {
                        message.success('操作成功');
                        setRefresh(!refresh);
                        return;
                    }
                    message.error(res.message);
                });
            },
        });
    };

    const clearSchema = graph => {
        Modal.confirm({
            title: '确定清除schema+数据？',
            content: '清除后无法恢复',
            onOk: () => {
                api.manage.clearGraphDataAndSchema(graphspace, graph).then(res => {
                    if (res.status === 200) {
                        message.success('操作成功');
                        setRefresh(!refresh);
                        return;
                    }
                    message.error(res.message);
                });
            },
        });
    };

    const showSchema = graph => {
        setViewLayer(true);
        setSelectGraph(graph);
    };

    const deleteGraph = graph => {
        Modal.confirm({
            title: '确定删除图吗?',
            content: '删除后无法恢复',
            onOk: () => {
                const hide = message.loading('删除中', 0);
                api.manage.delGraph(graphspace, graph).then(res => {
                    hide();
                    if (res.status === 200) {
                        message.success('删除成功');
                        setRefresh(!refresh);
                        return;
                    }

                    message.error('删除失败');
                });
            },
        });
    };

    const setDefault = graph => {
        const hide = message.loading('设置中...', 0);
        api.manage.setDefaultGraph(graphspace, graph).then(res => {
            hide();
            if (res.status === 200) {
                message.success('设置成功');
                setRefresh(!refresh);
                return;
            }
            message.error(res.message);
        });
    };

    const handleSetDefault = graph => {
        api.manage.getDefaultGraph(graphspace).then(res => {
            if (res.status !== 200) {
                message.error(res.message);
                return;
            }

            if (res.data.default_graph) {
                Modal.confirm({
                    title: '确认更改图的默认设置?',
                    onOk: () => setDefault(graph),
                });

                return;
            }

            setDefault(graph);
        });
    };

    const handleBack = useCallback(() => {
        navigate('/graphspace');
    }, [navigate]);

    const handleHideEditLayer = useCallback(() => {
        setEditLayer(false);
    }, []);

    const handleRefresh = useCallback(() => {
        setRefresh(!refresh);
    }, [refresh]);

    const handleHideViewLayer = useCallback(() => {
        setViewLayer(false);
    }, []);

    const handleDatePickerChange = useCallback((_, val) => setDateData(val), []);

    const handleGotoMeta = useCallback(item => {
        navigate(`/graphspace/${item.graphspace}/graph/${item.name}/meta`);
    }, [navigate]);

    const handleGotoAnalysis = useCallback(item => {
        navigate(`/gremlin/${item.graphspace}/${item.name}`);
    }, [navigate]);

    const showClone = graph => {
        setSelectGraph(graph);
        setCloneLayer(true);
    };

    const handleHideCloneLayer = useCallback(() => {
        setSelectGraph('');
        setCloneLayer(false);
    }, []);

    const columns = [
        {
            title: '图名称',
            render: row => (
                <Link to={`/gremlin/${row.graphspace}/${row.name}`}>
                    {row.nickname}{row.default && <span className={style.default}>默认</span>}
                </Link>
            ),
        },
        {
            title: '图空间',
            dataIndex: 'graphspace_nickname',
        },
        {
            title: '创建时间',
            dataIndex: 'create_time',
            align: 'center',
            width: 140,
            render: val => moment(val).format('YYYY-MM-DD'),
        },
        {
            title: '更新时间',
            dataIndex: 'update_time',
            align: 'center',
            width: 140,
            render: val => moment(val).format('YYYY-MM-DD'),
        },
        {
            title: '创建人',
            dataIndex: 'creator',
            align: 'center',
            width: 140,
        },
        {
            title: '操作',
            width: 420,
            align: 'center',
            render: row => {
                return (
                    <Space>
                        <Link to={`/graphspace/${graphspace}/graph/${row.name}/meta`}>元数据配置</Link>
                        {(row.default)
                            ? <span className={style.disable}>清空</span>
                            : <a onClick={() => clearSchema(row.name)}>清空</a>}
                        {(row.graphspace === 'neizhianli')
                            ? <span className={style.disable}>删除</span>
                            : <a onClick={() => deleteGraph(row.name)}>删除</a>}
                        <a onClick={() => showSchema(row.name)}>查看schema</a>
                        {(row.graphspace === 'neizhianli')
                            ? <span className={style.disable}>编辑</span>
                            : <a onClick={() => editGraph(row.name)}>编辑</a>}
                        {row.default
                            ? <span className={style.disable}>设为默认</span>
                            : <a onClick={() => handleSetDefault(row.name)}>设为默认</a>}
                        <a onClick={() => showClone(row.name)}>克隆图</a>
                    </Space>
                );
            },
        },
    ];

    const getMenus = item => [
        {
            key: '0',
            label: <a onClick={() => handleGotoAnalysis(item)}>进入图分析平台</a>,
        },
        {
            key: '1',
            label: <a onClick={() => handleGotoMeta(item)}>元数据配置</a>,
        },
        {
            key: '2',
            label: item.isDefault
                ? <span className={style.disable}>清空schema+数据</span>
                : <a onClick={() => clearSchema(item.name)}>清空schema+数据</a>,
        },
        {
            key: '3',
            label: <a onClick={() => clearData(item.name)}>清空数据</a>,
        },
        {
            key: '4',
            label: item.isDefault
                ? <span className={style.disable}>设为默认</span>
                : <a onClick={() => handleSetDefault(item.name)}>设为默认</a>,
        },
        {
            key: '5',
            label: <a onClick={() => showSchema(item.name)}>查看schema</a>,
        },
        {
            key: '6',
            label: item.graphspace === 'neizhianli'
                ? <span className={style.disable}>编辑</span>
                : <a onClick={() => editGraph(item.name)}>编辑</a>,
        },
        {
            key: '7',
            label: item.graphspace === 'neizhianli'
                ? <span className={style.disable}>删除</span>
                : <a onClick={() => deleteGraph(item.name)}>删除</a>,
        },
        {
            key: '8',
            label: <a onClick={() => showClone(item.name)}>克隆图</a>,
        },
        // {
        //     key: '8',
        //     label: <a onClick={() => showClone(item.name)}>克隆图</a>,
        // },
    ];

    useEffect(() => {
        api.manage.getGraphSpace(graphspace).then(res => {
            if (res.status === 200) {
                setGraphspaceInfo(res.data);
                return;
            }

            message.error(res.message);
        });
    }, [graphspace]);

    useEffect(() => {
        setLoading(true);

        api.manage.getGraphList(graphspace, {
            create_time: dateData,
            query: graphname,
            page_no: pagination.current,
            page_size: pagination.pageSize,
        }).then(res => {
            // hide();
            setLoading(false);
            if (res.status === 200) {
                setData(res.data.records);
                setPagination({...pagination, total: res.data.total});

                return;
            }

            message.error(res.message);
        });

    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [refresh, pagination.current, listType, dateData, graphspace, graphname]);

    return (
        <Spin spinning={loading}>
            <PageHeader
                ghost={false}
                onBack={handleBack}
                title={(graphspaceInfo.nickname ?? graphspace) + ' - 图管理'}
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
                                onSearch={handleSearch}
                                placeholder='请输入图名称'
                            />
                        </Space>
                    </Col>
                </Row>
            </PageHeader>

            <div className='container'>
                {listType === 'image'
                    ? (
                        <>
                            <Row gutter={[10, 10]} justify='start'>
                                <Col span={8} key='add'>
                                    <Card className={style.add_card} onClick={showEditLayer}>
                                        <Space><PlusOutlined />创建图</Space>
                                    </Card>
                                </Col>

                                {data.map(item => {
                                    const menus = getMenus(item);

                                    return (
                                        <Col span={8} key={item.name}>
                                            <GraphCard
                                                item={item}
                                                menus={menus}
                                            />
                                        </Col>
                                    );
                                })}
                            </Row>
                            <br />
                            <Row justify='end'>
                                <Col>
                                    <Pagination
                                        current={pagination.current}
                                        pageSize={pagination.pageSize}
                                        total={pagination.total}
                                        onChange={handlePagination}
                                    />
                                </Col>
                            </Row>
                        </>
                    )
                    : (
                        <>
                            <Row>
                                <Col>
                                    <Button onClick={showEditLayer} type='primary'>创建图</Button>
                                </Col>
                            </Row>
                            <br />
                            <Table
                                columns={columns}
                                dataSource={data}
                                pagination={pagination}
                                onChange={handleTable}
                            />
                        </>
                    )
                }
                <EditLayer
                    visible={editLayer}
                    onCancel={handleHideEditLayer}
                    graphspace={graphspace}
                    refresh={handleRefresh}
                    graph={selectGraph}
                />
                <ViewLayer
                    visible={viewLayer}
                    onCancel={handleHideViewLayer}
                    graph={selectGraph}
                    graphspace={graphspace}
                />
                <CloneLayer
                    open={cloneLayer}
                    onCancel={handleHideCloneLayer}
                    refresh={handleRefresh}
                    graph={selectGraph}
                    graphspace={graphspace}
                />
            </div>
        </Spin>
    );
};

export default Graph;
