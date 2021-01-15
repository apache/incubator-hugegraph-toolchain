import React, { useContext, useState } from 'react';
import { observer } from 'mobx-react';
import { isUndefined, isEmpty, size, cloneDeep } from 'lodash-es';
import { useTranslation } from 'react-i18next';
import classnames from 'classnames';
import { Input, Select, Checkbox, Message } from '@baidu/one-ui';

import { Tooltip } from '../../../../common';
import { DataImportRootStoreContext } from '../../../../../stores';
import {
  VertexType,
  EdgeType
} from '../../../../../stores/types/GraphManagementStore/metadataConfigsStore';
import TypeConfigManipulations from './TypeConfigManipulations';
import { getUnicodeLength } from '../../../../../utils';

import ArrowIcon from '../../../../../assets/imgs/ic_arrow_16.svg';
import BlueArrowIcon from '../../../../../assets/imgs/ic_arrow_blue.svg';
import CloseIcon from '../../../../../assets/imgs/ic_close_16.svg';
import MapIcon from '../../../../../assets/imgs/ic_yingshe_16.svg';

export interface EdgeMapProps {
  checkOrEdit: 'check' | 'edit' | boolean;
  onCancelCreateEdge: () => void;
  edgeMapIndex?: number;
}

const EdgeMap: React.FC<EdgeMapProps> = observer(
  ({ checkOrEdit, onCancelCreateEdge, edgeMapIndex }) => {
    const dataImportRootStore = useContext(DataImportRootStoreContext);
    const { dataMapStore } = dataImportRootStore;
    const [isAddMapping, switchAddMapping] = useState(false);
    const [isExpandAdvance, switchExpandAdvance] = useState(false);
    const { t } = useTranslation();

    const isCheck = checkOrEdit === 'check';
    const isEdit = checkOrEdit === 'edit';
    const edgeMap =
      checkOrEdit !== false
        ? dataMapStore.editedEdgeMap!
        : dataMapStore.newEdgeType;
    const filteredColumnNamesInSelection =
      checkOrEdit !== false
        ? dataMapStore.filteredColumnNamesInEdgeEditSelection
        : dataMapStore.filteredColumnNamesInEdgeNewSelection;

    const findEdge = (collection: EdgeType[], label: string) =>
      collection.find(({ name }) => name === label);

    const findVertex = (collection: VertexType[], label: string) =>
      collection.find(({ name }) => name === label);

    const selectedEdge = findEdge(dataImportRootStore.edgeTypes, edgeMap.label);

    let selectedSourceVertex: VertexType | undefined;
    let selectedTargetVertex: VertexType | undefined;

    if (!isUndefined(selectedEdge)) {
      selectedSourceVertex = findVertex(
        dataImportRootStore.vertexTypes,
        selectedEdge.source_label
      );
      selectedTargetVertex = findVertex(
        dataImportRootStore.vertexTypes,
        selectedEdge.target_label
      );
    }

    const isStrategyAutomatic =
      selectedSourceVertex?.id_strategy === 'AUTOMATIC' ||
      selectedTargetVertex?.id_strategy === 'AUTOMATIC';

    const handleExpand = () => {
      dataMapStore.switchExpand('type', !dataMapStore.isExpandTypeConfig);
    };

    const handleExpandAdvance = () => {
      switchExpandAdvance(!isExpandAdvance);
    };

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
      'import-tasks-manipulation-disabled': edgeMap.null_values.customized.includes(
        ''
      )
    });

    const addPropertyMapClassName = classnames({
      'import-tasks-manipulation': true,
      'import-tasks-manipulation-disabled':
        !dataMapStore.allowAddPropertyMapping('edge') || isStrategyAutomatic
    });

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
            <span>{t('data-configs.type.edge.title')}</span>
            <img
              src={CloseIcon}
              alt="collpaseOrExpand"
              onClick={onCancelCreateEdge}
            />
          </div>
        )}
        <div className="import-tasks-data-options">
          <span className="import-tasks-data-options-title in-card">
            {t('data-configs.type.edge.type')}:
          </span>
          {isCheck ? (
            <span>{dataMapStore.editedEdgeMap!.label}</span>
          ) : (
            <Select
              width={420}
              size="medium"
              getPopupContainer={(e: any) => e}
              selectorName={t('data-configs.type.placeholder.select-edge-type')}
              value={
                isEdit
                  ? dataMapStore.editedEdgeMap!.label
                  : dataMapStore.newEdgeType.label
              }
              onChange={(value: string) => {
                const newSelectedEdge = findEdge(
                  dataImportRootStore.edgeTypes,
                  value
                );

                let newSelectedSourceVertex: VertexType | undefined;
                let newSelectedTargetVertex: VertexType | undefined;

                if (!isUndefined(newSelectedEdge)) {
                  newSelectedSourceVertex = findVertex(
                    dataImportRootStore.vertexTypes,
                    newSelectedEdge.source_label
                  );
                  newSelectedTargetVertex = findVertex(
                    dataImportRootStore.vertexTypes,
                    newSelectedEdge.target_label
                  );
                }

                if (isEdit) {
                  // clear new edge map data fisrt
                  dataMapStore.resetEditMapping('edge');
                  dataMapStore.syncEditMap('edge', edgeMapIndex!);
                  switchAddMapping(false);

                  dataMapStore.editEdgeMapConfig('label', value, edgeMapIndex!);

                  if (newSelectedSourceVertex?.id_strategy === 'PRIMARY_KEY') {
                    dataMapStore.editEdgeMapConfig(
                      'source_fields',
                      newSelectedSourceVertex.primary_keys.map(() => ''),
                      edgeMapIndex!
                    );
                  } else {
                    dataMapStore.editEdgeMapConfig(
                      'source_fields',
                      [''],
                      edgeMapIndex!
                    );
                  }

                  if (newSelectedTargetVertex?.id_strategy === 'PRIMARY_KEY') {
                    dataMapStore.editEdgeMapConfig(
                      'target_fields',
                      newSelectedTargetVertex.primary_keys.map(() => ''),
                      edgeMapIndex!
                    );
                  } else {
                    dataMapStore.editEdgeMapConfig(
                      'target_fields',
                      [''],
                      edgeMapIndex!
                    );
                  }

                  // reset field mapping values since it binds with label
                  dataMapStore.editedEdgeMap?.field_mapping.forEach(
                    ({ column_name }, fieldIndex) => {
                      dataMapStore.setEdgeFieldMapping('edit', '', fieldIndex);
                    }
                  );
                } else {
                  // clear new vertex map data first
                  dataMapStore.resetNewMap('edge');
                  switchAddMapping(false);

                  dataMapStore.setNewEdgeConfig('label', value);

                  if (newSelectedSourceVertex?.id_strategy === 'PRIMARY_KEY') {
                    dataMapStore.setNewEdgeConfig(
                      'source_fields',
                      newSelectedSourceVertex.primary_keys.map(() => '')
                    );
                  } else {
                    dataMapStore.setNewEdgeConfig('source_fields', ['']);
                  }

                  if (newSelectedTargetVertex?.id_strategy === 'PRIMARY_KEY') {
                    dataMapStore.setNewEdgeConfig(
                      'target_fields',
                      newSelectedTargetVertex.primary_keys.map(() => '')
                    );
                  } else {
                    dataMapStore.setNewEdgeConfig('target_fields', ['']);
                  }

                  dataMapStore.newEdgeType.field_mapping.forEach(
                    ({ column_name }, fieldIndex) => {
                      dataMapStore.setEdgeFieldMapping('new', '', fieldIndex);
                    }
                  );
                }
              }}
            >
              {dataImportRootStore.edgeTypes.map(({ name }) => (
                <Select.Option value={name} key={name}>
                  {name}
                </Select.Option>
              ))}
            </Select>
          )}
        </div>

        {!isUndefined(selectedSourceVertex) && (
          <div className="import-tasks-data-options">
            <span className="import-tasks-data-options-title in-card">
              {t('data-configs.type.edge.source-ID-strategy')}:
            </span>
            <Tooltip
              placement="bottom-start"
              tooltipShown={selectedSourceVertex.id_strategy === 'AUTOMATIC'}
              modifiers={{
                offset: {
                  offset: '0, 10'
                }
              }}
              tooltipWrapperProps={{
                className: 'import-tasks-tooltips'
              }}
              tooltipWrapper={
                <span>
                  {t('data-configs.type.hint.lack-support-for-automatic')}
                </span>
              }
            >
              {t(
                `data-configs.type.ID-strategy.${selectedSourceVertex?.id_strategy}`
              )}
              {selectedSourceVertex?.id_strategy === 'PRIMARY_KEY' &&
                `-${selectedSourceVertex?.primary_keys.join('，')}`}
            </Tooltip>
          </div>
        )}
        {edgeMap.source_fields.map((idField, fieldIndex) => {
          return (
            <div
              className="import-tasks-data-options"
              key={idField + fieldIndex}
            >
              <span className="import-tasks-data-options-title in-card">
                {t('data-configs.type.edge.ID-column') +
                  (selectedSourceVertex?.id_strategy === 'PRIMARY_KEY'
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
                  disabled={isUndefined(selectedEdge) || isStrategyAutomatic}
                  value={idField}
                  onChange={(value: string) => {
                    const clonedIdField = cloneDeep(edgeMap.source_fields);
                    clonedIdField[fieldIndex] = value;

                    if (isEdit) {
                      dataMapStore.editEdgeMapConfig(
                        'source_fields',
                        clonedIdField,
                        edgeMapIndex!
                      );

                      // remove selected field mappings after reselect column name
                      if (
                        !isUndefined(
                          dataMapStore.editedEdgeMap?.field_mapping.find(
                            ({ column_name }) => column_name === value
                          )
                        )
                      ) {
                        dataMapStore.removeEdgeFieldMapping('edit', value);
                      }
                    } else {
                      dataMapStore.setNewEdgeConfig(
                        'source_fields',
                        clonedIdField
                      );

                      if (
                        !isUndefined(
                          dataMapStore.newEdgeType.field_mapping.find(
                            ({ column_name }) => column_name === value
                          )
                        )
                      ) {
                        dataMapStore.removeEdgeFieldMapping('new', value);
                      }
                    }
                  }}
                >
                  {dataMapStore
                    .selectedFileInfo!.file_setting.column_names.filter(
                      (columnName) =>
                        !edgeMap.source_fields.includes(columnName)
                    )
                    .map((name) => (
                      <Select.Option value={name} key={name}>
                        {name}
                      </Select.Option>
                    ))}
                </Select>
              )}
            </div>
          );
        })}

        {!isUndefined(selectedTargetVertex) && (
          <div className="import-tasks-data-options">
            <span className="import-tasks-data-options-title in-card">
              {t('data-configs.type.edge.target-ID-strategy')}:
            </span>
            <Tooltip
              placement="bottom-start"
              tooltipShown={selectedTargetVertex.id_strategy === 'AUTOMATIC'}
              modifiers={{
                offset: {
                  offset: '0, 10'
                }
              }}
              tooltipWrapperProps={{
                className: 'import-tasks-tooltips'
              }}
              tooltipWrapper={
                <span>
                  {t('data-configs.type.hint.lack-support-for-automatic')}
                </span>
              }
            >
              {t(
                `data-configs.type.ID-strategy.${selectedTargetVertex?.id_strategy}`
              )}
              {selectedTargetVertex?.id_strategy === 'PRIMARY_KEY' &&
                `-${selectedTargetVertex?.primary_keys.join('，')}`}
            </Tooltip>
          </div>
        )}
        {edgeMap.target_fields.map((idField, fieldIndex) => {
          return (
            <div
              className="import-tasks-data-options"
              key={idField + fieldIndex}
            >
              <span className="import-tasks-data-options-title in-card">
                {t('data-configs.type.edge.ID-column') +
                  (selectedTargetVertex?.id_strategy === 'PRIMARY_KEY'
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
                  disabled={isUndefined(selectedEdge) || isStrategyAutomatic}
                  value={idField}
                  onChange={(value: string) => {
                    const clonedIdField = cloneDeep(edgeMap.target_fields);
                    clonedIdField[fieldIndex] = value;

                    if (isEdit) {
                      dataMapStore.editEdgeMapConfig(
                        'target_fields',
                        clonedIdField,
                        edgeMapIndex!
                      );

                      // remove selected field mappings after reselect column name
                      if (
                        !isUndefined(
                          dataMapStore.editedEdgeMap?.field_mapping.find(
                            ({ column_name }) => column_name === value
                          )
                        )
                      ) {
                        dataMapStore.removeEdgeFieldMapping('edit', value);
                      }
                    } else {
                      dataMapStore.setNewEdgeConfig(
                        'target_fields',
                        clonedIdField
                      );

                      if (
                        !isUndefined(
                          dataMapStore.newEdgeType.field_mapping.find(
                            ({ column_name }) => column_name === value
                          )
                        )
                      ) {
                        dataMapStore.removeEdgeFieldMapping('new', value);
                      }
                    }
                  }}
                >
                  {dataMapStore
                    .selectedFileInfo!.file_setting.column_names.filter(
                      (columnName) =>
                        !edgeMap.target_fields.includes(columnName)
                    )
                    .map((name) => (
                      <Select.Option value={name} key={name}>
                        {name}
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
            {t('data-configs.type.edge.map-settings')}:
          </span>
          <div className="import-tasks-data-options-expand-table">
            {isCheck &&
              isEmpty(dataMapStore.editedEdgeMap?.field_mapping) &&
              '-'}

            {((Boolean(checkOrEdit) === false &&
              !isEmpty(dataMapStore.newEdgeType.field_mapping)) ||
              (Boolean(checkOrEdit) !== false &&
                !isEmpty(dataMapStore.editedEdgeMap!.field_mapping))) && (
              <div className="import-tasks-data-options-expand-table-row">
                <div className="import-tasks-data-options-expand-table-column">
                  {t('data-configs.type.edge.add-map.name')}
                </div>
                <div className="import-tasks-data-options-expand-table-column">
                  {t('data-configs.type.edge.add-map.sample')}
                </div>
                <div className="import-tasks-data-options-expand-table-column">
                  {t('data-configs.type.edge.add-map.property')}
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
                          ? dataMapStore.toggleEdgeSelectAllFieldMapping(
                              'edit',
                              false,
                              selectedEdge
                            )
                          : dataMapStore.toggleEdgeSelectAllFieldMapping(
                              'new',
                              false,
                              selectedEdge
                            );
                      }}
                    >
                      {t('data-configs.type.edge.add-map.clear')}
                    </span>
                  )}
                </div>
              </div>
            )}
            {edgeMap.field_mapping.map(
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
                            dataMapStore.setEdgeFieldMapping(
                              param,
                              value,
                              fieldIndex
                            );
                          }}
                        >
                          {selectedEdge?.properties.map(({ name }) => (
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
                            dataMapStore.removeEdgeFieldMapping(
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
                  style={{ marginBottom: 8, width: 'fit-content' }}
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
                    {t('data-configs.type.edge.add-map.title')}
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
                  <div className="import-tasks-data-options-expand-dropdown">
                    <div>
                      <span>
                        <Checkbox
                          checked={
                            isEdit
                              ? !isEmpty(
                                  dataMapStore.editedEdgeMap?.field_mapping
                                )
                              : !isEmpty(dataMapStore.newEdgeType.field_mapping)
                          }
                          indeterminate={
                            isEdit
                              ? !isEmpty(
                                  dataMapStore.editedEdgeMap?.field_mapping
                                ) &&
                                size(
                                  dataMapStore.editedEdgeMap?.field_mapping
                                ) !==
                                  size(
                                    dataMapStore.filteredColumnNamesInEdgeEditSelection
                                  )
                              : !isEmpty(
                                  dataMapStore.newEdgeType.field_mapping
                                ) &&
                                size(dataMapStore.newEdgeType.field_mapping) !==
                                  size(
                                    dataMapStore.filteredColumnNamesInEdgeNewSelection
                                  )
                          }
                          onChange={(e: any) => {
                            if (isEdit) {
                              const isIndeterminate =
                                !isEmpty(
                                  dataMapStore.editedEdgeMap?.field_mapping
                                ) &&
                                size(
                                  dataMapStore.editedEdgeMap?.field_mapping
                                ) !==
                                  size(
                                    dataMapStore.filteredColumnNamesInEdgeEditSelection
                                  );

                              dataMapStore.toggleEdgeSelectAllFieldMapping(
                                'edit',
                                // if isIndeterminate is true, e.target.checked is false
                                isIndeterminate || e.target.checked,
                                selectedEdge
                              );
                            } else {
                              const isIndeterminate =
                                !isEmpty(
                                  dataMapStore.newEdgeType.field_mapping
                                ) &&
                                size(dataMapStore.newEdgeType.field_mapping) !==
                                  size(
                                    dataMapStore.filteredColumnNamesInEdgeNewSelection
                                  );

                              dataMapStore.toggleEdgeSelectAllFieldMapping(
                                'new',
                                isIndeterminate || e.target.checked,
                                selectedEdge
                              );
                            }
                          }}
                        >
                          {t('data-configs.type.edge.select-all')}
                        </Checkbox>
                      </span>
                    </div>
                    {filteredColumnNamesInSelection.map((name) => {
                      const param = isEdit ? 'edit' : 'new';

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

                      const mappingValue = selectedEdge?.properties.find(
                        ({ name: propertyName }) => propertyName === name
                      )?.name;

                      return (
                        <div>
                          <span>
                            <Checkbox
                              checked={
                                !isUndefined(
                                  edgeMap.field_mapping.find(
                                    ({ column_name }) => column_name === name
                                  )
                                )
                              }
                              onChange={(e: any) => {
                                if (e.target.checked) {
                                  dataMapStore.setEdgeFieldMappingKey(
                                    param,
                                    name,
                                    mappingValue
                                  );
                                } else {
                                  dataMapStore.removeEdgeFieldMapping(
                                    param,
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
          <span>{t('data-configs.type.edge.advance.title')}</span>
          {!isCheck && (
            <img
              src={ArrowIcon}
              alt="collpaseOrExpand"
              className={expandAdvanceClassName}
              onClick={handleExpandAdvance}
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
                {t('data-configs.type.edge.advance.nullable-list.title')}:
              </span>
              {isCheck ? (
                <span>
                  {dataMapStore
                    .editedEdgeMap!.null_values.checked.filter(
                      (value) => value !== 'null'
                    )
                    .map((value) =>
                      value === ''
                        ? '空值'
                        : value === 'NULL'
                        ? 'NULL/null'
                        : value
                    )
                    .concat(dataMapStore.editedEdgeMap!.null_values.customized)
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
                              'edge',
                              checkedList
                            )
                          : dataMapStore.editCheckedNullValues(
                              'new',
                              'edge',
                              checkedList
                            );
                      }}
                      value={
                        isEdit
                          ? dataMapStore.editedEdgeMap!.null_values.checked
                          : dataMapStore.newEdgeType.null_values.checked
                      }
                    >
                      <Checkbox value="NULL">NULL/null</Checkbox>
                      <Checkbox value={''}>
                        {t(
                          'data-configs.type.edge.advance.nullable-list.empty'
                        )}
                      </Checkbox>
                    </Checkbox.Group>
                    <div style={{ marginLeft: 20 }}>
                      <Checkbox
                        disabled={isStrategyAutomatic}
                        value={t(
                          'data-configs.type.edge.advance.nullable-list.custom'
                        )}
                        onChange={(e: any) => {
                          dataMapStore.toggleCustomNullValue(
                            isEdit ? 'edit' : 'new',
                            'edge',
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
                          'data-configs.type.edge.advance.nullable-list.custom'
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
                        {dataMapStore.editedEdgeMap?.null_values.customized.map(
                          (nullValue, nullValueIndex) => (
                            <div className="import-tasks-data-options-expand-input">
                              <Input
                                size="medium"
                                width={122}
                                countMode="en"
                                placeholder={t(
                                  'data-configs.type.edge.advance.placeholder.input'
                                )}
                                value={nullValue}
                                onChange={(e: any) => {
                                  dataMapStore.editCustomNullValues(
                                    'edit',
                                    'edge',
                                    e.value,
                                    nullValueIndex
                                  );

                                  dataMapStore.validateValueMapping(
                                    'edge',
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
                                      'edge',
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
                          dataMapStore.editedEdgeMap?.null_values.customized
                        ) && (
                          <div style={{ marginTop: 8 }}>
                            <span
                              className={addNullValueClassName}
                              onClick={() => {
                                const extraNullValues =
                                  dataMapStore.editedEdgeMap?.null_values
                                    .customized;

                                if (!extraNullValues?.includes('')) {
                                  dataMapStore.addCustomNullValues(
                                    'edit',
                                    'edge'
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
                        {dataMapStore.newEdgeType.null_values.customized.map(
                          (nullValue, nullValueIndex) => (
                            <div className="import-tasks-data-options-expand-input">
                              <Input
                                size="medium"
                                width={122}
                                countMode="en"
                                placeholder={t(
                                  'data-configs.type.edge.advance.placeholder.input'
                                )}
                                value={nullValue}
                                onChange={(e: any) => {
                                  dataMapStore.editCustomNullValues(
                                    'new',
                                    'edge',
                                    e.value,
                                    nullValueIndex
                                  );

                                  dataMapStore.validateValueMapping(
                                    'edge',
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
                                      'edge',
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
                          dataMapStore.newEdgeType.null_values.customized
                        ) && (
                          <div style={{ marginTop: 8 }}>
                            <span
                              className={addNullValueClassName}
                              onClick={() => {
                                const extraNullValues =
                                  dataMapStore.newEdgeType?.null_values
                                    .customized;

                                if (!extraNullValues.includes('')) {
                                  dataMapStore.addCustomNullValues(
                                    'new',
                                    'edge'
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
                {t('data-configs.type.edge.advance.map-property-value.title')}:
              </span>
              {!isCheck &&
                (isEdit
                  ? isEmpty(dataMapStore.editedEdgeMap?.value_mapping)
                  : isEmpty(dataMapStore.newEdgeType.value_mapping)) && (
                  <div
                    className={addPropertyMapClassName}
                    style={{ lineHeight: '32px' }}
                    onClick={() => {
                      if (isStrategyAutomatic) {
                        return;
                      }

                      isEdit
                        ? dataMapStore.addEdgeValueMapping('edit')
                        : dataMapStore.addEdgeValueMapping('new');

                      dataMapStore.addValidateValueMappingOption(
                        'value_mappings'
                      );
                    }}
                  >
                    {t(
                      'data-configs.type.edge.advance.map-property-value.add-value'
                    )}
                  </div>
                )}

              {isCheck && (
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  {isEmpty(dataMapStore.editedEdgeMap?.value_mapping)
                    ? '-'
                    : dataMapStore.editedEdgeMap?.value_mapping.map(
                        ({ column_name, values }, valueMapIndex) => (
                          <div className="import-tasks-data-options-expand-values">
                            <div className="import-tasks-data-options-expand-info">
                              <span>
                                {t(
                                  'data-configs.type.edge.advance.map-property-value.fields.property'
                                )}
                                {valueMapIndex + 1}:
                              </span>
                              <span>{column_name}</span>
                            </div>
                            {values.map(({ column_value, mapped_value }) => (
                              <div className="import-tasks-data-options-expand-info">
                                <span>
                                  {t(
                                    'data-configs.type.edge.advance.map-property-value.fields.value-map'
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
                ? dataMapStore.newEdgeType.value_mapping.map(
                    ({ column_name, values }, valueMapIndex) => (
                      <div className="import-tasks-data-options-value-maps">
                        <div className="import-tasks-data-options">
                          <span
                            className="import-tasks-data-options-title in-card"
                            style={{ marginRight: 24 }}
                          >
                            {t(
                              'data-configs.type.edge.advance.map-property-value.fields.property'
                            )}
                            {valueMapIndex + 1}:
                          </span>
                          <Select
                            width={420}
                            size="medium"
                            selectorName={t(
                              'data-configs.type.edge.advance.placeholder.input-property'
                            )}
                            getPopupContainer={(e: any) => e}
                            value={column_name}
                            onChange={(value: string) => {
                              dataMapStore.editEdgeValueMappingColumnName(
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
                              dataMapStore.removeEdgeValueMapping(
                                'new',
                                valueMapIndex
                              );

                              dataMapStore.removeValidateValueMappingOption(
                                'edge',
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
                              'data-configs.type.edge.advance.map-property-value.fields.value-map'
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
                                      'data-configs.type.edge.advance.placeholder.input-file-value'
                                    )}
                                    value={column_value}
                                    onChange={(e: any) => {
                                      dataMapStore.editEdgeValueMappingColumnValueName(
                                        'new',
                                        'column_value',
                                        e.value,
                                        valueMapIndex,
                                        valueIndex
                                      );

                                      dataMapStore.validateValueMapping(
                                        'edge',
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
                                          'edge',
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
                                      'data-configs.type.edge.advance.placeholder.input-graph-value'
                                    )}
                                    value={mapped_value}
                                    onChange={(e: any) => {
                                      dataMapStore.editEdgeValueMappingColumnValueName(
                                        'new',
                                        'mapped_value',
                                        e.value,
                                        valueMapIndex,
                                        valueIndex
                                      );

                                      dataMapStore.validateValueMapping(
                                        'edge',
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
                                          'edge',
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
                                        dataMapStore.removeEdgeValueMappingValue(
                                          'new',
                                          valueMapIndex,
                                          valueIndex
                                        );

                                        dataMapStore.removeValidateValueMappingOption(
                                          'edge',
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
                                    'edge',
                                    valueMapIndex
                                  )
                                })}
                              >
                                <span
                                  onClick={() => {
                                    if (
                                      !dataMapStore.allowAddPropertyValueMapping(
                                        'edge',
                                        valueMapIndex
                                      )
                                    ) {
                                      return;
                                    }

                                    dataMapStore.addEdgeValueMappingValue(
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
                                    'data-configs.type.edge.advance.map-property-value.fields.add-value-map'
                                  )}
                                </span>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    )
                  )
                : dataMapStore.editedEdgeMap?.value_mapping.map(
                    ({ column_name, values }, valueMapIndex) => (
                      <div className="import-tasks-data-options-value-maps">
                        <div className="import-tasks-data-options">
                          <span
                            className="import-tasks-data-options-title in-card"
                            style={{ marginRight: 24 }}
                          >
                            {t(
                              'data-configs.type.edge.advance.map-property-value.fields.property'
                            )}
                            {valueMapIndex + 1}:
                          </span>
                          <Select
                            width={420}
                            size="medium"
                            getPopupContainer={(e: any) => e}
                            selectorName={t(
                              'data-configs.type.edge.advance.placeholder.input-property'
                            )}
                            value={column_name}
                            onChange={(value: string) => {
                              dataMapStore.editEdgeValueMappingColumnName(
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
                              dataMapStore.removeEdgeValueMapping(
                                'edit',
                                valueMapIndex
                              );

                              dataMapStore.removeValidateValueMappingOption(
                                'edge',
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
                              'data-configs.type.edge.advance.map-property-value.fields.value-map'
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
                                      'data-configs.type.edge.advance.placeholder.input-file-value'
                                    )}
                                    value={column_value}
                                    onChange={(e: any) => {
                                      dataMapStore.editEdgeValueMappingColumnValueName(
                                        'edit',
                                        'column_value',
                                        e.value,
                                        valueMapIndex,
                                        valueIndex
                                      );

                                      dataMapStore.validateValueMapping(
                                        'edge',
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
                                          'edge',
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
                                      'data-configs.type.edge.advance.placeholder.input-file-value'
                                    )}
                                    value={mapped_value}
                                    onChange={(e: any) => {
                                      dataMapStore.editEdgeValueMappingColumnValueName(
                                        'edit',
                                        'mapped_value',
                                        e.value,
                                        valueMapIndex,
                                        valueIndex
                                      );

                                      dataMapStore.validateValueMapping(
                                        'edge',
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
                                          'edge',
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
                                        dataMapStore.removeEdgeValueMappingValue(
                                          'edit',
                                          valueMapIndex,
                                          valueIndex
                                        );

                                        dataMapStore.removeValidateValueMappingOption(
                                          'edge',
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
                                    'edge',
                                    valueMapIndex
                                  )
                                })}
                              >
                                <span
                                  onClick={() => {
                                    if (
                                      !dataMapStore.allowAddPropertyValueMapping(
                                        'edge',
                                        valueMapIndex
                                      )
                                    ) {
                                      return;
                                    }

                                    dataMapStore.addEdgeValueMappingValue(
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
                                    'data-configs.type.edge.advance.map-property-value.fields.add-value-map'
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
                ? !isEmpty(dataMapStore.editedEdgeMap?.value_mapping)
                : !isEmpty(dataMapStore.newEdgeType.value_mapping)) && (
                <div
                  className={addPropertyMapClassName}
                  style={{ marginTop: 16 }}
                  onClick={() => {
                    if (!dataMapStore.allowAddPropertyMapping('edge')) {
                      return;
                    }

                    isEdit
                      ? dataMapStore.addEdgeValueMapping('edit')
                      : dataMapStore.addEdgeValueMapping('new');

                    dataMapStore.addValidateValueMappingOption(
                      'value_mappings'
                    );
                  }}
                >
                  {t(
                    'data-configs.type.edge.advance.map-property-value.add-value'
                  )}
                </div>
              )}
          </>
        )}

        {!isCheck && (
          <TypeConfigManipulations
            type="edge"
            status={isEdit ? 'edit' : 'add'}
            disableSave={
              isStrategyAutomatic ||
              isEmpty(edgeMap.label) ||
              edgeMap.source_fields.includes('') ||
              edgeMap.target_fields.includes('') ||
              edgeMap.field_mapping.some(({ mapped_name }) =>
                isEmpty(mapped_name)
              ) ||
              !dataMapStore.allowAddPropertyMapping('edge') ||
              !dataMapStore.isValidateSave
            }
            onCreate={async () => {
              dataMapStore.switchAddNewTypeConfig(false);
              dataMapStore.switchEditTypeConfig(false);

              isEdit
                ? await dataMapStore.updateEdgeMap(
                    'upgrade',
                    dataMapStore.selectedFileId
                  )
                : await dataMapStore.updateEdgeMap(
                    'add',
                    dataMapStore.selectedFileId
                  );

              if (dataMapStore.requestStatus.updateEdgeMap === 'failed') {
                Message.error({
                  content: dataMapStore.errorInfo.updateEdgeMap.message,
                  size: 'medium',
                  showCloseIcon: false
                });
              }

              onCancelCreateEdge();
              dataMapStore.resetNewMap('edge');
            }}
            onCancel={() => {
              dataMapStore.switchAddNewTypeConfig(false);
              dataMapStore.switchEditTypeConfig(false);

              if (!isEdit) {
                dataMapStore.resetNewMap('edge');
              } else {
                dataMapStore.resetEditMapping('edge');
              }

              onCancelCreateEdge();
              dataMapStore.resetNewMap('edge');
              dataMapStore.resetValidateValueMapping('all');
            }}
          />
        )}
      </div>
    );
  }
);

export default EdgeMap;
