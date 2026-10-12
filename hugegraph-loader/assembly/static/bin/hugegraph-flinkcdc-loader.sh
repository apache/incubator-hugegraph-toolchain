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
set -euo pipefail

BIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR=$(dirname "$BIN_DIR")
if [[ -z "${FLINK_HOME:-}" || ! -x "$FLINK_HOME/bin/flink" ]]; then
    echo "FLINK_HOME must point to a Flink installation" >&2
    exit 1
fi
shopt -s nullglob
ASSEMBLY_JARS=("$APP_DIR"/lib/*hugegraph-loader*-shaded.jar)
if [[ ${#ASSEMBLY_JARS[@]} -ne 1 ]]; then
    echo "Expected exactly one shaded HugeGraph Loader jar in $APP_DIR/lib" >&2
    exit 1
fi

# Without a separator all arguments belong to Loader. To set Flink options:
# hugegraph-flinkcdc-loader.sh -p 1 -- --file mapping.json --graph hugegraph
ENGINE_PARAMS=()
HUGEGRAPH_PARAMS=("$@")
for arg in "$@"; do
    if [[ "$arg" == "--" ]]; then
        HUGEGRAPH_PARAMS=()
        while [[ "$1" != "--" ]]; do
            ENGINE_PARAMS+=("$1")
            shift
        done
        shift
        HUGEGRAPH_PARAMS=("$@")
        break
    fi
done

# Do not echo arguments: they can contain authentication credentials.
exec "$FLINK_HOME/bin/flink" run ${ENGINE_PARAMS[@]+"${ENGINE_PARAMS[@]}"} \
    -c org.apache.hugegraph.loader.flink.HugeGraphFlinkCDCLoader \
    "${ASSEMBLY_JARS[0]}" ${HUGEGRAPH_PARAMS[@]+"${HUGEGRAPH_PARAMS[@]}"}
