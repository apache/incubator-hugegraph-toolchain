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
 * 从参数中筛选节点
 * @param {*} verticesParams
 * @param {*} nodes
 */

export default function getNodesFromParams(verticesParams, nodes) {
    const {ids, label, properties} = verticesParams;
    if (ids) {
        return ids;
    }
    const result = nodes.filter(node => {
        const {label: nLabel, properties: nProperties} = node;
        if (!nLabel || !nProperties) {
            return false;
        }
        if (label !== nLabel) {
            return false;
        }
        for (const [key, value] of Object.entries(properties)) {
            if (nProperties[key] !== value) {
                return false;
            }
        }
        return true;
    }).map(item => item.id);
    return result;
}
