@echo off
echo Compiling and running FastOverlay Benchmark...
call mvn -q clean install -DskipTests
if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%

java --enable-native-access=ALL-UNNAMED -cp "target\fastoverlay-0.1.0.jar" fastoverlay.FastOverlayBenchmark
pause
