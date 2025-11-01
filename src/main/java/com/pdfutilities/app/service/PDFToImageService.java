package com.pdfutilities.app.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Service for converting PDF pages to images (PNG/JPG) with configurable DPI
 * and color mode.
 */
public class PDFToImageService extends BasePDFService {

    public enum ImageFormat {
        PNG("png"),
        JPG("jpg");

        private final String ext;

        ImageFormat(String ext) {
            this.ext = ext;
        }

        public String getExt() {
            return ext;
        }
    }

    public enum ColorMode {
        COLOR(ImageType.RGB),
        GRAYSCALE(ImageType.GRAY);

        private final ImageType imageType;

        ColorMode(ImageType imageType) {
            this.imageType = imageType;
        }

        public ImageType getImageType() {
            return imageType;
        }
    }

    private ImageFormat imageFormat = ImageFormat.PNG;
    private int dpi = 150;
    private ColorMode colorMode = ColorMode.COLOR;
    private boolean eachPageToSingleImage = true; // true: each page to separate image; false: entire PDF to one long
                                                  // image

    public PDFToImageService() {
        super("Convert to Image", "Convert PDF pages into image files");
    }

    public PDFToImageService(ImageFormat format, int dpi, ColorMode colorMode, boolean eachPageToSingleImage) {
        this();
        this.imageFormat = format;
        this.dpi = dpi;
        this.colorMode = colorMode;
        this.eachPageToSingleImage = eachPageToSingleImage;
    }

    public void setImageFormat(ImageFormat format) {
        this.imageFormat = format;
    }

    public void setDpi(int dpi) {
        this.dpi = dpi;
    }

    public void setColorMode(ColorMode colorMode) {
        this.colorMode = colorMode;
    }

    public void setEachPageToSingleImage(boolean eachPageToSingleImage) {
        this.eachPageToSingleImage = eachPageToSingleImage;
    }

    @Override
    public boolean execute(List<File> inputFiles, String outputDirectory) {
        if (!validateInputFiles(inputFiles) || !createOutputDirectory(outputDirectory)) {
            return false;
        }

        boolean allSuccessful = true;
        for (File pdf : inputFiles) {
            try {
                if (eachPageToSingleImage) {
                    convertEachPage(pdf, outputDirectory);
                } else {
                    convertEntirePdfToSingleImage(pdf, outputDirectory);
                }
            } catch (Exception e) {
                System.err.println("Error converting " + pdf.getName() + " to images: " + e.getMessage());
                allSuccessful = false;
            }
        }
        return allSuccessful;
    }

    /**
     * Render each page to an individual image file.
     */
    private void convertEachPage(File pdfFile, String outputDirectory) throws IOException {
        String password = getPassword(pdfFile);

        // Check if file is encrypted but no password provided
        if (PdfSecurityUtils.isPasswordProtected(pdfFile) && (password == null || password.trim().isEmpty())) {
            System.err.println("Skipping encrypted file " + pdfFile.getName() + " - no password provided");
            throw new IOException("Cannot convert encrypted file without password: " + pdfFile.getName());
        }

        PDDocument document;
        if (password != null && !password.trim().isEmpty()) {
            document = Loader.loadPDF(pdfFile, password);
            // Remove encryption dictionary for image conversion
            document.setAllSecurityToBeRemoved(true);
        } else {
            document = Loader.loadPDF(pdfFile);
        }
        try {
            PDFRenderer renderer = new PDFRenderer(document);
            String base = stripPdfExt(pdfFile.getName());

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, dpi, colorMode.getImageType());
                String outName = String.format("%s_page_%d.%s", base, page + 1, imageFormat.getExt());
                File outFile = new File(outputDirectory, outName);
                writeImage(image, outFile);
                System.out.println("Saved image: " + outFile.getName());
            }
        } finally {
            document.close();
        }
    }

    /**
     * Render the entire PDF into one vertically concatenated image.
     * Note: This can be memory intensive for large documents; keep DPI reasonable.
     */
    private void convertEntirePdfToSingleImage(File pdfFile, String outputDirectory) throws IOException {
        String password = getPassword(pdfFile);

        // Check if file is encrypted but no password provided
        if (PdfSecurityUtils.isPasswordProtected(pdfFile) && (password == null || password.trim().isEmpty())) {
            System.err.println("Skipping encrypted file " + pdfFile.getName() + " - no password provided");
            throw new IOException("Cannot convert encrypted file without password: " + pdfFile.getName());
        }

        PDDocument document;
        if (password != null && !password.trim().isEmpty()) {
            document = Loader.loadPDF(pdfFile, password);
            // Remove encryption dictionary for image conversion
            document.setAllSecurityToBeRemoved(true);
        } else {
            document = Loader.loadPDF(pdfFile);
        }
        try {
            PDFRenderer renderer = new PDFRenderer(document);

            BufferedImage[] pages = new BufferedImage[document.getNumberOfPages()];
            int totalHeight = 0;
            int maxWidth = 0;

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage img = renderer.renderImageWithDPI(page, dpi, colorMode.getImageType());
                pages[page] = img;
                totalHeight += img.getHeight();
                maxWidth = Math.max(maxWidth, img.getWidth());
            }

            // Compose into one long image
            BufferedImage combined = new BufferedImage(maxWidth, totalHeight, determineBufferedImageType());
            java.awt.Graphics2D g = combined.createGraphics();
            g.setBackground(new java.awt.Color(255, 255, 255));
            g.clearRect(0, 0, maxWidth, totalHeight);

            int y = 0;
            for (BufferedImage img : pages) {
                g.drawImage(img, 0, y, null);
                y += img.getHeight();
            }
            g.dispose();

            String base = stripPdfExt(pdfFile.getName());
            File outFile = new File(outputDirectory, base + "_all." + imageFormat.getExt());
            writeImage(combined, outFile);
            System.out.println("Saved combined image: " + outFile.getName());
        } finally {
            document.close();
        }
    }

    private int determineBufferedImageType() {
        switch (colorMode) {
            case GRAYSCALE:
                return BufferedImage.TYPE_BYTE_GRAY;
            case COLOR:
            default:
                // For JPG, use TYPE_INT_RGB to avoid alpha channel; for PNG either is fine
                return BufferedImage.TYPE_INT_RGB;
        }
    }

    private void writeImage(BufferedImage image, File outFile) throws IOException {
        // For JPG ensure no alpha channel (already ensured by TYPE_INT_RGB)
        boolean ok = ImageIO.write(image, imageFormat.getExt(), outFile);
        if (!ok) {
            throw new IOException("No ImageIO writer found for format: " + imageFormat.getExt());
        }
    }

    private String stripPdfExt(String name) {
        int idx = name.toLowerCase().lastIndexOf(".pdf");
        if (idx >= 0)
            return name.substring(0, idx);
        return name;
    }
}
