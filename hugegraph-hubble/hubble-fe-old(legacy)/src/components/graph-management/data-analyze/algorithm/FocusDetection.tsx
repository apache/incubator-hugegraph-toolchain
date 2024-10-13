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

import React, { useContext, createContext } from 'react';
import { observer } from 'mobx-react';
import { Button, Radio, Input, Select, Switch } from 'hubble-ui';
import { useTranslation } from 'react-i18next';

import { styles } from '../QueryAndAlgorithmLibrary';
import { Tooltip as CustomTooltip } from '../../../common';
import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';
import { GraphManagementStoreContext } from '../../../../stores';

import QuestionMarkIcon from '../../../../assets/imgs/ic_question_mark.svg';
import { calcAlgorithmFormWidth } from '../../../../utils';

const FocusDetection = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const dataAnalyzeStore = useContext(DataAnalyzeStore);
  const algorithmAnalyzerStore = dataAnalyzeStore.algorithmAnalyzerStore;
  const { t } = useTranslation();

  const formWidth = calcAlgorithmFormWidth(
    graphManagementStore.isExpanded,
    340,
    400
  );

  const isValidExec =
    Object.values(
      algorithmAnalyzerStore.validateFocusDetectionParamsErrorMessage
    ).every((value) => value === '') &&
    algorithmAnalyzerStore.focusDetectionParams.source !== '' &&
    algorithmAnalyzerStore.focusDetectionParams.target !== '' &&
    algorithmAnalyzerStore.focusDetectionParams.max_depth !== '';

  return (
    <div className="query-tab-content-form">
      <div className="query-tab-content-form-row">
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <i>*</i>
            <span>
              {t('data-analyze.algorithm-forms.focus-detection.options.source')}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.focus-detection.placeholder.input-source-id'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateFocusDetectionParamsErrorMessage
                .source
            }
            value={algorithmAnalyzerStore.focusDetectionParams.source}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateFocusDetectionParams(
                'source',
                e.value as string
              );

              algorithmAnalyzerStore.validateFocusDetectionParams('source');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateFocusDetectionParams('source');
              }
            }}
          />
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t('data-analyze.algorithm-forms.focus-detection.options.label')}
            </span>
          </div>
          <Select
            size="medium"
            trigger="click"
            value={algorithmAnalyzerStore.focusDetectionParams.label}
            notFoundContent={t(
              'data-analyze.algorithm-forms.focus-detection.placeholder.no-edge-types'
            )}
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            width={formWidth}
            onChange={(value: string) => {
              algorithmAnalyzerStore.mutateFocusDetectionParams('label', value);
            }}
          >
            <Select.Option value="__all__" key="__all__">
              {t('data-analyze.algorithm-forms.focus-detection.pre-value')}
            </Select.Option>
            {dataAnalyzeStore.edgeTypes.map(({ name }) => (
              <Select.Option value={name} key={name}>
                {name}
              </Select.Option>
            ))}
          </Select>
        </div>
      </div>
      <div className="query-tab-content-form-row">
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <i>*</i>
            <span>
              {t('data-analyze.algorithm-forms.focus-detection.options.target')}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.focus-detection.placeholder.input-target-id'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateFocusDetectionParamsErrorMessage
                .target
            }
            value={algorithmAnalyzerStore.focusDetectionParams.target}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateFocusDetectionParams(
                'target',
                e.value as string
              );

              algorithmAnalyzerStore.validateFocusDetectionParams('target');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateFocusDetectionParams('target');
              }
            }}
          />
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t(
                'data-analyze.algorithm-forms.focus-detection.options.max_degree'
              )}
            </span>
            <CustomTooltip
              trigger="hover"
              placement="bottom-start"
              modifiers={{
                offset: {
                  offset: '0, 8'
                }
              }}
              tooltipWrapperProps={{
                className: 'tooltips-dark',
                style: {
                  zIndex: 7
                }
              }}
              tooltipWrapper={t(
                'data-analyze.algorithm-forms.focus-detection.hint.max-degree'
              )}
              childrenProps={{
                src: QuestionMarkIcon,
                alt: 'hint',
                style: {
                  marginLeft: 5
                }
              }}
              childrenWrapperElement="img"
            />
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.focus-detection.placeholder.input-positive-integer-or-negative-one-max-degree'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateFocusDetectionParamsErrorMessage
                .max_degree
            }
            value={algorithmAnalyzerStore.focusDetectionParams.max_degree}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateFocusDetectionParams(
                'max_degree',
                e.value as string
              );

              algorithmAnalyzerStore.validateFocusDetectionParams('max_degree');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateFocusDetectionParams(
                  'max_degree'
                );
              }
            }}
          />
        </div>
      </div>
      <div className="query-tab-content-form-row">
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <i>*</i>
            <span>
              {t(
                'data-analyze.algorithm-forms.focus-detection.options.direction'
              )}
            </span>
          </div>
          <Radio.Group
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            value={algorithmAnalyzerStore.focusDetectionParams.direction}
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
              algorithmAnalyzerStore.mutateFocusDetectionParams(
                'direction',
                e.target.value
              );
            }}
          >
            <Radio value="BOTH">both</Radio>
            <Radio value="OUT">out</Radio>
            <Radio value="IN">in</Radio>
          </Radio.Group>
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t(
                'data-analyze.algorithm-forms.focus-detection.options.capacity'
              )}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.focus-detection.placeholder.input-positive-integer-or-negative-one-capacity'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateFocusDetectionParamsErrorMessage
                .capacity
            }
            value={algorithmAnalyzerStore.focusDetectionParams.capacity}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateFocusDetectionParams(
                'capacity',
                e.value as string
              );

              algorithmAnalyzerStore.validateFocusDetectionParams('capacity');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateFocusDetectionParams('capacity');
              }
            }}
          />
        </div>
      </div>
      <div className="query-tab-content-form-row">
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <i>*</i>
            <span>
              {t(
                'data-analyze.algorithm-forms.focus-detection.options.max_depth'
              )}
            </span>
            <CustomTooltip
              trigger="hover"
              placement="bottom-start"
              modifiers={{
                offset: {
                  offset: '0, 8'
                }
              }}
              tooltipWrapperProps={{
                className: 'tooltips-dark',
                style: {
                  zIndex: 7
                }
              }}
              tooltipWrapper={t(
                'data-analyze.algorithm-forms.focus-detection.hint.max-depth'
              )}
              childrenProps={{
                src: QuestionMarkIcon,
                alt: 'hint',
                style: {
                  marginLeft: 5
                }
              }}
              childrenWrapperElement="img"
            />
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.focus-detection.placeholder.input-positive-integer'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateFocusDetectionParamsErrorMessage
                .max_depth
            }
            value={algorithmAnalyzerStore.focusDetectionParams.max_depth}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateFocusDetectionParams(
                'max_depth',
                e.value as string
              );

              algorithmAnalyzerStore.validateFocusDetectionParams('max_depth');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateFocusDetectionParams(
                  'max_depth'
                );
              }
            }}
          />
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t('data-analyze.algorithm-forms.focus-detection.options.limit')}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.focus-detection.placeholder.input-positive-integer-or-negative-one-limit'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateFocusDetectionParamsErrorMessage
                .limit
            }
            value={algorithmAnalyzerStore.focusDetectionParams.limit}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateFocusDetectionParams(
                'limit',
                e.value as string
              );

              algorithmAnalyzerStore.validateFocusDetectionParams('limit');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateFocusDetectionParams('limit');
              }
            }}
          />
        </div>
      </div>
      <div
        className="query-tab-content-form-row"
        style={{ marginLeft: 92, justifyContent: 'flex-start' }}
      >
        <Button
          type="primary"
          style={styles.primaryButton}
          disabled={
            !isValidExec ||
            dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
          }
          onClick={async () => {
            algorithmAnalyzerStore.switchCollapse(true);
            dataAnalyzeStore.switchGraphLoaded(false);

            const timerId = dataAnalyzeStore.addTempExecLog();
            await dataAnalyzeStore.fetchGraphs({
              url: 'crosspoints',
              type: 'focus-detection'
            });
            await dataAnalyzeStore.fetchExecutionLogs();
            window.clearTimeout(timerId);
          }}
        >
          {t('data-analyze.manipulations.execution')}
        </Button>
        <Button
          style={styles.primaryButton}
          disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
          onClick={() => {
            algorithmAnalyzerStore.resetFocusDetectionParams();
          }}
        >
          {t('data-analyze.manipulations.reset')}
        </Button>
      </div>
    </div>
  );
});

export default FocusDetection;
