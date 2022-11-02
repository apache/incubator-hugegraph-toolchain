#!/usr/bin/env bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

basepath=$(cd `dirname $0`; pwd)

# check whether there are new third-party dependencies by diff command,
# diff generated 'current-dependencies.txt' file with 'known-dependencies.txt' file.
diff -w -B -U0 <(sort < ${basepath}/known-dependencies.txt) \
<(sort < ${basepath}/current-dependencies.txt > ${basepath}/result.txt

# if has new third-party,the Action will fail and print diff
if [ -s ${basepath}/result.txt ]; then
  cat ${basepath}/result.txt
  exit 1
else
  echo 'All third dependencies is known!'
fi
