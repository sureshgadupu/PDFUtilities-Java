#!/bin/bash

# Script to create a truly standalone native installer for the JavaFX application

echo "Creating Standalone Native Installer for PDF Utilities App..."
echo

# Check if Maven is installed
if ! command -v mvn &> /dev/null;
    then
    echo "ERROR: Maven is not installed or not in PATH"
    echo "Please install Maven and add it to your PATH"
    exit 1
fi

# Clean and package the application
echo "Cleaning and packaging the application..."
mvn clean package
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to build application JAR"
    exit 1
fi

# Get the JAR file name
JAR_FILE=$(ls target/pdf-utilities-app-*.jar | grep -v sources | grep -v javadoc | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo "ERROR: Could not find packaged JAR file!"
    exit 1
fi
JAR_NAME=$(basename "$JAR_FILE")
echo "Application packaged successfully: $JAR_NAME"

# Define JavaFX modules
JAVAFX_MODULES=javafx.controls,javafx.fxml

# Remove previous installers
rm -rf installers

# Create native installer using jpackage
echo "Creating native installer using jpackage..."

# Determine OS-specific options and check for required tools
INSTALLER_TYPE=""
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OSTYPE" == "win32" ]]; then
    INSTALLER_TYPE="msi"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    INSTALLER_TYPE="dmg"
else
    if command -v fakeroot &> /dev/null && command -v dpkg-deb &> /dev/null; then
        INSTALLER_TYPE="deb"
    else
        echo "WARNING: 'fakeroot' and 'dpkg-deb' are not installed. Cannot create .deb package."
        echo "Please install them using: sudo apt-get install -y fakeroot dpkg"
        echo "Falling back to creating an app-image."
        INSTALLER_TYPE="app-image"
    fi
fi

jpackage \
    --input target \
    --name "PDF Utilities" \
    --app-version 1.0.0 \
    --main-class com.pdfutilities.app.Main \
    --main-jar "$JAR_NAME" \
    --type "$INSTALLER_TYPE" \
    --dest installers \
    --module-path "target/lib" \
    --add-modules "$JAVAFX_MODULES"

if [ $? -eq 0 ]; then
    echo
    echo "Native installer created successfully!"
    echo "Installers are located in the 'installers' directory."
else
    echo
    echo "ERROR: Failed to create native installer with jpackage"
    echo "Please check the error messages above."
    exit 1
fi

echo
echo "Script finished."