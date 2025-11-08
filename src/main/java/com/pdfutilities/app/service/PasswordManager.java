package com.pdfutilities.app.service;

import com.pdfutilities.app.model.SavedPassword;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Service class for managing saved passwords
 * Handles persistence, retrieval, and management of frequently used passwords
 */
public class PasswordManager {

    private static final String PASSWORDS_FILE = "saved_passwords.dat";
    private static final String APP_DATA_DIR = System.getProperty("user.home") + File.separator + ".pdfutilities";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ObservableList<SavedPassword> savedPasswords;
    private Path passwordsFilePath;

    /**
     * Constructor for PasswordManager
     */
    public PasswordManager() {
        this.savedPasswords = FXCollections.observableArrayList();
        this.passwordsFilePath = Paths.get(APP_DATA_DIR, PASSWORDS_FILE);
        loadPasswords();
    }

    /**
     * Get the observable list of saved passwords
     * 
     * @return ObservableList of SavedPassword objects
     */
    public ObservableList<SavedPassword> getSavedPasswords() {
        return savedPasswords;
    }

    /**
     * Add a new saved password
     * 
     * @param name     the descriptive name for the password
     * @param password the actual password value
     * @return true if added successfully, false if name already exists
     */
    public boolean addPassword(String name, String password) {
        if (name == null || name.trim().isEmpty() || password == null) {
            return false;
        }
        String normalizedName = normalizeName(name);

        // Check if name already exists (case-insensitive)
        for (SavedPassword savedPassword : savedPasswords) {
            if (savedPassword.getName().equalsIgnoreCase(normalizedName)) {
                return false;
            }
        }

        SavedPassword newPassword = new SavedPassword(normalizedName, password);
        savedPasswords.add(newPassword);
        savePasswords();
        return true;
    }

    /**
     * Update an existing saved password
     * 
     * @param oldName     the current name of the password
     * @param newName     the new name for the password
     * @param newPassword the new password value
     * @return true if updated successfully, false if old name not found or new name
     *         already exists
     */
    public boolean updatePassword(String oldName, String newName, String newPassword) {
        if (oldName == null || newName == null || newName.trim().isEmpty() || newPassword == null) {
            return false;
        }

        SavedPassword existingPassword = findPasswordByName(oldName);
        if (existingPassword == null) {
            return false;
        }

        String normalizedNewName = normalizeName(newName);
        // Check if new name already exists (case-insensitive) and not the same entry
        for (SavedPassword savedPassword : savedPasswords) {
            if (savedPassword != existingPassword && savedPassword.getName().equalsIgnoreCase(normalizedNewName)) {
                return false;
            }
        }

        existingPassword.setName(normalizedNewName);
        existingPassword.setPassword(newPassword);
        savePasswords();
        return true;
    }

    /**
     * Delete a saved password
     * 
     * @param name the name of the password to delete
     * @return true if deleted successfully, false if not found
     */
    public boolean deletePassword(String name) {
        if (name == null) {
            return false;
        }

        SavedPassword passwordToDelete = findPasswordByName(name);
        if (passwordToDelete != null) {
            savedPasswords.remove(passwordToDelete);
            savePasswords();
            return true;
        }
        return false;
    }

    /**
     * Find a saved password by name
     * 
     * @param name the name to search for
     * @return SavedPassword object if found, null otherwise
     */
    public SavedPassword findPasswordByName(String name) {
        if (name == null) {
            return null;
        }

        for (SavedPassword savedPassword : savedPasswords) {
            if (savedPassword.getName().equalsIgnoreCase(name.trim())) {
                return savedPassword;
            }
        }
        return null;
    }

    /**
     * Get password value by name
     * 
     * @param name the name of the password
     * @return the password value if found, null otherwise
     */
    public String getPasswordByName(String name) {
        SavedPassword savedPassword = findPasswordByName(name);
        if (savedPassword != null) {
            return savedPassword.getPassword();
        }
        return null;
    }

    /**
     * Load passwords from persistent storage
     */
    private void loadPasswords() {
        try {
            // Create app data directory if it doesn't exist
            Files.createDirectories(passwordsFilePath.getParent());

            if (Files.exists(passwordsFilePath)) {
                try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(passwordsFilePath))) {
                    @SuppressWarnings("unchecked")
                    List<SavedPasswordData> passwordDataList = (List<SavedPasswordData>) ois.readObject();

                    savedPasswords.clear();
                    for (SavedPasswordData data : passwordDataList) {
                        SavedPassword password = new SavedPassword(normalizeName(data.name), data.password);
                        savedPasswords.add(password);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading saved passwords: " + e.getMessage());
            // Initialize with empty list if loading fails
            savedPasswords.clear();
        }
    }

    /**
     * Save passwords to persistent storage
     */
    private void savePasswords() {
        try {
            // Create app data directory if it doesn't exist
            Files.createDirectories(passwordsFilePath.getParent());

            List<SavedPasswordData> passwordDataList = new ArrayList<>();
            for (SavedPassword password : savedPasswords) {
                SavedPasswordData data = new SavedPasswordData();
                data.name = password.getName();
                data.password = password.getPassword();
                passwordDataList.add(data);
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(passwordsFilePath))) {
                oos.writeObject(passwordDataList);
            }
        } catch (Exception e) {
            System.err.println("Error saving passwords: " + e.getMessage());
        }
    }

    /**
     * Export passwords to a JSON file
     * 
     * @param filePath the path where the JSON file should be saved
     * @return true if export was successful, false otherwise
     */
    public boolean exportPasswords(Path filePath) {
        try {
            // Build password entries
            StringBuilder passwordEntries = new StringBuilder();
            for (int i = 0; i < savedPasswords.size(); i++) {
                SavedPassword password = savedPasswords.get(i);
                passwordEntries.append(String.format("""
                        {
                          "name": "%s",
                          "password": "%s"
                        }""",
                        escapeJson(password.getName()),
                        escapeJson(password.getPassword())));

                if (i < savedPasswords.size() - 1) {
                    passwordEntries.append(",");
                }
                passwordEntries.append("\n");
            }

            // Use text block for JSON structure
            String json = String.format("""
                    {
                      "version": "1.0",
                      "exportDate": "%s",
                      "passwords": [
                    %s
                      ]
                    }
                    """,
                    LocalDateTime.now().format(DATE_FORMATTER),
                    passwordEntries.toString());

            Files.writeString(filePath, json);
            return true;
        } catch (Exception e) {
            System.err.println("Error exporting passwords: " + e.getMessage());
            return false;
        }
    }

    /**
     * Import passwords from a JSON file
     * 
     * @param filePath  the path to the JSON file to import
     * @param mergeMode if true, merge with existing passwords (skip duplicates), if
     *                  false, replace all
     * @return ImportResult containing success status and details
     */
    public ImportResult importPasswords(Path filePath, boolean mergeMode) {
        ImportResult result = new ImportResult();

        try {
            String jsonContent = Files.readString(filePath);

            // Simple JSON parsing (without external library)
            if (!jsonContent.trim().startsWith("{")) {
                result.errorMessage = "Invalid JSON format: File does not start with '{'";
                return result;
            }

            // Extract passwords array
            int passwordsStart = jsonContent.indexOf("\"passwords\":");
            if (passwordsStart == -1) {
                result.errorMessage = "Invalid JSON format: 'passwords' array not found";
                return result;
            }

            int arrayStart = jsonContent.indexOf("[", passwordsStart);
            if (arrayStart == -1) {
                result.errorMessage = "Invalid JSON format: passwords array not found";
                return result;
            }

            // Parse each password entry
            List<SavedPassword> importedPasswords = new ArrayList<>();
            int currentPos = arrayStart + 1;

            while (currentPos < jsonContent.length()) {
                int entryStart = jsonContent.indexOf("{", currentPos);
                if (entryStart == -1)
                    break;

                int entryEnd = jsonContent.indexOf("}", entryStart);
                if (entryEnd == -1)
                    break;

                String entry = jsonContent.substring(entryStart, entryEnd + 1);

                // Extract fields
                String name = extractJsonField(entry, "name");
                String password = extractJsonField(entry, "password");

                if (name == null || password == null) {
                    result.skipped++;
                    currentPos = entryEnd + 1;
                    continue;
                }

                SavedPassword importedPassword = new SavedPassword(normalizeName(name), password);
                importedPasswords.add(importedPassword);

                currentPos = entryEnd + 1;
            }

            // Apply import based on merge mode
            if (!mergeMode) {
                // Replace mode: clear existing and add imported
                savedPasswords.clear();
                savedPasswords.addAll(importedPasswords);
                result.imported = importedPasswords.size();
            } else {
                // Merge mode: add only non-duplicates
                for (SavedPassword imported : importedPasswords) {
                    boolean exists = false;
                    for (SavedPassword existing : savedPasswords) {
                        if (existing.getName().equalsIgnoreCase(imported.getName())) {
                            exists = true;
                            result.skipped++;
                            break;
                        }
                    }
                    if (!exists) {
                        savedPasswords.add(imported);
                        result.imported++;
                    }
                }
            }

            savePasswords();
            result.success = true;
            return result;

        } catch (Exception e) {
            result.errorMessage = "Error importing passwords: " + e.getMessage();
            System.err.println(result.errorMessage);
            return result;
        }
    }

    /**
     * Extract a field value from a JSON object string
     */
    private String extractJsonField(String json, String fieldName) {
        String searchPattern = "\"" + fieldName + "\"";
        int fieldIndex = json.indexOf(searchPattern);
        if (fieldIndex == -1)
            return null;

        int valueStart = json.indexOf(":", fieldIndex) + 1;
        // Skip whitespace
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length())
            return null;

        // Check if value is null
        String remaining = json.substring(valueStart).trim();
        if (remaining.startsWith("null")) {
            return null;
        }

        // Check if value is a string (starts with ")
        if (json.charAt(valueStart) == '"') {
            valueStart++; // Skip opening quote
            // Find closing quote, handling escaped quotes
            StringBuilder value = new StringBuilder();
            for (int i = valueStart; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    // Handle escape sequences
                    char next = json.charAt(i + 1);
                    if (next == '"' || next == '\\' || next == 'n' || next == 'r' || next == 't') {
                        value.append(c).append(next);
                        i++; // Skip next char as it's part of escape sequence
                    } else {
                        value.append(c);
                    }
                } else if (c == '"') {
                    // Found closing quote
                    return unescapeJson(value.toString());
                } else {
                    value.append(c);
                }
            }
            // No closing quote found
            return null;
        }

        return null;
    }

    /**
     * Escape special characters for JSON
     */
    private String escapeJson(String str) {
        if (str == null)
            return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Unescape JSON special characters
     */
    private String unescapeJson(String str) {
        if (str == null)
            return "";
        return str.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Result class for import operations
     */
    public static class ImportResult {
        public boolean success = false;
        public int imported = 0;
        public int skipped = 0;
        public String errorMessage = null;
    }

    /**
     * Internal data class for serialization
     */
    private static class SavedPasswordData implements Serializable {
        private static final long serialVersionUID = 1L;
        String name;
        String password;
    }
}
