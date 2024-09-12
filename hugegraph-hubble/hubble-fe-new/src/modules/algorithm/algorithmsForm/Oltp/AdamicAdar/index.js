/**
 * @file AdamicAdar算法
 * @author
 */

import React, {useState, useCallback, useContext} from 'react';
import {Input, Form, Collapse, Select, InputNumber} from 'antd';
import {ApiOutlined} from '@ant-design/icons';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import GraphAnalysisContext from '../../../../Context';
import _ from 'lodash';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {maxDegreeValidator} from '../../utils';
import {GRAPH_STATUS, Algorithm_Url, ALGORITHM_NAME} from '../../../../../utils/constants';

const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;
const {ADAMIC_ADAR} = ALGORITHM_NAME;
const directionOptions = [
    {label: '出边', value: 'OUT'},
    {label: '入边', value: 'IN'},
    {label: '双边', value: 'BOTH'},
];

const description = {
    vertex: '起始顶点',
    other: '终点顶点',
    direction: '起始顶点到目的顶点的方向，目的地到起始点是反方向，BOTH时不考虑方向',
    label: '默认代表所有edge label',
    max_degree: '查询过程中，单个顶点遍历的最大邻接边数目',
};

const algorithmDescription = '主要用于社交网络中判断两点紧密度的算法, 用来求两点间共同邻居密集度的一个系数';

const AdamicAdar = props => {
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
            updateCurrentAlgorithm(ADAMIC_ADAR);
            handleFormSubmit(LOADING);
            algorithmParams = {...algorithmParams, 'algorithmName': Algorithm_Url[ADAMIC_ADAR]};
            const filteredParams = removeNilKeys(algorithmParams);
            const response =  await api.analysis.runOltpInfo(graphSpace, graph, filteredParams);
            const {data, status, message} = response || {};
            const {graph_view, adamic_adar} = data || {};
            if (status !== 200) {
                handleFormSubmit(FAILED, {}, message);
            }
            else {
                const options = {jaccardsimilarity: adamic_adar};
                handleFormSubmit(SUCCESS, graph_view || {}, message, options);
            }
            setRequiring(false);
        },
        [graph, graphSpace, handleFormSubmit, updateCurrentAlgorithm]
    );

    const onFormFinish = useCallback(
        value => {
            handleSubmit(value);
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
                    icon={<ApiOutlined />}
                    name={ADAMIC_ADAR}
                    description={algorithmDescription}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === ADAMIC_ADAR}
                />
            }
            {...props}
        >
            <Form
                form={form}
                onFinish={onFormFinish}
                onValuesChange={_.debounce(onFormValuesChange, 300)}
                layout="vertical"
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
                    name='other'
                    label="other"
                    rules={[{required: true}]}
                    tooltip={description.other}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    label='direction'
                    name='direction'
                    initialValue='BOTH'
                    tooltip={description.direction}
                >
                    <Select
                        placeholder="顶点向外发散的方向"
                        allowClear
                        options={directionOptions}
                    />
                </Form.Item>
                <Form.Item
                    name='label'
                    label="label"
                    tooltip={description.label}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    label='max_degree'
                    name='max_degree'
                    initialValue='10000'
                    rules={[{validator: maxDegreeValidator}]}
                    tooltip={description.max_degree}
                >
                    <InputNumber />
                </Form.Item>
            </Form>
        </Collapse.Panel>
    );
};

export default AdamicAdar;