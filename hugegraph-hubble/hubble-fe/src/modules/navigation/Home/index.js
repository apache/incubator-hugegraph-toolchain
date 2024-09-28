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

/**
 * @file 导航首页
 * @author
 */

import {PageHeader} from 'antd';
import ManageItem from '../ManageItem';
import AnalyseItem from '../AnalyseItem';
import AdminItem from '../AdminItem';
import ConsoleItem from '../ConsoleItem';
import * as user from '../../../utils/user';

import imgLogo from '../../../assets/logo_new.png';

import style from './index.module.scss';


const NavigationHome = () => {
    const userInfo = user.getUser();

    return (
        <>
            <PageHeader
                ghost={false}
                title="导航"
            />

            <div className={style.navigation}>
                <div className={style.header}>
                    <img width={'30%'} src={imgLogo} title={'logoNew'} alt={'logoNew'} />
                </div>
                <div className={style.container}>
                    <ManageItem />
                    <AnalyseItem />
                    {userInfo.is_superadmin && <AdminItem />}
                    {userInfo.is_superadmin && <ConsoleItem />}
                </div>
            </div>
        </>
    );
};

export default NavigationHome;
