package com.studenthub.repository.spec;

import java.util.Locale;

/** Helpers shared by the JPA Specifications. */
public final class SpecUtils {

    /**
     * LIKE escape character. '!' rather than '\' because MySQL also treats a backslash
     * as an escape inside string literals, which makes "ESCAPE '\'" fragile.
     */
    public static final char ESCAPE = '!';

    private SpecUtils() {
    }

    /**
     * Builds a case-insensitive "contains" LIKE pattern, escaping %, _ and the escape
     * character itself so user input is matched literally. Values are always bound as
     * parameters, never concatenated into SQL.
     */
    public static String containsPattern(String raw) {
        String escaped = raw.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
