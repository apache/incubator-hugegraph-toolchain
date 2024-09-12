/**
 * @file ClosenessCentralityVermeer算法
 * @author gouzixing@
 */

import React, {useState, useCallback, useContext, useEffect} from 'react';
import {Form, Collapse, InputNumber, Select} from 'antd';
import {DeleteColumnOutlined} from '@ant-design/icons';
import GraphAnalysisContext from '../../../../Context';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import _ from 'lodash';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, ALGORITHM_NAME, GRAPH_LOAD_STATUS} from '../../../../../utils/constants';
import {positiveIntegerValidator, greaterThanZeroAndLowerThanOneContainsValidator} from '../../utils';

const {CLOSENESS_CENTRALITY} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;
const {LOADED} = GRAPH_LOAD_STATUS;

const boolOptions = [
    {label: '是', value: 1},
    {label: '否', value: 0},
];

const info = {
    name: 'Closeness Centrality',
    desc: '计算一个节点到所有其他可达节点的最短距离的倒数，进行累积后归一化的值。用于计算图中每个节点的度中心性值，支持无向图和有向图。',
    icon: <DeleteColumnOutlined />,
};

const ClosenessCentralityVermeer = props => {
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
            updateCurrentAlgorithm(CLOSENESS_CENTRALITY);
            handleFormSubmit(LOADING);
            const formParams =  {'compute.algorithm': 'closeness_centrality', ...algorithmParams};
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
                    name={CLOSENESS_CENTRALITY}
                    description={info.desc}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === CLOSENESS_CENTRALITY}
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
                    label='closeness_centrality.sample_rate'
                    name='closeness_centrality.sample_rate'
                    initialValue={1.0}
                    tooltip='边的采样率，由于此算法是指数型增长的算法，算力要求非常高，需要根据业务需求设置合理的采样率，得到一个近似结果'
                    rules={[{validator: greaterThanZeroAndLowerThanOneContainsValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='closeness_centrality.wf_improved'
                    name='closeness_centrality.wf_improved'
                    initialValue={1}
                    tooltip={'是否使用 Wasserman and Faust 紧密中心性公式'}
                >
                    <Select allowClear options={boolOptions} />
                </Form.Item>
            </Form>
        </Collapse.Panel>
    );
};

export default ClosenessCentralityVermeer;
