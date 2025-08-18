package com.pdfutilities.app.service;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Utility helpers for PDF security checks.
 */
public final class PdfSecurityUtils {

    private PdfSecurityUtils() {
    }

    /**
     * Returns true if the given PDF file is password protected (requires a password
     * to open).
     */
    public static boolean isPasswordProtected(File pdfFile) {
        if (pdfFile == null)
            return false;
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            return document.isEncrypted();
        } catch (IOException e) {
            // If we get here, the PDF might be encrypted
            return true;
        } catch (Exception e) {
            // On other errors, assume not encrypted so we don't block workflows
            return false;
        }
    }
}
