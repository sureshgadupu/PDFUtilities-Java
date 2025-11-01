package com.pdfutilities.app.service;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for converting PDF files to DOCX format
 */
public class DocxConversionService extends BasePDFService {

    public DocxConversionService() {
        super("Convert to DocX", "Convert PDF files to editable Word documents");
    }

    @Override
    public boolean execute(List<File> inputFiles, String outputDirectory) {
        if (!validateInputFiles(inputFiles) || !createOutputDirectory(outputDirectory)) {
            return false;
        }

        boolean allSuccessful = true;

        for (File pdfFile : inputFiles) {
            try {
                convertPdfToDocx(pdfFile, outputDirectory);
            } catch (Exception e) {
                System.err.println("Error converting " + pdfFile.getName() + ": " + e.getMessage());
                allSuccessful = false;
            }
        }

        return allSuccessful;
    }

    /**
     * Convert a single PDF file to DOCX
     * 
     * @param pdfFile         the PDF file to convert
     * @param outputDirectory the output directory
     * @throws IOException if an I/O error occurs
     */
    private void convertPdfToDocx(File pdfFile, String outputDirectory) throws IOException {
        PDDocument pdf = null;
        XWPFDocument docx = null;
        FileOutputStream fos = null;

        try {
            // Load PDF (PDFBox 3.x API) with password if available
            String password = getPassword(pdfFile);

            // Check if file is encrypted but no password provided
            if (PdfSecurityUtils.isPasswordProtected(pdfFile) && (password == null || password.trim().isEmpty())) {
                System.err.println("Skipping encrypted file " + pdfFile.getName() + " - no password provided");
                throw new IOException("Cannot convert encrypted file without password: " + pdfFile.getName());
            }

            if (password != null && !password.trim().isEmpty()) {
                pdf = Loader.loadPDF(pdfFile, password);
                // Remove encryption dictionary for DOCX conversion
                pdf.setAllSecurityToBeRemoved(true);
            } else {
                pdf = Loader.loadPDF(pdfFile);
            }

            // 1) Extract text with preserved line breaks (more editable-friendly)
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // helps with reading order
            stripper.setLineSeparator("\n"); // ensure line breaks are explicit
            String text = stripper.getText(pdf);

            // 2) Create DOCX with some basic styles
            docx = new XWPFDocument();

            // Title style (filename)
            XWPFParagraph titleP = docx.createParagraph();
            titleP.setStyle("Title");
            XWPFRun titleR = titleP.createRun();
            titleR.setText(pdfFile.getName());
            titleR.setBold(true);
            titleR.setFontSize(16);

            // Body text: preserve line breaks; split by single newline into runs
            // but merge consecutive blank lines into one empty paragraph to avoid too much
            // spacing
            String[] lines = text.split("\\r?\\n");
            List<String> currentPara = new ArrayList<>();
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    // flush current paragraph if we have content
                    if (!currentPara.isEmpty()) {
                        appendParagraph(docx, currentPara);
                        currentPara.clear();
                    } else {
                        // add an empty paragraph (single blank)
                        docx.createParagraph();
                    }
                } else {
                    currentPara.add(line);
                }
            }
            if (!currentPara.isEmpty()) {
                appendParagraph(docx, currentPara);
            }

            // 3) Extract images and append after text with simple captions
            appendExtractedImages(pdf, docx);

            // 4) Save
            String outputFileName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "") + ".docx";
            File out = new File(outputDirectory, outputFileName);
            fos = new FileOutputStream(out);
            docx.write(fos);

            System.out.println("Converted (editable) " + pdfFile.getName() + " -> " + out.getName());
        } finally {
            if (pdf != null)
                try {
                    pdf.close();
                } catch (IOException ignored) {
                }
            if (docx != null)
                try {
                    docx.close();
                } catch (IOException ignored) {
                }
            if (fos != null)
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
        }
    }

    /**
     * Append a paragraph to DOCX from collected lines.
     * Joins lines with soft breaks so it stays editable and close to source flow.
     */
    private void appendParagraph(XWPFDocument docx, List<String> lines) {
        XWPFParagraph p = docx.createParagraph();
        XWPFRun r = p.createRun();
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            if (!l.isEmpty()) {
                r.setText(l);
            }
            if (i < lines.size() - 1) {
                r.addCarriageReturn(); // soft line break inside paragraph
            }
        }
    }

    /**
     * Best-effort image extraction: walks pages/resources and appends images to
     * DOCX after text.
     * Images are inserted inline with a small caption indicating the page.
     */
    private void appendExtractedImages(PDDocument pdf, XWPFDocument docx) throws IOException {
        int pageIndex = 0;
        for (PDPage page : pdf.getPages()) {
            PDResources resources = page.getResources();
            if (resources == null) {
                pageIndex++;
                continue;
            }
            for (COSName name : resources.getXObjectNames()) {
                PDXObject x = resources.getXObject(name);
                if (x instanceof PDImageXObject image) {
                    // Create a paragraph for image
                    XWPFParagraph p = docx.createParagraph();
                    p.setAlignment(ParagraphAlignment.CENTER);
                    XWPFRun r = p.createRun();

                    // Convert PDImageXObject to byte[] (PNG is lossless and widely supported)
                    // PDFBox 2.0.x does not have getImageData(); use getImage() and encode to PNG.
                    java.awt.image.BufferedImage bimg = image.getImage();
                    if (bimg == null) {
                        continue;
                    }
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    javax.imageio.ImageIO.write(bimg, "png", baos);
                    baos.flush();
                    byte[] pngBytes = baos.toByteArray();
                    baos.close();

                    // Insert image; scale to a reasonable width (e.g., 6 inches), keep aspect ratio
                    int imgType = XWPFDocument.PICTURE_TYPE_PNG;
                    int pxWidth = image.getWidth();
                    int pxHeight = image.getHeight();
                    if (pxWidth <= 0 || pxHeight <= 0) {
                        // skip invalid metadata
                        continue;
                    }
                    // target width = 6 inches -> EMUs
                    double targetWidthInch = 6.0;
                    int targetWidth = (int) Units.toEMU(targetWidthInch * 72); // convert inches->points->EMUs later
                                                                               // handled by API
                    // Maintain aspect ratio for height (approximate via EMUs directly with Units)
                    double aspect = (double) pxHeight / (double) pxWidth;
                    int targetHeight = (int) Units.toEMU(targetWidthInch * 72 * aspect);

                    try {
                        r.addPicture(
                                new java.io.ByteArrayInputStream(pngBytes),
                                imgType,
                                "extracted-image.png",
                                Units.toEMU(6.0 * 72), // width in EMUs (approx via 6in * 72dpi to EMUs)
                                Units.toEMU(6.0 * 72 * aspect) // height in EMUs
                        );
                    } catch (InvalidFormatException ife) {
                        // Skip problematic image but continue conversion
                        System.err.println("Skipping one image due to format error: " + ife.getMessage());
                    }

                    // Caption
                    XWPFParagraph cap = docx.createParagraph();
                    cap.setAlignment(ParagraphAlignment.CENTER);
                    XWPFRun capRun = cap.createRun();
                    capRun.setItalic(true);
                    capRun.setText("Image from page " + (pageIndex + 1));
                }
            }
            pageIndex++;
        }
    }
}
