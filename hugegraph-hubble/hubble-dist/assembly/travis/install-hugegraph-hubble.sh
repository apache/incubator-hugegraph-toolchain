#!/bin/bash

set -ev

TRAVIS_DIR=$(dirname "$0")

echo "$TRAVIS_DIR"
pwd && mvn -e -X clean package -DskipTests

cp "${TRAVIS_DIR}"/jacocoagent.jar hugegraph-hubble-*/lib || exit 1
cp "${TRAVIS_DIR}"/start-hubble.sh hugegraph-hubble-*/bin || exit 1

hugegraph-hubble-*/bin/start-hubble.sh
