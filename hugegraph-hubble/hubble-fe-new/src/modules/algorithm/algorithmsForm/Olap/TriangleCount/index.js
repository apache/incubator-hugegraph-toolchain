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
import {GRAPH_STATUS, ALGORITHM_NAME, TEXT_PATH} from '../../../../../utils/constants';
import {
    greaterThanOneAndLowerThanOneHundredThousandIntegerValidator,
    greaterThanZeroAndLowerThanTwoThousandAndOneIntegerValidator,
} from '../../utils';
import {useTranslation} from 'react-i18next';

const {TRIANGLE_COUNT} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;


const OWNED_TEXT_PATH = TEXT_PATH.OLAP + '.triangle_count';
const TriangleCount = props => {
    const {
        handleFormSubmit,
        searchValue,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } = props;
    const {t} = useTranslation();
    const info = {
        name: 'Triangle Count',
        desc: t(OWNED_TEXT_PATH + '.desc'),
        icon: <SendOutlined />,
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
                    tooltip={t(TEXT_PATH.ALGORITHM_COMMON + '.instance_num')}
                >
                    <InputNumber min={1} precision={0} />
                </Form.Item>
                <Form.Item
                    label='input.limit_edges_in_one_vertex'
                    name='limit_edges_in_one_vertex'
                    initialValue={-1}
                    tooltip={t(OWNED_TEXT_PATH + '.limit_edges_in_one_vertex')}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='input.minimum_edges_use_superedge_cache'
                    name='input.minimum_edges_use_superedge_cache'
                    initialValue={100}
                    tooltip={t(OWNED_TEXT_PATH + '.minimum_edges_use_superedge_cache')}
                    rules={[{validator: greaterThanOneAndLowerThanOneHundredThousandIntegerValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='bsp.max_super_step'
                    name='bsp.max_super_step'
                    initialValue={10}
                    tooltip={t(TEXT_PATH.ALGORITHM_COMMON + '.max_iter_step')}
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
