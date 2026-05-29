package com.iaihub.toolbox.util;

import org.apache.commons.text.StringEscapeUtils;

public final class XssSanitizer {

    private XssSanitizer() {
        // Utility class
    }

    /**
     * Sanitize user input to prevent XSS attacks.
     * This is a basic sanitization - for Markdown content,
     * we allow some HTML-like syntax that markdown-it will render safely.
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }

        // Escape HTML special characters to prevent XSS in rendered content
        String sanitized = StringEscapeUtils.escapeHtml4(input);

        // Remove potential script-related patterns that might slip through
        sanitized = sanitized.replaceAll("(?i)javascript:", "");
        sanitized = sanitized.replaceAll("(?i)on\\w+\\s*=", "");

        return sanitized.trim();
    }

    /**
     * Sanitize for plain text display (no markdown interpretation)
     */
    public static String sanitizePlainText(String input) {
        if (input == null) {
            return null;
        }
        return StringEscapeUtils.escapeHtml4(input);
    }
}
