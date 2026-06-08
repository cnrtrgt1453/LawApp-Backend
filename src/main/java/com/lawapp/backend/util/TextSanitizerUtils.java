package com.lawapp.backend.util;

import java.util.regex.Pattern;

public class TextSanitizerUtils {

    // Matches Turkish phone numbers e.g., 05xx xxx xx xx, 5xx-xxx-xx-xx, +905xx
    private static final String PHONE_REGEX = "(?:(?:\\+|00)90|0?)[ -]?5[0-9]{2}[ -]?[0-9]{3}[ -]?[0-9]{2}[ -]?[0-9]{2}";

    // A simplified list of inappropriate words
    private static final String[] PROFANITY_LIST = { "aptal", "gerizekalı", "şerefsiz", "salak" };

    public static String maskSensitiveData(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String result = input;

        // Mask phone numbers
        Pattern phonePattern = Pattern.compile(PHONE_REGEX);
        result = phonePattern.matcher(result).replaceAll("[KVKK GEREĞİ GİZLENDİ]");

        // Mask profanity
        for (String word : PROFANITY_LIST) {
            // Case insensitive replacement using regex
            Pattern wordPattern = Pattern.compile("(?i)\\b" + Pattern.quote(word) + "\\b");
            result = wordPattern.matcher(result).replaceAll("[KVKK GEREĞİ GİZLENDİ]");
        }

        return result;
    }
}
