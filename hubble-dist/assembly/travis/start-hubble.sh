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
PID_FILE=${BIN_PATH}/pid

. ${BIN_PATH}/common_functions

print_usage() {
    echo "  usage: start-hubble.sh [options]"
    echo "  options: "
    echo "  -d,--debug      Start program in debug mode"
    echo "  -h,--help       Display help information"
}

java_env_check

if [[ ! -d ${LOG_PATH} ]]; then
    mkdir ${LOG_PATH}
fi

class_path="."
for jar in `ls ${LIB_PATH}/*.jar`; do
    class_path=${class_path}:${jar}
done

java_opts="-Xms512m"
while [[ $# -gt 0 ]]; do
    case $1 in
        --help|-help|-h)
        print_usage
        exit 0
        ;;
        --debug|-d)
        java_opts="$java_opts -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y"
        ;;
    esac
    shift
done

if [[ -f ${PID_FILE} ]] ; then
    pid=`cat ${PID_FILE}`
    if kill -0 ${pid} > /dev/null 2>&1; then
        echo "HugeGraphHubble is running as process ${pid}, please stop it first!"
        exit 1
    else
        rm ${PID_FILE}
    fi
fi

# for collecting codecov
agent_opts="-javaagent:${LIB_PATH}/jacocoagent.jar=includes=*,port=36320,destfile=jacoco-it.exec,output=tcpserver"
main_class="com.baidu.hugegraph.HugeGraphHubble"
args=${CONF_PATH}/hugegraph-hubble.properties
log=${LOG_PATH}/hugegraph-hubble.log

echo -n "starting HugeGraphHubble"
nohup nice -n 0 java -server ${java_opts} ${agent_opts} -cp ${class_path} ${main_class} ${args} > ${log} 2>&1 < /dev/null &
pid=$!
echo pid > ${PID_FILE}

# wait hubble start
timeout_s=30
server_host=`read_property ${CONF_PATH}/hugegraph-hubble.properties server.host`
server_port=`read_property ${CONF_PATH}/hugegraph-hubble.properties server.port`
server_url="http://${server_host}:${server_port}/actuator/health"

wait_for_startup ${server_url} ${timeout_s} || {
    cat ${log}
    exit 1
}
echo "logging to ${log}"
