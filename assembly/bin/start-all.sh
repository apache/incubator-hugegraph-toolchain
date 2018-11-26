#!/bin/bash

VERSION=""
INSTALL_PATH=""

function print_usage() {
    echo "USAGE: $0 -v {hugegraph-version} -p {install-path}"
    echo "eg   : $0 -v 0.8 -p ."
}

while getopts "v:p:" arg; do
    case ${arg} in
        v) VERSION="$OPTARG" ;;
        p) INSTALL_PATH="$OPTARG" ;;
        ?) print_usage && exit 1 ;;
    esac
done

if [[ "$VERSION" = "" || "$INSTALL_PATH" = "" ]]; then
    print_usage
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
. ${BIN}/util.sh

`ensure_path_writable ${INSTALL_PATH}`

# Convert to absolute path
INSTALL_PATH="$(cd ${INSTALL_PATH} && pwd)"

cd ${BIN}

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

SERVER_DIR="${INSTALL_PATH}/hugegraph-${SERVER_VERSION}"
STUDIO_DIR="${INSTALL_PATH}/hugegraph-studio-${STUDIO_VERSION}"

if [ ! -d "${SERVER_DIR}" ]; then
    echo "The server dir ${SERVER_DIR} doesn't exist"
    exit 1
fi

if [ ! -d "${STUDIO_DIR}" ]; then
    echo "The studio dir ${STUDIO_DIR} doesn't exist"
    exit 1
fi

function start_hugegraph_server() {
    $SERVER_DIR/bin/start-hugegraph.sh
    if [ $? -ne 0 ]; then
        echo "Failed to start HugeGraphServer, please check the logs under '$SERVER_DIR/logs' for details"
        exit 1
    fi
}

function start_hugegraph_studio() {
    # TODO: Let hugegraph-studio.sh can execute in any directory instead of $STUDIO_DIR
    cd $STUDIO_DIR

    local server_host=`read_property "conf/hugegraph-studio.properties" "studio.server.host"`
    local server_port=`read_property "conf/hugegraph-studio.properties" "studio.server.port"`
    local server_url="http://${server_host}:${server_port}"
    local start_timeout_s=20

    echo "Starting HugeGraphStudio..."
    bin/hugegraph-studio.sh >/dev/null 2>&1 &

    pid="$!"
    trap '$BIN/stop-all.sh; exit' SIGHUP SIGINT SIGQUIT SIGTERM

    wait_for_startup 'HugeGraphStudio' "$server_url" $start_timeout_s || {
        echo "Failed to start HugeGraphStudio, please check the logs under '$STUDIO_DIR/logs' for details"
        $SERVER_DIR/bin/stop-hugegraph.sh
        exit 1
    }
    cd ..
}

start_hugegraph_server
start_hugegraph_studio

echo "[OK] Started HugeGraphServer and HugeGraphStudio"
