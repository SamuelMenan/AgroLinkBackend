package com.agrolink.infrastructure.supabase;

import java.util.Map;

/**
 * Minimal interface representing an external Supabase HTTP client.
 */
public interface SupabaseHttpClient {
    void insert(String table, Map<String, Object> payload);
}
