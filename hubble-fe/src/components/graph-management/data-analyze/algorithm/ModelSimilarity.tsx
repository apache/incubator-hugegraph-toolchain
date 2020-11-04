import React, { useContext, createContext } from 'react';
import { observer } from 'mobx-react';
import { size } from 'lodash-es';
import { Button, Radio, Input, Select, Switch } from '@baidu/one-ui';
import { useTranslation } from 'react-i18next';
import { styles } from '../QueryAndAlgorithmLibrary';
import { Tooltip as CustomTooltip } from '../../../common';
import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';

import QuestionMarkIcon from '../../../../assets/imgs/ic_question_mark.svg';
import { Algorithm } from '../../../../stores/factory/dataAnalyzeStore/algorithmStore';

const ModelSimilarity = observer(() => {
  const { t } = useTranslation();
  const dataAnalyzeStore = useContext(DataAnalyzeStore);
  const algorithmAnalyzerStore = dataAnalyzeStore.algorithmAnalyzerStore;

  const sourceType = algorithmAnalyzerStore.modelSimilarityParams.method;

  const isValidExec =
    Object.values(
      algorithmAnalyzerStore.validateModelSimilartiyParamsErrorMessage
    ).every((value) => value === '') &&
    algorithmAnalyzerStore.modelSimilarityParams.least_neighbor !== '' &&
    algorithmAnalyzerStore.modelSimilarityParams.similarity !== '' &&
    (algorithmAnalyzerStore.modelSimilarityParams.property_filter !== ''
      ? algorithmAnalyzerStore.modelSimilarityParams.least_property_number !==
        ''
      : true) &&
    ((algorithmAnalyzerStore.modelSimilarityParams.method === 'id' &&
      algorithmAnalyzerStore.modelSimilarityParams.source !== '') ||
      algorithmAnalyzerStore.modelSimilarityParams.method === 'property');

  return (
    <div className="query-tab-content-form">
      <div className="query-tab-content-form-row">
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title large">
            <i>*</i>
            <span>
              {t(
                'data-analyze.algorithm-forms.model-similarity.options.method'
              )}
            </span>
          </div>
          <Radio.Group
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
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
            value={algorithmAnalyzerStore.modelSimilarityParams.property_filter}
            notFoundContent={t(
              'data-analyze.algorithm-forms.model-similarity.placeholder.no-properties'
            )}
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            width={400}
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
            {sourceType === 'id' && <i>*</i>}
            <span>
              {sourceType === 'id'
                ? t(
                    'data-analyze.algorithm-forms.model-similarity.options.source'
                  )
                : t(
                    'data-analyze.algorithm-forms.model-similarity.options.vertex-type'
                  )}
            </span>
          </div>
          {sourceType === 'id' ? (
            <Input
              width={400}
              size="medium"
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              placeholder={t(
                'data-analyze.algorithm-forms.model-similarity.placeholder.input-source-id'
              )}
              errorLocation="layer"
              errorMessage={
                algorithmAnalyzerStore.validateModelSimilartiyParamsErrorMessage
                  .source
              }
              value={algorithmAnalyzerStore.modelSimilarityParams.source}
              onChange={(e: any) => {
                algorithmAnalyzerStore.mutateModelSimilarityParams(
                  'source',
                  e.value as string
                );

                algorithmAnalyzerStore.validateModelSimilarityParams('source');
              }}
              originInputProps={{
                onBlur() {
                  algorithmAnalyzerStore.validateModelSimilarityParams(
                    'source'
                  );
                }
              }}
            />
          ) : (
            <Select
              size="medium"
              trigger="click"
              selectorName={t(
                'data-analyze.algorithm-forms.model-similarity.placeholder.input-vertex-type'
              )}
              value={algorithmAnalyzerStore.modelSimilarityParams.vertexType}
              notFoundContent={t(
                'data-analyze.algorithm-forms.model-similarity.placeholder.no-vertex-type'
              )}
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              width={400}
              onChange={(value: string) => {
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
          )}
        </div>
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
            width={400}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.model-similarity.placeholder.input-integer-gt-1'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateModelSimilartiyParamsErrorMessage
                .least_property_number
            }
            value={
              algorithmAnalyzerStore.modelSimilarityParams.least_property_number
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
            {sourceType === 'id' && <i>*</i>}
            <span>
              {sourceType === 'id'
                ? t(
                    'data-analyze.algorithm-forms.model-similarity.options.direction'
                  )
                : t(
                    'data-analyze.algorithm-forms.model-similarity.options.vertex-property'
                  )}
            </span>
          </div>
          {sourceType === 'id' ? (
            <Radio.Group
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              value={algorithmAnalyzerStore.shortestPathAllParams.direction}
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
          ) : (
            <Select
              width={400}
              mode="multiple"
              size="medium"
              trigger="click"
              placeholder={t(
                'data-analyze.algorithm-forms.model-similarity.placeholder.input-vertex-property'
              )}
              value={
                algorithmAnalyzerStore.modelSimilarityParams.vertexProperty
              }
              notFoundContent={t(
                'data-analyze.algorithm-forms.model-similarity.placeholder.no-properties'
              )}
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              onChange={(value: string[]) => {
                algorithmAnalyzerStore.mutateModelSimilarityParams(
                  'vertexProperty',
                  value
                );
              }}
            >
              {dataAnalyzeStore.vertexTypes
                .find(
                  ({ name }) =>
                    name ===
                    algorithmAnalyzerStore.modelSimilarityParams.vertexType
                )
                ?.properties.map(({ name }) => (
                  <Select.Option value={name} key={name}>
                    {name}
                  </Select.Option>
                ))}
            </Select>
          )}
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title large">
            <span>
              {t(
                'data-analyze.algorithm-forms.model-similarity.options.max_degree'
              )}
            </span>
          </div>
          <Input
            width={400}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.model-similarity.placeholder.input-integer'
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
            <i>*</i>
            <span>
              {sourceType === 'id'
                ? t(
                    'data-analyze.algorithm-forms.model-similarity.options.least_neighbor'
                  )
                : t(
                    'data-analyze.algorithm-forms.model-similarity.options.direction'
                  )}
            </span>
            {sourceType === 'id' && (
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
            )}
          </div>
          {sourceType === 'id' ? (
            <Input
              width={400}
              size="medium"
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              placeholder={t(
                'data-analyze.algorithm-forms.model-similarity.placeholder.input-source-id'
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
          ) : (
            <Radio.Group
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              value={algorithmAnalyzerStore.shortestPathAllParams.direction}
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
          )}
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title large">
            <span>
              {t(
                'data-analyze.algorithm-forms.model-similarity.options.capacity'
              )}
            </span>
          </div>
          <Input
            width={400}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.model-similarity.placeholder.input-source-id'
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

              algorithmAnalyzerStore.validateModelSimilarityParams('capacity');
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
            <i>*</i>
            <span>
              {sourceType === 'id'
                ? t(
                    'data-analyze.algorithm-forms.model-similarity.options.similarity'
                  )
                : t(
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
              tooltipWrapper={
                sourceType === 'id'
                  ? t(
                      'data-analyze.algorithm-forms.model-similarity.hint.similarity'
                    )
                  : t(
                      'data-analyze.algorithm-forms.model-similarity.hint.least_neighbor'
                    )
              }
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
          {sourceType === 'id' ? (
            <Input
              width={400}
              size="medium"
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              placeholder={t(
                'data-analyze.algorithm-forms.model-similarity.placeholder.input-source-id'
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
          ) : (
            <Input
              width={400}
              size="medium"
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              placeholder={t(
                'data-analyze.algorithm-forms.model-similarity.placeholder.input-source-id'
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
          )}
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title large">
            <span>
              {t(
                'data-analyze.algorithm-forms.model-similarity.options.skip_degree'
              )}
            </span>
          </div>
          <Input
            width={400}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.model-similarity.placeholder.input-integer'
            )}
            errorLocation="layer"
            errorMessage={
              algorithmAnalyzerStore.validateModelSimilartiyParamsErrorMessage
                .skip_degree
            }
            value={algorithmAnalyzerStore.modelSimilarityParams.skip_degree}
            onChange={(e: any) => {
              algorithmAnalyzerStore.mutateModelSimilarityParams(
                'skip_degree',
                e.value as string
              );

              algorithmAnalyzerStore.validateModelSimilarityParams(
                'skip_degree'
              );
            }}
            originInputProps={{
              onBlur() {
                algorithmAnalyzerStore.validateModelSimilarityParams(
                  'skip_degree'
                );
              }
            }}
          />
        </div>
      </div>

      <div className="query-tab-content-form-row">
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title large">
            {sourceType !== 'id' && <i>*</i>}
            <span>
              {sourceType === 'id'
                ? t(
                    'data-analyze.algorithm-forms.model-similarity.options.label'
                  )
                : t(
                    'data-analyze.algorithm-forms.model-similarity.options.similarity'
                  )}
            </span>
            {sourceType !== 'id' && (
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
            )}
          </div>
          {sourceType === 'id' ? (
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
              width={400}
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
          ) : (
            <Input
              width={400}
              size="medium"
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
              placeholder={t(
                'data-analyze.algorithm-forms.model-similarity.placeholder.input-source-id'
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
          )}
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title large">
            <span>
              {t('data-analyze.algorithm-forms.model-similarity.options.limit')}
            </span>
          </div>
          <Input
            width={400}
            size="medium"
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
            placeholder={t(
              'data-analyze.algorithm-forms.model-similarity.placeholder.input-integer'
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
            {sourceType === 'id' && <i>*</i>}
            <span>
              {sourceType === 'id'
                ? t(
                    'data-analyze.algorithm-forms.model-similarity.options.max_similar'
                  )
                : t(
                    'data-analyze.algorithm-forms.model-similarity.options.label'
                  )}
            </span>
            {sourceType === 'id' && (
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
            )}
          </div>
          {sourceType === 'id' ? (
            <Input
              width={400}
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
          ) : (
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
              width={400}
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
          )}
        </div>
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
          <div style={{ width: 400 }}>
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
            {sourceType !== 'id' && <i>*</i>}
            <span>
              {sourceType === 'id'
                ? t(
                    'data-analyze.algorithm-forms.model-similarity.options.least_similar'
                  )
                : t(
                    'data-analyze.algorithm-forms.model-similarity.options.max_similar'
                  )}
            </span>
            {sourceType !== 'id' && (
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
            )}
          </div>
          {sourceType === 'id' ? (
            <Input
              width={400}
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
          ) : (
            <Input
              width={400}
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
          )}
        </div>
        <div className="query-tab-content-form-item">
          <div className="query-tab-content-form-item-title large">
            <span>
              {t(
                'data-analyze.algorithm-forms.model-similarity.options.return_complete_info'
              )}
            </span>
          </div>
          <div style={{ width: 400 }}>
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

      {sourceType !== 'id' && (
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
              width={400}
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
      )}

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
            algorithmAnalyzerStore.resetShortestPathAllParams();
          }}
        >
          {t('data-analyze.manipulations.reset')}
        </Button>
      </div>
    </div>
  );
});

export default ModelSimilarity;
