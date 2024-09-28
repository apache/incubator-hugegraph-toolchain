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
 * @file 查找最短路径
 * @author
 */

import React, {useState, useCallback, useContext} from 'react';
import {Input, Form, Collapse, Select, InputNumber} from 'antd';
import {ForkOutlined} from '@ant-design/icons';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, Algorithm_Url, ALGORITHM_NAME} from '../../../../../utils/constants';
import {positiveIntegerValidator, maxDegreeValidator, skipDegreeValidator} from '../../utils';
import GraphAnalysisContext from '../../../../Context';
import _ from 'lodash';

const {FINDSHORTESTPATH} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;
const directionOptions = [
    {label: '出边', value: 'OUT'},
    {label: '入边', value: 'IN'},
    {label: '双边', value: 'BOTH'},
];

const description = {
    source: '起始顶点id',
    target: '目的顶点id',
    direction: '起始顶点向外发散的方向(出边，入边，双边)',
    max_depth: '最大步数',
    label: '边的类型, 默认代表所有edge label',
    max_degree: '查询过程中，单个顶点遍历的最大邻接边数目',
    skip_degree: `用于设置查询过程中舍弃超级顶点的最小边数，即当某个顶点的邻接边数目大于 skip_degree 时，
    完全舍弃该顶点。选填项，如果开启时，需满足 skip_degree >= max_degree 约束，默认为0 (不启用)，表示不跳过任何点 (注意: 开启此配置后，
    遍历时会尝试访问一个顶点的 skip_degree 条边，而不仅仅是 max_degree 条边，这样有额外的遍历开销，对查询性能影响可能有较大影响，请确认理解后再开启)`,
    capacity: '遍历过程中最大的访问的顶点数目',
};

const initialValue = {
    direction: 'BOTH',
    max_degree: 10000,
    skip_degree: 0,
    capacity: 10000000,
};
const algorithmDescription = '根据起始顶点、目的顶点、方向、边的类型（可选）和最大深度，查找两点间所有的最短路径';

const FindShortestPath = props => {
    const {
        handleFormSubmit,
        searchValue,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } = props;

    const {graphSpace, graph} = useContext(GraphAnalysisContext);
    const [form] = Form.useForm();
    const [isEnableRun, setEnableRun] = useState(false);
    const [isRequiring, setRequiring] = useState(false);

    const handleSubmit = useCallback(
        async algorithmParams => {
            setRequiring(true);
            updateCurrentAlgorithm(FINDSHORTESTPATH);
            handleFormSubmit(LOADING);
            algorithmParams = {...algorithmParams, 'algorithmName': Algorithm_Url[FINDSHORTESTPATH]};
            const filteredParams = removeNilKeys(algorithmParams);
            const response =  await api.analysis.runOltpInfo(graphSpace, graph, filteredParams);
            const {data, status, message} = response || {};
            const {graph_view} = data || {};
            if (status !== 200) {
                handleFormSubmit(FAILED, {}, message);
            }
            else {
                handleFormSubmit(SUCCESS, graph_view || {}, message, {});
            }
            setRequiring(false);
        },
        [graph, graphSpace, handleFormSubmit, updateCurrentAlgorithm]
    );

    const handleRunning = useCallback(
        e => {
            e.stopPropagation();
            form.submit();
        },
        [form]
    );

    const onFormFinish = useCallback(
        value => {
            handleSubmit(value);
        },
        [handleSubmit]
    );

    const onFormValuesChange = useCallback(
        () => {
            form.validateFields()
                .then(() => {
                    setEnableRun(true);
                })
                .catch(() => {
                    setEnableRun(false);
                });
        },
        [form]
    );

    return (
        <Collapse.Panel
            header={
                <AlgorithmNameHeader
                    icon={<ForkOutlined />}
                    name={FINDSHORTESTPATH}
                    searchValue={searchValue}
                    description={algorithmDescription}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    highlightName={currentAlgorithm === FINDSHORTESTPATH}
                />
            }
            {...props}
        >
            <Form
                form={form}
                onFinish={onFormFinish}
                onValuesChange={_.debounce(onFormValuesChange, 300)}
                layout="vertical"
                initialValues={initialValue}
            >
                <Form.Item
                    label='source'
                    name='source'
                    tooltip={description.source}
                    rules={[{required: true}]}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    name='target'
                    label="target"
                    tooltip={description.target}
                    rules={[{required: true}]}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    name='direction'
                    label="direction"
                    tooltip={description.direction}
                >
                    <Select options={directionOptions} allowClear />
                </Form.Item>
                <Form.Item
                    name='max_depth'
                    label="max_depth"
                    tooltip={description.max_depth}
                    rules={[{required: true}, {validator: positiveIntegerValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    name='label'
                    label="label"
                    tooltip={description.label}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    name='max_degree'
                    label="max_degree"
                    tooltip={description.max_degree}
                    rules={[{validator: maxDegreeValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    name='skip_degree'
                    label="skip_degree"
                    tooltip={description.skip_degree}
                    rules={[{validator: skipDegreeValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    name='capacity'
                    label="capacity"
                    tooltip={description.capacity}
                    rules={[{validator: maxDegreeValidator}]}
                >
                    <InputNumber />
                </Form.Item>
            </Form>
        </Collapse.Panel>
    );
};

export default FindShortestPath;
