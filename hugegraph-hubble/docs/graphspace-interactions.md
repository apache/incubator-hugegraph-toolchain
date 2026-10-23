# GraphSpace interactions and fixture runner

GraphSpace cards open from the title, date, header whitespace or body. Buttons,
menus and statistics help keep their own actions. The editor waits for the current
space details, ignores responses from previous editing sessions and prevents
multiple writes while a save is pending. Failed creates or saves retain entered
values for retry; failed deletes retain the named confirmation. Search and date
filters return to page one, and deleting the final record on a page recovers a
valid page.

These frontend behaviors preserve existing quotas and creation defaults. They do
not resolve inconsistent configuration supplied by different Server caches;
troubleshoot that separately by inspecting the actual Server responses and
routing.

## Run the stateful browser fixture

Run the Hubble frontend locally with its normal development command, then:

```sh
HUBBLE_URL=http://127.0.0.1:3000 \
  node hugegraph-hubble/hubble-dist/assembly/travis/run_ui_graphspace_crud.js
```

The runner requires the frontend's Playwright dependency and `/usr/bin/chromium`.
It uses stateful mocked APIs for GraphSpace CRUD, failures, delayed writes and
pagination; it never sends CRUD requests to a real backend. Fixture results do not
establish live-cluster acceptance or production performance.

Evidence is written to the system temporary directory's `hubble-graphspace-crud`
folder by default. Set `HUBBLE_UI_EVIDENCE_DIR` to choose another output folder. Set
`HUBBLE_AUDIT_INTERACTIVE=1` to pause after each capture for inspection and press
Enter to continue. Keep generated reports and screenshots outside the source
tree.
