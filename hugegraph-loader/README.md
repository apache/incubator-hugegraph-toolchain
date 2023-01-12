# hugegraph-loader

[![License](https://img.shields.io/badge/license-Apache%202-0E78BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Build Status](https://github.com/hugegraph/hugegraph-loader/actions/workflows/ci.yml/badge.svg)](https://github.com/hugegraph/hugegraph-loader/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/hugegraph/hugegraph-loader/branch/master/graph/badge.svg)](https://codecov.io/gh/hugegraph/hugegraph-loader)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.hugegraph/hugegraph-loader/badge.svg)](https://mvnrepository.com/artifact/org.apache.hugegraph/hugegraph-loader)

hugegraph-loader is a customizable command line utility for loading small to medium size graph datasets into the HugeGraph database from multiple data sources with various input formats.

## Features

- Multiple data sources, such as local file(path), HDFS file(path), MySQL
- Various input formats, such as json, csv, and text with any delimiters.
- Diverse options, with which users can manage the data loading intuitively.
- Detecting schema from data automatically, reduce the complex work of schema management.
- Advanced customized operations with groovy script, users can configure how to construct vertices and edges by themselves.

## Building

Required:

- Java 8
- Maven 3.6+

To build without executing tests:

```bash
mvn clean install -DskipTests=true
```

To build with default tests:

```bash
mvn clean install
```

## Doc

The [loader homepage](https://hugegraph.apache.org/docs/quickstart/hugegraph-loader/) contains more information about it. 

## License

hugegraph-loader is licensed under Apache 2.0 License.
