import { createContext } from 'react';
import { observable, action, flow } from 'mobx';
import axios, { AxiosResponse } from 'axios';

import { baseUrl, responseData } from '../types/common';
import {
  LincenseInfo,
  GraphData,
  GraphDataConfig,
  GraphDataPageConfig,
  GraphDataResponse
} from '../types/GraphManagementStore/graphManagementStore';
import i18next from '../../i18n';

export class GraphManagementStore {
  [key: string]: any;

  // display create new graph layout
  @observable showCreateNewGraph = false;

  // display delete modal after dropdown click in GraphList
  @observable showDeleteModal = false;

  // disable all other buttons except from the current new/edit layout
  @observable selectedEditIndex: number | null = null;

  // values from the Search component
  @observable searchWords = '';

  @observable errorInfo = {
    fetchLicenseInfo: {
      code: NaN,
      message: ''
    },
    fetchIdList: {
      code: NaN,
      message: ''
    },
    fetchGraphData: {
      code: NaN,
      message: ''
    },
    AddGraphData: {
      code: NaN,
      message: ''
    },
    upgradeGraphData: {
      code: NaN,
      message: ''
    },
    deleteGraphData: {
      code: NaN,
      message: ''
    }
  };

  // searched results rather than initial fetched result
  @observable.shallow isSearched = {
    status: false,
    value: ''
  };

  // is sidebar in graphdata details has expanded
  @observable isExpanded = false;

  // is clicekd submit or save to validate
  @observable isValidated = false;

  @observable.shallow requestStatus = {
    fetchLicenseInfo: 'standby',
    fetchIdList: 'standby',
    fetchGraphData: 'standby',
    AddGraphData: 'standby',
    upgradeGraphData: 'standby',
    deleteGraphData: 'standby'
  };

  @observable validateErrorMessage: Record<string, string> = {
    name: '',
    graph: '',
    host: '',
    port: '',
    usernameAndPassword: ''
  };

  @observable.shallow newGraphData: GraphDataConfig = {
    name: '',
    graph: '',
    host: '',
    port: '',
    username: '',
    password: ''
  };

  @observable.shallow editGraphData: GraphDataConfig = {
    name: '',
    graph: '',
    host: '',
    port: '',
    username: '',
    password: ''
  };

  @observable.ref idList: { id: number; name: string }[] = [];
  @observable.ref graphData: GraphData[] = [];
  @observable.ref licenseInfo: LincenseInfo | null = null;

  @observable.shallow graphDataPageConfig: GraphDataPageConfig = {
    pageNumber: 1,
    pageSize: 10,
    pageTotal: 0
  };

  @action
  dispose() {
    this.showCreateNewGraph = false;
    this.showDeleteModal = false;
    this.selectedEditIndex = null;
    this.searchWords = '';
    this.isSearched = {
      status: false,
      value: ''
    };
    this.isValidated = false;
    this.isExpanded = false;
    this.requestStatus = {
      fetchLicenseInfo: 'standby',
      fetchIdList: 'standby',
      fetchGraphData: 'standby',
      AddGraphData: 'standby',
      upgradeGraphData: 'standby',
      deleteGraphData: 'standby'
    };
    this.graphData = [];
    this.graphDataPageConfig = {
      pageNumber: 1,
      pageSize: 10,
      pageTotal: 0
    };
    this.resetGraphDataConfig('new');
    this.resetGraphDataConfig('edit');
    this.resetErrorInfo();
    this.resetValidateErrorMessage();
  }

  @action
  switchCreateNewGraph(flag: boolean) {
    this.showCreateNewGraph = flag;
  }

  @action
  switchDeleteModal(flag: boolean) {
    this.showDeleteModal = flag;
  }

  @action
  switchExpanded(flag: boolean) {
    this.isExpanded = flag;
  }

  @action
  changeSelectedEditIndex(index: number | null) {
    this.selectedEditIndex = index;
  }

  @action
  mutateSearchWords(text: string) {
    this.searchWords = text;
  }

  @action
  mutateGraphDataConfig(key: string, type: 'new' | 'edit') {
    return (eventTarget: HTMLInputElement) => {
      this.isValidated = false;

      if (type === 'new') {
        this.newGraphData[key] = eventTarget.value;
      }

      if (type === 'edit') {
        this.editGraphData[key] = eventTarget.value;
      }
    };
  }

  @action
  swtichIsSearchedStatus(isSearched: boolean) {
    this.isSearched.status = isSearched;

    isSearched
      ? (this.isSearched.value = this.searchWords)
      : (this.isSearched.value = '');
  }

  @action
  fillInGraphDataConfig(index: number) {
    this.editGraphData.id = String(this.graphData[index].id);
    this.editGraphData.name = this.graphData[index].name;
    this.editGraphData.graph = this.graphData[index].graph;
    this.editGraphData.host = this.graphData[index].host;
    this.editGraphData.port = String(this.graphData[index].port);
    this.editGraphData.username = this.graphData[index].username;
    this.editGraphData.password = this.graphData[index].password;
  }

  @action
  resetGraphDataConfig(type: 'new' | 'edit') {
    if (type === 'new') {
      Object.keys(this.newGraphData).forEach((key) => {
        this.newGraphData[key] = '';
      });
    }

    if (type === 'edit') {
      Object.keys(this.newGraphData).forEach((key) => {
        this.editGraphData[key] = '';
      });
    }
  }

  @action
  mutatePageNumber(pageNumber: number) {
    this.graphDataPageConfig.pageNumber = pageNumber;
  }

  @action
  validate(type: 'new' | 'edit') {
    const nameReg = /^[A-Za-z]\w{0,47}$/;
    const hostReg = /((\d{1,3}\.){3}\d{1,3}|([\w!~*'()-]+\.)*[\w!~*'()-]+)$/;
    const portReg = /^([1-9]|[1-9]\d{1}|[1-9]\d{2}|[1-9]\d{3}|[1-5]\d{4}|6[0-4]\d{3}|65[0-4]\d{2}|655[0-2]\d|6553[0-5])$/;
    const dataName = type + 'GraphData';
    let readyToSubmit = true;

    this.resetValidateErrorMessage();

    if (!nameReg.test(this[dataName].name)) {
      this[dataName].name.length === 0
        ? (this.validateErrorMessage.name = i18next.t(
            'addition.store.required'
          ))
        : (this.validateErrorMessage.name = i18next.t(
            'addition.store.no-match-input-requirements'
          ));
      readyToSubmit = false;
    }

    if (!nameReg.test(this[dataName].graph)) {
      this[dataName].graph.length === 0
        ? (this.validateErrorMessage.graph = i18next.t(
            'addition.store.required'
          ))
        : (this.validateErrorMessage.graph = i18next.t(
            'addition.store.no-match-input-requirements'
          ));
      readyToSubmit = false;
    }

    if (!hostReg.test(this[dataName].host)) {
      this[dataName].host.length === 0
        ? (this.validateErrorMessage.host = i18next.t(
            'addition.store.required'
          ))
        : (this.validateErrorMessage.host = i18next.t('addition.store.rule1'));
      readyToSubmit = false;
    }

    if (!portReg.test(this[dataName].port)) {
      this[dataName].port.length === 0
        ? (this.validateErrorMessage.port = i18next.t(
            'addition.store.required'
          ))
        : (this.validateErrorMessage.port = i18next.t('addition.store.rule2'));
      readyToSubmit = false;
    }

    if (
      dataName === 'newGraphData' &&
      ((this[dataName].username.length !== 0 &&
        this[dataName].password.length === 0) ||
        (this[dataName].username.length === 0 &&
          this[dataName].password.length !== 0))
    ) {
      this.validateErrorMessage.usernameAndPassword = i18next.t(
        'addition.store.rule3'
      );
      readyToSubmit = false;
    }

    return readyToSubmit;
  }

  @action
  switchValidateStatus(flag: boolean) {
    this.isValidated = flag;
  }

  @action
  resetErrorInfo() {
    this.errorInfo = {
      fetchLicenseInfo: {
        code: NaN,
        message: ''
      },
      fetchIdList: {
        code: NaN,
        message: ''
      },
      fetchGraphData: {
        code: NaN,
        message: ''
      },
      AddGraphData: {
        code: NaN,
        message: ''
      },
      upgradeGraphData: {
        code: NaN,
        message: ''
      },
      deleteGraphData: {
        code: NaN,
        message: ''
      }
    };
  }

  @action
  resetValidateErrorMessage() {
    Object.keys(this.validateErrorMessage).forEach((key) => {
      this.validateErrorMessage[key] = '';
    });
  }

  fetchLicenseInfo = flow(function* fetchLicenseInfo(
    this: GraphManagementStore
  ) {
    this.resetErrorInfo();
    this.requestStatus.fetchLicenseInfo = 'pending';

    try {
      const result: AxiosResponse<responseData<LincenseInfo>> = yield axios.get(
        '/about'
      );

      if (result.data.status !== 200) {
        this.errorInfo.fetchLicenseInfo.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.licenseInfo = result.data.data;
    } catch (error) {
      this.requestStatus.fetchLicenseInfo = 'failed';
      this.errorInfo.fetchLicenseInfo.message = error.message;
      console.error(error.message);
    }
  });

  fetchIdList = flow(function* fetchIdList(this: GraphManagementStore) {
    this.resetErrorInfo();
    this.requestStatus.fetchIdList = 'pending';

    try {
      const result: AxiosResponse<GraphDataResponse> = yield axios.get<
        GraphData
      >(baseUrl, {
        params: {
          page_size: -1
        }
      });

      if (result.data.status === 200 || result.data.status === 401) {
        if (result.data.status === 200) {
          this.requestStatus.fetchIdList = 'success';
        }

        this.idList = result.data.data.records.map(({ id, name }) => ({
          id,
          name
        }));

        this.graphData = result.data.data.records;
        this.graphDataPageConfig.pageTotal = result.data.data.total;
      }

      if (result.data.status !== 200) {
        this.errorInfo.fetchIdList.code = result.data.status;
        throw new Error(result.data.message);
      }
    } catch (error) {
      this.requestStatus.fetchIdList = 'failed';
      this.errorInfo.fetchIdList.message = error.message;
      console.error(error.message);
    }
  });

  fetchGraphDataList = flow(function* fetchGraphDataList(
    this: GraphManagementStore
  ) {
    this.resetErrorInfo();
    const url =
      `${baseUrl}?page_no=${this.graphDataPageConfig.pageNumber}&page_size=${this.graphDataPageConfig.pageSize}` +
      (this.isSearched.status && this.searchWords !== ''
        ? `&content=${this.searchWords}`
        : '');

    this.requestStatus.fetchGraphData = 'pending';

    try {
      const result: AxiosResponse<GraphDataResponse> = yield axios.get<
        GraphData
      >(url);

      if (result.data.status === 200 || result.data.status === 401) {
        if (result.data.status === 200) {
          this.requestStatus.fetchGraphData = 'success';
        }

        this.graphData = result.data.data.records;
        this.graphDataPageConfig.pageTotal = result.data.data.total;
      }

      if (result.data.status !== 200) {
        this.errorInfo.fetchGraphData.code = result.data.status;
        throw new Error(result.data.message);
      }
    } catch (error) {
      this.requestStatus.fetchGraphData = 'failed';
      this.errorInfo.fetchGraphData.message = error.message;
      console.error(error.message);
    }
  });

  AddGraphData = flow(function* AddGraphData(this: GraphManagementStore) {
    this.resetErrorInfo();
    this.requestStatus.AddGraphData = 'pending';
    const filteredParams = filterParams(this.newGraphData);

    try {
      const result: AxiosResponse<GraphDataResponse> = yield axios.post<
        GraphDataResponse
      >(baseUrl, filteredParams);

      if (result.data.status === 200 || result.data.status === 401) {
        if (result.data.status === 200) {
          this.requestStatus.AddGraphData = 'success';
        }
      }

      if (result.data.status !== 200) {
        this.errorInfo.AddGraphData.code = result.data.status;
        throw new Error(result.data.message);
      }
    } catch (error) {
      this.requestStatus.AddGraphData = 'failed';
      this.errorInfo.AddGraphData.message = error.message;
      console.error(error.message);
    }
  });

  upgradeGraphData = flow(function* upgradeGraphData(
    this: GraphManagementStore,
    id: number
  ) {
    this.resetErrorInfo();
    this.requestStatus.upgradeGraphData = 'pending';
    const filteredParams = filterParams(this.editGraphData);

    try {
      const result: AxiosResponse<GraphDataResponse> = yield axios.put<
        GraphDataResponse
      >(`${baseUrl}/${id}`, filteredParams);

      if (result.data.status === 200 || result.data.status === 401) {
        if (result.data.status === 200) {
          this.requestStatus.upgradeGraphData = 'success';
        }
      }

      if (result.data.status !== 200) {
        this.errorInfo.upgradeGraphData.code = result.data.status;
        throw new Error(result.data.message);
      }
    } catch (error) {
      this.requestStatus.upgradeGraphData = 'failed';
      this.errorInfo.upgradeGraphData.message = error.message;
      console.error(error.message);
    }
  });

  deleteGraphData = flow(function* deleteGraphData(
    this: GraphManagementStore,
    id
  ) {
    this.resetErrorInfo();
    this.requestStatus.deleteGraphData = 'pending';

    try {
      const result: AxiosResponse<GraphDataResponse> = yield axios.delete<
        GraphDataResponse
      >(`${baseUrl}/${id}`);

      if (result.data.status === 200 || result.data.status === 401) {
        if (result.data.status === 200) {
          this.requestStatus.deleteGraphData = 'success';
        }

        // if current pageNumber has no data after delete, set the pageNumber to the previous
        if (
          this.graphData.length === 1 &&
          this.graphDataPageConfig.pageNumber > 1
        ) {
          this.graphDataPageConfig.pageNumber =
            this.graphDataPageConfig.pageNumber - 1;
        }
      }

      if (result.data.status !== 200) {
        this.errorInfo.deleteGraphData.code = result.data.status;
        throw new Error(result.data.message);
      }
    } catch (error) {
      this.requestStatus.deleteGraphData = 'failed';
      this.errorInfo.deleteGraphData.message = error.message;
      console.error(error.message);
    }
  });
}

function filterParams(originParams: GraphDataConfig): GraphDataConfig {
  const newParams = {} as GraphDataConfig;

  Object.keys(originParams).forEach((key) => {
    const value = originParams[key];

    if (typeof value !== 'undefined') {
      newParams[key] = originParams[key];
    }
  });

  return newParams;
}

// For DI in subclass
export const GraphManagementStoreInstance = new GraphManagementStore();

export default createContext(GraphManagementStoreInstance);
