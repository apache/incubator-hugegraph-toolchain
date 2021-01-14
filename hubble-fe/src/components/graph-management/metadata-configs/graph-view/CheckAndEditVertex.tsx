import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback
} from 'react';
import { observer } from 'mobx-react';
import classnames from 'classnames';
import { cloneDeep, isUndefined, merge, isEmpty } from 'lodash-es';
import {
  Drawer,
  Button,
  Input,
  Select,
  Switch,
  Checkbox,
  Message
} from '@baidu/one-ui';

import { Tooltip } from '../../../common';
import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import {
  mapMetadataProperties,
  formatVertexIdText,
  vertexRadiusMapping
} from '../../../../stores/utils';

import type {
  VertexTypeValidatePropertyIndexes,
  VertexTypeProperty
} from '../../../../stores/types/GraphManagementStore/metadataConfigsStore';

import BlueArrowIcon from '../../../../assets/imgs/ic_arrow_blue.svg';
import CloseIcon from '../../../../assets/imgs/ic_close_16.svg';

const IDStrategyMappings: Record<string, string> = {
  PRIMARY_KEY: '主键ID',
  AUTOMATIC: '自动生成',
  CUSTOMIZE_STRING: '自定义字符串',
  CUSTOMIZE_NUMBER: '自定义数字',
  CUSTOMIZE_UUID: '自定义UUID'
};

const propertyIndexTypeMappings: Record<string, string> = {
  SECONDARY: '二级索引',
  RANGE: '范围索引',
  SEARCH: '全文索引'
};

const CheckAndEditVertex: React.FC = observer(() => {
  const { metadataPropertyStore, vertexTypeStore, graphViewStore } = useContext(
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

  const isEditVertex = graphViewStore.currentDrawer === 'edit-vertex';

  const metadataDrawerOptionClass = classnames({
    'metadata-drawer-options': true,
    'metadata-drawer-options-disabled': isEditVertex
  });

  // need useCallback to stop infinite callings of useEffect
  const handleOutSideClick = useCallback(
    (e: MouseEvent) => {
      const drawerWrapper = document.querySelector(
        '.new-fc-one-drawer-content-wrapper'
      );

      if (
        graphViewStore.currentDrawer === 'check-vertex' &&
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
        isAddProperty &&
        isEditVertex &&
        dropdownWrapperRef.current &&
        !dropdownWrapperRef.current.contains(e.target as Element)
      ) {
        switchIsAddProperty(false);
        return;
      }

      if (
        isDeletePop &&
        deleteWrapperRef.current &&
        !deleteWrapperRef.current.contains(e.target as Element)
      ) {
        switchDeletePop(false);
      }
    },
    [graphViewStore, isAddProperty, isEditVertex, isDeletePop]
  );

  const handleCloseDrawer = () => {
    switchIsAddProperty(false);
    graphViewStore.setCurrentDrawer('');
    vertexTypeStore.selectVertexType(null);
    // clear mutations in <Drawer />
    vertexTypeStore.resetEditedSelectedVertexType();
  };

  const handleDeleteVertex = async () => {
    // cache vertex name here before it gets removed
    const vertexName = vertexTypeStore.selectedVertexType!.name;
    const vertexInfo = graphViewStore.visDataSet?.nodes.get(vertexName);
    const connectedEdgeInfos = graphViewStore.visDataSet?.edges.get(
      graphViewStore.visNetwork?.getConnectedEdges(vertexName)
    );

    // close
    handleCloseDrawer();
    switchDeletePop(false);

    // if node > 1, delete node on local before send request
    if (graphViewStore.visDataSet!.nodes.length > 1) {
      graphViewStore.visDataSet!.nodes.remove(vertexName);
    }

    await vertexTypeStore.deleteVertexType([vertexName]);

    if (vertexTypeStore.requestStatus.deleteVertexType === 'success') {
      Message.success({
        content: '删除成功',
        size: 'medium',
        showCloseIcon: false
      });

      vertexTypeStore.fetchVertexTypeList({ fetchAll: true });

      // if delete the last node, fetch graph data to trigger re-render to reveal <EmptyDataView />
      if (graphViewStore.visDataSet?.nodes.length === 1) {
        graphViewStore.switchGraphDataEmpty(true);
        graphViewStore.fetchGraphViewData();
      }
    }

    if (vertexTypeStore.requestStatus.deleteVertexType === 'failed') {
      Message.error({
        content: vertexTypeStore.errorMessage,
        size: 'medium',
        showCloseIcon: false
      });

      // if failed, re-add vertex and edges
      graphViewStore.visDataSet!.nodes.add(vertexInfo);
      graphViewStore.visDataSet!.edges.add(connectedEdgeInfos);
    }
  };

  useEffect(() => {
    document.addEventListener('click', handleOutSideClick, false);

    return () => {
      document.removeEventListener('click', handleOutSideClick, false);
    };
  }, [handleOutSideClick]);

  if (vertexTypeStore.selectedVertexType === null) {
    return null;
  }

  return (
    <Drawer
      title={!isEditVertex ? '顶点类型详情' : '编辑顶点类型'}
      width={580}
      destroyOnClose
      visible={['check-vertex', 'edit-vertex'].includes(
        graphViewStore.currentDrawer
      )}
      mask={isEditVertex}
      onClose={handleCloseDrawer}
      footer={[
        <Button
          type="primary"
          size="medium"
          style={{ width: 60 }}
          disabled={
            isEditVertex &&
            (vertexTypeStore.editedSelectedVertexType.style.display_fields
              .length === 0 ||
              !vertexTypeStore.isEditReady)
          }
          onClick={async () => {
            if (!isEditVertex) {
              graphViewStore.setCurrentDrawer('edit-vertex');
              vertexTypeStore.validateEditVertexType();
            } else {
              const id = vertexTypeStore.selectedVertexType!.name;
              const updateInfo: Record<string, any> = {};

              if (
                !isEmpty(
                  vertexTypeStore.editedSelectedVertexType.append_properties
                )
              ) {
                const mappedProperties = mapMetadataProperties(
                  vertexTypeStore.selectedVertexType!.properties,
                  metadataPropertyStore.metadataProperties
                );

                const newMappedProperties = mapMetadataProperties(
                  vertexTypeStore.editedSelectedVertexType.append_properties,
                  metadataPropertyStore.metadataProperties
                );

                const mergedProperties = merge(
                  mappedProperties,
                  newMappedProperties
                );

                updateInfo.title = `
                  <div class="metadata-graph-view-tooltip-fields">
                    <div>顶点类型：</div>
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

              if (
                vertexTypeStore.editedSelectedVertexType.style.color !== null
              ) {
                updateInfo.color = {
                  background:
                    vertexTypeStore.editedSelectedVertexType.style.color,
                  border: vertexTypeStore.editedSelectedVertexType.style.color
                };
              }

              if (
                vertexTypeStore.editedSelectedVertexType.style.size !== null
              ) {
                updateInfo.value =
                  vertexRadiusMapping[
                    vertexTypeStore.editedSelectedVertexType.style.size
                  ];
              }

              if (!isEmpty(updateInfo)) {
                updateInfo.id = id;

                graphViewStore.visDataSet!.nodes.update(updateInfo);
              }
              await vertexTypeStore.updateVertexType();

              if (vertexTypeStore.requestStatus.updateVertexType === 'failed') {
                Message.error({
                  content: vertexTypeStore.errorMessage,
                  size: 'medium',
                  showCloseIcon: false
                });

                return;
              }

              if (
                vertexTypeStore.requestStatus.updateVertexType === 'success'
              ) {
                Message.success({
                  content: '修改成功',
                  size: 'medium',
                  showCloseIcon: false
                });
              }

              handleCloseDrawer();
              graphViewStore.visNetwork!.unselectAll();
              vertexTypeStore.fetchVertexTypeList({ fetchAll: true });
            }
          }}
          key="drawer-manipulation"
        >
          {isEditVertex ? '保存' : '编辑'}
        </Button>,
        <Tooltip
          placement="top-start"
          tooltipShown={isDeletePop}
          modifiers={{
            offset: {
              offset: '0, 15'
            }
          }}
          tooltipWrapperProps={{
            className: 'metadata-properties-tooltips',
            style: {
              zIndex: 1042
            }
          }}
          tooltipWrapper={
            <div ref={deleteWrapperRef}>
              {vertexTypeStore.vertexTypeUsingStatus &&
              vertexTypeStore.vertexTypeUsingStatus[
                vertexTypeStore.selectedVertexType!.name
              ] ? (
                <p style={{ width: 200 }}>当前顶点类型正在使用中，不可删除。</p>
              ) : (
                <>
                  <p style={{ marginBottom: 8 }}>确认删除此顶点类型？</p>
                  <p>删除后无法恢复，请谨慎操作</p>
                  <p>删除元数据耗时较久，详情可在任务管理中查看</p>
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
                      onClick={handleDeleteVertex}
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
                </>
              )}
            </div>
          }
          childrenProps={{
            onClick() {
              if (isEditVertex) {
                handleCloseDrawer();
                return;
              }

              switchDeletePop(true);
            }
          }}
        >
          <Button size="medium" style={{ width: 60 }}>
            {isEditVertex ? '关闭' : '删除'}
          </Button>
        </Tooltip>
      ]}
    >
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
            <span>顶点类型名称：</span>
          </div>
          {vertexTypeStore.selectedVertexType!.name}
        </div>

        <div className="metadata-drawer-options">
          <div className="metadata-drawer-options-name">
            <span
              className={
                isEditVertex ? 'metadata-drawer-options-name-edit' : ''
              }
            >
              顶点样式：
            </span>
          </div>
          <div className="new-vertex-type-options-colors">
            <Select
              width={66}
              size="medium"
              prefixCls="new-fc-one-select-another"
              dropdownMatchSelectWidth={false}
              showSearch={false}
              disabled={!isEditVertex}
              value={
                <div
                  className="new-vertex-type-select"
                  style={{
                    background:
                      vertexTypeStore.editedSelectedVertexType.style.color !==
                      null
                        ? vertexTypeStore.editedSelectedVertexType.style.color.toLowerCase()
                        : vertexTypeStore.selectedVertexType!.style.color!.toLowerCase(),
                    marginTop: 5
                  }}
                ></div>
              }
              onChange={(value: string) => {
                vertexTypeStore.mutateEditedSelectedVertexType({
                  ...vertexTypeStore.editedSelectedVertexType,
                  style: {
                    color: value,
                    icon: null,
                    size:
                      vertexTypeStore.editedSelectedVertexType.style.size !==
                      null
                        ? vertexTypeStore.editedSelectedVertexType.style.size
                        : vertexTypeStore.selectedVertexType!.style.size,
                    display_fields:
                      vertexTypeStore.editedSelectedVertexType.style
                        .display_fields.length !== 0
                        ? vertexTypeStore.editedSelectedVertexType.style
                            .display_fields
                        : vertexTypeStore.selectedVertexType!.style
                            .display_fields
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
                        (vertexTypeStore.editedSelectedVertexType.style
                          .color !== null
                          ? vertexTypeStore.editedSelectedVertexType.style.color.toLowerCase()
                          : vertexTypeStore.selectedVertexType!.style.color!.toLowerCase()) ===
                        color
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
              showSearch={false}
              disabled={!isEditVertex}
              style={{ paddingLeft: 7 }}
              value={
                vertexTypeStore.editedSelectedVertexType.style.size !== null
                  ? vertexTypeStore.editedSelectedVertexType.style.size
                  : vertexTypeStore.selectedVertexType!.style.size
              }
              onChange={(value: string) => {
                vertexTypeStore.mutateEditedSelectedVertexType({
                  ...vertexTypeStore.editedSelectedVertexType,
                  style: {
                    color:
                      vertexTypeStore.editedSelectedVertexType.style.color !==
                      null
                        ? vertexTypeStore.editedSelectedVertexType.style.color.toLowerCase()
                        : vertexTypeStore.selectedVertexType!.style.color!.toLowerCase(),
                    icon: null,
                    display_fields:
                      vertexTypeStore.editedSelectedVertexType.style
                        .display_fields.length !== 0
                        ? vertexTypeStore.editedSelectedVertexType.style
                            .display_fields
                        : vertexTypeStore.selectedVertexType!.style
                            .display_fields,
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
        <div className={metadataDrawerOptionClass}>
          <div className="metadata-drawer-options-name">
            <span>ID策略：</span>
          </div>
          {IDStrategyMappings[vertexTypeStore.selectedVertexType!.id_strategy]}
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
            {vertexTypeStore.selectedVertexType!.properties.map(
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
            {isEditVertex &&
              vertexTypeStore.editedSelectedVertexType.append_properties.map(
                ({ name }) => (
                  <div className="metadata-drawer-options-list-row" key={name}>
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
            {isEditVertex && (
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
            {isEditVertex && isAddProperty && (
              <div
                className="metadata-configs-content-dropdown"
                ref={dropdownWrapperRef}
              >
                {metadataPropertyStore.metadataProperties
                  .filter(
                    (property) =>
                      vertexTypeStore.selectedVertexType!.properties.find(
                        ({ name }) => name === property.name
                      ) === undefined
                  )
                  .map((property) => (
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

                            vertexTypeStore.mutateEditedSelectedVertexType({
                              ...vertexTypeStore.editedSelectedVertexType,
                              append_properties: [
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
            <span>主键属性：</span>
          </div>
          {vertexTypeStore.selectedVertexType!.primary_keys.join(';')}
        </div>
        <div className="metadata-drawer-options">
          <div className="metadata-drawer-options-name">
            <span
              className={
                isEditVertex ? 'metadata-drawer-options-name-edit' : ''
              }
            >
              顶点展示内容：
            </span>
          </div>
          {isEditVertex ? (
            <Select
              width={420}
              mode="multiple"
              size="medium"
              showSearch={false}
              disabled={!isEditVertex}
              placeholder="请选择顶点展示内容"
              onChange={(value: string[]) => {
                vertexTypeStore.mutateEditedSelectedVertexType({
                  ...vertexTypeStore.editedSelectedVertexType,
                  style: {
                    ...vertexTypeStore.editedSelectedVertexType.style,
                    display_fields: value.map((field) =>
                      formatVertexIdText(field, '顶点ID', true)
                    )
                  }
                });

                vertexTypeStore.validateEditVertexType();
              }}
              value={vertexTypeStore.editedSelectedVertexType.style.display_fields.map(
                (field) => formatVertexIdText(field, '顶点ID')
              )}
            >
              {vertexTypeStore.selectedVertexType?.properties
                .concat({ name: '~id', nullable: false })
                .concat(
                  vertexTypeStore.editedSelectedVertexType.append_properties
                )
                .filter(({ nullable }) => !nullable)
                .map((item) => {
                  const order = vertexTypeStore.editedSelectedVertexType.style.display_fields.findIndex(
                    (name) => name === item.name
                  );

                  const multiSelectOptionClassName = classnames({
                    'metadata-configs-sorted-multiSelect-option': true,
                    'metadata-configs-sorted-multiSelect-option-selected':
                      order !== -1
                  });

                  return (
                    <Select.Option
                      value={formatVertexIdText(item.name, '顶点ID')}
                      key={item.name}
                    >
                      <div className={multiSelectOptionClassName}>
                        <div
                          style={{
                            backgroundColor: vertexTypeStore.editedSelectedVertexType.style.display_fields.includes(
                              item.name
                            )
                              ? '#2b65ff'
                              : '#fff',
                            borderColor: vertexTypeStore.editedSelectedVertexType.style.display_fields.includes(
                              item.name
                            )
                              ? '#fff'
                              : '#e0e0e0'
                          }}
                        >
                          {order !== -1 ? order + 1 : ''}
                        </div>
                        <div style={{ color: '#333' }}>
                          {formatVertexIdText(item.name, '顶点ID')}
                        </div>
                      </div>
                    </Select.Option>
                  );
                })}
            </Select>
          ) : (
            <div>
              {vertexTypeStore.selectedVertexType?.style.display_fields
                .map((field) => formatVertexIdText(field, '顶点ID'))
                .join('-')}
            </div>
          )}
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
            checked={vertexTypeStore.selectedVertexType!.open_label_index}
            size="large"
            disabled
          />
        </div>
        <div className="metadata-drawer-options">
          <div className="metadata-drawer-options-name">
            <span>属性索引：</span>
          </div>
          <div className="metadata-drawer-options-list">
            {(vertexTypeStore.selectedVertexType!.property_indexes.length !==
              0 ||
              vertexTypeStore.editedSelectedVertexType.append_property_indexes
                .length !== 0) && (
              <div className="metadata-drawer-options-list-row metadata-drawer-options-list-row-normal">
                <span>索引名称</span>
                <span>索引类型</span>
                <span>属性</span>
              </div>
            )}
            {vertexTypeStore
              .selectedVertexType!.property_indexes.filter((propertyIndex) =>
                isUndefined(
                  vertexTypeStore.editedSelectedVertexType.remove_property_indexes.find(
                    (removedPropertyName) =>
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
                    <div style={{ display: 'flex', alignItems: 'center' }}>
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
                      {isEditVertex && (
                        <Tooltip
                          placement="bottom-end"
                          tooltipShown={index === deleteExistPopIndexInDrawer}
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
                                    const removedPropertyIndexes = cloneDeep(
                                      vertexTypeStore.editedSelectedVertexType
                                        .remove_property_indexes
                                    );

                                    removedPropertyIndexes.push(
                                      vertexTypeStore.selectedVertexType!
                                        .property_indexes[index].name
                                    );

                                    vertexTypeStore.mutateEditedSelectedVertexType(
                                      {
                                        ...vertexTypeStore.editedSelectedVertexType,
                                        remove_property_indexes: removedPropertyIndexes
                                      }
                                    );

                                    setDeleteExistPopIndexInDrawer(null);
                                    vertexTypeStore.validateEditVertexType(
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
                          }
                          childrenProps={{
                            src: CloseIcon,
                            alt: 'close',
                            style: { cursor: 'pointer' },
                            onClick() {
                              setDeleteExistPopIndexInDrawer(index);
                            }
                          }}
                        />
                      )}
                    </div>
                  </div>
                );
              })}
            {vertexTypeStore.editedSelectedVertexType.append_property_indexes.map(
              ({ name, type, fields }, index) => {
                return (
                  <div
                    className="metadata-drawer-options-list-row metadata-drawer-options-list-row-normal"
                    style={{
                      display: 'flex',
                      alignItems: 'start',
                      position: 'relative'
                    }}
                  >
                    <div>
                      <Input
                        size="medium"
                        width={100}
                        placeholder="索引名称"
                        errorLocation="layer"
                        errorMessage={
                          vertexTypeStore.validateEditVertexTypeErrorMessage
                            .propertyIndexes.length !== 0
                            ? (vertexTypeStore
                                .validateEditVertexTypeErrorMessage
                                .propertyIndexes[
                                index
                              ] as VertexTypeValidatePropertyIndexes).name
                            : ''
                        }
                        value={name}
                        onChange={(e: any) => {
                          const propertyIndexEntities = cloneDeep(
                            vertexTypeStore.editedSelectedVertexType
                              .append_property_indexes
                          );

                          propertyIndexEntities[index].name = e.value;

                          vertexTypeStore.mutateEditedSelectedVertexType({
                            ...vertexTypeStore.editedSelectedVertexType,
                            append_property_indexes: propertyIndexEntities
                          });
                        }}
                        originInputProps={{
                          onBlur() {
                            // check is ready to create
                            vertexTypeStore.validateEditVertexType();
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
                            vertexTypeStore.editedSelectedVertexType
                              .append_property_indexes
                          );

                          propertyIndexEntities[index].type = value;

                          vertexTypeStore.mutateEditedSelectedVertexType({
                            ...vertexTypeStore.editedSelectedVertexType,
                            append_property_indexes: propertyIndexEntities
                          });

                          vertexTypeStore.validateEditVertexType();
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
                            vertexTypeStore.editedSelectedVertexType
                              .append_property_indexes
                          );

                          if (Array.isArray(value)) {
                            propertyIndexEntities[index].fields = value;
                          } else {
                            propertyIndexEntities[index].fields = [value];
                          }

                          vertexTypeStore.mutateEditedSelectedVertexType({
                            ...vertexTypeStore.editedSelectedVertexType,
                            append_property_indexes: propertyIndexEntities
                          });

                          vertexTypeStore.validateEditVertexType();
                        }}
                      >
                        {type === 'SECONDARY' &&
                          vertexTypeStore
                            .selectedVertexType!.properties.concat(
                              vertexTypeStore.editedSelectedVertexType
                                .append_properties
                            )
                            .filter(
                              (property) =>
                                !vertexTypeStore.selectedVertexType!.primary_keys.includes(
                                  property.name
                                )
                            )
                            .map((property) => {
                              const order = vertexTypeStore.editedSelectedVertexType.append_property_indexes[
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
                          vertexTypeStore
                            .selectedVertexType!.properties.concat(
                              vertexTypeStore.editedSelectedVertexType
                                .append_properties
                            )
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
                          vertexTypeStore
                            .selectedVertexType!.properties.concat(
                              vertexTypeStore.editedSelectedVertexType
                                .append_properties
                            )
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

                      <Tooltip
                        placement="bottom-end"
                        tooltipShown={index === deleteAddedPopIndexInDrawer}
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
                                    vertexTypeStore.editedSelectedVertexType!
                                      .append_property_indexes
                                  );

                                  appendPropertyIndexes.splice(index, 1);

                                  vertexTypeStore.mutateEditedSelectedVertexType(
                                    {
                                      ...vertexTypeStore.editedSelectedVertexType,
                                      append_property_indexes: appendPropertyIndexes
                                    }
                                  );

                                  setDeleteAddedPopIndexInDrawer(null);
                                  vertexTypeStore.validateEditVertexType(true);
                                }}
                              >
                                确认
                              </div>
                              <div
                                onClick={() => {
                                  vertexTypeStore.resetEditedSelectedVertexType();
                                  setDeleteAddedPopIndexInDrawer(null);
                                }}
                              >
                                取消
                              </div>
                            </div>
                          </div>
                        }
                        childrenProps={{
                          src: CloseIcon,
                          alt: 'close',
                          style: { cursor: 'pointer' },
                          onClick() {
                            setDeleteAddedPopIndexInDrawer(index);
                          }
                        }}
                        childrenWrapperElement="img"
                      />
                    </div>
                  </div>
                );
              }
            )}
            {isEditVertex && (
              <div
                onClick={() => {
                  if (
                    vertexTypeStore.editedSelectedVertexType
                      .append_property_indexes.length === 0 ||
                    vertexTypeStore.isEditReady
                  ) {
                    vertexTypeStore.mutateEditedSelectedVertexType({
                      ...vertexTypeStore.editedSelectedVertexType,
                      append_property_indexes: [
                        ...vertexTypeStore.editedSelectedVertexType
                          .append_property_indexes,
                        {
                          name: '',
                          type: '',
                          fields: []
                        }
                      ]
                    });

                    vertexTypeStore.validateEditVertexType(true);
                  }
                }}
                style={{
                  cursor: 'pointer',
                  color: vertexTypeStore.isEditReady ? '#2b65ff' : '#999'
                }}
              >
                新增一组
              </div>
            )}
          </div>
        </div>
      </div>
    </Drawer>
  );
});

export default CheckAndEditVertex;
