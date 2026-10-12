#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
set -euo pipefail
umask 077

if [[ $# -ne 1 ]]; then
    echo 'Usage: tls-fixture.sh <new fixture directory>' >&2
    exit 1
fi
KEYTOOL=${JAVA_HOME:+$JAVA_HOME/bin/}keytool
mkdir "$1"
"$KEYTOOL" -genkeypair -alias hugegraph -keyalg RSA -keysize 2048 -sigalg SHA256withRSA \
    -validity 2 -dname CN=localhost -ext SAN=dns:localhost,ip:127.0.0.1 -storetype PKCS12 \
    -keystore "$1/hugegraph-server.keystore" -storepass hugegraph -keypass hugegraph -noprompt
"$KEYTOOL" -exportcert -alias hugegraph -keystore "$1/hugegraph-server.keystore" \
    -storepass hugegraph -file "$1/server.cer"
"$KEYTOOL" -importcert -alias hugegraph -storetype PKCS12 -keystore "$1/hugegraph.truststore" \
    -storepass hugegraph -file "$1/server.cer" -noprompt
