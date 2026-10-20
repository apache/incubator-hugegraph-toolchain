# syntax=docker/dockerfile:1
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

# Node is checked against hubble-fe/.node-version by the shared build entry.
ARG NODE_IMAGE=node:24.22.0-bookworm-slim
ARG MAVEN_BUILDER_IMAGE=maven:3.9.11-eclipse-temurin-17
FROM --platform=$BUILDPLATFORM ${NODE_IMAGE} AS node-runtime

FROM node-runtime AS frontend
WORKDIR /frontend
RUN corepack enable
# Persist the pinned pnpm download before source-only layers change. The build
# stage also copies this Corepack directory, so it must not live only in a mount.
COPY hugegraph-hubble/hubble-fe/package.json /frontend/package.json
RUN pnpm --version
COPY hugegraph-hubble/hubble-fe/ /frontend/
COPY hugegraph-hubble/hubble-dist/assembly/travis/build-frontend.mjs /build-frontend.mjs
# Cache only downloaded packages. A frozen install validates each source layer;
# host node_modules and build output are excluded by .dockerignore.
RUN --mount=type=cache,id=hubble-pnpm-store,target=/root/.local/share/pnpm/store,sharing=locked \
    node /build-frontend.mjs --frontend /frontend

FROM --platform=$BUILDPLATFORM ${MAVEN_BUILDER_IMAGE} AS backend
ARG MAVEN_ARGS
ENV MAVEN_OPTS="-Xmx3g -XX:MaxMetaspaceSize=768m"
WORKDIR /pkg
RUN apt-get -q update \
    && apt-get install --no-install-recommends curl git python3 -yq \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

COPY hugegraph-client/assembly/travis/install-candidate-sdk.sh \
     hugegraph-client/assembly/travis/checkout-server.sh \
     hugegraph-client/assembly/travis/verify_candidate_image_sdk.py /pkg/hugegraph-client/assembly/travis/
# Never mount a broad .m2 cache over this same-source repository. The supplied
# builder may already contain its locked SDK; every reuse is hash-verified.
RUN mkdir -p /opt/candidate-m2 \
    && if [ ! -f /opt/candidate-m2/candidate-sdk-manifest.json ]; then \
         SERVER_REPOSITORY=apache/hugegraph \
         bash /pkg/hugegraph-client/assembly/travis/install-candidate-sdk.sh \
           86c05ef9c5d417bd37eab21d4ecd5af82bab17ef /opt/candidate-source /opt/candidate-m2; \
       fi \
    && python3 /pkg/hugegraph-client/assembly/travis/verify_candidate_image_sdk.py /opt/candidate-m2

# Keep FE inputs out of the Java compilation layer. The root reactor still
# needs all module POMs for model discovery, but only these sources are built.
COPY pom.xml .editorconfig LICENSE NOTICE /pkg/
COPY tools/ /pkg/tools/
COPY hugegraph-client/ /pkg/hugegraph-client/
COPY hugegraph-loader/ /pkg/hugegraph-loader/
COPY hugegraph-hubble/pom.xml /pkg/hugegraph-hubble/pom.xml
COPY hugegraph-hubble/hubble-be/ /pkg/hugegraph-hubble/hubble-be/
COPY hugegraph-hubble/hubble-dist/pom.xml /pkg/hugegraph-hubble/hubble-dist/pom.xml
COPY hugegraph-hubble/hubble-dist/assembly/travis/check-java-build-inputs.py \
     /pkg/hugegraph-hubble/hubble-dist/assembly/travis/check-java-build-inputs.py
COPY hugegraph-dist/pom.xml /pkg/hugegraph-dist/pom.xml
COPY hugegraph-tools/pom.xml /pkg/hugegraph-tools/pom.xml
COPY hugegraph-spark-connector/pom.xml /pkg/hugegraph-spark-connector/pom.xml
RUN mvn install $MAVEN_ARGS -Dmaven.repo.local=/opt/candidate-m2 \
      -Dhugegraph.version=1.8.0 -Dsdk.validation.mode=candidate \
      -pl hugegraph-hubble/hubble-be -am -Dloader.distribution.skip=true \
      -Dmaven.javadoc.skip=true -DskipTests -B -ntp \
    && python3 /pkg/hugegraph-client/assembly/travis/verify_candidate_image_sdk.py /opt/candidate-m2

FROM backend AS build
ARG MAVEN_ARGS
ENV MAVEN_OPTS="-Xmx2g -XX:MaxMetaspaceSize=768m"
# Maven uses the exact same Node/pnpm and validates the complete frontend
# manifest; it does not install or rebuild an unchanged frontend stage.
COPY --from=frontend /usr/local/ /usr/local/
COPY --from=frontend /root/.cache/node/corepack/ /root/.cache/node/corepack/
COPY hugegraph-hubble/hubble-fe/ /pkg/hugegraph-hubble/hubble-fe/
COPY --from=frontend /frontend/build/ /pkg/hugegraph-hubble/hubble-fe/build/
COPY --from=frontend /frontend/.cache/frontend-build.json /pkg/hugegraph-hubble/hubble-fe/.cache/frontend-build.json
COPY hugegraph-hubble/hubble-dist/assembly/ /pkg/hugegraph-hubble/hubble-dist/assembly/
COPY hugegraph-hubble/README* /pkg/hugegraph-hubble/
COPY hugegraph-dist/release-docs/ /pkg/hugegraph-dist/release-docs/
RUN mvn package $MAVEN_ARGS -f hugegraph-hubble/hubble-dist/pom.xml \
      -Dmaven.repo.local=/opt/candidate-m2 -Dhugegraph.version=1.8.0 -Dsdk.validation.mode=candidate \
      -B -ntp -DskipTests -Dmaven.javadoc.skip=true \
    && python3 /pkg/hugegraph-client/assembly/travis/verify_candidate_image_sdk.py /opt/candidate-m2 \
    && set -- /pkg/hugegraph-hubble/target/apache-hugegraph-hubble-*.tar.gz \
    && test "$#" -eq 1 \
    && mkdir /hubble-release \
    && tar -xzf "$1" --strip-components=1 -C /hubble-release

FROM eclipse-temurin:17-jre-jammy

RUN apt-get -q update \
    && apt-get install --no-install-recommends curl -yq \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /hubble-release/ /hubble
RUN sed -i \
    -e 's/^server\.host=.*/server.host=0.0.0.0/' \
    -e 's/^dashboard\.address=.*/dashboard.address=/' \
    /hubble/conf/hugegraph-hubble.properties \
    && grep -Fqx 'server.host=0.0.0.0' \
        /hubble/conf/hugegraph-hubble.properties \
    && grep -Fqx 'dashboard.address=' \
        /hubble/conf/hugegraph-hubble.properties
WORKDIR /hubble/

# SECURITY: This is a plain HTTP port. Do not publish it to an untrusted network;
# production/public deployments must put Hubble behind HTTPS and block bypasses.
EXPOSE 8088
ENTRYPOINT ["./bin/start-hubble.sh", "-f"]
