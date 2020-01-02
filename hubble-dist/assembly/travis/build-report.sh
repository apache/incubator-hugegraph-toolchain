#!/bin/bash

set -ev

TRAVIS_DIR=`dirname $0`

cd hubble-be
mvn jacoco:dump@pull-test-data -Dapp.host=localhost -Dapp.port=36320 -Dskip.dump=false
cd ../
java -jar ${TRAVIS_DIR}/jacococli.jar report hubble-be/target/jacoco-it.exec \
     --classfiles hubble-be/target/classes/com/baidu/hugegraph --xml report.xml
