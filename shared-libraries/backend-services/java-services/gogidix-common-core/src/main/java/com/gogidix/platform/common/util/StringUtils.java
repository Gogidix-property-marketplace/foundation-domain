package com.gogidix.platform.common.util;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for string operations
 * Provides common string manipulation, validation, and generation methods
 */
@UtilityClass
public class StringUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[1-9]\\d{1,14}$"
    );

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Random SECURE_RANDOM = new SecureRandom();

    // Characters for random string generation
    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String NUMERIC_CHARS = "0123456789";
    private static final String ALPHABETIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * Check if string is null or empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Check if string is not null and not empty
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * Check if string is null, empty, or only whitespace
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Check if string is not null, not empty, and not only whitespace
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * Return empty string if null, otherwise return original
     */
    public static String emptyIfNull(String str) {
        return str == null ? "" : str;
    }

    /**
     * Return null if empty string, otherwise return original
     */
    public static String nullIfEmpty(String str) {
        return isEmpty(str) ? null : str;
    }

    /**
     * Truncate string to specified length with ellipsis
     */
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Capitalize first letter of string
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Convert camelCase to snake_case
     */
    public static String toSnakeCase(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Convert snake_case to camelCase
     */
    public static String toCamelCase(String str) {
        if (isEmpty(str)) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;

        for (char c : str.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }

        return result.toString();
    }

    /**
     * Convert string to kebab-case
     */
    public static String toKebabCase(String str) {
        return toSnakeCase(str).replace('_', '-');
    }

    /**
     * Generate random alphanumeric string
     */
    public static String randomAlphanumeric(int length) {
        return randomString(ALPHANUMERIC_CHARS, length);
    }

    /**
     * Generate random numeric string
     */
    public static String randomNumeric(int length) {
        return randomString(NUMERIC_CHARS, length);
    }

    /**
     * Generate random alphabetic string
     */
    public static String randomAlphabetic(int length) {
        return randomString(ALPHABETIC_CHARS, length);
    }

    /**
     * Generate random string from specified characters
     */
    public static String randomString(String source, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(source.charAt(SECURE_RANDOM.nextInt(source.length())));
        }
        return sb.toString();
    }

    /**
     * Generate random UUID
     */
    public static String randomUUID() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Generate random 6-digit OTP
     */
    public static String generateOTP() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1000000));
    }

    /**
     * Mask email address (show first 2 characters and domain)
     */
    public static String maskEmail(String email) {
        if (isEmpty(email) || !email.contains("@")) {
            return email;
        }

        int atIndex = email.indexOf("@");
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (username.length() <= 2) {
            return username + "******" + domain;
        }

        return username.substring(0, 2) + "******" + domain;
    }

    /**
     * Mask phone number (show last 4 digits)
     */
    public static String maskPhone(String phone) {
        if (isEmpty(phone) || phone.length() < 4) {
            return phone;
        }

        return "******" + phone.substring(phone.length() - 4);
    }

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        return isNotEmpty(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate phone number (E.164 format)
     */
    public static boolean isValidPhone(String phone) {
        return isNotEmpty(phone) && PHONE_PATTERN.matcher(phone.replaceAll("[\\s-()]", "")).matches();
    }

    /**
     * Validate UUID format
     */
    public static boolean isValidUUID(String uuid) {
        return isNotEmpty(uuid) && UUID_PATTERN.matcher(uuid).matches();
    }

    /**
     * Generate SHA-256 hash of string
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generate MD5 hash of string
     */
    public static String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * Encode string to Base64
     */
    public static String encodeBase64(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decode Base64 string
     */
    public static String decodeBase64(String input) {
        return new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
    }

    /**
     * Convert bytes to hexadecimal string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Join collection of strings with separator
     */
    public static String join(Collection<String> collection, String separator) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }
        return String.join(separator, collection);
    }

    /**
     * Convert collection to comma-separated string
     */
    public static String toCommaSeparated(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(", ", list);
    }

    /**
     * Convert comma-separated string to list
     */
    public static List<String> fromCommaSeparated(String str) {
        if (isEmpty(str)) {
            return List.of();
        }
        return List.of(str.split("\\s*,\\s*"));
    }

    /**
     * Remove all non-alphanumeric characters
     */
    public static String removeNonAlphanumeric(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replaceAll("[^a-zA-Z0-9]", "");
    }

    /**
     * Count occurrences of substring in string
     */
    public static int countOccurrences(String str, String substring) {
        if (isEmpty(str) || isEmpty(substring)) {
            return 0;
        }

        int count = 0;
        int index = 0;
        while ((index = str.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    /**
     * Check if string contains any of the provided substrings (case-insensitive)
     */
    public static boolean containsAnyIgnoreCase(String str, String... substrings) {
        if (isEmpty(str) || substrings == null || substrings.length == 0) {
            return false;
        }

        String lowerStr = str.toLowerCase();
        for (String substring : substrings) {
            if (isNotEmpty(substring) && lowerStr.contains(substring.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Pad string with leading zeros to specified length
     */
    public static String padWithZeros(String str, int length) {
        if (isEmpty(str)) {
            return "0".repeat(length);
        }
        return String.format("%" + length + "s", str).replace(' ', '0');
    }

    /**
     * Extract numbers from string
     */
    public static String extractNumbers(String str) {
        if (isEmpty(str)) {
            return "";
        }
        return str.replaceAll("[^0-9]", "");
    }

    /**
     * Check if string contains only numbers
     */
    public static boolean isNumeric(String str) {
        return isNotEmpty(str) && str.matches("\\d+");
    }

    /**
     * Check if string contains only alphabetic characters
     */
    public static boolean isAlphabetic(String str) {
        return isNotEmpty(str) && str.matches("[a-zA-Z]+");
    }

    /**
     * Check if string contains only alphanumeric characters
     */
    public static boolean isAlphanumeric(String str) {
        return isNotEmpty(str) && str.matches("[a-zA-Z0-9]+");
    }
}