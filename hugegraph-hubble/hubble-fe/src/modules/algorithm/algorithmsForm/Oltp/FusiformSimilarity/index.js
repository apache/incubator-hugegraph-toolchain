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
 * @file FusiformSimilarity算法
 * @author
 */

import React, {useState, useCallback, useContext} from 'react';
import {Input, Form, Collapse, Select, InputNumber} from 'antd';
import {MonitorOutlined} from '@ant-design/icons';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, Algorithm_Url, ALGORITHM_NAME} from '../../../../../utils/constants';
import {positiveIntegerValidator, groupPropertyValidator, maxDegreeValidator, includeZeroNumberValidator,
    alphaValidator, formatVerticesValue} from '../../utils';
import GraphAnalysisContext from '../../../../Context';
import VerticesItems from '../../VerticesItems';
import DirectionItem from '../../DirectionItem';
import _ from 'lodash';

const {FUSIFORM_SIMILARITY} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;
const algorithmDescription = '按照条件查询一批顶点对应的"梭形相似点"';
const description = {
    label: '默认代表所有edge label',
    direction: '起始顶点向外发散的方向',
    min_neighbors: `最少邻居数目，邻居数目少于这个阈值时，认为起点不具备"梭形相似点"。比如想要找一个"读者A"读过的书的"梭形相似点"，
    那么min_neighbors为100时，表示"读者A"至少要读过100本书才可以有"梭形相似点"。`,
    alpha: '相似度，代表：起点与梭形相似点的共同邻居数目占起点的全部邻居数目的比例',
    min_similars: '"梭形相似点"的最少个数，只有当起点的"梭形相似点"数目大于或等于该值时，才会返回起点及其"梭形相似点"',
    top: '返回一个起点的"梭形相似点"中相似度最高的top个，0表示全部',
    group_property: `与min_groups一起使用，当起点跟其所有的‘梭形相似点’某个属性的值有至少min_groups个不同值时，
    才会返回该起点及其‘梭形相似点’。比如为读者A推荐异地书友时，
    需要设置group_property为读者的'城市'属性，不填代表不需要根据属性过滤`,
    min_groups: '与group_property一起使用，只有group_property设置时才有意义',
    max_degree: '查询过程中，单个顶点遍历的最大邻接边数目',
    capacity: '遍历过程中最大的访问的顶点数目',
    limit: '返回的结果数目上限（一个起点及其"梭形相似点"算一个结果',
    with_intermediary: '是否返回起点及其"梭形相似点"共同关联的中间点',
};

const FusiformSimilarity = props => {
    const {
        handleFormSubmit,
        searchValue,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } = props;

    const [form] = Form.useForm();
    const {graphSpace, graph} = useContext(GraphAnalysisContext);
    const [isEnableRun, setEnableRun] = useState(false);
    const [isRequiring, setRequiring] = useState(false);

    const handleSubmit = useCallback(
        async algorithmParams => {
            setRequiring(true);
            updateCurrentAlgorithm(FUSIFORM_SIMILARITY);
            handleFormSubmit(LOADING);
            algorithmParams = {...algorithmParams, 'algorithmName': Algorithm_Url[FUSIFORM_SIMILARITY]};
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
            const {sources} = value;
            delete value.steps;
            const sourcesValue = formatVerticesValue(sources);
            let sumbitValues = {
                ...value,
                sources: sourcesValue,
            };
            handleSubmit(sumbitValues);
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
                    icon={<MonitorOutlined />}
                    name={FUSIFORM_SIMILARITY}
                    description={algorithmDescription}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === FUSIFORM_SIMILARITY}
                />
            }
            {...props}
        >
            <Form
                form={form}
                onFinish={onFormFinish}
                onValuesChange={_.debounce(onFormValuesChange, 300)}
                layout="vertical"
            >
                <VerticesItems name="sources" desc='起始顶点' />
                <Form.Item
                    label='label'
                    name='label'
                    tooltip={description.label}
                >
                    <Input />
                </Form.Item>
                <DirectionItem
                    desc={description.direction}
                />
                <Form.Item
                    label='min_neighbors'
                    name='min_neighbors'
                    rules={[{required: true}, {validator: positiveIntegerValidator}]}
                    tooltip={description.min_neighbors}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='alpha'
                    name='alpha'
                    rules={[{required: true}, {validator: alphaValidator}]}
                    tooltip={description.alpha}
                >
                    <InputNumber step={0.01} />
                </Form.Item>
                <Form.Item
                    label='min_similars'
                    name='min_similars'
                    initialValue='1'
                    rules={[{validator: positiveIntegerValidator}]}
                    tooltip={description.min_similars}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='top'
                    name='top'
                    rules={[{required: true, validator: includeZeroNumberValidator}]}
                    tooltip={description.top}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='group_property'
                    name='group_property'
                    rules={[{validator: groupPropertyValidator}]}
                    tooltip={description.group_property}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='min_groups'
                    name='min_groups'
                    rules={[{validator: positiveIntegerValidator}]}
                    tooltip={description.min_groups}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='max_degree'
                    name='max_degree'
                    initialValue='10000'
                    rules={[{validator: maxDegreeValidator}]}
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
                <Form.Item
                    label='limit'
                    name='limit'
                    initialValue='10'
                    rules={[{validator: maxDegreeValidator}]}
                    tooltip={description.limit}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='with_intermediary'
                    name='with_intermediary'
                    initialValue={false}
                    tooltip={description.with_intermediary}
                >
                    <Select
                        placeholder="请选择"
                        allowClear
                    >
                        <Select.Option value>是</Select.Option>
                        <Select.Option value={false}>否</Select.Option>
                    </Select>
                </Form.Item>
            </Form>
        </Collapse.Panel>
    );
};

export default FusiformSimilarity;
