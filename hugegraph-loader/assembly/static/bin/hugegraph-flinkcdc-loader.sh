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
BIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR=$(dirname "${BIN_DIR}")
LIB_DIR=${APP_DIR}/lib
ASSEMBLY_JAR_NAME=$(find "${LIB_DIR}" -name 'hugegraph-loader*.jar')

# get hugegraph_params and engine_params
source "$BIN_DIR"/get-params.sh
get_params "$@"
echo "engine_params: $ENGINE_PARAMS"
echo "hugegraph_params: $HUGEGRAPH_PARAMS"

CMD=${FLINK_HOME}/bin/flink run \
    "${ENGINE_PARAMS}" \
    -c org.apache.hugegraph.loader.flink.HugeGraphFlinkCDCLoader \
    "${ASSEMBLY_JAR_NAME}" "${HUGEGRAPH_PARAMS}"

echo "${CMD}"
exec "${CMD}"
