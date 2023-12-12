<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with this
work for additional information regarding copyright ownership. The ASF
licenses this file to You under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

# HugeGraph Spark Connector

[![License](https://img.shields.io/badge/license-Apache%202-0E78BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

HugeGraph Spark Connector is a Spark connector application for reading and writing HugeGraph data in Spark standard format.

## Building

Required:

- Java 8+
- Maven 3.6+

To build without executing tests:

```bash
mvn clean package -DskipTests
```

To build with default tests:

```bash
mvn clean packge
```

## How to use

If we have a graph, the schema is defined as follows:

### Schema

```groovy
schema.propertyKey("name").asText().ifNotExist().create()
schema.propertyKey("age").asInt().ifNotExist().create()
schema.propertyKey("city").asText().ifNotExist().create()
schema.propertyKey("weight").asDouble().ifNotExist().create()
schema.propertyKey("lang").asText().ifNotExist().create()
schema.propertyKey("date").asText().ifNotExist().create()
schema.propertyKey("price").asDouble().ifNotExist().create()

schema.vertexLabel("person")
        .properties("name", "age", "city")
        .useCustomizeStringId()
        .nullableKeys("age", "city")
        .ifNotExist()
        .create()

schema.vertexLabel("software")
        .properties("name", "lang", "price")
        .primaryKeys("name")
        .ifNotExist()
        .create()

schema.edgeLabel("knows")
        .sourceLabel("person")
        .targetLabel("person")
        .properties("date", "weight")
        .ifNotExist()
        .create()

schema.edgeLabel("created")
        .sourceLabel("person")
        .targetLabel("software")
        .properties("date", "weight")
        .ifNotExist()
        .create()
```

Then we can insert graph data through Spark, first add dependency in your pom.

```xml
<dependency>
    <groupId>org.apache.hugegraph</groupId>
    <artifactId>hugegraph-spark-connector</artifactId>
    <version>${revision}</version>
</dependency>
```

### Vertex Sink

```scala
val df = sparkSession.createDataFrame(Seq(
  Tuple3("marko", 29, "Beijing"),
  Tuple3("vadas", 27, "HongKong"),
  Tuple3("Josh", 32, "Beijing"),
  Tuple3("peter", 35, "ShangHai"),
  Tuple3("li,nary", 26, "Wu,han"),
  Tuple3("Bob", 18, "HangZhou"),
)) toDF("name", "age", "city")

df.show()

df.write
  .format("org.apache.hugegraph.spark.connector.DataSource")
  .option("host", "127.0.0.1")
  .option("port", "8080")
  .option("graph", "hugegraph")
  .option("data-type", "vertex")
  .option("label", "person")
  .option("id", "name")
  .option("batch-size", 2)
  .mode(SaveMode.Overwrite)
  .save()
```

### Edge Sink

```scala
val df = sparkSession.createDataFrame(Seq(
  Tuple4("marko", "vadas", "20160110", 0.5),
  Tuple4("peter", "Josh", "20230801", 1.0),
  Tuple4("peter", "li,nary", "20130220", 2.0)
)).toDF("source", "target", "date", "weight")

df.show()

df.write
  .format("org.apache.hugegraph.spark.connector.DataSource")
  .option("host", "127.0.0.1")
  .option("port", "8080")
  .option("graph", "hugegraph")
  .option("data-type", "edge")
  .option("label", "knows")
  .option("source-name", "source")
  .option("target-name", "target")
  .option("batch-size", 2)
  .mode(SaveMode.Overwrite)
  .save()
```

### Configs

Client Configs are used to configure hugegraph-client.

#### Client Configs

| Params               | Default Value | Description                                                                                  |
|----------------------|---------------|----------------------------------------------------------------------------------------------|
| `host`               | `localhost`   | Address of HugeGraphServer                                                                   |
| `port`               | `8080`        | Port of HugeGraphServer                                                                      |
| `graph`              | `hugegraph`   | Graph space name                                                                             |
| `protocol`           | `http`        | Protocol for sending requests to the server, optional `http` or `https`                      |
| `username`           | `null`        | Username of the current graph when HugeGraphServer enables permission authentication         |
| `token`              | `null`        | Token of the current graph when HugeGraphServer has enabled authorization authentication     |
| `timeout`            | `60`          | Timeout (seconds) for inserting results to return                                            |
| `max-conn`           | `CPUS * 4`    | The maximum number of HTTP connections between HugeClient and HugeGraphServer                |
| `max-conn-per-route` | `CPUS * 2`    | The maximum number of HTTP connections for each route between HugeClient and HugeGraphServer |
| `trust-store-file`   | `null`        | The client’s certificate file path when the request protocol is https                        |
| `trust-store-token`  | `null`        | The client's certificate password when the request protocol is https                         |

##### Graph Data Configs

Graph Data Configs are used to set graph space configuration.

| Params            | Default Value | Description                                                                                                                                                                                                                                                                                                                                                                                                                |
|-------------------|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `date-type`       |               | Graph data type, must be `vertex` or `edge`                                                                                                                                                                                                                                                                                                                                                                                |
| `label`           |               | Label to which the vertex/edge data to be imported belongs                                                                                                                                                                                                                                                                                                                                                                 |
| `id`              |               | Specify a column as the id column of the vertex. When the vertex id policy is CUSTOMIZE, it is required; when the id policy is PRIMARY_KEY, it must be empty                                                                                                                                                                                                                                                               |
| `source-name`     |               | Select certain columns of the input source as the id column of source vertex. When the id policy of the source vertex is CUSTOMIZE, a certain column must be specified as the id column of the vertex; when the id policy of the source vertex is When PRIMARY_KEY, one or more columns must be specified for splicing the id of the generated vertex, that is, no matter which id strategy is used, this item is required |
| `target-name`     |               | Specify certain columns as the id columns of target vertex, similar to source                                                                                                                                                                                                                                                                                                                                              |
| `selected-fields` |               | Select some columns to insert, other unselected ones are not inserted, cannot exist at the same time as ignored                                                                                                                                                                                                                                                                                                            |
| `ignored-fields`  |               | Ignore some columns so that they do not participate in insertion, cannot exist at the same time as selected                                                                                                                                                                                                                                                                                                                |
| `batch-size`      | `500`         | The number of data items in each batch when importing data                                                                                                                                                                                                                                                                                                                                                                 |

#### Common Configs

Common Configs contains some common configurations.

| Params      | Default Value | Description                                                                     |
|-------------|---------------|---------------------------------------------------------------------------------|
| `delimiter` | `,`           | Separator of `source-name`, `target-name`, `selected-fields` or `ignore-fields` |

## Licence

The same as HugeGraph, hugegraph-spark-connector is also licensed under Apache 2.0 License.
