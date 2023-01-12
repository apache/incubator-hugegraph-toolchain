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
function abs_path() {
    SOURCE="${BASH_SOURCE[0]}"
    while [ -h "$SOURCE" ]; do
        DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
        SOURCE="$(readlink "$SOURCE")"
        [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
    done
    echo "$( cd -P "$( dirname "$SOURCE" )" && pwd )"
}

BIN=$(abs_path)
TOP=$(dirname "$BIN")
cd "$BIN" || exit

# TODO: $JAVA is unused now
if [ -n "$JAVA_HOME" ]; then
    JAVA="$JAVA_HOME"/bin/java
else
    JAVA=java
fi

function parse_interval() {
    INTERVAL="${1//\\/} ${2//\\/} ${3//\\/} ${4//\\/} ${5//\\/}"
}

for((i=1;i<=$#;i+=2));
do
    current=$(eval echo '${'"$i"'}')
    case "$current" in
        "--url")
            URL=$(eval echo '${'$((i+1))'}')
            URL_ARG="--url "$URL
            ;;
        "--graph")
            GRAPH=$(eval echo '${'$((i+1))'}')
            GRAPH_ARG="--graph "$GRAPH
            ;;
        "--user")
            USERNAME=$(eval echo '${'$((i+1))'}')
            USERNAME_ARG="--user "$USERNAME
            ;;
        "--password")
            PASSWORD=$(eval echo '${'$((i+1))'}')
            PASSWORD_ARG="--password "$PASSWORD
            ;;
        "--timeout")
            TIMEOUT=$(eval echo '${'$((i+1))'}')
            TIMEOUT_ARG="--timeout "$TIMEOUT
            ;;
        "--interval")
            position=$((i+1))
            INTERVAL=${@:$position:5}
            INTERVAL=${INTERVAL//\\/}
            (( i+=4 ))
            ;;
        "--backup-num")
            NUM=$(eval echo '${'$((i+1))'}')
            ;;
        "--directory"|"-d")
            DIR=$(eval echo '${'$((i+1))'}')
            ;;
        *)
            echo "Invalid argument: $current"
            bash "$BIN"/hugegraph
            exit 1
    esac
done

if [ -z "$DIR" ]; then
    echo "Must provide backup directory"
    exit 1
else
    if [ "${DIR:0:1}" != "/" ]; then
        DIR=$TOP"/"$DIR
    fi
fi

DIR=$(dirname $DIR)/$(basename $DIR)

if [ -z "$GRAPH" ]; then
    GRAPH="hugegraph"
fi
GRAPH_DIR=$DIR/$GRAPH

if [ -d $GRAPH_DIR -o -f $GRAPH_DIR ]; then
    echo "Error: Directory/file $GRAPH already exists in $DIR"
    exit 1
fi

mkdir "$GRAPH_DIR"
if [ $? -ne 0 ]; then
    echo "Failed to create directory $GRAPH_DIR"
    exit 1
fi

if [ -z "$NUM" ]; then
    NUM=3
elif [ ! "$NUM" -gt 0 ] 2>/dev/null ;then
    echo "Number of backups must be positive number."
    exit 1
fi

if [ -z "$INTERVAL" ]; then
    INTERVAL="0 0 * * *"
fi

CRONTAB_JOB="$INTERVAL export JAVA_HOME=$JAVA_HOME && bash $BIN/backup.sh $URL_ARG $GRAPH_ARG $USERNAME_ARG $PASSWORD_ARG $TIMEOUT_ARG backup -t all -d $GRAPH_DIR --backup-num $NUM"

. $BIN/util.sh

crontab_append "$CRONTAB_JOB"
