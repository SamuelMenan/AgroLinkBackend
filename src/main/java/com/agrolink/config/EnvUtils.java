package com.agrolink.config;

import io.github.cdimascio.dotenv.Dotenv;

final class EnvUtils {
    private EnvUtils() {}

    static String getenv(Dotenv d, String key) {
        String sys = System.getenv(key);
        if (sys != null && !sys.isBlank()) return sys.trim();
        String v = d.get(key);
        return v == null ? "" : v.trim();
    }
}
