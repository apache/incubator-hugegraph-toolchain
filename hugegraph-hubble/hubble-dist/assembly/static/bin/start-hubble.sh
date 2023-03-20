#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
export LANG=zh_CN.UTF-8
set -e

HOME_PATH=$(dirname "$0")
HOME_PATH=$(cd "${HOME_PATH}"/.. && pwd)
cd "${HOME_PATH}"

BIN_PATH=${HOME_PATH}/bin
CONF_PATH=${HOME_PATH}/conf
LIB_PATH=${HOME_PATH}/lib
LOG_PATH=${HOME_PATH}/logs
PID_FILE=${BIN_PATH}/pid

. "${BIN_PATH}"/common_functions

java_env_check

if [[ ! -d ${LOG_PATH} ]]; then
    mkdir "${LOG_PATH}"
fi

class_path="."
for jar in "${LIB_PATH}"/*.jar; do
    [[ -e "$jar" ]] || break
    class_path=${class_path}:${jar}
done

JAVA_OPTS="-Xms512m"
JAVA_DEBUG_OPTS=""
DAEMON="true"

while getopts "f:d" arg; do
    case ${arg} in
        f) DAEMON="$OPTARG" ;;
        d) JAVA_DEBUG_OPTS=" -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n" ;;
        ?) echo "USAGE: $0 [-f true|false] [-d] " && exit 1 ;;
    esac
done

if [[ -f ${PID_FILE} ]] ; then
    PID=$(cat "${PID_FILE}")
    if kill -0 "${PID}" > /dev/null 2>&1; then
        echo "HugeGraphHubble is running as process ${PID}, please stop it first!"
        exit 1
    else
        rm "${PID_FILE}"
    fi
fi

MAIN_CLASS="org.apache.hugegraph.HugeGraphHubble"
ARGS=${CONF_PATH}/hugegraph-hubble.properties
LOG=${LOG_PATH}/hugegraph-hubble.log

if [[ $DAEMON == "true" ]]; then
    echo "Starting Hubble in daemon mode..."
    nice -n 0 java -server ${JAVA_OPTS} ${JAVA_DEBUG_OPTS} -Dhubble.home.path="${HOME_PATH}" \
  -cp ${class_path} ${MAIN_CLASS} ${ARGS} > ${LOG} 2>&1 < /dev/null &
else
    echo "Starting Hubble in foreground mode..."
    nice -n 0 java -server ${JAVA_OPTS} ${JAVA_DEBUG_OPTS} -Dhubble.home.path="${HOME_PATH}" \
  -cp ${class_path} ${MAIN_CLASS} ${ARGS} > ${LOG} 2>&1 < /dev/null
fi

PID=$!
echo ${PID} > "${PID_FILE}"

# wait hubble start
TIMEOUT_S=30
SERVER_HOST=$(read_property "${CONF_PATH}"/hugegraph-hubble.properties hubble.host)
SERVER_PORT=$(read_property "${CONF_PATH}"/hugegraph-hubble.properties hubble.port)
SERVER_URL="http://${SERVER_HOST}:${SERVER_PORT}/actuator/health"

wait_for_startup "${SERVER_URL}" ${TIMEOUT_S} || {
    cat "${LOG}"
    exit 1
}
echo "logging to ${LOG}, please check it"
