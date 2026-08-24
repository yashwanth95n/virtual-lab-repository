package com.ledger.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordUtil {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordUtil() {}

    public static String hash(String raw) {
        return ENCODER.encode(raw);
    }

    public static boolean matches(String raw, String hashed) {
        if (raw == null || hashed == null) return false;
        // Support legacy plain-text passwords from older installs
        if (!hashed.startsWith("$2a$") && !hashed.startsWith("$2b$") && !hashed.startsWith("$2y$")) {
            return raw.equals(hashed);
        }
        return ENCODER.matches(raw, hashed);
    }
}
