package com.agrolink.moderation;

public final class ProfanityDictionary {
    private static final ProfanityDictionary INSTANCE = new ProfanityDictionary();

    private ProfanityDictionary() {}

    public static ProfanityDictionary getInstance() {
        return INSTANCE;
    }

    public boolean contains(String text) {
        // base stub: no words flagged
        return false;
    }
}
