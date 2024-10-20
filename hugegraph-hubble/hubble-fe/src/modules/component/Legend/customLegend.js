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

import {Util} from '@antv/g6-core';
import {Legend} from '@antv/g6';
import {Canvas} from '@antv/g-canvas';
class CustomLegend extends Legend {
    render() {
        this.processData();
        let lc = this.get('legendCanvas');
        if (!lc) {
            lc = new Canvas({
                container: this.get('container'),
                width: 200,
                height: 200,
            });
            const rootGroup = lc.addGroup({name: 'root'});
            rootGroup.addGroup({name: 'node-group'});
            rootGroup.addGroup({name: 'edge-group'});
            this.set('legendCanvas', lc);
        }
        const group = lc.find(e => e.get('name') === 'root');
        const nodeGroup = group.find(e => e.get('name') === 'node-group');
        const edgeGroup = group.find(e => e.get('name') === 'edge-group');
        const itemsData = this.get('itemsData');
        const itemTypes = ['nodes', 'edges'];
        const itemGroup = [nodeGroup, edgeGroup];
        itemTypes.forEach((itemType, i) => {
            itemsData[itemType].forEach(data => {
                const subGroup = itemGroup[i].addGroup({
                    id: data.id,
                    name: 'node-container',
                });
                let attrs;
                let shapeType = data.type;
                const {width, height, r} = this.getShapeSize(data);
                const style = this.getStyle(itemType.substr(0, 4), data);
                switch (data.type) {
                    case 'circle':
                        attrs = {r, x: 0, y: 0};
                        break;
                    case 'line':
                        attrs = {x1: -width / 2, y1: 0, x2: width / 2, y2: 0};
                        shapeType = 'line';
                        break;
                    default:
                        attrs = {r, x: 0, y: 0};
                        break;
                }
                const keyShape = subGroup.addShape(shapeType, {
                    attrs: {...attrs, ...style},
                    name: `${data.type}-node-keyShape`,
                    oriAttrs: {opacity: 1, ...style},
                });
                if (data.label) {
                    const keyShapeBBox = keyShape.getBBox();
                    const labelStyle = data.labelCfg?.style || {};
                    const attrs = {
                        textAlign: 'begin',
                        fontSize: 12,
                        textBaseline: 'middle',
                        fill: '#000',
                        opacity: 1,
                        fontWeight: 'normal',
                        ...labelStyle,
                    };
                    subGroup.addShape('text', {
                        attrs: {
                            x: keyShapeBBox.maxX + 4,
                            y: 0,
                            text: data.label,
                            ...attrs,
                        },
                        className: 'legend-label',
                        name: `${data.type}-node-text`,
                        oriAttrs: attrs,
                    });
                }
            });
        });
        const padding = this.get('padding');

        let titleShape;
        let titleGroup = group.find(e => e.get('name') === 'title-container');
        let titleGroupBBox = {height: 0, maxY: 0, width: 0};
        if (this.get('title')) {
            if (!titleGroup) {
                titleGroup = group.addGroup({
                    name: 'title-container',
                });
            }
            const defaultTitleStyle = {
                fontSize: 20,
                fontFamily: 'Arial',
                fontWeight: 300,
                textBaseline: 'top',
                textAlign: 'center',
                fill: '#000',
                x: 0,
                y: padding[0],
            };
            const titleConfig = this.get('titleConfig') || {};
            const style = Object.assign(defaultTitleStyle, titleConfig.style || {});
            titleShape = titleGroup.addShape('text', {
                attrs: {
                    text: this.get('title'),
                    ...style,
                },
            });
            titleGroupBBox = titleGroup.getCanvasBBox();
            titleGroup.setMatrix([1, 0, 0, 0, 1, 0, titleConfig.offsetX, titleConfig.offsetY, 1]);
        }

        this.layoutItems();
        let lcBBox = group.getCanvasBBox();

        let nodeGroupBBox = nodeGroup.getCanvasBBox();
        // 若有图形超过边界的情况，平移回来
        let nodeGroupBeginX = nodeGroupBBox.minX < 0 ? Math.abs(nodeGroupBBox.minX) + padding[3] : padding[3];
        let nodeGroupBeginY = titleGroupBBox.maxY < nodeGroupBBox.minY
            ? Math.abs(titleGroupBBox.maxY - nodeGroupBBox.minY) + padding[0]
            : titleGroupBBox.maxY + padding[0];
        let nodeGroupMatrix = [1, 0, 0, 0, 1, 0, nodeGroupBeginX, nodeGroupBeginY, 1];
        nodeGroup.setMatrix(nodeGroupMatrix);
        lcBBox = group.getCanvasBBox();
        let size = [lcBBox.minX + lcBBox.width + padding[1], lcBBox.minY + lcBBox.height + padding[2]];
        // 根据 size 和 titleConfig 调整 title 位置，再调整 nodeGroup 位置
        if (titleShape) {
            const titleConfig = {
                position: 'center',
                offsetX: 0,
                offsetY: 0,
                ...this.get('titleConfig'),
            };
            titleGroupBBox = titleGroup.getCanvasBBox();
            const titleGroupMatrix = titleGroup.getMatrix() || [1, 0, 0, 0, 1, 0, 0, 0, 1];
            if (titleConfig.position === 'center') {
                titleGroupMatrix[6] = size[0] / 2 + titleConfig.offsetX;
            }
            else if (titleConfig.position === 'right') {
                titleGroupMatrix[6] = size[0] - padding[3] + titleConfig.offsetX;
                titleShape.attr({textAlign: 'right'});
            }
            else {
                titleGroupMatrix[6] = padding[3] + titleConfig.offsetX;
                titleShape.attr({textAlign: 'left'});
            }
            titleGroup.setMatrix(titleGroupMatrix);
            titleGroupBBox = titleGroup.getCanvasBBox();
            // 若有图形超过边界的情况，平移回来
            nodeGroupBeginX = nodeGroupBBox.minX < 0 ? Math.abs(nodeGroupBBox.minX) + padding[3] : padding[3];
            nodeGroupBeginY = nodeGroupBBox.minY < titleGroupBBox.maxY
                ? Math.abs(titleGroupBBox.maxY - nodeGroupBBox.minY) + padding[0]
                : titleGroupBBox.maxY + padding[0];
            nodeGroupMatrix = [1, 0, 0, 0, 1, 0, nodeGroupBeginX, nodeGroupBeginY, 1];
            nodeGroup.setMatrix(nodeGroupMatrix);

            const edgeGroupMatrix = [1, 0, 0, 0, 1, 0, nodeGroupBeginX, nodeGroupBeginY, 1];
            if (this.get('layout') === 'vertical') {
                edgeGroupMatrix[6] += nodeGroupBBox.maxX + this.get('horiSep');
            }
            else {
                edgeGroupMatrix[7] += nodeGroupBBox.maxY + this.get('vertiSep');
            }
            edgeGroup.setMatrix(edgeGroupMatrix);
        }
        else {
            // 没有 title，也需要平移 edgeGroup
            nodeGroupBBox = nodeGroup.getCanvasBBox();
            const edgeGroupMatrix = [1, 0, 0, 0, 1, 0, 0, 0, 1];
            if (this.get('layout') === 'vertical') {
                edgeGroupMatrix[6] += nodeGroupMatrix[6] + nodeGroupBBox.maxX + this.get('horiSep');
            }
            else {
                edgeGroupMatrix[7] += nodeGroupMatrix[7] + nodeGroupBBox.maxY + this.get('vertiSep');
            }
            edgeGroup.setMatrix(edgeGroupMatrix);
        }
        lcBBox = group.getCanvasBBox();
        nodeGroupBBox = nodeGroup.getCanvasBBox();
        nodeGroupMatrix = nodeGroup.getMatrix() || [1, 0, 0, 0, 1, 0, 0, 0, 1];
        const edgeGroupMatrix = edgeGroup.getMatrix() || [1, 0, 0, 0, 1, 0, 0, 0, 1];
        const edgeGroupBBox = edgeGroup.getCanvasBBox();
        size = [
            Math.max(nodeGroupBBox.width + nodeGroupMatrix[6], edgeGroupBBox.width + edgeGroupMatrix[6]) + padding[1],
            Math.max(nodeGroupBBox.height + nodeGroupMatrix[7], edgeGroupBBox.height + edgeGroupMatrix[7]) + padding[2],
        ];
        lc.changeSize(size[0], size[1]);

        // 更新容器背景样式
        const containerStyle = this.get('containerStyle');
        const viewportMatrix = group.getMatrix() || [1, 0, 0, 0, 1, 0, 0, 0, 1];
        const beginPos = Util.invertMatrix({x: 0, y: 0}, viewportMatrix);
        const backRect = group.addShape('rect', {
            attrs: {
                x: beginPos.x + (containerStyle.lineWidth || 1),
                y: beginPos.y + (containerStyle.lineWidth || 1),
                width: size[0] - 2 * (containerStyle.lineWidth || 1),
                height: size[1] - 2 * (containerStyle.lineWidth || 1),
                fill: '#f00',
                stroke: '#000',
                lineWidth: 1,
                opacity: 0.5,
                ...containerStyle,
            },
            name: 'legend-back-rect',
            capture: false,
        });
        backRect.toBack();
        return size;
    }

    layoutItems() {
        const lc = this.get('legendCanvas');
        const horiSep = this.get('horiSep');
        const vertiSep = this.get('vertiSep');
        const layout = this.get('layout');
        const align = this.get('align');
        const begin = [0, 0];

        const group = lc.find(e => e.get('name') === 'root');
        const nodeGroup = group.find(e => e.get('name') === 'node-group');
        const edgeGroup = group.find(e => e.get('name') === 'edge-group');

        const nodeLegendSize = {
            min: 0,
            max: -Infinity,
        };
        let rowMaxY = -Infinity;
        nodeGroup.get('children').forEach((cNodeGroup, i) => {
            if (i === 0) {
                nodeLegendSize.min = begin[0];
            }
            const keyShape = cNodeGroup.get('children')[0];
            const bbox = cNodeGroup.getCanvasBBox();
            const {width: keyShapeWidth, height: keyShapeHeight} = keyShape.getBBox();
            let curHeight = 0;
            let x = 0;
            let y = 0;
            if (layout === 'vertical') {
                x = begin[1];
                y = begin[0] + keyShapeWidth / 2;
                begin[0] = y + bbox.height + vertiSep;
                curHeight = bbox.maxX + x + keyShapeWidth / 2;
            }
            else {
                x = begin[0] + keyShapeWidth / 2;
                y = begin[1];
                begin[0] = x + bbox.width + horiSep;
                curHeight = bbox.maxY + y + keyShapeHeight / 2;
            }
            if (begin[0] > nodeLegendSize.max) {
                nodeLegendSize.max = begin[0];
            }
            if (curHeight > rowMaxY) {
                rowMaxY = curHeight;
            }
            cNodeGroup.setMatrix([1, 0, 0, 0, 1, 0, x, y, 1]);
        });
        const nw = nodeLegendSize.max - nodeLegendSize.min;

        const edgeLegendSize = {
            min: 0,
            max: -Infinity,
        };
        begin[0] = 0;
        begin[1] = 100;
        edgeGroup.get('children').forEach((subGroup, i) => {
            if (i === 0) {
                edgeLegendSize.min = begin[0];
            }
            const keyShapeHeight = 7;
            let y = begin[0];
            begin[0] = y + 23.5;
            subGroup.setMatrix([1, 0, 0, 0, 1, 0, 0, y + keyShapeHeight / 2, 1]);
            if (begin[0] > edgeLegendSize.max) {
                edgeLegendSize.max = begin[0];
            }
        });
        const ew = edgeLegendSize.max - edgeLegendSize.min;
        if (align && align !== '' && align !== 'left') {
            const widthDiff = nw - ew;
            const movement = align === 'center' ? Math.abs(widthDiff) / 2 : Math.abs(widthDiff);
            const shouldAdjustGroup = widthDiff < 0 ? nodeGroup : edgeGroup;
            shouldAdjustGroup.get('children').forEach(subGroup => {
                const matrix = subGroup.getMatrix() || [1, 0, 0, 0, 1, 0, 0, 0, 1];
                if (layout === 'vertical') {
                    matrix[7] += movement;
                }
                else {
                    matrix[6] += movement;
                }
                subGroup.setMatrix(matrix);
            });
        }
    }
}

export default CustomLegend;
