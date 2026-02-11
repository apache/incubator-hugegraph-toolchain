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

import {Layout, Space, Avatar, Dropdown, Menu, message, Modal, Select} from 'antd';
import {UserOutlined} from '@ant-design/icons';
import style from './index.module.scss';
import Logo from '../../assets/logo.png';
import {useNavigate, useLocation} from 'react-router-dom';
import * as api from '../../api/index';
import * as user from '../../utils/user';
import {useState} from 'react';
import {useTranslation} from 'react-i18next';

const {Option} = Select;

const Topbar = () => {
    const userInfo = user.getUser();
    const navigate = useNavigate();
    const location = useLocation();
    const {t} = useTranslation();
    const [languageType, setLanguageType] = useState(localStorage.getItem('languageType') || 'zh-CN');

    if (!userInfo || !userInfo.id) {
        sessionStorage.setItem('redirect', `${location.pathname}${location.search}`);
        window.location.href = '/login';
    }

    const i18Change = e => {
        localStorage.setItem('languageType', e);
        setLanguageType(e);
        window.location.reload();
    };

    const logout = () => {

        api.auth.logout().then(res => {
            if (res.status === 200) {
                sessionStorage.removeItem('redirect');
                user.clearUser();
                message.success(t('Topbar.exit.success'));
                navigate('/login');
            }
        });
    };

    const confirm = () => {
        Modal.confirm({
            title: t('Topbar.exit.confirm'),
            okText: t('common.verify.ok'),
            cancelText: t('common.verify.cancel'),
            onOk: logout,
        });
    };

    return (
        <Layout.Header>
            <div className={style.logo}><img src={Logo} alt='' /></div>
            <div className={style.rightContainer}>
                <Select
                    defaultValue={languageType}
                    style={{width: 120}}
                    size="small"
                    onChange={i18Change}
                >
                    <Option value="zh-CN">中文</Option>
                    <Option value="en-US">English</Option>
                </Select>
                <Dropdown overlay={<Menu
                    items={[{key: 'logout', label: <a onClick={confirm}>{t('Topbar.exit.name')}</a>}]}
                />}
                >
                    <Space className={style.right}>
                        <Avatar size={'small'} icon={<UserOutlined />} />
                        <span>{userInfo?.user_nickname ?? ''}</span>
                    </Space>
                </Dropdown>
            </div>
        </Layout.Header>
    );
};

export default Topbar;
