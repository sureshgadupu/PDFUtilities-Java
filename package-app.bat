@echo off
REM Script to create a standalone native installer for the JavaFX application on Windows

echo Starting packaging process...

REM Check for Maven
echo Checking for Maven...
where mvn >nul 2>nul
if errorlevel 1 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven and add it to your PATH
    exit /b 1
)
echo Maven found.

REM Check for jpackage
echo Checking for jpackage...
where jpackage >nul 2>nul
if errorlevel 1 (
    echo ERROR: jpackage is not installed or not in PATH
    echo Please install JDK 14+ which includes jpackage utility
    exit /b 1
)
echo jpackage found.

REM Clean, package, and create installer with Maven
echo Cleaning, packaging, and creating installer with Maven...
mvn clean install

if errorlevel 1 (
    echo ERROR: Failed to create native installer
    echo Check Maven build output above for details
    exit /b 1
) else (
    echo Native installer created successfully!
    echo Installers are located in the 'target/installers' directory.
    
    REM Show what was created
    if exist target\installers (
        echo Contents of target\installers directory:
        dir target\installers
    ) else (
        echo WARNING: Installers directory not found
    )
)

echo Script finished.