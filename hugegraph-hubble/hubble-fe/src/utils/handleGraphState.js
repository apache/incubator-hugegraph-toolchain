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
 * 清除所有边特定状态
 * @param {*} graph
 * @param {*} stateList
 */
const clearEdgesStates  = (graph, stateList) => {
    graph.getEdges().forEach(edge => {
        graph.clearItemStates(edge, stateList);
    });
};

/**
 * 清除所有点特定状态
 * @param {*} graph
 * @param {*} stateList
 */
const clearNodesStates  = (graph, stateList) => {
    graph.getNodes().forEach(node => {
        graph.clearItemStates(node, stateList);
    });
};


/**
 * 清除特定边特定状态
 * @param {*} graph
 * @param {*} item
 * @param {*} stateList
 */
const clearItemStates  = (graph, item, stateList) => {
    graph.clearItemStates(item, stateList);
};

/**
 * 清除特定边特定状态
 * @param {*} graph
 */
const clearSelectedStates  = graph => {
    graph.getEdges().forEach(edge => {
        graph.clearItemStates(edge, ['edgeSelected']);
    });
    graph.getNodes().forEach(node => {
        graph.clearItemStates(node, ['customSelected']);
    });
};

/**
 * 高亮节点的关联边
 * @param {*} graph
 */
const highLightRelatedEdges = (graph, node) => {
    graph.getEdges().forEach(edge => {
        if (edge.getSource() === node || edge.getTarget() === node) {
            graph.setItemState(edge, 'edgeActive', true);
        }
    });
};

/**
 * 高亮或普通标签
 * @param {*} graph
 * @param {*} item
 * @param {*} labelType
 */
const setItemLabelState = (graph, item, labelType) => {
    const model = item.getModel();
    const newModel = {
        ...model,
        labelCfg: {
            ...model.labelCfg,
            style: {
                ...model.labelCfg.style,
                fontWeight: labelType,
            },
        },
    };
    graph.updateItem(item, newModel, false);
};

export {
    clearEdgesStates,
    clearNodesStates,
    clearItemStates,
    clearSelectedStates,
    highLightRelatedEdges,
    setItemLabelState,
};
