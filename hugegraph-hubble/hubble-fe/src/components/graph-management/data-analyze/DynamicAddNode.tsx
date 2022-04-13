import React, { useContext, useCallback, useRef, useEffect } from 'react';
import { observer } from 'mobx-react';
import { Drawer, Select, Input, Button, Message } from 'hubble-ui';

import { DataAnalyzeStoreContext } from '../../../stores';
import { addGraphNodes } from '../../../stores/utils';
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

const DataAnalyzeAddNode: React.FC = observer(() => {
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
  const { t } = useTranslation();

  const selectedVertexLabel = dataAnalyzeStore.vertexTypes.find(
    ({ name }) => name === dataAnalyzeStore.newGraphNodeConfigs.label
  )!;

  const nonNullableProperties = [
    ...dataAnalyzeStore.newGraphNodeConfigs.properties.nonNullable.keys()
  ];

  const nullableProperties = [
    ...dataAnalyzeStore.newGraphNodeConfigs.properties.nullable.keys()
  ];

  const shouldRevealId =
    selectedVertexLabel &&
    selectedVertexLabel!.id_strategy !== 'PRIMARY_KEY' &&
    selectedVertexLabel!.id_strategy !== 'AUTOMATIC';

  const premitAddGraphNode =
    !isEmpty(dataAnalyzeStore.newGraphNodeConfigs.label) &&
    ![
      ...dataAnalyzeStore.newGraphNodeConfigs.properties.nonNullable.values()
    ].includes('') &&
    [
      ...dataAnalyzeStore.validateAddGraphNodeErrorMessage!.properties.nonNullable.values()
    ].every((value) => value === '') &&
    [
      ...dataAnalyzeStore.validateAddGraphNodeErrorMessage!.properties.nullable.values()
    ].every((value) => value === '') &&
    (shouldRevealId
      ? !isEmpty(dataAnalyzeStore.newGraphNodeConfigs.id) &&
        isEmpty(dataAnalyzeStore.validateAddGraphNodeErrorMessage!.id)
      : true);

  const handleDrawerClose = useCallback(() => {
    dataAnalyzeStore.setDynamicAddGraphDataStatus('');
    dataAnalyzeStore.resetNewGraphData('vertex');
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
      title={t('addition.common.add-vertex')}
      visible={dataAnalyzeStore.dynamicAddGraphDataStatus === 'vertex'}
      onClose={handleDrawerClose}
      maskClosable={false}
      footer={[
        <Button
          type="primary"
          size="medium"
          style={{ width: 60 }}
          disabled={!premitAddGraphNode}
          onClick={async () => {
            const newGraphNodes = await dataAnalyzeStore.addGraphNode();

            if (!isUndefined(newGraphNodes)) {
              const node = dataAnalyzeStore.visDataSet?.nodes.get(
                newGraphNodes[0].id
              );

              if (node !== null) {
                dataAnalyzeStore.visNetwork?.selectNodes([node.id]);
                dataAnalyzeStore.visNetwork?.focus(node.id, {
                  animation: true
                });

                handleDrawerClose();
                return;
              }

              addGraphNodes(
                newGraphNodes,
                dataAnalyzeStore.visDataSet?.nodes,
                dataAnalyzeStore.vertexSizeMappings,
                dataAnalyzeStore.colorMappings,
                dataAnalyzeStore.vertexWritingMappings
              );

              dataAnalyzeStore.visNetwork?.selectNodes(
                newGraphNodes.map(({ id }) => id)
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
        <div className="data-analyze-dynamic-add-option">
          <div>{t('addition.common.id-strategy')}：</div>
          <Select
            size="medium"
            trigger="click"
            selectorName={t('addition.common.vertex-type-select-desc')}
            value={dataAnalyzeStore.newGraphNodeConfigs.label}
            width={420}
            onChange={(value: string) => {
              dataAnalyzeStore.setNewGraphDataConfig('vertex', 'label', value);
              dataAnalyzeStore.syncNewGraphDataProperties('vertex');
              dataAnalyzeStore.initValidateAddGraphDataErrorMessage('vertex');
            }}
            dropdownClassName="data-analyze-sidebar-select"
          >
            {dataAnalyzeStore.vertexTypes.map(({ name }) => (
              <Select.Option value={name} key={name}>
                {name}
              </Select.Option>
            ))}
          </Select>
        </div>

        {!isUndefined(selectedVertexLabel) && (
          <>
            <div className="data-analyze-dynamic-add-option">
              <div>{t('addition.common.id-strategy')}</div>
              <span>
                {selectedVertexLabel.id_strategy === 'PRIMARY_KEY'
                  ? `${t(
                      'addition.common.primary-key-property'
                    )}-${selectedVertexLabel.primary_keys.join('，')}`
                  : IDStrategyMappings[selectedVertexLabel.id_strategy]}
              </span>
            </div>
            {shouldRevealId && (
              <div
                className="data-analyze-dynamic-add-option"
                // override style bugs in index.less
                style={{ alignItems: 'normal' }}
              >
                <div style={{ lineHeight: '32px' }}>
                  {t('addition.common.id-value')}：
                </div>
                <div>
                  <Input
                    size="medium"
                    width={420}
                    placeholder={t('addition.common.id-input-desc')}
                    errorLocation="layer"
                    errorMessage={
                      dataAnalyzeStore.validateAddGraphNodeErrorMessage!.id
                    }
                    value={dataAnalyzeStore.newGraphNodeConfigs.id}
                    onChange={(e: any) => {
                      dataAnalyzeStore.setNewGraphDataConfig(
                        'vertex',
                        'id',
                        e.value
                      );

                      dataAnalyzeStore.validateAddGraphNode(
                        selectedVertexLabel.id_strategy,
                        false,
                        'id'
                      );
                    }}
                    onBlur={() => {
                      dataAnalyzeStore.validateAddGraphNode(
                        selectedVertexLabel.id_strategy,
                        false,
                        'id'
                      );
                    }}
                  />
                </div>
              </div>
            )}

            {dataAnalyzeStore.newGraphNodeConfigs.properties.nonNullable
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
                          errorMessage={dataAnalyzeStore.validateAddGraphNodeErrorMessage?.properties.nonNullable.get(
                            property
                          )}
                          value={dataAnalyzeStore.newGraphNodeConfigs.properties.nonNullable.get(
                            property
                          )}
                          onChange={(e: any) => {
                            dataAnalyzeStore.setNewGraphDataConfigProperties(
                              'vertex',
                              'nonNullable',
                              property,
                              e.value
                            );

                            dataAnalyzeStore.validateAddGraphNode(
                              selectedVertexLabel.id_strategy,
                              false,
                              'nonNullable',
                              property
                            );
                          }}
                          onBlur={() => {
                            dataAnalyzeStore.validateAddGraphNode(
                              selectedVertexLabel.id_strategy,
                              false,
                              'nonNullable',
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

            {dataAnalyzeStore.newGraphNodeConfigs.properties.nullable.size !==
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
                          errorMessage={dataAnalyzeStore.validateAddGraphNodeErrorMessage?.properties.nullable.get(
                            property
                          )}
                          value={dataAnalyzeStore.newGraphNodeConfigs.properties.nullable.get(
                            property
                          )}
                          onChange={(e: any) => {
                            dataAnalyzeStore.setNewGraphDataConfigProperties(
                              'vertex',
                              'nullable',
                              property,
                              e.value
                            );

                            dataAnalyzeStore.validateAddGraphNode(
                              selectedVertexLabel.id_strategy,
                              false,
                              'nullable',
                              property
                            );
                          }}
                          onBlur={() => {
                            dataAnalyzeStore.validateAddGraphNode(
                              selectedVertexLabel.id_strategy,
                              false,
                              'nullable',
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

export default DataAnalyzeAddNode;
