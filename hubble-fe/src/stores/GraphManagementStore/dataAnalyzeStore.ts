import { createContext } from 'react';
import { observable, action, flow, computed, runInAction } from 'mobx';
import axios, { AxiosResponse } from 'axios';
import { isUndefined } from 'lodash-es';

import {
  GraphData,
  GraphDataResponse
} from '../types/GraphManagementStore/graphManagementStore';
import { checkIfLocalNetworkOffline } from '../utils';

import { baseUrl, responseData, dict } from '../types/common';
import {
  ColorSchemas,
  RuleMap,
  FetchColorSchemas,
  FetchFilteredPropertyOptions,
  GraphNode,
  GraphEdge,
  FetchGraphReponse,
  ValueTypes,
  AddQueryCollectionParams,
  ExecutionLogs,
  ExecutionLogsResponse,
  FavoriteQuery,
  FavoriteQueryResponse
} from '../types/GraphManagementStore/dataAnalyzeStore';
import { VertexTypeListResponse } from '../types/GraphManagementStore/metadataConfigsStore';

const ruleMap: RuleMap = {
  大于: 'gt',
  大于等于: 'gte',
  等于: 'eq',
  小于: 'lt',
  小于等于: 'lte',
  True: 'eq',
  False: 'eq'
};

export class DataAnalyzeStore {
  [key: string]: any;

  @observable currentId: number | null = null;
  @observable searchText = '';
  @observable isSidebarExpanded = false;
  @observable isLoadingGraph = false;
  @observable isFullScreenReuslt = false;
  @observable isShowFilterBoard = false;
  // right-side drawer
  @observable isShowGraphInfo = false;
  @observable isClickOnNodeOrEdge = false;
  @observable favoritePopUp = '';
  @observable graphInfoDataSet = '';
  @observable codeEditorText = '';
  @observable favoriteQueriesSortOrder: Record<
    'time' | 'name',
    'desc' | 'asc' | ''
  > = {
    time: '',
    name: ''
  };

  // Mutate this variable to let mobx#reaction fires it's callback and set value for CodeEditor
  @observable pulse = false;

  // datas
  @observable.ref idList: { id: number; name: string }[] = [];
  @observable.ref valueTypes: Record<string, string> = {};
  @observable.ref colorSchemas: ColorSchemas = {};
  @observable.ref colorList: string[] = [];
  @observable.ref colorMappings: Record<string, string> = {};
  @observable.ref
  originalGraphData: FetchGraphReponse = {} as FetchGraphReponse;
  @observable.ref graphData: FetchGraphReponse = {} as FetchGraphReponse;
  @observable.ref
  expandedGraphData: FetchGraphReponse = {} as FetchGraphReponse;
  @observable vertexCollection = new Set();
  @observable edgeCollection = new Set();
  @observable.ref executionLogData: ExecutionLogs[] = [];
  @observable.ref favoriteQueryData: FavoriteQuery[] = [];
  @observable.ref graphDataEdgeTypes: string[] = [];
  @observable.ref filteredPropertyOptions: string[] = [];

  // data struct sync to GraphManagementStore
  @observable.shallow isSearched = {
    status: false,
    value: ''
  };

  @observable filteredGraphQueryOptions = {
    line: {
      type: '',
      direction: 'BOTH'
    } as Record<'type' | 'direction', string>,
    properties: [] as dict<any>[]
  };

  @observable selectedGraphData: GraphNode = {
    id: '',
    label: '',
    properties: {}
  };

  @observable selectedGraphLinkData: GraphEdge = {
    id: '',
    source: '',
    target: '',
    label: '',
    properties: {}
  };

  @observable.ref rightClickedGraphData: GraphNode = {
    id: '',
    label: '',
    properties: {}
  };

  @observable pageConfigs: {
    [key: string]: { pageNumber: number; pageTotal: number; pageSize?: number };
  } = {
    tableResult: {
      pageNumber: 1,
      pageTotal: 0
    },
    executionLog: {
      pageNumber: 1,
      pageSize: 10,
      pageTotal: 0
    },
    favoriteQueries: {
      pageNumber: 1,
      pageSize: 10,
      pageTotal: 0
    }
  };

  @observable.shallow requestStatus = {
    fetchIdList: 'standby',
    fetchValueTypes: 'standby',
    fetchColorSchemas: 'standby',
    fetchColorList: 'standby',
    fetchAllNodeColors: 'standby',
    fetchGraphs: 'standby',
    expandGraphNode: 'standby',
    filteredGraphData: 'standby',
    fetchRelatedVertex: 'standby',
    fetchFilteredPropertyOptions: 'standby',
    addQueryCollection: 'standby',
    editQueryCollection: 'standby',
    deleteQueryCollection: 'standby',
    fetchExecutionLogs: 'standby',
    fetchFavoriteQueries: 'standby'
  };

  @observable errorInfo = {
    fetchIdList: {
      code: NaN,
      message: ''
    },
    fetchValueTypes: {
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
    fetchAllNodeColors: {
      code: NaN,
      message: ''
    },
    fetchGraphs: {
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

  @computed get graphNodes(): GraphNode[] {
    return this.originalGraphData.data.graph_view.vertices.map(
      ({ id, label, properties }) => {
        return {
          id,
          label: id.length <= 15 ? id : id.slice(0, 15) + '...',
          vLabel: label,
          properties,
          title: `
              <div class="tooltip-fields">
                <div>顶点类型：</div>
                <div>${label}</div>
              </div>
              <div class="tooltip-fields">
                <div>顶点ID：</div>
                <div>${id}</div>
              </div>
              ${Object.entries(properties)
                .map(([key, value]) => {
                  return `<div class="tooltip-fields">
                            <div>${key}: </div>
                            <div>${value}</div>
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
                values.size = 30;
              }
            }
          }
        };
      }
    );
  }

  @computed get graphEdges(): GraphEdge[] {
    return this.originalGraphData.data.graph_view.edges.map(edge => ({
      ...edge,
      from: edge.source,
      to: edge.target,
      font: {
        color: '#666'
      },
      title: `
        <div class="tooltip-fields">
          <div>边类型：</div>
          <div>${edge.label}</div>
        </div>
        <div class="tooltip-fields">
          <div>边ID：</div>
          <div>${edge.id}</div>
        </div>
        ${Object.entries(edge.properties)
          .map(([key, value]) => {
            return `<div class="tooltip-fields">
                      <div>${key}: </div>
                      <div>${value}</div>
                    </div>`;
          })
          .join('')}
      `
    }));
  }

  @action
  setCurrentId(id: number) {
    this.currentId = id;
  }

  @action
  setFullScreenReuslt(flag: boolean) {
    this.isFullScreenReuslt = flag;
  }

  @action
  mutateSearchText(text: string) {
    this.searchText = text;
  }

  @action
  switchShowScreenInfo(flag: boolean) {
    this.isShowGraphInfo = flag;
  }

  @action
  switchClickOnNodeOrEdge(flag: boolean) {
    this.isClickOnNodeOrEdge = flag;
  }

  @action
  switchShowFilterBoard(flag: boolean) {
    this.isShowFilterBoard = flag;
  }

  @action
  switchShowScreeDataSet(dataSet: string) {
    this.graphInfoDataSet = dataSet;
  }

  @action
  setFavoritePopUp(popupCategory: string) {
    this.favoritePopUp = popupCategory;
  }

  @action
  triggerLoadingStatementsIntoEditor() {
    this.pulse = !this.pulse;
  }

  @action
  mutateCodeEditorText(text: string) {
    this.codeEditorText = text;
  }

  // change selected graph node
  @action
  changeSelectedGraphData(selectedData: GraphNode) {
    this.selectedGraphData = selectedData;
  }

  // change selected graph edge
  @action
  changeSelectedGraphLinkData(selectedLinkData: GraphEdge) {
    this.selectedGraphLinkData = selectedLinkData;
  }

  @action
  changeRightClickedGraphData(rightClickedData: GraphNode) {
    this.rightClickedGraphData = rightClickedData;
  }

  @action
  mutatePageNumber(category: string, pageNumber: number) {
    this.pageConfigs[category].pageNumber = pageNumber;
  }

  @action
  mutatePageSize(category: string, pageSize: number) {
    this.pageConfigs[category].pageSize = pageSize;
  }

  @action
  editEdgeFilterOption(key: 'type' | 'direction', value: string) {
    this.filteredGraphQueryOptions.line[key] = value;
  }

  @action
  addPropertyFilterOption() {
    this.filteredGraphQueryOptions.properties.push({
      property: '',
      rule: '',
      value: ''
    });
  }

  @action
  editPropertyFilterOption(
    key: 'property' | 'rule' | 'value',
    value: string | number,
    index: number
  ) {
    this.filteredGraphQueryOptions.properties[index][key] = value;
  }

  @action
  deletePropertyFilterOption(index: number) {
    this.filteredGraphQueryOptions.properties.splice(index, 1);
  }

  @action
  addTempExecLog() {
    const date = new Date();
    const time = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()} ${date.getHours()}:${date.getMinutes()}:${date.getSeconds()}`;

    const tempData: ExecutionLogs = {
      id: NaN,
      type: 'GREMLIN',
      content: this.codeEditorText,
      status: 'RUNNING',
      duration: '0',
      create_time: time
    };

    this.executionLogData = [tempData].concat(
      this.executionLogData.slice(0, 9)
    );

    return window.setInterval(() => {
      this.executionLogData[0].duration =
        String(Number(this.executionLogData[0].duration) + 10) + 'ms';

      runInAction(() => {
        this.executionLogData = this.executionLogData.slice();
      });
    }, 10);
  }

  @action
  swtichIsSearchedStatus(isSearched: boolean) {
    this.isSearched.status = isSearched;

    isSearched
      ? (this.isSearched.value = this.searchText)
      : (this.isSearched.value = '');
  }

  @action
  changeFavoriteQueriesSortOrder(
    key: 'time' | 'name',
    order: 'desc' | 'asc' | ''
  ) {
    this.favoriteQueriesSortOrder[key] = order;
  }

  @action
  clearFilteredGraphQueryOptions() {
    this.filteredGraphQueryOptions = {
      line: {
        type: '',
        direction: 'BOTH'
      } as Record<'type' | 'direction', string>,
      properties: [] as dict<any>[]
    };
  }

  @action
  resetRightClickedGraphData() {
    this.rightClickedGraphData = {
      id: '',
      label: '',
      properties: {}
    };
  }

  @action
  resetFavoriteRequestStatus(type: 'add' | 'edit') {
    if (type === 'add') {
      this.requestStatus.addQueryCollection = 'standby';
      this.errorInfo.addQueryCollection.code = NaN;
      this.errorInfo.addQueryCollection.message = '';
    }

    if (type === 'edit') {
      this.requestStatus.editQueryCollection = 'standby';
      this.errorInfo.editQueryCollection.code = NaN;
      this.errorInfo.editQueryCollection.message = '';
    }
  }

  @action
  resetIdState() {
    this.currentId = null;
    this.searchText = '';
    this.isSidebarExpanded = false;
    this.isLoadingGraph = false;
    this.isShowGraphInfo = false;
    this.isFullScreenReuslt = false;
    this.codeEditorText = '';
    this.graphData = {} as FetchGraphReponse;

    this.isSearched = {
      status: false,
      value: ''
    };

    this.selectedGraphData = {
      id: '',
      label: '',
      properties: {}
    };

    this.selectedGraphLinkData = {
      id: '',
      source: '',
      target: '',
      label: '',
      properties: {}
    };

    this.pageConfigs = {
      tableResult: {
        pageNumber: 1,
        pageTotal: 0
      },
      executionLog: {
        pageNumber: 1,
        pageSize: 10,
        pageTotal: 0
      },
      favoriteQueries: {
        pageNumber: 1,
        pageSize: 10,
        pageTotal: 0
      }
    };

    this.requestStatus = {
      fetchIdList: 'standby',
      fetchValueTypes: 'standby',
      fetchColorSchemas: 'standby',
      fetchColorList: 'standby',
      fetchGraphs: 'standby',
      fetchAllNodeColors: 'standby',
      expandGraphNode: 'standby',
      filteredGraphData: 'standby',
      fetchRelatedVertex: 'standby',
      fetchFilteredPropertyOptions: 'standby',
      addQueryCollection: 'standby',
      editQueryCollection: 'standby',
      deleteQueryCollection: 'standby',
      fetchExecutionLogs: 'standby',
      fetchFavoriteQueries: 'standby'
    };

    this.errorInfo = {
      fetchIdList: {
        code: NaN,
        message: ''
      },
      fetchValueTypes: {
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
      fetchAllNodeColors: {
        code: NaN,
        message: ''
      },
      fetchGraphs: {
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

    this.clearFilteredGraphQueryOptions();
  }

  @action
  dispose() {
    this.resetIdState();
    this.idList = [];
  }

  fetchIdList = flow(function* fetchIdList(this: DataAnalyzeStore) {
    this.requestStatus.fetchIdList = 'pending';

    try {
      const result: AxiosResponse<GraphDataResponse> = yield axios.get<
        GraphData
      >(baseUrl, {
        params: {
          page_size: -1
        }
      });

      if (result.data.status !== 200) {
        this.errorInfo.fetchIdList.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.idList = result.data.data.records.map(({ id, name }) => ({
        id,
        name
      }));
      this.requestStatus.fetchIdList = 'success';
    } catch (error) {
      this.requestStatus.fetchIdList = 'failed';
      this.errorInfo.fetchIdList.message = error.message;
      console.error(error.message);
    }
  });

  // to know the type of properties
  fetchValueTypes = flow(function* fetchValueTypes(this: DataAnalyzeStore) {
    this.requestStatus.fetchValueTypes = 'pending';

    try {
      const result = yield axios.get<ValueTypes>(
        `${baseUrl}/${this.currentId}/schema/propertykeys`,
        {
          params: {
            page_size: -1
          }
        }
      );

      if (result.data.status !== 200) {
        this.errorInfo.fetchValueTypes.code = result.data.status;
        throw new Error(result.data.message);
      }

      result.data.data.records.forEach(
        ({ name, data_type }: Record<string, string>) => {
          this.valueTypes[name] = data_type;
        }
      );

      this.requestStatus.fetchValueTypes = 'success';
    } catch (error) {
      this.requestStatus.fetchValueTypes = 'failed';
      this.errorInfo.fetchValueTypes.message = error.message;
      console.error(error.message);
    }
  });

  fetchColorSchemas = flow(function* fetchColorSchemas(this: DataAnalyzeStore) {
    this.requestStatus.fetchColorSchemas = 'pending';

    try {
      const result: AxiosResponse<FetchColorSchemas> = yield axios.get<
        FetchGraphReponse
      >(`${baseUrl}/${this.currentId}/schema/vertexlabels/style`);

      if (result.data.status !== 200) {
        this.errorInfo.fetchColorSchemas.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.colorSchemas = result.data.data;
      this.requestStatus.fetchColorSchemas = 'success';
    } catch (error) {
      this.requestStatus.fetchColorSchemas = 'failed';
      this.errorInfo.fetchColorSchemas.message = error.message;
      console.error(error.message);
    }
  });

  fetchColorList = flow(function* fetchColorList(this: DataAnalyzeStore) {
    this.requestStatus.fetchColorList = 'pending';

    try {
      const result: AxiosResponse<responseData<string[]>> = yield axios.get(
        `${baseUrl}/${this.currentId}/schema/vertexlabels/optional-colors`
      );

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      this.colorList = result.data.data;
      this.requestStatus.fetchColorList = 'success';
    } catch (error) {
      this.requestStatus.fetchColorList = 'failed';
      this.errorMessage = error.message;
      console.error(error.message);
    }
  });

  fetchAllNodeColors = flow(function* fetchAllNodeColors(
    this: DataAnalyzeStore
  ) {
    this.requestStatus.fetchAllNodeColors = 'pending';

    try {
      const result: AxiosResponse<
        responseData<VertexTypeListResponse>
      > = yield axios.get(`${baseUrl}/${this.currentId}/schema/vertexlabels`, {
        params: {
          page_no: 1,
          page_size: -1
        }
      });

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      result.data.data.records.forEach(({ name, style }) => {
        if (style.color !== null) {
          this.colorMappings[name] = style.color;
        }
      });

      this.requestStatus.fetchAllNodeColors = 'success';
    } catch (error) {
      this.requestStatus.fetchAllNodeColors = 'failed';
      this.errorInfo.fetchAllNodeColors.message = error.message;
      console.error(error.message);
    }
  });

  fetchGraphs = flow(function* fetchGraphs(this: DataAnalyzeStore) {
    this.requestStatus.fetchGraphs = 'pending';
    this.isLoadingGraph = true;

    try {
      const result: AxiosResponse<FetchGraphReponse> = yield axios
        .post<FetchGraphReponse>(`${baseUrl}/${this.currentId}/gremlin-query`, {
          content: this.codeEditorText
        })
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.fetchGraphs.code = result.data.status;
        throw new Error(result.data.message);
      }

      // replace null with empty array when query result type is EMPTY
      if (result.data.data.type === 'EMPTY') {
        result.data.data.json_view.data = [];
        result.data.data.table_view = {
          header: ['result'],
          rows: []
        };
      }

      this.originalGraphData = result.data;

      if (
        result.data.data.graph_view.vertices !== null &&
        result.data.data.graph_view.edges !== null
      ) {
        this.vertexCollection = new Set(
          result.data.data.graph_view.vertices.map(({ id }) => id)
        );
        this.edgeCollection = new Set(
          result.data.data.graph_view.edges.map(({ id }) => id)
        );
      }

      this.graphData = result.data;
      this.pageConfigs.tableResult.pageTotal = this.originalGraphData.data.table_view.rows.length;
      this.requestStatus.fetchGraphs = 'success';
      this.isLoadingGraph = false;
    } catch (error) {
      this.isLoadingGraph = false;
      this.requestStatus.fetchGraphs = 'failed';
      this.errorInfo.fetchGraphs.message = error.message;
      console.error(error.message);
    }
  });

  expandGraphNode = flow(function* expandGraphNode(
    this: DataAnalyzeStore,
    // double click on a node, or right click a node
    nodeId?: string,
    label?: string
  ) {
    this.requestStatus.expandGraphNode = 'pending';

    try {
      const result: AxiosResponse<FetchGraphReponse> = yield axios.put(
        `${baseUrl}/${this.currentId}/gremlin-query`,
        {
          vertex_id: nodeId || this.rightClickedGraphData.id,
          vertex_label: label || this.rightClickedGraphData.label
        }
      );

      if (result.data.status !== 200) {
        this.errorInfo.expandGraphNode.code = result.data.status;
        throw new Error(result.data.message);
      }

      const newGraphData = result.data;

      const filteredVertices = newGraphData.data.graph_view.vertices.filter(
        ({ id }) => {
          if (this.vertexCollection.has(id)) {
            return false;
          }

          this.vertexCollection.add(id);
          return true;
        }
      );

      const filteredEdges = newGraphData.data.graph_view.edges.filter(
        ({ id }) => {
          if (this.edgeCollection.has(id)) {
            return false;
          }

          this.edgeCollection.add(id);
          return true;
        }
      );

      this.expandedGraphData = {
        ...newGraphData,
        data: {
          ...newGraphData.data,
          graph_view: {
            vertices: filteredVertices,
            edges: filteredEdges
          }
        }
      };

      const vertexCollection = new Set();
      const edgeCollection = new Set();

      const mergeData: FetchGraphReponse = {
        ...newGraphData,
        data: {
          ...newGraphData.data,
          graph_view: {
            vertices: this.graphData.data.graph_view.vertices
              .concat(newGraphData.data.graph_view.vertices)
              .filter(item => {
                const isDuplicate = vertexCollection.has(item.id);
                vertexCollection.add(item.id);
                return !isDuplicate;
              }),
            edges: this.graphData.data.graph_view.edges
              .concat(newGraphData.data.graph_view.edges)
              .filter(item => {
                const isDuplicate = edgeCollection.has(item.id);
                edgeCollection.add(item.id);
                return !isDuplicate;
              })
          }
        }
      };

      this.graphData = mergeData;
      this.requestStatus.expandGraphNode = 'success';
    } catch (error) {
      this.requestStatus.expandGraphNode = 'failed';
      this.errorInfo.expandGraphNode.message = error.message;
      console.error(error.message);
    }
  });

  @action
  hideGraphNode(nodeId: any) {
    this.graphData.data.graph_view.vertices = this.graphData.data.graph_view.vertices.filter(
      data => data.id !== this.rightClickedGraphData.id
    );

    // only delete node in vertexCollection, not edges in EdgeCollection
    this.vertexCollection.delete(nodeId);

    // assign new object to observable
    this.graphData = { ...this.graphData };
  }

  // require list of edge type options in QueryFilteredOptions
  fetchRelatedVertex = flow(function* fetchRelatedVertex(
    this: DataAnalyzeStore
  ) {
    this.requestStatus.fetchRelatedVertex = 'pending';

    try {
      const result = yield axios.get(
        `${baseUrl}/${this.currentId}/schema/vertexlabels/${this.rightClickedGraphData.label}/link`
      );

      if (result.data.status !== 200) {
        this.errorInfo.fetchRelatedVertex = result.data.status;
        throw new Error(result.data.message);
      }

      this.graphDataEdgeTypes = result.data.data;

      this.editEdgeFilterOption(
        'type',
        !isUndefined(result.data.data[0]) ? result.data.data[0] : ''
      );
    } catch (error) {
      this.requestStatus.fetchRelatedVertex = 'failed';
      this.errorInfo.fetchRelatedVertex.message = error.message;
      console.error(error.message);
    }
  });

  // require list of property options in QueryFilteredOptions
  fetchFilteredPropertyOptions = flow(function* fetchFilteredPropertyOptions(
    this: DataAnalyzeStore,
    edgeName: string
  ) {
    this.requestStatus.fetchFilteredPropertyOptions = 'pending';

    try {
      const result: AxiosResponse<
        FetchFilteredPropertyOptions
      > = yield axios.get(
        `${baseUrl}/${this.currentId}/schema/edgelabels/${edgeName}`
      );

      if (result.data.status !== 200) {
        this.errorInfo.filteredPropertyOptions.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.filteredPropertyOptions = result.data.data.properties.map(
        ({ name }) => name
      );
    } catch (error) {
      this.requestStatus.fetchFilteredPropertyOptions = 'failed';
      this.errorInfo.filteredPropertyOptions = error.message;
      console.error(error.message);
    }
  });

  filterGraphData = flow(function* filteredGraphData(this: DataAnalyzeStore) {
    this.requestStatus.filteredGraphData = 'pending';

    try {
      const result: AxiosResponse<FetchGraphReponse> = yield axios.put(
        `${baseUrl}/${this.currentId}/gremlin-query`,
        {
          vertex_id: this.rightClickedGraphData.id,
          vertex_label: this.rightClickedGraphData.label,
          edge_label: this.filteredGraphQueryOptions.line.type,
          direction: this.filteredGraphQueryOptions.line.direction,
          conditions: this.filteredGraphQueryOptions.properties.map(
            ({ property, rule, value }) => ({
              key: property,
              operator: ruleMap[rule],
              value: rule === 'True' || rule === 'False' ? Boolean(rule) : value
            })
          )
        }
      );

      if (result.data.status !== 200) {
        this.errorInfo.filteredGraphData.code = result.data.status;
        throw new Error(result.data.message);
      }

      const newGraphData = result.data;

      const filteredVertices = newGraphData.data.graph_view.vertices.filter(
        ({ id }) => {
          if (this.vertexCollection.has(id)) {
            return false;
          }

          this.vertexCollection.add(id);
          return true;
        }
      );

      const filteredEdges = newGraphData.data.graph_view.edges.filter(
        ({ id }) => {
          if (this.edgeCollection.has(id)) {
            return false;
          }

          this.edgeCollection.add(id);
          return true;
        }
      );

      this.expandedGraphData = {
        ...newGraphData,
        data: {
          ...newGraphData.data,
          graph_view: {
            vertices: filteredVertices,
            edges: filteredEdges
          }
        }
      };

      const vertexCollection = new Set();
      const edgeCollection = new Set();

      const mergeData: FetchGraphReponse = {
        ...newGraphData,
        data: {
          ...newGraphData.data,
          graph_view: {
            vertices: this.graphData.data.graph_view.vertices
              .concat(newGraphData.data.graph_view.vertices)
              .filter(item => {
                const isDuplicate = vertexCollection.has(item.id);
                vertexCollection.add(item.id);
                return !isDuplicate;
              }),
            edges: this.graphData.data.graph_view.edges
              .concat(newGraphData.data.graph_view.edges)
              .filter(item => {
                const isDuplicate = edgeCollection.has(item.id);
                edgeCollection.add(item.id);
                return !isDuplicate;
              })
          }
        }
      };

      this.graphData = mergeData;
      this.requestStatus.filteredGraphData = 'success';
    } catch (error) {
      this.errorInfo.filteredGraphData.message = error.message;
      console.error(error.message);
    }
  });

  addQueryCollection = flow(function* addQueryCollection(
    this: DataAnalyzeStore,
    name: string,
    // if content is not the value in codeEditor (e.g. in table)
    content?: string
  ) {
    this.requestStatus.addQueryCollection = 'pending';

    try {
      const result = yield axios.post<AddQueryCollectionParams>(
        `${baseUrl}/${this.currentId}/gremlin-collections`,
        {
          name,
          content: content || this.codeEditorText
        }
      );

      if (result.data.status !== 200) {
        this.errorInfo.addQueryCollection.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.requestStatus.addQueryCollection = 'success';
    } catch (error) {
      this.requestStatus.addQueryCollection = 'failed';
      this.errorInfo.addQueryCollection.message = error.message;
      console.error(error.message);
    }
  });

  editQueryCollection = flow(function* editQueryCollection(
    this: DataAnalyzeStore,
    id: number,
    name: string,
    content: string
  ) {
    this.requestStatus.editQueryCollection = 'pending';

    try {
      const result = yield axios.put<AddQueryCollectionParams>(
        `${baseUrl}/${this.currentId}/gremlin-collections/${id}`,
        {
          name,
          content
        }
      );

      if (result.data.status !== 200) {
        this.errorInfo.editQueryCollection.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.requestStatus.editQueryCollection = 'success';
    } catch (error) {
      this.requestStatus.editQueryCollection = 'failed';
      this.errorInfo.editQueryCollection.message = error.message;
      console.error(error.message);
    }
  });

  deleteQueryCollection = flow(function* deleteQueryCollection(
    this: DataAnalyzeStore,
    id: number
  ) {
    this.requestStatus.deleteQueryCollection = 'pending';

    try {
      const result = yield axios.delete(
        `${baseUrl}/${this.currentId}/gremlin-collections/${id}`
      );

      if (result.data.status !== 200) {
        this.errorInfo.deleteQueryCollection = result.data.status;
        throw new Error(result.data.message);
      }

      // if current pageNumber has no data after delete, set the pageNumber to the previous
      if (
        this.favoriteQueryData.length === 1 &&
        this.pageConfigs.favoriteQueries.pageNumber > 1
      ) {
        this.pageConfigs.favoriteQueries.pageNumber =
          this.pageConfigs.favoriteQueries.pageNumber - 1;
      }

      this.requestStatus.deleteQueryCollection = 'success';
    } catch (error) {
      this.requestStatus.deleteQueryCollection = 'failed';
      this.errorInfo.deleteQueryCollection.message = error.message;
      console.error(error.message);
    }
  });

  fetchExecutionLogs = flow(function* fetchExecutionLogs(
    this: DataAnalyzeStore
  ) {
    this.requestStatus.fetchExecutionLogs = 'pending';

    try {
      const result: AxiosResponse<ExecutionLogsResponse> = yield axios.get<
        ExecutionLogsResponse
      >(`${baseUrl}/${this.currentId}/execute-histories`, {
        params: {
          page_size: this.pageConfigs.executionLog.pageSize,
          page_no: this.pageConfigs.executionLog.pageNumber
        }
      });

      if (result.data.status !== 200) {
        this.errorInfo.fetchExecutionLogs.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.executionLogData = result.data.data.records;
      this.pageConfigs.executionLog.pageTotal = result.data.data.total;
      this.requestStatus.fetchExecutionLogs = 'success';
    } catch (error) {
      this.requestStatus.fetchExecutionLogs = 'failed';
      this.errorInfo.fetchExecutionLogs.message = error.message;
      console.error(error.message);
    }
  });

  fetchFavoriteQueries = flow(function* fetchFavoriteQueries(
    this: DataAnalyzeStore
  ) {
    const url =
      `${baseUrl}/${this.currentId}/gremlin-collections?` +
      `&page_no=${this.pageConfigs.favoriteQueries.pageNumber}` +
      `&page_size=${this.pageConfigs.favoriteQueries.pageSize}` +
      (this.favoriteQueriesSortOrder.time !== ''
        ? `&time_order=${this.favoriteQueriesSortOrder.time}`
        : '') +
      (this.favoriteQueriesSortOrder.name !== ''
        ? `&name_order=${this.favoriteQueriesSortOrder.name}`
        : '') +
      (this.isSearched.status && this.searchText !== ''
        ? `&content=${this.searchText}`
        : '');

    this.requestStatus.fetchFavoriteQueries = 'pending';

    try {
      const result: AxiosResponse<FavoriteQueryResponse> = yield axios.get<
        FavoriteQueryResponse
      >(url);

      if (result.data.status !== 200) {
        this.errorInfo.fetchFavoriteQueries.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.favoriteQueryData = result.data.data.records;
      this.pageConfigs.favoriteQueries.pageTotal = result.data.data.total;
      this.requestStatus.fetchFavoriteQueries = 'success';
    } catch (error) {
      this.requestStatus.fetchFavoriteQueries = 'failed';
      this.errorInfo.fetchFavoriteQueries.message = error.message;
      console.error(error.message);
    }
  });
}

export default createContext(new DataAnalyzeStore());
