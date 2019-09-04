#!/bin/bash

HOME_PATH=`dirname $0`
HOME_PATH=`cd ${HOME_PATH}/.. && pwd`
BIN_PATH=${HOME_PATH}/bin
PID_FILE=${BIN_PATH}/pid

if [[ -f ${PID_FILE} ]]; then
    pid=`cat ${PID_FILE}`
    if kill -0 ${pid} > /dev/null 2>&1; then
        kill -9 ${pid}
        echo "stopped HugeGraphHubble"
    else
        echo "process not exist"
    fi
    rm ${PID_FILE}
else
    echo "HugeGraphHubble not running"
fi
