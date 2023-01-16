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
INSTALL_PATH=""

function print_usage() {
    echo "USAGE: $0 -p {install-path}"
    echo "eg   : $0 -p ."
}

while getopts "p:" arg; do
    case ${arg} in
        p) INSTALL_PATH="$OPTARG" ;;
        ?) print_usage && exit 1 ;;
    esac
done

if [ "$INSTALL_PATH" = "" ]; then
    print_usage
    exit 1
fi

# Check path exist
if [ ! -d "${INSTALL_PATH}" ]; then
    echo "Package storage directory '${INSTALL_PATH}' doesn't exist"
fi
# Check for write permission
if [ ! -w "${INSTALL_PATH}" ]; then
    echo "No write permission on directory '${INSTALL_PATH}'"
    exit 1
fi

INSTALL_PATH="$(cd "${INSTALL_PATH}" && pwd)"

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

cd "${BIN}" || exit

SERVER_RELEASE_PREFIX="hugegraph"
STUDIO_RELEASE_PREFIX="hugegraph-studio"

function ensure_no_process() {
    local path=$1
    local prefix=$2

    for file in $path; do
        file=${path}/${file}
        if [[ -d "${file}" && "${file}" =~ ${prefix} ]]; then
            p_name=${file}
            process_status "${p_name}" >/dev/null
            if [ $? -eq 0 ]; then
                echo "Exist process corresponding to the directory '${file}', please stop it before clearing"
                exit 1
            fi
        fi
    done
}

ensure_no_process "${INSTALL_PATH}" ${SERVER_RELEASE_PREFIX}
ensure_no_process "${INSTALL_PATH}" ${STUDIO_RELEASE_PREFIX}

for file in "${INSTALL_PATH}/${SERVER_RELEASE_PREFIX}"*; do
    remove_with_prompt "${file}"
done

for file in "${INSTALL_PATH}/${STUDIO_RELEASE_PREFIX}"*; do
    remove_with_prompt "${file}"
done
