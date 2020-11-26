import { createContext } from 'react';
import { observable, action, flow, computed, runInAction } from 'mobx';
import axios, { AxiosResponse } from 'axios';
import { isUndefined, cloneDeep, isEmpty, remove, size } from 'lodash-es';
import vis from 'vis-network';
import isInt from 'validator/lib/isInt';
import isUUID from 'validator/lib/isUUID';

import { AlgorithmAnalyzerStore } from './algorithmAnalyzerStore';
import {
  initalizeErrorInfo,
  initalizeRequestStatus,
  createGraphNode,
  createGraphEdge,
  createGraphEditableProperties,
  createNewGraphDataConfig
} from '../../factory/dataAnalyzeStore/dataAnalyzeStore';
import { Algorithm } from '../../factory/dataAnalyzeStore/algorithmStore';
import {
  checkIfLocalNetworkOffline,
  convertArrayToString,
  validateGraphProperty,
  vertexRadiusMapping,
  edgeWidthMapping
} from '../../utils';

import { baseUrl, responseData, dict } from '../../types/common';
import type {
  GraphData,
  GraphDataResponse
} from '../../types/GraphManagementStore/graphManagementStore';
import type {
  ColorSchemas,
  RuleMap,
  FetchColorSchemas,
  FetchFilteredPropertyOptions,
  NewGraphData,
  GraphNode,
  GraphEdge,
  GraphView,
  FetchGraphResponse,
  ValueTypes,
  AddQueryCollectionParams,
  ExecutionLogs,
  ExecutionLogsResponse,
  FavoriteQuery,
  FavoriteQueryResponse,
  EditableProperties,
  ShortestPathAlgorithmParams,
  LoopDetectionParams,
  FocusDetectionParams,
  ShortestPathAllAlgorithmParams,
  AllPathAlgorithmParams,
  ModelSimilarityParams,
  NeighborRankParams,
  NeighborRankRule
} from '../../types/GraphManagementStore/dataAnalyzeStore';
import type {
  VertexTypeListResponse,
  VertexType,
  EdgeType
} from '../../types/GraphManagementStore/metadataConfigsStore';
import type { EdgeTypeListResponse } from '../../types/GraphManagementStore/metadataConfigsStore';

const ruleMap: RuleMap = {
  大于: 'gt',
  大于等于: 'gte',
  等于: 'eq',
  小于: 'lt',
  小于等于: 'lte',
  True: 'eq',
  False: 'eq'
};

const monthMaps: Record<string, string> = {
  Jan: '01',
  Feb: '02',
  Mar: '03',
  Apr: '04',
  May: '05',
  Jun: '06',
  Jul: '07',
  Aug: '08',
  Sep: '09',
  Oct: '10',
  Nov: '11',
  Dec: '12'
};

export class DataAnalyzeStore {
  [key: string]: any;
  algorithmAnalyzerStore: AlgorithmAnalyzerStore;

  constructor() {
    this.algorithmAnalyzerStore = new AlgorithmAnalyzerStore(this);
  }

  @observable currentId: number | null = null;
  @observable currentTab = 'gremlin-analyze';
  @observable searchText = '';
  @observable isSidebarExpanded = false;
  @observable isLoadingGraph = false;
  @observable isGraphLoaded = false;
  @observable isFullScreenReuslt = false;
  @observable isShowFilterBoard = false;
  // right-side drawer
  @observable isShowGraphInfo = false;
  @observable isClickOnNodeOrEdge = false;
  // v1.5.0: gremlin query mode
  @observable queryMode: 'query' | 'task' = 'query';
  @observable favoritePopUp = '';
  // whether user selects vertex or edge
  @observable graphInfoDataSet = '';
  @observable codeEditorText = '';
  @observable dynamicAddGraphDataStatus = '';
  @observable favoriteQueriesSortOrder: Record<
    'time' | 'name',
    'desc' | 'asc' | ''
  > = {
    time: '',
    name: ''
  };

  // vis instance
  @observable.ref visNetwork: vis.Network | null = null;
  @observable.ref visDataSet: Record<'nodes' | 'edges', any> | null = null;
  @observable.ref visCurrentCoordinates = {
    domX: '',
    domY: '',
    canvasX: '',
    canvasY: ''
  };

  // Mutate this variable to let mobx#reaction fires it's callback and set value for CodeEditor
  @observable pulse = false;

  // datas
  @observable.ref idList: { id: number; name: string }[] = [];
  @observable.ref properties: ValueTypes[] = [];
  @observable.ref valueTypes: Record<string, string> = {};
  @observable.ref vertexTypes: VertexType[] = [];
  @observable.ref edgeTypes: EdgeType[] = [];
  @observable.ref colorSchemas: ColorSchemas = {};
  @observable.ref colorList: string[] = [];
  @observable.ref colorMappings: Record<string, string> = {};
  @observable.ref vertexSizeMappings: Record<string, string> = {};
  @observable.ref vertexWritingMappings: Record<string, string[]> = {};
  @observable.ref edgeColorMappings: Record<string, string> = {};
  @observable.ref edgeWithArrowMappings: Record<string, boolean> = {};
  @observable.ref edgeThicknessMappings: Record<string, string> = {};
  @observable.ref edgeWritingMappings: Record<string, string[]> = {};
  @observable.ref
  originalGraphData: FetchGraphResponse = {} as FetchGraphResponse;
  @observable.ref
  graphData: FetchGraphResponse = {} as FetchGraphResponse;
  @observable.ref
  expandedGraphData: FetchGraphResponse = {} as FetchGraphResponse;
  @observable.ref
  relatedGraphEdges: string[] = [];
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

  @observable
  newGraphNodeConfigs: NewGraphData = createNewGraphDataConfig();
  @observable
  newGraphEdgeConfigs: NewGraphData = createNewGraphDataConfig();
  @observable selectedGraphData: GraphNode = createGraphNode();
  @observable
  editedSelectedGraphDataProperties = createGraphEditableProperties();
  @observable selectedGraphLinkData: GraphEdge = createGraphEdge();
  // @observable
  // editedSelectedGraphEdgeProperties = createGraphEditableProperties();
  @observable.ref rightClickedGraphData: GraphNode = createGraphNode();

  @observable pageConfigs: {
    [key: string]: {
      pageNumber: number;
      pageTotal: number;
      pageSize?: number;
    };
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

  @observable.shallow requestStatus = initalizeRequestStatus();
  @observable errorInfo = initalizeErrorInfo();

  @computed get graphNodes(): GraphNode[] {
    return this.originalGraphData.data.graph_view.vertices.map(
      ({ id, label, properties }) => {
        // if user create new node or edge in query statement
        // rather in schema manager, there's no style in default
        const joinedLabel = !isUndefined(this.vertexWritingMappings[label])
          ? this.vertexWritingMappings[label]
              .map((field) => (field === '~id' ? id : properties[field]))
              .filter((label) => label !== undefined && label !== null)
              .join('-')
          : id;

        return {
          id,
          label:
            size(joinedLabel) <= 15
              ? joinedLabel
              : joinedLabel.slice(0, 15) + '...',
          vLabel: label,
          value: vertexRadiusMapping[this.vertexSizeMappings[label]],
          font: { size: 16 },
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
                          <div>${convertArrayToString(value)}</div>
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

  @computed get graphEdges(): GraphEdge[] {
    return this.originalGraphData.data.graph_view.edges.map(
      ({ id, label, source, target, properties }) => {
        // if user create new node or edge in query statement
        // rather in schema manager, there's no style in default
        const joinedLabel = !isUndefined(this.edgeWritingMappings[label])
          ? this.edgeWritingMappings[label]
              .map((field) => (field === '~id' ? label : properties[field]))
              .join('-')
          : label;

        return {
          id,
          label:
            joinedLabel.length <= 15
              ? joinedLabel
              : joinedLabel.slice(0, 15) + '...',
          properties,
          source,
          target,
          from: source,
          to: target,
          font: {
            size: 16,
            strokeWidth: 0,
            color: '#666'
          },
          arrows: this.edgeWithArrowMappings[label] ? 'to' : '',
          color: this.edgeColorMappings[label],
          value: edgeWidthMapping[this.edgeThicknessMappings[label]],
          title: `
            <div class="tooltip-fields">
              <div>边类型：</div>
            <div>${label}</div>
            </div>
            <div class="tooltip-fields">
              <div>边ID：</div>
              <div>${id}</div>
            </div>
            ${Object.entries(properties)
              .map(([key, value]) => {
                return `<div class="tooltip-fields">
                            <div>${key}: </div>
                            <div>${convertArrayToString(value)}</div>
                          </div>`;
              })
              .join('')}
          `
        };
      }
    );
  }

  @action
  setCurrentId(id: number) {
    this.currentId = id;
  }

  @action
  setCurrentTab(tab: string) {
    this.currentTab = tab;
  }

  @action
  switchGraphLoaded(flag: boolean) {
    this.isGraphLoaded = flag;
  }

  @action
  setFullScreenReuslt(flag: boolean) {
    this.isFullScreenReuslt = flag;
  }

  @action
  setQueryMode(mode: 'query' | 'task') {
    this.queryMode = mode;
  }

  @action
  setDynamicAddGraphDataStatus(status: string) {
    this.dynamicAddGraphDataStatus = status;
  }

  @action
  setNewGraphDataConfig<T extends keyof NewGraphData>(
    type: 'vertex' | 'edge',
    key: T,
    value: NewGraphData[T]
  ) {
    if (type === 'vertex') {
      this.newGraphNodeConfigs[key] = value;
    } else {
      this.newGraphEdgeConfigs[key] = value;
    }
  }

  @action
  setNewGraphDataConfigProperties(
    type: 'vertex' | 'edge',
    nullable: 'nullable' | 'nonNullable',
    key: string,
    value: string
  ) {
    if (type === 'vertex') {
      if (nullable === 'nullable') {
        this.newGraphNodeConfigs.properties.nullable.set(key, value);
      } else {
        this.newGraphNodeConfigs.properties.nonNullable.set(key, value);
      }
    } else {
      if (nullable === 'nullable') {
        this.newGraphEdgeConfigs.properties.nullable.set(key, value);
      } else {
        this.newGraphEdgeConfigs.properties.nonNullable.set(key, value);
      }
    }
  }

  @action
  syncNewGraphDataProperties(type: 'vertex' | 'edge') {
    const config =
      type === 'vertex' ? this.newGraphNodeConfigs : this.newGraphEdgeConfigs;
    config.properties.nonNullable.clear();
    config.properties.nullable.clear();

    const selectedLabel =
      type === 'vertex'
        ? this.vertexTypes.find(({ name }) => name === config.label)
        : this.edgeTypes.find(({ name }) => name === config.label);

    if (!isUndefined(selectedLabel)) {
      const nonNullableProperties = selectedLabel.properties.filter(
        ({ nullable }) => !nullable
      );

      nonNullableProperties.forEach(({ name }) => {
        config.properties.nonNullable.set(name, '');
      });

      const nullableProperties = selectedLabel.properties.filter(
        ({ nullable }) => nullable
      );

      nullableProperties.forEach(({ name }) => {
        config.properties.nullable.set(name, '');
      });
    }
  }

  @action
  syncGraphEditableProperties(type: 'vertex' | 'edge') {
    Object.values(this.editedSelectedGraphDataProperties).forEach(
      (property) => {
        property.clear();
      }
    );

    const selectedLabel =
      type === 'vertex'
        ? this.vertexTypes.find(
            ({ name }) => name === this.selectedGraphData.label
          )
        : this.edgeTypes.find(
            ({ name }) => name === this.selectedGraphLinkData.label
          );

    if (!isUndefined(selectedLabel)) {
      const selectedGraphData =
        type === 'vertex' ? this.selectedGraphData : this.selectedGraphLinkData;
      const selectedGraphDataPropertKeys = Object.keys(
        type === 'vertex'
          ? this.selectedGraphData.properties
          : this.selectedGraphLinkData.properties
      );

      // to keep sort of primary keys, need to iter it first
      if (type === 'vertex') {
        (selectedLabel as VertexType).primary_keys.forEach((name) => {
          if (selectedGraphDataPropertKeys.includes(name)) {
            this.editedSelectedGraphDataProperties.primary.set(
              name,
              convertArrayToString(selectedGraphData.properties[name])
            );

            remove(selectedGraphDataPropertKeys, (key) => key === name);
          }
        });
      } else {
        (selectedLabel as EdgeType).sort_keys.forEach((name) => {
          if (selectedGraphDataPropertKeys.includes(name)) {
            this.editedSelectedGraphDataProperties.primary.set(
              name,
              convertArrayToString(selectedGraphData.properties[name])
            );

            remove(selectedGraphDataPropertKeys, (key) => key === name);
          }
        });
      }

      selectedLabel.properties
        .filter(({ nullable }) => !nullable)
        .forEach(({ name }) => {
          if (selectedGraphDataPropertKeys.includes(name)) {
            this.editedSelectedGraphDataProperties.nonNullable.set(
              name,
              convertArrayToString(selectedGraphData.properties[name])
            );
          }

          remove(selectedGraphDataPropertKeys, (key) => key === name);
        });

      selectedLabel.properties
        .filter(({ nullable }) => nullable)
        .forEach(({ name }) => {
          if (selectedGraphDataPropertKeys.includes(name)) {
            this.editedSelectedGraphDataProperties.nullable.set(
              name,
              convertArrayToString(selectedGraphData.properties[name])
            );
          }
        });
    }
  }

  @action
  resetNewGraphData(type: 'vertex' | 'edge') {
    if (type === 'vertex') {
      this.newGraphNodeConfigs = createNewGraphDataConfig();
    } else {
      this.newGraphEdgeConfigs = createNewGraphDataConfig();
    }
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
  setVisCurrentCoordinates(coordinates: {
    domX: string;
    domY: string;
    canvasX: string;
    canvasY: string;
  }) {
    this.visCurrentCoordinates = coordinates;
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
    const [week, month, day, year, time] = date.toString().split(' ');
    const timeString = `${year}-${monthMaps[month]}-${day} ${time}`;

    const tempData: ExecutionLogs = {
      id: NaN,
      async_id: NaN,
      type: 'GREMLIN',
      content: this.codeEditorText,
      status: 'RUNNING',
      duration: '0ms',
      create_time: timeString
    };

    this.executionLogData = [tempData].concat(
      this.executionLogData.slice(0, 9)
    );

    return window.setInterval(() => {
      this.executionLogData[0].duration =
        String(Number(this.executionLogData[0].duration.split('ms')[0]) + 10) +
        'ms';

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

  @observable
  validateAddGraphNodeErrorMessage: NewGraphData | null = null;
  @observable
  validateAddGraphEdgeErrorMessage: NewGraphData | null = null;
  @observable
  validateEditableGraphDataPropertyErrorMessage: EditableProperties | null = null;

  @action
  initValidateAddGraphDataErrorMessage(type: 'vertex' | 'edge') {
    const config =
      type === 'vertex' ? this.newGraphNodeConfigs : this.newGraphEdgeConfigs;
    const nonNullable = new Map([...config.properties.nonNullable]);
    const nullable = new Map([...config.properties.nullable]);

    if (type === 'vertex') {
      this.validateAddGraphNodeErrorMessage = {
        id: '',
        label: '',
        properties: {
          nonNullable,
          nullable
        }
      };
    } else {
      this.validateAddGraphEdgeErrorMessage = {
        id: '',
        label: '',
        properties: {
          nonNullable,
          nullable
        }
      };
    }
  }

  @action
  editGraphDataProperties(
    nullable: 'nonNullable' | 'nullable',
    key: string,
    value: string
  ) {
    this.editedSelectedGraphDataProperties[nullable].set(key, value);
  }

  @action
  initValidateEditGraphDataPropertiesErrorMessage() {
    this.validateEditableGraphDataPropertyErrorMessage = {
      nonNullable: new Map(),
      nullable: new Map()
    };

    this.editedSelectedGraphDataProperties.nonNullable.forEach((value, key) => {
      this.validateEditableGraphDataPropertyErrorMessage!.nonNullable.set(
        key,
        ''
      );
    });

    this.editedSelectedGraphDataProperties.nullable.forEach((value, key) => {
      this.validateEditableGraphDataPropertyErrorMessage!.nullable.set(key, '');
    });
  }

  @action
  validateAddGraphNode(
    idStrategy: string,
    initial = false,
    category?: 'id' | 'nonNullable' | 'nullable',
    key?: string
  ) {
    if (category === 'id') {
      if (idStrategy === 'CUSTOMIZE_STRING') {
        this.validateAddGraphNodeErrorMessage!.id =
          initial || !isEmpty(this.newGraphNodeConfigs.id)
            ? ''
            : '非法的数据格式';
      }

      if (idStrategy === 'CUSTOMIZE_NUMBER') {
        this.validateAddGraphNodeErrorMessage!.id =
          initial || isInt(this.newGraphNodeConfigs.id!)
            ? ''
            : '非法的数据格式';
      }

      if (idStrategy === 'CUSTOMIZE_UUID') {
        this.validateAddGraphNodeErrorMessage!.id =
          initial || isUUID(String(this.newGraphNodeConfigs.id), 4)
            ? ''
            : '非法的数据格式';
      }
    }

    if (category === 'nonNullable') {
      if (
        initial ||
        isEmpty(this.newGraphNodeConfigs.properties.nonNullable.get(key!))
      ) {
        this.validateAddGraphNodeErrorMessage?.properties.nonNullable.set(
          key!,
          '此项不能为空'
        );

        return;
      }

      if (
        initial ||
        !validateGraphProperty(
          this.valueTypes[key!],
          this.newGraphNodeConfigs.properties.nonNullable.get(key!)!
        )
      ) {
        this.validateAddGraphNodeErrorMessage?.properties.nonNullable.set(
          key!,
          '非法的数据格式'
        );

        return;
      }

      this.validateAddGraphNodeErrorMessage?.properties.nonNullable.set(
        key!,
        ''
      );
    }

    if (category === 'nullable') {
      if (
        initial ||
        !validateGraphProperty(
          this.valueTypes[key!],
          this.newGraphNodeConfigs.properties.nullable.get(key!)!,
          true
        )
      ) {
        this.validateAddGraphNodeErrorMessage?.properties.nullable.set(
          key!,
          '非法的数据格式'
        );

        return;
      }

      this.validateAddGraphNodeErrorMessage?.properties.nullable.set(key!, '');
    }
  }

  @action
  validateAddGraphEdge(
    category: 'id' | 'nonNullable' | 'nullable',
    initial = false,
    key?: string
  ) {
    if (category === 'id') {
      this.validateAddGraphEdgeErrorMessage!.id =
        initial || !isEmpty(this.newGraphEdgeConfigs.id)
          ? ''
          : '非法的数据格式';
    }

    if (category === 'nonNullable') {
      if (
        initial ||
        isEmpty(this.newGraphEdgeConfigs.properties.nonNullable.get(key!))
      ) {
        this.validateAddGraphEdgeErrorMessage?.properties.nonNullable.set(
          key!,
          '此项不能为空'
        );

        return;
      }

      if (
        initial ||
        !validateGraphProperty(
          this.valueTypes[key!],
          this.newGraphEdgeConfigs.properties.nonNullable.get(key!)!
        )
      ) {
        this.validateAddGraphEdgeErrorMessage?.properties.nonNullable.set(
          key!,
          '非法的数据格式'
        );

        return;
      }

      this.validateAddGraphEdgeErrorMessage?.properties.nonNullable.set(
        key!,
        ''
      );
    }

    if (category === 'nullable') {
      if (
        initial ||
        !validateGraphProperty(
          this.valueTypes[key!],
          this.newGraphEdgeConfigs.properties.nullable.get(key!)!,
          true
        )
      ) {
        this.validateAddGraphEdgeErrorMessage?.properties.nullable.set(
          key!,
          '非法的数据格式'
        );

        return;
      }

      this.validateAddGraphEdgeErrorMessage?.properties.nullable.set(key!, '');
    }
  }

  @action
  validateGraphDataEditableProperties(
    type: 'nonNullable' | 'nullable',
    key: string
  ) {
    if (type === 'nonNullable') {
      if (
        isEmpty(this.editedSelectedGraphDataProperties?.nonNullable.get(key))
      ) {
        this.validateEditableGraphDataPropertyErrorMessage?.nonNullable.set(
          key,
          '此项不能为空'
        );

        return;
      }

      if (
        !validateGraphProperty(
          this.valueTypes[key!],
          this.editedSelectedGraphDataProperties?.nonNullable.get(key)
        )
      ) {
        this.validateEditableGraphDataPropertyErrorMessage?.nonNullable.set(
          key,
          '非法的数据格式'
        );

        return;
      }

      this.validateEditableGraphDataPropertyErrorMessage?.nonNullable.set(
        key,
        ''
      );
    }

    if (type === 'nullable') {
      if (
        !validateGraphProperty(
          this.valueTypes[key!],
          this.editedSelectedGraphDataProperties?.nullable.get(key),
          true
        )
      ) {
        this.validateEditableGraphDataPropertyErrorMessage?.nullable.set(
          key,
          '非法的数据格式'
        );

        return;
      }

      this.validateEditableGraphDataPropertyErrorMessage?.nullable.set(key, '');
    }
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
  resetSwitchTabState() {
    this.isLoadingGraph = false;
    this.isGraphLoaded = false;
    this.isShowGraphInfo = false;
    this.isClickOnNodeOrEdge = false;
    this.queryMode = 'query';
    this.dynamicAddGraphDataStatus = '';
    this.graphData = {} as FetchGraphResponse;

    this.visNetwork = null;
    this.visDataSet = null;
    this.visCurrentCoordinates = {
      domX: '',
      domY: '',
      canvasX: '',
      canvasY: ''
    };

    this.selectedGraphData = createGraphNode();
    this.selectedGraphLinkData = createGraphEdge();

    this.pageConfigs.tableResult = {
      pageNumber: 1,
      pageTotal: 0
    };

    this.requestStatus.fetchGraphs = 'standby';
    this.requestStatus.createAsyncTask = 'standby';
    this.errorInfo.fetchGraphs = {
      code: NaN,
      message: ''
    };
    this.errorInfo.createAsyncTask = {
      code: NaN,
      message: ''
    };

    this.clearFilteredGraphQueryOptions();
  }

  @action
  resetIdState() {
    this.currentId = null;
    this.currentTab = 'gremlin-analyze';
    this.searchText = '';
    this.isSidebarExpanded = false;
    this.isLoadingGraph = false;
    this.isGraphLoaded = false;
    this.isShowGraphInfo = false;
    this.isFullScreenReuslt = false;
    this.isClickOnNodeOrEdge = false;
    this.queryMode = 'query';
    this.codeEditorText = '';
    this.dynamicAddGraphDataStatus = '';
    this.graphData = {} as FetchGraphResponse;

    this.visNetwork = null;
    this.visDataSet = null;
    this.visCurrentCoordinates = {
      domX: '',
      domY: '',
      canvasX: '',
      canvasY: ''
    };

    this.properties = [];

    this.isSearched = {
      status: false,
      value: ''
    };

    this.selectedGraphData = createGraphNode();
    this.selectedGraphLinkData = createGraphEdge();

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

    this.requestStatus = initalizeRequestStatus();
    this.errorInfo = initalizeErrorInfo();
    this.clearFilteredGraphQueryOptions();
  }

  @action
  dispose() {
    this.resetIdState();
    this.vertexTypes = [];
    this.edgeTypes = [];
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

      this.properties = result.data.data.records;

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

  fetchVertexTypes = flow(function* fetchVertexTypeList(
    this: DataAnalyzeStore
  ) {
    this.requestStatus.fetchVertexTypeList = 'pending';

    try {
      const result: AxiosResponse<responseData<
        VertexTypeListResponse
      >> = yield axios
        .get<responseData<VertexTypeListResponse>>(
          `${baseUrl}/${this.currentId}/schema/vertexlabels`,
          {
            params: {
              page_size: -1
            }
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.fetchVertexTypeList.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.vertexTypes = result.data.data.records;
      this.requestStatus.fetchVertexTypeList = 'success';
    } catch (error) {
      this.requestStatus.fetchVertexTypeList = 'failed';
      this.errorInfo.fetchVertexTypeList.message = error.message;
    }
  });

  fetchColorSchemas = flow(function* fetchColorSchemas(this: DataAnalyzeStore) {
    this.requestStatus.fetchColorSchemas = 'pending';

    try {
      const result: AxiosResponse<FetchColorSchemas> = yield axios.get<
        FetchGraphResponse
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

  fetchAllNodeStyle = flow(function* fetchAllNodeStyle(this: DataAnalyzeStore) {
    this.requestStatus.fetchAllNodeStyle = 'pending';
    try {
      const result: AxiosResponse<responseData<
        VertexTypeListResponse
      >> = yield axios.get(`${baseUrl}/${this.currentId}/schema/vertexlabels`, {
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
        if (style.size !== null) {
          this.vertexSizeMappings[name] = style.size;
        }
        if (style.display_fields.length !== 0) {
          this.vertexWritingMappings[name] = style.display_fields;
        }
      });

      this.requestStatus.fetchAllNodeStyle = 'success';
    } catch (error) {
      this.requestStatus.fetchAllNodeStyle = 'failed';
      this.errorInfo.fetchAllNodeStyle.message = error.message;
      console.error(error.message);
    }
  });

  fetchEdgeTypes = flow(function* fetchEdgeTypes(this: DataAnalyzeStore) {
    this.requestStatus.fetchEdgeTypes = 'pending';

    try {
      const result: AxiosResponse<responseData<
        EdgeTypeListResponse
      >> = yield axios.get(`${baseUrl}/${this.currentId}/schema/edgelabels`, {
        params: {
          page_no: 1,
          page_size: -1
        }
      });

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      this.edgeTypes = result.data.data.records;
      this.requestStatus.fetchEdgeTypes = 'success';
    } catch (error) {
      this.requestStatus.fetchEdgeTypes = 'failed';
      this.errorInfo.fetchEdgeTypes.message = error.message;
      console.error(error.message);
    }
  });

  fetchAllEdgeStyle = flow(function* fetchAllEdgeStyle(this: DataAnalyzeStore) {
    this.requestStatus.fetchAllEdgeStyle = 'pending';

    try {
      const result: AxiosResponse<responseData<
        EdgeTypeListResponse
      >> = yield axios.get(`${baseUrl}/${this.currentId}/schema/edgelabels`, {
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
          this.edgeColorMappings[name] = style.color;
        }
        if (style.with_arrow !== null) {
          this.edgeWithArrowMappings[name] = style.with_arrow;
        }
        if (style.thickness !== null) {
          this.edgeThicknessMappings[name] = style.thickness;
        }
        if (style.display_fields.length !== 0) {
          this.edgeWritingMappings[name] = style.display_fields;
        }
      });

      this.requestStatus.fetchAllEdgeStyle = 'success';
    } catch (error) {
      this.requestStatus.fetchAllEdgeStyle = 'failed';
      this.errorInfo.fetchAllEdgeStyle.message = error.message;
      console.error(error.message);
    }
  });

  fetchGraphs = flow(function* fetchGraphs(
    this: DataAnalyzeStore,
    algorithmConfigs?: { url: string; type: string }
  ) {
    // reset request status of create async task
    this.requestStatus.createAsyncTask = 'standby';
    this.requestStatus.fetchGraphs = 'pending';
    this.isLoadingGraph = true;

    let params:
      | LoopDetectionParams
      | FocusDetectionParams
      | ShortestPathAlgorithmParams
      | ShortestPathAllAlgorithmParams
      | AllPathAlgorithmParams
      | ModelSimilarityParams
      | NeighborRankParams
      | null = null;

    if (!isUndefined(algorithmConfigs)) {
      switch (algorithmConfigs.type) {
        case Algorithm.loopDetection: {
          if (
            this.algorithmAnalyzerStore.loopDetectionParams.label === '__all__'
          ) {
            const clonedParams: LoopDetectionParams = cloneDeep(
              this.algorithmAnalyzerStore.loopDetectionParams
            );

            delete clonedParams.label;
            params = clonedParams;
            break;
          }

          params = this.algorithmAnalyzerStore.loopDetectionParams;
          break;
        }
        case Algorithm.focusDetection: {
          if (
            this.algorithmAnalyzerStore.loopDetectionParams.label === '__all__'
          ) {
            const clonedParams: FocusDetectionParams = cloneDeep(
              this.algorithmAnalyzerStore.focusDetectionParams
            );

            delete clonedParams.label;
            params = clonedParams;
            break;
          }

          params = this.algorithmAnalyzerStore.focusDetectionParams;
          break;
        }
        case Algorithm.shortestPath: {
          if (
            this.algorithmAnalyzerStore.shortestPathAlgorithmParams.label ===
            '__all__'
          ) {
            const clonedParams: ShortestPathAlgorithmParams = cloneDeep(
              this.algorithmAnalyzerStore.shortestPathAlgorithmParams
            );

            delete clonedParams.label;
            params = clonedParams;
            break;
          }

          params = this.algorithmAnalyzerStore.shortestPathAlgorithmParams;
          break;
        }

        case Algorithm.shortestPathAll: {
          if (
            this.algorithmAnalyzerStore.shortestPathAllParams.label ===
            '__all__'
          ) {
            const clonedParams: ShortestPathAllAlgorithmParams = cloneDeep(
              this.algorithmAnalyzerStore.shortestPathAllParams
            );

            delete clonedParams.label;
            params = clonedParams;
            break;
          }

          params = this.algorithmAnalyzerStore.shortestPathAllParams;
          break;
        }

        case Algorithm.allPath: {
          if (this.algorithmAnalyzerStore.allPathParams.label === '__all__') {
            const clonedParams: AllPathAlgorithmParams = cloneDeep(
              this.algorithmAnalyzerStore.allPathParams
            );

            delete clonedParams.label;
            params = clonedParams;
            break;
          }

          params = this.algorithmAnalyzerStore.allPathParams;
          break;
        }

        case Algorithm.modelSimilarity: {
          const {
            source,
            vertexType,
            vertexProperty,
            direction,
            least_neighbor,
            similarity,
            label,
            max_similar,
            least_similar,
            property_filter,
            least_property_number,
            max_degree,
            skip_degree,
            capacity,
            limit,
            return_common_connection,
            return_complete_info
          } = this.algorithmAnalyzerStore.modelSimilarityParams;

          const convertedParams = {
            sources: {
              ids: [source],
              label: vertexType,
              properties: vertexProperty
            },
            label,
            direction,
            min_neighbors: least_neighbor,
            alpha: similarity,
            min_similars: least_similar,
            top: max_similar,
            group_property: property_filter,
            min_groups: least_property_number,
            max_degree,
            capacity,
            limit,
            with_intermediary: return_common_connection,
            with_vertex: return_complete_info
          };

          if (label === '__all__') {
            delete convertedParams.label;
          }

          // @ts-ignore
          params = convertedParams;
          break;
        }

        case Algorithm.neighborRankRecommendation: {
          const clonedNeighborRankParams = cloneDeep(
            this.algorithmAnalyzerStore.neighborRankParams
          );

          clonedNeighborRankParams.steps.forEach((step, index) => {
            delete step.uuid;

            if (step.label === '__all__') {
              const clonedStep: NeighborRankRule = cloneDeep(step);
              delete clonedStep.label;
              clonedNeighborRankParams.steps[index] = clonedStep;
            }
          });

          params = clonedNeighborRankParams;
          break;
        }

        // default:
        //   params = this.algorithmAnalyzerStore.shortestPathAlgorithmParams;
      }
    }

    try {
      let result: AxiosResponse<FetchGraphResponse>;

      if (!isUndefined(algorithmConfigs)) {
        result = yield axios
          .post<FetchGraphResponse>(
            `${baseUrl}/${this.currentId}/algorithms/${algorithmConfigs.url}`,
            {
              ...params
            }
          )
          .catch(checkIfLocalNetworkOffline);
      } else {
        result = yield axios
          .post<FetchGraphResponse>(
            `${baseUrl}/${this.currentId}/gremlin-query`,
            {
              content: this.codeEditorText
            }
          )
          .catch(checkIfLocalNetworkOffline);
      }

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

  createAsyncTask = flow(function* createAsyncTask(this: DataAnalyzeStore) {
    // reset request status of fetch graphs
    this.requestStatus.fetchGraphs = 'stanby';
    this.requestStatus.createAsyncTask = 'pending';

    try {
      const result: AxiosResponse<FetchGraphResponse> = yield axios
        .post<FetchGraphResponse>(
          `${baseUrl}/${this.currentId}/gremlin-query/async-task`,
          {
            content: this.codeEditorText
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.createAsyncTask.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.requestStatus.createAsyncTask = 'success';
    } catch (error) {
      this.requestStatus.createAsyncTask = 'failed';
      this.errorInfo.createAsyncTask.message = error.message;
      console.error(error.message);
    }
  });

  addGraphNode = flow(function* addGraphNode(this: DataAnalyzeStore) {
    this.requestStatus.addGraphNode = 'pending';

    try {
      const properties: Record<string, string> = {};
      this.newGraphNodeConfigs.properties.nonNullable.forEach((value, key) => {
        properties[key] = value;
      });
      this.newGraphNodeConfigs.properties.nullable.forEach((value, key) => {
        properties[key] = value;
      });

      const result: AxiosResponse<responseData<GraphView>> = yield axios.post(
        `${baseUrl}/${this.currentId}/graph/vertex`,
        {
          id: this.newGraphNodeConfigs.id,
          label: this.newGraphNodeConfigs.label,
          properties
        }
      );

      if (result.data.status !== 200) {
        this.errorInfo.addGraphNode.code = result.data.status;
        throw new Error(result.data.message);
      }

      const mergedGraphData = cloneDeep(this.graphData);
      mergedGraphData.data.graph_view.vertices.push(
        ...result.data.data.vertices
      );

      this.graphData = mergedGraphData;
      this.requestStatus.addGraphNode = 'success';

      return result.data.data.vertices;
    } catch (error) {
      this.requestStatus.addGraphNode = 'failed';
      this.errorInfo.addGraphNode.message = error.message;
      console.error(error.message);
    }
  });

  fetchRelatedEdges = flow(function* fetchRelatedEdges(this: DataAnalyzeStore) {
    this.requestStatus.fetchRelatedEdges = 'pending';
    try {
      const result: AxiosResponse<responseData<string[]>> = yield axios.get<
        responseData<string[]>
      >(
        `${baseUrl}/${this.currentId}/schema/vertexlabels/${this.rightClickedGraphData.label}/link`
      );

      if (result.data.status !== 200) {
        this.errorInfo.fetchRelatedEdges.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.relatedGraphEdges = result.data.data;
      this.requestStatus.fetchRelatedEdges = 'success';
    } catch (error) {
      this.requestStatus.fetchRelatedEdges = 'failed';
      this.errorInfo.fetchRelatedEdges.message = error.message;
      console.error(error.message);
    }
  });

  addGraphEdge = flow(function* addGraphEdge(this: DataAnalyzeStore) {
    this.requestStatus.addGraphEdge = 'pending';

    try {
      const vertices = [
        this.rightClickedGraphData.id,
        this.newGraphEdgeConfigs.id
      ];

      if (this.dynamicAddGraphDataStatus === 'inEdge') {
        vertices.reverse();
      }

      const properties: Record<string, string> = {};
      this.newGraphEdgeConfigs.properties.nonNullable.forEach((value, key) => {
        properties[key] = value;
      });
      this.newGraphEdgeConfigs.properties.nullable.forEach((value, key) => {
        properties[key] = value;
      });

      const result: AxiosResponse<responseData<GraphView>> = yield axios.post(
        `${baseUrl}/${this.currentId}/graph/edge`,
        {
          label: this.newGraphEdgeConfigs.label,
          source: vertices[0],
          target: vertices[1],
          properties
        }
      );

      if (result.data.status !== 200) {
        this.errorInfo.addGraphEdge.code = result.data.status;
        throw new Error(result.data.message);
      }

      const mergedGraphData = cloneDeep(this.graphData);
      const filteredVertices: GraphNode[] = [];

      result.data.data.vertices.forEach((vertex) => {
        if (this.visDataSet!.nodes.get(vertex.id) === null) {
          filteredVertices.push(vertex);
        }
      });

      mergedGraphData.data.graph_view.vertices.push(...filteredVertices);
      mergedGraphData.data.graph_view.edges.push(...result.data.data.edges);

      this.graphData = mergedGraphData;
      this.requestStatus.addGraphEdge = 'success';

      return {
        originalVertices: result.data.data.vertices,
        vertices: filteredVertices,
        edges: result.data.data.edges
      };
    } catch (error) {
      this.requestStatus.addGraphEdge = 'failed';
      this.errorInfo.addGraphEdge.message = error.message;
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
      const result: AxiosResponse<FetchGraphResponse> = yield axios.put(
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

      const mergeData: FetchGraphResponse = {
        ...newGraphData,
        data: {
          ...newGraphData.data,
          graph_view: {
            vertices: this.graphData.data.graph_view.vertices
              .concat(newGraphData.data.graph_view.vertices)
              .filter((item) => {
                const isDuplicate = vertexCollection.has(item.id);
                vertexCollection.add(item.id);
                return !isDuplicate;
              }),
            edges: this.graphData.data.graph_view.edges
              .concat(newGraphData.data.graph_view.edges)
              .filter((item) => {
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
      (data) => data.id !== this.rightClickedGraphData.id
    );

    // only delete node in vertexCollection, not edges in EdgeCollection
    this.vertexCollection.delete(nodeId);

    // assign new object to observable
    this.graphData = { ...this.graphData };
  }

  updateGraphProperties = flow(function* updateGraphProperties(
    this: DataAnalyzeStore
  ) {
    this.requestStatus.updateGraphProperties = 'pending';

    const { id, label, properties } =
      this.graphInfoDataSet === 'node'
        ? this.selectedGraphData
        : this.selectedGraphLinkData;

    const editedProperties: Record<string, string | Array<string>> = {
      ...Object.fromEntries([
        ...this.editedSelectedGraphDataProperties.primary
      ]),
      ...Object.fromEntries([
        ...this.editedSelectedGraphDataProperties.nonNullable
      ]),
      ...Object.fromEntries([
        ...this.editedSelectedGraphDataProperties.nullable
      ])
    };

    // check if originial type is Array
    Object.entries(editedProperties).forEach(([key, value]) => {
      if (Array.isArray(properties[key])) {
        if ((value as string).includes(',')) {
          editedProperties[key] = (value as string).split(',');
        }

        if ((value as string).includes('，')) {
          editedProperties[key] = (value as string).split('，');
        }
      }
    });

    try {
      const result: AxiosResponse<responseData<
        GraphNode | GraphEdge
      >> = yield axios.put<responseData<GraphNode | GraphEdge>>(
        `${baseUrl}/${this.currentId}/graph/${
          this.graphInfoDataSet === 'node' ? 'vertex' : 'edge'
        }/${encodeURIComponent(id)}`,
        this.graphInfoDataSet === 'node'
          ? {
              id,
              label,
              properties: editedProperties
            }
          : {
              id,
              label,
              properties: editedProperties,
              source: this.selectedGraphLinkData.source,
              target: this.selectedGraphLinkData.target
            }
      );

      if (result.data.status !== 200) {
        this.errorInfo.updateGraphProperties.code = result.data.status;
        throw new Error(result.data.message);
      }

      const graphData = cloneDeep(this.graphData);
      if (this.graphInfoDataSet === 'node') {
        const vertex = graphData.data.graph_view.vertices.find(
          ({ id }) => id === result.data.data.id
        );

        if (!isUndefined(vertex)) {
          vertex.id = result.data.data.id;
          vertex.label = result.data.data.label;
          vertex.properties = result.data.data.properties;
        }
      } else {
        const edge = graphData.data.graph_view.edges.find(
          ({ id }) => id === result.data.data.id
        );

        if (!isUndefined(edge)) {
          edge.id = result.data.data.id;
          edge.label = result.data.data.label;
          edge.properties = result.data.data.properties;
        }
      }

      this.graphData = graphData;
      this.requestStatus.updateGraphProperties = 'success';
      return result.data.data;
    } catch (error) {
      this.requestStatus.updateGraphProperties = 'failed';
      this.errorInfo.updateGraphProperties.message = error.message;
      console.error(error.message);
    }
  });

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
      const result: AxiosResponse<FetchFilteredPropertyOptions> = yield axios.get(
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
      const result: AxiosResponse<FetchGraphResponse> = yield axios.put(
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

      const mergeData: FetchGraphResponse = {
        ...newGraphData,
        data: {
          ...newGraphData.data,
          graph_view: {
            vertices: this.graphData.data.graph_view.vertices
              .concat(newGraphData.data.graph_view.vertices)
              .filter((item) => {
                const isDuplicate = vertexCollection.has(item.id);
                vertexCollection.add(item.id);
                return !isDuplicate;
              }),
            edges: this.graphData.data.graph_view.edges
              .concat(newGraphData.data.graph_view.edges)
              .filter((item) => {
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
