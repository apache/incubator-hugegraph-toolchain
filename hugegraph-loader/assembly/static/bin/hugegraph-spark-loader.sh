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

# get hugegraph_params and engine_params
source "$BIN_DIR"/get-params.sh
get_params "$@"
echo "engine_params: $ENGINE_PARAMS"
echo "hugegraph_params: $HUGEGRAPH_PARAMS"

ASSEMBLY_JAR_NAME=$(find "${LIB_DIR}" -name 'hugegraph-loader*.jar')

DEFAULT_APP_NAME="hugegraph-spark-loader"
APP_NAME=${APP_NAME:-$DEFAULT_APP_NAME}

CMD="${SPARK_HOME}/bin/spark-submit
    --class org.apache.hugegraph.loader.spark.HugeGraphSparkLoader ${ENGINE_PARAMS} \
    --jars $(echo "${LIB_DIR}"/*.jar | tr ' ' ',') ${ASSEMBLY_JAR_NAME} ${HUGEGRAPH_PARAMS}"

echo "${CMD}"
exec "${CMD}"
