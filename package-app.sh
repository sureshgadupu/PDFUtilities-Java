#!/bin/bash

# Script to create a standalone native installer for the JavaFX application on Linux/Mac

echo "Starting packaging process..."

# Check for Maven
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed or not in PATH"
    echo "Please install Maven and add it to your PATH"
    exit 1
fi
echo "Maven found."

# Check for jpackage
if ! command -v jpackage &> /dev/null; then
    echo "ERROR: jpackage is not installed or not in PATH"
    echo "Please install JDK 14+ which includes jpackage utility"
    exit 1
fi
echo "jpackage found."

# Create a clean staging directory
echo "Creating clean staging directory..."
mkdir -p staging
cp target/pdf-utilities-app-1.0.0.0.jar staging/

# Run jpackage to create installer
echo "Creating native installer with jpackage..."
jpackage \
  --input staging \
  --name "PDF Utilities" \
  --main-jar pdf-utilities-app-1.0.0.0.jar \
  --type pkg \
  --dest target/installers

if [ $? -eq 0 ]; then
    echo "Native installer created successfully!"
    echo "Installers are located in the 'target/installers' directory."
    
    # Show what was created
    if [ -d "target/installers" ]; then
        echo "Contents of target/installers directory:"
        ls -la target/installers
    fi
else
    echo "ERROR: Failed to create native installer"
    echo "Check Maven build output above for details"
    exit 1
fi

echo "Script finished."