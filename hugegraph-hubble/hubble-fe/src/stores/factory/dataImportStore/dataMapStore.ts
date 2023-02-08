/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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
