# hugegraph-toolchain

[![License](https://img.shields.io/badge/license-Apache%202-0E78BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Build Status](https://github.com/apache/hugegraph-toolchain/actions/workflows/ci.yml/badge.svg)](https://github.com/apache/hugegraph-toolchain/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/apache/hugegraph-toolchain/branch/master/graph/badge.svg)](https://codecov.io/gh/apache/hugegraph-toolchain)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.hugegraph/hugegraph-loader/badge.svg)](https://mvnrepository.com/artifact/org.apache.hugegraph/hugegraph-loader)

`hugegraph-toolchain` is the integration project of a series of utilities for [HugeGraph](https://github.com/apache/hugegraph), it includes 4 main modules.

## Modules

- [hugegraph-loader](./hugegraph-loader): Loading datasets into the HugeGraph from multiple data sources.
- [hugegraph-hubble](./hugegraph-hubble): Online HugeGraph management and analysis dashboard (Include: data loading, schema management, graph traverser and display)
- [hugegraph-tools](./hugegraph-tools): Command line tool for deploying, managing and backing-up/restoring graphs from HugeGraph.
- [hugegraph-client](./hugegraph-client): A Java-written client for HugeGraph, providing `RESTful` APIs for accessing graph vertex/edge/schema/gremlin/variables and traversals etc.

## Doc

The [project homepage](https://hugegraph.apache.org/docs/quickstart/) contains more information about `hugegraph-toolchain`. 

## License

hugegraph-toolchain is licensed under `Apache 2.0` License.
