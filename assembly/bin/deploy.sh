#!/bin/bash

if [ $# != 1 ]; then
    echo "USAGE: $0 {hugegraph-version}"
    echo "eg   : $0 version-0.6"
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
    echo "Not found the key '$VERSION.server' in version-map.yaml"
    exit 1
fi
STUDIO_VERSION=`parse_yaml version-map.yaml "${VERSION}" "studio"`
if [ "$STUDIO_VERSION" = "" ]; then
    echo "Not found the key '$VERSION.studio' in version-map.yaml"
    exit 1
fi

# Download and unzip
DOWNLOAD_LINK_PREFIX="http://yq01-sw-hdsserver16.yq01.baidu.com:8080/hadoop-web-proxy/yqns02/hugegraph"
ARCHIVE_FORMAT=".tar.gz"

# HugeGraphServer dir and tar package name
SERVER_DIR="hugegraph-release-${SERVER_VERSION}-SNAPSHOT"
SERVER_TAR=${SERVER_DIR}${ARCHIVE_FORMAT}

# HugeGraphStudio dir and tar package name
STUDIO_DIR="hugestudio-release-${STUDIO_VERSION}-SNAPSHOT"
STUDIO_TAR=${STUDIO_DIR}${ARCHIVE_FORMAT}

ensure_dir_exist $SERVER_DIR $SERVER_TAR ${DOWNLOAD_LINK_PREFIX}"/"${SERVER_TAR}
ensure_dir_exist $STUDIO_DIR $STUDIO_TAR ${DOWNLOAD_LINK_PREFIX}"/hugestudio/"${STUDIO_TAR}

IP=`get_ip`

function config_hugegraph_server() {
    local rest_server_conf="$SERVER_DIR/conf/rest-server.properties"
    local server_url="http://"$IP":8080"

    write_property $rest_server_conf "restserver\.url" $server_url
}

function config_hugegraph_studio() {
    local studio_server_conf="$STUDIO_DIR/conf/hugestudio.properties"

    write_property $studio_server_conf "server\.httpBindAddress" $IP
}

config_hugegraph_server
config_hugegraph_studio

$BIN/$SERVER_DIR/bin/init-store.sh

$BIN/start-all.sh $VERSION
