import React, { useContext } from 'react';
import { observer } from 'mobx-react';
import { Button, Radio, Input, Select } from 'hubble-ui';
import { useTranslation } from 'react-i18next';

import { styles } from '../QueryAndAlgorithmLibrary';
import { Tooltip as CustomTooltip } from '../../../common';
import { GraphManagementStoreContext } from '../../../../stores';
import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';
import { Algorithm } from '../../../../stores/factory/dataAnalyzeStore/algorithmStore';
import { calcAlgorithmFormWidth } from '../../../../utils';

import QuestionMarkIcon from '../../../../assets/imgs/ic_question_mark.svg';

const KStepNeighbor = observer(() => {
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
      algorithmAnalyzerStore.validateKStepNeighborParamsErrorMessage
    ).every((value) => value === '') &&
    algorithmAnalyzerStore.kStepNeighborParams.source !== '' &&
    algorithmAnalyzerStore.kStepNeighborParams.max_depth !== '';

  return (
    <div className="query-tab-content-form">
      <div className="query-tab-content-form-row">
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <i>*</i>
            <span>
              {t('data-analyze.algorithm-forms.k-step-neighbor.options.source')}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.k-step-neighbor.placeholder.input-source-id'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateKStepNeighborParamsErrorMessage
                .source
            }
            value={algorithmAnalyzerStore.kStepNeighborParams.source}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateKStepNeighborParams(
                'source',
                e.value as string
              );

              algorithmAnalyzerStore.validateKStepNeighborParams('source');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateKStepNeighborParams('source');
              }
            }}
          />
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t('data-analyze.algorithm-forms.k-step-neighbor.options.label')}
            </span>
          </div>
          <Select
            size="medium"
            trigger="click"
            value={algorithmAnalyzerStore.kStepNeighborParams.label}
            notFoundContent={t(
              'data-analyze.algorithm-forms.k-step-neighbor.placeholder.no-edge-types'
            )}
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            width={formWidth}
            onChange={(value: string) => {
              algorithmAnalyzerStore.mutateKStepNeighborParams('label', value);
            }}
          >
            <Select.Option value="__all__" key="__all__">
              {t('data-analyze.algorithm-forms.k-step-neighbor.pre-value')}
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
                'data-analyze.algorithm-forms.k-step-neighbor.options.direction'
              )}
            </span>
          </div>
          <Radio.Group
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            value={algorithmAnalyzerStore.kStepNeighborParams.direction}
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
              algorithmAnalyzerStore.mutateKStepNeighborParams(
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
                'data-analyze.algorithm-forms.k-step-neighbor.options.max_degree'
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
                'data-analyze.algorithm-forms.k-step-neighbor.hint.max-degree'
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
              'data-analyze.algorithm-forms.k-step-neighbor.placeholder.input-positive-integer-or-negative-one-max-degree'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateKStepNeighborParamsErrorMessage
                .max_degree
            }
            value={algorithmAnalyzerStore.kStepNeighborParams.max_degree}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateKStepNeighborParams(
                'max_degree',
                e.value as string
              );

              algorithmAnalyzerStore.validateKStepNeighborParams('max_degree');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateKStepNeighborParams(
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
                'data-analyze.algorithm-forms.k-step-neighbor.options.max_depth'
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
                'data-analyze.algorithm-forms.k-step-neighbor.hint.max-depth'
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
              'data-analyze.algorithm-forms.k-step-neighbor.placeholder.input-positive-integer'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateKStepNeighborParamsErrorMessage
                .max_depth
            }
            value={algorithmAnalyzerStore.kStepNeighborParams.max_depth}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateKStepNeighborParams(
                'max_depth',
                e.value as string
              );

              algorithmAnalyzerStore.validateKStepNeighborParams('max_depth');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateKStepNeighborParams('max_depth');
              }
            }}
          />
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t('data-analyze.algorithm-forms.k-step-neighbor.options.limit')}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.k-step-neighbor.placeholder.input-positive-integer-or-negative-one-limit'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateKStepNeighborParamsErrorMessage
                .limit
            }
            value={algorithmAnalyzerStore.kStepNeighborParams.limit}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateKStepNeighborParams(
                'limit',
                e.value as string
              );

              algorithmAnalyzerStore.validateKStepNeighborParams('limit');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateKStepNeighborParams('limit');
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
              url: 'kneighbor',
              type: Algorithm.kStepNeighbor
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
            algorithmAnalyzerStore.resetKStepNeighborParams();
          }}
        >
          {t('data-analyze.manipulations.reset')}
        </Button>
      </div>
    </div>
  );
});

export default KStepNeighbor;
