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
