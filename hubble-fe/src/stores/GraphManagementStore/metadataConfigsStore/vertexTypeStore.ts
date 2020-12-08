import { observable, action, flow, computed } from 'mobx';
import axios, { AxiosResponse } from 'axios';
import { cloneDeep, isUndefined, remove, size } from 'lodash-es';

import { MetadataConfigsRootStore } from './metadataConfigsStore';
import { checkIfLocalNetworkOffline } from '../../utils';

import { baseUrl, responseData } from '../../types/common';
import {
  VertexType,
  EditVertexTypeParams,
  VertexTypeListResponse,
  PageConfig,
  CheckedReusableData,
  VertexTypeValidateFields,
  VertexTypeValidatePropertyIndexes
} from '../../types/GraphManagementStore/metadataConfigsStore';

export class VertexTypeStore {
  metadataConfigsRootStore: MetadataConfigsRootStore;

  constructor(MetadataConfigsRootStore: MetadataConfigsRootStore) {
    this.metadataConfigsRootStore = MetadataConfigsRootStore;
  }

  @observable validateLicenseOrMemories = true;
  @observable currentTabStatus = 'list';

  @observable.shallow requestStatus = {
    fetchVertexTypeList: 'pending',
    checkIfUsing: 'pending',
    addVertexType: 'pending',
    updateVertexType: 'pending',
    deleteVertexType: 'pending',
    checkConflict: 'pending',
    recheckConflict: 'pending',
    reuseVertexType: 'pending'
  };

  @observable vertexListPageConfig: PageConfig = {
    pageNumber: 1,
    pageTotal: 0,
    sort: ''
  };

  @observable errorMessage = '';

  @observable.ref colorSchemas = [
    '#5c73e6',
    '#569380',
    '#8ecc93',
    '#fe9227',
    '#fe5b5d',
    '#fd6ace',
    '#4d8dda',
    '#57c7e3',
    '#ffe081',
    '#c570ff',
    '#2b65ff',
    '#0eb880',
    '#76c100',
    '#ed7600',
    '#e65055',
    '#a64ee6',
    '#108cee',
    '#00b5d9',
    '#f2ca00',
    '#e048ae'
  ];

  @observable.ref vertexSizeSchemas = [
    { ch: '超小', en: 'TINY' },
    { ch: '小', en: 'SMALL' },
    { ch: '中', en: 'NORMAL' },
    { ch: '大', en: 'BIG' },
    { ch: '超大', en: 'HUGE' }
  ];

  @observable.shallow newVertexType: VertexType = {
    name: '',
    id_strategy: 'PRIMARY_KEY',
    properties: [],
    primary_keys: [],
    property_indexes: [],
    open_label_index: false,
    style: {
      color: '#5c73e6',
      icon: null,
      size: 'NORMAL',
      display_fields: ['~id']
    }
  };

  // should user able to create new vertex type
  @observable isCreatedReady = false;
  // should user able to create new property index
  @observable isAddNewPropertyIndexReady = true;

  // only have to check property
  @observable isEditReady = true;

  @observable.ref selectedVertexType: VertexType | null = null;
  @observable.ref selectedVertexTypeNames: string[] = [];
  @observable.ref editedSelectedVertexType: EditVertexTypeParams = {
    append_properties: [],
    append_property_indexes: [],
    remove_property_indexes: [],
    style: {
      color: '#5c73e6',
      icon: null,
      size: 'NORMAL',
      display_fields: ['~id']
    }
  };

  @observable addedPropertiesInSelectedVertextType: Set<string> = new Set();

  @observable.ref vertexTypes: VertexType[] = [];

  @observable vertexTypeUsingStatus: Record<string, boolean> | null = null;

  // reuse
  @observable reusableVertexTypes: VertexType[] = [];
  @observable checkedReusableData: CheckedReusableData | null = null;
  @observable
  editedCheckedReusableData: CheckedReusableData | null = null;

  @observable reusableVertexTypeNameChangeIndexes: Set<number> = new Set<
    number
  >();
  @observable reusablePropertyNameChangeIndexes: Set<number> = new Set<
    number
  >();
  @observable reusablePropertyIndexNameChangeIndexes: Set<number> = new Set<
    number
  >();

  @observable validateNewVertexTypeErrorMessage: Record<
    VertexTypeValidateFields,
    string | VertexTypeValidatePropertyIndexes[]
  > = {
    name: '',
    properties: '',
    primaryKeys: '',
    displayFeilds: '',
    propertyIndexes: []
  };

  @observable.shallow validateEditVertexTypeErrorMessage: Record<
    'propertyIndexes',
    VertexTypeValidatePropertyIndexes[]
  > = {
    propertyIndexes: []
  };

  @observable validateReuseErrorMessage: Record<
    'vertexType' | 'property' | 'property_index',
    string
  > = {
    vertexType: '',
    property: '',
    property_index: ''
  };

  @observable validateRenameReuseVertexErrorMessage: Record<
    'vertex' | 'property' | 'property_index',
    { name: string }
  > = {
    vertex: {
      name: ''
    },
    property: {
      name: ''
    },
    property_index: {
      name: ''
    }
  };

  @computed get reusableVertexTypeDataMap() {
    const dataMap: Record<string, Record<'key' | 'title', string>> = {};

    this.reusableVertexTypes.forEach(({ name }) => {
      dataMap[name] = {
        key: name,
        title: name
      };
    });

    return dataMap;
  }

  @computed get isReadyToReuse() {
    return (
      this.editedCheckedReusableData &&
      this.editedCheckedReusableData!.propertykey_conflicts.every(
        ({ status }) => status === 'PASSED' || status === 'EXISTED'
      ) &&
      this.editedCheckedReusableData!.vertexlabel_conflicts.every(
        ({ status }) => status === 'PASSED' || status === 'EXISTED'
      ) &&
      this.editedCheckedReusableData!.propertyindex_conflicts.every(
        ({ status }) => status === 'PASSED' || status === 'EXISTED'
      ) &&
      // no data standingby validation
      this.reusableVertexTypeNameChangeIndexes.size === 0 &&
      this.reusablePropertyNameChangeIndexes.size === 0 &&
      this.reusablePropertyIndexNameChangeIndexes.size === 0
    );
  }

  @action
  changeCurrentTabStatus(status: string) {
    this.currentTabStatus = status;
  }

  @action
  mutateNewProperty(newVertexType: VertexType) {
    this.newVertexType = newVertexType;
  }

  @action
  mutateSelectedVertexTypeNames(names: string[]) {
    this.selectedVertexTypeNames = names;
  }

  @action
  mutatePageNumber(pageNumber: number) {
    this.vertexListPageConfig.pageNumber = pageNumber;
  }

  @action
  mutatePageSort(sort: 'desc' | 'asc') {
    this.vertexListPageConfig.sort = sort;
  }

  @action
  selectVertexType(index: number | null) {
    if (index === null) {
      this.selectedVertexType = null;
      return;
    }

    this.selectedVertexType = cloneDeep(this.vertexTypes[index]);
  }

  @action
  mutateSelectedProperty(selectedProperty: VertexType) {
    this.selectedVertexType = selectedProperty;
  }

  @action
  mutateEditedSelectedVertexType(
    editedSelectedVertexType: EditVertexTypeParams
  ) {
    this.editedSelectedVertexType = editedSelectedVertexType;
  }

  @action
  resetNewVertextType() {
    this.newVertexType = {
      name: '',
      id_strategy: 'PRIMARY_KEY',
      properties: [],
      primary_keys: [],
      property_indexes: [],
      open_label_index: false,
      style: {
        color: '#5c73e6',
        icon: null,
        size: 'NORMAL',
        display_fields: ['~id']
      }
    };

    this.isCreatedReady = false;
  }

  @action
  resetAddedPropertiesInSelectedVertextType() {
    this.addedPropertiesInSelectedVertextType.clear();
  }

  @action
  resetEditedSelectedVertexType() {
    this.editedSelectedVertexType = {
      append_properties: [],
      append_property_indexes: [],
      remove_property_indexes: [],
      style: {
        color: '#5c73e6',
        icon: null,
        size: 'NORMAL',
        display_fields: ['~id']
      }
    };

    // need to clear checkbox status either
    this.resetAddedPropertiesInSelectedVertextType();
  }

  // reuse

  @action
  mutateEditedReusableData(newEditedReusableVertexTypes: CheckedReusableData) {
    this.editedCheckedReusableData = newEditedReusableVertexTypes;
  }

  @action
  mutateReusableVertexTypeChangeIndexes(index: number) {
    this.reusableVertexTypeNameChangeIndexes.add(index);
  }

  @action
  mutateReusablePropertyNameChangeIndexes(index: number) {
    this.reusablePropertyNameChangeIndexes.add(index);
  }

  @action
  mutateReusablePropertyIndexNameChangeIndexes(index: number) {
    this.reusablePropertyIndexNameChangeIndexes.add(index);
  }

  // if cancel clicked, reset to the original name
  @action
  resetEditedReusableVertexTypeName(index: number) {
    this.editedCheckedReusableData!.vertexlabel_conflicts[
      index
    ].entity.name = this.checkedReusableData!.vertexlabel_conflicts[
      index
    ].entity.name;
  }

  // if cancel clicked, reset to the original name
  @action
  resetEditedReusablePropertyName(index: number) {
    this.editedCheckedReusableData!.propertykey_conflicts[
      index
    ].entity.name = this.checkedReusableData!.propertykey_conflicts[
      index
    ].entity.name;
  }

  // if cancel clicked, reset to the original name
  @action
  resetEditedReusablePropertyIndexName(index: number) {
    this.editedCheckedReusableData!.propertyindex_conflicts[
      index
    ].entity.name = this.checkedReusableData!.propertyindex_conflicts[
      index
    ].entity.name;
  }

  @action
  clearReusableNameChangeIndexes() {
    this.reusableVertexTypeNameChangeIndexes.clear();
    this.reusablePropertyNameChangeIndexes.clear();
    this.reusablePropertyIndexNameChangeIndexes.clear();
  }

  @action
  resetReusableVertexTypes() {
    this.reusableVertexTypes = [];
  }

  @action
  validateNewVertexType(category: VertexTypeValidateFields, initial = false) {
    let isReady = true;

    // if initial is true, error message won't be assigned
    // which intends to not pop up error layer
    if (category === 'name') {
      if (!/^[\w\u4e00-\u9fa5]{1,128}$/.test(this.newVertexType.name)) {
        if (this.newVertexType.name.length === 0) {
          !initial &&
            (this.validateNewVertexTypeErrorMessage.name = '此项为必填项');
          isReady = false;
        } else {
          !initial &&
            (this.validateNewVertexTypeErrorMessage.name =
              '必须为中英文，数字和下划线');
          isReady = false;
        }
      } else {
        this.validateNewVertexTypeErrorMessage.name = '';
      }
    }

    if (category === 'properties') {
      if (
        this.newVertexType.properties.length === 0 &&
        this.newVertexType.id_strategy === 'PRIMARY_KEY'
      ) {
        !initial &&
          (this.validateNewVertexTypeErrorMessage.properties = '此项为必填项');
        isReady = false;
      }
    }

    if (category === 'primaryKeys') {
      if (
        this.newVertexType.id_strategy === 'PRIMARY_KEY' &&
        this.newVertexType.primary_keys.length === 0
      ) {
        !initial &&
          (this.validateNewVertexTypeErrorMessage.primaryKeys = '此项为必填项');
        isReady = false;
      }
    }

    if (category === 'displayFeilds') {
      if (this.newVertexType.style.display_fields.length === 0) {
        !initial &&
          (this.validateNewVertexTypeErrorMessage.displayFeilds =
            '此项为必填项');
        isReady = false;
      }
    }

    if (category === 'propertyIndexes') {
      this.isAddNewPropertyIndexReady = true;

      this.validateNewVertexTypeErrorMessage.propertyIndexes = this.newVertexType.property_indexes.map(
        ({ name, type, fields }) => {
          const validatedPropertyIndex = {
            name: '',
            type: '',
            properties: ''
          };

          if (!/^[\w\u4e00-\u9fa5]{1,128}$/.test(name)) {
            if (!initial) {
              if (name.length !== 0) {
                validatedPropertyIndex.name = '必须为中英文，数字和下划线';
              } else {
                validatedPropertyIndex.name = '此项为必填项';
              }
            }

            isReady = false;
            this.isAddNewPropertyIndexReady = false;
          } else {
            validatedPropertyIndex.name = '';
          }

          if (type.length === 0) {
            !initial && (validatedPropertyIndex.type = '此项为必填项');
            isReady = false;
            this.isAddNewPropertyIndexReady = false;
          } else {
            validatedPropertyIndex.type = '';
          }

          if (Array.isArray(fields)) {
            if (fields.length === 0) {
              !initial && (validatedPropertyIndex.properties = '此项为必填项');
              isReady = false;
              this.isAddNewPropertyIndexReady = false;
            }
          } else {
            validatedPropertyIndex.properties = '';
          }

          return validatedPropertyIndex;
        }
      );
    }

    return isReady;
  }

  @action
  validateAllNewVertexType(initial = false) {
    this.isCreatedReady =
      this.validateNewVertexType('name', initial) &&
      this.validateNewVertexType('properties', initial) &&
      this.validateNewVertexType('primaryKeys', initial) &&
      this.validateNewVertexType('propertyIndexes', initial) &&
      this.validateNewVertexType('displayFeilds', initial);
  }

  @action
  validateEditVertexType(initial = false) {
    this.isEditReady = true;

    this.validateEditVertexTypeErrorMessage.propertyIndexes = this.editedSelectedVertexType.append_property_indexes.map(
      ({ name, type, fields }) => {
        const validatedPropertyIndex = {
          name: '',
          type: '',
          properties: ''
        };
        if (!/^[\w\u4e00-\u9fa5]{1,128}$/.test(name)) {
          if (!initial) {
            if (name.length !== 0) {
              validatedPropertyIndex.name = '必须为中英文，数字和下划线';
            } else {
              validatedPropertyIndex.name = '此项为必填项';
            }
          }

          this.isEditReady = false;
        } else {
          validatedPropertyIndex.name = '';
        }

        if (type.length === 0) {
          !initial && (validatedPropertyIndex.type = '此项为必填项');
          this.isEditReady = false;
        } else {
          validatedPropertyIndex.type = '';
        }

        if (Array.isArray(fields)) {
          if (fields.length === 0) {
            !initial && (validatedPropertyIndex.properties = '此项为必填项');
            this.isEditReady = false;
          }
        } else {
          validatedPropertyIndex.properties = '';
        }

        return validatedPropertyIndex;
      }
    );
  }

  @action
  validateReuseData(
    category: 'vertexType' | 'property' | 'property_index',
    originalValue: string,
    newValue: string
  ) {
    if (!/^[\w\u4e00-\u9fa5]{1,128}$/.test(newValue)) {
      if (newValue.length === 0) {
        this.validateReuseErrorMessage[category] = '此项为必填项';
      } else {
        this.validateReuseErrorMessage[category] = '必须为中英文，数字和下划线';
      }

      return false;
    }

    // if value has changed
    if (originalValue !== newValue) {
      if (category === 'vertexType') {
        if (
          !isUndefined(
            this.checkedReusableData!.vertexlabel_conflicts.find(
              ({ entity }) => entity.name === newValue
            )
          )
        ) {
          this.validateReuseErrorMessage[category] =
            '存在同名顶点，请输入其它名称';

          return false;
        }
      }

      if (category === 'property') {
        // check if there's an existed value equals to new value
        if (
          !isUndefined(
            this.checkedReusableData!.propertykey_conflicts.find(
              ({ entity }) => entity.name === newValue
            )
          ) ||
          !isUndefined(
            this.checkedReusableData!.propertyindex_conflicts.find(
              ({ entity }) =>
                !isUndefined(
                  entity.fields.find((fieldName) => fieldName === newValue)
                )
            )
          ) ||
          !isUndefined(
            this.checkedReusableData!.vertexlabel_conflicts.find(
              ({ entity }) =>
                !isUndefined(
                  entity.properties.find(({ name }) => name === newValue)
                ) ||
                !isUndefined(
                  entity.primary_keys.find((key) => key === newValue)
                ) ||
                !isUndefined(
                  entity.property_indexes.find(
                    ({ fields }) =>
                      !isUndefined(
                        fields.find((fieldName) => fieldName === newValue)
                      )
                  )
                )
            )
          )
        ) {
          this.validateReuseErrorMessage[category] =
            '存在同名属性，请输入其它名称';

          return false;
        }
      }

      if (category === 'property_index') {
        if (
          !isUndefined(
            this.checkedReusableData!.propertyindex_conflicts.find(
              ({ entity }) => entity.name === newValue
            )
          ) ||
          !isUndefined(
            this.checkedReusableData!.vertexlabel_conflicts.find(
              ({ entity }) =>
                !isUndefined(
                  entity.property_indexes.find(({ name }) => name === newValue)
                )
            )
          )
        ) {
          this.validateReuseErrorMessage[category] =
            '存在同名属性索引，请输入其它名称';

          return false;
        }
      }
    }

    return true;
  }

  @action
  mutateReuseData(
    category: 'vertexType' | 'property' | 'property_index',
    originalValue: string,
    newValue: string
  ) {
    const editedCheckedReusableData = cloneDeep(this.editedCheckedReusableData);

    if (category === 'vertexType') {
    }

    if (category === 'property') {
      editedCheckedReusableData!.vertexlabel_conflicts.forEach(
        ({ entity }, index) => {
          const mutatePropertyIndex = entity.properties.findIndex(
            ({ name }) => name === originalValue
          );

          if (mutatePropertyIndex !== -1) {
            entity.properties[mutatePropertyIndex].name = newValue;
            // property name in current vertex label has been edited
            this.reusableVertexTypeNameChangeIndexes.add(index);
          }

          const primaryKeyIndex = entity.primary_keys.findIndex(
            (key) => key === originalValue
          );

          if (primaryKeyIndex !== -1) {
            entity.primary_keys[primaryKeyIndex] = newValue;
            this.reusableVertexTypeNameChangeIndexes.add(index);
          }

          entity.property_indexes.forEach(({ fields }) => {
            const mutatePropertyIndexIndex = fields.findIndex(
              (fieldName) => fieldName === originalValue
            );

            if (mutatePropertyIndexIndex !== -1) {
              fields[mutatePropertyIndex] = newValue;
              this.reusableVertexTypeNameChangeIndexes.add(index);
            }
          });
        }
      );

      editedCheckedReusableData!.propertyindex_conflicts.forEach(
        ({ entity }, index) => {
          const mutateIndex = entity.fields.findIndex(
            (fieldName) => fieldName === originalValue
          );

          if (mutateIndex !== -1) {
            entity.fields[mutateIndex] = newValue;
            this.reusablePropertyIndexNameChangeIndexes.add(index);
          }
        }
      );
    }

    if (category === 'property_index') {
      editedCheckedReusableData!.vertexlabel_conflicts.forEach(
        ({ entity }, index) => {
          const mutateVertexIndex = entity.property_indexes.findIndex(
            ({ name }) => name === originalValue
          );

          if (mutateVertexIndex !== -1) {
            entity.property_indexes[mutateVertexIndex].name = newValue;
            this.reusableVertexTypeNameChangeIndexes.add(index);
          }
        }
      );
    }

    this.editedCheckedReusableData = editedCheckedReusableData;
    this.checkedReusableData = cloneDeep(editedCheckedReusableData);
  }

  @action
  deleteReuseData(
    category:
      | 'vertexlabel_conflicts'
      | 'propertykey_conflicts'
      | 'propertyindex_conflicts',
    index: number
  ) {
    if (this.editedCheckedReusableData !== null) {
      const editedCheckedReusableData = cloneDeep(
        this.editedCheckedReusableData
      );

      if (category === 'vertexlabel_conflicts') {
        const deletedVertexType =
          editedCheckedReusableData.vertexlabel_conflicts[index];
        const deletedPropertyNames: string[] = [];
        const deletedPropertyIndexNames: string[] = [];

        deletedVertexType.entity.properties.forEach(({ name }) => {
          deletedPropertyNames.push(name);
        });

        deletedVertexType.entity.property_indexes.forEach(({ name }) => {
          deletedPropertyIndexNames.push(name);
        });

        editedCheckedReusableData.vertexlabel_conflicts.splice(index, 1);

        // if there's no vertex labels, return since it will move back to the previous step
        if (editedCheckedReusableData.vertexlabel_conflicts.length === 0) {
          return;
        }

        deletedPropertyIndexNames.forEach((propertyIndexName) => {
          remove(
            editedCheckedReusableData.propertyindex_conflicts,
            ({ entity }) => entity.name === propertyIndexName
          );
        });

        deletedPropertyNames
          .filter(
            (propertyName) =>
              !isUndefined(
                editedCheckedReusableData.vertexlabel_conflicts.find(
                  ({ entity }) =>
                    isUndefined(
                      entity.properties.find(
                        ({ name }) => name === propertyName
                      )
                    )
                )
              )
          )
          .forEach((propertyName) => {
            remove(
              editedCheckedReusableData.propertykey_conflicts,
              ({ entity }) => entity.name === propertyName
            );
          });
      }

      if (category === 'propertykey_conflicts') {
        const deletedName =
          editedCheckedReusableData.propertykey_conflicts[index].entity.name;
        // remove property in properties
        editedCheckedReusableData.propertykey_conflicts.splice(index, 1);

        // remove property in property index
        editedCheckedReusableData.propertyindex_conflicts.forEach(
          ({ entity }) => {
            remove(entity.fields, (name) => name === deletedName);
          }
        );

        remove(
          editedCheckedReusableData.propertyindex_conflicts,
          ({ entity }) => entity.fields.length === 0
        );

        // remove property in vertex labels
        editedCheckedReusableData.vertexlabel_conflicts.forEach(
          ({ entity }, vertexlabelIndex) => {
            const cb = (param: { name: string } | string) => {
              const name = typeof param === 'string' ? param : param.name;

              if (name === deletedName) {
                this.reusableVertexTypeNameChangeIndexes.add(vertexlabelIndex);
                return true;
              }

              return false;
            };

            remove(entity.properties, cb);
            remove(entity.primary_keys, cb);

            entity.property_indexes.forEach(({ fields }) => {
              remove(fields, cb);
            });
          }
        );
      }

      if (category === 'propertyindex_conflicts') {
        const {
          name: deletedPropertyIndexName,
          fields
        } = editedCheckedReusableData.propertyindex_conflicts[index].entity;

        editedCheckedReusableData.propertyindex_conflicts.splice(index, 1);

        // delete property index in vertex label
        editedCheckedReusableData.vertexlabel_conflicts.forEach(
          ({ entity }, index) => {
            const deletedIndex = entity.property_indexes.findIndex(
              ({ name }) => name === deletedPropertyIndexName
            );

            if (deletedIndex !== -1) {
              this.reusableVertexTypeNameChangeIndexes.add(index);
              entity.property_indexes.splice(deletedIndex, 1);
            }
          }
        );
      }

      this.mutateEditedReusableData(editedCheckedReusableData);
    }
  }

  @action
  resetValidateNewVertexTypeMessage(
    category?: VertexTypeValidateFields,
    propertIndexIndex?: number,
    propertIndexProperty?: keyof VertexTypeValidatePropertyIndexes
  ) {
    if (isUndefined(category)) {
      this.validateNewVertexTypeErrorMessage = {
        name: '',
        properties: '',
        primaryKeys: '',
        displayFeilds: '',
        propertyIndexes: []
      };

      return;
    }

    if (category === 'propertyIndexes') {
      (this.validateNewVertexTypeErrorMessage
        .propertyIndexes as VertexTypeValidatePropertyIndexes[])[
        propertIndexIndex as number
      ][propertIndexProperty as keyof VertexTypeValidatePropertyIndexes] = '';

      return;
    }

    this.validateNewVertexTypeErrorMessage[category] = '';
  }

  @action
  resetValidateReuseErrorMessage(
    category?: 'vertexType' | 'property' | 'property_index'
  ) {
    if (isUndefined(category)) {
      this.validateReuseErrorMessage = {
        property: '',
        property_index: '',
        vertexType: ''
      };

      return;
    }

    this.validateReuseErrorMessage[category] = '';
  }

  @action
  dispose() {
    this.currentTabStatus = 'list';
    this.requestStatus = {
      fetchVertexTypeList: 'pending',
      checkIfUsing: 'pending',
      addVertexType: 'pending',
      updateVertexType: 'pending',
      deleteVertexType: 'pending',
      checkConflict: 'pending',
      recheckConflict: 'pending',
      reuseVertexType: 'pending'
    };
    this.vertexListPageConfig = {
      pageNumber: 1,
      pageTotal: 0,
      sort: ''
    };
    this.errorMessage = '';

    this.isCreatedReady = false;
    this.isAddNewPropertyIndexReady = true;
    this.isEditReady = true;

    this.resetNewVertextType();
    this.selectedVertexType = null;
    this.selectedVertexTypeNames = [];
    this.resetEditedSelectedVertexType();
    this.vertexTypes = [];
    this.vertexTypeUsingStatus = null;
    // reuse
    this.reusableVertexTypes = [];
    this.checkedReusableData = null;
    this.editedCheckedReusableData = null;
    this.resetValidateNewVertexTypeMessage();
    this.resetValidateReuseErrorMessage();
  }

  fetchVertexTypeList = flow(function* fetchVertexTypeList(
    this: VertexTypeStore,
    options?: { fetchAll?: boolean; reuseId?: number }
  ) {
    this.requestStatus.fetchVertexTypeList = 'pending';

    const conn_id =
      options && typeof options.reuseId === 'number'
        ? options.reuseId
        : this.metadataConfigsRootStore.currentId;

    try {
      const result: AxiosResponse<responseData<
        VertexTypeListResponse
      >> = yield axios
        .get(`${baseUrl}/${conn_id}/schema/vertexlabels`, {
          params: {
            page_no: this.vertexListPageConfig.pageNumber,
            page_size: !options ? 10 : -1,
            name_order:
              this.vertexListPageConfig.sort !== ''
                ? this.vertexListPageConfig.sort
                : null
          }
        })
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        if (result.data.status === 401) {
          this.validateLicenseOrMemories = false;
        }

        throw new Error(result.data.message);
      }

      if (options && typeof options.reuseId === 'number') {
        this.reusableVertexTypes = result.data.data.records;
      } else {
        this.vertexTypes = result.data.data.records;
        this.vertexListPageConfig.pageTotal = result.data.data.total;
      }

      if (this.currentTabStatus !== 'reuse') {
        result.data.data.records.length === 0
          ? (this.currentTabStatus = 'empty')
          : (this.currentTabStatus = 'list');
      }

      this.requestStatus.fetchVertexTypeList = 'success';
    } catch (error) {
      this.requestStatus.fetchVertexTypeList = 'failed';
      this.errorMessage = error.message;
    }
  });

  checkIfUsing = flow(function* checkIfUsing(
    this: VertexTypeStore,
    selectedPropertyNames: string[]
  ) {
    this.requestStatus.checkIfUsing = 'pending';

    try {
      const result: AxiosResponse<responseData<
        Record<string, boolean>
      >> = yield axios
        .post(
          `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/vertexlabels/check_using`,
          {
            names: selectedPropertyNames
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      this.vertexTypeUsingStatus = result.data.data;
      this.requestStatus.checkIfUsing = 'success';
    } catch (error) {
      this.requestStatus.checkIfUsing = 'failed';
      this.errorMessage = error.message;
    }
  });

  addVertexType = flow(function* addVertexType(this: VertexTypeStore) {
    this.requestStatus.addVertexType = 'pending';

    try {
      const result: AxiosResponse<responseData<null>> = yield axios
        .post(
          `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/vertexlabels`,
          {
            name: this.newVertexType.name,
            id_strategy: this.newVertexType.id_strategy,
            properties: this.newVertexType.properties,
            primary_keys: this.newVertexType.primary_keys,
            property_indexes: this.newVertexType.property_indexes,
            open_label_index: this.newVertexType.open_label_index,
            style: this.newVertexType.style
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      this.requestStatus.addVertexType = 'success';
    } catch (error) {
      this.requestStatus.addVertexType = 'failed';
      this.errorMessage = error.message;
    }
  });

  updateVertexType = flow(function* updateVertexType(this: VertexTypeStore) {
    this.requestStatus.updateVertexType = 'pending';

    try {
      const result: AxiosResponse<responseData<null>> = yield axios
        .put(
          `${baseUrl}/${
            this.metadataConfigsRootStore.currentId
          }/schema/vertexlabels/${this.selectedVertexType!.name}`,
          {
            append_properties: this.editedSelectedVertexType.append_properties,
            append_property_indexes: this.editedSelectedVertexType
              .append_property_indexes,
            remove_property_indexes: this.editedSelectedVertexType
              .remove_property_indexes,
            style: this.editedSelectedVertexType.style
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      this.requestStatus.updateVertexType = 'success';
    } catch (error) {
      this.requestStatus.updateVertexType = 'failed';
      this.errorMessage = error.message;
    }
  });

  deleteVertexType = flow(function* deleteVertexType(
    this: VertexTypeStore,
    selectedVertexTypeNames: string[]
  ) {
    this.requestStatus.deleteVertexType = 'pending';

    const combinedParams = selectedVertexTypeNames
      .map((name) => 'names=' + name)
      .join('&');

    try {
      const result: AxiosResponse<responseData<null>> = yield axios
        .delete(
          `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/vertexlabels?` +
            combinedParams +
            `&skip_using=${String(size(selectedVertexTypeNames) !== 1)}`
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      if (
        selectedVertexTypeNames.length === this.vertexTypes.length &&
        this.vertexListPageConfig.pageNumber ===
          Math.ceil(this.vertexListPageConfig.pageTotal / 10) &&
        this.vertexListPageConfig.pageNumber > 1
      ) {
        this.vertexListPageConfig.pageNumber =
          this.vertexListPageConfig.pageNumber - 1;
      }

      this.requestStatus.deleteVertexType = 'success';
    } catch (error) {
      this.requestStatus.deleteVertexType = 'failed';
      this.errorMessage = error.message;
    }
  });

  checkConflict = flow(function* checkConflict(
    this: VertexTypeStore,
    reuseId: string,
    selectedVertexTypes: string[]
  ) {
    this.requestStatus.checkConflict = 'pending';

    try {
      const result: AxiosResponse<responseData<null>> = yield axios
        .post(
          `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/vertexlabels/check_conflict`,
          {
            vertexlabels: selectedVertexTypes.map((selectedVertexType) =>
              this.reusableVertexTypes.find(
                ({ name }) => name === selectedVertexType
              )
            )
          },
          {
            params: {
              reused_conn_id: this.metadataConfigsRootStore.idList.find(
                ({ name }) => name === reuseId
              )!.id
            }
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      this.checkedReusableData = result.data.data;
      this.editedCheckedReusableData = cloneDeep(result.data.data);
      this.requestStatus.checkConflict = 'success';
    } catch (error) {
      this.requestStatus.checkConflict = 'failed';
      this.errorMessage = error.message;
    }
  });

  recheckConflict = flow(function* recheckConflict(this: VertexTypeStore) {
    this.requestStatus.recheckConflict = 'pending';

    try {
      const result: AxiosResponse<responseData<
        CheckedReusableData
      >> = yield axios
        .post(
          `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/vertexlabels/recheck_conflict`,
          {
            propertykeys: this.editedCheckedReusableData!.propertykey_conflicts.map(
              ({ entity }) => ({
                ...entity
              })
            ),
            propertyindexes: this.editedCheckedReusableData!.propertyindex_conflicts.map(
              ({ entity }) => ({
                ...entity
              })
            ),
            vertexlabels: this.editedCheckedReusableData!.vertexlabel_conflicts.map(
              ({ entity }) => ({
                ...entity
              })
            )
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      this.checkedReusableData = result.data.data;
      this.editedCheckedReusableData = cloneDeep(result.data.data);
      this.requestStatus.recheckConflict = 'success';
    } catch (error) {
      this.requestStatus.recheckConflict = 'failed';
      this.errorMessage = error.message;
    }
  });

  reuseVertexType = flow(function* reuseVertexType(this: VertexTypeStore) {
    this.requestStatus.reuseVertexType = 'pending';

    try {
      const result: AxiosResponse<responseData<null>> = yield axios
        .post(
          `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/vertexlabels/reuse`,
          this.editedCheckedReusableData
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      this.requestStatus.reuseVertexType = 'success';
    } catch (error) {
      this.requestStatus.reuseVertexType = 'failed';
      this.errorMessage = error.message;
    }
  });
}
