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
 * @file MultiNodesShortestPath
 * @author
 */

import React, {useState, useCallback, useContext} from 'react';
import {Input, Form, Collapse, Select, Tooltip, InputNumber} from 'antd';
import {DownOutlined, RightOutlined, SubnodeOutlined, QuestionCircleOutlined} from '@ant-design/icons';
import VerticesItems from '../../VerticesItems';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import GraphAnalysisContext from '../../../../Context';
import * as api from '../../../../../api';
import getNodesFromParams from '../../../../../utils/getNodesFromParams';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, Algorithm_Url, ALGORITHM_NAME} from '../../../../../utils/constants';
import {integerValidator, positiveIntegerValidator, maxDegreeValidator, skipDegreeValidator,
    formatVerticesValue} from '../../utils';
import _ from 'lodash';
import s from '../OltpItem/index.module.scss';
import classnames from 'classnames';

const {MULTINODESSHORTESTPATH} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;
const directionOptions = [
    {label: '出边', value: 'OUT'},
    {label: '入边', value: 'IN'},
    {label: '双边', value: 'BOTH'},
];

const description = {
    step: {
        direction: '起始顶点向外发散的方向(出边，入边，双边)',
        label: '边的类型, 默认代表所有edge label',
        max_degree: '查询过程中，单个顶点遍历的最大邻接边数目',
        skip_degree: `用于设置查询过程中舍弃超级顶点的最小边数，即当某个顶点的邻接边数目大于 skip_degree 时，完全舍弃该顶点。选填项，如果开启时，需满足 skip_degree >= max_d
         egree约束，默认为0 (不启用)，表示不跳过任何点 (注意: 开启此配置后，遍历时会尝试访问一个顶点的 skip_degree 条边，而不仅仅是 max_degree 条边，这样有额外的遍历开销，对查询
         性能影响可能有较大影响，请确认理解后再开启)`,
    },
    max_depth: '步数',
    capacity: '遍历过程中最大的访问的顶点数目',
};

const initialValue = {
    step: {
        direction: 'BOTH',
        max_degree: 10000,
        skip_degree: 0,
    },
    capacity: 10000000,
};
const algorithmDescription = '查找指定顶点集两两之间的最短路径';

const MultiNodesShortestPath = props => {
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

    const handleSubmit = useCallback(
        async algorithmParams => {
            setRequiring(true);
            updateCurrentAlgorithm(MULTINODESSHORTESTPATH);
            handleFormSubmit(LOADING);
            algorithmParams = {...algorithmParams, 'algorithmName': Algorithm_Url[MULTINODESSHORTESTPATH]};
            const filteredParams = removeNilKeys(algorithmParams);
            const response =  await api.analysis.runOltpInfo(graphSpace, graph, filteredParams);
            const {data, status, message} = response || {};
            const {graph_view} = data || {};
            if (status !== 200) {
                handleFormSubmit(FAILED, {}, message);
            }
            else {
                const multiVertices = getNodesFromParams(algorithmParams.vertices, algorithmParams.vertices);
                const options = {endPointsId: {startNodes: [...multiVertices], endNodes: []}};
                handleFormSubmit(SUCCESS, graph_view || {}, message, options);
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
            const {vertices, step = {}} = value;
            const {label} = step;
            const verticesValue = formatVerticesValue(vertices);
            const sumbitValues = {
                ...value,
                vertices: verticesValue,
                step: {...step, label: label && label.split(',')},
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

    const stepFormItems = () => {
        return (
            <>
                <Form.Item
                    name={['step', 'direction']}
                    label="direction"
                    tooltip={description.step.direction}
                >
                    <Select
                        allowClear
                        options={directionOptions}
                    />
                </Form.Item>
                <Form.Item
                    name={['step', 'label']}
                    label="label"
                    tooltip={description.step.label}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    name={['step', 'max_degree']}
                    label="max_degree"
                    tooltip={description.step.max_degree}
                    rules={[{validator: integerValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    name={['step', 'skip_degree']}
                    label="skip_degree"
                    rules={[{validator: skipDegreeValidator}]}
                    tooltip={description.step.skip_degree}
                >
                    <InputNumber />
                </Form.Item>
            </>
        );
    };

    const renderSteps = () => {
        return (
            <div>
                <div className={s.stepHeader} onClick={changeStepVisible}>
                    <div className={s.stepIcon}>
                        {stepVisible ? <DownOutlined /> : <RightOutlined />}
                    </div>
                    <div className={s.stepTitle}>step:</div>
                    <div className={s.tooltip}>
                        <Tooltip
                            placement="rightTop"
                            title='表示从起始顶点到终止顶点走过的路径'
                        >
                            <QuestionCircleOutlined />
                        </Tooltip>
                    </div>
                </div>
                <div className={stepContentClassName}>
                    {stepFormItems()}
                </div>
            </div>
        );
    };

    return (
        <Collapse.Panel
            header={
                <AlgorithmNameHeader
                    icon={<SubnodeOutlined />}
                    name={MULTINODESSHORTESTPATH}
                    searchValue={searchValue}
                    description={algorithmDescription}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    highlightName={currentAlgorithm === MULTINODESSHORTESTPATH}
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
                initialValues={initialValue}
            >
                <VerticesItems name='vertices' desc='起始顶点' />
                {renderSteps()}
                <Form.Item
                    name='max_depth'
                    label="max_depth"
                    tooltip={description.max_depth}
                    rules={[{required: true}, {validator: positiveIntegerValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    name='capacity'
                    label="capacity"
                    rules={[{validator: maxDegreeValidator}]}
                    tooltip={description.capacity}
                >
                    <InputNumber />
                </Form.Item>
            </Form>
        </Collapse.Panel>
    );
};

export default MultiNodesShortestPath;
