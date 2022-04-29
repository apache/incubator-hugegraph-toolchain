import React, { useContext } from 'react';
import { observer } from 'mobx-react';
import { flatten, cloneDeep, size, isEmpty } from 'lodash-es';
import { Button, Radio, Input, Select, Switch } from 'hubble-ui';
import { useTranslation } from 'react-i18next';

import { styles } from '../QueryAndAlgorithmLibrary';
import { Tooltip as CustomTooltip } from '../../../common';
import { GraphManagementStoreContext } from '../../../../stores';
import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';
import { calcAlgorithmFormWidth } from '../../../../utils';

import QuestionMarkIcon from '../../../../assets/imgs/ic_question_mark.svg';
import { Algorithm } from '../../../../stores/factory/dataAnalyzeStore/algorithmStore';

const ModelSimilarity = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const dataAnalyzeStore = useContext(DataAnalyzeStore);
  const algorithmAnalyzerStore = dataAnalyzeStore.algorithmAnalyzerStore;
  const { t } = useTranslation();

  const formWidth = calcAlgorithmFormWidth(
    graphManagementStore.isExpanded,
    340,
    390
  );

  const sourceType = algorithmAnalyzerStore.modelSimilarityParams.method;

  const allowAddNewProperty = !flatten(
    algorithmAnalyzerStore.modelSimilarityParams.vertexProperty
  ).some((value) => value === '');

  const addNewPropertyClassName =
    allowAddNewProperty &&
    dataAnalyzeStore.requestStatus.fetchGraphs !== 'pending'
      ? 'query-tab-content-internal-expand-manipulation-enable'
      : 'query-tab-content-internal-expand-manipulation-disabled';

  const isValidSourceId =
    algorithmAnalyzerStore.modelSimilarityParams.method === 'id' &&
    algorithmAnalyzerStore.modelSimilarityParams.source !== '';

  const isValidProperty =
    algorithmAnalyzerStore.modelSimilarityParams.method === 'property' &&
    !(
      isEmpty(algorithmAnalyzerStore.modelSimilarityParams.vertexType) &&
      flatten(
        algorithmAnalyzerStore.modelSimilarityParams.vertexProperty
      ).every((value) => isEmpty(value))
    ) &&
    algorithmAnalyzerStore.modelSimilarityParams.vertexProperty.every(
      ([key, value]) => (!isEmpty(key) ? !isEmpty(value) : true)
    );

  const isValidExec =
    Object.values(
      algorithmAnalyzerStore.validateModelSimilartiyParamsErrorMessage
    ).every((value) => value === '') &&
    algorithmAnalyzerStore.modelSimilarityParams.least_neighbor !== '' &&
    algorithmAnalyzerStore.modelSimilarityParams.similarity !== '' &&
    (!isEmpty(algorithmAnalyzerStore.modelSimilarityParams.property_filter)
      ? algorithmAnalyzerStore.modelSimilarityParams.least_property_number !==
        ''
      : true) &&
    (isValidSourceId || isValidProperty);

  return (
    <div style={{ display: 'flex' }}>
      <div className="query-tab-content-form" style={{ width: '50%' }}>
        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div
              className="query-tab-content-form-item-title large"
              style={{ lineHeight: '32px' }}
            >
              <i>*</i>
              <span>
                {t(
                  'data-analyze.algorithm-forms.model-similarity.options.method'
                )}
              </span>
            </div>
            <Radio.Group
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              value={algorithmAnalyzerStore.modelSimilarityParams.method}
              onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                algorithmAnalyzerStore.switchModelSimilarityMethod(
                  e.target.value
                );
              }}
            >
              <Radio value="id">
                {t(
                  'data-analyze.algorithm-forms.model-similarity.radio-value.specific-id'
                )}
              </Radio>
              <Radio value="property">
                {t(
                  'data-analyze.algorithm-forms.model-similarity.radio-value.filtered-type-property'
                )}
              </Radio>
            </Radio.Group>
          </div>
        </div>

        {sourceType === 'id' && (
          <div className="query-tab-content-form-row">
            <div className="query-tab-content-form-item">
              <div className="query-tab-content-form-item-title large">
                <i>*</i>
                <span>
                  {t(
                    'data-analyze.algorithm-forms.model-similarity.options.source'
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
                  'data-analyze.algorithm-forms.model-similarity.placeholder.input-source-id'
                )}
                errorLocation="layer"
                errorMessage={
                  algorithmAnalyzerStore
                    .validateModelSimilartiyParamsErrorMessage.source
                }
                value={algorithmAnalyzerStore.modelSimilarityParams.source}
                onChange={(e: any) => {
                  algorithmAnalyzerStore.mutateModelSimilarityParams(
                    'source',
                    e.value as string
                  );

                  algorithmAnalyzerStore.validateModelSimilarityParams(
                    'source'
                  );
                }}
                originInputProps={{
                  onBlur() {
                    algorithmAnalyzerStore.validateModelSimilarityParams(
                      'source'
                    );
                  }
                }}
              />
            </div>
          </div>
        )}

        {sourceType !== 'id' && (
          <>
            <div className="query-tab-content-form-row">
              <div className="query-tab-content-form-item">
                <div className="query-tab-content-form-item-title large">
                  <span>
                    {t(
                      'data-analyze.algorithm-forms.model-similarity.options.vertex-type'
                    )}
                  </span>
                </div>
                <Select
                  size="medium"
                  trigger="click"
                  selectorName={t(
                    'data-analyze.algorithm-forms.model-similarity.placeholder.input-vertex-type'
                  )}
                  value={
                    algorithmAnalyzerStore.modelSimilarityParams.vertexType
                  }
                  notFoundContent={t(
                    'data-analyze.algorithm-forms.model-similarity.placeholder.no-vertex-type'
                  )}
                  disabled={
                    dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                  }
                  width={formWidth}
                  onChange={(value: string) => {
                    // switch property selections from properties of index to certain properties
                    if (
                      isEmpty(
                        algorithmAnalyzerStore.modelSimilarityParams.vertexType
                      )
                    ) {
                      algorithmAnalyzerStore.mutateModelSimilarityParams(
                        'vertexProperty',
                        [['', '']]
                      );
                    }

                    algorithmAnalyzerStore.mutateModelSimilarityParams(
                      'vertexType',
                      value
                    );
                  }}
                >
                  {dataAnalyzeStore.vertexTypes.map(({ name }) => (
                    <Select.Option value={name} key={name}>
                      {name}
                    </Select.Option>
                  ))}
                </Select>
              </div>
            </div>

            <div
              className="query-tab-content-form-row"
              style={{ marginBottom: 0 }}
            >
              <div
                className="query-tab-content-form-item"
                style={{ alignItems: 'start' }}
              >
                <div
                  className="query-tab-content-form-item-title large"
                  style={{ lineHeight: '32px' }}
                >
                  <span>
                    {t(
                      'data-analyze.algorithm-forms.model-similarity.options.vertex-property'
                    )}
                  </span>
                </div>

                <div>
                  {algorithmAnalyzerStore.modelSimilarityParams.vertexProperty.map(
                    ([key, value], propertyIndex) => {
                      const currentVertexType = dataAnalyzeStore.vertexTypes.find(
                        ({ name }) =>
                          name ===
                          algorithmAnalyzerStore.modelSimilarityParams
                            .vertexType
                      );

                      return (
                        <div className="query-tab-content-internal-expand-row">
                          <Select
                            width={161}
                            size="medium"
                            trigger="click"
                            placeholder={t(
                              'data-analyze.algorithm-forms.model-similarity.placeholder.select-vertex-property'
                            )}
                            value={key}
                            notFoundContent={t(
                              'data-analyze.algorithm-forms.model-similarity.placeholder.no-properties'
                            )}
                            disabled={
                              dataAnalyzeStore.requestStatus.fetchGraphs ===
                              'pending'
                            }
                            onChange={(value: string) => {
                              const vertexProperty = cloneDeep(
                                algorithmAnalyzerStore.modelSimilarityParams
                                  .vertexProperty
                              );

                              vertexProperty[propertyIndex][0] = value;

                              algorithmAnalyzerStore.mutateModelSimilarityParams(
                                'vertexProperty',
                                vertexProperty
                              );
                            }}
                          >
                            {!isEmpty(
                              algorithmAnalyzerStore.modelSimilarityParams
                                .vertexType
                            )
                              ? flatten(
                                  currentVertexType?.property_indexes.map(
                                    ({ fields }) => fields
                                  )
                                )
                                  .concat(currentVertexType!.primary_keys)
                                  .filter(
                                    (property) =>
                                      !algorithmAnalyzerStore.modelSimilarityParams.vertexProperty
                                        .map(([key]) => key)
                                        .includes(property)
                                  )
                                  .map((property) => (
                                    <Select.Option
                                      value={property}
                                      key={property}
                                    >
                                      {property}
                                    </Select.Option>
                                  ))
                              : algorithmAnalyzerStore.allPropertyIndexProperty.map(
                                  (property) => (
                                    <Select.Option
                                      value={property}
                                      key={property}
                                    >
                                      {property}
                                    </Select.Option>
                                  )
                                )}
                          </Select>
                          <div className="query-tab-content-internal-expand-input">
                            <Input
                              width={161}
                              size="medium"
                              disabled={
                                dataAnalyzeStore.requestStatus.fetchGraphs ===
                                'pending'
                              }
                              placeholder={t(
                                'data-analyze.algorithm-forms.model-similarity.placeholder.input-vertex-property'
                              )}
                              errorLocation="layer"
                              errorMessage={
                                algorithmAnalyzerStore
                                  .validateModelSimilartiyParamsErrorMessage
                                  .vertexProperty
                              }
                              value={value}
                              onChange={(e: any) => {
                                const vertexProperty = cloneDeep(
                                  algorithmAnalyzerStore.modelSimilarityParams
                                    .vertexProperty
                                );

                                vertexProperty[propertyIndex][1] = e.value;

                                algorithmAnalyzerStore.mutateModelSimilarityParams(
                                  'vertexProperty',
                                  vertexProperty
                                );

                                algorithmAnalyzerStore.validateModelSimilarityParams(
                                  'vertexProperty'
                                );
                              }}
                              originInputProps={{
                                onBlur() {
                                  algorithmAnalyzerStore.validateModelSimilarityParams(
                                    'vertexProperty'
                                  );
                                }
                              }}
                            />
                          </div>
                          {size(
                            algorithmAnalyzerStore.modelSimilarityParams
                              .vertexProperty
                          ) > 1 && (
                            <div
                              style={{
                                fontSize: 14,
                                lineHeight: '32px',
                                color: '#2b65ff',
                                cursor: 'pointer'
                              }}
                              onClick={() => {
                                algorithmAnalyzerStore.removeModelSimilarityVertexProperty(
                                  propertyIndex
                                );
                              }}
                            >
                              {t(
                                'data-analyze.algorithm-forms.model-similarity.delete'
                              )}
                            </div>
                          )}
                        </div>
                      );
                    }
                  )}
                </div>
              </div>
            </div>
            {algorithmAnalyzerStore.modelSimilarityParams.vertexType === '' &&
            !allowAddNewProperty &&
            size(
              algorithmAnalyzerStore.modelSimilarityParams.vertexProperty
            ) === 1 ? (
              <div
                className="query-tab-algorithm-hint"
                style={{ margin: '0 0 16px 172px' }}
              >
                <span>
                  {t(
                    'data-analyze.algorithm-forms.model-similarity.hint.vertex_type_or_property'
                  )}
                </span>
              </div>
            ) : (
              <div className={addNewPropertyClassName}>
                <span
                  onClick={() => {
                    if (
                      allowAddNewProperty &&
                      dataAnalyzeStore.requestStatus.fetchGraphs !== 'pending'
                    ) {
                      algorithmAnalyzerStore.addModelSimilarityVertexProperty();
                    }
                  }}
                >
                  {t('data-analyze.algorithm-forms.model-similarity.add')}
                </span>
              </div>
            )}
          </>
        )}

        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div className="query-tab-content-form-item-title large">
              <i>*</i>
              <span>
                {t(
                  'data-analyze.algorithm-forms.model-similarity.options.direction'
                )}
              </span>
            </div>
            <Radio.Group
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              value={algorithmAnalyzerStore.modelSimilarityParams.direction}
              onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                algorithmAnalyzerStore.mutateModelSimilarityParams(
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

        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div className="query-tab-content-form-item-title large">
              <i>*</i>
              <span>
                {t(
                  'data-analyze.algorithm-forms.model-similarity.options.least_neighbor'
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
                  'data-analyze.algorithm-forms.model-similarity.hint.least_neighbor'
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
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              placeholder={t(
                'data-analyze.algorithm-forms.model-similarity.placeholder.input-positive-integer'
              )}
              errorLocation="layer"
              errorMessage={
                algorithmAnalyzerStore.validateModelSimilartiyParamsErrorMessage
                  .least_neighbor
              }
              value={
                algorithmAnalyzerStore.modelSimilarityParams.least_neighbor
              }
              onChange={(e: any) => {
                algorithmAnalyzerStore.mutateModelSimilarityParams(
                  'least_neighbor',
                  e.value as string
                );

                algorithmAnalyzerStore.validateModelSimilarityParams(
                  'least_neighbor'
                );
              }}
              originInputProps={{
                onBlur() {
                  algorithmAnalyzerStore.validateModelSimilarityParams(
                    'least_neighbor'
                  );
                }
              }}
            />
          </div>
        </div>

        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div className="query-tab-content-form-item-title large">
              <i>*</i>
              <span>
                {t(
                  'data-analyze.algorithm-forms.model-similarity.options.similarity'
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
                  'data-analyze.algorithm-forms.model-similarity.hint.similarity'
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
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              placeholder={t(
                'data-analyze.algorithm-forms.model-similarity.placeholder.similarity'
              )}
              errorLocation="layer"
              errorMessage={
                algorithmAnalyzerStore.validateModelSimilartiyParamsErrorMessage
                  .similarity
              }
              value={algorithmAnalyzerStore.modelSimilarityParams.similarity}
              onChange={(e: any) => {
                algorithmAnalyzerStore.mutateModelSimilarityParams(
                  'similarity',
                  e.value as string
                );

                algorithmAnalyzerStore.validateModelSimilarityParams(
                  'similarity'
                );
              }}
              originInputProps={{
                onBlur() {
                  algorithmAnalyzerStore.validateModelSimilarityParams(
                    'similarity'
                  );
                }
              }}
            />
          </div>
        </div>

        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div className="query-tab-content-form-item-title large">
              <span>
                {t(
                  'data-analyze.algorithm-forms.model-similarity.options.label'
                )}
              </span>
            </div>
            <Select
              size="medium"
              trigger="click"
              value={algorithmAnalyzerStore.modelSimilarityParams.label}
              notFoundContent={t(
                'data-analyze.algorithm-forms.model-similarity.placeholder.no-edge-types'
              )}
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              width={formWidth}
              onChange={(value: string) => {
                algorithmAnalyzerStore.mutateModelSimilarityParams(
                  'label',
                  value
                );
              }}
            >
              <Select.Option value="__all__" key="__all__">
                {t('data-analyze.algorithm-forms.model-similarity.pre-value')}
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
            <div className="query-tab-content-form-item-title large">
              <span>
                {t(
                  'data-analyze.algorithm-forms.model-similarity.options.max_similar'
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
                  'data-analyze.algorithm-forms.model-similarity.hint.max_similar'
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
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              placeholder={t(
                'data-analyze.algorithm-forms.model-similarity.placeholder.input-integer'
              )}
              errorLocation="layer"
              errorMessage={
                algorithmAnalyzerStore.validateModelSimilartiyParamsErrorMessage
                  .max_similar
              }
              value={algorithmAnalyzerStore.modelSimilarityParams.max_similar}
              onChange={(e: any) => {
                algorithmAnalyzerStore.mutateModelSimilarityParams(
                  'max_similar',
                  e.value as string
                );

                algorithmAnalyzerStore.validateModelSimilarityParams(
                  'max_similar'
                );
              }}
              originInputProps={{
                onBlur() {
                  algorithmAnalyzerStore.validateModelSimilarityParams(
                    'max_similar'
                  );
                }
              }}
            />
          </div>
        </div>

        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div className="query-tab-content-form-item-title large">
              <span>
                {t(
                  'data-analyze.algorithm-forms.model-similarity.options.least_similar'
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
                'data-analyze.algorithm-forms.model-similarity.placeholder.input-positive-integer'
              )}
              errorLocation="layer"
              errorMessage={
                algorithmAnalyzerStore.validateModelSimilartiyParamsErrorMessage
                  .least_similar
              }
              value={algorithmAnalyzerStore.modelSimilarityParams.least_similar}
              onChange={(e: any) => {
                algorithmAnalyzerStore.mutateModelSimilarityParams(
                  'least_similar',
                  e.value as string
                );

                algorithmAnalyzerStore.validateModelSimilarityParams(
                  'least_similar'
                );
              }}
              originInputProps={{
                onBlur() {
                  algorithmAnalyzerStore.validateModelSimilarityParams(
                    'least_similar'
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
                url: 'fsimilarity',
                type: Algorithm.modelSimilarity
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
              algorithmAnalyzerStore.resetModelSimilarityParams();
            }}
          >
            {t('data-analyze.manipulations.reset')}
          </Button>
        </div>
      </div>

      <div className="query-tab-content-form" style={{ width: '50%' }}>
        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div className="query-tab-content-form-item-title large">
              <span>
                {t(
                  'data-analyze.algorithm-forms.model-similarity.options.property_filter'
                )}
              </span>
            </div>
            <Select
              size="medium"
              trigger="click"
              selectorName={t(
                'data-analyze.algorithm-forms.model-similarity.placeholder.input-filtered-property'
              )}
              value={
                algorithmAnalyzerStore.modelSimilarityParams.property_filter
              }
              notFoundContent={t(
                'data-analyze.algorithm-forms.model-similarity.placeholder.no-properties'
              )}
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              width={formWidth}
              onChange={(value: string) => {
                algorithmAnalyzerStore.mutateModelSimilarityParams(
                  'property_filter',
                  value
                );
              }}
            >
              {dataAnalyzeStore.properties.map(({ name }) => (
                <Select.Option value={name} key={name}>
                  {name}
                </Select.Option>
              ))}
            </Select>
          </div>
        </div>

        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div className="query-tab-content-form-item-title large">
              <span>
                {t(
                  'data-analyze.algorithm-forms.model-similarity.options.least_property_number'
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
                  'data-analyze.algorithm-forms.model-similarity.hint.least_property_number'
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
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              placeholder={t(
                'data-analyze.algorithm-forms.model-similarity.placeholder.input-integer-gt-1'
              )}
              errorLocation="layer"
              errorMessage={
                algorithmAnalyzerStore.validateModelSimilartiyParamsErrorMessage
                  .least_property_number
              }
              value={
                algorithmAnalyzerStore.modelSimilarityParams
                  .least_property_number
              }
              onChange={(e: any) => {
                algorithmAnalyzerStore.mutateModelSimilarityParams(
                  'least_property_number',
                  e.value as string
                );

                algorithmAnalyzerStore.validateModelSimilarityParams(
                  'least_property_number'
                );
              }}
              originInputProps={{
                onBlur() {
                  algorithmAnalyzerStore.validateModelSimilarityParams(
                    'least_property_number'
                  );
                }
              }}
            />
          </div>
        </div>

        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div className="query-tab-content-form-item-title large">
              <span>
                {t(
                  'data-analyze.algorithm-forms.model-similarity.options.max_degree'
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
                  'data-analyze.algorithm-forms.model-similarity.hint.max-degree'
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
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              placeholder={t(
                'data-analyze.algorithm-forms.model-similarity.placeholder.input-positive-integer-or-negative-one-max-degree'
              )}
              errorLocation="layer"
              errorMessage={
                algorithmAnalyzerStore.validateModelSimilartiyParamsErrorMessage
                  .max_degree
              }
              value={algorithmAnalyzerStore.modelSimilarityParams.max_degree}
              onChange={(e: any) => {
                algorithmAnalyzerStore.mutateModelSimilarityParams(
                  'max_degree',
                  e.value as string
                );

                algorithmAnalyzerStore.validateModelSimilarityParams(
                  'max_degree'
                );
              }}
              originInputProps={{
                onBlur() {
                  algorithmAnalyzerStore.validateModelSimilarityParams(
                    'max_degree'
                  );
                }
              }}
            />
          </div>
        </div>

        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div className="query-tab-content-form-item-title large">
              <span>
                {t(
                  'data-analyze.algorithm-forms.model-similarity.options.capacity'
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
                'data-analyze.algorithm-forms.model-similarity.placeholder.input-positive-integer-or-negative-one-capacity'
              )}
              errorLocation="layer"
              errorMessage={
                algorithmAnalyzerStore.validateModelSimilartiyParamsErrorMessage
                  .capacity
              }
              value={algorithmAnalyzerStore.modelSimilarityParams.capacity}
              onChange={(e: any) => {
                algorithmAnalyzerStore.mutateModelSimilarityParams(
                  'capacity',
                  e.value as string
                );

                algorithmAnalyzerStore.validateModelSimilarityParams(
                  'capacity'
                );
              }}
              originInputProps={{
                onBlur() {
                  algorithmAnalyzerStore.validateModelSimilarityParams(
                    'capacity'
                  );
                }
              }}
            />
          </div>
        </div>

        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div className="query-tab-content-form-item-title large">
              <span>
                {t(
                  'data-analyze.algorithm-forms.model-similarity.options.limit'
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
                'data-analyze.algorithm-forms.model-similarity.placeholder.input-positive-integer-or-negative-one-limit'
              )}
              errorLocation="layer"
              errorMessage={
                algorithmAnalyzerStore.validateModelSimilartiyParamsErrorMessage
                  .limit
              }
              value={algorithmAnalyzerStore.modelSimilarityParams.limit}
              onChange={(e: any) => {
                algorithmAnalyzerStore.mutateModelSimilarityParams(
                  'limit',
                  e.value as string
                );

                algorithmAnalyzerStore.validateModelSimilarityParams('limit');
              }}
              originInputProps={{
                onBlur() {
                  algorithmAnalyzerStore.validateModelSimilarityParams('limit');
                }
              }}
            />
          </div>
        </div>

        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div className="query-tab-content-form-item-title large">
              <span>
                {t(
                  'data-analyze.algorithm-forms.model-similarity.options.return_common_connection'
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
                  'data-analyze.algorithm-forms.model-similarity.hint.return_common_connection'
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
            <div style={{ width: formWidth }}>
              <Switch
                width={40}
                size="medium"
                disabled={
                  dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                }
                checked={
                  algorithmAnalyzerStore.modelSimilarityParams
                    .return_common_connection
                }
                onChange={(checked: boolean) => {
                  algorithmAnalyzerStore.mutateModelSimilarityParams(
                    'return_common_connection',
                    checked
                  );
                }}
              />
            </div>
          </div>
        </div>

        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div className="query-tab-content-form-item-title large">
              <span>
                {t(
                  'data-analyze.algorithm-forms.model-similarity.options.return_complete_info'
                )}
              </span>
            </div>
            <div style={{ width: formWidth }}>
              <Switch
                width={40}
                size="medium"
                disabled={
                  dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                }
                checked={
                  algorithmAnalyzerStore.modelSimilarityParams
                    .return_complete_info
                }
                onChange={(checked: boolean) => {
                  algorithmAnalyzerStore.mutateModelSimilarityParams(
                    'return_complete_info',
                    checked
                  );
                }}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
});

export default ModelSimilarity;
