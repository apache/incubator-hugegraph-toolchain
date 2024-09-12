/**
 * @file FilteredRingsDetection算法
 * @author zhanghao14@
 */

import React, {useState, useCallback, useContext} from 'react';
import {Form, Collapse, InputNumber, Input} from 'antd';
import {SafetyCertificateOutlined} from '@ant-design/icons';
import GraphAnalysisContext from '../../../../Context';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import OlapComputerItem from '../OlapComputerItem';
import _ from 'lodash';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, ALGORITHM_NAME} from '../../../../../utils/constants';
import {
    integerValidator,
    greaterThanZeroAndLowerThanTwoThousandAndOneIntegerValidator,
} from '../../utils';

const {FILTERED_RINGS_DETECTION} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;

const info = {
    name: 'Filtered Rings Detection',
    desc: `带过滤条件的环路检测算法（Filtered Rings Detection）用于检测图中的环路，
        环路的路径由环路中最小id的顶点来记录。可通过指定点、边属性过滤规则让算法选择性的做路径传播。`,
    icon: <SafetyCertificateOutlined />,
};
const placeholder = '{"vertex_filter":[{"label":"user","property_filter":"$element.weight==1"}]}';

const FilteredRingsDetection = props => {
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
            updateCurrentAlgorithm(FILTERED_RINGS_DETECTION);
            handleFormSubmit(LOADING);
            const {worker, ...args} = algorithmParams;
            const formParams = {
                algorithm: 'rings-with-filter',
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
                    name={FILTERED_RINGS_DETECTION}
                    description={info.desc}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === FILTERED_RINGS_DETECTION}
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
                    label='rings.property_filter'
                    name='rings.property_filter'
                    initialValue={placeholder}
                    tooltip='点边属性过滤条件'
                >
                    <Input.TextArea
                        placeholder={placeholder}
                        autoSize={{minRows: 5}}
                    />
                </Form.Item>
                <Form.Item
                    label='rings.min_ring_length'
                    name='rings.min_ring_length'
                    initialValue={0}
                    tooltip='输出环路的最小长度'
                    rules={[{validator: integerValidator}]}
                >
                    <InputNumber />
                </Form.Item>
                <Form.Item
                    label='rings.max_ring_length'
                    name='rings.max_ring_length'
                    initialValue={Number.MAX_SAFE_INTEGER}
                    tooltip='输出环路的最大长度'
                    rules={[{validator: integerValidator}]}
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

export default FilteredRingsDetection;
