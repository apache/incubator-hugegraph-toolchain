import React, { useContext } from 'react';
import { observer } from 'mobx-react';
import { size } from 'lodash-es';
import { useTranslation } from 'react-i18next';
import { styles } from '../QueryAndAlgorithmLibrary';
import { Button, Radio, Input, Select } from 'hubble-ui';

import { Tooltip as CustomTooltip } from '../../../common';
import { GraphManagementStoreContext } from '../../../../stores';
import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';
import { calcAlgorithmFormWidth } from '../../../../utils';

import QuestionMarkIcon from '../../../../assets/imgs/ic_question_mark.svg';
import { Algorithm } from '../../../../stores/factory/dataAnalyzeStore/algorithmStore';

const NeighborRank = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const dataAnalyzeStore = useContext(DataAnalyzeStore);
  const algorithmAnalyzerStore = dataAnalyzeStore.algorithmAnalyzerStore;
  const { t } = useTranslation();

  const formWidth = calcAlgorithmFormWidth(
    graphManagementStore.isExpanded,
    340,
    400
  );

  const formWidthInStep = calcAlgorithmFormWidth(
    graphManagementStore.isExpanded,
    310,
    380
  );

  const isValidExec =
    Object.values(
      algorithmAnalyzerStore.validateNeighborRankParamsParamsErrorMessage
    ).every((value) => Array.isArray(value) || value === '') &&
    algorithmAnalyzerStore.validateNeighborRankParamsParamsErrorMessage.steps.every(
      (step) => Object.values(step).every((value) => value === '')
    ) &&
    algorithmAnalyzerStore.neighborRankParams.source !== '' &&
    algorithmAnalyzerStore.neighborRankParams.alpha !== '';

  const isValidAddRule = algorithmAnalyzerStore.validateNeighborRankParamsParamsErrorMessage.steps.every(
    (step) => Object.values(step).every((value) => value === '')
  );

  return (
    <div style={{ display: 'flex' }}>
      <div className="query-tab-content-form" style={{ width: '50%' }}>
        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div
              className="query-tab-content-form-item-title"
              style={{ width: 105 }}
            >
              <i>*</i>
              <span>
                {t('data-analyze.algorithm-forms.neighbor-rank.options.source')}
              </span>
            </div>
            <Input
              width={formWidth}
              size="medium"
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              placeholder={t(
                'data-analyze.algorithm-forms.neighbor-rank.placeholder.input-source-id'
              )}
              errorLocation="layer"
              errorMessage={
                algorithmAnalyzerStore
                  .validateNeighborRankParamsParamsErrorMessage.source
              }
              value={algorithmAnalyzerStore.neighborRankParams.source}
              onChange={(e: any) => {
                algorithmAnalyzerStore.mutateNeighborRankParams(
                  'source',
                  e.value as string
                );

                algorithmAnalyzerStore.validateNeighborRankParams('source');
              }}
              originInputProps={{
                onBlur() {
                  algorithmAnalyzerStore.validateNeighborRankParams('source');
                }
              }}
            />
          </div>
        </div>
        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div
              className="query-tab-content-form-item-title"
              style={{ width: 105 }}
            >
              <i>*</i>
              <span>
                {t('data-analyze.algorithm-forms.neighbor-rank.options.alpha')}
              </span>
            </div>
            <Input
              width={formWidth}
              size="medium"
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              placeholder={t(
                'data-analyze.algorithm-forms.neighbor-rank.placeholder.range'
              )}
              errorLocation="layer"
              errorMessage={
                algorithmAnalyzerStore
                  .validateNeighborRankParamsParamsErrorMessage.alpha
              }
              value={algorithmAnalyzerStore.neighborRankParams.alpha}
              onChange={(e: any) => {
                algorithmAnalyzerStore.mutateNeighborRankParams(
                  'alpha',
                  e.value as string
                );

                algorithmAnalyzerStore.validateNeighborRankParams('alpha');
              }}
              originInputProps={{
                onBlur() {
                  algorithmAnalyzerStore.validateNeighborRankParams('alpha');
                }
              }}
            />
          </div>
        </div>
        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div
              className="query-tab-content-form-item-title"
              style={{ width: 105 }}
            >
              <span>
                {t(
                  'data-analyze.algorithm-forms.neighbor-rank.options.capacity'
                )}
              </span>
            </div>
            <Input
              width={formWidth}
              size="medium"
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              placeholder={t(
                'data-analyze.algorithm-forms.neighbor-rank.placeholder.input-positive-integer-or-negative-one-capacity'
              )}
              errorLocation="layer"
              errorMessage={
                algorithmAnalyzerStore
                  .validateNeighborRankParamsParamsErrorMessage.capacity
              }
              value={algorithmAnalyzerStore.neighborRankParams.capacity}
              onChange={(e: any) => {
                algorithmAnalyzerStore.mutateNeighborRankParams(
                  'capacity',
                  e.value as string
                );

                algorithmAnalyzerStore.validateNeighborRankParams('capacity');
              }}
              originInputProps={{
                onBlur() {
                  algorithmAnalyzerStore.validateNeighborRankParams('capacity');
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
                url: 'neighborrank',
                type: Algorithm.neighborRank
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
              algorithmAnalyzerStore.resetNeighborRankParams();
            }}
          >
            {t('data-analyze.manipulations.reset')}
          </Button>
        </div>
      </div>

      <div
        className="query-tab-content-form-expand-wrapper"
        style={{ width: '50%' }}
      >
        {algorithmAnalyzerStore.neighborRankParams.steps.map(
          ({ uuid, direction, labels, degree, top }, ruleIndex) => {
            return (
              <div className="query-tab-content-form-expand-items">
                <div className="query-tab-content-form-expand-item">
                  <div className="query-tab-content-form-item-title query-tab-content-form-expand-title">
                    <i>*</i>
                    <span>
                      {t(
                        'data-analyze.algorithm-forms.neighbor-rank.options.direction'
                      )}
                    </span>
                  </div>
                  <Radio.Group
                    disabled={
                      dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                    }
                    value={direction}
                    onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                      algorithmAnalyzerStore.mutateNeighborRankRuleParams(
                        'direction',
                        e.target.value,
                        ruleIndex
                      );
                    }}
                  >
                    <Radio value="BOTH">both</Radio>
                    <Radio value="OUT">out</Radio>
                    <Radio value="IN">in</Radio>
                  </Radio.Group>
                  {size(algorithmAnalyzerStore.neighborRankParams.steps) >
                    1 && (
                    <div
                      style={{
                        marginLeft: 175,
                        fontSize: 14,
                        color: '#2b65ff',
                        cursor: 'pointer',
                        lineHeight: '22px'
                      }}
                      onClick={() => {
                        algorithmAnalyzerStore.removeNeighborRankRule(
                          ruleIndex
                        );
                      }}
                    >
                      {t('addition.common.del')}
                    </div>
                  )}
                </div>
                <div className="query-tab-content-form-expand-item">
                  <div className="query-tab-content-form-item-title query-tab-content-form-expand-title">
                    <span>
                      {t(
                        'data-analyze.algorithm-forms.neighbor-rank.options.label'
                      )}
                    </span>
                  </div>
                  <Select
                    size="medium"
                    trigger="click"
                    value={labels[0]}
                    notFoundContent={t(
                      'data-analyze.algorithm-forms.neighbor-rank.placeholder.no-edge-types'
                    )}
                    disabled={
                      dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                    }
                    width={formWidthInStep}
                    onChange={(value: string) => {
                      algorithmAnalyzerStore.mutateNeighborRankRuleParams(
                        'labels',
                        [value],
                        ruleIndex
                      );
                    }}
                  >
                    <Select.Option value="__all__" key="__all__">
                      {t(
                        'data-analyze.algorithm-forms.neighbor-rank.pre-value'
                      )}
                    </Select.Option>
                    {dataAnalyzeStore.edgeTypes.map(({ name }) => (
                      <Select.Option value={name} key={name}>
                        {name}
                      </Select.Option>
                    ))}
                  </Select>
                </div>
                <div className="query-tab-content-form-expand-item">
                  <div className="query-tab-content-form-item-title query-tab-content-form-expand-title">
                    <span>
                      {t(
                        'data-analyze.algorithm-forms.neighbor-rank.options.degree'
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
                        'data-analyze.algorithm-forms.neighbor-rank.hint.max-degree'
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
                    width={formWidthInStep}
                    size="medium"
                    disabled={
                      dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                    }
                    placeholder={t(
                      'data-analyze.algorithm-forms.neighbor-rank.placeholder.input-positive-integer-or-negative-one-max-degree'
                    )}
                    errorLocation="layer"
                    errorMessage={
                      algorithmAnalyzerStore
                        .validateNeighborRankParamsParamsErrorMessage.steps[
                        ruleIndex
                      ].degree
                    }
                    value={degree}
                    onChange={(e: any) => {
                      algorithmAnalyzerStore.mutateNeighborRankRuleParams(
                        'degree',
                        e.value as string,
                        ruleIndex
                      );

                      algorithmAnalyzerStore.validateNeighborRankRules(
                        'degree',
                        ruleIndex
                      );
                    }}
                    originInputProps={{
                      onBlur() {
                        algorithmAnalyzerStore.validateNeighborRankRules(
                          'degree',
                          ruleIndex
                        );
                      }
                    }}
                  />
                </div>
                <div className="query-tab-content-form-expand-item">
                  <div className="query-tab-content-form-item-title query-tab-content-form-expand-title">
                    <span>
                      {t(
                        'data-analyze.algorithm-forms.neighbor-rank.options.top'
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
                        'data-analyze.algorithm-forms.neighbor-rank.hint.top'
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
                    width={formWidthInStep}
                    size="medium"
                    disabled={
                      dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                    }
                    placeholder={t(
                      'data-analyze.algorithm-forms.neighbor-rank.placeholder.input-integer-lt-1000'
                    )}
                    errorLocation="layer"
                    errorMessage={
                      algorithmAnalyzerStore
                        .validateNeighborRankParamsParamsErrorMessage.steps[
                        ruleIndex
                      ].top
                    }
                    value={top}
                    onChange={(e: any) => {
                      algorithmAnalyzerStore.mutateNeighborRankRuleParams(
                        'top',
                        e.value as string,
                        ruleIndex
                      );

                      algorithmAnalyzerStore.validateNeighborRankRules(
                        'top',
                        ruleIndex
                      );
                    }}
                    originInputProps={{
                      onBlur() {
                        algorithmAnalyzerStore.validateNeighborRankRules(
                          'top',
                          ruleIndex
                        );
                      }
                    }}
                  />
                </div>
              </div>
            );
          }
        )}
        <div
          style={{
            width: 'fix-content',
            fontSize: 14,
            color: isValidAddRule ? '#2b65ff' : '#999',
            marginTop: 8
          }}
        >
          <span
            style={{ cursor: 'pointer' }}
            onClick={() => {
              if (isValidAddRule) {
                algorithmAnalyzerStore.addNeighborRankRule();
              }
            }}
          >
            {t('data-analyze.algorithm-forms.neighbor-rank.add-new-rule')}
          </span>
        </div>
      </div>
    </div>
  );
});

export default NeighborRank;
