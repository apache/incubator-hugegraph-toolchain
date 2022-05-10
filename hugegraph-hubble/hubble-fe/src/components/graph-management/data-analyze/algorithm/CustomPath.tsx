import React, { useContext } from 'react';
import { observer } from 'mobx-react';
import {
  size,
  flatten,
  flattenDeep,
  uniq,
  isEmpty,
  cloneDeep,
  fromPairs
} from 'lodash-es';
import { Button, Radio, Input, Select } from 'hubble-ui';
import { useTranslation } from 'react-i18next';

import { styles } from '../QueryAndAlgorithmLibrary';
import { GraphManagementStoreContext } from '../../../../stores';
import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';
import { Algorithm } from '../../../../stores/factory/dataAnalyzeStore/algorithmStore';
import { isDataTypeNumeric, calcAlgorithmFormWidth } from '../../../../utils';

const CustomPath = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const dataAnalyzeStore = useContext(DataAnalyzeStore);
  const { t } = useTranslation();
  const algorithmAnalyzerStore = dataAnalyzeStore.algorithmAnalyzerStore;

  const formWidth = calcAlgorithmFormWidth(
    graphManagementStore.isExpanded,
    340,
    390
  );

  const formWidthInStep = calcAlgorithmFormWidth(
    graphManagementStore.isExpanded,
    310,
    380
  );

  const formSmallWidthInStep = calcAlgorithmFormWidth(
    graphManagementStore.isExpanded,
    150,
    160
  );

  const sourceType = algorithmAnalyzerStore.customPathParams.method;

  const allowAddNewProperty = !flatten(
    algorithmAnalyzerStore.customPathParams.vertexProperty
  ).some((value) => value === '');

  const addNewPropertyClassName =
    allowAddNewProperty &&
    dataAnalyzeStore.requestStatus.fetchGraphs !== 'pending'
      ? 'query-tab-content-internal-expand-manipulation-enable'
      : 'query-tab-content-internal-expand-manipulation-disabled';

  const allowAddNewRuleProperty = !flattenDeep(
    algorithmAnalyzerStore.customPathParams.steps.map(
      ({ properties }) => properties
    )
  ).some((value) => value === '');

  const addNewRulePropertyClassName =
    allowAddNewRuleProperty &&
    dataAnalyzeStore.requestStatus.fetchGraphs !== 'pending'
      ? 'query-tab-content-internal-expand-manipulation-enable'
      : 'query-tab-content-internal-expand-manipulation-disabled';

  const isValidSourceId =
    algorithmAnalyzerStore.customPathParams.method === 'id' &&
    algorithmAnalyzerStore.customPathParams.source !== '';

  const isValidProperty =
    algorithmAnalyzerStore.customPathParams.method === 'property' &&
    !(
      isEmpty(algorithmAnalyzerStore.customPathParams.vertexType) &&
      flatten(
        algorithmAnalyzerStore.customPathParams.vertexProperty
      ).every((value) => isEmpty(value))
    ) &&
    algorithmAnalyzerStore.customPathParams.vertexProperty.every(
      ([key, value]) => (!isEmpty(key) ? !isEmpty(value) : true)
    );

  const isValidateRuleProperties = algorithmAnalyzerStore.customPathParams.steps.every(
    ({ properties }) => {
      if (size(properties) === 1) {
        return (
          (isEmpty(properties[0][0]) && isEmpty(properties[0][1])) ||
          (!isEmpty(properties[0][0]) && !isEmpty(properties[0][1]))
        );
      } else {
        return properties.every(([, value]) => !isEmpty(value));
      }
    }
  );

  const isValidRuleWeight = algorithmAnalyzerStore.customPathParams.steps.every(
    ({ weight_by, default_weight }) =>
      algorithmAnalyzerStore.customPathParams.sort_by === 'NONE' ||
      (weight_by === '__CUSTOM_WEIGHT__' && default_weight !== '') ||
      (weight_by !== '__CUSTOM_WEIGHT__' && !isEmpty(weight_by))
  );

  const isValidExec =
    Object.values(
      algorithmAnalyzerStore.validateCustomPathParmasErrorMessage
    ).every((value) => Array.isArray(value) || value === '') &&
    algorithmAnalyzerStore.validateCustomPathParmasErrorMessage.steps.every(
      (step) => Object.values(step).every((value) => value === '')
    ) &&
    (algorithmAnalyzerStore.customPathParams.method === 'id'
      ? algorithmAnalyzerStore.customPathParams.source !== ''
      : true) &&
    (isValidSourceId || isValidProperty) &&
    isValidateRuleProperties &&
    isValidRuleWeight;

  const isValidAddRule = algorithmAnalyzerStore.validateCustomPathParmasErrorMessage.steps.every(
    (step) => Object.values(step).every((value) => value === '')
  );

  return (
    <div style={{ display: 'flex' }}>
      <div className="query-tab-content-form" style={{ width: '50%' }}>
        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div className="query-tab-content-form-item-title large">
              <i>*</i>
              <span>
                {t('data-analyze.algorithm-forms.custom-path.options.method')}
              </span>
            </div>
            <Radio.Group
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              value={algorithmAnalyzerStore.customPathParams.method}
              onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                algorithmAnalyzerStore.switchCustomPathMethod(e.target.value);
              }}
            >
              <Radio value="id">
                {t(
                  'data-analyze.algorithm-forms.custom-path.radio-value.specific-id'
                )}
              </Radio>
              <Radio value="property">
                {t(
                  'data-analyze.algorithm-forms.custom-path.radio-value.filtered-type-property'
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
                  {t('data-analyze.algorithm-forms.custom-path.options.source')}
                </span>
              </div>

              <Input
                width={formWidth}
                size="medium"
                disabled={
                  dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                }
                placeholder={t(
                  'data-analyze.algorithm-forms.custom-path.placeholder.input-source-id'
                )}
                errorLocation="layer"
                errorMessage={
                  algorithmAnalyzerStore.validateCustomPathParmasErrorMessage
                    .source
                }
                value={algorithmAnalyzerStore.customPathParams.source}
                onChange={(e: any) => {
                  algorithmAnalyzerStore.mutateCustomPathParams(
                    'source',
                    e.value as string
                  );

                  algorithmAnalyzerStore.validateCustomPathParams('source');
                }}
                originInputProps={{
                  onBlur() {
                    algorithmAnalyzerStore.validateCustomPathParams('source');
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
                      'data-analyze.algorithm-forms.custom-path.options.vertex-type'
                    )}
                  </span>
                </div>
                <Select
                  size="medium"
                  trigger="click"
                  selectorName={t(
                    'data-analyze.algorithm-forms.custom-path.placeholder.select-vertex-type'
                  )}
                  value={algorithmAnalyzerStore.customPathParams.vertexType}
                  notFoundContent={t(
                    'data-analyze.algorithm-forms.custom-path.placeholder.no-vertex-type'
                  )}
                  disabled={
                    dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                  }
                  width={formWidth}
                  onChange={(value: string) => {
                    // switch property selections from properties of index to certain properties
                    if (
                      isEmpty(
                        algorithmAnalyzerStore.customPathParams.vertexType
                      )
                    ) {
                      algorithmAnalyzerStore.mutateCustomPathParams(
                        'vertexProperty',
                        [['', '']]
                      );
                    }

                    algorithmAnalyzerStore.mutateCustomPathParams(
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
                      'data-analyze.algorithm-forms.custom-path.options.vertex-property'
                    )}
                  </span>
                </div>

                <div>
                  {algorithmAnalyzerStore.customPathParams.vertexProperty.map(
                    ([key, value], propertyIndex) => {
                      const currentVertexType = dataAnalyzeStore.vertexTypes.find(
                        ({ name }) =>
                          name ===
                          algorithmAnalyzerStore.customPathParams.vertexType
                      );

                      return (
                        <div className="query-tab-content-internal-expand-row">
                          <Select
                            width={161}
                            size="medium"
                            trigger="click"
                            selectorName={t(
                              'data-analyze.algorithm-forms.custom-path.placeholder.select-vertex-property'
                            )}
                            value={key}
                            notFoundContent={t(
                              'data-analyze.algorithm-forms.custom-path.placeholder.no-properties'
                            )}
                            disabled={
                              dataAnalyzeStore.requestStatus.fetchGraphs ===
                              'pending'
                            }
                            onChange={(value: string) => {
                              const vertexProperty = cloneDeep(
                                algorithmAnalyzerStore.customPathParams
                                  .vertexProperty
                              );

                              vertexProperty[propertyIndex][0] = value;

                              algorithmAnalyzerStore.mutateCustomPathParams(
                                'vertexProperty',
                                vertexProperty
                              );
                            }}
                          >
                            {!isEmpty(
                              algorithmAnalyzerStore.customPathParams.vertexType
                            )
                              ? flatten(
                                  currentVertexType?.property_indexes.map(
                                    ({ fields }) => fields
                                  )
                                )
                                  .concat(currentVertexType!.primary_keys)
                                  .filter(
                                    (property) =>
                                      !algorithmAnalyzerStore.customPathParams.vertexProperty
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
                                'data-analyze.algorithm-forms.custom-path.placeholder.input-multiple-properties'
                              )}
                              errorLocation="layer"
                              errorMessage={
                                algorithmAnalyzerStore
                                  .validateCustomPathParmasErrorMessage
                                  .vertexProperty
                              }
                              value={value}
                              onChange={(e: any) => {
                                const vertexProperty = cloneDeep(
                                  algorithmAnalyzerStore.customPathParams
                                    .vertexProperty
                                );

                                vertexProperty[propertyIndex][1] = e.value;

                                algorithmAnalyzerStore.mutateCustomPathParams(
                                  'vertexProperty',
                                  vertexProperty
                                );

                                algorithmAnalyzerStore.validateCustomPathParams(
                                  'vertexProperty'
                                );
                              }}
                              originInputProps={{
                                onBlur() {
                                  algorithmAnalyzerStore.validateCustomPathParams(
                                    'vertexProperty'
                                  );
                                }
                              }}
                            />
                          </div>
                          {size(
                            algorithmAnalyzerStore.customPathParams
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
                                algorithmAnalyzerStore.removeCustomPathVertexProperty(
                                  propertyIndex
                                );
                              }}
                            >
                              {t(
                                'data-analyze.algorithm-forms.custom-path.delete'
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
            {algorithmAnalyzerStore.customPathParams.vertexType === '' &&
            !allowAddNewProperty &&
            size(algorithmAnalyzerStore.customPathParams.vertexProperty) ===
              1 ? (
              <div
                className="query-tab-algorithm-hint"
                style={{ margin: '0 0 16px 172px' }}
              >
                <span>
                  {t(
                    'data-analyze.algorithm-forms.custom-path.hint.vertex_type_or_property'
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
                      algorithmAnalyzerStore.addCustomPathVertexProperty();
                    }
                  }}
                >
                  {t('data-analyze.algorithm-forms.custom-path.add')}
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
                {t('data-analyze.algorithm-forms.custom-path.options.sort_by')}
              </span>
            </div>
            <Radio.Group
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              value={algorithmAnalyzerStore.customPathParams.sort_by}
              onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                algorithmAnalyzerStore.mutateCustomPathParams(
                  'sort_by',
                  e.target.value
                );

                if (e.target.value === 'NONE') {
                  algorithmAnalyzerStore.customPathParams.steps.forEach(
                    (_, ruleIndex) => {
                      algorithmAnalyzerStore.mutateCustomPathRuleParams(
                        'weight_by',
                        '',
                        ruleIndex
                      );

                      algorithmAnalyzerStore.mutateCustomPathRuleParams(
                        'default_weight',
                        '',
                        ruleIndex
                      );
                    }
                  );
                }
              }}
            >
              <Radio value="NONE">
                {t('data-analyze.algorithm-forms.custom-path.radio-value.none')}
              </Radio>
              <Radio value="INCR">
                {t(
                  'data-analyze.algorithm-forms.custom-path.radio-value.ascend'
                )}
              </Radio>
              <Radio value="DECR">
                {t(
                  'data-analyze.algorithm-forms.custom-path.radio-value.descend'
                )}
              </Radio>
            </Radio.Group>
          </div>
        </div>
        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div className="query-tab-content-form-item-title large">
              <span>
                {t('data-analyze.algorithm-forms.custom-path.options.capacity')}
              </span>
            </div>
            <Input
              width={formWidth}
              size="medium"
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              placeholder={t(
                'data-analyze.algorithm-forms.custom-path.placeholder.input-positive-integer-or-negative-one-capacity'
              )}
              errorLocation="layer"
              errorMessage={
                algorithmAnalyzerStore.validateCustomPathParmasErrorMessage
                  .capacity
              }
              value={algorithmAnalyzerStore.customPathParams.capacity}
              onChange={(e: any) => {
                algorithmAnalyzerStore.mutateCustomPathParams(
                  'capacity',
                  e.value as string
                );

                algorithmAnalyzerStore.validateCustomPathParams('capacity');
              }}
              originInputProps={{
                onBlur() {
                  algorithmAnalyzerStore.validateCustomPathParams('capacity');
                }
              }}
            />
          </div>
        </div>
        <div className="query-tab-content-form-row">
          <div className="query-tab-content-form-item">
            <div className="query-tab-content-form-item-title large">
              <span>
                {t('data-analyze.algorithm-forms.custom-path.options.limit')}
              </span>
            </div>
            <Input
              width={formWidth}
              size="medium"
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              placeholder={t(
                'data-analyze.algorithm-forms.custom-path.placeholder.input-positive-integer-or-negative-one-limit'
              )}
              errorLocation="layer"
              errorMessage={
                algorithmAnalyzerStore.validateCustomPathParmasErrorMessage
                  .limit
              }
              value={algorithmAnalyzerStore.customPathParams.limit}
              onChange={(e: any) => {
                algorithmAnalyzerStore.mutateCustomPathParams(
                  'limit',
                  e.value as string
                );

                algorithmAnalyzerStore.validateCustomPathParams('limit');
              }}
              originInputProps={{
                onBlur() {
                  algorithmAnalyzerStore.validateCustomPathParams('limit');
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
                url: 'customizedpaths',
                type: Algorithm.customPath
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
              algorithmAnalyzerStore.resetCustomPathParams();
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
        {algorithmAnalyzerStore.customPathParams.steps.map(
          (
            {
              uuid,
              direction,
              labels,
              degree,
              sample,
              properties,
              weight_by,
              default_weight
            },
            ruleIndex
          ) => {
            return (
              <div className="query-tab-content-form-expand-items" key={uuid}>
                <div className="query-tab-content-form-expand-item">
                  <div className="query-tab-content-form-item-title query-tab-content-form-expand-title">
                    <i>*</i>
                    <span>
                      {t(
                        'data-analyze.algorithm-forms.custom-path.options.direction'
                      )}
                    </span>
                  </div>
                  <Radio.Group
                    disabled={
                      dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                    }
                    value={direction}
                    onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                      algorithmAnalyzerStore.mutateCustomPathRuleParams(
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
                  {size(algorithmAnalyzerStore.customPathParams.steps) > 1 && (
                    <div
                      style={{
                        marginLeft: 175,
                        fontSize: 14,
                        color: '#2b65ff',
                        cursor: 'pointer',
                        lineHeight: '22px'
                      }}
                      onClick={() => {
                        algorithmAnalyzerStore.removeCustomPathRule(ruleIndex);
                      }}
                    >
                      删除
                    </div>
                  )}
                </div>
                <div className="query-tab-content-form-expand-item">
                  <div className="query-tab-content-form-item-title query-tab-content-form-expand-title">
                    <span>
                      {t(
                        'data-analyze.algorithm-forms.custom-path.options.labels'
                      )}
                    </span>
                  </div>
                  <Select
                    size="medium"
                    trigger="click"
                    value={labels}
                    mode="multiple"
                    showSearch={false}
                    placeholder={t(
                      'data-analyze.algorithm-forms.custom-path.placeholder.select-edge-type'
                    )}
                    notFoundContent={t(
                      'data-analyze.algorithm-forms.custom-path.placeholder.no-edge-type'
                    )}
                    disabled={
                      dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                    }
                    width={formWidthInStep}
                    onChange={(value: string[]) => {
                      algorithmAnalyzerStore.mutateCustomPathRuleParams(
                        'labels',
                        value,
                        ruleIndex
                      );
                    }}
                  >
                    {dataAnalyzeStore.edgeTypes.map(({ name }) => (
                      <Select.Option value={name} key={name}>
                        {name}
                      </Select.Option>
                    ))}
                  </Select>
                </div>

                <div
                  className="query-tab-content-form-expand-item"
                  style={{ alignItems: 'start', marginBottom: 16 }}
                >
                  <div
                    className="query-tab-content-form-item-title query-tab-content-form-expand-title"
                    style={{ lineHeight: '32px' }}
                  >
                    <span>
                      {t(
                        'data-analyze.algorithm-forms.custom-path.options.properties'
                      )}
                    </span>
                  </div>
                  <div>
                    <>
                      {properties.map(([key, value], propertyIndex) => {
                        return (
                          <div
                            style={{ display: 'flex', marginBottom: 16 }}
                            key={key}
                          >
                            <div style={{ marginRight: 8 }}>
                              <Select
                                size="medium"
                                trigger="click"
                                value={key}
                                selectorName={t(
                                  'data-analyze.algorithm-forms.custom-path.placeholder.select-edge-property'
                                )}
                                notFoundContent={t(
                                  'data-analyze.algorithm-forms.custom-path.placeholder.no-edge-property'
                                )}
                                disabled={
                                  dataAnalyzeStore.requestStatus.fetchGraphs ===
                                  'pending'
                                }
                                width={formSmallWidthInStep}
                                onChange={(value: string) => {
                                  const clonedRuleProperties = cloneDeep(
                                    properties
                                  );

                                  clonedRuleProperties[
                                    propertyIndex
                                  ][0] = value;

                                  algorithmAnalyzerStore.mutateCustomPathRuleParams(
                                    'properties',
                                    clonedRuleProperties,
                                    ruleIndex
                                  );
                                }}
                              >
                                {uniq(
                                  flatten(
                                    dataAnalyzeStore.edgeTypes
                                      .filter(({ name }) =>
                                        labels.includes(name)
                                      )
                                      .map(({ properties }) =>
                                        properties.map(({ name }) => name)
                                      )
                                  )
                                )
                                  .filter(
                                    (name) =>
                                      !Object.keys(
                                        fromPairs(properties)
                                      ).includes(name)
                                  )
                                  .map((name) => (
                                    <Select.Option value={name} key={name}>
                                      {name}
                                    </Select.Option>
                                  ))}
                              </Select>
                            </div>

                            <div style={{ marginRight: 8 }}>
                              <Input
                                width={formSmallWidthInStep}
                                size="medium"
                                disabled={
                                  dataAnalyzeStore.requestStatus.fetchGraphs ===
                                  'pending'
                                }
                                placeholder={t(
                                  'data-analyze.algorithm-forms.custom-path.placeholder.input-multiple-properties'
                                )}
                                value={value}
                                onChange={(e: any) => {
                                  const clonedRuleProperties = cloneDeep(
                                    properties
                                  );

                                  clonedRuleProperties[propertyIndex][1] =
                                    e.value;

                                  algorithmAnalyzerStore.mutateCustomPathRuleParams(
                                    'properties',
                                    clonedRuleProperties,
                                    ruleIndex
                                  );
                                }}
                              />
                            </div>

                            {size(properties) > 1 && (
                              <div
                                style={{
                                  fontSize: 14,
                                  lineHeight: '32px',
                                  color: '#2b65ff',
                                  cursor: 'pointer'
                                }}
                                onClick={() => {
                                  algorithmAnalyzerStore.removeCustomPathRuleProperty(
                                    ruleIndex,
                                    propertyIndex
                                  );
                                }}
                              >
                                {t(
                                  'data-analyze.algorithm-forms.custom-path.delete'
                                )}
                              </div>
                            )}
                          </div>
                        );
                      })}

                      <div
                        className={addNewRulePropertyClassName}
                        style={{
                          marginTop: 8,
                          marginBottom: 0,
                          paddingLeft: 0
                        }}
                      >
                        <span
                          onClick={() => {
                            if (
                              allowAddNewRuleProperty &&
                              dataAnalyzeStore.requestStatus.fetchGraphs !==
                                'pending'
                            ) {
                              algorithmAnalyzerStore.addCustomPathRuleProperty(
                                ruleIndex
                              );
                            }
                          }}
                        >
                          {t('data-analyze.algorithm-forms.custom-path.add')}
                        </span>
                      </div>
                    </>
                  </div>
                </div>

                {algorithmAnalyzerStore.customPathParams.sort_by !== 'NONE' && (
                  <div className="query-tab-content-form-expand-item">
                    <div className="query-tab-content-form-item-title query-tab-content-form-expand-title">
                      <i>*</i>
                      <span>
                        {t(
                          'data-analyze.algorithm-forms.custom-path.options.weight_by'
                        )}
                      </span>
                    </div>
                    <Select
                      size="medium"
                      trigger="click"
                      value={weight_by}
                      selectorName={t(
                        'data-analyze.algorithm-forms.custom-path.placeholder.select-property'
                      )}
                      notFoundContent={t(
                        'data-analyze.algorithm-forms.custom-path.placeholder.no-property'
                      )}
                      disabled={
                        dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                      }
                      width={formWidthInStep}
                      onChange={(value: string) => {
                        algorithmAnalyzerStore.mutateCustomPathRuleParams(
                          'weight_by',
                          value,
                          ruleIndex
                        );
                      }}
                    >
                      <Select.Option
                        value="__CUSTOM_WEIGHT__"
                        key="__CUSTOM_WEIGHT__"
                      >
                        {t(
                          'data-analyze.algorithm-forms.custom-path.custom-weight'
                        )}
                      </Select.Option>
                      {dataAnalyzeStore.allPropertiesFromEdge.map(
                        (property) => (
                          <Select.Option value={property} property={property}>
                            {property}
                          </Select.Option>
                        )
                      )}
                    </Select>
                  </div>
                )}
                {algorithmAnalyzerStore.customPathParams.steps[ruleIndex]
                  .weight_by === '__CUSTOM_WEIGHT__' && (
                  <div className="query-tab-content-form-expand-item">
                    <div className="query-tab-content-form-item-title query-tab-content-form-expand-title">
                      <span></span>
                    </div>
                    <Input
                      width={formWidthInStep}
                      size="medium"
                      disabled={
                        dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                      }
                      placeholder={t(
                        'data-analyze.algorithm-forms.custom-path.placeholder.input-number'
                      )}
                      errorLocation="layer"
                      errorMessage={
                        algorithmAnalyzerStore
                          .validateCustomPathParmasErrorMessage.steps[ruleIndex]
                          .default_weight
                      }
                      value={default_weight}
                      onChange={(e: any) => {
                        algorithmAnalyzerStore.mutateCustomPathRuleParams(
                          'default_weight',
                          e.value as string,
                          ruleIndex
                        );

                        algorithmAnalyzerStore.validateCustomPathRules(
                          'default_weight',
                          ruleIndex
                        );
                      }}
                      originInputProps={{
                        onBlur() {
                          algorithmAnalyzerStore.validateCustomPathRules(
                            'default_weight',
                            ruleIndex
                          );
                        }
                      }}
                    />
                  </div>
                )}
                <div className="query-tab-content-form-expand-item">
                  <div className="query-tab-content-form-item-title query-tab-content-form-expand-title">
                    <span>
                      {t(
                        'data-analyze.algorithm-forms.custom-path.options.degree'
                      )}
                    </span>
                  </div>
                  <Input
                    width={formWidthInStep}
                    size="medium"
                    disabled={
                      dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                    }
                    placeholder={t(
                      'data-analyze.algorithm-forms.custom-path.placeholder.input-positive-integer-or-negative-one-degree'
                    )}
                    errorLocation="layer"
                    errorMessage={
                      algorithmAnalyzerStore
                        .validateCustomPathParmasErrorMessage.steps[ruleIndex]
                        .degree
                    }
                    value={degree}
                    onChange={(e: any) => {
                      algorithmAnalyzerStore.mutateCustomPathRuleParams(
                        'degree',
                        e.value as string,
                        ruleIndex
                      );

                      algorithmAnalyzerStore.validateCustomPathRules(
                        'degree',
                        ruleIndex
                      );
                    }}
                    originInputProps={{
                      onBlur() {
                        algorithmAnalyzerStore.validateCustomPathRules(
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
                        'data-analyze.algorithm-forms.custom-path.options.sample'
                      )}
                    </span>
                  </div>
                  <Input
                    width={formWidthInStep}
                    size="medium"
                    disabled={
                      dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                    }
                    placeholder={t(
                      'data-analyze.algorithm-forms.custom-path.placeholder.input-integer'
                    )}
                    errorLocation="layer"
                    errorMessage={
                      algorithmAnalyzerStore
                        .validateCustomPathParmasErrorMessage.steps[ruleIndex]
                        .sample
                    }
                    value={sample}
                    onChange={(e: any) => {
                      algorithmAnalyzerStore.mutateCustomPathRuleParams(
                        'sample',
                        e.value as string,
                        ruleIndex
                      );

                      algorithmAnalyzerStore.validateCustomPathRules(
                        'sample',
                        ruleIndex
                      );
                    }}
                    originInputProps={{
                      onBlur() {
                        algorithmAnalyzerStore.validateCustomPathRules(
                          'sample',
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
                algorithmAnalyzerStore.addCustomPathRule();
              }
            }}
          >
            {t('data-analyze.algorithm-forms.custom-path.add-new-rule')}
          </span>
        </div>
      </div>
    </div>
  );
});

export default CustomPath;
