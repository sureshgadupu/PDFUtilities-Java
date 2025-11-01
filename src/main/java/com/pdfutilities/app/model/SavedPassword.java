package com.pdfutilities.app.model;

import java.time.LocalDateTime;

/**
 * Model class representing a saved password entry
 * Used for storing frequently used passwords with descriptive names
 */
public class SavedPassword {
    
    private String name;
    private String password;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsed;
    
    /**
     * Constructor for SavedPassword
     * 
     * @param name the descriptive name for the password
     * @param password the actual password value
     */
    public SavedPassword(String name, String password) {
        this.name = name;
        this.password = password;
        this.createdAt = LocalDateTime.now();
        this.lastUsed = null;
    }
    
    /**
     * Constructor for SavedPassword with timestamps
     * 
     * @param name the descriptive name for the password
     * @param password the actual password value
     * @param createdAt when this password was created
     * @param lastUsed when this password was last used
     */
    public SavedPassword(String name, String password, LocalDateTime createdAt, LocalDateTime lastUsed) {
        this.name = name;
        this.password = password;
        this.createdAt = createdAt;
        this.lastUsed = lastUsed;
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastUsed() {
        return lastUsed;
    }
    
    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }
    
    /**
     * Mark this password as used by updating the lastUsed timestamp
     */
    public void markAsUsed() {
        this.lastUsed = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SavedPassword that = (SavedPassword) obj;
        return name != null ? name.equals(that.name) : that.name == null;
    }
    
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
