#!/bin/bash

# Script to run the PDF Utilities App

# Build the application
mvn clean package

# Run the application
java --module-path target/lib --add-modules javafx.controls,javafx.fxml -jar target/pdf-utilities-app-1.0.1.jar