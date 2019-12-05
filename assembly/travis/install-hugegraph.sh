#!/bin/bash

set -ev

if [[ $# -ne 1 ]]; then
    echo "Must pass server version of hugegraph"
    exit 1
fi

VERSION=$1
HUGEGRAPH_LINK="https://github.com/hugegraph/hugegraph/releases/download/v${VERSION}/hugegraph-${VERSION}.tar.gz"

wget ${HUGEGRAPH_LINK} || exit 1

tar -zxvf hugegraph-${VERSION}.tar.gz

cd hugegraph-${VERSION}

bin/init-store.sh || exit 1

bin/start-hugegraph.sh || exit 1
