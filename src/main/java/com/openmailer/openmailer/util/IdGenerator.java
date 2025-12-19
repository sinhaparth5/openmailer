package com.openmailer.openmailer.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for generating custom IDs with "opm_" prefix.
 * Generates IDs in the format: opm_<short_hash>
 */
public class IdGenerator {

    private static final String PREFIX = "opm_";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int HASH_LENGTH = 16; // Length of the hash part

    /**
     * Generate a unique ID with "opm_" prefix followed by a short hash.
     * Format: opm_<16_character_hash>
     *
     * @return the generated ID
     */
    public static String generateId() {
        try {
            // Generate random bytes
            byte[] randomBytes = new byte[16];
            SECURE_RANDOM.nextBytes(randomBytes);

            // Add timestamp for additional uniqueness
            long timestamp = System.currentTimeMillis();
            String input = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes) + timestamp;

            // Create SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Encode to base64 and take first HASH_LENGTH characters
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            String shortHash = encoded.substring(0, HASH_LENGTH).toLowerCase();

            return PREFIX + shortHash;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ID", e);
        }
    }

    /**
     * Validate if a string is a valid OpenMailer ID.
     *
     * @param id the ID to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidId(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }

        // Check if it starts with the prefix
        if (!id.startsWith(PREFIX)) {
            return false;
        }

        // Check if the length is correct
        int expectedLength = PREFIX.length() + HASH_LENGTH;
        if (id.length() != expectedLength) {
            return false;
        }

        // Check if the hash part contains only valid characters
        String hashPart = id.substring(PREFIX.length());
        return hashPart.matches("[a-z0-9_-]+");
    }
}
