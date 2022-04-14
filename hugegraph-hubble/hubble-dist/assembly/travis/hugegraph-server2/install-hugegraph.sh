#!/bin/bash

set -ev

SERVER_CONFIG_DIR=$(dirname "$0")
SERVER_VERSION=$1
SERVER_PARENT_DIR="hugegraph-server2"
SERVER_DIR="${SERVER_PARENT_DIR}"/hugegraph-$SERVER_VERSION

mkdir ${SERVER_PARENT_DIR}
TAR=$(echo hugegraph-*.tar.gz)
tar -zxvf "$TAR" -C "${SERVER_PARENT_DIR}" >/dev/null 2>&1

cp "${SERVER_CONFIG_DIR}"/gremlin-server.yaml "${SERVER_DIR}"/conf
cp "${SERVER_CONFIG_DIR}"/rest-server.properties "${SERVER_DIR}"/conf
cp "${SERVER_CONFIG_DIR}"/graphs/hugegraph3.properties "${SERVER_DIR}"/conf/graphs


cd "${SERVER_DIR}" && pwd

echo -e "pa" | bin/init-store.sh || exit 1
bin/start-hugegraph.sh || exit 1
