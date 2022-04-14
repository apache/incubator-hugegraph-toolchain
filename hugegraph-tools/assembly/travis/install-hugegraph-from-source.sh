#!/bin/bash
set -ev
if [[ $# -ne 1 ]]; then
    echo "Must pass commit id of hugegraph repo"
    exit 1
fi

COMMIT_ID=$1
HUGEGRAPH_GIT_URL="https://github.com/hugegraph/hugegraph.git"
GIT_DIR=hugegraph

# download code and compile
git clone --depth 100 ${HUGEGRAPH_GIT_URL}
cd "${GIT_DIR}"
git checkout ${COMMIT_ID}
mvn package -DskipTests

TAR=$(echo hugegraph-*.tar.gz)
tar -zxvf "${TAR}" -C ../
cd ../
rm -rf "${GIT_DIR}"

HTTP_SERVER_DIR=$(echo hugegraph-*)
HTTPS_SERVER_DIR="hugegraph_https"

cp -r "${HTTP_SERVER_DIR}" "${HTTPS_SERVER_DIR}"

# config auth options just for http server (must keep '/.')
cp -rf "${TRAVIS_DIR}"/conf/. "${HTTP_SERVER_DIR}"/conf/

# start HugeGraphServer with http protocol
cd "${HTTP_SERVER_DIR}"
echo -e "pa" | bin/init-store.sh || exit 1
bin/start-hugegraph.sh || exit 1

# config options for https server
cd ../"${HTTPS_SERVER_DIR}"
REST_SERVER_CONFIG="conf/rest-server.properties"
GREMLIN_SERVER_CONFIG="conf/gremlin-server.yaml"
sed -i "s?http://127.0.0.1:8080?https://127.0.0.1:8443?g" "$REST_SERVER_CONFIG"
sed -i "s/#port: 8182/port: 8282/g" "$GREMLIN_SERVER_CONFIG"
echo "gremlinserver.url=http://127.0.0.1:8282" >> ${REST_SERVER_CONFIG}
# start HugeGraphServer with https protocol
bin/init-store.sh
bin/start-hugegraph.sh
cd ../
