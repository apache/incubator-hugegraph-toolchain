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

source "$BIN_DIR"/get-params.sh
get_params "$@" || exit $?

# Standalone cluster does not make --files available before this driver's mapping parse.
MASTER="" DEPLOY_MODE="" CONF_MASTER="" CONF_DEPLOY_MODE=""
for ((i=0; i<${#ENGINE_ARGS[@]}; i++)); do
  case "${ENGINE_ARGS[i]}" in
    --master) MASTER=${ENGINE_ARGS[i+1]}; ((i+=1)) ;;
    --master=*) MASTER=${ENGINE_ARGS[i]#*=} ;;
    --deploy-mode) DEPLOY_MODE=${ENGINE_ARGS[i+1]}; ((i+=1)) ;;
    --conf | -c)
      case "${ENGINE_ARGS[i+1]}" in
        spark.master=*) CONF_MASTER=${ENGINE_ARGS[i+1]#*=} ;;
        spark.submit.deployMode=*) CONF_DEPLOY_MODE=${ENGINE_ARGS[i+1]#*=} ;;
      esac
      ((i+=1))
      ;;
    --conf=spark.master=*) CONF_MASTER=${ENGINE_ARGS[i]#--conf=spark.master=} ;;
    --conf=spark.submit.deployMode=*) CONF_DEPLOY_MODE=${ENGINE_ARGS[i]#--conf=spark.submit.deployMode=} ;;
    --archives | --class | --driver-class-path | --driver-cores | --driver-java-options | \
    --driver-library-path | --driver-memory | --executor-cores | --executor-memory | --files | --jars | \
    --keytab | --kill | --remote | --name | --num-executors | --packages | --exclude-packages | \
    --principal | --properties-file | --proxy-user | --py-files | --queue | --repositories | --status | \
    --total-executor-cores) ((i+=1)) ;;
  esac
done
MASTER=${MASTER:-$CONF_MASTER}
DEPLOY_MODE=${DEPLOY_MODE:-$CONF_DEPLOY_MODE}
# Keep mapping localization consistent with the mode actually passed to Spark.
# Configuration files must not silently switch this launcher to a remote driver.
if [[ -z "$DEPLOY_MODE" ]]; then
  DEPLOY_MODE=client
  ENGINE_ARGS+=(--deploy-mode client)
fi
if [[ "$DEPLOY_MODE" == cluster && -z "$MASTER" ]]; then
  echo "Cluster deploy mode requires --master or --conf spark.master on the command line" >&2
  exit 2
fi
if [[ "$DEPLOY_MODE" == cluster && "$MASTER" == spark://* ]]; then
  echo "Standalone cluster deploy mode is not supported; use client deploy mode" >&2
  exit 2
fi

shopt -s nullglob
ASSEMBLY_JARS=("${LIB_DIR}"/apache-hugegraph-loader-*-shaded.jar)
if [ "${#ASSEMBLY_JARS[@]}" -ne 1 ]; then
  echo "Expected exactly one shaded HugeGraph Loader jar in ${LIB_DIR}" >&2
  exit 1
fi
ASSEMBLY_JAR=${ASSEMBLY_JARS[0]}
# The shaded application already contains its runtime dependencies.
CMD=("${SPARK_HOME}/bin/spark-submit" --class org.apache.hugegraph.loader.spark.HugeGraphSparkLoader)
CMD+=("${ENGINE_ARGS[@]}" "$ASSEMBLY_JAR" "${HUGEGRAPH_ARGS[@]}")
exec "${CMD[@]}"
