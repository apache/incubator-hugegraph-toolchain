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
 * @file 3D ForceGraph
 * @author
 */

import React, {useEffect, useMemo, useRef} from 'react';
import * as THREE from 'three';
import ForceGraph3D from '3d-force-graph';
import {EDGELABEL_TYPE, EDGELABEL_TYPE_NAME} from '../../../utils/constants';
import _ from 'lodash';
import c from './index.module.scss';

const formatLabelString = (type, label, id, propertiesStr, edgeLabelType) => {
    let tagStr = '';
    if (edgeLabelType && edgeLabelType !== EDGELABEL_TYPE.NORMAL) {
        const classname = edgeLabelType === EDGELABEL_TYPE.PARENT ? c.tagGlod : c.tagBlue;
        tagStr = `<span class=${classname}>${EDGELABEL_TYPE_NAME[edgeLabelType]}</span>`;
    }
    return `<div class=${c.tooltip}>
                <div>
                    <span>${type}类型: ${label}</span>
                    ${tagStr}
                </div>
                <div>${type}ID: ${id}</div>
                ${propertiesStr}
            </div>`;
};

const Canvas3D = props => {

    const {data} = props;
    const graphRef = useRef(null);

    const graphData = useMemo(
        () => ({
            nodes: data.nodes.map(node => {
                const {id, properties, itemType, label, style, size} = node;
                return {id, properties, itemType, label, style, size};
            }),
            links: data.edges.map(edge => {
                const {metaConfig = {}, curvature = 0, rotation = 0} = edge;
                const {style: metaStyle = {}, edgelabel_type: edgeLabelType} = metaConfig;
                const {with_arrow: hasArrow} = metaStyle;
                const {id, source, target, label, type, style, properties} = edge;
                return {id, source, target, label, style, type, hasArrow,
                    edgeLabelType, properties, curvature, rotation};
            }),
        }),
        [data]
    );

    useEffect(
        () => {
            const elem = document.getElementById('3d-graph');
            const MaterialMap = new Map();
            const width = elem.offsetWidth;
            const height = elem.offsetHeight;
            // eslint-disable-next-line @babel/new-cap
            graphRef.current = ForceGraph3D()(elem)
                // basicConfig
                .width(width)
                .height(height)
                .backgroundColor('rgba(0,0,0,0)')
                .enableNodeDrag(true)
                .showNavInfo(false)
                // dataInput
                .graphData(graphData)
                // nodeStyle
                .nodeLabel(node => {
                    const {itemType, id, properties} = node;
                    let propertiesStr = '';
                    for (const [key, value] of Object.entries(properties)) {
                        propertiesStr += `<div>${key}：${value}</div>`;
                    }
                    return formatLabelString('节点', itemType, id, propertiesStr);
                })
                .nodeThreeObject(node => {
                    const {itemType} = node;
                    const {size = 30, style} = node;
                    // 处理椭圆形形状
                    const nodeSize = !_.isArray(size) ? size : (size[0] + size[1]) / 2;
                    const {fill = '#ddd', fillOpacity = 0.65} = style;
                    const geometry = new THREE.SphereGeometry(nodeSize / 5);
                    let material = MaterialMap.get(itemType);
                    if (!material) {
                        material = new THREE.MeshPhongMaterial({
                            color: fill,
                            transparent: true,
                            opacity: fillOpacity,
                        });
                        MaterialMap.set(itemType, material);
                    }
                    const circle = new THREE.Mesh(geometry, material);
                    return circle;
                })
                // linkStyle
                .linkLabel(link => {
                    const {label, id, properties, edgeLabelType} = link;
                    let propertiesStr = '';
                    for (const [key, value] of Object.entries(properties)) {
                        propertiesStr += `<div>${key}：${value}</div>`;
                    }
                    return formatLabelString('边', label, id, propertiesStr, edgeLabelType);
                })
                .linkColor(links => links.style.stroke)
                .linkOpacity(0.4)
                .linkWidth(link => link.style.lineWidth / 3 || 0)
                .linkCurvature('curvature')
                .linkCurveRotation('rotation')
                // 交互
                .onNodeClick(node => {
                    const distance = 40;
                    const distRatio = 1 + distance / Math.hypot(node.x, node.y, node.z);
                    if (graphRef.current) {
                        graphRef.current.cameraPosition(
                            {
                                x: node.x * distRatio,
                                y: node.y * distRatio,
                                z: node.z * distRatio,
                            },
                            node,
                            3000
                        );
                    }
                });
            return () => {
                if (graphRef.current) {
                    graphRef.current._destructor();
                }
            };
        },
        [graphData]
    );

    return (
        <div className={c.canvas3d}>
            <div className={c.graph} id='3d-graph'></div>
            <span className={c.useGuide}>
                提示：
                <span className={c.emphasis}>鼠标左键 </span>旋转
                ｜
                <span className={c.emphasis}>鼠标右键 </span>移动
            </span>
        </div>
    );
};

export default Canvas3D;
