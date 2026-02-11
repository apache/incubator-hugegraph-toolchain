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
 * @file  自定义节点
 * @author
 */

import icons from '../utils/graph';
import G6 from '@antv/g6';
import {useEffect} from 'react';

const getBadgePosition = (size, type) => {
    let badgeX = 0;
    let badgeY = 0;
    if (type === 'circle') {
        const r = size / 2;
        badgeX = r * Math.cos((Math.PI * 7) / 4);
        badgeY = -r * Math.sin((Math.PI * 7) / 4);
    }
    else if (type === 'diamond') {
        const r = size / 2;
        badgeX = r / 2;
        badgeY = r / 2;
    }
    else if (type === 'triangle') {
        const r = size;
        badgeX = r * Math.cos((Math.PI) / 6);
        badgeY = r - 5;
    }
    else if (type === 'star') {
        const r = size / 2;
        badgeX = r;
        badgeY = 1.39 * r;
    }
    else if (type === 'ellipse') {
        badgeX = size[0] / 4;
        badgeY = size[1] / 3;
    }
    return {
        x: badgeX,
        y: badgeY,
    };
};

const drawBadge = (group, size, type) => {
    const [width, height] = [10, 10];
    const {x: badgeX, y: badgeY} = getBadgePosition(size, type);
    if (width === height) {
        const shape = {
            attrs: {
                r: 5,
                fill: 'grey',
                x: badgeX,
                y: badgeY,
            },
            name: 'badges-circle',
            id: 'badges-circle',
        };
        group.addShape('circle', shape);
    }
    group.addShape('text', {
        attrs: {
            x: badgeX,
            y: badgeY,
            fontFamily: 'graphin',
            text: icons.pushpin,
            textAlign: 'center',
            textBaseline: 'middle',
            fontSize: 8,
            color: '#fff',
            fill: '#fff',
        },
        capture: false,
        name: 'badges-text',
        id: 'badges-text',
    });
};

const removeBadge = group => {
    const a = group.findById('badges-circle');
    group.removeChild(a);
    const b = group.findById('badges-text');
    group.removeChild(b);
};

const options = {
    setState(name, value, item) {
        if (!name) {
            return;
        }
        const group = item.getContainer();
        const groupChildren = group?.get('children');
        if (groupChildren) {
            const shape = group?.get('children')[0];
            const model = item.getModel();
            const {stateStyles = {}, size, type} = model;
            const currentStateStyle = stateStyles[name] || '';
            const status = item._cfg?.states || [];
            if (value) {
                Object.entries(currentStateStyle).forEach(
                    item => {
                        shape.attr(item[0], item[1]);
                    }
                );
                // 固定节点增加样式
                if (name === 'customFixed') {
                    // 如果有icon就不添加
                    const badgeGroup = group.findById('badges-circle');
                    if (badgeGroup == null) {
                        drawBadge(group, size, type);
                    }
                }
            }
            else {
                if (name === 'customFixed') {
                    removeBadge(group);
                }
                Object.entries(currentStateStyle).forEach(
                    item => {
                        const [key] = item;
                        shape.attr(key, model.style[key]);
                    }
                );
                // 如果有其他状态 设置过去；
                if (status.length > 0) {
                    status.forEach(key => {
                        const currentStateStyle = stateStyles[key] || {};
                        Object.entries(currentStateStyle).forEach(
                            item => {
                                shape.attr(item[0], item[1]);
                            }
                        );
                        if (name === 'customFixed') {
                            // 如果有icon就不添加
                            const badgeGroup = group.findById('badges-circle');
                            if (badgeGroup == null) {
                                drawBadge(group, size, type);
                            }
                        }
                    }
                    );
                }
            }
        }
    },
};

const useCustomNode = () => {
    useEffect(
        () => {
            G6.registerNode('circle', options, 'circle');
            G6.registerNode('diamond', options, 'diamond');
            G6.registerNode('triangle', options, 'triangle');
            G6.registerNode('star', options, 'star');
            G6.registerNode('ellipse', options, 'ellipse');
        },
        []
    );
};

export default useCustomNode;
