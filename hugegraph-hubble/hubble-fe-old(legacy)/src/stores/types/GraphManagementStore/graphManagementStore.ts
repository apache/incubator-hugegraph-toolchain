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

import { responseData } from '../common';

export interface LincenseInfo {
  name: string;
  edition: string;
  version: string;
  allowed_graphs: number;
  allowed_datasize: string;
}

export interface GraphData {
  id: number;
  name: string;
  graph: string;
  host: string;
  port: number;
  create_time: string;
  username: string;
  enabled: boolean;
  password: string;
}

export interface GraphDataConfig {
  [index: string]: string | undefined;
  name: string;
  graph: string;
  host: string;
  port: string;
  username: string;
  password: string;
}

export interface GraphDataPageConfig {
  pageNumber: number;
  pageSize: number;
  pageTotal: number;
}

export interface GraphDataList {
  records: GraphData[];
  total: number;
}

export type GraphDataResponse = responseData<GraphDataList>;
