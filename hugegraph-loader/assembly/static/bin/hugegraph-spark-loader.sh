#!/bin/bash

PARAMS=""
while (( "$#" )); do
  case "$1" in
    -m|--master)
      MASTER=$2
      shift 2
      ;;

    -n|--name)
      APP_NAME=$2
      shift 2
      ;;

    -e|--deploy-mode)
      DEPLOY_MODE=$2
      shift 2
      ;;

    -c|--conf)
      SPARK_CONFIG=${SPARK_CONFIG}" --conf "$2
      shift 2
      ;;

    --) # end argument parsing
      shift
      break
      ;;

    *) # preserve positional arguments
      PARAMS="$PARAMS $1"
      shift
      ;;

  esac
done

if [ -z ${MASTER} ] || [ -z ${DEPLOY_MODE} ]; then
  echo "Error: The following options are required:
  [-e | --deploy-mode], [-m | --master]"
  usage
  exit 0
fi

BIN_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_DIR=$(dirname ${BIN_DIR})
LIB_DIR=${APP_DIR}/lib

assemblyJarName=$(find ${LIB_DIR} -name hugegraph-loader*.jar)

DEFAULT_APP_NAME="hugegraph-spark-loader"
APP_NAME=${APP_NAME:-$DEFAULT_APP_NAME}

CMD="${SPARK_HOME}/bin/spark-submit
    --name ${APP_NAME}  \
    --master ${MASTER}  \
    --deploy-mode ${DEPLOY_MODE} \
    --class com.baidu.hugegraph.loader.spark.HugeGraphSparkLoader \
    ${SPARK_CONFIG}
    --jars $(echo ${LIB_DIR}/*.jar | tr ' ' ',') ${assemblyJarName} ${PARAMS}"

echo ${CMD}
exec ${CMD}
