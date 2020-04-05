import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback
} from 'react';
import { observer } from 'mobx-react';
import classnames from 'classnames';
import { cloneDeep, merge, isUndefined, isEmpty } from 'lodash-es';
import {
  Drawer,
  Button,
  Input,
  Select,
  Switch,
  Checkbox,
  Message
} from '@baidu/one-ui';
import TooltipTrigger from 'react-popper-tooltip';

import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import { EdgeTypeValidatePropertyIndexes } from '../../../../stores/types/GraphManagementStore/metadataConfigsStore';
import {
  mapMetadataProperties,
  generateGraphModeId
} from '../../../../stores/utils';

import BlueArrowIcon from '../../../../assets/imgs/ic_arrow_blue.svg';
import CloseIcon from '../../../../assets/imgs/ic_close_16.svg';

const propertyIndexTypeMappings: Record<string, string> = {
  SECONDARY: '二级索引',
  RANGE: '范围索引',
  SEARCH: '全文索引'
};

const CheckAndEditEdge: React.FC = observer(() => {
  const { metadataPropertyStore, edgeTypeStore, graphViewStore } = useContext(
    MetadataConfigsRootStore
  );
  const [isAddProperty, switchIsAddProperty] = useState(false);
  const [isDeletePop, switchDeletePop] = useState(false);
  const [
    deleteExistPopIndexInDrawer,
    setDeleteExistPopIndexInDrawer
  ] = useState<number | null>(null);
  const [
    deleteAddedPopIndexInDrawer,
    setDeleteAddedPopIndexInDrawer
  ] = useState<number | null>(null);

  const deleteWrapperRef = useRef<HTMLImageElement>(null);
  const dropdownWrapperRef = useRef<HTMLDivElement>(null);
  const deleteWrapperInDrawerRef = useRef<HTMLDivElement>(null);

  const isEditEdge = graphViewStore.currentDrawer === 'edit-edge';

  const metadataDrawerOptionClass = classnames({
    'metadata-drawer-options': true,
    'metadata-drawer-options-disabled': isEditEdge
  });

  // need useCallback to stop infinite callings of useEffect
  const handleOutSideClick = useCallback(
    (e: MouseEvent) => {
      const drawerWrapper = document.querySelector(
        '.new-fc-one-drawer-content-wrapper'
      );

      if (
        graphViewStore.currentDrawer === 'check-edge' &&
        drawerWrapper &&
        !drawerWrapper.contains(e.target as Element)
      ) {
        /*
          handleOutSideClick is being called after the value assignment of data and drawer-name,
          we need to judge whether a node or edge is being clicked
        */
        if (graphViewStore.isNodeOrEdgeClicked) {
          // if node/edge is clicked, reset state and prepare for next outside clicks
          graphViewStore.switchNodeOrEdgeClicked(false);
        } else {
          graphViewStore.setCurrentDrawer('');
        }
      }

      if (
        isEditEdge &&
        isAddProperty &&
        dropdownWrapperRef.current &&
        !dropdownWrapperRef.current.contains(e.target as Element)
      ) {
        switchIsAddProperty(false);
        return;
      }

      if (
        isDeletePop !== null &&
        deleteWrapperRef.current &&
        !deleteWrapperRef.current.contains(e.target as Element)
      ) {
        switchDeletePop(false);
      }
    },
    [graphViewStore, isEditEdge, isAddProperty, isDeletePop]
  );

  const handleCloseDrawer = () => {
    switchIsAddProperty(false);
    graphViewStore.setCurrentDrawer('');
    edgeTypeStore.selectEdgeType(null);
    // clear mutations in <Drawer />
    edgeTypeStore.resetEditedSelectedEdgeType();
  };

  const handleDeleteEdge = async () => {
    // cache vertex name here before it gets removed
    const edgeName = edgeTypeStore.selectedEdgeType!.name;
    const edgeId = generateGraphModeId(
      edgeName,
      edgeTypeStore.selectedEdgeType!.source_label,
      edgeTypeStore.selectedEdgeType!.target_label
    );

    switchIsAddProperty(false);
    graphViewStore.setCurrentDrawer('');
    switchDeletePop(false);
    edgeTypeStore.selectEdgeType(null);
    edgeTypeStore.resetEditedSelectedEdgeType();
    graphViewStore.visDataSet!.edges.remove(edgeId);

    await edgeTypeStore.deleteEdgeType(edgeName);

    if (edgeTypeStore.requestStatus.deleteEdgeType === 'success') {
      Message.success({
        content: '已删除未使用项',
        size: 'medium',
        showCloseIcon: false
      });

      edgeTypeStore.fetchEdgeTypeList();
    }

    if (edgeTypeStore.requestStatus.deleteEdgeType === 'failed') {
      Message.error({
        content: edgeTypeStore.errorMessage,
        size: 'medium',
        showCloseIcon: false
      });
    }
  };

  useEffect(() => {
    document.addEventListener('click', handleOutSideClick, false);

    return () => {
      document.removeEventListener('click', handleOutSideClick, false);
    };
  }, [handleOutSideClick]);

  if (edgeTypeStore.selectedEdgeType === null) {
    return null;
  }

  return (
    <Drawer
      title={!isEditEdge ? '边类型详情' : '编辑边类型'}
      width={580}
      destroyOnClose
      visible={['check-edge', 'edit-edge'].includes(
        graphViewStore.currentDrawer
      )}
      mask={isEditEdge}
      onClose={handleCloseDrawer}
      footer={[
        <Button
          type="primary"
          size="medium"
          style={{ width: 60 }}
          disabled={isEditEdge && !edgeTypeStore.isEditReady}
          onClick={async () => {
            if (!isEditEdge) {
              graphViewStore.setCurrentDrawer('edit-edge');
              edgeTypeStore.validateEditEdgeType();
            } else {
              const id = generateGraphModeId(
                edgeTypeStore.selectedEdgeType!.name,
                edgeTypeStore.selectedEdgeType!.source_label,
                edgeTypeStore.selectedEdgeType!.target_label
              );
              const updateInfo: Record<string, any> = {};

              if (
                !isEmpty(edgeTypeStore.editedSelectedEdgeType.append_properties)
              ) {
                const mappedProperties = mapMetadataProperties(
                  edgeTypeStore.selectedEdgeType!.properties,
                  metadataPropertyStore.metadataProperties
                );

                const newMappedProperties = mapMetadataProperties(
                  edgeTypeStore.editedSelectedEdgeType.append_properties,
                  metadataPropertyStore.metadataProperties
                );

                const mergedProperties = merge(
                  mappedProperties,
                  newMappedProperties
                );

                updateInfo.title = `
                  <div class="metadata-graph-view-tooltip-fields">
                    <div>边类型：</div>
                    <div style="min-width: 60px; max-width: 145px; marigin-right: 0">${id}</div>
                  </div>
                  <div class="metadata-graph-view-tooltip-fields">
                    <div style="max-width: 120px">关联属性及类型：</div>
                  </div>
                  ${Object.entries(mergedProperties)
                    .map(([key, value]) => {
                      const convertedValue =
                        value.toLowerCase() === 'text'
                          ? 'string'
                          : value.toLowerCase();

                      return `<div class="metadata-graph-view-tooltip-fields">
                        <div>${key}: </div>
                        <div>${convertedValue}</div>
                        <div></div>
                      </div>`;
                    })
                    .join('')}
                `;
              }

              if (edgeTypeStore.editedSelectedEdgeType.style.color !== null) {
                updateInfo.color = {
                  color: edgeTypeStore.editedSelectedEdgeType.style.color,
                  highlight: edgeTypeStore.editedSelectedEdgeType.style.color,
                  hover: edgeTypeStore.editedSelectedEdgeType.style.color
                };
              }

              if (!isEmpty(updateInfo)) {
                updateInfo.id = id;

                graphViewStore.visDataSet!.edges.update(updateInfo);
              }

              await edgeTypeStore.updateEdgeType();

              if (edgeTypeStore.requestStatus.updateEdgeType === 'failed') {
                Message.error({
                  content: edgeTypeStore.errorMessage,
                  size: 'medium',
                  showCloseIcon: false
                });

                return;
              }

              if (edgeTypeStore.requestStatus.updateEdgeType === 'success') {
                Message.success({
                  content: '修改成功',
                  size: 'medium',
                  showCloseIcon: false
                });
              }

              graphViewStore.visNetwork!.unselectAll();
              graphViewStore.setCurrentDrawer('');
              edgeTypeStore.selectEdgeType(null);
              edgeTypeStore.resetEditedSelectedEdgeType();
              edgeTypeStore.fetchEdgeTypeList();
            }
          }}
          key="drawer-manipulation"
        >
          {isEditEdge ? '保存' : '编辑'}
        </Button>,
        <TooltipTrigger
          tooltipShown={isDeletePop}
          placement="top-start"
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
                className: 'metadata-properties-tooltips',
                style: {
                  zIndex: 1042
                }
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
                <p>确认删除此边类型？</p>
                <p>删除后无法恢复，请谨慎操作。</p>
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
                    onClick={handleDeleteEdge}
                  >
                    确认
                  </div>
                  <div
                    onClick={() => {
                      switchDeletePop(false);
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
            <span
              {...getTriggerProps({
                ref: triggerRef,
                onClick() {
                  if (isEditEdge) {
                    handleCloseDrawer();
                    return;
                  }

                  switchDeletePop(true);
                }
              })}
              key="drawer-close"
            >
              <Button size="medium" style={{ width: 60 }}>
                {isEditEdge ? '关闭' : '删除'}
              </Button>
            </span>
          )}
        </TooltipTrigger>
      ]}
    >
      <div>
        <div className="metadata-configs-drawer">
          <div
            className="metadata-title"
            style={{ marginBottom: 16, width: 88, textAlign: 'right' }}
          >
            基础信息
          </div>
          <div
            className={metadataDrawerOptionClass}
            style={{ alignItems: 'center' }}
          >
            <div className="metadata-drawer-options-name">
              <span>边类型名称：</span>
            </div>
            {edgeTypeStore.selectedEdgeType!.name}
            <div className="new-vertex-type-options-colors">
              <Select
                width={66}
                size="medium"
                showSearch={false}
                disabled={!isEditEdge}
                value={
                  edgeTypeStore.editedSelectedEdgeType.style.color !== null
                    ? edgeTypeStore.editedSelectedEdgeType.style.color.toLowerCase()
                    : edgeTypeStore.selectedEdgeType!.style.color!.toLowerCase()
                }
                onChange={(value: string) => {
                  edgeTypeStore.mutateEditedSelectedEdgeType({
                    ...edgeTypeStore.editedSelectedEdgeType,
                    style: {
                      color: value,
                      icon: null
                    }
                  });
                }}
              >
                {edgeTypeStore.colorSchemas.map((color: string) => (
                  <Select.Option value={color} key={color}>
                    <div
                      className="new-vertex-type-options-color"
                      style={{
                        background: color,
                        marginTop: 6
                      }}
                    ></div>
                  </Select.Option>
                ))}
              </Select>
            </div>
          </div>
          <div className={metadataDrawerOptionClass}>
            <div className="metadata-drawer-options-name">
              <span>起点类型：</span>
            </div>
            {edgeTypeStore.selectedEdgeType!.source_label}
          </div>
          <div className={metadataDrawerOptionClass}>
            <div className="metadata-drawer-options-name">
              <span>终点类型：</span>
            </div>
            {edgeTypeStore.selectedEdgeType!.target_label}
          </div>
          <div className="metadata-drawer-options">
            <div className="metadata-drawer-options-name">
              <span>关联属性：</span>
            </div>
            <div className="metadata-drawer-options-list">
              <div className="metadata-drawer-options-list-row">
                <span>属性</span>
                <span>允许为空</span>
              </div>
              {edgeTypeStore.selectedEdgeType!.properties.map(
                ({ name, nullable }) => (
                  <div className="metadata-drawer-options-list-row" key={name}>
                    <div>{name}</div>
                    <div style={{ width: 70, textAlign: 'center' }}>
                      <Switch
                        checkedChildren="开"
                        unCheckedChildren="关"
                        checked={nullable}
                        size="large"
                        disabled
                      />
                    </div>
                  </div>
                )
              )}
              {isEditEdge &&
                edgeTypeStore.editedSelectedEdgeType.append_properties.map(
                  ({ name }) => (
                    <div
                      className="metadata-drawer-options-list-row"
                      key={name}
                    >
                      <div>{name}</div>
                      <div style={{ width: 70, textAlign: 'center' }}>
                        <Switch
                          checkedChildren="开"
                          unCheckedChildren="关"
                          checked={true}
                          size="large"
                          disabled
                        />
                      </div>
                    </div>
                  )
                )}
              {isEditEdge && (
                <div
                  className="metadata-drawer-options-list-row"
                  style={{
                    color: '#2b65ff',
                    cursor: 'pointer',
                    justifyContent: 'normal',
                    alignItems: 'center'
                  }}
                  onClick={() => {
                    switchIsAddProperty(!isAddProperty);
                  }}
                >
                  <span style={{ marginRight: 4 }}>添加属性</span>
                  <img src={BlueArrowIcon} alt="toogleAddProperties" />
                </div>
              )}
              {isEditEdge && isAddProperty && (
                <div
                  className="metadata-configs-content-dropdown"
                  ref={dropdownWrapperRef}
                >
                  {metadataPropertyStore.metadataProperties
                    .filter(
                      property =>
                        edgeTypeStore.selectedEdgeType!.properties.find(
                          ({ name }) => name === property.name
                        ) === undefined
                    )
                    .map(property => (
                      <div key={property.name}>
                        <span>
                          <Checkbox
                            checked={
                              [
                                ...edgeTypeStore.addedPropertiesInSelectedEdgeType
                              ].findIndex(
                                propertyIndex => propertyIndex === property.name
                              ) !== -1
                            }
                            onChange={() => {
                              const addedPropertiesInSelectedVertextType =
                                edgeTypeStore.addedPropertiesInSelectedEdgeType;

                              addedPropertiesInSelectedVertextType.has(
                                property.name
                              )
                                ? addedPropertiesInSelectedVertextType.delete(
                                    property.name
                                  )
                                : addedPropertiesInSelectedVertextType.add(
                                    property.name
                                  );

                              edgeTypeStore.mutateEditedSelectedEdgeType({
                                ...edgeTypeStore.editedSelectedEdgeType,
                                append_properties: [
                                  ...addedPropertiesInSelectedVertextType
                                ].map(propertyName => {
                                  const currentProperty = edgeTypeStore.newEdgeType.properties.find(
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
                            }}
                          >
                            {property.name}
                          </Checkbox>
                        </span>
                      </div>
                    ))}
                </div>
              )}
            </div>
          </div>

          <div className={metadataDrawerOptionClass}>
            <div className="metadata-drawer-options-name">
              <span>允许多次连接：</span>
            </div>
            <Switch
              checkedChildren="开"
              unCheckedChildren="关"
              checked={edgeTypeStore.selectedEdgeType!.link_multi_times}
              size="large"
              disabled
            />
          </div>
          <div className={metadataDrawerOptionClass}>
            <div className="metadata-drawer-options-name">
              <span>区分键属性：</span>
            </div>
            {edgeTypeStore.selectedEdgeType!.sort_keys.join(';')}
          </div>

          <div
            className="metadata-title"
            style={{
              marginTop: 40,
              marginBottom: 16,
              width: 88,
              textAlign: 'right'
            }}
          >
            索引信息
          </div>
          <div className={metadataDrawerOptionClass}>
            <div className="metadata-drawer-options-name">
              <span>类型索引：</span>
            </div>
            <Switch
              checkedChildren="开"
              unCheckedChildren="关"
              checked={edgeTypeStore.selectedEdgeType!.open_label_index}
              size="large"
              disabled
            />
          </div>
          <div className="metadata-drawer-options">
            <div className="metadata-drawer-options-name">
              <span>属性索引：</span>
            </div>
            <div className="metadata-drawer-options-list">
              {(edgeTypeStore.selectedEdgeType!.property_indexes.length !== 0 ||
                edgeTypeStore.editedSelectedEdgeType.append_property_indexes
                  .length !== 0) && (
                <div className="metadata-drawer-options-list-row metadata-drawer-options-list-row-normal">
                  <span>索引名称</span>
                  <span>索引类型</span>
                  <span>属性</span>
                </div>
              )}
              {edgeTypeStore
                .selectedEdgeType!.property_indexes.filter(propertyIndex =>
                  isUndefined(
                    edgeTypeStore.editedSelectedEdgeType.remove_property_indexes.find(
                      removedPropertyName =>
                        removedPropertyName === propertyIndex.name
                    )
                  )
                )
                .map(({ name, type, fields }, index) => {
                  return (
                    <div
                      className="metadata-drawer-options-list-row metadata-drawer-options-list-row-normal"
                      key={name}
                    >
                      <div>{name}</div>
                      <div>{propertyIndexTypeMappings[type]}</div>
                      <div
                        style={{
                          display: 'flex',
                          alignItems: 'center'
                        }}
                      >
                        <span
                          style={{
                            marginRight: 3,
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            overflow: 'hidden'
                          }}
                        >
                          {fields
                            .map((field, index) => index + 1 + '.' + field)
                            .join(';')}
                        </span>

                        {isEditEdge && (
                          <TooltipTrigger
                            tooltipShown={index === deleteExistPopIndexInDrawer}
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
                                  className: 'metadata-properties-tooltips',
                                  style: { zIndex: 1041 }
                                })}
                              >
                                <div
                                  {...getArrowProps({
                                    ref: arrowRef,
                                    className: 'tooltip-arrow',
                                    'data-placement': placement
                                  })}
                                />
                                <div ref={deleteWrapperInDrawerRef}>
                                  <p
                                    style={{
                                      width: 200,
                                      lineHeight: '28px'
                                    }}
                                  >
                                    确认删除此属性？
                                  </p>
                                  <p
                                    style={{
                                      width: 200,
                                      lineHeight: '28px'
                                    }}
                                  >
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
                                      style={{
                                        marginRight: 16,
                                        cursor: 'pointer'
                                      }}
                                      onClick={() => {
                                        const removedPropertyIndex = cloneDeep(
                                          edgeTypeStore.editedSelectedEdgeType
                                            .remove_property_indexes
                                        );

                                        removedPropertyIndex.push(
                                          edgeTypeStore.selectedEdgeType!
                                            .property_indexes[index].name
                                        );

                                        edgeTypeStore.mutateEditedSelectedEdgeType(
                                          {
                                            ...edgeTypeStore.editedSelectedEdgeType,
                                            remove_property_indexes: removedPropertyIndex
                                          }
                                        );

                                        setDeleteExistPopIndexInDrawer(null);
                                        edgeTypeStore.validateEditEdgeType(
                                          true
                                        );
                                      }}
                                    >
                                      确认
                                    </div>
                                    <div
                                      onClick={() => {
                                        setDeleteExistPopIndexInDrawer(null);
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
                                  src: CloseIcon,
                                  alt: 'close',
                                  style: { cursor: 'pointer' },
                                  onClick() {
                                    setDeleteExistPopIndexInDrawer(index);
                                  }
                                })}
                              />
                            )}
                          </TooltipTrigger>
                        )}
                      </div>
                    </div>
                  );
                })}
              {edgeTypeStore.editedSelectedEdgeType.append_property_indexes.map(
                ({ name, type, fields }, index) => {
                  return (
                    <div
                      className="metadata-drawer-options-list-row metadata-drawer-options-list-row-normal"
                      style={{
                        display: 'flex',
                        alignItems: 'start',
                        position: 'relative'
                      }}
                      // cannot set key prop with name here, weired
                    >
                      <div>
                        <Input
                          size="medium"
                          width={100}
                          placeholder="索引名称"
                          errorLocation="layer"
                          errorMessage={
                            edgeTypeStore.validateEditEdgeTypeErrorMessage
                              .propertyIndexes.length !== 0
                              ? (edgeTypeStore.validateEditEdgeTypeErrorMessage
                                  .propertyIndexes[
                                  index
                                ] as EdgeTypeValidatePropertyIndexes).name
                              : ''
                          }
                          value={name}
                          onChange={(e: any) => {
                            const propertyIndexEntities = cloneDeep(
                              edgeTypeStore.editedSelectedEdgeType
                                .append_property_indexes
                            );

                            propertyIndexEntities[index].name = e.value;

                            edgeTypeStore.mutateEditedSelectedEdgeType({
                              ...edgeTypeStore.editedSelectedEdgeType,
                              append_property_indexes: propertyIndexEntities
                            });
                          }}
                          originInputProps={{
                            onBlur() {
                              // check is ready to create
                              edgeTypeStore.validateEditEdgeType();
                            }
                          }}
                        />
                      </div>
                      <div>
                        <Select
                          width={110}
                          placeholder="请选择索引类型"
                          size="medium"
                          showSearch={false}
                          value={type === '' ? [] : type}
                          onChange={(value: string) => {
                            const propertyIndexEntities = cloneDeep(
                              edgeTypeStore.editedSelectedEdgeType
                                .append_property_indexes
                            );

                            propertyIndexEntities[index].type = value;

                            edgeTypeStore.mutateEditedSelectedEdgeType({
                              ...edgeTypeStore.editedSelectedEdgeType,
                              append_property_indexes: propertyIndexEntities
                            });

                            edgeTypeStore.validateEditEdgeType();
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
                      <div>
                        <Select
                          width={120}
                          mode={type === 'SECONDARY' ? 'multiple' : 'default'}
                          placeholder="请选择属性"
                          size="medium"
                          showSearch={false}
                          value={fields}
                          onChange={(value: string | string[]) => {
                            const propertyIndexEntities = cloneDeep(
                              edgeTypeStore.editedSelectedEdgeType
                                .append_property_indexes
                            );

                            if (Array.isArray(value)) {
                              propertyIndexEntities[index].fields = value;
                            } else {
                              propertyIndexEntities[index].fields = [value];
                            }

                            edgeTypeStore.mutateEditedSelectedEdgeType({
                              ...edgeTypeStore.editedSelectedEdgeType,
                              append_property_indexes: propertyIndexEntities
                            });

                            edgeTypeStore.validateEditEdgeType();
                          }}
                        >
                          {type === 'SECONDARY' &&
                            edgeTypeStore
                              .selectedEdgeType!.properties.concat(
                                edgeTypeStore.editedSelectedEdgeType
                                  .append_properties
                              )
                              .map(property => {
                                const order = edgeTypeStore.editedSelectedEdgeType.append_property_indexes[
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
                            edgeTypeStore
                              .selectedEdgeType!.properties.concat(
                                edgeTypeStore.editedSelectedEdgeType
                                  .append_properties
                              )
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
                            edgeTypeStore
                              .selectedEdgeType!.properties.concat(
                                edgeTypeStore.editedSelectedEdgeType
                                  .append_properties
                              )
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

                        <TooltipTrigger
                          tooltipShown={index === deleteAddedPopIndexInDrawer}
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
                                className: 'metadata-properties-tooltips',
                                style: { zIndex: 1041 }
                              })}
                            >
                              <div
                                {...getArrowProps({
                                  ref: arrowRef,
                                  className: 'tooltip-arrow',
                                  'data-placement': placement
                                })}
                              />
                              <div ref={deleteWrapperInDrawerRef}>
                                <p
                                  style={{
                                    width: 200,
                                    lineHeight: '28px'
                                  }}
                                >
                                  确认删除此属性？
                                </p>
                                <p
                                  style={{
                                    width: 200,
                                    lineHeight: '28px'
                                  }}
                                >
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
                                    style={{
                                      marginRight: 16,
                                      cursor: 'pointer'
                                    }}
                                    onClick={() => {
                                      const appendPropertyIndexes = cloneDeep(
                                        edgeTypeStore.editedSelectedEdgeType!
                                          .append_property_indexes
                                      );

                                      appendPropertyIndexes.splice(index, 1);

                                      edgeTypeStore.mutateEditedSelectedEdgeType(
                                        {
                                          ...edgeTypeStore.editedSelectedEdgeType,
                                          append_property_indexes: appendPropertyIndexes
                                        }
                                      );

                                      setDeleteAddedPopIndexInDrawer(null);
                                      edgeTypeStore.validateEditEdgeType(true);
                                    }}
                                  >
                                    确认
                                  </div>
                                  <div
                                    onClick={() => {
                                      edgeTypeStore.resetEditedSelectedEdgeType();
                                      setDeleteAddedPopIndexInDrawer(null);
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
                                src: CloseIcon,
                                alt: 'close',
                                style: { cursor: 'pointer' },
                                onClick() {
                                  setDeleteAddedPopIndexInDrawer(index);
                                }
                              })}
                            />
                          )}
                        </TooltipTrigger>
                      </div>
                    </div>
                  );
                }
              )}
              {isEditEdge && (
                <div
                  onClick={() => {
                    if (
                      edgeTypeStore.editedSelectedEdgeType
                        .append_property_indexes.length === 0 ||
                      edgeTypeStore.isEditReady
                    ) {
                      edgeTypeStore.mutateEditedSelectedEdgeType({
                        ...edgeTypeStore.editedSelectedEdgeType,
                        append_property_indexes: [
                          ...edgeTypeStore.editedSelectedEdgeType
                            .append_property_indexes,
                          {
                            name: '',
                            type: '',
                            fields: []
                          }
                        ]
                      });

                      edgeTypeStore.validateEditEdgeType(true);
                    }
                  }}
                  style={{
                    cursor: 'pointer',
                    color: edgeTypeStore.isEditReady ? '#2b65ff' : '#999'
                  }}
                >
                  新增一组
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </Drawer>
  );
});

export default CheckAndEditEdge;
