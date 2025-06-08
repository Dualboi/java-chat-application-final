@echo off
setlocal

REM Set variables
set "APP_DIR=%~dp0"
set "JDK_ZIP=%APP_DIR%jdk-21.0.6_windows-x64_bin.zip"
set "JDK_DIR=%APP_DIR%jdk-21.0.6"
set "JAR=%APP_DIR%target\java_chat_app-1.0-SNAPSHOT-jar-with-dependencies.jar"

REM Unzip JDK to application if not already extracted
if not exist "%JDK_DIR%\bin\java.exe" (
    if exist "%JDK_ZIP%" (
        echo Extracting JDK to application folder...
        powershell -Command "Expand-Archive -Path '%JDK_ZIP%' -DestinationPath '%APP_DIR%' -Force"
    ) else (
        echo JDK zip not found: %JDK_ZIP%
    )
)

REM Set JAVA_HOME and update PATH
set "JAVA_HOME=%JDK_DIR%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM Show Java version for confirmation
java -version

REM Debug: Show the resolved JAR path
echo JAR path: %JAR%

REM Check if JAR exists
if not exist "%JAR%" (
    echo ERROR: JAR file not found at %JAR%
    pause
    exit /b
)

REM Run the application (change 'server' to 'client' or 'gui' as needed)
start "" cmd /k ""%JAVA_HOME%\bin\java.exe" -jar "%JAR%" server"

pause