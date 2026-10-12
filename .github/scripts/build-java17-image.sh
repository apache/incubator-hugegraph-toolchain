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
set -euo pipefail

if [[ $# -ne 6 || ( "$1" != loader && "$1" != hubble ) ]]; then
    echo "Usage: $0 <loader|hubble> <server SHA> <SDK Maven repository> <server archive> <archive SHA256> <evidence directory>" >&2
    exit 1
fi
module=$1
server_commit=$2
repository=$3
archive=$4
archive_sha=$5
evidence=$(mkdir -p "$6" && cd "$6" && pwd)
root=$(git rev-parse --show-toplevel)
work=$(mktemp -d "${RUNNER_TEMP:-/tmp}/java17-image-build.XXXXXX")
phase="prepare build inputs"
phase_log="$evidence/manifest.json"
report_failure() {
    local status=$?
    printf 'Java 17 image build failed during %s (exit %s); evidence: %s\n' \
        "$phase" "$status" "$phase_log" >&2
    exit "$status"
}
trap report_failure ERR
mkdir -p "$work/builder/repository/org/apache"
phase="verify candidate SDK"
phase_log="$evidence/sdk-verification.log"
python3 "$root/.github/scripts/verify_candidate_image_sdk.py" "$repository" \
    > "$evidence/sdk-verification.log" 2>&1
phase="prepare candidate provenance"
phase_log="$evidence/prepare.log"
python3 "$root/.github/scripts/java17_image.py" prepare \
    "$module" "$server_commit" "$repository" "$archive" "$archive_sha" "$work" "$evidence" \
    > "$phase_log" 2>&1
cp -a "$repository/org/apache/hugegraph" "$work/builder/repository/org/apache/"
cp "$repository/candidate-sdk-manifest.json" "$work/builder/repository/"
cat > "$work/builder/Dockerfile" <<'DOCKERFILE'
FROM maven:3.9.11-eclipse-temurin-17
ARG CANDIDATE_SOURCE_COMMIT
ENV CANDIDATE_SOURCE_COMMIT=$CANDIDATE_SOURCE_COMMIT
COPY repository /opt/candidate-m2
DOCKERFILE
builder="hugegraph-candidate-maven:$server_commit"
phase="build candidate Maven image"
phase_log="$evidence/builder.log"
docker build --build-arg "CANDIDATE_SOURCE_COMMIT=$server_commit" -t "$builder" "$work/builder" > "$evidence/builder.log" 2>&1
# These are fresh task-owned build inputs; retain the server archive and evidence.
rm -rf -- "$work/builder"
phase="build product image"
phase_log="$evidence/image-build.log"
docker build -f "$root/hugegraph-$module/Dockerfile" \
    --build-arg "MAVEN_BUILDER_IMAGE=$builder" \
    --build-arg 'MAVEN_ARGS=-Dmaven.repo.local=/opt/candidate-m2' \
    --label "org.apache.hugegraph.server.revision=$server_commit" \
    --label "org.opencontainers.image.revision=$(git rev-parse HEAD)" \
    -t "hugegraph-java17-$module:ci" "$root" > "$evidence/image-build.log" 2>&1
