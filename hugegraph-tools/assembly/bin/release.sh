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
BASE_BRANCH="master"

# Checkout a new release-branch and bump base branch version
function print_usage() {
    echo "USAGE: $0 -d {project-dir} [-o {base-branch}] -n {release-branch}"
    echo "eg   : $0 -d ~/workspace/hugegraph [-o master2] -n release-1.6"
}

while getopts "d:n:o:" arg; do
    case ${arg} in
        d) PROJECT_DIR="$OPTARG" ;;
        n) RELEASE_BRANCH="$OPTARG" ;;
        o) BASE_BRANCH="$OPTARG" ;;
        ?) print_usage && exit 1 ;;
    esac
done

if [[ "$PROJECT_DIR" = "" || "$RELEASE_BRANCH" = "" || "$BASE_BRANCH" = "" ]]; then
    print_usage
    exit 1
fi

function abs_path() {
    SOURCE="${BASH_SOURCE[0]}"
    while [ -h "$SOURCE" ]; do
        DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
        SOURCE="$(readlink "$SOURCE")"
        [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
    done
    echo "$(cd -P "$(dirname "$SOURCE")" && pwd)"
}

function ensure_path_writable() {
    local path=$1
    # Ensure input path exist
    if [ ! -d "${path}" ]; then
        mkdir -p "${path}"
        if [ $? -ne 0 ]; then
            echo "Failed to mkdir $path"
            exit 1
        fi
    fi
    # Check for write permission
    if [ ! -w "${path}" ]; then
        echo "No write permission on directory ${path}"
        exit 1
    fi
}

function replace() {
    file=$1
    from=$2
    to=$3

    local os=$(uname)
    case $os in
        Darwin) sed -i '' "s!$from!$to!g" "$file" >/dev/null 2>&1 ;;
        *) sed -i "s!$from!$to!g" "$file" >/dev/null 2>&1 ;;
    esac
}

# Ensure the project dir is valid
ensure_path_writable $PROJECT_DIR

cd $PROJECT_DIR || exit

################################################################################
# Checkout release branch
################################################################################
git checkout $BASE_BRANCH >/dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "Failed to checkout old branch $BASE_BRANCH"
    exit 1
fi

git diff --quiet HEAD
if [ $? -ne 0 ]; then
    echo "There are uncommitted changes in branch $BASE_BRANCH"
    exit 1
fi

# Check new branch exist?
git rev-parse --verify $RELEASE_BRANCH >/dev/null 2>&1
if [ $? -eq 0 ]; then
    git checkout $RELEASE_BRANCH >/dev/null 2>&1
else
    git checkout -b $RELEASE_BRANCH >/dev/null 2>&1
fi

if [ $? -ne 0 ]; then
    echo "Failed to checkout new branch $RELEASE_BRANCH"
    exit 1
fi

################################################################################
# Modify README.md
################################################################################
README="README.md"
RELEASE_VERSION=$(cat "pom.xml" | grep "<version>" | head -1 | awk -F '<|>' '{print $3}')

function update_readme_maven_version() {
    # Append version to maven repository link
    # Extract the Maven Central line, then split the badge and dependency link
    MAVEN_LINK_LINE=$(cat $README | grep 'Maven Central')
    OLD_MAVEN_BADGE_LINK=$(echo $MAVEN_LINK_LINE | awk -F '[\\(|\\)]' '{print $2}')
    OLD_MAVEN_DEPEN_LINK=$(echo $MAVEN_LINK_LINE | awk -F '[\\(|\\)]' '{print $4}')

    # Replace or append the branch name in maven badge link
    if [[ "$OLD_MAVEN_BADGE_LINK" =~ .*\?version=.* ]]; then
        NEW_MAVEN_BADGE_LINK=${OLD_MAVEN_BADGE_LINK/version=*/version=${RELEASE_VERSION}}
    else
        NEW_MAVEN_BADGE_LINK="$OLD_MAVEN_BADGE_LINK?version=$RELEASE_VERSION"
    fi

    # Replace or append the version in maven dependency link
    if [[ "$OLD_MAVEN_DEPEN_LINK" =~ .*/[0-9]\.[0-9]\.[0-9]$ ]]; then
        NEW_MAVEN_DEPEN_LINK=${OLD_MAVEN_DEPEN_LINK/[0-9]\.[0-9]\.[0-9]/${RELEASE_VERSION}}
    else
        NEW_MAVEN_DEPEN_LINK="$OLD_MAVEN_DEPEN_LINK/$RELEASE_VERSION"
    fi

    replace $README "$OLD_MAVEN_BADGE_LINK" "$NEW_MAVEN_BADGE_LINK"
    replace $README "$OLD_MAVEN_DEPEN_LINK" "$NEW_MAVEN_DEPEN_LINK"
}

function check_update_readme_status() {
    local tag=$1
    local result=$2

    if [ $result -eq 0 ]; then
        echo "Modify $README '$tag' successfully"
    else
        echo "Modify $README '$tag' failed"
        exit 1
    fi
}

if [ ! -f "$README" ]; then
    echo "Skipping modify $README"
else
    echo "Checkout to branch $RELEASE_BRANCH, ready to modify $README"
    if [ $(grep -c "Build Status" "$README") -eq 1 ]; then
        # Replace old branch with new
        replace $README "branch=$BASE_BRANCH" "branch=$RELEASE_BRANCH"
        check_update_readme_status "Build Status" $?
    fi
    if [ $(grep -c "codecov" "$README") -eq 1 ]; then
        # Replace old branch with new
        replace $README "branch/$BASE_BRANCH" "branch/$RELEASE_BRANCH"
        check_update_readme_status "codecov" $?
    fi
    if [ $(grep -c "Maven Central" "$README") -eq 1 ]; then
        update_readme_maven_version
        check_update_readme_status "Maven Central" $?
    fi
fi

git diff --quiet HEAD
if [ $? -ne 0 ]; then
    # Git commit in release branch
    git commit -a -m "HugeGraph-1358: Release $RELEASE_VERSION" >/dev/null 2>&1 || exit 1
    echo "Add a commit in branch $RELEASE_BRANCH, remember to push"
else
    echo "Nothing modified for branch $RELEASE_BRANCH"
fi

################################################################################
# Checkout to base branch and bump version
################################################################################
function update_hugegraph_version() {
    # Second digit plus 1
    BUMP_VERSION=$(echo $RELEASE_VERSION | awk -F '.' '{prefix=$2}END{print $1"."prefix+1".0"}')
    CORE_POM_XML_FILE="$PROJECT_DIR/hugegraph-core/pom.xml"
    CORE_VERSION_JAVA_FILE="$PROJECT_DIR/hugegraph-core/src/main/java/org/apache/hugegraph/version/CoreVersion.java"
    API_VERSION_JAVA_FILE="$PROJECT_DIR/hugegraph-api/src/main/java/org/apache/hugegraph/version/ApiVersion.java"
    # Replace Implementation-Version in core pom.xml
    replace $CORE_POM_XML_FILE "<Implementation-Version>.*</Implementation-Version>" \
        "<Implementation-Version>$BUMP_VERSION.0</Implementation-Version>" || return 1
    # Replace version in CoreVersion.java
    replace $CORE_VERSION_JAVA_FILE "Version.of(CoreVersion.class, \".*\")" \
        "Version.of(CoreVersion.class, \"$BUMP_VERSION\")" || return 1
    # Replace version in ApiVersion.java
    # Extract the first two digits of the version number
    MIN_VERSION=$(echo $BUMP_VERSION | awk -F '.' '{print $1"."$2}')
    # Second digit plus 1
    MAX_VERSION=$(echo $BUMP_VERSION | awk -F '.' '{prefix=$2}END{print $1"."prefix+1}')
    replace $API_VERSION_JAVA_FILE "VersionUtil.check(CoreVersion.VERSION, \".*\", \".*\", CoreVersion.NAME);" \
        "VersionUtil.check(CoreVersion.VERSION, \"$MIN_VERSION\", \"$MAX_VERSION\", CoreVersion.NAME);" || return 1
}

function update_general_component_version() {
    # Third digit plus 1
    BUMP_VERSION=$(echo $RELEASE_VERSION | awk -F '.' '{prefix=$3}END{print $1"."$2"."prefix+1}')
    POM_XML_FILE="$PROJECT_DIR/pom.xml"
    # Replace Implementation-Version in pom.xml
    replace $POM_XML_FILE "<Implementation-Version>.*</Implementation-Version>" \
        "<Implementation-Version>$BUMP_VERSION.0</Implementation-Version>" || return 1
}

function check_update_version_status() {
    local artifact=$1
    local result=$2

    if [ $result -eq 0 ]; then
        echo "Bump up artifact '$artifact' implementation version successfully"
    else
        echo "Bump up artifact '$artifact' implementation version failed"
        exit 1
    fi
}

git checkout $BASE_BRANCH >/dev/null 2>&1 || exit 1
echo "Checkout to branch $BASE_BRANCH, ready to bump pom version"

if [ -f "pom.xml" ]; then
    ARTIFACT=$(cat "pom.xml" | grep "<artifactId>" | head -1 | awk -F '<|>' '{print $3}')
    # Bump up maven implementation version
    if [ "$ARTIFACT" = "hugegraph" ]; then
        update_hugegraph_version
        check_update_version_status $ARTIFACT $?
    else
        update_general_component_version
        check_update_version_status $ARTIFACT $?
    fi

    # Bump up maven version
    mvn versions:set -DnewVersion=$BUMP_VERSION >/dev/null 2>&1 && mvn versions:commit >/dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo "Bump up artifact '$ARTIFACT' version successfully"
    else
        echo "Bump up artifact '$ARTIFACT' version failed"
        exit 1
    fi
fi

git diff --quiet HEAD
if [ $? -ne 0 ]; then
    # Git commit in base branch
    git commit -a -m "HugeGraph-622: Bump up to version $BUMP_VERSION" >/dev/null 2>&1 || exit 1
    echo "Add a commit in branch $BASE_BRANCH, remember to push"
else
    echo "Nothing modified for branch $BASE_BRANCH"
fi
