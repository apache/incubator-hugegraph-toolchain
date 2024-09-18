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
import {
    GRAPH_STATUS,
    ALGORITHM_NAME,
    GRAPH_LOAD_STATUS,
    TEXT_PATH,
    useTranslatedConstants,
} from '../../../../../utils/constants';
import {positiveIntegerValidator, greaterThanZeroAndLowerThanOneContainsValidator} from '../../utils';
import {useTranslation} from 'react-i18next';

const {CLOSENESS_CENTRALITY} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;
const {LOADED} = GRAPH_LOAD_STATUS;
const OWNED_TEXT_PATH = TEXT_PATH.OLAP + '.closeness_centrality';

const ClosenessCentralityVermeer = props => {

    const {
        handleFormSubmit,
        searchValue,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } = props;
    const info = {
        name: 'Closeness Centrality',
        desc: t(OWNED_TEXT_PATH + '.desc'),
        icon: <DeleteColumnOutlined />,
    };
    const {boolOptions} = useTranslatedConstants();
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
            const formParams = {'compute.algorithm': 'closeness_centrality', ...algorithmParams};
            const filteredParams = removeNilKeys(formParams);
            const response = await api.analysis.runOlapVermeer(graphSpace, graph, filteredParams);
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
                    tooltip={t(TEXT_PATH.ALGORITHM_COMMON + './worker_num')}
                    rules={[{validator: positiveIntegerValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='closeness_centrality.sample_rate'
                    name='closeness_centrality.sample_rate'
                    initialValue={1.0}
                    tooltip={t(TEXT_PATH.ALGORITHM_COMMON + './sample_rate')}
                    rules={[{validator: greaterThanZeroAndLowerThanOneContainsValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='closeness_centrality.wf_improved'
                    name='closeness_centrality.wf_improved'
                    initialValue={1}
                    tooltip={t(OWNED_TEXT_PATH + './wf_improved')}
                >
                    <Select allowClear options={boolOptions} />
                </Form.Item>
            </Form>
        </Collapse.Panel>
    );
};

export default ClosenessCentralityVermeer;
