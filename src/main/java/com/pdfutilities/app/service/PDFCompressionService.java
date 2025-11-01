package com.pdfutilities.app.service;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.Loader;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Service for compressing PDF files to reduce file size
 *
 * Approach (works with PDFBox 2.0.x):
 * - Iterate over pages and downscale/re-encode raster images to JPEG at a given
 * quality.
 * - Leave vector graphics/text untouched for quality.
 * - Save incremental copy which may apply object stream compression as per
 * PDFBox defaults.
 */
public class PDFCompressionService extends BasePDFService {

    /**
     * Optional target output size in bytes. When > 0, target-size mode
     * is used instead of the predefined compression level.
     */
    private long targetSizeBytes = 0L;

    /**
     * Iteration controls for target-size mode
     * We tune JPEG quality and image scale to approach the target size.
     */
    private float minJpegQuality = 0.25f;
    private float maxJpegQuality = 0.9f;
    private double minScale = 0.4; // 40% of original
    private double maxScale = 1.0; // 100% (no downscale)
    private int maxIterations = 6;

    public enum CompressionLevel {
        VERY_LOW("Tiny (Very Low Quality)", 0.25f, 0.4), // below current LOW
        LOW("Smallest (Low Quality)", 0.35f, 0.5), // existing LOW
        LOW_MEDIUM("Small (Low-Medium Quality)", 0.5f, 0.65), // between LOW and MEDIUM
        MEDIUM("Balanced (Medium Quality)", 0.6f, 0.75), // existing MEDIUM
        MEDIUM_HIGH("Balanced+ (Medium-High Quality)", 0.7f, 0.9), // between MEDIUM and HIGH
        HIGH("Largest (High Quality)", 0.8f, 1.0); // existing HIGH

        private final String displayName;
        private final float jpegQuality;
        private final double scale;

        CompressionLevel(String displayName, float jpegQuality, double scale) {
            this.displayName = displayName;
            this.jpegQuality = jpegQuality;
            this.scale = scale;
        }

        public String getDisplayName() {
            return displayName;
        }

        public float getJpegQuality() {
            return jpegQuality;
        }

        public double getScale() {
            return scale;
        }

        /**
         * Choose a starting level based on source file size.
         * For KB-sized files: start from LOW and go upwards.
         * For MB-sized files: start from middle and go upwards.
         */
        public static CompressionLevel startForSize(long bytes) {
            if (bytes <= 0)
                return MEDIUM;
            long kb = Math.round(bytes / 1024.0);
            long mb = Math.round(bytes / (1024.0 * 1024.0));
            if (mb >= 1) {
                // MB files -> middle starting point
                return LOW_MEDIUM; // slightly below MEDIUM to allow going upwards
            } else {
                // KB files -> start from VERY_LOW
                return VERY_LOW;
            }
        }

        /**
         * Provide an ordered list from a given starting level upwards to higher
         * quality/larger size.
         * This helps when iterating to try progressively larger outputs.
         */
        public static CompressionLevel[] ascendingFrom(CompressionLevel start) {
            CompressionLevel[] order = new CompressionLevel[] {
                    VERY_LOW, LOW, LOW_MEDIUM, MEDIUM, MEDIUM_HIGH, HIGH
            };
            // find start index
            int idx = 0;
            for (int i = 0; i < order.length; i++) {
                if (order[i] == start) {
                    idx = i;
                    break;
                }
            }
            CompressionLevel[] seq = new CompressionLevel[order.length - idx];
            System.arraycopy(order, idx, seq, 0, seq.length);
            return seq;
        }
    }

    private CompressionLevel compressionLevel = CompressionLevel.MEDIUM;

    public PDFCompressionService() {
        super("Compress PDF", "Compress PDF files to reduce file size");
    }

    public PDFCompressionService(CompressionLevel compressionLevel) {
        this();
        this.compressionLevel = compressionLevel;
    }

    public void setCompressionLevel(CompressionLevel compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    /**
     * Enable or disable target size mode. If bytes > 0, the service will
     * iteratively search compression parameters to meet the target.
     */
    public void setTargetSizeBytes(long bytes) {
        this.targetSizeBytes = Math.max(0L, bytes);
    }

    public long getTargetSizeBytes() {
        return targetSizeBytes;
    }

    private void log(String msg) {
        System.out.println("[CompressTarget] " + msg);
    }

    /**
     * Optional tuning for the iterative search bounds.
     */
    public void setTargetSearchBounds(float minJpegQ, float maxJpegQ, double minScale, double maxScale,
            int maxIterations) {
        this.minJpegQuality = Math.max(0.1f, Math.min(minJpegQ, 0.95f));
        this.maxJpegQuality = Math.max(this.minJpegQuality, Math.min(maxJpegQ, 0.99f));
        this.minScale = Math.max(0.2, Math.min(minScale, 1.0));
        this.maxScale = Math.max(this.minScale, Math.min(maxScale, 1.0));
        this.maxIterations = Math.max(2, maxIterations);
    }

    @Override
    public boolean execute(List<File> inputFiles, String outputDirectory) {
        if (!validateInputFiles(inputFiles) || !createOutputDirectory(outputDirectory)) {
            return false;
        }

        boolean allSuccessful = true;

        for (File pdfFile : inputFiles) {
            try {
                // Check if file is encrypted but no password provided
                String password = getPassword(pdfFile);
                if (PdfSecurityUtils.isPasswordProtected(pdfFile) && (password == null || password.trim().isEmpty())) {
                    System.err.println("Skipping encrypted file " + pdfFile.getName() + " - no password provided");
                    allSuccessful = false; // Mark as failed since we couldn't process this file
                    continue;
                }

                if (targetSizeBytes > 0L) {
                    compressToTargetSize(pdfFile, outputDirectory, targetSizeBytes);
                } else {
                    // Choose starting level based on file size rule if the caller left default
                    CompressionLevel start = compressionLevel;
                    if (start == null)
                        start = CompressionLevel.MEDIUM;
                    // If UI didn't override and we want adaptive start:
                    if (compressionLevel == CompressionLevel.MEDIUM) {
                        start = CompressionLevel.startForSize(pdfFile.length());
                    }
                    // Try from start and go upwards until success (here one pass equals success)
                    // We still write only one output; the first attempt is used.
                    compressPdf(pdfFile, outputDirectory, start);
                }
            } catch (Exception e) {
                System.err.println("Error compressing " + pdfFile.getName() + ": " + e.getMessage());
                allSuccessful = false;
            }
        }

        return allSuccessful;
    }

    private void compressPdf(File pdfFile, String outputDirectory, CompressionLevel level) throws IOException {
        String password = getPassword(pdfFile);

        PDDocument doc;
        if (password != null && !password.trim().isEmpty()) {
            doc = Loader.loadPDF(pdfFile, password);
        } else {
            doc = Loader.loadPDF(pdfFile);
        }
        try (FileInputStream fis = new FileInputStream(pdfFile)) {

            // Iterate all pages and downscale/convert raster images to JPEG
            for (PDPage page : doc.getPages()) {
                PDResources resources = page.getResources();
                if (resources == null)
                    continue;

                for (COSName name : resources.getXObjectNames()) {
                    PDXObject xobj = resources.getXObject(name);
                    if (xobj instanceof PDImageXObject img) {
                        try {
                            BufferedImage bimg = img.getImage();
                            if (bimg == null)
                                continue;

                            // Optional: skip tiny images (icons) to preserve clarity
                            if (bimg.getWidth() < 64 || bimg.getHeight() < 64)
                                continue;

                            // Downscale if required
                            BufferedImage scaled = bimg;
                            if (level.getScale() < 1.0) {
                                int newW = Math.max(1, (int) Math.round(bimg.getWidth() * level.getScale()));
                                int newH = Math.max(1, (int) Math.round(bimg.getHeight() * level.getScale()));
                                java.awt.Image tmp = bimg.getScaledInstance(newW, newH, java.awt.Image.SCALE_SMOOTH);
                                BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
                                java.awt.Graphics2D g2 = resized.createGraphics();
                                g2.drawImage(tmp, 0, 0, null);
                                g2.dispose();
                                scaled = resized;
                            }

                            // Re-encode as JPEG with specified quality
                            PDImageXObject jpegImg = JPEGFactory.createFromImage(doc, scaled, level.getJpegQuality());

                            // Replace in resources
                            resources.put(name, jpegImg);
                        } catch (Throwable t) {
                            // On any image-specific issue, skip and continue to keep process robust
                            System.err.println("Skipping image compression for one object: " + t.getMessage());
                        }
                    }
                }
            }

            // Save to destination
            String outName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "") + "_compressed.pdf";
            File out = new File(outputDirectory, outName);

            // Remove encryption dictionary if the original was encrypted
            if (password != null && !password.trim().isEmpty()) {
                doc.setAllSecurityToBeRemoved(true);
            }

            doc.save(out);
            System.out.println("Compressed: " + pdfFile.getName() + " -> " + out.getName());
        } finally {
            doc.close();
        }
    }

    /**
     * Iteratively try to reach a target size by adjusting JPEG quality and scale.
     * Heuristic approach:
     * - Start from medium settings and binary-search quality
     * - If still larger than target, reduce scale and retry
     */
    private void compressToTargetSize(File pdfFile, String outputDirectory, long targetBytes) throws IOException {
        log("Target-size mode enabled. Target: " + targetBytes + " bytes (" + (targetBytes / 1024) + " KB)");
        log("Search bounds: quality=[" + minJpegQuality + "," + maxJpegQuality + "] scale=[" + minScale + "," + maxScale
                + "], maxIter=" + maxIterations);

        float lowQ = minJpegQuality;
        float highQ = maxJpegQuality;
        double lowScale = minScale;
        double highScale = maxScale;

        float curQ = 0.35f; // seed: LOW
        double curScale = 0.5;
        curQ = Math.max(lowQ, Math.min(curQ, highQ));
        curScale = Math.max(lowScale, Math.min(curScale, highScale));
        log(String.format("Seed params -> quality=%.3f, scale=%.2f", curQ, curScale));

        File bestFile = null;
        long bestDelta = Long.MAX_VALUE;

        for (int iter = 1; iter <= maxIterations; iter++) {
            log(String.format("Iter %d/%d: trying quality=%.3f, scale=%.2f ...", iter, maxIterations, curQ, curScale));
            File candidate = compressWithParams(pdfFile, outputDirectory, curQ, curScale);
            long size = candidate.length();
            long sizeKB = Math.round(size / 1024.0);
            long delta = Math.abs(size - targetBytes);
            long deltaKB = Math.round(delta / 1024.0);
            log(String.format(" -> result size: %d KB (delta=%d KB)", sizeKB, deltaKB));

            if (delta < bestDelta) {
                bestDelta = delta;
                bestFile = candidate;
                log(" -> new best candidate");
            } else {
                deleteQuietly(candidate);
                log(" -> worse than best; discarded");
            }

            long tol = Math.max(10_000, Math.round(targetBytes * 0.08)); // 10KB or 8%
            if (size == targetBytes || delta <= tol) {
                log("Stopping early: within tolerance (" + (tol / 1024) + " KB)");
                break;
            }

            if (size > targetBytes) {
                // Too big -> decrease quality first
                highQ = curQ;
                curQ = (lowQ + highQ) / 2f;
                log(String.format(" size>target -> lower quality: new quality=%.3f (bounds [%.3f, %.3f])", curQ, lowQ,
                        highQ));

                if ((highQ - lowQ) < 0.03f) {
                    highScale = curScale;
                    curScale = (lowScale + highScale) / 2.0;
                    log(String.format(" quality converged -> reduce scale: new scale=%.2f (bounds [%.2f, %.2f])",
                            curScale, lowScale, highScale));
                }
            } else {
                // Too small -> increase quality first
                lowQ = curQ;
                curQ = (lowQ + highQ) / 2f;
                log(String.format(" size<target -> raise quality: new quality=%.3f (bounds [%.3f, %.3f])", curQ, lowQ,
                        highQ));

                if ((highQ - lowQ) < 0.03f) {
                    lowScale = curScale;
                    curScale = Math.min(1.0, (lowScale + highScale) / 2.0);
                    log(String.format(" quality converged -> increase scale: new scale=%.2f (bounds [%.2f, %.2f])",
                            curScale, lowScale, highScale));
                }
            }
        }

        if (bestFile != null) {
            long bestKB = Math.round(bestFile.length() / 1024.0);
            log("Best candidate after search: " + bestKB + " KB (delta="
                    + Math.round(Math.abs(bestFile.length() - targetBytes) / 1024.0) + " KB)");

            // Safety compare with LOW preset
            File lowCandidate = null;
            try {
                long bestDeltaKB = Math.round(bestDelta / 1024.0);
                if (bestDelta > Math.max(10_000, Math.round(targetBytes * 0.10))) {
                    log("Creating LOW preset candidate for comparison...");
                    lowCandidate = compressWithParams(pdfFile, outputDirectory, 0.35f, 0.5);
                    long lowDelta = Math.abs(lowCandidate.length() - targetBytes);
                    long lowKB = Math.round(lowCandidate.length() / 1024.0);
                    log("LOW candidate: " + lowKB + " KB (delta=" + Math.round(lowDelta / 1024.0) + " KB)");
                    if (lowDelta < bestDelta) {
                        deleteQuietly(bestFile);
                        bestFile = lowCandidate;
                        bestDelta = lowDelta;
                        lowCandidate = null;
                        log("LOW candidate selected as better match to target");
                    } else {
                        log("Original best candidate retained");
                    }
                }
            } finally {
                if (lowCandidate != null)
                    deleteQuietly(lowCandidate);
            }

            String outName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "") + "_compressed.pdf";
            File finalOut = new File(outputDirectory, outName);
            deleteQuietly(finalOut);
            if (!bestFile.renameTo(finalOut)) {
                java.nio.file.Files.copy(bestFile.toPath(), finalOut.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                deleteQuietly(bestFile);
            }
            log("Final output: " + finalOut.length() / 1024 + " KB");
        } else {
            log("No valid candidate produced; falling back to LOW preset compression");
            compressPdf(pdfFile, outputDirectory, CompressionLevel.LOW);
        }
    }

    /**
     * Quietly delete a file if it exists (no exception if it fails).
     */
    private void deleteQuietly(File f) {
        if (f == null)
            return;
        try {
            if (f.exists()) {
                if (!f.delete()) {
                    // try NIO as fallback
                    java.nio.file.Files.deleteIfExists(f.toPath());
                }
            }
        } catch (Exception ignore) {
            // swallow
        }
    }

    /**
     * Create a compressed output using explicit JPEG quality and scale.
     * Returns the temporary output File created.
     */
    private File compressWithParams(File pdfFile, String outputDirectory, float jpegQuality, double scale)
            throws IOException {
        File out;
        String password = getPassword(pdfFile);

        // Check if file is encrypted but no password provided
        if (PdfSecurityUtils.isPasswordProtected(pdfFile) && (password == null || password.trim().isEmpty())) {
            System.err.println("Skipping encrypted file " + pdfFile.getName() + " - no password provided");
            throw new IOException("Cannot compress encrypted file without password: " + pdfFile.getName());
        }

        PDDocument doc;
        if (password != null && !password.trim().isEmpty()) {
            doc = Loader.loadPDF(pdfFile, password);
        } else {
            doc = Loader.loadPDF(pdfFile);
        }
        try (FileInputStream fis = new FileInputStream(pdfFile)) {
            int pageIndex = 0;
            for (PDPage page : doc.getPages()) {
                PDResources resources = page.getResources();
                if (resources == null) {
                    pageIndex++;
                    continue;
                }

                int replacedOnPage = 0;
                for (COSName name : resources.getXObjectNames()) {
                    PDXObject xobj = resources.getXObject(name);
                    if (xobj instanceof PDImageXObject img) {
                        try {
                            BufferedImage bimg = img.getImage();
                            if (bimg == null)
                                continue;
                            if (bimg.getWidth() < 64 || bimg.getHeight() < 64)
                                continue;

                            BufferedImage scaled = bimg;
                            if (scale < 1.0) {
                                int newW = Math.max(1, (int) Math.round(bimg.getWidth() * scale));
                                int newH = Math.max(1, (int) Math.round(bimg.getHeight() * scale));
                                java.awt.Image tmp = bimg.getScaledInstance(newW, newH, java.awt.Image.SCALE_SMOOTH);
                                BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
                                java.awt.Graphics2D g2 = resized.createGraphics();
                                g2.drawImage(tmp, 0, 0, null);
                                g2.dispose();
                                scaled = resized;
                            }

                            PDImageXObject jpegImg = JPEGFactory.createFromImage(doc, scaled, jpegQuality);
                            resources.put(name, jpegImg);
                            replacedOnPage++;
                        } catch (Throwable t) {
                            System.err.println(
                                    "[CompressTarget] Page " + pageIndex + " image re-encode skip: " + t.getMessage());
                        }
                    }
                }
                log("Page " + pageIndex + ": replaced " + replacedOnPage + " image(s)");
                pageIndex++;
            }

            String base = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
            out = File.createTempFile(base + "_cand_", ".pdf", new File(outputDirectory));

            // Remove encryption dictionary if the original was encrypted
            if (password != null && !password.trim().isEmpty()) {
                doc.setAllSecurityToBeRemoved(true);
            }

            doc.save(out);
        } finally {
            doc.close();
        }
        log("Candidate written: " + (out.length() / 1024) + " KB with params quality=" + jpegQuality + ", scale="
                + scale);
        return out;
    }
}
