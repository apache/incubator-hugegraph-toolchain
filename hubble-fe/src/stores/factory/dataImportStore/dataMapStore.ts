export function initRequestStatus() {
  return {
    updateFileConfig: 'standby',
    fetchDataMaps: 'standby',
    updateVertexMap: 'standby',
    updateEdgeMap: 'standby',
    deleteVertexMap: 'standby',
    deleteEdgeMap: 'standby'
  };
}

export function initErrorInfo() {
  return {
    updateFileConfig: {
      code: NaN,
      message: ''
    },
    fetchDataMaps: {
      code: NaN,
      message: ''
    },
    updateVertexMap: {
      code: NaN,
      message: ''
    },
    updateEdgeMap: {
      code: NaN,
      message: ''
    },
    deleteVertexMap: {
      code: NaN,
      message: ''
    },
    deleteEdgeMap: {
      code: NaN,
      message: ''
    }
  };
}

export function createNewVertexType() {
  return {
    label: '',
    id_fields: [''],
    field_mapping: [],
    value_mapping: [],
    null_values: {
      checked: ['NULL', 'null'],
      customized: []
    }
  };
}

export function createNewEdgeType() {
  return {
    label: '',
    source_fields: [''],
    target_fields: [''],
    field_mapping: [],
    value_mapping: [],
    null_values: {
      checked: ['NULL', 'null'],
      customized: []
    }
  };
}

export function createValidateFileInfoErrorMessage() {
  return {
    delimiter: '',
    charset: '',
    date_format: '',
    skipped_line: ''
  };
}

export function createValidateAdvanceConfigErrorMessage() {
  return {
    null_values: [],
    value_mapping: []
  };
}
