# hugegraph-loader

[![License](https://img.shields.io/badge/license-Apache%202-0E78BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Build Status](https://github.com/apache/hugegraph-toolchain/actions/workflows/loader-ci.yml/badge.svg)](https://github.com/apache/hugegraph-toolchain/actions/workflows/loader-ci.yml)
[![codecov](https://codecov.io/gh/hugegraph/hugegraph-loader/branch/master/graph/badge.svg)](https://codecov.io/gh/hugegraph/hugegraph-loader)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.hugegraph/hugegraph-loader/badge.svg)](https://mvnrepository.com/artifact/org.apache.hugegraph/hugegraph-loader)

hugegraph-loader is a customizable command line utility for loading small to medium size graph datasets into the HugeGraph database from multiple data sources with various input formats.

## 1. Features

- Multiple data sources, such as local file(path), HDFS file(path), MySQL
- Various input formats, such as json, csv, and text with any delimiters.
- Diverse options, with which users can manage the data loading intuitively.
- Detecting schema from data automatically, reduce the complex work of schema management.
- Advanced customized operations with groovy script, users can configure how to construct vertices and edges by themselves.

## 2. Usage for Docker(Recommand)

- Run `loader` with Docker
    - Docker run
    - Docker-compose
- Load data in docker container `loader`

### 2.1 Start with Docker

#### 2.1.1 Docker run

Use the command `docker run -itd --name loader hugegraph/loader` to start loader.

If you want to load your data, you can mount the data folder like `-v /path/to/data/file:/loader/file`


#### 2.1.2 Docker-compose

The example `docker-compose.yml` is [here](./docker/example/docker-compose.yml)

If you want to load your data, you can mount the data folder like:
```yaml
volumes:
    - /path/to/data/file:/loader/file
```

Use the command `docker-compose up -d` to deploy `loader` with `server` and `hubble`.

### 2.2 Load data with docker container

#### 2.2.1 load data with docker

> If the `loader` and `server` is in the same docker network (for example, you deploy `loader` and `server` with `docker-compose`), we can set `-h {server_container_name}`. In our example, the container name of `server` is `graph`
>
> If `loader` is deployed alone, the `-h` should be set to the ip of the host of `server`. Other parameter description is [here](https://hugegraph.apache.org/docs/quickstart/hugegraph-loader/#341-parameter-description)

```bash
docker exec -it loader bin/hugegraph-loader.sh -g hugegraph -f example/file/struct.json -s example/file/schema.groovy -h graph -p 8080
```

Then we can see the result.

```bash
HugeGraphLoader worked in NORMAL MODE
vertices/edges loaded this time : 8/6
--------------------------------------------------
count metrics
    input read success            : 14                  
    input read failure            : 0                   
    vertex parse success          : 8                   
    vertex parse failure          : 0                   
    vertex insert success         : 8                   
    vertex insert failure         : 0                   
    edge parse success            : 6                   
    edge parse failure            : 0                   
    edge insert success           : 6                   
    edge insert failure           : 0                   
--------------------------------------------------
meter metrics
    total time                    : 0.199s              
    read time                     : 0.046s              
    load time                     : 0.153s              
    vertex load time              : 0.077s              
    vertex load rate(vertices/s)  : 103                 
    edge load time                : 0.112s              
    edge load rate(edges/s)       : 53   
```

Then you can use `curl` or `hubble` to see the result.

```bash
> curl "http://localhost:8080/graphs/hugegraph/graph/vertices" | gunzip
{"vertices":[{"id":1,"label":"software","type":"vertex","properties":{"name":"lop","lang":"java","price":328.0}},{"id":2,"label":"software","type":"vertex","properties":{"name":"ripple","lang":"java","price":199.0}},{"id":"1:tom","label":"person","type":"vertex","properties":{"name":"tom"}},{"id":"1:josh","label":"person","type":"vertex","properties":{"name":"josh","age":32,"city":"Beijing"}},{"id":"1:marko","label":"person","type":"vertex","properties":{"name":"marko","age":29,"city":"Beijing"}},{"id":"1:peter","label":"person","type":"vertex","properties":{"name":"peter","age":35,"city":"Shanghai"}},{"id":"1:vadas","label":"person","type":"vertex","properties":{"name":"vadas","age":27,"city":"Hongkong"}},{"id":"1:li,nary","label":"person","type":"vertex","properties":{"name":"li,nary","age":26,"city":"Wu,han"}}]}
```

If you want to check the edges, use `curl "http://localhost:8080/graphs/hugegraph/graph/edges" | gunzip`

#### 2.2.2 enter the docker container to load data

If you want to do some additional operation in the container, you can enter the container as follows:

```bash
docker exec -it loader bash
```

Then, you can load data as follows:

```bash
sh bin/hugegraph-loader.sh -g hugegraph -f example/file/struct.json -s example/file/schema.groovy -h graph -p 8080
```

The result is as same as above.

## 3. Use loader directly

> notice: currently, version is `1.0.0`

Download and unzip the compiled archive

```bash
wget https://downloads.apache.org/incubator/hugegraph/{version}/apache-hugegraph-toolchain-incubating-{version}.tar.gz
tar zxf *hugegraph*.tar.gz
```

Then, load data with example file:

```bash
cd apache-hugegraph-toolchain-incubating-{version}
cd apache-hugegraph-loader-incubating-{version}
sh bin/hugegraph-loader.sh -g hugegraph -f example/file/struct.json -s example/file/schema.groovy
```

More details is in the [doc](https://hugegraph.apache.org/docs/quickstart/hugegraph-loader/)

## 4. Building

You can also build the `loader` by yourself.

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

## 5. Doc

The [loader homepage](https://hugegraph.apache.org/docs/quickstart/hugegraph-loader/) contains more information about it. 

## 6. License

hugegraph-loader is licensed under Apache 2.0 License.
