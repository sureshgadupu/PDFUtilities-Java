package com.pdfutilities.app.service;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Abstract base class for PDF service implementations
 * Provides common functionality and utility methods
 */
public abstract class BasePDFService implements PDFService {

    protected String serviceName;
    protected String description;
    protected Map<File, String> filePasswords = new HashMap<>();

    /**
     * Constructor
     * 
     * @param serviceName name of the service
     * @param description description of the service
     */
    public BasePDFService(String serviceName, String description) {
        this.serviceName = serviceName;
        this.description = description;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Validate input files
     * 
     * @param inputFiles list of input files
     * @return true if files are valid, false otherwise
     */
    protected boolean validateInputFiles(List<File> inputFiles) {
        if (inputFiles == null || inputFiles.isEmpty()) {
            return false;
        }

        for (File file : inputFiles) {
            if (file == null || !file.exists() || !file.isFile()) {
                return false;
            }
            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validate output directory
     * 
     * @param outputDirectory output directory path
     * @return true if directory is valid, false otherwise
     */
    protected boolean validateOutputDirectory(String outputDirectory) {
        if (outputDirectory == null || outputDirectory.trim().isEmpty()) {
            return false;
        }

        File dir = new File(outputDirectory);
        return dir.exists() && dir.isDirectory();
    }

    /**
     * Create output directory if it doesn't exist
     * 
     * @param outputDirectory output directory path
     * @return true if directory was created or already exists, false on error
     */
    protected boolean createOutputDirectory(String outputDirectory) {
        if (outputDirectory == null || outputDirectory.trim().isEmpty()) {
            return false;
        }

        File dir = new File(outputDirectory);
        if (dir.exists()) {
            return dir.isDirectory();
        }

        return dir.mkdirs();
    }

    /**
     * Get file size in human-readable format
     * 
     * @param file the file
     * @return formatted file size string
     */
    protected String getFileSize(File file) {
        long sizeInBytes = file.length();
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < 1024 * 1024) {
            return String.format("%.1f KB", sizeInBytes / 1024.0);
        } else if (sizeInBytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", sizeInBytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", sizeInBytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Set passwords for files
     * 
     * @param passwords map of file to password
     */
    public void setFilePasswords(Map<File, String> passwords) {
        this.filePasswords = passwords != null ? new HashMap<>(passwords) : new HashMap<>();
    }

    /**
     * Get password for a specific file
     * 
     * @param file the file
     * @return password if available, null otherwise
     */
    protected String getPassword(File file) {
        return filePasswords.get(file);
    }

    /**
     * Check if a file has a password
     * 
     * @param file the file
     * @return true if password is available
     */
    protected boolean hasPassword(File file) {
        String password = filePasswords.get(file);
        return password != null && !password.trim().isEmpty();
    }
}
