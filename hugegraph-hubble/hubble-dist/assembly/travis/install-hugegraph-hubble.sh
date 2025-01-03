#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
export LANG=zh_CN.UTF-8
set -ev

TRAVIS_DIR=$(dirname "$0")
HUBBLE_DIR=apache-hugegraph-hubble-*

echo "$TRAVIS_DIR"
pwd && mvn -e -X clean package -DskipTests -ntp

if [[ ! -e "${TRAVIS_DIR}/jacocoagent.jar" ]]; then
  wget -P "${TRAVIS_DIR}" https://github.com/apache/hugegraph-doc/raw/binary-1.0/dist/server/jacocoagent.jar
fi

cp -v "${TRAVIS_DIR}"/jacocoagent.jar $HUBBLE_DIR/lib || exit 1
cp -v "${TRAVIS_DIR}"/start-hubble.sh $HUBBLE_DIR/bin || exit 1

${HUBBLE_DIR}/bin/start-hubble.sh
