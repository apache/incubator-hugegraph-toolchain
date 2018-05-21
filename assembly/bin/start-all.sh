#!/bin/bash

if [ $# != 1 ]; then
    echo "USAGE: $0 {hugegraph-version}"
    echo "eg   : $0 version-0.5"
    exit 1
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

VERSION=$1

# Check input version can be found in version-map.yaml
OPTIONAL_VERSIONS=`cat version-map.yaml | grep 'version' | awk -F ':' '{print $1}' | xargs`
if [[ ! "$OPTIONAL_VERSIONS" =~ "$VERSION" ]]; then
    echo "Invalid version '${VERSION}' for hugegraph, the optional values are [$OPTIONAL_VERSIONS]"
    exit 1
fi

# Parse module version from 'version-map.yaml'
SERVER_VERSION=`parse_yaml version-map.yaml "${VERSION}" "server"`
if [ "$SERVER_VERSION" = "" ]; then
    echo "Please check the format and content of file 'version-map.yaml' is normal"
    exit 1
fi
STUDIO_VERSION=`parse_yaml version-map.yaml "${VERSION}" "studio"`
if [ "$STUDIO_VERSION" = "" ]; then
    echo "Please check the format and content of file 'version-map.yaml' is normal"
    exit 1
fi

SERVER_DIR="hugegraph-release-${SERVER_VERSION}-SNAPSHOT"
STUDIO_DIR="hugestudio-release-${STUDIO_VERSION}-SNAPSHOT"

if [ ! -d "$SERVER_DIR" ]; then
    echo "The server dir $SERVER_DIR doesn't exist"
    exit 1
fi

if [ ! -d "$STUDIO_DIR" ]; then
    echo "The studio dir $STUDIO_DIR doesn't exist"
    exit 1
fi

function start_hugegraph_server() {
    bin/start-hugegraph.sh
    if [ $? -ne 0 ]; then
        echo "Failed to start HugeGraphServer, please check the logs under '$BIN/$SERVER_DIR/logs' for details"
        exit 1
    fi
}

function start_hugegraph_studio() {
    local server_host=`read_property "conf/hugestudio.properties" "server.httpBindAddress"`
    local server_port=`read_property "conf/hugestudio.properties" "server.httpPort"`
    local server_url="http://${server_host}:${server_port}"
    local start_timeout_s=20

    echo "Starting HugeGraphStudio..."
    bin/hugestudio.sh >/dev/null 2>&1 &

    pid="$!"
    trap '$BIN/stop-all.sh; exit' SIGHUP SIGINT SIGQUIT SIGTERM

    wait_for_startup 'HugeStudio' "$server_url" $start_timeout_s || {
        echo "Failed to start HugeGraphStudio, please check the logs under '$BIN/$STUDIO_DIR/logs' for details"
        bin/stop-hugegraph.sh
        exit 1
    }
}

cd $BIN/$SERVER_DIR
start_hugegraph_server

cd $BIN/$STUDIO_DIR
start_hugegraph_studio

echo "[OK] Started HugeGraphServer and HugeGraphStudio"
