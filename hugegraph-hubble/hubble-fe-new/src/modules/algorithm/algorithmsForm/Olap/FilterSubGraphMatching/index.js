/**
 * @file FilterSubGraphMatching算法
 * @author zhanghao14@
 */

import React, {useState, useCallback, useContext} from 'react';
import {Form, Collapse, InputNumber, Input} from 'antd';
import {FunnelPlotOutlined} from '@ant-design/icons';
import GraphAnalysisContext from '../../../../Context';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import OlapComputerItem from '../OlapComputerItem';
import _ from 'lodash';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, ALGORITHM_NAME} from '../../../../../utils/constants';

const {TextArea} = Input;
const {FILTER_SUBGRAPH_MATCHING} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;

const info = {
    name: 'Filter SubGraph Matching',
    desc: '带属性过滤的子图匹配算法。用户可以传入一个带属性过滤的查询图结构，算法会在图中匹配所有与该查询图同构的子图。',
    icon: <FunnelPlotOutlined />,
};

// eslint-disable-next-line
const placeholder = '[{"id":"A","label":"person",},{"id":"B","label":"person","property_filter":"$element.x > 3"},{"id":"C","label":"person","edges":[{"targetId":"A","label":"knows","property_filter":"$element.x > 3"}]},{"id":"D","label":"person","property_filter":"$element.x > 3","edges":[{"targetId":"B","label":"knows",},{"targetId":"F","label":"knows","property_filter":"$element.x > 3"},{"targetId":"C","label":"knows",},{"targetId":"E","label":"knows",}]},{"id":"E","label":"person",},{"id":"F","label":"person","property_filter":"$element.x > 3","edges":[{"targetId":"B","label":"knows","property_filter":"$element.x > 3"},{"targetId":"C","label":"knows","property_filter":"$element.x > 3"}]}]';

const FilterSubGraphMatching = props => {
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
            updateCurrentAlgorithm(FILTER_SUBGRAPH_MATCHING);
            handleFormSubmit(LOADING);
            const {worker, ...args} = algorithmParams;
            const formParams = {
                algorithm: 'subgraph-match',
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
                    name={FILTER_SUBGRAPH_MATCHING}
                    description={info.desc}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === FILTER_SUBGRAPH_MATCHING}
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
                    label='subgraph.query_graph_config'
                    name='subgraph.query_graph_config'
                    tooltip='查询图配置，json数组字符串'
                    initialValue={placeholder}
                    rules={[{required: true}]}
                >
                    <TextArea
                        placeholder={placeholder}
                        autoSize={{minRows: 15}}
                    />
                </Form.Item>
                <OlapComputerItem />
            </Form>
        </Collapse.Panel>
    );
};

export default FilterSubGraphMatching;
