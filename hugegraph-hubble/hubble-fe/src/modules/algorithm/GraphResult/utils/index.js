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

import * as api from '../../../../api/index';
import {message} from 'antd';
import {formatToDownloadData, formatToGraphData,
    formatToOptionedGraphData} from '../../../../utils/formatGraphResultData';
import {processParallelEdges} from '../../../../utils/graph';
import {clearSelectedStates} from '../../../../utils/handleGraphState';
import _ from 'lodash';

const fetchExpandInfo = async (params, graphInstance, graphSpaceInfo) => {
    const {graphSpace, graph} = graphSpaceInfo;
    const response = await api.analysis.putExecutionQuery(graphSpace, graph, params);
    if (response.status !== 200) {
        message.error('展开失败');
        return;
    }
    const {vertices, edges} = response.data.graph_view;
    if (vertices.length === 0) {
        message.warning('不存在更多邻接点');
        return;
    }
    const tmp = {vertices: [], edges: []};
    const rawGraphData = formatToDownloadData(graphInstance.save());
    const verticesIds = rawGraphData.vertices.map(item => item.id);
    const edgesIds = rawGraphData.edges.map(item => item.id);
    for (let item of vertices) {
        if (!verticesIds.includes(item.id)) {
            tmp.vertices.push(item);
        }
    }
    for (let item of edges) {
        if (!edgesIds.includes(item.id)) {
            tmp.edges.push(item);
        }
    }
    if (tmp.vertices.length === 0) {
        message.warning('不存在更多邻接点');
        return;
    }
    const searchResultRaw = tmp;
    return searchResultRaw;
};

const handleAddGraphNode = (data, metaData, styleConfigData, graph) => {
    const addItem = data.vertices;
    const {id} = addItem[0];
    const newStyledData = formatToGraphData(data, metaData, styleConfigData);
    const styledItem = newStyledData.nodes[0];
    graph.addItem('node', styledItem, false);
    const {layout} = graph.cfg;
    if (layout) {
        graph.destroyLayout();
        graph.updateLayout(layout);
        graph.refresh();
    }
    clearSelectedStates(graph);
    const instance = graph.findById(id);
    graph.setItemState(instance, 'addActive', true);
    return styledItem;
};

const handleAddGraphEdge = (data, metaData, graphData, styleConfigData, graph) => {
    const addItem = data.edges;
    const {id} = addItem[0];
    const {nodes, edges} = graphData;
    const newStyledData = formatToGraphData(data, metaData, styleConfigData);
    const processedEdges = processParallelEdges([...edges, newStyledData.edges[0]]);
    const styledItem = _.find(processedEdges, {id: id});
    graph.addItem('edge', styledItem, false);
    const newGraphData = {edges: processedEdges, nodes};
    clearSelectedStates(graph);
    const instance = graph.findById(id);
    graph.setItemState(instance, 'addActive', true);
    return newGraphData;
};

const handleExpandGraph = (newData, metaData, styleConfigData, options, algorithmName, graphInstance) => {
    const newRawGraphData = formatToGraphData(newData, metaData, styleConfigData);
    const newGraphData = formatToOptionedGraphData(newRawGraphData, options, algorithmName);
    const saveData = graphInstance.save();
    const nodes = [];
    const edges = [];
    const saveNodeIds = saveData.nodes.map(item => {
        const {id, icon, label, labelCfg, itemType, metaConfig, properties, size, style, comboId} = item;
        nodes.push({id, icon, label, labelCfg,
            itemType, metaConfig, properties, size, style, legendType: itemType, comboId});
        return id;
    });
    const saveEdgesIds = saveData.edges.map(item => {
        const {id, label, labelCfg, itemType, loopCfg, metaConfig, properties,
            source, stateStyles, style, target, type} = item;
        edges.push({id, label, labelCfg, itemType, loopCfg, metaConfig, properties,
            source, stateStyles, style, target, type, legendType: itemType});
        return id;
    });
    newGraphData.nodes.map(item => {
        if (!saveNodeIds.includes(saveData)) {
            nodes.push(item);
        }
    });
    newGraphData.edges.map(item => {
        if (!saveEdgesIds.includes(saveData)) {
            edges.push(item);
        }
    });
    graphInstance.changeData({nodes, edges, combos: saveData?.combos}, true);
    graphInstance.refresh();
    return {nodes, edges, combos: saveData?.combos};
};


export {
    fetchExpandInfo,
    handleAddGraphNode,
    handleAddGraphEdge,
    handleExpandGraph,
};
