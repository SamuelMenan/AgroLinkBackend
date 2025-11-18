package com.agrolink.infrastructure.supabase;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class SupabasePostgrestService {
    private final String baseUrl;
    private final String anonKey;
    private final String serviceKey;
    private final RestTemplate rest;

    public SupabasePostgrestService(String baseUrl, String anonKey, String serviceKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.anonKey = anonKey;
        this.serviceKey = serviceKey;
        this.rest = new RestTemplate();
    }

    public ResponseEntity<String> insert(String table, Map<String, Object> payload, String userBearer) {
        if (baseUrl.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Supabase PostgREST no configurado");
        }
        HttpHeaders headers = baseHeaders(userBearer);
        headers.set("Prefer", "return=representation");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        try {
            return rest.postForEntity(baseUrl + "/rest/v1/" + table, entity, String.class);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode().value()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("[SupabasePostgrestService] insert error: " + e.getClass().getName() + " -> " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("PostgREST insert error: " + e.getMessage());
        }
    }

    @SuppressWarnings("null")
    public ResponseEntity<String> get(String table, String query, String userBearer) {
        if (baseUrl.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Supabase PostgREST no configurado");
        }
        HttpHeaders headers = baseHeaders(userBearer);
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        String url = baseUrl + "/rest/v1/" + table + (query == null || query.isBlank() ? "" : (query.startsWith("?") ? query : ("?" + query)));
        try {
            return rest.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode().value()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("[SupabasePostgrestService] get error: " + e.getClass().getName() + " -> " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("PostgREST get error: " + e.getMessage());
        }
    }

    @SuppressWarnings("null")
    public ResponseEntity<String> update(String table, Map<String, Object> payload, String filters, String userBearer) {
        if (baseUrl.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Supabase PostgREST no configurado");
        }
        HttpHeaders headers = baseHeaders(userBearer);
        headers.set("Prefer", "return=representation");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        String url = baseUrl + "/rest/v1/" + table + buildFilterQuery(filters);
        try {
            return rest.exchange(url, HttpMethod.PATCH, entity, String.class);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode().value()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("[SupabasePostgrestService] update error: " + e.getClass().getName() + " -> " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("PostgREST update error: " + e.getMessage());
        }
    }

    @SuppressWarnings("null")
    public ResponseEntity<String> delete(String table, String filters, String userBearer) {
        if (baseUrl.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Supabase PostgREST no configurado");
        }
        HttpHeaders headers = baseHeaders(userBearer);
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        String url = baseUrl + "/rest/v1/" + table + buildFilterQuery(filters);
        try {
            return rest.exchange(url, HttpMethod.DELETE, entity, String.class);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode().value()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("[SupabasePostgrestService] delete error: " + e.getClass().getName() + " -> " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("PostgREST delete error: " + e.getMessage());
        }
    }

    private String buildFilterQuery(String filters) {
        if (filters == null || filters.isBlank()) return "";
        // Expect filters like "?id=eq.uuid" or "?user_id=eq.uuid&status=eq.activo"
        return filters.startsWith("?") ? filters : ("?" + filters);
    }

    private HttpHeaders baseHeaders(String userBearer) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (userBearer != null && !userBearer.isBlank()) {
            headers.set("apikey", anonKey);
            headers.set("Authorization", userBearer.startsWith("Bearer ") ? userBearer : ("Bearer " + userBearer));
        } else {
            headers.set("apikey", serviceKey);
            headers.set("Authorization", "Bearer " + serviceKey);
        }
        return headers;
    }
}
