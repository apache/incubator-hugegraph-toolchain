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
        DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
        SOURCE="$(readlink "$SOURCE")"
        [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
    done
    echo "$(cd -P "$(dirname "$SOURCE")" && pwd)"
}

BIN=$(abs_path)
cd "$BIN" || exit

# do backup
bash "$BIN"/hugegraph "${*:1:$(($# - 2))}/hugegraph-backup-$(date +%y%m%d%H%M)/"

DIR=$(eval echo '${'$(($# - 2))'}')
NUM=$(eval echo '${'$#'}')
# delete redundant backups if needed
for i in $(ls -lt "$DIR" | grep -v "total" | grep "hugegraph-backup-" | awk -v awkNum="$NUM" '{if(NR>awkNum){print $9}}'); do
    rm -rf "${DIR:?}/$i"
done
