# Memory Index - HugeGraph Toolchain Project

## Onboarding Complete ✓

This project has been successfully initialized with Serena MCP. Below is an index of all available memory files.

## Available Memories (7 Core Files)

### Core Project Information

1. **project_overview.md** (125 lines)
   - Project purpose and goals
   - All 6 modules (client, loader, hubble, tools, client-go, spark-connector)
   - Technology stack (Java 8, Node.js 18.20.8, Go, React, Spring Boot)
   - Module dependencies and relationships
   - External dependencies
   - Project structure

### Code Quality and Style

2. **code_style_and_conventions.md** (311 lines)
   - Java code style (indentation, naming, formatting)
   - Naming conventions (classes, methods, variables)
   - Import rules and prohibited imports
   - Maven POM style
   - Frontend code style (TypeScript, CSS/Less)
   - Go code style
   - Design patterns used in each module
   - File organization standards
   - Commit message format

3. **task_completion_checklist.md** (372 lines) ⭐ **IMPORTANT**
   - Code quality checks before committing
   - Testing requirements
   - Build verification steps
   - Documentation update checklist
   - Git pre-commit checklist
   - Pull request checklist
   - CI/CD pipeline details
   - Common issues and solutions
   - Release-specific tasks

### Architecture and Design

4. **architecture_and_design_patterns.md** (571 lines)
   - Overall system architecture
   - Module-specific architectures:
     - hugegraph-client: Layered architecture, Manager pattern
     - hugegraph-loader: Pipeline architecture
     - hugegraph-hubble: Frontend (React+MobX) + Backend (Spring Boot)
     - hugegraph-tools: Command pattern
   - Design patterns (Factory, Builder, Strategy, Observer, Repository)
   - Cross-cutting concerns (error handling, logging)
   - Configuration management

### Testing

5. **testing_infrastructure.md** (634 lines)
   - Testing philosophy (unit, integration, functional)
   - Test organization and structure
   - Module-specific testing:
     - hugegraph-client: UnitTestSuite, ApiTestSuite, FuncTestSuite
     - hugegraph-loader: Test profiles (unit, file, hdfs, jdbc, kafka)
     - hugegraph-hubble: Backend (Spring Test) + Frontend (Jest)
     - hugegraph-client-go: Go standard testing
   - CI/CD testing pipelines
   - Test coverage tools and targets
   - Common testing patterns
   - Debugging tests

### Development Workflows

6. **common_development_workflows.md** (657 lines)
   - Daily development workflows:
     - Starting new features
     - Fixing bugs
     - Adding tests
     - Refactoring code
   - Module-specific workflows
   - Troubleshooting common issues
   - Release workflow
   - Useful development commands
   - Git hooks setup
   - IDE configuration (IntelliJ IDEA, VS Code)

## Quick Start Guide

### For New Developers

1. **Read First**:
   - `project_overview.md` - Understand what the project is
   - `common_development_workflows.md` - Learn essential commands and workflows

2. **Before Making Changes**:
   - `code_style_and_conventions.md` - Learn coding standards
   - `task_completion_checklist.md` - Know what to check before committing

3. **When Working on Code**:
   - `architecture_and_design_patterns.md` - Understand design patterns

4. **When Writing Tests**:
   - `testing_infrastructure.md` - Learn testing approach

### For System Setup

**Prerequisites** (macOS):
```bash
# Java 11 (required)
/usr/libexec/java_home -V
export JAVA_HOME=$(/usr/libexec/java_home -v 11)

# Maven
brew install maven

# Node.js 18.20.8 (for Hubble)
nvm install 18.20.8
nvm use 18.20.8
npm install -g yarn

# Python 3 (for Hubble build)
brew install python3
pip3 install -r hugegraph-hubble/hubble-dist/assembly/travis/requirements.txt
```

**Build Entire Project**:
```bash
mvn clean install -DskipTests -Dmaven.javadoc.skip=true -ntp
```

**Run Tests**:
```bash
# Client tests
cd hugegraph-client
mvn test -Dtest=UnitTestSuite -ntp

# Loader tests
cd hugegraph-loader
mvn test -P unit -ntp

# Hubble tests
cd hugegraph-hubble/hubble-fe
yarn test
```

## Essential Commands Cheat Sheet

### Build Commands
```bash
# Full project
mvn clean install -DskipTests -Dmaven.javadoc.skip=true -ntp

# Specific module (e.g., client)
mvn install -pl hugegraph-client -am -DskipTests -ntp

# Hubble (requires dependencies built first)
mvn install -pl hugegraph-client,hugegraph-loader -am -DskipTests -ntp
cd hugegraph-hubble
mvn -e compile package -Dmaven.test.skip=true -ntp
```

### Testing Commands
```bash
# Client unit tests
cd hugegraph-client && mvn test -Dtest=UnitTestSuite -ntp

# Loader tests
cd hugegraph-loader && mvn test -P unit -ntp

# Single test
mvn test -Dtest=ClassName#methodName -ntp
```

### Code Quality
```bash
# Checkstyle
mvn checkstyle:check

# License check
mvn apache-rat:check

# EditorConfig validation
mvn editorconfig:check
```

### Git Commands (IMPORTANT: Always use --no-pager)
```bash
# View history
git --no-pager log --oneline -10

# View changes
git --no-pager diff HEAD~1
```

**See `common_development_workflows.md` for complete command reference**

## Key Project Facts

- **Language**: Java 8 (main), Go, TypeScript
- **Build Tool**: Maven 3.x
- **Test Framework**: JUnit 4 + Mockito
- **Frontend**: React + TypeScript + MobX (Node.js 18.20.8)
- **Backend**: Spring Boot
- **Version**: 1.7.0
- **License**: Apache 2.0
- **Repository**: https://github.com/apache/hugegraph-toolchain

## Common Pitfalls to Avoid

1. ❌ **DON'T** use `git log` without `--no-pager` flag
2. ❌ **DON'T** commit without running checkstyle and tests
3. ❌ **DON'T** use star imports (`import org.apache.*`)
4. ❌ **DON'T** use `System.out.println` (use logger instead)
5. ❌ **DON'T** forget Apache 2.0 license headers
6. ❌ **DON'T** use tabs (use 4 spaces for Java, 2 for frontend)
7. ❌ **DON'T** exceed 100 character line length
8. ❌ **DON'T** commit code that fails CI checks

## Getting Help

- **Documentation**: https://hugegraph.apache.org/docs/
- **Issues**: https://github.com/apache/hugegraph-toolchain/issues
- **Mailing List**: dev@hugegraph.apache.org
- **Memory Files**: Check `.serena/memories/` directory

## Memory Statistics

- **Total Memory Files**: 7 (including this index)
- **Total Lines**: ~2,900+
- **Total Size**: ~85KB
- **Coverage Areas**: 
  - Project overview and structure
  - Code style and conventions
  - Architecture and design patterns
  - Testing infrastructure
  - Development workflows
  - Task completion checklists

## Last Updated

Onboarding completed: 2025-11-05

---

**Note**: All memories are stored in `.serena/memories/` directory and can be read using Serena MCP tools.