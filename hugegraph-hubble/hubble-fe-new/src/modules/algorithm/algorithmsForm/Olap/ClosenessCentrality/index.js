/**
 * @file ClosenessCentrality算法
 * @author zhanghao14@
 */

import React, {useState, useCallback, useContext} from 'react';
import {Input, Form, Collapse, InputNumber} from 'antd';
import {DeleteColumnOutlined} from '@ant-design/icons';
import GraphAnalysisContext from '../../../../Context';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import OlapComputerItem from '../OlapComputerItem';
import _ from 'lodash';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, ALGORITHM_NAME} from '../../../../../utils/constants';
import {alphaValidator, greaterThanZeroAndLowerThanTwoThousandAndOneIntegerValidator} from '../../utils';

const {CLOSENESS_CENTRALITY} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;

const info = {
    name: 'Closeness Centrality',
    desc: '计算一个节点到所有其他可达节点的最短距离的倒数，进行累积后归一化的值。用于计算图中每个节点的度中心性值，支持无向图和有向图。',
    icon: <DeleteColumnOutlined />,
};

const ClosenessCentrality = props => {
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
            updateCurrentAlgorithm(CLOSENESS_CENTRALITY);
            handleFormSubmit(LOADING);
            const {worker, ...args} = algorithmParams;
            const formParams = {
                algorithm: 'closeness-centrality',
                worker: worker,
                params: {...args},
            };
            const filteredParams = removeNilKeys(formParams);
            const response =  await api.analysis.postOlapInfo(graphSpace, graph, filteredParams);
            const {data, status, message} = response || {};
            if (status !== 200) {
                handleFormSubmit(FAILED, '', message);
            }
            else {
                handleFormSubmit(SUCCESS, data?.task_id, message);
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
                    icon={info.icon}
                    name={CLOSENESS_CENTRALITY}
                    description={info.desc}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === CLOSENESS_CENTRALITY}
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
                    label='worker'
                    name='worker'
                    rules={[{required: true}]}
                    tooltip='实例数'
                >
                    <InputNumber min={1} precision={0} />
                </Form.Item>
                <Form.Item
                    label='closeness_centrality.weight_property'
                    name='closeness_centrality.weight_property'
                    tooltip='权重属性名'
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    label='closeness_centrality.sample_rate'
                    name='closeness_centrality.sample_rate'
                    initialValue={1.0}
                    tooltip='边的采样率'
                    rules={[{validator: alphaValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='input.limit_edges_in_one_vertex'
                    name='input.limit_edges_in_one_vertex'
                    initialValue={-1}
                    tooltip='最大出边限制'
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='bsp.max_super_step'
                    name='bsp.max_super_step'
                    initialValue={10}
                    tooltip='最大迭代次数'
                    rules={[{validator: greaterThanZeroAndLowerThanTwoThousandAndOneIntegerValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <OlapComputerItem />
            </Form>
        </Collapse.Panel>
    );
};

export default ClosenessCentrality;
