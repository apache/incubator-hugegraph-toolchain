# HugeGraph Hubble 2.0 Build & Debug Guide

## Current Environment Setup Status

- ✅ Java 21 (installed, project compatible with backward compatibility)
- ✅ Maven 3.9.6 (installed at `C:\Tools\maven`)
- ✅ Node.js v22.12.0
- ✅ npm v10.9.0

## Known Issues & Solutions

### Issue 1: Java Version Compatibility
**Problem**: Project specifies Java 1.8, but Java 21 is installed
**Solution**: Java 21 maintains backward compatibility with Java 1.8
**Status**: ✅ Resolved - Build should work

### Issue 2: Maven Download Dependencies
**Problem**: Initial Maven builds download large plugin dependencies
**Expected**: First build takes 2-5 minutes
**Workaround**: Monitor build progress with Maven verbose logging

## Build Steps

### Step 1: Build Core Dependencies
```bash
cd d:\incubator-hugegraph-toolchain
C:\Tools\maven\bin\mvn.cmd -DskipTests clean install
```

**Expected Output**:
- BUILD SUCCESS
- All modules compiled
- JARs installed in ~/.m2/repository

### Step 2: Build Hubble Backend
```bash
cd hugegraph-hubble
C:\Tools\maven\bin\mvn.cmd clean package -DskipTests
```

**Expected Output**:
- `hubble-be/target/hubble-be-1.7.0.jar`

### Step 3: Build Hubble Frontend
```bash
cd hubble-fe
npm install
npm run build
```

**Expected Output**:
- `dist/` folder with production build

## Runtime Setup

### Backend Server
```bash
java -jar hugegraph-hubble/hubble-be/target/hubble-be-*.jar --server.port=36320
```

**Access**: `http://localhost:36320`

### Frontend Server (Development)
```bash
cd hugegraph-hubble/hubble-fe
npm run dev
```

**Access**: `http://localhost:8080`

## Expected Configuration Files

### Backend Configuration
- `application.properties` or `application.yml` in backend
- H2 database configuration
- Logging configuration (log4j2)

### Frontend Configuration
- `package.json` - All npm dependencies
- `tsconfig.json` - TypeScript configuration
- `config-overrides.js` - Webpack custom configuration

## Common Build Errors & Fixes

### Error: "No plugin found for prefix"
**Solution**: Ensure pom.xml has correct maven plugin declarations

### Error: Module not found (Node)
**Solution**: Run `npm install` before build
```bash
npm install --legacy-peer-deps  # If peer dependency conflicts
```

### Error: Port 36320 already in use
**Solution**: Kill existing process or change port
```bash
netstat -ano | findstr :36320
taskkill /PID <PID> /F
```

## Next Steps After Build

1. **Verify Backend Startup**
   - Check `logs/hubble-backend.log` for errors
   - Confirm H2 database initialization
   - Test API endpoint: `curl http://localhost:36320/health`

2. **Verify Frontend Build**
   - Check for TypeScript compilation errors
   - Verify all dependencies resolved
   - Test dev server connectivity

3. **End-to-End Testing**
   - Connect frontend to backend
   - Create a test graph
   - Verify data flow

## Troubleshooting

### Build hangs
- Press Ctrl+C and retry
- Increase Maven heap: `export MAVEN_OPTS="-Xmx2G"`

### Port conflicts
- Change port in application properties
- Update frontend API URL accordingly

### Database errors (H2)
- Clear H2 database files: `rm -rf ~/h2-data`
- Reinitialize on next start

### NPM package conflicts
- Clear node_modules: `rm -rf node_modules && npm install`
- Try: `npm install --legacy-peer-deps`

## Java 21 Fix Applied

**Issue**: Lombok 1.18.8 (OLD) doesn't support Java 21 module system
**Solution**: Upgraded Lombok to 1.18.30 in root pom.xml

**Changes Made**:
```xml
<!-- Before -->
<lombok.version>1.18.8</lombok.version>

<!-- After -->
<lombok.version>1.18.30</lombok.version>
```

This resolves the IllegalAccessError issues with Lombok and Java 21's jdk.compiler module.

## Build Commands

### Complete Automated Build
```bash
cd d:\incubator-hugegraph-toolchain
# Fix editor config formatting issues
mvn editorconfig:format

# Build all modules
mvn -DskipTests clean install

# Verify artifacts
cd hugegraph-hubble\hubble-be
mvn clean package -DskipTests
```

### Individual Module Builds
```bash
# Client
cd hugegraph-client
mvn clean install -DskipTests

# Loader
cd hugegraph-loader
mvn clean install -DskipTests

# Backend
cd hugegraph-hubble\hubble-be
mvn clean package -DskipTests

# Frontend
cd hugegraph-hubble\hubble-fe
npm install
npm run build
```

## Success Criteria

- [ ] Backend JAR builds without errors
- [ ] Backend starts and initializes H2 database
- [ ] Frontend builds successfully
- [ ] Frontend dev server starts on port 8080
- [ ] Frontend can reach backend API
- [ ] No runtime errors in console logs
