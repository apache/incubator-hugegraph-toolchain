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

import {Button} from 'antd';
import {SwapOutlined} from '@ant-design/icons';
import {useTranslation} from 'react-i18next';
import {analysisLayout} from '../../../utils/analysisGraphPresentation';

const QuickLayoutSwitcher = ({layout, defaultLayout, startId, buttonEnable, onClick}) => {
    const {t} = useTranslation();
    const original = defaultLayout ?? analysisLayout('force');
    const radial = original.type === 'radial' ? original : analysisLayout({layout: 'radial', startId});
    const returnLayout = original.type === 'radial' ? analysisLayout('force') : original;
    const next = layout?.type === 'radial' ? returnLayout : radial;
    const label = next.type === 'radial' ? 'radial' : next.type === 'd3-force' ? 'force' : 'default';
    return (
        <Button disabled={!buttonEnable} icon={<SwapOutlined aria-hidden />}
            onClick={() => onClick(next)}>
            {t(`analysis.canvas.quick_layout_${label}`)}
        </Button>
    );
};

export default QuickLayoutSwitcher;
