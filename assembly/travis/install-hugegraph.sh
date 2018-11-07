#!/bin/bash

set -ev

if [ $# -ne 1 ]; then
    echo "Must pass base branch name of pull request"
    exit 1
fi

CLIENT_BRANCH=$1
HUGEGRAPH_BRANCH=$CLIENT_BRANCH

HUGEGRAPH_GIT_URL="https://github.com/hugegraph/hugegraph.git"

git clone $HUGEGRAPH_GIT_URL || exit 1

cd hugegraph

git checkout $HUGEGRAPH_BRANCH || exit 1

mvn package -DskipTests || exit 1

mv hugegraph-*.tar.gz ../

cd ../

rm -rf hugegraph

tar -zxvf hugegraph-*.tar.gz

cd hugegraph-*

bin/init-store.sh || exit 1

bin/start-hugegraph.sh || exit 1
