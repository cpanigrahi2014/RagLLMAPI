package com.ragllm.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Utility to sanitize user inputs and protect against prompt injection attacks.
 */
public final class InputSanitizer {

    private static final Logger log = LoggerFactory.getLogger(InputSanitizer.class);

    // Patterns that may indicate prompt injection attempts
    private static final Pattern[] INJECTION_PATTERNS = {
            Pattern.compile("ignore\\s+(previous|above|all)\\s+instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you\\s+are\\s+now", Pattern.CASE_INSENSITIVE),
            Pattern.compile("system\\s*:\\s*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\{\\{.*?\\}\\}", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<%.*?%>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<script.*?>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\$\\{.*?\\}", Pattern.CASE_INSENSITIVE),
    };

    private static final int MAX_QUERY_LENGTH = 2000;

    private InputSanitizer() {
    }

    /**
     * Sanitize a user query string for safe use in RAG prompts.
     */
    public static String sanitizeQuery(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }

        // Trim and limit length
        String sanitized = input.trim();
        if (sanitized.length() > MAX_QUERY_LENGTH) {
            sanitized = sanitized.substring(0, MAX_QUERY_LENGTH);
        }

        // Check for prompt injection patterns
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(sanitized).find()) {
                log.warn("Potential prompt injection detected: {}", sanitized.substring(0, Math.min(100, sanitized.length())));
                throw new IllegalArgumentException("Query contains disallowed patterns");
            }
        }

        // Remove control characters
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

        return sanitized;
    }

    /**
     * Sanitize text content for storage.
     */
    public static String sanitizeContent(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                     .trim();
    }
}
