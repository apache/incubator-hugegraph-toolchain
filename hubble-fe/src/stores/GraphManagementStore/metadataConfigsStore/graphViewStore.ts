import { observable, action, flow, computed } from 'mobx';
import axios from 'axios';
import { isUndefined, cloneDeep, clone } from 'lodash-es';

import vis from 'vis-network';
import { MetadataConfigsRootStore } from './metadataConfigsStore';
import {
  checkIfLocalNetworkOffline,
  vertexRadiusMapping,
  edgeWidthMapping
} from '../../utils';

import { baseUrl } from '../../types/common';
import type {
  GraphViewData,
  DrawerTypes
} from '../../types/GraphManagementStore/metadataConfigsStore';

export class GraphViewStore {
  metadataConfigsRootStore: MetadataConfigsRootStore;

  constructor(MetadataConfigsRootStore: MetadataConfigsRootStore) {
    this.metadataConfigsRootStore = MetadataConfigsRootStore;
  }

  @observable.ref colorMappings: Record<string, string> = {};
  @observable.ref vertexSizeMappings: Record<string, string> = {};
  @observable.ref vertexWritingMappings: Record<string, string[]> = {};
  @observable.ref edgeColorMappings: Record<string, string> = {};
  @observable.ref edgeWithArrowMappings: Record<string, boolean> = {};
  @observable.ref edgeThicknessMappings: Record<string, string> = {};
  @observable.ref edgeWritingMappings: Record<string, string[]> = {};

  @observable currentDrawer: DrawerTypes = '';
  @observable currentSelected = '';
  @observable isNodeOrEdgeClicked = false;

  // avoid to re-assign value to originalGraphViewData from re-rendering
  // have to set a flag to inform data is empty
  @observable isGraphVertexEmpty = true;

  @observable visNetwork: vis.Network | null = null;
  @observable visDataSet: Record<'nodes' | 'edges', any> | null = null;
  @observable.ref graphViewData: GraphViewData | null = null;
  @observable.ref originalGraphViewData: GraphViewData | null = null;

  @observable.shallow requestStatus = {
    fetchGraphViewData: 'standby'
  };

  @observable errorInfo = {
    fetchGraphViewData: {
      code: NaN,
      message: ''
    }
  };

  @computed get graphNodes() {
    if (this.originalGraphViewData === null) {
      return [];
    }

    return this.originalGraphViewData.vertices.map(
      ({ id, label, properties, primary_keys }) => {
        return {
          id,
          label: id.length <= 15 ? id : id.slice(0, 15) + '...',
          vLabel: id,
          value: vertexRadiusMapping[this.vertexSizeMappings[label]],
          font: { size: 16 },
          properties,
          title: `
            <div class="metadata-graph-view-tooltip-fields">
              <div>顶点类型：</div>
              <div style="min-width: 60px; max-width: 145px; marigin-right: 0">${label}</div>
            </div>
            <div class="metadata-graph-view-tooltip-fields">
              <div style="max-width: 120px">关联属性及类型：</div>
            </div>
            ${Object.entries(properties)
              .map(([key, value]) => {
                const convertedValue =
                  value.toLowerCase() === 'text'
                    ? 'string'
                    : value.toLowerCase();

                const primaryKeyIndex = primary_keys.findIndex(
                  (primaryKey) => primaryKey === key
                );

                return `<div class="metadata-graph-view-tooltip-fields">
                          <div>${key}: </div>
                          <div>${convertedValue}</div>
                          <div>${
                            primaryKeyIndex === -1
                              ? ''
                              : `(主键${primaryKeyIndex + 1})`
                          }</div>
                        </div>`;
              })
              .join('')}
          `,
          color: {
            background: this.colorMappings[label] || '#5c73e6',
            border: this.colorMappings[label] || '#5c73e6',
            highlight: { background: '#fb6a02', border: '#fb6a02' },
            hover: { background: '#ec3112', border: '#ec3112' }
          },
          chosen: {
            node(
              values: any,
              id: string,
              selected: boolean,
              hovering: boolean
            ) {
              if (hovering || selected) {
                values.shadow = true;
                values.shadowColor = 'rgba(0, 0, 0, 0.6)';
                values.shadowX = 0;
                values.shadowY = 0;
                values.shadowSize = 25;
              }

              if (selected) {
                values.size += 5;
              }
            }
          }
        };
      }
    );
  }

  @computed get graphEdges() {
    if (this.originalGraphViewData === null) {
      return [];
    }

    return this.originalGraphViewData.edges.map(
      ({ id, label, source, target, properties, sort_keys }) => {
        return {
          id,
          label,
          properties,
          source,
          target,
          from: source,
          to: target,
          font: { size: 16, strokeWidth: 0, color: '#666' },
          arrows: this.edgeWithArrowMappings[label] === true ? 'to' : '',
          value: edgeWidthMapping[this.edgeThicknessMappings[label]],
          title: `
            <div class="metadata-graph-view-tooltip-fields">
              <div>边类型：</div>
              <div style="min-width: 60px; max-width: 145px; marigin-right: 0">${label}</div>
            </div>
            <div class="metadata-graph-view-tooltip-fields">
              <div style="max-width: 120px">关联属性及类型：</div>
            </div>
            ${Object.entries(properties)
              .map(([key, value]) => {
                const convertedValue =
                  value.toLowerCase() === 'text'
                    ? 'string'
                    : value.toLowerCase();

                const sortKeyIndex = sort_keys.findIndex(
                  (sortKey) => sortKey === key
                );

                return `<div class="metadata-graph-view-tooltip-fields">
                          <div>${key}: </div>
                          <div>${convertedValue}</div>
                          <div>${
                            sortKeyIndex === -1
                              ? ''
                              : `(区分键${sortKeyIndex + 1})`
                          }</div>
                        </div>`;
              })
              .join('')}
          `,
          color: this.edgeColorMappings[label] || '#5c73e6'
        };
      }
    );
  }

  @action
  setCurrentDrawer(drawer: DrawerTypes) {
    this.currentDrawer = drawer;
  }

  @action
  switchNodeOrEdgeClicked(flag: boolean) {
    this.isNodeOrEdgeClicked = flag;
  }

  @action
  switchGraphDataEmpty(flag: boolean) {
    this.isGraphVertexEmpty = flag;
  }

  @action
  setVisNetwork(visNetwork: vis.Network) {
    this.visNetwork = visNetwork;
  }

  @action
  setVisDataSet(visDataSet: Record<'nodes' | 'edges', any>) {
    this.visDataSet = visDataSet;
  }

  @action
  dispose() {
    this.edgeColorMappings = {};
    this.currentDrawer = '';
    this.currentSelected = '';
    this.colorMappings = {};
    this.edgeColorMappings = {};
    this.graphViewData = null;
    this.isNodeOrEdgeClicked = false;
    this.isGraphVertexEmpty = true;
    this.visNetwork = null;
    this.visDataSet = null;
    this.graphViewData = null;
    this.originalGraphViewData = null;
    this.requestStatus = {
      fetchGraphViewData: 'standby'
    };
    this.errorInfo = {
      fetchGraphViewData: {
        code: NaN,
        message: ''
      }
    };
  }

  fetchGraphViewData = flow(function* fetchGraphViewData(
    this: GraphViewStore,
    colorMappings?: Record<string, string>,
    vertexSizeMappings?: Record<string, string>,
    vertexWritingMappings?: Record<string, string[]>,
    edgeColorMappings?: Record<string, string>,
    edgeThicknessMappings?: Record<string, string>,
    edgeWithArrowMappings?: Record<string, boolean>,
    edgeWritingMappings?: Record<string, string[]>
  ) {
    this.requestStatus.fetchGraphViewData = 'pending';

    if (!isUndefined(colorMappings)) {
      this.colorMappings = colorMappings;
    }
    if (!isUndefined(vertexSizeMappings)) {
      this.vertexSizeMappings = vertexSizeMappings;
    }
    if (!isUndefined(vertexWritingMappings)) {
      this.vertexWritingMappings = vertexWritingMappings;
    }
    if (!isUndefined(edgeColorMappings)) {
      this.edgeColorMappings = edgeColorMappings;
    }
    if (!isUndefined(edgeThicknessMappings)) {
      this.edgeThicknessMappings = edgeThicknessMappings;
    }
    if (!isUndefined(edgeWithArrowMappings)) {
      this.edgeWithArrowMappings = edgeWithArrowMappings;
    }
    if (!isUndefined(edgeWritingMappings)) {
      this.edgeWritingMappings = edgeWritingMappings;
    }

    try {
      const result = yield axios
        .get(
          `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/graphview`
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.fetchGraphViewData.code = result.data.status;
        throw new Error(result.data.message);
      }

      const data = result.data.data;

      if (data.vertices.length !== 0) {
        this.switchGraphDataEmpty(false);
      }

      this.originalGraphViewData = data;
      this.graphViewData = data;

      this.requestStatus.fetchGraphViewData = 'success';
    } catch (error) {
      this.requestStatus.fetchGraphViewData = 'failed';
      this.errorInfo.fetchGraphViewData.message = error.message;
      console.error(error.message);
    }
  });
}
