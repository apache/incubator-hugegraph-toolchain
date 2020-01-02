#!/bin/bash

set -ev

TRAVIS_DIR=`dirname $0`

mvn clean package -DskipTests

cp ${TRAVIS_DIR}/jacocoagent.jar hugegraph-hubble-*/lib
cp ${TRAVIS_DIR}/start-hubble.sh hugegraph-hubble-*/bin

cd hugegraph-hubble-*

bin/start-hubble.sh
