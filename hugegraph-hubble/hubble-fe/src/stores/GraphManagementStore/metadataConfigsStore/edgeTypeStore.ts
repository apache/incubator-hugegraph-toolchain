import { observable, action, flow, computed } from 'mobx';
import axios, { AxiosResponse } from 'axios';
import { cloneDeep, isUndefined, remove } from 'lodash-es';

import { MetadataConfigsRootStore } from './metadataConfigsStore';
import { checkIfLocalNetworkOffline } from '../../utils';

import { baseUrl, responseData } from '../../types/common';
import {
  EdgeType,
  EditEdgeTypeParams,
  EdgeTypeListResponse,
  PageConfig,
  CheckedReusableData,
  EdgeTypeValidateFields,
  EdgeTypeValidatePropertyIndexes
} from '../../types/GraphManagementStore/metadataConfigsStore';

import SelectedSolidArrowIcon from '../../../assets/imgs/ic_arrow_selected.svg';
import NoSelectedSolidArrowIcon from '../../../assets/imgs/ic_arrow.svg';
import SelectedSolidStraightIcon from '../../../assets/imgs/ic_straight_selected.svg';
import NoSelectedSolidStraightIcon from '../../../assets/imgs/ic_straight.svg';
import i18next from '../../../i18n';
export class EdgeTypeStore {
  metadataConfigsRootStore: MetadataConfigsRootStore;

  constructor(MetadataConfigsRootStore: MetadataConfigsRootStore) {
    this.metadataConfigsRootStore = MetadataConfigsRootStore;
  }

  @observable validateLicenseOrMemories = true;
  @observable currentTabStatus = 'list';

  @observable.shallow requestStatus = {
    fetchEdgeTypeList: 'pending',
    addEdgeType: 'pending',
    updateEdgeType: 'pending',
    deleteEdgeType: 'pending',
    checkConflict: 'pending',
    recheckConflict: 'pending',
    reuseEdgeType: 'pending'
  };

  @observable errorMessage = '';

  // should user able to create new vertex type
  @observable isCreatedReady = false;
  // should user able to create new property index
  @observable isAddNewPropertyIndexReady = true;

  // only have to check property
  @observable isEditReady = true;

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

  @observable.ref edgeShapeSchemas = [
    {
      blackicon: NoSelectedSolidArrowIcon,
      blueicon: SelectedSolidArrowIcon,
      flag: true,
      shape: 'solid'
    },
    {
      blackicon: NoSelectedSolidStraightIcon,
      blueicon: SelectedSolidStraightIcon,
      flag: false,
      shape: 'solid'
    }
  ];

  @observable.ref thicknessSchemas = [
    { ch: '粗', en: 'THICK' },
    { ch: '中', en: 'NORMAL' },
    { ch: '细', en: 'FINE' }
  ];

  @observable edgeTypeListPageConfig: PageConfig = {
    pageNumber: 1,
    pageTotal: 0,
    sort: ''
  };

  @observable.shallow newEdgeType: EdgeType = {
    name: '',
    source_label: '',
    target_label: '',
    link_multi_times: false,
    properties: [],
    sort_keys: [],
    property_indexes: [],
    open_label_index: false,
    style: {
      color: '#5c73e6',
      icon: null,
      with_arrow: true,
      thickness: 'NORMAL',
      display_fields: ['~id']
    }
  };

  @observable.ref edgeTypes: EdgeType[] = [];

  @observable.ref selectedEdgeType: EdgeType | null = null;
  @observable.ref selectedEdgeTypeNames: string[] = [];

  @observable.ref editedSelectedEdgeType: EditEdgeTypeParams = {
    append_properties: [],
    append_property_indexes: [],
    remove_property_indexes: [],
    style: {
      color: null,
      icon: null,
      with_arrow: null,
      thickness: 'NORMAL',
      display_fields: []
    }
  };

  @observable addedPropertiesInSelectedEdgeType: Set<string> = new Set();

  // reuse
  @observable reusableEdgeTypes: EdgeType[] = [];
  @observable checkedReusableData: CheckedReusableData | null = null;
  @observable
  editedCheckedReusableData: CheckedReusableData | null = null;

  @observable reusableEdgeTypeNameChangeIndexes: Set<number> = new Set<
    number
  >();
  @observable reusableVertexTypeNameChangeIndexes: Set<number> = new Set<
    number
  >();
  @observable reusablePropertyNameChangeIndexes: Set<number> = new Set<
    number
  >();
  @observable reusablePropertyIndexNameChangeIndexes: Set<number> = new Set<
    number
  >();

  @observable validateNewEdgeTypeErrorMessage: Record<
    EdgeTypeValidateFields,
    string | EdgeTypeValidatePropertyIndexes[]
  > = {
    name: '',
    sourceLabel: '',
    targetLabel: '',
    properties: '',
    sortKeys: '',
    propertyIndexes: [],
    displayFeilds: []
  };

  @observable.shallow validateEditEdgeTypeErrorMessage: Record<
    'propertyIndexes',
    EdgeTypeValidatePropertyIndexes[]
  > = {
    propertyIndexes: []
  };

  @observable validateReuseErrorMessage: Record<
    'edgeType' | 'vertexType' | 'property' | 'property_index',
    string
  > = {
    edgeType: '',
    vertexType: '',
    property: '',
    property_index: ''
  };

  @computed get reusableEdgeTypeDataMap() {
    const dataMap: Record<string, Record<'key' | 'title', string>> = {};

    this.reusableEdgeTypes.forEach(({ name }) => {
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
      this.editedCheckedReusableData!.edgelabel_conflicts.every(
        ({ status }) => status === 'PASSED' || status === 'EXISTED'
      ) &&
      // no data standingby validation
      this.reusableEdgeTypeNameChangeIndexes.size === 0 &&
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
  mutateNewEdgeType(newEdgeType: EdgeType) {
    this.newEdgeType = newEdgeType;
  }

  @action
  mutateSelectedEdgeTypeNames(names: string[]) {
    this.selectedEdgeTypeNames = names;
  }

  @action
  mutatePageNumber(pageNumber: number) {
    this.edgeTypeListPageConfig.pageNumber = pageNumber;
  }

  @action
  mutatePageSort(sort: 'desc' | 'asc') {
    this.edgeTypeListPageConfig.sort = sort;
  }

  @action
  selectEdgeType(index: number | null) {
    if (index === null) {
      this.selectedEdgeType = null;
      return;
    }

    this.selectedEdgeType = cloneDeep(this.edgeTypes[index]);
  }

  @action
  mutateSelectedProperty(selectedProperty: EdgeType) {
    this.selectedEdgeType = selectedProperty;
  }

  @action
  mutateEditedSelectedEdgeType(editedSelectedEdgeType: EditEdgeTypeParams) {
    this.editedSelectedEdgeType = editedSelectedEdgeType;
  }

  @action
  resetNewEdgeType() {
    this.newEdgeType = {
      name: '',
      source_label: '',
      target_label: '',
      link_multi_times: false,
      properties: [],
      sort_keys: [],
      property_indexes: [],
      open_label_index: false,
      style: {
        color: '#5c73e6',
        icon: null,
        with_arrow: true,
        thickness: 'NORMAL',
        display_fields: ['~id']
      }
    };

    this.isCreatedReady = false;
  }

  @action
  resetAddedPropertiesInSelectedEdgeType() {
    this.addedPropertiesInSelectedEdgeType.clear();
  }

  @action
  resetEditedSelectedEdgeType() {
    this.editedSelectedEdgeType = {
      append_properties: [],
      append_property_indexes: [],
      remove_property_indexes: [],
      style: {
        color: null,
        icon: null,
        with_arrow: null,
        thickness: 'NORMAL',
        display_fields: []
      }
    };

    this.resetAddedPropertiesInSelectedEdgeType();
  }

  // reuse

  @action
  mutateEditedReusableData(newEditedReusableVertexTypes: CheckedReusableData) {
    this.editedCheckedReusableData = newEditedReusableVertexTypes;
  }

  @action
  mutateReusableEdgeTypeChangeIndexes(index: number) {
    this.reusableEdgeTypeNameChangeIndexes.add(index);
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
  resetEditedReusableEdgeTypeName(index: number) {
    this.editedCheckedReusableData!.edgelabel_conflicts[
      index
    ].entity.name = this.checkedReusableData!.edgelabel_conflicts[
      index
    ].entity.name;
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
    this.reusableEdgeTypeNameChangeIndexes.clear();
    this.reusableVertexTypeNameChangeIndexes.clear();
    this.reusablePropertyNameChangeIndexes.clear();
    this.reusablePropertyIndexNameChangeIndexes.clear();
  }

  @action
  resetReusableEdgeTypes() {
    this.reusableEdgeTypes = [];
  }

  @action
  validateNewEdgeType(category: EdgeTypeValidateFields, initial = false) {
    let isReady = true;

    if (category === 'name') {
      if (!/^[\w\u4e00-\u9fa5]{1,128}$/.test(this.newEdgeType.name)) {
        if (this.newEdgeType.name.length === 0) {
          !initial &&
            (this.validateNewEdgeTypeErrorMessage.name = i18next.t(
              'addition.store.item-is-required'
            ));
          isReady = false;
        } else {
          !initial &&
            (this.validateNewEdgeTypeErrorMessage.name = i18next.t(
              'addition.store.rule4'
            ));
          isReady = false;
        }
      } else {
        this.validateNewEdgeTypeErrorMessage.name = '';
      }
    }

    if (category === 'sourceLabel') {
      if (this.newEdgeType.source_label === '') {
        !initial &&
          (this.validateNewEdgeTypeErrorMessage.properties = i18next.t(
            'addition.store.item-is-required'
          ));
        isReady = false;
      }
    }

    if (category === 'targetLabel') {
      if (this.newEdgeType.target_label === '') {
        !initial &&
          (this.validateNewEdgeTypeErrorMessage.properties = i18next.t(
            'addition.store.item-is-required'
          ));
        isReady = false;
      }
    }

    if (category === 'properties') {
      if (
        this.newEdgeType.properties.length === 0 &&
        this.newEdgeType.link_multi_times
      ) {
        !initial &&
          (this.validateNewEdgeTypeErrorMessage.properties = i18next.t(
            'addition.store.item-is-required'
          ));
        isReady = false;
      }
    }

    if (category === 'sortKeys') {
      if (
        this.newEdgeType.link_multi_times &&
        this.newEdgeType.sort_keys.length === 0
      ) {
        !initial &&
          (this.validateNewEdgeTypeErrorMessage.sortKeys = i18next.t(
            'addition.store.item-is-required'
          ));
        isReady = false;
      }
    }

    if (category === 'displayFeilds') {
      if (this.newEdgeType.style.display_fields.length === 0) {
        !initial &&
          (this.validateNewEdgeTypeErrorMessage.displayFeilds = i18next.t(
            'addition.store.item-is-required'
          ));
        isReady = false;
      }
    }

    if (category === 'propertyIndexes') {
      this.isAddNewPropertyIndexReady = true;
      this.validateNewEdgeTypeErrorMessage.propertyIndexes = this.newEdgeType.property_indexes.map(
        ({ name, type, fields }) => {
          const validatedPropertyIndex = {
            name: '',
            type: '',
            properties: ''
          };

          if (!/^[\w\u4e00-\u9fa5]{1,128}$/.test(name)) {
            if (!initial) {
              if (name.length !== 0) {
                validatedPropertyIndex.name = i18next.t('addition.store.rule4');
              } else {
                validatedPropertyIndex.name = i18next.t(
                  'addition.store.item-is-required'
                );
              }
            }

            isReady = false;
            this.isAddNewPropertyIndexReady = false;
          } else {
            validatedPropertyIndex.name = '';
          }

          if (type.length === 0) {
            !initial &&
              (validatedPropertyIndex.type = i18next.t(
                'addition.store.item-is-required'
              ));
            isReady = false;
            this.isAddNewPropertyIndexReady = false;
          } else {
            validatedPropertyIndex.type = '';
          }

          if (Array.isArray(fields)) {
            if (fields.length === 0) {
              !initial &&
                (validatedPropertyIndex.properties = i18next.t(
                  'addition.store.item-is-required'
                ));
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
  validateAllNewEdgeType(initial = false) {
    this.isCreatedReady =
      this.validateNewEdgeType('name', initial) &&
      this.validateNewEdgeType('sourceLabel', initial) &&
      this.validateNewEdgeType('targetLabel', initial) &&
      this.validateNewEdgeType('properties', initial) &&
      this.validateNewEdgeType('sortKeys', initial) &&
      this.validateNewEdgeType('propertyIndexes', initial) &&
      this.validateNewEdgeType('displayFeilds', initial);
  }

  @action
  validateEditEdgeType(initial = false) {
    this.isEditReady = true;

    this.validateEditEdgeTypeErrorMessage.propertyIndexes = this.editedSelectedEdgeType.append_property_indexes.map(
      ({ name, type, fields }) => {
        const validatedPropertyIndex = {
          name: '',
          type: '',
          properties: ''
        };

        if (!/^[\w\u4e00-\u9fa5]{1,128}$/.test(name)) {
          if (!initial) {
            if (name.length !== 0) {
              validatedPropertyIndex.name = i18next.t('addition.store.rule4');
            } else {
              validatedPropertyIndex.name = i18next.t(
                'addition.store.item-is-required'
              );
            }
          }

          this.isEditReady = false;
        } else {
          validatedPropertyIndex.name = '';
        }

        if (type.length === 0) {
          !initial &&
            (validatedPropertyIndex.type = i18next.t(
              'addition.store.item-is-required'
            ));
          this.isEditReady = false;
        } else {
          validatedPropertyIndex.type = '';
        }

        if (Array.isArray(fields)) {
          if (fields.length === 0) {
            !initial &&
              (validatedPropertyIndex.properties = i18next.t(
                'addition.store.item-is-required'
              ));
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
    category: 'edgeType' | 'vertexType' | 'property' | 'property_index',
    originalValue: string,
    newValue: string
  ) {
    if (!/^[\w\u4e00-\u9fa5]{1,128}$/.test(newValue)) {
      if (newValue.length === 0) {
        this.validateReuseErrorMessage[category] = i18next.t(
          'addition.store.item-is-required'
        );
      } else {
        this.validateReuseErrorMessage[category] = i18next.t(
          'addition.store.rule4'
        );
      }

      return false;
    }

    // if value has changed
    if (originalValue !== newValue) {
      if (category === 'edgeType') {
        if (
          !isUndefined(
            this.checkedReusableData!.edgelabel_conflicts.find(
              ({ entity }) => entity.name === newValue
            )
          )
        ) {
          this.validateReuseErrorMessage[category] = i18next.t(
            'addition.store.same-edge-name-notice'
          );

          return false;
        }
      }

      if (category === 'vertexType') {
        if (
          !isUndefined(
            this.checkedReusableData!.vertexlabel_conflicts.find(
              ({ entity }) => entity.name === newValue
            )
          ) ||
          !isUndefined(
            this.checkedReusableData!.edgelabel_conflicts.find(
              ({ entity }) =>
                entity.source_label === newValue ||
                entity.target_label === newValue
            )
          )
        ) {
          this.validateReuseErrorMessage[category] = i18next.t(
            'addition.store.same-vertex-name-notice'
          );

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
          ) ||
          !isUndefined(
            this.checkedReusableData!.edgelabel_conflicts.find(
              ({ entity }) =>
                !isUndefined(
                  entity.properties.find(({ name }) => name === newValue)
                ) ||
                !isUndefined(
                  entity.sort_keys.find((key) => key === newValue)
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
          this.validateReuseErrorMessage[category] = i18next.t(
            'addition.store.same-property-name-notice'
          );

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
          ) ||
          !isUndefined(
            this.checkedReusableData!.edgelabel_conflicts.find(
              ({ entity }) =>
                !isUndefined(
                  entity.property_indexes.find(({ name }) => name === newValue)
                )
            )
          )
        ) {
          this.validateReuseErrorMessage[category] = i18next.t(
            'addition.store.same-index-name-notice'
          );

          return false;
        }
      }
    }

    return true;
  }

  @action
  mutateReuseData(
    category: 'edgeType' | 'vertexType' | 'property' | 'property_index',
    originalValue: string,
    newValue: string
  ) {
    const editedCheckedReusableData = cloneDeep(this.editedCheckedReusableData);

    if (category === 'edgeType') {
    }

    if (category === 'vertexType') {
      editedCheckedReusableData!.edgelabel_conflicts.forEach(
        ({ entity }, index) => {
          if (entity.source_label === originalValue) {
            entity.source_label = newValue;
            this.reusableEdgeTypeNameChangeIndexes.add(index);
          }

          if (entity.target_label === originalValue) {
            entity.target_label = newValue;
            this.reusableEdgeTypeNameChangeIndexes.add(index);
          }
        }
      );
    }

    if (category === 'property') {
      editedCheckedReusableData!.edgelabel_conflicts.forEach(
        ({ entity }, index) => {
          const mutatePropertyIndex = entity.properties.findIndex(
            ({ name }) => name === originalValue
          );

          if (mutatePropertyIndex !== -1) {
            entity.properties[mutatePropertyIndex].name = newValue;
            // property name in current vertex label has been edited
            this.reusableEdgeTypeNameChangeIndexes.add(index);
          }

          const sortKeyIndex = entity.sort_keys.findIndex(
            (key) => key === originalValue
          );

          if (sortKeyIndex !== -1) {
            entity.sort_keys[sortKeyIndex] = newValue;
            this.reusableEdgeTypeNameChangeIndexes.add(index);
          }

          entity.property_indexes.forEach(({ fields }) => {
            const mutatePropertyIndexIndex = fields.findIndex(
              (fieldName) => fieldName === originalValue
            );

            if (mutatePropertyIndexIndex !== -1) {
              fields[mutatePropertyIndex] = newValue;
              this.reusableEdgeTypeNameChangeIndexes.add(index);
            }
          });
        }
      );

      editedCheckedReusableData!.vertexlabel_conflicts.forEach(
        ({ entity }, index) => {
          const mutatePropertyIndex = entity.properties.findIndex(
            ({ name }) => name === originalValue
          );

          if (mutatePropertyIndex !== -1) {
            entity.properties[mutatePropertyIndex].name = newValue;
            // property name in current vertex label has been edited
            this.reusableVertexTypeNameChangeIndexes.add(index);

            // current vertex belongs to which edge
            const mutateEdgeIndex = editedCheckedReusableData!.edgelabel_conflicts.findIndex(
              (edge) =>
                edge.entity.source_label === entity.name ||
                edge.entity.target_label === entity.name
            );

            // property name in source_label or target_label has been edited
            this.reusableEdgeTypeNameChangeIndexes.add(mutateEdgeIndex);
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
      editedCheckedReusableData!.edgelabel_conflicts.forEach(
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
      | 'edgelabel_conflicts'
      | 'vertexlabel_conflicts'
      | 'propertykey_conflicts'
      | 'propertyindex_conflicts',
    index: number
  ) {
    if (this.editedCheckedReusableData !== null) {
      const editedCheckedReusableData = cloneDeep(
        this.editedCheckedReusableData
      );

      if (category === 'edgelabel_conflicts') {
        const deletedEdgeType =
          editedCheckedReusableData.edgelabel_conflicts[index];
        const deletedPropertyNames: string[] = [];
        const deletedPropertyIndexNames: string[] = [];
        const deletedSourceVertexNames = deletedEdgeType.entity.source_label;
        const deletedTargetVertexNames = deletedEdgeType.entity.target_label;

        deletedEdgeType.entity.properties.forEach(({ name }) => {
          deletedPropertyNames.push(name);
        });

        deletedEdgeType.entity.property_indexes.forEach(({ name }) => {
          deletedPropertyIndexNames.push(name);
        });

        editedCheckedReusableData.edgelabel_conflicts.splice(index, 1);

        // if there's no edge labels, return since it will move back to the previous step
        if (editedCheckedReusableData.edgelabel_conflicts.length === 0) {
          return;
        }

        // remove source vertex
        if (
          isUndefined(
            editedCheckedReusableData.edgelabel_conflicts.find(
              ({ entity }) =>
                entity.source_label === deletedSourceVertexNames ||
                entity.target_label === deletedSourceVertexNames
            )
          )
        ) {
          remove(
            editedCheckedReusableData.vertexlabel_conflicts,
            ({ entity }) => entity.name === deletedSourceVertexNames
          );
        }

        // remove target vertex
        if (
          isUndefined(
            editedCheckedReusableData.edgelabel_conflicts.find(
              ({ entity }) =>
                entity.source_label === deletedTargetVertexNames ||
                entity.target_label === deletedTargetVertexNames
            )
          )
        ) {
          remove(
            editedCheckedReusableData.vertexlabel_conflicts,
            ({ entity }) => entity.name === deletedTargetVertexNames
          );
        }

        deletedPropertyIndexNames.forEach((propertyIndexName) => {
          editedCheckedReusableData.vertexlabel_conflicts.forEach(
            ({ entity }) => {
              remove(
                entity.property_indexes,
                ({ name }) => name === propertyIndexName
              );
            }
          );

          editedCheckedReusableData.edgelabel_conflicts.forEach(
            ({ entity }) => {
              remove(
                entity.property_indexes,
                ({ name }) => name === propertyIndexName
              );
            }
          );

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
              ) &&
              !isUndefined(
                editedCheckedReusableData.edgelabel_conflicts.find(
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

        // remove property in edge labels
        editedCheckedReusableData.edgelabel_conflicts.forEach(
          ({ entity }, edgelabelIndex) => {
            const cb = (param: { name: string } | string) => {
              const name = typeof param === 'string' ? param : param.name;

              if (name === deletedName) {
                this.reusableEdgeTypeNameChangeIndexes.add(edgelabelIndex);
                return true;
              }

              return false;
            };

            remove(entity.properties, cb);
            remove(entity.sort_keys, cb);

            entity.property_indexes.forEach(({ fields }) => {
              remove(fields, cb);
            });
          }
        );
      }

      if (category === 'propertyindex_conflicts') {
        const {
          name: deletedPropertyIndexName
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

        editedCheckedReusableData.edgelabel_conflicts.forEach(({ entity }) => {
          const deletedIndex = entity.property_indexes.findIndex(
            ({ name }) => name === deletedPropertyIndexName
          );

          if (deletedIndex !== -1) {
            this.reusableEdgeTypeNameChangeIndexes.add(index);
            entity.property_indexes.splice(deletedIndex, 1);
          }
        });
      }

      this.mutateEditedReusableData(editedCheckedReusableData);
    }
  }

  @action
  resetValidateNewEdgeTypeMessage(
    category?: EdgeTypeValidateFields,
    propertIndexIndex?: number,
    propertIndexProperty?: keyof EdgeTypeValidatePropertyIndexes
  ) {
    if (isUndefined(category)) {
      this.validateNewEdgeTypeErrorMessage = {
        name: '',
        sourceLabel: '',
        targetLabel: '',
        properties: '',
        sortKeys: '',
        propertyIndexes: [],
        displayFeilds: []
      };

      return;
    }

    if (category === 'propertyIndexes') {
      (this.validateNewEdgeTypeErrorMessage
        .propertyIndexes as EdgeTypeValidatePropertyIndexes[])[
        propertIndexIndex as number
      ][propertIndexProperty as keyof EdgeTypeValidatePropertyIndexes] = '';

      return;
    }

    this.validateNewEdgeTypeErrorMessage[category] = '';
  }

  @action
  resetValidateReuseErrorMessage(
    category?: 'edgeType' | 'vertexType' | 'property' | 'property_index'
  ) {
    if (isUndefined(category)) {
      this.validateReuseErrorMessage = {
        edgeType: '',
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
      fetchEdgeTypeList: 'pending',
      addEdgeType: 'pending',
      updateEdgeType: 'pending',
      deleteEdgeType: 'pending',
      checkConflict: 'pending',
      recheckConflict: 'pending',
      reuseEdgeType: 'pending'
    };
    this.errorMessage = '';

    this.isCreatedReady = false;
    this.isAddNewPropertyIndexReady = true;
    this.isEditReady = true;

    this.edgeTypeListPageConfig = {
      pageNumber: 1,
      pageTotal: 0,
      sort: ''
    };
    this.resetNewEdgeType();
    this.edgeTypes = [];
    this.selectedEdgeType = null;
    this.selectedEdgeTypeNames = [];
    this.editedSelectedEdgeType = {
      append_properties: [],
      append_property_indexes: [],
      remove_property_indexes: [],
      style: {
        color: null,
        icon: null,
        with_arrow: null,
        thickness: 'NORMAL',
        display_fields: []
      }
    };
    this.resetValidateNewEdgeTypeMessage();
    this.resetReusableEdgeTypes();
    this.validateReuseErrorMessage = {
      edgeType: '',
      vertexType: '',
      property: '',
      property_index: ''
    };
  }

  fetchEdgeTypeList = flow(function* fetchEdgeTypeList(
    this: EdgeTypeStore,
    options?: { fetchAll?: boolean; reuseId?: number }
  ) {
    this.requestStatus.fetchEdgeTypeList = 'pending';

    const conn_id =
      options && typeof options.reuseId === 'number'
        ? options.reuseId
        : this.metadataConfigsRootStore.currentId;

    try {
      const result: AxiosResponse<responseData<
        EdgeTypeListResponse
      >> = yield axios
        .get(`${baseUrl}/${conn_id}/schema/edgelabels`, {
          params: {
            page_no: this.edgeTypeListPageConfig.pageNumber,
            page_size: !options ? 10 : -1,
            name_order:
              this.edgeTypeListPageConfig.sort !== ''
                ? this.edgeTypeListPageConfig.sort
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
        this.reusableEdgeTypes = result.data.data.records;
      } else {
        this.edgeTypes = result.data.data.records;
        this.edgeTypeListPageConfig.pageTotal = result.data.data.total;
      }

      if (this.currentTabStatus !== 'reuse') {
        result.data.data.records.length === 0
          ? (this.currentTabStatus = 'empty')
          : (this.currentTabStatus = 'list');
      }

      this.requestStatus.fetchEdgeTypeList = 'success';
    } catch (error) {
      this.requestStatus.fetchEdgeTypeList = 'failed';
      this.errorMessage = error.message;
    }
  });

  addEdgeType = flow(function* addEdgeType(this: EdgeTypeStore) {
    this.requestStatus.addEdgeType = 'pending';

    try {
      const result: AxiosResponse<responseData<null>> = yield axios
        .post(
          `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/edgelabels`,
          {
            name: this.newEdgeType.name,
            source_label: this.newEdgeType.source_label,
            target_label: this.newEdgeType.target_label,
            link_multi_times: this.newEdgeType.link_multi_times,
            properties: this.newEdgeType.properties,
            sort_keys: this.newEdgeType.sort_keys,
            property_indexes: this.newEdgeType.property_indexes,
            open_label_index: this.newEdgeType.open_label_index,
            style: this.newEdgeType.style
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      this.requestStatus.addEdgeType = 'success';
    } catch (error) {
      this.requestStatus.addEdgeType = 'failed';
      this.errorMessage = error.message;
    }
  });

  updateEdgeType = flow(function* updateEdgeType(this: EdgeTypeStore) {
    this.requestStatus.updateEdgeType = 'pending';

    try {
      const result: AxiosResponse<responseData<null>> = yield axios
        .put(
          `${baseUrl}/${
            this.metadataConfigsRootStore.currentId
          }/schema/edgelabels/${this.selectedEdgeType!.name}`,
          {
            append_properties: this.editedSelectedEdgeType.append_properties,
            append_property_indexes: this.editedSelectedEdgeType
              .append_property_indexes,
            remove_property_indexes: this.editedSelectedEdgeType
              .remove_property_indexes,
            style: this.editedSelectedEdgeType.style
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      this.requestStatus.updateEdgeType = 'success';
    } catch (error) {
      this.requestStatus.updateEdgeType = 'failed';
      this.errorMessage = error.message;
    }
  });

  deleteEdgeType = flow(function* deleteEdgeType(
    this: EdgeTypeStore,
    selectedEdgeTypeNames: string[]
  ) {
    this.requestStatus.deleteEdgeType = 'pending';

    const combinedParams = selectedEdgeTypeNames
      .map((name) => 'names=' + name)
      .join('&');

    try {
      const result: AxiosResponse<responseData<null>> = yield axios
        .delete(
          `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/edgelabels?` +
            combinedParams +
            `&skip_using=${String(
              Array.isArray(selectedEdgeTypeNames) &&
                selectedEdgeTypeNames.length !== 1
            )}`
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      if (
        selectedEdgeTypeNames.length === this.edgeTypes.length &&
        this.edgeTypeListPageConfig.pageNumber ===
          Math.ceil(this.edgeTypeListPageConfig.pageTotal / 10) &&
        this.edgeTypeListPageConfig.pageNumber > 1
      ) {
        this.edgeTypeListPageConfig.pageNumber =
          this.edgeTypeListPageConfig.pageNumber - 1;
      }

      this.requestStatus.deleteEdgeType = 'success';
    } catch (error) {
      this.requestStatus.deleteEdgeType = 'failed';
      this.errorMessage = error.message;
    }
  });

  checkConflict = flow(function* checkConflict(
    this: EdgeTypeStore,
    reuseId: string,
    selectedEdgeTypes: string[]
  ) {
    this.requestStatus.checkConflict = 'pending';

    try {
      const result: AxiosResponse<responseData<
        CheckedReusableData
      >> = yield axios
        .post(
          `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/edgelabels/check_conflict`,
          {
            edgelabels: selectedEdgeTypes.map((selectedEdgeType) =>
              this.reusableEdgeTypes.find(
                ({ name }) => name === selectedEdgeType
              )
            )
          },
          {
            params: {
              reused_conn_id: this.metadataConfigsRootStore.graphManagementStore.idList.find(
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

  recheckConflict = flow(function* recheckConflict(this: EdgeTypeStore) {
    this.requestStatus.recheckConflict = 'pending';

    try {
      const result: AxiosResponse<responseData<
        CheckedReusableData
      >> = yield axios
        .post(
          `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/edgelabels/recheck_conflict`,
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
            ),
            edgelabels: this.editedCheckedReusableData!.edgelabel_conflicts.map(
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

  reuseEdgeType = flow(function* reuseEdgeType(this: EdgeTypeStore) {
    this.requestStatus.reuseEdgeType = 'pending';

    try {
      const result: AxiosResponse<responseData<null>> = yield axios
        .post(
          `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/edgelabels/reuse`,
          this.editedCheckedReusableData
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      this.requestStatus.reuseEdgeType = 'success';
    } catch (error) {
      this.requestStatus.reuseEdgeType = 'failed';
      this.errorMessage = error.message;
    }
  });
}
