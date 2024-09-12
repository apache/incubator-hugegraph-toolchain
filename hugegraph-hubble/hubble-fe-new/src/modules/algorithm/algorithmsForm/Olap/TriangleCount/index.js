/**
 * @file TriangleCount算法
 * @author zhanghao14@
 */

import React, {useState, useCallback, useContext} from 'react';
import {Form, Collapse, InputNumber} from 'antd';
import {SendOutlined} from '@ant-design/icons';
import GraphAnalysisContext from '../../../../Context';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import OlapComputerItem from '../OlapComputerItem';
import _ from 'lodash';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, ALGORITHM_NAME} from '../../../../../utils/constants';
import {
    greaterThanOneAndLowerThanOneHundredThousandIntegerValidator,
    greaterThanZeroAndLowerThanTwoThousandAndOneIntegerValidator,
} from '../../utils';

const {TRIANGLE_COUNT} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;

const info = {
    name: 'Triangle Count',
    desc: '三角形计数算法，用于计算通过每个顶点的三角形个数。',
    icon: <SendOutlined />,
};

const TriangleCount = props => {
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
            updateCurrentAlgorithm(TRIANGLE_COUNT);
            handleFormSubmit(LOADING);
            const {worker, limit_edges_in_one_vertex, ...args} = algorithmParams;
            const formParams = {
                algorithm: 'triangle-count',
                worker: worker,
                'input.limit_edges_in_one_vertex': limit_edges_in_one_vertex,
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
                    name={TRIANGLE_COUNT}
                    description={info.desc}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === TRIANGLE_COUNT}
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
                    label='input.limit_edges_in_one_vertex'
                    name='limit_edges_in_one_vertex'
                    initialValue={-1}
                    tooltip='最大出边限制'
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='input.minimum_edges_use_superedge_cache'
                    name='input.minimum_edges_use_superedge_cache'
                    initialValue={100}
                    tooltip='利用内存减少消息量，如果内存不够，可以从100改成1000，但三角计数可能计算不完'
                    rules={[{validator: greaterThanOneAndLowerThanOneHundredThousandIntegerValidator}]}
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

export default TriangleCount;
