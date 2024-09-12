/**
 * @file DegreeCentralityVermeer算法
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
import {positiveIntegerValidator} from '../../utils';
import {GRAPH_STATUS, ALGORITHM_NAME, GRAPH_LOAD_STATUS} from '../../../../../utils/constants';

const directionOptions = [
    {label: '出边', value: 'out'},
    {label: '入边', value: 'in'},
    {label: '双边', value: 'both'},
];

const {DEGREE_CENTRALIT} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;
const {LOADED} = GRAPH_LOAD_STATUS;

const info = {
    name: 'Degree Centrality',
    desc: '用于计算图中每个节点的度中心性值，支持无向图和有向图。',
    icon: <ControlOutlined />,
};

const DegreeCentralityVermeer = props => {
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
            updateCurrentAlgorithm(DEGREE_CENTRALIT);
            handleFormSubmit(LOADING);
            const formParams =  {'compute.algorithm': 'degree', ...algorithmParams};
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
                    name={DEGREE_CENTRALIT}
                    description={info.desc}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === DEGREE_CENTRALIT}
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
                    label='degree.direction'
                    name='degree.direction'
                    initialValue='out'
                    tooltip={'边的方向'}
                >
                    <Select allowClear options={directionOptions} />
                </Form.Item>
            </Form>
        </Collapse.Panel>
    );
};

export default DegreeCentralityVermeer;
