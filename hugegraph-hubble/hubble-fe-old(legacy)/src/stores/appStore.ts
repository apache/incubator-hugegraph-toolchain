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
import { responseData, baseUrl } from './types/common';

export class AppStore {
  @observable user: string = 'Hi, User name';
  @observable currentId: number | null = null;
  @observable currentTab: string = 'graph-management';
  @observable errorMessage = '';

  @observable colorList: string[] = [];

  @observable.shallow requestStatus = {
    fetchColorList: 'pending'
  };

  @action.bound
  setCurrentId(id: number) {
    this.currentId = id;
  }

  @action.bound
  switchCurrentTab(tab: string) {
    this.currentTab = tab;
  }

  @action.bound
  setUser(user: string) {
    this.user = user;
  }

  @action
  dispose() {
    this.user = 'Hi, User name';
    this.currentTab = 'graph-management';
    this.requestStatus = {
      fetchColorList: 'pending'
    };
  }

  fetchColorList = flow(function* fetchColorList(this: AppStore) {
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
}

export default createContext(new AppStore());
