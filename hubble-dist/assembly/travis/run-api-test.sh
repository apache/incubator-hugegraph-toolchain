#!/bin/bash

set -ev

TRAVIS_DIR=`dirname $0`

behave hubble-be/src/test/python
