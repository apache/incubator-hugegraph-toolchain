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
 * @file K-out API(POST，高级版) StepForm
 * @author
 */

import React, {useState, useCallback} from 'react';
import {Form, Select, Tooltip, InputNumber} from 'antd';
import {DownOutlined, RightOutlined, QuestionCircleOutlined} from '@ant-design/icons';
import StepsItems from '../../../StepsItems';
import {integerValidator} from '../../../utils';
import classnames from 'classnames';
import s from '../../OltpItem/index.module.scss';

const directionOptions = [
    {label: '出', value: 'OUT'},
    {label: '入', value: 'IN'},
    {label: '双向', value: 'BOTH'},
];

const description = {
    source: '起始顶点id',
    max_depth: '步数',
    nearest: `nearest为true时，代表起始顶点到达结果顶点的最短路径长度为depth，不存在更短的路径；
    nearest为false时，代表起始顶点到结果顶点有一条长度为depth的路径（未必最短且可以有环）`,
    capacity: '遍历过程中最大的访问的顶点数目',
    limit: '返回的顶点的最大数目',
    algorithm: '遍历方式,常情况下，deep_first（深度优先搜索）方式会具有更好的遍历性能。但当参数nearest为true时，可能会包含非最近邻的节点，尤其是数据量较大时',
    steps: '从起始点出发的Step集合',
    edge_steps: '边Step集合',
    vertex_steps: '点Step集合',
    stepsObj: {
        direction: '起始顶点向外发散的方向',
        max_degree: '查询过程中，单个顶点遍历的最大邻接边数目(注: 0.12版之前 step 内仅支持 degree 作为参数名, 0.12开始统一使用 max_degree, 并向下兼容 degree 写法)',
        skip_degree: `用于设置查询过程中舍弃超级顶点的最小边数，即当某个顶点的邻接边数目大于 skip_degree 时，完全舍弃该顶点。选填项，如果开启时, 需满足 skip_degree >= 
        max_degree 约束，默认为0 (不启用)，表示不跳过任何点 (注意: 开启此配置后，遍历时会尝试访问一个顶点的 skip_degree 条边，而不仅仅是 max_degree 条边，这样有额外的遍历
        开销，对查询性能影响可能有较大影响，请确认理解后再开启)`,
        steps: {
            label: '点边类型',
            properties: '通过属性的值过滤点边',
        },
    },
};

const StepFormItem = () => {
    const [stepVisible, setStepVisible] = useState(false);

    const stepContentClassName = classnames(
        s.stepContent,
        {[s.contentHidden]: !stepVisible}
    );


    const changeStepVisible = useCallback(() => {
        setStepVisible(pre => !pre);
    }, []
    );

    return (
        <>
            <div className={s.stepHeader} onClick={changeStepVisible}>
                <div className={s.stepIcon}>
                    {stepVisible ? <DownOutlined /> : <RightOutlined />}
                </div>
                <div className={s.stepTitle}>steps:</div>
                <div className={s.tooltip}>
                    <Tooltip
                        placement="rightTop"
                        title='从起始点出发的Step集合'
                    >
                        <QuestionCircleOutlined />
                    </Tooltip>
                </div>
            </div>
            <div className={stepContentClassName}>
                <Form.Item
                    name={['steps', 'direction']}
                    label="direction"
                    initialValue={'BOTH'}
                    tooltip={description.stepsObj.direction}
                >
                    <Select
                        allowClear
                        options={directionOptions}
                    />
                </Form.Item>
                <Form.Item
                    name={['steps', 'max_degree']}
                    label="max_degree"
                    initialValue={10000}
                    tooltip={description.stepsObj.max_degree}
                    rules={[{validator: integerValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    name={['steps', 'skip_degree']}
                    label="skip_degree"
                    initialValue={0}
                    tooltip={description.stepsObj.skip_degree}
                    rules={[{validator: integerValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <StepsItems param={'steps'} type={'edge_steps'} desc={description.edge_steps} />
                <StepsItems param={'steps'} type={'vertex_steps'} desc={description.vertex_steps} />
            </div>
        </>
    );
};

export default StepFormItem;
