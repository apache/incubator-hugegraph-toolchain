/**
 * @file DegreeCentrality算法
 * @author zhanghao14@
 */

import React, {useState, useCallback, useContext} from 'react';
import {Input, Form, Collapse, InputNumber} from 'antd';
import {ControlOutlined} from '@ant-design/icons';
import GraphAnalysisContext from '../../../../Context';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import OlapComputerItem from '../OlapComputerItem';
import _ from 'lodash';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, ALGORITHM_NAME} from '../../../../../utils/constants';
import {greaterThanZeroAndLowerThanTwoThousandAndOneIntegerValidator} from '../../utils';

const {DEGREE_CENTRALIT} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;

const info = {
    name: 'Degree Centrality',
    desc: '用于计算图中每个节点的度中心性值，支持无向图和有向图。',
    icon: <ControlOutlined />,
};

const DegreeCentrality = props => {
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
            updateCurrentAlgorithm(DEGREE_CENTRALIT);
            handleFormSubmit(LOADING);
            const {worker, ...args} = algorithmParams;
            const formParams = {
                algorithm: 'degree-centrality',
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
                    name={DEGREE_CENTRALIT}
                    description={info.desc}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === DEGREE_CENTRALIT}
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
                    label='degree_centrality.weight_property'
                    name='degree_centrality.weight_property'
                    tooltip='权重属性名'
                    initialValue=''
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    label='degree_centrality.direction'
                    name='degree_centrality.direction'
                    initialValue=''
                    tooltip='方向，in/out/both 入边/出边/双边'
                >
                    <Input />
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

export default DegreeCentrality;
