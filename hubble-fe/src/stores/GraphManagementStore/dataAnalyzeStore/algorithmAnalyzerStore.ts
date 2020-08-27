import { DataAnalyzeStore } from './dataAnalyzeStore';
import { observable, action } from 'mobx';
import { isEmpty } from 'lodash-es';
import isInt from 'validator/lib/isInt';

import {
  initializeRequestStatus,
  initializeErrorInfo,
  createShortestPathDefaultParams,
  createValidateShortestPathParamsErrorMessage
} from '../../factory/dataAnalyzeStore/algorithmStore';
import i18next from '../../../i18n';

import type { ShortestPathAlgorithmParams } from '../../types/GraphManagementStore/dataAnalyzeStore';

export class AlgorithmAnalyzerStore {
  dataAnalyzeStore: DataAnalyzeStore;

  constructor(dataAnalyzeStore: DataAnalyzeStore) {
    this.dataAnalyzeStore = dataAnalyzeStore;
  }

  @observable requestStatus = initializeRequestStatus();
  @observable errorInfo = initializeErrorInfo();

  @observable isCollapse = false;
  @observable currentAlgorithm = '';

  @observable
  shortestPathAlgorithmParams: ShortestPathAlgorithmParams = createShortestPathDefaultParams();

  @observable
  validateShortestPathParamsErrorMessage: ShortestPathAlgorithmParams = createValidateShortestPathParamsErrorMessage();

  @action
  switchCollapse(flag: boolean) {
    this.isCollapse = flag;
  }

  @action
  changeCurrentAlgorithm(algorithm: string) {
    this.currentAlgorithm = algorithm;
  }

  @action
  mutateShortestPathParams<T extends keyof ShortestPathAlgorithmParams>(
    key: T,
    value: ShortestPathAlgorithmParams[T]
  ) {
    this.shortestPathAlgorithmParams[key] = value;
  }

  @action
  validateShortestPathParams<T extends keyof ShortestPathAlgorithmParams>(
    key: T
  ) {
    const value = this.shortestPathAlgorithmParams[key];

    switch (key) {
      case 'source':
      case 'target':
        if (isEmpty(value)) {
          this.validateShortestPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.no-empty'
          );

          return;
        }
        break;
      case 'max_depth':
        if (isEmpty(value)) {
          this.validateShortestPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.no-empty'
          );

          return;
        }

        if (!isInt(value, { min: 1 })) {
          this.validateShortestPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'max_degree':
        if (!isInt(value, { min: 1 })) {
          this.validateShortestPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'skip_degree':
        if (!isInt(value, { min: 0 })) {
          this.validateShortestPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.integer-only'
          );

          return;
        }

        break;
      case 'capacity':
        if (!isInt(value, { min: 1 })) {
          this.validateShortestPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.postive-integer-only'
          );

          return;
        }

        break;
    }

    this.validateShortestPathParamsErrorMessage[key] = '';
  }

  @action
  resetShortestPathParams() {
    this.shortestPathAlgorithmParams = createShortestPathDefaultParams();
  }

  @action
  dispose() {
    this.requestStatus = initializeRequestStatus();
    this.errorInfo = initializeErrorInfo();
    this.currentAlgorithm = '';
    this.shortestPathAlgorithmParams = createShortestPathDefaultParams();
    this.validateShortestPathParamsErrorMessage = createValidateShortestPathParamsErrorMessage();
  }
}
