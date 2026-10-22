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

"""Derive serialized query-call costs from the retained observations."""

import bisect
import gzip
import json
from pathlib import Path
import statistics


ROOT = Path(__file__).resolve().parents[1]


def read_jsonl(name):
    with gzip.open(ROOT / name, "rt") as stream:
        return [json.loads(line) for line in stream]


def main():
    traces = sorted(read_jsonl("trace.jsonl.gz"), key=lambda item: item["ts"])
    timestamps = [item["ts"] for item in traces]
    samples = read_jsonl("samples.jsonl.gz")
    output = []
    labels = ["hubble_default_vertices", "hubble_vertices_12000",
              "hubble_paths_12000"]
    for label in labels:
        for graph in ["perf_small64", "perf_large8192"]:
            values = []
            for sample in samples:
                if (sample["label"] != label or sample.get("graph") != graph or
                        sample.get("excluded_from_report")):
                    continue
                start = bisect.bisect_left(timestamps, sample["ts"])
                end = bisect.bisect_right(
                    timestamps, sample["ts"] + sample["ms"] / 1000 + .002)
                calls = [item for item in traces[start:end]
                         if item.get("kind") == "server-http" and
                         item.get("path") == "/gremlin" and "ms" in item]
                if not calls:
                    continue
                values.append({
                    "main_ms": calls[0]["ms"],
                    "main_bytes": calls[0]["bytes"],
                    "enrichment_calls": len(calls) - 1,
                    "enrichment_ms_sum": sum(item["ms"] for item in calls[1:]),
                    "enrichment_bytes_sum": sum(item["bytes"] for item in calls[1:]),
                    "hubble_body_bytes": sample["bytes"],
                    "e2e_ms": sample["ms"],
                })
            if values:
                output.append({
                    "label": label, "graph": graph, "n": len(values),
                    **{key: statistics.median(item[key] for item in values)
                       for key in values[0]},
                })
    (ROOT / "call-breakdown.json").write_text(
        json.dumps(output, indent=2) + "\n")


if __name__ == "__main__":
    main()
