import React, { useContext } from 'react';
import { observer } from 'mobx-react';
import { Button, Radio, Input, Select, Switch } from 'hubble-ui';
import { useTranslation } from 'react-i18next';
import { styles } from '../QueryAndAlgorithmLibrary';
import { Tooltip as CustomTooltip } from '../../../common';

import { GraphManagementStoreContext } from '../../../../stores';
import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';
import { Algorithm } from '../../../../stores/factory/dataAnalyzeStore/algorithmStore';
import { isDataTypeNumeric, calcAlgorithmFormWidth } from '../../../../utils';

import QuestionMarkIcon from '../../../../assets/imgs/ic_question_mark.svg';

const WeightedShortestPath = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const dataAnalyzeStore = useContext(DataAnalyzeStore);
  const algorithmAnalyzerStore = dataAnalyzeStore.algorithmAnalyzerStore;
  const { t } = useTranslation();

  const formWidth = calcAlgorithmFormWidth(
    graphManagementStore.isExpanded,
    320,
    390
  );

  const isValidExec =
    Object.values(
      algorithmAnalyzerStore.validateWeightedShortestPathParamsErrorMessage
    ).every((value) => value === '') &&
    algorithmAnalyzerStore.weightedShortestPathParams.source !== '' &&
    algorithmAnalyzerStore.weightedShortestPathParams.target !== '' &&
    algorithmAnalyzerStore.weightedShortestPathParams.weight !== '';

  return (
    <div className="query-tab-content-form">
      <div className="query-tab-content-form-row">
        <div className="query-tab-content-form-item">
          <div
            className="query-tab-content-form-item-title"
            style={{ minWidth: 118 }}
          >
            <i>*</i>
            <span>
              {t(
                'data-analyze.algorithm-forms.weighted-shortest-path.options.source'
              )}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.weighted-shortest-path.placeholder.input-source-id'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore
                .validateWeightedShortestPathParamsErrorMessage.source
            }
            value={algorithmAnalyzerStore.weightedShortestPathParams.source}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateWeightedShortestPathParams(
                'source',
                e.value as string
              );

              algorithmAnalyzerStore.validateWeightedShortestPathParams(
                'source'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateWeightedShortestPathParams(
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
                'data-analyze.algorithm-forms.weighted-shortest-path.options.label'
              )}
            </span>
          </div>
          <Select
            size="medium"
            trigger="click"
            value={algorithmAnalyzerStore.weightedShortestPathParams.label}
            notFoundContent={t(
              'data-analyze.algorithm-forms.weighted-shortest-path.placeholder.no-edge-types'
            )}
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            width={formWidth}
            onChange={(value: string) => {
              algorithmAnalyzerStore.mutateWeightedShortestPathParams(
                'label',
                value
              );
            }}
          >
            <Select.Option value="__all__" key="__all__">
              {t(
                'data-analyze.algorithm-forms.weighted-shortest-path.pre-value'
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
          <div
            className="query-tab-content-form-item-title"
            style={{ minWidth: 118 }}
          >
            <i>*</i>
            <span>
              {t(
                'data-analyze.algorithm-forms.weighted-shortest-path.options.target'
              )}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.weighted-shortest-path.placeholder.input-target-id'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore
                .validateWeightedShortestPathParamsErrorMessage.target
            }
            value={algorithmAnalyzerStore.weightedShortestPathParams.target}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateWeightedShortestPathParams(
                'target',
                e.value as string
              );

              algorithmAnalyzerStore.validateWeightedShortestPathParams(
                'target'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateWeightedShortestPathParams(
                  'target'
                );
              }
            }}
          />
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t(
                'data-analyze.algorithm-forms.weighted-shortest-path.options.max_degree'
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
                'data-analyze.algorithm-forms.weighted-shortest-path.hint.max-degree'
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
              'data-analyze.algorithm-forms.weighted-shortest-path.placeholder.input-positive-integer-or-negative-one-max-degree'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore
                .validateWeightedShortestPathParamsErrorMessage.max_degree
            }
            value={algorithmAnalyzerStore.weightedShortestPathParams.max_degree}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateWeightedShortestPathParams(
                'max_degree',
                e.value as string
              );

              algorithmAnalyzerStore.validateWeightedShortestPathParams(
                'max_degree'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateWeightedShortestPathParams(
                  'max_degree'
                );
              }
            }}
          />
        </div>
      </div>
      <div className="query-tab-content-form-row">
        <div className="query-tab-content-form-item">
          <div
            className="query-tab-content-form-item-title"
            style={{ minWidth: 118 }}
          >
            <i>*</i>
            <span>
              {t(
                'data-analyze.algorithm-forms.weighted-shortest-path.options.direction'
              )}
            </span>
          </div>
          <Radio.Group
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            value={algorithmAnalyzerStore.weightedShortestPathParams.direction}
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
              algorithmAnalyzerStore.mutateWeightedShortestPathParams(
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
                'data-analyze.algorithm-forms.weighted-shortest-path.options.skip_degree'
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
                'data-analyze.algorithm-forms.weighted-shortest-path.hint.skip-degree'
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
              'data-analyze.algorithm-forms.weighted-shortest-path.placeholder.input-integer'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore
                .validateWeightedShortestPathParamsErrorMessage.skip_degree
            }
            value={
              algorithmAnalyzerStore.weightedShortestPathParams.skip_degree
            }
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateWeightedShortestPathParams(
                'skip_degree',
                e.value as string
              );

              algorithmAnalyzerStore.validateWeightedShortestPathParams(
                'skip_degree'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateWeightedShortestPathParams(
                  'skip_degree'
                );
              }
            }}
          />
        </div>
      </div>
      <div className="query-tab-content-form-row">
        <div className="query-tab-content-form-item">
          <div
            className="query-tab-content-form-item-title"
            style={{ minWidth: 118 }}
          >
            <i>*</i>
            <span>
              {t(
                'data-analyze.algorithm-forms.weighted-shortest-path.options.weight'
              )}
            </span>
          </div>
          <Select
            size="medium"
            trigger="click"
            value={algorithmAnalyzerStore.weightedShortestPathParams.weight}
            notFoundContent={t(
              'data-analyze.algorithm-forms.weighted-shortest-path.placeholder.no-property'
            )}
            selectorName={t(
              'data-analyze.algorithm-forms.weighted-shortest-path.placeholder.select-property'
            )}
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            width={formWidth}
            onChange={(value: string) => {
              algorithmAnalyzerStore.mutateWeightedShortestPathParams(
                'weight',
                value
              );
            }}
          >
            {dataAnalyzeStore.allPropertiesFromEdge
              .filter((propertyName) =>
                isDataTypeNumeric(
                  dataAnalyzeStore.properties.find(
                    ({ name }) => name === propertyName
                  )?.data_type
                )
              )
              .map((propertyName) => (
                <Select.Option value={propertyName} key={propertyName}>
                  {propertyName}
                </Select.Option>
              ))}
          </Select>
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t(
                'data-analyze.algorithm-forms.weighted-shortest-path.options.capacity'
              )}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.weighted-shortest-path.placeholder.input-positive-integer-or-negative-one-capacity'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore
                .validateWeightedShortestPathParamsErrorMessage.capacity
            }
            value={algorithmAnalyzerStore.weightedShortestPathParams.capacity}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateWeightedShortestPathParams(
                'capacity',
                e.value as string
              );

              algorithmAnalyzerStore.validateWeightedShortestPathParams(
                'capacity'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateWeightedShortestPathParams(
                  'capacity'
                );
              }
            }}
          />
        </div>
      </div>
      <div className="query-tab-content-form-row">
        <div className="query-tab-content-form-item">
          <div
            className="query-tab-content-form-item-title"
            style={{ minWidth: 118 }}
          >
            <span>
              {t(
                'data-analyze.algorithm-forms.weighted-shortest-path.options.with_vertex'
              )}
            </span>
          </div>
          <Switch
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            checked={
              algorithmAnalyzerStore.weightedShortestPathParams.with_vertex
            }
            onChange={(checked: boolean) => {
              algorithmAnalyzerStore.mutateWeightedShortestPathParams(
                'with_vertex',
                checked
              );
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
              url: 'weightedshortpath',
              type: Algorithm.weightedShortestPath
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
            algorithmAnalyzerStore.resetWeightedShortestPathParams();
          }}
        >
          {t('data-analyze.manipulations.reset')}
        </Button>
      </div>
    </div>
  );
});

export default WeightedShortestPath;
