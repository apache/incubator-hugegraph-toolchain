FROM ubuntu:xenial

RUN set -x \
    && apt-get -q update \
    && apt-get -q install -y --no-install-recommends --no-install-suggests \
       curl \
       lsof \
       g++ \
       gcc \
       openjdk-8-jdk \
    && apt-get clean

ENV HUBBLE_HOME /opt/hugegraph-hubble
WORKDIR ${HUBBLE_HOME}

COPY ./hugegraph-hubble $HUBBLE_HOME
EXPOSE 8088

ENTRYPOINT ["./bin/start-hubble.sh"]
