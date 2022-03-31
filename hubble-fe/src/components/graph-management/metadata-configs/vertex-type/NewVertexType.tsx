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
  Input,
  Radio,
  Select,
  Button,
  Switch,
  Tooltip,
  Checkbox,
  Message
} from 'hubble-ui';
import { useTranslation } from 'react-i18next';

import { Tooltip as CustomTooltip } from '../../../common/';
import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';
import { formatVertexIdText } from '../../../../stores/utils';

import type { VertexTypeValidatePropertyIndexes } from '../../../../stores/types/GraphManagementStore/metadataConfigsStore';

import HintIcon from '../../../../assets/imgs/ic_question_mark.svg';
import BlueArrowIcon from '../../../../assets/imgs/ic_arrow_blue.svg';
import closeIcon from '../../../../assets/imgs/ic_close_16.svg';

import './NewVertexType.less';

const NewVertexType: React.FC = observer(() => {
  const dataAnalyzeStore = useContext(DataAnalyzeStore);
  const { metadataPropertyStore, vertexTypeStore } = useContext(
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

  useEffect(() => {
    metadataPropertyStore.fetchMetadataPropertyList({ fetchAll: true });
    vertexTypeStore.validateAllNewVertexType(true);
  }, [metadataPropertyStore, vertexTypeStore]);

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
            <span>{t('addition.vertex.vertex-type-name')}：</span>
          </div>
          <Input
            size="medium"
            width={420}
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

        <div className="new-vertex-type-options">
          <div className="new-vertex-type-options-name">
            <span className="metdata-essential-form-options">*</span>
            <span>{t('addition.vertex.vertex-style')}：</span>
          </div>
          <div className="new-vertex-type-options-colors">
            <Select
              width={66}
              size="medium"
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
          <div className="new-vertex-type-options-sizes">
            <Select
              width={67}
              size="medium"
              value={vertexTypeStore.newVertexType.style.size}
              style={{ paddingLeft: 7 }}
              getPopupContainer={(e: any) => e.parentNode}
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
            <span>{t('addition.common.id-strategy')}：</span>
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
            {vertexTypeStore.newVertexType.id_strategy === 'PRIMARY_KEY' && (
              <span className="metdata-essential-form-options">*</span>
            )}
            <span>{t('addition.common.association-property')}：</span>
          </div>
          <div
            className="new-vertex-type-options-expands"
            style={{ flexDirection: 'column' }}
          >
            {vertexTypeStore.newVertexType.properties.length !== 0 && (
              <div style={{ width: 382 }}>
                <div
                  style={{ display: 'flex', justifyContent: 'space-between' }}
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
                            onChange={(checked: boolean) => {
                              currentProperties[index].nullable = checked;

                              vertexTypeStore.mutateNewProperty({
                                ...vertexTypeStore.newVertexType,
                                properties: currentProperties
                              });

                              // remove primary keys since it could be empty value
                              if (checked) {
                                vertexTypeStore.mutateNewProperty({
                                  ...vertexTypeStore.newVertexType,
                                  primary_keys:
                                    vertexTypeStore.newVertexType.primary_keys.filter(
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
                  }
                )}
              </div>
            )}
            <div
              style={{ display: 'flex', color: '#2b65ff', cursor: 'pointer' }}
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
                          ...vertexTypeStore.addedPropertiesInSelectedVertextType
                        ].findIndex(
                          (propertyIndex) => propertyIndex === property.name
                        ) !== -1
                      }
                      onChange={() => {
                        const addedPropertiesInSelectedVertextType =
                          vertexTypeStore.addedPropertiesInSelectedVertextType;

                        addedPropertiesInSelectedVertextType.has(property.name)
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
                            const currentProperty =
                              vertexTypeStore.newVertexType.properties.find(
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
          <div className="new-vertex-type-options">
            <div className="new-vertex-type-options-name">
              <span className="metdata-essential-form-options">*</span>
              <span>{t('addition.common.primary-key-property')}：</span>
            </div>
            <Select
              width={420}
              mode="multiple"
              placeholder={t(
                'addition.common.select-primary-key-property-placeholder'
              )}
              selectorName={t(
                'addition.message.select-association-key-property-placeholder'
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
              {vertexTypeStore.newVertexType.properties.map((item) => {
                const order =
                  vertexTypeStore.newVertexType.primary_keys.findIndex(
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
              {/* {vertexTypeStore.newVertexType.properties
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
                })} */}
            </Select>
          </div>
        )}

        <div className="new-vertex-type-options">
          <div className="new-vertex-type-options-name">
            <span className="metdata-essential-form-options">*</span>
            <span>{t('addition.vertex.vertex-display-content')}：</span>
          </div>
          <Select
            width={420}
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
                const order =
                  vertexTypeStore.newVertexType.style.display_fields.findIndex(
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
                          backgroundColor:
                            vertexTypeStore.newVertexType.style.display_fields.includes(
                              item.name
                            )
                              ? '#2b65ff'
                              : '#fff',
                          borderColor:
                            vertexTypeStore.newVertexType.style.display_fields.includes(
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
            placement="bottom"
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
            checked={vertexTypeStore.newVertexType.open_label_index}
            onChange={() => {
              vertexTypeStore.mutateNewProperty({
                ...vertexTypeStore.newVertexType,
                open_label_index:
                  !vertexTypeStore.newVertexType.open_label_index
              });
            }}
            size="large"
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
            {vertexTypeStore.newVertexType.property_indexes.length !== 0 && (
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
                          ? (
                              vertexTypeStore.validateNewVertexTypeErrorMessage
                                .propertyIndexes[
                                index
                              ] as VertexTypeValidatePropertyIndexes
                            ).name
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
                      placeholder={t('addition.edge.index-type-select-desc')}
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
                            const order =
                              vertexTypeStore.newVertexType.property_indexes[
                                index
                              ].fields.findIndex(
                                (name) => name === property.name
                              );

                            const multiSelectOptionClassName = classnames({
                              'metadata-configs-sorted-multiSelect-option':
                                true,
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
                        vertexTypeStore.newVertexType.properties
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
                                vertexTypeStore.newVertexType.property_indexes
                              );

                              propertyIndexEntities.splice(index, 1);

                              vertexTypeStore.mutateNewProperty({
                                ...vertexTypeStore.newVertexType,
                                property_indexes: propertyIndexEntities
                              });

                              vertexTypeStore.validateAllNewVertexType(true);
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
                  vertexTypeStore.newVertexType.property_indexes.length === 0 ||
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

        <div className="new-vertex-type-manipulations">
          <div className="new-vertex-type-options-name"></div>
          <Button
            type="primary"
            size="medium"
            style={{ width: 78, marginRight: 12 }}
            disabled={!vertexTypeStore.isCreatedReady}
            onClick={async () => {
              vertexTypeStore.validateAllNewVertexType();

              if (!vertexTypeStore.isCreatedReady) {
                return;
              }
              const id = vertexTypeStore.newVertexType.name;
              if (vertexTypeStore.newVertexType.style.color !== null) {
                dataAnalyzeStore.colorMappings[id] =
                  vertexTypeStore.newVertexType.style.color!;
              }

              if (vertexTypeStore.newVertexType.style.size !== null) {
                dataAnalyzeStore.vertexSizeMappings[id] =
                  vertexTypeStore.newVertexType.style.size;
              }
              await vertexTypeStore.addVertexType();

              if (vertexTypeStore.requestStatus.addVertexType === 'success') {
                vertexTypeStore.fetchVertexTypeList();
                vertexTypeStore.resetNewVertextType();
                vertexTypeStore.resetAddedPropertiesInSelectedVertextType();
                vertexTypeStore.changeCurrentTabStatus('list');

                Message.success({
                  content: t('addition.newGraphConfig.create-scuccess'),
                  size: 'medium',
                  showCloseIcon: false
                });
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
          </Button>
          <Button
            size="medium"
            style={{ width: 78 }}
            onClick={() => {
              vertexTypeStore.vertexTypes.length === 0
                ? vertexTypeStore.changeCurrentTabStatus('empty')
                : vertexTypeStore.changeCurrentTabStatus('list');
              vertexTypeStore.resetNewVertextType();
              vertexTypeStore.resetAddedPropertiesInSelectedVertextType();
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
