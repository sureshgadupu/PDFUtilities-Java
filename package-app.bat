@echo off
REM Script to create a standalone native installer for the JavaFX application on Windows

setlocal enabledelayedexpansion

REM Check for Maven
where mvn >nul 2>nul
if errorlevel 1 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven and add it to your PATH
    exit /b 1
)

REM Clean and package the application
mvn clean package
if errorlevel 1 (
    echo ERROR: Failed to build application JAR
    exit /b 1
)

REM Get the JAR file name
for /f "delims=" %%F in ('dir /b /a-d target\pdf-utilities-app-*.jar ^| findstr /v sources ^| findstr /v javadoc') do set JAR_FILE=%%F
if not defined JAR_FILE (
    echo ERROR: Could not find packaged JAR file!
    exit /b 1
)
set JAR_NAME=%JAR_FILE%
echo Application packaged successfully: %JAR_NAME%

REM Define JavaFX modules
set JAVAFX_MODULES=javafx.controls,javafx.fxml

REM Remove previous installers
if exist installers rmdir /s /q installers

REM Create native installer using jpackage
set INSTALLER_TYPE=msi

jpackage ^
    --input target ^
    --name "PDF Utilities" ^
    --app-version 1.0.0 ^
    --main-class com.pdfutilities.app.Main ^
    --main-jar "%JAR_NAME%" ^
    --type "%INSTALLER_TYPE%" ^
    --dest installers ^
    --module-path "target\lib" ^
    --add-modules "%JAVAFX_MODULES%"

if errorlevel 1 (
    echo ERROR: Failed to create native installer with jpackage
    exit /b 1
) else (
    echo Native installer created successfully!
    echo Installers are located in the 'installers' directory.
)

echo Script finished.
endlocal

