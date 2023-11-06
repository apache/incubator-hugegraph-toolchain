FROM maven:3.9.0-eclipse-temurin-11 AS build

COPY . /pkg
WORKDIR /pkg

RUN set -x \
    && mvn install -pl hugegraph-client,hugegraph-loader -am -Dmaven.javadoc.skip=true -DskipTests -ntp


RUN set -x \
    && cd /pkg/hugegraph-loader/ \
    && echo "$(ls)" \
    && mvn clean package -DskipTests


FROM openjdk:11-slim

COPY --from=build /pkg/hugegraph-loader/apache-hugegraph-loader-incubating-*/ /loader
WORKDIR /loader/

RUN set -x \
    && apt-get -q update \
    && apt-get -q install -y --no-install-recommends --no-install-suggests \
       dumb-init \
       procps \
       curl \
       lsof \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

VOLUME /loader

ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD [ "/bin/bash" ]