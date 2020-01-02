#!/bin/bash

set -ev

SERVER_CONFIG_DIR=`dirname $0`
SERVER_PARENT_DIR="hugegraph-server1"
SERVER_DIR=${SERVER_PARENT_DIR}/hugegraph-*

mkdir ${SERVER_PARENT_DIR}
tar -zxvf hugegraph-*.tar.gz -C ${SERVER_PARENT_DIR} >/dev/null 2>&1

cp ${SERVER_CONFIG_DIR}/gremlin-server.yaml ${SERVER_DIR}/conf
cp ${SERVER_CONFIG_DIR}/rest-server.properties ${SERVER_DIR}/conf
cp ${SERVER_CONFIG_DIR}/hugegraph1.properties ${SERVER_DIR}/conf
cp ${SERVER_CONFIG_DIR}/hugegraph2.properties ${SERVER_DIR}/conf

cd ${SERVER_DIR}

bin/init-store.sh || exit 1
bin/start-hugegraph.sh || exit 1
