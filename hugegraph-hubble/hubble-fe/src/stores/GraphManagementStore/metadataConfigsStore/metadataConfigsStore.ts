import { createContext } from 'react';
import { observable, action, flow } from 'mobx';
import axios from 'axios';

import { MetadataPropertyStore } from './metadataPropertyStore';
import { VertexTypeStore } from './vertexTypeStore';
import { EdgeTypeStore } from './edgeTypeStore';
import { MetadataPropertyIndexStore } from './metadataPropertyIndexStore';
import { GraphViewStore } from './graphViewStore';
import { baseUrl } from '../../types/common';

export class MetadataConfigsRootStore {
  metadataPropertyStore: MetadataPropertyStore;
  vertexTypeStore: VertexTypeStore;
  edgeTypeStore: EdgeTypeStore;
  metadataPropertyIndexStore: MetadataPropertyIndexStore;
  graphViewStore: GraphViewStore;

  @observable currentId: number | null = null;

  @observable requestStatus = {
    fetchIdList: 'pending'
  };

  @observable errorInfo = {
    fetchIdList: {
      code: NaN,
      message: ''
    }
  };

  @observable idList: { id: string; name: string }[] = [];

  constructor() {
    this.metadataPropertyStore = new MetadataPropertyStore(this);
    this.vertexTypeStore = new VertexTypeStore(this);
    this.edgeTypeStore = new EdgeTypeStore(this);
    this.metadataPropertyIndexStore = new MetadataPropertyIndexStore(this);
    this.graphViewStore = new GraphViewStore(this);
  }

  @action
  setCurrentId(id: number) {
    this.currentId = id;
  }

  @action
  dispose() {
    this.currentId = null;
    this.requestStatus = {
      fetchIdList: 'pending'
    };
    this.errorInfo = {
      fetchIdList: {
        code: NaN,
        message: ''
      }
    };
    this.idList = [];
  }

  fetchIdList = flow(function* fetchIdList(this: MetadataConfigsRootStore) {
    this.requestStatus.fetchIdList = 'pending';

    try {
      const result = yield axios.get(`${baseUrl}`, {
        params: {
          page_size: -1
        }
      });

      if (result.data.status !== 200) {
        this.errorInfo.fetchIdList.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.idList = result.data.data.records.map(
        ({ id, name }: { id: string; name: string }) => ({
          id,
          name
        })
      );
      this.requestStatus.fetchIdList = 'success';
    } catch (error) {
      this.requestStatus.fetchIdList = 'failed';
      this.errorInfo.fetchIdList.message = error.message;
      console.error(error.message);
    }
  });
}

export default createContext(new MetadataConfigsRootStore());
