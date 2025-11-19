package com.agrolink.infrastructure.supabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.Map;

public class SupabasePostgrestService {
    private static final Logger log = LoggerFactory.getLogger(SupabasePostgrestService.class);
    private final String baseUrl;
    private final String anonKey;
    private final String serviceKey;
    private final RestTemplate rest;

    public SupabasePostgrestService(String baseUrl, String anonKey, String serviceKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.anonKey = anonKey;
        this.serviceKey = serviceKey;
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(5000);
        rf.setReadTimeout(10000);
        this.rest = new RestTemplate(rf);
    }

    public ResponseEntity<String> insert(String table, Map<String, Object> payload, String userBearer) {
        if (baseUrl.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Supabase PostgREST no configurado");
        }
        HttpHeaders headers = baseHeaders(userBearer);
        headers.set("Prefer", "return=representation");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        try {
            String url = baseUrl + "/rest/v1/" + table;
            long start = System.currentTimeMillis();
            log.debug("[SupabasePostgrestService] POST {}", url);
            ResponseEntity<String> response = rest.postForEntity(url, entity, String.class);
            log.debug("[SupabasePostgrestService] POST {} -> status={} in {}ms", url, response.getStatusCode().value(), System.currentTimeMillis() - start);
            return response;
        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.error("[SupabasePostgrestService] insert error (RestClientResponseException): status={} body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode().value()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[SupabasePostgrestService] insert error: {} -> {}", e.getClass().getName(), e.getMessage(), e);
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
            long start = System.currentTimeMillis();
            log.debug("[SupabasePostgrestService] GET {}", url);
            ResponseEntity<String> response = rest.exchange(url, HttpMethod.GET, entity, String.class);
            log.debug("[SupabasePostgrestService] GET {} -> status={} in {}ms", url, response.getStatusCode().value(), System.currentTimeMillis() - start);
            return response;
        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.error("[SupabasePostgrestService] get error (RestClientResponseException): status={} body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode().value()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[SupabasePostgrestService] get error: {} -> {}", e.getClass().getName(), e.getMessage(), e);
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
            long start = System.currentTimeMillis();
            log.debug("[SupabasePostgrestService] PATCH {}", url);
            ResponseEntity<String> response = rest.exchange(url, HttpMethod.PATCH, entity, String.class);
            log.debug("[SupabasePostgrestService] PATCH {} -> status={} in {}ms", url, response.getStatusCode().value(), System.currentTimeMillis() - start);
            return response;
        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.error("[SupabasePostgrestService] update error (RestClientResponseException): status={} body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode().value()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[SupabasePostgrestService] update error: {} -> {}", e.getClass().getName(), e.getMessage(), e);
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
            long start = System.currentTimeMillis();
            log.debug("[SupabasePostgrestService] DELETE {}", url);
            ResponseEntity<String> response = rest.exchange(url, HttpMethod.DELETE, entity, String.class);
            log.debug("[SupabasePostgrestService] DELETE {} -> status={} in {}ms", url, response.getStatusCode().value(), System.currentTimeMillis() - start);
            return response;
        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.error("[SupabasePostgrestService] delete error (RestClientResponseException): status={} body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode().value()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[SupabasePostgrestService] delete error: {} -> {}", e.getClass().getName(), e.getMessage(), e);
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
