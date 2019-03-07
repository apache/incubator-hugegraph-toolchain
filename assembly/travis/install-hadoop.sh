#!/bin/bash

set -ev

TRAVIS_DIR=`dirname $0`
HADOOP_DOWNLOAD_ADDRESS="http://archive.apache.org/dist/hadoop/core"
HADOOP_VERSION="2.8.0"
HADOOP_PACKAGE="hadoop-${HADOOP_VERSION}"
HADOOP_TAR="${HADOOP_PACKAGE}.tar.gz"

# install ssh
ssh-keygen -t rsa -P '' -f ~/.ssh/id_dsa
cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
# check ssh
ssh localhost || exit 1

# download hadoop
if [[ ! -f $HOME/downloads/${HADOOP_TAR} ]]; then
  sudo wget -q -O $HOME/downloads/${HADOOP_TAR} ${HADOOP_DOWNLOAD_ADDRESS}/${HADOOP_VERSION}/${HADOOP_TAR}
fi

# decompress hadoop
sudo cp $HOME/downloads/${HADOOP_TAR} ${HADOOP_TAR} && tar xzf ${HADOOP_TAR}

# config hadoop
sudo cp -f ${TRAVIS_DIR}/core-site.xml ${HADOOP_PACKAGE}/etc/hadoop/
sudo cp -f ${TRAVIS_DIR}/hdfs-site.xml ${HADOOP_PACKAGE}/etc/hadoop/

# format hdfs
sudo ${HADOOP_PACKAGE}/bin/hadoop namenode -format

# start hadoop service
sudo ${HADOOP_PACKAGE}/sbin/start-dfs.sh

jps
