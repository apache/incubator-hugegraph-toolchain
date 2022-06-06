#!/bin/bash

BIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR=$(dirname ${BIN_DIR})
LIB_DIR=${APP_DIR}/lib

# get hugegraph_params and engine_params
source "$BIN_DIR"/get_params.sh
get_params $*
echo "engine_params: $engine_params"
echo "hugegraph_params: $hugegraph_params"

assemblyJarName=$(find ${LIB_DIR} -name hugegraph-loader*.jar)

DEFAULT_APP_NAME="hugegraph-spark-loader"
APP_NAME=${APP_NAME:-$DEFAULT_APP_NAME}

CMD="${SPARK_HOME}/bin/spark-submit
    --class com.baidu.hugegraph.loader.spark.HugeGraphSparkLoader \
    ${engine_params}
    --jars $(echo ${LIB_DIR}/*.jar | tr ' ' ',') ${assemblyJarName} ${hugegraph_params}"

echo ${CMD}
exec ${CMD}
