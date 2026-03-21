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

import {Space, Button, Form, Collapse, Typography, message, List} from 'antd';
import {useState, useEffect} from 'react';
import * as api from '../../../api';
import VertexForm from './Vertex';
import EdgeForm from './Edge';
import * as rules from '../../../utils/rules';

const fieldMapping = list => {
    const obj = {};
    if (!list) {
        return obj;
    }

    list.map(item => {
        if (!item || !item.key) {
            return;
        }

        obj[item.key] = item.val;
    });

    return obj;
};

const valueMapping = list => {
    const obj = {};
    if (!list) {
        return obj;
    }

    list.map(item => {
        if (!item || !item.key) {
            return;
        }

        if (!obj[item.key]) {
            obj[item.key] = {};
        }
        obj[item.key][item.origin] = item.replace;
    });

    return obj;
};

const MappingForm = ({prev,
    visible,
    targetField,
    graphspace,
    graph,
    vertexList,
    changeVertexList,
    edgeList,
    changeEdgeList,
}) => {
    const [mappingForm] = Form.useForm();
    const [type, setType] = useState('');
    const [vertex, setVertex] = useState([]);
    const [edge, setEdge] = useState([]);
    const [vertexIndex, setVertexIndex] = useState(-1);
    const [edgeIndex, setEdgeIndex] = useState(-1);
    const [submitEnable, setSubmitEnable] = useState(false);

    const formatVertex = item => {
        const {label, id, attr, value} = item;
        const field_mapping = fieldMapping(attr);
        const value_mapping = valueMapping(value);
        // const id = selectLabel.id_strategy === 'PRIMARY_KEY' ? null : info.id;
        const selected = Object.keys(field_mapping);

        return {
            label,
            skip: false,
            id: id ?? null,
            unfold: false,
            field_mapping,
            value_mapping,
            selected: id ? selected.concat(id) : selected,
            ignored: [],
            null_values: [''],
            update_strategies: {},
        };
    };

    const formatEdge = item => {
        const {label, attr, value, source, target} = item;
        const field_mapping = fieldMapping(attr);
        const value_mapping = valueMapping(value);

        return {
            label,
            skip: false,
            source: [source],
            unfold_source: false,
            target: [target],
            unfold_target: false,
            field_mapping,
            value_mapping,
            selected: Object.keys(field_mapping).concat(source, target),
            ignored: [],
            null_values: [''],
            update_strategies: {},
        };
    };

    const onFinish = () => {
        const vertices = vertexList.map(item => formatVertex(item));
        const edges = edgeList.map(item => formatEdge(item));
        // console.log({vertices, edges});
        mappingForm.setFieldsValue({vertices, edges});
        mappingForm.submit();
    };

    const removeVertex = index => {
        vertexList.splice(index, 1);
        changeVertexList([...vertexList]);
    };

    const removeEdge = index => {
        edgeList.splice(index, 1);
        changeEdgeList([...edgeList]);
    };

    const editVertex = index => {
        setVertexIndex(index);
        setType('vertex');
    };

    const editEdge = index => {
        setEdgeIndex(index);
        setType('edge');
    };

    useEffect(() => {
        setSubmitEnable(vertexList.length > 0 || edgeList.length > 0);
    }, [vertexList, edgeList]);

    useEffect(() => {
        if (!graphspace || !graph) {
            return;
        }

        api.manage.getMetaVertexList(graphspace, graph).then(res => {
            if (res.status === 200) {
                setVertex(res.data.records);
                return;
            }

            message.error(res.message);
        });

        api.manage.getMetaEdgeList(graphspace, graph).then(res => {
            if (res.status === 200) {
                setEdge(res.data.records);
                return;
            }

            message.error(res.message);
        });
    }, [graphspace, graph]);

    return (
        <div style={{display: visible ? '' : 'none'}}>
            <Typography.Title level={5}>映射字段</Typography.Title>
            <Space className={'form_attr_button'}>
                <Button onClick={() => editVertex(-1)}>
                    新增顶点映射
                </Button>
                <Button onClick={() => editEdge(-1)}>
                    新增边映射
                </Button>
            </Space>

            <VertexForm
                open={type === 'vertex'}
                onCancel={() => setType('')}
                targetField={targetField}
                sourceField={vertex || []}
                index={vertexIndex}
                vertexList={vertexList}
            />

            <EdgeForm
                open={type === 'edge'}
                onCancel={() => setType('')}
                targetField={targetField}
                sourceField={edge || []}
                index={edgeIndex}
                edgeList={edgeList}
            />

            {vertexList.length > 0 && (
                <List
                    className='form_attr_table'
                    itemLayout='horizontal'
                    dataSource={vertexList}
                    bordered
                    locale={{emptyText: <></>}}
                    renderItem={(item, index) => (
                        <List.Item
                            actions={[
                                <a key={'1'} onClick={() => editVertex(index)}>编辑</a>,
                                <a key={'2'} onClick={() => removeVertex(index)}>删除</a>,
                            ]}
                        >
                            <Space><span>类型：顶点</span><span>名称：{item.label}</span></Space>
                        </List.Item>
                    )}
                />
            )}

            {edgeList.length > 0 && (
                <List
                    className='form_attr_table'
                    itemLayout='horizontal'
                    dataSource={edgeList}
                    bordered
                    locale={{emptyText: <></>}}
                    renderItem={(item, index) => (
                        <List.Item
                            actions={[
                                <a key={'1'} onClick={() => editEdge(index)}>编辑</a>,
                                <a key={'2'} onClick={() => removeEdge(index)}>删除</a>,
                            ]}
                        >
                            <Space><span>类型：边</span><span>名称：{item.label}</span></Space>
                        </List.Item>
                    )}
                />
            )}

            <Form form={mappingForm} name='mapping_form'>
                <Form.Item name='vertices' hidden />
                <Form.Item name='edges' hidden />

                <Form.Item extra='请至少创建一个顶点或边映射'>
                    <Space>
                        <Button onClick={prev}>上一步</Button>
                        <Button type='primary' onClick={onFinish} disabled={!submitEnable}>下一步</Button>
                    </Space>
                </Form.Item>
            </Form>

        </div>
    );
};

export default MappingForm;
