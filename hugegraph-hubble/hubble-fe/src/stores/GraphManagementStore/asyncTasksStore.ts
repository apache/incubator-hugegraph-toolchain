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
import { observable, action, flow } from 'mobx';
import axios, { AxiosResponse } from 'axios';

import { initErrorInfo, initRequestStatus } from '../factory/asyncTasksStore';
import { checkIfLocalNetworkOffline } from '../utils';

import { baseUrl, responseData } from '../types/common';
import type {
  AsyncTask,
  AsyncTaskListResponse
} from '../types/GraphManagementStore/asyncTasksStore';

export class AsyncTasksStore {
  @observable requestStatus = initRequestStatus();
  @observable errorInfo = initErrorInfo();

  @observable currentId: number | null = null;
  @observable searchWords = '';
  @observable abortingId = NaN;

  @observable.shallow asyncTasksPageConfig = {
    pageNumber: 1,
    pageTotal: 0,
    sort: ''
  };

  // searched results rather than initial fetched result
  @observable.shallow isSearched = {
    status: false,
    value: ''
  };

  @observable.shallow filterOptions = {
    type: '',
    status: ''
  };

  @observable.ref asyncTaskList: AsyncTask[] = [];
  @observable.ref singleAsyncTask: AsyncTask | null = null;

  @action
  setCurrentId(id: number) {
    this.currentId = id;
  }

  @action
  setAbortingId(id: number) {
    this.abortingId = id;
  }

  @action
  mutateSearchWords(word: string) {
    this.searchWords = word;
  }

  @action
  mutateFilterOptions(category: 'type' | 'status', value: string) {
    this.filterOptions[category] = value;
  }

  @action
  mutateAsyncTasksPageNumber(pageNumber: number) {
    this.asyncTasksPageConfig.pageNumber = pageNumber;
  }

  @action
  switchSearchedStatus(isSearched: boolean) {
    this.isSearched.status = isSearched;

    isSearched
      ? (this.isSearched.value = this.searchWords)
      : (this.isSearched.value = '');
  }

  @action
  dispose() {
    this.requestStatus = initRequestStatus();
    this.errorInfo = initErrorInfo();
    this.currentId = null;
    this.searchWords = '';
    this.abortingId = NaN;
    this.asyncTasksPageConfig = {
      pageNumber: 1,
      pageTotal: 0,
      sort: ''
    };
    this.isSearched = {
      status: false,
      value: ''
    };
    this.filterOptions = {
      type: '',
      status: ''
    };
    this.asyncTaskList = [];
    this.singleAsyncTask = null;
  }

  fetchAsyncTaskList = flow(function* fetchAsyncTaskList(
    this: AsyncTasksStore
  ) {
    this.requestStatus.fetchAsyncTaskList = 'pending';

    try {
      const result: AxiosResponse<responseData<
        AsyncTaskListResponse
      >> = yield axios
        .get<responseData<AsyncTaskListResponse>>(
          `${baseUrl}/${this.currentId}/async-tasks?page_no=${
            this.asyncTasksPageConfig.pageNumber
          }&page_size=10&type=${
            this.filterOptions.type
          }&status=${this.filterOptions.status.toUpperCase()}` +
            (this.isSearched.status && this.searchWords !== ''
              ? `&content=${this.searchWords}`
              : '')
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.fetchAsyncTaskList.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.asyncTaskList = result.data.data.records;
      this.asyncTasksPageConfig.pageTotal = result.data.data.total;
      this.requestStatus.fetchAsyncTaskList = 'success';
    } catch (error) {
      this.requestStatus.fetchAsyncTaskList = 'failed';
      this.errorInfo.fetchAsyncTaskList.message = error.message;
    }
  });

  fetchAsyncTask = flow(function* fetchAsyncTask(
    this: AsyncTasksStore,
    id: number
  ) {
    this.requestStatus.fetchAsyncTask = 'pending';

    try {
      const result: AxiosResponse<responseData<AsyncTask>> = yield axios
        .get<responseData<AsyncTaskListResponse>>(
          `${baseUrl}/${this.currentId}/async-tasks/${id}`
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.fetchAsyncTask.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.singleAsyncTask = result.data.data;
      this.requestStatus.fetchAsyncTask = 'success';
    } catch (error) {
      this.requestStatus.fetchAsyncTask = 'failed';
      this.errorInfo.fetchAsyncTask.message = error.message;
    }
  });

  deleteAsyncTask = flow(function* deleteAsyncTask(
    this: AsyncTasksStore,
    selectedTaskIds: number[]
  ) {
    this.requestStatus.deleteAsyncTask = 'pending';

    try {
      const result: AxiosResponse<responseData<null>> = yield axios
        .delete<responseData<null>>(
          `${baseUrl}/${this.currentId}/async-tasks?` +
            selectedTaskIds.map((id) => 'ids=' + id).join('&')
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.deleteAsyncTask.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.requestStatus.deleteAsyncTask = 'success';
    } catch (error) {
      this.requestStatus.deleteAsyncTask = 'failed';
      this.errorInfo.deleteAsyncTask.message = error.message;
    }
  });

  abortAsyncTask = flow(function* abortAsyncTask(
    this: AsyncTasksStore,
    id: number
  ) {
    this.requestStatus.abortAsyncTask = 'pending';

    try {
      const result: AxiosResponse<responseData<AsyncTask>> = yield axios
        .post<responseData<AsyncTask>>(
          `${baseUrl}/${this.currentId}/async-tasks/cancel/${id}`
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.abortAsyncTask.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.requestStatus.abortAsyncTask = 'success';
    } catch (error) {
      this.requestStatus.abortAsyncTask = 'failed';
      this.errorInfo.abortAsyncTask.message = error.message;
    }
  });
}

export default createContext(new AsyncTasksStore());
