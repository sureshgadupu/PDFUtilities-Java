package com.pdfutilities.app.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Service for extracting text and images from PDF files
 */
public class TextExtractionService extends BasePDFService {
    
    private boolean extractText = true;
    private boolean extractImages = false;
    
    public TextExtractionService() {
        super("Extract Text", "Extract text content from PDF files");
    }
    
    public TextExtractionService(boolean extractText, boolean extractImages) {
        this();
        this.extractText = extractText;
        this.extractImages = extractImages;
    }
    
    public void setExtractText(boolean extractText) {
        this.extractText = extractText;
    }
    
    public void setExtractImages(boolean extractImages) {
        this.extractImages = extractImages;
    }
    
    @Override
    public boolean execute(List<File> inputFiles, String outputDirectory) {
        if (!validateInputFiles(inputFiles) || !createOutputDirectory(outputDirectory)) {
            return false;
        }
        
        boolean allSuccessful = true;
        
        for (File pdfFile : inputFiles) {
            try {
                extractContent(pdfFile, outputDirectory);
            } catch (Exception e) {
                System.err.println("Error extracting from " + pdfFile.getName() + ": " + e.getMessage());
                allSuccessful = false;
            }
        }
        
        return allSuccessful;
    }
    
    /**
     * Extract content from a PDF file
     * @param pdfFile the PDF file to extract from
     * @param outputDirectory the output directory
     * @throws IOException if an I/O error occurs
     */
    private void extractContent(File pdfFile, String outputDirectory) throws IOException {
        PDDocument document = null;
        try {
            // Load PDF document (PDFBox 2.0.x)
            document = PDDocument.load(pdfFile);
            
            String baseName = pdfFile.getName().replace(".pdf", "");
            
            // Extract text if requested
            if (extractText) {
                extractText(document, baseName, outputDirectory);
            }
            
            // Extract images if requested
            if (extractImages) {
                extractImages(document, baseName, outputDirectory);
            }
            
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }
    
    /**
     * Extract text from PDF document
     * @param document the PDF document
     * @param baseName base name for output files
     * @param outputDirectory the output directory
     * @throws IOException if an I/O error occurs
     */
    private void extractText(PDDocument document, String baseName, String outputDirectory) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        
        // Save text to file
        String outputFileName = baseName + "_extracted.txt";
        File outputFile = new File(outputDirectory, outputFileName);
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(text);
        }
        
        System.out.println("Extracted text to " + outputFile.getName());
    }
    
    /**
     * Extract images from PDF document
     * @param document the PDF document
     * @param baseName base name for output files
     * @param outputDirectory the output directory
     * @throws IOException if an I/O error occurs
     */
    private void extractImages(PDDocument document, String baseName, String outputDirectory) throws IOException {
        int imageCount = 0;
        
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            PDPage page = document.getPage(i);
            if (page.getResources() != null) {
                for (org.apache.pdfbox.cos.COSName cosName : page.getResources().getXObjectNames()) {
                    org.apache.pdfbox.pdmodel.graphics.PDXObject xobject = page.getResources().getXObject(cosName);
                    if (xobject instanceof PDImageXObject) {
                        PDImageXObject image = (PDImageXObject) xobject;
                        imageCount++;
                        BufferedImage bufferedImage = image.getImage();
                        
                        // Save image to file
                        String imageFileName = baseName + "_image_" + imageCount + ".png";
                        File imageFile = new File(outputDirectory, imageFileName);
                        
                        ImageIO.write(bufferedImage, "PNG", imageFile);
                        System.out.println("Extracted image to " + imageFile.getName());
                    }
                }
            }
        }
        
        if (imageCount == 0) {
            System.out.println("No images found in the PDF");
        } else {
            System.out.println("Extracted " + imageCount + " images");
        }
    }
}
