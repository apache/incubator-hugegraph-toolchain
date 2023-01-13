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

print_usage() {
    echo "  usage: start-hubble.sh [options]"
    echo "  options: "
    echo "  -d,--debug      Start program in debug mode"
    echo "  -h,--help       Display help information"
}

java_env_check

if [[ ! -d ${LOG_PATH} ]]; then
    mkdir "${LOG_PATH}"
fi

class_path="."
for jar in "${LIB_PATH}"/*.jar; do
    [[ -e "$jar" ]] || break
    class_path=${class_path}:${jar}
done

java_opts="-Xms512m"
java_debug_opts=""
while [[ $# -gt 0 ]]; do
    case $1 in
        --help|-help|-h)
        print_usage
        exit 0
        ;;
        --debug|-d)
        java_debug_opts=" -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n"
        ;;
    esac
    shift
done

if [[ -f ${PID_FILE} ]] ; then
    pid=$(cat "${PID_FILE}")
    if kill -0 "${pid}" > /dev/null 2>&1; then
        echo "HugeGraphHubble is running as process ${pid}, please stop it first!"
        exit 1
    else
        rm "${PID_FILE}"
    fi
fi

main_class="org.apache.hugegraph.HugeGraphHubble"
args=${CONF_PATH}/hugegraph-hubble.properties
log=${LOG_PATH}/hugegraph-hubble.log

echo -n "starting HugeGraphHubble "
nohup nice -n 0 java -server ${java_opts} ${java_debug_opts} -Dhubble.home.path="${HOME_PATH}" \
  -cp ${class_path} ${main_class} ${args} > ${log} 2>&1 < /dev/null &
pid=$!
echo ${pid} > "${PID_FILE}"

# wait hubble start
timeout_s=30
server_host=$(read_property "${CONF_PATH}"/hugegraph-hubble.properties hubble.host)
server_port=$(read_property "${CONF_PATH}"/hugegraph-hubble.properties hubble.port)
server_url="http://${server_host}:${server_port}/actuator/health"

wait_for_startup "${server_url}" ${timeout_s} || {
    cat "${log}"
    exit 1
}
echo "logging to ${log}, please check it"
