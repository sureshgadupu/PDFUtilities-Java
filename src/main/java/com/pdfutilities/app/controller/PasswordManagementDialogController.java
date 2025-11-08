package com.pdfutilities.app.controller;

import com.pdfutilities.app.model.SavedPassword;
import com.pdfutilities.app.service.PasswordManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.file.Path;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Controller for the password management dialog
 * Allows users to add, edit, and delete saved passwords
 */
public class PasswordManagementDialogController {

    @FXML
    private TableView<SavedPassword> passwordTable;

    @FXML
    private TableColumn<SavedPassword, String> nameColumn;

    @FXML
    private TableColumn<SavedPassword, String> passwordColumn;

    @FXML
    private TextField nameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordTextField;

    @FXML
    private ToggleButton passwordPeekToggle;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button clearButton;

    @FXML
    private ToggleButton showPasswordsToggle;

    @FXML
    private Button importButton;

    @FXML
    private Button exportButton;

    private PasswordManager passwordManager;
    private Stage dialogStage;
    private boolean okClicked = false;

    private final BooleanProperty showPasswords = new SimpleBooleanProperty(false);

    /**
     * Initialize the controller
     */
    @FXML
    private void initialize() {
        passwordManager = new PasswordManager();

        // Configure table columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        passwordColumn.setCellValueFactory(new PropertyValueFactory<>("password"));

        // Set columns to share space equally
        nameColumn.prefWidthProperty().bind(passwordTable.widthProperty().divide(2));
        passwordColumn.prefWidthProperty().bind(passwordTable.widthProperty().divide(2));

        // Set table items
        passwordTable.setItems(passwordManager.getSavedPasswords());

        // Configure password column to show masked or plain based on toggle
        passwordColumn.setCellFactory(_ -> new TableCell<SavedPassword, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(showPasswords.get() ? item : maskPassword(item));
                }
            }

            private String maskPassword(String password) {
                if (password == null || password.isEmpty()) {
                    return "";
                }
                return "*".repeat(password.length());
            }
        });

        // Wire toggle to control visibility of passwords in the table only
        if (showPasswordsToggle != null) {
            showPasswordsToggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                showPasswords.set(isSelected);
                showPasswordsToggle.setText(isSelected ? "Hide Passwords" : "Show Passwords");
                passwordTable.refresh();
            });
            // Initialize toggle text
            showPasswordsToggle.setText("Show Passwords");
        }

        // Bind password text between masked and plain input fields
        if (passwordTextField != null && passwordField != null) {
            passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
        }

        // Wire dedicated peek toggle for input field (icon-only)
        if (passwordPeekToggle != null && passwordTextField != null && passwordField != null) {
            passwordPeekToggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                passwordTextField.setVisible(isSelected);
                passwordTextField.setManaged(isSelected);
                passwordField.setVisible(!isSelected);
                passwordField.setManaged(!isSelected);
                updatePasswordPeekIcon(isSelected);
            });
            // Initialize input visibility and icon based on peek toggle state
            boolean isSelected = passwordPeekToggle.isSelected();
            passwordTextField.setVisible(isSelected);
            passwordTextField.setManaged(isSelected);
            passwordField.setVisible(!isSelected);
            passwordField.setManaged(!isSelected);
            updatePasswordPeekIcon(isSelected);
        }

        // Set tooltip for the peek toggle for clarity
        if (passwordPeekToggle != null) {
            passwordPeekToggle.setTooltip(new Tooltip("Show/Hide password"));
        }

        // Enable/disable buttons based on selection
        passwordTable.getSelectionModel().selectedItemProperty().addListener((_, _, newSelection) -> {
            boolean hasSelection = newSelection != null;
            editButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection);

            if (hasSelection) {
                nameField.setText(newSelection.getName());
                passwordField.setText(newSelection.getPassword());
            } else {
                clearFields();
            }
        });

        // Initially disable edit and delete buttons
        editButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    /**
     * Set the dialog stage
     * 
     * @param dialogStage the dialog stage
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Check if OK was clicked
     * 
     * @return true if OK was clicked
     */
    public boolean isOkClicked() {
        return okClicked;
    }

    /**
     * Handle add button click
     */
    @FXML
    private void handleAdd() {
        String name = nameField.getText().trim();
        String password = passwordField.getText();

        if (name.isEmpty() || password.isEmpty()) {
            showAlert("Invalid Input", "Please enter both name and password.");
            return;
        }

        if (passwordManager.addPassword(name, password)) {
            clearFields();
            passwordTable.refresh();
        } else {
            showAlert("Error", "A password with this name already exists.");
        }
    }

    /**
     * Handle edit button click
     */
    @FXML
    private void handleEdit() {
        SavedPassword selectedPassword = passwordTable.getSelectionModel().getSelectedItem();
        if (selectedPassword == null) {
            return;
        }

        String newName = nameField.getText().trim();
        String newPassword = passwordField.getText();

        if (newName.isEmpty() || newPassword.isEmpty()) {
            showAlert("Invalid Input", "Please enter both name and password.");
            return;
        }

        if (passwordManager.updatePassword(selectedPassword.getName(), newName, newPassword)) {
            clearFields();
            passwordTable.refresh();
        } else {
            showAlert("Error", "Could not update password. Name may already exist.");
        }
    }

    /**
     * Handle delete button click
     */
    @FXML
    private void handleDelete() {
        SavedPassword selectedPassword = passwordTable.getSelectionModel().getSelectedItem();
        if (selectedPassword == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Password");
        alert.setContentText("Are you sure you want to delete the password '" + selectedPassword.getName() + "'?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            passwordManager.deletePassword(selectedPassword.getName());
            clearFields();
            passwordTable.refresh();
        }
    }

    /**
     * Handle clear button click
     */
    @FXML
    private void handleClear() {
        clearFields();
        passwordTable.getSelectionModel().clearSelection();
    }

    /**
     * Handle OK button click
     */
    @FXML
    private void handleOk() {
        okClicked = true;
        dialogStage.close();
    }

    /**
     * Handle Cancel button click
     */
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    /**
     * Handle Export button click
     */
    @FXML
    private void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Passwords");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        fileChooser.setInitialFileName("pdf_utilities_passwords.json");

        java.io.File file = fileChooser.showSaveDialog(dialogStage);
        if (file != null) {
            try {
                Path filePath = file.toPath();
                if (passwordManager.exportPasswords(filePath)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Export Successful");
                    alert.setHeaderText(null);
                    alert.setContentText("Passwords exported successfully to:\n" + file.getAbsolutePath());
                    alert.showAndWait();
                } else {
                    showAlert("Export Failed", "Failed to export passwords. Please check file permissions.");
                }
            } catch (Exception e) {
                showAlert("Export Error", "Error exporting passwords: " + e.getMessage());
            }
        }
    }

    /**
     * Handle Import button click
     */
    @FXML
    private void handleImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Passwords");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        java.io.File file = fileChooser.showOpenDialog(dialogStage);
        if (file != null) {
            try {
                // Ask user for import mode
                Alert modeAlert = new Alert(Alert.AlertType.CONFIRMATION);
                modeAlert.setTitle("Import Mode");
                modeAlert.setHeaderText("Choose Import Mode");
                modeAlert.setContentText("How would you like to import passwords?\n\n" +
                        "Merge: Add imported passwords, skip duplicates\n" +
                        "Replace: Replace all existing passwords with imported ones");

                ButtonType mergeButton = new ButtonType("Merge");
                ButtonType replaceButton = new ButtonType("Replace");
                ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

                modeAlert.getButtonTypes().setAll(mergeButton, replaceButton, cancelButton);

                java.util.Optional<ButtonType> result = modeAlert.showAndWait();
                if (result.isEmpty() || result.get() == cancelButton) {
                    return;
                }

                boolean mergeMode = result.get() == mergeButton;

                // If replace mode, confirm with user
                if (!mergeMode) {
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Confirm Replace");
                    confirmAlert.setHeaderText("Replace All Passwords?");
                    confirmAlert.setContentText(
                            "This will delete all existing passwords and replace them with imported ones.\n\n" +
                                    "Are you sure you want to continue?");

                    if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.CANCEL) {
                        return;
                    }
                }

                Path filePath = file.toPath();
                PasswordManager.ImportResult importResult = passwordManager.importPasswords(filePath, mergeMode);

                if (importResult.success) {
                    passwordTable.refresh();
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Import Successful");
                    successAlert.setHeaderText(null);
                    String message = String.format(
                            "Import completed successfully!\n\n" +
                                    "Imported: %d password(s)\n" +
                                    "Skipped: %d password(s)",
                            importResult.imported,
                            importResult.skipped);
                    successAlert.setContentText(message);
                    successAlert.showAndWait();
                } else {
                    showAlert("Import Failed",
                            importResult.errorMessage != null
                                    ? importResult.errorMessage
                                    : "Failed to import passwords. Please check the file format.");
                }
            } catch (Exception e) {
                showAlert("Import Error", "Error importing passwords: " + e.getMessage());
            }
        }
    }

    /**
     * Clear input fields
     */
    private void clearFields() {
        nameField.clear();
        passwordField.clear();
        if (passwordTextField != null) {
            passwordTextField.clear();
        }
    }

    /**
     * Show alert dialog
     * 
     * @param title   the alert title
     * @param message the alert message
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updatePasswordPeekIcon(boolean visible) {
        if (passwordPeekToggle == null) {
            return;
        }
        Label icon = new Label(visible ? "👁" : "🙈");
        icon.setStyle("-fx-font-size: 14px;");
        passwordPeekToggle.setGraphic(icon);
    }

    /**
     * Create and show the password management dialog
     * 
     * @param parentStage the parent stage
     * @return true if OK was clicked, false otherwise
     */
    public static boolean showDialog(Stage parentStage) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(
                    PasswordManagementDialogController.class.getResource("/fxml/password_management_dialog.fxml"));
            VBox dialogPane = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Manage Saved Passwords");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(parentStage);
            dialogStage.setResizable(false);

            PasswordManagementDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);

            javafx.scene.Scene scene = new javafx.scene.Scene(dialogPane);
            dialogStage.setScene(scene);

            dialogStage.showAndWait();

            return controller.isOkClicked();
        } catch (IOException e) {
            System.err.println("Error loading password management dialog: " + e.getMessage());
            return false;
        }
    }
}
