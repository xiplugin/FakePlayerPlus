@echo off
title FakePlayerPlus Test Server (Paper 26.2)
cd /d "%~dp0"

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo =======================================================
echo  Starting FakePlayerPlus Test Server (Paper 26.2)...
echo  Server Port: 25565
echo  Online Mode: false
echo =======================================================

call gradlew.bat :plugin:paper-26.2
pause
