#!/bin/bash

set -ev

TRAVIS_DIR=$(dirname "$0")
HUBBLE_DIR="hugegraph-hubble"

echo "$TRAVIS_DIR"
pwd && mvn -e -X clean package -DskipTests

cp "${TRAVIS_DIR}"/jacocoagent.jar $HUBBLE_DIR/lib || exit 1
cp "${TRAVIS_DIR}"/start-hubble.sh $HUBBLE_DIR/bin || exit 1

${HUBBLE_DIR}/bin/start-hubble.sh
