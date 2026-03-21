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
 * @file TemplatePaths
 * @author
 */

import React, {useState, useCallback, useContext} from 'react';
import {Form, Collapse, InputNumber} from 'antd';
import {ReconciliationOutlined} from '@ant-design/icons';
import GraphAnalysisContext from '../../../../../Context';
import * as api from '../../../../../../api';
import removeNilKeys from '../../../../../../utils/removeNilKeys';
import AlgorithmNameHeader from '../../../AlgorithmNameHeader';
import {GRAPH_STATUS, Algorithm_Url, ALGORITHM_NAME} from '../../../../../../utils/constants';
import {maxDegreeValidator, formatPropertiesValue, formatVerticesValue} from '../../../utils';
import getNodesFromParams from '../../../../../../utils/getNodesFromParams';
import VerticesItems from '../../../VerticesItems';
import StepFormItem from '../StepFormItem';
import BoolSelectItem from '../../../BoolSelectItem';
import _ from 'lodash';
import s from '../../OltpItem/index.module.scss';

const {TEMPLATEPATHS} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;

const description = {
    sources: '起始顶点',
    targets: '终止顶点',
    steps: {
        direction: '起始顶点向外发散的方向(出边，入边，双边)',
        labels: '边的类型列表',
        properties: `属性Map,通过属性的值过滤边，key为属性名(String类型)，value为属性值(类型由schema定义决定)。
        注意：properties中的属性值可以是列表，表示只要key对应的value在列表中就可以`,
        max_times: '当前step可以重复的次数，当为N时，表示从起始顶点可以经过当前step 1-N 次',
        max_degree: '查询过程中，单个顶点遍历的最大邻接边数目',
        skip_degree: `用于设置查询过程中舍弃超级顶点的最小边数，即当某个顶点的邻接边数目大于 skip_degree 时，
        完全舍弃该顶点。选填项，如果开启时，需满足 skip_degree >= max_degree 约束，默认为0 (不启用)，表示不跳过任何点 (注意: 开启此配置后，
        遍历时会尝试访问一个顶点的 skip_degree 条边，而不仅仅是 max_degree 条边，这样有额外的遍历开销，对查询性能影响可能有较大影响，请确认理解后再开启)`,
        sample: '当需要对某个step的符合条件的边进行采样时设置，-1表示不采样',
    },
    with_ring: 'true表示包含环路；false表示不包含环路',
    capacity: '遍历过程中最大的访问的顶点数目',
    limit: '返回的路径的最大条数',
};

const algorithmDescription = '根据一批起始顶点、边规则（包括方向、边的类型和属性过滤）和最大深度等条件查找符合条件的所有的路径';

const TemplatePaths = props => {
    const {
        handleFormSubmit,
        searchValue,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } = props;

    const [isEnableRun, setEnableRun] = useState(false);
    const [isRequiring, setRequiring] = useState(false);
    const {graphSpace, graph} = useContext(GraphAnalysisContext);

    const [form] = Form.useForm();

    const handleSubmit = useCallback(
        async algorithmParams => {
            setRequiring(true);
            updateCurrentAlgorithm(TEMPLATEPATHS);
            handleFormSubmit(LOADING);
            algorithmParams = {...algorithmParams, 'algorithmName': Algorithm_Url[TEMPLATEPATHS]};
            const filteredParams = removeNilKeys(algorithmParams);
            const response =  await api.analysis.runOltpInfo(graphSpace, graph, filteredParams);
            const {data, status, message} = response || {};
            const {graph_view} = data || {};
            const {vertices} = graph_view || {};
            if (status !== 200) {
                handleFormSubmit(FAILED, {}, message);
            }
            else {
                const sources = getNodesFromParams(algorithmParams.sources, vertices);
                const targets = getNodesFromParams(algorithmParams.targets, vertices);
                const options = {endPointsId: {startNodes: [...sources, ...targets], endNodes: []}};
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
            const {sources, targets, steps} = value;
            const sourcesValue = formatVerticesValue(sources);
            const targetsValue = formatVerticesValue(targets);
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
                targets: targetsValue,
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
                    icon={<ReconciliationOutlined />}
                    name={TEMPLATEPATHS}
                    description={algorithmDescription}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === TEMPLATEPATHS}
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
                <VerticesItems name="sources" desc={description.sources} />
                <VerticesItems name="targets" desc={description.targets} />
                <StepFormItem />
                <BoolSelectItem
                    name={'with_ring'}
                    desc={description.with_ring}
                />
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

export default TemplatePaths;
