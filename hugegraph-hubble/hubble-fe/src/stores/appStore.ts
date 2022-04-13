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
