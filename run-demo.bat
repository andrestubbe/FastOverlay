@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Starting FastOverlay Demo...
java --enable-native-access=ALL-UNNAMED -cp "target\fastoverlay-0.1.0.jar" fastoverlay.Demo
pause
