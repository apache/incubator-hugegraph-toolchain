import { dict } from '../common';
import { VertexTypeStore } from '../../GraphManagementStore/metadataConfigsStore/vertexTypeStore';

export interface PageConfig {
  pageNumber: number;
  pageTotal: number;
  sort: 'asc' | 'desc' | '';
}

// metadata property

export interface MetadataProperty {
  name: string;
  data_type: string;
  cardinality: string;
  create_time?: string;
}

export interface NewMetadataProperty extends MetadataProperty {
  _name?: string;
}

export interface MetadataPropertyListResponse {
  records: MetadataProperty[];
  total: number;
}

// vertex types

export type VertexTypeProperty = {
  name: string;
  nullable: boolean;
};

export type VertexTypePropertyIndex = {
  name: string;
  type: string;
  fields: string[];
};

export type VertexTypeStyle = {
  icon: string | null;
  color: string | null;
  size: string;
  display_fields: string[];
};

export type VertexTypeValidateFields =
  | 'name'
  | 'properties'
  | 'primaryKeys'
  | 'displayFeilds'
  | 'propertyIndexes';

export type VertexTypeValidatePropertyIndexes = {
  name: string;
  type: string;
  properties: string;
};

export interface EditVertexTypeParams {
  append_properties: VertexTypeProperty[];
  append_property_indexes: VertexTypePropertyIndex[];
  remove_property_indexes: string[];
  style: {
    color: string | null;
    icon: null;
    size: string;
    display_fields: string[];
  };
}

export interface VertexType {
  [index: string]: string | any[] | VertexTypeStyle | boolean;

  name: string;
  id_strategy: string;
  properties: VertexTypeProperty[];
  primary_keys: string[];
  property_indexes: VertexTypePropertyIndex[];
  open_label_index: boolean;
  style: VertexTypeStyle;
}

export interface VertexTypeListResponse {
  records: VertexType[];
  total: number;
}

export interface CheckedReusableData {
  type: string;
  propertykey_conflicts: { entity: MetadataProperty; status: string }[];
  propertyindex_conflicts: { entity: MetadataPropertyIndex; status: string }[];
  vertexlabel_conflicts: { entity: VertexType; status: string }[];
  edgelabel_conflicts: { entity: EdgeType; status: string }[];
}

export interface ReCheckedReusableData {
  propertykeys: MetadataProperty[];
  propertyindexes: MetadataPropertyIndex[];
  vertexlabels: VertexType[];
}

// edge types

type EdgeTypeProperty = VertexTypeProperty;
type EdgeTypePropertyIndex = VertexTypePropertyIndex;

type EdgeTypeStyle = {
  color: string | null;
  icon: string | null;
  with_arrow: boolean;
  thickness: string;
  display_fields: string[];
};

export type EdgeTypeValidateFields =
  | 'name'
  | 'sourceLabel'
  | 'targetLabel'
  | 'properties'
  | 'sortKeys'
  | 'propertyIndexes'
  | 'displayFeilds';

export type EdgeTypeValidatePropertyIndexes = VertexTypeValidatePropertyIndexes;

export interface EditEdgeTypeParams {
  append_properties: VertexTypeProperty[];
  append_property_indexes: VertexTypePropertyIndex[];
  remove_property_indexes: string[];
  style: {
    color: string | null;
    icon: null;
    with_arrow: boolean | null;
    thickness: string;
    display_fields: string[];
  };
}

export interface EdgeType {
  name: string;
  source_label: string;
  target_label: string;
  link_multi_times: boolean;
  properties: EdgeTypeProperty[];
  sort_keys: string[];
  property_indexes: EdgeTypePropertyIndex[];
  open_label_index: boolean;
  style: EdgeTypeStyle;
}

export interface EdgeTypeListResponse {
  records: EdgeType[];
  total: number;
}

// metadata property index

export interface MetadataPropertyIndex {
  owner: string;
  owner_type: string;
  name: string;
  type: string;
  fields: string[];
}

export interface MetadataPropertyIndexResponse {
  records: MetadataPropertyIndex[];
  total: number;
}

// graph view data

export type DrawerTypes =
  | 'create-property'
  | 'create-vertex'
  | 'create-edge'
  | 'check-property'
  | 'check-vertex'
  | 'check-edge'
  | 'edit-vertex'
  | 'edit-edge'
  | '';

export interface GraphViewData {
  vertices: {
    id: string;
    label: string;
    properties: Record<string, string>;
    primary_keys: string[];
    style?: dict<string | number>;
  }[];
  edges: {
    id: string;
    label: string;
    source: string;
    target: string;
    properties: Record<string, string>;
    sort_keys: string[];
  }[];
}
