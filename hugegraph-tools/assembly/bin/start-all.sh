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
        DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
        SOURCE="$(readlink "$SOURCE")"
        [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
    done
    echo "$(cd -P "$(dirname "$SOURCE")" && pwd)"
}

BIN=$(abs_path)
. "${BIN}"/util.sh

ensure_path_writable "${INSTALL_PATH}"

# Convert to absolute path
INSTALL_PATH="$(cd "${INSTALL_PATH}" && pwd)"

cd "${BIN}" || exit

# Check input version can be found in version-map.yaml
OPTIONAL_VERSIONS=$(cat version-map.yaml | grep 'version' | awk -F ':' '{print $1}' | xargs)
if [[ ! "$OPTIONAL_VERSIONS" =~ $VERSION ]]; then
    echo "Invalid version '${VERSION}' for hugegraph, the optional values are [$OPTIONAL_VERSIONS]"
    exit 1
fi

# Parse module version from 'version-map.yaml'
SERVER_VERSION=$(parse_yaml version-map.yaml "${VERSION}" "server")
if [ "$SERVER_VERSION" = "" ]; then
    echo "Please check the format and content of file 'version-map.yaml' is normal"
    exit 1
fi
STUDIO_VERSION=$(parse_yaml version-map.yaml "${VERSION}" "studio")
if [ "$STUDIO_VERSION" = "" ]; then
    echo "Please check the format and content of file 'version-map.yaml' is normal"
    exit 1
fi
# Parse Hubble module version
HUBBLE_VERSION=$(parse_yaml version-map.yaml "${VERSION}" "hubble")
if [ "$HUBBLE_VERSION" = "" ]; then
    echo "Please check the format and content of file 'version-map.yaml' is normal"
    exit 1
fi

SERVER_DIR="${INSTALL_PATH}/hugegraph-${SERVER_VERSION}"
STUDIO_DIR="${INSTALL_PATH}/hugegraph-studio-${STUDIO_VERSION}"
HUBBLE_DIR="${INSTALL_PATH}/hugegraph-hubble-${HUBBLE_VERSION}"

if [ ! -d "${SERVER_DIR}" ]; then
    echo "The server dir ${SERVER_DIR} doesn't exist"
    exit 1
fi

if [ ! -d "${STUDIO_DIR}" ]; then
    echo "The studio dir ${STUDIO_DIR} doesn't exist"
    exit 1
fi

if [ ! -d "${HUBBLE_DIR}" ]; then
    echo "The hubble dir ${HUBBLE_DIR} doesn't exist"
    exit 1
fi

function start_hugegraph_server() {
    "$SERVER_DIR"/bin/start-hugegraph.sh
    if [ $? -ne 0 ]; then
        echo "Failed to start HugeGraphServer, please check the logs under '$SERVER_DIR/logs' for details"
        exit 1
    fi
}

# Generic function to start a component (e.g., Hubble, Studio)
# Usage: start_component <name> <dir> <prop_prefix> <stop_on_fail_cmds...>
function start_component() {
    local name="$1"
    local dir="$2"
    local prop_prefix="$3"
    shift 3
    local stop_on_fail_cmds=("$@")

    # The component script may need to be run from its own directory
    # TODO: Modify component scripts to be executable from any directory
    cd "${dir}" || exit

    local prop_file="conf/hugegraph-${prop_prefix}.properties"
    local script_file="bin/hugegraph-${prop_prefix}.sh"

    local server_host
    server_host=$(read_property "${prop_file}" "${prop_prefix}.server.host")
    local server_port
    server_port=$(read_property "${prop_file}" "${prop_prefix}.server.port")
    local server_url="http://${server_host}:${server_port}"
    local start_timeout_s=20

    echo "Starting ${name}..."
    "${script_file}" >/dev/null 2>&1 &

    wait_for_startup "${name}" "${server_url}" "${start_timeout_s}" || {
        echo "Failed to start ${name}, please check the logs under '${dir}/logs' for details"
        for cmd in "${stop_on_fail_cmds[@]}"; do
            eval "${cmd}"
        done
        exit 1
    }
    # Return to the previous directory
    cd -
}

# Set a trap to clean up background processes on script exit
trap '"${BIN}"/stop-all.sh; exit' SIGHUP SIGINT SIGQUIT SIGTERM

start_hugegraph_server

start_component "HugeGraphHubble" "${HUBBLE_DIR}" "hubble" \
                "'${SERVER_DIR}'/bin/stop-hugegraph.sh"

start_component "HugeGraphStudio" "${STUDIO_DIR}" "studio" \
                "'${HUBBLE_DIR}'/bin/stop-hugegraph-hubble.sh" \
                "'${SERVER_DIR}'/bin/stop-hugegraph.sh"

echo "[OK] Started HugeGraphServer, HugeGraphHubble and HugeGraphStudio"