package com.pdfutilities.app.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Service for splitting PDF files into multiple smaller PDF files
 */
public class PDFSplitService extends BasePDFService {

    public enum SplitMode {
        EVERY_PAGE("Every Page"),
        CUSTOM_RANGE("Custom Range"),
        SIZE_BASED("Size-Based");

        private final String displayName;

        SplitMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private SplitMode splitMode = SplitMode.EVERY_PAGE;
    private String customRange = "";
    private long maxSizeInBytes = 0;

    public PDFSplitService() {
        super("Split PDF", "Split PDF files into multiple documents");
    }

    public PDFSplitService(SplitMode splitMode) {
        this();
        this.splitMode = splitMode;
    }

    public void setSplitMode(SplitMode splitMode) {
        this.splitMode = splitMode;
    }

    public void setCustomRange(String customRange) {
        this.customRange = customRange;
    }

    public void setMaxSizeInBytes(long maxSizeInBytes) {
        this.maxSizeInBytes = maxSizeInBytes;
    }

    @Override
    public boolean execute(List<File> inputFiles, String outputDirectory) {
        if (!validateInputFiles(inputFiles) || inputFiles == null || inputFiles.isEmpty()
                || !createOutputDirectory(outputDirectory)) {
            return false;
        }

        boolean allSuccessful = true;

        for (File pdfFile : inputFiles) {
            try {
                splitPdf(pdfFile, outputDirectory);
            } catch (Exception e) {
                System.err.println("Error splitting " + pdfFile.getName() + ": " + e.getMessage());
                allSuccessful = false;
            }
        }

        return allSuccessful;
    }

    /**
     * Split a single PDF file
     * 
     * @param pdfFile         the PDF file to split
     * @param outputDirectory the output directory
     * @throws IOException if an I/O error occurs
     */
    private void splitPdf(File pdfFile, String outputDirectory) throws IOException {
        String password = getPassword(pdfFile);

        // Check if file is encrypted but no password provided
        if (PdfSecurityUtils.isPasswordProtected(pdfFile) && (password == null || password.trim().isEmpty())) {
            System.err.println("Skipping encrypted file " + pdfFile.getName() + " - no password provided");
            throw new IOException("Cannot split encrypted file without password: " + pdfFile.getName());
        }

        PDDocument document;
        if (password != null && !password.trim().isEmpty()) {
            document = Loader.loadPDF(pdfFile, password);
            // Remove encryption dictionary for splitting
            document.setAllSecurityToBeRemoved(true);
        } else {
            document = Loader.loadPDF(pdfFile);
        }
        try {

            switch (splitMode) {
                case EVERY_PAGE:
                    splitEveryPage(document, pdfFile, outputDirectory);
                    break;
                case CUSTOM_RANGE:
                    splitCustomRange(document, pdfFile, outputDirectory);
                    break;
                case SIZE_BASED:
                    splitSizeBased(document, pdfFile, outputDirectory);
                    break;
                default:
                    splitEveryPage(document, pdfFile, outputDirectory);
                    break;
            }

        } finally {
            document.close();
        }
    }

    /**
     * Split PDF into one file per page
     * 
     * @param document        the PDF document
     * @param originalFile    the original file
     * @param outputDirectory the output directory
     * @throws IOException if an I/O error occurs
     */
    private void splitEveryPage(PDDocument document, File originalFile, String outputDirectory) throws IOException {
        Splitter splitter = new Splitter();
        List<PDDocument> splitDocuments = splitter.split(document);

        String originalName = originalFile.getName();
        String baseName = originalName.endsWith(".pdf") ? originalName.substring(0, originalName.length() - 4)
                : originalName;

        for (int i = 0; i < splitDocuments.size(); i++) {
            PDDocument splitDoc = splitDocuments.get(i);
            // Name files as filename-{number}.pdf in order of split (1-based)
            String outputFileName = baseName + "-" + (i + 1) + ".pdf";
            File outputFile = new File(outputDirectory, outputFileName);
            splitDoc.save(outputFile);
            splitDoc.close();
            System.out.println("Created " + outputFile.getName());
        }

        System.out.println("Split " + originalFile.getName() + " into " + splitDocuments.size() + " pages");
    }

    /**
     * Split PDF by custom page ranges
     * 
     * @param document        the PDF document
     * @param originalFile    the original file
     * @param outputDirectory the output directory
     * @throws IOException if an I/O error occurs
     */
    private void splitCustomRange(PDDocument document, File originalFile, String outputDirectory) throws IOException {
        // Expected format: "1-3,5,7-8" (1-based inclusive). Invalids ignored.
        // One output per segment, pages in each segment preserved order.
        String originalName = originalFile.getName();
        String baseName = originalName.endsWith(".pdf") ? originalName.substring(0, originalName.length() - 4)
                : originalName;

        int totalPages = document.getNumberOfPages();
        if (customRange == null || customRange.trim().isEmpty()) {
            // nothing specified, fallback to every page
            splitEveryPage(document, originalFile, outputDirectory);
            return;
        }

        String[] segments = customRange.split(",");
        int fileIndex = 1;
        for (String seg : segments) {
            String s = seg.trim();
            if (s.isEmpty())
                continue;

            int start, end;
            if (s.contains("-")) {
                String[] parts = s.split("-");
                if (parts.length != 2)
                    continue;
                try {
                    start = Integer.parseInt(parts[0].trim());
                    end = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    continue; // ignore invalid
                }
            } else {
                try {
                    start = Integer.parseInt(s);
                    end = start;
                } catch (NumberFormatException e) {
                    continue; // ignore invalid
                }
            }

            // Normalize bounds: 1..totalPages, start <= end
            if (start < 1)
                start = 1;
            if (end > totalPages)
                end = totalPages;
            if (start > end)
                continue;

            // Create a new document for this segment
            try (PDDocument out = new PDDocument()) {
                for (int p = start; p <= end; p++) {
                    out.addPage(document.getPage(p - 1)); // PDFBox pages are 0-based
                }
                String outputFileName = baseName + "-" + (fileIndex++) + ".pdf";
                File outputFile = new File(outputDirectory, outputFileName);
                out.save(outputFile);
                System.out.println("Created " + outputFile.getName() + " for range " + start + "-" + end);
            }
        }
    }

    /**
     * Split PDF by size
     * 
     * @param document        the PDF document
     * @param originalFile    the original file
     * @param outputDirectory the output directory
     * @throws IOException if an I/O error occurs
     */
    private void splitSizeBased(PDDocument document, File originalFile, String outputDirectory) throws IOException {
        // Sequential chunks preserving order, each chunk approx <= maxSizeInBytes.
        // If a single page is larger than the limit, allow that chunk to exceed the
        // limit.
        String originalName = originalFile.getName();
        String baseName = originalName.endsWith(".pdf") ? originalName.substring(0, originalName.length() - 4)
                : originalName;

        int totalPages = document.getNumberOfPages();
        if (totalPages == 0)
            return;

        // If no limit provided or <=0, fallback to every page
        if (maxSizeInBytes <= 0) {
            splitEveryPage(document, originalFile, outputDirectory);
            return;
        }

        int fileIndex = 1;
        int currentStart = 0; // 0-based inclusive
        while (currentStart < totalPages) {
            // Start with at least one page in the chunk
            int currentEnd = currentStart; // 0-based inclusive
            long estimatedSize = 0L;

            // Greedily try to add pages while size stays under threshold
            while (currentEnd < totalPages) {
                long sizeIfAdded;
                // Estimate size by actually saving to a temp in-memory buffer for accuracy
                try (PDDocument temp = new PDDocument()) {
                    for (int p = currentStart; p <= currentEnd; p++) {
                        temp.addPage(document.getPage(p));
                    }
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    temp.save(baos);
                    sizeIfAdded = baos.size();
                }

                if (sizeIfAdded <= maxSizeInBytes || currentEnd == currentStart) {
                    // Accept this end; keep trying to expand
                    estimatedSize = sizeIfAdded;
                    currentEnd++;
                } else {
                    // Exceeded size and we already have at least one page -> stop expansion
                    break;
                }
            }

            // currentEnd was incremented past last accepted page; adjust to last included
            int lastIncluded = Math.min(currentEnd - 1, totalPages - 1);
            if (lastIncluded < currentStart) {
                // Safety (shouldn't happen due to single-page acceptance)
                lastIncluded = currentStart;
            }

            // Save the chunk [currentStart, lastIncluded]
            try (PDDocument out = new PDDocument()) {
                for (int p = currentStart; p <= lastIncluded; p++) {
                    out.addPage(document.getPage(p));
                }
                String outputFileName = baseName + "-" + (fileIndex++) + ".pdf";
                File outputFile = new File(outputDirectory, outputFileName);
                out.save(outputFile);
                System.out.println("Created " + outputFile.getName() + " pages " + (currentStart + 1) + "-"
                        + (lastIncluded + 1) + " size~" + estimatedSize + "B");
            }

            currentStart = lastIncluded + 1;
        }
    }
}
