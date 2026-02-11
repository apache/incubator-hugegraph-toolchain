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

import { createContext } from 'react';
import { observable, action } from 'mobx';

import { MetadataPropertyStore } from './metadataPropertyStore';
import { VertexTypeStore } from './vertexTypeStore';
import { EdgeTypeStore } from './edgeTypeStore';
import { MetadataPropertyIndexStore } from './metadataPropertyIndexStore';
import { GraphViewStore } from './graphViewStore';

import {
  GraphManagementStore,
  GraphManagementStoreInstance
} from '../graphManagementStore';

export class MetadataConfigsRootStore {
  graphManagementStore: GraphManagementStore;
  metadataPropertyStore: MetadataPropertyStore;
  vertexTypeStore: VertexTypeStore;
  edgeTypeStore: EdgeTypeStore;
  metadataPropertyIndexStore: MetadataPropertyIndexStore;
  graphViewStore: GraphViewStore;

  @observable currentId: number | null = null;

  constructor(GraphManagementStore: GraphManagementStore) {
    this.graphManagementStore = GraphManagementStore;

    this.metadataPropertyStore = new MetadataPropertyStore(this);
    this.vertexTypeStore = new VertexTypeStore(this);
    this.edgeTypeStore = new EdgeTypeStore(this);
    this.metadataPropertyIndexStore = new MetadataPropertyIndexStore(this);
    this.graphViewStore = new GraphViewStore(this);
  }

  @action
  setCurrentId(id: number | null) {
    this.currentId = id;
  }

  @action
  dispose() {
    this.currentId = null;
  }
}

export default createContext(
  new MetadataConfigsRootStore(GraphManagementStoreInstance)
);
