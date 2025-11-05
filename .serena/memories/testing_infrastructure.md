# Testing Infrastructure - HugeGraph Toolchain

## Testing Philosophy

- **Unit Tests**: Test individual components in isolation, no external dependencies
- **Integration Tests**: Test interactions with HugeGraph server and external systems
- **Functional Tests**: End-to-end workflows testing complete features

## Test Organization

### Test Suite Structure (Java Modules)

All Java modules use **JUnit 4** with test suites:

```
src/test/java/
├── unit/
│   ├── *Test.java          # Individual unit tests
│   └── UnitTestSuite.java   # Suite aggregator
├── api/
│   ├── *ApiTest.java        # API integration tests
│   └── ApiTestSuite.java
└── functional/
    ├── *FuncTest.java       # Functional tests
    └── FuncTestSuite.java
```

### Test Naming Conventions

**Class Names**:
- Unit tests: `ClassNameTest.java`
- Integration tests: `ClassNameApiTest.java` or `ClassNameIntegrationTest.java`
- Test suites: `UnitTestSuite.java`, `ApiTestSuite.java`, `FuncTestSuite.java`

**Method Names**:
- Descriptive: `testGetVertexById()`, `testCreateEdgeWithInvalidLabel()`
- Pattern: `test<MethodUnderTest><Condition>()`

## Module-Specific Testing

## 1. hugegraph-client Tests

### Test Suites

#### UnitTestSuite
**Purpose**: Test serialization, utilities, and internal logic  
**No External Dependencies**: Can run without HugeGraph server

**Example Tests**:
```java
@RunWith(Suite.class)
@Suite.SuiteClasses({
    VertexSerializerTest.class,
    PathSerializerTest.class,
    RestResultTest.class,
    BatchElementRequestTest.class,
    PropertyKeyTest.class,
    IndexLabelTest.class,
    CommonUtilTest.class,
    IdUtilTest.class,
    SplicingIdGeneratorTest.class
})
public class UnitTestSuite {}
```

**Run Command**:
```bash
cd hugegraph-client
mvn test -Dtest=UnitTestSuite -ntp
```

#### ApiTestSuite
**Purpose**: Test REST API interactions  
**Requires**: HugeGraph server running on localhost:8080

**Example Tests**:
- `VertexApiTest`: Test vertex CRUD operations
- `EdgeApiTest`: Test edge CRUD operations
- `SchemaApiTest`: Test schema management
- `TraverserApiTest`: Test graph traversal algorithms
- `GremlinApiTest`: Test Gremlin query execution

**Run Command**:
```bash
cd hugegraph-client
mvn test -Dtest=ApiTestSuite
```

#### FuncTestSuite
**Purpose**: End-to-end functional scenarios  
**Requires**: HugeGraph server + complete setup

**Run Command**:
```bash
cd hugegraph-client
mvn test -Dtest=FuncTestSuite
```

### Test Setup/Teardown Pattern

```java
public class VertexApiTest extends BaseApiTest {
    private static HugeClient client;
    private static GraphManager graph;
    
    @BeforeClass
    public static void setup() {
        client = new HugeClient("http://localhost:8080", "hugegraph");
        graph = client.graph();
        
        // Setup schema
        setupSchema();
    }
    
    @AfterClass
    public static void teardown() {
        client.close();
    }
    
    @Before
    public void prepare() {
        // Clear data before each test
        graph.clearVertices();
    }
    
    @Test
    public void testAddVertex() {
        Vertex vertex = graph.addVertex("person", "name", "Alice");
        assertNotNull(vertex.id());
        assertEquals("Alice", vertex.property("name"));
    }
}
```

### Mocking (Unit Tests)

```java
public class RestClientTest {
    @Mock
    private RestClient mockClient;
    
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }
    
    @Test
    public void testGetVertex() {
        // Mock response
        Vertex expectedVertex = new Vertex("person");
        when(mockClient.get("/vertices/1", Vertex.class))
            .thenReturn(expectedVertex);
        
        // Test
        Vertex result = mockClient.get("/vertices/1", Vertex.class);
        assertEquals(expectedVertex, result);
        
        // Verify
        verify(mockClient).get("/vertices/1", Vertex.class);
    }
}
```

## 2. hugegraph-loader Tests

### Test Profiles (Maven)

#### Profile: unit
**Purpose**: Unit tests only  
**Run Command**:
```bash
cd hugegraph-loader
mvn test -P unit -ntp
```

**Tests**: Parser, mapper, builder unit tests

#### Profile: file
**Purpose**: File source loading tests  
**Requires**: Test data files (CSV, JSON, TXT)  
**Run Command**:
```bash
mvn test -P file
```

**Test Resources**:
```
src/test/resources/
├── file/
│   ├── persons.csv
│   ├── knows.json
│   └── struct.json      # Mapping configuration
```

#### Profile: hdfs
**Purpose**: HDFS source loading tests  
**Requires**: Hadoop HDFS cluster (local or remote)  
**Setup**: CI installs Hadoop via `install-hadoop.sh`  
**Run Command**:
```bash
mvn test -P hdfs
```

#### Profile: jdbc
**Purpose**: Database source loading tests  
**Requires**: MySQL running (CI uses Docker)  
**Setup**: CI installs MySQL via `install-mysql.sh`  
**Run Command**:
```bash
mvn test -P jdbc
```

**Test Databases**: MySQL, PostgreSQL, Oracle (if driver available)

#### Profile: kafka
**Purpose**: Kafka streaming source tests  
**Requires**: Kafka broker running  
**Run Command**:
```bash
mvn test -P kafka
```

### Test Data Management

**Test Resources Structure**:
```
src/test/resources/
├── struct/
│   ├── vertices.json     # Vertex mapping configs
│   └── edges.json        # Edge mapping configs
├── file/
│   ├── vertex_person.csv
│   ├── edge_knows.csv
│   └── example.json
├── jdbc/
│   └── init.sql          # Database init script
└── log4j2.xml            # Test logging config
```

### Integration Test Pattern (Loader)

```java
public class FileLoadTest extends BaseLoadTest {
    private static LoadContext context;
    private static HugeClient client;
    
    @BeforeClass
    public static void setup() {
        // Start HugeGraph server (CI does this)
        client = new HugeClient("http://localhost:8080", "hugegraph");
        
        // Create schema
        createSchema(client);
        
        // Prepare load context
        context = new LoadContext();
        context.setStructPath("src/test/resources/struct/vertices.json");
    }
    
    @Test
    public void testLoadCSV() {
        // Load data
        LoadOptions options = new LoadOptions();
        options.file = "src/test/resources/file/vertex_person.csv";
        
        HugeGraphLoader loader = new HugeGraphLoader(context, options);
        loader.load();
        
        // Verify
        List<Vertex> vertices = client.graph().listVertices("person");
        assertEquals(100, vertices.size());
    }
}
```

## 3. hugegraph-hubble Tests

### Backend Tests (Java/Spring Boot)

**Test Framework**: JUnit 4 + Spring Test + MockMvc

**Example Controller Test**:
```java
@RunWith(SpringRunner.class)
@WebMvcTest(GraphConnectionController.class)
public class GraphConnectionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private GraphConnectionService service;
    
    @Test
    public void testCreateConnection() throws Exception {
        GraphConnection connection = new GraphConnection();
        connection.setName("test-graph");
        connection.setHost("localhost");
        connection.setPort(8080);
        
        when(service.create(any())).thenReturn(connection);
        
        mockMvc.perform(post("/api/graph-connections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(connection)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("test-graph"));
    }
}
```

### Frontend Tests (React/TypeScript)

**Test Framework**: Jest + React Testing Library

**Run Command**:
```bash
cd hugegraph-hubble/hubble-fe
yarn test
```

**Example Component Test**:
```typescript
import { render, screen, fireEvent } from '@testing-library/react';
import GraphSelector from '../GraphSelector';

describe('GraphSelector', () => {
  it('renders graph list', () => {
    const graphs = [
      { id: 1, name: 'graph1' },
      { id: 2, name: 'graph2' }
    ];
    
    render(<GraphSelector graphs={graphs} />);
    
    expect(screen.getByText('graph1')).toBeInTheDocument();
    expect(screen.getByText('graph2')).toBeInTheDocument();
  });
  
  it('calls onSelect when graph clicked', () => {
    const onSelect = jest.fn();
    const graphs = [{ id: 1, name: 'graph1' }];
    
    render(<GraphSelector graphs={graphs} onSelect={onSelect} />);
    
    fireEvent.click(screen.getByText('graph1'));
    expect(onSelect).toHaveBeenCalledWith(graphs[0]);
  });
});
```

**Store Test (MobX)**:
```typescript
import GraphManagementStore from '../stores/GraphManagementStore';

describe('GraphManagementStore', () => {
  let store: GraphManagementStore;
  
  beforeEach(() => {
    store = new GraphManagementStore();
  });
  
  it('loads graphs from API', async () => {
    // Mock API
    jest.spyOn(api, 'getGraphs').mockResolvedValue([
      { id: 1, name: 'graph1' }
    ]);
    
    await store.loadGraphs();
    
    expect(store.graphList).toHaveLength(1);
    expect(store.graphList[0].name).toBe('graph1');
  });
});
```

## 4. hugegraph-client-go Tests

**Test Framework**: Go standard testing + testify

**Run Command**:
```bash
cd hugegraph-client-go
make test  # Runs: go test -race -timeout 30s
```

**Test Structure**:
```
.
├── client_test.go
├── graph_test.go
├── schema_test.go
└── traverser_test.go
```

**Example Test**:
```go
package hugegraph

import (
    "testing"
    "github.com/stretchr/testify/assert"
)

func TestCreateVertex(t *testing.T) {
    client := NewClient("http://localhost:8080", "hugegraph")
    defer client.Close()
    
    vertex := Vertex{
        Label: "person",
        Properties: map[string]interface{}{
            "name": "Alice",
            "age": 30,
        },
    }
    
    created, err := client.Graph().AddVertex(vertex)
    assert.NoError(t, err)
    assert.NotEmpty(t, created.ID)
    assert.Equal(t, "Alice", created.Properties["name"])
}

func TestGetVertexNotFound(t *testing.T) {
    client := NewClient("http://localhost:8080", "hugegraph")
    defer client.Close()
    
    _, err := client.Graph().GetVertex("non-existent-id")
    assert.Error(t, err)
}
```

**Benchmark Tests**:
```go
func BenchmarkAddVertex(b *testing.B) {
    client := NewClient("http://localhost:8080", "hugegraph")
    defer client.Close()
    
    b.ResetTimer()
    for i := 0; i < b.N; i++ {
        client.Graph().AddVertex(Vertex{
            Label: "person",
            Properties: map[string]interface{}{"name": "test"},
        })
    }
}
```

## CI/CD Testing Pipeline

### GitHub Actions Workflow

Each module has its own CI workflow:

#### client-ci.yml
```yaml
steps:
  - name: Install HugeGraph Server
    run: ./assembly/travis/install-hugegraph-from-source.sh
  
  - name: Compile
    run: mvn compile -pl hugegraph-client -ntp
  
  - name: Run Unit Tests
    run: mvn test -Dtest=UnitTestSuite -ntp
  
  - name: Run API Tests
    run: mvn test -Dtest=ApiTestSuite
  
  - name: Run Func Tests
    run: mvn test -Dtest=FuncTestSuite
  
  - name: Upload Coverage
    uses: codecov/codecov-action@v3
```

#### loader-ci.yml
```yaml
steps:
  - name: Install Dependencies
    run: |
      ./assembly/travis/install-hadoop.sh
      ./assembly/travis/install-mysql.sh
      ./assembly/travis/install-hugegraph-from-source.sh
  
  - name: Run Tests
    run: |
      mvn test -P unit
      mvn test -P file
      mvn test -P hdfs
      mvn test -P jdbc
      mvn test -P kafka
```

### Test Utilities

#### CI Setup Scripts
```bash
# Install HugeGraph server from source
./assembly/travis/install-hugegraph-from-source.sh <commit-id>

# Install Hadoop for HDFS tests
./assembly/travis/install-hadoop.sh

# Install MySQL for JDBC tests
./assembly/travis/install-mysql.sh <db-name> <password>
```

## Test Coverage

### Coverage Tools
- **Java**: JaCoCo Maven plugin
- **JavaScript/TypeScript**: Jest built-in coverage
- **Go**: go test -cover

### Generating Coverage Reports

**Java (JaCoCo)**:
```bash
mvn test jacoco:report
# Report: target/site/jacoco/index.html
```

**Frontend (Jest)**:
```bash
cd hugegraph-hubble/hubble-fe
yarn test --coverage
# Report: coverage/lcov-report/index.html
```

**Go**:
```bash
cd hugegraph-client-go
go test -coverprofile=coverage.out ./...
go tool cover -html=coverage.out
```

### Coverage Targets
- Unit tests: Aim for 80%+ coverage
- Integration tests: Cover critical paths
- Functional tests: Cover end-to-end scenarios

## Common Testing Patterns

### Test Data Builders
```java
public class TestDataBuilder {
    public static Vertex createPersonVertex(String name, int age) {
        return new Vertex("person")
            .property("name", name)
            .property("age", age);
    }
    
    public static Edge createKnowsEdge(Vertex source, Vertex target) {
        return source.addEdge("knows", target)
            .property("date", "2023-01-01");
    }
}
```

### Test Assertions (Custom)
```java
public class GraphAssertions {
    public static void assertVertexExists(HugeClient client, Object id) {
        Vertex vertex = client.graph().getVertex(id);
        assertNotNull("Vertex should exist", vertex);
    }
    
    public static void assertEdgeCount(HugeClient client, 
                                       String label, int expected) {
        List<Edge> edges = client.graph().listEdges(label);
        assertEquals("Edge count mismatch", expected, edges.size());
    }
}
```

### Parameterized Tests (JUnit 4)
```java
@RunWith(Parameterized.class)
public class IdGeneratorTest {
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "alice", "person:alice" },
            { "bob", "person:bob" },
            { "charlie", "person:charlie" }
        });
    }
    
    private String input;
    private String expected;
    
    public IdGeneratorTest(String input, String expected) {
        this.input = input;
        this.expected = expected;
    }
    
    @Test
    public void testGenerateId() {
        String result = IdGenerator.generate("person", input);
        assertEquals(expected, result);
    }
}
```

## Debugging Tests

### Running Single Test
```bash
# Java
mvn test -Dtest=ClassName#methodName -ntp

# Go
go test -run TestFunctionName -v

# Frontend
yarn test ComponentName.test.tsx
```

### Debug Mode (Java)
```bash
# Run with remote debugging enabled
mvnDebug test -Dtest=ClassName
# Then attach debugger to port 8000
```

### Verbose Output
```bash
# Maven verbose
mvn test -X

# Go verbose
go test -v

# Frontend verbose
yarn test --verbose
```