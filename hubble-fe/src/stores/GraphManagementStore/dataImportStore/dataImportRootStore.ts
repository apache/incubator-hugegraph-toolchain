import { createContext } from 'react';
import { observable, action, flow, computed } from 'mobx';
import axios, { AxiosResponse } from 'axios';
import { remove } from 'lodash-es';

import { DataMapStore } from './dataMapStore';
import { ServerDataImportStore } from './serverDataImportStore';
import {
  initErrorInfo,
  initRequestStatus
} from '../../factory/dataImportStore/dataImportRootStore';
import { baseUrl, responseData } from '../../types/common';
import {
  FileUploadResult,
  FileUploadTask,
  FileUploadQueue
} from '../../types/GraphManagementStore/dataImportStore';
import {
  VertexType,
  VertexTypeListResponse,
  EdgeType,
  EdgeTypeListResponse
} from '../../types/GraphManagementStore/metadataConfigsStore';
import { checkIfLocalNetworkOffline } from '../../utils';

const MAX_CONCURRENT_UPLOAD = 5;

export class DataImportRootStore {
  dataMapStore: DataMapStore;
  serverDataImportStore: ServerDataImportStore;

  constructor() {
    this.dataMapStore = new DataMapStore(this);
    this.serverDataImportStore = new ServerDataImportStore(this);
  }

  @observable currentId: number | null = null;
  @observable currentJobId: number | null = null;
  @observable currentStep = 1;

  @observable requestStatus = initRequestStatus();
  @observable errorInfo = initErrorInfo();

  @observable fileList: File[] = [];
  @observable fileUploadTasks: FileUploadTask[] = [];
  @observable fileUploadQueue: FileUploadQueue[] = [];
  @observable fileRetryUploadList: string[] = [];

  @observable.ref fileInfos: FileUploadResult[] = [];
  @observable.ref vertexTypes: VertexType[] = [];
  @observable.ref edgeTypes: EdgeType[] = [];

  @computed get successFileUploadTaskNames() {
    return this.fileUploadTasks
      .filter(({ status }) => status === 'success')
      .map(({ name }) => name);
  }

  @computed get unsuccessFileUploadTasks() {
    return this.fileUploadTasks.filter(({ status }) => status !== 'success');
  }

  @action
  setCurrentId(id: number) {
    this.currentId = id;
  }

  @action
  setCurrentJobId(id: number) {
    this.currentJobId = id;
  }

  @action
  setCurrentStep(step: number) {
    this.currentStep = step;
  }

  @action
  updateFileList(files: File[]) {
    this.fileList = [...files, ...this.fileList];
  }

  @action
  initFileUploadTask(tasks: FileUploadTask) {
    this.fileUploadTasks = [tasks, ...this.fileUploadTasks];
  }

  @action
  addFileUploadQueue(element: FileUploadQueue) {
    this.fileUploadQueue.push(element);
  }

  @action
  removeFileUploadQueue(fileName: string) {
    remove(this.fileUploadQueue, ({ fileName: name }) => name === fileName);
  }

  @action
  mutateFileUploadQueue<T extends keyof FileUploadQueue>(
    key: T,
    value: FileUploadQueue[T],
    index: number
  ) {
    this.fileUploadQueue[index][key] = value;
  }

  @action
  mutateFileUploadTasks<T extends keyof FileUploadTask>(
    key: T,
    value: FileUploadTask[T],
    fileName: string
  ) {
    const fileUploadTask = this.fileUploadTasks.find(
      ({ name }) => name === fileName
    )!;

    fileUploadTask[key] = value;
  }

  @action
  removeFileUploadTasks(fileName: string) {
    remove(this.fileUploadTasks, ({ name }) => fileName === name);
  }

  @action
  addRetryFileUploadQueue(fileName: string) {
    this.fileRetryUploadList.push(fileName);
  }

  @action
  pullRetryFileUploadQueue() {
    return this.fileRetryUploadList.shift();
  }

  @action
  resetAllFileInfos() {
    this.fileList = [];
    this.fileUploadTasks = [];
    this.fileUploadQueue = [];
    this.fileInfos = [];
  }

  @action
  dispose() {
    this.currentId = null;
    this.currentJobId = null;
    this.currentStep = 1;
    this.vertexTypes = [];
    this.edgeTypes = [];

    this.resetAllFileInfos();
    this.requestStatus = initRequestStatus();
    this.errorInfo = initErrorInfo();
  }

  uploadFiles = flow(function* uploadFiles(
    this: DataImportRootStore,
    file: File
  ) {
    this.requestStatus.uploadFiles = 'pending';
    const formData = new FormData();
    formData.append('file', file);

    try {
      const result = yield axios.post<responseData<FileUploadResult>>(
        `${baseUrl}/${this.currentId}/job-manager/${this.currentJobId}/upload-file?total=1&index=1`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        }
      );

      if (result.data.status !== 200) {
        this.errorInfo.uploadFiles.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.fileInfos.push(result.data.data);
      this.requestStatus.uploadFiles = 'success';
    } catch (error) {
      this.requestStatus.uploadFiles = 'failed';
      this.errorInfo.uploadFiles.message = error.message;
      console.error(error.message);
    }
  });

  uploadFiles2 = flow(function* uploadFiles2(
    this: DataImportRootStore,
    {
      fileName,
      fileChunkList,
      fileChunkTotal
    }: {
      fileName: string;
      fileChunkList: {
        chunkIndex: number;
        chunk: Blob;
      };
      fileChunkTotal: number;
    }
  ) {
    this.requestStatus.uploadFiles = 'pending';
    const formData = new FormData();
    formData.append('file', fileChunkList.chunk);

    try {
      const result: AxiosResponse<responseData<
        FileUploadResult
      >> = yield axios.post<responseData<FileUploadResult>>(
        `${baseUrl}/${this.currentId}/job-manager/${this.currentJobId}/upload-file?total=${fileChunkTotal}&index=${fileChunkList.chunkIndex}&name=${fileName}`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        }
      );

      if (result.data.status !== 200) {
        this.errorInfo.uploadFiles.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.fileInfos.push(result.data.data);
      this.requestStatus.uploadFiles = 'success';
      return result.data.data;
    } catch (error) {
      this.requestStatus.uploadFiles = 'failed';
      this.errorInfo.uploadFiles.message = error.message;
      console.error(error.message);
    }
  });

  deleteFiles = flow(function* deleteFiles(
    this: DataImportRootStore,
    fileNames: string[]
  ) {
    this.requestStatus.deleteFiles = 'pending';

    try {
      const result = yield axios.delete(
        `${baseUrl}/${this.currentId}/job-manager/${
          this.currentJobId
        }/upload-file?${fileNames
          .map((fileName) => `names=${fileName}`)
          .join('&')}`
      );

      if (result.data.status !== 200) {
        this.errorInfo.deleteFiles.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.requestStatus.deleteFiles = 'success';
    } catch (error) {
      this.requestStatus.deleteFiles = 'failed';
      this.errorInfo.deleteFiles.message = error.message;
      console.error(error.message);
    }
  });

  fetchVertexTypeList = flow(function* fetchVertexTypeList(
    this: DataImportRootStore
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

  fetchEdgeTypeList = flow(function* fetchEdgeTypeList(
    this: DataImportRootStore
  ) {
    this.requestStatus.fetchEdgeTypeList = 'pending';

    try {
      const result: AxiosResponse<responseData<
        EdgeTypeListResponse
      >> = yield axios
        .get<responseData<EdgeTypeListResponse>>(
          `${baseUrl}/${this.currentId}/schema/edgelabels`,
          {
            params: {
              page_size: -1
            }
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.fetchEdgeTypeList.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.edgeTypes = result.data.data.records;
      this.requestStatus.fetchEdgeTypeList = 'success';
    } catch (error) {
      this.requestStatus.fetchEdgeTypeList = 'failed';
      this.errorInfo.fetchEdgeTypeList.message = error.message;
    }
  });
}

export default createContext(new DataImportRootStore());
