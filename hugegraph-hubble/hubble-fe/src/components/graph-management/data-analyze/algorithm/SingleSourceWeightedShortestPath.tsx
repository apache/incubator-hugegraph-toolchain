import React, { useContext } from 'react';
import { observer } from 'mobx-react';
import { Button, Radio, Input, Select, Switch } from 'hubble-ui';
import { useTranslation } from 'react-i18next';
import { styles } from '../QueryAndAlgorithmLibrary';
import { Tooltip as CustomTooltip } from '../../../common';

import { GraphManagementStoreContext } from '../../../../stores';
import { Algorithm } from '../../../../stores/factory/dataAnalyzeStore/algorithmStore';
import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';
import { isDataTypeNumeric, calcAlgorithmFormWidth } from '../../../../utils';

import QuestionMarkIcon from '../../../../assets/imgs/ic_question_mark.svg';

const SingleSourceWeightedShortestPath = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const dataAnalyzeStore = useContext(DataAnalyzeStore);
  const algorithmAnalyzerStore = dataAnalyzeStore.algorithmAnalyzerStore;
  const { t } = useTranslation();

  const formWidth = calcAlgorithmFormWidth(
    graphManagementStore.isExpanded,
    340,
    380
  );

  const isValidExec =
    Object.values(
      algorithmAnalyzerStore.validateSingleSourceWeightedShortestPathParamsErrorMessage
    ).every((value) => value === '') &&
    algorithmAnalyzerStore.singleSourceWeightedShortestPathParams.source !== '';

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
                'data-analyze.algorithm-forms.single-source-weighted-shortest-path.options.source'
              )}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.single-source-weighted-shortest-path.placeholder.input-source-id'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore
                .validateSingleSourceWeightedShortestPathParamsErrorMessage
                .source
            }
            value={
              algorithmAnalyzerStore.singleSourceWeightedShortestPathParams
                .source
            }
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateSingleSourceWeightedShortestPathParams(
                'source',
                e.value as string
              );

              algorithmAnalyzerStore.validateSingleSourceWeightedShortestPathParams(
                'source'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateSingleSourceWeightedShortestPathParams(
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
                'data-analyze.algorithm-forms.single-source-weighted-shortest-path.options.label'
              )}
            </span>
          </div>
          <Select
            size="medium"
            trigger="click"
            value={
              algorithmAnalyzerStore.singleSourceWeightedShortestPathParams
                .label
            }
            notFoundContent={t(
              'data-analyze.algorithm-forms.single-source-weighted-shortest-path.placeholder.no-edge-types'
            )}
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            width={formWidth}
            onChange={(value: string) => {
              algorithmAnalyzerStore.mutateSingleSourceWeightedShortestPathParams(
                'label',
                value
              );
            }}
          >
            <Select.Option value="__all__" key="__all__">
              {t(
                'data-analyze.algorithm-forms.single-source-weighted-shortest-path.pre-value'
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
                'data-analyze.algorithm-forms.single-source-weighted-shortest-path.options.direction'
              )}
            </span>
          </div>
          <Radio.Group
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            value={
              algorithmAnalyzerStore.singleSourceWeightedShortestPathParams
                .direction
            }
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
              algorithmAnalyzerStore.mutateSingleSourceWeightedShortestPathParams(
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
                'data-analyze.algorithm-forms.single-source-weighted-shortest-path.options.max_degree'
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
                'data-analyze.algorithm-forms.single-source-weighted-shortest-path.hint.max-degree'
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
              'data-analyze.algorithm-forms.single-source-weighted-shortest-path.placeholder.input-positive-integer-or-negative-one-max-degree'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore
                .validateSingleSourceWeightedShortestPathParamsErrorMessage
                .max_degree
            }
            value={
              algorithmAnalyzerStore.singleSourceWeightedShortestPathParams
                .max_degree
            }
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateSingleSourceWeightedShortestPathParams(
                'max_degree',
                e.value as string
              );

              algorithmAnalyzerStore.validateSingleSourceWeightedShortestPathParams(
                'max_degree'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateSingleSourceWeightedShortestPathParams(
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
            <span>
              {t(
                'data-analyze.algorithm-forms.single-source-weighted-shortest-path.options.weight'
              )}
            </span>
          </div>
          <Select
            size="medium"
            trigger="click"
            value={
              algorithmAnalyzerStore.singleSourceWeightedShortestPathParams
                .weight
            }
            notFoundContent={t(
              'data-analyze.algorithm-forms.single-source-weighted-shortest-path.placeholder.no-edge-types'
            )}
            selectorName={t(
              'data-analyze.algorithm-forms.single-source-weighted-shortest-path.placeholder.no-property'
            )}
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            width={formWidth}
            onChange={(value: string) => {
              algorithmAnalyzerStore.mutateSingleSourceWeightedShortestPathParams(
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
                'data-analyze.algorithm-forms.single-source-weighted-shortest-path.options.skip_degree'
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
                'data-analyze.algorithm-forms.single-source-weighted-shortest-path.hint.skip-degree'
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
              'data-analyze.algorithm-forms.single-source-weighted-shortest-path.placeholder.input-integer'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore
                .validateSingleSourceWeightedShortestPathParamsErrorMessage
                .skip_degree
            }
            value={
              algorithmAnalyzerStore.singleSourceWeightedShortestPathParams
                .skip_degree
            }
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateSingleSourceWeightedShortestPathParams(
                'skip_degree',
                e.value as string
              );

              algorithmAnalyzerStore.validateSingleSourceWeightedShortestPathParams(
                'skip_degree'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateSingleSourceWeightedShortestPathParams(
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
            <span>
              {t(
                'data-analyze.algorithm-forms.single-source-weighted-shortest-path.options.with_vertex'
              )}
            </span>
          </div>
          <Switch
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            checked={
              algorithmAnalyzerStore.singleSourceWeightedShortestPathParams
                .with_vertex
            }
            onChange={(checked: boolean) => {
              algorithmAnalyzerStore.mutateSingleSourceWeightedShortestPathParams(
                'with_vertex',
                checked
              );
            }}
          />
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title">
            <span>
              {t(
                'data-analyze.algorithm-forms.single-source-weighted-shortest-path.options.capacity'
              )}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.single-source-weighted-shortest-path.placeholder.input-positive-integer-or-negative-one-capacity'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore
                .validateSingleSourceWeightedShortestPathParamsErrorMessage
                .capacity
            }
            value={
              algorithmAnalyzerStore.singleSourceWeightedShortestPathParams
                .capacity
            }
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateSingleSourceWeightedShortestPathParams(
                'capacity',
                e.value as string
              );

              algorithmAnalyzerStore.validateSingleSourceWeightedShortestPathParams(
                'capacity'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateSingleSourceWeightedShortestPathParams(
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
                'data-analyze.algorithm-forms.single-source-weighted-shortest-path.options.limit'
              )}
            </span>
          </div>
          <Input
            width={formWidth}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.single-source-weighted-shortest-path.placeholder.input-positive-integer-or-negative-one-limit'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore
                .validateSingleSourceWeightedShortestPathParamsErrorMessage
                .limit
            }
            value={
              algorithmAnalyzerStore.singleSourceWeightedShortestPathParams
                .limit
            }
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateSingleSourceWeightedShortestPathParams(
                'limit',
                e.value as string
              );

              algorithmAnalyzerStore.validateSingleSourceWeightedShortestPathParams(
                'limit'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateSingleSourceWeightedShortestPathParams(
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
              url: 'singleshortpath',
              type: Algorithm.singleSourceWeightedShortestPath
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
            algorithmAnalyzerStore.resetSingleSourceWeightedShortestPathParams();
          }}
        >
          {t('data-analyze.manipulations.reset')}
        </Button>
      </div>
    </div>
  );
});

export default SingleSourceWeightedShortestPath;
