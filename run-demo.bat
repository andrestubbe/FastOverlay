@echo off
setlocal
cd /d "%~dp0"

echo [1/3] Building FastOverlay...
call mvn clean install -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] FastOverlay build failed.
    pause
    exit /b %ERRORLEVEL%
)

powershell -NoProfile -Command "Unblock-File -Path '%USERPROFILE%\.fastcore\native\fastoverlay\*', '%~dp0build\*', '%~dp0src\main\resources\*' -ErrorAction SilentlyContinue" >nul 2>&1

echo [2/3] Compiling Demo...
cd examples\Demo
call mvn compile dependency:build-classpath "-Dmdep.outputFile=cp.txt" "-DincludeScope=runtime" -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Demo compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

echo [3/3] Running Demo...
set /p CP=<cp.txt
java --enable-native-access=ALL-UNNAMED "-Djava.library.path=%~dp0build" -cp "target\classes;%CP%" fastoverlay.Demo

cd ..\..
pause
