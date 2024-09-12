/**
 * @file PageRank_Vermeer算法
 * @author gouzixing@
 */

import React, {useState, useCallback, useContext, useEffect} from 'react';
import {Form, Collapse, InputNumber} from 'antd';
import {BranchesOutlined} from '@ant-design/icons';
import GraphAnalysisContext from '../../../../Context';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import _ from 'lodash';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {greaterThanZeroAndLowerThanOneValidator, positiveIntegerValidator} from '../../utils';
import {GRAPH_STATUS, ALGORITHM_NAME, GRAPH_LOAD_STATUS} from '../../../../../utils/constants';

const {PAGE_RANK} = ALGORITHM_NAME;
const {LOADED} = GRAPH_LOAD_STATUS;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;

const info = {
    name: 'PageRank',
    desc: 'PageRank算法又称网页排名算法，是一种由搜索引擎根据网页（节点）之间相互的超链接进行计算的技术，用来体现网页（节点）的相关性和重要性。',
    icon: <BranchesOutlined />,
};

const PageRankVermeer = props => {
    const {
        handleFormSubmit,
        searchValue,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } = props;

    const {graphSpace, graph, graphStatus} = useContext(GraphAnalysisContext);

    const [isEnableRun, setEnableRun] = useState(true);
    const [isRequiring, setRequiring] = useState(false);
    const [form] = Form.useForm();

    useEffect(
        () => {
            setEnableRun(graphStatus === LOADED);
        },
        [graphStatus]
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
            updateCurrentAlgorithm(PAGE_RANK);
            handleFormSubmit(LOADING);
            const formParams =  {'compute.algorithm': 'pagerank', ...algorithmParams};
            const filteredParams = removeNilKeys(formParams);
            const response =  await api.analysis.runOlapVermeer(graphSpace, graph, filteredParams);
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
                    name={PAGE_RANK}
                    description={info.desc}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === PAGE_RANK}
                />
            }
            forceRender
            {...props}
        >
            <Form
                form={form}
                disabled={graphStatus !== LOADED}
                onFinish={onFormFinish}
                onValuesChange={_.debounce(onFormValuesChange, 300)}
                layout="vertical"
            >
                <Form.Item
                    label='compute.parallel'
                    name='compute.parallel'
                    initialValue={1}
                    tooltip='worker计算线程数'
                    rules={[{validator: positiveIntegerValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='compute.max_step'
                    name='compute.max_step'
                    initialValue={10}
                    tooltip='最大迭代次数'
                    rules={[{validator: positiveIntegerValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='pagerank.damping'
                    name='pagerank.damping'
                    initialValue={0.85}
                    tooltip='阻尼系数，传导到下个点的百分比'
                    rules={[{validator: greaterThanZeroAndLowerThanOneValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='pagerank.diff_threshold'
                    name='pagerank.diff_threshold'
                    initialValue={0.00001}
                    tooltip='收敛精度,为每次迭代各个点相较于上次迭代变化的绝对值累加和上限，当小于这个值时认为计算收敛，算法停止。'
                    rules={[{validator: greaterThanZeroAndLowerThanOneValidator}]}
                >
                    <InputNumber />
                </Form.Item>
            </Form>
        </Collapse.Panel>
    );
};

export default PageRankVermeer;
