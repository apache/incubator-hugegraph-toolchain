import React, { useContext, useCallback, useRef, useEffect } from 'react';
import { observer } from 'mobx-react';
import { Drawer, Select, Input, Button, Message } from 'hubble-ui';

import { DataAnalyzeStoreContext } from '../../../stores';
import { addGraphNodes, addGraphEdges } from '../../../stores/utils';
import { isUndefined, isEmpty } from 'lodash-es';
import { useTranslation } from 'react-i18next';
import i18next from '../../../i18n';

const IDStrategyMappings: Record<string, string> = {
  PRIMARY_KEY: i18next.t('addition.constant.primary-key-id'),
  AUTOMATIC: i18next.t('addition.constant.automatic-generation'),
  CUSTOMIZE_STRING: i18next.t('addition.constant.custom-string'),
  CUSTOMIZE_NUMBER: i18next.t('addition.constant.custom-number'),
  CUSTOMIZE_UUID: i18next.t('addition.constant.custom-uuid')
};

const DataAnalyzeAddEdge: React.FC = observer(() => {
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
  const { t } = useTranslation();

  const title =
    dataAnalyzeStore.dynamicAddGraphDataStatus === 'inEdge'
      ? t('addition.common.in-edge')
      : t('addition.common.out-edge');

  const selectedEdgeLabel = dataAnalyzeStore.edgeTypes.find(
    ({ name }) => name === dataAnalyzeStore.newGraphEdgeConfigs.label
  )!;

  const nonNullableProperties = [
    ...dataAnalyzeStore.newGraphEdgeConfigs.properties.nonNullable.keys()
  ];

  const nullableProperties = [
    ...dataAnalyzeStore.newGraphEdgeConfigs.properties.nullable.keys()
  ];

  const premitAddGraphEdge =
    !isEmpty(dataAnalyzeStore.newGraphEdgeConfigs.label) &&
    !isEmpty(dataAnalyzeStore.newGraphEdgeConfigs.id) &&
    ![
      ...dataAnalyzeStore.newGraphEdgeConfigs.properties.nonNullable.values()
    ].includes('') &&
    [
      ...dataAnalyzeStore.validateAddGraphEdgeErrorMessage!.properties.nonNullable.values()
    ].every((value) => value === '') &&
    [
      ...dataAnalyzeStore.validateAddGraphEdgeErrorMessage!.properties.nullable.values()
    ].every((value) => value === '');

  const handleDrawerClose = useCallback(() => {
    dataAnalyzeStore.setDynamicAddGraphDataStatus('');
    dataAnalyzeStore.resetNewGraphData('edge');
  }, [dataAnalyzeStore]);

  useEffect(() => {
    const handleOutSideClick = (e: MouseEvent) => {
      const drawerWrapper = document.querySelector(
        '.new-fc-one-drawer-content-wrapper'
      );

      if (
        dataAnalyzeStore.isShowGraphInfo &&
        !dataAnalyzeStore.isClickOnNodeOrEdge &&
        drawerWrapper &&
        !drawerWrapper.contains(e.target as Element)
      ) {
        dataAnalyzeStore.setDynamicAddGraphDataStatus('');
      }
    };

    document.addEventListener('click', handleOutSideClick, false);

    return () => {
      document.removeEventListener('click', handleOutSideClick, false);
    };
  }, [dataAnalyzeStore]);

  return (
    <Drawer
      width={580}
      title={`${t('addition.common.add')}${title}`}
      visible={dataAnalyzeStore.dynamicAddGraphDataStatus.includes('Edge')}
      onClose={handleDrawerClose}
      maskClosable={false}
      footer={[
        <Button
          type="primary"
          size="medium"
          style={{ width: 60 }}
          disabled={!premitAddGraphEdge}
          onClick={async () => {
            const graphViewData = await dataAnalyzeStore.addGraphEdge();

            if (!isUndefined(graphViewData)) {
              const { vertices, originalVertices, edges } = graphViewData;

              addGraphNodes(
                vertices,
                dataAnalyzeStore.visDataSet?.nodes,
                dataAnalyzeStore.vertexSizeMappings,
                dataAnalyzeStore.colorMappings,
                dataAnalyzeStore.vertexWritingMappings
              );

              addGraphEdges(
                edges,
                dataAnalyzeStore.visDataSet?.edges,
                dataAnalyzeStore.edgeColorMappings,
                dataAnalyzeStore.edgeThicknessMappings,
                dataAnalyzeStore.edgeWithArrowMappings,
                dataAnalyzeStore.edgeWritingMappings
              );

              dataAnalyzeStore.visNetwork?.selectNodes(
                originalVertices.map(({ id }) => id)
              );

              dataAnalyzeStore.visNetwork?.selectEdges(
                edges.map(({ id }) => id)
              );

              Message.success({
                content: t('addition.common.add-success'),
                size: 'medium',
                showCloseIcon: false
              });
            } else {
              Message.error({
                content: t('addition.common.add-fail'),
                size: 'medium',
                showCloseIcon: false
              });
            }

            handleDrawerClose();
          }}
        >
          {t('addition.common.add')}
        </Button>,
        <Button size="medium" style={{ width: 60 }} onClick={handleDrawerClose}>
          {t('addition.common.cancel')}
        </Button>
      ]}
    >
      <div className="data-analyze-dynamic-add-options">
        <div
          className="data-analyze-dynamic-add-option"
          style={{ marginBottom: 24 }}
        >
          <div>
            {title === t('addition.common.out-edge')
              ? t('addition.common.source')
              : t('addition.common.target')}
            ：
          </div>
          <span>{dataAnalyzeStore.rightClickedGraphData.id}</span>
        </div>
        <div className="data-analyze-dynamic-add-option">
          <div>{t('addition.common.edge-type')}：</div>
          <Select
            size="medium"
            trigger="click"
            selectorName={t('addition.common.edge-type-select-desc')}
            value={dataAnalyzeStore.newGraphEdgeConfigs.label}
            width={420}
            onChange={(value: string) => {
              dataAnalyzeStore.setNewGraphDataConfig('edge', 'label', value);
              dataAnalyzeStore.syncNewGraphDataProperties('edge');
              dataAnalyzeStore.initValidateAddGraphDataErrorMessage('edge');
            }}
            dropdownClassName="data-analyze-sidebar-select"
          >
            {dataAnalyzeStore.relatedGraphEdges.map((label) => (
              <Select.Option value={label} key={label}>
                {label}
              </Select.Option>
            ))}
          </Select>
        </div>

        {!isUndefined(selectedEdgeLabel) && (
          <>
            <div
              className="data-analyze-dynamic-add-option"
              // override style bugs in index.less
              style={{ alignItems: 'normal' }}
            >
              <div style={{ lineHeight: '32px' }}>
                {title === t('addition.common.out-edge')
                  ? t('addition.common.target')
                  : t('addition.common.source')}
                ：
              </div>
              <div>
                <Input
                  size="medium"
                  width={420}
                  placeholder={`${t('addition.common.please-input')}${
                    title === t('addition.common.out-edge')
                      ? t('addition.common.target')
                      : t('addition.common.source')
                  }ID`}
                  errorLocation="layer"
                  errorMessage={
                    dataAnalyzeStore.validateAddGraphEdgeErrorMessage!.id
                  }
                  value={dataAnalyzeStore.newGraphEdgeConfigs.id}
                  onChange={(e: any) => {
                    dataAnalyzeStore.setNewGraphDataConfig(
                      'edge',
                      'id',
                      e.value
                    );

                    dataAnalyzeStore.validateAddGraphEdge('id', false);
                  }}
                  onBlur={() => {
                    dataAnalyzeStore.validateAddGraphEdge('id', false);
                  }}
                />
              </div>
            </div>

            {dataAnalyzeStore.newGraphEdgeConfigs.properties.nonNullable
              .size !== 0 && (
              <div className="data-analyze-dynamic-add-option data-analyze-dynamic-add-option-with-expand">
                <div>{t('addition.common.required-property')}：</div>
                <div className="data-analyze-dynamic-add-option-expands">
                  <div className="data-analyze-dynamic-add-option-expand">
                    <div>{t('addition.common.property')}</div>
                    <div>{t('addition.common.property-value')}</div>
                  </div>
                  {nonNullableProperties.map((property) => (
                    <div
                      className="data-analyze-dynamic-add-option-expand"
                      key={property}
                    >
                      <div>{property}</div>
                      <div>
                        <Input
                          size="medium"
                          width={190}
                          placeholder={t('addition.common.property-input-desc')}
                          errorLocation="layer"
                          errorMessage={dataAnalyzeStore.validateAddGraphEdgeErrorMessage?.properties.nonNullable.get(
                            property
                          )}
                          value={dataAnalyzeStore.newGraphEdgeConfigs.properties.nonNullable.get(
                            property
                          )}
                          onChange={(e: any) => {
                            dataAnalyzeStore.setNewGraphDataConfigProperties(
                              'edge',
                              'nonNullable',
                              property,
                              e.value
                            );

                            dataAnalyzeStore.validateAddGraphEdge(
                              'nonNullable',
                              false,
                              property
                            );
                          }}
                          onBlur={() => {
                            dataAnalyzeStore.validateAddGraphEdge(
                              'nonNullable',
                              false,
                              property
                            );
                          }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {dataAnalyzeStore.newGraphEdgeConfigs.properties.nullable.size !==
              0 && (
              <div className="data-analyze-dynamic-add-option data-analyze-dynamic-add-option-with-expand">
                <div>{t('addition.common.nullable-property')}：</div>
                <div className="data-analyze-dynamic-add-option-expands">
                  <div className="data-analyze-dynamic-add-option-expand">
                    <div>{t('addition.common.property')}</div>
                    <div>{t('addition.common.property-value')}</div>
                  </div>
                  {nullableProperties.map((property) => (
                    <div
                      className="data-analyze-dynamic-add-option-expand"
                      key={property}
                    >
                      <div>{property}</div>
                      <div>
                        <Input
                          size="medium"
                          width={190}
                          placeholder={t('addition.common.property-input-desc')}
                          errorLocation="layer"
                          errorMessage={dataAnalyzeStore.validateAddGraphEdgeErrorMessage!.properties.nullable.get(
                            property
                          )}
                          value={dataAnalyzeStore.newGraphEdgeConfigs.properties.nullable.get(
                            property
                          )}
                          onChange={(e: any) => {
                            dataAnalyzeStore.setNewGraphDataConfigProperties(
                              'edge',
                              'nullable',
                              property,
                              e.value
                            );

                            dataAnalyzeStore.validateAddGraphEdge(
                              'nullable',
                              false,
                              property
                            );
                          }}
                          onBlur={() => {
                            dataAnalyzeStore.validateAddGraphEdge(
                              'nullable',
                              false,
                              property
                            );
                          }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </Drawer>
  );
});

export default DataAnalyzeAddEdge;
