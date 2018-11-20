#!/bin/bash

function abs_path() {
    SOURCE="${BASH_SOURCE[0]}"
    while [ -h "$SOURCE" ]; do
        DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
        SOURCE="$(readlink "$SOURCE")"
        [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
    done
    echo "$( cd -P "$( dirname "$SOURCE" )" && pwd )"
}

BIN=`abs_path`
cd $BIN

# do backup
bash $BIN/hugegraph ${*:1:$(($#-2))}"/hugegraph-backup-`date +%y%m%d%H%M`/"

DIR=`eval echo '${'$(($#-2))'}'`
NUM=`eval echo '${'$#'}'`
# delete redundant backups if needed
for i in `ls -lt $DIR | grep -v "total" | grep "hugegraph-backup-" | awk -v awkNum="$NUM" '{if(NR>awkNum){print $9}}'`
do
    rm -fr "$DIR/$i"
done
