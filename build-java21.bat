::
:: Licensed to the Apache Software Foundation (ASF) under one or more
:: contributor license agreements. See the NOTICE file distributed with this
:: work for additional information regarding copyright ownership. The ASF
:: licenses this file to You under the Apache License, Version 2.0 (the
:: "License"); you may not use this file except in compliance with the License.
:: You may obtain a copy of the License at
::
::     http://www.apache.org/licenses/LICENSE-2.0
::
:: Unless required by applicable law or agreed to in writing, software
:: distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
:: WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
:: License for the specific language governing permissions and limitations
:: under the License.
::

@echo off
REM HugeGraph Hubble Build Script with Java 21 Lombok Fix

setlocal enabledelayedexpansion

REM Set Maven options for Lombok/Java 21 compatibility
REM These exports are needed because Lombok uses internal javac APIs
set "MAVEN_OPTS=--add-opens jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED"
set "MAVEN_CMD=C:\Tools\maven\bin\mvn.cmd"

echo.
echo ========================================
echo Building HugeGraph Toolchain with Java 21
echo ========================================
echo.
echo Maven Options: %MAVEN_OPTS%
echo.

REM Change to correct directory
cd /d d:\incubator-hugegraph-toolchain

REM Build hugegraph-client
echo [1/4] Building hugegraph-client...
cd hugegraph-client
call %MAVEN_CMD% clean install -DskipTests -q
if %errorlevel% neq 0 (
    echo ERROR: hugegraph-client build failed!
    exit /b 1
)
echo [OK] hugegraph-client built
cd ..

REM Build hugegraph-loader  
echo [2/4] Building hugegraph-loader...
cd hugegraph-loader
call %MAVEN_CMD% clean install -DskipTests -q
if %errorlevel% neq 0 (
    echo ERROR: hugegraph-loader build failed!
    exit /b 1
)
echo [OK] hugegraph-loader built
cd ..

REM Build Hubble backend
echo [3/4] Building Hubble backend...
cd hugegraph-hubble
call %MAVEN_CMD% clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo ERROR: Hubble backend build failed!
    exit /b 1
)
echo [OK] Hubble backend built
cd ..

REM Build Hubble frontend
echo [4/4] Building Hubble frontend...
cd hugegraph-hubble\hubble-fe
call npm install
if %errorlevel% neq 0 (
    echo ERROR: npm install failed!
    exit /b 1
)

call npm run build
if %errorlevel% neq 0 (
    echo ERROR: Frontend build failed!
    exit /b 1
)
echo [OK] Hubble frontend built
cd ..\..

echo.
echo ========================================
echo Build Completed Successfully!
echo ========================================
echo.
echo Backend JAR: hugegraph-hubble\hubble-be\target\*.jar
echo Frontend: hugegraph-hubble\hubble-fe\dist
echo.
echo Next Steps:
echo 1. Start Backend:
echo    java -jar hugegraph-hubble\hubble-be\target\hubble-be-*.jar
echo.
echo 2. Start Frontend (new terminal):
echo    cd hugegraph-hubble\hubble-fe && npm run dev
echo.

endlocal
