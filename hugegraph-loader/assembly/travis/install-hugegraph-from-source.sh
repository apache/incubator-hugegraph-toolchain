#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with this
# work for additional information regarding copyright ownership. The ASF
# licenses this file to You under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
set -ev

# Validate input parameters
if [[ $# -ne 1 ]]; then
    echo "Usage: $0 <commit_id>"
    echo "Must input an existing commit id of hugegraph server"
    exit 1
fi

COMMIT_ID=$1
HUGEGRAPH_GIT_URL="https://github.com/apache/hugegraph.git"
WORK_DIR=$(pwd)
CLONE_TIMEOUT=300

echo "Cloning HugeGraph repository with commit: ${COMMIT_ID}..."

# Clone with timeout and error handling
timeout ${CLONE_TIMEOUT} git clone --depth 150 ${HUGEGRAPH_GIT_URL} hugegraph || {
    echo "Failed to clone HugeGraph repository"
    exit 1
}

cd hugegraph || {
    echo "Failed to enter hugegraph directory"
    exit 1
}

echo "Checking out commit: ${COMMIT_ID}..."
git checkout "${COMMIT_ID}" || {
    echo "Failed to checkout commit ${COMMIT_ID}"
    cd ${WORK_DIR}
    exit 1
}

echo "Building HugeGraph server..."
mvn package -DskipTests -Dmaven.javadoc.skip=true -ntp || {
    echo "Failed to build HugeGraph"
    cd ${WORK_DIR}
    exit 1
}

cd hugegraph-server || {
    echo "Failed to find hugegraph-server directory"
    cd ${WORK_DIR}
    exit 1
}

# Move tar.gz files to parent directory
mv apache-hugegraph-*.tar.gz ../../ || {
    echo "Failed to find or move HugeGraph distribution"
    cd ${WORK_DIR}
    exit 1
}

cd ${WORK_DIR}
rm -rf hugegraph

echo "Extracting HugeGraph distribution..."
tar zxf apache-hugegraph-*.tar.gz || {
    echo "Failed to extract HugeGraph distribution"
    exit 1
}

# Setup HTTPS server directory
HTTPS_SERVER_DIR="hugegraph_https"
mkdir -p ${HTTPS_SERVER_DIR}
cp -r apache-hugegraph-*/. ${HTTPS_SERVER_DIR}

# Start HTTP server
HTTP_SERVER_DIR=$(find . -maxdepth 1 -type d -name "apache-hugegraph-*" | head -1)

if [ -z "${HTTP_SERVER_DIR}" ] || [ ! -d "${HTTP_SERVER_DIR}" ]; then
    echo "Failed to find HTTP server directory"
    exit 1
fi

cd "${HTTP_SERVER_DIR}" || {
    echo "Failed to enter HTTP server directory"
    exit 1
}

echo "Configuring and starting HTTP HugeGraph server..."

# Configure HTTP server
sed -i 's|gremlin.graph=org.apache.hugegraph.HugeFactory|gremlin.graph=org.apache.hugegraph.auth.HugeFactoryAuthProxy|' conf/graphs/hugegraph.properties
sed -i 's|#auth.authenticator=.*|auth.authenticator=org.apache.hugegraph.auth.StandardAuthenticator|' conf/rest-server.properties
sed -i 's|#auth.admin_pa=.*|auth.admin_pa=pa|' conf/rest-server.properties

# Initialize and start server
echo -e "pa" | bin/init-store.sh || {
    echo "Failed to initialize HugeGraph store"
    exit 1
}

bin/start-hugegraph.sh || {
    echo "Failed to start HTTP HugeGraph server"
    exit 1
}

echo "Waiting for HTTP server to start..."
sleep 10

# Start HTTPS server
cd ${WORK_DIR}/${HTTPS_SERVER_DIR} || {
    echo "Failed to enter HTTPS server directory"
    exit 1
}

echo "Configuring HTTPS HugeGraph server..."

REST_SERVER_CONFIG="conf/rest-server.properties"
GREMLIN_SERVER_CONFIG="conf/gremlin-server.yaml"

# Check if config files exist
if [ ! -f "${REST_SERVER_CONFIG}" ]; then
    echo "REST server configuration not found"
    exit 1
fi

if [ ! -f "${GREMLIN_SERVER_CONFIG}" ]; then
    echo "Gremlin server configuration not found"
    exit 1
fi

# Configure HTTPS server
sed -i "s?http://127.0.0.1:8080?https://127.0.0.1:8443?g" "${REST_SERVER_CONFIG}"
sed -i "s/rpc.server_port=8091/rpc.server_port=8092/g" "${REST_SERVER_CONFIG}"
sed -i "s/#port: 8182/port: 8282/g" "${GREMLIN_SERVER_CONFIG}"
echo "gremlinserver.url=http://127.0.0.1:8282" >> ${REST_SERVER_CONFIG}

# Configure authentication for HTTPS
sed -i 's|gremlin.graph=org.apache.hugegraph.HugeFactory|gremlin.graph=org.apache.hugegraph.auth.HugeFactoryAuthProxy|' conf/graphs/hugegraph.properties
sed -i 's|#auth.authenticator=.*|auth.authenticator=org.apache.hugegraph.auth.StandardAuthenticator|' conf/rest-server.properties
sed -i 's|#auth.admin_pa=.*|auth.admin_pa=pa|' conf/rest-server.properties

echo "Initializing and starting HTTPS HugeGraph server..."
echo -e "pa" | bin/init-store.sh || {
    echo "Failed to initialize HTTPS HugeGraph store"
    exit 1
}

bin/start-hugegraph.sh || {
    echo "Failed to start HTTPS HugeGraph server"
    exit 1
}

echo "HTTPS server started"
cd ${WORK_DIR}

echo "HugeGraph installation completed successfully"

