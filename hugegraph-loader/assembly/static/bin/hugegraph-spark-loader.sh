#!/bin/bash

BIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR=$(dirname ${BIN_DIR})
LIB_DIR=${APP_DIR}/lib

# get hugegraph_params and engine_params
source "$BIN_DIR"/get-params.sh
get_params $*
echo "engine_params: $ENGINE_PARAMS"
echo "hugegraph_params: $HUGEGRAPH_PARAMS"

ASSEMBLY_JAR_NAME=$(find ${LIB_DIR} -name hugegraph-loader*.jar)


DEFAULT_APP_NAME="hugegraph-spark-loader"
APP_NAME=${APP_NAME:-$DEFAULT_APP_NAME}

CMD="${SPARK_HOME}/bin/spark-submit
    --class com.baidu.hugegraph.loader.spark.HugeGraphSparkLoader \
    ${ENGINE_PARAMS}
    --jars $(echo ${LIB_DIR}/*.jar | tr ' ' ',') ${ASSEMBLY_JAR_NAME} ${HUGEGRAPH_PARAMS}"

echo ${CMD}
exec ${CMD}
