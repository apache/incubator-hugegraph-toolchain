import { observable, action, flow, computed } from 'mobx';
import axios, { AxiosResponse } from 'axios';
import { cloneDeep } from 'lodash-es';
import { v4 } from 'uuid';

import { MetadataConfigsRootStore } from './metadataConfigsStore';
import { checkIfLocalNetworkOffline } from '../../utils';

import { baseUrl, responseData } from '../../types/common';
import {
  MetadataProperty,
  MetadataPropertyListResponse,
  PageConfig,
  CheckedReusableData,
  NewMetadataProperty
} from '../../types/GraphManagementStore/metadataConfigsStore';
import i18next from '../../../i18n';

export class MetadataPropertyStore {
  metadataConfigsRootStore: MetadataConfigsRootStore;

  constructor(MetadataConfigsRootStore: MetadataConfigsRootStore) {
    this.metadataConfigsRootStore = MetadataConfigsRootStore;
  }

  @observable validateLicenseOrMemories = true;
  @observable currentTabStatus = 'list';

  @observable.shallow requestStatus = {
    fetchMetadataPropertyList: 'standby',
    checkIfUsing: 'standby',
    addMetadataProperty: 'standby',
    deleteMetadataProperty: 'standby',
    checkConflict: 'standby',
    recheckConflict: 'standby',
    reuseMetadataProperties: 'standy'
  };

  @observable errorMessage = '';

  @observable searchWords = '';

  @observable isSearched = {
    status: false,
    value: ''
  };

  @observable metadataPropertyPageConfig: PageConfig = {
    pageNumber: 1,
    pageTotal: 0,
    sort: ''
  };

  @observable isCreateNewProperty = false;
  // should user able to create new vertex type
  @observable isCreatedReady = false;

  @observable newMetadataProperty: NewMetadataProperty = {
    name: v4(),
    // real input name, to handle <Table /> key problems
    _name: '',
    data_type: 'string',
    cardinality: 'single'
  };

  @observable.ref metadataProperties: MetadataProperty[] = [];

  @observable metadataPropertyUsingStatus: Record<string, boolean> | null =
    null;

  @observable selectedMetadataProperty: MetadataProperty | null = null;
  // table selection from user
  @observable.ref selectedMetadataPropertyNames: string[] = [];

  // reuse
  @observable reuseableProperties: MetadataProperty[] = [];
  @observable
  checkedReusableProperties: CheckedReusableData | null = null;
  @observable
  editedCheckedReusableProperties: CheckedReusableData | null = null;
  @observable reusablePropertyNameChangeIndexes: Set<number> =
    new Set<number>();

  @observable validateNewPropertyErrorMessage = {
    name: ''
  };

  @observable validateRenameReusePropertyErrorMessage = {
    name: ''
  };

  @computed get reunionMetadataProperty() {
    return this.metadataProperties.length < 10
      ? [this.newMetadataProperty].concat(this.metadataProperties)
      : [this.newMetadataProperty].concat(this.metadataProperties.slice(0, 9));
  }

  @computed get reuseablePropertyDataMap() {
    const dataMap: Record<string, Record<'key' | 'title', string>> = {};

    this.reuseableProperties.forEach(({ name }) => {
      dataMap[name] = {
        key: name,
        title: name
      };
    });

    return dataMap;
  }

  @computed get isReadyToReuse() {
    return (
      this.editedCheckedReusableProperties &&
      this.editedCheckedReusableProperties!.propertykey_conflicts.every(
        ({ status }) => status === 'PASSED' || status === 'EXISTED'
      ) &&
      // no data standingby validation
      this.reusablePropertyNameChangeIndexes.size === 0
    );
  }

  @action
  changeCurrentTabStatus(status: string) {
    this.currentTabStatus = status;
  }

  @action
  mutateSearchWords(text: string) {
    this.searchWords = text;
  }

  @action
  mutatePageNumber(pageNumber: number) {
    this.metadataPropertyPageConfig.pageNumber = pageNumber;
  }

  @action
  mutatePageSort(sort: 'desc' | 'asc') {
    this.metadataPropertyPageConfig.sort = sort;
  }

  @action
  switchIsSearchedStatus(isSearched: boolean) {
    this.isSearched.status = isSearched;

    isSearched
      ? (this.isSearched.value = this.searchWords)
      : (this.isSearched.value = '');
  }

  @action
  switchIsCreateNewProperty(flag: boolean) {
    this.isCreateNewProperty = flag;
  }

  @action
  resetNewProperties() {
    this.mutateNewProperty({
      name: v4(),
      _name: '',
      data_type: 'string',
      cardinality: 'single'
    });

    this.isCreatedReady = false;
  }

  @action
  selectProperty(index: number | null) {
    if (index === null) {
      this.selectedMetadataProperty = null;
      return;
    }

    this.selectedMetadataProperty = cloneDeep(this.metadataProperties[index]);
  }

  @action
  mutateNewProperty(newMetadataProperty: NewMetadataProperty) {
    this.newMetadataProperty = newMetadataProperty;
  }

  @action
  mutateSelectedPropertyName(name: string) {
    this.selectedMetadataProperty!.name = name;
  }

  @action
  mutateSelectedMetadataProperyNames(names: string[]) {
    this.selectedMetadataPropertyNames = names;
  }

  @action
  mutateEditedReusableProperties(
    newEditedReusableProperties: CheckedReusableData
  ) {
    this.editedCheckedReusableProperties = newEditedReusableProperties;
  }

  @action
  mutateReusablePropertyNameChangeIndexes(index: number) {
    this.reusablePropertyNameChangeIndexes.add(index);
  }

  @action
  clearReusablePropertyNameChangeIndexes() {
    this.reusablePropertyNameChangeIndexes.clear();
  }

  @action
  resetReusablePropeties() {
    this.reuseableProperties = [];
  }

  @action
  validateNewProperty() {
    let isReady = true;

    if (!/^[\w\u4e00-\u9fa5]{1,128}$/.test(this.newMetadataProperty._name!)) {
      if (this.newMetadataProperty._name!.length === 0) {
        this.validateNewPropertyErrorMessage.name = i18next.t(
          'addition.store.item-is-required'
        );
        isReady = false;
      } else {
        this.validateNewPropertyErrorMessage.name = i18next.t(
          'addition.store.rule4'
        );
        isReady = false;
      }
    } else {
      this.validateNewPropertyErrorMessage.name = '';
    }

    this.isCreatedReady = isReady;
    return isReady;
  }

  @action
  validateRenameReuseProperty(index: number) {
    let isReady = true;
    const propertyName =
      this.editedCheckedReusableProperties!.propertykey_conflicts[index].entity
        .name;

    if (!/^[\w\u4e00-\u9fa5]{1,128}$/.test(propertyName)) {
      if (propertyName.length === 0) {
        this.validateRenameReusePropertyErrorMessage.name = i18next.t(
          'addition.store.item-is-required'
        );
        isReady = false;
      } else {
        this.validateRenameReusePropertyErrorMessage.name = i18next.t(
          'addition.store.rule4'
        );
        isReady = false;
      }
    } else {
      this.validateRenameReusePropertyErrorMessage.name = '';
    }

    return isReady;
  }

  @action
  resetValidateNewProperty() {
    this.validateNewPropertyErrorMessage.name = '';
  }

  @action
  resetValidateRenameReuseProperty() {
    this.validateRenameReusePropertyErrorMessage.name = '';
  }

  // if cancel clicked, reset to the original name
  @action
  resetEditedReusablePropertyName(index: number) {
    this.editedCheckedReusableProperties!.propertykey_conflicts[
      index
    ].entity.name =
      this.checkedReusableProperties!.propertykey_conflicts[index].entity.name;
  }

  @action
  dispose() {
    this.currentTabStatus = 'list';
    this.requestStatus = {
      fetchMetadataPropertyList: 'standby',
      checkIfUsing: 'standby',
      addMetadataProperty: 'standby',
      deleteMetadataProperty: 'standby',
      checkConflict: 'standby',
      recheckConflict: 'standby',
      reuseMetadataProperties: 'standy'
    };
    this.errorMessage = '';
    this.searchWords = '';
    this.isSearched = {
      status: false,
      value: ''
    };
    this.metadataPropertyPageConfig = {
      pageNumber: 1,
      pageTotal: 0,
      sort: ''
    };
    this.isCreateNewProperty = false;
    this.isCreatedReady = false;
    this.resetNewProperties();
    this.metadataProperties = [];
    this.metadataPropertyUsingStatus = null;
    this.selectedMetadataProperty = null;
    this.selectedMetadataPropertyNames = [];

    this.resetValidateNewProperty();
    this.resetValidateRenameReuseProperty();

    // reuse
    this.reuseableProperties = [];
    this.checkedReusableProperties = null;
    this.editedCheckedReusableProperties = null;
  }

  fetchMetadataPropertyList = flow(function* fetchMetadataPropertyList(
    this: MetadataPropertyStore,
    options?: { fetchAll?: boolean; reuseId?: number }
  ) {
    this.requestStatus.fetchMetadataPropertyList = 'pending';
    const conn_id =
      options && typeof options.reuseId === 'number'
        ? options.reuseId
        : this.metadataConfigsRootStore.currentId;

    try {
      const result: AxiosResponse<responseData<MetadataPropertyListResponse>> =
        yield axios
          .get(`${baseUrl}/${conn_id}/schema/propertykeys`, {
            params: {
              page_no: this.metadataPropertyPageConfig.pageNumber,
              page_size: !options ? 10 : -1,
              name_order:
                this.metadataPropertyPageConfig.sort !== ''
                  ? this.metadataPropertyPageConfig.sort
                  : null,
              content:
                this.isSearched.status && this.searchWords !== ''
                  ? this.searchWords
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
        this.reuseableProperties = result.data.data.records;
      } else {
        this.metadataProperties = result.data.data.records;
        this.metadataPropertyPageConfig.pageTotal = result.data.data.total;
      }

      if (result.data.data.records.length === 0) {
        if (this.isSearched.status === true) {
          this.currentTabStatus = 'list';
        } else {
          this.currentTabStatus = 'empty';
        }
      } else if (this.currentTabStatus !== 'reuse') {
        // if currentTabStatus is reuse, stay at reuse page
        this.currentTabStatus = 'list';
      }

      this.requestStatus.fetchMetadataPropertyList = 'success';
    } catch (error) {
      this.requestStatus.fetchMetadataPropertyList = 'failed';
      this.errorMessage = error.message;
    }
  });

  checkIfUsing = flow(function* checkIfUsing(
    this: MetadataPropertyStore,
    selectedPropertyNames: string[]
  ) {
    this.requestStatus.checkIfUsing = 'pending';

    try {
      const result: AxiosResponse<responseData<Record<string, boolean>>> =
        yield axios
          .post(
            `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/propertykeys/check_using`,
            {
              names: selectedPropertyNames
            }
          )
          .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      this.metadataPropertyUsingStatus = result.data.data;
      this.requestStatus.checkIfUsing = 'success';
    } catch (error) {
      this.requestStatus.checkIfUsing = 'failed';
      this.errorMessage = error.message;
    }
  });

  addMetadataProperty = flow(function* addMetadataProperty(
    this: MetadataPropertyStore
  ) {
    this.requestStatus.addMetadataProperty = 'pending';

    try {
      const result: AxiosResponse<responseData<null>> = yield axios
        .post(
          `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/propertykeys`,
          {
            name: this.newMetadataProperty._name,
            data_type:
              this.newMetadataProperty.data_type === 'string'
                ? 'TEXT'
                : this.newMetadataProperty.data_type.toUpperCase(),
            cardinality: this.newMetadataProperty.cardinality.toUpperCase()
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      this.requestStatus.addMetadataProperty = 'success';
    } catch (error) {
      this.requestStatus.addMetadataProperty = 'failed';
      this.errorMessage = error.message;
    }
  });

  deleteMetadataProperty = flow(function* deleteMetadataProperty(
    this: MetadataPropertyStore,
    selectedPropertyNames: string[]
  ) {
    this.requestStatus.deleteMetadataProperty = 'pending';

    try {
      const result: AxiosResponse<responseData<null>> = yield axios
        .delete(
          `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/propertykeys?` +
            selectedPropertyNames.map((name) => 'names=' + name).join('&') +
            `&skip_using=${String(selectedPropertyNames.length !== 1)}`
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      if (
        selectedPropertyNames.length === this.metadataProperties.length &&
        this.metadataPropertyPageConfig.pageNumber ===
          Math.ceil(this.metadataPropertyPageConfig.pageTotal / 10) &&
        this.metadataPropertyPageConfig.pageNumber > 1
      ) {
        this.metadataPropertyPageConfig.pageNumber =
          this.metadataPropertyPageConfig.pageNumber - 1;
      }

      this.requestStatus.deleteMetadataProperty = 'success';
    } catch (error) {
      this.requestStatus.deleteMetadataProperty = 'failed';
      this.errorMessage = error.message;
    }
  });

  checkConflict = flow(function* checkConflict(
    this: MetadataPropertyStore,
    selectedNameList: string[]
  ) {
    this.requestStatus.checkConflict = 'pending';

    try {
      const result: AxiosResponse<responseData<CheckedReusableData>> =
        yield axios
          .post(
            `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/propertykeys/check_conflict`,
            {
              propertykeys: selectedNameList.map((selectedName) =>
                this.reuseableProperties.find(
                  ({ name }) => name === selectedName
                )
              )
            }
          )
          .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      this.checkedReusableProperties = result.data.data;
      this.editedCheckedReusableProperties = cloneDeep(result.data.data);
      this.requestStatus.checkConflict = 'success';
    } catch (error) {
      this.requestStatus.checkConflict = 'failed';
      this.errorMessage = error.message;
    }
  });

  recheckConflict = flow(function* recheckConflict(
    this: MetadataPropertyStore
  ) {
    this.requestStatus.recheckConflict = 'pending';

    try {
      const result: AxiosResponse<responseData<CheckedReusableData>> =
        yield axios
          .post(
            `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/propertykeys/recheck_conflict`,
            {
              propertykeys:
                this.editedCheckedReusableProperties!.propertykey_conflicts.map(
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

      this.checkedReusableProperties = result.data.data;
      this.editedCheckedReusableProperties = cloneDeep(result.data.data);
      this.requestStatus.recheckConflict = 'success';
    } catch (error) {
      this.requestStatus.recheckConflict = 'failed';
      this.errorMessage = error.message;
    }
  });

  reuseMetadataProperties = flow(function* reuseMetadataProperties(
    this: MetadataPropertyStore
  ) {
    this.requestStatus.reuseMetadataProperties = 'pending';

    try {
      const result: AxiosResponse<responseData<null>> = yield axios
        .post(
          `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/propertykeys/reuse`,
          this.editedCheckedReusableProperties!
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        throw new Error(result.data.message);
      }

      this.requestStatus.reuseMetadataProperties = 'success';
    } catch (error) {
      this.requestStatus.reuseMetadataProperties = 'failed';
      this.errorMessage = error.message;
    }
  });
}
