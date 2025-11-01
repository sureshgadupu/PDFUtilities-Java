package com.pdfutilities.app.service;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

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

        // Filter out encrypted files without passwords before processing
        List<File> processableFiles = new ArrayList<>();
        for (File file : inputFiles) {
            String password = getPassword(file);
            if (PdfSecurityUtils.isPasswordProtected(file) && (password == null || password.trim().isEmpty())) {
                System.err.println("Skipping encrypted file " + file.getName() + " - no password provided");
            } else {
                processableFiles.add(file);
            }
        }

        // Check if we still have enough files to merge
        if (processableFiles.size() < 2) {
            System.err.println(
                    "Not enough processable files for merge (need at least 2, got " + processableFiles.size() + ")");
            return false;
        }

        try {
            mergeWithPDFMergerUtility(processableFiles, outputDirectory);
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

        // Load each PDF with password if available and add to merger
        for (File file : inputFiles) {
            String password = getPassword(file);

            if (password != null && !password.trim().isEmpty()) {
                // For password-protected files, we need to load them first and save to temp
                // file
                PDDocument doc = Loader.loadPDF(file, password);
                try {
                    // Remove encryption dictionary before saving
                    doc.setAllSecurityToBeRemoved(true);

                    // Create a temporary file for the decrypted PDF
                    File tempFile = File.createTempFile("temp_merge_", ".pdf");
                    tempFile.deleteOnExit();
                    doc.save(tempFile);
                    merger.addSource(tempFile);
                } finally {
                    doc.close();
                }
            } else {
                // For non-password protected files, use the simpler approach
                merger.addSource(file);
            }
        }
        merger.mergeDocuments(null); // Use default memory settings

        System.out.println("Merged " + inputFiles.size() + " PDF files into " + outputFile.getName());
    }
}
