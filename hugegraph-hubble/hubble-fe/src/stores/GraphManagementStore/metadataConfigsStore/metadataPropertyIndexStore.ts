import { observable, action, flow, computed } from 'mobx';
import axios, { AxiosResponse } from 'axios';

import { MetadataConfigsRootStore } from './metadataConfigsStore';
import { checkIfLocalNetworkOffline } from '../../utils';

import { baseUrl, responseData } from '../../types/common';
import {
  MetadataPropertyIndex,
  MetadataPropertyIndexResponse,
  PageConfig
} from '../../types/GraphManagementStore/metadataConfigsStore';

export class MetadataPropertyIndexStore {
  metadataConfigsRootStore: MetadataConfigsRootStore;

  constructor(MetadataConfigsRootStore: MetadataConfigsRootStore) {
    this.metadataConfigsRootStore = MetadataConfigsRootStore;
  }

  @observable validateLicenseOrMemories = true;
  @observable searchWords = '';
  @observable.shallow isSearched = {
    status: false,
    value: ''
  };

  @observable.shallow requestStatus = {
    fetchMetadataPropertIndexes: 'pending'
  };

  @observable metadataPropertyIndexPageConfig: PageConfig = {
    pageNumber: 1,
    pageTotal: 0,
    sort: ''
  };

  @observable errorMessage = '';

  @observable.ref metadataPropertyIndexes: MetadataPropertyIndex[] = [
    {
      owner: 'person',
      owner_type: 'VERTEX_LABEL',
      name: 'personByBorn',
      type: 'RANGE',
      fields: ['age', 'city']
    },
    {
      owner: 'person',
      owner_type: 'VERTEX_LABEL',
      name: 'personByBorn',
      type: 'RANGE',
      fields: ['age']
    },
    {
      owner: 'person',
      owner_type: 'VERTEX_LABEL',
      name: 'personByBorn',
      type: 'RANGE',
      fields: ['city']
    },
    {
      owner: 'company',
      owner_type: 'VERTEX_LABEL',
      name: 'personByBorn',
      type: 'RANGE',
      fields: ['age', 'city']
    },
    {
      owner: 'company',
      owner_type: 'VERTEX_LABEL',
      name: 'personByBorn',
      type: 'RANGE',
      fields: ['age', 'city']
    },
    {
      owner: 'company',
      owner_type: 'VERTEX_LABEL',
      name: 'personByBorn',
      type: 'RANGE',
      fields: ['age', 'city']
    },
    {
      owner: 'city',
      owner_type: 'VERTEX_LABEL',
      name: 'personByBorn',
      type: 'RANGE',
      fields: ['age', 'city']
    },
    {
      owner: 'city',
      owner_type: 'VERTEX_LABEL',
      name: 'personByBorn',
      type: 'RANGE',
      fields: ['age', 'city']
    },
    {
      owner: 'city',
      owner_type: 'VERTEX_LABEL',
      name: 'personByBorn',
      type: 'RANGE',
      fields: ['age', 'city']
    },
    {
      owner: 'name',
      owner_type: 'VERTEX_LABEL',
      name: 'personByBorn',
      type: 'RANGE',
      fields: ['age', 'city']
    }
  ];

  @computed get collpaseInfo(): null | number[][] {
    const collpaseNumbers: number[] = [];

    this.metadataPropertyIndexes.forEach(
      ({ owner }: MetadataPropertyIndex, index: number) => {
        // first owner has default rowSpanStart = 1
        if (index === 0) {
          collpaseNumbers.push(1);
          return;
        }

        // if owner equals to the previous one, plus the owner rowSpan by 1
        // else push this different owner rowSpanStartIndex in to array
        owner === this.metadataPropertyIndexes[index - 1].owner
          ? ++collpaseNumbers[collpaseNumbers.length - 1]
          : collpaseNumbers.push(1);
      }
    );

    if (collpaseNumbers.length === this.metadataPropertyIndexes.length) {
      return null;
    }

    const collpaseStartIndexes: number[] = [0];

    collpaseNumbers
      .slice(0, collpaseNumbers.length - 1)
      .reduce((prev, curr) => {
        collpaseStartIndexes.push(prev + curr);
        return prev + curr;
      }, 0);

    return [collpaseStartIndexes, collpaseNumbers];
  }

  @action
  mutateSearchWords(text: string) {
    this.searchWords = text;
  }

  @action
  mutatePageNumber(pageNumber: number) {
    this.metadataPropertyIndexPageConfig.pageNumber = pageNumber;
  }

  @action
  switchIsSearchedStatus(isSearched: boolean) {
    this.isSearched.status = isSearched;

    isSearched
      ? (this.isSearched.value = this.searchWords)
      : (this.isSearched.value = '');
  }

  @action
  dispose() {
    this.searchWords = '';
    this.isSearched = {
      status: false,
      value: ''
    };
    this.requestStatus = {
      fetchMetadataPropertIndexes: 'pending'
    };
    this.metadataPropertyIndexPageConfig = {
      pageNumber: 1,
      pageTotal: 0,
      sort: ''
    };
    this.errorMessage = '';
    this.metadataPropertyIndexes = [];
  }

  fetchMetadataPropertIndexes = flow(function* fetchMetadataPropertIndexes(
    this: MetadataPropertyIndexStore,
    indexType: 'vertex' | 'edge'
  ) {
    this.requestStatus.fetchMetadataPropertIndexes = 'pending';

    try {
      const result: AxiosResponse<
        responseData<MetadataPropertyIndexResponse>
      > = yield axios
        .get(
          `${baseUrl}/${this.metadataConfigsRootStore.currentId}/schema/propertyindexes`,
          {
            params: {
              page_no: this.metadataPropertyIndexPageConfig.pageNumber,
              page_size: 10,
              is_vertex_label: indexType === 'vertex',
              content:
                this.isSearched.status && this.searchWords !== ''
                  ? this.searchWords
                  : null
            }
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        if (result.data.status === 401) {
          this.validateLicenseOrMemories = false;
        }

        throw new Error(result.data.message);
      }

      this.metadataPropertyIndexes = result.data.data.records;
      this.metadataPropertyIndexPageConfig.pageTotal = result.data.data.total;
      this.requestStatus.fetchMetadataPropertIndexes = 'success';
    } catch (error) {
      this.requestStatus.fetchMetadataPropertIndexes = 'failed';
      this.errorMessage = error.message;
    }
  });
}
