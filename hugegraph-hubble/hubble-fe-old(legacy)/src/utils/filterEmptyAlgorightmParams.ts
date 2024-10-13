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

import { omitBy, cloneDeep, isEmpty } from 'lodash-es';

export function removeLabelKey<T extends Record<string, any>>(
  params: T,
  isCloned = true,
  key = 'label'
) {
  const paramObject = isCloned ? params : cloneDeep(params);

  if (paramObject[key] === '__all__') {
    delete paramObject[key];
  }

  return paramObject;
}

export function filterEmptyAlgorightmParams(params: object, keys: string[]) {
  return omitBy(params, (value, key) => keys.includes(key) && isEmpty(value));
}
