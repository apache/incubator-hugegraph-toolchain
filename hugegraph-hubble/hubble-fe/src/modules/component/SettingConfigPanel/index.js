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

/**
 * @file  设置表单
 */

import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {Form, Select, Switch, Cascader} from 'antd';
import {useTranslation} from 'react-i18next';
import SliderComponent from '../../../components/SlideComponent';
import _ from 'lodash';
import classnames from 'classnames';
import c from './index.module.scss';

const SettingConfigPanel = props => {
    const {onChange, data, open} = props;
    const {t} = useTranslation();
    const {useForm} = Form;
    const [labelForm, moduleForm] = useForm();

    const defaultDisplayFields = useMemo(
        () => ({
            nodes: {label: t('analysis.canvas.setting_panel.node_id'), value: '~id'},
            edges: {label: t('analysis.canvas.setting_panel.edge_type'), value: '~id'},
        }),
        [t]
    );

    const labelPosition = useMemo(
        () => [
            {value: 'center', label: t('analysis.canvas.setting_panel.node_center')},
            {value: 'bottom', label: t('analysis.canvas.setting_panel.node_bottom')},
        ],
        [t]
    );

    const settingClassName = classnames(
        c.canvasSetting,
        {[c.canvasSettingHidden]: !open}
    );

    const [graphData, setGraphData] = useState();

    const uniqueArr = arr => {
        const res = [...new Set([...arr.map(item => item.toString())])].map(item => item.split(','));
        return res;
    };

    const transformDataToCascaderValue = useCallback(
        dataArray => {
            const defaultValue = [];
            dataArray.forEach(item => {
                const {itemType, metaConfig} = item || {};
                const {display_fields} = metaConfig?.style || {};
                display_fields?.forEach(item2 => {
                    const arr = [itemType, item2];
                    defaultValue.push(arr);
                });
            });
            const value = uniqueArr(defaultValue);
            return value;
        },
        []
    );

    useEffect(
        () => {
            data && setGraphData(_.cloneDeep(data));
        },
        [data]
    );

    useEffect(
        () => {
            // 节点标签为空的情况 不需要刷新属性filedValue;
            const enableNodeLabel = labelForm.getFieldValue('enableNodeLabel');
            if (enableNodeLabel) {
                const nodeValue = transformDataToCascaderValue(graphData?.nodes || []);
                labelForm.setFieldValue('nodeLabelPropertyName', nodeValue);
            }
            const enableEdgeLabel = labelForm.getFieldValue('enableEdgeLabel');
            if (enableEdgeLabel) {
                const edgeValue = transformDataToCascaderValue(graphData?.edges || []);
                labelForm.setFieldValue('edgeLabelPropertyName', edgeValue);
            }

        },
        [graphData, graphData?.edges, graphData?.nodes, labelForm, transformDataToCascaderValue]
    );


    const getLabelPropertyOptions = useCallback(
        (data, type) => {
            const types = [...new Set(data && data.map(item => item.itemType))];
            const defaultFields = defaultDisplayFields[type];
            const options = types.map(
                item => {
                    const {properties = {}} = _.find(data, {itemType: item}) || {};
                    return {
                        label: item,
                        value: item,
                        children: [defaultFields].concat(Object.keys(properties).map(item2 => {
                            return {
                                value: item2,
                                label: item2,
                            };
                        })),
                    };
                }
            );
            return options;
        },
        [defaultDisplayFields]
    );

    const onModuleFormChange = useCallback(
        changedValues => {
            const [key, value] = Object.entries(changedValues)[0];
            switch (key) {
                case 'enableMinimap':
                    if (value) {
                        document.getElementsByClassName('g6-minimap')[0].classList.remove('hiddenMiniMap');
                    }
                    else {
                        document.getElementsByClassName('g6-minimap')[0].classList.add('hiddenMiniMap');
                    }
                    break;
                case 'enableLegend':
                    if (value) {
                        document.getElementsByClassName('g6-legend-container')[0].classList.remove('hiddenLegend');
                    }
                    else {
                        document.getElementsByClassName('g6-legend-container')[0].classList.add('hiddenLegend');
                    }
                    break;
                case 'enableGrid':
                    if (value) {
                        document.getElementById('graph').classList.remove('hiddenGrid');
                    }
                    else {
                        document.getElementById('graph').classList.add('hiddenGrid');
                    }
                    break;
            }
        },
        []
    );

    const disableShowLabel = useCallback(
        dataArr => {
            dataArr.map(item => {
                item.label = [];
                item.metaConfig.style.display_fields = [];
            }
            );
        },
        []
    );

    const processLabeledDataToBreakLine = useCallback(
        dataArr => {
            dataArr.map(
                item => {
                    const {label} = item;
                    item.label = label.join('\n');
                }
            );
        },
        []
    );

    const processLabeledDataBg = useCallback(
        dataArr => {
            dataArr.map(
                item => {
                    const {label, labelCfg = {style: {}}} = item || {};
                    const {style} = labelCfg;
                    if (label.length === 0) {
                        item.labelCfg.style = {
                            fill: style?.fill,
                            fontSize: style?.fontSize,
                            background: {
                                fill: undefined,
                                padding: [0, 0, 0, 0],
                                radius: 0,
                            },
                        };
                    }
                    else {
                        item.labelCfg.style.background = {
                            fill: '#ffffff',
                            padding: [2, 2, 2, 2],
                            radius: 2,
                        };
                    }
                }
            );
        },
        []
    );

    const getLabeledData = useCallback(
        (rawData, labelPropertyName, type) => {
            disableShowLabel(rawData);
            labelPropertyName.map(
                propertyItem => {
                    // 属性全选的情况设置
                    if (propertyItem.length === 1) {
                        const [changedType] = propertyItem;
                        rawData.map(
                            dataItem => {
                                const {properties, id, itemType} = dataItem;
                                let defaultDisplayInfo = type === 'nodes' ? id : itemType;
                                if (dataItem.itemType === changedType) {
                                    dataItem.metaConfig.style.display_fields = ['~id'].concat(Object.keys(properties));
                                    dataItem.label = [defaultDisplayInfo].concat(Object.values(properties));
                                }
                            }
                        );
                    }
                    // 属性未全选的情况设置
                    else {
                        const [changedType, propertyKey] = propertyItem;
                        rawData.map(
                            dataItem => {
                                const {properties, id, itemType} = dataItem;
                                let defaultDisplayInfo = type === 'nodes' ? id : itemType;
                                if (dataItem.itemType === changedType) {
                                    if (propertyKey === '~id') {
                                        dataItem.metaConfig.style.display_fields.push('~id');
                                        dataItem.label.push(defaultDisplayInfo);
                                    }
                                    else {
                                        dataItem.metaConfig.style.display_fields.push(propertyKey);
                                        dataItem.label.push(properties[propertyKey]);
                                    }
                                }
                            }
                        );
                    }
                }
            );
        },
        [disableShowLabel]
    );

    const onLabelFormChange = useCallback(
        (changedValues, allValues) => {
            const {enableNodeLabel, nodeLabelPropertyName, enableEdgeLabel, edgeLabelPropertyName} = allValues;
            switch (Object.keys(changedValues)[0]) {
                case 'enableNodeLabel':
                    if (!enableNodeLabel) {
                        disableShowLabel(graphData.nodes);
                    }
                    else {
                        getLabeledData(graphData.nodes, nodeLabelPropertyName, 'nodes');
                        processLabeledDataToBreakLine(graphData.nodes);
                    }
                    break;
                case 'nodeLabelPropertyName':
                    if (enableNodeLabel) {
                        getLabeledData(graphData.nodes, nodeLabelPropertyName, 'nodes');
                        processLabeledDataToBreakLine(graphData.nodes);
                    }
                    break;
                case 'nodeLabelPos':
                    graphData.nodes.map(item => {
                        item.labelCfg.position = changedValues.nodeLabelPos;
                    });
                    break;
                case 'nodeLabelSize':
                    graphData.nodes.map(item => {
                        item.labelCfg.style.fontSize = changedValues.nodeLabelSize;
                    });
                    break;
                case 'enableEdgeLabel':
                    if (!enableEdgeLabel) {
                        disableShowLabel(graphData.edges);
                    }
                    else {
                        getLabeledData(graphData.edges, edgeLabelPropertyName, 'edges');
                        processLabeledDataToBreakLine(graphData.edges);
                    }
                    processLabeledDataBg(graphData.edges);
                    break;
                case 'edgeLabelPropertyName':
                    if (enableEdgeLabel) {
                        getLabeledData(graphData.edges, edgeLabelPropertyName, 'edges');
                        processLabeledDataToBreakLine(graphData.edges);
                        processLabeledDataBg(graphData.edges);
                    }
                    break;
                case 'edgeLabelSize':
                    graphData.edges.map(item => {
                        item.labelCfg.style.fontSize = changedValues.edgeLabelSize;
                    });
                    break;
                default:
                    break;
            }
            onChange(graphData);
            setGraphData(graphData);
        },
        [disableShowLabel, getLabeledData,
            graphData, onChange, processLabeledDataBg, processLabeledDataToBreakLine]
    );

    const cascaderSearch = (inputValue, path) =>
        path.some(
            option => (option.label).toLowerCase().indexOf(inputValue.toLowerCase()) > -1
        );

    const displayRender = useCallback(
        label => {
            return (label.join('/'));
        },
        []
    );

    return (
        <div className={settingClassName}>
            <Form
                form={labelForm}
                colon={false}
                onValuesChange={onLabelFormChange}
            >
                <div className={c.canvasSettingTitle}>{t('analysis.canvas.setting_panel.title')}</div>
                <Form.Item
                    name='enableNodeLabel'
                    label={t('analysis.canvas.setting_panel.node_label')}
                    valuePropName='checked'
                    labelCol={{span: 20}}
                    labelAlign='left'
                    initialValue
                >
                    <Switch />
                </Form.Item>
                <Form.Item
                    name='nodeLabelPropertyName'
                    label={t('analysis.canvas.setting_panel.node_label_property')}
                    labelCol={{span: 24}}
                >
                    <Cascader
                        style={{width: '100%'}}
                        options={getLabelPropertyOptions(data?.nodes, 'nodes')}
                        maxTagCount="5"
                        multiple
                        displayRender={displayRender}
                        showSearch={{cascaderSearch}}
                    />
                </Form.Item>
                <Form.Item
                    name='nodeLabelPos'
                    label={t('analysis.canvas.setting_panel.node_label_position')}
                    labelCol={{span: 24}}
                    initialValue='bottom'
                >
                    <Select options={labelPosition} />
                </Form.Item>
                <Form.Item
                    name='nodeLabelSize'
                    label={t('analysis.canvas.setting_panel.node_label_size')}
                    initialValue={12}
                    labelCol={{span: 24}}
                >
                    <SliderComponent />
                </Form.Item>
                <Form.Item
                    name='enableEdgeLabel'
                    label={t('analysis.canvas.setting_panel.edge_label')}
                    valuePropName='checked'
                    labelCol={{span: 20}}
                    labelAlign='left'
                    initialValue
                >
                    <Switch />
                </Form.Item>
                <Form.Item
                    name='edgeLabelPropertyName'
                    label={t('analysis.canvas.setting_panel.edge_label_property')}
                    labelCol={{span: 24}}
                >
                    <Cascader
                        style={{width: '100%'}}
                        options={getLabelPropertyOptions(data?.edges, 'edges')}
                        multiple
                        maxTagCount="5"
                        displayRender={displayRender}
                        showSearch={{cascaderSearch}}
                    />
                </Form.Item>
                <Form.Item
                    name='edgeLabelSize'
                    label={t('analysis.canvas.setting_panel.edge_label_size')}
                    labelCol={{span: 24}}
                    initialValue={12}
                >
                    <SliderComponent />
                </Form.Item>
            </Form>
            <Form
                form={moduleForm}
                colon={false}
                onValuesChange={onModuleFormChange}
            >
                <div className={c.canvasSettingTitle}>{t('analysis.canvas.setting_panel.components')}</div>
                <Form.Item
                    name='enableMinimap'
                    label={t('analysis.canvas.setting_panel.enable_minimap')}
                    labelCol={{span: 20}}
                    labelAlign='left'
                    valuePropName='checked'
                    initialValue
                >
                    <Switch />
                </Form.Item>
                <Form.Item
                    name='enableGrid'
                    label={t('analysis.canvas.setting_panel.enable_grid')}
                    labelAlign='left'
                    labelCol={{span: 20}}
                    valuePropName='checked'
                    initialValue
                >
                    <Switch />
                </Form.Item>
                <Form.Item
                    name='enableLegend'
                    label={t('analysis.canvas.setting_panel.enable_legend')}
                    labelCol={{span: 20}}
                    valuePropName='checked'
                    labelAlign='left'
                    initialValue
                >
                    <Switch />
                </Form.Item>
            </Form>
        </div>
    );
};

export default SettingConfigPanel;
