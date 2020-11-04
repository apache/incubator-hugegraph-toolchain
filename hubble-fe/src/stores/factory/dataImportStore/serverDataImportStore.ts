import { LoadParameter } from '../../types/GraphManagementStore/dataImportStore';

export function initRequestStatus() {
  return {
    fetchImportTasks: 'standby',
    fetchAllImportTasks: 'standby',
    setConfigParams: 'standby',
    startImport: 'standby',
    pauseImport: 'standby',
    resumeImport: 'standby',
    abortImport: 'standby',
    retryImport: 'standby',
    deleteTaskImport: 'standby',
    checkErrorLogs: 'standby'
  };
}

export function initErrorInfo() {
  return {
    fetchImportTasks: {
      code: NaN,
      message: ''
    },
    fetchAllImportTasks: {
      code: NaN,
      message: ''
    },
    setConfigParams: {
      code: NaN,
      message: ''
    },
    startImport: {
      code: NaN,
      message: ''
    },
    pauseImport: {
      code: NaN,
      message: ''
    },
    resumeImport: {
      code: NaN,
      message: ''
    },
    abortImport: {
      code: NaN,
      message: ''
    },
    retryImport: {
      code: NaN,
      message: ''
    },
    deleteTaskImport: {
      code: NaN,
      message: ''
    },
    checkErrorLogs: {
      code: NaN,
      message: ''
    }
  };
}

export function createValidateFileInfoErrorMessage(): Record<
  keyof LoadParameter,
  string
> {
  return {
    check_vertex: '',
    insert_timeout: '',
    max_parse_errors: '',
    max_insert_errors: '',
    retry_times: '',
    retry_interval: ''
  };
}
