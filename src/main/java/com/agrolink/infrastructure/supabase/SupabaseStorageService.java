package com.agrolink.infrastructure.supabase;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

public class SupabaseStorageService {
    private final String baseUrl;
    private final String serviceKey;
    private final RestTemplate rest;

    public SupabaseStorageService(String baseUrl, String serviceKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.serviceKey = serviceKey;
        this.rest = new RestTemplate();
    }

    public ResponseEntity<String> upload(String bucket, String path, byte[] content, String contentType) {
        if (baseUrl.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Supabase Storage no configurado");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", serviceKey);
        headers.set("Authorization", "Bearer " + serviceKey);
        headers.setContentType(MediaType.parseMediaType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE));
        HttpEntity<byte[]> entity = new HttpEntity<>(content, headers);
        String url = baseUrl + "/storage/v1/object/" + bucket + "/" + path;
        return rest.postForEntity(url, entity, String.class);
    }

    public String publicUrl(String bucket, String path) {
        if (baseUrl.isBlank()) {
            return "supabase-storage-not-configured";
        }
        return baseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }
}
