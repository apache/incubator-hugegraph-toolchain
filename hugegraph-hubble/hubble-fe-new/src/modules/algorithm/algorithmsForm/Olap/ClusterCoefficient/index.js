/**
 * @file ClusterCoefficient算法
 * @author zhanghao14@
 */

import React, {useState, useCallback, useContext} from 'react';
import {Form, Collapse, InputNumber} from 'antd';
import {WalletOutlined} from '@ant-design/icons';
import GraphAnalysisContext from '../../../../Context';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import OlapComputerItem from '../OlapComputerItem';
import _ from 'lodash';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, TEXT_PATH, useTranslatedConstants} from '../../../../../utils/constants';
import {greaterThanOneAndLowerThanTenThousandIntegerValidator} from '../../utils';
import {useTranslation} from 'react-i18next';



const ClusterCoefficient = props => {
    const {ALGORITHM_NAME} = useTranslatedConstants();
    const {CLUSTER_COEFFICIENT} = ALGORITHM_NAME;
    const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;
    const OWNED_TEXT_PATH = TEXT_PATH.OLAP + '.cluster_coefficient';
    const {t} = useTranslation();
    const {
        handleFormSubmit,
        searchValue,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } = props;
    const info = {
        name: 'Cluster Coefficient',
        desc: t(OWNED_TEXT_PATH + '.desc'),
        icon: <WalletOutlined />,
    };
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
            updateCurrentAlgorithm(CLUSTER_COEFFICIENT);
            handleFormSubmit(LOADING);
            const {worker, ...args} = algorithmParams;
            const formParams = {
                algorithm: 'clustering-coefficient',
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
                    name={CLUSTER_COEFFICIENT}
                    description={info.desc}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === CLUSTER_COEFFICIENT}
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
                    tooltip={t(TEXT_PATH.ALGORITHM_COMMON + '.instance_num')}
                >
                    <InputNumber min={1} precision={0} />
                </Form.Item>
                <Form.Item
                    label='input.minimum_edges_use_superedge_cache'
                    name='input.minimum_edges_use_superedge_cache'
                    initialValue={100}
                    tooltip={t(OWNED_TEXT_PATH + '.minimum_edges_use_superedge_cache')}
                    rules={[{validator: greaterThanOneAndLowerThanTenThousandIntegerValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <OlapComputerItem />
            </Form>
        </Collapse.Panel>
    );
};

export default ClusterCoefficient;
