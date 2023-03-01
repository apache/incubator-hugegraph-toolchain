# hugegraph-toolchain

[![License](https://img.shields.io/badge/license-Apache%202-0E78BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Build Status](https://github.com/apache/hugegraph-toolchain/actions/workflows/client-ci.yml/badge.svg)](https://github.com/apache/hugegraph-toolchain/actions/workflows/client-ci.yml)
[![Build Status](https://github.com/apache/hugegraph-toolchain/actions/workflows/loader-ci.yml/badge.svg)](https://github.com/apache/hugegraph-toolchain/actions/workflows/loader-ci.yml)
[![Build Status](https://github.com/apache/hugegraph-toolchain/actions/workflows/hubble-ci.yml/badge.svg)](https://github.com/apache/hugegraph-toolchain/actions/workflows/hubble-ci.yml)
[![Build Status](https://github.com/apache/hugegraph-toolchain/actions/workflows/tools-ci.yml/badge.svg)](https://github.com/apache/hugegraph-toolchain/actions/workflows/tools-ci.yml)
[![codecov](https://codecov.io/gh/apache/hugegraph-toolchain/branch/master/graph/badge.svg)](https://codecov.io/gh/apache/hugegraph-toolchain)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.hugegraph/hugegraph-client/badge.svg)](https://mvnrepository.com/artifact/org.apache.hugegraph/hugegraph-client)

`hugegraph-toolchain` is the integration project of a series of utilities for [HugeGraph](https://github.com/apache/hugegraph), it includes 4 main modules.

## Modules

- [hugegraph-loader](./hugegraph-loader): Loading datasets into the HugeGraph from multiple data sources.
- [hugegraph-hubble](./hugegraph-hubble): Online HugeGraph management and analysis dashboard (Include: data loading, schema management, graph traverser and display)
- [hugegraph-tools](./hugegraph-tools): Command line tool for deploying, managing and backing-up/restoring graphs from HugeGraph.
- [hugegraph-client](./hugegraph-client): A Java-written client for HugeGraph, providing `RESTful` APIs for accessing graph vertex/edge/schema/gremlin/variables and traversals etc.

## Maven Dependencies

You could use import the dependencies in maven like this

```xml
  <dependency>
       <groupId>org.apache.hugegraph</groupId>
       <artifactId>hugegraph-client</artifactId>
       <version>1.0.0</version>
  </dependency>
  
  <dependency>
       <groupId>org.apache.hugegraph</groupId>
       <artifactId>hugegraph-loader</artifactId>
       <version>1.0.0</version>
  </dependency>
```

## Doc

The [project homepage](https://hugegraph.apache.org/docs/quickstart/) contains more information about `hugegraph-toolchain`. 

## License

hugegraph-toolchain is licensed under `Apache 2.0` License.
