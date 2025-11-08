@echo off
set PATH_TO_FX=E:\javafx-sdk-21.0.8\lib
java --module-path %PATH_TO_FX% --add-modules javafx.controls,javafx.fxml -jar ./target/pdf-utilities-app-1.0.1.jar
