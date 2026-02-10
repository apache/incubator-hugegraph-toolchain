#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with this
# work for additional information regarding copyright ownership. The ASF
# licenses this file to You under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
set -ev

# Validate environment
if ! command -v wget &> /dev/null; then
    echo "wget not found, attempting to install..."
    sudo apt-get update && sudo apt-get install -y wget
fi

HADOOP_VERSION="2.8.5"
HADOOP_HOME="/usr/local/hadoop"
HADOOP_DOWNLOAD_URL="http://archive.apache.org/dist/hadoop/common/hadoop-${HADOOP_VERSION}/hadoop-${HADOOP_VERSION}.tar.gz"
HADOOP_TAR="hadoop-${HADOOP_VERSION}.tar.gz"

echo "Downloading Hadoop ${HADOOP_VERSION}..."
sudo wget --quiet -O /tmp/${HADOOP_TAR} ${HADOOP_DOWNLOAD_URL} || {
    echo "Failed to download Hadoop from ${HADOOP_DOWNLOAD_URL}"
    exit 1
}

echo "Extracting Hadoop..."
sudo tar -zxf /tmp/${HADOOP_TAR} -C /usr/local || {
    echo "Failed to extract Hadoop"
    exit 1
}

cd /usr/local
sudo mv hadoop-${HADOOP_VERSION} hadoop
cd ${HADOOP_HOME}

echo "Setting up Hadoop environment..."
echo "export HADOOP_HOME=${HADOOP_HOME}" >> ~/.bashrc
echo "export HADOOP_COMMON_LIB_NATIVE_DIR=\$HADOOP_HOME/lib/native" >> ~/.bashrc
echo "export PATH=\$PATH:\$HADOOP_HOME/bin:\$HADOOP_HOME/sbin" >> ~/.bashrc

source ~/.bashrc

echo "Creating Hadoop configuration files..."
mkdir -p /opt/hdfs/name /opt/hdfs/data
sudo chown -R $(whoami) /opt/hdfs

tee etc/hadoop/core-site.xml <<EOF
<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://localhost:8020</value>
    </property>
</configuration>
EOF

tee etc/hadoop/hdfs-site.xml <<EOF
<configuration>
    <property>
        <name>dfs.namenode.name.dir</name>
        <value>/opt/hdfs/name</value>
    </property>
    <property>
        <name>dfs.datanode.data.dir</name>
        <value>/opt/hdfs/data</value>
    </property>
    <property>
        <name>dfs.permissions.superusergroup</name>
        <value>hadoop</value>
    </property>
    <property>
        <name>dfs.support.append</name>
        <value>true</value>
    </property>
</configuration>
EOF

echo "Formatting HDFS namenode..."
bin/hdfs namenode -format -force || {
    echo "Failed to format HDFS namenode"
    exit 1
}

echo "Starting Hadoop services..."
sbin/hadoop-daemon.sh start namenode || {
    echo "Failed to start namenode"
    exit 1
}

sbin/hadoop-daemon.sh start datanode || {
    echo "Failed to start datanode"
    exit 1
}

echo "Waiting for services to start..."
sleep 5

echo "Checking Hadoop status..."
jps || {
    echo "Warning: jps command failed, but services may still be running"
}

echo "Hadoop installation completed successfully"

