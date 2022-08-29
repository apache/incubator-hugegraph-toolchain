#!/bin/bash

BIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR=$(dirname ${BIN_DIR})
LIB_DIR=${APP_DIR}/lib
ASSEMBLY_JAR_NAME=$(find ${LIB_DIR} -name hugegraph-loader*.jar)

# get hugegraph_params and engine_params
source "$BIN_DIR"/get-params.sh
get_params $*
echo "engine_params: $ENGINE_PARAMS"
echo "hugegraph_params: $HUGEGRAPH_PARAMS"

CMD=${FLINK_HOME}/bin/flink run \
  ${ENGINE_PARAMS} \
  -c com.baidu.hugegraph.loader.flink.HugeGraphFlinkCDCLoader \
  ${ASSEMBLY_JAR_NAME} ${HUGEGRAPH_PARAMS}

echo ${CMD}
exec ${CMD}
