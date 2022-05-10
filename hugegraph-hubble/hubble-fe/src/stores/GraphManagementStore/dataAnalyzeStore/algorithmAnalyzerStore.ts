import { DataAnalyzeStore } from './dataAnalyzeStore';
import { observable, action, computed } from 'mobx';
import { isEmpty, remove, isEqual, isUndefined, flatten } from 'lodash-es';
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
  createValidateNeighborRankErrorMessage,
  createKStepNeighborDefaultParams,
  createValidateKStepNeighborParamsErrorMessage,
  createKHopDefaultParams,
  createValidateKHopParamsErrorMessage,
  createRadiographicInspectionDefaultParams,
  createValidateRadiographicInspectionParamsErrorMessage,
  createSameNeighborDefaultParams,
  createValidateSameNeighborParamsErrorMessage,
  createWeightedShortestPathDefaultParams,
  createValidateWeightedShortestPathParamsErrorMessage,
  createSingleSourceWeightedShortestPathDefaultParams,
  createValidateSingleSourceWeightedShortestPathParamsErrorMessage,
  createJaccardDefaultParams,
  createValidateJaccardParamsErrorMessage,
  createPersonalRankDefaultParams,
  createValidatePersonalRankParamsErrorMessage,
  createCustomPathDefaultParams,
  createValidateCustomPathParamsErrorMessage
} from '../../factory/dataAnalyzeStore/algorithmStore';
import i18next from '../../../i18n';
import { isGtNegativeOneButZero } from '../../utils';

import type { dict } from '../../types/common';
import type {
  ShortestPathAlgorithmParams,
  LoopDetectionParams,
  FocusDetectionParams,
  ShortestPathAllAlgorithmParams,
  AllPathAlgorithmParams,
  ModelSimilarityParams,
  NeighborRankParams,
  NeighborRankRule,
  KStepNeighbor,
  KHop,
  RadiographicInspection,
  SameNeighbor,
  WeightedShortestPath,
  SingleSourceWeightedShortestPath,
  Jaccard,
  PersonalRank,
  CustomPathParams,
  CustomPathRule
} from '../../types/GraphManagementStore/dataAnalyzeStore';
import isFloat from 'validator/lib/isFloat';

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

  @observable
  kStepNeighborParams: KStepNeighbor = createKStepNeighborDefaultParams();

  @observable
  validateKStepNeighborParamsErrorMessage = createValidateKStepNeighborParamsErrorMessage();

  @observable
  kHopParams: KHop = createKHopDefaultParams();

  @observable
  validateKHopParamsErrorMessage = createValidateKHopParamsErrorMessage();

  @observable
  customPathParams: CustomPathParams = createCustomPathDefaultParams();

  @observable
  validateCustomPathParmasErrorMessage = createValidateCustomPathParamsErrorMessage();

  @observable
  radiographicInspectionParams: RadiographicInspection = createRadiographicInspectionDefaultParams();

  @observable
  validateRadiographicInspectionParamsErrorMessage = createValidateRadiographicInspectionParamsErrorMessage();

  @observable
  sameNeighborParams: SameNeighbor = createSameNeighborDefaultParams();

  @observable
  validateSameNeighborParamsErrorMessage: SameNeighbor = createValidateSameNeighborParamsErrorMessage();

  @observable
  weightedShortestPathParams: WeightedShortestPath = createWeightedShortestPathDefaultParams();

  @observable
  validateWeightedShortestPathParamsErrorMessage = createValidateWeightedShortestPathParamsErrorMessage();

  @observable
  singleSourceWeightedShortestPathParams: SingleSourceWeightedShortestPath = createSingleSourceWeightedShortestPathDefaultParams();

  @observable
  validateSingleSourceWeightedShortestPathParamsErrorMessage = createValidateSingleSourceWeightedShortestPathParamsErrorMessage();

  @observable
  jaccardParams: Jaccard = createJaccardDefaultParams();

  @observable
  validateJaccardParamsErrorMessage = createValidateJaccardParamsErrorMessage();

  @observable
  personalRankParams: PersonalRank = createPersonalRankDefaultParams();

  @observable
  validatePersonalRankErrorMessage = createValidatePersonalRankParamsErrorMessage();

  @computed get allPropertyIndexProperty() {
    return flatten(
      this.dataAnalyzeStore.propertyIndexes.map(({ fields }) => fields)
    );
  }

  @computed get currentAlgorithmParams() {
    switch (this.currentAlgorithm) {
      case 'loop-detection':
        return this.loopDetectionParams;
      case 'focus-detection':
        return this.focusDetectionParams;
      case 'shortest-path':
        return this.shortestPathAlgorithmParams;
      case 'shortest-path-all':
        return this.shortestPathAllParams;
      case 'all-path':
        return this.allPathParams;
      case 'model-similarity':
        return this.modelSimilarityParams;
      case 'neighbor-rank':
        return this.neighborRankParams;
      case 'k-step-neighbor':
        return this.kStepNeighborParams;
      case 'k-hop':
        return this.kHopParams;
      case 'custom-path':
        return this.customPathParams;
      case 'radiographic-inspection':
        return this.loopDetectionParams;
      case 'same-neighbor':
        return this.sameNeighborParams;
      case 'weighted-shortest-path':
        return this.weightedShortestPathParams;
      case 'single-source-weighted-shortest-path':
        return this.singleSourceWeightedShortestPathParams;
      case 'jaccard':
        return this.jaccardParams;
      case 'personal-rank':
        return this.personalRankParams;
    }
  }

  @action
  switchCollapse(flag: boolean) {
    this.isCollapse = flag;
  }

  @action
  changeCurrentAlgorithm(algorithm: string) {
    this.currentAlgorithm = algorithm;
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
            'data-analyze.algorithm-forms.loop-detection.validations.no-empty'
          );

          return;
        }
        break;
      case 'max_depth':
        if (isEmpty(value)) {
          this.validateLoopDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.loop-detection.validations.no-empty'
          );

          return;
        }

        if (!isInt(value as string, { min: 1 })) {
          this.validateLoopDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.loop-detection.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'max_degree':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateLoopDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.loop-detection.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'limit':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateLoopDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.loop-detection.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'capacity':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateLoopDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.loop-detection.validations.positive-integer-or-negative-one-only'
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
            'data-analyze.algorithm-forms.focus-detection.validations.no-empty'
          );

          return;
        }
        break;
      case 'max_depth':
        if (isEmpty(value)) {
          this.validateFocusDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.focus-detection.validations.no-empty'
          );

          return;
        }

        if (!isInt(value, { min: 1 })) {
          this.validateFocusDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.focus-detection.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'max_degree':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateFocusDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.focus-detection.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'limit':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateFocusDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.focus-detection.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'capacity':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateFocusDetectionParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.focus-detection.validations.positive-integer-or-negative-one-only'
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
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateShortestPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'skip_degree':
        if (value !== '' && !isInt(value, { min: 0 })) {
          this.validateShortestPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.integer-only'
          );

          return;
        }

        break;
      case 'capacity':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateShortestPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.positive-integer-or-negative-one-only'
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
            'data-analyze.algorithm-forms.shortest-path-all.validations.no-empty'
          );

          return;
        }
        break;
      case 'target':
        if (isEmpty(value)) {
          this.validateShortestPathAllParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path-all.validations.no-empty'
          );

          return;
        }
        break;
      case 'max_depth':
        if (isEmpty(value)) {
          this.validateShortestPathAllParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path-all.validations.no-empty'
          );

          return;
        }

        if (!isInt(value, { min: 1 })) {
          this.validateShortestPathAllParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path-all.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'max_degree':
        if (!isGtNegativeOneButZero(value)) {
          this.validateShortestPathAllParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path-all.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'capacity':
        if (!isGtNegativeOneButZero(value)) {
          this.validateShortestPathAllParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path-all.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'skip_degree':
        if (value !== '' && !isInt(value, { min: 0 })) {
          this.validateShortestPathAllParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path-all.validations.integer-only'
          );

          return;
        }
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
            'data-analyze.algorithm-forms.all-path.validations.no-empty'
          );

          return;
        }
        break;
      case 'target':
        if (isEmpty(value)) {
          this.validateAllPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.all-path.validations.no-empty'
          );

          return;
        }
        break;
      case 'max_depth':
        if (isEmpty(value)) {
          this.validateAllPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.all-path.validations.no-empty'
          );

          return;
        }

        if (!isInt(value, { min: 1 })) {
          this.validateAllPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.all-path.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'max_degree':
        if (!isGtNegativeOneButZero(value)) {
          this.validateAllPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.all-path.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'capacity':
        if (!isGtNegativeOneButZero(value)) {
          this.validateAllPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'limit':
        if (!isGtNegativeOneButZero(value)) {
          this.validateAllPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.shortest-path.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
    }

    this.validateAllPathParamsErrorMessage[key] = '';
  }

  @action
  resetAllPathParams() {
    this.allPathParams = createAllPathDefaultParams();
    this.validateAllPathParamsErrorMessage = createValidateAllPathParamsErrorMessage();
  }

  @action
  addModelSimilarityVertexProperty() {
    this.modelSimilarityParams.vertexProperty.push(['', '']);
  }

  @action
  editModelSimilarityVertexProperty(
    index: number,
    type: 'key' | 'value',
    value: string
  ) {
    if (type === 'key') {
      this.modelSimilarityParams.vertexProperty[index][0] = value;
    } else {
      this.modelSimilarityParams.vertexProperty[index][1] = value;
    }
  }

  @action
  removeModelSimilarityVertexProperty(propertyIndex: number) {
    remove(
      this.modelSimilarityParams.vertexProperty,
      (_, index) => index === propertyIndex
    );
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
        if (isEmpty(value)) {
          this.validateModelSimilartiyParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.model-similarity.validations.no-empty'
          );

          return;
        }

        break;
      case 'least_neighbor':
        if (isEmpty(value)) {
          this.validateModelSimilartiyParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.model-similarity.validations.no-empty'
          );

          return;
        }

        if (!isInt(value as string, { min: 1 })) {
          this.validateModelSimilartiyParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.model-similarity.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'similarity':
        if (isEmpty(value)) {
          this.validateModelSimilartiyParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.model-similarity.validations.no-empty'
          );

          return;
        }

        if (
          Object.is(Number(value), NaN) ||
          Number(value) > 1 ||
          Number(value) <= 0
        ) {
          this.validateModelSimilartiyParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.model-similarity.validations.similarity'
          );

          return;
        }

        break;
      case 'max_similar':
        if (value !== '' && !isInt(value as string, { min: 0 })) {
          this.validateModelSimilartiyParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.model-similarity.validations.integer-only'
          );

          return;
        }

        break;
      case 'least_similar':
        if (value !== '' && !isInt(value as string, { min: 1 })) {
          this.validateModelSimilartiyParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.model-similarity.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'least_property_number':
        if (
          !isEmpty(this.modelSimilarityParams.property_filter) &&
          isEmpty(value)
        ) {
          this.validateModelSimilartiyParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.model-similarity.validations.no-empty'
          );

          return;
        }

        if (value !== '' && !isInt(value as string, { min: 2 })) {
          this.validateModelSimilartiyParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.model-similarity.validations.integer-gt-1'
          );

          return;
        }

        break;
      case 'max_degree':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateModelSimilartiyParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.model-similarity.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'capacity':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateModelSimilartiyParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.model-similarity.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'limit':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateModelSimilartiyParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.model-similarity.validations.positive-integer-or-negative-one-only'
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
      this.modelSimilarityParams.vertexProperty = [['', '']];
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
      labels: ['__all__'],
      degree: '10000',
      top: '100'
    });

    // add error message together
    this.addValidateNeighborRankRule();
  }

  @action
  addValidateNeighborRankRule() {
    this.validateNeighborRankParamsParamsErrorMessage.steps.push({
      uuid: '',
      direction: '',
      labels: '',
      degree: '',
      top: ''
    });
  }

  @action
  removeNeighborRankRule(ruleIndex: number) {
    remove(this.neighborRankParams.steps, (_, index) => index === ruleIndex);
    // remove error message together
    this.removeValidateNeighborRankRule(ruleIndex);
  }

  @action
  removeValidateNeighborRankRule(ruleIndex: number) {
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
        if (isEmpty(value)) {
          this.validateNeighborRankParamsParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.neighbor-rank.validations.no-empty'
          );

          return;
        }

        if (
          Object.is(Number(value), NaN) ||
          Number(value) > 1 ||
          Number(value) <= 0
        ) {
          this.validateNeighborRankParamsParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.neighbor-rank.validations.range'
          );

          return;
        }

        this.validateNeighborRankParamsParamsErrorMessage.alpha = '';
        break;
      case 'capacity':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateNeighborRankParamsParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.neighbor-rank.validations.positive-integer-or-negative-one-only'
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
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateNeighborRankParamsParamsErrorMessage.steps[ruleIndex][
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.neighbor-rank.validations.positive-integer-or-negative-one-only'
          );

          return;
        }
        break;
      case 'top':
        if (!isEmpty(value) && !isInt(value as string, { min: 0, max: 999 })) {
          this.validateNeighborRankParamsParamsErrorMessage.steps[ruleIndex][
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.neighbor-rank.validations.integer-only-lt-1000'
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
  resetNeighborRankParams() {
    this.neighborRankParams = createNeighborRankDefaultParams();
    this.validateNeighborRankParamsParamsErrorMessage = createValidateNeighborRankErrorMessage();
  }

  @action
  mutateKStepNeighborParams<T extends keyof KStepNeighbor>(
    key: T,
    value: KStepNeighbor[T]
  ) {
    this.kStepNeighborParams[key] = value;
  }

  @action
  validateKStepNeighborParams<T extends keyof KStepNeighbor>(key: T) {
    const value = this.kStepNeighborParams[key];

    switch (key) {
      case 'source':
        if (isEmpty(value)) {
          this.validateKStepNeighborParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.k-step-neighbor.validations.no-empty'
          );

          return;
        }

        break;
      case 'max_depth':
        if (isEmpty(value)) {
          this.validateKStepNeighborParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.k-step-neighbor.validations.no-empty'
          );

          return;
        }

        if (!isInt(value, { min: 1 })) {
          this.validateKStepNeighborParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.k-step-neighbor.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'max_degree':
        if (!isGtNegativeOneButZero(value)) {
          this.validateKStepNeighborParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.k-step-neighbor.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'limit':
        if (!isGtNegativeOneButZero(value)) {
          this.validateKStepNeighborParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.k-step-neighbor.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
    }

    this.validateKStepNeighborParamsErrorMessage[key] = '';
  }

  @action
  resetKStepNeighborParams() {
    this.kStepNeighborParams = createKStepNeighborDefaultParams();
    this.validateKStepNeighborParamsErrorMessage = createValidateKStepNeighborParamsErrorMessage();
  }

  @action
  mutateKHopParams<T extends keyof KHop>(key: T, value: KHop[T]) {
    this.kHopParams[key] = value;
  }

  @action
  validateKHopParams<T extends keyof KHop>(key: T) {
    const value = this.kHopParams[key];

    switch (key) {
      case 'source':
        if (isEmpty(value)) {
          this.validateKHopParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.k-hop.validations.no-empty'
          );

          return;
        }

        break;
      case 'max_depth':
        if (isEmpty(value)) {
          this.validateKHopParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.k-hop.validations.no-empty'
          );

          return;
        }

        if (!isInt(value as string, { min: 1 })) {
          this.validateKHopParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.k-hop.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'max_degree':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateKHopParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.k-hop.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'limit':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateKHopParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.k-hop.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'capacity':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateKHopParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.k-hop.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
    }

    this.validateKHopParamsErrorMessage[key] = '';
  }

  @action
  resetKHopParams() {
    this.kHopParams = createKHopDefaultParams();
    this.validateKHopParamsErrorMessage = createValidateKHopParamsErrorMessage();
  }

  @action
  addCustomPathRule() {
    this.customPathParams.steps.push({
      uuid: v4(),
      direction: 'BOTH',
      labels: this.dataAnalyzeStore.edgeTypes.map(({ name }) => name),
      weight_by: '',
      default_weight: '',
      properties: [['', '']],
      degree: '10000',
      sample: '100'
    });

    // add error message together
    this.validateCustomPathParmasErrorMessage.steps.push({
      uuid: '',
      direction: '',
      labels: '',
      weight_by: '',
      default_weight: '',
      properties: '',
      degree: '',
      sample: ''
    });
  }

  @action
  removeCustomPathRule(ruleIndex: number) {
    remove(this.customPathParams.steps, (_, index) => index === ruleIndex);
    // remove error message together
    remove(
      this.validateCustomPathParmasErrorMessage.steps,
      (_, index) => index === ruleIndex
    );
  }

  @action
  addCustomPathVertexProperty() {
    this.customPathParams.vertexProperty.push(['', '']);
  }

  @action
  removeCustomPathVertexProperty(propertyIndex: number) {
    remove(
      this.customPathParams.vertexProperty,
      (_, index) => index === propertyIndex
    );
  }

  @action
  addCustomPathRuleProperty(ruleIndex: number) {
    this.customPathParams.steps[ruleIndex].properties.push(['', '']);
  }

  @action
  removeCustomPathRuleProperty(ruleIndex: number, propertyIndex: number) {
    remove(
      this.customPathParams.steps[ruleIndex].properties,
      (_, index) => index === propertyIndex
    );
  }

  @action
  mutateCustomPathParams<T extends keyof CustomPathParams>(
    key: T,
    value: CustomPathParams[T]
  ) {
    this.customPathParams[key] = value;
  }

  @action
  mutateCustomPathRuleParams<T extends keyof CustomPathRule>(
    key: T,
    value: CustomPathRule[T],
    index: number
  ) {
    this.customPathParams.steps[index][key] = value;
  }

  @action
  validateCustomPathParams<T extends keyof CustomPathParams>(key: T) {
    const value = this.customPathParams[key];

    switch (key) {
      case 'source':
        if (isEmpty(value)) {
          this.validateCustomPathParmasErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.custom-path.validations.no-empty'
          );

          return;
        }

        this.validateCustomPathParmasErrorMessage.source = '';
        break;
      case 'capacity':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateCustomPathParmasErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.custom-path.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        this.validateCustomPathParmasErrorMessage.capacity = '';
        break;
      case 'limit':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateCustomPathParmasErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.custom-path.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        this.validateCustomPathParmasErrorMessage.capacity = '';
        break;
    }
  }

  @action
  validateCustomPathRules<T extends keyof CustomPathRule>(
    key: T,
    ruleIndex: number
  ) {
    const value = this.customPathParams.steps[ruleIndex][key];

    switch (key) {
      case 'properties':
        if (isEmpty(value)) {
          this.validateCustomPathParmasErrorMessage.steps[ruleIndex][
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.custom-path.validations.no-empty'
          );

          return;
        }

        break;
      case 'degree':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateCustomPathParmasErrorMessage.steps[ruleIndex][
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.neighbor-rank.validations.positive-integer-or-negative-one-only'
          );

          return;
        }
        break;
      case 'sample':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateCustomPathParmasErrorMessage.steps[ruleIndex][
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.neighbor-rank.validations.positive-integer-or-negative-one-only'
          );

          return;
        }
        break;
      case 'default_weight':
        if (isEmpty(value)) {
          this.validateCustomPathParmasErrorMessage.steps[ruleIndex][
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.custom-path.validations.no-empty'
          );

          return;
        }

        if (!isFloat(value as string)) {
          this.validateCustomPathParmasErrorMessage.steps[ruleIndex][
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.custom-path.validations.input-number'
          );

          return;
        }

        break;
      default:
        return;
    }

    this.validateCustomPathParmasErrorMessage.steps[ruleIndex][key] = '';
  }

  @action
  switchCustomPathMethod(method: string) {
    this.customPathParams.method = method;

    if (method === 'id') {
      this.customPathParams.vertexType = '';
      this.customPathParams.vertexProperty = [['', '']];
      this.validateCustomPathParmasErrorMessage.vertexType = '';
      this.validateCustomPathParmasErrorMessage.vertexProperty = '';
    } else {
      this.customPathParams.source = '';
      this.validateCustomPathParmasErrorMessage.source = '';
    }
  }

  @action
  resetCustomPathParams() {
    this.customPathParams = createCustomPathDefaultParams();

    // manually assign step edge values
    this.customPathParams.steps[0].labels = this.dataAnalyzeStore.edgeTypes.map(
      ({ name }) => name
    );
    this.validateCustomPathParmasErrorMessage = createValidateCustomPathParamsErrorMessage();
  }

  @action
  mutateRadiographicInspectionParams<T extends keyof RadiographicInspection>(
    key: T,
    value: RadiographicInspection[T]
  ) {
    this.radiographicInspectionParams[key] = value;
  }

  @action
  validateRadiographicInspectionParams<T extends keyof RadiographicInspection>(
    key: T
  ) {
    const value = this.radiographicInspectionParams[key];

    switch (key) {
      case 'source':
        if (isEmpty(value)) {
          this.validateRadiographicInspectionParamsErrorMessage[
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.radiographic-inspection.validations.no-empty'
          );

          return;
        }

        break;
      case 'max_depth':
        if (isEmpty(value)) {
          this.validateRadiographicInspectionParamsErrorMessage[
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.radiographic-inspection.validations.no-empty'
          );

          return;
        }

        if (!isInt(value, { min: 1 })) {
          this.validateRadiographicInspectionParamsErrorMessage[
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.radiographic-inspection.validations.postive-integer-only'
          );

          return;
        }

        break;
      case 'max_degree':
        if (!isGtNegativeOneButZero(value)) {
          this.validateRadiographicInspectionParamsErrorMessage[
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.radiographic-inspection.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'capacity':
        if (!isGtNegativeOneButZero(value)) {
          this.validateRadiographicInspectionParamsErrorMessage[
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.radiographic-inspection.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'limit':
        if (!isGtNegativeOneButZero(value)) {
          this.validateRadiographicInspectionParamsErrorMessage[
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.radiographic-inspection.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
    }

    this.validateRadiographicInspectionParamsErrorMessage[key] = '';
  }

  @action
  resetRadiographicInspectionParams() {
    this.radiographicInspectionParams = createRadiographicInspectionDefaultParams();
    this.validateRadiographicInspectionParamsErrorMessage = createValidateRadiographicInspectionParamsErrorMessage();
  }

  @action
  mutateSameNeighborParams<T extends keyof SameNeighbor>(
    key: T,
    value: SameNeighbor[T]
  ) {
    this.sameNeighborParams[key] = value;
  }

  @action
  validateSameNeighborParams<T extends keyof SameNeighbor>(key: T) {
    const value = this.sameNeighborParams[key];

    switch (key) {
      case 'vertex':
        if (isEmpty(value)) {
          this.validateSameNeighborParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.same-neighbor.validations.no-empty'
          );

          return;
        }

        if (value === this.sameNeighborParams.other) {
          this.validateSameNeighborParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.same-neighbor.validations.no-same-value-with-other'
          );

          return;
        }

        break;
      case 'other':
        if (isEmpty(value)) {
          this.validateSameNeighborParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.same-neighbor.validations.no-empty'
          );

          return;
        }

        if (value === this.sameNeighborParams.vertex) {
          this.validateSameNeighborParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.same-neighbor.validations.no-same-value-with-vertex'
          );

          return;
        }

        break;
      case 'max_degree':
        if (!isGtNegativeOneButZero(value)) {
          this.validateSameNeighborParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.same-neighbor.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'limit':
        if (!isGtNegativeOneButZero(value)) {
          this.validateSameNeighborParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.same-neighbor.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
    }

    this.validateSameNeighborParamsErrorMessage[key] = '';
  }

  @action
  resetSameNeighborParams() {
    this.sameNeighborParams = createSameNeighborDefaultParams();
    this.validateSameNeighborParamsErrorMessage = createValidateSameNeighborParamsErrorMessage();
  }

  @action
  mutateWeightedShortestPathParams<T extends keyof WeightedShortestPath>(
    key: T,
    value: WeightedShortestPath[T]
  ) {
    this.weightedShortestPathParams[key] = value;
  }

  @action
  validateWeightedShortestPathParams<T extends keyof WeightedShortestPath>(
    key: T
  ) {
    const value = this.weightedShortestPathParams[key];

    switch (key) {
      case 'source':
      case 'target':
        if (isEmpty(value)) {
          this.validateWeightedShortestPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.weighted-shortest-path.validations.no-empty'
          );

          return;
        }

        break;
      case 'max_degree':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateWeightedShortestPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.weighted-shortest-path.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'skip_degree':
        if (value !== '' && !isInt(value as string, { min: 0 })) {
          this.validateWeightedShortestPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.weighted-shortest-path.validations.integer-only'
          );

          return;
        }

        break;
      case 'capacity':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateWeightedShortestPathParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.weighted-shortest-path.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
    }

    this.validateWeightedShortestPathParamsErrorMessage[key] = '';
  }

  @action
  resetWeightedShortestPathParams() {
    this.weightedShortestPathParams = createWeightedShortestPathDefaultParams();
    this.validateWeightedShortestPathParamsErrorMessage = createValidateWeightedShortestPathParamsErrorMessage();
  }

  @action
  mutateSingleSourceWeightedShortestPathParams<
    T extends keyof SingleSourceWeightedShortestPath
  >(key: T, value: SingleSourceWeightedShortestPath[T]) {
    this.singleSourceWeightedShortestPathParams[key] = value;
  }

  @action
  validateSingleSourceWeightedShortestPathParams<
    T extends keyof SingleSourceWeightedShortestPath
  >(key: T) {
    const value = this.singleSourceWeightedShortestPathParams[key];

    switch (key) {
      case 'source':
        if (isEmpty(value)) {
          this.validateSingleSourceWeightedShortestPathParamsErrorMessage[
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.single-source-weighted-shortest-path.validations.no-empty'
          );

          return;
        }

        break;
      case 'max_degree':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateSingleSourceWeightedShortestPathParamsErrorMessage[
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.single-source-weighted-shortest-path.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'skip_degree':
        if (value !== '' && !isInt(value as string, { min: 0 })) {
          this.validateSingleSourceWeightedShortestPathParamsErrorMessage[
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.single-source-weighted-shortest-path.validations.integer-only'
          );

          return;
        }

        break;
      case 'capacity':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateSingleSourceWeightedShortestPathParamsErrorMessage[
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.single-source-weighted-shortest-path.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'limit':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validateSingleSourceWeightedShortestPathParamsErrorMessage[
            key
          ] = i18next.t(
            'data-analyze.algorithm-forms.single-source-weighted-shortest-path.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
    }

    this.validateSingleSourceWeightedShortestPathParamsErrorMessage[key] = '';
  }

  @action
  resetSingleSourceWeightedShortestPathParams() {
    this.singleSourceWeightedShortestPathParams = createSingleSourceWeightedShortestPathDefaultParams();
    this.validateSingleSourceWeightedShortestPathParamsErrorMessage = createValidateSingleSourceWeightedShortestPathParamsErrorMessage();
  }

  @action
  mutateJaccardParams<T extends keyof Jaccard>(key: T, value: Jaccard[T]) {
    this.jaccardParams[key] = value;
  }

  @action
  validateJaccardParams<T extends keyof Jaccard>(key: T) {
    const value = this.jaccardParams[key];

    switch (key) {
      case 'vertex':
        if (isEmpty(value)) {
          this.validateJaccardParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.jaccard.validations.no-empty'
          );

          return;
        }

        if (value === this.jaccardParams.other) {
          this.validateJaccardParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.jaccard.validations.no-same-value-with-other'
          );

          return;
        }

        break;
      case 'other':
        if (isEmpty(value)) {
          this.validateJaccardParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.jaccard.validations.no-empty'
          );

          return;
        }

        if (value === this.jaccardParams.vertex) {
          this.validateJaccardParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.jaccard.validations.no-same-value-with-vertex'
          );

          return;
        }

        break;
      case 'max_degree':
        if (!isGtNegativeOneButZero(value)) {
          this.validateJaccardParamsErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.jaccard.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
    }

    this.validateJaccardParamsErrorMessage[key] = '';
  }

  @action
  resetJaccardParams() {
    this.jaccardParams = createJaccardDefaultParams();
    this.validateJaccardParamsErrorMessage = createValidateJaccardParamsErrorMessage();
  }

  @action
  mutatePersonalRankParams<T extends keyof PersonalRank>(
    key: T,
    value: PersonalRank[T]
  ) {
    this.personalRankParams[key] = value;
  }

  @action
  validatePersonalRankParams<T extends keyof PersonalRank>(key: T) {
    const value = this.personalRankParams[key];

    switch (key) {
      case 'source':
        if (isEmpty(value)) {
          this.validatePersonalRankErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.personal-rank.validations.no-empty'
          );

          return;
        }

        break;
      case 'alpha':
        if (isEmpty(value)) {
          this.validatePersonalRankErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.personal-rank.validations.no-empty'
          );

          return;
        }

        if (
          Object.is(Number(value), NaN) ||
          Number(value) > 1 ||
          Number(value) <= 0
        ) {
          this.validatePersonalRankErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.personal-rank.validations.alpha-range'
          );

          return;
        }

        break;
      case 'max_depth':
        if (isEmpty(value)) {
          this.validatePersonalRankErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.personal-rank.validations.no-empty'
          );

          return;
        }

        if (
          Object.is(Number(value), NaN) ||
          Number(value) > 50 ||
          Number(value) <= 0
        ) {
          this.validatePersonalRankErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.personal-rank.validations.depth-range'
          );

          return;
        }

        break;
      case 'degree':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validatePersonalRankErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.personal-rank.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
      case 'limit':
        if (!isGtNegativeOneButZero(value as string)) {
          this.validatePersonalRankErrorMessage[key] = i18next.t(
            'data-analyze.algorithm-forms.personal-rank.validations.positive-integer-or-negative-one-only'
          );

          return;
        }

        break;
    }

    this.validatePersonalRankErrorMessage[key] = '';
  }

  @action
  resetPersonalRankParams() {
    this.personalRankParams = createPersonalRankDefaultParams();
    this.validatePersonalRankErrorMessage = createValidatePersonalRankParamsErrorMessage();
  }

  @action
  dispose() {
    this.requestStatus = initializeRequestStatus();
    this.errorInfo = initializeErrorInfo();
    this.currentAlgorithm = '';

    this.resetLoopDetectionParams();
    this.resetFocusDetectionParams();
    this.resetShortestPathParams();
    this.resetShortestPathAllParams();
    this.resetAllPathParams();
    this.resetModelSimilarityParams();
    this.resetNeighborRankParams();
    this.resetKStepNeighborParams();
    this.resetKHopParams();
    this.resetCustomPathParams();
    this.resetRadiographicInspectionParams();
    this.resetSameNeighborParams();
    this.resetWeightedShortestPathParams();
    this.resetSingleSourceWeightedShortestPathParams();
    this.resetJaccardParams();
    this.resetPersonalRankParams();
  }
}
