import { observable, action, flow, computed } from 'mobx';
import axios, { AxiosResponse } from 'axios';
import { size } from 'lodash-es';
import isInt from 'validator/lib/isInt';

import { DataImportRootStore } from './dataImportRootStore';
import i18next from '../../../i18n';
import { baseUrl, responseData } from '../../types/common';
import {
  LoadParameter,
  ImportTasks
} from '../../types/GraphManagementStore/dataImportStore';
import {
  initRequestStatus,
  initErrorInfo,
  createValidateFileInfoErrorMessage
} from '../../../stores/factory/dataImportStore/serverDataImportStore';
import { checkIfLocalNetworkOffline } from '../../utils';

export class ServerDataImportStore {
  dataImportRootStore: DataImportRootStore;

  constructor(dataImportRootStore: DataImportRootStore) {
    this.dataImportRootStore = dataImportRootStore;
  }

  @observable requestStatus = initRequestStatus();
  @observable errorInfo = initErrorInfo();

  @observable isExpandImportConfig = true;
  @observable isImporting = false;
  @observable isImportFinished = false;
  @observable importConfigs: LoadParameter | null = null;
  @observable
  validateImportConfigErrorMessage = createValidateFileInfoErrorMessage();

  // @observable importTasks: ImportTasks[] = [
  //   {
  //     id: 1,
  //     conn_id: 1,
  //     file_id: 1,
  //     vertices: ['name', 'person', 'age'],
  //     edges: ['name', 'person', 'age'],
  //     load_rate: 0.0,
  //     load_progress: 100,
  //     file_total_lines: 7,
  //     file_read_lines: 7,
  //     status: 'SUCCEED',
  //     duration: '0s'
  //   },
  //   {
  //     id: 2,
  //     conn_id: 2,
  //     file_id: 2,
  //     vertices: ['name', 'person', 'age'],
  //     edges: ['name', 'person', 'age'],
  //     load_rate: 0.0,
  //     load_progress: 70,
  //     file_total_lines: 7,
  //     file_read_lines: 7,
  //     status: 'FAILED',
  //     duration: '0s'
  //   },
  //   {
  //     id: 3,
  //     conn_id: 3,
  //     file_id: 3,
  //     vertices: ['name', 'person', 'age'],
  //     edges: ['name', 'person', 'age'],
  //     load_rate: 0.0,
  //     load_progress: 30,
  //     file_total_lines: 7,
  //     file_read_lines: 7,
  //     status: 'STOPPED',
  //     duration: '0s'
  //   },
  //   {
  //     id: 4,
  //     conn_id: 4,
  //     file_id: 4,
  //     vertices: ['name', 'person', 'age'],
  //     edges: ['name', 'person', 'age'],
  //     load_rate: 0.0,
  //     load_progress: 50,
  //     file_total_lines: 7,
  //     file_read_lines: 7,
  //     status: 'PAUSED',
  //     duration: '0s'
  //   },
  //   {
  //     id: 5,
  //     conn_id: 5,
  //     file_id: 5,
  //     vertices: ['name', 'person', 'age'],
  //     edges: ['name', 'person', 'age'],
  //     load_rate: 0.0,
  //     load_progress: 20,
  //     file_total_lines: 7,
  //     file_read_lines: 7,
  //     status: 'RUNNING',
  //     duration: '0s'
  //   }
  // ];

  @observable importTasks: ImportTasks[] = [];
  @observable.ref fileImportTaskIds: number[] = [];
  @observable errorLogs = '';

  @computed get isServerStartImport() {
    return this.requestStatus.startImport !== 'standby';
  }

  @computed get successImportFileStatusNumber() {
    return size(this.importTasks.filter(({ status }) => status === 'SUCCEED'));
  }

  @computed get pausedImportFileNumber() {
    return size(this.importTasks.filter(({ status }) => status === 'PAUSED'));
  }

  @computed get abortImportFileNumber() {
    return size(this.importTasks.filter(({ status }) => status === 'STOPPED'));
  }

  @action
  switchExpandImportConfig(flag: boolean) {
    this.isExpandImportConfig = flag;
  }

  @action
  switchImporting(flag: boolean) {
    this.isImporting = flag;
  }

  @action
  switchImportFinished(flag: boolean) {
    this.isImportFinished = flag;
  }

  @action
  switchFetchImportStatus(status: string) {
    this.requestStatus.fetchImportTasks = status;
  }

  @action
  syncImportConfigs(configs: LoadParameter) {
    this.importConfigs = configs;
  }

  @action
  mutateImportConfigs<T extends keyof LoadParameter>(
    key: T,
    value: LoadParameter[T]
  ) {
    this.importConfigs![key] = value;
  }

  @action
  validateImportConfigs(key: keyof LoadParameter) {
    if (key !== 'check_vertex') {
      if (this.importConfigs![key] === '') {
        this.validateImportConfigErrorMessage[key] = i18next.t(
          'server-data-import.validator.no-empty'
        );

        return;
      } else if (!isInt(String(this.importConfigs![key]), { min: 0 })) {
        this.validateImportConfigErrorMessage[key] = i18next.t(
          'server-data-import.validator.need-positive-integer'
        );

        return;
      }

      this.validateImportConfigErrorMessage[key] = '';
    }
  }

  @action
  resetValidateImportConfigs() {
    this.validateImportConfigErrorMessage = createValidateFileInfoErrorMessage();
  }

  @action
  dispose() {
    this.requestStatus = initRequestStatus();
    this.errorInfo = initErrorInfo();
    this.errorLogs = '';
    this.isExpandImportConfig = true;
    this.isImporting = false;
    this.isImportFinished = false;
    this.importConfigs = null;
    this.importTasks = [];
    this.fileImportTaskIds = [];
    this.validateImportConfigErrorMessage = createValidateFileInfoErrorMessage();
  }

  fetchImportTasks = flow(function* fetchImportTasks(
    this: ServerDataImportStore,
    taskIds: number[]
  ) {
    this.requestStatus.fetchImportTasks = 'pending';

    try {
      const result: AxiosResponse<responseData<
        ImportTasks[]
      >> = yield axios
        .get<responseData<ImportTasks[]>>(
          `${baseUrl}/${
            this.dataImportRootStore.currentId
          }/load-tasks/ids?${taskIds.map((id) => 'task_ids=' + id).join('&')}`
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.fetchImportTasks.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.importTasks = result.data.data;

      // for (const task of this.importTasks) {
      //   if (task.status === 'RUNNING') {
      //     this.fetchImportTasks();
      //     break;
      //   }
      // }

      if (!this.importTasks.some(({ status }) => status === 'RUNNING')) {
        // this.requestStatus.fetchImportTasks = 'success';
        // this.switchFetchImportStatus('standby');
        this.switchFetchImportStatus('pending');
        this.switchImporting(false);
        this.switchImportFinished(true);
      }

      this.requestStatus.fetchImportTasks = 'success';
    } catch (error) {
      this.requestStatus.fetchImportTasks = 'failed';
      this.errorInfo.fetchImportTasks.message = error.message;
      console.error(error.message);
    }
  });

  startImport = flow(function* startImport(
    this: ServerDataImportStore,
    fileIds: number[]
  ) {
    this.requestStatus.startImport = 'pending';

    try {
      const result: AxiosResponse<responseData<
        ImportTasks[]
      >> = yield axios
        .post<responseData<ImportTasks[]>>(
          `${baseUrl}/${
            this.dataImportRootStore.currentId
          }/load-tasks/start?${fileIds
            .map((id) => 'file_mapping_ids=' + id)
            .join('&')}`,
          {}
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.startImport.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.fileImportTaskIds = result.data.data.map(({ id }) => id);
      this.requestStatus.startImport = 'success';
    } catch (error) {
      this.requestStatus.startImport = 'failed';
      this.errorInfo.startImport.message = error.message;
      console.error(error.message);
    }
  });

  pauseImport = flow(function* pauseImport(
    this: ServerDataImportStore,
    taskId: number
  ) {
    this.requestStatus.pauseImport = 'pending';

    try {
      const result: AxiosResponse<responseData<ImportTasks>> = yield axios
        .post<responseData<ImportTasks>>(
          `${baseUrl}/${this.dataImportRootStore.currentId}/load-tasks/pause`,
          {},
          {
            params: {
              task_id: taskId
            }
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.pauseImport.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.requestStatus.pauseImport = 'success';
    } catch (error) {
      this.requestStatus.pauseImport = 'failed';
      this.errorInfo.pauseImport.message = error.message;
      console.error(error.message);
    }
  });

  resumeImport = flow(function* resumeImport(
    this: ServerDataImportStore,
    taskId: number
  ) {
    this.requestStatus.resumeImport = 'pending';

    try {
      const result: AxiosResponse<responseData<ImportTasks>> = yield axios
        .post<responseData<ImportTasks>>(
          `${baseUrl}/${this.dataImportRootStore.currentId}/load-tasks/resume`,
          {},
          {
            params: {
              task_id: taskId
            }
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.resumeImport.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.requestStatus.resumeImport = 'success';
    } catch (error) {
      this.requestStatus.resumeImport = 'failed';
      this.errorInfo.resumeImport.message = error.message;
      console.error(error.message);
    }
  });

  abortImport = flow(function* abortImport(
    this: ServerDataImportStore,
    taskId: number
  ) {
    this.requestStatus.abortImport = 'pending';

    try {
      const result: AxiosResponse<responseData<ImportTasks>> = yield axios
        .post<responseData<ImportTasks>>(
          `${baseUrl}/${this.dataImportRootStore.currentId}/load-tasks/stop`,
          {},
          {
            params: {
              task_id: taskId
            }
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.abortImport.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.requestStatus.abortImport = 'success';
    } catch (error) {
      this.requestStatus.abortImport = 'failed';
      this.errorInfo.abortImport.message = error.message;
      console.error(error.message);
    }
  });

  retryImport = flow(function* retryImport(
    this: ServerDataImportStore,
    taskId: number
  ) {
    this.requestStatus.retryImport = 'pending';

    try {
      const result: AxiosResponse<responseData<ImportTasks>> = yield axios
        .post<responseData<ImportTasks>>(
          `${baseUrl}/${this.dataImportRootStore.currentId}/load-tasks/retry`,
          {},
          {
            params: {
              task_id: taskId
            }
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.retryImport.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.requestStatus.retryImport = 'success';
    } catch (error) {
      this.requestStatus.retryImport = 'failed';
      this.errorInfo.retryImport.message = error.message;
      console.error(error.message);
    }
  });

  deleteTaskImport = flow(function* deleteTaskImport(
    this: ServerDataImportStore,
    taskId: number
  ) {
    this.requestStatus.deleteTaskImport = 'pending';

    try {
      const result: AxiosResponse<responseData<null>> = yield axios
        .delete<responseData<null>>(
          `${baseUrl}/${this.dataImportRootStore.currentId}/load-tasks/${taskId}`
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.deleteTaskImport.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.requestStatus.deleteTaskImport = 'success';
    } catch (error) {
      this.requestStatus.deleteTaskImport = 'failed';
      this.errorInfo.deleteTaskImport.message = error.message;
      console.error(error.message);
    }
  });

  checkErrorLogs = flow(function* checkErrorLogs(
    this: ServerDataImportStore,
    taskId: number,
    id?: number
  ) {
    this.requestStatus.checkErrorLogs = 'pending';

    try {
      const result: AxiosResponse<responseData<string>> = yield axios
        .get<responseData<string>>(
          `${baseUrl}/${
            id || this.dataImportRootStore.currentId
          }/load-tasks/${taskId}/reason`
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.checkErrorLogs.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.errorLogs = result.data.data;
      this.requestStatus.checkErrorLogs = 'success';
    } catch (error) {
      this.requestStatus.checkErrorLogs = 'failed';
      this.errorInfo.checkErrorLogs.message = error.message;
      console.error(error.message);
    }
  });
}
