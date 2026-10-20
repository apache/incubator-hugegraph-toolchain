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

/** @file 渲染方式选择（仅设置面板内使用） */

import {useTranslation} from 'react-i18next';
import {Select} from 'antd';
import {GRAPH_RENDER_MODE} from '../../../utils/constants';

const RenderModeSwitcher = ({buttonEnable, onClick, value = GRAPH_RENDER_MODE.CANVAS2D}) => {
    const {t} = useTranslation();
    return (
        <Select
            aria-label={t('analysis.canvas.render_mode')}
            style={{width: '100%'}}
            disabled={!buttonEnable}
            value={value}
            options={[
                {label: t('analysis.canvas.mode_2d'), value: GRAPH_RENDER_MODE.CANVAS2D},
                {label: t('analysis.canvas.mode_3d_experimental'), value: GRAPH_RENDER_MODE.CANVAS3D},
            ]}
            onChange={mode => { if (mode !== value) onClick(mode); }}
        />
    );
};

export default RenderModeSwitcher;
