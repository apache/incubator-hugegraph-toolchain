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

import React, {useState} from 'react';
import {Layout, Menu} from 'antd';
import {
    HomeOutlined,
    DatabaseOutlined,
    AlertOutlined,
    FundViewOutlined,
    MenuUnfoldOutlined,
    MenuFoldOutlined,
} from '@ant-design/icons';
import {Link, useLocation} from 'react-router-dom';
import * as user from '../../utils/user';
import {useTranslation} from 'react-i18next';

const items = t => {
    const userInfo = user.getUser();
    const MY = {label: <Link to='/my'>{t('home.my')}</Link>, key: 'my'};
    const ACCOUNT = {label: <Link to='/account'>{t('home.account')}</Link>, key: 'account'};
    const RESOURCE = {label: <Link to='/resource'>{t('home.resource')}</Link>, key: 'resource'};
    const ROLE = {label: <Link to='/role'>{t('home.role')}</Link>, key: 'role'};

    let systemList = [MY];
    if (userInfo.is_superadmin) {
        systemList = [MY, ACCOUNT, RESOURCE, ROLE];
    }
    else if (userInfo.resSpaces && userInfo.resSpaces.length > 0) {
        systemList = [MY, RESOURCE, ROLE];
    }

    const menu = [
        {
            label: <Link to='/navigation'>{t('navigation.name')}</Link>,
            key: 'navigation',
            icon: <HomeOutlined />,
        },
        {
            label: t('manage.name'),
            key: 'manage',
            icon: <FundViewOutlined />,
            children: [
                {label: <Link to='/graphspace'>{t('manage.graphspace')}</Link>, key: 'graphspace'},
                {label: <Link to='/source'>{t('manage.source')}</Link>, key: 'source'}, // TODO X fix import
                {label: <Link to='/task'>{t('manage.task')}</Link>, key: 'task'},
            ],
        },
        {
            label: t('analysis.name'),
            key: 'analysis',
            icon: <DatabaseOutlined />,
            children: [
                {label: <Link to='/gremlin'>{t('analysis.query.name')}</Link>, key: 'gremlin'},
                {label: <Link to='/algorithms'>{t('analysis.algorithm.name')}</Link>, key: 'algorithms'},
                {label: <Link to='/asyncTasks'>{t('analysis.async_task.name')}</Link>, key: 'asyncTasks'},
            ],
        },
        {
            label: t('home.name'),
            key: 'system',
            icon: <AlertOutlined />,
            children: [...systemList],
        },
    ];

    return menu;
};

const Sidebar = () => {
    const [collapsed, setCollapsed] = useState(false);
    const href = useLocation();
    const {t} = useTranslation();
    const menuKey = href.pathname.split('/')[1] || 'navigation';

    return (
        <Layout.Sider
            collapsible
            collapsed={collapsed}
            onCollapse={value => setCollapsed(value)}
            theme='light'
            trigger={
                collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />
            }
        >
            <Menu
                defaultSelectedKeys={['graphspace']}
                defaultOpenKeys={['manage', 'analysis', 'system']}
                mode="inline"
                items={items(t)}
                selectedKeys={[menuKey]}
            />
        </Layout.Sider>
    );
};

export default Sidebar;
