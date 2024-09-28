/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import {
    PageHeader,
    Form,
    Steps,
    message,
    Modal,
} from 'antd';
import {useState} from 'react';
import Style from './index.module.scss';
import BaseForm from './BaseForm/index';
import FieldForm from './FieldForm/index';
import MappingForm from './MappingForm/index';
import ScheduleForm from './ScheduleForm/index';
import {useNavigate} from 'react-router-dom';
import * as api from '../../api';
import JSONbig from 'json-bigint';

const TaskEdit = () => {
    const [current, setCurrent] = useState(0);
    const [targetField, setTargetField] = useState([]);
    // const [datasource, setDatasource] = useState({});
    // header的意义为表头，因表头可自定义更改，因此这里的策略是 如果原header为null, 则无论如何自定义都为null
    // 如果原header不为null, 则自定义header将覆盖原header
    const [header, setHeader] = useState(null);
    const [form] = Form.useForm();
    const [graphspace, setGraphspace] = useState('');
    const [graph, setGraph] = useState('');
    const [datasourceID, setDatasourceID] = useState('');
    const [datasource, setDatasource] = useState({});
    const [vertexList, setVertexList] = useState([]);
    const [edgeList, setEdgeList] = useState([]);
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const cancel = () => {
        navigate('/task');
    };

    const prev = () => {
        setCurrent(current - 1);
    };

    // const addEdge = item => {
    //     edgeList.push(item);
    //     setEdgeList([...edgeList]);
    // };

    // const removeVertex = index => {
    //     //
    // };

    // const removeEdge = index => {
    //     //
    // };

    const submitBase = values => {
        const {ingestion_option, datasource_id} = values;
        setGraphspace(ingestion_option.graphspace);
        setGraph(ingestion_option.graph);
        setDatasourceID(datasource_id.toString());

        api.manage.getDatasource(datasource_id.toString()).then(res => {
            if (res.status === 200) {
                setDatasource(res.data);
            }
        });

        api.manage.getGraphSpace(ingestion_option.graphspace).then(res => {
            if (res.status !== 200) {
                message.error('图不存在');
                return;
            }

            if (res.data.storage_percent >= 1) {
                Modal.error({
                    title: '警告',
                    content: `${ingestion_option.graphspace}图空间已达到最大存储容量，无法继续写入`,
                });
                return;
            }

            setCurrent(1);
        });
    };

    const submitField = values => {
        const {source_keys, target_keys} = values;
        setTargetField(target_keys);
        setHeader(source_keys.map(item => item.key));

        setCurrent(2);
    };

    const submitMapping = () => {
        setCurrent(3);
    };

    const submitVertex = values => {
        const {index} = values;

        if (index >= 0) {
            vertexList[index] = values;
        }
        else {
            vertexList.push(values);
        }

        setVertexList([...vertexList]);
    };

    const submitEdge = values => {
        const {index} = values;

        if (index >= 0) {
            edgeList[index] = values;
        }
        else {
            edgeList.push(values);
        }

        setEdgeList([...edgeList]);
    };

    const submitForms = ({base_form, mapping_form, schedule_form}) => {
        const baseValues = base_form.getFieldsValue();
        const mappingValues = mapping_form.getFieldsValue();
        const scheduleValues = schedule_form.getFieldsValue();

        setLoading(true);

        const values = {
            ...baseValues,
            ...scheduleValues,
            datasource_id: datasource.datasource_id,
            ingestion_mapping: {
                version: '2.0',
                structs: [
                    {
                        id: '1',
                        skip: false,
                        input: {
                            ...datasource.datasource_config,
                            header: header,
                        },
                        ...mappingValues,
                    },
                ],
            },
        };

        api.manage.addTask(JSONbig.stringify(values)).then(res => {
            setLoading(false);
            if (res.status === 200) {
                message.success('创建成功');
                navigate('/task');
                return;
            }

            message.error(res.message);
        });
    };

    const handleFinish = (name, {values, forms}) => {
        // console.log(name, values, forms, JSONbig.stringify(values));
        if (name === 'base_form') {
            submitBase(values);
        }

        if (name === 'field_form') {
            submitField(values);
        }

        if (name === 'vertex_form') {
            submitVertex(values);
        }

        if (name === 'edge_form') {
            submitEdge(values);
        }

        if (name === 'mapping_form') {
            submitMapping();
        }

        if (name === 'schedule_form') {
            submitForms(forms);
        }
    };

    // const onFinish = () => {
    //     form.validateFields().then(values => {
    //         const {vertices, edges} = values;
    //         values.datasource_id = datasource.datasource_id;
    //         values.ingestion_mapping = {
    //             version: '2.0',
    //             structs: [
    //                 {
    //                     id: '1',
    //                     skip: false,
    //                     input: {
    //                         ...datasource.datasource_config,
    //                         header: header,
    //                     },
    //                     vertices,
    //                     edges,
    //                 },
    //             ],
    //         };
    //         delete values.edge_form;
    //         delete values.vertex_form;
    //         api.manage.addTask(JSONbig.stringify(values)).then(res => {
    //             if (res.status === 200) {
    //                 message.success('创建成功');
    //                 navigate('/task');
    //                 return;
    //             }

    //             message.error(res.message);
    //         });
    //     });
    // };

    return (
        <>
            <PageHeader
                ghost={false}
                onBack={false}
                title="数据导入"
            />

            <div className='container'>
                <Steps labelPlacement='vertical' current={current}>
                    <Steps.Step key="1" title="输入基础信息" />
                    <Steps.Step key="2" title="选择源端字段" />
                    <Steps.Step key="3" title="选择映射字段" />
                    <Steps.Step key="4" title="输入调度信息" />
                </Steps>
                <br />

                <div className={Style.form}>
                    <Form.Provider
                        labelCol={{span: 4}}
                        onFormFinish={handleFinish}
                    >
                        <BaseForm
                            cancel={cancel}
                            visible={current === 0}
                        />
                        <FieldForm
                            datasourceID={datasourceID}
                            prev={prev}
                            visible={current === 1}
                            setTargetField={val => setTargetField(val)}
                            setHeader={val => setHeader(val)}
                        />
                        <MappingForm
                            targetField={targetField}
                            graphspace={graphspace}
                            graph={graph}
                            vertexList={vertexList}
                            changeVertexList={list => setVertexList(list)}
                            edgeList={edgeList}
                            changeEdgeList={list => setEdgeList(list)}
                            prev={prev}
                            visible={current === 2}
                        />
                        <ScheduleForm
                            form={form}
                            prev={prev}
                            datasource={datasource}
                            visible={current === 3}
                            loading={loading}
                        />
                    </Form.Provider>
                </div>
            </div>
        </>
    );
};

export default TaskEdit;
