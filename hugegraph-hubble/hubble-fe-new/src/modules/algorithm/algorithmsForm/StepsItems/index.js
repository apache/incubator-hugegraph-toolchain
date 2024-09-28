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
 * @file StepFormItem封装
 * @author gouzixing@
 */

import React, {useState, useCallback} from 'react';
import {Form, Input, Button, Tooltip} from 'antd';
import {RightOutlined, DownOutlined, PlusOutlined, QuestionCircleOutlined} from '@ant-design/icons';
import {propertiesValidator} from '../utils';
import classnames from 'classnames';
import c from './index.module.scss';

const description = {
    edge_steps: {
        label: '边类型',
        properties: '通过属性的值过滤边',
    },
    vertex_steps: {
        label: '点类型',
        properties: '通过属性的值过滤点',
    },
};

const StepsItems = props => {

    const {param, type, desc} = props;

    const [itemVisible, setItemVisible] = useState(true);

    const stepContentClassName = classnames(
        c.stepsItemsContent,
        {[c.contentHidden]: !itemVisible}
    );

    const changeItemVisibleState = useCallback(() => {
        setItemVisible(pre => !pre);
    }, []
    );

    const itemsContent = (name, type) => {
        return (
            <>
                <Form.Item
                    name={[name, 'label']}
                    label="label"
                    tooltip={description[type].label}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    name={[name, 'properties']}
                    label="properties"
                    tooltip={description[type].properties}
                    rules={[{validator: propertiesValidator}]}
                >
                    <Input.TextArea />
                </Form.Item>
            </>
        );
    };

    return (
        <Form.List
            name={[param, type]}
            initialValue={[{}]}
        >
            {(lists, {add, remove}, {errors}) => (
                <div className={c.stepsItemsContent}>
                    <div className={c.stepHeader} onClick={changeItemVisibleState}>
                        <div className={c.stepIcon}>
                            {itemVisible ? <DownOutlined /> : <RightOutlined />}
                        </div>
                        <div className={c.stepsItemsTitle}>{type}:</div>
                        <div className={c.tooltip}>
                            <Tooltip
                                placement="rightTop"
                                title={desc}
                            >
                                <QuestionCircleOutlined />
                            </Tooltip>
                        </div>
                    </div>
                    {

                        lists.map((item, name, index) => {
                            return (
                                <div key={item.key} className={stepContentClassName} style={{paddingLeft: '20px'}}>
                                    {itemsContent(name, type)}
                                    {lists.length > 1 ? (
                                        <Form.Item>
                                            <Button
                                                block
                                                danger
                                                onClick={() => remove(name)}
                                            >
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
                        onClick={() => {
                            add();
                            setItemVisible(true);
                        }}
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

export default StepsItems;
