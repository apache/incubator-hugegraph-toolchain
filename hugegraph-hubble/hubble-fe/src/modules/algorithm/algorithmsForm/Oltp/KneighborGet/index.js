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
 * @file KneighborGet算法
 * @author gouzixing@
 */

import React, {useState, useCallback, useContext} from 'react';
import {Input, Form, Collapse} from 'antd';
import {HeatMapOutlined} from '@ant-design/icons';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import DirectionItem from '../../DirectionItem';
import MaxDepthItem from '../../MaxDepthItem';
import MaxDegreeItem from '../../MaxDegreeItem';
import LabelItem from '../../LabelItem';
import LimitItem from '../../LimitItem';
import _ from 'lodash';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {maxDegreeValidator, positiveIntegerValidator} from '../../utils';
import {GRAPH_STATUS, Algorithm_Url, ALGORITHM_NAME} from '../../../../../utils/constants';
import GraphAnalysisContext from '../../../../Context';

const {K_NEIGHBOR} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;

const algorithmDescription = '根据起始顶点、方向、边的类型（可选）和深度depth，查找包括起始顶点在内、depth步之内可达的所有顶点';

const KneighborGet = props => {
    const {
        handleFormSubmit,
        searchValue,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } = props;

    const {graphSpace, graph} = useContext(GraphAnalysisContext);
    const [isEnableRun, setEnableRun] = useState(false);
    const [isRequiring, setRequiring] = useState(false);

    const [crosspointsForm] = Form.useForm();

    const handleSubmit = useCallback(
        async algorithmParams => {
            setRequiring(true);
            updateCurrentAlgorithm(K_NEIGHBOR);
            handleFormSubmit(LOADING);
            algorithmParams = {...algorithmParams, 'algorithmName': Algorithm_Url[K_NEIGHBOR]};
            const filteredParams = removeNilKeys(algorithmParams);
            const response =  await api.analysis.runOltpInfo(graphSpace, graph, filteredParams);
            const {data, status, message} = response || {};
            const {graph_view} = data || {};
            if (status !== 200) {
                handleFormSubmit(FAILED, {}, message);
            }
            else {
                const newOptions = {startId: algorithmParams.source};
                handleFormSubmit(SUCCESS, graph_view || {}, message, newOptions);
            }
            setRequiring(false);
        },
        [graph, graphSpace, handleFormSubmit, updateCurrentAlgorithm]
    );

    const handleRunning = useCallback(
        e => {
            e.stopPropagation();
            crosspointsForm.submit();
        },
        [crosspointsForm]
    );

    const onFormFinish = useCallback(
        value => {
            handleSubmit(value);
        },
        [handleSubmit]
    );

    const onFormValuesChange = useCallback(
        () => {
            crosspointsForm.validateFields()
                .then(() => {
                    setEnableRun(true);
                })
                .catch(() => {
                    setEnableRun(false);
                });
        },
        [crosspointsForm]
    );

    return (
        <Collapse.Panel
            header={
                <AlgorithmNameHeader
                    icon={<HeatMapOutlined />}
                    name={K_NEIGHBOR}
                    searchValue={searchValue}
                    description={algorithmDescription}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    highlightName={currentAlgorithm === K_NEIGHBOR}
                />
            }
            {...props}
        >
            <Form
                form={crosspointsForm}
                onFinish={onFormFinish}
                onValuesChange={_.debounce(onFormValuesChange, 300)}
                layout="vertical"
            >
                <Form.Item
                    label='source'
                    name='source'
                    rules={[{required: true}]}
                    tooltip="起始顶点id"
                >
                    <Input />
                </Form.Item>
                <DirectionItem desc='起始顶点向外发散的方向' />
                <MaxDepthItem validator={positiveIntegerValidator} />
                <LabelItem />
                <MaxDegreeItem
                    isRequired={false}
                    initialValue={10000}
                    validator={maxDegreeValidator}
                />
                <LimitItem initialValue={10000000} desc='返回的顶点的最大数目' />
            </Form>
        </Collapse.Panel>
    );
};

export default KneighborGet;
