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
  echo "params: $*"
  ENGINE_PARAMS=""
  HUGEGRAPH_PARAMS=""
  while (("$#")); do
    case "$1" in
      --graph | --schema | --host | --port | --username | --token | --protocol | \
      --trust-store-file | --trust-store-password | --clear-all-data | --clear-timeout | \
      --incremental-mode | --failure-mode | --batch-insert-threads | --single-insert-threads | \
      --max-conn | --max-conn-per-route | --batch-size | --max-parse-errors | --max-insert-errors | \
      --timeout | --shutdown-timeout | --retry-times | --retry-interval | --check-vertex | \
      --print-progress | --dry-run | --sink-type | --vertex-partitions | --edge-partitions | --help )
        HUGEGRAPH_PARAMS="$HUGEGRAPH_PARAMS $1 $2"
        shift 2
        ;;
      --file)
        file=$2
        shift 2
        ;;
      --deploy-mode)
        mode=$2
        ENGINE_PARAMS="$ENGINE_PARAMS $1 $2"
        shift 2
        ;;
      *) # preserve positional arguments
        ENGINE_PARAMS="$ENGINE_PARAMS $1"
        shift
        ;;
    esac
  done

  if [ "$mode" = 'cluster' ];then
    HUGEGRAPH_PARAMS="$HUGEGRAPH_PARAMS --file ${file##*/}"
    ENGINE_PARAMS="$ENGINE_PARAMS --files ${file}"
  else
    HUGEGRAPH_PARAMS="$HUGEGRAPH_PARAMS --file ${file}"
  fi
}
