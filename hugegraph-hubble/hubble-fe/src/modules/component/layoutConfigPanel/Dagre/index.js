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
 * @file 图分析组件 层次布局
 */

import {useMemo, useId} from 'react';
import {Form, Select, InputNumber} from 'antd';
import {useTranslation} from 'react-i18next';

const DagreLayoutForm = props => {
    const {handleFormChange, initialValues, disabled} = props;
    const {t} = useTranslation();
    const instanceId = useId();
    const {useForm} = Form;
    const [dagreLayoutForm] = useForm();
    const rankdirOptions = useMemo(
        () => [
            {value: 'TB', label: t('analysis.canvas.layout_panel.top_bottom')},
            {value: 'BT', label: t('analysis.canvas.layout_panel.bottom_top')},
            {value: 'LR', label: t('analysis.canvas.layout_panel.left_right')},
            {value: 'RL', label: t('analysis.canvas.layout_panel.right_left')},
        ],
        [t]
    );
    const alignOptions = useMemo(
        () => [
            {value: 'center', label: t('analysis.canvas.layout_panel.align_center')},
            {value: 'UL', label: t('analysis.canvas.layout_panel.align_ul')},
            {value: 'UR', label: t('analysis.canvas.layout_panel.align_ur')},
            {value: 'DL', label: t('analysis.canvas.layout_panel.align_dl')},
            {value: 'DR', label: t('analysis.canvas.layout_panel.align_dr')},
        ],
        [t]
    );

    return (
        <Form
            name={instanceId}
            form={dagreLayoutForm}
            onValuesChange={handleFormChange}
            disabled={disabled}
            initialValues={initialValues}
            labelCol={{span: 24}}
        >
            <Form.Item
                name='rankdir'
                label={t('analysis.canvas.layout_panel.rankdir')}
                tooltip={t('analysis.canvas.layout_panel.rankdir_tooltip')}
            >
                <Select aria-label={t('analysis.canvas.layout_panel.rankdir')} options={rankdirOptions} />
            </Form.Item>
            <Form.Item
                name='align'
                label={t('analysis.canvas.layout_panel.align')}
                tooltip={t('analysis.canvas.layout_panel.align_tooltip')}
            >
                <Select aria-label={t('analysis.canvas.layout_panel.align')} options={alignOptions} />
            </Form.Item>
            <Form.Item
                name='ranksep'
                label={t('analysis.canvas.layout_panel.ranksep')}
                tooltip={t('analysis.canvas.layout_panel.ranksep_tooltip')}
            >
                <InputNumber aria-label={t('analysis.canvas.layout_panel.ranksep')} min={0} style={{width: '100%'}} max={1000} />
            </Form.Item>
            <Form.Item
                name='nodesep'
                label={t('analysis.canvas.layout_panel.nodesep')}
                tooltip={t('analysis.canvas.layout_panel.nodesep_tooltip')}
            >
                <InputNumber aria-label={t('analysis.canvas.layout_panel.nodesep')} min={0} style={{width: '100%'}} max={1000} />
            </Form.Item>
        </Form>
    );
};

export default DagreLayoutForm;
