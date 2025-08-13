package com.pdfutilities.app.service;

import java.io.File;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

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
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            return doc.isEncrypted();
        } catch (InvalidPasswordException e) {
            return true;
        } catch (Exception e) {
            // On other errors, assume not encrypted so we don't block workflows
            return false;
        }
    }
}
