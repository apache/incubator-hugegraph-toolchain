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

# Identity includes every build input. Never restore an archive from another source.
FIXTURE_REPOSITORY=${FIXTURE_REPOSITORY:-${SERVER_REPOSITORY:-apache/hugegraph}}
FIXTURE_COMMIT=${FIXTURE_COMMIT:-${1:-}}
FIXTURE_REF=${FIXTURE_REF:-$FIXTURE_COMMIT}
FIXTURE_JAVA=${FIXTURE_JAVA:-11}
FIXTURE_RELEASE_VERSION=${FIXTURE_RELEASE_VERSION:-1.7.0}
FIXTURE_DIR=${FIXTURE_DIR:-${RUNNER_TEMP:-${TMPDIR:-/tmp}}/hugegraph-fixture}
FIXTURE_HELPER=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/manifest.py
FIXTURE_BUILD_INPUTS=${FIXTURE_BUILD_INPUTS:-$(python3 "$FIXTURE_HELPER" inputs)}
FIXTURE_PLATFORM=${FIXTURE_PLATFORM:-$(uname -sm)}
FIXTURE_EXPORT_CANDIDATE_SDK=${FIXTURE_EXPORT_CANDIDATE_SDK:-false}
if [[ "$FIXTURE_JAVA" == 17 ]]; then
    FIXTURE_BUILD_COMMAND=candidate-sdk-install
else
    FIXTURE_BUILD_COMMAND="asf-release-$FIXTURE_RELEASE_VERSION"
fi
FIXTURE_CONFIG="$FIXTURE_BUILD_COMMAND;schema=2;inputs=$FIXTURE_BUILD_INPUTS;platform=$FIXTURE_PLATFORM"
if [[ "$FIXTURE_EXPORT_CANDIDATE_SDK" == true ]]; then
    [[ "$FIXTURE_JAVA" == 17 ]] || { echo 'SDK fixture export requires JDK 17' >&2; exit 1; }
    sdk_identity=$(python3 "$FIXTURE_HELPER" sdk-identity)
elif [[ "$FIXTURE_EXPORT_CANDIDATE_SDK" != false ]]; then
    echo 'Invalid SDK fixture export mode' >&2
    exit 1
fi
export FIXTURE_EXPORT_CANDIDATE_SDK
export FIXTURE_REPOSITORY FIXTURE_COMMIT FIXTURE_JAVA FIXTURE_CONFIG FIXTURE_DIR FIXTURE_RELEASE_VERSION
[[ "$FIXTURE_REPOSITORY" =~ ^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$ ]]
[[ "$FIXTURE_COMMIT" =~ ^[0-9a-f]{40}$ ]]
[[ "$FIXTURE_JAVA" == 11 || "$FIXTURE_JAVA" == 17 ]] || {
    echo 'Fixture requires JDK 11 for the official release or JDK 17 for the candidate SDK' >&2
    exit 1
}

if [[ "$FIXTURE_JAVA" == 11 ]]; then
    # Reject caller/source mismatch before restoring a cache or downloading bytes.
    python3 "${FIXTURE_HELPER%/*}/release.py" validate
fi

fixture_validate_java() {
    actual_java=$(java -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*".*/\1/p' | head -1)
    [[ "$actual_java" == "$FIXTURE_JAVA" ]] || { echo 'Fixture JDK mismatch' >&2; return 1; }
}

fixture_verify() { python3 "$FIXTURE_HELPER" verify; }
fixture_build() {
    local cache_key cache_dir source_dir archive candidate_repo='' cache_config="$FIXTURE_CONFIG"
    if [[ "$FIXTURE_EXPORT_CANDIDATE_SDK" == true ]]; then
        cache_config="$cache_config;sdk-export=1;identity=$sdk_identity"
    fi
    cache_key=$(printf '%s\n' "$FIXTURE_REPOSITORY" "$FIXTURE_COMMIT" "$FIXTURE_JAVA" "$cache_config" | shasum -a 256 | cut -d ' ' -f 1)
    cache_dir="${HOME}/hugegraph-fixture-cache/$cache_key"
    mkdir -p "$FIXTURE_DIR"
    if [[ "$FIXTURE_EXPORT_CANDIDATE_SDK" == true && "${FIXTURE_SDK_CACHE_HIT:-}" == true &&
          ! -d "$cache_dir" ]]; then
        echo 'Exact SDK cache hit is missing its expected payload; refusing to rebuild' >&2
        return 1
    fi
    if [[ "$FIXTURE_EXPORT_CANDIDATE_SDK" == true && -e "$cache_dir" ]]; then
        # An SDK cache hit must prove the entire payload. Never hide a broken hit
        # behind another source build or accept a Server-only cache as an SDK hit.
        FIXTURE_DIR="$cache_dir" fixture_verify || return 1
        cp "$cache_dir/server.tar.gz" "$cache_dir/manifest.json" "$FIXTURE_DIR/"
        cp -a "$cache_dir/sdk" "$FIXTURE_DIR/"
        echo 'Reused verified SDK and Server fixture; reactor builds: 0'
        return
    fi
    if [[ -f "$cache_dir/server.tar.gz" && ! -L "$cache_dir/server.tar.gz" &&
          -f "$cache_dir/manifest.json" && ! -L "$cache_dir/manifest.json" ]] &&
       cp "$cache_dir"/server.tar.gz "$cache_dir"/manifest.json "$FIXTURE_DIR/"; then
        if fixture_verify; then return; fi
    fi
    rm -f "$FIXTURE_DIR/server.tar.gz" "$FIXTURE_DIR/manifest.json"
    fixture_validate_java
    if [[ "$FIXTURE_JAVA" == 17 ]]; then
        source_dir="$(mktemp -d "${TMPDIR:-/tmp}/hg-fixture-source.XXXXXX")/source"
        # Reuse the verified SDK build's same-source distribution and isolated repository.
        candidate_repo=$(mktemp -d "${TMPDIR:-/tmp}/hg-fixture-sdk-m2.XXXXXX")
        SERVER_REPOSITORY=$FIXTURE_REPOSITORY SERVER_FETCH_REF=$FIXTURE_REF \
            bash "${FIXTURE_HELPER%/*}/install-candidate-sdk.sh" \
                "$FIXTURE_COMMIT" "$source_dir" "$candidate_repo"
        archive=$(find "$source_dir/hugegraph-server" -maxdepth 1 -name 'apache-hugegraph-*.tar.gz' -print)
        [[ -n "$archive" && -f "$archive" ]]
        cp "$archive" "$FIXTURE_DIR/server.tar.gz"
        if [[ "$FIXTURE_EXPORT_CANDIDATE_SDK" == true ]]; then
            mkdir -p "$FIXTURE_DIR/sdk/org/apache"
            cp -a "$candidate_repo/org/apache/hugegraph" "$FIXTURE_DIR/sdk/org/apache/"
            cp "$candidate_repo/candidate-sdk-manifest.json" "$FIXTURE_DIR/sdk/"
        fi
        export FIXTURE_ARCHIVE_NAME=${archive##*/}
    else
        python3 "${FIXTURE_HELPER%/*}/release.py" download
        export FIXTURE_ARCHIVE_NAME=$(python3 "${FIXTURE_HELPER%/*}/release.py" name)
    fi
    python3 "$FIXTURE_HELPER" create
    fixture_verify
    mkdir -p "$cache_dir"
    cp "$FIXTURE_DIR/server.tar.gz" "$FIXTURE_DIR/manifest.json" "$cache_dir/"
    if [[ "$FIXTURE_EXPORT_CANDIDATE_SDK" == true ]]; then
        cp -a "$FIXTURE_DIR/sdk" "$cache_dir/"
    fi
    if [[ -n "${source_dir:-}" ]]; then rm -rf "$source_dir"; fi
    if [[ -n "$candidate_repo" ]]; then rm -rf "$candidate_repo"; fi
}
fixture_copy_archive() {
    fixture_validate_java
    fixture_verify
    local name
    name=$(python3 "$FIXTURE_HELPER" name)
    cp "$FIXTURE_DIR/server.tar.gz" "./$name"
}
source "${FIXTURE_HELPER%/*}/service-wait.sh"

case "${FIXTURE_MODE:-standalone}" in
    build) fixture_build ;;
    start) fixture_copy_archive ;;
    verify) fixture_verify ;;
    standalone) fixture_build; fixture_copy_archive ;;
    *) echo 'Invalid fixture mode' >&2; exit 1 ;;
esac
