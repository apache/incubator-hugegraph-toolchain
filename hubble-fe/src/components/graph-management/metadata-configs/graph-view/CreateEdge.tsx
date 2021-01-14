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
  Switch,
  Checkbox,
  Tooltip,
  Message
} from '@baidu/one-ui';

import { Tooltip as CustomTooltip } from '../../../common';
import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import { EdgeTypeValidatePropertyIndexes } from '../../../../stores/types/GraphManagementStore/metadataConfigsStore';
import {
  mapMetadataProperties,
  generateGraphModeId,
  edgeWidthMapping,
  formatVertexIdText
} from '../../../../stores/utils';

import BlueArrowIcon from '../../../../assets/imgs/ic_arrow_blue.svg';
import HintIcon from '../../../../assets/imgs/ic_question_mark.svg';
import closeIcon from '../../../../assets/imgs/ic_close_16.svg';
import SelectedSoilidArrowIcon from '../../../../assets/imgs/ic_arrow_selected.svg';
import NoSelectedSoilidArrowIcon from '../../../../assets/imgs/ic_arrow.svg';
import SelectedSoilidStraightIcon from '../../../../assets/imgs/ic_straight_selected.svg';
import NoSelectedSoilidStraightIcon from '../../../../assets/imgs/ic_straight.svg';

const CreateEdge: React.FC = observer(() => {
  const {
    metadataPropertyStore,
    vertexTypeStore,
    edgeTypeStore,
    graphViewStore
  } = useContext(MetadataConfigsRootStore);
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
    edgeTypeStore.resetNewEdgeType();
    edgeTypeStore.resetAddedPropertiesInSelectedEdgeType();
  };

  useEffect(() => {
    document.addEventListener('click', handleOutSideClick, false);

    return () => {
      document.removeEventListener('click', handleOutSideClick, false);
    };
  }, [handleOutSideClick]);

  return (
    <Drawer
      title="创建边类型"
      width={687}
      destroyOnClose
      visible={graphViewStore.currentDrawer === 'create-edge'}
      maskClosable={false}
      onClose={handleCloseDrawer}
      footer={[
        <Button
          type="primary"
          size="medium"
          style={{ width: 60 }}
          disabled={!edgeTypeStore.isCreatedReady}
          onClick={async () => {
            edgeTypeStore.validateAllNewEdgeType();

            if (!edgeTypeStore.isCreatedReady) {
              return;
            }

            const {
              name,
              source_label,
              target_label,
              properties,
              sort_keys,
              style,
              ...rest
            } = edgeTypeStore.newEdgeType;

            const mappedProperties = mapMetadataProperties(
              properties,
              metadataPropertyStore.metadataProperties
            );

            graphViewStore.visDataSet!.edges.add({
              ...rest,
              id: generateGraphModeId(name, source_label, target_label),
              label: name.length <= 15 ? name : name.slice(0, 15) + '...',
              from: source_label,
              to: target_label,
              value: edgeWidthMapping[style.thickness],
              font: { size: 16, strokeWidth: 0, color: '#666' },
              arrows: style.with_arrow === true ? 'to' : '',
              title: `
                <div class="metadata-graph-view-tooltip-fields">
                  <div>边类型：</div>
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

                    const sortKeyIndex = sort_keys.findIndex(
                      (sortKey) => sortKey === key
                    );

                    return `<div class="metadata-graph-view-tooltip-fields">
                        <div>${key}: </div>
                        <div>${convertedValue}</div>
                        <div>${
                          sortKeyIndex === -1
                            ? ''
                            : `(区分键${sortKeyIndex + 1})`
                        }</div>
                      </div>`;
                  })
                  .join('')}
              `,
              color: {
                color: style.color,
                highlight: style.color,
                hover: style.color
              }
            });

            await edgeTypeStore.addEdgeType();

            if (edgeTypeStore.requestStatus.addEdgeType === 'success') {
              edgeTypeStore.fetchEdgeTypeList();
              edgeTypeStore.resetNewEdgeType();
              edgeTypeStore.resetAddedPropertiesInSelectedEdgeType();
              graphViewStore.setCurrentDrawer('');

              Message.success({
                content: '创建成功',
                size: 'medium',
                showCloseIcon: false
              });
            }

            if (edgeTypeStore.requestStatus.addEdgeType === 'failed') {
              Message.error({
                content: edgeTypeStore.errorMessage,
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
              style={{ width: 112 }}
            >
              基础信息
            </div>
            <div className="metadata-graph-drawer-options">
              <div
                className="metadata-graph-drawer-options-name"
                style={{ width: 118, marginRight: 10 }}
              >
                <span className="metdata-essential-form-options">*</span>
                <span>边类型名称:</span>
              </div>
              <Input
                size="medium"
                width={430}
                maxLen={128}
                placeholder="允许出现中英文、数字、下划线"
                errorLocation="layer"
                errorMessage={
                  edgeTypeStore.validateNewEdgeTypeErrorMessage.name
                }
                value={edgeTypeStore.newEdgeType.name}
                onChange={(e: any) => {
                  edgeTypeStore.mutateNewEdgeType({
                    ...edgeTypeStore.newEdgeType,
                    name: e.value
                  });
                }}
                originInputProps={{
                  onBlur() {
                    edgeTypeStore.validateAllNewEdgeType(true);
                    edgeTypeStore.validateNewEdgeType('name');
                  }
                }}
              />
            </div>
            <div className="metadata-graph-drawer-options">
              <div
                className="metadata-graph-drawer-options-name"
                style={{ width: 118, marginRight: 0 }}
              >
                <span className="metdata-essential-form-options">*</span>
                <span>边样式:</span>
              </div>
              <div className="metadata-graph-drawer-options-colors">
                <Select
                  width={66}
                  size="medium"
                  style={{ marginRight: 12 }}
                  value={
                    <div
                      className="new-vertex-type-select"
                      style={{
                        background: edgeTypeStore.newEdgeType.style.color!.toLowerCase(),
                        marginTop: 5
                      }}
                    ></div>
                  }
                  prefixCls="new-fc-one-select-another"
                  dropdownMatchSelectWidth={false}
                  onChange={(value: string) => {
                    edgeTypeStore.mutateNewEdgeType({
                      ...edgeTypeStore.newEdgeType,
                      style: {
                        ...edgeTypeStore.newEdgeType.style,
                        color: value
                      }
                    });
                  }}
                >
                  {edgeTypeStore.colorSchemas.map(
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
                            edgeTypeStore.newEdgeType.style.color === color
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
                  width={66}
                  size="medium"
                  value={
                    edgeTypeStore.newEdgeType.style.with_arrow ? (
                      <div>
                        <img src={NoSelectedSoilidArrowIcon} />
                      </div>
                    ) : (
                      <div>
                        <img src={NoSelectedSoilidStraightIcon} />
                      </div>
                    )
                  }
                  onChange={(e: any) => {
                    edgeTypeStore.mutateNewEdgeType({
                      ...edgeTypeStore.newEdgeType,
                      style: {
                        ...edgeTypeStore.newEdgeType.style,
                        with_arrow: e[0] && e[1] === 'solid'
                      }
                    });
                  }}
                >
                  {edgeTypeStore.edgeShapeSchemas.map((item, index) => (
                    <Select.Option
                      value={[item.flag, item.shape]}
                      key={item.flag}
                      style={{ width: 66 }}
                    >
                      <div
                        className="new-vertex-type-options-color"
                        style={{
                          marginTop: 6,
                          marginLeft: 5
                        }}
                      >
                        <img
                          src={
                            edgeTypeStore.newEdgeType.style.with_arrow ===
                            item.flag
                              ? item.blueicon
                              : item.blackicon
                          }
                          alt="toogleEdgeArrow"
                        />
                      </div>
                    </Select.Option>
                  ))}
                </Select>
              </div>

              <div className="new-vertex-type-options-colors">
                <Select
                  width={66}
                  size="medium"
                  value={edgeTypeStore.newEdgeType.style.thickness}
                  style={{ paddingLeft: 7 }}
                  onChange={(value: string) => {
                    edgeTypeStore.mutateNewEdgeType({
                      ...edgeTypeStore.newEdgeType,
                      style: {
                        ...edgeTypeStore.newEdgeType.style,
                        thickness: value
                      }
                    });
                  }}
                >
                  {edgeTypeStore.thicknessSchemas.map((value, index) => (
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
                style={{ width: 118, marginRight: 10 }}
              >
                <span className="metdata-essential-form-options">*</span>
                <span>起点类型:</span>
              </div>
              <Select
                width={430}
                placeholder="请选择起点类型"
                size="medium"
                onChange={(value: string) => {
                  edgeTypeStore.mutateNewEdgeType({
                    ...edgeTypeStore.newEdgeType,
                    source_label: value
                  });

                  edgeTypeStore.validateAllNewEdgeType(true);
                  edgeTypeStore.validateNewEdgeType('sourceLabel');
                }}
              >
                {vertexTypeStore.vertexTypes.map(({ name }) => (
                  <Select.Option value={name} key={name}>
                    {name}
                  </Select.Option>
                ))}
              </Select>
            </div>

            <div className="metadata-graph-drawer-options">
              <div
                className="metadata-graph-drawer-options-name"
                style={{ width: 118, marginRight: 10 }}
              >
                <span className="metdata-essential-form-options">*</span>
                <span>终点类型:</span>
              </div>
              <Select
                width={430}
                placeholder="请选择终点类型"
                size="medium"
                onChange={(value: string) => {
                  edgeTypeStore.mutateNewEdgeType({
                    ...edgeTypeStore.newEdgeType,
                    target_label: value
                  });

                  edgeTypeStore.validateAllNewEdgeType(true);
                  edgeTypeStore.validateNewEdgeType('targetLabel');
                }}
              >
                {vertexTypeStore.vertexTypes.map(({ name }) => (
                  <Select.Option value={name} key={name}>
                    {name}
                  </Select.Option>
                ))}
              </Select>
            </div>

            <div className="metadata-graph-drawer-options">
              <div
                className="metadata-graph-drawer-options-name"
                style={{
                  width: 118,
                  marginRight: 10,
                  display: 'flex',
                  alignItems: 'center'
                }}
              >
                <span className="metdata-essential-form-options">*</span>
                <span>允许多次连接：</span>
                <Tooltip
                  placement="right"
                  title="开启后两顶点间允许存在多条该类型的边"
                  type="dark"
                >
                  <img src={HintIcon} alt="hint" />
                </Tooltip>
              </div>
              <Switch
                checked={edgeTypeStore.newEdgeType.link_multi_times}
                size="large"
                onChange={(checked: boolean) => {
                  edgeTypeStore.mutateNewEdgeType({
                    ...edgeTypeStore.newEdgeType,
                    link_multi_times: checked
                  });
                }}
              />
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
                style={{ lineHeight: 'inherit', width: 118, marginRight: 10 }}
              >
                {edgeTypeStore.newEdgeType.link_multi_times && (
                  <span className="metdata-essential-form-options">*</span>
                )}
                <span>关联属性:</span>
              </div>
              <div
                className="metadata-graph-drawer-options-expands"
                style={{ flexDirection: 'column' }}
              >
                {edgeTypeStore.newEdgeType.properties.length !== 0 && (
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
                    {edgeTypeStore.newEdgeType.properties.map(
                      (property, index) => {
                        const currentProperties = cloneDeep(
                          edgeTypeStore.newEdgeType.properties
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

                                  edgeTypeStore.mutateNewEdgeType({
                                    ...edgeTypeStore.newEdgeType,
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
                    fontSize: 14,
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
                  style={{ width: 118, marginRight: 10 }}
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
                              ...edgeTypeStore.addedPropertiesInSelectedEdgeType
                            ].findIndex(
                              (propertyIndex) => propertyIndex === property.name
                            ) !== -1
                          }
                          onChange={() => {
                            const addedPropertiesIndexInSelectedEdgeType =
                              edgeTypeStore.addedPropertiesInSelectedEdgeType;

                            addedPropertiesIndexInSelectedEdgeType.has(
                              property.name
                            )
                              ? addedPropertiesIndexInSelectedEdgeType.delete(
                                  property.name
                                )
                              : addedPropertiesIndexInSelectedEdgeType.add(
                                  property.name
                                );

                            edgeTypeStore.mutateNewEdgeType({
                              ...edgeTypeStore.newEdgeType,
                              properties: [
                                ...addedPropertiesIndexInSelectedEdgeType
                              ].map((propertyName) => {
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

                            edgeTypeStore.validateAllNewEdgeType(true);
                            edgeTypeStore.validateNewEdgeType('properties');
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

            {edgeTypeStore.newEdgeType.link_multi_times && (
              <div className="metadata-graph-drawer-options">
                <div
                  className="metadata-graph-drawer-options-name"
                  style={{ width: 118, marginRight: 10 }}
                >
                  <span className="metdata-essential-form-options">*</span>
                  <span>区分键:</span>
                </div>
                <Select
                  width={356}
                  mode="multiple"
                  placeholder="请选择区分键属性"
                  size="medium"
                  showSearch={false}
                  onChange={(e: string[]) => {
                    edgeTypeStore.mutateNewEdgeType({
                      ...edgeTypeStore.newEdgeType,
                      sort_keys: e
                    });

                    edgeTypeStore.validateAllNewEdgeType(true);
                    edgeTypeStore.validateNewEdgeType('sortKeys');
                  }}
                  value={edgeTypeStore.newEdgeType.sort_keys}
                >
                  {edgeTypeStore.newEdgeType.properties
                    .filter(({ nullable }) => !nullable)
                    .map((item) => {
                      const order = edgeTypeStore.newEdgeType.sort_keys.findIndex(
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
                style={{ width: 118, marginRight: 10 }}
              >
                <span className="metdata-essential-form-options">*</span>
                <span>边展示内容:</span>
              </div>
              <Select
                width={430}
                mode="multiple"
                size="medium"
                placeholder="请选择边展示内容"
                showSearch={false}
                onChange={(value: string[]) => {
                  edgeTypeStore.mutateNewEdgeType({
                    ...edgeTypeStore.newEdgeType,
                    style: {
                      ...edgeTypeStore.newEdgeType.style,
                      display_fields: value
                    }
                  });

                  edgeTypeStore.validateAllNewEdgeType(true);
                  edgeTypeStore.validateNewEdgeType('displayFeilds');
                }}
                value={edgeTypeStore.newEdgeType.style.display_fields.map(
                  (field) => formatVertexIdText(field, '边类型')
                )}
              >
                {edgeTypeStore.newEdgeType.properties
                  .concat({ name: '~id', nullable: false })
                  .filter(({ nullable }) => !nullable)
                  .map((item) => {
                    const order = edgeTypeStore.newEdgeType.style.display_fields.findIndex(
                      (name) => name === item.name
                    );

                    const multiSelectOptionClassName = classnames({
                      'metadata-configs-sorted-multiSelect-option': true,
                      'metadata-configs-sorted-multiSelect-option-selected':
                        order !== -1
                    });

                    return (
                      <Select.Option
                        value={formatVertexIdText(item.name, '边类型')}
                        key={item.name}
                      >
                        <div className={multiSelectOptionClassName}>
                          <div
                            style={{
                              backgroundColor: edgeTypeStore.newEdgeType.style.display_fields.includes(
                                item.name
                              )
                                ? '#2b65ff'
                                : '#fff',
                              borderColor: edgeTypeStore.newEdgeType.style.display_fields.includes(
                                item.name
                              )
                                ? '#fff'
                                : '#e0e0e0'
                            }}
                          >
                            {order !== -1 ? order + 1 : ''}
                          </div>
                          <div style={{ color: '#333' }}>
                            {formatVertexIdText(item.name, '边类型')}
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
                style={{ width: 118, marginRight: 10 }}
              >
                <span className="metdata-essential-form-options">*</span>
                <span>类型索引:</span>
              </div>
              <Switch
                checked={edgeTypeStore.newEdgeType.open_label_index}
                size="large"
                onChange={(checked: boolean) => {
                  edgeTypeStore.mutateNewEdgeType({
                    ...edgeTypeStore.newEdgeType,
                    open_label_index: checked
                  });
                }}
              />
            </div>

            <div
              className="metadata-graph-drawer-options"
              style={{ marginBottom: 12, alignItems: 'start' }}
            >
              <div
                className="metadata-graph-drawer-options-name"
                style={{ width: 118, marginRight: 10 }}
              >
                <span>属性索引:</span>
              </div>
              <div className="metadata-graph-drawer-options-expands">
                {edgeTypeStore.newEdgeType.property_indexes.length !== 0 && (
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
                {edgeTypeStore.newEdgeType.property_indexes.map(
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
                            edgeTypeStore.validateNewEdgeTypeErrorMessage
                              .propertyIndexes.length !== 0
                              ? (edgeTypeStore.validateNewEdgeTypeErrorMessage
                                  .propertyIndexes[
                                  index
                                ] as EdgeTypeValidatePropertyIndexes).name
                              : ''
                          }
                          value={name}
                          onChange={(e: any) => {
                            const propertyIndexEntities = cloneDeep(
                              edgeTypeStore.newEdgeType.property_indexes
                            );

                            propertyIndexEntities[index].name = e.value;

                            edgeTypeStore.mutateNewEdgeType({
                              ...edgeTypeStore.newEdgeType,
                              property_indexes: propertyIndexEntities
                            });
                          }}
                          originInputProps={{
                            onBlur() {
                              // check is ready to create
                              edgeTypeStore.validateAllNewEdgeType(true);
                              edgeTypeStore.validateNewEdgeType(
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
                              edgeTypeStore.newEdgeType.property_indexes
                            );

                            propertyIndexEntities[index].type = value;

                            edgeTypeStore.mutateNewEdgeType({
                              ...edgeTypeStore.newEdgeType,
                              property_indexes: propertyIndexEntities
                            });

                            edgeTypeStore.validateAllNewEdgeType(true);
                            edgeTypeStore.validateNewEdgeType(
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
                          mode="multiple"
                          placeholder="请选择属性"
                          size="medium"
                          showSearch={false}
                          value={fields}
                          onChange={(value: string[]) => {
                            const propertyIndexEntities = cloneDeep(
                              edgeTypeStore.newEdgeType.property_indexes
                            );

                            if (Array.isArray(value)) {
                              propertyIndexEntities[index].fields = value;
                            } else {
                              propertyIndexEntities[index].fields = [value];
                            }

                            edgeTypeStore.mutateNewEdgeType({
                              ...edgeTypeStore.newEdgeType,
                              property_indexes: propertyIndexEntities
                            });

                            edgeTypeStore.validateAllNewEdgeType(true);
                            edgeTypeStore.validateNewEdgeType(
                              'propertyIndexes'
                            );
                          }}
                        >
                          {type === 'SECONDARY' &&
                            edgeTypeStore.newEdgeType.properties.map(
                              (property) => {
                                const order = edgeTypeStore.newEdgeType.property_indexes[
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
                              }
                            )}

                          {type === 'RANGE' &&
                            edgeTypeStore.newEdgeType.properties
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
                            edgeTypeStore.newEdgeType.properties
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
                        tooltipWrapperProps={{
                          className: 'metadata-properties-tooltips',
                          style: { zIndex: 1041 }
                        }}
                        tooltipWrapper={
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
                                    edgeTypeStore.newEdgeType.property_indexes
                                  );

                                  propertyIndexEntities.splice(index, 1);

                                  edgeTypeStore.mutateNewEdgeType({
                                    ...edgeTypeStore.newEdgeType,
                                    property_indexes: propertyIndexEntities
                                  });

                                  edgeTypeStore.validateAllNewEdgeType(true);
                                  edgeTypeStore.validateNewEdgeType(
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
                      edgeTypeStore.newEdgeType.property_indexes.length === 0 ||
                      edgeTypeStore.isAddNewPropertyIndexReady
                    ) {
                      edgeTypeStore.mutateNewEdgeType({
                        ...edgeTypeStore.newEdgeType,
                        property_indexes: [
                          ...edgeTypeStore.newEdgeType.property_indexes,
                          {
                            name: '',
                            type: '',
                            fields: []
                          }
                        ]
                      });

                      edgeTypeStore.validateAllNewEdgeType(true);
                      // set isAddNewPropertyIndexReady to false
                      edgeTypeStore.validateNewEdgeType(
                        'propertyIndexes',
                        true
                      );
                    }
                  }}
                  style={{
                    cursor: 'pointer',
                    color: edgeTypeStore.isAddNewPropertyIndexReady
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

export default CreateEdge;
