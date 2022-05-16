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

const RadiographicInspection = observer(() => {
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
      algorithmAnalyzerStore.validateRadiographicInspectionParamsErrorMessage
    ).every((value) => value === '') &&
    algorithmAnalyzerStore.radiographicInspectionParams.source !== '' &&
    algorithmAnalyzerStore.radiographicInspectionParams.max_depth !== '';

  return (
    <div className="query-tab-content-form">
      <div className="query-tab-content-form-row">
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <i>*</i>
            <span>
              {t(
                'data-analyze.algorithm-forms.radiographic-inspection.options.source'
              )}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.radiographic-inspection.placeholder.input-source-id'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore
                .validateRadiographicInspectionParamsErrorMessage.source
            }
            value={algorithmAnalyzerStore.radiographicInspectionParams.source}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateRadiographicInspectionParams(
                'source',
                e.value as string
              );

              algorithmAnalyzerStore.validateRadiographicInspectionParams(
                'source'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateRadiographicInspectionParams(
                  'source'
                );
              }
            }}
          />
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t(
                'data-analyze.algorithm-forms.radiographic-inspection.options.label'
              )}
            </span>
          </div>
          <Select
            size="medium"
            trigger="click"
            value={algorithmAnalyzerStore.radiographicInspectionParams.label}
            notFoundContent={t(
              'data-analyze.algorithm-forms.radiographic-inspection.placeholder.no-edge-types'
            )}
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            width={formWidth}
            onChange={(value: string) => {
              algorithmAnalyzerStore.mutateRadiographicInspectionParams(
                'label',
                value
              );
            }}
          >
            <Select.Option value="__all__" key="__all__">
              {t(
                'data-analyze.algorithm-forms.radiographic-inspection.pre-value'
              )}
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
                'data-analyze.algorithm-forms.radiographic-inspection.options.direction'
              )}
            </span>
          </div>
          <Radio.Group
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            value={
              algorithmAnalyzerStore.radiographicInspectionParams.direction
            }
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
              algorithmAnalyzerStore.mutateRadiographicInspectionParams(
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
                'data-analyze.algorithm-forms.radiographic-inspection.options.max_degree'
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
                'data-analyze.algorithm-forms.radiographic-inspection.hint.max-degree'
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
              'data-analyze.algorithm-forms.radiographic-inspection.placeholder.input-positive-integer-or-negative-one-max-degree'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore
                .validateRadiographicInspectionParamsErrorMessage.max_degree
            }
            value={
              algorithmAnalyzerStore.radiographicInspectionParams.max_degree
            }
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateRadiographicInspectionParams(
                'max_degree',
                e.value as string
              );

              algorithmAnalyzerStore.validateRadiographicInspectionParams(
                'max_degree'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateRadiographicInspectionParams(
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
                'data-analyze.algorithm-forms.radiographic-inspection.options.max_depth'
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
                'data-analyze.algorithm-forms.radiographic-inspection.hint.max-depth'
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
              'data-analyze.algorithm-forms.radiographic-inspection.placeholder.input-positive-integer'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore
                .validateRadiographicInspectionParamsErrorMessage.max_depth
            }
            value={
              algorithmAnalyzerStore.radiographicInspectionParams.max_depth
            }
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateRadiographicInspectionParams(
                'max_depth',
                e.value as string
              );

              algorithmAnalyzerStore.validateRadiographicInspectionParams(
                'max_depth'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateRadiographicInspectionParams(
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
                'data-analyze.algorithm-forms.radiographic-inspection.options.capacity'
              )}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.radiographic-inspection.placeholder.input-positive-integer-or-negative-one-capacity'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore
                .validateRadiographicInspectionParamsErrorMessage.capacity
            }
            value={algorithmAnalyzerStore.radiographicInspectionParams.capacity}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateRadiographicInspectionParams(
                'capacity',
                e.value as string
              );

              algorithmAnalyzerStore.validateRadiographicInspectionParams(
                'capacity'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateRadiographicInspectionParams(
                  'capacity'
                );
              }
            }}
          />
        </div>
      </div>
      <div className="query-tab-content-form-row">
        <div className="query-tab-content-form-item"></div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t(
                'data-analyze.algorithm-forms.radiographic-inspection.options.limit'
              )}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.radiographic-inspection.placeholder.input-positive-integer-or-negative-one-limit'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore
                .validateRadiographicInspectionParamsErrorMessage.limit
            }
            value={algorithmAnalyzerStore.radiographicInspectionParams.limit}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateRadiographicInspectionParams(
                'limit',
                e.value as string
              );

              algorithmAnalyzerStore.validateRadiographicInspectionParams(
                'limit'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateRadiographicInspectionParams(
                  'limit'
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
              url: 'rays',
              type: Algorithm.radiographicInspection
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
            algorithmAnalyzerStore.resetRadiographicInspectionParams();
          }}
        >
          {t('data-analyze.manipulations.reset')}
        </Button>
      </div>
    </div>
  );
});

export default RadiographicInspection;
