@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
cd /d "%~dp0"

echo ================================================================================
echo   FastOverlay - Official JMH Performance Benchmark
echo ================================================================================
echo.

echo [1/3] Building Main Project (FastOverlay)...
call mvn clean install -DskipTests -q
if %ERRORLEVEL% NEQ 0 ( echo [ERROR] Main build failed. & pause & exit /b %ERRORLEVEL% )

powershell -NoProfile -Command "Unblock-File -Path '%USERPROFILE%\.fastcore\native\fastoverlay\*', '%~dp0build\*', '%~dp0src\main\resources\*' -ErrorAction SilentlyContinue" >nul 2>&1

echo [2/3] Building Benchmark Uber-JAR...
cd examples\Benchmark
call mvn clean package -DskipTests -q
if %ERRORLEVEL% NEQ 0 ( echo [ERROR] Benchmark build failed. & cd ..\.. & pause & exit /b %ERRORLEVEL% )

echo [3/3] Running Official JMH Benchmarks for FastOverlay...
java --enable-native-access=ALL-UNNAMED -Djmh.ignoreLock=true -jar target\benchmarks.jar -f 1 -wi 2 -i 3 -tu ms -bm thrpt

cd ..\..
echo.
pause
