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
