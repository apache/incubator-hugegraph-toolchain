# Common Development Workflows - HugeGraph Toolchain

## Daily Development Workflows

### 1. Starting a New Feature

**Step 1: Create Feature Branch**
```bash
# Update master
git checkout master
git pull origin master

# Create feature branch
git checkout -b feature/add-batch-query-api
```

**Step 2: Make Changes**
```bash
# Edit code in your IDE
# Follow code style guidelines (see code_style_and_conventions.md)
```

**Step 3: Local Testing**
```bash
# Run unit tests
cd hugegraph-client
mvn test -Dtest=UnitTestSuite -ntp

# Run checkstyle
mvn checkstyle:check
```

**Step 4: Commit Changes**
```bash
git add .
git commit -m "feat(client): add batch query API for vertices"
```

**Step 5: Push and Create PR**
```bash
git push origin feature/add-batch-query-api
# Open PR on GitHub
```

### 2. Fixing a Bug

**Step 1: Reproduce the Bug**
```bash
# Write a failing test first (TDD approach)
cd hugegraph-loader
vim src/test/java/org/apache/hugegraph/loader/test/functional/CSVLoadTest.java

# Add test case
@Test
public void testHandleEmptyCSVFile() {
    // Test that reproduces the bug
}

# Run test - should fail
mvn test -Dtest=CSVLoadTest#testHandleEmptyCSVFile -ntp
```

**Step 2: Fix the Bug**
```bash
# Edit source code to fix the issue
vim src/main/java/org/apache/hugegraph/loader/reader/CSVReader.java
```

**Step 3: Verify Fix**
```bash
# Run test again - should pass
mvn test -Dtest=CSVLoadTest#testHandleEmptyCSVFile -ntp

# Run all related tests
mvn test -P file
```

**Step 4: Commit with Issue Reference**
```bash
git add .
git commit -m "fix(loader): handle empty CSV files correctly

Fixes #123

Previously, the loader would throw NullPointerException when
encountering empty CSV files. Now it gracefully skips empty files
and logs a warning."
```

### 3. Adding Tests for Existing Code

**Step 1: Identify Coverage Gaps**
```bash
# Generate coverage report
mvn test jacoco:report

# Open report
open target/site/jacoco/index.html

# Find classes with low coverage
```

**Step 2: Write Tests**
```bash
# Create test class if doesn't exist
vim src/test/java/org/apache/hugegraph/client/RestClientTest.java
```

```java
public class RestClientTest {
    @Test
    public void testConnectionTimeout() {
        // Test timeout handling
    }
    
    @Test
    public void testRetryOnNetworkError() {
        // Test retry logic
    }
}
```

**Step 3: Add to Test Suite**
```java
@RunWith(Suite.class)
@Suite.SuiteClasses({
    // ... existing tests
    RestClientTest.class  // Add new test
})
public class UnitTestSuite {}
```

### 4. Refactoring Code

**Step 1: Ensure Tests Pass**
```bash
# Run all tests before refactoring
mvn test
```

**Step 2: Make Changes Incrementally**
```bash
# Small, focused changes
# Run tests after each change
mvn test -Dtest=RelevantTestClass -ntp
```

**Step 3: Verify All Tests Still Pass**
```bash
# Run full test suite
mvn test

# Check code style
mvn checkstyle:check
```

**Step 4: Commit**
```bash
git commit -m "refactor(client): extract common HTTP logic to base class

No functional changes, just code organization improvement."
```

## Module-Specific Workflows

### Working on hugegraph-client

**Setup Development Environment**
```bash
# Build client only
mvn clean install -pl hugegraph-client -am -DskipTests -ntp

# Start HugeGraph server for integration tests
# Option 1: Docker
docker run -d --name hugegraph -p 8080:8080 hugegraph/hugegraph

# Option 2: From source
./hugegraph-client/assembly/travis/install-hugegraph-from-source.sh b7998c1
```

**Development Cycle**
```bash
# 1. Edit code
vim src/main/java/org/apache/hugegraph/api/VertexAPI.java

# 2. Quick compile check
mvn compile -pl hugegraph-client -ntp

# 3. Run relevant tests
mvn test -Dtest=VertexApiTest -ntp

# 4. Full test suite (before commit)
mvn test -Dtest=UnitTestSuite -ntp
mvn test -Dtest=ApiTestSuite
```

### Working on hugegraph-loader

**Setup Development Environment**
```bash
# Build loader with dependencies
mvn install -pl hugegraph-client,hugegraph-loader -am -DskipTests -ntp

# Setup test environment
cd hugegraph-loader/assembly/travis
./install-hugegraph-from-source.sh 5b3d295

# For JDBC tests
./install-mysql.sh load_test root

# For HDFS tests
./install-hadoop.sh
```

**Testing New Data Source**
```bash
# 1. Create test data files
mkdir -p src/test/resources/my-test
echo "id,name,age" > src/test/resources/my-test/data.csv
echo "1,Alice,30" >> src/test/resources/my-test/data.csv

# 2. Create mapping config
vim src/test/resources/struct/my-test.json

# 3. Write test
vim src/test/java/org/apache/hugegraph/loader/test/functional/MySourceTest.java

# 4. Run test
mvn test -Dtest=MySourceTest -ntp
```

### Working on hugegraph-hubble

**Setup Development Environment**
```bash
# Ensure Node.js 18.20.8
node -v  # Must be 18.20.8

# Install dependencies
npm install -g yarn
cd hugegraph-hubble/hubble-fe
yarn install

# Install Python requirements (for build)
pip install -r ../hubble-dist/assembly/travis/requirements.txt
```

**Frontend Development Cycle**
```bash
cd hugegraph-hubble/hubble-fe

# 1. Edit code
vim src/components/GraphViewer.tsx

# 2. Run linter
yarn lint

# 3. Auto-fix formatting
npx prettier --write src/components/GraphViewer.tsx

# 4. Run tests
yarn test GraphViewer.test.tsx

# 5. Start dev server (optional)
yarn start
```

**Backend Development Cycle**
```bash
cd hugegraph-hubble/hubble-be

# 1. Edit code
vim src/main/java/org/apache/hugegraph/hubble/controller/GraphController.java

# 2. Run tests
mvn test -Dtest=GraphControllerTest -ntp

# 3. Build and run
mvn spring-boot:run
```

**Full Hubble Build**
```bash
# Build dependencies first
mvn install -pl hugegraph-client,hugegraph-loader -am -DskipTests -ntp

# Build hubble
cd hugegraph-hubble
mvn -e compile package -Dmaven.test.skip=true -ntp

# Start hubble
cd apache-hugegraph-hubble-incubating-*/
bin/start-hubble.sh -d

# Check logs
tail -f logs/hugegraph-hubble.log

# Access UI
open http://localhost:8088
```

### Working on hugegraph-client-go

**Setup Development Environment**
```bash
cd hugegraph-client-go

# Download dependencies
make prepare

# Setup Go environment
go env -w GO111MODULE=on
```

**Development Cycle**
```bash
# 1. Edit code
vim client.go

# 2. Format code
go fmt ./...

# 3. Vet code
go vet ./...

# 4. Run tests with race detector
make test

# 5. Build binary
make compile

# 6. Run binary
./hugegraph-client-go
```

## Troubleshooting Common Issues

### Issue: Maven Build Fails with Dependency Errors

**Solution 1: Clear Local Cache**
```bash
rm -rf ~/.m2/repository/org/apache/hugegraph
mvn clean install -U
```

**Solution 2: Use Stage Repository**
```bash
mvn clean install -P stage
```

### Issue: Tests Fail with "Connection Refused"

**Problem**: HugeGraph server not running

**Solution**:
```bash
# Check if server is running
curl http://localhost:8080/versions

# If not, start it
cd apache-hugegraph-*
bin/start-hugegraph.sh

# Wait for startup (check logs)
tail -f logs/hugegraph-server.log
```

### Issue: Checkstyle Violations

**Common Fixes**:
```bash
# Line too long (max 100 chars)
# Solution: Break into multiple lines

# Star imports
# Solution: Expand imports in IDE (IntelliJ: Ctrl+Alt+O)

# Wrong indentation
# Solution: Use 4 spaces, not tabs
# IntelliJ: Settings → Editor → Code Style → Java → Indent: 4

# Missing whitespace
# Solution: Add space around operators
# Before: if(x==5)
# After: if (x == 5)
```

### Issue: Frontend Build Fails

**Solution 1: Node.js Version**
```bash
# Check version
node -v

# If wrong version, use nvm
nvm install 18.20.8
nvm use 18.20.8
```

**Solution 2: Clear Cache**
```bash
cd hugegraph-hubble/hubble-fe
rm -rf node_modules yarn.lock
yarn install
```

**Solution 3: Memory Limit**
```bash
# Increase Node.js memory
export NODE_OPTIONS="--max-old-space-size=4096"
mvn clean package
```

### Issue: HDFS Tests Fail

**Solution**: Check Hadoop setup
```bash
# Verify Hadoop is running
jps | grep -E 'NameNode|DataNode'

# Check HDFS status
hadoop fs -ls /

# If issues, reinstall
./assembly/travis/install-hadoop.sh
```

### Issue: JDBC Tests Fail

**Solution**: Check MySQL
```bash
# Check MySQL is running
mysql -u root -proot -e "SHOW DATABASES;"

# Verify test database exists
mysql -u root -proot -e "USE load_test; SHOW TABLES;"

# If issues, reinstall
./assembly/travis/install-mysql.sh load_test root
```

## Release Workflow

### Preparing a Release

**Step 1: Update Version**
```bash
# Update root pom.xml
vim pom.xml
# Change <revision>1.7.0</revision> to <revision>1.8.0</revision>

# Update frontend version
vim hugegraph-hubble/hubble-fe/package.json
# Change "version": "1.7.0" to "version": "1.8.0"
```

**Step 2: Update CHANGELOG**
```bash
vim CHANGELOG.md
# Add release notes:
# ## [1.8.0] - 2025-02-01
# ### Added
# - New batch query API
# ### Fixed
# - CSV loading bug
```

**Step 3: Run Full Test Suite**
```bash
# Run all tests
mvn clean verify

# Run integration tests
cd hugegraph-client && mvn test -Dtest=ApiTestSuite
cd hugegraph-loader && mvn test -P file,hdfs,jdbc
```

**Step 4: Build Release Artifacts**
```bash
# Build with Apache release profile
mvn clean package -P apache-release -DskipTests

# Artifacts in hugegraph-dist/target/
ls hugegraph-dist/target/*.tar.gz
```

**Step 5: Create Release Tag**
```bash
git tag -a v1.8.0 -m "Release version 1.8.0"
git push origin v1.8.0
```

## Useful Development Commands

### Quick Checks
```bash
# Check what you've changed
git --no-pager diff
git --no-pager diff --staged

# Check recent commits
git --no-pager log --oneline -5

# Find files by name
find . -name "*Test.java" -type f

# Search in code
grep -r "RestClient" --include="*.java" .
```

### Clean Everything
```bash
# Clean Maven build
mvn clean

# Deep clean
find . -name target -type d -exec rm -rf {} +
find . -name .flattened-pom.xml -delete

# Clean frontend
cd hugegraph-hubble/hubble-fe
rm -rf node_modules build

# Clean Go
cd hugegraph-client-go
make clean
```

### Performance Profiling
```bash
# Maven build with timing
mvn clean install -Dorg.slf4j.simpleLogger.showDateTime=true

# Java heap dump on OutOfMemoryError
export MAVEN_OPTS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp"

# Go benchmarks
cd hugegraph-client-go
go test -bench=. -benchmem
```

## Git Hooks

### Pre-commit Hook (Optional)
```bash
vim .git/hooks/pre-commit
```

```bash
#!/bin/bash
# Pre-commit hook for HugeGraph Toolchain

# Run checkstyle
echo "Running checkstyle..."
mvn checkstyle:check -q
if [ $? -ne 0 ]; then
    echo "Checkstyle failed. Please fix violations."
    exit 1
fi

# Run license check
echo "Checking licenses..."
mvn apache-rat:check -q
if [ $? -ne 0 ]; then
    echo "License check failed. Please add Apache 2.0 headers."
    exit 1
fi

echo "Pre-commit checks passed."
exit 0
```

```bash
chmod +x .git/hooks/pre-commit
```

## IDE Configuration

### IntelliJ IDEA Setup

**Import Project**:
1. File → Open → Select `pom.xml`
2. Import as Maven project
3. Wait for dependency resolution

**Configure Code Style**:
1. Settings → Editor → Code Style → Java
2. Import Scheme → IntelliJ IDEA code style XML
3. Load from: `.editorconfig`

**Configure Checkstyle Plugin**:
1. Install Checkstyle-IDEA plugin
2. Settings → Tools → Checkstyle
3. Add configuration file: `tools/checkstyle.xml`

**Run Configurations**:
```xml
<!-- Client Tests -->
<configuration name="Client-UnitTests" type="JUnit">
  <module name="hugegraph-client" />
  <option name="TEST_OBJECT" value="class" />
  <option name="MAIN_CLASS_NAME" value="org.apache.hugegraph.unit.UnitTestSuite" />
</configuration>

<!-- Loader Tests -->
<configuration name="Loader-FileTests" type="JUnit">
  <module name="hugegraph-loader" />
  <option name="VM_PARAMETERS" value="-Pfile" />
</configuration>
```

### VS Code Setup

**Extensions**:
- Java Extension Pack
- Prettier (for Hubble frontend)
- ESLint
- Go (for client-go)

**Settings** (`.vscode/settings.json`):
```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "editor.formatOnSave": true,
  "editor.tabSize": 4,
  "editor.insertSpaces": true,
  "[typescript]": {
    "editor.tabSize": 2,
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[javascript]": {
    "editor.tabSize": 2,
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  }
}
```

## Continuous Learning

### Understanding the Codebase

**Start Here**:
1. Read module READMEs
2. Check `example/` directories for usage examples
3. Read test cases to understand expected behavior
4. Follow imports to understand dependencies

**Key Files to Understand**:
- `hugegraph-client/src/main/java/org/apache/hugegraph/driver/HugeClient.java`
- `hugegraph-loader/src/main/java/org/apache/hugegraph/loader/HugeGraphLoader.java`
- `hugegraph-hubble/hubble-fe/src/stores/` (MobX stores)
- `hugegraph-client-go/client.go`

### Documentation Resources
- Project Docs: https://hugegraph.apache.org/docs/
- API Docs: https://hugegraph.apache.org/docs/clients/restful-api/
- GitHub Issues: https://github.com/apache/hugegraph-toolchain/issues
- Mailing List: dev@hugegraph.apache.org
