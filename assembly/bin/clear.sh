#!/bin/bash

STOP_PROCESS="false"
if [ $# -gt 1 ]; then
    echo "USAGE: $0 [true|false]"
    echo "The param indicates whether to stop process, default is false"
    exit 1
elif [ $# -eq 1 ]; then
    if [[ $1 != "true" && $1 != "false" ]]; then
        echo "USAGE: $0 [true|false]"
        exit 1
    else
        STOP_PROCESS=$1
    fi
fi

function abs_path() {
    SOURCE="${BASH_SOURCE[0]}"
    while [ -h "$SOURCE" ]; do
        DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
        SOURCE="$(readlink "$SOURCE")"
        [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
    done
    echo "$( cd -P "$( dirname "$SOURCE" )" && pwd )"
}

BIN=`abs_path`
cd $BIN

. util.sh

SERVER_RELEASE_PREFIX="hugegraph-release"
STUDIO_RELEASE_PREFIX="hugestudio-release"

function check_no_process() {
    local p_name=$1
    process_status "$p_name" >/dev/null
    if [ $? -eq 0 ]; then
        echo "Please stop process '$p_name' before clear"
        exit 1
    fi
}

if [ "$STOP_PROCESS" == "true" ]; then
    ./stop-all.sh
else
    # Check process before clear
    check_no_process "HugeStudio"
    check_no_process "HugeGraphServer"
fi

for file in ${SERVER_RELEASE_PREFIX}*; do
    remove_with_prompt "${file}"
done

for file in ${STUDIO_RELEASE_PREFIX}*; do
    remove_with_prompt "${file}"
done
