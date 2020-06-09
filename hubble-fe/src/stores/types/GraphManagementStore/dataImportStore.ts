import { CancellablePromise } from 'mobx/lib/api/flow';

export interface FileUploadTask {
  name: string;
  size: number;
  status: 'uploading' | 'failed' | 'success';
  chunkList: {
    chunkIndex: number;
    chunk: Blob;
  }[];
  chunkTotal: number;
  uploadedChunkTotal: number;
  pendingChunkIndexes: number[];
  failedChunkIndexes: number[];
  uploadedChunksIndexes: number[];
}

export interface FileUploadResult {
  id: string;
  name: string;
  size: string;
  status: string;
  cause: string | null;
}

export interface FileUploadQueue {
  fileName: string;
  status: string;
  task: CancellablePromise<FileUploadResult | undefined>;
}

export interface FileConfig {
  has_header: boolean;
  column_names: string[];
  column_values: string[];
  format: string;
  delimiter: string;
  charset: string;
  date_format: string;
  time_zone: string;
  skipped_line: string;
}

export type FileValidator = Pick<
  FileConfig,
  'delimiter' | 'charset' | 'date_format' | 'skipped_line'
>;

export interface FieldMapping {
  column_name: string;
  mapped_name: string;
}

export interface ValueMapping {
  column_name: string;
  values: {
    column_value: string;
    mapped_value: string;
  }[];
}

export type ValueMapValidator = {
  null_values: string[];
  value_mapping: ValueMapping[];
};

export interface NullValues {
  checked: string[];
  customized: string[];
}

// export type NullValues = string[];

export interface VertexMap {
  id?: string;
  label: string;
  id_fields: string[];
  field_mapping: FieldMapping[];
  value_mapping: ValueMapping[];
  null_values: NullValues;
}

export interface EdgeMap {
  id?: string;
  label: string;
  source_fields: string[];
  target_fields: string[];
  field_mapping: FieldMapping[];
  value_mapping: ValueMapping[];
  null_values: NullValues;
}

export interface LoadParameter {
  check_vertex: boolean;
  insert_timeout: string;
  max_parse_errors: string;
  max_insert_errors: string;
  retry_times: string;
  retry_interval: string;
}

export interface FileMapInfo {
  id: number;
  name: string;
  total_lines: number;
  total_size: string;
  file_setting: FileConfig;
  vertex_mappings: VertexMap[];
  edge_mappings: EdgeMap[];
  load_parameter: LoadParameter;
  last_access_time: string;
}

export interface FileMapResult {
  records: FileMapInfo[];
}

export interface ImportTasks {
  id: number;
  conn_id: number;
  file_id: number;
  vertices: string[];
  edges: string[];
  load_rate: number;
  load_progress: number;
  file_total_lines: number;
  file_read_lines: number;
  status: string;
  duration: string;
}
