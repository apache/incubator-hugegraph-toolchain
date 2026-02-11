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
 * @file CustomizedCrosspoints PathPatter
 * @author
 */

import React, {useState, useCallback} from 'react';
import {Input, Form, Button, Select, Tooltip, InputNumber} from 'antd';
import {DownOutlined, RightOutlined, PlusOutlined, QuestionCircleOutlined} from '@ant-design/icons';
import {propertiesValidator, maxDegreeValidator} from '../../../utils';
import classnames from 'classnames';
import s from '../../OltpItem/index.module.scss';

const directionOptions = [
    {label: '出边', value: 'OUT'},
    {label: '入边', value: 'IN'},
    {label: '双边', value: 'BOTH'},
];

const description = {
    path_patterns: '表示从起始顶点走过的路径规则，是一组规则的列表',
    capacity: '遍历过程中最大的访问的顶点数目',
    limit: '返回的路径的最大数目',
    direction: '边的方向(OUT,IN,BOTH)，默认是BOTH',
    labels: '边的类型列表',
    properties: '通过属性的值过滤边',
    max_degree: `查询过程中，单个顶点遍历的最大邻接边数目，默认为 10000 
    (注: 0.12版之前 step 内仅支持 degree 作为参数名, 0.12开始统一使用 max_degree, 并向下兼容 degree 写法)`,
};

const PathPatternsFormItems = () => {

    const [patternVisible, setPatternVisible] = useState(false);
    const patternContentClassName = classnames({[s.contentHidden]: !patternVisible});

    const changePatternsVisible = useCallback(() => {
        setPatternVisible(pre => !pre);
    }, []);

    const stepFormItems = item => {
        return (
            <>
                <Form.Item
                    name={[item.name, 'direction']}
                    label="direction"
                    initialValue='BOTH'
                    tooltip={description.direction}
                >
                    <Select
                        placeholder={description.direction}
                        allowClear
                        options={directionOptions}
                    />
                </Form.Item>
                <Form.Item
                    name={[item.name, 'labels']}
                    label="labels"
                    tooltip={description.labels}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    name={[item.name, 'properties']}
                    label="properties"
                    tooltip={description.properties}
                    rules={[{validator: propertiesValidator}]}
                >
                    <Input.TextArea />
                </Form.Item>
                <Form.Item
                    name={[item.name, 'max_degree']}
                    label='max_degree'
                    initialValue='10000'
                    rules={[{validator: maxDegreeValidator}]}
                    tooltip={description.max_degree}
                >
                    <InputNumber />
                </Form.Item>
            </>
        );
    };

    const renderPathPatterns = param => {
        return (
            <Form.List
                name={[param, 'steps']}
                initialValue={[{}]}
            >
                {(lists, {add, remove}, {errors}) => (
                    <div className={s.stepsItemsContent}>
                        {
                            lists.map((item, index) => {
                                return (
                                    <div key={item.key} className={s.stepContent}>
                                        {stepFormItems(item)}
                                        {lists.length > 1 ? (
                                            <Form.Item>
                                                <Button block danger onClick={() => remove(item.name)}>
                                                    Delete
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
                    </div>

                )}
            </Form.List>
        );
    };

    return (
        <>
            <div className={s.stepHeader} onClick={changePatternsVisible}>
                <div className={s.stepIcon}>
                    {patternVisible ? <DownOutlined /> : <RightOutlined />}
                </div>
                <div className={s.stepTitle}>path_patterns:</div>
                <div className={s.tooltip}>
                    <Tooltip
                        placement="rightTop"
                        title={description.path_patterns}
                    >
                        <QuestionCircleOutlined />
                    </Tooltip>
                </div>
            </div>
            <div className={patternContentClassName}>
                {renderPathPatterns('path_patterns')}
            </div>
        </>
    );
};

export default PathPatternsFormItems;
