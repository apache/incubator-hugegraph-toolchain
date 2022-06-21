#!/bin/bash

BIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR=$(dirname ${BIN_DIR})
LIB_DIR=${APP_DIR}/lib
assemblyJarName=$(find ${LIB_DIR} -name hugegraph-loader*.jar)

# get hugegraph_params and engine_params
source "$BIN_DIR"/get_params.sh
get_params $*
echo "engine_params: $engine_params"
echo "hugegraph_params: $hugegraph_params"

CMD=${FLINK_HOME}/bin/flink run \
  ${engine_params} \
  -c com.baidu.hugegraph.loader.flink.HugeGraphFlinkCDCLoader \
  ${assemblyJarName} ${hugegraph_params}

echo ${CMD}
exec ${CMD}
