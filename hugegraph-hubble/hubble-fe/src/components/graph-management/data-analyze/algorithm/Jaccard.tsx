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

const Jaccard = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const dataAnalyzeStore = useContext(DataAnalyzeStore);
  const { t } = useTranslation();
  const algorithmAnalyzerStore = dataAnalyzeStore.algorithmAnalyzerStore;

  const isValidExec =
    Object.values(
      algorithmAnalyzerStore.validateJaccardParamsErrorMessage
    ).every((value) => value === '') &&
    algorithmAnalyzerStore.jaccardParams.vertex !== '' &&
    algorithmAnalyzerStore.jaccardParams.other !== '';

  const formWidth = calcAlgorithmFormWidth(
    graphManagementStore.isExpanded,
    340,
    400
  );

  return (
    <div className="query-tab-content-form">
      <div className="query-tab-content-form-row">
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <i>*</i>
            <span>
              {t('data-analyze.algorithm-forms.jaccard.options.vertex')}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.jaccard.placeholder.input-source-id'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateJaccardParamsErrorMessage.vertex
            }
            value={algorithmAnalyzerStore.jaccardParams.vertex}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateJaccardParams(
                'vertex',
                e.value as string
              );

              algorithmAnalyzerStore.validateJaccardParams('vertex');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateJaccardParams('vertex');
              }
            }}
          />
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t('data-analyze.algorithm-forms.jaccard.options.label')}
            </span>
          </div>
          <Select
            size="medium"
            trigger="click"
            value={algorithmAnalyzerStore.jaccardParams.label}
            notFoundContent={t(
              'data-analyze.algorithm-forms.jaccard.placeholder.no-edge-types'
            )}
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            width={formWidth}
            onChange={(value: string) => {
              algorithmAnalyzerStore.mutateJaccardParams('label', value);
            }}
          >
            <Select.Option value="__all__" key="__all__">
              {t('data-analyze.algorithm-forms.jaccard.pre-value')}
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
              {t('data-analyze.algorithm-forms.jaccard.options.other')}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.jaccard.placeholder.input-other-id'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateJaccardParamsErrorMessage.other
            }
            value={algorithmAnalyzerStore.jaccardParams.other}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateJaccardParams(
                'other',
                e.value as string
              );

              algorithmAnalyzerStore.validateJaccardParams('other');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateJaccardParams('other');
              }
            }}
          />
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t('data-analyze.algorithm-forms.jaccard.options.max_degree')}
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
                'data-analyze.algorithm-forms.jaccard.hint.max-degree'
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
              'data-analyze.algorithm-forms.jaccard.placeholder.input-positive-integer-or-negative-one-max-degree'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateJaccardParamsErrorMessage
                .max_degree
            }
            value={algorithmAnalyzerStore.jaccardParams.max_degree}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateJaccardParams(
                'max_degree',
                e.value as string
              );

              algorithmAnalyzerStore.validateJaccardParams('max_degree');
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateJaccardParams('max_degree');
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
              {t('data-analyze.algorithm-forms.jaccard.options.direction')}
            </span>
          </div>
          <Radio.Group
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            value={algorithmAnalyzerStore.jaccardParams.direction}
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
              algorithmAnalyzerStore.mutateJaccardParams(
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
              url: 'jaccardsimilarity',
              type: Algorithm.jaccard
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
            algorithmAnalyzerStore.resetJaccardParams();
          }}
        >
          {t('data-analyze.manipulations.reset')}
        </Button>
      </div>
    </div>
  );
});

export default Jaccard;
