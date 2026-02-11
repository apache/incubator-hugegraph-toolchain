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
 * @file Egonet算法
 * @author
 */

import React, {useState, useCallback, useContext} from 'react';
import {Input, Form, Collapse, Select, Tooltip, InputNumber} from 'antd';
import {GatewayOutlined, DownOutlined, RightOutlined, QuestionCircleOutlined} from '@ant-design/icons';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import StepsItems from '../../StepsItems';
import MaxDegreeItem from '../../MaxDegreeItem';
import LimitItem from '../../LimitItem';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, Algorithm_Url, ALGORITHM_NAME} from '../../../../../utils/constants';
import {positiveIntegerValidator, skipDegreeValidator, integerValidator, formatPropertiesValue} from '../../utils';
import GraphAnalysisContext from '../../../../Context';
import classnames from 'classnames';
import _ from 'lodash';
import s from '../OltpItem/index.module.scss';

const {EGONET} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;
const directionOptions = [
    {label: '出边', value: 'OUT'},
    {label: '入边', value: 'IN'},
    {label: '双边', value: 'BOTH'},
];

const description = {
    sources: '起始顶点 id 集合，支持传入多个不同顶点',
    max_degree: '查询过程中，单个顶点遍历的最大邻接边数目',
    skip_degree: `用于设置查询过程中舍弃超级顶点的最小边数，即当某个顶点的邻接边数目大于 skip_degree 时，
    完全舍弃该顶点。选填项，如果开启时，需满足 skip_degree >= max_degree 约束，默认为0 (不启用)，
    表示不跳过任何点 (注意: 开启此配置后，遍历时会尝试访问一个顶点的 skip_degree 条边，
    而不仅仅是 max_degree 条边，这样有额外的遍历开销，对查询性能影响可能有较大影响，请确认理解后再开启)`,
    max_depth: '步数',
    limit: '返回的交点的最大数目',
    steps: '从起始点出发的Step集合',
};

const steps = {
    direction: '起始顶点向外发散的方向',
    max_degree: '查询过程中，单个顶点遍历的最大邻接边数目(注: 0.12版之前 step 内仅支持 degree 作为参数名, 0.12开始统一使用 max_degree, 并向下兼容 degree 写法)',
    skip_degree: `用于设置查询过程中舍弃超级顶点的最小边数，即当某个顶点的邻接边数目大于 skip_degree 时，
    完全舍弃该顶点。选填项，如果开启时，需满足 skip_degree >= max_degree 约束，默认为0 (不启用)，
    表示不跳过任何点 (注意: 开启此配置后，遍历时会尝试访问一个顶点的 skip_degree 条边，
    而不仅仅是 max_degree 条边，这样有额外的遍历开销，对查询性能影响可能有较大影响，请确认理解后再开启)`,
    edge_steps: '边Step集合',
    vertex_steps: '点Step集合',
};
const algorithmDescription = '查找一批顶点的 K 层邻居, 并支持针对不同点 / 边 Label 的过滤条件, 结果中会包含起始顶点';

const Egonet = props => {
    const {
        handleFormSubmit,
        searchValue,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } = props;

    const {graphSpace, graph} = useContext(GraphAnalysisContext);
    const [isEnableRun, setEnableRun] = useState(false);
    const [stepVisible, setStepVisible] = useState(true);
    const [isRequiring, setRequiring] = useState(false);

    const stepContentClassName = classnames(
        s.stepContent,
        {[s.contentHidden]: !stepVisible}
    );

    const [egonetForm] = Form.useForm();
    const handleRunning = useCallback(
        e => {
            e.stopPropagation();
            egonetForm.submit();
        },
        [egonetForm]
    );

    const changeStepVisibleState = useCallback(() => {
        setStepVisible(pre => !pre);
    }, []
    );

    const handleSubmit = useCallback(
        async algorithmParams => {
            setRequiring(true);
            updateCurrentAlgorithm(EGONET);
            handleFormSubmit(LOADING);
            algorithmParams = {...algorithmParams, 'algorithmName': Algorithm_Url[EGONET]};
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

    const onFormFinish = useCallback(
        value => {
            const {sources, steps} = value;
            const {edge_steps, vertex_steps} = steps;
            const edgesSteps = _.cloneDeep(edge_steps);
            const vertexSteps = _.cloneDeep(vertex_steps);
            edgesSteps.forEach(item => {
                const {properties} = item;
                item.properties = formatPropertiesValue(properties);
            });
            vertexSteps.forEach(item => {
                const {properties} = item;
                item.properties = formatPropertiesValue(properties);
            });
            let sumbitValues = {
                ...value,
                steps: {
                    ...steps,
                    edge_steps: edgesSteps,
                    vertex_steps: vertexSteps,
                },
                sources: sources.split(','),
            };
            handleSubmit(sumbitValues);
        },
        [handleSubmit]
    );

    const onFormValuesChange = useCallback(
        () => {
            egonetForm.validateFields()
                .then(() => {
                    setEnableRun(true);
                })
                .catch(() => {
                    setEnableRun(false);
                });
        },
        [egonetForm]
    );


    const renderStepItems = param => {
        return (
            <div>
                <div className={s.stepHeader} onClick={changeStepVisibleState}>
                    <div className={s.stepIcon}>
                        {stepVisible ? <DownOutlined /> : <RightOutlined />}
                    </div>
                    <div className={s.stepTitle}>steps:</div>
                    <div className={s.tooltip}>
                        <Tooltip
                            placement="rightTop"
                            title={description.steps}
                        >
                            <QuestionCircleOutlined />
                        </Tooltip>
                    </div>
                </div>
                <div className={stepContentClassName}>
                    <Form.Item
                        name={[param, 'direction']}
                        label="direction"
                        initialValue='BOTH'
                        tooltip={steps.direction}
                    >
                        <Select
                            placeholder='起始顶点向外发散的方向'
                            allowClear
                            options={directionOptions}
                        />
                    </Form.Item>
                    <Form.Item
                        name={[param, 'max_degree']}
                        label="max_degree"
                        initialValue='10000'
                        rules={[{validator: integerValidator}]}
                        tooltip={steps.max_degree}
                    >
                        <InputNumber />
                    </Form.Item>
                    <Form.Item
                        name={[param, 'skip_degree']}
                        label="skip_degree"
                        initialValue={0}
                        tooltip={steps.skip_degree}
                        rules={[{validator: skipDegreeValidator}]}
                    >
                        <InputNumber />
                    </Form.Item>
                    <StepsItems param={param} type='edge_steps' desc={steps.edge_steps} />
                    <StepsItems param={param} type='vertex_steps' desc={steps.vertex_steps} />
                </div>
            </div>
        );
    };

    return (
        <Collapse.Panel
            header={
                <AlgorithmNameHeader
                    icon={<GatewayOutlined />}
                    name={EGONET}
                    description={algorithmDescription}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === EGONET}
                />
            }
            {...props}
        >
            <Form
                form={egonetForm}
                onFinish={onFormFinish}
                onValuesChange={_.debounce(onFormValuesChange, 300)}
                className={s.oltpForms}
                layout="vertical"
            >
                <Form.Item
                    label='sources'
                    name='sources'
                    rules={[{required: true}]}
                    tooltip={description.sources}
                >
                    <Input />
                </Form.Item>
                {renderStepItems('steps')}
                <MaxDegreeItem
                    isRequired
                    initialValue={10000}
                    validator={integerValidator}
                />
                <Form.Item
                    label='skip_degree'
                    name='skip_degree'
                    initialValue='0'
                    rules={[{validator: skipDegreeValidator}]}
                    tooltip={description.skip_degree}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='max_depth'
                    name='max_depth'
                    rules={[{required: true}, {validator: positiveIntegerValidator}]}
                    tooltip={description.max_depth}
                >
                    <InputNumber />
                </Form.Item>
                <LimitItem
                    initialValue='10000000'
                    desc={description.limit}
                />
            </Form>
        </Collapse.Panel>
    );
};

export default Egonet;
