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
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
    echo "Usage: $0 <apache-hugegraph-hubble-*.tar.gz> [server-url]" >&2
    exit 1
fi

tarball=$1
server_url=${2:-http://127.0.0.1:8080}
hubble_url=${HUBBLE_URL:-http://127.0.0.1:8088}
graph_name=${HUGEGRAPH_GRAPH:-hugegraph}
graphspace=${HUGEGRAPH_GRAPHSPACE:-DEFAULT}
evidence_dir=${HUBBLE_694_EVIDENCE_DIR:-.workflow/hubble-v2-issue-694/evidence}
script_dir=$(cd "$(dirname "$0")" && pwd)
python_bin=${PYTHON:-python3}
node_bin=${NODE:-node}
username=${HUBBLE_USERNAME:-admin}
password=${HUBBLE_PASSWORD:-pa}

if [[ ! -f "${tarball}" ]]; then
    echo "Hubble tarball not found: ${tarball}" >&2
    exit 1
fi

mkdir -p "${evidence_dir}/ui"
runtime_work=$(mktemp -d "${TMPDIR:-/tmp}/hubble-694-runtime-XXXXXX")
cleanup() {
    hubble_home=$(find "${runtime_work}" -maxdepth 1 -type d \
        -name 'apache-hugegraph-hubble-*' | head -n 1)
    if [[ -n "${hubble_home}" && -x "${hubble_home}/bin/stop-hubble.sh" ]]; then
        "${hubble_home}/bin/stop-hubble.sh" >/dev/null 2>&1 || true
    fi
    mkdir -p "${evidence_dir}/logs"
    if [[ -f "${runtime_work}/hubble-live-smoke.log" ]]; then
        cp "${runtime_work}/hubble-live-smoke.log" \
            "${evidence_dir}/logs/hubble-live-smoke.log"
    fi
    if [[ -n "${hubble_home}" && \
          -f "${hubble_home}/logs/hugegraph-hubble.log" ]]; then
        cp "${hubble_home}/logs/hugegraph-hubble.log" \
            "${evidence_dir}/logs/hugegraph-hubble.log"
    fi
    rm -rf "${runtime_work}"
}
trap cleanup EXIT

"${python_bin}" "${script_dir}/run_live_hubble_smoke.py" "${tarball}" \
    --server-url "${server_url}" \
    --hubble-url "${hubble_url}" \
    --graph "${graph_name}" \
    --graphspace "${graphspace}" \
    --loader-flow \
    --analysis-boundary \
    --work-dir "${runtime_work}" \
    --keep-work-dir \
    --leave-running \
    --json-output "${evidence_dir}/live-hubble-smoke.json"

HUBBLE_USERNAME="${username}" HUBBLE_PASSWORD="${password}" \
"${node_bin}" "${script_dir}/run_ui_full_acceptance.js" \
    --hubble-url "${hubble_url}" \
    --output-dir "${evidence_dir}/ui" \
    --json-output "${evidence_dir}/ui-full-acceptance.json"

cat > "${evidence_dir}/manifest.json" <<JSON
{
  "status": "passed",
  "hubbleUrl": "${hubble_url}",
  "serverUrl": "${server_url}",
  "graphspace": "${graphspace}",
  "graph": "${graph_name}",
  "runtime": "${evidence_dir}/live-hubble-smoke.json",
  "ui": "${evidence_dir}/ui-full-acceptance.json"
}
JSON

echo "Hubble issue #694 smoke passed; evidence: ${evidence_dir}"
