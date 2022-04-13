export function initRequestStatus() {
  return {
    fetchAsyncTaskList: 'standby',
    fetchAsyncTask: 'standby',
    deleteAsyncTask: 'standby',
    abortAsyncTask: 'standby'
  };
}

export function initErrorInfo() {
  return {
    fetchAsyncTaskList: {
      code: NaN,
      message: ''
    },
    fetchAsyncTask: {
      code: NaN,
      message: ''
    },
    deleteAsyncTask: {
      code: NaN,
      message: ''
    },
    abortAsyncTask: {
      code: NaN,
      message: ''
    }
  };
}
