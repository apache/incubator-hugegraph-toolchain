import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback
} from 'react';
import { observer } from 'mobx-react';
import classnames from 'classnames';
import { isUndefined, cloneDeep } from 'lodash-es';
import {
  Input,
  Select,
  Button,
  Switch,
  Tooltip,
  Checkbox,
  Message
} from '@baidu/one-ui';
import TooltipTrigger from 'react-popper-tooltip';
import HintIcon from '../../../../assets/imgs/ic_question_mark.svg';

import BlueArrowIcon from '../../../../assets/imgs/ic_arrow_blue.svg';
import closeIcon from '../../../../assets/imgs/ic_close_16.svg';
import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import { EdgeTypeValidatePropertyIndexes } from '../../../../stores/types/GraphManagementStore/metadataConfigsStore';

const NewVertexType: React.FC = observer(() => {
  const { metadataPropertyStore, vertexTypeStore, edgeTypeStore } = useContext(
    MetadataConfigsRootStore
  );
  const [isAddNewProperty, switchIsAddNewProperty] = useState(false);
  const [deletePopIndex, setDeletePopIndex] = useState<number | null>(null);
  const dropdownWrapperRef = useRef<HTMLDivElement>(null);
  const deleteWrapperRef = useRef<HTMLImageElement>(null);

  const handleOutSideClick = useCallback(
    (e: MouseEvent) => {
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

  useEffect(() => {
    metadataPropertyStore.fetchMetadataPropertyList({ fetchAll: true });
    vertexTypeStore.fetchVertexTypeList({ fetchAll: true });
    edgeTypeStore.validateAllNewEdgeType(true);
  }, [edgeTypeStore, metadataPropertyStore, vertexTypeStore]);

  useEffect(() => {
    document.addEventListener('click', handleOutSideClick, false);

    return () => {
      document.removeEventListener('click', handleOutSideClick, false);
    };
  }, [handleOutSideClick]);

  return (
    <div className="new-vertex-type-wrapper">
      <div className="new-vertex-type">
        <div className="metadata-title new-vertex-type-title">基础信息</div>
        <div className="new-vertex-type-options">
          <div className="new-vertex-type-options-name">
            <span className="metdata-essential-form-options">*</span>
            <span>边类型名称：</span>
          </div>
          <Input
            size="medium"
            width={420}
            maxLen={128}
            placeholder="允许出现中英文、数字、下划线"
            errorLocation="layer"
            errorMessage={edgeTypeStore.validateNewEdgeTypeErrorMessage.name}
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
          <div className="new-vertex-type-options-colors">
            <Select
              width={66}
              size="medium"
              value={edgeTypeStore.newEdgeType.style.color}
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
              {edgeTypeStore.colorSchemas.map((color: string) => (
                <Select.Option value={color} key={color}>
                  <div
                    className="new-vertex-type-options-color"
                    style={{
                      background: color
                    }}
                  ></div>
                </Select.Option>
              ))}
            </Select>
          </div>
        </div>
        <div className="new-vertex-type-options">
          <div className="new-vertex-type-options-name">
            <span className="metdata-essential-form-options">*</span>
            <span>起点类型：</span>
          </div>
          <Select
            width={420}
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
        <div className="new-vertex-type-options">
          <div className="new-vertex-type-options-name">
            <span className="metdata-essential-form-options">*</span>
            <span>终点类型：</span>
          </div>
          <Select
            width={420}
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
        <div className="new-vertex-type-options">
          <div
            className="new-vertex-type-options-name"
            style={{
              display: 'flex',
              alignItems: 'center',
              width: 118,
              marginRight: 48.9
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
          className="new-vertex-type-options"
          style={{
            marginBottom: isAddNewProperty ? 7 : 32,
            alignItems: 'start'
          }}
        >
          <div
            className="new-vertex-type-options-name"
            style={{ lineHeight: 'initial' }}
          >
            {edgeTypeStore.newEdgeType.link_multi_times && (
              <span className="metdata-essential-form-options">*</span>
            )}
            <span>关联属性：</span>
          </div>
          <div
            className="new-vertex-type-options-expands"
            style={{ flexDirection: 'column' }}
          >
            {edgeTypeStore.newEdgeType.properties.length !== 0 && (
              <div style={{ width: 382 }}>
                <div
                  style={{ display: 'flex', justifyContent: 'space-between' }}
                >
                  <div>属性</div>
                  <div>允许为空</div>
                </div>
                {edgeTypeStore.newEdgeType.properties.map((property, index) => {
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
                            ].nullable = !currentProperties[index].nullable;

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
                })}
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
          <div className="new-vertex-type-options">
            <div className="new-vertex-type-options-name"></div>
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
                          ...edgeTypeStore.addedPropertiesInSelectedEdgeType
                        ].findIndex(
                          propertyIndex => propertyIndex === property.name
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
          <div className="new-vertex-type-options">
            <div className="new-vertex-type-options-name">
              <span className="metdata-essential-form-options">*</span>
              <span>区分键：</span>
            </div>
            <Select
              width={420}
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
                .map(item => {
                  const order = edgeTypeStore.newEdgeType.sort_keys.findIndex(
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
          className="metadata-title new-vertex-type-title"
          style={{
            marginTop: 46,
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center'
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
        <div className="new-vertex-type-options">
          <div className="new-vertex-type-options-name">
            <span className="metdata-essential-form-options">*</span>
            <span>类型索引：</span>
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
          className="new-vertex-type-options"
          style={{ marginBottom: 12, alignItems: 'start' }}
        >
          <div className="new-vertex-type-options-name">
            <span>属性索引：</span>
          </div>
          <div className="new-vertex-type-options-expands">
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
                          edgeTypeStore.validateNewEdgeType('propertyIndexes');
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
                        edgeTypeStore.validateNewEdgeType('propertyIndexes');
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
                        edgeTypeStore.validateNewEdgeType('propertyIndexes');
                      }}
                    >
                      {type === 'SECONDARY' &&
                        edgeTypeStore.newEdgeType.properties.map(property => {
                          const order = edgeTypeStore.newEdgeType.property_indexes[
                            index
                          ].fields.findIndex(name => name === property.name);

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
                        edgeTypeStore.newEdgeType.properties
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
                        edgeTypeStore.newEdgeType.properties
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
                  edgeTypeStore.validateNewEdgeType('propertyIndexes', true);
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

        <div className="new-vertex-type-manipulations">
          <div className="new-vertex-type-options-name"></div>
          <Button
            type="primary"
            size="medium"
            style={{ width: 78, marginRight: 12 }}
            disabled={!edgeTypeStore.isCreatedReady}
            onClick={async () => {
              edgeTypeStore.validateAllNewEdgeType();

              if (!edgeTypeStore.isCreatedReady) {
                return;
              }

              await edgeTypeStore.addEdgeType();

              if (edgeTypeStore.requestStatus.addEdgeType === 'success') {
                edgeTypeStore.fetchEdgeTypeList();
                edgeTypeStore.resetNewEdgeType();
                edgeTypeStore.resetAddedPropertiesInSelectedEdgeType();
                edgeTypeStore.changeCurrentTabStatus('list');

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
          </Button>
          <Button
            size="medium"
            style={{ width: 78 }}
            onClick={() => {
              edgeTypeStore.edgeTypes.length === 0
                ? edgeTypeStore.changeCurrentTabStatus('empty')
                : edgeTypeStore.changeCurrentTabStatus('list');
              edgeTypeStore.resetNewEdgeType();
              edgeTypeStore.resetAddedPropertiesInSelectedEdgeType();
            }}
          >
            取消
          </Button>
        </div>
      </div>
    </div>
  );
});

export default NewVertexType;
