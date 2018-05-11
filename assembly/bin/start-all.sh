#!/bin/bash

if [ $# != 1 ]; then
    echo "USAGE: $0 ${hugegraph-version}"
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

TOP=`abs_path`
cd $TOP

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

# Download and unzip
DOWNLOAD_LINK_PREFIX="http://yq01-sw-hdsserver16.yq01.baidu.com:8080/hadoop-web-proxy/yqns02/hugegraph"
SERVER_DIR_NAME="hugegraph-release-{version}-SNAPSHOT"
STUDIO_DIR_NAME="hugestudio-release-{version}-SNAPSHOT"
ARCHIVE_FORMAT=".tar.gz"

# HugeGraphServer dir and tar package name
SERVER_DIR=${SERVER_DIR_NAME/"{version}"/$SERVER_VERSION}
SERVER_TAR=${SERVER_DIR}${ARCHIVE_FORMAT}

# HugeGraphStudio dir and tar package name
STUDIO_DIR=${STUDIO_DIR_NAME/"{version}"/$STUDIO_VERSION}
STUDIO_TAR=${STUDIO_DIR}${ARCHIVE_FORMAT}

ensure_dir_exist $SERVER_DIR $SERVER_TAR ${DOWNLOAD_LINK_PREFIX}"/"${SERVER_TAR}
ensure_dir_exist $STUDIO_DIR $STUDIO_TAR ${DOWNLOAD_LINK_PREFIX}"/hugestudio/"${STUDIO_TAR}

IP=`get_ip`

function start_hugegraph_server() {
    local rest_server_conf="conf/rest-server.properties"
    local server_url="http://"$IP":8080"

    write_property $rest_server_conf "restserver\.url" $server_url

    bin/start-hugegraph.sh
    if [ $? -ne 0 ]; then
        echo "Failed to start HugeGraphServer, please check the logs under '$TOP/$SERVER_DIR/logs' for details"
        exit 1
    fi
}

function start_hugegraph_studio() {
    local studio_server_conf="conf/hugestudio.properties"

    write_property $studio_server_conf "server\.httpBindAddress" $IP

    local server_host=$IP
    local server_port=`read_property "conf/hugestudio.properties" "server.httpPort"`
    local server_url="http://${server_host}:${server_port}"
    local start_timeout_s=20

    echo "Starting HugeGraphStudio..."
    bin/hugestudio.sh >/dev/null 2>&1 &

    pid="$!"
    trap '$TOP/stop-all.sh; exit' SIGHUP SIGINT SIGQUIT SIGTERM

    wait_for_startup 'HugeStudio' "$server_url" $start_timeout_s || {
        echo "Failed to start HugeGraphStudio, please check the logs under '$TOP/$STUDIO_DIR/logs' for details"
        bin/stop-hugegraph.sh
        exit 1
    }
}

cd $SERVER_DIR
start_hugegraph_server

cd ../$STUDIO_DIR
start_hugegraph_studio

echo "[OK] Started HugeGraphServer and HugeGraphStudio"
