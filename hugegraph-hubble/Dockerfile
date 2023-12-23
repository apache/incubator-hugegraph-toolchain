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

FROM maven:3.9.0-eclipse-temurin-11 AS build

RUN set -x \
    && apt-get -q update \
    && apt-get install -y nodejs npm \
    && npm install --global yarn \
    && apt-get clean

COPY . /pkg
WORKDIR /pkg

RUN set -x \
    && cd /pkg/hugegraph-hubble/ \
    && mvn package -e -B -ntp -DskipTests -Dmaven.javadoc.skip=true


FROM openjdk:11-slim

COPY --from=build /pkg/hugegraph-hubble/apache-hugegraph-hubble-incubating-*/ /hubble
WORKDIR /hubble/

RUN set -x \
    && apt-get -q update \
    && apt-get install -y curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*
    

EXPOSE 8088
ENTRYPOINT ["./bin/start-hubble.sh", "-f true"]
