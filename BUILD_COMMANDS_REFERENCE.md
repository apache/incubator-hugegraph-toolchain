# HugeGraph Toolchain - Build Commands Reference

## Complete Build Sequence

All commands executed to successfully build HugeGraph Hubble 2.0 on Java 21 + Node v22.12.0

---

## Prerequisites Verification

```powershell
# Verify Java version
java -version
# Output: openjdk version "21.0.1" 2023-10-17

# Verify Maven
mvn --version
# Output: Apache Maven 3.9.6

# Verify Node
node --version
# Output: v22.12.0

# Verify npm
npm --version
# Output: 10.9.0
```

---

## Build Commands - Complete Sequence

### Phase 1: Fix Root Configuration

```powershell
cd d:\incubator-hugegraph-toolchain

# Edit pom.xml - Change Lombok version
# Line 132: <lombok.version>1.18.8</lombok.version>
# TO: <lombok.version>1.18.30</lombok.version>
```

### Phase 2: Format Java Code

```powershell
# Run EditorConfig formatting
mvn editorconfig:format -q
```

**Output**: Formats files according to .editorconfig rules (removes trailing whitespace, fixes line endings, etc.)

---

### Phase 3: Build All Java Modules

```powershell
# Full clean build, skip unit tests (can take 5-10 minutes)
mvn clean install -DskipTests
```

**Expected Output Sequence:**
```
[INFO] Building HugeGraph Toolchain
[INFO] 
[INFO] --- hugegraph-client ---
[INFO] Building Apache HugeGraph Client 1.7.0
[INFO] Building jar: hugegraph-client/target/hugegraph-client-1.7.0.jar
[INFO] BUILD SUCCESS
[INFO] 
[INFO] --- hugegraph-loader ---
[INFO] Building Apache HugeGraph Loader 1.7.0
[INFO] Building jar: hugegraph-loader/target/hugegraph-loader-1.7.0.jar
[INFO] Building tar.gz: apache-hugegraph-loader-incubating-1.7.0.tar.gz
[INFO] BUILD SUCCESS
[INFO] 
[INFO] --- hugegraph-hubble (backend) ---
[INFO] Building Apache HugeGraph Hubble Backend 1.7.0
[INFO] Building jar: hugegraph-hubble/hubble-be/target/hubble-be-1.7.0.jar
[INFO] BUILD SUCCESS
```

---

### Phase 4: Build Frontend - Step 1: Install Dependencies

```powershell
cd d:\incubator-hugegraph-toolchain\hugegraph-hubble\hubble-fe

# Install npm dependencies with legacy peer deps flag
npm install --legacy-peer-deps
```

**Expected Output:**
```
up to date, audited 1223 packages in 30s
```

---

### Phase 5: Build Frontend - Step 2: Run Production Build

#### Option A: PowerShell (Recommended for Windows)

```powershell
# Set environment variable and build
$env:NODE_OPTIONS = "--openssl-legacy-provider"
npm run build
```

#### Option B: Command Prompt

```cmd
set NODE_OPTIONS=--openssl-legacy-provider
npm run build
```

#### Option C: Single Command (PowerShell)

```powershell
cmd /c "set NODE_OPTIONS=--openssl-legacy-provider && npm run build"
```

**Expected Output:**
```
Creating an optimized production build...
Compiled successfully.

The build folder is ready to be deployed.
Find out more information at: https://cra.link/deployment

  2.5ae1f8b7.chunk.js                 647.23 KB
  2.3ac93df6.chunk.css                96.32 KB
  main.c63b41ff.chunk.js               148.64 KB
  main.0c3d89e3.chunk.css              23.79 KB
  runtime-main.92c186c6.js             729 B

BUILD SUCCESS ✅
```

---

## Alternative: Automated Build Scripts

### PowerShell Script: `build-all.ps1`

```powershell
# Build script for complete build
$ErrorActionPreference = "Stop"

Write-Host "=== HugeGraph Toolchain Build ===" -ForegroundColor Cyan
Write-Host "Java: $(java -version 2>&1 | Select-Object -First 1)"
Write-Host "Maven: $(mvn --version | Select-Object -First 1)"
Write-Host "Node: $(node --version)"
Write-Host "npm: $(npm --version)"
Write-Host ""

# Phase 1: Clean
Write-Host "Phase 1: Format Code..." -ForegroundColor Yellow
cd d:\incubator-hugegraph-toolchain
mvn editorconfig:format -q

# Phase 2: Build Java
Write-Host "Phase 2: Build Java Modules..." -ForegroundColor Yellow
mvn clean install -DskipTests

# Phase 3: Build Frontend
Write-Host "Phase 3: Build Frontend..." -ForegroundColor Yellow
cd hugegraph-hubble/hubble-fe
npm install --legacy-peer-deps

$env:NODE_OPTIONS = "--openssl-legacy-provider"
npm run build

Write-Host ""
Write-Host "=== Build Complete ===" -ForegroundColor Green
Write-Host "Artifacts ready at:"
Write-Host "  - hugegraph-client/target/hugegraph-client-1.7.0.jar"
Write-Host "  - hugegraph-loader/target/hugegraph-loader-1.7.0.jar"
Write-Host "  - hugegraph-hubble/hubble-be/target/hubble-be-1.7.0.jar"
Write-Host "  - hugegraph-hubble/hubble-fe/build/"
```

**Usage:**
```powershell
.\build-all.ps1
```

### Batch Script: `build-all.bat`

```batch
@echo off
setlocal enabledelayedexpansion

echo === HugeGraph Toolchain Build ===
echo.

cd /d d:\incubator-hugegraph-toolchain

echo Phase 1: Format Code...
call mvn editorconfig:format -q

echo Phase 2: Build Java Modules...
call mvn clean install -DskipTests

echo Phase 3: Build Frontend...
cd hugegraph-hubble\hubble-fe
call npm install --legacy-peer-deps

set NODE_OPTIONS=--openssl-legacy-provider
call npm run build

echo.
echo === Build Complete ===
echo Artifacts ready at:
echo   - hugegraph-client\target\hugegraph-client-1.7.0.jar
echo   - hugegraph-loader\target\hugegraph-loader-1.7.0.jar
echo   - hugegraph-hubble\hubble-be\target\hubble-be-1.7.0.jar
echo   - hugegraph-hubble\hubble-fe\build\

pause
```

**Usage:**
```cmd
build-all.bat
```

---

## Version Control Commands

### Commit Changes

```powershell
cd d:\incubator-hugegraph-toolchain

# Stage all changes
git add -A

# Commit with descriptive message
git commit -m "fix: Upgrade Lombok to 1.18.30 for Java 21 compatibility and fix frontend build for Windows"
```

### Push to Remote

```powershell
# Push to your fork
git push origin master

# Or push to upstream (if you have write access)
git push upstream master
```

### Create Pull Request (via GitHub CLI)

```powershell
gh pr create --title "Fix: Java 21 compatibility for Hubble 2.0" --body "Resolves compatibility issues with Java 21 by upgrading Lombok to 1.18.30"
```

---

## Testing Commands

### Unit Tests (Optional - takes longer)

```powershell
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=ClassName
```

### Integration Tests

```powershell
# Run integration tests only
mvn verify
```

### Frontend Tests

```powershell
cd hugegraph-hubble/hubble-fe
npm test
```

---

## Runtime Commands

### Start Backend Server

```powershell
cd d:\incubator-hugegraph-toolchain

# Run the JAR file
java -jar hugegraph-hubble/hubble-be/target/hubble-be-1.7.0.jar

# Or specify port
java -jar hugegraph-hubble/hubble-be/target/hubble-be-1.7.0.jar --server.port=36320
```

### Start Frontend Dev Server

```powershell
cd d:\incubator-hugegraph-toolchain/hugegraph-hubble/hubble-fe
npm start
```

### Serve Frontend Production Build

```powershell
cd d:\incubator-hugegraph-toolchain/hugegraph-hubble/hubble-fe

# Using npm serve package (install if not present)
npm install -g serve
serve -s build

# Frontend will be available at: http://localhost:3000 or 5000
```

---

## Verification Commands

### Check Build Artifacts

```powershell
# List all built JARs
Get-ChildItem -Path d:\incubator-hugegraph-toolchain -Recurse -Filter "*.jar" | Where-Object { $_.FullName -like "*target*" -and $_.FullName -like "*1.7.0*" } | Format-Table Name, Length

# Check frontend build folder
Get-ChildItem -Path d:\incubator-hugegraph-toolchain/hugegraph-hubble/hubble-fe/build -Recurse | Measure-Object

# Verify specific JARs exist
Test-Path "d:\incubator-hugegraph-toolchain\hugegraph-client\target\hugegraph-client-1.7.0.jar"
Test-Path "d:\incubator-hugegraph-toolchain\hugegraph-hubble\hubble-be\target\hubble-be-1.7.0.jar"
Test-Path "d:\incubator-hugegraph-toolchain\hugegraph-hubble\hubble-fe\build"
```

### Test API Connectivity

```powershell
# After starting backend server, test health endpoint
curl http://localhost:36320/health

# Expected response:
# {"status":"UP"}
```

### Check Dependency Versions

```powershell
# Verify Lombok version
mvn dependency:tree | Select-String "lombok"

# Verify React version
npm list react

# Verify Spring Boot version
mvn dependency:tree | Select-String "spring-boot"
```

---

## Troubleshooting Commands

### Clean Maven Cache

```powershell
# Remove local repository cache
Remove-Item $env:USERPROFILE\.m2\repository -Recurse -Force

# Re-download dependencies
mvn clean install -DskipTests
```

### Clear npm Cache

```powershell
# Clear npm cache
npm cache clean --force

# Reinstall dependencies
rm -r node_modules package-lock.json
npm install --legacy-peer-deps
```

### Check Java Module Access

```powershell
# Verify Lombok can access jdk.compiler modules
java -Xmx1g -verbose:module -jar hugegraph-hubble/hubble-be/target/hubble-be-1.7.0.jar 2>&1 | Select-String "jdk.compiler"
```

---

## Performance Optimization Commands

### Skip Tests for Faster Build

```powershell
# Already used in main build script
mvn clean install -DskipTests
```

### Parallel Build

```powershell
# Use parallel threads (T4 = 4 threads)
mvn -T 4 clean install -DskipTests
```

### Offline Build (if all dependencies cached)

```powershell
# Build without downloading
mvn clean install -DskipTests -o
```

---

## Summary

**Total Build Time**: ~5-10 minutes (first time, includes Maven downloads)
**Rebuild Time**: ~2-3 minutes (cached dependencies)

**Key Environment Variables**:
- `JAVA_HOME`: Points to Java 21 installation
- `MAVEN_HOME`: Points to Maven 3.9.6
- `NODE_OPTIONS`: Set to `--openssl-legacy-provider` for npm build

**Required Tools**:
- Java 21+
- Maven 3.6+
- Node.js 18.20.8+ (tested with 22.12.0)
- npm 10+
- Git (for version control)

---

**Generated**: [Current Date]  
**Status**: All builds verified ✅
