/**
 * @file Links算法
 * @author zhanghao14@
 */

import React, {useState, useCallback, useContext} from 'react';
import {Form, Collapse, InputNumber, Input} from 'antd';
import {UngroupOutlined} from '@ant-design/icons';
import GraphAnalysisContext from '../../../../Context';
import AlgorithmNameHeader from '../../AlgorithmNameHeader';
import OlapComputerItem from '../OlapComputerItem';
import _ from 'lodash';
import * as api from '../../../../../api';
import removeNilKeys from '../../../../../utils/removeNilKeys';
import {GRAPH_STATUS, ALGORITHM_NAME} from '../../../../../utils/constants';
import {greaterThanZeroAndLowerThanTwoThousandAndOneIntegerValidator} from '../../utils';

const {TextArea} = Input;

const {LINKS} = ALGORITHM_NAME;
const {LOADING, SUCCESS, FAILED} = GRAPH_STATUS;

const info = {
    name: 'Links',
    desc: '链路追踪算法，通过指定的一批开始顶点，按照指定的传播规则进行传播，到指定的结束条件后停止并记录下路径。',
    icon: <UngroupOutlined />,
};
// eslint-disable-next-line
const placeholder='{"start_vertexes":["A","B","C","D","E"],"vertex_end_condition":{"label":"user","property_filter":"$element.age <= 90"},"edge_end_condition":{"label":"pay","property_filter":"double($out.money) >= 4"},"edge_compare_condition":{"label":"pay","property_filter":"$out.money > $in.money"}}';

const Links = props => {
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
            updateCurrentAlgorithm(LINKS);
            handleFormSubmit(LOADING);
            const {worker, ...args} = algorithmParams;
            const formParams = {
                algorithm: 'links',
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
                    name={LINKS}
                    description={info.desc}
                    isRunning={isRequiring}
                    isDisabled={!isEnableRun}
                    handleRunning={handleRunning}
                    searchValue={searchValue}
                    highlightName={currentAlgorithm === LINKS}
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
                    label='links.analyze_config'
                    name='links.analyze_config'
                    initialValue={placeholder}
                    tooltip='链路传播条件配置'
                    rules={[{required: true}]}
                >
                    <TextArea
                        placeholder={placeholder}
                        autoSize={{minRows: 15}}
                    />
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

export default Links;
