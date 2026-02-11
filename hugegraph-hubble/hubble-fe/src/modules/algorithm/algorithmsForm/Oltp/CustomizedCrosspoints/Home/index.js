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
 * @file CustomizedCrosspoints算法
 * @author
 */

import React, {useState, useCallback, useContext} from 'react';
import {Form, Collapse, InputNumber} from 'antd';
import {NodeIndexOutlined} from '@ant-design/icons';
import VerticesItems from '../../../VerticesItems';
import AlgorithmNameHeader from '../../../AlgorithmNameHeader';
import PathPatternsFormItems from '../PathPatternForm';
import * as api from '../../../../../../api';
import getNodesFromParams from '../../../../../../utils/getNodesFromParams';
import removeNilKeys from '../../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, Algorithm_Url, ALGORITHM_NAME} from '../../../../../../utils/constants';
import {maxDegreeValidator, formatPropertiesValue, formatVerticesValue} from '../../../utils';
import GraphAnalysisContext from '../../../../../Context';
import _ from 'lodash';
import s from '../../OltpItem/index.module.scss';

const {CUSTOMIZED_CROSSPOINTS} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;

const description = {
    path_patterns: '表示从起始顶点走过的路径规则，是一组规则的列表',
    capacity: '遍历过程中最大的访问的顶点数目',
    limit: '返回的路径的最大数目',
    direction: '边的方向(OUT,IN,BOTH)，默认是BOTH',
    labels: '边的类型列表',
    properties: '通过属性的值过滤边',
    max_degree: `查询过程中，单个顶点遍历的最大邻接边数目，默认为 10000 
    (注: 0.12版之前 step 内仅支持 degree 作为参数名, 0.12开始统一使用 max_degree, 并向下兼容 degree 写法)`,
};
const algorithmDescription = '根据一批起始顶点、多种边规则（包括方向、边的类型和属性过滤）和最大深度等条件查找符合条件的所有的路径终点的交集';

const CustomizedCrosspoints = props => {
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
            updateCurrentAlgorithm(CUSTOMIZED_CROSSPOINTS);
            handleFormSubmit(LOADING);
            algorithmParams = {...algorithmParams, 'algorithmName': Algorithm_Url[CUSTOMIZED_CROSSPOINTS]};
            const filteredParams = removeNilKeys(algorithmParams);
            const response =  await api.analysis.runOltpInfo(graphSpace, graph, filteredParams);
            const {data, status, message} = response || {};
            const {graph_view} = data || {};
            if (status !== 200) {
                handleFormSubmit(FAILED, {}, message);
            }
            else {
                const graphNode = graph_view.vertices;
                const newVertexs = getNodesFromParams(algorithmParams.sources, graphNode);
                const options = {endPointsId: {startNodes: [...newVertexs], endNodes: []}};
                handleFormSubmit(SUCCESS, graph_view || {}, message, options);
            }
            setRequiring(false);
        },
        [graph, graphSpace, handleFormSubmit, updateCurrentAlgorithm]
    );

    const onFormFinish = useCallback(
        value => {
            const {sources, path_patterns} = value;
            const sourcesValue = formatVerticesValue(sources);
            const {steps} = path_patterns;
            const copySteps = _.cloneDeep(steps);
            copySteps.forEach(item => {
                const {labels, properties} = item;
                item.labels = labels?.split(','),
                item.properties = formatPropertiesValue(properties);
            });
            let sumbitValues = {
                ...value,
                sources: sourcesValue,
                path_patterns: [{steps: copySteps}],
            };
            handleSubmit(sumbitValues);
        },
        [handleSubmit]
    );

    const onFormValuesChange = useCallback(() => {
        form.validateFields()
            .then(() => {
                setEnableRun(true);
            })
            .catch(() => {
                setEnableRun(false);
            });
    }, [form]);


    return (
        <Collapse.Panel
            header={
                <AlgorithmNameHeader
                    icon={<NodeIndexOutlined />}
                    name={CUSTOMIZED_CROSSPOINTS}
                    description={algorithmDescription}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === CUSTOMIZED_CROSSPOINTS}
                />
            }
            {...props}
        >
            <Form
                form={form}
                onFinish={onFormFinish}
                onValuesChange={_.debounce(onFormValuesChange, 300)}
                layout="vertical"
                className={s.oltpForms}
            >
                <VerticesItems name="sources" desc='起始顶点' />
                <PathPatternsFormItems />
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
            </Form>
        </Collapse.Panel>
    );
};

export default CustomizedCrosspoints;
