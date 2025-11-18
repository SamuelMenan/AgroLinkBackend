package com.agrolink.config;

import io.github.cdimascio.dotenv.Dotenv;

final class EnvUtils {
    private EnvUtils() {}

    static String getenv(Dotenv d, String key) {
        String v = d.get(key);
        return v == null ? "" : v.trim();
    }
}
