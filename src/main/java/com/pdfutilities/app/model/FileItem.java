package com.pdfutilities.app.model;

import java.io.File;

/**
 * Model class representing a file item in the PDF Utilities application
 * Used to display file information in the table view
 */
public class FileItem {

    private File file;
    private String fileName;
    private String fileSize;
    private String status;
    private String password;
    private boolean revealPassword;
    private boolean encrypted;

    /**
     * Constructor for FileItem
     * 
     * @param file the File object
     */
    public FileItem(File file) {
        this.file = file;
        this.fileName = file.getName();
        this.fileSize = formatFileSize(file.length());
        this.status = "Ready";
        this.password = "";
        this.revealPassword = false;
        this.encrypted = false;
    }

    /**
     * Format file size in human-readable format
     * 
     * @param sizeInBytes the file size in bytes
     * @return formatted file size string
     */
    private String formatFileSize(long sizeInBytes) {
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

    // Getters and setters
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isRevealPassword() {
        return revealPassword;
    }

    public void setRevealPassword(boolean revealPassword) {
        this.revealPassword = revealPassword;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    @Override
    public String toString() {
        return "FileItem{" +
                "fileName='" + fileName + '\'' +
                ", fileSize='" + fileSize + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
