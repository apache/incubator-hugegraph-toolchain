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
