export function initRequestStatus() {
  return {
    fetchImportJobList: 'standby',
    createNewJob: 'standby',
    updateJobInfo: 'standby',
    deleteJob: 'standby',
    fetchFailedReason: 'standby'
  };
}

export function initErrorInfo() {
  return {
    fetchImportJobList: {
      code: NaN,
      message: ''
    },
    createNewJob: {
      code: NaN,
      message: ''
    },
    updateJobInfo: {
      code: NaN,
      message: ''
    },
    deleteJob: {
      code: NaN,
      message: ''
    },
    fetchFailedReason: {
      code: NaN,
      message: ''
    }
  };
}
