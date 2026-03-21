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
 * @file AllPaths算法
 * @author gouzixing@
 */

import React, {useState, useCallback, useContext} from 'react';
import {Input, Form, Collapse, Select, Tooltip, InputNumber} from 'antd';
import {ClusterOutlined, QuestionCircleOutlined, DownOutlined, RightOutlined} from '@ant-design/icons';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import VerticesItems from '../../VerticesItems';
import MaxDepthItem from '../../MaxDepthItem';
import NearestItem from '../../NearestItem';
import MaxDegreeItem from '../../MaxDegreeItem';
import CapacityItem from '../../CapacityItem';
import LimitItem from '../../LimitItem';
import _ from 'lodash';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import getNodesFromParams from '../../../../../utils/getNodesFromParams';
import {maxDepthValidator, integerValidator, propertiesValidator,
    formatPropertiesValue, formatVerticesValue} from '../../utils';
import {GRAPH_STATUS, Algorithm_Url, ALGORITHM_NAME} from '../../../../../utils/constants';
import GraphAnalysisContext from '../../../../Context';
import classnames from 'classnames';
import s from '../OltpItem/index.module.scss';

const {ALLPATHS} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;

const directionOptions = [
    {label: '出边', value: 'OUT'},
    {label: '入边', value: 'IN'},
    {label: '双边', value: 'BOTH'},
];// TODO WORK AT HERE

const algorithmDescription = '根据起始顶点、目的顶点、步骤（step）和最大深度等条件查找所有路径';

const AllPaths = props => {
    const {
        handleFormSubmit,
        searchValue,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } = props;

    const {graphSpace, graph} = useContext(GraphAnalysisContext);
    const [isEnableRun, setEnableRun] = useState(false);
    const [isRequiring, setRequiring] = useState(false);
    const [stepVisible, setStepVisible] = useState(false);


    const stepContentClassName = classnames(
        s.stepContent,
        {[s.contentHidden]: !stepVisible}
    );

    const [crosspointsForm] = Form.useForm();

    const changeStepVisibleState = useCallback(() => {
        setStepVisible(pre => !pre);
    }, []);

    const handleSubmit = useCallback(
        async algorithmParams => {
            setRequiring(true);
            updateCurrentAlgorithm(ALLPATHS);
            handleFormSubmit(LOADING);
            algorithmParams = {...algorithmParams, 'algorithmName': Algorithm_Url[ALLPATHS]};
            const filteredParams = removeNilKeys(algorithmParams);
            const response =  await api.analysis.runOltpInfo(graphSpace, graph, filteredParams);
            const {data, status, message} = response || {};
            const {graph_view, pathnum} = data || {};
            const {vertices} = graph_view || {};
            if (status !== 200) {
                handleFormSubmit(FAILED, {}, message);
            }
            else {
                const sourceIds = getNodesFromParams(algorithmParams.sources, vertices);
                const targetIds = getNodesFromParams(algorithmParams.targets, vertices);
                const options = {
                    endPointsId: {startNodes: [...sourceIds], endNodes: [...targetIds]},
                    pathNum: pathnum,
                };
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
            const {sources, targets, step} = value;
            const sourcesValue = formatVerticesValue(sources);
            const targetsValue = formatVerticesValue(targets);
            const {properties} = step;
            const sumbitValues = {
                ...value,
                sources: sourcesValue,
                targets: targetsValue,
                step: {...step, properties: formatPropertiesValue(properties)},
            };
            handleSubmit(sumbitValues);
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

    const stepFormItems = (
        <div>
            <div className={s.stepHeader} onClick={changeStepVisibleState}>
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
                <Form.Item
                    name={['step', 'direction']}
                    label="direction"
                    initialValue='BOTH'
                    tooltip="起始顶点向外发散的方向(出边，入边，双边)"
                >
                    <Select placeholder="顶点向外发散的方向" allowClear options={directionOptions} />
                </Form.Item>
                <Form.Item
                    name={['step', 'label']}
                    label="label"
                    tooltip="边的类型, 默认代表所有edge label"
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    name={['step', 'properties']}
                    label="properties"
                    tooltip={`属性Map，key为属性名(String类型)，value为属性值(类型由schema定义决定)。注意：properties中的属性值可以是列表，
                    表示只要key对应的value在列表中就可以`}
                    rules={[{validator: propertiesValidator}]}
                >
                    <Input.TextArea />
                </Form.Item>
                <MaxDegreeItem isRequired={false} initialValue={10000} validator={integerValidator} />
                <Form.Item
                    name={['step', 'skip_degree']}
                    label="skip_degree"
                    initialValue={0}
                    rules={[{validator: integerValidator}]}
                    tooltip={`用于设置查询过程中舍弃超级顶点的最小边数，即当某个顶点的邻接边数目大于 skip_degree 时，完全舍弃该顶点。选填项，如果开启时，需满足
                    skip_degree >= max_degree 约束，默认为0 (不启用)，表示不跳过任何点 (注意: 开启此配置后，遍历时会尝试访问一个顶点的 skip_degree 条
                    边，而不仅仅是 max_degree 条边，这样有额外的遍历开销，对查询性能影响可能有较大影响，请确认理解后再开启)`}
                >
                    <InputNumber />
                </Form.Item>
            </div>
        </div>
    );

    return (
        <Collapse.Panel
            header={
                <AlgorithmNameHeader
                    icon={<ClusterOutlined />}
                    name={ALLPATHS}
                    searchValue={searchValue}
                    description={algorithmDescription}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    highlightName={currentAlgorithm === ALLPATHS}
                />
            }
            {...props}
        >
            <Form
                form={crosspointsForm}
                onFinish={onFormFinish}
                onValuesChange={_.debounce(onFormValuesChange, 300)}
                layout="vertical"
                className={s.oltpForms}
            >
                <VerticesItems name="sources" desc='起始顶点' />
                <VerticesItems name="targets" desc='终止顶点' />
                {stepFormItems}
                <MaxDepthItem validator={maxDepthValidator} />
                <NearestItem />
                <CapacityItem />
                <LimitItem initialValue={10} desc='返回的路径的最大条数' />
            </Form>
        </Collapse.Panel>
    );
};

export default AllPaths;
