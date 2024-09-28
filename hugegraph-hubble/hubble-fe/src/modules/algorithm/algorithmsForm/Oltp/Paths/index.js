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
 * @file 查找所有路径（GET）
 * @author
 */

import React, {useState, useCallback, useContext} from 'react';
import {Input, Form, Collapse, InputNumber} from 'antd';
import {PullRequestOutlined} from '@ant-design/icons';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import LimitItem from '../../LimitItem';
import DirectionItem from '../../DirectionItem';
import GraphAnalysisContext from '../../../../Context';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, Algorithm_Url, ALGORITHM_NAME} from '../../../../../utils/constants';
import {maxDepthValidator, maxDegreeValidator, maxDegreeValidatorForCrossPoint} from '../../utils';
import _ from 'lodash';

const algorithmDescription = '根据起始顶点、目的顶点、方向、边的类型（可选）和最大深度等条件查找所有路径';
const {PATHS} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;
const description = {
    source: '起始顶点id',
    target: '目的顶点id',
    direction: '起始顶点向外发散的方向(出边，入边，双边)',
    max_depth: '最大步数',
    label: '边的类型，默认代表所有edge label',
    max_degree: '查询过程中，单个顶点遍历的最大邻接边数目',
    limit: '查询到的目标顶点个数，也是返回的最短路径的条数',
    capacity: '遍历过程中最大的访问的顶点数目',
};

const Paths = props => {
    const {
        handleFormSubmit,
        searchValue,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } = props;

    const [isEnableRun, setEnableRun] = useState(false);
    const [isRequiring, setRequiring] = useState(false);
    const {graphSpace, graph} = useContext(GraphAnalysisContext);
    const [pathsForm] = Form.useForm();

    const handleSubmit = useCallback(
        async algorithmParams => {
            setRequiring(true);
            updateCurrentAlgorithm(PATHS);
            handleFormSubmit(LOADING);
            algorithmParams = {...algorithmParams, 'algorithmName': Algorithm_Url[PATHS]};
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
            pathsForm.submit();
        },
        [pathsForm]
    );


    const onFormFinish = useCallback(
        value => {
            handleSubmit(value);
        },
        [handleSubmit]
    );

    const onFormValuesChange = useCallback(
        () => {
            pathsForm.validateFields()
                .then(() => {
                    setEnableRun(true);
                })
                .catch(() => {
                    setEnableRun(false);
                });
        },
        [pathsForm]
    );

    return (
        <Collapse.Panel
            header={
                <AlgorithmNameHeader
                    icon={<PullRequestOutlined />}
                    name={PATHS}
                    description={algorithmDescription}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === PATHS}
                />
            }
            {...props}
        >
            <Form
                form={pathsForm}
                onFinish={onFormFinish}
                onValuesChange={_.debounce(onFormValuesChange, 300)}
                layout="vertical"
            >
                <Form.Item
                    label='source'
                    name='source'
                    rules={[{required: true}]}
                    tooltip={description.source}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    label='target'
                    name='target'
                    rules={[{required: true}]}
                    tooltip={description.target}
                >
                    <Input />
                </Form.Item>
                <DirectionItem
                    desc={description.direction}
                />
                <Form.Item
                    label='max_depth'
                    name='max_depth'
                    rules={[{required: true}, {validator: maxDepthValidator}]}
                    tooltip={description.max_depth}
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
                    label='max_degree'
                    name='max_degree'
                    initialValue='10000'
                    rules={[{validator: maxDegreeValidatorForCrossPoint}]}
                    tooltip={description.max_degree}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='capacity'
                    name='capacity'
                    initialValue='10000000'
                    rules={[{validator: maxDegreeValidator}]}
                    tooltip={description.capacity}
                >
                    <InputNumber />
                </Form.Item>
                <LimitItem
                    initialValue='10'
                    desc={description.limit}
                />
            </Form>
        </Collapse.Panel>
    );
};

export default Paths;
