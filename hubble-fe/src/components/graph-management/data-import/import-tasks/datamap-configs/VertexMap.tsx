import React, {
  useContext,
  useState,
  useCallback,
  useRef,
  useEffect
} from 'react';
import { observer } from 'mobx-react';
import { isUndefined, isEmpty, size, cloneDeep } from 'lodash-es';
import { useTranslation } from 'react-i18next';
import classnames from 'classnames';
import { Input, Select, Checkbox, Message } from '@baidu/one-ui';

import { Tooltip } from '../../../../common';
import TypeConfigManipulations from './TypeConfigManipulations';
import { DataImportRootStoreContext } from '../../../../../stores';
import { VertexType } from '../../../../../stores/types/GraphManagementStore/metadataConfigsStore';
import { getUnicodeLength } from '../../../../../utils';

import ArrowIcon from '../../../../../assets/imgs/ic_arrow_16.svg';
import BlueArrowIcon from '../../../../../assets/imgs/ic_arrow_blue.svg';
import CloseIcon from '../../../../../assets/imgs/ic_close_16.svg';
import MapIcon from '../../../../../assets/imgs/ic_yingshe_16.svg';

export interface VertexMapProps {
  checkOrEdit: 'check' | 'edit' | boolean;
  onCancelCreateVertex: () => void;
  vertexMapIndex?: number;
}

const VertexMap: React.FC<VertexMapProps> = observer(
  ({ checkOrEdit, onCancelCreateVertex, vertexMapIndex }) => {
    const dataImportRootStore = useContext(DataImportRootStoreContext);
    const { dataMapStore } = dataImportRootStore;
    const [isAddMapping, switchAddMapping] = useState(false);
    const [isExpandAdvance, switchExpandAdvance] = useState(false);
    const addMappingWrapperRef = useRef<HTMLDivElement>(null);
    const { t } = useTranslation();

    const isCheck = checkOrEdit === 'check';
    const isEdit = checkOrEdit === 'edit';
    const vertexMap =
      checkOrEdit !== false
        ? dataMapStore.editedVertexMap!
        : dataMapStore.newVertexType;
    const filteredColumnNamesInSelection =
      checkOrEdit !== false
        ? dataMapStore.filteredColumnNamesInVertexEditSelection
        : dataMapStore.filteredColumnNamesInVertexNewSelection;

    const findVertex = (collection: VertexType[], label: string) =>
      collection.find(({ name }) => name === label);

    const selectedVertex = findVertex(
      dataImportRootStore.vertexTypes,
      vertexMap.label
    );

    const isStrategyAutomatic = selectedVertex?.id_strategy === 'AUTOMATIC';

    const handleAdvanceExpand = () => {
      switchExpandAdvance(!isExpandAdvance);
    };

    // need useCallback to stop infinite callings of useEffect
    const handleOutSideClick = useCallback(
      (e: MouseEvent) => {
        // if clicked element is not on dropdown, collpase it
        if (
          isAddMapping &&
          addMappingWrapperRef.current &&
          !addMappingWrapperRef.current.contains(e.target as Element)
        ) {
          switchAddMapping(false);
        }
      },
      [isAddMapping]
    );

    const wrapperName = classnames({
      'import-tasks-data-map-config-card': !Boolean(checkOrEdit),
      'import-tasks-data-map-config-view': Boolean(checkOrEdit)
    });

    const expandAdvanceClassName = classnames({
      'import-tasks-step-content-header-expand': isExpandAdvance,
      'import-tasks-step-content-header-collpase': !isExpandAdvance
    });

    const expandAddMapClassName = classnames({
      'import-tasks-step-content-header-expand': isAddMapping,
      'import-tasks-step-content-header-collpase': !isAddMapping
    });

    const addMappingManipulationClassName = classnames({
      'import-tasks-manipulation': true,
      'import-tasks-manipulation-disabled':
        size(filteredColumnNamesInSelection) === 0 || isStrategyAutomatic
    });

    const addNullValueClassName = classnames({
      'import-tasks-manipulation': true,
      'import-tasks-manipulation-disabled': isEdit
        ? dataMapStore.editedVertexMap!.null_values.customized.includes('')
        : dataMapStore.newVertexType.null_values.customized.includes('')
    });

    const addPropertyMapClassName = classnames({
      'import-tasks-manipulation': true,
      'import-tasks-manipulation-disabled':
        !dataMapStore.allowAddPropertyMapping('vertex') || isStrategyAutomatic
    });

    useEffect(() => {
      document.addEventListener('click', handleOutSideClick, false);

      return () => {
        document.removeEventListener('click', handleOutSideClick, false);
      };
    }, [handleOutSideClick]);

    return (
      <div className={wrapperName}>
        {Boolean(checkOrEdit) ? (
          <div
            className="import-tasks-step-content-header"
            style={{
              justifyContent: 'space-between',
              fontSize: 14,
              paddingLeft: 14
            }}
          >
            <span>{t('data-configs.type.basic-settings')}</span>
          </div>
        ) : (
          <div
            className="import-tasks-step-content-header"
            style={{ justifyContent: 'space-between' }}
          >
            <span>{t('data-configs.type.vertex.title')}</span>
            <img
              src={CloseIcon}
              alt="collpaseOrExpand"
              onClick={onCancelCreateVertex}
            />
          </div>
        )}
        <div className="import-tasks-data-options">
          <span className="import-tasks-data-options-title in-card">
            {t('data-configs.type.vertex.type')}:
          </span>
          {isCheck ? (
            <span>{dataMapStore.editedVertexMap!.label}</span>
          ) : (
            <Select
              width={420}
              size="medium"
              getPopupContainer={(e: any) => e}
              selectorName={t(
                'data-configs.type.placeholder.select-vertex-type'
              )}
              value={
                isEdit
                  ? dataMapStore.editedVertexMap!.label
                  : dataMapStore.newVertexType.label
              }
              onChange={(value: string) => {
                const newSelectedVertex = findVertex(
                  dataImportRootStore.vertexTypes,
                  value
                );

                if (isEdit) {
                  // clear new vertex map data first
                  dataMapStore.resetEditMapping('vertex');
                  dataMapStore.syncEditMap('vertex', vertexMapIndex!);
                  switchAddMapping(false);

                  dataMapStore.editVertexMapConfig(
                    'label',
                    value,
                    vertexMapIndex!
                  );

                  if (newSelectedVertex?.id_strategy === 'PRIMARY_KEY') {
                    dataMapStore.editVertexMapConfig(
                      'id_fields',
                      newSelectedVertex.primary_keys.map(() => ''),
                      vertexMapIndex!
                    );
                  } else {
                    dataMapStore.editVertexMapConfig(
                      'id_fields',
                      [''],
                      vertexMapIndex!
                    );
                  }

                  // reset field mapping values since it binds with label
                  dataMapStore.editedVertexMap?.field_mapping.forEach(
                    ({ column_name }, fieldIndex) => {
                      dataMapStore.setVertexFieldMapping(
                        'edit',
                        '',
                        fieldIndex
                      );
                    }
                  );
                } else {
                  // clear new vertex map data first
                  dataMapStore.resetNewMap('vertex');
                  switchAddMapping(false);

                  dataMapStore.setNewVertexConfig('label', value);

                  if (newSelectedVertex?.id_strategy === 'PRIMARY_KEY') {
                    dataMapStore.setNewVertexConfig(
                      'id_fields',
                      newSelectedVertex.primary_keys.map(() => '')
                    );
                  } else {
                    dataMapStore.setNewVertexConfig('id_fields', ['']);
                  }

                  dataMapStore.newVertexType.field_mapping.forEach(
                    ({ column_name }, fieldIndex) => {
                      dataMapStore.setVertexFieldMapping('new', '', fieldIndex);
                    }
                  );
                }
              }}
            >
              {dataImportRootStore.vertexTypes.map(({ name }) => (
                <Select.Option value={name} key={name}>
                  {name}
                </Select.Option>
              ))}
            </Select>
          )}
        </div>
        {!isUndefined(selectedVertex) && (
          <div className="import-tasks-data-options">
            <span className="import-tasks-data-options-title in-card">
              {t('data-configs.type.vertex.ID-strategy')}:
            </span>
            <Tooltip
              placement="bottom-start"
              tooltipShown={isStrategyAutomatic}
              modifiers={{
                offset: {
                  offset: '0, 10'
                }
              }}
              tooltipWrapperProps={{ className: 'import-tasks-tooltips' }}
              tooltipWrapper={
                <span>
                  {t('data-configs.type.hint.lack-support-for-automatic')}
                </span>
              }
            >
              {t(`data-configs.type.ID-strategy.${selectedVertex.id_strategy}`)}
              {selectedVertex.id_strategy === 'PRIMARY_KEY' &&
                `-${selectedVertex.primary_keys.join('，')}`}
            </Tooltip>
          </div>
        )}
        {vertexMap.id_fields.map((idField, fieldIndex) => {
          return (
            <div
              className="import-tasks-data-options"
              key={idField + fieldIndex}
            >
              <span className="import-tasks-data-options-title in-card">
                {t('data-configs.type.vertex.ID-column') +
                  (selectedVertex?.id_strategy === 'PRIMARY_KEY'
                    ? fieldIndex + 1
                    : '')}
                :
              </span>
              {isCheck ? (
                <span>{idField}</span>
              ) : (
                <Select
                  width={420}
                  size="medium"
                  getPopupContainer={(e: any) => e}
                  selectorName={t(
                    'data-configs.type.placeholder.select-id-column'
                  )}
                  disabled={isUndefined(selectedVertex) || isStrategyAutomatic}
                  value={idField}
                  onChange={(value: string) => {
                    const clonedIdField = cloneDeep(vertexMap.id_fields);
                    clonedIdField[fieldIndex] = value;

                    if (isEdit) {
                      dataMapStore.editVertexMapConfig(
                        'id_fields',
                        clonedIdField,
                        vertexMapIndex!
                      );

                      // remove selected field mappings after reselect column name
                      if (
                        !isUndefined(
                          dataMapStore.editedVertexMap?.field_mapping.find(
                            ({ column_name }) => column_name === value
                          )
                        )
                      ) {
                        dataMapStore.removeVertexFieldMapping('edit', value);
                      }
                    } else {
                      dataMapStore.setNewVertexConfig(
                        'id_fields',
                        clonedIdField
                      );

                      if (
                        !isUndefined(
                          dataMapStore.newVertexType.field_mapping.find(
                            ({ column_name }) => column_name === value
                          )
                        )
                      ) {
                        dataMapStore.removeVertexFieldMapping('new', value);
                      }
                    }
                  }}
                >
                  {dataMapStore
                    .selectedFileInfo!.file_setting.column_names.filter(
                      (columnName) => !vertexMap.id_fields.includes(columnName)
                    )
                    .map((name, index) => (
                      <Select.Option value={name} key={name}>
                        <div className="no-line-break">
                          {name}
                          {!dataMapStore.selectedFileInfo!.file_setting
                            .has_header &&
                            `（${
                              dataMapStore.selectedFileInfo!.file_setting
                                .column_values[index]
                            }）`}
                        </div>
                      </Select.Option>
                    ))}
                </Select>
              )}
            </div>
          );
        })}

        <div
          className="import-tasks-data-options"
          style={{ alignItems: 'flex-start' }}
        >
          <span className="import-tasks-data-options-title in-card">
            {t('data-configs.type.vertex.map-settings')}:
          </span>
          <div className="import-tasks-data-options-expand-table">
            {isCheck &&
              isEmpty(dataMapStore.editedVertexMap?.field_mapping) &&
              '-'}

            {((Boolean(checkOrEdit) === false &&
              !isEmpty(dataMapStore.newVertexType.field_mapping)) ||
              (Boolean(checkOrEdit) !== false &&
                !isEmpty(dataMapStore.editedVertexMap!.field_mapping))) && (
              <div className="import-tasks-data-options-expand-table-row">
                <div className="import-tasks-data-options-expand-table-column">
                  {t('data-configs.type.vertex.add-map.name')}
                </div>
                <div className="import-tasks-data-options-expand-table-column">
                  {t('data-configs.type.vertex.add-map.sample')}
                </div>
                <div className="import-tasks-data-options-expand-table-column">
                  {t('data-configs.type.vertex.add-map.property')}
                </div>
                <div
                  className="import-tasks-data-options-expand-table-column
                import-tasks-manipulation"
                  style={{ cursor: 'default' }}
                >
                  {!isCheck && (
                    <span
                      style={{ cursor: 'pointer' }}
                      onClick={() => {
                        isEdit
                          ? dataMapStore.toggleVertexSelectAllFieldMapping(
                              'edit',
                              false,
                              selectedVertex
                            )
                          : dataMapStore.toggleVertexSelectAllFieldMapping(
                              'new',
                              false,
                              selectedVertex
                            );
                      }}
                    >
                      {t('data-configs.type.vertex.add-map.clear')}
                    </span>
                  )}
                </div>
              </div>
            )}

            {vertexMap.field_mapping.map(
              ({ column_name, mapped_name }, fieldIndex) => {
                const param = checkOrEdit === false ? 'new' : 'edit';

                return (
                  <div className="import-tasks-data-options-expand-table-row">
                    <div className="import-tasks-data-options-expand-table-column">
                      {column_name}
                    </div>
                    <div className="import-tasks-data-options-expand-table-column">
                      {
                        dataMapStore.selectedFileInfo?.file_setting
                          .column_values[
                          dataMapStore.selectedFileInfo?.file_setting.column_names.findIndex(
                            (name) => column_name === name
                          )
                        ]
                      }
                    </div>
                    <div className="import-tasks-data-options-expand-table-column">
                      {isCheck ? (
                        <span>{mapped_name}</span>
                      ) : (
                        <Select
                          width={170}
                          size="medium"
                          getPopupContainer={(e: any) => e.parentNode}
                          value={mapped_name}
                          onChange={(value: string) => {
                            dataMapStore.setVertexFieldMapping(
                              param,
                              value,
                              fieldIndex
                            );
                          }}
                        >
                          {selectedVertex?.properties.map(({ name }) => (
                            <Select.Option value={name} key={name}>
                              {name}
                            </Select.Option>
                          ))}
                        </Select>
                      )}
                    </div>
                    <div
                      className="import-tasks-data-options-expand-table-column import-tasks-manipulation"
                      style={{ cursor: 'default' }}
                    >
                      {!isCheck && (
                        <img
                          style={{ cursor: 'pointer' }}
                          src={CloseIcon}
                          alt="close"
                          onClick={() => {
                            dataMapStore.removeVertexFieldMapping(
                              param,
                              column_name
                            );
                          }}
                        />
                      )}
                    </div>
                  </div>
                );
              }
            )}

            {!isCheck && (
              <>
                <div
                  className="import-tasks-data-options-expand-value"
                  style={{ marginBottom: 8, height: 32, width: 'fit-content' }}
                  onClick={() => {
                    if (
                      size(filteredColumnNamesInSelection) === 0 ||
                      isStrategyAutomatic
                    ) {
                      switchAddMapping(false);
                      return;
                    }

                    switchAddMapping(!isAddMapping);
                  }}
                >
                  <div
                    className={addMappingManipulationClassName}
                    style={{ lineHeight: '32px' }}
                  >
                    {t('data-configs.type.vertex.add-map.title')}
                  </div>
                  <img
                    src={
                      size(filteredColumnNamesInSelection) !== 0 &&
                      !isStrategyAutomatic
                        ? BlueArrowIcon
                        : ArrowIcon
                    }
                    alt="expand"
                    className={expandAddMapClassName}
                    style={{ marginLeft: 4, cursor: 'pointer' }}
                  />
                </div>
                {isAddMapping && (
                  <div
                    className="import-tasks-data-options-expand-dropdown"
                    ref={addMappingWrapperRef}
                  >
                    <div>
                      <span>
                        <Checkbox
                          checked={
                            isEdit
                              ? !isEmpty(
                                  dataMapStore.editedVertexMap?.field_mapping
                                )
                              : !isEmpty(
                                  dataMapStore.newVertexType.field_mapping
                                )
                          }
                          indeterminate={
                            isEdit
                              ? !isEmpty(
                                  dataMapStore.editedVertexMap?.field_mapping
                                ) &&
                                size(
                                  dataMapStore.editedVertexMap?.field_mapping
                                ) !==
                                  size(
                                    dataMapStore.filteredColumnNamesInVertexEditSelection
                                  )
                              : !isEmpty(
                                  dataMapStore.newVertexType.field_mapping
                                ) &&
                                size(
                                  dataMapStore.newVertexType.field_mapping
                                ) !==
                                  size(
                                    dataMapStore.filteredColumnNamesInVertexNewSelection
                                  )
                          }
                          onChange={(e: any) => {
                            if (isEdit) {
                              const isIndeterminate =
                                !isEmpty(
                                  dataMapStore.editedVertexMap?.field_mapping
                                ) &&
                                size(
                                  dataMapStore.editedVertexMap?.field_mapping
                                ) !==
                                  size(
                                    dataMapStore.filteredColumnNamesInVertexEditSelection
                                  );

                              dataMapStore.toggleVertexSelectAllFieldMapping(
                                'edit',
                                // if isIndeterminate is true, e.target.checked is false
                                isIndeterminate || e.target.checked,
                                selectedVertex
                              );
                            } else {
                              const isIndeterminate =
                                !isEmpty(
                                  dataMapStore.newVertexType.field_mapping
                                ) &&
                                size(
                                  dataMapStore.newVertexType.field_mapping
                                ) !==
                                  size(
                                    dataMapStore.filteredColumnNamesInVertexNewSelection
                                  );

                              dataMapStore.toggleVertexSelectAllFieldMapping(
                                'new',
                                isIndeterminate || e.target.checked,
                                selectedVertex
                              );
                            }
                          }}
                        >
                          {t('data-configs.type.vertex.select-all')}
                        </Checkbox>
                      </span>
                    </div>
                    {dataMapStore.selectedFileInfo?.file_setting.column_names
                      .filter((name) => !vertexMap.id_fields.includes(name))
                      .map((name) => {
                        let combinedText = name;
                        let currentColumnValue = dataMapStore.selectedFileInfo
                          ?.file_setting.column_values[
                          dataMapStore.selectedFileInfo?.file_setting.column_names.findIndex(
                            (columnName) => name === columnName
                          )
                        ] as string;
                        combinedText += `（${currentColumnValue}）`;

                        if (getUnicodeLength(combinedText) > 35) {
                          combinedText = combinedText.slice(0, 35) + '...';
                        }

                        const mappingValue = selectedVertex?.properties.find(
                          ({ name: propertyName }) => propertyName === name
                        )?.name;

                        return (
                          <div>
                            <span>
                              <Checkbox
                                checked={
                                  !isUndefined(
                                    vertexMap.field_mapping.find(
                                      ({ column_name }) => column_name === name
                                    )
                                  )
                                }
                                onChange={(e: any) => {
                                  if (e.target.checked) {
                                    isEdit
                                      ? dataMapStore.setVertexFieldMappingKey(
                                          'edit',
                                          name,
                                          mappingValue
                                        )
                                      : dataMapStore.setVertexFieldMappingKey(
                                          'new',
                                          name,
                                          mappingValue
                                        );
                                  } else {
                                    isEdit
                                      ? dataMapStore.removeVertexFieldMapping(
                                          'edit',
                                          name
                                        )
                                      : dataMapStore.removeVertexFieldMapping(
                                          'new',
                                          name
                                        );
                                  }
                                }}
                              >
                                {combinedText}
                              </Checkbox>
                            </span>
                          </div>
                        );
                      })}
                  </div>
                )}
              </>
            )}
          </div>
        </div>

        <div
          className="import-tasks-step-content-header"
          style={{ fontSize: 14, paddingLeft: 14 }}
        >
          <span>{t('data-configs.type.vertex.advance.title')}</span>
          {!isCheck && (
            <img
              src={ArrowIcon}
              alt="collpaseOrExpand"
              className={expandAdvanceClassName}
              onClick={handleAdvanceExpand}
            />
          )}
        </div>
        {(isExpandAdvance || isCheck) && (
          <>
            <div
              className="import-tasks-data-options"
              style={{ alignItems: 'flex-start' }}
            >
              <span className="import-tasks-data-options-title in-card">
                {t('data-configs.type.vertex.advance.nullable-list.title')}:
              </span>
              {isCheck ? (
                <span style={{ lineHeight: '32px' }}>
                  {dataMapStore
                    .editedVertexMap!.null_values.checked.filter(
                      (value) => value !== 'null'
                    )
                    .map((value) =>
                      value === ''
                        ? '空值'
                        : value === 'NULL'
                        ? 'NULL/null'
                        : value
                    )
                    .concat(
                      dataMapStore.editedVertexMap!.null_values.customized
                    )
                    .join('，')}
                </span>
              ) : (
                <>
                  <div style={{ display: 'flex', alignItems: 'center' }}>
                    <Checkbox.Group
                      disabled={isStrategyAutomatic}
                      onChange={(checkedList: string[]) => {
                        isEdit
                          ? dataMapStore.editCheckedNullValues(
                              'edit',
                              'vertex',
                              checkedList
                            )
                          : dataMapStore.editCheckedNullValues(
                              'new',
                              'vertex',
                              checkedList
                            );
                      }}
                      value={
                        isEdit
                          ? dataMapStore.editedVertexMap!.null_values.checked
                          : dataMapStore.newVertexType.null_values.checked
                      }
                    >
                      <Checkbox value="NULL">NULL/null</Checkbox>
                      <Checkbox value={''}>
                        {t(
                          'data-configs.type.vertex.advance.nullable-list.empty'
                        )}
                      </Checkbox>
                    </Checkbox.Group>
                    <div style={{ marginLeft: 20 }}>
                      <Checkbox
                        disabled={isStrategyAutomatic}
                        value={t(
                          'data-configs.type.vertex.advance.nullable-list.custom'
                        )}
                        onChange={(e: any) => {
                          dataMapStore.toggleCustomNullValue(
                            isEdit ? 'edit' : 'new',
                            'vertex',
                            e.target.checked
                          );

                          if (e.target.checked) {
                            dataMapStore.addValidateValueMappingOption(
                              'null_values'
                            );
                          } else {
                            dataMapStore.resetValidateValueMapping(
                              'null_values'
                            );
                          }
                        }}
                      >
                        {t(
                          'data-configs.type.vertex.advance.nullable-list.custom'
                        )}
                      </Checkbox>
                    </div>
                  </div>
                  <div
                    className="import-tasks-data-options-expand-values"
                    style={{ marginLeft: 12 }}
                  >
                    {isEdit ? (
                      <>
                        {dataMapStore.editedVertexMap?.null_values.customized.map(
                          (nullValue, nullValueIndex) => (
                            <div className="import-tasks-data-options-expand-input">
                              <Input
                                size="medium"
                                width={122}
                                countMode="en"
                                placeholder={t(
                                  'data-configs.type.vertex.advance.placeholder.input'
                                )}
                                value={nullValue}
                                onChange={(e: any) => {
                                  dataMapStore.editCustomNullValues(
                                    'edit',
                                    'vertex',
                                    e.value,
                                    nullValueIndex
                                  );

                                  dataMapStore.validateValueMapping(
                                    'vertex',
                                    'edit',
                                    'null_values',
                                    nullValueIndex
                                  );
                                }}
                                errorLocation="layer"
                                errorMessage={
                                  dataMapStore.validateAdvanceConfigErrorMessage
                                    .null_values[nullValueIndex]
                                }
                                originInputProps={{
                                  onBlur: () => {
                                    dataMapStore.validateValueMapping(
                                      'vertex',
                                      'edit',
                                      'null_values',
                                      nullValueIndex
                                    );
                                  }
                                }}
                              />
                            </div>
                          )
                        )}
                        {!isEmpty(
                          dataMapStore.editedVertexMap?.null_values.customized
                        ) && (
                          <div style={{ marginTop: 8 }}>
                            <span
                              className={addNullValueClassName}
                              onClick={() => {
                                const extraNullValues =
                                  dataMapStore.editedVertexMap?.null_values
                                    .customized;

                                if (!extraNullValues?.includes('')) {
                                  dataMapStore.addCustomNullValues(
                                    'edit',
                                    'vertex'
                                  );
                                }
                              }}
                            >
                              {t('data-configs.manipulations.add')}
                            </span>
                          </div>
                        )}
                      </>
                    ) : (
                      <>
                        {dataMapStore.newVertexType.null_values.customized.map(
                          (nullValue, nullValueIndex) => (
                            <div className="import-tasks-data-options-expand-input">
                              <Input
                                size="medium"
                                width={122}
                                countMode="en"
                                placeholder={t(
                                  'data-configs.type.vertex.advance.placeholder.input'
                                )}
                                value={nullValue}
                                onChange={(e: any) => {
                                  dataMapStore.editCustomNullValues(
                                    'new',
                                    'vertex',
                                    e.value,
                                    nullValueIndex
                                  );

                                  dataMapStore.validateValueMapping(
                                    'vertex',
                                    'new',
                                    'null_values',
                                    nullValueIndex
                                  );
                                }}
                                errorLocation="layer"
                                errorMessage={
                                  dataMapStore.validateAdvanceConfigErrorMessage
                                    .null_values[nullValueIndex]
                                }
                                originInputProps={{
                                  onBlur: () => {
                                    dataMapStore.validateValueMapping(
                                      'vertex',
                                      'new',
                                      'null_values',
                                      nullValueIndex
                                    );
                                  }
                                }}
                              />
                            </div>
                          )
                        )}
                        {!isEmpty(
                          dataMapStore.newVertexType.null_values.customized
                        ) && (
                          <div style={{ marginTop: 8 }}>
                            <span
                              className={addNullValueClassName}
                              onClick={() => {
                                const extraNullValues =
                                  dataMapStore.newVertexType?.null_values
                                    .customized;

                                if (!extraNullValues.includes('')) {
                                  dataMapStore.addCustomNullValues(
                                    'new',
                                    'vertex'
                                  );
                                }
                              }}
                            >
                              {t('data-configs.manipulations.add')}
                            </span>
                          </div>
                        )}
                      </>
                    )}
                  </div>
                </>
              )}
            </div>

            <div
              className="import-tasks-data-options"
              style={{ marginBottom: 16, alignItems: 'flex-start' }}
            >
              <span className="import-tasks-data-options-title in-card">
                {t('data-configs.type.vertex.advance.map-property-value.title')}
                :
              </span>
              {!isCheck &&
                (isEdit
                  ? isEmpty(dataMapStore.editedVertexMap?.value_mapping)
                  : isEmpty(dataMapStore.newVertexType.value_mapping)) && (
                  <div
                    className={addPropertyMapClassName}
                    style={{ lineHeight: '32px' }}
                    onClick={() => {
                      if (isStrategyAutomatic) {
                        return;
                      }

                      isEdit
                        ? dataMapStore.addVertexValueMapping('edit')
                        : dataMapStore.addVertexValueMapping('new');

                      dataMapStore.addValidateValueMappingOption(
                        'value_mappings'
                      );
                    }}
                  >
                    {t(
                      'data-configs.type.vertex.advance.map-property-value.add-value'
                    )}
                  </div>
                )}

              {isCheck && (
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  {isEmpty(dataMapStore.editedVertexMap?.value_mapping)
                    ? '-'
                    : dataMapStore.editedVertexMap?.value_mapping.map(
                        ({ column_name, values }, valueMapIndex) => (
                          <div className="import-tasks-data-options-expand-values">
                            <div className="import-tasks-data-options-expand-info">
                              <span>
                                {t(
                                  'data-configs.type.vertex.advance.map-property-value.fields.property'
                                )}
                                {valueMapIndex + 1}:
                              </span>
                              <span>{column_name}</span>
                            </div>
                            {values.map(({ column_value, mapped_value }) => (
                              <div className="import-tasks-data-options-expand-info">
                                <span>
                                  {t(
                                    'data-configs.type.vertex.advance.map-property-value.fields.value-map'
                                  )}
                                </span>
                                <span>{column_value}</span>
                                <img src={MapIcon} alt="map" />
                                <span>{mapped_value}</span>
                              </div>
                            ))}
                          </div>
                        )
                      )}
                </div>
              )}
            </div>

            {/* property value mapping form */}
            {!isCheck &&
              (!Boolean(checkOrEdit)
                ? dataMapStore.newVertexType.value_mapping.map(
                    ({ column_name, values }, valueMapIndex) => (
                      <div className="import-tasks-data-options-value-maps">
                        <div className="import-tasks-data-options">
                          <span
                            className="import-tasks-data-options-title in-card"
                            style={{ marginRight: 24 }}
                          >
                            {t(
                              'data-configs.type.vertex.advance.map-property-value.fields.property'
                            )}
                            {valueMapIndex + 1}:
                          </span>
                          <Select
                            width={420}
                            size="medium"
                            selectorName={t(
                              'data-configs.type.vertex.advance.placeholder.input-property'
                            )}
                            getPopupContainer={(e: any) => e}
                            value={column_name}
                            onChange={(value: string) => {
                              dataMapStore.editVertexValueMappingColumnName(
                                'new',
                                value,
                                valueMapIndex
                              );
                            }}
                          >
                            {dataMapStore.selectedFileInfo?.file_setting.column_names.map(
                              (columnName) => (
                                <Select.Option
                                  value={columnName}
                                  key={columnName}
                                >
                                  {columnName}
                                </Select.Option>
                              )
                            )}
                          </Select>
                          <span
                            className="import-tasks-manipulation"
                            style={{ marginLeft: 16, lineHeight: '32px' }}
                            onClick={() => {
                              dataMapStore.removeVertexValueMapping(
                                'new',
                                valueMapIndex
                              );

                              dataMapStore.removeValidateValueMappingOption(
                                'vertex',
                                'new',
                                'value_mappings',
                                valueMapIndex
                              );
                            }}
                          >
                            {t('data-configs.manipulations.delete')}
                          </span>
                        </div>
                        <div
                          className="import-tasks-data-options"
                          style={{ alignItems: 'flex-start', marginBottom: 0 }}
                        >
                          <span
                            className="import-tasks-data-options-title in-card"
                            style={{ marginRight: 24 }}
                          >
                            {t(
                              'data-configs.type.vertex.advance.map-property-value.fields.value-map'
                            )}
                            :
                          </span>
                          <div className="import-tasks-data-options-expand-values">
                            {values.map(
                              ({ column_value, mapped_value }, valueIndex) => (
                                <div className="import-tasks-data-options-expand-value">
                                  <Input
                                    size="medium"
                                    width={200}
                                    countMode="en"
                                    placeholder={t(
                                      'data-configs.type.vertex.advance.placeholder.input-file-value'
                                    )}
                                    value={column_value}
                                    onChange={(e: any) => {
                                      dataMapStore.editVertexValueMappingColumnValueName(
                                        'new',
                                        'column_value',
                                        e.value,
                                        valueMapIndex,
                                        valueIndex
                                      );

                                      dataMapStore.validateValueMapping(
                                        'vertex',
                                        'new',
                                        'value_mappings',
                                        valueMapIndex,
                                        {
                                          field: 'column_value',
                                          valueIndex: valueIndex
                                        }
                                      );
                                    }}
                                    errorLocation="layer"
                                    errorMessage={
                                      dataMapStore
                                        .validateAdvanceConfigErrorMessage
                                        .value_mapping[valueMapIndex].values[
                                        valueIndex
                                      ].column_value
                                    }
                                    originInputProps={{
                                      onBlur: () => {
                                        dataMapStore.validateValueMapping(
                                          'vertex',
                                          'new',
                                          'value_mappings',
                                          valueMapIndex,
                                          {
                                            field: 'column_value',
                                            valueIndex: valueIndex
                                          }
                                        );
                                      }
                                    }}
                                  />
                                  <img src={MapIcon} alt="map" />
                                  <Input
                                    size="medium"
                                    width={200}
                                    countMode="en"
                                    placeholder={t(
                                      'data-configs.type.vertex.advance.placeholder.input-graph-value'
                                    )}
                                    value={mapped_value}
                                    onChange={(e: any) => {
                                      dataMapStore.editVertexValueMappingColumnValueName(
                                        'new',
                                        'mapped_value',
                                        e.value,
                                        valueMapIndex,
                                        valueIndex
                                      );

                                      dataMapStore.validateValueMapping(
                                        'vertex',
                                        'new',
                                        'value_mappings',
                                        valueMapIndex,
                                        {
                                          field: 'mapped_value',
                                          valueIndex: valueIndex
                                        }
                                      );
                                    }}
                                    errorLocation="layer"
                                    errorMessage={
                                      dataMapStore
                                        .validateAdvanceConfigErrorMessage
                                        .value_mapping[valueMapIndex].values[
                                        valueIndex
                                      ].mapped_value
                                    }
                                    originInputProps={{
                                      onBlur: () => {
                                        dataMapStore.validateValueMapping(
                                          'vertex',
                                          'new',
                                          'value_mappings',
                                          valueMapIndex,
                                          {
                                            field: 'mapped_value',
                                            valueIndex: valueIndex
                                          }
                                        );
                                      }
                                    }}
                                  />
                                  {values.length > 1 && (
                                    <span
                                      className="import-tasks-manipulation"
                                      style={{
                                        marginLeft: 16,
                                        lineHeight: '32px'
                                      }}
                                      onClick={() => {
                                        dataMapStore.removeVertexValueMappingValue(
                                          'new',
                                          valueMapIndex,
                                          valueIndex
                                        );

                                        dataMapStore.removeValidateValueMappingOption(
                                          'vertex',
                                          'new',
                                          'value_mappings_value',
                                          valueMapIndex,
                                          valueIndex
                                        );
                                      }}
                                    >
                                      {t('data-configs.manipulations.delete')}
                                    </span>
                                  )}
                                </div>
                              )
                            )}
                            <div className="import-tasks-data-options-expand-manipulation">
                              <div
                                className={classnames({
                                  'import-tasks-manipulation': true,
                                  'import-tasks-manipulation-disabled': !dataMapStore.allowAddPropertyValueMapping(
                                    'vertex',
                                    valueMapIndex
                                  )
                                })}
                              >
                                <span
                                  onClick={() => {
                                    if (
                                      !dataMapStore.allowAddPropertyValueMapping(
                                        'vertex',
                                        valueMapIndex
                                      )
                                    ) {
                                      return;
                                    }

                                    dataMapStore.addVertexValueMappingValue(
                                      'new',
                                      valueMapIndex
                                    );

                                    dataMapStore.addValidateValueMappingOption(
                                      'value_mappings_value',
                                      valueMapIndex
                                    );
                                  }}
                                >
                                  {t(
                                    'data-configs.type.vertex.advance.map-property-value.fields.add-value-map'
                                  )}
                                </span>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    )
                  )
                : dataMapStore.editedVertexMap?.value_mapping.map(
                    ({ column_name, values }, valueMapIndex) => (
                      <div className="import-tasks-data-options-value-maps">
                        <div className="import-tasks-data-options">
                          <span
                            className="import-tasks-data-options-title in-card"
                            style={{ marginRight: 24 }}
                          >
                            {t(
                              'data-configs.type.vertex.advance.map-property-value.fields.property'
                            )}
                            {valueMapIndex + 1}:
                          </span>
                          <Select
                            width={420}
                            size="medium"
                            getPopupContainer={(e: any) => e}
                            selectorName={t(
                              'data-configs.type.vertex.advance.placeholder.input-property'
                            )}
                            value={column_name}
                            onChange={(value: string) => {
                              dataMapStore.editVertexValueMappingColumnName(
                                'edit',
                                value,
                                valueMapIndex
                              );
                            }}
                          >
                            {dataMapStore.selectedFileInfo?.file_setting.column_names.map(
                              (columnName) => (
                                <Select.Option
                                  value={columnName}
                                  key={columnName}
                                >
                                  {columnName}
                                </Select.Option>
                              )
                            )}
                          </Select>
                          <span
                            className="import-tasks-manipulation"
                            style={{ marginLeft: 16, lineHeight: '32px' }}
                            onClick={() => {
                              dataMapStore.removeVertexValueMapping(
                                'edit',
                                valueMapIndex
                              );

                              dataMapStore.removeValidateValueMappingOption(
                                'vertex',
                                'edit',
                                'value_mappings',
                                valueMapIndex
                              );
                            }}
                          >
                            {t('data-configs.manipulations.delete')}
                          </span>
                        </div>
                        <div
                          className="import-tasks-data-options"
                          style={{ alignItems: 'flex-start', marginBottom: 0 }}
                        >
                          <span
                            className="import-tasks-data-options-title in-card"
                            style={{ marginRight: 24 }}
                          >
                            {t(
                              'data-configs.type.vertex.advance.map-property-value.fields.value-map'
                            )}
                            :
                          </span>
                          <div className="import-tasks-data-options-expand-values">
                            {values.map(
                              ({ column_value, mapped_value }, valueIndex) => (
                                <div className="import-tasks-data-options-expand-value">
                                  <Input
                                    size="medium"
                                    width={200}
                                    countMode="en"
                                    placeholder={t(
                                      'data-configs.type.vertex.advance.placeholder.input-file-value'
                                    )}
                                    value={column_value}
                                    onChange={(e: any) => {
                                      dataMapStore.editVertexValueMappingColumnValueName(
                                        'edit',
                                        'column_value',
                                        e.value,
                                        valueMapIndex,
                                        valueIndex
                                      );

                                      dataMapStore.validateValueMapping(
                                        'vertex',
                                        'edit',
                                        'value_mappings',
                                        valueMapIndex,
                                        {
                                          field: 'column_value',
                                          valueIndex: valueIndex
                                        }
                                      );
                                    }}
                                    errorLocation="layer"
                                    errorMessage={
                                      dataMapStore
                                        .validateAdvanceConfigErrorMessage
                                        .value_mapping[valueMapIndex].values[
                                        valueIndex
                                      ].column_value
                                    }
                                    originInputProps={{
                                      onBlur: () => {
                                        dataMapStore.validateValueMapping(
                                          'vertex',
                                          'edit',
                                          'value_mappings',
                                          valueMapIndex,
                                          {
                                            field: 'column_value',
                                            valueIndex: valueIndex
                                          }
                                        );
                                      }
                                    }}
                                  />
                                  <img src={MapIcon} alt="map" />
                                  <Input
                                    size="medium"
                                    width={200}
                                    countMode="en"
                                    placeholder={t(
                                      'data-configs.type.vertex.advance.placeholder.input-file-value'
                                    )}
                                    value={mapped_value}
                                    onChange={(e: any) => {
                                      dataMapStore.editVertexValueMappingColumnValueName(
                                        'edit',
                                        'mapped_value',
                                        e.value,
                                        valueMapIndex,
                                        valueIndex
                                      );

                                      dataMapStore.validateValueMapping(
                                        'vertex',
                                        'edit',
                                        'value_mappings',
                                        valueMapIndex,
                                        {
                                          field: 'mapped_value',
                                          valueIndex: valueIndex
                                        }
                                      );
                                    }}
                                    errorLocation="layer"
                                    errorMessage={
                                      dataMapStore
                                        .validateAdvanceConfigErrorMessage
                                        .value_mapping[valueMapIndex].values[
                                        valueIndex
                                      ].mapped_value
                                    }
                                    originInputProps={{
                                      onBlur: () => {
                                        dataMapStore.validateValueMapping(
                                          'vertex',
                                          'edit',
                                          'value_mappings',
                                          valueMapIndex,
                                          {
                                            field: 'mapped_value',
                                            valueIndex: valueIndex
                                          }
                                        );
                                      }
                                    }}
                                  />
                                  {values.length > 1 && (
                                    <span
                                      className="import-tasks-manipulation"
                                      style={{
                                        marginLeft: 16,
                                        lineHeight: '32px'
                                      }}
                                      onClick={() => {
                                        dataMapStore.removeVertexValueMappingValue(
                                          'edit',
                                          valueMapIndex,
                                          valueIndex
                                        );

                                        dataMapStore.removeValidateValueMappingOption(
                                          'vertex',
                                          'edit',
                                          'value_mappings_value',
                                          valueMapIndex,
                                          valueIndex
                                        );
                                      }}
                                    >
                                      {t('data-configs.manipulations.delete')}
                                    </span>
                                  )}
                                </div>
                              )
                            )}
                            <div className="import-tasks-data-options-expand-manipulation">
                              <div
                                className={classnames({
                                  'import-tasks-manipulation': true,
                                  'import-tasks-manipulation-disabled': !dataMapStore.allowAddPropertyValueMapping(
                                    'vertex',
                                    valueMapIndex
                                  )
                                })}
                              >
                                <span
                                  onClick={() => {
                                    if (
                                      !dataMapStore.allowAddPropertyValueMapping(
                                        'vertex',
                                        valueMapIndex
                                      )
                                    ) {
                                      return;
                                    }

                                    dataMapStore.addVertexValueMappingValue(
                                      'edit',
                                      valueMapIndex
                                    );

                                    dataMapStore.addValidateValueMappingOption(
                                      'value_mappings_value',
                                      valueMapIndex
                                    );
                                  }}
                                >
                                  {t(
                                    'data-configs.type.vertex.advance.map-property-value.fields.add-value-map'
                                  )}
                                </span>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    )
                  ))}

            {!isCheck &&
              (isEdit
                ? !isEmpty(dataMapStore.editedVertexMap?.value_mapping)
                : !isEmpty(dataMapStore.newVertexType.value_mapping)) && (
                <div
                  className={addPropertyMapClassName}
                  style={{ marginTop: 16 }}
                  onClick={() => {
                    if (!dataMapStore.allowAddPropertyMapping('vertex')) {
                      return;
                    }

                    isEdit
                      ? dataMapStore.addVertexValueMapping('edit')
                      : dataMapStore.addVertexValueMapping('new');

                    dataMapStore.addValidateValueMappingOption(
                      'value_mappings'
                    );
                  }}
                >
                  {t(
                    'data-configs.type.vertex.advance.map-property-value.add-value'
                  )}
                </div>
              )}
          </>
        )}

        {!isCheck && (
          <TypeConfigManipulations
            type="vertex"
            status={isEdit ? 'edit' : 'add'}
            disableSave={
              isStrategyAutomatic ||
              isEmpty(vertexMap.label) ||
              vertexMap.id_fields.includes('') ||
              vertexMap.field_mapping.some(({ mapped_name }) =>
                isEmpty(mapped_name)
              ) ||
              !dataMapStore.allowAddPropertyMapping('vertex') ||
              !dataMapStore.isValidateSave
            }
            onCreate={async () => {
              dataMapStore.switchAddNewTypeConfig(false);
              dataMapStore.switchEditTypeConfig(false);

              // really weird! if import task comes from import-manager
              // datamapStore.fileMapInfos cannot be updated in <TypeConfigs />
              // though it's already re-rendered
              if (dataMapStore.isIrregularProcess) {
                isEdit
                  ? await dataMapStore.updateVertexMap(
                      'upgrade',
                      dataMapStore.selectedFileId
                    )
                  : await dataMapStore.updateVertexMap(
                      'add',
                      dataMapStore.selectedFileId
                    );

                dataMapStore.fetchDataMaps();
              } else {
                isEdit
                  ? await dataMapStore.updateVertexMap(
                      'upgrade',
                      dataMapStore.selectedFileId
                    )
                  : await dataMapStore.updateVertexMap(
                      'add',
                      dataMapStore.selectedFileId
                    );
              }

              if (dataMapStore.requestStatus.updateVertexMap === 'failed') {
                Message.error({
                  content: dataMapStore.errorInfo.updateVertexMap.message,
                  size: 'medium',
                  showCloseIcon: false
                });
              }

              onCancelCreateVertex();
              dataMapStore.resetNewMap('vertex');
            }}
            onCancel={() => {
              dataMapStore.switchAddNewTypeConfig(false);
              dataMapStore.switchEditTypeConfig(false);

              if (!isEdit) {
                dataMapStore.resetNewMap('vertex');
              } else {
                dataMapStore.resetEditMapping('vertex');
              }

              onCancelCreateVertex();
              dataMapStore.resetNewMap('vertex');
              dataMapStore.resetValidateValueMapping('all');
            }}
          />
        )}
      </div>
    );
  }
);

export default VertexMap;
