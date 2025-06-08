@echo off
setlocal

REM Set variables
set "JDK_ZIP=%~dp0jdk-21.0.6_windows-x64_bin.zip"
set "TARGET_DIR=%~dp0target"
set "JDK_DIR=%TARGET_DIR%\jdk-21.0.6"
set "JAR=%TARGET_DIR%\java_chat_app-1.0-SNAPSHOT-jar-with-dependencies.jar"

REM Unzip JDK to target if not already extracted
if not exist "%JDK_DIR%\bin\java.exe" (
    echo Extracting JDK to target folder...
    powershell -Command "Expand-Archive -Path '%JDK_ZIP%' -DestinationPath '%TARGET_DIR%' -Force"
)

REM Set JAVA_HOME and update PATH
set "JAVA_HOME=%JDK_DIR%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM Show Java version for confirmation
java -version

REM Run the application (change 'server' to 'client' or 'gui' as needed)
java -jar "%JAR%" server

pause