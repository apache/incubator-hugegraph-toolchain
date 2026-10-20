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

import {lazy, Suspense} from 'react';
import {Button, Result, Spin} from 'antd';
import {useTranslation} from 'react-i18next';
import RouteErrorBoundary from '../../../components/RouteErrorBoundary';
import c from './index.module.scss';

const Renderer = lazy(() => import('./Renderer'));

const Canvas3D = ({onExit, children, ...props}) => {
    const {t} = useTranslation();
    const fallback = (
        <div className={c.loading} role="alert">
            <Result
                status="warning"
                title={t('analysis.canvas.canvas_3d.unavailable')}
                subTitle={t('analysis.canvas.canvas_3d.refresh_hint')}
                extra={<Button onClick={onExit}>{t('analysis.canvas.canvas_3d.back_to_2d')}</Button>}
            />
        </div>
    );
    return (
        <div className={c.container}>
            <RouteErrorBoundary fallback={fallback}>
                <Suspense fallback={<div className={c.loading} role="status"><Spin /></div>}>
                    <Renderer {...props} />
                </Suspense>
            </RouteErrorBoundary>
            {children}
        </div>
    );
};

export default Canvas3D;
