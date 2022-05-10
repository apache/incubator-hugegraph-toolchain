import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback
} from 'react';
import { observer } from 'mobx-react';
import classnames from 'classnames';
import { cloneDeep, isUndefined } from 'lodash-es';
import {
  Drawer,
  Button,
  Input,
  Select,
  Radio,
  Switch,
  Checkbox,
  Tooltip,
  Message
} from 'hubble-ui';

import { Tooltip as CustomTooltip } from '../../../common';
import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';
import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import {
  vertexRadiusMapping,
  mapMetadataProperties,
  formatVertexIdText
} from '../../../../stores/utils';
import { useTranslation } from 'react-i18next';

import type { VertexTypeValidatePropertyIndexes } from '../../../../stores/types/GraphManagementStore/metadataConfigsStore';

import BlueArrowIcon from '../../../../assets/imgs/ic_arrow_blue.svg';
import HintIcon from '../../../../assets/imgs/ic_question_mark.svg';
import closeIcon from '../../../../assets/imgs/ic_close_16.svg';
import { clearObserving } from 'mobx/lib/internal';

const CreateVertex: React.FC = observer(() => {
  const dataAnalyzeStore = useContext(DataAnalyzeStore);
  const { metadataPropertyStore, vertexTypeStore, graphViewStore } = useContext(
    MetadataConfigsRootStore
  );
  const { t } = useTranslation();
  const [isAddNewProperty, switchIsAddNewProperty] = useState(false);
  const [deletePopIndex, setDeletePopIndex] = useState<number | null>(null);
  const deleteWrapperRef = useRef<HTMLImageElement>(null);
  const dropdownWrapperRef = useRef<HTMLDivElement>(null);

  // need useCallback to stop infinite callings of useEffect
  const handleOutSideClick = useCallback(
    (e: MouseEvent) => {
      // if clicked element is not on dropdown, collpase it
      if (
        isAddNewProperty &&
        dropdownWrapperRef.current &&
        !dropdownWrapperRef.current.contains(e.target as Element)
      ) {
        switchIsAddNewProperty(false);
      }

      if (
        deletePopIndex &&
        deleteWrapperRef.current &&
        !deleteWrapperRef.current.contains(e.target as Element)
      ) {
        setDeletePopIndex(null);
      }
    },
    [deletePopIndex, isAddNewProperty]
  );

  const handleCloseDrawer = () => {
    graphViewStore.setCurrentDrawer('');
    vertexTypeStore.resetNewVertextType();
    vertexTypeStore.resetAddedPropertiesInSelectedVertextType();
  };

  useEffect(() => {
    document.addEventListener('click', handleOutSideClick, false);

    return () => {
      document.removeEventListener('click', handleOutSideClick, false);
    };
  }, [handleOutSideClick]);

  return (
    <Drawer
      title={t('addition.vertex.create-vertex-type')}
      width={669}
      destroyOnClose
      maskClosable={false}
      visible={graphViewStore.currentDrawer === 'create-vertex'}
      onClose={handleCloseDrawer}
      footer={[
        <Button
          type="primary"
          size="medium"
          style={{ width: 60 }}
          disabled={!vertexTypeStore.isCreatedReady}
          onClick={async () => {
            vertexTypeStore.validateAllNewVertexType();

            if (!vertexTypeStore.isCreatedReady) {
              return;
            }

            const {
              name,
              properties,
              primary_keys,
              style,
              ...rest
            } = vertexTypeStore.newVertexType;

            const mappedProperties = mapMetadataProperties(
              properties,
              metadataPropertyStore.metadataProperties
            );

            graphViewStore.visDataSet!.nodes.add({
              ...rest,
              id: name,
              label: name.length <= 15 ? name : name.slice(0, 15) + '...',
              vLabel: name,
              properties,
              value: vertexRadiusMapping[style.size],
              font: { size: 16 },
              title: `
                <div class="metadata-graph-view-tooltip-fields">
                  <div>${t('addition.common.vertex-type')}：</div>
                  <div style="min-width: 60px; max-width: 145px; marigin-right: 0">${name}</div>
                </div>
                <div class="metadata-graph-view-tooltip-fields">
                  <div style="max-width: 120px">${t(
                    'addition.common.association-property-and-type'
                  )}：</div>
                </div>
                ${Object.entries(mappedProperties)
                  .map(([key, value]) => {
                    const convertedValue =
                      value.toLowerCase() === 'text'
                        ? 'string'
                        : value.toLowerCase();

                    const primaryKeyIndex = primary_keys.findIndex(
                      (primaryKey) => primaryKey === key
                    );

                    return `<div class="metadata-graph-view-tooltip-fields">
                          <div>${key}: </div>
                          <div>${convertedValue}</div>
                          <div>${
                            primaryKeyIndex === -1
                              ? ''
                              : `(${t('addition.common.primary-key')}${
                                  primaryKeyIndex + 1
                                })`
                          }</div>
                        </div>`;
                  })
                  .join('')}
              `,
              color: {
                background: style.color,
                border: style.color,
                highlight: {
                  background: '#fb6a02',
                  border: '#fb6a02'
                },
                hover: { background: '#ec3112', border: '#ec3112' }
              },
              // reveal label when zoom to max
              scaling: {
                label: {
                  max: Infinity,
                  maxVisible: Infinity
                }
              },
              chosen: {
                node(
                  values: any,
                  id: string,
                  selected: boolean,
                  hovering: boolean
                ) {
                  if (hovering || selected) {
                    values.shadow = true;
                    values.shadowColor = 'rgba(0, 0, 0, 0.6)';
                    values.shadowX = 0;
                    values.shadowY = 0;
                    values.shadowSize = 25;
                  }

                  if (selected) {
                    values.size += 5;
                  }
                }
              }
            });

            await vertexTypeStore.addVertexType();

            if (vertexTypeStore.requestStatus.addVertexType === 'success') {
              vertexTypeStore.fetchVertexTypeList();
              vertexTypeStore.resetNewVertextType();
              vertexTypeStore.resetAddedPropertiesInSelectedVertextType();
              graphViewStore.setCurrentDrawer('');

              Message.success({
                content: t('addition.newGraphConfig.create-scuccess'),
                size: 'medium',
                showCloseIcon: false
              });

              // if vertex is empty before, trigger re-render here to reveal <GraphDataView />
              if (graphViewStore.isGraphVertexEmpty) {
                graphViewStore.switchGraphDataEmpty(false);
                // need to get node colors again since fetchGraphViewData() will cause re-render in <GraphView />
                // the graph use graphNode() rather than local added node
                await dataAnalyzeStore.fetchAllNodeStyle();
                graphViewStore.fetchGraphViewData(
                  dataAnalyzeStore.colorMappings,
                  dataAnalyzeStore.vertexSizeMappings,
                  dataAnalyzeStore.vertexWritingMappings,
                  dataAnalyzeStore.edgeColorMappings,
                  dataAnalyzeStore.edgeThicknessMappings,
                  dataAnalyzeStore.edgeWithArrowMappings,
                  dataAnalyzeStore.edgeWritingMappings
                );
              }
              return;
            }

            if (vertexTypeStore.requestStatus.addVertexType === 'failed') {
              Message.error({
                content: vertexTypeStore.errorMessage,
                size: 'medium',
                showCloseIcon: false
              });
            }
          }}
        >
          {t('addition.newGraphConfig.create')}
        </Button>,
        <Button size="medium" style={{ width: 60 }} onClick={handleCloseDrawer}>
          {t('addition.common.cancel')}
        </Button>
      ]}
    >
      <div className="metadata-configs-drawer">
        <div className="metadata-graph-drawer-wrapper">
          <div className="metadata-graph-drawer">
            <div
              className="metadata-title metadata-graph-drawer-title"
              style={{ width: 88 }}
            >
              {t('addition.menu.base-info')}
            </div>
            <div className="metadata-graph-drawer-options">
              <div
                className="metadata-graph-drawer-options-name"
                style={{ width: 95, marginRight: 14 }}
              >
                <span className="metdata-essential-form-options">*</span>
                <span>{t('addition.vertex.vertex-type-name')}:</span>
              </div>
              <Input
                size="medium"
                width={356}
                maxLen={128}
                placeholder={t('addition.message.edge-name-rule')}
                errorLocation="layer"
                errorMessage={
                  vertexTypeStore.validateNewVertexTypeErrorMessage.name
                }
                value={vertexTypeStore.newVertexType.name}
                onChange={(e: any) => {
                  vertexTypeStore.mutateNewProperty({
                    ...vertexTypeStore.newVertexType,
                    name: e.value
                  });
                }}
                originInputProps={{
                  onBlur() {
                    vertexTypeStore.validateAllNewVertexType(true);
                    vertexTypeStore.validateNewVertexType('name');
                  }
                }}
              />
            </div>
            <div className="metadata-graph-drawer-options">
              <div
                className="metadata-graph-drawer-options-name"
                style={{ width: 95, marginRight: 0 }}
              >
                <span className="metdata-essential-form-options">*</span>
                <span>{t('addition.vertex.vertex-style')}:</span>
              </div>
              <div className="metadata-graph-drawer-options-colors">
                <Select
                  width={66}
                  size="medium"
                  style={{ marginRight: 12 }}
                  value={vertexTypeStore.newVertexType.style.color}
                  prefixCls="new-fc-one-select-another"
                  dropdownMatchSelectWidth={false}
                  onChange={(value: string) => {
                    vertexTypeStore.mutateNewProperty({
                      ...vertexTypeStore.newVertexType,
                      style: {
                        ...vertexTypeStore.newVertexType.style,
                        color: value,
                        size: vertexTypeStore.newVertexType.style.size
                      }
                    });
                  }}
                >
                  {vertexTypeStore.colorSchemas.map(
                    (color: string, index: number) => (
                      <Select.Option
                        value={color}
                        key={color}
                        style={{
                          display: 'inline-block',
                          marginLeft: index % 5 === 0 ? 8 : 0,
                          marginTop: index < 5 ? 6 : 2,
                          width: 31
                        }}
                      >
                        <div
                          className={
                            vertexTypeStore.newVertexType.style.color === color
                              ? 'new-vertex-type-options-border new-vertex-type-options-color'
                              : 'new-vertex-type-options-no-border new-vertex-type-options-color'
                          }
                          style={{
                            background: color,
                            marginLeft: -4,
                            marginTop: 4.4
                          }}
                        ></div>
                      </Select.Option>
                    )
                  )}
                </Select>
              </div>
              <div className="new-vertex-type-options-colors">
                <Select
                  width={67}
                  size="medium"
                  value={vertexTypeStore.newVertexType.style.size}
                  style={{ paddingLeft: 7 }}
                  onChange={(value: string) => {
                    vertexTypeStore.mutateNewProperty({
                      ...vertexTypeStore.newVertexType,
                      style: {
                        ...vertexTypeStore.newVertexType.style,
                        size: value
                      }
                    });
                  }}
                >
                  {vertexTypeStore.vertexSizeSchemas.map((value, index) => (
                    <Select.Option
                      value={value.en}
                      key={value.en}
                      style={{ width: 66 }}
                    >
                      <div
                        className="new-vertex-type-options-color"
                        style={{
                          marginTop: 4,
                          marginLeft: 5
                        }}
                      >
                        {value.ch}
                      </div>
                    </Select.Option>
                  ))}
                </Select>
              </div>
            </div>
            <div className="metadata-graph-drawer-options">
              <div
                className="metadata-graph-drawer-options-name"
                style={{ width: 95, marginRight: 14 }}
              >
                <span className="metdata-essential-form-options">*</span>
                <span>{t('addition.common.id-strategy')}:</span>
              </div>
              <Radio.Group
                value={vertexTypeStore.newVertexType.id_strategy}
                onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                  vertexTypeStore.mutateNewProperty({
                    ...vertexTypeStore.newVertexType,
                    id_strategy: e.target.value
                  });
                  vertexTypeStore.validateAllNewVertexType(true);
                  vertexTypeStore.validateNewVertexType('primaryKeys');
                }}
              >
                <Radio.Button value="PRIMARY_KEY">
                  {t('addition.constant.primary-key-id')}
                </Radio.Button>
                <Radio.Button value="AUTOMATIC">
                  {t('addition.constant.automatic-generation')}
                </Radio.Button>
                <Radio.Button value="CUSTOMIZE_STRING">
                  {t('addition.constant.custom-string')}
                </Radio.Button>
                <Radio.Button value="CUSTOMIZE_NUMBER">
                  {t('addition.constant.custom-number')}
                </Radio.Button>
                <Radio.Button value="CUSTOMIZE_UUID">
                  {t('addition.constant.custom-uuid')}
                </Radio.Button>
              </Radio.Group>
            </div>
            <div
              className="metadata-graph-drawer-options"
              style={{
                marginBottom: isAddNewProperty ? 7 : 32,
                alignItems: 'start'
              }}
            >
              <div
                className="metadata-graph-drawer-options-name"
                style={{ lineHeight: 'inherit', width: 95, marginRight: 14 }}
              >
                {vertexTypeStore.newVertexType.id_strategy ===
                  'PRIMARY_KEY' && (
                  <span className="metdata-essential-form-options">*</span>
                )}
                <span>{t('addition.common.association-property')}:</span>
              </div>
              <div
                className="metadata-graph-drawer-options-expands"
                style={{ flexDirection: 'column' }}
              >
                {vertexTypeStore.newVertexType.properties.length !== 0 && (
                  <div style={{ width: 382 }}>
                    <div
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between'
                      }}
                    >
                      <div>{t('addition.common.property')}</div>
                      <div>{t('addition.common.allow-null')}</div>
                    </div>
                    {vertexTypeStore.newVertexType.properties.map(
                      (property, index) => {
                        const currentProperties = cloneDeep(
                          vertexTypeStore.newVertexType.properties
                        );

                        return (
                          <div
                            className="metadata-selected-properties"
                            key={property.name}
                          >
                            <div>{property.name}</div>
                            <div style={{ width: 56 }}>
                              <Switch
                                checked={property.nullable}
                                onChange={() => {
                                  currentProperties[
                                    index
                                  ].nullable = !currentProperties[index]
                                    .nullable;

                                  vertexTypeStore.mutateNewProperty({
                                    ...vertexTypeStore.newVertexType,
                                    properties: currentProperties
                                  });
                                }}
                                size="large"
                              />
                            </div>
                          </div>
                        );
                      }
                    )}
                  </div>
                )}
                <div
                  style={{
                    display: 'flex',
                    color: '#2b65ff',
                    cursor: 'pointer'
                  }}
                  onClick={() => {
                    switchIsAddNewProperty(!isAddNewProperty);
                  }}
                >
                  <span>{t('addition.common.add-property')}</span>
                  <img
                    src={BlueArrowIcon}
                    alt="toggleAddProperty"
                    style={{
                      marginLeft: 4,
                      transform: isAddNewProperty
                        ? 'rotate(180deg)'
                        : 'rotate(0deg)'
                    }}
                  />
                </div>
              </div>
            </div>

            {isAddNewProperty && (
              <div className="metadata-graph-drawer-options">
                <div
                  className="metadata-graph-drawer-options-name"
                  style={{
                    width: 95,
                    marginRight: 14
                  }}
                ></div>
                <div
                  className="metadata-configs-content-dropdown"
                  ref={dropdownWrapperRef}
                >
                  {metadataPropertyStore.metadataProperties.map((property) => (
                    <div key={property.name}>
                      <span>
                        <Checkbox
                          checked={
                            [
                              ...vertexTypeStore.addedPropertiesInSelectedVertextType
                            ].findIndex(
                              (propertyIndex) => propertyIndex === property.name
                            ) !== -1
                          }
                          onChange={() => {
                            const addedPropertiesInSelectedVertextType =
                              vertexTypeStore.addedPropertiesInSelectedVertextType;

                            addedPropertiesInSelectedVertextType.has(
                              property.name
                            )
                              ? addedPropertiesInSelectedVertextType.delete(
                                  property.name
                                )
                              : addedPropertiesInSelectedVertextType.add(
                                  property.name
                                );

                            vertexTypeStore.mutateNewProperty({
                              ...vertexTypeStore.newVertexType,
                              properties: [
                                ...addedPropertiesInSelectedVertextType
                              ].map((propertyName) => {
                                const currentProperty = vertexTypeStore.newVertexType.properties.find(
                                  ({ name }) => name === propertyName
                                );

                                return {
                                  name: propertyName,
                                  nullable: !isUndefined(currentProperty)
                                    ? currentProperty.nullable
                                    : true
                                };
                              })
                            });

                            vertexTypeStore.validateAllNewVertexType(true);
                            vertexTypeStore.validateNewVertexType('properties');
                          }}
                        >
                          {property.name}
                        </Checkbox>
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {vertexTypeStore.newVertexType.id_strategy === 'PRIMARY_KEY' && (
              <div className="metadata-graph-drawer-options">
                <div
                  className="metadata-graph-drawer-options-name"
                  style={{
                    width: 95,
                    marginRight: 14
                  }}
                >
                  <span className="metdata-essential-form-options">*</span>
                  <span>{t('addition.common.primary-key-property')}:</span>
                </div>
                <Select
                  width={356}
                  mode="multiple"
                  placeholder={t(
                    'addition.common.select-primary-key-property-placeholder'
                  )}
                  size="medium"
                  showSearch={false}
                  onChange={(e: string[]) => {
                    vertexTypeStore.mutateNewProperty({
                      ...vertexTypeStore.newVertexType,
                      primary_keys: e
                    });

                    vertexTypeStore.validateAllNewVertexType(true);
                    vertexTypeStore.validateNewVertexType('primaryKeys');
                  }}
                  value={vertexTypeStore.newVertexType.primary_keys}
                >
                  {vertexTypeStore.newVertexType.properties
                    .filter(({ nullable }) => !nullable)
                    .map((item) => {
                      const order = vertexTypeStore.newVertexType.primary_keys.findIndex(
                        (name) => name === item.name
                      );

                      const multiSelectOptionClassName = classnames({
                        'metadata-configs-sorted-multiSelect-option': true,
                        'metadata-configs-sorted-multiSelect-option-selected':
                          order !== -1
                      });

                      return (
                        <Select.Option value={item.name} key={item.name}>
                          <div className={multiSelectOptionClassName}>
                            <div>{order !== -1 ? order + 1 : ''}</div>
                            <div>{item.name}</div>
                          </div>
                        </Select.Option>
                      );
                    })}
                </Select>
              </div>
            )}

            <div className="metadata-graph-drawer-options">
              <div
                className="metadata-graph-drawer-options-name"
                style={{ width: 95, marginRight: 14 }}
              >
                <span className="metdata-essential-form-options">*</span>
                <span>{t('addition.vertex.vertex-display-content')}:</span>
              </div>
              <Select
                width={356}
                mode="multiple"
                size="medium"
                placeholder={t(
                  'addition.vertex.select-vertex-display-content-placeholder'
                )}
                showSearch={false}
                onChange={(value: string[]) => {
                  vertexTypeStore.mutateNewProperty({
                    ...vertexTypeStore.newVertexType,
                    style: {
                      ...vertexTypeStore.newVertexType.style,
                      display_fields: value.map((field) =>
                        formatVertexIdText(
                          field,
                          t('addition.function-parameter.vertex-id'),
                          true
                        )
                      )
                    }
                  });

                  vertexTypeStore.validateAllNewVertexType(true);
                  vertexTypeStore.validateNewVertexType('displayFeilds');
                }}
                value={vertexTypeStore.newVertexType.style.display_fields.map(
                  (field) =>
                    formatVertexIdText(
                      field,
                      t('addition.function-parameter.vertex-id')
                    )
                )}
              >
                {vertexTypeStore.newVertexType.properties
                  .concat({ name: '~id', nullable: false })
                  .filter(({ nullable }) => !nullable)
                  .map((item) => {
                    const order = vertexTypeStore.newVertexType.style.display_fields.findIndex(
                      (name) => name === item.name
                    );

                    const multiSelectOptionClassName = classnames({
                      'metadata-configs-sorted-multiSelect-option': true,
                      'metadata-configs-sorted-multiSelect-option-selected':
                        order !== -1
                    });

                    return (
                      <Select.Option
                        value={formatVertexIdText(
                          item.name,
                          t('addition.function-parameter.vertex-id')
                        )}
                        key={item.name}
                      >
                        <div className={multiSelectOptionClassName}>
                          <div
                            style={{
                              backgroundColor: vertexTypeStore.newVertexType.style.display_fields.includes(
                                item.name
                              )
                                ? '#2b65ff'
                                : '#fff',
                              borderColor: vertexTypeStore.newVertexType.style.display_fields.includes(
                                item.name
                              )
                                ? '#fff'
                                : '#e0e0e0'
                            }}
                          >
                            {order !== -1 ? order + 1 : ''}
                          </div>
                          <div style={{ color: '#333' }}>
                            {formatVertexIdText(
                              item.name,
                              t('addition.function-parameter.vertex-id')
                            )}
                          </div>
                        </div>
                      </Select.Option>
                    );
                  })}
              </Select>
            </div>

            <div
              className="metadata-title metadata-graph-drawer-title"
              style={{
                marginTop: 46,
                display: 'flex',
                justifyContent: 'flex-end',
                alignItems: 'center',
                width: 86
              }}
            >
              <span style={{ marginRight: 5 }}>
                {t('addition.edge.index-info')}
              </span>
              <Tooltip
                placement="right"
                title={t('addition.message.index-open-notice')}
                type="dark"
              >
                <img src={HintIcon} alt="hint" />
              </Tooltip>
            </div>

            <div className="metadata-graph-drawer-options">
              <div
                className="metadata-graph-drawer-options-name"
                style={{ width: 95, marginRight: 14 }}
              >
                <span className="metdata-essential-form-options">*</span>
                <span>{t('addition.menu.type-index')}:</span>
              </div>
              <Switch
                checked={vertexTypeStore.newVertexType.open_label_index}
                onChange={() => {
                  vertexTypeStore.mutateNewProperty({
                    ...vertexTypeStore.newVertexType,
                    open_label_index: !vertexTypeStore.newVertexType
                      .open_label_index
                  });
                }}
                size="large"
              />
            </div>

            <div
              className="metadata-graph-drawer-options"
              style={{ marginBottom: 12, alignItems: 'start' }}
            >
              <div
                className="metadata-graph-drawer-options-name"
                style={{ width: 95, marginRight: 14 }}
              >
                <span>{t('addition.common.property-index')}:</span>
              </div>
              <div className="metadata-graph-drawer-options-expands">
                {vertexTypeStore.newVertexType.property_indexes.length !==
                  0 && (
                  <div
                    style={{
                      display: 'flex',
                      lineHeight: '32px',
                      marginBottom: 12
                    }}
                  >
                    <div style={{ width: 110, marginRight: 12 }}>
                      {t('addition.edge.index-name')}
                    </div>
                    <div style={{ width: 130, marginRight: 12 }}>
                      {t('addition.edge.index-type')}
                    </div>
                    <div>{t('addition.common.property')}</div>
                  </div>
                )}
                {vertexTypeStore.newVertexType.property_indexes.map(
                  ({ name, type, fields }, index) => (
                    <div
                      style={{
                        display: 'flex',
                        justifyContent: 'center',
                        marginBottom: 9
                      }}
                    >
                      <div style={{ marginRight: 12 }}>
                        <Input
                          size="medium"
                          width={110}
                          placeholder={t('addition.edge.index-name')}
                          errorLocation="layer"
                          errorMessage={
                            vertexTypeStore.validateNewVertexTypeErrorMessage
                              .propertyIndexes.length !== 0
                              ? (vertexTypeStore
                                  .validateNewVertexTypeErrorMessage
                                  .propertyIndexes[
                                  index
                                ] as VertexTypeValidatePropertyIndexes).name
                              : ''
                          }
                          value={name}
                          onChange={(e: any) => {
                            const propertyIndexEntities = cloneDeep(
                              vertexTypeStore.newVertexType.property_indexes
                            );

                            propertyIndexEntities[index].name = e.value;

                            vertexTypeStore.mutateNewProperty({
                              ...vertexTypeStore.newVertexType,
                              property_indexes: propertyIndexEntities
                            });
                          }}
                          originInputProps={{
                            onBlur() {
                              // check is ready to create
                              vertexTypeStore.validateAllNewVertexType(true);
                              vertexTypeStore.validateNewVertexType(
                                'propertyIndexes'
                              );
                            }
                          }}
                        />
                      </div>
                      <div style={{ marginRight: 12 }}>
                        <Select
                          width={130}
                          placeholder={t(
                            'addition.edge.index-type-select-desc'
                          )}
                          size="medium"
                          showSearch={false}
                          value={type === '' ? [] : type}
                          onChange={(value: string) => {
                            const propertyIndexEntities = cloneDeep(
                              vertexTypeStore.newVertexType.property_indexes
                            );

                            propertyIndexEntities[index].type = value;

                            vertexTypeStore.mutateNewProperty({
                              ...vertexTypeStore.newVertexType,
                              property_indexes: propertyIndexEntities
                            });

                            vertexTypeStore.validateAllNewVertexType(true);
                            vertexTypeStore.validateNewVertexType(
                              'propertyIndexes'
                            );
                          }}
                        >
                          <Select.Option value="SECONDARY" key="SECONDARY">
                            {t('addition.menu.secondary-index')}
                          </Select.Option>
                          <Select.Option value="RANGE" key="RANGE">
                            {t('addition.menu.range-index')}
                          </Select.Option>
                          <Select.Option value="SEARCH" key="SEARCH">
                            {t('addition.menu.full-text-index')}
                          </Select.Option>
                        </Select>
                      </div>
                      <div style={{ marginRight: 12 }}>
                        <Select
                          width={220}
                          mode={type === 'SECONDARY' ? 'multiple' : 'default'}
                          placeholder={t('addition.edge.property-select-desc')}
                          size="medium"
                          showSearch={false}
                          value={fields}
                          onChange={(value: string | string[]) => {
                            const propertyIndexEntities = cloneDeep(
                              vertexTypeStore.newVertexType.property_indexes
                            );

                            if (Array.isArray(value)) {
                              propertyIndexEntities[index].fields = value;
                            } else {
                              propertyIndexEntities[index].fields = [value];
                            }

                            vertexTypeStore.mutateNewProperty({
                              ...vertexTypeStore.newVertexType,
                              property_indexes: propertyIndexEntities
                            });

                            vertexTypeStore.validateAllNewVertexType(true);
                            vertexTypeStore.validateNewVertexType(
                              'propertyIndexes'
                            );
                          }}
                        >
                          {type === 'SECONDARY' &&
                            vertexTypeStore.newVertexType.properties
                              .filter(
                                (property) =>
                                  !vertexTypeStore.newVertexType.primary_keys.includes(
                                    property.name
                                  )
                              )
                              .map((property) => {
                                const order = vertexTypeStore.newVertexType.property_indexes[
                                  index
                                ].fields.findIndex(
                                  (name) => name === property.name
                                );

                                const multiSelectOptionClassName = classnames({
                                  'metadata-configs-sorted-multiSelect-option': true,
                                  'metadata-configs-sorted-multiSelect-option-selected':
                                    order !== -1
                                });

                                return (
                                  <Select.Option
                                    value={property.name}
                                    key={property.name}
                                  >
                                    <div className={multiSelectOptionClassName}>
                                      <div>{order !== -1 ? order + 1 : ''}</div>
                                      <div>{property.name}</div>
                                    </div>
                                  </Select.Option>
                                );
                              })}

                          {type === 'RANGE' &&
                            vertexTypeStore.newVertexType.properties
                              .filter((property) => {
                                const matchedProperty = metadataPropertyStore.metadataProperties.find(
                                  ({ name }) => name === property.name
                                );

                                if (!isUndefined(matchedProperty)) {
                                  const { data_type } = matchedProperty;

                                  return (
                                    data_type !== 'TEXT' &&
                                    data_type !== 'BOOLEAN' &&
                                    data_type !== 'UUID' &&
                                    data_type !== 'BLOB'
                                  );
                                }
                              })
                              .map(({ name }) => (
                                <Select.Option value={name} key={name}>
                                  {name}
                                </Select.Option>
                              ))}

                          {type === 'SEARCH' &&
                            vertexTypeStore.newVertexType.properties
                              .filter((property) => {
                                const matchedProperty = metadataPropertyStore.metadataProperties.find(
                                  ({ name }) => name === property.name
                                );

                                if (!isUndefined(matchedProperty)) {
                                  const { data_type } = matchedProperty;

                                  return data_type === 'TEXT';
                                }
                              })
                              .map(({ name }) => (
                                <Select.Option value={name} key={name}>
                                  {name}
                                </Select.Option>
                              ))}
                        </Select>
                      </div>

                      <CustomTooltip
                        placement="bottom-end"
                        tooltipShown={index === deletePopIndex}
                        modifiers={{
                          offset: {
                            offset: '0, 10'
                          }
                        }}
                        tooltipWrapperProps={{
                          className: 'metadata-properties-tooltips',
                          style: { zIndex: 1041 }
                        }}
                        tooltipWrapper={
                          <div ref={deleteWrapperRef}>
                            <p style={{ width: 200, lineHeight: '28px' }}>
                              {t('addition.message.property-del-confirm')}
                            </p>
                            <p style={{ width: 200, lineHeight: '28px' }}>
                              {t('addition.message.index-del-confirm')}
                            </p>
                            <div
                              style={{
                                display: 'flex',
                                marginTop: 12,
                                color: '#2b65ff',
                                cursor: 'pointer'
                              }}
                            >
                              <div
                                style={{ marginRight: 16, cursor: 'pointer' }}
                                onClick={() => {
                                  const propertyIndexEntities = cloneDeep(
                                    vertexTypeStore.newVertexType
                                      .property_indexes
                                  );

                                  propertyIndexEntities.splice(index, 1);

                                  vertexTypeStore.mutateNewProperty({
                                    ...vertexTypeStore.newVertexType,
                                    property_indexes: propertyIndexEntities
                                  });

                                  vertexTypeStore.validateAllNewVertexType(
                                    true
                                  );
                                  vertexTypeStore.validateNewVertexType(
                                    'propertyIndexes'
                                  );

                                  setDeletePopIndex(null);
                                }}
                              >
                                {t('addition.common.confirm')}
                              </div>
                              <div
                                onClick={() => {
                                  setDeletePopIndex(null);
                                }}
                              >
                                {t('addition.common.cancel')}
                              </div>
                            </div>
                          </div>
                        }
                        childrenProps={{
                          src: closeIcon,
                          alt: 'close',
                          style: { cursor: 'pointer' },
                          onClick() {
                            setDeletePopIndex(index);
                          }
                        }}
                        childrenWrapperElement="img"
                      />
                    </div>
                  )
                )}
                <span
                  onClick={() => {
                    if (
                      vertexTypeStore.newVertexType.property_indexes.length ===
                        0 ||
                      vertexTypeStore.isAddNewPropertyIndexReady
                    ) {
                      vertexTypeStore.mutateNewProperty({
                        ...vertexTypeStore.newVertexType,
                        property_indexes: [
                          ...vertexTypeStore.newVertexType.property_indexes,
                          {
                            name: '',
                            type: '',
                            fields: []
                          }
                        ]
                      });

                      vertexTypeStore.validateAllNewVertexType(true);
                      // set isAddNewPropertyIndexReady to false
                      vertexTypeStore.validateNewVertexType(
                        'propertyIndexes',
                        true
                      );
                    }
                  }}
                  style={{
                    cursor: 'pointer',
                    color: vertexTypeStore.isAddNewPropertyIndexReady
                      ? '#2b65ff'
                      : '#999',
                    lineHeight: '32px'
                  }}
                >
                  {t('addition.edge.add-group')}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Drawer>
  );
});

export default CreateVertex;
