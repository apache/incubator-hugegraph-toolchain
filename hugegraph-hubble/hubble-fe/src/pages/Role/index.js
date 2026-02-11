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
    PageHeader,
    Row,
    Col,
    Input,
    Space,
    Table,
    List,
    Divider,
    Dropdown,
    Menu,
    Modal,
    message,
    Spin,
    Select,
} from 'antd';
import {useCallback, useEffect, useState} from 'react';
import {DetailLayer, AddRoleLayer, AddAccountLayer} from './EditLayer';
import {EllipsisOutlined, PlusOutlined} from '@ant-design/icons';
import ListButton from '../../components/ListButton';
import * as api from '../../api';
import {Link} from 'react-router-dom';
import style from './index.module.scss';
import * as user from '../../utils/user';
import _ from 'lodash';

const Role = () => {
    const [editLayerVisible, setEditLayerVisible] = useState(false);
    const [addLayerVisible, setAddLayerVisible] = useState(false);
    const [refresh, setRefresh] = useState(false);
    const [refreshRole, setRefreshRole] = useState(false);
    const [detail, setDetail] = useState({});
    const [graphspace, setGraphspace] = useState(user.getDefaultGraphspace());
    const [graphspaceList, setGraphspaceList] = useState([]);
    const [role, setRole] = useState({});
    const [roleList, setRoleList] = useState([]);
    const [allRoleList, setAllRoleList] = useState([]);
    const [userList, setUserList] = useState([]);
    const [pagination, setPagination] = useState({current: 1, pageSize: 10});
    const [query, setQuery] = useState('');
    const [accountLayerVisible, setAccountLayerVisible] = useState(false);
    const [selectedItems, setSelectedItems] = useState([]);
    const [loading, setLoading] = useState(false);

    const showDetail = useCallback(row => {
        setDetail(row);
        setEditLayerVisible(true);
    }, []);

    const handleGraphspace = useCallback(data => {
        setGraphspace(data);
        setRole({});
    }, []);

    const handleAdd = useCallback(() => {
        setAddLayerVisible(true);
    }, []);

    const handleSearch = useCallback(val => {
        setPagination({...pagination, current: 1});
        setQuery(val);
    }, [pagination]);

    const handleRole = useCallback(role => {
        setRole(role);
        setSelectedItems([]);
        setPagination({...pagination, current: 1});
    }, [pagination]);

    const handleDelete = useCallback(item => {
        Modal.confirm({
            title: `确定删除角色${item.role_name}吗？`,
            onOk: () => {
                api.auth.delRole(graphspace, item.id).then(res => {
                    if (res.status === 200) {
                        message.success('删除成功');
                        setRefreshRole(!refreshRole);

                        return;
                    }
                    message.error(res.message);
                });
            },
        });
    }, [graphspace, refreshRole]);

    const handleDeleteAccount = useCallback(() => {
        if (selectedItems.length === 0) {
            message.error('至少选择一项');
            return;
        }

        Modal.confirm({
            title: '确定移除角色下的账号吗？',
            onOk: () => {
                api.auth.delRoleUserBatch(graphspace, {ids: selectedItems}).then(res => {
                    if (res.status === 200) {
                        message.success('删除成功');
                        setRefresh(!refresh);
                        setSelectedItems([]);
                        return;
                    }
                    message.error(res.message);
                });
            },
        });
    }, [graphspace, refresh, selectedItems]);

    const handleAccount = useCallback(() => {
        setAccountLayerVisible(true);
    }, []);

    const handleFilter = useCallback(e => {
        setRoleList(allRoleList.filter(item => item.role_name.match(e.target.value) !== null));
    }, [allRoleList]);

    const columns = [
        {
            title: '账号ID',
            dataIndex: 'user_id',
            width: 200,
        },
        {
            title: '账号名',
            dataIndex: 'user_name',
            width: 200,
        },
        {
            title: '备注',
            dataIndex: 'user_description',
        },
        {
            title: '创建时间',
            dataIndex: 'user_create',
            width: 200,
            align: 'center',
        },
        {
            title: '操作',
            align: 'center',
            width: 120,
            render: row => (
                <Space>
                    <a onClick={() => showDetail(row)}>详情</a>
                </Space>
            ),
        },
    ];

    const menu = item => (
        <Menu
            items={[
                {key: '1', label: <a onClick={() => handleDelete(item)}>删除</a>},
                {key: '2', label: <Link to={`/role/graphspace/${item.graphspace}/${item.id}`}>配置权限</Link>},
            ]}
        />
    );

    const handleTable = useCallback(newPagination => {
        setPagination(newPagination);
    }, []);

    const handleHideEditLayer = useCallback(() => {
        setEditLayerVisible(false);
    }, []);

    const handleHideAddLayer = useCallback(() => {
        setAddLayerVisible(false);
    }, []);

    const handleHideAccountLayer = useCallback(() => {
        setAccountLayerVisible(false);
    }, []);

    const handleRefreshRole = useCallback(() => {
        setRefreshRole(!refreshRole);
    }, [refreshRole]);

    const handleRefresh = useCallback(() => {
        setRefresh(!refresh);
    }, [refresh]);

    const rowKey = useCallback(row => row.id, []);

    useEffect(() => {
        const userInfo = user.getUser();
        if (userInfo.is_superadmin) {
            api.manage.getGraphSpaceList({page_size: 99999}).then(res => {
                if (res.status === 200) {
                    setGraphspaceList(res.data.records);
                }
            });
        }
        else if (userInfo.resSpaces && userInfo.resSpaces.length > 0) {
            setGraphspaceList(userInfo.resSpaces);
        }
    }, []);

    useEffect(() => {
        api.auth.getAllRoleList(graphspace).then(res => {
            if (res.status === 200) {
                setRoleList(res.data);
                setAllRoleList(res.data);
            }
        });
    }, [refreshRole, graphspace]);

    useEffect(() => {
        setLoading(true);
        api.auth.getRoleUser(graphspace, {role_id: role.id, user_id: query}).then(res => {
            setLoading(false);
            if (res.status === 200) {
                setUserList(res.data.records);
                setPagination({...pagination, total: res.data.total});
            }
        });
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [refresh, role.id, query, pagination.current, graphspace]);

    return (
        <>
            <PageHeader
                ghost={false}
                onBack={false}
                title={'角色管理'}
            >
                <span>当前图空间:</span>
                <Select
                    onChange={handleGraphspace}
                    options={graphspaceList.map(item => ({label: item.name, value: item.name}))}
                    style={{width: 120}}
                    bordered={false}
                    defaultValue={graphspace}
                />
            </PageHeader>

            <div className='container'>
                <Row gutter={[24, 24]}>
                    <Col span={4}>
                        <List className={style.rolelist}>
                            <List.Item style={{paddingBottom: 20}}>
                                <span style={{fontSize: 16, fontWeight: 500}}>角色组</span>
                                <a onClick={handleAdd} style={{color: '#000', fontSize: 14}}><PlusOutlined /></a>
                            </List.Item>
                            <List.Item>
                                <Input
                                    style={{
                                        marginBottom: 8,
                                    }}
                                    placeholder="请输入角色名检索内容"
                                    onChange={handleFilter}
                                />
                            </List.Item>
                            <List.Item className={role.id ? '' : style.current}>
                                <span
                                    className={style.role_name}
                                    onClick={() => handleRole({})}
                                >全部角色
                                </span>
                            </List.Item>
                            {roleList.map(item => {
                                return (
                                    <List.Item
                                        key={item.id}
                                        className={role.id === item.id ? style.current : ''}
                                    >
                                        <span
                                            className={style.role_name}
                                            onClick={() => handleRole(item)}
                                            title={item.role_nickname}
                                        >
                                            {_.truncate(item.role_nickname, {length: 20})}
                                        </span>
                                        <Dropdown overlay={menu(item)} trigger='click'>
                                            <EllipsisOutlined />
                                        </Dropdown>
                                    </List.Item>
                                );
                            })}
                        </List>
                    </Col>
                    <Col span={20}>
                        <Row justify='space-between' style={{marginBottom: 12}}>
                            <Col>
                                <Space align='baseline'>
                                    <span style={{fontSize: 16, fontWeight: 500}}>
                                        {role.role_name ? role.role_nickname : '全部角色'}
                                    </span>
                                    {role.id && (<a onClick={handleAccount}>添加账号</a>)}
                                    {role.id && (<a onClick={handleDeleteAccount}>移除账号</a>)}
                                </Space>
                            </Col>
                            <Col offset={10}>
                                <Input.Search
                                    placeholder='请输入账号ID或账号名检索内容'
                                    onSearch={handleSearch}
                                />
                            </Col>
                        </Row>
                        <Spin spinning={loading}>
                            <Table
                                columns={columns}
                                dataSource={userList}
                                pagination={pagination}
                                onChange={handleTable}
                                rowSelection={{
                                    type: 'checkbox',
                                    onChange: items => setSelectedItems(items),
                                    selectedRowKeys: selectedItems,
                                }}
                                rowKey={rowKey}
                            />
                        </Spin>
                    </Col>
                </Row>
            </div>

            <DetailLayer
                visible={editLayerVisible}
                detail={detail}
                onCancel={handleHideEditLayer}
            />

            <AddRoleLayer
                visible={addLayerVisible}
                onCancel={handleHideAddLayer}
                refresh={handleRefreshRole}
                graphspace={graphspace}
            />

            <AddAccountLayer
                visible={accountLayerVisible}
                onCancel={handleHideAccountLayer}
                refresh={handleRefresh}
                role={role}
                graphspace={graphspace}
            />
        </>
    );
};

export default Role;
