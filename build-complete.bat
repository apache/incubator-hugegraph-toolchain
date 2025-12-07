@echo off
REM Complete HugeGraph Build with all Java 21 Lombok fixes
REM All required --add-opens for Lombok to work with Java 21

setlocal enabledelayedexpansion

REM All required Lombok/Java 21 exports
set "MAVEN_OPTS=--add-opens jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED"
set "MAVEN_CMD=C:\Tools\maven\bin\mvn.cmd"

echo.
echo ============================================================
echo   Building HugeGraph with Java 21 Lombok Support
echo ============================================================
echo MAVEN_OPTS: %MAVEN_OPTS%
echo.

cd /d d:\incubator-hugegraph-toolchain

echo [Step 1/4] Building hugegraph-client...
cd hugegraph-client
call %MAVEN_CMD% clean install -DskipTests -q
if !errorlevel! neq 0 (
    echo ERROR: hugegraph-client failed
    exit /b 1
)
echo ✓ hugegraph-client built
cd ..

echo [Step 2/4] Building hugegraph-loader...
cd hugegraph-loader
call %MAVEN_CMD% clean install -DskipTests -q
if !errorlevel! neq 0 (
    echo ERROR: hugegraph-loader failed
    exit /b 1
)
echo ✓ hugegraph-loader built
cd ..

echo [Step 3/4] Building Hubble backend...
cd hugegraph-hubble\hubble-be
call %MAVEN_CMD% clean package -DskipTests -q
if !errorlevel! neq 0 (
    echo ERROR: Hubble backend failed
    exit /b 1
)
echo ✓ Hubble backend built
cd ..\..

echo [Step 4/4] Building Hubble frontend...
cd hugegraph-hubble\hubble-fe
call npm install
if !errorlevel! neq 0 (
    echo ERROR: npm install failed
    exit /b 1
)
call npm run build
if !errorlevel! neq 0 (
    echo ERROR: npm build failed
    exit /b 1
)
echo ✓ Hubble frontend built
cd ..\..

echo.
echo ============================================================
echo   Build Complete!
echo ============================================================
echo.
echo Backend JAR:  hugegraph-hubble\hubble-be\target\hubble-be-*.jar
echo Frontend:    hugegraph-hubble\hubble-fe\dist\
echo.

endlocal
