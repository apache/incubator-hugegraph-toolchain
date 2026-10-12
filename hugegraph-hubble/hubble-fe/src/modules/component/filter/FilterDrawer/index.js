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
import {useTranslation} from 'react-i18next';
import {Drawer, Radio, Card, Form, Button} from 'antd';
import FilterForm from './FilterForm';
import GraphAnalysisContext from '../../../Context';
import * as api from '../../../../api';
import style from './index.module.scss';

const Filter = ({open, onCancel, dataSource, onChange}) => {

    const {t} = useTranslation();
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
            title={t('analysis.canvas.filter_drawer.filter')}
            open={open}
            onClose={onCancel}
        >
            <Form.Provider
                onFormFinish={onFinish}
            >
                <Form>
                    <Form.Item label={t('analysis.canvas.filter_drawer.logic')} name='logic' initialValue={'and'}>
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

                <Button type='dashed' block onClick={addForm} className={style.add}>
                    {t('analysis.canvas.filter_drawer.add_expression')}
                </Button>
                <Form>
                    <Button type='primary' block htmlType='submit'>
                        {t('analysis.canvas.filter_drawer.filter')}
                    </Button>
                </Form>
            </Form.Provider>
        </Drawer>
    );
};

export default Filter;
