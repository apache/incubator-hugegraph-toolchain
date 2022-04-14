import { createContext } from 'react';
import { observable, action, flow, computed } from 'mobx';
import axios, { AxiosResponse } from 'axios';
import { isEmpty, size } from 'lodash-es';

import i18next from '../../../i18n';
import {
  Job,
  JobResponse
} from '../../types/GraphManagementStore/dataImportStore';
import {
  initRequestStatus,
  initErrorInfo
} from '../../factory/dataImportStore/importManagmentStore';
import { checkIfLocalNetworkOffline } from '../../utils';

import { baseUrl, responseData } from '../../types/common';
import type {
  JobGlance,
  JobFailedReason
} from '../../types/GraphManagementStore/dataImportStore';

export class ImportManagerStore {
  @observable requestStatus = initRequestStatus();
  @observable errorInfo = initErrorInfo();

  @observable currentId: number | null = null;
  @observable jobDetailsStep = 'basic';
  @observable selectedJobIndex = NaN;
  @observable selectedJob: Job | null = null;
  @observable searchWords = '';
  @observable isCreateNewProperty = false;

  @observable.shallow importJobListPageConfig = {
    pageNumber: 1,
    pageTotal: 0,
    sort: ''
  };

  @observable.shallow isSearched = {
    status: false,
    value: ''
  };

  @observable.shallow newJob = {
    name: '',
    description: ''
  };

  // @observable editJob: JobGlance | null = null;
  @observable editJob: JobGlance | null = {
    name: '',
    description: ''
  };

  @observable.ref importJobList: Job[] = [];
  @observable.ref failedReason: JobFailedReason[] = [];

  @observable validateNewJobErrorMessage = {
    name: '',
    description: ''
  };

  @observable validateEditJobErrorMessage = {
    name: '',
    description: ''
  };

  @computed get currentJobDetails() {
    return this.importJobList[this.selectedJobIndex];
  }

  @action
  setCurrentId(id: number) {
    this.currentId = id;
  }

  @action
  switchCreateNewProperty(flag: boolean) {
    this.isCreateNewProperty = flag;
  }

  @action
  switchSearchedStatus(isSearched: boolean) {
    this.isSearched.status = isSearched;

    isSearched
      ? (this.isSearched.value = this.searchWords)
      : (this.isSearched.value = '');
  }

  @action
  setCurrentJobDetailStep(step: string) {
    this.jobDetailsStep = step;
  }

  @action
  setSelectedJobIndex(index: number) {
    this.selectedJobIndex = index;
  }

  @action
  setSelectedJob(jobId: number | null) {
    if (jobId === null) {
      this.selectedJob = null;
    } else {
      this.selectedJob = this.importJobList.find(
        ({ id }) => jobId === id
      ) as Job;
    }
  }

  @action
  mutateSearchWords(word: string) {
    this.searchWords = word;
  }

  @action
  mutateNewJob<T extends keyof JobGlance>(key: T, value: JobGlance[T]) {
    this.newJob[key] = value;
  }

  @action
  mutateEditJob<T extends keyof JobGlance>(key: T, value: JobGlance[T]) {
    if (this.editJob !== null) {
      this.editJob[key] = value;
    }
  }

  @action
  mutateImportJobListPageNumber(pageNumber: number) {
    this.importJobListPageConfig.pageNumber = pageNumber;
  }

  @action
  validateJob(type: 'new' | 'edit', category: keyof JobGlance) {
    const job = type === 'new' ? this.newJob : this.editJob!;
    const errorMessage =
      type === 'new'
        ? this.validateNewJobErrorMessage
        : this.validateEditJobErrorMessage;

    if (category === 'name') {
      const name = job.name;

      if (isEmpty(name)) {
        errorMessage.name = i18next.t('import-manager.validator.no-empty');
      } else if (size(name) > 48) {
        errorMessage.name = i18next.t(
          'import-manager.validator.over-limit-size'
        );
      } else if (!/^[\w\d\u4e00-\u9fa5]{1,48}$/.test(name)) {
        errorMessage.name = i18next.t(
          'import-manager.validator.invalid-format'
        );
      } else {
        errorMessage.name = '';
      }
    }

    if (category === 'description') {
      const description = job.description;

      if (size(description) > 200) {
        errorMessage.description = i18next.t(
          'import-manager.validator.over-limit-size'
        );
      } else if (!/^[\w\d\u4e00-\u9fa5]{0,200}$/.test(description)) {
        errorMessage.description = i18next.t(
          'import-manager.validator.invalid-format'
        );
      } else {
        errorMessage.description = '';
      }
    }
  }

  @action
  resetJob(type: 'new' | 'edit') {
    if (type === 'new') {
      this.newJob = {
        name: '',
        description: ''
      };
    } else {
      this.editJob = null;
    }
  }

  @action
  dispose() {
    this.requestStatus = initRequestStatus();
    this.errorInfo = initErrorInfo();
    this.currentId = null;
    this.jobDetailsStep = 'basic';
    this.selectedJobIndex = NaN;
    this.selectedJob = null;
    this.isCreateNewProperty = false;

    this.importJobListPageConfig = {
      pageNumber: 1,
      pageTotal: 0,
      sort: ''
    };

    this.isSearched = {
      status: false,
      value: ''
    };

    this.newJob = {
      name: '',
      description: ''
    };

    this.editJob = {
      name: '',
      description: ''
    };

    this.importJobList = [];
    this.failedReason = [];

    this.validateNewJobErrorMessage = {
      name: '',
      description: ''
    };

    this.validateEditJobErrorMessage = {
      name: '',
      description: ''
    };
  }

  fetchImportJobList = flow(function* fetchImportJobList(
    this: ImportManagerStore
  ) {
    this.requestStatus.fetchImportJobList = 'pending';

    try {
      const result: AxiosResponse<responseData<JobResponse>> = yield axios
        .get<responseData<JobResponse>>(
          `${baseUrl}/${this.currentId}/job-manager?page_no=${this.importJobListPageConfig.pageNumber}&page_size=10` +
            (this.isSearched.status && this.searchWords !== ''
              ? `&content=${this.searchWords}`
              : '')
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.fetchImportJobList.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.importJobList = result.data.data.records;
      this.importJobListPageConfig.pageTotal = result.data.data.total;
      this.requestStatus.fetchImportJobList = 'success';
    } catch (error) {
      this.requestStatus.fetchImportJobList = 'failed';
      this.errorInfo.fetchImportJobList.message = error.message;
    }
  });

  createNewJob = flow(function* createNewJob(this: ImportManagerStore) {
    this.requestStatus.createNewJob = 'pending';

    try {
      const result: AxiosResponse<responseData<Job>> = yield axios
        .post<responseData<Job>>(`${baseUrl}/${this.currentId}/job-manager`, {
          job_name: this.newJob.name,
          job_remarks: this.newJob.description
        })
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.createNewJob.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.requestStatus.createNewJob = 'success';
    } catch (error) {
      this.requestStatus.createNewJob = 'failed';
      this.errorInfo.createNewJob.message = error.message;
    }
  });

  updateJobInfo = flow(function* updateJobInfo(this: ImportManagerStore) {
    this.requestStatus.updateJobInfo = 'pending';

    try {
      const result: AxiosResponse<responseData<Job>> = yield axios
        .put<responseData<Job>>(
          `${baseUrl}/${this.currentId}/job-manager/${this.selectedJob!.id}`,
          {
            job_name: this.editJob!.name,
            job_remarks: this.editJob!.description
          }
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.updateJobInfo.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.selectedJob = result.data.data;
      this.requestStatus.updateJobInfo = 'success';
    } catch (error) {
      this.requestStatus.updateJobInfo = 'failed';
      this.errorInfo.updateJobInfo.message = error.message;
    }
  });

  deleteJob = flow(function* deleteJob(this: ImportManagerStore, id: number) {
    this.requestStatus.deleteJob = 'pending';

    try {
      const result: AxiosResponse<responseData<Job>> = yield axios
        .delete<responseData<Job>>(
          `${baseUrl}/${this.currentId}/job-manager/${id}`
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.deleteJob.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.requestStatus.deleteJob = 'success';
    } catch (error) {
      this.requestStatus.deleteJob = 'failed';
      this.errorInfo.deleteJob.message = error.message;
    }
  });

  fetchFailedReason = flow(function* fetchFailedReason(
    this: ImportManagerStore,
    connectId: number,
    jobId: number
  ) {
    this.requestStatus.fetchFailedReason = 'pending';

    try {
      const result: AxiosResponse<responseData<
        JobFailedReason[]
      >> = yield axios
        .get<responseData<JobFailedReason[]>>(
          `${baseUrl}/${connectId}/job-manager/${jobId}/reason`
        )
        .catch(checkIfLocalNetworkOffline);

      if (result.data.status !== 200) {
        this.errorInfo.fetchFailedReason.code = result.data.status;
        throw new Error(result.data.message);
      }

      this.failedReason = result.data.data;
      this.requestStatus.fetchFailedReason = 'success';
    } catch (error) {
      this.requestStatus.fetchFailedReason = 'failed';
      this.errorInfo.fetchFailedReason.message = error.message;
    }
  });
}

export default createContext(new ImportManagerStore());
