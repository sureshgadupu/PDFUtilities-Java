package com.pdfutilities.app.model;

/**
 * Model class representing a saved password entry
 * Used for storing frequently used passwords with descriptive names
 */
public class SavedPassword {
    
    private String name;
    private String password;
    
    /**
     * Constructor for SavedPassword
     * 
     * @param name the descriptive name for the password
     * @param password the actual password value
     */
    public SavedPassword(String name, String password) {
        this.name = name;
        this.password = password;
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
