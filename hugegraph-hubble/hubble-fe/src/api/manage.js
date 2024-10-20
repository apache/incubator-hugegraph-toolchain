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

// 图空间
const getGraphSpaceList = params => {
    return request.get('/graphspaces', {params});
};

const getGraphSpace = graphspace => {
    return request.get(`/graphspaces/${graphspace}`);
};

const addGraphSpace = data => {
    return request.post('/graphspaces', data);
};

const updateGraphSpace = (graphspace, data) => {
    return request.put(`/graphspaces/${graphspace}`, data);
};

const delGraphSpace = graphspace => {
    return request.delete(`/graphspaces/${graphspace}`);
};

const setDefaultGraphSpace = graphspace => {
    return request.get(`/graphspaces/${graphspace}/setdefault`);
};

const getDefaultGraphSpace = () => {
    return request.get('/graphspaces/getdefault');
};

const initBuiltin = params => {
    return request.post('/graphspaces/builtin', params);
};

export {getGraphSpace, getGraphSpaceList, addGraphSpace, updateGraphSpace,
    delGraphSpace, setDefaultGraphSpace, getDefaultGraphSpace, initBuiltin};

// schema
const getSchemaList = (graphspace, params) => {
    return request.get(`/graphspaces/${graphspace}/schematemplates`, {params});
};

const addSchema = (graphspace, data) => {
    return request.post(`/graphspaces/${graphspace}/schematemplates`, data);
};

const getSchema = (graphspace, name) => {
    return request.get(`graphspaces/${graphspace}/schematemplates/${name}`);
};

const getGraphSchema = (graphspace, graph) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/schema/groovy`);
};

const exportSchema = (graphspace, graph) => {
    return request.get(`graphspaces/${graphspace}/graphs/${graph}/schema/groovy/export`);
};

const updateSchema = (graphspace, name, data) => {
    return request.put(`graphspaces/${graphspace}/schematemplates/${name}`, data);
};

const delSchema = (graphspace, name) => {
    return request.delete(`graphspaces/${graphspace}/schematemplates/${name}`);
};

export {getSchemaList, addSchema, updateSchema, getSchema, getGraphSchema, exportSchema, delSchema};

// 图
const getGraphList = (graphspace, params) => {
    return request.get(`/graphspaces/${graphspace}/graphs`, {params});
};

const addGraph = (graphspace, data) => {
    return request.post(`/graphspaces/${graphspace}/graphs`,
        qs.stringify(data),
        {headers: {'Content-Type': 'application/x-www-form-urlencoded'}}
    );
};

const updateGraph = (graphspace, graph, params) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/update`, {params});
};

const getGraph = (graphspace, graph) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/get`);
};

const delGraph = (graphspace, graph) => {
    return request.delete(`/graphspaces/${graphspace}/graphs/${graph}`);
};

const getGraphView = (graphspace, graph) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/schema/graphview`);
};

const setDefaultGraph = (graphspace, graph) => {
    return request.get(`graphspaces/${graphspace}/graphs/${graph}/setdefault`);
};

const getDefaultGraph = graphspace => {
    return request.get(`graphspaces/${graphspace}/graphs/getdefault`);
};

const clearGraphData = (graphspace, graph) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/truncate`, {
        params: {clear_schema: false, clear_data: true},
    });
};

const clearGraphDataAndSchema = (graphspace, graph) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/truncate`, {params: {clear_schema: true}});
};

const getGraphStatistic = (graphspace, graph) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/statistics`);
};

const updateGraphStatistic = (graphspace, graph) => {
    return request.post(`/graphspaces/${graphspace}/graphs/${graph}/statistics`);
};

// no-use
const getGraphStorage = (graphspace, graph) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/storage`);
};

const cloneGraph = (graphspace, graph, params) => {
    return request.post(`/graphspaces/${graphspace}/graphs/${graph}/clone`, params);
};

export {getGraphList, getGraph, addGraph, updateGraph, delGraph, getDefaultGraph,
    getGraphView, clearGraphData, setDefaultGraph, clearGraphDataAndSchema,
    getGraphStatistic, updateGraphStatistic, getGraphStorage, cloneGraph};

// meta property
const getMetaPropertyList = (graphspace, graph, params) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/schema/propertykeys`, {params});
};

const addMetaProperty = (graphspace, graph, data) => {
    return request.post(`/graphspaces/${graphspace}/graphs/${graph}/schema/propertykeys`, data);
};

const checkMetaProperty = (graphspace, graph, data) => {
    return request.post(`/graphspaces/${graphspace}/graphs/${graph}/schema/propertykeys/check_using`, data);
};

const updateMetaProperty = () => {};

const delMetaProperty = (graphspace, graph, data) => {
    const {names} = data;
    const str = names.map(name => 'names=' + encodeURIComponent(name)).join('&');
    const skip_using = String(names.length !== 1);

    // return request.delete(`/graphspaces/${graphspace}/graphs/${graph}/schema/propertykeys`, data);
    return request.delete(
        `/graphspaces/${graphspace}/graphs/${graph}/schema/propertykeys?${str}&skip_using=${skip_using}`);
};

export {getMetaPropertyList, addMetaProperty, updateMetaProperty, delMetaProperty, checkMetaProperty};

// meta vertex
const getMetaVertexList = (graphspace, graph, params) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/schema/vertexlabels`, {params});
};

const getMetaVertex = (graphspace, graph, name) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/schema/vertexlabels/${name}`);
};

const getMetaVertexLink = (graphspace, graph, name) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/schema/vertexlabels/${name}/link`);
};

const getMetaVertexNew = (graphspace, graph, name) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/schema/vertexlabels/${name}/new`);
};

const addMetaVertex = (graphspace, graph, data) => {
    return request.post(`/graphspaces/${graphspace}/graphs/${graph}/schema/vertexlabels`, data);
};

const addMetaVertexNew = (graphspace, graph, data) => {
    return request.post(`/graphspaces/${graphspace}/graphs/${graph}/schema/vertexlabels/create_new`, data);
};

const checkMetaVertex = (graphspace, graph, data) => {
    return request.post(`/graphspaces/${graphspace}/graphs/${graph}/schema/vertexlabels/check_using`, data);
};

const updateMetaVertex = (graphspace, graph, name, data) => {
    return request.put(`/graphspaces/${graphspace}/graphs/${graph}/schema/vertexlabels/${name}`, data);
};

const delMetaVertex = (graphspace, graph, data) => {
    // return request.delete(`/graphspaces/${graphspace}/graphs/${graph}/schema/vertexlabels`, data);

    const {names} = data;
    const str = names.map(name => 'names=' + encodeURIComponent(name)).join('&');
    const skip_using = String(names.length !== 1);

    return request.delete(
        `/graphspaces/${graphspace}/graphs/${graph}/schema/vertexlabels?${str}&skip_using=${skip_using}`);
};

export {getMetaVertexList, addMetaVertex, updateMetaVertex, delMetaVertex, checkMetaVertex, getMetaVertex,
    getMetaVertexNew, addMetaVertexNew, getMetaVertexLink};

// meta edge
const getMetaEdgeList = (graphspace, graph, params) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/schema/edgelabels`, {params});
};

const getMetaEdge = (graphspace, graph, name) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/schema/edgelabels/${name}`);
};

const addMetaEdge = (graphspace, graph, data) => {
    return request.post(`/graphspaces/${graphspace}/graphs/${graph}/schema/edgelabels`, data);
};

const updateMetaEdge = (graphspace, graph, name, data) => {
    return request.put(`/graphspaces/${graphspace}/graphs/${graph}/schema/edgelabels/${name}`, data);
};

const delMetaEdge = (graphspace, graph, data) => {
    // return request.delete(`/graphspaces/${graphspace}/graphs/${graph}/schema/edgelabels`, data);

    const {names} = data;
    const str = names.map(name => 'names=' + encodeURIComponent(name)).join('&');
    const skip_using = String(names.length !== 1);

    return request.delete(
        `/graphspaces/${graphspace}/graphs/${graph}/schema/edgelabels?${str}&skip_using=${skip_using}`);
};

export {getMetaEdgeList, getMetaEdge, addMetaEdge, updateMetaEdge, delMetaEdge};

// meta vertex index
const getMetaVertexIndexList = (graphspace, graph, params) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/schema/propertyindexes?is_vertex_label=true`,
        {params});
};

// meta edge index
const getMetaEdgeIndexList = (graphspace, graph, params) => {
    return request.get(`/graphspaces/${graphspace}/graphs/${graph}/schema/propertyindexes?is_vertex_label=false`,
        {params});
};

export {getMetaVertexIndexList, getMetaEdgeIndexList};

// datasource
const testhost = '/ingest';
const getDatasourceList = params => {
    // return request.get('/datasources/list', data);
    return request.get(`${testhost}/datasources/list`, {params});
};

const getDatasource = id => {
    // return request.get(`/datasources/${id}`);
    return request.get(`${testhost}/datasources/${id}`);
};

const addDatasource = data => {
    // return request.post('/datasources', data);
    return request.post(`${testhost}/datasources`, data);
};

// const updateDatasource = () => {};

const delDatasource = id => {
    // return request.delete(`/datasources/${id}`);
    return request.delete(`${testhost}/datasources/${id}`);
};

const delBatchDatasource = data => {
    return request.post(`${testhost}/datasources/delete`, data);
};

const getDatasourceSchema = datasourceID => {
    return request.get(`${testhost}/schemas`, {params: {datasource: datasourceID}});
};

const checkJDBC = data => {
    return request.post(`${testhost}/jdbc/check`, data);
};


const datasourceUploadUrl = '/api/v1.3/ingest/files/upload';

export {getDatasource, getDatasourceList, addDatasource, delDatasource,
    getDatasourceSchema, delBatchDatasource, checkJDBC, datasourceUploadUrl};

// task
const addTask = data => {
    return request.post(`${testhost}/tasks`, data, {
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
    });
};

const getTaskList = params => {
    return request.get(`${testhost}/tasks/list`, {params});
};

const getTaskDetail = id => {
    return request.get(`${testhost}/tasks/${id}`);
};

const deleteTask = id => {
    return request.delete(`${testhost}/tasks/${id}`);
};

const disableTask = id => {
    return request.put(`${testhost}/tasks/${id}/disable`);
};

const enableTask = id => {
    return request.put(`${testhost}/tasks/${id}/enable`);
};

const updateTask = (id, data) => {
    return request.put(`${testhost}/tasks/${id}`, data);
};

const getMetricsTask = () => {
    return request.get(`${testhost}/metrics/task`);
};

export {addTask, getTaskList, getTaskDetail, deleteTask, disableTask, enableTask, updateTask, getMetricsTask};

// job
const getJobsList = params => {
    return request.get(`${testhost}/jobs/list`, {params});
};

const getJobsDetail = id => {
    return request.get(`${testhost}/jobs/${id}`);
};

const deleteJobs = id => {
    return request.delete(`${testhost}/jobs/${id}`);
};

export {getJobsList, getJobsDetail, deleteJobs};
