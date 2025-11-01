package com.pdfutilities.app.controller;

import com.pdfutilities.app.model.FileItem;
import com.pdfutilities.app.model.SavedPassword;
import com.pdfutilities.app.service.PasswordManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Main controller class for the PDF Utilities application
 * Handles UI interactions and coordinates between view and model
 */
public class MainController implements Initializable {

    // File Table components
    @FXML
    private TableView<FileItem> fileTable;
    @FXML
    private TableColumn<FileItem, String> fileNameColumn;
    @FXML
    private TableColumn<FileItem, String> fileSizeColumn;
    @FXML
    private TableColumn<FileItem, String> statusColumn;
    @FXML
    private TableColumn<FileItem, String> passwordColumn;

    // Button components
    @FXML
    private Button selectFilesButton;
    @FXML
    private Button selectFolderButton;
    @FXML
    private Button removeFileButton;
    @FXML
    private Button clearAllButton;

    // Function buttons
    @FXML
    private ToggleButton convertToDocXButton;
    @FXML
    private ToggleButton compressPDFButton;
    @FXML
    private ToggleButton mergePDFsButton;
    @FXML
    private ToggleButton splitPDFButton;
    @FXML
    private ToggleButton extractTextButton;
    @FXML
    private ToggleButton convertToImageButton;

    // Tab components
    @FXML
    private TabPane functionTabPane;

    // Compression tab components
    @FXML
    private ComboBox<String> compressionLevelComboBox;
    @FXML
    private TextField targetSizeField;
    @FXML
    private ComboBox<String> targetSizeUnit;

    // Merge tab components
    @FXML
    private ListView<String> mergeOrderList;
    @FXML
    private Button moveUpButton;
    @FXML
    private Button moveDownButton;
    @FXML
    private Button removeFromMergeButton;

    // Split tab components
    @FXML
    private RadioButton splitEveryPage;
    @FXML
    private RadioButton splitCustomRange;
    @FXML
    private RadioButton splitSizeBased;
    @FXML
    private TextField customRangeField;
    @FXML
    private TextField maxSizeField;
    @FXML
    private ComboBox<String> maxSizeUnit;

    // Toggle group for split options to enforce single selection
    private final ToggleGroup splitModeGroup = new ToggleGroup();

    // Image conversion components
    @FXML
    private ComboBox<String> imageFormatComboBox;
    @FXML
    private ComboBox<String> dpiComboBox;
    @FXML
    private ComboBox<String> colorModeComboBox;
    @FXML
    private ComboBox<String> imageModeComboBox;

    // Output folder components
    @FXML
    private RadioButton sameAsInputRadioButton;
    @FXML
    private RadioButton customFolderRadioButton;
    @FXML
    private TextField outputFolderTextField;
    @FXML
    private Button browseFolderButton;

    // Progress components
    @FXML
    private ProgressBar progressBar;

    // Root overlay target for toast (we'll create on demand if null)
    private StackPane toastHost;

    // Data
    private ObservableList<FileItem> fileItems = FXCollections.observableArrayList();
    private PasswordManager passwordManager;

    // UI state
    @FXML
    private CheckBox showPasswordsCheckBox;
    private final BooleanProperty showPasswords = new SimpleBooleanProperty(false);

    // Password management components
    @FXML
    private ComboBox<SavedPassword> savedPasswordsComboBox;
    @FXML
    private Button savePasswordButton;
    @FXML
    private Button managePasswordsButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize table columns
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        fileSizeColumn.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Set table items
        fileTable.setItems(fileItems);

        // Wire show passwords checkbox to property if present (bidirectional)
        if (showPasswordsCheckBox != null) {
            showPasswordsCheckBox.selectedProperty().bindBidirectional(showPasswords);
        }
        showPasswords.addListener((obs, o, n) -> {
            if (fileTable != null)
                fileTable.refresh();
        });

        // Configure password column with masked cell
        if (passwordColumn != null) {
            passwordColumn.setCellValueFactory(new PropertyValueFactory<>("password"));
            // Ensure simple text header without a toggle
            passwordColumn.setText("Password");
            passwordColumn.setGraphic(null);
            // Custom cell: text area + inline eye toggle + dropdown on the right; supports
            // edit/display
            passwordColumn.setCellFactory(col -> new TableCell<>() {
                private final PasswordField maskedEditor = new PasswordField();
                private final TextField plainEditor = new TextField();
                private final Label displayLabel = new Label();
                private final ToggleButton rowEye = new ToggleButton("👁");
                private final Button dropdownButton = new Button("🔽");
                private final Button saveButton = new Button("💾");
                private final HBox displayBox = new HBox(6);
                private final HBox editBox = new HBox(6);

                {
                    maskedEditor.setPromptText("Enter password");
                    maskedEditor.setOnAction(e -> commitEdit(maskedEditor.getText()));
                    maskedEditor.focusedProperty().addListener((obs, was, is) -> {
                        if (!is) {
                            javafx.application.Platform.runLater(() -> {
                                var fo = currentFocusOwner();
                                if (fo == rowEye || fo == plainEditor || fo == dropdownButton || fo == saveButton)
                                    return;
                                commitEdit(maskedEditor.getText());
                            });
                        }
                    });

                    plainEditor.setPromptText("Enter password");
                    plainEditor.setOnAction(e -> commitEdit(plainEditor.getText()));
                    plainEditor.focusedProperty().addListener((obs, was, is) -> {
                        if (!is) {
                            javafx.application.Platform.runLater(() -> {
                                var fo = currentFocusOwner();
                                if (fo == rowEye || fo == maskedEditor || fo == dropdownButton || fo == saveButton)
                                    return;
                                commitEdit(plainEditor.getText());
                            });
                        }
                    });

                    rowEye.setFocusTraversable(false);
                    rowEye.setTooltip(new Tooltip("Show/Hide password (row)"));
                    // Match styling with dropdown button for consistent sizing
                    rowEye.setStyle(
                            "-fx-font-size: 16px; -fx-min-width: 26px; -fx-max-width: 26px; -fx-pref-width: 26px; -fx-min-height: 24px; -fx-max-height: 24px; -fx-pref-height: 24px; -fx-padding: 2 4; -fx-content-display: CENTER;");
                    rowEye.setOnAction(e -> {
                        int row = getIndex();
                        if (row < 0 || row >= getTableView().getItems().size())
                            return;
                        FileItem fi = getTableView().getItems().get(row);
                        if (fi == null)
                            return;

                        // keep current text from whichever editor is active (or item when not editing)
                        String currentText = isEditing()
                                ? ((editBox.getChildren().get(0) == plainEditor) ? plainEditor.getText()
                                        : maskedEditor.getText())
                                : getItem();

                        fi.setRevealPassword(!fi.isRevealPassword());

                        if (isEditing()) {
                            boolean reveal = showPasswords.get() || fi.isRevealPassword();
                            if (reveal) {
                                editBox.getChildren().set(0, plainEditor);
                                plainEditor.setText(currentText);
                                plainEditor.requestFocus();
                                plainEditor.end();
                            } else {
                                editBox.getChildren().set(0, maskedEditor);
                                maskedEditor.setText(currentText);
                                maskedEditor.requestFocus();
                                maskedEditor.end();
                            }
                            setGraphic(editBox);
                        } else {
                            updateDisplay(currentText);
                        }
                    });

                    // Configure dropdown button
                    dropdownButton.setFocusTraversable(false);
                    dropdownButton.setTooltip(new Tooltip("Select saved password"));
                    // Match styling with the eye toggle button exactly
                    dropdownButton.setStyle(
                            "-fx-font-size: 16px; -fx-min-width: 26px; -fx-max-width: 26px; -fx-pref-width: 26px; -fx-min-height: 24px; -fx-max-height: 24px; -fx-pref-height: 24px; -fx-padding: 2 4; -fx-content-display: CENTER;");
                    dropdownButton.setOnAction(e -> {
                        int row = getIndex();
                        if (row < 0 || row >= getTableView().getItems().size())
                            return;
                        FileItem fi = getTableView().getItems().get(row);
                        if (fi == null || !fi.isEncrypted())
                            return;

                        // Show context menu with saved passwords
                        ContextMenu contextMenu = new ContextMenu();

                        // Add saved passwords
                        for (SavedPassword savedPassword : MainController.this.passwordManager.getSavedPasswords()) {
                            MenuItem menuItem = new MenuItem(savedPassword.getName());
                            menuItem.setOnAction(event -> {
                                fi.setPassword(savedPassword.getPassword());
                                savedPassword.markAsUsed();
                                // Note: PasswordManager doesn't have a public savePasswords method
                                // The lastUsed timestamp will be updated when the password is used
                                updateDisplay(savedPassword.getPassword());
                                showToast("Password applied: " + savedPassword.getName(), ToastType.SUCCESS);
                            });
                            contextMenu.getItems().add(menuItem);
                        }

                        // Add separator and manage option
                        if (!contextMenu.getItems().isEmpty()) {
                            contextMenu.getItems().add(new SeparatorMenuItem());
                        }

                        MenuItem manageItem = new MenuItem("Manage Passwords...");
                        manageItem.setOnAction(event -> {
                            boolean okClicked = PasswordManagementDialogController.showDialog(
                                    (Stage) getTableView().getScene().getWindow());
                            if (okClicked) {
                                showToast("Password management completed", ToastType.SUCCESS);
                            }
                        });
                        contextMenu.getItems().add(manageItem);

                        // Show context menu
                        contextMenu.show(dropdownButton, javafx.geometry.Side.BOTTOM, 0, 0);
                    });

                    // Configure save button
                    saveButton.setFocusTraversable(false);
                    saveButton.setTooltip(new Tooltip("Save password to password manager"));
                    saveButton.setStyle(
                            "-fx-font-size: 16px; -fx-min-width: 26px; -fx-max-width: 26px; -fx-pref-width: 26px; -fx-min-height: 24px; -fx-max-height: 24px; -fx-pref-height: 24px; -fx-padding: 2 4; -fx-content-display: CENTER;");
                    saveButton.setOnAction(e -> {
                        int row = getIndex();
                        if (row < 0 || row >= getTableView().getItems().size())
                            return;
                        FileItem fi = getTableView().getItems().get(row);
                        if (fi == null || !fi.isEncrypted())
                            return;

                        // Get password from editor if editing, otherwise from FileItem
                        String password;
                        if (isEditing()) {
                            // If we're in edit mode, get password from the active editor
                            password = (editBox.getChildren().get(0) == plainEditor)
                                    ? plainEditor.getText()
                                    : maskedEditor.getText();

                            // Also commit the edit so password is saved to FileItem
                            commitEdit(password);
                        } else {
                            // If not editing, get password from FileItem
                            password = fi.getPassword();
                        }

                        if (password == null || password.trim().isEmpty()) {
                            showToast("Please enter a password first", ToastType.WARNING);
                            return;
                        }

                        // Open shared save password dialog
                        MainController.this.showSavePasswordDialogShared(password);
                    });

                    displayLabel.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(displayLabel, javafx.scene.layout.Priority.ALWAYS);
                    HBox.setHgrow(maskedEditor, javafx.scene.layout.Priority.ALWAYS);
                    HBox.setHgrow(plainEditor, javafx.scene.layout.Priority.ALWAYS);

                    displayBox.getChildren().addAll(displayLabel, dropdownButton, saveButton, rowEye);
                    editBox.getChildren().addAll(maskedEditor, dropdownButton, saveButton, rowEye);
                }

                @Override
                public void startEdit() {
                    if (isEmpty())
                        return;

                    int row = getIndex();
                    if (row < 0 || row >= getTableView().getItems().size()) {
                        return;
                    }
                    FileItem fi = getTableView().getItems().get(row);

                    // Only allow editing if the PDF is encrypted
                    if (fi == null || !fi.isEncrypted()) {
                        // Show a toast message instead of allowing edit
                        javafx.application.Platform.runLater(() -> {
                            showToast("This PDF is not password protected - no password needed", ToastType.INFO);
                        });
                        return;
                    }

                    super.startEdit();
                    String val = getItem();
                    boolean reveal = showPasswords.get() || (fi != null && fi.isRevealPassword());

                    // Apply highlighting to editors for encrypted files
                    if (fi != null && fi.isEncrypted()) {
                        maskedEditor.getStyleClass().add("password-field-highlighted");
                        plainEditor.getStyleClass().add("password-field-highlighted");
                        editBox.getStyleClass().add("password-field-highlighted");

                        // Add tooltips to editors
                        if (maskedEditor.getTooltip() == null) {
                            maskedEditor.setTooltip(new Tooltip("This PDF is password protected - enter password"));
                        }
                        if (plainEditor.getTooltip() == null) {
                            plainEditor.setTooltip(new Tooltip("This PDF is password protected - enter password"));
                        }

                        // Show dropdown button and save button for encrypted files
                        dropdownButton.setVisible(true);
                        saveButton.setVisible(true);
                    } else {
                        maskedEditor.getStyleClass().removeAll("password-field-highlighted");
                        plainEditor.getStyleClass().removeAll("password-field-highlighted");
                        editBox.getStyleClass().removeAll("password-field-highlighted");
                        maskedEditor.setTooltip(null);
                        plainEditor.setTooltip(null);

                        // Hide dropdown button and save button for non-encrypted files
                        dropdownButton.setVisible(false);
                        saveButton.setVisible(false);
                    }

                    if (reveal) {
                        // Switch editor to plain text by temporarily swapping
                        editBox.getChildren().set(0, plainEditor);
                        plainEditor.setText(val);
                    } else {
                        editBox.getChildren().set(0, maskedEditor);
                        maskedEditor.setText(val);
                    }
                    setGraphic(editBox);
                    setText(null);
                    (reveal ? plainEditor : maskedEditor).requestFocus();
                    (reveal ? plainEditor : maskedEditor).selectAll();
                }

                @Override
                public void cancelEdit() {
                    super.cancelEdit();
                    setGraphic(null);
                    updateDisplay(getItem());
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setGraphic(null);
                    } else if (isEditing()) {
                        // startEdit handles editor setup
                    } else {
                        updateDisplay(item);
                    }
                }

                @Override
                public void commitEdit(String newValue) {
                    super.commitEdit(newValue);
                    int row = getIndex();
                    if (row >= 0 && row < fileTable.getItems().size()) {
                        FileItem fi = fileTable.getItems().get(row);
                        fi.setPassword(newValue != null ? newValue : "");
                        fileTable.refresh();
                    }
                }

                private void updateDisplay(String value) {
                    int row = getIndex();
                    FileItem fi = null;
                    if (getTableView() != null && getTableView().getItems() != null
                            && row >= 0 && row < getTableView().getItems().size()) {
                        fi = getTableView().getItems().get(row);
                    }
                    boolean reveal = showPasswords.get() || (fi != null && fi.isRevealPassword());
                    rowEye.setSelected(fi != null && fi.isRevealPassword());

                    // Apply highlighting and text for encrypted files
                    if (fi != null && fi.isEncrypted()) {
                        displayLabel.setText(reveal ? (value == null ? "" : value) : mask(value));
                        displayLabel.getStyleClass().removeAll("password-not-required");
                        displayLabel.getStyleClass().add("password-label-highlighted");
                        displayBox.getStyleClass().add("password-field-highlighted");
                        getStyleClass().add("password-cell-highlighted");

                        // Show the eye button, dropdown button, and save button for encrypted files
                        rowEye.setVisible(true);
                        dropdownButton.setVisible(true);
                        saveButton.setVisible(true);

                        // Add tooltip to indicate password is required
                        if (displayLabel.getTooltip() == null) {
                            Tooltip tooltip = new Tooltip(
                                    "This PDF is password protected - enter password or select from saved passwords");
                            displayLabel.setTooltip(tooltip);
                        }
                    } else {
                        // For non-encrypted files, show "Not Required" and disable editing
                        displayLabel.setText("Not Required");
                        displayLabel.getStyleClass().add("password-not-required");
                        displayLabel.getStyleClass().removeAll("password-label-highlighted");
                        displayBox.getStyleClass().removeAll("password-field-highlighted");
                        getStyleClass().removeAll("password-cell-highlighted");

                        // Add tooltip to indicate password is not needed
                        if (displayLabel.getTooltip() == null) {
                            Tooltip tooltip = new Tooltip("This PDF is not password protected");
                            displayLabel.setTooltip(tooltip);
                        }

                        // Hide the eye button, dropdown button, and save button for non-encrypted files
                        rowEye.setVisible(false);
                        dropdownButton.setVisible(false);
                        saveButton.setVisible(false);
                    }

                    setGraphic(displayBox);
                    setText(null);
                }

                private javafx.scene.Node currentFocusOwner() {
                    var tv = getTableView();
                    if (tv == null)
                        return null;
                    var scene = tv.getScene();
                    return scene == null ? null : scene.getFocusOwner();
                }

                private String mask(String value) {
                    if (value == null || value.isEmpty())
                        return "";
                    return "•".repeat(Math.min(value.length(), 12));
                }

                private void showSavePasswordDialog(String password) {
                    // Create custom dialog; validate without closing on errors
                    Dialog<Void> dialog = new Dialog<>();
                    dialog.setTitle("Save Password");
                    dialog.setHeaderText("Save Password");

                    // Set button types
                    ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
                    dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

                    // Layout with fixed pref width to keep size stable
                    VBox vbox = new VBox(10);
                    vbox.setPadding(new Insets(20));
                    vbox.setPrefWidth(420);

                    Label instructionLabel = new Label("Enter a descriptive name for this password:");
                    TextField textField = new TextField();
                    Label errorLabel = new Label();
                    errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
                    errorLabel.setWrapText(true);
                    errorLabel.setVisible(false);

                    vbox.getChildren().addAll(instructionLabel, textField, errorLabel);
                    dialog.getDialogPane().setContent(vbox);
                    dialog.getDialogPane().setPrefWidth(440);
                    dialog.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);

                    // Disable Save when empty
                    final javafx.scene.Node saveNode = dialog.getDialogPane().lookupButton(saveButtonType);
                    saveNode.disableProperty().bind(textField.textProperty().isEmpty());

                    // Validate on Save; prevent close on error
                    saveNode.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
                        String nameRaw = textField.getText();
                        String name = nameRaw == null ? "" : nameRaw.trim();

                        if (name.isEmpty()) {
                            errorLabel.setText("Password name cannot be empty");
                            errorLabel.setVisible(true);
                            textField.requestFocus();
                            ev.consume();
                            return;
                        }

                        boolean added = MainController.this.passwordManager.addPassword(name, password);
                        if (!added) {
                            errorLabel.setText(
                                    "A password with this name already exists. Please enter a different name.");
                            errorLabel.setVisible(true);
                            textField.requestFocus();
                            textField.selectAll();
                            ev.consume();
                            return;
                        }

                        // Success: refresh any UI and allow close
                        showToast("Password saved as '" + name + "'", ToastType.SUCCESS);
                        if (MainController.this.savedPasswordsComboBox != null) {
                            MainController.this.savedPasswordsComboBox
                                    .setItems(MainController.this.passwordManager.getSavedPasswords());
                        }
                    });

                    dialog.showAndWait();
                }
            });
            passwordColumn.setEditable(true);
            if (fileTable != null)
                fileTable.setEditable(true);
        }

        // Per-row eye icon column removed in favor of inline icon inside password cells

        // Initialize combo boxes
        initializeComboBoxes();

        // Initialize password management
        initializePasswordManagement();

        // Initialize left nav toggle group and sync with tabs
        initializeNavAndTabs();

        // Initialize split mode toggle group (mutually exclusive)
        initializeSplitModeGroup();

        // Initial toast
        showToast("Ready", ToastType.INFO);
    }

    /**
     * Initialize combo boxes with default values
     */
    private void initializeComboBoxes() {
        // Compression levels
        if (compressionLevelComboBox != null) {
            compressionLevelComboBox.setItems(FXCollections.observableArrayList(
                    "Tiny (Very Low Quality)",
                    "Smallest (Low Quality)",
                    "Small (Low-Medium Quality)",
                    "Balanced (Medium Quality)",
                    "Balanced+ (Medium-High Quality)",
                    "Largest (High Quality)"));
            // Default selection strategy:
            // For now, keep "Balanced (Medium Quality)" as default visible selection
            compressionLevelComboBox.getSelectionModel().select("Balanced (Medium Quality)");
        }
        if (targetSizeUnit != null) {
            targetSizeUnit.setItems(FXCollections.observableArrayList("KB", "MB"));
            targetSizeUnit.getSelectionModel().select("KB");
        }

        // Image formats
        if (imageFormatComboBox != null) {
            imageFormatComboBox.setItems(FXCollections.observableArrayList("PNG", "JPG"));
            imageFormatComboBox.getSelectionModel().select("PNG");
        }

        // DPI options
        if (dpiComboBox != null) {
            dpiComboBox.setItems(FXCollections.observableArrayList("75", "100", "125", "150", "200", "250", "300"));
            dpiComboBox.getSelectionModel().select("150");
        }

        // Color mode
        if (colorModeComboBox != null) {
            colorModeComboBox.setItems(FXCollections.observableArrayList("Color", "Black & White"));
            colorModeComboBox.getSelectionModel().select("Color");
        }

        // Image mode
        if (imageModeComboBox != null) {
            imageModeComboBox.setItems(FXCollections.observableArrayList(
                    "Each Page to Single Image",
                    "Entire PDF to Single Image"));
            imageModeComboBox.getSelectionModel().select(0);
        }

        // Split size unit
        if (maxSizeUnit != null) {
            maxSizeUnit.setItems(FXCollections.observableArrayList("KB", "MB"));
            maxSizeUnit.getSelectionModel().select("MB");
        }
    }

    /**
     * Initialize password management components
     */
    private void initializePasswordManagement() {
        // Initialize password manager
        passwordManager = new PasswordManager();

        // Initialize saved passwords combo box
        if (savedPasswordsComboBox != null) {
            savedPasswordsComboBox.setItems(passwordManager.getSavedPasswords());

            // Handle selection from combo box
            savedPasswordsComboBox.setOnAction(e -> {
                SavedPassword selectedPassword = savedPasswordsComboBox.getSelectionModel().getSelectedItem();
                if (selectedPassword != null) {
                    // Apply password to selected row in table
                    FileItem selectedFileItem = fileTable.getSelectionModel().getSelectedItem();
                    if (selectedFileItem != null) {
                        selectedFileItem.setPassword(selectedPassword.getPassword());
                        fileTable.refresh();
                        showToast("Password applied to " + selectedFileItem.getFileName(), ToastType.SUCCESS);
                    } else {
                        showToast("Please select a file first", ToastType.WARNING);
                    }
                }
            });
        }
    }

    /**
     * Handle select files button click
     */
    @FXML
    private void handleSelectFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select PDF Files");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(
                selectFilesButton.getScene().getWindow());

        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                FileItem fi = new FileItem(file);
                boolean enc = com.pdfutilities.app.service.PdfSecurityUtils.isPasswordProtected(file);
                fi.setEncrypted(enc);
                if (enc) {
                    fi.setStatus("Encrypted");
                }
                fileItems.add(fi);
            }
            showToast("Selected " + selectedFiles.size() + " file(s)", ToastType.SUCCESS);

            // Refresh password field highlighting for newly added files
            refreshPasswordFieldHighlighting();
        }
    }

    /**
     * Handle select folder button click
     */
    @FXML
    private void handleSelectFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder Containing PDF Files");

        File selectedDirectory = directoryChooser.showDialog(
                selectFolderButton.getScene().getWindow());

        if (selectedDirectory != null) {
            // TODO: Implement folder scanning for PDF files
            showToast("Folder selected: " + selectedDirectory.getName(), ToastType.INFO);
        }
    }

    /**
     * Handle remove file button click
     */
    @FXML
    private void handleRemoveFile() {
        FileItem selectedItem = fileTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            fileItems.remove(selectedItem);
            showToast("File removed", ToastType.INFO);
        } else {
            showToast("Please select a file to remove", ToastType.WARNING);
        }
    }

    /**
     * Handle clear all button click
     */
    @FXML
    private void handleClearAll() {
        fileItems.clear();
        showToast("All files cleared", ToastType.INFO);
    }

    /**
     * Handle save password button click
     */
    @FXML
    private void handleSavePassword() {
        FileItem selectedFileItem = fileTable.getSelectionModel().getSelectedItem();
        if (selectedFileItem == null) {
            showToast("Please select a row in the table first", ToastType.WARNING);
            return;
        }

        String password = selectedFileItem.getPassword();
        if (password == null || password.trim().isEmpty()) {
            showToast("Please enter a password for the selected row first", ToastType.WARNING);
            return;
        }

        // Use shared dialog
        showSavePasswordDialogShared(password);
    }

    /**
     * Handle manage passwords button click
     */
    @FXML
    private void handleManagePasswords() {
        Stage owner = null;
        try {
            if (fileTable != null && fileTable.getScene() != null) {
                owner = (Stage) fileTable.getScene().getWindow();
            } else if (selectFilesButton != null && selectFilesButton.getScene() != null) {
                owner = (Stage) selectFilesButton.getScene().getWindow();
            }
        } catch (Exception ignore) {
        }

        boolean okClicked = PasswordManagementDialogController.showDialog(owner);

        if (okClicked) {
            // Refresh combo box after management dialog, if present in UI
            if (savedPasswordsComboBox != null) {
                savedPasswordsComboBox.setItems(passwordManager.getSavedPasswords());
            }
            showToast("Password management completed", ToastType.SUCCESS);
        }
    }

    private void initializeNavAndTabs() {
        // Make left nav behave exclusively and select tabs accordingly
        ToggleGroup navGroup = new ToggleGroup();
        if (convertToDocXButton != null)
            convertToDocXButton.setToggleGroup(navGroup);
        if (compressPDFButton != null)
            compressPDFButton.setToggleGroup(navGroup);
        if (mergePDFsButton != null)
            mergePDFsButton.setToggleGroup(navGroup);
        if (splitPDFButton != null)
            splitPDFButton.setToggleGroup(navGroup);
        if (extractTextButton != null)
            extractTextButton.setToggleGroup(navGroup);
        if (convertToImageButton != null)
            convertToImageButton.setToggleGroup(navGroup);
        // default select first
        if (convertToDocXButton != null)
            convertToDocXButton.setSelected(true);
        if (functionTabPane != null)
            functionTabPane.getSelectionModel().select(0);

        // Enable drag-and-drop reordering for the main file table (affects all
        // operations ordering incl. Merge)
        enableDragReorderForFileTable();

        // Enable drag-and-drop reordering for merge order list if present (optional
        // separate UI)
        enableDragReorderForMergeList();
    }

    // Ensure only one split option can be selected at a time
    private void initializeSplitModeGroup() {
        if (splitEveryPage != null)
            splitEveryPage.setToggleGroup(splitModeGroup);
        if (splitCustomRange != null)
            splitCustomRange.setToggleGroup(splitModeGroup);
        if (splitSizeBased != null)
            splitSizeBased.setToggleGroup(splitModeGroup);

        // Set default selection if none is selected
        if (splitModeGroup.getSelectedToggle() == null && splitEveryPage != null) {
            splitEveryPage.setSelected(true);
        }
    }

    /**
     * Handle convert to DocX button click
     */
    @FXML
    private void handleConvertToDocX() {
        functionTabPane.getSelectionModel().select(0); // First tab
        if (fileItems.isEmpty()) {
            showToast("Please select files first", ToastType.WARNING);
            return;
        }
        // Resolve output directory
        String outDir = null;
        try {
            if (customFolderRadioButton != null && customFolderRadioButton.isSelected()) {
                outDir = outputFolderTextField != null ? outputFolderTextField.getText() : null;
            } else if (sameAsInputRadioButton != null && sameAsInputRadioButton.isSelected()) {
                // Use parent of first file
                File f = fileItems.get(0).getFile();
                outDir = (f != null && f.getParentFile() != null) ? f.getParentFile().getAbsolutePath() : null;
            }
            if (outDir == null || outDir.isBlank()) {
                showToast("Select output folder (bottom) or choose 'Same as Input'", ToastType.WARNING);
                return;
            }
            // Convert using service
            java.util.List<File> inputs = fileItems.stream().map(FileItem::getFile).toList();
            com.pdfutilities.app.service.DocxConversionService svc = new com.pdfutilities.app.service.DocxConversionService();
            svc.setFilePasswords(createPasswordMap());

            boolean ok = svc.execute(inputs, outDir);
            if (ok) {
                showToast("DOCX created in: " + outDir, ToastType.SUCCESS);
            } else {
                int missing = countEncryptedMissingPasswords(inputs);
                if (missing == inputs.size() && missing > 0) {
                    showToast("All files failed due to password protection. Enter passwords and retry.",
                            ToastType.ERROR);
                } else if (missing > 0) {
                    showToast("Some files failed due to password protection.", ToastType.WARNING);
                } else {
                    showToast("Some files failed to convert. Check logs.", ToastType.ERROR);
                }
            }
        } catch (Exception ex) {
            showToast("Conversion failed: " + ex.getMessage(), ToastType.ERROR);
            ex.printStackTrace();
        }
    }

    /**
     * Handle compress PDF button click
     */
    @FXML
    private void handleCompressPDF() {
        functionTabPane.getSelectionModel().select(1); // Second tab
        if (fileItems.isEmpty()) {
            showToast("Please select files first", ToastType.WARNING);
            return;
        }
        long encCount = fileItems.stream().filter(FileItem::isEncrypted).count();
        if (encCount > 0) {
            showToast(encCount + " file(s) are encrypted. Enter password(s) in the table to process them.",
                    ToastType.WARNING);
        }
        // Resolve output directory
        String outDir = null;
        if (customFolderRadioButton != null && customFolderRadioButton.isSelected()) {
            outDir = outputFolderTextField != null ? outputFolderTextField.getText() : null;
        } else if (sameAsInputRadioButton != null && sameAsInputRadioButton.isSelected()) {
            File f = fileItems.get(0).getFile();
            outDir = (f != null && f.getParentFile() != null) ? f.getParentFile().getAbsolutePath() : null;
        }
        if (outDir == null || outDir.isBlank()) {
            showToast("Select output folder (bottom) or choose 'Same as Input'", ToastType.WARNING);
            return;
        }

        // Determine compression level from UI (default MEDIUM)
        com.pdfutilities.app.service.PDFCompressionService.CompressionLevel level = com.pdfutilities.app.service.PDFCompressionService.CompressionLevel.MEDIUM;

        if (compressionLevelComboBox != null && compressionLevelComboBox.getValue() != null) {
            String sel = compressionLevelComboBox.getValue();
            if (sel.startsWith("Tiny")) {
                level = com.pdfutilities.app.service.PDFCompressionService.CompressionLevel.VERY_LOW;
            } else if (sel.startsWith("Smallest")) {
                level = com.pdfutilities.app.service.PDFCompressionService.CompressionLevel.LOW;
            } else if (sel.startsWith("Small (Low-Medium")) {
                level = com.pdfutilities.app.service.PDFCompressionService.CompressionLevel.LOW_MEDIUM;
            } else if (sel.startsWith("Balanced+")) {
                level = com.pdfutilities.app.service.PDFCompressionService.CompressionLevel.MEDIUM_HIGH;
            } else if (sel.startsWith("Largest")) {
                level = com.pdfutilities.app.service.PDFCompressionService.CompressionLevel.HIGH;
            } else {
                level = com.pdfutilities.app.service.PDFCompressionService.CompressionLevel.MEDIUM;
            }
        }

        try {
            java.util.List<File> inputs = fileItems.stream().map(FileItem::getFile).toList();
            com.pdfutilities.app.service.PDFCompressionService svc = new com.pdfutilities.app.service.PDFCompressionService(
                    level);
            svc.setFilePasswords(createPasswordMap());
            showToast("Compressing PDFs...", ToastType.INFO);
            boolean ok = svc.execute(inputs, outDir);
            if (ok) {
                showToast("Compressed PDF(s) saved to: " + outDir, ToastType.SUCCESS);
            } else {
                int missing = countEncryptedMissingPasswords(inputs);
                if (missing == inputs.size() && missing > 0) {
                    showToast("All files failed due to password protection. Enter passwords and retry.",
                            ToastType.ERROR);
                } else if (missing > 0) {
                    showToast("Some files failed due to password protection.", ToastType.WARNING);
                } else {
                    showToast("Some files failed to compress. Check logs.", ToastType.ERROR);
                }
            }
        } catch (Exception ex) {
            showToast("Compression failed: " + ex.getMessage(), ToastType.ERROR);
            ex.printStackTrace();
        }
    }

    /**
     * Handle merge PDFs button click
     */
    @FXML
    private void handleMergePDFs() {
        functionTabPane.getSelectionModel().select(2); // Third tab
        if (fileItems.size() >= 2) {
            long encCount = fileItems.stream().filter(FileItem::isEncrypted).count();
            if (encCount > 0) {
                showToast(encCount + " file(s) are encrypted. Enter password(s) in the table to include them.",
                        ToastType.WARNING);
            }
            // Resolve output directory
            String outDir = null;
            if (customFolderRadioButton != null && customFolderRadioButton.isSelected()) {
                outDir = outputFolderTextField != null ? outputFolderTextField.getText() : null;
            } else if (sameAsInputRadioButton != null && sameAsInputRadioButton.isSelected()) {
                File f = fileItems.get(0).getFile();
                outDir = (f != null && f.getParentFile() != null) ? f.getParentFile().getAbsolutePath() : null;
            }
            if (outDir == null || outDir.isBlank()) {
                showToast("Select output folder (bottom) or choose 'Same as Input'", ToastType.WARNING);
                return;
            }
            try {
                // If a dedicated merge order list is visible and populated, respect its order;
                // else use table order.
                java.util.List<File> inputs;
                if (mergeOrderList != null && mergeOrderList.getItems() != null
                        && !mergeOrderList.getItems().isEmpty()) {
                    // Map mergeOrderList entries back to files by matching displayed names
                    var nameToFile = fileItems.stream()
                            .collect(java.util.stream.Collectors.toMap(fi -> fi.getFileName(), FileItem::getFile,
                                    (a, b) -> a, java.util.LinkedHashMap::new));
                    inputs = mergeOrderList.getItems().stream()
                            .map(nameToFile::get)
                            .filter(java.util.Objects::nonNull)
                            .toList();
                    if (inputs.size() < 2) {
                        // Fallback to file table order if mapping failed
                        inputs = fileItems.stream().map(FileItem::getFile).toList();
                    }
                } else {
                    inputs = fileItems.stream().map(FileItem::getFile).toList();
                }

                com.pdfutilities.app.service.PDFMergeService svc = new com.pdfutilities.app.service.PDFMergeService();
                svc.setFilePasswords(createPasswordMap());
                showToast("Merging PDFs...", ToastType.INFO);
                boolean ok = svc.execute(inputs, outDir);
                if (ok) {
                    showToast("Merged PDF created in: " + outDir, ToastType.SUCCESS);
                } else {
                    int missing = countEncryptedMissingPasswords(inputs);
                    if (missing == inputs.size() && missing > 0) {
                        showToast("All files failed due to password protection. Enter passwords and retry.",
                                ToastType.ERROR);
                    } else if (missing > 0) {
                        showToast("Some files failed due to password protection.", ToastType.WARNING);
                    } else {
                        showToast("Merge failed. Check logs.", ToastType.ERROR);
                    }
                }
            } catch (Exception ex) {
                showToast("Merge failed: " + ex.getMessage(), ToastType.ERROR);
                ex.printStackTrace();
            }
        } else {
            showToast("Please select at least 2 files to merge", ToastType.WARNING);
        }
    }

    /**
     * Handle split PDF button click
     */
    @FXML
    private void handleSplitPDF() {
        functionTabPane.getSelectionModel().select(3); // Fourth tab
        if (fileItems.isEmpty()) {
            showToast("Please select files first", ToastType.WARNING);
            return;
        }
        long encCount = fileItems.stream().filter(FileItem::isEncrypted).count();
        if (encCount > 0) {
            showToast(encCount + " file(s) are encrypted. Enter password(s) in the table to split them.",
                    ToastType.WARNING);
        }

        // Resolve output directory
        String outDir = null;
        if (customFolderRadioButton != null && customFolderRadioButton.isSelected()) {
            outDir = outputFolderTextField != null ? outputFolderTextField.getText() : null;
        } else if (sameAsInputRadioButton != null && sameAsInputRadioButton.isSelected()) {
            File f = fileItems.get(0).getFile();
            outDir = (f != null && f.getParentFile() != null) ? f.getParentFile().getAbsolutePath() : null;
        }
        if (outDir == null || outDir.isBlank()) {
            showToast("Select output folder (bottom) or choose 'Same as Input'", ToastType.WARNING);
            return;
        }

        try {
            // Configure split service from UI
            com.pdfutilities.app.service.PDFSplitService svc = new com.pdfutilities.app.service.PDFSplitService();

            // Determine split mode
            if (splitEveryPage != null && splitEveryPage.isSelected()) {
                svc.setSplitMode(com.pdfutilities.app.service.PDFSplitService.SplitMode.EVERY_PAGE);
            } else if (splitCustomRange != null && splitCustomRange.isSelected()) {
                svc.setSplitMode(com.pdfutilities.app.service.PDFSplitService.SplitMode.CUSTOM_RANGE);
                if (customRangeField != null) {
                    svc.setCustomRange(customRangeField.getText() != null ? customRangeField.getText().trim() : "");
                }
            } else if (splitSizeBased != null && splitSizeBased.isSelected()) {
                svc.setSplitMode(com.pdfutilities.app.service.PDFSplitService.SplitMode.SIZE_BASED);
                // Parse size and unit
                long sizeBytes = 0L;
                if (maxSizeField != null && maxSizeField.getText() != null && !maxSizeField.getText().isBlank()) {
                    try {
                        double val = Double.parseDouble(maxSizeField.getText().trim());
                        String unit = (maxSizeUnit != null && maxSizeUnit.getValue() != null) ? maxSizeUnit.getValue()
                                : "MB";
                        if ("KB".equalsIgnoreCase(unit)) {
                            sizeBytes = (long) (val * 1024L);
                        } else {
                            sizeBytes = (long) (val * 1024L * 1024L);
                        }
                    } catch (NumberFormatException nfe) {
                        showToast("Invalid size value. Using default.", ToastType.WARNING);
                    }
                }
                svc.setMaxSizeInBytes(sizeBytes);
            } else {
                // Default
                svc.setSplitMode(com.pdfutilities.app.service.PDFSplitService.SplitMode.EVERY_PAGE);
            }

            showToast("Splitting PDF...", ToastType.INFO);
            java.util.List<File> inputs = fileItems.stream().map(FileItem::getFile).toList();
            svc.setFilePasswords(createPasswordMap());
            boolean ok = svc.execute(inputs, outDir);
            if (ok) {
                showToast("Split complete. Files saved to: " + outDir, ToastType.SUCCESS);
            } else {
                int missing = countEncryptedMissingPasswords(inputs);
                if (missing == inputs.size() && missing > 0) {
                    showToast("All files failed due to password protection. Enter password(s) and retry.",
                            ToastType.ERROR);
                } else if (missing > 0) {
                    showToast("Some files failed due to password protection.", ToastType.WARNING);
                } else {
                    showToast("Split encountered errors. Check logs.", ToastType.ERROR);
                }
            }
        } catch (Exception ex) {
            showToast("Split failed: " + ex.getMessage(), ToastType.ERROR);
            ex.printStackTrace();
        }
    }

    /**
     * Handle extract text/images button click
     */
    @FXML
    private void handleExtractText() {
        functionTabPane.getSelectionModel().select(4); // Fifth tab
        if (fileItems.isEmpty()) {
            showToast("Please select files first", ToastType.WARNING);
            return;
        }
        long encCount = fileItems.stream().filter(FileItem::isEncrypted).count();
        if (encCount > 0) {
            showToast(encCount + " file(s) are encrypted. Enter password(s) in the table.", ToastType.WARNING);
        }
        // Resolve output directory
        String outDir = null;
        if (customFolderRadioButton != null && customFolderRadioButton.isSelected()) {
            outDir = outputFolderTextField != null ? outputFolderTextField.getText() : null;
        } else if (sameAsInputRadioButton != null && sameAsInputRadioButton.isSelected()) {
            File f = fileItems.get(0).getFile();
            outDir = (f != null && f.getParentFile() != null) ? f.getParentFile().getAbsolutePath() : null;
        }
        if (outDir == null || outDir.isBlank()) {
            showToast("Select output folder (bottom) or choose 'Same as Input'", ToastType.WARNING);
            return;
        }
        try {
            java.util.List<File> inputs = fileItems.stream().map(FileItem::getFile).toList();

            // Determine what to extract based on checkboxes in the Extract tab.
            // Defaults: if nothing is selected, extract text (backward-compatible).
            boolean wantText = true;
            boolean wantImages = false;

            // These controls are declared in FXML: extractTextCheckBox,
            // extractImagesCheckBox
            // They are optional at runtime; null-check before use.
            try {
                // Use reflection-free access by looking up via fx:id bindings if they exist as
                // @FXML fields.
                // The fields are not declared in this controller, so we query via lookup from
                // the current scene.
                CheckBox textCb = (CheckBox) fileTable.getScene().lookup("#extractTextCheckBox");
                CheckBox imgCb = (CheckBox) fileTable.getScene().lookup("#extractImagesCheckBox");
                if (textCb != null || imgCb != null) {
                    // If checkboxes are present, read values with sensible defaults
                    wantText = (textCb == null) ? true : textCb.isSelected();
                    wantImages = (imgCb == null) ? false : imgCb.isSelected();

                    // If neither is selected, default to text to avoid "no-op"
                    if (!wantText && !wantImages) {
                        wantText = true;
                    }
                }
            } catch (Exception ignore) {
                // Fallback to defaults if lookup fails
                wantText = true;
                wantImages = false;
            }

            com.pdfutilities.app.service.TextExtractionService svc = new com.pdfutilities.app.service.TextExtractionService();
            svc.setExtractText(wantText);
            svc.setExtractImages(wantImages);
            svc.setFilePasswords(createPasswordMap());

            if (wantText && wantImages) {
                showToast("Extracting text and images...", ToastType.INFO);
            } else if (wantImages) {
                showToast("Extracting images...", ToastType.INFO);
            } else {
                showToast("Extracting text...", ToastType.INFO);
            }

            boolean ok = svc.execute(inputs, outDir);
            if (ok) {
                if (wantText && wantImages) {
                    showToast("Text and images extracted to: " + outDir, ToastType.SUCCESS);
                } else if (wantImages) {
                    showToast("Images extracted to: " + outDir, ToastType.SUCCESS);
                } else {
                    showToast("Text extracted to: " + outDir, ToastType.SUCCESS);
                }
            } else {
                int missing = countEncryptedMissingPasswords(inputs);
                if (missing == inputs.size() && missing > 0) {
                    showToast("All files failed due to password protection. Enter password(s) and retry.",
                            ToastType.ERROR);
                } else if (missing > 0) {
                    showToast("Some files failed due to password protection.", ToastType.WARNING);
                } else {
                    showToast("Some files failed to extract. Check logs.", ToastType.ERROR);
                }
            }
        } catch (Exception ex) {
            showToast("Extraction failed: " + ex.getMessage(), ToastType.ERROR);
            ex.printStackTrace();
        }
    }

    /**
     * Handle convert to image button click
     */
    @FXML
    private void handleConvertToImage() {
        functionTabPane.getSelectionModel().select(5); // Sixth tab
        if (fileItems.isEmpty()) {
            showToast("Please select files first", ToastType.WARNING);
            return;
        }
        long encCount = fileItems.stream().filter(FileItem::isEncrypted).count();
        if (encCount > 0) {
            showToast(encCount + " file(s) are encrypted. Enter password(s) in the table.", ToastType.WARNING);
        }
        // Resolve output directory
        String outDir = null;
        if (customFolderRadioButton != null && customFolderRadioButton.isSelected()) {
            outDir = outputFolderTextField != null ? outputFolderTextField.getText() : null;
        } else if (sameAsInputRadioButton != null && sameAsInputRadioButton.isSelected()) {
            File f = fileItems.get(0).getFile();
            outDir = (f != null && f.getParentFile() != null) ? f.getParentFile().getAbsolutePath() : null;
        }
        if (outDir == null || outDir.isBlank()) {
            showToast("Select output folder (bottom) or choose 'Same as Input'", ToastType.WARNING);
            return;
        }
        try {
            java.util.List<File> inputs = fileItems.stream().map(FileItem::getFile).toList();
            // Use existing PDFToImageService for page rendering to images (PNG/JPG) based
            // on UI selections
            com.pdfutilities.app.service.PDFToImageService svc = new com.pdfutilities.app.service.PDFToImageService();
            svc.setFilePasswords(createPasswordMap());
            // Configure from UI if present
            if (imageFormatComboBox != null && imageFormatComboBox.getValue() != null) {
                String fmt = imageFormatComboBox.getValue();
                if ("JPG".equalsIgnoreCase(fmt) || "JPEG".equalsIgnoreCase(fmt)) {
                    svc.setImageFormat(com.pdfutilities.app.service.PDFToImageService.ImageFormat.JPG);
                } else {
                    svc.setImageFormat(com.pdfutilities.app.service.PDFToImageService.ImageFormat.PNG);
                }
            }
            if (dpiComboBox != null && dpiComboBox.getValue() != null) {
                try {
                    int dpi = Integer.parseInt(dpiComboBox.getValue());
                    svc.setDpi(dpi);
                } catch (NumberFormatException ignore) {
                }
            }
            if (colorModeComboBox != null && colorModeComboBox.getValue() != null) {
                String mode = colorModeComboBox.getValue();
                if ("Black & White".equalsIgnoreCase(mode) || "Black & White".equals(mode)) {
                    svc.setColorMode(com.pdfutilities.app.service.PDFToImageService.ColorMode.GRAYSCALE);
                } else {
                    svc.setColorMode(com.pdfutilities.app.service.PDFToImageService.ColorMode.COLOR);
                }
            }
            // Image mode selection (if supported by service; default is per-page)
            showToast("Converting to images...", ToastType.INFO);
            boolean ok = svc.execute(inputs, outDir);
            if (ok) {
                showToast("Images saved to: " + outDir, ToastType.SUCCESS);
            } else {
                int missing = countEncryptedMissingPasswords(inputs);
                if (missing == inputs.size() && missing > 0) {
                    showToast("All files failed due to password protection. Enter password(s) and retry.",
                            ToastType.ERROR);
                } else if (inputs.size() > 1) {
                    showToast("Some files failed to convert.", ToastType.WARNING);
                } else {
                    showToast("Conversion failed. The file may be password protected.", ToastType.ERROR);
                }
            }
        } catch (Exception ex) {
            showToast("Image conversion failed: " + ex.getMessage(), ToastType.ERROR);
            ex.printStackTrace();
        }
    }

    /**
     * Handle browse folder button click
     */
    @FXML
    private void handleBrowseFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Output Folder");

        File selectedDirectory = directoryChooser.showDialog(
                browseFolderButton.getScene().getWindow());

        if (selectedDirectory != null) {
            outputFolderTextField.setText(selectedDirectory.getAbsolutePath());
            customFolderRadioButton.setSelected(true);
            showToast("Output folder selected", ToastType.SUCCESS);
        }
    }

    /**
     * Handle exit menu item
     */
    @FXML
    private void handleExit() {
        System.exit(0);
    }

    /**
     * Handle about menu item
     */
    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About PDF Utilities");
        alert.setHeaderText("PDF Utilities App");
        alert.setContentText("Version 1.0.0\n\nA comprehensive tool for PDF manipulation.");
        alert.showAndWait();
    }

    // ---------- Helper methods ----------

    /**
     * Create a map of passwords for encrypted files
     * 
     * @return Map of File to password for encrypted files that have passwords
     */
    private java.util.Map<File, String> createPasswordMap() {
        java.util.Map<File, String> passwords = new java.util.HashMap<>();
        for (FileItem item : fileItems) {
            if (item.isEncrypted() && item.getPassword() != null && !item.getPassword().trim().isEmpty()) {
                passwords.put(item.getFile(), item.getPassword());
            }
        }
        return passwords;
    }

    // ---------- Toast utilities ----------

    private enum ToastType {
        SUCCESS, INFO, WARNING, ERROR
    }

    /**
     * Count how many of the given files correspond to items that are marked
     * encrypted and have no password entered.
     */
    private int countEncryptedMissingPasswords(java.util.List<File> files) {
        if (files == null || files.isEmpty())
            return 0;
        int count = 0;
        for (File f : files) {
            FileItem match = null;
            for (FileItem fi : fileItems) {
                if (fi.getFile() != null && fi.getFile().equals(f)) {
                    match = fi;
                    break;
                }
            }
            if (match != null && match.isEncrypted()) {
                String pw = match.getPassword();
                if (pw == null || pw.isBlank())
                    count++;
            }
        }
        return count;
    }

    /**
     * Refresh password field highlighting for all rows in the table
     */
    private void refreshPasswordFieldHighlighting() {
        if (fileTable != null) {
            fileTable.refresh();
        }
    }

    // Shared dialog for saving a password by name with inline validation
    private void showSavePasswordDialogShared(String password) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Save Password");
        dialog.setHeaderText("Save Password");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.setPrefWidth(420);

        Label instructionLabel = new Label("Enter a descriptive name for this password:");
        TextField textField = new TextField();
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);

        vbox.getChildren().addAll(instructionLabel, textField, errorLabel);
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().setPrefWidth(440);
        dialog.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);

        final javafx.scene.Node saveNode = dialog.getDialogPane().lookupButton(saveButtonType);
        saveNode.disableProperty().bind(textField.textProperty().isEmpty());

        saveNode.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String nameRaw = textField.getText();
            String name = nameRaw == null ? "" : nameRaw.trim();

            if (name.isEmpty()) {
                errorLabel.setText("Password name cannot be empty");
                errorLabel.setVisible(true);
                textField.requestFocus();
                ev.consume();
                return;
            }

            boolean added = passwordManager.addPassword(name, password);
            if (!added) {
                errorLabel.setText("A password with this name already exists. Please enter a different name.");
                errorLabel.setVisible(true);
                textField.requestFocus();
                textField.selectAll();
                ev.consume();
                return;
            }

            showToast("Password saved as '" + name + "'", ToastType.SUCCESS);
            if (savedPasswordsComboBox != null) {
                savedPasswordsComboBox.setItems(passwordManager.getSavedPasswords());
            }
        });

        dialog.showAndWait();
    }

    private void showToast(String message, ToastType type) {
        // Defer until the scene is ready to avoid NPE when called early (e.g., during
        // initialize)
        if (fileTable == null)
            return;
        if (fileTable.getScene() == null) {
            // schedule once the node is attached to a scene
            javafx.application.Platform.runLater(() -> showToast(message, type));
            return;
        }
        StackPane host = getToastHost();
        if (host == null)
            return;

        Label toast = new Label(message);
        toast.setStyle(getToastStyle(type));
        toast.setMaxWidth(Double.MAX_VALUE);
        toast.setMinHeight(Region.USE_PREF_SIZE);
        toast.setPadding(new Insets(8, 12, 8, 12));
        toast.setOpacity(0);

        // Position bottom-right with a container
        StackPane.setAlignment(toast, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toast, new Insets(0, 10, 10, 0));

        host.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(2.0));
        fadeOut.setOnFinished(e -> host.getChildren().remove(toast));

        fadeIn.setOnFinished(e -> fadeOut.play());
        fadeIn.play();
    }

    private String getToastStyle(ToastType type) {
        // Use one-pass-box shadow instead of gaussian to avoid GPU driver issues
        // (ArrayIndexOutOfBounds in D3D pipeline)
        String base = "-fx-background-radius:6; -fx-background-color:%s; -fx-text-fill:white; -fx-font-size:12px; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.35), 6, 0.0, 0, 2);";
        String color;
        switch (type) {
            case SUCCESS:
                color = "#2e7d32";
                break; // green
            case INFO:
                color = "#1565c0";
                break; // blue
            case WARNING:
                color = "#ef6c00";
                break; // orange
            case ERROR:
                color = "#c62828";
                break; // red
            default:
                color = "#424242";
        }
        return String.format(base, color);
    }

    private StackPane getToastHost() {
        if (toastHost != null)
            return toastHost;
        if (fileTable == null)
            return null;
        var scene = fileTable.getScene();
        if (scene == null)
            return null;

        // If root already a StackPane, reuse it, otherwise wrap once
        if (scene.getRoot() instanceof StackPane sp) {
            toastHost = sp;
            return toastHost;
        }
        StackPane wrapper = new StackPane();
        wrapper.getChildren().add(scene.getRoot());
        scene.setRoot(wrapper);
        toastHost = wrapper;
        return toastHost;
    }

    // ---------- Drag & Drop Reordering ----------

    private void enableDragReorderForFileTable() {
        if (fileTable == null)
            return;

        fileTable.setRowFactory(tv -> {
            TableRow<FileItem> row = new TableRow<>();

            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    Integer index = row.getIndex();
                    javafx.scene.input.Dragboard db = row.startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
                    javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                    cc.putString(index.toString());
                    db.setContent(cc);
                    event.consume();
                }
            });

            row.setOnDragOver(event -> {
                javafx.scene.input.Dragboard db = event.getDragboard();
                if (db.hasString()) {
                    event.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
                    event.consume();
                }
            });

            row.setOnDragDropped(event -> {
                javafx.scene.input.Dragboard db = event.getDragboard();
                if (db.hasString()) {
                    int draggedIndex = Integer.parseInt(db.getString());
                    FileItem draggedItem = fileTable.getItems().remove(draggedIndex);

                    int dropIndex;
                    if (row.isEmpty()) {
                        dropIndex = fileTable.getItems().size();
                    } else {
                        dropIndex = row.getIndex();
                    }
                    fileTable.getItems().add(dropIndex, draggedItem);
                    event.setDropCompleted(true);
                    fileTable.getSelectionModel().clearAndSelect(dropIndex);

                    // Optionally sync mergeOrderList if present
                    syncMergeOrderListFromFileTable();

                    event.consume();
                }
            });

            return row;
        });
    }

    private void enableDragReorderForMergeList() {
        if (mergeOrderList == null)
            return;

        mergeOrderList.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };

            cell.setOnDragDetected(event -> {
                if (!cell.isEmpty()) {
                    int index = cell.getIndex();
                    javafx.scene.input.Dragboard db = cell.startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
                    javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                    cc.putString(Integer.toString(index));
                    db.setContent(cc);
                    event.consume();
                }
            });

            cell.setOnDragOver(event -> {
                javafx.scene.input.Dragboard db = event.getDragboard();
                if (db.hasString()) {
                    event.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
                    event.consume();
                }
            });

            cell.setOnDragDropped(event -> {
                javafx.scene.input.Dragboard db = event.getDragboard();
                if (db.hasString()) {
                    int draggedIndex = Integer.parseInt(db.getString());
                    String draggedItem = mergeOrderList.getItems().remove(draggedIndex);

                    int dropIndex;
                    if (cell.isEmpty()) {
                        dropIndex = mergeOrderList.getItems().size();
                    } else {
                        dropIndex = cell.getIndex();
                    }
                    mergeOrderList.getItems().add(dropIndex, draggedItem);
                    event.setDropCompleted(true);
                    mergeOrderList.getSelectionModel().clearAndSelect(dropIndex);
                    event.consume();
                }
            });

            return cell;
        });

        // Initialize merge list from file table once
        syncMergeOrderListFromFileTable();
    }

    private void syncMergeOrderListFromFileTable() {
        if (mergeOrderList == null)
            return;
        if (fileItems == null)
            return;
        var names = fileItems.stream().map(FileItem::getFileName).toList();
        mergeOrderList.setItems(FXCollections.observableArrayList(names));
    }
}
