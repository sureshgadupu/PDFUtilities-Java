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
     * Get password value by name and mark as used
     * 
     * @param name the name of the password
     * @return the password value if found, null otherwise
     */
    public String getPasswordByName(String name) {
        SavedPassword savedPassword = findPasswordByName(name);
        if (savedPassword != null) {
            savedPassword.markAsUsed();
            savePasswords(); // Save to update lastUsed timestamp
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
                        LocalDateTime createdAt = data.createdAt != null
                                ? LocalDateTime.parse(data.createdAt, DATE_FORMATTER)
                                : LocalDateTime.now();
                        LocalDateTime lastUsed = data.lastUsed != null
                                ? LocalDateTime.parse(data.lastUsed, DATE_FORMATTER)
                                : null;

                        SavedPassword password = new SavedPassword(normalizeName(data.name), data.password, createdAt,
                                lastUsed);
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
                data.createdAt = password.getCreatedAt().format(DATE_FORMATTER);
                data.lastUsed = password.getLastUsed() != null ? password.getLastUsed().format(DATE_FORMATTER) : null;
                passwordDataList.add(data);
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(passwordsFilePath))) {
                oos.writeObject(passwordDataList);
            }
        } catch (Exception e) {
            System.err.println("Error saving passwords: " + e.getMessage());
        }
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Internal data class for serialization
     */
    private static class SavedPasswordData implements Serializable {
        private static final long serialVersionUID = 1L;
        String name;
        String password;
        String createdAt;
        String lastUsed;
    }
}
