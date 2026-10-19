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

import ReactDOM from 'react-dom/client';
import {BrowserRouter} from 'react-router-dom';
import {App as AntApp, ConfigProvider} from 'antd';
import {useTranslation} from 'react-i18next';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import 'antd/dist/reset.css';
import {useEffect} from 'react';
import './index.css';
import App from './App';
import zhCN from 'antd/locale/zh_CN';
import enUS from 'antd/locale/en_US';
import './i18n';

const LocalizedApp = () => {
    const {i18n} = useTranslation();
    const locale = i18n.language === 'zh-CN' ? zhCN : enUS;
    useEffect(() => {
        dayjs.locale(i18n.language === 'zh-CN' ? 'zh-cn' : 'en');
    }, [i18n.language]);
    return (
        <ConfigProvider locale={locale}>
            <AntApp>
                <App />
            </AntApp>
        </ConfigProvider>
    );
};
const root = ReactDOM.createRoot(document.getElementById('root'));

root.render(
    <BrowserRouter>
        <LocalizedApp />
    </BrowserRouter>
);
