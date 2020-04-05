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
} from '@baidu/one-ui';
import TooltipTrigger from 'react-popper-tooltip';

import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore';
import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import { VertexTypeValidatePropertyIndexes } from '../../../../stores/types/GraphManagementStore/metadataConfigsStore';
import { mapMetadataProperties } from '../../../../stores/utils';

import BlueArrowIcon from '../../../../assets/imgs/ic_arrow_blue.svg';
import HintIcon from '../../../../assets/imgs/ic_question_mark.svg';
import closeIcon from '../../../../assets/imgs/ic_close_16.svg';

const CreateVertex: React.FC = observer(() => {
  const dataAnalyzeStore = useContext(DataAnalyzeStore);
  const { metadataPropertyStore, vertexTypeStore, graphViewStore } = useContext(
    MetadataConfigsRootStore
  );
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
      title="创建顶点类型"
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
              title: `
                <div class="metadata-graph-view-tooltip-fields">
                  <div>顶点类型：</div>
                  <div style="min-width: 60px; max-width: 145px; marigin-right: 0">${name}</div>
                </div>
                <div class="metadata-graph-view-tooltip-fields">
                  <div style="max-width: 120px">关联属性及类型：</div>
                </div>
                ${Object.entries(mappedProperties)
                  .map(([key, value]) => {
                    const convertedValue =
                      value.toLowerCase() === 'text'
                        ? 'string'
                        : value.toLowerCase();

                    const primaryKeyIndex = primary_keys.findIndex(
                      primaryKey => primaryKey === key
                    );

                    return `<div class="metadata-graph-view-tooltip-fields">
                          <div>${key}: </div>
                          <div>${convertedValue}</div>
                          <div>${
                            primaryKeyIndex === -1
                              ? ''
                              : `(主键${primaryKeyIndex + 1})`
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
                    values.size = 30;
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
                content: '创建成功',
                size: 'medium',
                showCloseIcon: false
              });

              // if vertex is empty before, trigger re-render here to reveal <GraphDataView />
              if (graphViewStore.isGraphVertexEmpty) {
                graphViewStore.switchGraphDataEmpty(false);
                // need to get node colors again since fetchGraphViewData() will cause re-render in <GraphView />
                // the graph use graphNode() rather than local added node
                await dataAnalyzeStore.fetchAllNodeColors();
                graphViewStore.fetchGraphViewData(
                  dataAnalyzeStore.colorMappings
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
          创建
        </Button>,
        <Button size="medium" style={{ width: 60 }} onClick={handleCloseDrawer}>
          取消
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
              基础信息
            </div>
            <div className="metadata-graph-drawer-options">
              <div
                className="metadata-graph-drawer-options-name"
                style={{ width: 95, marginRight: 14 }}
              >
                <span className="metdata-essential-form-options">*</span>
                <span>顶点类型名称:</span>
              </div>
              <Input
                size="medium"
                width={356}
                maxLen={128}
                placeholder="允许出现中英文、数字、下划线"
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
              <div className="metadata-graph-drawer-options-colors">
                <Select
                  width={66}
                  size="medium"
                  value={vertexTypeStore.newVertexType.style.color}
                  onChange={(value: string) => {
                    vertexTypeStore.mutateNewProperty({
                      ...vertexTypeStore.newVertexType,
                      style: {
                        ...vertexTypeStore.newVertexType.style,
                        color: value
                      }
                    });
                  }}
                >
                  {vertexTypeStore.colorSchemas.map((color: string) => (
                    <Select.Option value={color} key={color}>
                      <div
                        className="metadata-graph-drawer-options-color"
                        style={{
                          background: color
                        }}
                      ></div>
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
                <span>ID策略:</span>
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
                <Radio.Button value="PRIMARY_KEY">主键ID</Radio.Button>
                <Radio.Button value="AUTOMATIC">自动生成</Radio.Button>
                <Radio.Button value="CUSTOMIZE_STRING">
                  自定义字符串
                </Radio.Button>
                <Radio.Button value="CUSTOMIZE_NUMBER">自定义数字</Radio.Button>
                <Radio.Button value="CUSTOMIZE_UUID">自定义UUID</Radio.Button>
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
                <span>关联属性:</span>
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
                      <div>属性</div>
                      <div>允许为空</div>
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
                  <span>添加属性</span>
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
                  {metadataPropertyStore.metadataProperties.map(property => (
                    <div key={property.name}>
                      <span>
                        <Checkbox
                          checked={
                            [
                              ...vertexTypeStore.addedPropertiesInSelectedVertextType
                            ].findIndex(
                              propertyIndex => propertyIndex === property.name
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
                              ].map(propertyName => {
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
                  <span>主键属性:</span>
                </div>
                <Select
                  width={356}
                  mode="multiple"
                  placeholder="请选择主键属性"
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
                    .map(item => {
                      const order = vertexTypeStore.newVertexType.primary_keys.findIndex(
                        name => name === item.name
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
              <span style={{ marginRight: 5 }}>索引信息</span>
              <Tooltip
                placement="right"
                title="开启索引会影响使用性能，请按需开启"
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
                <span>类型索引:</span>
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
                <span>属性索引:</span>
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
                    <div style={{ width: 110, marginRight: 12 }}>索引名称</div>
                    <div style={{ width: 130, marginRight: 12 }}>索引类型</div>
                    <div>属性</div>
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
                          placeholder="索引名称"
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
                          placeholder="请选择索引类型"
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
                            二级索引
                          </Select.Option>
                          <Select.Option value="RANGE" key="RANGE">
                            范围索引
                          </Select.Option>
                          <Select.Option value="SEARCH" key="SEARCH">
                            全文索引
                          </Select.Option>
                        </Select>
                      </div>
                      <div style={{ marginRight: 12 }}>
                        <Select
                          width={220}
                          mode={type === 'SECONDARY' ? 'multiple' : 'default'}
                          placeholder="请选择属性"
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
                                property =>
                                  !vertexTypeStore.newVertexType.primary_keys.includes(
                                    property.name
                                  )
                              )
                              .map(property => {
                                const order = vertexTypeStore.newVertexType.property_indexes[
                                  index
                                ].fields.findIndex(
                                  name => name === property.name
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
                              .filter(property => {
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
                              .filter(property => {
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

                      <TooltipTrigger
                        tooltipShown={index === deletePopIndex}
                        placement="bottom-end"
                        tooltip={({
                          arrowRef,
                          tooltipRef,
                          getArrowProps,
                          getTooltipProps,
                          placement
                        }) => (
                          <div
                            {...getTooltipProps({
                              ref: tooltipRef,
                              className: 'metadata-properties-tooltips'
                            })}
                          >
                            <div
                              {...getArrowProps({
                                ref: arrowRef,
                                className: 'tooltip-arrow',
                                'data-placement': placement
                              })}
                            />
                            <div ref={deleteWrapperRef}>
                              <p style={{ width: 200, lineHeight: '28px' }}>
                                确认删除此属性？
                              </p>
                              <p style={{ width: 200, lineHeight: '28px' }}>
                                删除索引后，无法根据此属性索引进行查询，请谨慎操作。
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
                                  确认
                                </div>
                                <div
                                  onClick={() => {
                                    setDeletePopIndex(null);
                                  }}
                                >
                                  取消
                                </div>
                              </div>
                            </div>
                          </div>
                        )}
                      >
                        {({ getTriggerProps, triggerRef }) => (
                          <img
                            {...getTriggerProps({
                              ref: triggerRef,
                              src: closeIcon,
                              alt: 'close',
                              style: { cursor: 'pointer' },
                              onClick() {
                                setDeletePopIndex(index);
                              }
                            })}
                          />
                        )}
                      </TooltipTrigger>
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
                  新增一组
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
