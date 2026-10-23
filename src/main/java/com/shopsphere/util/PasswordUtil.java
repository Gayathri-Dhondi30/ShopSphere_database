package com.shopsphere.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for hashing and verifying passwords securely
 * to prevent storing plaintext passwords in the database.
 */
public class PasswordUtil {

    private PasswordUtil() {
        // Prevent instantiation
    }

    /**
     * Hashes a plain-text password using SHA-256 algorithm.
     *
     * @param plainPassword Raw password string
     * @return Hexadecimal representation of the SHA-256 hash
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available in current JRE", e);
        }
    }

    /**
     * Verifies if a given plain password matches an existing stored hash.
     *
     * @param plainPassword Raw password input from user login
     * @param storedHash    Hash stored in the database
     * @return true if matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        return hashPassword(plainPassword).equalsIgnoreCase(storedHash);
    }
}
