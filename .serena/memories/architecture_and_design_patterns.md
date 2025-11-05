# Architecture and Design Patterns - HugeGraph Toolchain

## Overall Architecture

### System Context
```
┌─────────────────────────────────────────────────────────────┐
│                     HugeGraph Ecosystem                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐      ┌──────────────────────────────┐   │
│  │  HugeGraph   │◄─────┤   HugeGraph Toolchain        │   │
│  │   Server     │      │                               │   │
│  │   (Core)     │      │  ┌─────────────────────────┐  │   │
│  └──────────────┘      │  │  hugegraph-client       │  │   │
│         ▲              │  │  (RESTful API wrapper)  │  │   │
│         │              │  └──────────┬──────────────┘  │   │
│         │              │             │                  │   │
│         │              │      ┌──────▼──────────┐      │   │
│    REST API            │      │  ┌──────────────┐     │   │
│    (HTTP/HTTPS)        │      │  │ loader       │     │   │
│         │              │      │  │ tools        │     │   │
│         │              │      │  │ hubble-be    │     │   │
│         │              │      │  │ spark        │     │   │
│         └──────────────┼──────┘  │ client-go    │     │   │
│                        │         └──────────────┘     │   │
│                        │                               │   │
│  ┌──────────────┐     │      ┌──────────────────┐   │   │
│  │  External     │────►│      │  hubble-fe       │   │   │
│  │  Data Sources │     │      │  (React Web UI)  │   │   │
│  │  (CSV/HDFS/   │     │      └──────────────────┘   │   │
│  │   JDBC/Kafka) │     │                               │   │
│  └──────────────┘     └──────────────────────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Module-Specific Architectures

## 1. hugegraph-client Architecture

### Layered Architecture
```
┌─────────────────────────────────────────────┐
│         Application Layer                   │
│  (User code using HugeGraph client)         │
└───────────────┬─────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────┐
│         Manager Layer                       │
│  ┌──────────────┐ ┌──────────────┐        │
│  │SchemaManager │ │GraphManager  │        │
│  ├──────────────┤ ├──────────────┤        │
│  │TraverserMgr  │ │JobManager    │        │
│  ├──────────────┤ ├──────────────┤        │
│  │TaskManager   │ │AuthManager   │        │
│  └──────────────┘ └──────────────┘        │
└───────────────┬─────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────┐
│         API Layer                           │
│  ┌──────────────┐ ┌──────────────┐        │
│  │ VertexAPI    │ │  EdgeAPI     │        │
│  ├──────────────┤ ├──────────────┤        │
│  │ SchemaAPI    │ │ GremlinAPI   │        │
│  ├──────────────┤ ├──────────────┤        │
│  │ TraverserAPI │ │  JobAPI      │        │
│  └──────────────┘ └──────────────┘        │
└───────────────┬─────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────┐
│         REST Client Layer                   │
│  ┌──────────────────────────────┐          │
│  │      RestClient               │          │
│  │  - HTTP connection pool       │          │
│  │  - Request/Response handling  │          │
│  │  - Authentication             │          │
│  │  - Error handling             │          │
│  └──────────────────────────────┘          │
└───────────────┬─────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────┐
│      HugeGraph Server (REST API)            │
└─────────────────────────────────────────────┘
```

### Key Components

#### 1. RestClient (Core)
**Responsibilities**:
- HTTP/HTTPS connection management
- Request serialization (Java objects → JSON)
- Response deserialization (JSON → Java objects)
- Authentication (Basic Auth, Token)
- Error handling and retry logic
- Connection pooling

**Key Methods**:
```java
// Generic request methods
public <T> T get(String path, Class<T> responseClass)
public <T> T post(String path, Object request, Class<T> responseClass)
public <T> T put(String path, Object request, Class<T> responseClass)
public <T> T delete(String path, Class<T> responseClass)

// With custom headers
public <T> T request(HttpMethod method, String path, Object request, 
                     Map<String, String> headers, Class<T> responseClass)
```

#### 2. Manager Pattern
Each manager handles a specific domain:

**SchemaManager**: Schema CRUD operations
```java
// Get manager
SchemaManager schema = hugegraph.schema();

// Operations
schema.propertyKey("name").asText().create();
schema.vertexLabel("person").properties("name", "age").create();
schema.edgeLabel("knows").link("person", "person").create();
schema.indexLabel("personByName").onV("person").by("name").create();
```

**GraphManager**: Vertex/Edge operations
```java
GraphManager graph = hugegraph.graph();

// CRUD
Vertex v = graph.addVertex("person", "name", "Alice", "age", 30);
Edge e = v.addEdge("knows", target, "date", "2023-01-01");
Vertex retrieved = graph.getVertex(id);
graph.removeVertex(id);
```

**TraverserManager**: Graph algorithms
```java
TraverserManager traverser = hugegraph.traverser();

// Algorithms
Path shortestPath = traverser.shortestPath(sourceId, targetId, direction, maxDepth);
List<Vertex> kHop = traverser.kHop(sourceId, direction, depth);
List<Path> kShortestPaths = traverser.kShortestPaths(sourceId, targetId, k);
```

#### 3. Builder Pattern (Fluent API)
```java
// PropertyKey builder
PropertyKey age = schema.propertyKey("age")
    .asInt()
    .valueSingle()           // Single value (not set)
    .ifNotExist()            // Create only if not exists
    .create();

// VertexLabel builder
VertexLabel person = schema.vertexLabel("person")
    .properties("name", "age", "city")
    .primaryKeys("name")     // Required fields
    .nullableKeys("city")    // Optional fields
    .ifNotExist()
    .create();

// EdgeLabel builder
EdgeLabel knows = schema.edgeLabel("knows")
    .sourceLabel("person")
    .targetLabel("person")
    .properties("date", "weight")
    .frequency(Frequency.SINGLE)  // One edge per (source,target) pair
    .ifNotExist()
    .create();
```

### Serialization Layer
**Purpose**: Convert between Java objects and JSON

**Key Classes**:
- `VertexSerializer`: Serialize/deserialize vertices
- `EdgeSerializer`: Serialize/deserialize edges
- `PathSerializer`: Serialize/deserialize paths
- `ResultDeserializer`: Generic result parsing

## 2. hugegraph-loader Architecture

### Pipeline Architecture
```
┌──────────────────────────────────────────────────────────┐
│                  Data Loading Pipeline                    │
└──────────────────────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────┐
│  Phase 1: Data Source Connection                         │
│  ┌─────────────────────────────────────────────────┐    │
│  │ Source Factory (based on SourceType)            │    │
│  │  - FileSource  (CSV, JSON, TXT)                 │    │
│  │  - HDFSSource  (HDFS files)                     │    │
│  │  - JDBCSource  (MySQL, PostgreSQL, Oracle)      │    │
│  │  - KafkaSource (Kafka topics)                   │    │
│  └─────────────────────────────────────────────────┘    │
└──────────────────┬───────────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────────┐
│  Phase 2: Data Reading & Parsing                        │
│  ┌─────────────────────────────────────────────────┐    │
│  │ Fetcher (source-specific)                       │    │
│  │  - FileFetcher: Read file line-by-line          │    │
│  │  - JDBCFetcher: Execute SQL query               │    │
│  │  - KafkaFetcher: Consume messages               │    │
│  └────────────────┬────────────────────────────────┘    │
│                   ▼                                       │
│  ┌─────────────────────────────────────────────────┐    │
│  │ Reader (format-specific)                        │    │
│  │  - CSVReader: Parse CSV records                 │    │
│  │  - JSONReader: Parse JSON objects               │    │
│  │  - TextReader: Parse text lines                 │    │
│  └────────────────┬────────────────────────────────┘    │
└───────────────────┼──────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────────────────┐
│  Phase 3: Element Building                              │
│  ┌─────────────────────────────────────────────────┐    │
│  │ Mapping Config (struct.json)                    │    │
│  │  - Field mappings: source → graph property      │    │
│  │  - ID generation strategies                     │    │
│  │  - Value conversions                            │    │
│  └────────────────┬────────────────────────────────┘    │
│                   ▼                                       │
│  ┌─────────────────────────────────────────────────┐    │
│  │ ElementBuilder                                  │    │
│  │  - Build Vertex from row/record                 │    │
│  │  - Build Edge from row/record                   │    │
│  │  - Apply transformations                        │    │
│  └────────────────┬────────────────────────────────┘    │
└───────────────────┼──────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────────────────┐
│  Phase 4: Batch Insertion                               │
│  ┌─────────────────────────────────────────────────┐    │
│  │ InsertTask (multi-threaded)                     │    │
│  │  - Buffer elements (batch size: 500 default)    │    │
│  │  - Bulk insert via hugegraph-client API         │    │
│  │  - Error handling & retry logic                 │    │
│  └────────────────┬────────────────────────────────┘    │
└───────────────────┼──────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────────────────┐
│             HugeGraph Server                             │
└──────────────────────────────────────────────────────────┘
```

### Key Design Patterns

#### 1. Factory Pattern (Source Creation)
```java
public interface Source {
    Fetcher createFetcher();
}

// Factory method
public static Source create(SourceType type, SourceConfig config) {
    switch (type) {
        case FILE:
            return new FileSource(config);
        case HDFS:
            return new HDFSSource(config);
        case JDBC:
            return new JDBCSource(config);
        case KAFKA:
            return new KafkaSource(config);
        default:
            throw new IllegalArgumentException();
    }
}
```

#### 2. Strategy Pattern (ID Generation)
Different strategies for generating vertex/edge IDs:
- `PrimaryKeyIdStrategy`: Use primary key fields
- `CustomIdStrategy`: User-defined ID field
- `AutomaticIdStrategy`: Server-generated IDs

#### 3. Template Method Pattern (Parsing)
```java
abstract class AbstractReader {
    // Template method
    public final List<Record> read() {
        open();
        List<Record> records = parseRecords();
        close();
        return records;
    }
    
    protected abstract void open();
    protected abstract List<Record> parseRecords();
    protected abstract void close();
}
```

## 3. hugegraph-hubble Architecture

### Frontend Architecture (React + MobX)
```
┌──────────────────────────────────────────────────────────┐
│                    Hubble Frontend                        │
├──────────────────────────────────────────────────────────┤
│                                                           │
│  ┌────────────────────────────────────────────────┐     │
│  │  Presentation Layer (React Components)         │     │
│  │                                                 │     │
│  │  ┌──────────────┐  ┌──────────────┐           │     │
│  │  │ GraphManager │  │ DataAnalyze  │           │     │
│  │  │   Pages      │  │    Pages     │           │     │
│  │  └──────────────┘  └──────────────┘           │     │
│  │  ┌──────────────┐  ┌──────────────┐           │     │
│  │  │ SchemaManage │  │ DataImport   │           │     │
│  │  │   Pages      │  │    Pages     │           │     │
│  │  └──────────────┘  └──────────────┘           │     │
│  └────────────────┬───────────────────────────────┘     │
│                   │                                       │
│                   ▼                                       │
│  ┌────────────────────────────────────────────────┐     │
│  │  State Management Layer (MobX Stores)          │     │
│  │                                                 │     │
│  │  ┌──────────────────────┐                      │     │
│  │  │ GraphManagementStore │ (graph connections)  │     │
│  │  ├──────────────────────┤                      │     │
│  │  │ DataAnalyzeStore     │ (query & analysis)   │     │
│  │  ├──────────────────────┤                      │     │
│  │  │ SchemaStore          │ (schema operations)  │     │
│  │  ├──────────────────────┤                      │     │
│  │  │ DataImportStore      │ (data loading)       │     │
│  │  └──────────────────────┘                      │     │
│  └────────────────┬───────────────────────────────┘     │
│                   │                                       │
│                   ▼                                       │
│  ┌────────────────────────────────────────────────┐     │
│  │  API Service Layer                             │     │
│  │  (HTTP requests to backend)                    │     │
│  └────────────────┬───────────────────────────────┘     │
└───────────────────┼──────────────────────────────────────┘
                    │
                    ▼ HTTP/REST API
┌──────────────────────────────────────────────────────────┐
│               Hubble Backend (Spring Boot)                │
├──────────────────────────────────────────────────────────┤
│                                                           │
│  ┌────────────────────────────────────────────────┐     │
│  │  Controller Layer                              │     │
│  │  ┌──────────────┐  ┌──────────────┐           │     │
│  │  │ GraphConn    │  │ Schema       │           │     │
│  │  │ Controller   │  │ Controller   │           │     │
│  │  ├──────────────┤  ├──────────────┤           │     │
│  │  │ Query        │  │ DataImport   │           │     │
│  │  │ Controller   │  │ Controller   │           │     │
│  │  └──────────────┘  └──────────────┘           │     │
│  └────────────────┬───────────────────────────────┘     │
│                   │                                       │
│                   ▼                                       │
│  ┌────────────────────────────────────────────────┐     │
│  │  Service Layer (Business Logic)               │     │
│  │  ┌──────────────┐  ┌──────────────┐           │     │
│  │  │ GraphConn    │  │ Schema       │           │     │
│  │  │ Service      │  │ Service      │           │     │
│  │  ├──────────────┤  ├──────────────┤           │     │
│  │  │ Query        │  │ DataImport   │           │     │
│  │  │ Service      │  │ Service      │           │     │
│  │  └──────────────┘  └──────────────┘           │     │
│  └────────────────┬───────────────────────────────┘     │
│                   │                                       │
│                   ▼                                       │
│  ┌────────────────────────────────────────────────┐     │
│  │  Repository Layer (Data Persistence)           │     │
│  │  - File-based storage (local disk)             │     │
│  │  - Graph connection metadata                   │     │
│  └────────────────┬───────────────────────────────┘     │
└───────────────────┼──────────────────────────────────────┘
                    │
                    ▼ REST API (via hugegraph-client)
┌──────────────────────────────────────────────────────────┐
│             HugeGraph Server                             │
└──────────────────────────────────────────────────────────┘
```

### Key Design Patterns (Hubble)

#### 1. Observer Pattern (MobX)
```typescript
// Store definition
class GraphManagementStore {
  @observable currentGraph: GraphConnection | null = null;
  @observable graphList: GraphConnection[] = [];
  
  @action
  async loadGraphs() {
    const response = await api.getGraphs();
    this.graphList = response.data;
  }
  
  @computed
  get activeGraphName() {
    return this.currentGraph?.name || 'None';
  }
}

// Component observing store
@observer
class GraphSelector extends React.Component {
  render() {
    const { graphStore } = this.props;
    return <div>{graphStore.activeGraphName}</div>;
  }
}
```

#### 2. Repository Pattern (Backend)
```java
// Entity
@Entity
public class GraphConnection {
    @Id
    private Long id;
    private String name;
    private String host;
    private Integer port;
    // ...
}

// Repository interface
public interface GraphConnectionRepository {
    GraphConnection save(GraphConnection connection);
    GraphConnection findById(Long id);
    List<GraphConnection> findAll();
    void deleteById(Long id);
}

// Service using repository
@Service
public class GraphConnectionService {
    @Autowired
    private GraphConnectionRepository repository;
    
    public GraphConnection create(GraphConnection connection) {
        return repository.save(connection);
    }
}
```

## 4. hugegraph-tools Architecture

### Command Pattern
```
┌─────────────────────────────────────────┐
│        CLI Entry Point                  │
└───────────────┬─────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────┐
│    Command Router                       │
│    (parse args, dispatch command)       │
└───────────────┬─────────────────────────┘
                │
                ├─► backup              (GraphBackupCommand)
                ├─► restore             (GraphRestoreCommand)
                ├─► deploy              (DeployCommand)
                ├─► graph-list          (GraphListCommand)
                ├─► graph-clear         (GraphClearCommand)
                └─► graph-mode-set      (GraphModeCommand)
```

**Command Interface**:
```java
public interface Command {
    String name();
    void execute(String[] args);
}

// Example implementation
public class BackupCommand implements Command {
    public String name() { return "backup"; }
    
    public void execute(String[] args) {
        // Parse options
        String graph = parseGraphOption(args);
        String directory = parseDirectoryOption(args);
        
        // Execute backup via client API
        HugeClient client = createClient();
        client.graphs().backup(graph, directory);
    }
}
```

## Cross-Cutting Concerns

### Error Handling Strategy

**Client/Loader/Tools**:
```java
try {
    // Operation
} catch (ServerException e) {
    // Server-side error (4xx, 5xx)
    log.error("Server error: {}", e.getMessage());
    throw new LoaderException("Failed to load data", e);
} catch (ClientException e) {
    // Client-side error (network, serialization)
    log.error("Client error: {}", e.getMessage());
    throw new LoaderException("Client communication failed", e);
}
```

### Logging Strategy

**All modules use Log4j2**:
```java
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyClass {
    private static final Logger LOG = LogManager.getLogger(MyClass.class);
    
    public void method() {
        LOG.debug("Debug message");
        LOG.info("Info message");
        LOG.warn("Warning message");
        LOG.error("Error message", exception);
    }
}
```

### Configuration Management

**Loader** uses JSON structure files:
```json
{
  "version": "2.0",
  "vertices": [
    {
      "label": "person",
      "input": {
        "type": "file",
        "path": "data/persons.csv",
        "format": "CSV"
      },
      "mapping": {
        "id": "id",
        "properties": {
          "name": "name",
          "age": "age"
        }
      }
    }
  ]
}
```

**Hubble** uses Spring properties:
```properties
server.port=8088
spring.application.name=hugegraph-hubble
graph.server.host=localhost
graph.server.port=8080
```