# hugegraph-client

[![License](https://img.shields.io/badge/license-Apache%202-0E78BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Build Status](https://travis-ci.org/hugegraph/hugegraph-client.svg?branch=master)](https://travis-ci.org/hugegraph/hugegraph-client)
[![codecov](https://codecov.io/gh/hugegraph/hugegraph-client/branch/master/graph/badge.svg)](https://codecov.io/gh/hugegraph/hugegraph-client)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.hugegraph/hugegraph-client/badge.svg)](https://mvnrepository.com/artifact/org.apache.hugegraph/hugegraph-client)

hugegraph-client is a Java-written client of [HugeGraph](https://github.com/hugegraph/hugegraph), providing operations of graph, schema, gremlin, variables and traversals etc. All these operations are interpreted and translated into RESTful requests to HugeGraph Server. Besides, hugegraph-client also checks arguments, serializes and deserializes structures and encapsulates server exceptions.

## Features

- Graph Operation, CRUD of vertexes and edges, batch load of vertexes and edges
- Schema Operation, CRUD of vertex label, edge label, index label and property key
- Gremlin Traversal Statements
- RESTful Traversals, shortest path, k-out, k-neighbor, paths and crosspoints etc.
- Variables, CRUD of variables

## Doc

The [client homepage](https://hugegraph.apache.org/docs/quickstart/hugegraph-client/) contains more information about it.

## Licence
The same as HugeGraph, hugegraph-client is also licensed under Apache 2.0 License.
