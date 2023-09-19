# Apache HugeGraph-Hubble

[![License](https://img.shields.io/badge/license-Apache%202-0E78BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![hugegraph-hubble-ci](https://github.com/apache/incubator-hugegraph-toolchain/actions/workflows/hubble-ci.yml/badge.svg?branch=master)](https://github.com/apache/incubator-hugegraph-toolchain/actions/workflows/hubble-ci.yml)
[![CodeQL](https://github.com/apache/incubator-hugegraph-toolchain/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/apache/incubator-hugegraph-toolchain/actions/workflows/codeql-analysis.yml)

hugegraph-hubble is a graph management and analysis platform that provides features: graph data load, schema management, graph relationship analysis and graphical display.

## Features

- Graph connection management, supporting to easily switch graph to operate
- Graph data load, supporting to load large amounts of data from files into hugegraph-server
- Schema management, supporting to easily perform schema manipulation and display
- Graph analysis and graphical display, supporting to build query via the gremlin or algorithms with a little effort then will get cool graphical results

## Quick Start

We can quickly start `hubble` in two ways:

1. We can use `docker run -itd --name=hubble -p 8088:8088 hugegraph/hubble` to quickly start [hubble](https://hub.docker.com/r/hugegraph/hubble).
2. Or we can use the `docker-compose.yml` to start `hubble` with `hugegraph-server`. If we set `PRELOAD=true`, we can preload the example graph when starting `hugegraph-server`:
    

    ```
    version: '3'
    services:
        server:
            image: hugegraph/hugegraph
            container_name: graph
            #environment:
            #  - PRELOAD=true
            ports:
              - 18080:8080

        hubble:
            image: hugegraph/hubble
            container_name: hubble
            ports:
              - 8088:8088
    ```

Then we should follow the [hubble doc](https://hugegraph.apache.org/docs/quickstart/hugegraph-hubble/#3platform-workflow) to create the graph.

## Doc

[The hubble homepage](https://hugegraph.apache.org/docs/quickstart/hugegraph-hubble/) contains more information about it.

## License

hugegraph-hubble is licensed under Apache 2.0 License.
