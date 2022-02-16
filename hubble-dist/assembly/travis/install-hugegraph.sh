#!/bin/bash

set -ev

SERVER_VERSION=$1

"$TRAVIS_DIR"/download-hugegraph.sh "$SERVER_VERSION"
"$TRAVIS_DIR"/hugegraph-server1/install-hugegraph.sh "$SERVER_VERSION"
"$TRAVIS_DIR"/hugegraph-server2/install-hugegraph.sh "$SERVER_VERSION"
