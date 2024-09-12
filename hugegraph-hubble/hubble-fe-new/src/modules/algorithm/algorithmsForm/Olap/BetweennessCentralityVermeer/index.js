/**
 * @file BetweennessCentralityVermeer算法
 * @author gouzixing@
 */

import React, {useState, useCallback, useContext, useEffect} from 'react';
import {Form, Collapse, InputNumber, Select} from 'antd';
import {ControlOutlined} from '@ant-design/icons';
import GraphAnalysisContext from '../../../../Context';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import _ from 'lodash';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, ALGORITHM_NAME, GRAPH_LOAD_STATUS} from '../../../../../utils/constants';
import {positiveIntegerValidator, greaterThanZeroAndLowerThanOneContainsValidator} from '../../utils';

const {BETWEENNESS_CENTRALITY} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;
const {LOADED} = GRAPH_LOAD_STATUS;

const info = {
    name: 'Betweenness Centrality',
    desc: `中介中心性算法（Betweeness Centrality）判断一个节点具有"桥梁"节点的值,
        值越大说明它作为图中两点间必经路径的可能性越大, 典型的例子包括社交网络中的共同关注的人`,
    icon: <ControlOutlined />,
};

const boolOptions = [
    {label: '是', value: 1},
    {label: '否', value: 0},
];

const BetweennessCentralityVermeer = props => {
    const {
        handleFormSubmit,
        searchValue,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } = props;

    const {graphSpace, graph, graphStatus} = useContext(GraphAnalysisContext);
    const [isEnableRun, setEnableRun] = useState(true);
    const [isRequiring, setRequiring] = useState(false);

    useEffect(
        () => {
            setEnableRun(graphStatus === LOADED);
        },
        [graphStatus]
    );

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
            updateCurrentAlgorithm(BETWEENNESS_CENTRALITY);
            handleFormSubmit(LOADING);
            const formParams =  {'compute.algorithm': 'betweenness_centrality', ...algorithmParams};
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
                    name={BETWEENNESS_CENTRALITY}
                    description={info.desc}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === BETWEENNESS_CENTRALITY}
                />
            }
            forceRender
            {...props}
        >
            <Form
                disabled={graphStatus !== LOADED}
                form={form}
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
                    label='betweenness_centrality.sample_rate'
                    name='betweenness_centrality.sample_rate'
                    initialValue={1}
                    tooltip='边的采样率，由于此算法是指数型增长的算法，算力要求非常高，需要根据业务需求设置合理的采样率，得到一个近似结果'
                    rules={[{validator: greaterThanZeroAndLowerThanOneContainsValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='betweenness_centrality.use_endpoint'
                    name='betweenness_centrality.use_endpoint'
                    initialValue={0}
                    tooltip={'是否计算最后一个点'}
                >
                    <Select allowClear options={boolOptions} />
                </Form.Item>
            </Form>
        </Collapse.Panel>
    );
};

export default BetweennessCentralityVermeer;
