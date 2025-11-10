# Code Style and Conventions for HugeGraph Toolchain

## General Principles
- **Language**: English for all code, comments, and documentation
- **License**: All source files require Apache 2.0 license headers
- **Encoding**: UTF-8 for all files
- **Line Endings**: LF (Unix-style)
- **Final Newline**: Always insert final newline

## Java Code Style

### Basic Formatting
- **Indentation**: 4 spaces (NO TABS)
- **Continuation Indent**: 8 spaces
- **Line Length**: Maximum 100 characters
- **Line Wrapping**: Enabled for long lines
- **Blank Lines**:
  - Keep max 1 blank line in declarations
  - Keep max 1 blank line in code
  - 1 blank line around classes
  - 1 blank line after class header

### Naming Conventions
- **Package Names**: `^[a-z]+(\.[a-z][a-z0-9]*)*$`
  - Example: `org.apache.hugegraph.client`
- **Class Names**: `PascalCase` (e.g., `RestClient`, `GraphManager`)
- **Type Parameters**: `^[A-Z][a-zA-Z0-9]*$` (e.g., `T`, `K`, `V`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `DEFAULT_TIMEOUT`, `MAX_RETRIES`)
- **Variables**: `camelCase` starting with lowercase (e.g., `vertexId`, `edgeLabel`)
- **Methods**: `camelCase` starting with lowercase, must have 2+ chars
  - Pattern: `^[a-z][a-z0-9][a-zA-Z0-9_]*$`
  - Example: `getVertexById()`, `createEdge()`
- **Parameters**: `camelCase` (e.g., `userId`, `timeout`)

### Import Rules
- NO star imports (`import org.apache.*` forbidden)
- Remove unused imports
- Remove redundant imports
- Import order (configured in .editorconfig):
  1. Static imports
  2. `java.**`
  3. `javax.**`
  4. `org.**`
  5. `com.**`
  6. All others

### Prohibited Imports (Checkstyle)
- `java.util.logging.Logging`
- `sun.misc.BASE64Encoder/Decoder`
- Shaded/internal packages from Hadoop, HBase, Netty, etc.
- `org.codehaus.jackson` (use `com.fasterxml.jackson` instead)
- `org.jetbrains.annotations`

### Code Structure
- **Braces**:
  - Always use braces for if/while/for (multi-line)
  - `do-while` always requires braces
  - Opening brace on same line (K&R style)
- **Whitespace**:
  - No whitespace before: `,`, `;`, `.`, post-increment/decrement
  - Whitespace around operators: `=`, `+`, `-`, `*`, `/`, etc.
  - Proper padding in parentheses
- **Empty Blocks**: Only `{}` allowed (not `{ }`)

### Java-Specific Rules
- **Array Style**: `String[] args` (NOT `String args[]`)
- **Generic Whitespace**: Follow standard Java conventions
- **Equals/HashCode**: Must implement both or neither
- **Switch Statement**: Must have `default` case
- **Finalize**: No finalizers allowed
- **System.out.println**: PROHIBITED in source code (use logger)

### Comments and JavaDoc
- **Line Comments**: Not at first column, use proper indentation
- **JavaDoc**:
  - Add `<p>` tag on empty lines
  - Do not wrap if one line
  - Comment indentation: 4 spaces

### Annotations
- Each annotation on separate line (for methods/constructors)
- Single parameterless annotation allowed on same line (other contexts)

## Maven POM Style

### XML Formatting
- **Indentation**: 4 spaces
- **Line Length**: Maximum 120 characters
- **Text Wrap**: Off for XML
- **Empty Tags**: Space inside (`<tag />`)

### POM Organization
```xml
<project>
  <modelVersion/>
  <parent/>
  <groupId/>
  <artifactId/>
  <version/>
  <packaging/>
  
  <name/>
  <description/>
  <url/>
  
  <licenses/>
  <developers/>
  <scm/>
  
  <properties/>
  <dependencyManagement/>
  <dependencies/>
  
  <build/>
  <profiles/>
</project>
```

## Frontend Code Style (Hubble)

### TypeScript/JavaScript
- **Formatter**: Prettier (configured in `.prettierrc`)
- **Linter**: ESLint/TSLint
- **Naming**: 
  - Components: PascalCase (`GraphViewer.tsx`)
  - Files: kebab-case or PascalCase
  - Variables: camelCase

### CSS/Less
- **Linter**: Stylelint (configured in `.stylelintrc`)
- **Naming**: kebab-case for class names
- **Indentation**: 2 spaces

### Pre-commit Hooks
- **Husky**: Runs on git commit
- **lint-staged**: Auto-format staged files
- Configuration: `.lintstagedrc.yml`

## Go Code Style (client-go)

### Standard Go Conventions
- Follow official Go formatting (`gofmt`)
- Use `go vet` for static analysis
- Run tests with race detector: `go test -race`

### Naming
- Exported names: Start with uppercase
- Unexported names: Start with lowercase
- Package names: Short, lowercase, single word

## Design Patterns and Architecture

### hugegraph-client Patterns

#### Manager Pattern
Separate managers for different API domains:
```java
// Schema operations
SchemaManager schemaManager = hugegraph.schema();

// Graph operations
GraphManager graphManager = hugegraph.graph();

// Traversal algorithms
TraverserManager traverser = hugegraph.traverser();

// Async jobs
JobManager jobManager = hugegraph.job();

// Authentication
AuthManager authManager = hugegraph.auth();
```

#### Builder Pattern
Fluent API for constructing schema elements:
```java
VertexLabel person = schema.vertexLabel("person")
    .properties("name", "age", "city")
    .primaryKeys("name")
    .nullableKeys("city")
    .create();
```

#### RESTful Wrapper
- `RestClient`: Base HTTP communication layer
- All API classes extend or use `RestClient`
- Consistent error handling with custom exceptions

### hugegraph-loader Patterns

#### Pipeline Architecture
```
Source → Parser → Transformer → Builder → BatchInserter → HugeGraph
```

- **ParseTask**: Read and parse data from sources
- **InsertTask**: Batch insert into HugeGraph
- **ElementBuilder**: Construct vertices/edges from raw data

#### Source Abstraction
Unified interface for different data sources:
```java
interface Source {
    Fetcher createFetcher();
}

// Implementations:
- FileSource (CSV, JSON, TXT)
- HDFSSource
- JDBCSource
- KafkaSource
```

### hugegraph-hubble Patterns

#### Frontend Architecture
- **Store Pattern**: MobX stores for state management
  - `GraphManagementStore`: Graph connection management
  - `DataAnalyzeStore`: Query and analysis state
  - `SchemaStore`: Schema management state
- **Component Hierarchy**: Container → Component → Sub-component

#### Backend Architecture (Spring Boot)
- **Controller**: HTTP request handling
- **Service**: Business logic layer
- **Repository**: Data persistence (local file-based)
- **DTO/Entity**: Data transfer and domain objects

## File Organization

### Java Package Structure
```
org.apache.hugegraph/
├── api/              # RESTful API implementations
├── client/           # Client interfaces and implementations
├── driver/           # Driver layer
├── structure/        # Graph structure elements (Vertex, Edge, etc.)
├── exception/        # Custom exceptions
├── serializer/       # JSON serialization/deserialization
├── util/             # Utility classes
└── version/          # Version information
```

### Test Organization
```
src/test/java/
├── unit/             # Unit tests (no external dependencies)
├── api/              # API integration tests (require server)
└── functional/       # End-to-end functional tests
```

## Version Control Practices

### Commit Messages
- Format: `type(scope): subject`
- Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
- Examples:
  - `feat(client): add batch vertex query API`
  - `fix(loader): handle empty CSV files correctly`
  - `chore(hubble): update Node.js version to 18.20.8`

### Branch Naming
- `master`: Main development branch
- `release-*`: Release branches
- `feature/*`: Feature branches
- `fix/*`: Bug fix branches

## Testing Conventions

### Test Class Naming
- Unit tests: `*Test.java` (e.g., `RestClientTest.java`)
- Test suites: `*TestSuite.java` (e.g., `UnitTestSuite.java`)

### Test Method Naming
- Descriptive names: `testGetVertexById()`, `testCreateEdgeWithInvalidLabel()`
- Use `@Test` annotation (JUnit 4)

### Test Organization
- Group tests into suites:
  - `UnitTestSuite`: No external dependencies
  - `ApiTestSuite`: API integration tests
  - `FuncTestSuite`: Functional/E2E tests

## Documentation Standards

### JavaDoc Requirements
- All public APIs must have JavaDoc
- Include `@param`, `@return`, `@throws` tags
- Example usage in class-level JavaDoc

### README Structure
```markdown
# Module Name

## Features
## Quick Start
## Usage
## Doc
## License
```

## Error Handling

### Java Exceptions
- Use custom exceptions: `HugeException`, `ServerException`, `ClientException`
- Proper exception chaining with causes
- Meaningful error messages

### Go Error Handling
- Return errors explicitly: `func() (result, error)`
- Handle errors at call site
- Wrap errors with context: `fmt.Errorf("context: %w", err)`
