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
 * @file NeighborRankApi
 * @author
 */

import React, {useState, useCallback, useContext} from 'react';
import {Input, Form, Collapse, Select, InputNumber, Button, Tooltip} from 'antd';
import {ScheduleOutlined, DownOutlined, RightOutlined, PlusOutlined, QuestionCircleOutlined} from '@ant-design/icons';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import _ from 'lodash';
import GraphAnalysisContext from '../../../../Context';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, Algorithm_Url, ALGORITHM_NAME} from '../../../../../utils/constants';
import {integerValidator, topValidator, alphaValidator, positiveIntegerValidator} from '../../utils';
import classnames from 'classnames';
import s from '../OltpItem/index.module.scss';

const {NEIGHBOR_RANK_API} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;

const directionOptions = [
    {label: '出', value: 'OUT'},
    {label: '入', value: 'IN'},
    {label: '双向', value: 'BOTH'},
];

const description = {
    source: '源顶点id',
    alpha: '每轮迭代时从某个点往外走的概率，与 PageRank 算法中的 alpha 类似',
    capacity: '遍历过程中最大的访问定点数目',
    stepsObj: {
        direction: '表示边的方向（OUT, IN, BOTH）',
        labels: '边的类型列表，多个边类型取并集',
        max_degree: '查询过程中，单个顶点遍历的最大邻接边数目',
        top: '在结果中每一层只保留权重最高的前 N 个结果',
    },
};
const algorithmDescription = '在一般图结构中，找出每一层与给定起点相关性最高的前 N 个顶点及其相关度，用图的语义理解就是：从起点往外走， 走到各层各个顶点的概率';

const NeighborRankApi = props => {
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
    const [stepVisible, setStepVisible] = useState(false);

    const stepContentClassName = classnames(
        s.stepContent,
        {[s.contentHidden]: !stepVisible}
    );

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
            updateCurrentAlgorithm(NEIGHBOR_RANK_API);
            handleFormSubmit(LOADING);
            algorithmParams = {...algorithmParams, 'algorithmName': Algorithm_Url[NEIGHBOR_RANK_API]};
            const filteredParams = removeNilKeys(algorithmParams);
            const response =  await api.analysis.runOltpInfo(graphSpace, graph, filteredParams);
            const {data, status, message} = response || {};
            const {graph_view, rankslist} = data || {};
            if (status !== 200) {
                handleFormSubmit(FAILED, {}, message);
            }
            else {
                const options = {rankArray: rankslist || []};
                handleFormSubmit(SUCCESS, graph_view || {}, message, options);
            }
            setRequiring(false);
        },
        [graph, graphSpace, handleFormSubmit, updateCurrentAlgorithm]
    );

    const onFormFinish = useCallback(
        value => {
            const {steps} = value;
            const formatedSteps = steps.map(
                item => {
                    const {labels} = item;
                    return {
                        ...item,
                        labels: labels?.split(','),
                    };
                }
            );
            let sumbitValues = {
                ...value,
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

    const changeStepVisible = useCallback(
        () => {
            setStepVisible(pre => !pre);
        },
        []
    );

    const stepFormItems = item => {
        return (
            <>
                <Form.Item
                    name={[item.name, 'direction']}
                    label="direction"
                    initialValue='BOTH'
                    tooltip={description.stepsObj.direction}
                >
                    <Select options={directionOptions} allowClear />
                </Form.Item>
                <Form.Item
                    name={[item.name, 'labels']}
                    label="labels"
                    rules={[{required: true}]}
                    tooltip={description.stepsObj.labels}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    name={[item.name, 'max_degree']}
                    label="max_degree"
                    rules={[{validator: integerValidator}]}
                    tooltip={description.stepsObj.max_degree}
                    initialValue={10000}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    name={[item.name, 'top']}
                    label="top"
                    tooltip={description.stepsObj.top}
                    rules={[{validator: topValidator}]}
                    initialValue={100}
                >
                    <InputNumber />
                </Form.Item>
            </>
        );
    };

    const renderStepsFormItems = () => {
        return (
            <Form.List
                name={['steps']}
                initialValue={[{}]}
            >
                {(lists, {add, remove}, {errors}) => (
                    <>
                        {
                            lists.map((item, index) => {
                                return (
                                    <div key={item.key}>
                                        {stepFormItems(item)}
                                        {lists.length > 1 ? (
                                            <Form.Item>
                                                <Button block danger onClick={() => remove(item.name)}>
                                                    删除
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
                    </>

                )}
            </Form.List>
        );
    };

    const renderSteps = () => {
        return (
            <div>
                <div className={s.stepHeader} onClick={changeStepVisible}>
                    <div className={s.stepIcon}>
                        {stepVisible ? <DownOutlined /> : <RightOutlined />}
                    </div>
                    <div className={s.stepTitle}>steps:</div>
                    <div className={s.tooltip}>
                        <Tooltip
                            placement="rightTop"
                            title='表示从起始顶点走过的路径规则'
                        >
                            <QuestionCircleOutlined />
                        </Tooltip>
                    </div>
                </div>
                <div className={stepContentClassName}>
                    {renderStepsFormItems()}
                </div>
            </div>
        );
    };

    return (
        <Collapse.Panel
            header={
                <AlgorithmNameHeader
                    icon={<ScheduleOutlined />}
                    name={NEIGHBOR_RANK_API}
                    description={algorithmDescription}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === NEIGHBOR_RANK_API}
                />
            }
            {...props}
        >
            <Form
                form={form}
                onFinish={onFormFinish}
                onValuesChange={_.debounce(onFormValuesChange, 300)}
                className={s.oltpForms}
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
                    name='alpha'
                    label="alpha"
                    initialValue={0.85}
                    rules={[{validator: alphaValidator}]}
                    tooltip={description.alpha}
                >
                    <InputNumber style={{width: '100%'}} step={0.01} />
                </Form.Item>
                <Form.Item
                    name='capacity'
                    label="capacity"
                    initialValue={10000000}
                    rules={[{validator: positiveIntegerValidator}]}
                    tooltip={description.interge}
                >
                    <InputNumber />
                </Form.Item>
                {renderSteps()}
            </Form>
        </Collapse.Panel>
    );
};

export default NeighborRankApi;
