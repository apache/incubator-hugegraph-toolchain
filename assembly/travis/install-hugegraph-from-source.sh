#!/bin/bash

set -ev

if [[ $# -ne 1 ]]; then
    echo "Must pass commit id of hugegraph repo"
    exit 1
fi

BRANCH_ID=$1
HUGEGRAPH_GIT_URL="https://github.com/starhugegraph/hugegraph.git"

git clone --depth 100 ${HUGEGRAPH_GIT_URL}
cd hugegraph
git checkout -b ${BRANCH_ID}
mvn package -DskipTests
mv hugegraph-*.tar.gz ../
cd ../
rm -rf hugegraph
tar -zxvf hugegraph-*.tar.gz

HTTPS_SERVER_DIR="hugegraph_https"
mkdir ${HTTPS_SERVER_DIR}
cp -r hugegraph-*/. ${HTTPS_SERVER_DIR}
cd hugegraph-*/
# start HugeGraphServer with http protocol
bin/init-store.sh || exit 1
bin/start-hugegraph.sh || exit 1

cd ../${HTTPS_SERVER_DIR}
REST_SERVER_CONFIG="conf/rest-server.properties"
GREMLIN_SERVER_CONFIG="conf/gremlin-server.yaml"
sed -i "s?http://127.0.0.1:8080?https://127.0.0.1:8443?g" "$REST_SERVER_CONFIG"
sed -i "s/#port: 8182/port: 8282/g" "$GREMLIN_SERVER_CONFIG"
echo "gremlinserver.url=http://127.0.0.1:8282" >> ${REST_SERVER_CONFIG}

# start HugeGraphServer with https protocol
bin/init-store.sh
bin/start-hugegraph.sh
cd ../
