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
 * @file CustomizedPaths
 * @author
 */

import React, {useState, useCallback, useContext} from 'react';
import {Form, Collapse, Select, InputNumber} from 'antd';
import {ProfileOutlined} from '@ant-design/icons';
import * as api from '../../../../../../api';
import getNodesFromParams from '../../../../../../utils/getNodesFromParams';
import removeNilKeys from '../../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, Algorithm_Url, ALGORITHM_NAME} from '../../../../../../utils/constants';
import {integerValidator, maxDegreeValidator, formatPropertiesValue, formatVerticesValue} from '../../../utils';
import GraphAnalysisContext from '../../../../../Context';
import VerticesItems from '../../../VerticesItems';
import StepItem from '../StepItem';
import AlgorithmNameHeader from '../../../AlgorithmNameHeader';
import _ from 'lodash';
import s from '../../OltpItem/index.module.scss';

const {CUSTOMIZEDPATHS} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;

const sortByOptions = [
    {label: '不排序', value: 'NONE'},
    {label: '按照路径权重的升序', value: 'INCR'},
    {label: '按照路径权重的降序', value: 'DECR'},
];
const description = {
    max_depth: '步数',
    sort_by: '根据路径的权重排序：NONE表示不排序，INCR表示按照路径权重的升序排序，DECR表示按照路径权重的降序排序',
    capacity: '遍历过程中最大的访问的顶点数目',
    limit: '返回的路径的最大条数',
};
const algorithmDescription = '根据一批起始顶点、边规则（包括方向、边的类型和属性过滤）和最大深度等条件查找符合条件的所有的路径';

const CustomizedPaths = props => {
    const {
        handleFormSubmit,
        searchValue,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } = props;

    const {graphSpace, graph} = useContext(GraphAnalysisContext);
    const [isEnableRun, setEnableRun] = useState(false);
    const [isRequiring, setRequiring] = useState(false);
    const [form] = Form.useForm();

    const handleRunning = useCallback(
        e => {
            e.stopPropagation();
            form.submit();
        },
        [form]
    );

    const handleSubmit = useCallback(
        async algorithmParams => {
            setRequiring(true);
            updateCurrentAlgorithm(CUSTOMIZEDPATHS);
            handleFormSubmit(LOADING);
            algorithmParams = {...algorithmParams, 'algorithmName': Algorithm_Url[CUSTOMIZEDPATHS]};
            const filteredParams = removeNilKeys(algorithmParams);
            const response =  await api.analysis.runOltpInfo(graphSpace, graph, filteredParams);
            const {data, status, message} = response || {};
            const {graph_view} = data || {};
            if (status !== 200) {
                handleFormSubmit(FAILED, {}, message);
            }
            else {
                const vertexs = getNodesFromParams(algorithmParams.sources, algorithmParams.vertices);
                const options = {endPointsId: {startNodes: [...vertexs], endNodes: []}};
                handleFormSubmit(SUCCESS, graph_view || {}, message, options);
            }
            setRequiring(false);
        },
        [graph, graphSpace, handleFormSubmit, updateCurrentAlgorithm]
    );

    const onFormFinish = useCallback(
        value => {
            const {sources, steps} = value;
            const sourcesValue = formatVerticesValue(sources);
            const formatedSteps = steps.map(
                item => {
                    const {labels, properties} = item;
                    return {
                        ...item,
                        labels: labels && labels.split(','),
                        properties: formatPropertiesValue(properties),
                    };
                }
            );
            let sumbitValues = {
                ...value,
                sources: sourcesValue,
                steps: [...formatedSteps],
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
                    icon={<ProfileOutlined />}
                    name={CUSTOMIZEDPATHS}
                    description={algorithmDescription}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === CUSTOMIZEDPATHS}
                />
            }
            {...props}
        >
            <Form
                form={form}
                onFinish={onFormFinish}
                className={s.oltpForms}
                onValuesChange={_.debounce(onFormValuesChange, 300)}
                layout="vertical"
            >
                <VerticesItems name="sources" desc='起始顶点' />
                <StepItem />
                <Form.Item
                    label='max_depth'
                    name='max_depth'
                    rules={[{required: true}, {validator: integerValidator}]}
                    tooltip={description.max_depth}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='sort_by'
                    name='sort_by'
                    initialValue={'NONE'}
                    tooltip={description.sort_by}
                >
                    <Select options={sortByOptions} allowClear />
                </Form.Item>
                <Form.Item
                    label='capacity'
                    name='capacity'
                    initialValue={10000000}
                    rules={[{validator: maxDegreeValidator}]}
                    tooltip={description.capacity}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='limit'
                    name='limit'
                    initialValue={10}
                    rules={[{validator: maxDegreeValidator}]}
                    tooltip={description.limit}
                >
                    <InputNumber />
                </Form.Item>
            </Form>
        </Collapse.Panel>
    );
};

export default CustomizedPaths;
