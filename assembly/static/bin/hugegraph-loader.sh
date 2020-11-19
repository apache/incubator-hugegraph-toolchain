#!/bin/bash

abs_path() {
    SOURCE="${BASH_SOURCE[0]}"
    while [[ -h "$SOURCE" ]]; do
        DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
        SOURCE="$(readlink "$SOURCE")"
        [[ ${SOURCE} != /* ]] && SOURCE="$DIR/$SOURCE"
    done
    echo "$( cd -P "$( dirname "$SOURCE" )" && pwd )"
}

BIN=`abs_path`
TOP="$(cd ${BIN}/../ && pwd)"
CONF="$TOP/conf"
LIB="$TOP/lib"
NATIVE="$TOP/native"
LOG="$TOP/logs"

# Use the unofficial bash strict mode to avoid subtle bugs impossible.
# Don't use -u option for now because LOADER_HOME might not yet defined.
set -eo pipefail

export VARS=${@:1}

# Use JAVA_HOME if set, otherwise look for java in PATH
if [[ -n "$JAVA_HOME" ]]; then
    # Why we can't have nice things: Solaris combines x86 and x86_64
    # installations in the same tree, using an unconventional path for the
    # 64bit JVM.  Since we prefer 64bit, search the alternate path first,
    # (see https://issues.apache.org/jira/browse/CASSANDRA-4638).
    for java in "$JAVA_HOME"/bin/amd64/java "$JAVA_HOME"/bin/java; do
        if [[ -x "$java" ]]; then
            JAVA="$java"
            break
        fi
    done
else
    JAVA=java
fi

if [[ -z ${JAVA} ]] ; then
    echo Unable to find java executable. Check JAVA_HOME and PATH environment variables. > /dev/stderr
    exit 1;
fi

# Add the slf4j-log4j12 binding
CP=$(find -L ${LIB} -name 'log4j-slf4j-impl*.jar' | sort | tr '\n' ':')
# Add the jars in lib that start with "hugegraph"
CP="$CP":$(find -L ${LIB} -name 'hugegraph*.jar' | sort | tr '\n' ':')
# Add the remaining jars in lib.
CP="$CP":$(find -L ${LIB} -name '*.jar' \
                \! -name 'hugegraph*' \
                \! -name 'log4j-slf4j-impl*.jar' | sort | tr '\n' ':')

export LOADER_CLASSPATH="${CLASSPATH:-}:$CP"

# Xmx needs to be set so that it is big enough to cache all the vertexes in the run
export JVM_OPTS="$JVM_OPTS -Xmx10g -cp $LOADER_CLASSPATH"

# Uncomment to enable debugging
#JVM_OPTS="$JVM_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=1414"

exec ${JAVA} -Dname="HugeGraphLoader" -Dloader.home.path=${TOP} -Dlog4j.configurationFile=${CONF}/log4j2.xml \
-Djava.library.path=${NATIVE} \
${JVM_OPTS} com.baidu.hugegraph.loader.HugeGraphLoader ${VARS}
