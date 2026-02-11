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
 * @file 图分析组件 外观设置弹窗
 * @author
 */

import React, {useCallback, useEffect, useState} from 'react';
import {Modal, Form, Select, Divider, Switch} from 'antd';
import '@antv/graphin-icons/dist/index.css';
import _ from 'lodash';

import {InputColorSelect} from '../../../../components/ColorSelect';
import IconSelect from '../../../../components/IconSelect';
import icons from '../../../../utils/graph';
import {iconsMap} from '../../../../utils/constants';

import c from './index.module.scss';

const FORM_TYPE = {
    NODES: 'nodes',
    EDGES: 'edges',
};

const formTypeOptions = [
    {value: FORM_TYPE.NODES, label: '点外观'},
    {value: FORM_TYPE.EDGES, label: '边外观'},
];
const shapesOptions = [
    {value: 'circle', label: '圆型'},
    {value: 'diamond', label: '菱形'},
    {value: 'triangle', label: '三角形'},
    {value: 'star', label: '五角星'},
    {value: 'ellipse', label: '椭圆'},
];
const sizeOptions = [
    {value: 20, label: '20'},
    {value: 25, label: '25'},
    {value: 30, label: '30'},
    {value: 35, label: '35'},
    {value: 40, label: '40'},
    {value: 45, label: '45'},
    {value: 50, label: '50'},
];
const lineWidthOptions = [
    {value: 0.5, label: '0.5'},
    {value: 1, label: '1'},
    {value: 1.5, label: '1.5'},
    {value: 2, label: '2'},
    {value: 2.5, label: '2.5'},
    {value: 3, label: '3'},
];
const strokeWidthOptions = [
    {value: 1, label: '1'},
    {value: 2, label: '2'},
    {value: 3, label: '3'},
    {value: 4, label: '4'},
    {value: 5, label: '5'},
];
const fontSizeOptions = [
    {value: 14, label: '14'},
    {value: 16, label: '16'},
    {value: 18, label: '18'},
    {value: 20, label: '20'},
    {value: 22, label: '22'},
    {value: 24, label: '24'},
    {value: 26, label: '26'},
];
const lineTypeOptions = [
    {value: 'line', label: '直线'},
    {value: 'dashed', label: '虚线'},
];
const opacityOptions = [
    {value: 0.2, label: '0.2'},
    {value: 0.4, label: '0.4'},
    {value: 0.6, label: '0.6'},
    {value: 0.8, label: '0.8'},
    {value: 1.0, label: '1.0'},
];

const anchorPointsMap = {
    'circle': null,
    'diamond': [[0, 0.5], [0.5, 0], [0.5, 1], [1, 0.5]],
    'ellipse': null,
    'triangle': [[0.5, 0], [0, 1], [1, 1]],
    'star': [[0.5, 0], [0, 0.39], [1, 0.39], [0.19, 1], [0.81, 1]],
};

const offsetMap = {
    0.5: -0.3,
    1: -0.5,
    1.5: -0.7,
    1.6: -0.7,
    2: -1.1,
    2.5: -1.2,
    3: -1.6,
};

const ConfigModal = props => {
    const {
        style,
        visible,
        onOk,
        onCancel,
    } = props;
    const {useForm} = Form;
    const [nodesForm] = useForm();
    const [edgesForm] = useForm();

    const [styleConfig, setStyleConfig] = useState({nodes: [], edges: []});
    const [formType, setFormType] = useState(FORM_TYPE.NODES);
    const [nodeType, setNodeType] = useState();
    const [edgeType, setEdgeType] = useState();
    const isNodeForm = formType === FORM_TYPE.NODES;
    const isFormDisabled = isNodeForm ? !nodeType : !edgeType;

    useEffect(
        () => {
            style && setStyleConfig(_.cloneDeep(style));
        },
        [style]
    );

    const resetModalForm = useCallback(
        () => {
            setStyleConfig(style);
            setFormType(FORM_TYPE.NODES);
            setNodeType(undefined);
            setEdgeType(undefined);
            nodesForm.resetFields();
            edgesForm.resetFields();
        },
        [edgesForm, nodesForm, style]
    );

    const handleModalOk = useCallback(
        () => {
            onOk(styleConfig);
            resetModalForm();
        },
        [onOk, resetModalForm, styleConfig]
    );

    const handleCancel = useCallback(
        () => {
            onCancel();
            resetModalForm();
        },
        [onCancel, resetModalForm]
    );

    const handleNodesStyleChange = useCallback(
        changedValues => {
            const {[nodeType]: currentNode} = styleConfig.nodes;
            if ('shape' in changedValues) {
                currentNode.type = changedValues.shape;
                currentNode.anchorPoints = anchorPointsMap[changedValues.shape];
                if (changedValues.shape === 'ellipse') {
                    const size = currentNode.size || 60;
                    currentNode.size = [size + 10, size - 10];
                }
            }
            else if ('size' in changedValues) {
                const {size} = changedValues;
                const {icon} = currentNode;
                currentNode.size = size;
                if (currentNode.type === 'ellipse') {
                    currentNode.size = [size + 10, size - 10];
                }
                currentNode.icon = {
                    ...icon,
                    fontSize: size * 0.6,
                };
            }
            else if ('strokeColor' in changedValues) {
                currentNode.style = {
                    ...currentNode.style,
                    stroke: changedValues.strokeColor,
                };
            }
            else if ('lineWidth' in changedValues) {
                currentNode.style = {
                    ...currentNode.style,
                    lineWidth: changedValues.lineWidth,
                };
            }
            else if ('fillColor' in changedValues) {
                const {style} = currentNode;
                const color = changedValues.fillColor;
                currentNode.style = {
                    ...style,
                    fill: color,
                };
                currentNode.stateStyles = {
                    ...currentNode.stateStyles,
                    customActive: {
                        shadowColor: color,
                        shadowBlur: 10,
                    },
                    customSelected: {
                        shadowColor: color,
                        shadowBlur: 15,
                    },
                    activeByLegend: {
                        shadowColor: color,
                        shadowBlur: 10,
                    },
                };
            }
            else if ('iconType' in changedValues) {
                const {icon} = currentNode;
                const iconName = changedValues.iconType;
                currentNode.icon = {
                    ...icon,
                    show: true,
                    text: icons[iconsMap[iconName]],
                    fontFamily: 'graphin',
                    fontSize: 12,
                    _iconName: iconName,
                };
            }
            else if ('iconColor' in changedValues) {
                const {icon} = currentNode;
                const color = changedValues.iconColor;
                currentNode.icon = {
                    ...icon,
                    fill: color,
                };
            }
            else if ('labelSize' in changedValues) {
                const labelCfg = currentNode.labelCfg;
                currentNode.labelCfg = {
                    ...labelCfg,
                    style: {
                        ...labelCfg?.style,
                        fontSize: changedValues.labelSize,
                    },
                };
            }
            else if ('labelColor' in changedValues) {
                const labelCfg = currentNode.labelCfg;
                currentNode.labelCfg = {
                    ...labelCfg,
                    style: {
                        ...labelCfg?.style,
                        fill: changedValues.labelColor,
                    },
                };
            }
            else if ('opacity' in changedValues) {
                currentNode.style = {
                    ...currentNode.style,
                    fillOpacity: changedValues.opacity,
                };
            }
            setStyleConfig({...styleConfig});
        },
        [nodeType, styleConfig]
    );


    const handleEdgesStyleChange = useCallback(
        changedValues => {
            const {[edgeType]: currentEdge} = styleConfig.edges;
            if ('type' in changedValues) {
                const lineType = changedValues.type;
                if (lineType === 'dashed') {
                    const {style} = currentEdge;
                    currentEdge.style = {
                        ...currentEdge.style,
                        lineDash: [style.lineWidth * 3],
                    };
                }
                else {
                    currentEdge.style = {
                        ...currentEdge.style,
                        lineDash: false,
                    };
                }
            }
            else if ('width' in changedValues) {
                const width = changedValues.width;
                const arrowWidth = width + 5;
                const d = offsetMap[width];
                currentEdge.style = {
                    ...currentEdge.style,
                    lineWidth: width,
                    endArrow: {
                        ...currentEdge.style.endArrow,
                        d: d,
                        path: `M 0,0 L ${arrowWidth} ${arrowWidth / 2}  L ${arrowWidth} ${-arrowWidth / 2} Z`,
                        lineDash: [0, 0],
                    },
                };
            }
            else if ('color' in changedValues) {
                const color = changedValues.color;
                const {endArrow} = currentEdge.style;
                currentEdge.style = {
                    ...currentEdge.style,
                    stroke: color,
                    endArrow: endArrow && {
                        ...currentEdge.style.endArrow,
                        fill: color,
                    },
                };
                currentEdge.stateStyles = {
                    ...currentEdge.stateStyles,
                    edgeActive: {
                        shadowColor: color,
                        shadowBlur: 10,
                    },
                    edgeSelected: {
                        shadowColor: color,
                        shadowBlur: 10,
                    },
                    activeByLegend: {
                        shadowColor: color,
                        shadowBlur: 8,
                    },
                    inactiveByLegend: {
                        opacity: 0.5,
                    },
                };
            }
            else if ('labelSize' in changedValues) {
                const labelCfg = currentEdge.labelCfg;
                currentEdge.labelCfg = {
                    ...labelCfg,
                    style: {
                        ...labelCfg?.style,
                        fontSize: changedValues.labelSize,
                    },
                };
            }
            else if ('labelColor' in changedValues) {
                const labelCfg = currentEdge.labelCfg;
                currentEdge.labelCfg = {
                    ...labelCfg,
                    style: {
                        ...labelCfg?.style,
                        fill: changedValues.labelColor,
                    },
                };
            }
            else if ('edgeAnimation' in changedValues) {
                const edgeAnimation = changedValues.edgeAnimation;
                currentEdge.type = edgeAnimation ? 'runningLine' : 'line';
            }
            setStyleConfig({...styleConfig});
        },
        [edgeType, styleConfig]
    );

    const hanleFormTypeChange = useCallback(
        value => {
            setFormType(value);
        },
        []
    );

    const handleItemTypeChange = useCallback(
        value => {
            if (isNodeForm) {
                setNodeType(value);
                const {[value]: currentNode = {}} = styleConfig.nodes;
                const {type, size, style = {}, labelCfg = {}, icon = {}} = currentNode;
                const {
                    fill: fillColor,
                    stroke: strokeColor,
                    lineWidth,
                    fillOpacity,
                } = style;
                const {
                    style: labelStyle,
                } = labelCfg;
                const {
                    fontSize: labelSize,
                    fill: labelColor,
                } = labelStyle;
                const {
                    _iconName: iconName,
                    fill: iconColor,
                    show: showIcon,
                } = icon;
                let nodeSize;
                if (_.isArray(size)) {
                    nodeSize = (size[0] + size [1]) / 2;
                }
                else {
                    nodeSize = size;
                }
                nodesForm.setFieldsValue({
                    shape: type,
                    size: nodeSize,
                    strokeColor,
                    lineWidth,
                    fillColor,
                    iconType: showIcon ? iconName : '',
                    iconColor: showIcon ? iconColor : '',
                    labelSize,
                    labelColor,
                    opacity: fillOpacity,
                });
            }
            else {
                setEdgeType(value);
                const {[value]: currentEdge = {}} = styleConfig.edges;
                const {type: edgeType, style = {}, labelCfg = {}} = currentEdge;
                const {
                    lineWidth,
                    stroke: strokeColor,
                    lineDash,
                } = style;
                const {
                    style: labelStyle,
                } = labelCfg;
                const {
                    fontSize: labelSize,
                    fill: labelColor,
                } = labelStyle;
                nodesForm.setFieldsValue({
                    type: lineDash ? 'dashed' : 'line',
                    width: lineWidth,
                    color: strokeColor,
                    labelSize,
                    labelColor,
                    edgeAnimation: edgeType === 'runningLine',
                });
            }
        },
        [isNodeForm, nodesForm, styleConfig.edges, styleConfig.nodes]
    );

    const renderNodesConfigForm = () => {
        return (
            <Form
                form={nodesForm}
                labelCol={{span: 6}}
                wrapperCol={{span: 18}}
                onValuesChange={handleNodesStyleChange}
            >
                <Form.Item name="shape" label="形状" required>
                    <Select
                        options={shapesOptions}
                        disabled={isFormDisabled}
                        placeholder="请选择"
                    />
                </Form.Item>
                <Form.Item name="size" label="大小" required>
                    <Select
                        options={sizeOptions}
                        disabled={isFormDisabled}
                        placeholder="请选择"
                    />
                </Form.Item>
                <Form.Item name="strokeColor" label="边框色" required>
                    <InputColorSelect disable={isFormDisabled} />
                </Form.Item>
                <Form.Item name="lineWidth" label="边框粗细" required>
                    <Select
                        options={strokeWidthOptions}
                        disabled={isFormDisabled}
                        placeholder="请选择"
                    />
                </Form.Item>
                <Form.Item name="fillColor" label="填充色" required>
                    <InputColorSelect disable={isFormDisabled} />
                </Form.Item>
                <Form.Item name="iconType" label="图标样式">
                    <IconSelect disabled={isFormDisabled} />
                </Form.Item>
                <Form.Item name="iconColor" label="图标颜色">
                    <InputColorSelect disable={isFormDisabled} />
                </Form.Item>
                <Form.Item name="labelSize" label="标签大小" required>
                    <Select
                        options={fontSizeOptions}
                        disabled={isFormDisabled}
                        placeholder="请选择"
                    />
                </Form.Item>
                <Form.Item name="labelColor" label="标签颜色" required>
                    <InputColorSelect disable={isFormDisabled} />
                </Form.Item>
                <Form.Item name="opacity" label="节点透明度" required>
                    <Select
                        options={opacityOptions}
                        disabled={isFormDisabled}
                        placeholder="请选择"
                    />
                </Form.Item>
            </Form>
        );
    };

    const renderEdgesConfigForm = () => {
        return (
            <Form
                form={edgesForm}
                labelCol={{span: 6}}
                wrapperCol={{span: 18}}
                onValuesChange={handleEdgesStyleChange}
            >
                <Form.Item name="type" label="类别" required>
                    <Select
                        options={lineTypeOptions}
                        disabled={isFormDisabled}
                        placeholder="请选择"
                    />
                </Form.Item>
                <Form.Item name="width" label="边的粗细" required>
                    <Select
                        options={lineWidthOptions}
                        disabled={isFormDisabled}
                        placeholder="请选择"
                    />
                </Form.Item>
                <Form.Item name="color" label="边的颜色" required>
                    <InputColorSelect disable={isFormDisabled} />
                </Form.Item>
                <Form.Item name="labelSize" label="标签大小" required>
                    <Select
                        options={fontSizeOptions}
                        disabled={isFormDisabled}
                        placeholder="请选择"
                    />
                </Form.Item>
                <Form.Item name="labelColor" label="标签颜色" required>
                    <InputColorSelect disable={isFormDisabled} />
                </Form.Item>
                <Form.Item
                    name="edgeAnimation"
                    label="边动画"
                    valuePropName="checked"
                >
                    <Switch
                        checkedChildren="ON"
                        unCheckedChildren="OFF"
                        disabled={isFormDisabled}
                    />
                </Form.Item>
            </Form>
        );
    };

    const types = isNodeForm ? style.nodes : style.edges;
    const typeOptions = Object.keys(types).map(type => ({value: type, label: type}));

    return (
        <Modal
            title="外观设置"
            open={visible}
            onOk={handleModalOk}
            onCancel={handleCancel}
            width={600}
            className={c.styleConfigModal}
            forceRender
        >
            <div className={c.title}>基础属性</div>
            <Form
                labelCol={{span: 6}}
                wrapperCol={{span: 18}}
            >
                <Form.Item label="点外观/边外观" required>
                    <Select
                        options={formTypeOptions}
                        value={formType}
                        onChange={hanleFormTypeChange}
                    />
                </Form.Item>
                <Form.Item label="类型" required>
                    <Select
                        options={typeOptions}
                        value={isNodeForm ? nodeType : edgeType}
                        onChange={handleItemTypeChange}
                        placeholder="请选择"
                    />
                </Form.Item>
            </Form>
            <Divider />
            <div className={c.title}>外观配置</div>
            {isNodeForm ? renderNodesConfigForm() : renderEdgesConfigForm()}
        </Modal>
    );
};

export default ConfigModal;
