@echo off
echo Compiling and running FastOverlay Benchmark...

java --enable-native-access=ALL-UNNAMED -cp "target\fastoverlay-0.1.0.jar" fastoverlay.FastOverlayBenchmark
pause
