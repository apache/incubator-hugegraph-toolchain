#!/bin/bash

set -ev

EXEC_PATH="hubble-dist/assembly/travis"
pwd

"$EXEC_PATH"/install-hugegraph-hubble.sh

behave hubble-be/src/test/python

"$EXEC_PATH"/build-report.sh
