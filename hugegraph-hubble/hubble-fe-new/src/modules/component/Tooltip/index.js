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
 * @file  Tooltip
 * @author
 */

import G6 from '@antv/g6';
import {useContext, useEffect} from 'react';
import {GraphContext} from '../Context';
import {EDGELABEL_TYPE, EDGE_TYPE, EDGELABEL_TYPE_NAME} from '../../../utils/constants';
import c from './index.module.scss';

const Tooltip = () => {
    const {graph} = useContext(GraphContext);

    const getContent = e => {
        const model = e.item.getModel();
        const {type, id, itemType, properties, metaConfig} = model || {};
        const elementType = EDGE_TYPE.includes(type) ? '边' : '节点';
        const {edgelabel_type} = metaConfig || {};
        const container = document.createElement('div');
        const typeInfoDiv = document.createElement('div');
        // 类型
        const typeSpan = document.createElement('span');
        typeSpan.className = c.item;
        typeSpan.innerText = `${elementType}类型：${itemType}`;
        typeInfoDiv.appendChild(typeSpan);

        // 标签
        if (EDGE_TYPE.includes(type) && edgelabel_type !== 'NORMAL') {
            const tagSpan = document.createElement('span');
            tagSpan.className = edgelabel_type === EDGELABEL_TYPE.PARENT ? c.tagGlod : c.tagBlue;
            tagSpan.innerText = EDGELABEL_TYPE_NAME[edgelabel_type];
            typeInfoDiv.insertBefore(tagSpan, typeSpan.nextSibling);
        }
        container.appendChild(typeInfoDiv);
        const idDiv = document.createElement('div');
        idDiv.className = c.item;
        idDiv.innerText = `${elementType}ID：${id}`;
        container.appendChild(idDiv);
        for (const [key, value] of Object.entries(properties)) {
            const propertyDiv = document.createElement('div');
            propertyDiv.className = c.item;
            propertyDiv.innerText = `${key}：${value}`;
            container.appendChild(propertyDiv);
        }
        return container;
    };

    useEffect(
        () => {
            const options = {
                itemTypes: ['node', 'edge'],
                offsetX: 15,
                offsetY: -100,
                getContent,
            };
            const tooltip = new G6.Tooltip(options);
            if (graph && !graph.destroyed) {
                graph.addPlugin(tooltip);
            }
            return () => {
                if (graph && !graph.destroyed) {
                    graph.removePlugin(tooltip);
                }
            };
        },
        [graph]
    );

    return null;
};

export default Tooltip;
