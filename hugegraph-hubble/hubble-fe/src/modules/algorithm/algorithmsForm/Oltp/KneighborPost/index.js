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
 * @file K-neighbor API(POST，高级版)
 * @author
 */

import React, {useState, useCallback, useContext} from 'react';
import {Input, Form, Collapse, Select, Tooltip, InputNumber} from 'antd';
import {DownOutlined, RightOutlined, DeleteRowOutlined, QuestionCircleOutlined} from '@ant-design/icons';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, Algorithm_Url, ALGORITHM_NAME} from '../../../../../utils/constants';
import {integerValidator, positiveIntegerValidator, maxDegreeValidator} from '../../utils';
import StepsItems from '../../StepsItems';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import GraphAnalysisContext from '../../../../Context';
import _ from 'lodash';
import classnames from 'classnames';
import s from '../OltpItem/index.module.scss';

const {KNEIGHBOR_POST} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;
const directionOptions = [
    {label: '出', value: 'OUT'},
    {label: '入', value: 'IN'},
    {label: '双向', value: 'BOTH'},
];

const description = {
    source: '起始顶点id',
    max_depth: '步数',
    limit: '返回的顶点的最大数目',
    algorithm: '遍历方式,常情况下，deep_first（深度优先搜索）方式会具有更好的遍历性能。但当参数nearest为true时，可能会包含非最近邻的节点，尤其是数据量较大时',
    steps: '从起始点出发的Step集合',
    edge_steps: '边Step集合',
    vertex_steps: '点Step集合',
    stepsObj: {
        direction: '起始顶点向外发散的方向',
        max_degree: '查询过程中，单个顶点遍历的最大邻接边数目(注: 0.12版之前 step 内仅支持 degree 作为参数名, 0.12开始统一使用 max_degree, 并向下兼容 degree 写法)',
        skip_degree: `用于设置查询过程中舍弃超级顶点的最小边数，即当某个顶点的邻接边数目大于 skip_degree 时，完全舍弃该顶点。选填项，如果开启时, 需满足 skip_degree >= 
          max_degree 约束，默认为0 (不启用)，表示不跳过任何点 (注意: 开启此配置后，遍历时会尝试访问一个顶点的 skip_degree 条边，而不仅仅是 max_degree 条边，这样有额外的遍历
          开销，对查询性能影响可能有较大影响，请确认理解后再开启)`,
        steps: {
            label: '点边类型',
            properties: '通过属性的值过滤点边',
        },
    },
};
const algorithmDescription = '根据起始顶点、步骤（包括方向、边类型和过滤属性）和深度depth，查找从起始顶点出发depth步内可达的所有顶点';

const KneighborPost = props => {
    const {
        handleFormSubmit,
        searchValue,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } = props;

    const [form] = Form.useForm();
    const {graphSpace, graph} = useContext(GraphAnalysisContext);
    const [isRequiring, setRequiring] = useState(false);
    const [isEnableRun, setEnableRun] = useState(false);
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
            updateCurrentAlgorithm(KNEIGHBOR_POST);
            handleFormSubmit(LOADING);
            algorithmParams = {...algorithmParams, 'algorithmName': Algorithm_Url[KNEIGHBOR_POST]};
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

    const formatPropertiesValue = useCallback(
        properties => {
            if (!properties) {
                return;
            }
            const propertiesArr = properties.trim().split('\n') || [];
            const propertiesValue = {};
            for (const item of propertiesArr) {
                const [key, value] = item?.split('=');
                if (key && value) {
                    const valueLength = value.length;
                    if (valueLength > 2 && value[0] === '\'' && value[valueLength - 1] === '\'') {
                        propertiesValue[key] = value.slice(1, valueLength - 1);
                    }
                    else if (!isNaN(+value)) {
                        propertiesValue[key] = +value;
                    }
                }
            }
            return propertiesValue;
        },
        []
    );

    const onFormFinish = useCallback(
        value => {
            const {steps} = value;
            const {edge_steps = [], vertex_steps = []} = steps;
            const formatedEdgeSteps = edge_steps.map(
                item => {
                    const {properties} = item;
                    return {
                        ...item,
                        properties: formatPropertiesValue(properties),
                    };
                }
            );
            const formatedNodeSteps = vertex_steps.map(
                item => {
                    const {properties} = item;
                    return {
                        ...item,
                        properties: formatPropertiesValue(properties),
                    };
                }
            );
            const formatedSteps = {
                ...steps,
                edge_steps: [...formatedEdgeSteps],
                vertex_steps: [...formatedNodeSteps],
            };
            const sumbitValues = {
                ...value,
                steps: {...formatedSteps},
            };
            handleSubmit(sumbitValues);
        },
        [formatPropertiesValue, handleSubmit]
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

    const changeStepVisible = useCallback(() => {
        setStepVisible(pre => !pre);
    }, []
    );

    const stepFormItems = () => {
        return (
            <>
                <div className={s.stepHeader} onClick={changeStepVisible}>
                    <div className={s.stepIcon}>
                        {stepVisible ? <DownOutlined /> : <RightOutlined />}
                    </div>
                    <div className={s.stepTitle}>steps:</div>
                    <div className={s.tooltip}>
                        <Tooltip
                            placement="rightTop"
                            title='从起始点出发的Step集合'
                        >
                            <QuestionCircleOutlined />
                        </Tooltip>
                    </div>
                </div>
                <div className={stepContentClassName}>
                    <Form.Item
                        name={['steps', 'direction']}
                        label="direction"
                        initialValue={'BOTH'}
                        tooltip={description.stepsObj.direction}
                    >
                        <Select
                            allowClear
                            options={directionOptions}
                        />
                    </Form.Item>
                    <Form.Item
                        name={['steps', 'max_degree']}
                        label="max_degree"
                        initialValue={10000}
                        tooltip={description.stepsObj.max_degree}
                        rules={[{validator: integerValidator}]}
                    >
                        <InputNumber />
                    </Form.Item>
                    <Form.Item
                        name={['steps', 'skip_degree']}
                        label="skip_degree"
                        initialValue={0}
                        tooltip={description.stepsObj.skip_degree}
                        rules={[{validator: integerValidator}]}
                    >
                        <InputNumber />
                    </Form.Item>
                    <StepsItems param={'steps'} type={'edge_steps'} desc={description.edge_steps} />
                    <StepsItems param={'steps'} type={'vertex_steps'} desc={description.vertex_steps} />
                </div>
            </>
        );
    };

    return (
        <Collapse.Panel
            header={
                <AlgorithmNameHeader
                    icon={<DeleteRowOutlined />}
                    name={KNEIGHBOR_POST}
                    searchValue={searchValue}
                    description={algorithmDescription}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    highlightName={currentAlgorithm === KNEIGHBOR_POST}
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
                    label='max_depth'
                    name='max_depth'
                    rules={[{required: true}, {validator: positiveIntegerValidator}]}
                    tooltip={description.max_depth}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    name='limit'
                    label="limit"
                    initialValue={10000000}
                    rules={[{validator: maxDegreeValidator}]}
                    tooltip={description.limit}
                >
                    <InputNumber />
                </Form.Item>
                {stepFormItems()}
            </Form>
        </Collapse.Panel>
    );
};

export default KneighborPost;
