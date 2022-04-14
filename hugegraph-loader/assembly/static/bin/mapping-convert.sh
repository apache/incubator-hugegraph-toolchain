#!/bin/bash

export LANG=zh_CN.UTF-8
set -e

HOME_PATH=`dirname $0`
HOME_PATH=`cd ${HOME_PATH}/.. && pwd`
cd ${HOME_PATH}

BIN_PATH=${HOME_PATH}/bin
CONF_PATH=${HOME_PATH}/conf
LIB_PATH=${HOME_PATH}/lib
LOG_PATH=${HOME_PATH}/logs

if [[ -z "$1" ]]; then
    echo "usage: $0 file"
    echo "Convert old mapping file from old version to 2.0"
    exit 1
fi

if [[ ! -d ${LOG_PATH} ]]; then
    mkdir ${LOG_PATH}
fi

class_path="."
for jar in `ls ${LIB_PATH}/*.jar`; do
    class_path=${class_path}:${jar}
done

args=$1
main_class="com.baidu.hugegraph.loader.MappingConverter"
exec java -Dlog4j.configurationFile=${CONF_PATH}/log4j2.xml \
-cp ${class_path} ${main_class} ${args}
