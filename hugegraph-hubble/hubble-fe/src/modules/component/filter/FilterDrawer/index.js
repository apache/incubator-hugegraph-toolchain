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

import {useState, useEffect, useCallback, useContext} from 'react';
import {Drawer, Radio, Card, Form, Button} from 'antd';
import FilterForm from './FilterForm';
import GraphAnalysisContext from '../../../Context';
import * as api from '../../../../api';
import style from './index.module.scss';

const checkRule = (left, right, rule) => {
    if (rule === 'eq') {
        return left === right;
    }

    if (rule === 'gt') {
        return left > right;
    }

    if (rule === 'gte') {
        return left >= right;
    }

    if (rule === 'lt') {
        return left < right;
    }

    if (rule === 'lte') {
        return left <= right;
    }

    if (rule === 'ltlt') {
        return left < right[0] && left < right[1];
    }

    if (rule === 'ltelt') {
        return left <= right[0] && left < right[1];
    }

    if (rule === 'ltlte') {
        return left < right[0] && left <= right[1];
    }

    if (rule === 'ltelte') {
        return left <= right[0] && left <= right[1];
    }
};

const filterCondition = (node, rules, logic) => {
    let flag = '';

    if (rules.length === 0) {
        return true;
    }

    for (let rule of rules) {
        if (rule.op_type === 'id') {
            flag = checkRule(node.id.split(':')[1], rule.value, rule.rule);
        }

        if (rule.op_type === 'property') {
            flag = checkRule(node.properties[rule.property], rule.value, rule.rule);
        }

        // or 条件下一个为true结果为true, and 条件下一个为false结果为false
        if (logic === 'and' && flag === false) {
            return false;
        }

        if (logic === 'or' && flag === true) {
            return true;
        }
    }

    return flag;
};

const filterData = (dataList, rules, logic) => {
    const filterVertex = [];
    const filterEdge = [];
    const {vertices, edges} = dataList;

    for (let k in vertices) {
        if ({}.hasOwnProperty.call(vertices, k)) {
            const vertex = vertices[k];
            if (filterCondition(vertex, rules.vertex, logic)) {
                filterVertex.push(vertex);
            }
        }
    }

    for (let k in edges) {
        if ({}.hasOwnProperty.call(edges, k)) {
            const edge = edges[k];
            if (filterCondition(edge, rules.edge, logic)) {
                filterEdge.push(edge);
            }
        }
    }

    return {vertices: filterVertex, edges: filterEdge};
};

const Filter = ({open, onCancel, dataSource, onChange}) => {

    const {graphSpace: currentGraphSpace, graph: currentGraph} = useContext(GraphAnalysisContext);
    const [list, setList] = useState([]);
    // const [autoID, setAutoID] = useState(1);
    const [propertyList, setPropertyList] = useState([]);
    const [vertexList, setVertexList] = useState([]);
    const [edgeList, setEdgeList] = useState([]);
    const [logic, setLogic] = useState('and');
    const [autoID, setAutoID] = useState(0);
    // let autoID = 1;
    const addForm = useCallback(() => {
        const id = autoID + 1;
        list.push({id});
        setAutoID(id);
        setList([...list]);
    }, [list, autoID]);

    const handleLogic = useCallback(value => {
        setLogic(value);
    }, []);

    const removeItem = useCallback(index => {
        // console.log(index, list, list.filter(item => item.id !== index));
        // console.log(list, index);
        const newList = list.filter(item => item.id !== index);
        // list.splice(index, 1);
        setList([...newList]);
    }, [list]);

    const onFinish = useCallback(async (name, {forms}) => {
        const rules = {vertex: [], edge: []};
        let flag = true;

        const setFlagFalse = () => {
            flag = false;
        };

        for (let formName in forms) {
            if ({}.hasOwnProperty.call(forms, formName)) {
                const data = forms[formName].getFieldsValue();
                data.type === 'vertex' ? rules.vertex.push(data) : rules.edge.push(data);

                await forms[formName].validateFields().catch(setFlagFalse);
            }
        }
        // const data = filterData(dataSource, rules, logic);
        // setQueryResultGraphFilter(data);
        if (flag === true) {
            onChange?.call(null, {rules, logic});
        }
    }, [onChange, logic]);

    useEffect(() => {
        if (!currentGraphSpace || !currentGraph) {
            return;
        }

        api.manage.getMetaPropertyList(currentGraphSpace, currentGraph, {page_size: -1}).then(res => {
            if (res.status === 200) {
                setPropertyList(res.data.records);
            }
        });

        api.manage.getMetaVertexList(currentGraphSpace, currentGraph, {page_size: -1}).then(res => {
            if (res.status === 200) {
                setVertexList(res.data.records);
            }
        });

        api.manage.getMetaEdgeList(currentGraphSpace, currentGraph, {page_size: -1}).then(res => {
            if (res.status === 200) {
                setEdgeList(res.data.records);
            }
        });
    }, [currentGraphSpace, currentGraph]);

    return (
        <Drawer
            title='筛选'
            open={open}
            onClose={onCancel}
        >
            <Form.Provider
                onFormFinish={onFinish}
            >
                <Form>
                    <Form.Item label='逻辑表达式' name='logic' initialValue={'and'}>
                        <Radio.Group
                            options={[{label: 'and', value: 'and'}, {label: 'or', value: 'or'}]}
                            onChange={handleLogic}
                        />
                    </Form.Item>
                </Form>
                {list.map((item, index) => {
                    return (
                        <Card key={item.id} className={style.card}>
                            <FilterForm
                                remove={removeItem}
                                index={item.id}
                                propertyList={propertyList}
                                vertexList={vertexList}
                                edgeList={edgeList}
                            />
                        </Card>
                    );
                })}

                <Button type='dashed' block onClick={addForm} className={style.add}>添加表达式</Button>
                <Form>
                    <Button type='primary' block htmlType='submit'>筛选</Button>
                </Form>
            </Form.Provider>
        </Drawer>
    );
};

export default Filter;
