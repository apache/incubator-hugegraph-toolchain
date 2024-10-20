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

import request from './request';
import qs from 'qs';

// 图分析通用
const getGraphSpaceList = () => {
    return request.get('/graphspaces/list');
};

const getGraphList = graphSpace => {
    return request.get(`/graphspaces/${graphSpace}/graphs/list`);
};

const getOlapMode = (graphSpace, graph) => {
    return request.get(`/graphspaces/${graphSpace}/graphs/${graph}/graph_read_mode`);
};

const switchOlapMode = (graphSpace, graph, params) => {
    return request.put(`/graphspaces/${graphSpace}/graphs/${graph}/graph_read_mode`, params);
};

const getUploadList = (graphspace, graph) => {
    return `/api/v1.3//graphspaces/${graphspace}/graphs/${graph}/import`;
};

const getMetaEdgeList = (graphspace, graph) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/schema/edgelabels`);
};

const getMetaVertexList = (graphspace, graph, params) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/schema/vertexlabels`);
};

const updateVertexProperties = (graphspace, graph, params) => {
    return request.put(`/graphspaces/${graphspace}/graphs/${graph}/vertex/${params.id}`, params);
};

const updateEdgeProperties = (graphspace, graph, params) => {
    return request.put(`/graphspaces/${graphspace}/graphs/${graph}/edge/${params.id}`, params);
};

const getVertexProperties = (graphspace, graph, params) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/vertexlabel/${params}`);
};

const getEdgeProperties = (graphspace, graph, params) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/edgelabel/${params}`);
};

const getExecutionLogs = (graphspace, graph, params) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/execute-histories`, {params});
};

const getExecutionQuery = (graphspace, graph, params) => {
    return request.post(`/graphspaces/${graphspace}/graphs/${graph}/gremlin-query`, params);
};

const getGraphData = (graphspace, graph) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/gremlin-query`);
};

const putExecutionQuery = (graphspace, graph, params) => {
    return request.put(`/graphspaces/${graphspace}/graphs/${graph}/gremlin-query`, params);
};

const getCypherExecutionQuery = (graphspace, graph, params) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/cypher`, {params});
};

const getExecutionTask = (graphspace, graph, params) => {
    return request.post(`/graphspaces/${graphspace}/graphs/${graph}/gremlin-query/async-task`, params);
};

const getCypherTask = (graphspace, graph, params) => {
    return request.post(`/graphspaces/${graphspace}/graphs/${graph}/cypher/async-task`, params);
};

const fetchManageTaskList = (graphspace, graph, params) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/async-tasks?`, {params});
};

const addFavoriate = (graphspace, graph, params) => {
    return request.post(`graphspaces/${graphspace}/graphs/${graph}/gremlin-collections`, params);
};

const deleteQueryCollection = (graphspace, graph, id) => {
    return request.delete(`graphspaces/${graphspace}/graphs/${graph}/gremlin-collections/${id}`);
};

const editQueryCollection  = (graphspace, graph, parmas) => {
    return request.put(`graphspaces/${graphspace}/graphs/${graph}/gremlin-collections/${parmas.id}`, parmas);
};

const fetchFavoriteQueries = (graphspace, graph, params) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/gremlin-collections?`, {params});
};

const addGraphNode = (graphspace, graph, params) => {
    return request.post(`/graphspaces/${graphspace}/graphs/${graph}/vertex`, params);
};

const fetchEdgeLabels = (graphspace, graph, params) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/schema/edgelabels/${params}`);
};

const fetchVertexlinks = (graphspace, graph, params) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/schema/vertexlabels/${params}/link`);
};

const addEdge = (graphspace, graph, params) => {
    return request.post(`/graphspaces/${graphspace}/graphs/${graph}/edge`, params);
};


// 图算法
const postOlapInfo =  (graphspace, graph, data) => {
    return request.post(`/graphspaces/${graphspace}/graphs/${graph}/algorithms/olap`, data);
};

const runOltpInfo =  (graphspace, graph, data) => {
    return request.post(`/graphspaces/${graphspace}/graphs/${graph}/algorithms/oltp/` + data.algorithmName, data);
};

const runOlapVermeer =  (graphspace, graph, params) => {
    return request.post(`/graphspaces/${graphspace}/graphs/${graph}/algorithms/vermeer`, {params});
};

// 任务管理
const fetchAsyncTaskResult = (graphspace, graph, taskId) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/async-tasks/${taskId}`);
};

const deleteAsyncTask = (graphspace, graph, selectedTaskIds) => {
    const params  = qs.stringify(selectedTaskIds, {arrayFormat: 'repeat'});
    return request.delete(`/graphspaces/${graphspace}/graphs/${graph}/async-tasks?` + params);
};

const abortAsyncTask = (graphspace, graph, taskId) => {
    return request.post(`/graphspaces/${graphspace}/graphs/${graph}/async-tasks/cancel/${taskId}`);
};

const getExecuteAsyncTaskList = (graphspace, graph, params) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/execute-histories`, {params});
};

const loadVermeerTask = params => {
    return request.post('/vermeer/task', {...params});
};

export {
    getGraphSpaceList,
    getGraphList,
    getOlapMode,
    switchOlapMode,
    getExecutionLogs,
    getExecutionQuery,
    getGraphData,
    putExecutionQuery,
    getExecutionTask,
    getCypherTask,
    fetchManageTaskList,
    addFavoriate,
    postOlapInfo,
    runOltpInfo,
    runOlapVermeer,
    fetchAsyncTaskResult,
    addGraphNode,
    deleteAsyncTask,
    abortAsyncTask,
    getMetaEdgeList,
    getMetaVertexList,
    deleteQueryCollection,
    editQueryCollection,
    fetchFavoriteQueries,
    getUploadList,
    updateVertexProperties,
    updateEdgeProperties,
    getCypherExecutionQuery,
    getVertexProperties,
    getEdgeProperties,
    getExecuteAsyncTaskList,
    fetchEdgeLabels,
    fetchVertexlinks,
    addEdge,
    loadVermeerTask,
};
