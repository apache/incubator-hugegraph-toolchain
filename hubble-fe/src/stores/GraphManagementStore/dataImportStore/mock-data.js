export default [
  {
    id: 1,
    name: 'person.csv',
    total_lines: 7,
    file_setting: {
      has_header: true,
      column_names: ['name', 'age', 'city'],
      column_values: ['marko', '28', 'Beijing'],
      format: 'TEXT',
      delimiter: ';',
      charset: 'UTF-8',
      date_format: 'yyyy-MM-dd HH:mm:ss',
      time_zone: 'GMT+8',
      skipped_line: '(^#|^//).*|'
    },
    vertex_mappings: [
      {
        id: '6be6d34d-6978-4bb5-a077-913e02255c55',
        label: 'fre',
        field_mapping: [
          {
            column_name: 'age',
            mapped_name: 'age'
          },
          {
            column_name: 'city',
            mapped_name: 'city'
          }
        ],
        value_mapping: [
          {
            column_name: 'name',
            values: [
              {
                column_value: 'A',
                mapped_value: 'Anny'
              },
              {
                column_value: 'B',
                mapped_value: 'Bob'
              }
            ]
          },
          {
            column_name: 'age',
            values: [
              {
                column_value: '1',
                mapped_value: '10'
              },
              {
                column_value: '2',
                mapped_value: '20'
              },
              {
                column_value: '3',
                mapped_value: '30'
              }
            ]
          }
        ],
        null_values: ['', 'NULL', 'null'],
        id_fields: ['name', 'hello']
      }
    ],
    edge_mappings: [
      {
        id: '90abec81-a076-427d-9f65-2e0f86eefa27',
        label: 'created',
        source_fields: ['name'],
        target_fields: ['id'],
        field_mapping: [
          {
            column_name: 'age',
            mapped_name: 'age'
          },
          {
            column_name: 'city',
            mapped_name: 'city'
          }
        ],
        value_mapping: [
          {
            column_name: 'name',
            values: [
              {
                column_value: 'A',
                mapped_value: 'Anny'
              },
              {
                column_value: 'B',
                mapped_value: 'Bob'
              }
            ]
          },
          {
            column_name: 'age',
            values: [
              {
                column_value: '1',
                mapped_value: '10'
              },
              {
                column_value: '2',
                mapped_value: '20'
              },
              {
                column_value: '3',
                mapped_value: '30'
              }
            ]
          }
        ],
        null_values: ['', 'NULL', 'EMPTY']
      }
    ],
    load_parameter: {
      check_vertex: false,
      insert_timeout: 60,
      max_parse_errors: 1,
      max_insert_errors: 500,
      retry_times: 3,
      retry_interval: 10
    },
    last_access_time: '2020-03-31 22:39:22'
  },
  {
    id: 2,
    name: 'abc.csv',
    total_lines: 7,
    file_setting: {
      has_header: false,
      column_names: ['name', 'age', 'city'],
      column_values: ['marko', '28', 'Beijing'],
      format: 'TEXT',
      delimiter: ',',
      charset: 'UTF-8',
      date_format: 'yyyy-MM-dd HH:mm:ss',
      time_zone: 'GMT+8',
      skipped_line: '(^#|^//).*|'
    },
    vertex_mappings: [
      {
        id: '6be6d34d-6978-4bb5-a077-913e02255c56',
        label: 'age',
        field_mapping: [
          {
            column_name: 'age',
            mapped_name: 'age'
          },
          {
            column_name: 'city',
            mapped_name: 'city'
          }
        ],
        value_mapping: [
          {
            column_name: 'name',
            values: [
              {
                column_value: 'A',
                mapped_value: 'AnnyWay'
              },
              {
                column_value: 'B',
                mapped_value: 'Bob'
              }
            ]
          },
          {
            column_name: 'age',
            values: [
              {
                column_value: '1',
                mapped_value: '10'
              },
              {
                column_value: '2',
                mapped_value: '20'
              },
              {
                column_value: '3',
                mapped_value: '30'
              }
            ]
          }
        ],
        null_values: ['', 'NULL', 'null'],
        id_fields: ['name']
      }
    ],
    edge_mappings: [],
    load_parameter: {
      check_vertex: false,
      insert_timeout: 60,
      max_parse_errors: 1,
      max_insert_errors: 500,
      retry_times: 3,
      retry_interval: 10
    },
    last_access_time: '2020-03-31 22:39:22'
  },
  {
    id: 3,
    name: 'edf.csv',
    total_lines: 7,
    file_setting: {
      has_header: false,
      column_names: ['name', 'age', 'city'],
      column_values: ['marko', '28', 'Beijing'],
      format: 'TEXT',
      delimiter: ',',
      charset: 'UTF-8',
      date_format: 'yyyy-MM-dd HH:mm:ss.SSS',
      time_zone: 'GMT+8',
      skipped_line: '(^#|^//).*|'
    },
    vertex_mappings: [
      {
        id: '6be6d34d-6978-4bb5-a077-913e02255c59',
        label: 'fre',
        field_mapping: [
          {
            column_name: 'age',
            mapped_name: 'age'
          },
          {
            column_name: 'city',
            mapped_name: 'city'
          }
        ],
        value_mapping: [
          {
            column_name: 'name',
            values: [
              {
                column_value: 'A',
                mapped_value: 'Anny'
              },
              {
                column_value: 'B',
                mapped_value: 'Bob'
              }
            ]
          },
          {
            column_name: 'age',
            values: [
              {
                column_value: '1',
                mapped_value: '10'
              },
              {
                column_value: '2',
                mapped_value: '20'
              },
              {
                column_value: '3',
                mapped_value: '30'
              }
            ]
          }
        ],
        null_values: ['', 'NULL', 'null'],
        id_fields: ['name', 'hello']
      }
    ],
    edge_mappings: [
      {
        id: '90abec81-a076-427d-9f65-2e0f86eefa27',
        label: 'created',
        source_fields: ['name'],
        target_fields: ['id'],
        field_mapping: [
          {
            column_name: 'age',
            mapped_name: 'age'
          },
          {
            column_name: 'city',
            mapped_name: 'city'
          }
        ],
        value_mapping: [
          {
            column_name: 'name',
            values: [
              {
                column_value: 'A',
                mapped_value: 'Anny'
              },
              {
                column_value: 'B',
                mapped_value: 'Bob'
              }
            ]
          },
          {
            column_name: 'age',
            values: [
              {
                column_value: '1',
                mapped_value: '10'
              },
              {
                column_value: '2',
                mapped_value: '20'
              },
              {
                column_value: '3',
                mapped_value: '30'
              }
            ]
          }
        ],
        null_values: ['', 'NULL', 'EMPTY']
      }
    ],
    load_parameter: {
      check_vertex: false,
      insert_timeout: 60,
      max_parse_errors: 1,
      max_insert_errors: 500,
      retry_times: 3,
      retry_interval: 10
    },
    last_access_time: '2020-03-31 22:39:22'
  }
];
