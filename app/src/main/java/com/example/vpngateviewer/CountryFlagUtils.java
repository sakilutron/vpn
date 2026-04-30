package com.example.vpngateviewer;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for converting ISO 3166-1 alpha-2 country codes to Unicode
 * regional indicator symbols (flag emoji).
 * <p>
 * Thread-safe. Results are memoized for repeated lookups of the same code.
 */
public final class CountryFlagUtils {

    private static final int REGIONAL_INDICATOR_OFFSET = 0x1F1E6 - 'A';
    private static final Map<String, String> FLAG_CACHE = new HashMap<>();

    private CountryFlagUtils() {
        throw new AssertionError("Utility class — do not instantiate");
    }

    /**
     * Converts a two-letter country code to its flag emoji.
     *
     * @param countryCode ISO 3166-1 alpha-2 code (e.g. "US", "JP")
     * @return flag emoji string, or empty string for invalid input
     */
    public static String countryCodeToFlag(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) {
            return "";
        }

        String upper = countryCode.toUpperCase();
        synchronized (FLAG_CACHE) {
            String cached = FLAG_CACHE.get(upper);
            if (cached != null) {
                return cached;
            }
        }

        char first = upper.charAt(0);
        char second = upper.charAt(1);
        if (first < 'A' || first > 'Z' || second < 'A' || second > 'Z') {
            return "";
        }

        int firstCode = first + REGIONAL_INDICATOR_OFFSET;
        int secondCode = second + REGIONAL_INDICATOR_OFFSET;
        String flag = new String(Character.toChars(firstCode))
                + new String(Character.toChars(secondCode));

        synchronized (FLAG_CACHE) {
            FLAG_CACHE.put(upper, flag);
        }
        return flag;
    }

    /** Clears the internal flag cache. Useful for testing. */
    static void clearCache() {
        synchronized (FLAG_CACHE) {
            FLAG_CACHE.clear();
        }
    }
}