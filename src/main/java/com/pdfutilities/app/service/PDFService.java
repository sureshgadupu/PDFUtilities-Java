package com.pdfutilities.app.service;

import java.io.File;
import java.util.List;

/**
 * Base interface for all PDF service operations
 */
public interface PDFService {
    
    /**
     * Execute the PDF service operation
     * @param inputFiles list of input files
     * @param outputDirectory output directory for results
     * @return true if operation was successful, false otherwise
     */
    boolean execute(List<File> inputFiles, String outputDirectory);
    
    /**
     * Get the name of the service
     * @return service name
     */
    String getServiceName();
    
    /**
     * Get the description of the service
     * @return service description
     */
    String getDescription();
}
