/**
 * @file Louvain算法
 * @author zhanghao14@
 */

import React, {useState, useCallback, useContext} from 'react';
import {Input, Form, Collapse, InputNumber} from 'antd';
import {HomeOutlined} from '@ant-design/icons';
import GraphAnalysisContext from '../../../../Context';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import OlapComputerItem from '../OlapComputerItem';
import _ from 'lodash';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, ALGORITHM_NAME, TEXT_PATH} from '../../../../../utils/constants';
import {useTranslation} from 'react-i18next';

const {LOUVAIN} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;

const OWNED_TEXT_PATH = TEXT_PATH.OLAP + '.louvain';


const Louvain = props => {
    const {
        handleFormSubmit,
        searchValue,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } = props;
    const {t} = useTranslation();
    const info = {
        name: 'Louvain',
        desc: t(OWNED_TEXT_PATH + '.desc'),
        icon: <HomeOutlined />,
    };
    const {graphSpace, graph} = useContext(GraphAnalysisContext);
    const [isEnableRun, setEnableRun] = useState(true);
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
            updateCurrentAlgorithm(LOUVAIN);
            handleFormSubmit(LOADING);
            const {worker, ...args} = algorithmParams;
            const formParams = {
                algorithm: 'louvain',
                worker: 1,
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
                    name={LOUVAIN}
                    description={info.desc}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === LOUVAIN}
                />
            }
            {...props}
            forceRender
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
                    tooltip={t(TEXT_PATH.ALGORITHM_COMMON + '.instance_num')}
                    initialValue={1}
                >
                    <InputNumber disabled />
                </Form.Item>
                <Form.Item
                    label='louvain.weightkey'
                    name='louvain.weightkey'
                    tooltip={t(TEXT_PATH.ALGORITHM_COMMON + '.weight_property')}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    label='k8s.workerRequestMemory'
                    name='k8s.workerRequestMemory'
                    tooltip={t(TEXT_PATH.ALGORITHM_COMMON + '.request_memory')}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    label='k8s.jvm_options'
                    name='k8s.jvm_options'
                    tooltip={t(TEXT_PATH.ALGORITHM_COMMON + '.JVM_memory')}
                >
                    <Input />
                </Form.Item>
                <OlapComputerItem />
            </Form>
        </Collapse.Panel>
    );
};

export default Louvain;
