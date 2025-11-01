package com.pdfutilities.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main application class for PDF Utilities App
 * This is the entry point for the JavaFX application
 */
public class Main extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("main"), 1200, 800);

        // Load CSS styles
        scene.getStylesheets().add(getClass().getResource("/css/password-highlight.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("PDF Utilities - All-in-One PDF Tool");

        // Set application icon
        try {
            Image appIcon = new Image(getClass().getResourceAsStream("/images/app.png"));
            stage.getIcons().add(appIcon);
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }

        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.show();
    }

    /**
     * Load FXML file and return the root Parent node
     * 
     * @param fxml the name of the FXML file (without .fxml extension)
     * @return the loaded Parent node
     * @throws IOException if the FXML file cannot be loaded
     */
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    /**
     * Set the root of the current scene
     * 
     * @param fxml the name of the FXML file to load
     * @throws IOException if the FXML file cannot be loaded
     */
    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    /**
     * Main entry point for native-image and standard Java execution
     */
    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}
