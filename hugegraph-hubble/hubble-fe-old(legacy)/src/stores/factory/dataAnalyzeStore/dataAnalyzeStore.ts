/*
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

export function initalizeRequestStatus() {
  return {
    fetchIdList: 'standby',
    fetchValueTypes: 'standby',
    fetchVertexTypeList: 'standby',
    fetchColorSchemas: 'standby',
    fetchColorList: 'standby',
    fetchEdgeTypes: 'standby',
    fetchAllNodeStyle: 'standby',
    fetchAllEdgeStyle: 'standby',
    fetchAllPropertyIndexes: 'standby',
    fetchGraphs: 'standby',
    createAsyncTask: 'standby',
    addGraphNode: 'standby',
    fetchRelatedEdges: 'standby',
    addGraphEdge: 'standby',
    expandGraphNode: 'standby',
    filteredGraphData: 'standby',
    updateGraphProperties: 'standby',
    fetchRelatedVertex: 'standby',
    fetchFilteredPropertyOptions: 'standby',
    addQueryCollection: 'standby',
    editQueryCollection: 'standby',
    deleteQueryCollection: 'standby',
    fetchExecutionLogs: 'standby',
    fetchFavoriteQueries: 'standby'
  };
}

export function initalizeErrorInfo() {
  return {
    fetchIdList: {
      code: NaN,
      message: ''
    },
    fetchValueTypes: {
      code: NaN,
      message: ''
    },
    fetchVertexTypeList: {
      code: NaN,
      message: ''
    },
    fetchColorSchemas: {
      code: NaN,
      message: ''
    },
    fetchColorList: {
      code: NaN,
      message: ''
    },
    fetchEdgeTypes: {
      code: NaN,
      message: ''
    },
    fetchAllNodeStyle: {
      code: NaN,
      message: ''
    },
    fetchAllEdgeStyle: {
      code: NaN,
      message: ''
    },
    fetchAllPropertyIndexes: {
      code: NaN,
      message: ''
    },
    fetchGraphs: {
      code: NaN,
      message: ''
    },
    createAsyncTask: {
      code: NaN,
      message: ''
    },
    addGraphNode: {
      code: NaN,
      message: ''
    },
    fetchRelatedEdges: {
      code: NaN,
      message: ''
    },
    addGraphEdge: {
      code: NaN,
      message: ''
    },
    expandGraphNode: {
      code: NaN,
      message: ''
    },
    filteredGraphData: {
      code: NaN,
      message: ''
    },
    updateGraphProperties: {
      code: NaN,
      message: ''
    },
    fetchRelatedVertex: {
      code: NaN,
      message: ''
    },
    filteredPropertyOptions: {
      code: NaN,
      message: ''
    },
    addQueryCollection: {
      code: NaN,
      message: ''
    },
    editQueryCollection: {
      code: NaN,
      message: ''
    },
    fetchExecutionLogs: {
      code: NaN,
      message: ''
    },
    fetchFavoriteQueries: {
      code: NaN,
      message: ''
    },
    deleteQueryCollection: {
      code: NaN,
      message: ''
    }
  };
}

export function createGraphNode() {
  return {
    id: '',
    label: '',
    properties: {}
  };
}

export function createGraphEdge() {
  return {
    id: '',
    source: '',
    target: '',
    label: '',
    properties: {}
  };
}

export function createGraphEditableProperties() {
  return {
    primary: new Map(),
    nonNullable: new Map(),
    nullable: new Map()
  };
}

export function createNewGraphDataConfig() {
  return {
    id: '',
    label: '',
    properties: {
      nullable: new Map(),
      nonNullable: new Map()
    }
  };
}
