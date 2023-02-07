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
import { GraphManagementStoreContext } from '../../../../stores';
import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';
import { calcAlgorithmFormWidth } from '../../../../utils';

import QuestionMarkIcon from '../../../../assets/imgs/ic_question_mark.svg';
import { Algorithm } from '../../../../stores/factory/dataAnalyzeStore/algorithmStore';

const ShortestPathAll = observer(() => {
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
      algorithmAnalyzerStore.validateShortestPathAllParamsErrorMessage
    ).every((value) => value === '') &&
    algorithmAnalyzerStore.shortestPathAllParams.source !== '' &&
    algorithmAnalyzerStore.shortestPathAllParams.target !== '' &&
    algorithmAnalyzerStore.shortestPathAllParams.max_depth !== '';

  return (
    <div className="query-tab-content-form">
      <div className="query-tab-content-form-row">
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <i>*</i>
            <span>
              {t(
                'data-analyze.algorithm-forms.shortest-path-all.options.source'
              )}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.shortest-path-all.placeholder.input-source-id'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateShortestPathAllParamsErrorMessage
                .source
            }
            value={algorithmAnalyzerStore.shortestPathAllParams.source}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateShortestPathAllParams(
                'source',
                e.value as string
              );

              algorithmAnalyzerStore.validateShortestPathAllParams('source');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateShortestPathAllParams('source');
              }
            }}
          />
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t(
                'data-analyze.algorithm-forms.shortest-path-all.options.label'
              )}
            </span>
          </div>
          <Select
            size="medium"
            trigger="click"
            value={algorithmAnalyzerStore.shortestPathAllParams.label}
            notFoundContent={t(
              'data-analyze.algorithm-forms.shortest-path-all.placeholder.no-edge-types'
            )}
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            width={formWidth}
            onChange={(value: string) => {
              algorithmAnalyzerStore.mutateShortestPathAllParams(
                'label',
                value
              );
            }}
          >
            <Select.Option value="__all__" key="__all__">
              {t('data-analyze.algorithm-forms.shortest-path-all.pre-value')}
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
                'data-analyze.algorithm-forms.shortest-path-all.options.target'
              )}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.shortest-path-all.placeholder.input-target-id'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateShortestPathAllParamsErrorMessage
                .target
            }
            value={algorithmAnalyzerStore.shortestPathAllParams.target}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateShortestPathAllParams(
                'target',
                e.value as string
              );

              algorithmAnalyzerStore.validateShortestPathAllParams('target');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateShortestPathAllParams('target');
              }
            }}
          />
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t(
                'data-analyze.algorithm-forms.shortest-path-all.options.max_degree'
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
                'data-analyze.algorithm-forms.shortest-path-all.hint.max-degree'
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
              'data-analyze.algorithm-forms.shortest-path-all.placeholder.input-positive-integer-or-negative-one-max-degree'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateShortestPathAllParamsErrorMessage
                .max_degree
            }
            value={algorithmAnalyzerStore.shortestPathAllParams.max_degree}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateShortestPathAllParams(
                'max_degree',
                e.value as string
              );

              algorithmAnalyzerStore.validateShortestPathAllParams(
                'max_degree'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateShortestPathAllParams(
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
                'data-analyze.algorithm-forms.shortest-path-all.options.direction'
              )}
            </span>
          </div>
          <Radio.Group
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            value={algorithmAnalyzerStore.shortestPathAllParams.direction}
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
              algorithmAnalyzerStore.mutateShortestPathAllParams(
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
                'data-analyze.algorithm-forms.shortest-path-all.options.capacity'
              )}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.shortest-path-all.placeholder.input-positive-integer-or-negative-one-capacity'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateShortestPathAllParamsErrorMessage
                .capacity
            }
            value={algorithmAnalyzerStore.shortestPathAllParams.capacity}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateShortestPathAllParams(
                'capacity',
                e.value as string
              );

              algorithmAnalyzerStore.validateShortestPathAllParams('capacity');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateShortestPathAllParams(
                  'capacity'
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
                'data-analyze.algorithm-forms.shortest-path-all.options.max_depth'
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
                'data-analyze.algorithm-forms.shortest-path-all.hint.max-depth'
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
              'data-analyze.algorithm-forms.shortest-path-all.placeholder.input-positive-integer'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateShortestPathAllParamsErrorMessage
                .max_depth
            }
            value={algorithmAnalyzerStore.shortestPathAllParams.max_depth}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateShortestPathAllParams(
                'max_depth',
                e.value as string
              );

              algorithmAnalyzerStore.validateShortestPathAllParams('max_depth');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateShortestPathAllParams(
                  'max_depth'
                );
              }
            }}
          />
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t(
                'data-analyze.algorithm-forms.shortest-path-all.options.skip_degree'
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
                'data-analyze.algorithm-forms.shortest-path-all.hint.skip-degree'
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
              'data-analyze.algorithm-forms.shortest-path-all.placeholder.input-integer'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateShortestPathAllParamsErrorMessage
                .skip_degree
            }
            value={algorithmAnalyzerStore.shortestPathAllParams.skip_degree}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateShortestPathAllParams(
                'skip_degree',
                e.value as string
              );

              algorithmAnalyzerStore.validateShortestPathAllParams(
                'skip_degree'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateShortestPathAllParams(
                  'skip_degree'
                );
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
              url: 'allshortpath',
              type: Algorithm.shortestPathAll
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
            algorithmAnalyzerStore.resetShortestPathAllParams();
          }}
        >
          {t('data-analyze.manipulations.reset')}
        </Button>
      </div>
    </div>
  );
});

export default ShortestPathAll;
