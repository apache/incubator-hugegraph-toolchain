export function initRequestStatus() {
  return {
    uploadFiles: 'standby',
    deleteFiles: 'standby',
    fetchVertexTypeList: 'standby',
    fetchEdgeTypeList: 'standby'
  };
}

export function initErrorInfo() {
  return {
    uploadFiles: {
      code: NaN,
      message: ''
    },
    deleteFiles: {
      code: NaN,
      message: ''
    },
    fetchVertexTypeList: {
      code: NaN,
      message: ''
    },
    fetchEdgeTypeList: {
      code: NaN,
      message: ''
    }
  };
}
