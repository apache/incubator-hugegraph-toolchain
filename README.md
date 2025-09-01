# hugegraph-toolchain

[![License](https://img.shields.io/badge/license-Apache%202-0E78BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Build Status](https://github.com/apache/hugegraph-toolchain/actions/workflows/client-ci.yml/badge.svg)](https://github.com/apache/hugegraph-toolchain/actions/workflows/client-ci.yml)
[![Build Status](https://github.com/apache/hugegraph-toolchain/actions/workflows/loader-ci.yml/badge.svg)](https://github.com/apache/hugegraph-toolchain/actions/workflows/loader-ci.yml)
[![Build Status](https://github.com/apache/hugegraph-toolchain/actions/workflows/hubble-ci.yml/badge.svg)](https://github.com/apache/hugegraph-toolchain/actions/workflows/hubble-ci.yml)
[![Build Status](https://github.com/apache/hugegraph-toolchain/actions/workflows/tools-ci.yml/badge.svg)](https://github.com/apache/hugegraph-toolchain/actions/workflows/tools-ci.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.hugegraph/hugegraph-client/badge.svg)](https://mvnrepository.com/artifact/org.apache.hugegraph/hugegraph-client)

`hugegraph-toolchain` is the integration project contains a series of utilities for [HugeGraph](https://github.com/apache/hugegraph), 
it includes 5+ main modules.

## Modules

- [hugegraph-loader](./hugegraph-loader): Loading datasets into the HugeGraph from multiple data sources.
- [hugegraph-hubble](./hugegraph-hubble): Online HugeGraph management and analysis dashboard (Include: data loading, schema management, graph traverser and display).
- [hugegraph-tools](./hugegraph-tools): Command line tool for deploying, managing and backing-up/restoring graphs from HugeGraph.
- [hugegraph-client](./hugegraph-client): A Java-written client for HugeGraph, providing `RESTful` APIs for accessing graph vertex/edge/schema/gremlin/variables and traversals etc.
- [hugegraph-client-go](./hugegraph-client-go): A Go-written client for HugeGraph, providing `RESTful` APIs for accessing graph vertex/edge/schema/gremlin/variables and traversals etc. (WIP)
- [hugegraph-spark-connector](./hugegraph-spark-connector): A Spark connector for reading & writing HugeGraph data in Spark standard format.

## Usage

- [hugegraph-loader](./hugegraph-loader): We can follow the [doc](https://hugegraph.apache.org/docs/quickstart/hugegraph-loader/) to learn how to quickly start with `loader`.
- [hugegraph-hubble](./hugegraph-hubble): We can follow the [doc](https://hugegraph.apache.org/docs/quickstart/hugegraph-hubble/) to learn how to quickly start with `hubble`.
- [hugegraph-client](./hugegraph-client): We can follow the [doc](https://hugegraph.apache.org/docs/quickstart/hugegraph-client/) to learn how to quickly start with `client`.

## Maven Dependencies

You could use import the dependencies in `maven` like this:

```xml
  <!-- Note: use the latest release version in maven repo, here is just an example -->
  <dependency>
       <groupId>org.apache.hugegraph</groupId>
       <artifactId>hugegraph-client</artifactId>
       <version>1.7.0</version>
  </dependency>
  
  <dependency>
       <groupId>org.apache.hugegraph</groupId>
       <artifactId>hugegraph-loader</artifactId>
       <version>1.7.0</version>
  </dependency>
```

And here are links of other **HugeGraph** component/repositories:
1. [hugegraph](https://github.com/apache/hugegraph) (**[pd](https://github.com/apache/hugegraph/tree/master/hugegraph-pd)/[store](https://github.com/apache/hugegraph/tree/master/hugegraph-store)/[server](https://github.com/apache/hugegraph/tree/master/hugegraph-server)/[commons](https://github.com/apache/hugegraph/tree/master/hugegraph-commons)**)
2. [hugegraph-computer](https://github.com/apache/hugegraph-computer) (integrated **graph computing** system)
3. [hugegraph-ai](https://github.com/apache/incubator-hugegraph-ai) (integrated **Graph AI/LLM/KG** system)
4. [hugegraph-website](https://github.com/apache/hugegraph-doc) (**doc & website** code)


## Doc

The [project homepage](https://hugegraph.apache.org/docs/quickstart/) contains more information about `hugegraph-toolchain`. 

## License

hugegraph-toolchain is licensed under [Apache 2.0](https://github.com/apache/hugegraph-toolchain/blob/master/LICENSE) License.

## Contributing

- Welcome to contribute to HugeGraph, please see [How to Contribute](https://hugegraph.apache.org/docs/contribution-guidelines/contribute/) for more information.
- Note: It's recommended to use [GitHub Desktop](https://desktop.github.com/) to greatly simplify the PR and commit process.
- Thank you to all the people who already contributed to HugeGraph!

[![contributors graph](https://contrib.rocks/image?repo=apache/hugegraph-toolchain)](https://github.com/apache/hugegraph-toolchain/graphs/contributors)

### Contact Us

---

 - [GitHub Issues](https://github.com/apache/hugegraph-toolchain/issues): Feedback on usage issues and functional requirements (quick response)
 - Feedback Email: [dev@hugegraph.apache.org](mailto:dev@hugegraph.apache.org) ([subscriber](https://hugegraph.apache.org/docs/contribution-guidelines/subscribe/) only)
 - WeChat public account: Apache HugeGraph, welcome to scan this QR code to follow us.

 <img src="https://raw.githubusercontent.com/apache/hugegraph-doc/master/assets/images/wechat.png" alt="QR png" width="350"/>

