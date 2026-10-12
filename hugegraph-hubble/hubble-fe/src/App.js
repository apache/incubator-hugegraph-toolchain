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

import Route from './routes';
import 'antd/dist/antd.css';
import './App.scss';
import './App.css';
import './styles/workbench.scss';
import Layout from './layout.ant';
import {AuthContextProvider} from './auth/AuthContext';
import * as api from './api';
import {setConfig} from './utils/config';
import {clearLogin} from './utils/user';
import {useEffect, useState} from 'react';

const CONFIG_RETRY_DELAY_MS = 2000;
const CONFIG_REVALIDATE_DELAY_MS = 30_000;
const routeConfigSignature = config => JSON.stringify([
    config.auth_enabled !== false,
    config.pd_enabled === true,
    config.graph_create_enabled === true,
    config.cypher_enabled === true,
]);

function App() {
    const [configReady, setConfigReady] = useState(false);
    const [configError, setConfigError] = useState(false);
    const [configRevision, setConfigRevision] = useState(0);

    useEffect(() => {
        let active = true;
        let hasSafeConfig = false;
        let mountedConfigSignature;
        let mountedAuthEnabled;
        let retryTimer;
        const loadConfig = () => {
            api.config.getConfig().then(response => {
                if (response?.status !== 200 || !response.data) {
                    throw new Error('invalid_hubble_config');
                }
                if (active) {
                    hasSafeConfig = true;
                    const nextSignature = routeConfigSignature(response.data);
                    const nextAuthEnabled
                            = response.data.auth_enabled !== false;
                    const verified
                            = response.data.server_capabilities_verified !== false;
                    if (verified && mountedAuthEnabled !== false
                        && !nextAuthEnabled) {
                        clearLogin();
                    }
                    if (verified) {
                        mountedAuthEnabled = nextAuthEnabled;
                    }
                    setConfig(response.data);
                    if (nextSignature !== mountedConfigSignature) {
                        mountedConfigSignature = nextSignature;
                        setConfigRevision(value => value + 1);
                    }
                    setConfigReady(true);
                    setConfigError(false);
                    const unverified = response.data
                        .server_capabilities_verified === false;
                    const delay = unverified
                        ? CONFIG_RETRY_DELAY_MS : CONFIG_REVALIDATE_DELAY_MS;
                    retryTimer = window.setTimeout(loadConfig, delay);
                }
            }).catch(() => {
                if (active) {
                    if (hasSafeConfig) {
                        retryTimer = window.setTimeout(
                            loadConfig, CONFIG_RETRY_DELAY_MS
                        );
                    }
                    else {
                        setConfigError(true);
                    }
                }
            });
        };
        loadConfig();
        return () => {
            active = false;
            window.clearTimeout(retryTimer);
        };
    }, []);

    if (configError) {
        return (
            <div role='alert'>
                Unable to load Hubble configuration.
                <button type='button' onClick={() => window.location.reload()}>
                    Retry
                </button>
            </div>
        );
    }
    if (!configReady) {
        return null;
    }
    return (
        <div>
            <AuthContextProvider key={configRevision}>
                <Route element={<Layout />} />
            </AuthContextProvider>
        </div>
    );
};

export default App;
