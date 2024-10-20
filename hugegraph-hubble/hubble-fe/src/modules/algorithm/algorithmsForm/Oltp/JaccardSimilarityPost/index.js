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
 * @file JaccardSimilarityPost
 * @author
 */

import React, {useState, useCallback, useContext} from 'react';
import {Input, Form, Collapse, Select, Tooltip, InputNumber} from 'antd';
import {CrownOutlined, DownOutlined, RightOutlined, QuestionCircleOutlined} from '@ant-design/icons';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, Algorithm_Url, ALGORITHM_NAME} from '../../../../../utils/constants';
import {integerValidator, positiveIntegerValidator, maxDegreeValidator, propertiesValidator} from '../../utils';
import GraphAnalysisContext from '../../../../Context';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import _ from 'lodash';
import classnames from 'classnames';
import s from '../OltpItem/index.module.scss';

const {JACCARD_SIMILARITY_POST} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;
const directionOptions = [
    {label: '出', value: 'OUT'},
    {label: '入', value: 'IN'},
    {label: '双向', value: 'BOTH'},
];
const description = {
    vertex: '顶点id',
    top: '返回一个起点的jaccard similarity中最大的top个',
    capacity: '遍历过程中最大的访问的顶点数目',
    steps: '从起始点出发的Step集合',
    stepsObj: {
        direction: '起始顶点向外发散的方向',
        max_degree: '查询过程中，单个顶点遍历的最大邻接边数目(注: 0.12版之前 step 内仅支持 degree 作为参数名, 0.12开始统一使用 max_degree, 并向下兼容 degree 写法)',
        skip_degree: `用于设置查询过程中舍弃超级顶点的最小边数，即当某个顶点的邻接边数目大于 skip_degree 时，完全舍弃该顶点。选填项，如果开启时, 需满足 skip_
        degree >= max_degree 约束，默认为0 (不启用)，表示不跳过任何点 (注意: 开启此配置后，遍历时会尝试访问一个顶点的 skip_degree 条边，'而不仅仅是 max_degree 条边，这
        样有额外的遍历开销，对查询性能影响可能有较大影响，请确认理解后再开启)`,
        label: '边类型',
        properties: '通过属性的值过滤边',
    },
};

const initialValue = {
    top: 100,
    capacity: 10000000,
    step: {
        direction: 'BOTH',
        max_degree: 10000,
        skip_degree: 0,
    },
};
const algorithmDescription = '计算与指定顶点的jaccard similarity最大的N个点';

const JaccardSimilarityPost = props => {
    const {
        handleFormSubmit,
        searchValue,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } = props;

    const [form] = Form.useForm();
    const {graphSpace, graph} = useContext(GraphAnalysisContext);
    const [stepVisible, setStepVisible] = useState(false);
    const [isEnableRun, setEnableRun] = useState(false);
    const [isRequiring, setRequiring] = useState(false);

    const stepContentClassName = classnames(
        s.stepContent,
        {[s.contentHidden]: !stepVisible}
    );

    const handleSubmit = useCallback(
        async algorithmParams => {
            setRequiring(true);
            updateCurrentAlgorithm(JACCARD_SIMILARITY_POST);
            handleFormSubmit(LOADING);
            algorithmParams = {...algorithmParams, 'algorithmName': Algorithm_Url[JACCARD_SIMILARITY_POST]};
            const filteredParams = removeNilKeys(algorithmParams);
            const response =  await api.analysis.runOltpInfo(graphSpace, graph, filteredParams);
            const {data, status, message} = response || {};
            const {graph_view, jaccardsimilarity} = data || {};
            if (status !== 200) {
                handleFormSubmit(FAILED, {}, message);
            }
            else {
                const options = {rankObj: jaccardsimilarity};
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

    const formatPropertiesValue = useCallback(
        properties => {
            if (!properties) {
                return undefined;
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
            let sumbitValues = {
                ...value,
                step: {
                    ...value.step,
                    properties: formatPropertiesValue(value.step.properties),
                },
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

    const stepFormItems = (
        <>
            <div className={s.stepHeader} onClick={changeStepVisible}>
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
                    name={['step', 'direction']}
                    label="direction"
                    tooltip={description.stepsObj.direction}
                >
                    <Select
                        allowClear
                        options={directionOptions}
                    />
                </Form.Item>
                <Form.Item
                    name={['step', 'max_degree']}
                    label="max_degree"
                    rules={[{validator: integerValidator}]}
                    tooltip={description.stepsObj.max_degree}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    name={['step', 'skip_degree']}
                    label="skip_degree"
                    rules={[{validator: integerValidator}]}
                    tooltip={description.stepsObj.skip_degree}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label="label"
                    name={['step', 'label']}
                    tooltip={description.stepsObj.label}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    label="properties"
                    name={['step', 'properties']}
                    tooltip={description.stepsObj.properties}
                    rules={[{validator: propertiesValidator}]}
                >
                    <Input.TextArea />
                </Form.Item>
            </div>
        </>
    );

    return (
        <Collapse.Panel
            header={
                <AlgorithmNameHeader
                    icon={<CrownOutlined />}
                    name={JACCARD_SIMILARITY_POST}
                    searchValue={searchValue}
                    description={algorithmDescription}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    highlightName={currentAlgorithm === JACCARD_SIMILARITY_POST}
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
                <Form.Item
                    label='vertex'
                    name='vertex'
                    rules={[{required: true}]}
                    tooltip={description.vertex}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    name='top'
                    label="top"
                    rules={[{required: true}, {validator: positiveIntegerValidator}]}
                    tooltip={description.top}
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
                {stepFormItems}
            </Form>
        </Collapse.Panel>
    );
};

export default JaccardSimilarityPost;
