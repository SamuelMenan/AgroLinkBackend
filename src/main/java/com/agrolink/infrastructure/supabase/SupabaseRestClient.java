package com.agrolink.infrastructure.supabase;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Simple RestTemplate-based Supabase HTTP client.
 */
public class SupabaseRestClient implements SupabaseHttpClient {
    private final String baseUrl;
    private final String serviceKey;
    private final RestTemplate rest;

    public SupabaseRestClient(String baseUrl, String serviceKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.serviceKey = serviceKey;
        this.rest = new RestTemplate();
    }

    @Override
    public void insert(String table, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", serviceKey);
        headers.set("Authorization", "Bearer " + serviceKey);
        headers.set("Prefer", "return=representation");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        String url = baseUrl + "/rest/v1/" + table;
        rest.postForEntity(url, entity, String.class);
    }
}
