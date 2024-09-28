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
 * @file Rank API
 * @author
 */

import React, {useState, useCallback, useContext} from 'react';
import {Input, Form, Collapse, Select, InputNumber} from 'antd';
import {DeleteColumnOutlined} from '@ant-design/icons';
import _ from 'lodash';
import GraphAnalysisContext from '../../../../Context';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, Algorithm_Url, ALGORITHM_NAME} from '../../../../../utils/constants';
import {positiveIntegerValidator, maxDegreeValidator, alphaValidator,
    maxDepthForRankValidator, maxDiffValidator} from '../../utils';
import BoolSelectItem from '../../BoolSelectItem';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';

const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;
const {RANK_API} = ALGORITHM_NAME;
const withLabelOptions = [
    {label: '仅保留与源顶点相同类别的顶点', value: 'SAME_LABEL'},
    {label: '仅保留与源顶点不同类别（二分图的另一端）的顶点', value: 'OTHER_LABEL'},
    {label: '同时保留与源顶点相同和相反类别的顶点', value: 'BOTH_LABEL'},
];

const description = {
    source: '源顶点id',
    label: '源点出发的某类边 label，须连接两类不同顶点',
    alpha: '每轮迭代时从某个点往外走的概率，与 PageRank 算法中的 alpha 类似',
    max_degree: '查询过程中，单个顶点遍历的最大邻接边数目',
    max_depth: '迭代次数',
    limit: '返回顶点的最大数目',
    max_diff: '提前收敛的精度差',
    sorted: '返回的结果是否根据 rank 排序',
    with_label: `筛选结果中保留哪些结果，可从三种类型选一个: SAME_LABEL：仅保留与源顶点相同类别的顶点; OTHER_LABEL：仅保留与源顶点不同类别（二分图的另
    一端）的顶点; BOTH_LABEL：同时保留与源顶点相同和相反类别的顶点`,
};

const initialValue = {
    alpha: 0.85,
    max_degree: 10000,
    max_depth: 5,
    limit: 100,
    max_diff: 0.0001,
    sorted: true,
    with_label: 'BOTH_LABEL',
};

const algorithmDescription = '根据某个点现有的出边, 推荐具有相近 / 相同关系的其他点';

const RankApi = props => {
    const {
        handleFormSubmit,
        searchValue,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } = props;

    const [form] = Form.useForm();
    const [isEnableRun, setEnableRun] = useState(false);
    const [isRequiring, setRequiring] = useState(false);
    const {graphSpace, graph} = useContext(GraphAnalysisContext);

    const handleSubmit = useCallback(
        async algorithmParams => {
            setRequiring(true);
            updateCurrentAlgorithm(RANK_API);
            handleFormSubmit(LOADING);
            algorithmParams = {...algorithmParams, 'algorithmName': Algorithm_Url[RANK_API]};
            const filteredParams = removeNilKeys(algorithmParams);
            const response =  await api.analysis.runOltpInfo(graphSpace, graph, filteredParams);
            const {data, status, message} = response || {};
            const {ranks} = data || {};
            if (status !== 200) {
                handleFormSubmit(FAILED, {}, message);
            }
            else {
                const options = {rankObj: ranks || {}};
                handleFormSubmit(SUCCESS, {}, message, options);
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
                    icon={<DeleteColumnOutlined />}
                    name={RANK_API}
                    description={algorithmDescription}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === RANK_API}
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
                    rules={[{required: true}]}
                    tooltip={description.source}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    name='label'
                    label="label"
                    rules={[{required: true}]}
                    tooltip={description.label}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    name='alpha'
                    label="alpha"
                    rules={[{validator: alphaValidator}]}
                    tooltip={description.alpha}
                >
                    <InputNumber step={0.01} />
                </Form.Item>
                <Form.Item
                    name='max_degree'
                    label="max_degree"
                    rules={[{validator: positiveIntegerValidator}]}
                    tooltip={description.max_degree}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    name='max_depth'
                    label="max_depth"
                    rules={[{validator: maxDepthForRankValidator}]}
                    tooltip={description.max_depth}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    name='limit'
                    label="limit"
                    rules={[{validator: maxDegreeValidator}]}
                    tooltip={description.limit}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    name='max_diff'
                    label="max_diff"
                    rules={[{validator: maxDiffValidator}]}
                    tooltip={description.max_diff}
                >
                    <InputNumber step={0.0001} />
                </Form.Item>
                <BoolSelectItem
                    name={'sorted'}
                    desc={description.sorted}
                />
                <Form.Item
                    name='with_label'
                    label="with_label"
                    tooltip={description.with_label}
                >
                    <Select options={withLabelOptions} allowClear />
                </Form.Item>
            </Form>
        </Collapse.Panel>
    );
};

export default RankApi;
