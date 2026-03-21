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
    Space,
    Divider,
    Tree,
    Spin,
    message,
    Select,
} from 'antd';
import {useEffect, useState, useCallback} from 'react';
import {PlusOutlined} from '@ant-design/icons';
import {useParams, useNavigate} from 'react-router-dom';
import * as api from '../../api';
import treeData from './treeData';
import {AddResourceLayer} from './EditLayer';
import * as user from '../../utils/user';
import {StatusA} from '../../components/Status';
import ListButton from '../../components/ListButton';

const RoleAuth = () => {
    const params = useParams();
    const navigate = useNavigate();
    const [graphspace, setGraphspace] = useState(params.graphspace);
    const [graphspaceList, setGraphspaceList] = useState([]);
    const [role, setRole] = useState(params.role);
    const [roleList, setRoleList] = useState([]);
    const [resourceList, setResourceList] = useState([]);
    const [refresh, setRefresh] = useState(false);
    const [addLayerVisible, setAddLayerVisible] = useState(false);
    const [isChange, setIsChange] = useState(false);
    const [loading, setLoading] = useState(false);

    const isDefault = name => /.*DEFAULT_OBSERVER_GROUP$/.test(name);

    const handleGraphspace = useCallback(data => {
        setGraphspace(data);
        setIsChange(true);
    }, []);

    const handleRole = useCallback(data => {
        setRole(data);
    }, []);

    const handleAdd = useCallback(() => {
        setAddLayerVisible(true);
    }, []);

    const handleBack = useCallback(() => {
        navigate('/role');
    }, [navigate]);

    const handleHieLayer = useCallback(() => {
        setAddLayerVisible(false);
    }, []);

    const handleRefresh = useCallback(() => {
        setRefresh(!refresh);
    }, [refresh]);

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
                isChange && setRole(res.data.length > 0 ? res.data[0].id : '');
            }
        });
    }, [graphspace, isChange]);

    useEffect(() => {
        if (!role) {
            return;
        }

        setLoading(true);
        api.auth.getRoleResourceList(graphspace, {role_id: role}).then(res => {
            setLoading(false);
            if (res.status === 200) {
                setResourceList(res.data);
                return;
            }
            message.error(res.message);
        });
    }, [role, refresh, graphspace]);

    return (
        <>
            <PageHeader
                ghost={false}
                onBack={handleBack}
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
                <span>当前角色:</span>
                <Select
                    onChange={handleRole}
                    options={roleList.map(item => ({label: item.role_nickname, value: item.id}))}
                    style={{width: 120}}
                    bordered={false}
                    placeholder="请选择"
                    optionLabelProp="value"
                    value={role}
                />
            </PageHeader>

            <div className='container'>
                <Spin spinning={loading}>
                    <Tree
                        showLine
                        treeData={resourceList.map(item => treeData(item, role, () => setRefresh(!refresh))).concat(
                            {
                                title: (
                                    <StatusA
                                        onClick={handleAdd}
                                        disable={isDefault(role)}
                                    >添加资源<PlusOutlined />
                                    </StatusA>),
                                key: '_add',
                            })}
                    />
                </Spin>
            </div>

            <AddResourceLayer
                visible={addLayerVisible}
                onCancel={handleHieLayer}
                refresh={handleRefresh}
                graphspace={graphspace}
                role={role}
            />
        </>
    );
};

export default RoleAuth;
