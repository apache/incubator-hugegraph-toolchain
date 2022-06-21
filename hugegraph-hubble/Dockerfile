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

ENV PKG_URL https://github.com/hugegraph
ENV HUBBLE_HOME /opt/hugegraph-hubble
ENV HUBBLE_VERSION 1.6.5

RUN set -e \
    && mkdir -p ${HUBBLE_HOME} \
    && curl -L -S ${PKG_URL}/hugegraph-hubble/releases/download/v${HUBBLE_VERSION}/hugegraph-hubble-${HUBBLE_VERSION}.tar.gz -o ${HUBBLE_HOME}.tar.gz \
    && tar xzf ${HUBBLE_HOME}.tar.gz --strip-components 1 -C ${HUBBLE_HOME}	\
    && sed -n '66p' ${HUBBLE_HOME}/bin/start-hubble.sh | grep "&" > /dev/null && sed -i 66{s/\&$/#/g} ${HUBBLE_HOME}/bin/start-hubble.sh \
    && sed -n '78p' ${HUBBLE_HOME}/bin/start-hubble.sh | grep "exit" > /dev/null && sed -i 78{s/^/#/g} ${HUBBLE_HOME}/bin/start-hubble.sh


EXPOSE 8088
WORKDIR ${HUBBLE_HOME}

ENTRYPOINT ["./bin/start-hubble.sh"]
