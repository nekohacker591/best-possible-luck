@echo off
setlocal enabledelayedexpansion

set "ROOT=%~dp0"
set "BOOTSTRAP_DIR=%ROOT%.gradle-bootstrap"
set "DIST_NAME=gradle-4.10.3-bin.zip"
set "DIST_URL=https://services.gradle.org/distributions/%DIST_NAME%"
set "DIST_ZIP=%BOOTSTRAP_DIR%\%DIST_NAME%"
set "GRADLE_HOME=%BOOTSTRAP_DIR%\gradle-4.10.3"

if exist "%ROOT%gradlew.bat" if exist "%ROOT%gradle\wrapper\gradle-wrapper.jar" (
    echo Gradle wrapper already exists.
    exit /b 0
)

if not exist "%BOOTSTRAP_DIR%" mkdir "%BOOTSTRAP_DIR%"

if not exist "%DIST_ZIP%" (
    echo Downloading Gradle 4.10.3...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri '%DIST_URL%' -OutFile '%DIST_ZIP%'"
    if errorlevel 1 exit /b 1
)

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
    echo Extracting Gradle 4.10.3...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%DIST_ZIP%' -DestinationPath '%BOOTSTRAP_DIR%' -Force"
    if errorlevel 1 exit /b 1
)

echo Generating Forge-compatible Gradle wrapper...
call "%GRADLE_HOME%\bin\gradle.bat" wrapper --gradle-version 4.10.3 --distribution-type all --no-validate-url
if errorlevel 1 exit /b 1

echo Wrapper generated. You can now run gradlew.bat build
