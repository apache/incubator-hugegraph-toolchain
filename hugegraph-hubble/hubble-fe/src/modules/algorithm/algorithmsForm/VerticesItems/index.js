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
 * @file VerticesItem封装
 * @author gouzixing@
 */

import React, {useState, useCallback} from 'react';
import {Form, Input, Tooltip} from 'antd';
import {RightOutlined, DownOutlined, QuestionCircleOutlined} from '@ant-design/icons';
import {propertiesValidator} from '../utils';
import classnames from 'classnames';
import c from './index.module.scss';

const VerticesItems = props => {
    const {name, desc} = props;

    const [itemVisible, setItemVisible] = useState(false);

    const verticesContentClassName = classnames(
        c.verticesItemsContent,
        {[c.contentHidden]: !itemVisible}
    );

    const changeItemVisibleState = useCallback(() => {
        setItemVisible(pre => !pre);
    }, []
    );

    return (
        <div className={c.verticesItems}>
            <div className={c.stepHeader} onClick={changeItemVisibleState}>
                <div className={c.stepIcon}>
                    {itemVisible ? <DownOutlined /> : <RightOutlined />}
                </div>
                <div className={c.verticesItemsTitle}>{name}:</div>
                <div className={c.tooltip}>
                    <Tooltip
                        placement="rightTop"
                        title={desc}
                    >
                        <QuestionCircleOutlined />
                    </Tooltip>
                </div>
            </div>
            <div className={verticesContentClassName}>
                <Form.Item
                    label="ids"
                    name={[name, 'ids']}
                    rules={[
                        formInstance => ({
                            validator(_, value) {
                                const label = formInstance.getFieldValue([name, 'label']);
                                const properties = formInstance.getFieldValue([name, 'properties']);
                                if (value) {
                                    return Promise.resolve();
                                }
                                else if (label && properties) {
                                    return Promise.resolve();
                                }
                                return Promise.reject(new Error('未指定ids或label和properties的联合查询'));
                            },
                        }),
                    ]}
                    tooltip="通过顶点id列表提供起始(或终止)顶点，如果没有指定ids，则使用label和properties的联合条件查询起始(或终止)顶点"
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    label="label"
                    name={[name, 'label']}
                    tooltip="顶点的类型，ids参数为空，label和properties参数才会生效"
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    label="properties"
                    name={[name, 'properties']}
                    tooltip={'属性Map，key为属性名(String类型)，value为属性值(类型由schema定义决定)。注意：properties中的属性值可以是列表，表'
                    + '示只要key对应的value在列表中就可以'}
                    rules={[{validator: propertiesValidator}]}
                >
                    <Input.TextArea />
                </Form.Item>
            </div>
        </div>
    );
};

export default VerticesItems;
