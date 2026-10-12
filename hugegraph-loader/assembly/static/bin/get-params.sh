#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with this
# work for additional information regarding copyright ownership. The ASF
# licenses this file to You under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#

function get_params() {
  ENGINE_ARGS=()
  HUGEGRAPH_ARGS=()
  local file="" mode="" option="" value="" files="" file_set=false files_set=false inline=false
  local conf_mode=""
  while (("$#")); do
    option=${1%%=*}
    inline=false
    if [[ "$1" == *=* ]]; then
      inline=true
      value=${1#*=}
    fi
    case "$option" in
      -g | --graph | -s | --schema | -h | -i | --host | -p | --port | --username | --password | --token | --protocol | \
      --pd-peers | --pd-token | --meta-endpoints | --direct | --route-type | --cluster | --graphspace | --create-graph | \
      --trust-store-file | --trust-store-password | --clear-all-data | --clear-timeout | \
      --incremental-mode | --failure-mode | --batch-insert-threads | --single-insert-threads | \
      --max-conn | --max-conn-per-route | --batch-size | --parallel-count | --parser-threads | \
      --start-file | --end-file | --scatter-sources | --cdc-flush-interval | --cdc-sink-parallelism | \
      --max-read-errors | --max-parse-errors | --max-insert-errors | --max-read-lines | \
      --timeout | --shutdown-timeout | --retry-times | --retry-interval | --check-vertex | \
      --print-progress | --dry-run | --test-mode | --use-prefilter | --short-id | --vertex-edge-limit | \
      --sink-type | --vertex-partitions | --edge-partitions | --vertex-table-name | --edge-table-name | \
      --hbase-zk-quorum | --hbase-zk-port | --hbase-zk-parent | --restore | --backend | --serializer | \
      --scheduler-type | --batch-failure-fallback)
        # LoadOptions uses arity=1 even for booleans; ShortIdConfig lists also consume one value.
        if [[ "$inline" == true ]]; then
          shift
        else
          if (( $# < 2 )); then
            echo "Missing value for $option" >&2
            return 2
          fi
          value=$2
          shift 2
        fi
        HUGEGRAPH_ARGS+=("$option" "$value")
        ;;
      -help | --help)
        if [[ "$inline" == true ]]; then
          echo "Option $option does not take a value" >&2
          return 2
        fi
        HUGEGRAPH_ARGS+=("$option")
        shift
        ;;
      -f | --file | --deploy-mode)
        if [[ "$inline" == true ]]; then
          shift
        else
          if (( $# < 2 )); then
            echo "Missing value for $option" >&2
            return 2
          fi
          value=$2
          shift 2
        fi
        if [[ "$option" == '--file' || "$option" == '-f' ]]; then
          file=$value
          file_set=true
        else
          mode=$value
          ENGINE_ARGS+=("$option" "$value")
        fi
        ;;
      --files)
        if [[ "$inline" == true ]]; then
          shift
        else
          if (( $# < 2 )); then
            echo "Missing value for $option" >&2
            return 2
          fi
          value=$2
          shift 2
        fi
        if [[ -z "$files" ]]; then
          files=$value
        elif [[ -n "$value" ]]; then
          files+=",$value"
        fi
        files_set=true
        ;;
      --archives | --class | --conf | -c | --driver-class-path | --driver-cores | --driver-java-options | \
      --driver-library-path | --driver-memory | --executor-cores | --executor-memory | --jars | \
      --keytab | --kill | --master | --remote | --name | --num-executors | --packages | --exclude-packages | \
      --principal | --properties-file | --proxy-user | --py-files | --queue | --repositories | --status | \
      --total-executor-cores)
        # Consume Spark's value with its option so Loader-looking values stay engine arguments.
        if [[ "$inline" == true ]]; then
          ENGINE_ARGS+=("$1")
          shift
        else
          if (( $# < 2 )); then
            echo "Missing value for $option" >&2
            return 2
          fi
          value=$2
          ENGINE_ARGS+=("$1" "$2")
          shift 2
        fi
        if [[ "$option" == --conf || ( "$option" == -c && "$inline" == false ) ]]; then
          case "$value" in
            spark.submit.deployMode=*) conf_mode=${value#*=} ;;
          esac
        fi
        ;;
      *)
        ENGINE_ARGS+=("$1")
        shift
        ;;
    esac
  done

  # Spark's explicit --deploy-mode takes precedence over its --conf property.
  mode=${mode:-$conf_mode}
  if [[ "$file_set" == true ]]; then
    if [ "$mode" = 'cluster' ]; then
      if [[ "$file" == *#* || "$file" == *,* ]]; then
        echo "Cluster --file must be a local mapping path without commas or # fragments" >&2
        return 2
      fi
      HUGEGRAPH_ARGS+=(--file "${file##*/}")
      if [[ -z "$files" ]]; then
        files=$file
      else
        files+=",$file"
      fi
      files_set=true
    else
      HUGEGRAPH_ARGS+=(--file "$file")
    fi
  fi

  # Spark assigns --files on each occurrence, so emit one list containing every resource.
  if [[ "$files_set" == true ]]; then
    ENGINE_ARGS+=(--files "$files")
  fi

  # Retain the existing string interface for other engine launchers.
  ENGINE_PARAMS="${ENGINE_ARGS[*]}"
  HUGEGRAPH_PARAMS="${HUGEGRAPH_ARGS[*]}"
}
