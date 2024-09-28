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
 * @file JaccardSimilarityGet算法
 * @author gouzixing@
 */

import React, {useState, useCallback, useContext} from 'react';
import {Input, Form, Collapse} from 'antd';
import {AppstoreAddOutlined} from '@ant-design/icons';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import DirectionItem from '../../DirectionItem';
import LabelItem from '../../LabelItem';
import MaxDegreeItem from '../../MaxDegreeItem';
import _ from 'lodash';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {maxDegreeValidator} from '../../utils';
import {GRAPH_STATUS, Algorithm_Url, ALGORITHM_NAME} from '../../../../../utils/constants';
import GraphAnalysisContext from '../../../../Context';

const {JACCARD_SIMILARITY} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;

const algorithmDescription = '计算两个顶点的jaccard similarity（两个顶点邻居的交集比上两个顶点邻居的并集）';

const JaccardSimilarityGet = props => {
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
            updateCurrentAlgorithm(JACCARD_SIMILARITY);
            handleFormSubmit(LOADING);
            algorithmParams = {...algorithmParams, 'algorithmName': Algorithm_Url[JACCARD_SIMILARITY]};
            const filteredParams = removeNilKeys(algorithmParams);
            const response =  await api.analysis.runOltpInfo(graphSpace, graph, filteredParams);
            const {data, status, message} = response || {};
            const {graph_view, jaccardsimilarity} = data || {};
            if (status !== 200) {
                handleFormSubmit(FAILED, {}, message);
            }
            else {
                const options = {jaccardsimilarity: jaccardsimilarity};
                handleFormSubmit(SUCCESS, graph_view || {}, message, options);
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
                    icon={<AppstoreAddOutlined />}
                    name={JACCARD_SIMILARITY}
                    searchValue={searchValue}
                    description={algorithmDescription}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    highlightName={currentAlgorithm === JACCARD_SIMILARITY}
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
                    label='vertex'
                    name='vertex'
                    rules={[{required: true}]}
                    tooltip="顶点id"
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    label='other'
                    name='other'
                    rules={[{required: true}]}
                    tooltip="另一个顶点id"
                >
                    <Input />
                </Form.Item>
                <DirectionItem desc='起始顶点向外发散的方向' />
                <LabelItem />
                <MaxDegreeItem isRequired={false} initialValue={10000} validator={maxDegreeValidator} />
            </Form>
        </Collapse.Panel>
    );
};

export default JaccardSimilarityGet;
