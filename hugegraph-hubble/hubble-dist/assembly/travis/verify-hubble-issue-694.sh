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

# The shared Python guard runs before runtime setup or successful evidence writes.
# Pass URLs in the environment; invalid input emits only a fixed field diagnostic
# and replaces an existing success manifest with a sanitized validation failure.
HUBBLE_URL="${hubble_url}" HUBBLE_SMOKE_SERVER_URL="${server_url}" \
"${python_bin}" "${script_dir}/run_live_hubble_smoke.py" "${tarball}" \
    --validate-urls --json-output "${evidence_dir}/manifest.json"

mkdir -p "${evidence_dir}/ui"
rm -f "${evidence_dir}/manifest.json"
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

HUBBLE_USERNAME="${username}" HUBBLE_PASSWORD="${password}" \
HUBBLE_URL="${hubble_url}" HUBBLE_SMOKE_SERVER_URL="${server_url}" \
"${python_bin}" "${script_dir}/run_live_hubble_smoke.py" "${tarball}" \
    --graph "${graph_name}" \
    --graphspace "${graphspace}" \
    --loader-flow \
    --analysis-boundary \
    --work-dir "${runtime_work}" \
    --keep-work-dir \
    --leave-running \
    --json-output "${evidence_dir}/live-hubble-smoke.json"

rm -f "${evidence_dir}/ui-full-acceptance.json"
ui_exit=0
if HUBBLE_USERNAME="${username}" HUBBLE_PASSWORD="${password}" \
HUBBLE_URL="${hubble_url}" \
"${node_bin}" "${script_dir}/run_ui_full_acceptance.js" \
    --graphspace "${graphspace}" \
    --graph "${graph_name}" \
    --live-report "${evidence_dir}/live-hubble-smoke.json" \
    --output-dir "${evidence_dir}/ui" \
    --json-output "${evidence_dir}/ui-full-acceptance.json"; then
    :
else
    ui_exit=$?
fi

"${node_bin}" - "${evidence_dir}" "${ui_exit}" "${script_dir}/run_ui_full_acceptance.js" <<'JS'
const fs = require('node:fs');
const path = require('node:path');
const {acceptanceSummary} = require(process.argv[4]);
const directory = process.argv[2];
const runtime = JSON.parse(fs.readFileSync(path.join(directory, 'live-hubble-smoke.json'), 'utf8'));
const uiExit = Number(process.argv[3]);
let ui;
try { ui = JSON.parse(fs.readFileSync(path.join(directory, 'ui-full-acceptance.json'), 'utf8')); }
catch { ui = {status: 'failed', error: 'UI acceptance did not write a valid current report'}; }
if (!ui || typeof ui !== 'object' || Array.isArray(ui)) ui = {status: 'failed'};
const status = runtime.status !== 'passed' || !['passed', 'failed', 'needs input'].includes(ui.status)
  || (uiExit !== 0 && ui.status === 'passed') ? 'failed' : ui.status;
const graphRequestedChecks = Array.isArray(ui.graphRequestedChecks) ? ui.graphRequestedChecks : [];
fs.writeFileSync(path.join(directory, 'manifest.json'), JSON.stringify({
  status,
  hubbleUrl: runtime.hubble_url, serverUrl: runtime.server_url,
  graphspace: runtime.graphspace, graph: runtime.graph,
  graphRequestedChecks,
  ...acceptanceSummary(status, ui.graphChecksComplete === true, graphRequestedChecks),
  runtime: path.join(directory, 'live-hubble-smoke.json'), ui: path.join(directory, 'ui-full-acceptance.json')
}, null, 2) + '\n');
if (uiExit === 0 && status !== 'passed') process.exitCode = 1;
JS

if [[ "${ui_exit}" -ne 0 ]]; then
    exit "${ui_exit}"
fi

echo "Hubble issue #694 requested smoke checks passed; evidence: ${evidence_dir}"
