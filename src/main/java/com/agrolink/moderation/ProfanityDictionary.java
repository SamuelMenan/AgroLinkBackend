package com.agrolink.moderation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ProfanityDictionary {
    private static final ProfanityDictionary INSTANCE = new ProfanityDictionary();
    private final Set<String> words;

    private ProfanityDictionary() {
        this.words = loadFromResource("profanity.txt");
    }

    public static ProfanityDictionary getInstance() {
        return INSTANCE;
    }

    public boolean contains(String text) {
        if (text == null || text.isBlank() || words.isEmpty()) return false;
        var tokens = tokenize(text);
        for (var t : tokens) {
            if (words.contains(t)) return true;
        }
        return false;
    }

    public Set<String> findAll(String text) {
        if (text == null || text.isBlank() || words.isEmpty()) return Collections.emptySet();
        var tokens = tokenize(text);
        Set<String> found = new HashSet<>();
        for (var t : tokens) {
            if (words.contains(t)) found.add(t);
        }
        return found;
    }

    public String mask(String text) {
        if (text == null || text.isBlank() || words.isEmpty()) return text;
        String masked = text;
        for (String w : words) {
            String regex = "(?i)\\b" + Pattern.quote(w) + "\\b";
            masked = masked.replaceAll(regex, repeat('*', w.length()));
        }
        return masked;
    }

    private static String[] tokenize(String text) {
        return text.toLowerCase(Locale.ROOT).split("\\W+");
    }

    private static String repeat(char c, int times) {
        if (times <= 0) return "";
        char[] arr = new char[times];
        for (int i = 0; i < times; i++) arr[i] = c;
        return new String(arr);
    }

    private static Set<String> loadFromResource(String resourceName) {
        Set<String> set = new HashSet<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = ProfanityDictionary.class.getClassLoader();
        try (InputStream is = cl.getResourceAsStream(resourceName)) {
            if (is == null) return Collections.unmodifiableSet(set);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String w = line.trim().toLowerCase(Locale.ROOT);
                    if (w.isEmpty() || w.startsWith("#")) continue;
                    set.add(w);
                }
            }
        } catch (IOException ignored) {
        }
        return Collections.unmodifiableSet(set);
    }
}
