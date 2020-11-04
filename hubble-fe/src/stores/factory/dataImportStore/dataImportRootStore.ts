export function initRequestStatus() {
  return {
    fetchFilehashes: 'standby',
    uploadFiles: 'standby',
    deleteFiles: 'standby',
    fetchVertexTypeList: 'standby',
    fetchEdgeTypeList: 'standby',
    sendUploadCompleteSignal: 'standby',
    sendMappingCompleteSignal: 'standby'
  };
}

export function initErrorInfo() {
  return {
    fetchFilehashes: {
      code: NaN,
      message: ''
    },
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
    },
    sendUploadCompleteSignal: {
      code: NaN,
      message: ''
    },
    sendMappingCompleteSignal: {
      code: NaN,
      message: ''
    }
  };
}
