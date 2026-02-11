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

import React, { useContext } from 'react';
import { observer } from 'mobx-react';
import { Button, Radio, Input, Select, Switch } from 'hubble-ui';
import { useTranslation } from 'react-i18next';

import { styles } from '../QueryAndAlgorithmLibrary';
import { Tooltip as CustomTooltip } from '../../../common';
import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';
import { Algorithm } from '../../../../stores/factory/dataAnalyzeStore/algorithmStore';
import { GraphManagementStoreContext } from '../../../../stores';
import { calcAlgorithmFormWidth } from '../../../../utils';

import QuestionMarkIcon from '../../../../assets/imgs/ic_question_mark.svg';

const LoopDetection = observer(() => {
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
      algorithmAnalyzerStore.validateLoopDetectionParamsErrorMessage
    ).every((value) => value === '') &&
    algorithmAnalyzerStore.loopDetectionParams.source !== '' &&
    algorithmAnalyzerStore.loopDetectionParams.max_depth !== '';

  return (
    <div className="query-tab-content-form">
      <div className="query-tab-content-form-row">
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <i>*</i>
            <span>
              {t('data-analyze.algorithm-forms.loop-detection.options.source')}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.loop-detection.placeholder.input-source-id'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateLoopDetectionParamsErrorMessage
                .source
            }
            value={algorithmAnalyzerStore.loopDetectionParams.source}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateLoopDetectionParams(
                'source',
                e.value as string
              );

              algorithmAnalyzerStore.validateLoopDetectionParams('source');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateLoopDetectionParams('source');
              }
            }}
          />
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t('data-analyze.algorithm-forms.loop-detection.options.label')}
            </span>
          </div>
          <Select
            size="medium"
            trigger="click"
            value={algorithmAnalyzerStore.loopDetectionParams.label}
            notFoundContent={t(
              'data-analyze.algorithm-forms.loop-detection.placeholder.no-edge-types'
            )}
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            width={formWidth}
            onChange={(value: string) => {
              algorithmAnalyzerStore.mutateLoopDetectionParams('label', value);
            }}
          >
            <Select.Option value="__all__" key="__all__">
              {t('data-analyze.algorithm-forms.loop-detection.pre-value')}
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
              {t(
                'data-analyze.algorithm-forms.loop-detection.options.direction'
              )}
            </span>
          </div>
          <Radio.Group
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            value={algorithmAnalyzerStore.loopDetectionParams.direction}
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
              algorithmAnalyzerStore.mutateLoopDetectionParams(
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
                'data-analyze.algorithm-forms.loop-detection.options.max_degree'
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
                'data-analyze.algorithm-forms.loop-detection.hint.max-degree'
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
              'data-analyze.algorithm-forms.loop-detection.placeholder.input-positive-integer-or-negative-one-max-degree'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateLoopDetectionParamsErrorMessage
                .max_degree
            }
            value={algorithmAnalyzerStore.loopDetectionParams.max_degree}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateLoopDetectionParams(
                'max_degree',
                e.value as string
              );

              algorithmAnalyzerStore.validateLoopDetectionParams('max_degree');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateLoopDetectionParams(
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
                'data-analyze.algorithm-forms.loop-detection.options.max_depth'
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
                'data-analyze.algorithm-forms.loop-detection.hint.max-depth'
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
              'data-analyze.algorithm-forms.loop-detection.placeholder.input-positive-integer'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateLoopDetectionParamsErrorMessage
                .max_depth
            }
            value={algorithmAnalyzerStore.loopDetectionParams.max_depth}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateLoopDetectionParams(
                'max_depth',
                e.value as string
              );

              algorithmAnalyzerStore.validateLoopDetectionParams('max_depth');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateLoopDetectionParams('max_depth');
              }
            }}
          />
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t('data-analyze.algorithm-forms.loop-detection.options.limit')}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.loop-detection.placeholder.input-positive-integer-or-negative-one-limit'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateLoopDetectionParamsErrorMessage
                .limit
            }
            value={algorithmAnalyzerStore.loopDetectionParams.limit}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateLoopDetectionParams(
                'limit',
                e.value as string
              );

              algorithmAnalyzerStore.validateLoopDetectionParams('limit');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateLoopDetectionParams('limit');
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
                'data-analyze.algorithm-forms.loop-detection.options.source_in_ring'
              )}
            </span>
          </div>
          <Switch
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            checked={algorithmAnalyzerStore.loopDetectionParams.source_in_ring}
            onChange={(checked: boolean) => {
              algorithmAnalyzerStore.mutateLoopDetectionParams(
                'source_in_ring',
                checked
              );
            }}
          />
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t(
                'data-analyze.algorithm-forms.loop-detection.options.capacity'
              )}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.loop-detection.placeholder.input-positive-integer-or-negative-one-capacity'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateLoopDetectionParamsErrorMessage
                .capacity
            }
            value={algorithmAnalyzerStore.loopDetectionParams.capacity}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateLoopDetectionParams(
                'capacity',
                e.value as string
              );

              algorithmAnalyzerStore.validateLoopDetectionParams('capacity');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateLoopDetectionParams('capacity');
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
              url: 'rings',
              type: Algorithm.loopDetection
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
            algorithmAnalyzerStore.resetLoopDetectionParams();
          }}
        >
          {t('data-analyze.manipulations.reset')}
        </Button>
      </div>
    </div>
  );
});

export default LoopDetection;
