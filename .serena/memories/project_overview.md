# HugeGraph Toolchain Project Overview

## Project Purpose
Apache HugeGraph Toolchain is an integration project containing a series of utilities for [Apache HugeGraph](https://github.com/apache/hugegraph), a distributed graph database. The toolchain provides essential tools for data loading, management, visualization, and client access.

## Version Information
- Current Version: 1.7.0
- License: Apache 2.0
- Repository: https://github.com/apache/hugegraph-toolchain
- Project Status: Apache Incubator

## Main Modules (6 Total)

### 1. hugegraph-client (Java)
**Purpose**: Java RESTful API client for HugeGraph  
**Language**: Java 8  
**Key Features**:
- RESTful APIs for accessing graph vertex/edge/schema operations
- Gremlin query support
- Graph traversal algorithms (shortest path, k-hop, etc.)
- Authentication and authorization support

### 2. hugegraph-loader
**Purpose**: Data loading utility from multiple sources into HugeGraph  
**Language**: Java 8  
**Supported Sources**:
- File sources: CSV, JSON, TXT (local files)
- HDFS sources
- JDBC sources: MySQL, PostgreSQL, Oracle, SQL Server
- Kafka streaming sources

### 3. hugegraph-hubble
**Purpose**: Web-based graph management and analysis dashboard  
**Tech Stack**:
- **Backend**: Spring Boot (Java 8)
- **Frontend**: React + TypeScript + MobX (Node.js 18.20.8 required)
**Features**:
- Data loading interface
- Schema management
- Graph visualization
- Query builder (Gremlin and algorithm-based)

### 4. hugegraph-tools
**Purpose**: Command-line tools for deployment and management  
**Language**: Java 8  
**Features**:
- Deployment management
- Backup and restore operations
- Administrative tasks

### 5. hugegraph-client-go (WIP)
**Purpose**: Go client library for HugeGraph  
**Language**: Go  
**Status**: Work In Progress

### 6. hugegraph-spark-connector
**Purpose**: Spark connector for HugeGraph data I/O  
**Language**: Java 8 + Scala 2.12  
**Spark Version**: 3.2.2

## Module Dependencies
```
hugegraph-dist (assembly)
  └── hugegraph-hubble
      └── hugegraph-loader
          └── hugegraph-client
              └── hugegraph-common (external: v1.5.0)

hugegraph-tools
  └── hugegraph-client

hugegraph-spark-connector
  └── hugegraph-client

hugegraph-client-go (independent)
```

## Technology Stack

### Java Ecosystem
- **Java Version**: 1.8 (source/target)
- **Build Tool**: Maven 3.x
- **Test Framework**: JUnit 4 + Mockito 2.25.1
- **Common Libraries**:
  - Apache Commons (IO, Lang3, Compress, CLI, Text, Codec)
  - Jackson 2.12.3 (JSON processing)
  - Log4j2 2.18.0 (Logging)
  - Netty 4.1.65.Final
  - Lombok 1.18.8

### Frontend (Hubble)
- **Node.js**: 18.20.8 (required exact version)
- **Package Manager**: yarn (not npm)
- **Framework**: React
- **Language**: TypeScript
- **State Management**: MobX
- **Code Quality**: Prettier + Stylelint + Husky

### Go (Client-Go)
- **Build Tool**: Makefile
- **Testing**: Built-in Go test with race detector

## Key External Dependencies
- HugeGraph Server (required for testing)
- HugeGraph Common library v1.5.0
- Spark 3.2.2 (for connector)
- Flink 1.13.5 (for stream processing)

## Project Structure
```
toolchain/
├── hugegraph-client/        # Java client library
├── hugegraph-loader/         # Data loading tool
├── hugegraph-hubble/         # Web dashboard
│   ├── hubble-be/            # Backend (Spring Boot)
│   ├── hubble-fe/            # Frontend (React)
│   └── hubble-dist/          # Distribution files
├── hugegraph-tools/          # CLI tools
├── hugegraph-client-go/      # Go client (WIP)
├── hugegraph-spark-connector/# Spark connector
├── hugegraph-dist/           # Assembly module
├── assembly/                 # Build descriptors
├── tools/                    # Checkstyle, suppressions
├── .github/workflows/        # CI/CD pipelines
└── pom.xml                   # Root Maven config
```
