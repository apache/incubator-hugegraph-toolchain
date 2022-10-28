#!/bin/bash

set -ev

if [[ $# -ne 1 ]]; then
    echo "Must input an existing commit id of hugegraph server" && exit 1
fi

COMMIT_ID=$1
HUGEGRAPH_GIT_URL="https://github.com/apache/hugegraph.git"

git clone --depth 100 ${HUGEGRAPH_GIT_URL} hugegraph
cd hugegraph
git checkout "${COMMIT_ID}"
mvn package -DskipTests -Dmaven.javadoc.skip=true -ntp
# TODO: lack incubator after apache package release (update it later)
mv hugegraph-*.tar.gz ../
cd ../
rm -rf hugegraph
tar zxf hugegraph-*.tar.gz

HTTPS_SERVER_DIR="hugegraph_https"
mkdir ${HTTPS_SERVER_DIR}
# TODO: lack incubator after apache package release (update it later)
cp -r hugegraph-*/. ${HTTPS_SERVER_DIR}
cd "$(find hugegraph-* | head -1)"
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
