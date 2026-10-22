# Browser and failure-path checks

The browser scripts import the tracked `browser_probe.py` beside them. They do not read a private runtime copy of another script. Install Python dependencies `requests`, `hpack`, and `playwright`, and provide Chromium through `--chromium` (or `chromium` on `PATH`). The UI fixture must contain the graph spaces and graphs described in the report. These checks use the English UI.

Run from any working directory, using the path to the checked-out scripts:

```sh
python path/to/scripts/browser_comparison.py after \
  --base-url http://127.0.0.1:3000 --output-dir ./hubble-results
python path/to/scripts/profile_canvas.py --phase after \
  --base-url http://127.0.0.1:3000 --output-dir ./hubble-results
python path/to/scripts/final_stack_smoke.py \
  --base-url http://127.0.0.1:3000 --output-dir ./hubble-results
```

`browser_benchmark.py` accepts the same browser options and retains the baseline default of 250 nodes; run it only against the baseline checkout/runtime. The final runtime defaults to 300 nodes and requires `browser_comparison.py after`. `browser_large_properties.py` additionally requires `--fixture PATH` and accepts `--server-url URL`. Supply fixture metadata for the instance being tested; the file contains `space`, `graph`, and original vertex IDs/property SHA256s. Output directories are created as needed. `--timeout-ms` controls browser waits. Browser comparison/baseline diagnostics are retained in JSONL, and any recorded query, application, page, or context-switch failure makes the process exit nonzero. Async submission additionally requires HTTP/application 200 and a nonempty scalar task ID; missing or malformed task data is retained as a failed diagnostic.

`async_result_check.py` is specifically a check of the retained 16MiB task-result limit. It submits a 12,000-vertex query against `--space` and `--graph`, using `--hubble-api-url` for submission and `--server-url` for polling. Configure `--output PATH`, `--timeout SECONDS` (default 300), and `--poll-interval SECONDS` as needed. Exit zero means the task **failed with the expected 16MiB size rejection**; success, cancellation, another failure, transport errors, and timeout exit nonzero. Recorded terminal state and failure details remain available in the output file. Successful full task results are not downloaded.

`preview_import_check.py` requires `--fixture PATH`, accepts the same API/Server URL options, and writes `--output PATH`. It first verifies original property hashes, then imports valid empty arrays as an endpoint/parser control (no elements are written). It then adds `properties_truncated: true` to the valid empty import and requires the dedicated HTTP/application 400 guard error. This root-marker case remains valid, non-mutating import data if the guard is removed. A separate element-marker file deliberately contains no vertex ID or label and must reach the same dedicated error, proving that the element guard runs before entity parsing. Generic parsing, schema, and authentication failures do not count as a pass for either case. The original hashes are checked again before recording success.

These script corrections do not replace the historical samples or change their reported performance figures. Lightweight regression tests run fake local HTTP services and browser pages, without requiring HugeGraph services or modifying graph data:

```sh
python -m unittest discover -s path/to/scripts -p test_evidence_scripts.py -v
```

The two real-browser negative checks require Playwright and Chromium; the test reports an explicit skip when they are unavailable.
