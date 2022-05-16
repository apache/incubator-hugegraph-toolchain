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
  async_status:
    | 'UNKNOWN'
    | 'SCHEDULING'
    | 'SCHEDULED'
    | 'QUEUED'
    | 'RESTORING'
    | 'RUNNING'
    | 'SUCCESS'
    | 'CANCELLING'
    | 'CANCELLED'
    | 'FAILED';
  type: string;
  algorithm_name: string;
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

/* algorithm store  */
export interface LoopDetectionParams {
  source: string;
  direction: string;
  max_depth: string;
  label: string;
  source_in_ring: boolean;
  max_degree: string;
  limit: string;
  capacity: string;
}

export interface FocusDetectionParams {
  source: string;
  target: string;
  direction: string;
  max_depth: string;
  label: string;
  max_degree: string;
  limit: string;
  capacity: string;
}

export interface ShortestPathAlgorithmParams {
  source: string;
  target: string;
  direction: string;
  max_depth: string;
  label: string;
  max_degree: string;
  skip_degree: string;
  capacity: string;
}

// export type ShortestPathAllAlgorithmParams = ShortestPathAlgorithmParams;
export interface ShortestPathAllAlgorithmParams {
  source: string;
  target: string;
  direction: string;
  max_depth: string;
  label: string;
  max_degree: string;
  skip_degree: string;
  capacity: string;
}

export type AllPathAlgorithmParams = {
  source: string;
  target: string;
  direction: string;
  max_depth: string;
  label: string;
  max_degree: string;
  capacity: string;
  limit: string;
};

export interface ModelSimilarityParams {
  method: string;
  source: string;
  vertexType: string;
  vertexProperty: string[][];
  direction: string;
  least_neighbor: string;
  similarity: string;
  label: string;
  max_similar: string;
  least_similar: string;
  property_filter: string;
  least_property_number: string;
  max_degree: string;
  capacity: string;
  limit: string;
  return_common_connection: boolean;
  return_complete_info: boolean;
}

export interface NeighborRankRule {
  uuid: string;
  direction: string;
  labels: string[];
  degree: string;
  top: string;
}

export interface NeighborRankParams {
  source: string;
  alpha: string;
  capacity: string;
  steps: NeighborRankRule[];
}

export interface KStepNeighbor {
  source: string;
  direction: string;
  max_depth: string;
  label: string;
  max_degree: string;
  limit: string;
}

export interface KHop {
  source: string;
  direction: string;
  max_depth: string;
  nearest: boolean;
  label: string;
  max_degree: string;
  limit: string;
  capacity: string;
}

export interface CustomPathParams {
  method: string;
  source: string;
  vertexType: string;
  vertexProperty: string[][];
  sort_by: string;
  capacity: string;
  limit: string;
  steps: CustomPathRule[];
}

export interface CustomPathRule {
  uuid: string;
  direction: string;
  labels: string[];
  properties: string[][];
  weight_by: string;
  default_weight: string;
  degree: string;
  sample: string;
}

export interface RadiographicInspection {
  source: string;
  direction: string;
  max_depth: string;
  label: string;
  max_degree: string;
  capacity: string;
  limit: string;
}

export interface SameNeighbor {
  vertex: string;
  other: string;
  direction: string;
  label: string;
  max_degree: string;
  limit: string;
}

export interface WeightedShortestPath {
  source: string;
  target: string;
  direction: string;
  weight: string;
  with_vertex: boolean;
  label: string;
  max_degree: string;
  skip_degree: string;
  capacity: string;
}

export interface SingleSourceWeightedShortestPath {
  source: string;
  direction: string;
  weight: string;
  with_vertex: boolean;
  label: string;
  max_degree: string;
  skip_degree: string;
  capacity: string;
  limit: string;
}

export interface Jaccard {
  vertex: string;
  other: string;
  direction: string;
  label: string;
  max_degree: string;
}

export interface PersonalRank {
  source: string;
  alpha: string;
  max_depth: string;
  with_label: string;
  label: string;
  degree: string;
  limit: string;
  sorted: boolean;
}
