import { Node, Edge } from 'vis-network';
import { dict, responseData } from '../common';
import { EdgeType } from './metadataConfigsStore';

export type ColorSchemas = dict<string>;
export type RuleMap = dict<string>;

export interface FetchColorSchemas {
  status: number;
  data: ColorSchemas;
  message: string;
}

export type FetchFilteredPropertyOptions = responseData<EdgeType>;

export interface GraphNode extends Node {
  id: string;
  label: string;
  properties: dict<any>;
  chosen?: any;
  vLabel?: string;
  style?: dict<string | number>;
}

export interface GraphEdge extends Edge {
  id: string;
  label: string;
  properties: dict<any>;
  source: string;
  target: string;
}

export interface GraphView {
  vertices: GraphNode[];
  edges: GraphEdge[];
}

export interface NewGraphData {
  id?: string;
  label: string;
  properties: {
    nullable: Map<string, string>;
    nonNullable: Map<string, string>;
  };
}

export interface EditableProperties {
  nonNullable: Map<string, string>;
  nullable: Map<string, string>;
}

export interface QueryResult {
  table_view: {
    header: string[];
    rows: dict<any>[];
  };
  json_view: {
    data: dict<any>[];
  };
  graph_view: GraphView;
  type: string;
}

export type FetchGraphResponse = responseData<QueryResult>;

export interface ValueTypes {
  name: string;
  data_type: string;
  cardinality: string;
  create_time: string;
}

export interface AddQueryCollectionParams {
  name: string;
  content: string;
}

export interface ExecutionLogs {
  id: number;
  async_id: number;
  type: string;
  content: string;
  status: 'SUCCESS' | 'RUNNING' | 'FAILED';
  duration: string;
  create_time: string;
}

export type ExecutionLogsResponse = responseData<{
  records: ExecutionLogs[];
  total: number;
}>;

export interface FavoriteQuery {
  id: number;
  name: string;
  content: string;
  create_time: string;
}

export type FavoriteQueryResponse = responseData<{
  records: FavoriteQuery[];
  total: number;
}>;
