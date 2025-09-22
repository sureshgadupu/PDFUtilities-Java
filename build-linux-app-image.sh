#!/bin/bash

# Script to build a self-contained Linux app image for PDF Utilities
# This script uses Maven with the jpackage plugin to create a portable app image

set -e  # Exit on any error

echo "=========================================="
echo "PDF Utilities - Linux App Image Builder"
echo "=========================================="

# Check for required tools
echo "Checking prerequisites..."

# Check for Maven
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed or not in PATH"
    echo "Please install Maven and add it to your PATH"
    exit 1
fi
echo "✓ Maven found: $(mvn -version | head -1)"

# Check for jpackage
if ! command -v jpackage &> /dev/null; then
    echo "ERROR: jpackage is not installed or not in PATH"
    echo "Please install JDK 14+ which includes jpackage utility"
    exit 1
fi
echo "✓ jpackage found: $(jpackage --version 2>&1 | head -1)"

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 14 ]; then
    echo "ERROR: Java 14 or higher is required for jpackage"
    echo "Current Java version: $(java -version 2>&1 | head -1)"
    exit 1
fi
echo "✓ Java version: $(java -version 2>&1 | head -1)"

echo ""
echo "Starting build process..."

# Clean previous builds
echo "Cleaning previous builds..."
mvn clean

# Package the application
echo "Packaging application..."
mvn package

# Create the app image
echo "Creating Linux app image..."
mvn exec:exec@jpackage-app-image -Dskip.app.image=false

# Check if the app image was created successfully
if [ -d "target/installers/app-image/" ]; then
    echo ""
    echo "✓ App image created successfully!"
    echo ""
    echo "App image location: target/installers/app-image/"
    echo ""
    echo "Contents:"
    ls -la target/installers/app-image/
    echo ""
    
    # Find the main executable
    APP_DIR=$(find target/installers/app-image -name "PDF Utilities" -type d | head -1)
    if [ -n "$APP_DIR" ]; then
        echo "App directory: $APP_DIR"
        echo "Executable: $APP_DIR/bin/PDF Utilities"
        echo ""
        echo "To run the application:"
        echo "  cd $APP_DIR"
        echo "  ./bin/PDF Utilities"
        echo ""
        echo "Or run directly:"
        echo "  $APP_DIR/bin/PDF Utilities"
    else
        echo "WARNING: Could not find the main app directory"
    fi
    
    # Show size information
    echo "App image size:"
    du -sh target/installers/app-image/
    echo ""
    
    # Create a simple launcher script
    LAUNCHER_SCRIPT="target/installers/app-image/run-pdf-utilities.sh"
    cat > "$LAUNCHER_SCRIPT" << 'EOF'
#!/bin/bash
# PDF Utilities Launcher Script
# This script launches the PDF Utilities application

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$SCRIPT_DIR"

if [ -f "$APP_DIR/bin/PDF Utilities" ]; then
    echo "Starting PDF Utilities..."
    cd "$APP_DIR"
    exec "./bin/PDF Utilities" "$@"
else
    echo "ERROR: PDF Utilities executable not found!"
    echo "Expected location: $APP_DIR/bin/PDF Utilities"
    exit 1
fi
EOF
    chmod +x "$LAUNCHER_SCRIPT"
    echo "✓ Created launcher script: $LAUNCHER_SCRIPT"
    
else
    echo "ERROR: App image creation failed!"
    echo "Check the Maven output above for details"
    exit 1
fi

echo ""
echo "=========================================="
echo "Build completed successfully!"
echo "=========================================="


