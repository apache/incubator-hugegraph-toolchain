#!/bin/bash

set -ev

TRAVIS_DIR="hubble-dist/assembly/travis"
pwd

"$TRAVIS_DIR"/install-hugegraph-hubble.sh

behave hubble-be/src/test/python

"$TRAVIS_DIR"/build-report.sh
