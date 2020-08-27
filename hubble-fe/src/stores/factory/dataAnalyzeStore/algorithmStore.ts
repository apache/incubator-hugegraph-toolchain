export function initializeRequestStatus() {
  return {
    fetchAlgorithmResult: 'standby'
  };
}

export function initializeErrorInfo() {
  return {
    fetchAlgorithmResult: {
      code: NaN,
      message: ''
    }
  };
}

export function createShortestPathDefaultParams() {
  return {
    source: '',
    target: '',
    direction: 'BOTH',
    max_depth: '',
    label: '__all__',
    max_degree: '10000',
    skip_degree: '0',
    capacity: '10000000',
    limit: '1000000'
  };
}

export function createValidateShortestPathParamsErrorMessage() {
  return {
    source: '',
    target: '',
    direction: '',
    max_depth: '',
    label: '',
    max_degree: '',
    skip_degree: '',
    capacity: '',
    limit: ''
  };
}
