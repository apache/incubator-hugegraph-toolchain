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

group="hugegraph"
# current repository name
repo="${group}-toolchain"
# release version (input by committer)
release_version=$1
# git release branch (check it carefully)
git_branch="release-${release_version}"

release_version=${release_version:?"Please input the release version behind script"}

work_dir=$(cd "$(dirname "$0")" || exit; pwd)
cd "${work_dir}" || exit
echo "In the work dir: $(pwd)"

# clean old dir then build a new one
rm -rfv dist && mkdir -p dist/apache-${repo}

# step1: package the source code
git archive --format=tar.gz \
  --output="dist/apache-${repo}/apache-${repo}-${release_version}-incubating-src.tar.gz" \
  --prefix=apache-${repo}-"${release_version}"-incubating-src/ "${git_branch}" || exit

# step2: copy the binary file (Optional)
# Note: it's optional for project to generate binary package (skip this step if not need)
cp -v ${group}-dist/target/apache-${repo}-"${release_version}"-incubating-bin.tar.gz \
  dist/apache-${repo} || exit

# step3: sign + hash
##### 3.1 sign in source & binary package
gpg --version 1>/dev/null || exit
cd ./dist/apache-${repo} || exit
for i in *.tar.gz; do
  echo "$i" && gpg --armor --output "$i".asc --detach-sig "$i"
done

##### 3.2 Generate SHA512 file
shasum --version 1>/dev/null || exit
for i in *.tar.gz; do
  echo "$i" && shasum -a 512 "$i" >"$i".sha512
done

#### 3.3 check signature & sha512
for i in *.tar.gz; do
  echo "$i"
  gpg --verify "$i".asc "$i" || exit
done

for i in *.tar.gz; do
  echo "$i"
  shasum -a 512 --check "$i".sha512 || exit
done

# step4: upload to Apache-SVN
svn_dir="${group}-svn-dev"
cd ../
rm -rfv ${svn_dir}

svn co "https://dist.apache.org/repos/dist/dev/incubator/${group}" ${svn_dir}
mkdir -p ${svn_dir}/"${release_version}"
cp -v apache-${repo}/*tar.gz* "${svn_dir}/${release_version}"
cd ${svn_dir} || exit

# check status first
svn status
svn add "${release_version}"
# check status again
svn status
# commit & push files
svn commit -m "submit files for ${repo} ${release_version}"

echo "Finished all, please check all steps in script manually again! "
