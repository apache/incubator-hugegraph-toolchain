# hugegraph-client

hugegraph-client is a Java-written client of [HugeGraph](https://github.com/hugegraph/hugegraph), providing operations of graph, schema, gremlin, variables and traversals etc. All these operations are interpreted and translated into RESTful requests to HugeGraph Server. Besides, hugegraph-client also checks arguments, serializes and deserializes structures and encapsulates server exceptions.

## Features

- Graph Operation, CRUD of vertexes and edges, batch load of vertexes and edges
- Schema Operation, CRUD of vertex label, edge label, index label and property key
- Gremlin Traversal Statements
- RESTful Traversals, shortest path, k-out, k-neighbor, paths and crosspoints etc.
- Variables, CRUD of variables


## Licence
The same as HugeGraph, hugegraph-client is also licensed under Apache 2.0 License.
