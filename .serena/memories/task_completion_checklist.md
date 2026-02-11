# Task Completion Checklist for HugeGraph Toolchain

## Before Committing Code

### 1. Code Quality Checks

#### Java Modules (client, loader, tools, hubble-be, spark-connector)

**A. Checkstyle Validation**
```bash
# Run checkstyle on affected modules
mvn checkstyle:check

# Or for specific module
mvn checkstyle:check -pl hugegraph-client
```
**Must Pass**: No checkstyle violations allowed

**B. License Header Check**
```bash
# Verify all files have Apache 2.0 license headers
mvn apache-rat:check
```
**Must Pass**: All source files must have proper license headers

**C. EditorConfig Validation**
```bash
# Verify file formatting (indentation, line endings, etc.)
mvn editorconfig:check
```
**Must Pass**: All files must conform to .editorconfig rules

**D. Compilation**
```bash
# Ensure code compiles without errors
mvn clean compile -pl <module> -am -Dmaven.javadoc.skip=true -ntp
```
**Must Pass**: No compilation errors

#### Frontend Module (hubble-fe)

**A. Prettier Formatting**
```bash
cd hugegraph-hubble/hubble-fe

# Check formatting
npx prettier --check .

# Auto-fix if needed
npx prettier --write .
```
**Must Pass**: All files properly formatted

**B. Stylelint (CSS/Less)**
```bash
# Check CSS/Less files
npx stylelint "**/*.{css,less}"

# Auto-fix if needed
npx stylelint "**/*.{css,less}" --fix
```
**Must Pass**: No linting errors

**C. TypeScript/JavaScript Linting**
```bash
# Run yarn lint
yarn lint
```
**Must Pass**: No linting errors

#### Go Module (client-go)

**A. Go Formatting**
```bash
cd hugegraph-client-go

# Format code
go fmt ./...

# Vet code
go vet ./...
```
**Must Pass**: No formatting or vet issues

### 2. Run Tests

#### Java Tests

**A. Unit Tests** (Always run)
```bash
# For hugegraph-client
cd hugegraph-client
mvn test -Dtest=UnitTestSuite -ntp

# For hugegraph-loader
cd hugegraph-loader
mvn test -P unit -ntp
```
**Must Pass**: All unit tests passing

**B. Integration/API Tests** (If API changes made)
```bash
# For hugegraph-client (requires HugeGraph server)
mvn test -Dtest=ApiTestSuite
mvn test -Dtest=FuncTestSuite

# For hugegraph-loader (requires HugeGraph server + data sources)
mvn test -P file
mvn test -P hdfs  # If HDFS changes
mvn test -P jdbc  # If JDBC changes
mvn test -P kafka # If Kafka changes
```
**Required**: If you modified API/integration code

#### Frontend Tests
```bash
cd hugegraph-hubble/hubble-fe
yarn test
```
**Must Pass**: All frontend tests passing

#### Go Tests
```bash
cd hugegraph-client-go
make test  # Runs with race detector
```
**Must Pass**: All tests passing with no race conditions

### 3. Build Verification

#### Full Module Build
```bash
# Build the module(s) you changed
mvn clean install -pl <module> -am -DskipTests -Dmaven.javadoc.skip=true -ntp
```
**Must Pass**: Build succeeds without errors

#### Hubble Build (if frontend/backend changed)
```bash
# Build dependencies
mvn install -pl hugegraph-client,hugegraph-loader -am -DskipTests -ntp

# Build hubble
cd hugegraph-hubble
mvn -e compile package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -ntp
```
**Must Pass**: Hubble builds successfully

### 4. Documentation Updates

**Check if any of these need updating:**
- [ ] Module README.md
- [ ] API documentation (JavaDoc)
- [ ] Code comments
- [ ] CHANGELOG (if applicable)
- [ ] Version numbers (if release)

### 5. Git Pre-commit

**A. Verify Changes**
```bash
# Check what you're committing
git status
git --no-pager diff --staged
```

**B. Ensure Proper Commit Message**
Format: `type(scope): subject`

Examples:
- `feat(client): add batch query support for vertices`
- `fix(loader): handle null values in CSV parsing`
- `docs(hubble): update installation instructions`
- `chore(deps): upgrade jackson to 2.12.3`
- `refactor(tools): simplify backup command logic`
- `test(loader): add HDFS connection retry tests`

**C. Verify No Unintended Files**
```bash
# Check .gitignore is working
git status --ignored
```
Do NOT commit:
- `target/` directories
- `*.iml`, `.idea/` files (IDE specific)
- `node_modules/`
- `.flattened-pom.xml`
- Log files
- Build artifacts

## Pull Request Checklist

Before opening a PR:

- [ ] All tests passing locally
- [ ] Code style checks passing (checkstyle, prettier, etc.)
- [ ] No merge conflicts with target branch
- [ ] PR description clearly explains:
  - What changed
  - Why it changed
  - How to test it
- [ ] Reference issue number (if applicable): `Fixes #123`
- [ ] Updated documentation (if applicable)
- [ ] Added tests for new functionality
- [ ] CI builds passing on GitHub Actions

## CI/CD Pipeline Checks

The following will be automatically checked by GitHub Actions:

### Java Client CI (`client-ci.yml`)
1. Compile hugegraph-client
2. Run UnitTestSuite
3. Run ApiTestSuite (requires HugeGraph server)
4. Run FuncTestSuite
5. Upload coverage to Codecov

### Loader CI (`loader-ci.yml`)
1. Install dependencies (Hadoop, MySQL, HugeGraph)
2. Compile client + loader
3. Run unit tests (`-P unit`)
4. Run file tests (`-P file`)
5. Run HDFS tests (`-P hdfs`)
6. Run JDBC tests (`-P jdbc`)
7. Run Kafka tests (`-P kafka`)
8. Upload coverage to Codecov

### Hubble CI (`hubble-ci.yml`)
1. Setup Node.js 18.20.8
2. Install frontend dependencies (yarn)
3. Build frontend (React + TypeScript)
4. Compile backend (Spring Boot)
5. Run tests
6. Package distribution

### Go Client CI (`client-go-ci.yml`)
1. Setup Go environment
2. Download dependencies
3. Run `make test` (with race detector)
4. Build binary

### Tools CI (`tools-ci.yml`)
1. Compile hugegraph-tools
2. Run tests
3. Package distribution

### Spark Connector CI (`spark-connector-ci.yml`)
1. Setup Scala environment
2. Compile spark-connector
3. Run tests

### CodeQL Analysis (`codeql-analysis.yml`)
- Security vulnerability scanning
- Code quality analysis

### License Checker (`license-checker.yml`)
- Verify Apache 2.0 license headers
- Check dependency licenses

## Common Issues and Solutions

### Issue: Checkstyle Failures
**Solution**:
1. Check error message for specific rule violation
2. Fix manually or use IDE auto-format (IntelliJ IDEA)
3. Common issues:
   - Line too long (max 100 chars)
   - Star imports
   - Missing whitespace
   - Wrong indentation (use 4 spaces)

### Issue: Test Failures
**Solution**:
1. Check if HugeGraph server is running (for API/Func tests)
2. Verify dependencies are installed (HDFS, MySQL, Kafka)
3. Check test logs for specific error
4. Run single test for debugging:
   ```bash
   mvn test -Dtest=ClassName#methodName -ntp
   ```

### Issue: Hubble Build Failures
**Solution**:
1. Verify Node.js version: `node -v` (must be 18.20.8)
2. Clear cache and reinstall:
   ```bash
   rm -rf node_modules yarn.lock
   yarn install
   ```
3. Check for frontend errors in build output

### Issue: Maven Build Hangs
**Solution**:
1. Kill stuck maven process: `pkill -9 -f maven`
2. Clear local repository cache:
   ```bash
   rm -rf ~/.m2/repository/org/apache/hugegraph
   ```
3. Retry with `-X` for debug output:
   ```bash
   mvn clean install -X
   ```

## Release-Specific Tasks

When preparing a release:

1. **Update Version Numbers**
   - Root `pom.xml`: `<revision>` property
   - Frontend: `package.json` version
   - Go: Version constants

2. **Update CHANGELOG**
   - Document new features
   - List bug fixes
   - Note breaking changes

3. **Run Full Test Suite**
   ```bash
   mvn clean verify -P apache-release
   ```

4. **Generate Distribution**
   ```bash
   mvn clean package -DskipTests
   ```

5. **Sign Artifacts** (for Apache release)
   ```bash
   mvn clean install -P apache-release
   ```

## Summary - Minimum Required Checks

**For any code change, ALWAYS run:**

```bash
# 1. Checkstyle (Java)
mvn checkstyle:check

# 2. License check
mvn apache-rat:check

# 3. EditorConfig
mvn editorconfig:check

# 4. Unit tests
mvn test -Dtest=UnitTestSuite -ntp  # or appropriate suite

# 5. Build
mvn clean install -DskipTests -ntp
```

**For frontend changes, ALSO run:**
```bash
cd hugegraph-hubble/hubble-fe
npx prettier --check .
npx stylelint "**/*.{css,less}"
yarn lint
yarn test
```

**For Go changes, ALSO run:**
```bash
cd hugegraph-client-go
go fmt ./...
go vet ./...
make test
```

---

**CRITICAL**: Do NOT commit code that fails any of the required checks. CI will fail and PR will be blocked.
