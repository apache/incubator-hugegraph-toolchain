@echo off
setlocal enabledelayedexpansion
cd /d d:\incubator-hugegraph-toolchain

echo.
echo ========================================
echo Building HugeGraph Toolchain Dependencies
echo ========================================
echo.

echo [Step 1/3] Building all dependencies...
C:\Tools\maven\bin\mvn.cmd -q -DskipTests install
if %errorlevel% neq 0 (
    echo Build failed!
    exit /b 1
)
echo [OK] Dependencies installed

echo.
echo [Step 2/3] Building Hubble backend...
cd hugegraph-hubble
C:\Tools\maven\bin\mvn.cmd clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo Hubble backend build failed!
    exit /b 1
)
echo [OK] Hubble backend built

echo.
echo [Step 3/3] Building Hubble frontend...
cd hubble-fe
call npm install
if %errorlevel% neq 0 (
    echo npm install failed!
    exit /b 1
)

call npm run build
if %errorlevel% neq 0 (
    echo Frontend build failed!
    exit /b 1
)
echo [OK] Frontend built

echo.
echo ========================================
echo Build Summary
echo ========================================
echo All builds completed successfully!
echo.
echo Next steps:
echo 1. Start backend: java -jar hugegraph-hubble\hubble-be\target\*.jar
echo 2. Start frontend: cd hugegraph-hubble\hubble-fe ^&^& npm run dev
echo 3. Access frontend: http://localhost:8080
echo 4. Backend API: http://localhost:36320
echo.

endlocal
