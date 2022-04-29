import { observable, action, flow, computed } from 'mobx';
import axios, { AxiosResponse } from 'axios';
import { isUndefined, isEmpty, cloneDeep, remove, flatMap } from 'lodash-es';

import { DataImportRootStore } from './dataImportRootStore';
import { baseUrl, responseData } from '../../types/common';
import {
  initRequestStatus,
  initErrorInfo,
  createNewEdgeType,
  createNewVertexType,
  createValidateFileInfoErrorMessage,
  createValidateAdvanceConfigErrorMessage
} from '../../factory/dataImportStore/dataMapStore';
import {
  FileMapInfo,
  FileConfig,
  FileMapResult,
  VertexMap,
  EdgeMap,
  FileValidator,
  ValueMapValidator
} from '../../types/GraphManagementStore/dataImportStore';
import { checkIfLocalNetworkOffline, validateGraphProperty } from '../../utils';

import type {
  VertexType,
  EdgeType
} from '../../types/GraphManagementStore/metadataConfigsStore';
import i18next from '../../../i18n';

export class DataMapStore {
  dataImportRootStore: DataImportRootStore;

  constructor(dataImportRootStore: DataImportRootStore) {
    this.dataImportRootStore = dataImportRootStore;
  }

  // v1.3.1: check details from import-manager
  @observable readOnly = false;
  // v1.3.1: task process comes from import-manager entrance
  @observable isIrregularProcess = false;
  // v1.5.0 no check details but in progress when LOADING
  @observable lock = false;

  @observable isExpandFileConfig = true;
  // If one of the type info card is being edited
  @observable isExpandTypeConfig = false;
  @observable isAddNewTypeConfig = false;
  @observable fileMapInfos: FileMapInfo[] = [];
  // v1.3.1: preFetched file mapping infos from import-manager
  @observable preFetchedFileMapInfos: FileMapInfo[] = [];
  @observable selectedFileId: number = NaN;
  @observable selectedFileInfo: FileMapInfo | null = null;

  @observable newVertexType: VertexMap = createNewVertexType();
  @observable editedVertexMap: VertexMap | null = null;

  @observable newEdgeType: EdgeMap = createNewEdgeType();
  @observable editedEdgeMap: EdgeMap | null = null;

  // validators
  @observable
  validateFileInfoErrorMessage: FileValidator = createValidateFileInfoErrorMessage();
  @observable
  validateAdvanceConfigErrorMessage: ValueMapValidator = createValidateAdvanceConfigErrorMessage();

  @observable requestStatus = initRequestStatus();
  @observable errorInfo = initErrorInfo();

  @computed get isValidateFileInfo() {
    return (
      !isEmpty(this.selectedFileInfo?.file_setting.delimiter) &&
      !isEmpty(this.selectedFileInfo?.file_setting.charset) &&
      !isEmpty(this.selectedFileInfo?.file_setting.date_format) &&
      !isEmpty(this.selectedFileInfo?.file_setting.skipped_line) &&
      Object.values(this.validateFileInfoErrorMessage).every(
        (errorMessage) => errorMessage === ''
      )
    );
  }

  @computed get isValidateSave() {
    return (
      this.validateAdvanceConfigErrorMessage.null_values.every((value) =>
        isEmpty(value)
      ) &&
      this.validateAdvanceConfigErrorMessage.value_mapping.every(
        ({ column_name, values }) => {
          const errorMessages = flatMap(
            values,
            ({ column_value, mapped_value }) => [column_value, mapped_value]
          );

          return (
            isEmpty(column_name) &&
            errorMessages.every((message) => isEmpty(message))
          );
        }
      )
    );
  }

  @computed get filteredColumnNamesInVertexEditSelection() {
    return this.selectedFileInfo!.file_setting.column_names.filter(
      (column_name) => !this.editedVertexMap!.id_fields.includes(column_name)
    );
  }

  @computed get filteredColumnNamesInVertexNewSelection() {
    return this.selectedFileInfo!.file_setting.column_names.filter(
      (column_name) => !this.newVertexType?.id_fields.includes(column_name)
    );
  }

  @computed get filteredColumnNamesInEdgeEditSelection() {
    return this.selectedFileInfo!.file_setting.column_names.filter(
      (column_name) =>
        !this.editedEdgeMap!.source_fields.includes(column_name) &&
        !this.editedEdgeMap!.target_fields.includes(column_name)
    );
  }

  @computed get filteredColumnNamesInEdgeNewSelection() {
    return this.selectedFileInfo!.file_setting.column_names.filter(
      (column_name) =>
        !this.newEdgeType.source_fields.includes(column_name) &&
        !this.newEdgeType.target_fields.includes(column_name)
    );
  }

  @computed get filteredFileMapInfo() {
    return this.fileMapInfos.filter(({ name }) =>
      this.dataImportRootStore.fileList.map(({ name }) => name).includes(name)
    );
  }

  @action
  switchReadOnly(isReadOnly: boolean) {
    this.readOnly = isReadOnly;
  }

  @action
  switchLock(lock: boolean) {
    this.lock = lock;
  }

  @action
  switchIrregularProcess(flag: boolean) {
    this.isIrregularProcess = flag;
  }

  @action
  switchExpand(card: 'file' | 'type', flag: boolean) {
    if (card === 'file') {
      this.isExpandFileConfig = flag;
    } else {
      this.isExpandTypeConfig = flag;
    }
  }

  @action
  switchEditTypeConfig(flag: boolean) {
    this.isExpandTypeConfig = flag;
  }

  @action
  switchAddNewTypeConfig(flag: boolean) {
    this.isAddNewTypeConfig = flag;
  }

  @action
  setSelectedFileId(id: number) {
    this.selectedFileId = id;
  }

  @action
  setSelectedFileInfo() {
    const fileInfo = this.fileMapInfos.find(
      ({ id }) => id === this.selectedFileId
    );

    if (!isUndefined(fileInfo)) {
      this.selectedFileInfo = fileInfo;
    }
  }

  @action
  setFileConfig<T extends keyof FileConfig>(key: T, value: FileConfig[T]) {
    if (this.selectedFileInfo !== null) {
      this.selectedFileInfo.file_setting[key] = value;
    }
  }

  @action
  syncEditMap(type: 'vertex' | 'edge', mapIndex: number) {
    if (type === 'vertex') {
      this.editedVertexMap = cloneDeep(
        this.selectedFileInfo!.vertex_mappings[mapIndex]
      );
    } else {
      this.editedEdgeMap = cloneDeep(
        this.selectedFileInfo!.edge_mappings[mapIndex]
      );
    }
  }

  @action
  setNewVertexConfig<T extends keyof VertexMap>(key: T, value: VertexMap[T]) {
    this.newVertexType[key] = value;
  }

  @action
  editVertexMapConfig<T extends keyof VertexMap>(
    key: T,
    value: VertexMap[T],
    vertexMapIndex: number
  ) {
    this.editedVertexMap![key] = value;
  }

  @action
  setNewEdgeConfig<T extends keyof EdgeMap>(key: T, value: EdgeMap[T]) {
    this.newEdgeType[key] = value;
  }

  @action
  editEdgeMapConfig<T extends keyof EdgeMap>(
    key: T,
    value: EdgeMap[T],
    edgeMapIndex: number
  ) {
    this.editedEdgeMap![key] = value;
  }

  @action
  setVertexFieldMappingKey(type: 'new' | 'edit', key: string, value?: string) {
    if (type === 'new') {
      this.newVertexType.field_mapping.unshift({
        column_name: key,
        mapped_name: isUndefined(value) ? '' : value
      });
    } else {
      this.editedVertexMap!.field_mapping.unshift({
        column_name: key,
        mapped_name: isUndefined(value) ? '' : value
      });
    }
  }

  @action
  setEdgeFieldMappingKey(type: 'new' | 'edit', key: string, value?: string) {
    if (type === 'new') {
      this.newEdgeType.field_mapping.unshift({
        column_name: key,
        mapped_name: isUndefined(value) ? '' : value
      });
    } else {
      this.editedEdgeMap!.field_mapping.unshift({
        column_name: key,
        mapped_name: isUndefined(value) ? '' : value
      });
    }
  }

  @action
  setVertexFieldMapping(
    type: 'new' | 'edit',
    value: string,
    vertexMapFieldIndex: number
  ) {
    if (type === 'new') {
      this.newVertexType.field_mapping[vertexMapFieldIndex].mapped_name = value;
    } else {
      this.editedVertexMap!.field_mapping[
        vertexMapFieldIndex
      ].mapped_name = value;
    }
  }

  @action
  setEdgeFieldMapping(
    type: 'new' | 'edit',
    value: string,
    edgeMapFieldIndex: number
  ) {
    if (type === 'new') {
      this.newEdgeType.field_mapping[edgeMapFieldIndex].mapped_name = value;
    } else {
      this.editedEdgeMap!.field_mapping[edgeMapFieldIndex].mapped_name = value;
    }
  }

  @action
  removeVertexFieldMapping(type: 'new' | 'edit', columnName: string) {
    if (type === 'new') {
      remove(
        this.newVertexType.field_mapping,
        ({ column_name }) => column_name === columnName
      );
    } else {
      remove(
        this.editedVertexMap!.field_mapping,
        ({ column_name }) => column_name === columnName
      );
    }
  }

  @action
  removeEdgeFieldMapping(type: 'new' | 'edit', columnName: string) {
    if (type === 'new') {
      remove(
        this.newEdgeType.field_mapping,
        ({ column_name }) => column_name === columnName
      );
    } else {
      remove(
        this.editedEdgeMap!.field_mapping,
        ({ column_name }) => column_name === columnName
      );
    }
  }

  @action
  toggleVertexSelectAllFieldMapping(
    type: 'new' | 'edit',
    selectAll: boolean,
    selectedVertex?: VertexType
  ) {
    if (selectAll) {
      if (type === 'new') {
        const existedFieldColumnNames = this.newVertexType.field_mapping.map(
          ({ column_name }) => column_name
        );

        this.selectedFileInfo!.file_setting.column_names.filter(
          (column_name) =>
            !existedFieldColumnNames.includes(column_name) &&
            !this.newVertexType.id_fields.includes(column_name)
        ).map((columnName) => {
          this.setVertexFieldMappingKey(
            type,
            columnName,
            selectedVertex?.properties.find(({ name }) => name === columnName)
              ?.name
          );
        });
      } else {
        const existedFieldColumnNames = this.editedVertexMap!.field_mapping.map(
          ({ column_name }) => column_name
        );

        this.selectedFileInfo!.file_setting.column_names.filter(
          (column_name) =>
            !existedFieldColumnNames.includes(column_name) &&
            !this.editedVertexMap!.id_fields.includes(column_name)
        ).forEach((columnName) => {
          this.setVertexFieldMappingKey(
            type,
            columnName,
            selectedVertex?.properties.find(({ name }) => name === columnName)
              ?.name
          );
        });
      }
    } else {
      if (type === 'new') {
        this.newVertexType.field_mapping = [];
      } else {
        this.editedVertexMap!.field_mapping = [];
      }
    }
  }

  @action
  toggleEdgeSelectAllFieldMapping(
    type: 'new' | 'edit',
    selectAll: boolean,
    selectedEdge?: EdgeType
  ) {
    if (selectAll) {
      if (type === 'new') {
        const existedFieldColumnNames = this.newEdgeType.field_mapping.map(
          ({ column_name }) => column_name
        );

        this.selectedFileInfo!.file_setting.column_names.filter(
          (column_name) =>
            !existedFieldColumnNames.includes(column_name) &&
            !this.newEdgeType.source_fields.includes(column_name) &&
            !this.newEdgeType.target_fields.includes(column_name)
        ).forEach((columnName) => {
          this.setEdgeFieldMappingKey(
            type,
            columnName,
            selectedEdge?.properties.find(({ name }) => name === columnName)
              ?.name
          );
        });
      } else {
        const existedFieldColumnNames = this.editedEdgeMap!.field_mapping.map(
          ({ column_name }) => column_name
        );

        this.selectedFileInfo!.file_setting.column_names.filter(
          (column_name) =>
            !existedFieldColumnNames.includes(column_name) &&
            !this.editedEdgeMap!.source_fields.includes(column_name) &&
            !this.editedEdgeMap!.target_fields.includes(column_name)
        ).map((columnName) => {
          this.setEdgeFieldMappingKey(
            type,
            columnName,
            selectedEdge?.properties.find(({ name }) => name === columnName)
              ?.name
          );
        });
      }
    } else {
      if (type === 'new') {
        this.newEdgeType.field_mapping = [];
      } else {
        this.editedEdgeMap!.field_mapping = [];
      }
    }
  }

  toggleCustomNullValue(
    type: 'new' | 'edit',
    collection: 'vertex' | 'edge',
    flag: boolean
  ) {
    if (type === 'new') {
      if (collection === 'vertex') {
        if (flag) {
          this.newVertexType.null_values.customized = [''];
        } else {
          this.newVertexType.null_values.customized = [];
        }
      } else {
        if (flag) {
          this.newEdgeType.null_values.customized = [''];
        } else {
          this.newEdgeType.null_values.customized = [];
        }
      }
    } else {
      if (collection === 'vertex') {
        if (flag) {
          this.editedVertexMap!.null_values.customized = [''];
        } else {
          this.editedVertexMap!.null_values.customized = [];
        }
      } else {
        if (flag) {
          this.editedEdgeMap!.null_values.customized = [''];
        } else {
          this.editedEdgeMap!.null_values.customized = [];
        }
      }
    }
  }

  editCheckedNullValues(
    type: 'new' | 'edit',
    collection: 'vertex' | 'edge',
    values: string[]
  ) {
    if (type === 'new') {
      if (collection === 'vertex') {
        this.newVertexType.null_values.checked = values;
      } else {
        this.newEdgeType.null_values.checked = values;
      }
    } else {
      if (collection === 'vertex') {
        this.editedVertexMap!.null_values.checked = values;
      } else {
        this.editedEdgeMap!.null_values.checked = values;
      }
    }
  }

  addCustomNullValues(type: 'new' | 'edit', collection: 'vertex' | 'edge') {
    if (type === 'new') {
      if (collection === 'vertex') {
        this.newVertexType.null_values.customized.push('');
      } else {
        this.newEdgeType.null_values.customized.push('');
      }
    } else {
      if (collection === 'vertex') {
        this.editedVertexMap!.null_values.customized.push('');
      } else {
        this.editedVertexMap!.null_values.customized.push('');
      }
    }
  }

  editCustomNullValues(
    type: 'new' | 'edit',
    collection: 'vertex' | 'edge',
    value: string,
    nullValueIndex: number
  ) {
    if (type === 'new') {
      if (collection === 'vertex') {
        this.newVertexType.null_values.customized[nullValueIndex] = value;
      } else {
        this.newEdgeType.null_values.customized[nullValueIndex] = value;
      }
    } else {
      if (collection === 'vertex') {
        this.editedVertexMap!.null_values.customized[nullValueIndex] = value;
      } else {
        this.editedEdgeMap!.null_values.customized[nullValueIndex] = value;
      }
    }
  }

  @action
  addVertexValueMapping(type: 'new' | 'edit') {
    const newValueMapping = {
      column_name: '',
      values: [
        {
          column_value: '',
          mapped_value: ''
        }
      ]
    };

    if (type === 'new') {
      this.newVertexType.value_mapping.push(newValueMapping);
    } else {
      this.editedVertexMap!.value_mapping.push(newValueMapping);
    }
  }

  @action
  addEdgeValueMapping(type: 'new' | 'edit') {
    const newValueMapping = {
      column_name: '',
      values: [
        {
          column_value: '',
          mapped_value: ''
        }
      ]
    };

    if (type === 'new') {
      this.newEdgeType.value_mapping.push(newValueMapping);
    } else {
      this.editedEdgeMap!.value_mapping.push(newValueMapping);
    }
  }

  @action
  addVertexValueMappingValue(type: 'new' | 'edit', vertexMapIndex: number) {
    const newValue = {
      column_value: '',
      mapped_value: ''
    };

    if (type === 'new') {
      this.newVertexType.value_mapping[vertexMapIndex].values.push(newValue);
    } else {
      this.editedVertexMap!.value_mapping[vertexMapIndex].values.push(newValue);
    }
  }

  @action
  addEdgeValueMappingValue(type: 'new' | 'edit', vertexMapIndex: number) {
    const newValue = {
      column_value: '',
      mapped_value: ''
    };

    if (type === 'new') {
      this.newEdgeType.value_mapping[vertexMapIndex].values.push(newValue);
    } else {
      this.editedEdgeMap!.value_mapping[vertexMapIndex].values.push(newValue);
    }
  }

  @action
  editVertexValueMappingColumnName(
    type: 'new' | 'edit',
    value: string,
    valueMapIndex: number
  ) {
    if (type === 'new') {
      this.newVertexType.value_mapping[valueMapIndex].column_name = value;
    } else {
      this.editedVertexMap!.value_mapping[valueMapIndex].column_name = value;
    }
  }

  @action
  editEdgeValueMappingColumnName(
    type: 'new' | 'edit',
    value: string,
    valueMapIndex: number
  ) {
    if (type === 'new') {
      this.newEdgeType.value_mapping[valueMapIndex].column_name = value;
    } else {
      this.editedEdgeMap!.value_mapping[valueMapIndex].column_name = value;
    }
  }

  @action
  editVertexValueMappingColumnValueName(
    type: 'new' | 'edit',
    field: 'column_value' | 'mapped_value',
    value: string,
    valueMapIndex: number,
    valueIndex: number
  ) {
    if (type === 'new') {
      this.newVertexType.value_mapping[valueMapIndex].values[valueIndex][
        field
      ] = value;
    } else {
      this.editedVertexMap!.value_mapping[valueMapIndex].values[valueIndex][
        field
      ] = value;
    }
  }

  @action
  editEdgeValueMappingColumnValueName(
    type: 'new' | 'edit',
    field: 'column_value' | 'mapped_value',
    value: string,
    valueMapIndex: number,
    valueIndex: number
  ) {
    if (type === 'new') {
      this.newEdgeType.value_mapping[valueMapIndex].values[valueIndex][
        field
      ] = value;
    } else {
      this.editedEdgeMap!.value_mapping[valueMapIndex].values[valueIndex][
        field
      ] = value;
    }
  }

  @action
  removeVertexValueMapping(type: 'new' | 'edit', valueMapIndex: number) {
    if (type === 'new') {
      remove(
        this.newVertexType.value_mapping,
        (_, index) => index === valueMapIndex
      );
    } else {
      remove(
        this.editedVertexMap!.value_mapping,
        (_, index) => index === valueMapIndex
      );
    }
  }

  @action
  removeEdgeValueMapping(type: 'new' | 'edit', valueMapIndex: number) {
    if (type === 'new') {
      remove(
        this.newEdgeType.value_mapping,
        (_, index) => index === valueMapIndex
      );
    } else {
      remove(
        this.editedEdgeMap!.value_mapping,
        (_, index) => index === valueMapIndex
      );
    }
  }

  @action
  removeVertexValueMappingValue(
    type: 'new' | 'edit',
    valueMapIndex: number,
    valueIndex: number
  ) {
    if (type === 'new') {
      remove(
        this.newVertexType.value_mapping[valueMapIndex].values,
        (_, index) => index === valueIndex
      );
    } else {
      remove(
        this.editedVertexMap!.value_mapping[valueMapIndex].values,
        (_, index) => index === valueIndex
      );
    }
  }

  @action
  removeEdgeValueMappingValue(
    type: 'new' | 'edit',
    valueMapIndex: number,
    valueIndex: number
  ) {
    if (type === 'new') {
      remove(
        this.newEdgeType.value_mapping[valueMapIndex].values,
        (_, index) => index === valueIndex
      );
    } else {
      remove(
        this.editedEdgeMap!.value_mapping[valueMapIndex].values,
        (_, index) => index === valueIndex
      );
    }
  }

  @action
  resetNewMap(newMap: 'vertex' | 'edge') {
    if (newMap === 'vertex') {
      this.newVertexType = createNewVertexType();
    } else {
      this.newEdgeType = createNewEdgeType();
    }
  }

  @action
  resetEditMapping(editMapping: 'vertex' | 'edge') {
    if (editMapping === 'vertex') {
      this.editedVertexMap = createNewVertexType();
    } else {
      this.editedEdgeMap = createNewEdgeType();
    }
  }

  @action
  validateFileInfo(
    category: 'delimiter' | 'charset' | 'date_format' | 'skipped_line'
  ) {
    if (this.selectedFileInfo?.file_setting[category] === '') {
      this.validateFileInfoErrorMessage[category] = i18next.t(
        'addition.store.cannot-be-empty1'
      );
      return;
    }

    if (category === 'date_format') {
      const date_format = this.selectedFileInfo!.file_setting.date_format;

      if (
        date_format === 'yyyy-MM-dd' ||
        date_format === 'yyyy-MM-dd HH:mm:ss' ||
        date_format === 'yyyy-MM-dd HH:mm:ss.SSS'
      ) {
        this.validateFileInfoErrorMessage.date_format = '';
        return;
      }

      if (
        !validateGraphProperty(
          'DATE',
          // hack
          this.selectedFileInfo!.file_setting.date_format.replace(/\w/g, '1')
        )
      ) {
        this.validateFileInfoErrorMessage.date_format = i18next.t(
          'addition.store.incorrect-time-format'
        );
        return;
      }
    }

    this.validateFileInfoErrorMessage[category] = '';
  }

  @action
  validateValueMapping(
    type: 'vertex' | 'edge',
    status: 'new' | 'edit',
    category: 'null_values' | 'value_mappings',
    optionIndex: number,
    valueMapOptions?: {
      field: 'column_name' | 'column_value' | 'mapped_value';
      valueIndex?: number;
    }
  ) {
    let mapping;

    if (type === 'vertex') {
      mapping = status === 'new' ? this.newVertexType : this.editedVertexMap;
    } else {
      mapping = status === 'new' ? this.newEdgeType : this.editedEdgeMap;
    }

    if (category === 'null_values') {
      const value = mapping!.null_values.customized[optionIndex];

      if (isEmpty(value)) {
        this.validateAdvanceConfigErrorMessage.null_values[
          optionIndex
        ] = i18next.t('data-configs.validator.no-empty');
      } else {
        this.validateAdvanceConfigErrorMessage.null_values[optionIndex] = '';
      }
    }

    if (category === 'value_mappings') {
      const { column_name, values } = mapping!.value_mapping[optionIndex];

      if (valueMapOptions?.field === 'column_name') {
        this.validateAdvanceConfigErrorMessage.value_mapping[
          optionIndex
        ].column_name = isEmpty(column_name)
          ? i18next.t('data-configs.validator.no-empty')
          : '';
      } else {
        const { column_value, mapped_value } = values[
          valueMapOptions?.valueIndex!
        ];

        if (valueMapOptions?.field === 'column_value') {
          this.validateAdvanceConfigErrorMessage.value_mapping[
            optionIndex
          ].values[valueMapOptions!.valueIndex!].column_value = isEmpty(
            column_value
          )
            ? i18next.t('data-configs.validator.no-empty')
            : '';
        }

        if (valueMapOptions?.field === 'mapped_value') {
          this.validateAdvanceConfigErrorMessage.value_mapping[
            optionIndex
          ].values[valueMapOptions!.valueIndex!].mapped_value = isEmpty(
            mapped_value
          )
            ? i18next.t('data-configs.validator.no-empty')
            : '';
        }
      }
    }
  }

  @action
  addValidateValueMappingOption(
    category: 'null_values' | 'value_mappings' | 'value_mappings_value',
    valueMapIndex?: number
  ) {
    if (category === 'null_values') {
      this.validateAdvanceConfigErrorMessage.null_values.push('');
    }

    if (category === 'value_mappings') {
      this.validateAdvanceConfigErrorMessage.value_mapping.push({
        column_name: '',
        values: [
          {
            column_value: '',
            mapped_value: ''
          }
        ]
      });
    }

    if (category === 'value_mappings_value') {
      this.validateAdvanceConfigErrorMessage.value_mapping[
        valueMapIndex!
      ].values.push({
        column_value: '',
        mapped_value: ''
      });
    }
  }

  @action
  syncValidateNullAndValueMappings(type: 'vertex' | 'edge') {
    const { null_values, value_mapping } =
      type === 'vertex' ? this.editedVertexMap! : this.editedEdgeMap!;

    null_values.customized.forEach(() => {
      this.validateAdvanceConfigErrorMessage.null_values.push('');
    });

    value_mapping.forEach(({ values }) => {
      this.validateAdvanceConfigErrorMessage.value_mapping.push({
        column_name: '',
        values: values.map(() => ({
          column_value: '',
          mapped_value: ''
        }))
      });
    });
  }

  @action
  removeValidateValueMappingOption(
    type: 'vertex' | 'edge',
    status: 'new' | 'edit',
    category: 'null_values' | 'value_mappings' | 'value_mappings_value',
    optionIndex: number,
    valueMappingValueIndex?: number
  ) {
    let mapping;

    if (type === 'vertex') {
      mapping = status === 'new' ? this.newVertexType : this.editedVertexMap;
    } else {
      mapping = status === 'new' ? this.newEdgeType : this.editedEdgeMap;
    }

    if (category === 'null_values') {
      remove(
        this.validateAdvanceConfigErrorMessage.null_values,
        (_, index) => index === optionIndex
      );
    }

    if (category === 'value_mappings') {
      remove(
        this.validateAdvanceConfigErrorMessage.value_mapping,
        (_, index) => index === optionIndex
      );
    }

    if (category === 'value_mappings_value') {
      remove(
        this.validateAdvanceConfigErrorMessage.value_mapping[optionIndex]
          .values,
        (_, index) => index === valueMappingValueIndex
      );
    }
  }

  @action
  allowAddPropertyMapping(type: 'vertex' | 'edge') {
    let valueMapping;

    if (type === 'vertex') {
      valueMapping = this.isExpandTypeConfig
        ? this.editedVertexMap!.value_mapping
        : this.newVertexType.value_mapping;
    } else {
      valueMapping = this.isExpandTypeConfig
        ? this.editedEdgeMap!.value_mapping
        : this.newEdgeType.value_mapping;
    }

    return valueMapping.every((_, valueMapIndex) =>
      this.allowAddPropertyValueMapping(type, valueMapIndex)
    );
  }

  @action
  allowAddPropertyValueMapping(type: 'vertex' | 'edge', valueMapIndex: number) {
    let currentValueMapping;

    if (type === 'vertex') {
      currentValueMapping = this.isExpandTypeConfig
        ? this.editedVertexMap!.value_mapping
        : this.newVertexType.value_mapping;
    } else {
      currentValueMapping = this.isExpandTypeConfig
        ? this.editedEdgeMap!.value_mapping
        : this.newEdgeType.value_mapping;
    }

    // when status is edit, init currentValueMapping is empty
    // it's weired that TS has no error in destruction below
    // where column_name or values could be undefined
    // if (isEmpty(currentValueMapping[valueMapIndex])) {
    //   return true;
    // }

    const { column_name, values } = currentValueMapping[valueMapIndex];

    return (
      !isEmpty(column_name) &&
      values.every(
        ({ column_value, mapped_value }) =>
          !isEmpty(column_value) && !isEmpty(mapped_value)
      ) &&
      this.validateAdvanceConfigErrorMessage.value_mapping[
        valueMapIndex
      ].values.every(
        ({ column_value, mapped_value }) =>
          isEmpty(column_value) && isEmpty(mapped_value)
      )
    );
  }

  @action
  resetValidateFileInfoErrorMessage() {
    this.validateFileInfoErrorMessage = createValidateFileInfoErrorMessage();
  }

  @action
  resetValidateValueMapping(category: 'null_values' | 'value_mapping' | 'all') {
    switch (category) {
      case 'null_values':
      case 'value_mapping':
        this.validateAdvanceConfigErrorMessage[category] = [];
        return;
      case 'all':
        this.validateAdvanceConfigErrorMessage = createValidateAdvanceConfigErrorMessage();
        return;
    }
  }

  @action
  resetDataMaps() {
    this.isExpandTypeConfig = false;
    this.isAddNewTypeConfig = false;

    this.newVertexType = createNewVertexType();
    this.editedVertexMap = null;

    this.newEdgeType = createNewEdgeType();
    this.editedEdgeMap = null;

    this.validateFileInfoErrorMessage = createValidateFileInfoErrorMessage();
    this.validateAdvanceConfigErrorMessage = createValidateAdvanceConfigErrorMessage();

    this.requestStatus = initRequestStatus();
    this.errorInfo = initErrorInfo();
  }

  @action
  dispose() {
    this.resetDataMaps();

    this.readOnly = false;
    this.lock = false;
    this.isIrregularProcess = false;
    this.isExpandFileConfig = true;
    this.fileMapInfos = [];
    this.preFetchedFileMapInfos = [];
    this.selectedFileId = NaN;
    this.selectedFileInfo = null;
  }

  fetchDataMaps = flow(function* fetchDataMaps(this: DataMapStore) {
    this.requestStatus.fetchDataMaps = 'pending';

    try {
      const result: AxiosResponse<responseData<FileMapResult>> = yield axios
        .get<responseData<FileMapResult>>(
          `${baseUrl}/${this.dataImportRootStore.currentId}/job-manager/${this.dataImportRootStore.currentJobId}/file-mappings`,
          {
            params: {
              page_size: -1
            }
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.fetchDataMaps.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.fileMapInfos = result.data.data.records;
      this.requestStatus.fetchDataMaps = 'success';
    } catch (error) {
      this.requestStatus.fetchDataMaps = 'failed';
      this.errorInfo.fetchDataMaps.message = error.message;
      console.error(error.message);
    }
  });

  updateFileConfig = flow(function* updateFileConfig(
    this: DataMapStore,
    fileId: number
  ) {
    this.requestStatus.updateFileConfig = 'pending';

    try {
      const result: AxiosResponse<responseData<FileMapInfo>> = yield axios
        .post<responseData<FileMapInfo>>(
          `${baseUrl}/${this.dataImportRootStore.currentId}/job-manager/${this.dataImportRootStore.currentJobId}/file-mappings/${fileId}/file-setting`,
          this.selectedFileInfo?.file_setting
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.updateFileConfig.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.selectedFileInfo!.file_setting = result.data.data.file_setting;
      this.requestStatus.updateFileConfig = 'success';
    } catch (error) {
      this.requestStatus.updateFileConfig = 'failed';
      this.errorInfo.updateFileConfig.message = error.message;
      console.error(error.message);
    }
  });

  updateVertexMap = flow(function* updateVertexMap(
    this: DataMapStore,
    method: 'add' | 'upgrade' | 'delete',
    fileId: number
  ) {
    this.requestStatus.updateVertexMap = 'pending';

    try {
      let result: AxiosResponse<responseData<FileMapInfo>>;

      switch (method) {
        case 'add': {
          const newVertexType = cloneDeep(this.newVertexType);

          if (
            newVertexType.null_values.checked.includes('NULL') &&
            !newVertexType.null_values.checked.includes('null')
          ) {
            newVertexType.null_values.checked.push('null');
          }

          result = yield axios
            .post(
              `${baseUrl}/${this.dataImportRootStore.currentId}/job-manager/${this.dataImportRootStore.currentJobId}/file-mappings/${fileId}/vertex-mappings`,
              newVertexType
            )
            .catch(checkIfLocalNetworkOffline);
          break;
        }
        case 'upgrade': {
          const editedVertexMap = cloneDeep(this.editedVertexMap);

          if (
            editedVertexMap!.null_values.checked.includes('NULL') &&
            !editedVertexMap!.null_values.checked.includes('null')
          ) {
            editedVertexMap!.null_values.checked.push('null');
          }

          result = yield axios
            .put(
              `${baseUrl}/${this.dataImportRootStore.currentId}/job-manager/${this.dataImportRootStore.currentJobId}/file-mappings/${fileId}/vertex-mappings/${this.editedVertexMap?.id}`,
              editedVertexMap
            )
            .catch(checkIfLocalNetworkOffline);
          break;
        }
        case 'delete':
          result = yield axios
            .delete(
              `${baseUrl}/${this.dataImportRootStore.currentId}/job-manager/${this.dataImportRootStore.currentJobId}/file-mappings/${fileId}/vertex-mappings/${this.editedVertexMap?.id}`
            )
            .catch(checkIfLocalNetworkOffline);
          break;
      }

      if (result.data.status !== 200) {
        this.errorInfo.updateVertexMap.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.selectedFileInfo!.vertex_mappings = result.data.data.vertex_mappings;
      this.requestStatus.updateVertexMap = 'success';
    } catch (error) {
      this.requestStatus.updateVertexMap = 'failed';
      this.errorInfo.updateVertexMap.message = error.message;
      console.error(error.message);
    }
  });

  updateEdgeMap = flow(function* updateEdgeMap(
    this: DataMapStore,
    method: 'add' | 'upgrade' | 'delete',
    fileId: number
  ) {
    this.requestStatus.updateEdgeMap = 'pending';

    try {
      let result: AxiosResponse<responseData<FileMapInfo>>;

      switch (method) {
        case 'add': {
          const newEdgeType = cloneDeep(this.newEdgeType);

          if (
            newEdgeType.null_values.checked.includes('NULL') &&
            !newEdgeType.null_values.checked.includes('null')
          ) {
            newEdgeType.null_values.checked.push('null');
          }

          result = yield axios
            .post(
              `${baseUrl}/${this.dataImportRootStore.currentId}/job-manager/${this.dataImportRootStore.currentJobId}/file-mappings/${fileId}/edge-mappings`,
              newEdgeType
            )
            .catch(checkIfLocalNetworkOffline);
          break;
        }
        case 'upgrade': {
          const editedEdgeMap = cloneDeep(this.editedEdgeMap);

          if (
            editedEdgeMap!.null_values.checked.includes('NULL') &&
            !editedEdgeMap!.null_values.checked.includes('null')
          ) {
            editedEdgeMap!.null_values.checked.push('null');
          }

          result = yield axios
            .put(
              `${baseUrl}/${this.dataImportRootStore.currentId}/job-manager/${this.dataImportRootStore.currentJobId}/file-mappings/${fileId}/edge-mappings/${this.editedEdgeMap?.id}`,
              this.editedEdgeMap
            )
            .catch(checkIfLocalNetworkOffline);
          break;
        }
        case 'delete':
          result = yield axios
            .delete(
              `${baseUrl}/${this.dataImportRootStore.currentId}/job-manager/${this.dataImportRootStore.currentJobId}/file-mappings/${fileId}/edge-mappings/${this.editedEdgeMap?.id}`
            )
            .catch(checkIfLocalNetworkOffline);
          break;
      }

      if (result.data.status !== 200) {
        this.errorInfo.updateEdgeMap.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.selectedFileInfo!.edge_mappings = result.data.data.edge_mappings;
      this.requestStatus.updateEdgeMap = 'success';
    } catch (error) {
      this.requestStatus.updateEdgeMap = 'failed';
      this.errorInfo.updateEdgeMap.message = error.message;
      console.error(error.message);
    }
  });

  deleteVertexMap = flow(function* deleteVertexMap(
    this: DataMapStore,
    mapIndex: number
  ) {
    this.requestStatus.deleteVertexMap = 'pending';

    try {
      const result: AxiosResponse<responseData<
        FileMapInfo
      >> = yield axios
        .delete<responseData<FileMapInfo>>(
          `${baseUrl}/${this.dataImportRootStore.currentId}/job-manager/${
            this.dataImportRootStore.currentJobId
          }/file-mappings/${this.selectedFileInfo!.id}/vertex-mappings/${
            this.selectedFileInfo?.vertex_mappings[mapIndex].id
          }`
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.deleteVertexMap.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.selectedFileInfo!.vertex_mappings = result.data.data.vertex_mappings;
      this.requestStatus.deleteVertexMap = 'success';
    } catch (error) {
      this.requestStatus.deleteVertexMap = 'failed';
      this.errorInfo.deleteVertexMap.message = error.message;
      console.error(error.message);
    }
  });

  deleteEdgeMap = flow(function* deleteEdgeMap(
    this: DataMapStore,
    mapIndex: number
  ) {
    this.requestStatus.deleteEdgeMap = 'pending';

    try {
      const result: AxiosResponse<responseData<
        FileMapInfo
      >> = yield axios
        .delete<responseData<FileMapInfo>>(
          `${baseUrl}/${this.dataImportRootStore.currentId}/job-manager/${
            this.dataImportRootStore.currentJobId
          }/file-mappings/${this.selectedFileInfo!.id}/edge-mappings/${
            this.selectedFileInfo?.edge_mappings[mapIndex].id
          }`
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.deleteEdgeMap.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.selectedFileInfo!.edge_mappings = result.data.data.edge_mappings;
      this.requestStatus.deleteEdgeMap = 'success';
    } catch (error) {
      this.requestStatus.deleteEdgeMap = 'failed';
      this.errorInfo.deleteEdgeMap.message = error.message;
      console.error(error.message);
    }
  });
}
