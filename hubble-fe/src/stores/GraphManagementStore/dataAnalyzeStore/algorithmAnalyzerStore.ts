import { DataAnalyzeStore } from './dataAnalyzeStore';
import { observable, action, toJS } from 'mobx';
import { isEmpty, remove, isEqual } from 'lodash-es';
import { v4 } from 'uuid';
import isInt from 'validator/lib/isInt';

import {
  initializeRequestStatus,
  initializeErrorInfo,
  createLoopDetectionDefaultParams,
  createValidateLoopDetectionParamsErrorMessage,
  createFocusDetectionDefaultParams,
  createValidateFocusDetectionParamsErrorMessage,
  createShortestPathDefaultParams,
  createValidateShortestPathParamsErrorMessage,
  createShortestPathAllDefaultParams,
  createValidateShortestPathAllParamsErrorMessage,
  createAllPathDefaultParams,
  createValidateAllPathParamsErrorMessage,
  createModelSimilarityDefaultParams,
  createValidateModelSimilarParamsErrorMessage,
  createNeighborRankDefaultParams,
  createValidateNeighborRankErrorMessage
} from '../../factory/dataAnalyzeStore/algorithmStore';
import i18next from '../../../i18n';

import type { dict } from '../../types/common';
import type {
  ShortestPathAlgorithmParams,
  LoopDetectionParams,
  FocusDetectionParams,
  ShortestPathAllAlgorithmParams,
  AllPathAlgorithmParams,
  ModelSimilarityParams,
  NeighborRankParams,
  NeighborRankRule
} from '../../types/GraphManagementStore/dataAnalyzeStore';
import { isUndefined } from 'util';

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
  loopDetectionParams: LoopDetectionParams = createLoopDetectionDefaultParams();

  @observable
  validateLoopDetectionParamsErrorMessage: any = createValidateLoopDetectionParamsErrorMessage();

  @observable
  focusDetectionParams: FocusDetectionParams = createFocusDetectionDefaultParams();

  @observable
  validateFocusDetectionParamsErrorMessage: any = createValidateFocusDetectionParamsErrorMessage();

  @observable
  shortestPathAlgorithmParams: ShortestPathAlgorithmParams = createShortestPathDefaultParams();

  @observable
  validateShortestPathParamsErrorMessage: ShortestPathAlgorithmParams = createValidateShortestPathParamsErrorMessage();

  @observable
  shortestPathAllParams: ShortestPathAllAlgorithmParams = createShortestPathAllDefaultParams();

  @observable
  validateShortestPathAllParamsErrorMessage: ShortestPathAllAlgorithmParams = createValidateShortestPathAllParamsErrorMessage();

  @observable
  allPathParams: AllPathAlgorithmParams = createAllPathDefaultParams();

  @observable
  validateAllPathParamsErrorMessage: AllPathAlgorithmParams = createValidateAllPathParamsErrorMessage();

  @observable
  modelSimilarityParams: ModelSimilarityParams = createModelSimilarityDefaultParams();

  @observable
  validateModelSimilartiyParamsErrorMessage: dict<
    string
  > = createValidateModelSimilarParamsErrorMessage();

  @observable
  neighborRankParams: NeighborRankParams = createNeighborRankDefaultParams();

  @observable
  validateNeighborRankParamsParamsErrorMessage = createValidateNeighborRankErrorMessage();

  @observable isDuplicateNeighborRankRule = false;
  duplicateNeighborRankRuleSet = new Set<string>();

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
    this.validateShortestPathParamsErrorMessage = createValidateShortestPathParamsErrorMessage();
  }

  @action
  mutateLoopDetectionParams<T extends keyof LoopDetectionParams>(
    key: T,
    value: LoopDetectionParams[T]
  ) {
    this.loopDetectionParams[key] = value;
  }

  @action
  validateLoopDetectionParams<T extends keyof LoopDetectionParams>(key: T) {
    const value = this.loopDetectionParams[key];

    switch (key) {
      case 'source':
        if (isEmpty(value)) {
          this.validateLoopDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.no-empty'
          );

          return;
        }
        break;
      case 'max_depth':
        if (isEmpty(value)) {
          this.validateLoopDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.no-empty'
          );

          return;
        }

        if (!isInt(value as string, { min: 1 })) {
          this.validateLoopDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'max_degree':
        if (!isInt(value as string, { min: 1 })) {
          this.validateLoopDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'limit':
        if (!isInt(value as string, { min: 0 })) {
          this.validateLoopDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.integer-only'
          );

          return;
        }

        break;
      case 'capacity':
        if (!isInt(value as string, { min: 1 })) {
          this.validateLoopDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.postive-integer-only'
          );

          return;
        }

        break;
    }

    this.validateLoopDetectionParamsErrorMessage[key] = '';
  }

  @action
  resetLoopDetectionParams() {
    this.loopDetectionParams = createLoopDetectionDefaultParams();
    this.validateLoopDetectionParamsErrorMessage = createValidateLoopDetectionParamsErrorMessage();
  }

  @action
  mutateFocusDetectionParams<T extends keyof FocusDetectionParams>(
    key: T,
    value: FocusDetectionParams[T]
  ) {
    this.focusDetectionParams[key] = value;
  }

  @action
  validateFocusDetectionParams<T extends keyof FocusDetectionParams>(key: T) {
    const value = this.focusDetectionParams[key];

    switch (key) {
      case 'source':
      case 'target':
        if (isEmpty(value)) {
          this.validateFocusDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.no-empty'
          );

          return;
        }
        break;
      case 'max_depth':
        if (isEmpty(value)) {
          this.validateFocusDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.no-empty'
          );

          return;
        }

        if (!isInt(value, { min: 1 })) {
          this.validateFocusDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'max_degree':
        if (!isInt(value, { min: 1 })) {
          this.validateFocusDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'limit':
        if (!isInt(value, { min: 0 })) {
          this.validateFocusDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.integer-only'
          );

          return;
        }

        break;
      case 'capacity':
        if (!isInt(value, { min: 1 })) {
          this.validateFocusDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.postive-integer-only'
          );

          return;
        }

        break;
    }

    this.validateFocusDetectionParamsErrorMessage[key] = '';
  }

  @action
  resetFocusDetectionParams() {
    this.focusDetectionParams = createFocusDetectionDefaultParams();
    this.validateFocusDetectionParamsErrorMessage = createValidateFocusDetectionParamsErrorMessage();
  }

  @action
  mutateShortestPathAllParams<T extends keyof ShortestPathAllAlgorithmParams>(
    key: T,
    value: ShortestPathAlgorithmParams[T]
  ) {
    this.shortestPathAllParams[key] = value;
  }

  @action
  validateShortestPathAllParams<T extends keyof ShortestPathAllAlgorithmParams>(
    key: T
  ) {
    const value = this.shortestPathAllParams[key];

    switch (key) {
      case 'source':
        if (isEmpty(value)) {
          this.validateShortestPathAllParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.no-empty'
          );

          return;
        }
        break;
      case 'max_depth':
        if (isEmpty(value)) {
          this.validateShortestPathAllParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.no-empty'
          );

          return;
        }

        if (!isInt(value, { min: 1 })) {
          this.validateShortestPathAllParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'max_degree':
        if (!isInt(value, { min: 1 })) {
          this.validateShortestPathAllParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'max_capacity':
        if (!isInt(value, { min: 0 })) {
          this.validateShortestPathAllParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.integer-only'
          );

          return;
        }

        break;
      case 'capacity':
        if (!isInt(value, { min: 1 })) {
          this.validateShortestPathAllParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.postive-integer-only'
          );

          return;
        }

        break;
    }

    this.validateShortestPathAllParamsErrorMessage[key] = '';
  }

  @action
  resetShortestPathAllParams() {
    this.shortestPathAllParams = createShortestPathAllDefaultParams();
    this.validateShortestPathAllParamsErrorMessage = createValidateShortestPathAllParamsErrorMessage();
  }

  @action
  mutateAllPathParams<T extends keyof AllPathAlgorithmParams>(
    key: T,
    value: AllPathAlgorithmParams[T]
  ) {
    this.allPathParams[key] = value;
  }

  @action
  validateAllPathParams<T extends keyof AllPathAlgorithmParams>(key: T) {
    const value = this.allPathParams[key];

    switch (key) {
      case 'source':
        if (isEmpty(value)) {
          this.validateAllPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.no-empty'
          );

          return;
        }
        break;
      case 'max_depth':
        if (isEmpty(value)) {
          this.validateAllPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.no-empty'
          );

          return;
        }

        if (!isInt(value, { min: 1 })) {
          this.validateAllPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'max_degree':
        if (!isInt(value, { min: 1 })) {
          this.validateAllPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'max_capacity':
        if (!isInt(value, { min: 0 })) {
          this.validateAllPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.integer-only'
          );

          return;
        }

        break;
      case 'capacity':
        if (!isInt(value, { min: 1 })) {
          this.validateAllPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.postive-integer-only'
          );

          return;
        }

        break;
    }

    this.validateAllPathParamsErrorMessage[key] = '';
  }

  @action
  resetAllPathParams() {
    this.allPathParams = createValidateAllPathParamsErrorMessage();
    this.validateAllPathParamsErrorMessage = createValidateAllPathParamsErrorMessage();
  }

  @action
  mutateModelSimilarityParams<T extends keyof ModelSimilarityParams>(
    key: T,
    value: ModelSimilarityParams[T]
  ) {
    this.modelSimilarityParams[key] = value;
  }

  @action
  validateModelSimilarityParams<T extends keyof ModelSimilarityParams>(key: T) {
    const value = this.modelSimilarityParams[key];

    switch (key) {
      case 'source':
      case 'least_neighbor':
      case 'similarity':
        if (isEmpty(value)) {
          this.validateModelSimilartiyParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.model-similarity.validations.no-empty'
          );

          return;
        }

        // if (!isInt(value, { min: 1 })) {
        //   this.validateModelSimilartiyParamsErrorMessage[key] = i18next.t(
        //     'data-analyze.algorithm-forms.shortest-path.validations.postive-integer-only'
        //   );

        //   return;
        // }

        break;
      case 'max_similar':
        // case 'least_similar':
        // case 'max_degree':
        // case 'skip_degree':
        // case 'capacity':
        // case 'limit':
        if (!isInt(value as string, { min: 0 })) {
          this.validateModelSimilartiyParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.model-similarity.validations.integer-only'
          );

          return;
        }

        break;
      case 'least_property_number':
        if (value !== '' && !isInt(value as string, { min: 2 })) {
          this.validateModelSimilartiyParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.model-similarity.validations.integer-gt-1'
          );

          return;
        }

        break;
    }

    this.validateModelSimilartiyParamsErrorMessage[key] = '';
  }

  @action
  resetModelSimilarityParams() {
    this.modelSimilarityParams = createModelSimilarityDefaultParams();
    this.validateModelSimilartiyParamsErrorMessage = createValidateModelSimilarParamsErrorMessage();
  }

  @action
  switchModelSimilarityMethod(method: string) {
    this.modelSimilarityParams.method = method;

    if (method === 'id') {
      this.modelSimilarityParams.vertexType = '';
      this.modelSimilarityParams.vertexProperty = [];
      this.validateModelSimilartiyParamsErrorMessage.vertexType = '';
      this.validateModelSimilartiyParamsErrorMessage.vertexProperty = '';
    } else {
      this.modelSimilarityParams.source = '';
      this.validateModelSimilartiyParamsErrorMessage.source = '';
    }
  }

  @action
  addNeighborRankRule() {
    this.neighborRankParams.steps.push({
      uuid: v4(),
      direction: 'BOTH',
      label: '__all__',
      degree: '10000',
      top: '100'
    });

    // add error message together
    this.validateNeighborRankParamsParamsErrorMessage.steps.push({
      uuid: '',
      direction: '',
      label: '',
      degree: '',
      top: ''
    });
  }

  @action
  removeNeighborRankRule(ruleIndex: number) {
    remove(this.neighborRankParams.steps, (_, index) => index === ruleIndex);
    // remove error message together
    remove(
      this.validateNeighborRankParamsParamsErrorMessage.steps,
      (_, index) => index === ruleIndex
    );
  }

  @action
  mutateNeighborRankParams<T extends keyof NeighborRankParams>(
    key: T,
    value: NeighborRankParams[T]
  ) {
    this.neighborRankParams[key] = value;
  }

  @action
  mutateNeighborRankRuleParams<T extends keyof NeighborRankRule>(
    key: T,
    value: NeighborRankRule[T],
    index: number
  ) {
    this.neighborRankParams.steps[index][key] = value;
  }

  @action
  validateNeighborRankParams<T extends keyof NeighborRankParams>(key: T) {
    const value = this.neighborRankParams[key];

    switch (key) {
      case 'source':
        if (isEmpty(value)) {
          this.validateNeighborRankParamsParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.neighbor-rank.validations.no-empty'
          );

          return;
        }

        this.validateNeighborRankParamsParamsErrorMessage.source = '';
        break;
      case 'alpha':
        if (Number(value) > 1 && Number(value) <= 0) {
          this.validateNeighborRankParamsParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.neighbor-rank.validations.no-empty'
          );

          return;
        }

        this.validateNeighborRankParamsParamsErrorMessage.alpha = '';
        break;
      case 'capacity':
        if (!isEmpty(value) && !isInt(value as string, { min: 0 })) {
          this.validateNeighborRankParamsParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.neighbor-rank.validations.integer-only'
          );

          return;
        }

        this.validateNeighborRankParamsParamsErrorMessage.capacity = '';
        break;
    }
  }

  @action
  validateNeighborRankRules<T extends keyof NeighborRankRule>(
    key: T,
    ruleIndex: number
  ) {
    const value = this.neighborRankParams.steps[ruleIndex][key];

    switch (key) {
      case 'degree':
      case 'top':
        if (!isEmpty(value) && !isInt(value as string, { min: 0 })) {
          this.validateNeighborRankParamsParamsErrorMessage.steps[ruleIndex][
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.neighbor-rank.validations.integer-only'
          );

          return;
        }
        break;
      default:
        return;
    }

    this.validateNeighborRankParamsParamsErrorMessage.steps[ruleIndex][key] =
      '';
  }

  @action
  validateDuplicateNeighborRankRules(uuid: string) {
    // for (let index = 0; index < this.neighborRankParams.steps.length; index++) {
    //   if (index !== ruleIndex) {
    //     if (
    //       isEqual(
    //         this.neighborRankParams.steps[index],
    //         this.neighborRankParams.steps[ruleIndex]
    //       )
    //     ) {
    //       this.duplicateNeighborRankRuleSet.add(ruleIndex);
    //       return;
    //     }
    //   }
    // }
    const currentStep = this.neighborRankParams.steps.find(
      ({ uuid: currentUUID }) => currentUUID === uuid
    );

    for (const step of this.neighborRankParams.steps) {
      if (step.uuid !== uuid && !isUndefined(currentStep)) {
        // need toJS util here since there will not be converted to object
        // console.log('wtf is currentstep: ', { ...currentStep, id: '' });
        // console.log('wtf is step: ', { ...step, id: '' });
        // if (isEqual(toJS(currentStep), toJS(step))) {
        if (isEqual({ ...currentStep, uuid: '' }, { ...step, uuid: '' })) {
          this.duplicateNeighborRankRuleSet.add(uuid);
          return;
        }
      }
    }

    this.duplicateNeighborRankRuleSet.delete(uuid);

    if (this.duplicateNeighborRankRuleSet.size !== 0) {
      this.duplicateNeighborRankRuleSet.forEach((uuid) => {
        this.validateDuplicateNeighborRankRules(uuid);
      });
    }

    // const arr: number[][] = [];

    // const keys: (keyof NeighborRankRule)[] = [
    //   'direction',
    //   'label',
    //   'degree',
    //   'top'
    // ];

    // keys.forEach((key) => {
    //   this.neighborRankParams.steps.forEach((step, stepIndex) => {
    //     const ruleValue = step[key];
    //     const index = arr.findIndex(
    //       (value) =>
    //         value.length !== 0 &&
    //         this.neighborRankParams.steps[value[0]][key] === ruleValue
    //     );

    //     if (index !== -1) {
    //       arr.push([stepIndex]);
    //     } else {
    //       arr[index].push(stepIndex);
    //     }
    //   });
    // });
  }

  @action
  resetNeighborRankParams() {
    this.neighborRankParams = createNeighborRankDefaultParams();
    this.validateNeighborRankParamsParamsErrorMessage = createValidateNeighborRankErrorMessage();
  }

  @action
  dispose() {
    this.requestStatus = initializeRequestStatus();
    this.errorInfo = initializeErrorInfo();
    this.currentAlgorithm = '';
    this.shortestPathAlgorithmParams = createShortestPathDefaultParams();
    this.validateShortestPathParamsErrorMessage = createValidateShortestPathParamsErrorMessage();

    this.loopDetectionParams = createLoopDetectionDefaultParams();
    this.validateLoopDetectionParamsErrorMessage = createValidateLoopDetectionParamsErrorMessage();

    this.focusDetectionParams = createFocusDetectionDefaultParams();
    this.validateFocusDetectionParamsErrorMessage = createValidateFocusDetectionParamsErrorMessage();

    this.shortestPathAllParams = createShortestPathAllDefaultParams();
    this.validateShortestPathAllParamsErrorMessage = createValidateShortestPathAllParamsErrorMessage();

    this.allPathParams = createAllPathDefaultParams();
    this.validateAllPathParamsErrorMessage = createValidateAllPathParamsErrorMessage();

    this.modelSimilarityParams = createModelSimilarityDefaultParams();
    this.validateModelSimilartiyParamsErrorMessage = createValidateModelSimilarParamsErrorMessage();

    this.neighborRankParams = createNeighborRankDefaultParams();
    this.validateNeighborRankParamsParamsErrorMessage = createValidateNeighborRankErrorMessage();
  }
}
