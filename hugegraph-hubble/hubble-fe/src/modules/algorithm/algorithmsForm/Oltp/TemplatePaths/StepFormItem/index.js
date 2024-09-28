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
 * @file TemplatePaths
 * @author
 */

import React, {useState, useCallback} from 'react';
import {Input, Form, Select, Button, Tooltip, InputNumber} from 'antd';
import {DownOutlined, RightOutlined, PlusOutlined, QuestionCircleOutlined} from '@ant-design/icons';
import {integerValidator, propertiesValidator} from '../../../utils';
import classnames from 'classnames';
import s from '../../OltpItem/index.module.scss';

const directionOptions = [
    {label: '出边', value: 'OUT'},
    {label: '入边', value: 'IN'},
    {label: '双边', value: 'BOTH'},
];

const description = {
    sources: '起始顶点',
    targets: '终止顶点',
    steps: {
        direction: '起始顶点向外发散的方向(出边，入边，双边)',
        labels: '边的类型列表',
        properties: `属性Map,通过属性的值过滤边，key为属性名(String类型)，value为属性值(类型由schema定义决定)。
        注意：properties中的属性值可以是列表，表示只要key对应的value在列表中就可以`,
        max_times: '当前step可以重复的次数，当为N时，表示从起始顶点可以经过当前step 1-N 次',
        max_degree: '查询过程中，单个顶点遍历的最大邻接边数目',
        skip_degree: `用于设置查询过程中舍弃超级顶点的最小边数，即当某个顶点的邻接边数目大于 skip_degree 时，
        完全舍弃该顶点。选填项，如果开启时，需满足 skip_degree >= max_degree 约束，默认为0 (不启用)，表示不跳过任何点 (注意: 开启此配置后，
        遍历时会尝试访问一个顶点的 skip_degree 条边，而不仅仅是 max_degree 条边，这样有额外的遍历开销，对查询性能影响可能有较大影响，请确认理解后再开启)`,
        sample: '当需要对某个step的符合条件的边进行采样时设置，-1表示不采样',
    },
    with_ring: 'true表示包含环路；false表示不包含环路',
    capacity: '遍历过程中最大的访问的顶点数目',
    limit: '返回的路径的最大条数',
};


const StepFormItem = () => {

    const [stepVisible, setStepVisible] = useState(true);
    const stepContentClassName = classnames(
        s.stepContent,
        {[s.contentHidden]: !stepVisible}
    );

    const changeStepVisible = useCallback(() => {
        setStepVisible(pre => !pre);
    }, []
    );

    const stepFormItems = item => {
        return (
            <>
                <Form.Item
                    name={[item.name, 'direction']}
                    label="direction"
                    initialValue={'BOTH'}
                    tooltip={description.steps.direction}
                >
                    <Select options={directionOptions} allowClear />
                </Form.Item>
                <Form.Item
                    name={[item.name, 'labels']}
                    label="labels"
                    tooltip={description.steps.labels}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    name={[item.name, 'properties']}
                    label="properties"
                    tooltip={description.steps.properties}
                    rules={[{validator: propertiesValidator}]}
                >
                    <Input.TextArea />
                </Form.Item>
                <Form.Item
                    name={[item.name, 'max_times']}
                    label='max_times'
                    tooltip={description.steps.max_times}
                    rules={[{validator: integerValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    name={[item.name, 'max_degree']}
                    label='max_degree'
                    tooltip={description.steps.max_degree}
                    rules={[{validator: integerValidator}]}
                    initialValue={10000}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    name={[item.name, 'skip_degree']}
                    label='skip_degree'
                    initialValue={0}
                    rules={[{validator: integerValidator}]}
                    tooltip={description.steps.skip_degree}
                >
                    <InputNumber />
                </Form.Item>
            </>
        );
    };

    const renderStepsFormItems = () => {
        return (
            <Form.List
                name={['steps']}
                initialValue={[{}]}
            >
                {(lists, {add, remove}, {errors}) => (
                    <>
                        {
                            lists.map((item, index) => {
                                return (
                                    <div key={item.key}>
                                        {stepFormItems(item)}
                                        {lists.length > 1 ? (
                                            <Form.Item>
                                                <Button block danger onClick={() => remove(item.name)}>
                                                    删除
                                                </Button>
                                            </Form.Item>
                                        ) : null}
                                    </div>
                                );
                            })
                        }
                        <Button
                            type="dashed"
                            onClick={() => add()}
                            style={{width: '100%'}}
                            icon={<PlusOutlined />}
                        >
                            Add
                        </Button>
                    </>

                )}
            </Form.List>
        );
    };

    return (
        <div>
            <div className={s.stepHeader} onClick={changeStepVisible}>
                <div className={s.stepIcon}>
                    {stepVisible ? <DownOutlined /> : <RightOutlined />}
                </div>
                <div className={s.stepTitle}>steps:</div>
                <div className={s.tooltip}>
                    <Tooltip
                        placement="rightTop"
                        title='表示从起始顶点走过的路径规则，是一组Step的列表'
                    >
                        <QuestionCircleOutlined />
                    </Tooltip>
                </div>
            </div>
            <div className={stepContentClassName}>
                {renderStepsFormItems()}
            </div>
        </div>
    );
};

export default StepFormItem;
