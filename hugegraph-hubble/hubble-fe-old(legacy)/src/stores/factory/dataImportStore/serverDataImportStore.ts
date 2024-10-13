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
