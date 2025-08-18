package com.pdfutilities.app.service;

import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Service for merging multiple PDF files into a single PDF document
 */
public class PDFMergeService extends BasePDFService {
    
    public PDFMergeService() {
        super("Merge PDFs", "Merge multiple PDF files into one document");
    }
    
    @Override
    public boolean execute(List<File> inputFiles, String outputDirectory) {
        if (!validateInputFiles(inputFiles) || inputFiles.size() < 2 || !createOutputDirectory(outputDirectory)) {
            return false;
        }
        
        try {
            mergeWithPDFMergerUtility(inputFiles, outputDirectory);
            return true;
        } catch (Exception e) {
            System.err.println("Error merging PDFs: " + e.getMessage());
            return false;
        }
    }

    /**
     * Merge using PDFMergerUtility to safely copy pages and resources.
     * Avoids COSStream lifecycle issues seen with manual PDPage reuse.
     */
    private void mergeWithPDFMergerUtility(List<File> inputFiles, String outputDirectory) throws IOException {
        String outputFileName = "merged_" + System.currentTimeMillis() + ".pdf";
        File outputFile = new File(outputDirectory, outputFileName);

        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationFileName(outputFile.getAbsolutePath());

        for (File file : inputFiles) {
            merger.addSource(file);
        }
        merger.mergeDocuments(null); // Use default memory settings

        System.out.println("Merged " + inputFiles.size() + " PDF files into " + outputFile.getName());
    }
}
