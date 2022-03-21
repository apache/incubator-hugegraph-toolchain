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
} from 'hubble-ui';

import { Tooltip as CustomTooltip } from '../../../common';
import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';
import { formatVertexIdText } from '../../../../stores/utils';

import type { EdgeTypeValidatePropertyIndexes } from '../../../../stores/types/GraphManagementStore/metadataConfigsStore';

import HintIcon from '../../../../assets/imgs/ic_question_mark.svg';
import BlueArrowIcon from '../../../../assets/imgs/ic_arrow_blue.svg';
import SelectedSoilidArrowIcon from '../../../../assets/imgs/ic_arrow_selected.svg';
import NoSelectedSoilidArrowIcon from '../../../../assets/imgs/ic_arrow.svg';
import SelectedSoilidStraightIcon from '../../../../assets/imgs/ic_straight_selected.svg';
import NoSelectedSoilidStraightIcon from '../../../../assets/imgs/ic_straight.svg';
import closeIcon from '../../../../assets/imgs/ic_close_16.svg';
import { useTranslation } from 'react-i18next';

const NewVertexType: React.FC = observer(() => {
  const dataAnalyzeStore = useContext(DataAnalyzeStore);
  const { t } = useTranslation();
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
        <div className="metadata-title new-vertex-type-title">
          {t('addition.menu.base-info')}
        </div>
        <div className="new-vertex-type-options">
          <div className="new-vertex-type-options-name">
            <span className="metdata-essential-form-options">*</span>
            <span>{t('addition.common.edge-type-name')}：</span>
          </div>
          <Input
            size="medium"
            width={420}
            maxLen={128}
            placeholder={t('addition.message.edge-name-rule')}
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
        </div>
        <div className="new-vertex-type-options">
          <div className="new-vertex-type-options-name">
            <span className="metdata-essential-form-options">*</span>
            <span>{t('addition.common.edge-style')}：</span>
          </div>
          <div className="new-vertex-type-options-colors">
            <Select
              width={66}
              size="medium"
              value={edgeTypeStore.newEdgeType.style.color}
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
              value={edgeTypeStore.newEdgeType.style.with_arrow}
              onChange={(e: any) => {
                edgeTypeStore.mutateNewEdgeType({
                  ...edgeTypeStore.newEdgeType,
                  style: {
                    ...edgeTypeStore.newEdgeType.style,
                    with_arrow: e
                  }
                });
              }}
            >
              {edgeTypeStore.edgeShapeSchemas.map((item, index) => (
                <Select.Option
                  value={item.flag}
                  key={item.flag}
                  style={{ width: 66 }}
                >
                  <div
                    className="new-vertex-type-options-color"
                    style={{
                      marginTop: index === 0 ? 4 : 1,
                      marginLeft: 5
                    }}
                  >
                    <img
                      src={
                        edgeTypeStore.newEdgeType.style.with_arrow === item.flag
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
          <div className="new-vertex-type-options-sizes">
            <Select
              width={66}
              size="medium"
              value={edgeTypeStore.newEdgeType.style.thickness}
              getPopupContainer={(e: any) => e.parentNode}
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
              {edgeTypeStore.thicknessSchemas.map((value) => (
                <Select.Option
                  value={value.en}
                  key={value.en}
                  style={{ width: 66 }}
                >
                  <div
                    className="new-vertex-type-options-color"
                    style={{
                      marginTop: 2.5,
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

        <div className="new-vertex-type-options">
          <div className="new-vertex-type-options-name">
            <span className="metdata-essential-form-options">*</span>
            <span>{t('addition.common.source-type')}：</span>
          </div>
          <Select
            width={420}
            placeholder={t('addition.message.source-type-select-placeholder')}
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
            <span>{t('addition.common.target-type')}：</span>
          </div>
          <Select
            width={420}
            placeholder={t('addition.message.target-type-select-placeholder')}
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
            <span>{t('addition.common.allow-multiple-connections')}：</span>
            <Tooltip
              placement="right"
              title={t('addition.common.multiple-connections-notice')}
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
            <span>{t('addition.common.association-property')}：</span>
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
                  <div>{t('addition.common.property')}</div>
                  <div>{t('addition.common.allow-null')}</div>
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
                          onChange={(checked: boolean) => {
                            currentProperties[index].nullable =
                              !currentProperties[index].nullable;

                            edgeTypeStore.mutateNewEdgeType({
                              ...edgeTypeStore.newEdgeType,
                              properties: currentProperties
                            });

                            // remove primary keys since it could be empty value
                            if (checked) {
                              edgeTypeStore.mutateNewEdgeType({
                                ...edgeTypeStore.newEdgeType,
                                sort_keys:
                                  edgeTypeStore.newEdgeType.sort_keys.filter(
                                    (key) => key !== property.name
                                  )
                              });
                            }
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
          <div className="new-vertex-type-options">
            <div className="new-vertex-type-options-name"></div>
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
                            const currentProperty =
                              edgeTypeStore.newEdgeType.properties.find(
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
              <span>{t('addition.common.distinguishing-key')}：</span>
            </div>
            <Select
              width={420}
              mode="multiple"
              placeholder={t(
                'addition.message.select-distinguishing-key-property-placeholder'
              )}
              selectorName={t(
                'addition.message.select-association-key-property-placeholder'
              )}
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
              {edgeTypeStore.newEdgeType.properties.map((item) => {
                const order = edgeTypeStore.newEdgeType.sort_keys.findIndex(
                  (name) => name === item.name
                );

                const multiSelectOptionClassName = classnames({
                  'metadata-configs-sorted-multiSelect-option': true,
                  'metadata-configs-sorted-multiSelect-option-selected':
                    order !== -1
                });

                return (
                  <Select.Option
                    value={item.name}
                    key={item.name}
                    disabled={item.nullable}
                  >
                    <div className={multiSelectOptionClassName}>
                      <div>{order !== -1 ? order + 1 : ''}</div>
                      <div>{item.name}</div>
                    </div>
                  </Select.Option>
                );
              })}
              {/* {edgeTypeStore.newEdgeType.properties
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
                })} */}
            </Select>
          </div>
        )}

        <div className="new-vertex-type-options">
          <div className="new-vertex-type-options-name">
            <span className="metdata-essential-form-options">*</span>
            <span>{t('addition.edge.display-content')}：</span>
          </div>
          <Select
            width={420}
            mode="multiple"
            size="medium"
            placeholder={t('addition.edge.display-content-select-desc')}
            showSearch={false}
            onChange={(value: string[]) => {
              console.log(value, 123, edgeTypeStore.newEdgeType.properties);
              edgeTypeStore.mutateNewEdgeType({
                ...edgeTypeStore.newEdgeType,
                style: {
                  ...edgeTypeStore.newEdgeType.style,
                  display_fields: value.map((field) =>
                    formatVertexIdText(
                      field,
                      t('addition.function-parameter.edge-type'),
                      true
                    )
                  )
                }
              });

              edgeTypeStore.validateAllNewEdgeType(true);
              edgeTypeStore.validateNewEdgeType('displayFeilds');
            }}
            value={edgeTypeStore.newEdgeType.style.display_fields.map((field) =>
              formatVertexIdText(
                field,
                t('addition.function-parameter.edge-type')
              )
            )}
          >
            {edgeTypeStore.newEdgeType.properties
              .concat({ name: '~id', nullable: false })
              .filter(({ nullable }) => !nullable)
              .map((item) => {
                const order =
                  edgeTypeStore.newEdgeType.style.display_fields.findIndex(
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
                      t('addition.function-parameter.edge-type')
                    )}
                    key={item.name}
                  >
                    <div className={multiSelectOptionClassName}>
                      <div
                        style={{
                          backgroundColor:
                            edgeTypeStore.newEdgeType.style.display_fields.includes(
                              item.name
                            )
                              ? '#2b65ff'
                              : '#fff',
                          borderColor:
                            edgeTypeStore.newEdgeType.style.display_fields.includes(
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
                          t('addition.function-parameter.edge-type')
                        )}
                      </div>
                    </div>
                  </Select.Option>
                );
              })}
          </Select>
        </div>

        <div
          className="metadata-title new-vertex-type-title"
          style={{
            marginTop: 46,
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center'
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
        <div className="new-vertex-type-options">
          <div className="new-vertex-type-options-name">
            <span className="metdata-essential-form-options">*</span>
            <span>{t('addition.menu.type-index')}：</span>
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
            <span>{t('addition.common.property-index')}：</span>
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
                <div style={{ width: 110, marginRight: 12 }}>
                  {t('addition.edge.index-name')}
                </div>
                <div style={{ width: 130, marginRight: 12 }}>
                  {t('addition.edge.index-type')}
                </div>
                <div>{t('addition.common.property')}</div>
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
                      placeholder={t('addition.edge.index-name')}
                      errorLocation="layer"
                      errorMessage={
                        edgeTypeStore.validateNewEdgeTypeErrorMessage
                          .propertyIndexes.length !== 0
                          ? (
                              edgeTypeStore.validateNewEdgeTypeErrorMessage
                                .propertyIndexes[
                                index
                              ] as EdgeTypeValidatePropertyIndexes
                            ).name
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
                      placeholder={t('addition.edge.index-type-select-desc')}
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
                      mode="multiple"
                      placeholder={t('addition.edge.property-select-desc')}
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
                        edgeTypeStore.newEdgeType.properties.map((property) => {
                          const order =
                            edgeTypeStore.newEdgeType.property_indexes[
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
                        edgeTypeStore.newEdgeType.properties
                          .filter((property) => {
                            const matchedProperty =
                              metadataPropertyStore.metadataProperties.find(
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
                            const matchedProperty =
                              metadataPropertyStore.metadataProperties.find(
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
                      className: 'metadata-properties-tooltips'
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
              {t('addition.edge.add-group')}
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

              const id = edgeTypeStore.newEdgeType.name;
              if (edgeTypeStore.newEdgeType.style.color !== null) {
                dataAnalyzeStore.edgeColorMappings[id] =
                  edgeTypeStore.newEdgeType.style.color!;
              }

              if (edgeTypeStore.newEdgeType.style.with_arrow !== null) {
                dataAnalyzeStore.edgeWithArrowMappings[id] =
                  edgeTypeStore.newEdgeType.style.with_arrow;
              }

              if (edgeTypeStore.newEdgeType.style.thickness !== null) {
                dataAnalyzeStore.edgeThicknessMappings[id] =
                  edgeTypeStore.newEdgeType.style.thickness;
              }

              await edgeTypeStore.addEdgeType();

              if (edgeTypeStore.requestStatus.addEdgeType === 'success') {
                edgeTypeStore.fetchEdgeTypeList();
                edgeTypeStore.resetNewEdgeType();
                edgeTypeStore.resetAddedPropertiesInSelectedEdgeType();
                edgeTypeStore.changeCurrentTabStatus('list');

                Message.success({
                  content: t('addition.newGraphConfig.create-scuccess'),
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
            {t('addition.newGraphConfig.create')}
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
            {t('addition.common.cancel')}
          </Button>
        </div>
      </div>
    </div>
  );
});

export default NewVertexType;
